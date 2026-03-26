# Spike Findings: TB-028 — immudb Financial Ledger Integrity (ADR-0029)

**Branch:** `spike/TB-028-immudb-financial-ledger-integrity`  
**ADR:** [docs/adr/0029-immudb-for-financial-ledger-integrity.md](../adr/0029-immudb-for-financial-ledger-integrity.md)  
**Status:** Implementable sub-tasks (TB-028-b, TB-028-c) complete — platform sub-tasks
(TB-028-a, TB-028-d, TB-028-e, TB-028-f) pending UFST HDP validation.

---

## What was implemented in this spike

| Sub-task | Status | Deliverable |
|---|---|---|
| TB-028-b | ✅ Code complete | `immudb4j` dependency; `ImmudbConfig` bean; `ImmuLedgerClient` |
| TB-028-c | ✅ Code complete | `LedgerImmuRecord` DTO; dual-write in `BookkeepingServiceImpl` |
| TB-028-a | ⏳ Needs platform | Checklist below |
| TB-028-d | ⏳ Needs load test | Checklist below |
| TB-028-e | ⏳ Needs proof demo | Checklist below |
| TB-028-f | ⏳ Needs HA review | Checklist below |

---

## TB-028-a: Confirm immudb on UFST HDP Kubernetes

**What must be validated on UFST Horizontale Driftsplatform:**

- [ ] immudb `Deployment` can be scheduled on HDP — confirm available node pool, CPU/memory
      ceiling (immudb recommends ≥ 1 vCPU / 512 MiB RAM for non-HA single node)
- [ ] Persistent volume: HDP storage class supports `ReadWriteOnce` block storage with adequate
      IOPS (immudb is write-intensive with Merkle tree updates on every commit)
- [ ] Port 3322 (gRPC) accessible from `payment-service` pod within the same namespace
      via Kubernetes `Service` — not exposed externally
- [ ] Port 9497 (Prometheus metrics) scrapeable by Prometheus / HDP monitoring stack
- [ ] Network policy allows `payment-service` → `immudb` egress on TCP/3322
- [ ] Image pull: `codenotary/immudb:latest` (or pinned version) pullable from HDP container
      registry mirror — confirm no image whitelist block
- [ ] `immudb` process runs as non-root; confirm pod security policy / PodSecurityAdmission
      allows immudb's default UID

**Evidence criteria:**

- `kubectl get pods -n opendebt` shows immudb pod `Running` with all containers ready
- `kubectl exec -n opendebt deploy/immudb -- immuadmin status` returns `OK`
- `kubectl exec -n opendebt deploy/payment-service -- nc -zv immudb 3322` succeeds

**ACCEPT:** All items checked, evidence captured in HDP acceptance test report.  
**REJECT blocker:** PVC storage class unavailable, network policy irresolvable, or image
  pull restricted — escalate to HDP platform team before committing to ADR-0029.

---

## TB-028-d: Measure dual-write latency under realistic load

**What must be validated:**

- [ ] Run k6 or Gatling load test against `payment-service` with concurrent ledger postings
      (target: 100 concurrent users, 500 req/s, simulating peak reconciliation load)
- [ ] Baseline: measure p50/p95/p99 latency of `recordPaymentReceived` with immudb **disabled**
- [ ] Active: measure same metrics with `opendebt.immudb.enabled=true` against a running immudb
- [ ] Assert: async path must not increase **synchronous** p95 response time by > 5 ms
      (the immudb write is async, so the caller should not block — validate this)
- [ ] Measure immudb write throughput: confirm immudb can sustain the posting rate without
      queue buildup in the async executor (check `ThreadPoolTaskExecutor` queue depth metrics)
- [ ] Measure immudb tail latency: p99 of `ImmuLedgerClient.appendAsync` execution time
      (expected: < 50 ms in local dev; acceptable on HDP: < 200 ms p99)

**Known implementation concern — AIDEV-TODO in code:**  
The current `@Async` + `@Retryable` combination does not correctly retry *inside* the async
thread. Spring-Retry's proxy intercepts at submission time; the async thread returns `void`
immediately, so Spring-Retry sees a successful return and never retries. To fix:
- Inject `RetryTemplate` into `ImmuLedgerClient` and call it explicitly inside `appendAsync`
- Or: use a separate `@Retryable`-annotated private `doAppend` method called from within the
  async body (requires a self-injected proxy bean)
This must be addressed in TB-029 implementation before production deployment.

**Evidence criteria:**

- Load test report showing p95 latency delta < 5 ms (synchronous path)
- Grafana dashboard screenshot showing async queue depth stays bounded
- immudb metrics (`immudb_write_duration_seconds`) histogram exported and visible

**ACCEPT:** p95 delta < 5 ms; no queue saturation at target load.  
**REJECT blocker:** synchronous path degrades > 5 ms p95 (likely indicates immudb write
  is blocking the calling thread — would mean `@Async` proxy is not working correctly);
  or async queue saturates and OOMs the payment-service JVM.

---

## TB-028-e: Demonstrate tamper-evidence proof verification

**What must be validated:**

- [ ] Post a ledger entry via `payment-service` with immudb enabled
- [ ] Retrieve the entry from immudb by `transactionId` using `immudb4j.verifiedGet(key)`
      or the `immuadmin` CLI — confirm the proof payload is returned
