# ADR 0028: Backup and Disaster Recovery Strategy

## Status

Accepted

## Context

OpenDebt handles sensitive financial data for Danish public institutions and operates under GDPR,
Rigsarkivet, and Gældsstyrelsen compliance requirements. A formal Disaster Recovery (DR) strategy
is required to underpin any production SLA commitment.

The following RTO and RPO targets have been agreed:

| Target | Value | Definition |
|--------|-------|------------|
| **RTO** | **4 hours** | Maximum time from declaring a DR event to restored service |
| **RPO** | **4 hours** | Maximum data loss window acceptable in a worst-case failure |

### Current state before this ADR

| Component | DR-relevant capability present | Gap |
|-----------|-------------------------------|-----|
| Stateless services (K8s, 3 replicas in prod) | ✅ Self-healing, pod anti-affinity | None for pod/node failures |
| PostgreSQL | ✅ Temporal tables, audit_log (ADR-0013) | ❌ No WAL archiving, no base backups, no standby |
| Keycloak | ✅ Realm config in git (`config/keycloak/`) | ❌ No user DB backup |
| Encryption key (`ENCRYPTION_KEY`) | ❌ Set via env var only | ❌ No key vault, key loss = all PII irrecoverable |
| Flowable BPMN state | ✅ Stored in PostgreSQL | ❌ No in-flight process reconciliation strategy |
| Kubernetes manifests | ✅ In git, ArgoCD GitOps | ❌ No PVC snapshot policy documented |
| Observability stack | ✅ Grafana/Loki/Tempo (ADR-0024) | ❌ No backup monitoring alerts |

A point-in-time *query* capability (ADR-0013 temporal tables) is not equivalent to a point-in-time
*restore* capability. These are complementary but distinct.

### Scope of a DR event

This ADR covers two distinct failure classes:

1. **Service-level failure** — single pod, node, or service crashes. Kubernetes self-healing handles
   this within minutes. No DR declaration required.
2. **Data-level failure** — database corruption, accidental mass deletion, datacenter loss, storage
   failure. This ADR addresses this class. DR declaration required.

### Databases in scope

OpenDebt runs 8 application databases on a shared PostgreSQL instance plus a separate Keycloak DB:

| Database | Owner service | Criticality |
|----------|--------------|-------------|
| `opendebt_person` | person-registry | **CRITICAL** — GDPR vault, encrypted PII |
| `opendebt_debt` | debt-service | **CRITICAL** — fordringer, ledger, financial state |
| `opendebt_case` | case-service | **HIGH** — case workflow, Flowable state |
| `opendebt_payment` | payment-service | **HIGH** — payment records, bookkeeping |
| `opendebt_creditor` | creditor-service | **HIGH** — creditor master data |
| `opendebt_letter` | letter-service | **MEDIUM** — letter delivery records |
| `opendebt_offsetting` | debt-service (ADR-0027) | **HIGH** — offsetting transactions |
| `opendebt_wage_garnishment` | wage-garnishment-service | **HIGH** — garnishment records |
| Keycloak DB | Keycloak | **HIGH** — user accounts, roles |

## Decision

We adopt a layered backup and DR strategy using **pgBackRest** for PostgreSQL, **Azure Key Vault**
(or platform-equivalent) for the encryption key, and documented runbook procedures. The strategy
is designed to comfortably meet RTO 4h / RPO 4h with realistic restore times of 1–2 hours.

### Layer 1 — Encryption Key Vault (highest priority)

The `ENCRYPTION_KEY` used by `person-registry` for AES field-level encryption must be stored in a
managed key vault **before any backup strategy is meaningful**. Loss of this key makes all
encrypted PII permanently irrecoverable — a worse outcome than data loss.

```
Platform key vault (Azure Key Vault / HashiCorp Vault)
  └── opendebt/person-registry/ENCRYPTION_KEY
        ├── current version (active)
        └── previous versions (retained for 90 days)
```

**Requirements:**
- Key vault itself must be in a region / redundancy tier that survives a datacenter failure.
- Key rotation procedure must be documented (rotate key, re-encrypt existing records).
- Key vault access is audited (who fetched the key and when).
- Services retrieve the key at startup via the platform secret management integration;
  the key is never committed to git.

### Layer 2 — PostgreSQL Continuous Backup (pgBackRest)

We use **pgBackRest** as the PostgreSQL backup tool for all production databases.

#### Why pgBackRest over alternatives

| Tool | Reason not chosen |
|------|------------------|
| `pg_dump` | Logical backup only; no WAL archiving; restore takes longer; no PITR |
| `WAL-G` | Good alternative; pgBackRest chosen for richer retention policy management and native Kubernetes integration |
| Platform-managed PostgreSQL snapshots | Acceptable if the platform provides it; pgBackRest is the fallback for self-managed PostgreSQL |

#### Backup schedule

| Backup type | Frequency | Retention |
|-------------|-----------|-----------|
| Full base backup | Weekly (Sunday 02:00) | 4 weeks |
| Differential backup | Daily (02:00, Mon–Sat) | 2 weeks |
| WAL archiving | Continuous (every WAL segment, ~16 MB or ~5 min) | 7 days |

