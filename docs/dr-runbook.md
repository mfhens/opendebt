# OpenDebt — Disaster Recovery Runbook

**ADR:** ADR-0028  
**RTO target:** 4 hours  
**RPO target:** 4 hours  
**Last tested:** *(update after each drill)*  
**Runbook owner:** Platform / Operations team  

> **This document must be accessible outside the primary system.** Store a copy in the operations
> wiki, a printed binder, and/or a separate read-only storage location. During a DR event the
> primary system may be unavailable.

---

## 1. Failure Classification

Before starting the DR procedure, classify the failure:

| Symptom | Classification | Action |
|---------|---------------|--------|
| One or more pods crash-looping | Pod failure | Wait for K8s self-heal; if >10 min, escalate |
| One service returns 503, others OK | Service failure | Check circuit breaker state, logs; no DR |
| PostgreSQL primary unreachable, **standby is healthy** | DB primary failure | Go to **Section 3** (Standby Failover) |
| PostgreSQL data corruption or accidental deletion | Data-level failure | Go to **Section 4** (Full PITR Restore) |
| Encryption key vault unreachable | Key vault failure | Go to **Section 6** (Key Vault Emergency) |
| Full datacenter / cluster loss | Catastrophic failure | Go to **Section 5** (Catastrophic DR) |

---

## 2. DR Declaration

A DR event must be formally declared before executing Sections 3–6. This triggers the SLA clock.

**Declare DR when:**
- PostgreSQL data is corrupt or lost and cannot be recovered without a backup restore.
- The primary database host is unrecoverable (hardware failure, cloud instance termination).
- A full datacenter or Kubernetes cluster is unavailable.

**Declaration steps:**

1. Notify the incident commander and platform team.
2. Open an incident ticket: label `DR`, record declaration time.
3. Inform UFST operations of the declared DR event and expected recovery time.
4. Assign roles: **DR Lead** (owns the procedure), **DBA** (PostgreSQL restore), **Verifier** (smoke tests).

---

## 3. Standby Failover (RTO ~30 min)

Use when the PostgreSQL primary is down but streaming replication standby is healthy.

**Estimated time: 15–30 minutes**

### Step 3.1 — Confirm primary is unreachable

```bash
psql -h <primary-host> -U opendebt -c "SELECT 1;"
# Should time out or refuse connection
```

### Step 3.2 — Confirm standby is healthy

```bash
psql -h <standby-host> -U opendebt -c "SELECT pg_is_in_recovery();"
# Expected: t (true = still in recovery / standby mode)
```

### Step 3.3 — Promote the standby

```bash
# On the standby host
pg_ctl promote -D /var/lib/postgresql/data
# Or via signal file:
touch /var/lib/postgresql/data/failover.signal
```

Wait ~30 seconds, then verify:

```bash
psql -h <standby-host> -U opendebt -c "SELECT pg_is_in_recovery();"
# Expected: f (false = now primary)
```

### Step 3.4 — Update Kubernetes configuration

```bash
# Update the database hostname in the Kubernetes ConfigMap
kubectl -n opendebt-production patch configmap opendebt-config \
  --type merge \
  -p '{"data": {"DB_HOST": "<standby-host>"}}'

# Rolling restart all application pods
kubectl -n opendebt-production rollout restart deployment --all

# Wait for rollout
kubectl -n opendebt-production rollout status deployment --all
```

### Step 3.5 — Verify health

```bash
# Check all pods are Running
kubectl -n opendebt-production get pods

# Spot-check API health
curl -f https://opendebt.dk/case-service/actuator/health
curl -f https://opendebt.dk/debt-service/actuator/health
```

### Step 3.6 — Record actual RTO and close incident

Record standby promotion time minus declaration time. Update the tested RTO in this runbook header.

---

## 4. Full PITR Restore from pgBackRest (RTO ~2–3 hours)

Use when there is no healthy standby and data must be restored from backup.

**Estimated time: 90–150 minutes**

### Step 4.1 — Determine the restore target time

Establish the latest safe point-in-time — the moment *before* the failure or corruption:

```bash
# List available WAL archive range
pgbackrest --stanza=opendebt info

# Check audit_log for the last known good operation
psql -h <backup-restore-host> -U opendebt -c \
  "SELECT MAX(timestamp) FROM audit_log WHERE table_name NOT LIKE '%_history';"
```

Choose a restore target time `T` that is:
- After the most recent full or differential backup.
- Before the first sign of corruption/loss.
- Within the WAL archive window (typically 7 days).