- [ ] Verify the proof **independently** using the immudb CLI from a machine with no
      application credentials (simulating a Rigsrevisionen auditor):
      ```bash
      immuclient login immudb
      immuclient get <entry-uuid-key>
      immuclient verifiedget <entry-uuid-key>
      ```
- [ ] Confirm that manually altering the immudb data (e.g., via direct database file edit
      or using `immuclient set` with a modified value) is detected by `verifiedGet`
- [ ] Document the verification procedure in a format suitable for Rigsrevisionen:
      who runs it, what credentials are needed, what constitutes a successful proof,
      and what an audit failure looks like

**Evidence criteria:**

- Screenshot of `immuclient verifiedget` output showing `verified: true`
- Demonstration of tamper detection: `verifiedGet` after manual data modification returns
  error or `verified: false`
- Draft auditor procedure document (1–2 pages) with step-by-step instructions

**ACCEPT:** Independent proof verification demonstrated; auditor procedure drafted.  
**REJECT blocker:** `verifiedGet` does not produce a cryptographic proof (SDK limitation
  in this version), or the proof cannot be verified without application access (defeats
  the purpose of the tamper-evidence layer).

---

## TB-028-f: Assess immudb backup and HA requirements (ADR-0028 alignment)

**What must be validated:**

- [ ] Evaluate immudb **follower replication** (immudb replication uses a primary-follower model
      on port 9485 by default) — determine if HDP can schedule a follower node
- [ ] Determine how immudb data (`/var/lib/immudb`) is included in the pgBackRest / Velero
      DR strategy from ADR-0028:
      - Option A: Velero PVC snapshot (simplest; requires VolumeSnapshotClass on HDP)
      - Option B: `immuadmin database export` scheduled job to object storage
      - Option C: immudb follower replication as DR replica (most complex; highest RPO/RTO)
- [ ] Confirm RTO/RPO targets for the immudb layer:
      - RPO: how much tamper-evidence data can be lost in a DR event? (acceptable: < 1 hour)
      - RTO: how long can payment-service operate without immudb before integrity gap becomes
        unacceptable? (acceptable: indefinitely — immudb is non-blocking; entries can be
        re-appended if immudb is restored and PostgreSQL has the source data)
- [ ] Validate that immudb backup restores correctly: restore from snapshot/export and
      confirm `verifiedGet` on previously stored entries still returns `verified: true`
- [ ] Review immudb's GDPR posture: no PII is stored (confirmed by architecture constraint);
      document this for DPA compliance record

**Evidence criteria:**

- Written assessment of replication and backup options with HDP feasibility verdict
- Backup restore test log showing `verifiedGet` passes after restore
- RPO/RTO statement for immudb aligned with ADR-0028 targets

**ACCEPT:** At least one viable backup strategy confirmed on HDP; RPO/RTO within targets.  
**REJECT blocker:** HDP has no viable PVC snapshot or export mechanism; immudb replication
  requires infrastructure not supportable by the platform team.

---

## Known implementation issues and AIDEV-TODOs

These were identified during spike implementation and must be resolved in TB-029
before production deployment:

| ID | File | Issue |
|---|---|---|
| 1 | `ImmuLedgerClient` | `@Async` + `@Retryable` ordering — retry fires at submission, not inside async thread. Use `RetryTemplate` internally instead. |
| 2 | `ImmudbConfig` | Single shared `ImmuClient` session — not thread-safe under concurrent async writes. Replace with per-call sessions or a connection pool (e.g., semaphore-guarded pool). |
| 3 | `LedgerImmuRecord` | `contraAccountCode` is `null` — populate from the paired entry by restructuring the `appendAsync` call to receive both entries and cross-populate. |
| 4 | `ImmudbConfig` | Startup failure is fatal (`throw` on session open failure). Production should degrade gracefully with a circuit breaker and health indicator. |
| 5 | `ImmuLedgerClient` | No dead-letter / persistent retry queue for entries that exhaust all retry attempts. If immudb is down for > 5 retries, the entry is lost from immudb (PostgreSQL is unaffected). Production needs a reconciliation job to re-append missing entries. |
| 6 | `pom.xml` | immudb4j `0.9.4` — verify exact version on Maven Central and confirm no gRPC / Netty classpath conflicts with `spring-boot-starter-webflux`. |

---

## Spike outcome determination

After completing TB-028-a through TB-028-f, evaluate:

| Criterion | ACCEPT threshold | REJECT threshold |
|---|---|---|
| Platform support (TB-028-a) | immudb runs on HDP; PVC and network confirmed | Platform blocker irresolvable |
| SDK integration (TB-028-b) | immudb4j compiles; no classpath conflicts | SDK not on Maven Central; breaking conflicts |
| Dual-write latency (TB-028-d) | p95 sync delta < 5 ms | p95 sync delta ≥ 5 ms at target load |
| Proof verification (TB-028-e) | `verifiedGet` produces valid proof; auditor procedure drafted | Proof API non-functional or requires app access |
| Backup / HA (TB-028-f) | Viable backup strategy confirmed on HDP | No viable backup; immudb state irrecoverable |

**ACCEPT → ADR-0029 moves to Accepted; create TB-029 for full implementation.**  
**REJECT → ADR-0029 moves to Rejected with documented reason; revert spike branch or
  archive as reference for future re-evaluation.**