With this schedule, worst-case RPO is the time since the last WAL segment was archived. In
practice this is under 5 minutes — well within the 4-hour target.

#### pgBackRest configuration skeleton

```ini
[global]
# Object storage (Azure Blob Storage or S3-compatible)
repo1-type=azure
repo1-azure-container=opendebt-pgbackrest
repo1-azure-account=${AZURE_STORAGE_ACCOUNT}
repo1-azure-key=${AZURE_STORAGE_KEY}

# Encryption (backup repository encrypted at rest)
repo1-cipher-type=aes-256-cbc
repo1-cipher-pass=${PGBACKREST_CIPHER_PASSPHRASE}

# Retention
repo1-retention-full=4
repo1-retention-diff=14

[opendebt]
pg1-path=/var/lib/postgresql/data
pg1-port=5432

[global:archive-push]
compress-level=3
```

#### PostgreSQL WAL archiving configuration

```
# postgresql.conf additions
wal_level = replica
archive_mode = on
archive_command = 'pgbackrest --stanza=opendebt archive-push %p'
archive_timeout = 300   # Force WAL switch after 5 min even if no writes
```

#### Restore procedure (PITR)

```bash
# Stop PostgreSQL
systemctl stop postgresql

# Restore to a specific point in time (e.g., 2 hours before disaster)
pgbackrest --stanza=opendebt --delta \
  --target="2026-03-25 10:00:00 Europe/Copenhagen" \
  --target-action=promote \
  restore

# Start PostgreSQL and verify
systemctl start postgresql
psql -c "SELECT now(), pg_is_in_recovery();"
```

Full restore from a daily differential backup + WAL replay is expected to take **20–45 minutes**
for an OpenDebt-scale database, leaving ample margin within the 4-hour RTO.

### Layer 3 — PostgreSQL Hot Standby (streaming replication)

A streaming replication standby eliminates most single-host failure scenarios without needing a
full restore from backup.

```
Primary PostgreSQL ──WAL stream──▶ Standby PostgreSQL (hot standby)
       │                                    │
       └────── pgBackRest ──────────────────┘
                (both primary and standby archive WAL)
```

**Failover procedure (manual, <30 min):**

1. Confirm primary is unreachable.
2. Promote standby: `pg_ctl promote -D /var/lib/postgresql/data`
3. Update `PGHOST` in Kubernetes ConfigMap / Secrets to point to standby.
4. Perform rolling restart of all application pods.
5. Verify health endpoints.

When streaming replication is in place, a primary failure is recoverable in **15–30 minutes** —
well within RTO, without touching backups.

### Layer 4 — Keycloak Backup

Keycloak requires two complementary backups:

1. **Realm configuration** — already covered: `config/keycloak/*.json` is in git.
2. **Keycloak database** — the Keycloak PostgreSQL database must be included in the pgBackRest
   stanza or backed up separately with the same schedule.

```ini
# Add to pgBackRest config
[opendebt-keycloak]
pg1-path=/var/lib/postgresql/keycloak-data
```

Keycloak also supports a `kc.sh export` command for a full realm + user export. This should run
nightly to a separate file in the backup repository:

```bash
# Nightly Keycloak export (CronJob)
kc.sh export --realm opendebt --users realm_file \
  --dir /backups/keycloak/$(date +%Y-%m-%d)/
```

### Layer 5 — Kubernetes PVC Snapshots (Velero)

If production uses Kubernetes PersistentVolumes for PostgreSQL storage, **Velero** with the CSI
snapshot plugin provides cluster-level volume backup.

```yaml
# VolumeSnapshotClass for Velero
apiVersion: snapshot.storage.k8s.io/v1
kind: VolumeSnapshotClass
metadata:
  name: opendebt-csi-snapclass
  labels:
    velero.io/csi-volumesnapshot-class: "true"
driver: disk.csi.azure.com  # or equivalent for UFST HDP
deletionPolicy: Retain
```

Velero schedule:

```yaml
apiVersion: velero.io/v1
kind: Schedule
metadata:
  name: opendebt-daily-backup
  namespace: velero
spec:
  schedule: "0 3 * * *"   # 03:00 daily
  template:
    includedNamespaces:
      - opendebt-production
    storageLocation: default
    volumeSnapshotLocations:
      - default
    ttl: 720h   # 30 days
```

Note: pgBackRest is the authoritative data backup. Velero PVC snapshots are a complementary
cluster-state backup (for secrets, ConfigMaps, PVCs) and do not replace pgBackRest.

### Layer 6 — Flowable In-Flight Process Reconciliation

After a point-in-time PostgreSQL restore, Flowable BPMN processes that were active between the
restore point and the failure time will be in an inconsistent state relative to already-dispatched
external effects (letters sent, payments processed, notifications delivered).

**Strategy:**