### Step 4.2 — Provision restore environment

> If restoring in-place, stop all application pods first.

```bash
# Stop all OpenDebt services to prevent writes during restore
kubectl -n opendebt-production scale deployment --all --replicas=0

# Confirm all pods are terminated
kubectl -n opendebt-production get pods
```

### Step 4.3 — Stop PostgreSQL

```bash
# On the PostgreSQL host
systemctl stop postgresql
# Or for containerised PostgreSQL:
kubectl -n opendebt-production scale deployment postgres --replicas=0
```

### Step 4.4 — Restore from pgBackRest

```bash
# PITR restore to chosen target time T
pgbackrest --stanza=opendebt \
  --delta \
  --target="<T in ISO 8601, e.g. 2026-03-25 10:00:00 Europe/Copenhagen>" \
  --target-action=promote \
  --target-timeline=latest \
  restore

# Expected output: "restore command end: completed successfully"
```

This replays WAL segments from the backup to reconstruct the database state at time T.

### Step 4.5 — Start PostgreSQL and verify restore

```bash
systemctl start postgresql
# Wait ~60 seconds for WAL replay to complete

psql -U opendebt -c "SELECT now();"
psql -U opendebt -d opendebt_debt -c "SELECT COUNT(*) FROM debts;"
psql -U opendebt -d opendebt_person -c "SELECT COUNT(*) FROM persons;"
```

### Step 4.6 — Reconcile Flowable in-flight processes

After a PITR restore, Flowable processes that ran between time T and the failure need reconciliation.

```bash
# Start services with async executor disabled
# Set in the opendebt-config ConfigMap before scaling up:
kubectl -n opendebt-production patch configmap opendebt-config \
  --type merge \
  -p '{"data": {"FLOWABLE_ASYNC_EXECUTOR_ACTIVATE": "false"}}'
```

Scale up only `case-service` to perform reconciliation:

```bash
kubectl -n opendebt-production scale deployment opendebt-case-service-production --replicas=1
```

Connect to the case-service admin endpoint to list in-flight processes:

```bash
curl -H "Authorization: Bearer <admin-token>" \
  https://opendebt.dk/case-service/api/v1/cases?status=OPEN
```

For each open case:

1. Compare the case's `updatedAt` to restore time T.
2. Cases with `updatedAt` > T were modified after the restore point — review manually.
3. Check the timeline via `GET /cases/{id}/tidslinje` to identify what happened after T.
4. Re-apply any post-T mutations (write-downs, letter sends, status transitions) as needed.

```sql
-- Query Flowable job table for stuck/pending jobs at restore point
SELECT id, handler_type, retries, exception_message, create_time
FROM opendebt_case.act_ru_job
WHERE retries < 3
ORDER BY create_time;
```

Resolve each stuck job, then re-enable the async executor:

```bash
kubectl -n opendebt-production patch configmap opendebt-config \
  --type merge \
  -p '{"data": {"FLOWABLE_ASYNC_EXECUTOR_ACTIVATE": "true"}}'

kubectl -n opendebt-production rollout restart deployment opendebt-case-service-production
```

### Step 4.7 — Scale up all services

```bash
kubectl -n opendebt-production scale deployment --all --replicas=3
kubectl -n opendebt-production rollout status deployment --all
```

### Step 4.8 — Smoke tests

Run the smoke test checklist (Section 7).

### Step 4.9 — Record RTO and close incident

---

## 5. Catastrophic Failure — Full Cluster Recovery

Use when the entire Kubernetes cluster or datacenter is unavailable.

**Estimated time: 2–4 hours**

### Step 5.1 — Provision new Kubernetes cluster

Follow UFST Horizontale Driftsplatform provisioning procedure for a new cluster.

### Step 5.2 — Restore namespace via ArgoCD

```bash
# ArgoCD is source of truth for all K8s manifests
argocd app sync opendebt-production

# Or apply manually from git
kubectl apply -k k8s/overlays/production/
```

### Step 5.3 — Retrieve encryption key from key vault

```bash
# Retrieve ENCRYPTION_KEY from Azure Key Vault (or platform equivalent)
az keyvault secret show \
  --vault-name opendebt-keyvault \
  --name ENCRYPTION_KEY \
  --query value -o tsv
```

Update the Kubernetes secret:

```bash
kubectl -n opendebt-production create secret generic opendebt-secrets \
  --from-literal=ENCRYPTION_KEY=<retrieved-value> \
  --dry-run=client -o yaml | kubectl apply -f -
```

### Step 5.4 — Restore PostgreSQL

Provision a new PostgreSQL instance and run Section 4 (PITR restore from pgBackRest).

### Step 5.5 — Restore Keycloak

```bash
# Start Keycloak pointed at restored DB
# Import realm from git if user DB is also lost:
kc.sh import --file config/keycloak/opendebt-realm.json
```

If user accounts were in the Keycloak DB and are not in the realm export, restore from the nightly
`kc.sh export` backup in the pgBackRest repository.

### Step 5.6 — Scale up services and smoke test (Section 7)

---

## 6. Encryption Key Vault Emergency

The `ENCRYPTION_KEY` controls all PII encryption in `person-registry`. If the key vault is
unreachable, `person-registry` cannot decrypt PII — all CPR/CVR lookups will fail.

**This is a Sev-1 incident. Escalate immediately to platform and security teams.**

### Step 6.1 — Verify key vault availability

```bash
az keyvault show --name opendebt-keyvault
# Check Azure Service Health for key vault outage
```

### Step 6.2 — If key vault is temporarily unavailable (transient outage)

`person-registry` caches the encryption key in memory for the lifetime of the JVM. If the service
has not been restarted since the key vault became unavailable, it continues to function normally.

- Do **not** restart `person-registry` pods during a key vault outage.
- Wait for key vault availability to be restored.
- Monitor `person-registry` health endpoint.

### Step 6.3 — If key vault is permanently lost

This is a catastrophic scenario requiring escalation to the platform provider.

1. Retrieve the key from the key vault backup/export (each key vault provider has a key export procedure).
2. Provision a new key vault with the retrieved key.
3. Update Kubernetes secrets to point to the new key vault.
4. Perform a rolling restart of `person-registry` only.

---

## 7. Smoke Test Checklist

Run after any DR recovery before declaring service restored.

| Test | Command / Check | Expected result |
|------|----------------|-----------------|
| All pods running | `kubectl -n opendebt-production get pods` | All `Running`, 0 `CrashLoopBackOff` |
| debt-service health | `curl -f .../debt-service/actuator/health` | `{"status":"UP"}` |
| case-service health | `curl -f .../case-service/actuator/health` | `{"status":"UP"}` |
| payment-service health | `curl -f .../payment-service/actuator/health` | `{"status":"UP"}` |
| person-registry health | `curl -f .../person-registry/actuator/health` | `{"status":"UP"}` |
| Keycloak login | Login with `caseworker`/`caseworker123` (staging) | Token returned |
| CPR lookup | `POST /api/v1/persons/lookup` with test CPR | Returns UUID (no PII leak) |
| Debt list | `GET /api/v1/debts` (with CASEWORKER token) | Returns list |
| Audit log intact | `SELECT COUNT(*) FROM opendebt_debt.audit_log` | Non-zero, recent entries |
| Flowable jobs | `SELECT COUNT(*) FROM opendebt_case.act_ru_job WHERE exception_message IS NOT NULL` | 0 (or known residual) |
| Circuit breakers | Grafana → Resilience4j dashboard | All CLOSED |
| WAL archiving | `pgbackrest --stanza=opendebt check` | No errors |

---

## 8. DR Drill Procedure (Quarterly)

DR capability degrades without regular testing. Run this drill every quarter in a **staging** clone:

1. **Snapshot** current staging database state.
2. **Simulate failure**: drop and recreate the PostgreSQL schema.
3. **Execute** the PITR restore procedure (Section 4) against staging.
4. **Measure** actual restore time (start → smoke tests pass).
5. **Compare** actual RTO against the 4-hour target.
6. **Update** the tested RTO timestamp in this runbook's header.
7. **File a report** in the incident tracker with lessons learned.
8. **Update** this runbook if any step was unclear or incorrect.

---

## 9. Contact List

| Role | Responsibility | Contact |
|------|---------------|---------|
| DR Lead | Owns the procedure, coordinates teams | *(fill in)* |
| DBA / Platform Engineer | PostgreSQL restore, pgBackRest | *(fill in)* |
| UFST Operations | Platform SLA, key vault access | *(fill in)* |
| Security Officer | Encryption key emergency | *(fill in)* |
| Gældsstyrelsen liaison | Regulatory notification if data loss | *(fill in)* |

---

## 10. Recovery Time Log

| Date | DR type | Declared at | Resolved at | Actual RTO | Notes |
|------|---------|-------------|-------------|-----------|-------|
| *(drill)* | PITR restore | — | — | — | *(First drill — not yet run)* |