1. After any PITR restore, Flowable's job executor is **suspended** before the application starts:

   ```yaml
   # application-dr-restore.yml profile (applied during DR startup)
   flowable:
     async-executor-activate: false
   ```

2. A caseworker or admin reviews the `ACT_RU_JOB` and `ACT_RU_TASK` tables to identify in-flight
   processes at the restore point.

3. For each in-flight case:
   - If the case was closed after the restore point but is now re-opened: recheck payment state
     against SKB CREMUL records and re-close if appropriate.
   - If a letter was sent after the restore point but the `NOTIFICATIONS` table shows it as pending:
     mark as sent (idempotent check against Digital Post delivery status API).
   - If a write-down was applied after the restore point: re-apply via `POST /api/v1/debts/{id}/write-down`.

4. Once reconciliation is complete, re-enable the async executor and resume normal operations.

5. The integration-gateway's CREMUL ingestion is idempotent: each CREMUL file has a unique
   `cremulRef`. Files already processed will be rejected as duplicates (no double-processing risk).

### Layer 7 — Backup Monitoring and Alerting

Backup operations must be monitored with the same rigour as application health.

```yaml
# Prometheus alerting rules for backups
groups:
  - name: opendebt-backup
    rules:
      - alert: PostgreSQLBackupStale
        expr: time() - pgbackrest_backup_timestamp_last_backup > 86400
        for: 1h
        labels:
          severity: critical
        annotations:
          summary: "PostgreSQL backup is more than 24 hours old"

      - alert: WALArchivingLagging
        expr: pgbackrest_wal_archive_diff_seconds > 900
        for: 15m
        labels:
          severity: warning
        annotations:
          summary: "WAL archiving is lagging more than 15 minutes"

      - alert: EncryptionKeyVaultUnreachable
        expr: up{job="keyvault-probe"} == 0
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Encryption key vault is unreachable — person-registry PII unrecoverable if lost"
```

### DR Classification and Declaration

| Failure class | Response | Declared? |
|---------------|----------|-----------|
| Pod crash / node eviction | Kubernetes self-heals | No |
| Service degraded (circuit open) | Resilience4j fallback + alert | No |
| Single DB connection failure | Connection pool recovery | No |
| DB instance failure (primary down, standby available) | Manual failover (Layer 3) | No — operational incident |
| DB instance failure (no standby, restore from backup) | Full DR procedure | **Yes** |
| Datacenter-level failure | Full DR procedure | **Yes** |
| Encryption key vault unavailable | Emergency key retrieval procedure | **Yes** |

### RTO / RPO verification

| Scenario | Expected restore time | Within RTO 4h? |
|----------|----------------------|----------------|
| Pod/node failure | 2–5 min (K8s) | ✅ |
| DB primary failure, standby exists | 15–30 min | ✅ |
| DB restore from pgBackRest (PITR) | 45–90 min | ✅ |
| Flowable reconciliation | 30–60 min | ✅ |
| Smoke tests + verification | 30 min | ✅ |
| **Total worst-case (no standby)** | **~2.5 hours** | ✅ |

RPO: WAL archiving every ~5 minutes → actual data loss window is well under 4 hours in all
scenarios where pgBackRest is operational.

## Consequences

### Positive

- 4h RTO / 4h RPO targets are comfortably met with a realistic estimated worst-case of ~2.5 hours.
- Continuous WAL archiving effectively delivers near-zero RPO in practice.
- The encryption key vault eliminates the worst failure mode (irrecoverable PII).
- Flowable reconciliation strategy prevents double-processing of financial mutations after restore.
- Backup monitoring alerts catch silent backup failures before they become DR events.

### Negative

- pgBackRest adds operational complexity (must be installed, configured, and monitored on the DB host).
- Streaming replication requires a second PostgreSQL instance (infrastructure cost).
- Flowable reconciliation after PITR requires manual caseworker review — not fully automated.
- pgBackRest cipher passphrase is an additional secret to manage (store in key vault alongside ENCRYPTION_KEY).

### Mitigations

- Use the UFST Horizontale Driftsplatform's managed PostgreSQL offering if available — it likely
  includes backup and replication out of the box, reducing the pgBackRest operational burden.
- Automate the Flowable reconciliation check as a Spring Boot `CommandLineRunner` under the
  `dr-restore` profile to reduce manual steps.
- Quarterly DR drills (see runbook) validate that the actual RTO stays within target.

## Runbook

See `docs/dr-runbook.md` for the step-by-step DR execution procedure.

## Alternatives considered

| Option | Reason not chosen |
|--------|------------------|
| **pg_dump only** | Logical backup; no WAL archiving; restore is slower; no PITR; does not meet RPO target reliably |
| **Platform-managed PostgreSQL snapshots only** | Acceptable if the platform provides PITR; retain pgBackRest as fallback for self-managed instances |
| **Active-active multi-region** | Significant complexity increase; not required for 4h/4h targets; revisit if SLA tightens to <1h |
| **Patroni cluster (HA)** | Good option for automated failover; deferred — manual standby promotion is sufficient for current targets |
