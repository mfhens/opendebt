# ADR-0029: immudb for Financial Ledger Integrity

## Status

Accepted — conditionally pending UFST HDP platform validation (TB-028-a)

**Spike outcome (TB-028, 2026-03-26):** All implementable sub-tasks completed on
branch `spike/TB-028-immudb-financial-ledger-integrity`. Key findings:

- `io.codenotary:immudb4j:1.0.1` is on Maven Central and integrates with Spring Boot 3.5 / Java 21.
- Netty version conflict (Spring Boot 3.5 → Netty 4.1.131 vs gRPC 1.44.1 → Netty 4.1.74) resolved
  by excluding `grpc-netty` and substituting `grpc-netty-shaded:1.44.1`.
- Dual-write pattern (PostgreSQL primary + immudb tamper-evidence) is operational in the demo stack.
- `DemoDataSeeder` seeds all 28 ledger entries to immudb on startup, confirmed via REST API and
  the `immudb-view.py` audit viewer script.
- Remaining platform sub-tasks (TB-028-a: HDP validation, TB-028-d: latency measurement,
  TB-028-e: proof verification demo, TB-028-f: HA/backup assessment) remain open and are
  prerequisites for moving to **Fully Accepted**.

**Condition for full acceptance:** TB-028-a must confirm that immudb can be scheduled on the
UFST Horizontale Driftsplatform with a persistent volume and accessible gRPC port (3322).
If this is not feasible, this ADR must be revisited.

See `docs/spike/TB-028-findings.md` for detailed findings and acceptance criteria per sub-task.

## Date

2026-03-25

## Context

OpenDebt's financial ledger (`ledger_entries`, `debt_events` in payment-service) records
every financial movement against a debt: registrations, payments, interest accruals,
offsetting, write-offs, refunds, and storno corrections. These records:

- Are the source of truth for outstanding balances and afstemning (reconciliation)
- May be referenced in legal proceedings (fogedretten, klagesager)
- Are subject to Rigsrevisionen audit
- Represent binding financial obligations under Danish public law

The ledger is already designed for immutability: the **storno pattern** (ADR-0018)
prohibits modification of posted entries; corrections are always reverse + repost.
The `debt_events` table is an append-only event timeline.

Despite this application-level immutability, a PostgreSQL `SUPERUSER` can issue
`UPDATE` or `DELETE` against `ledger_entries` without leaving a trace in the application
audit trail. This is a trust gap that cannot be closed within PostgreSQL alone.

The general `audit_log` tables (ADR-0022) are shipped to the UFST Common Logging System
(CLS) via Filebeat, which provides an external anchor for operational audit events.
However, CLS is an ELK stack optimised for log search — it provides no
**cryptographic proof of record integrity** that can be presented as evidence.

**immudb** (codenotary/immudb) is an open-source, tamper-evident database built on a
Merkle tree with a cryptographically signed transaction log. Every write produces a
verifiable proof. A verifier (auditor, court, Rigsrevisionen) can independently confirm
that no record was ever modified or deleted, without trusting the database operator.

A spike (TB-028) is required before this ADR is accepted, to validate platform support
and integration cost.

## Decision

### Where immudb IS used

**Financial ledger entries only** — specifically the following tables in payment-service:

| Table | Rationale |
|---|---|
| `ledger_entries` | Every financial posting; legal record of monetary movements |
| `debt_events` | Immutable event timeline; source of truth for debt state |

The integration pattern is **dual-write**:

```
BookkeepingService.record*()
  ├── save LedgerEntryEntity  →  PostgreSQL   (primary; queryable, operational)
  └── append LedgerRecord     →  immudb       (tamper-evident; verifiable proof)
```

PostgreSQL remains the **operational system of record**. immudb is the
**cryptographic integrity layer** — it is never queried for normal operations.

Each immudb record mirrors the PostgreSQL row exactly, keyed by `transactionId`
(the shared UUID linking a DEBIT+CREDIT pair). A verifier can retrieve any
`transactionId` from immudb and prove the amount, account codes, and dates were
never altered after posting.

The `BookkeepingService` is the sole write path; dual-write is implemented there
using an `ImmuLedgerClient` (Spring `@Component`, wrapping `immudb4j`) that appends
asynchronously after the PostgreSQL transaction commits. If the immudb write fails,
it is retried with exponential backoff and logged; the PostgreSQL record is never
rolled back. This preserves eventual integrity without degrading ledger availability.

### Where immudb is NOT used

| Area | Reason |
|---|---|
| `audit_log` tables (all services) | CLS/Filebeat provides sufficient external anchor; operational audit, not financial proof |
| `payments` table | Payment status is operational data, not a final ledger posting |
| `interest_journal_entries` (debt-service) | Intermediate calculation state; final outcome lands in `ledger_entries` |
| `fees` table (debt-service) | Supporting reference data; the fee posting is recorded in `ledger_entries` |
| Person registry | GDPR PII — must not be replicated to additional stores |
| Keycloak / config data | Not financial records; no legal standing requirement |
| Case events | Operational workflow history; CLS covers this |
| All other entities | Application-level immutability (storno, history tables, sys_period) is sufficient |

The guiding principle: **immudb is reserved for records where cryptographic non-repudiation
has legal or regulatory significance.** It is not a general-purpose audit store.

## Consequences

### Positive

1. **Cryptographic non-repudiation** — Any financial record can be independently verified
   by an auditor without trusting the database operator or the application team.
2. **Closes the DBA trust gap** — PostgreSQL `SUPERUSER` access cannot silently alter
   posted ledger entries.
3. **Low architectural disruption** — Dual-write is isolated to `BookkeepingService`;
   no changes to query paths, APIs, or other services.
4. **Aligns with existing design** — The storno-based, append-only ledger maps
   naturally to immudb's write model.
5. **Legal defensibility** — Supports use of ledger records in fogedretten proceedings
   and Rigsrevisionen audits with cryptographic proof.

### Negative

1. **Additional operational dependency** — immudb must be deployed, monitored, backed
   up, and included in the DR plan (ADR-0028).
2. **Dual-write consistency** — Eventual consistency between PostgreSQL and immudb
   means a window (seconds) where a record exists in PostgreSQL but not yet in immudb.
   Acceptable for the proof layer; not acceptable if immudb were the primary store.
3. **SDK maturity** — `immudb4j` is maintained by codenotary but is less widely used
   than the PostgreSQL ecosystem. Requires evaluation during spike.
4. **Backup scope** — immudb's own data must be included in backup strategy; its
   tamper-evidence is only meaningful if the immudb state itself is preserved.

### Neutral

1. immudb runs as an independent Kubernetes `Deployment` alongside the existing stack.
2. No changes to existing PostgreSQL schema, Flyway migrations, or API contracts.
3. Verification tooling (CLI, SDK) can be run independently by auditors without
   application access.

## Implementation Sketch

```java
// opendebt-payment-service — BookkeepingService

@Transactional
public LedgerTransaction recordPaymentReceived(UUID debtId, BigDecimal amount, ...) {
    // 1. Post to PostgreSQL (primary)
    LedgerEntryEntity debit  = ledgerEntryRepository.save(buildDebit(...));
    LedgerEntryEntity credit = ledgerEntryRepository.save(buildCredit(...));

    // 2. Append to immudb (integrity layer) — after commit, async with retry
    immuLedgerClient.appendAsync(debit, credit);

    return new LedgerTransaction(debit, credit);
}
```

```java
// opendebt-payment-service — ImmuLedgerClient

@Component
public class ImmuLedgerClient {

    private final ImmuClient immuClient;  // immudb4j

    @Async
    @Retryable(maxAttempts = 5, backoff = @Backoff(delay = 500, multiplier = 2))
    public void appendAsync(LedgerEntryEntity... entries) {
        for (LedgerEntryEntity entry : entries) {
            immuClient.set(
                entry.getId().toString(),
                LedgerImmuRecord.from(entry).toBytes()
            );
        }
    }
}
```

```yaml
# docker-compose.yml addition
immudb:
  image: codenotary/immudb:latest
  ports:
    - "3322:3322"
    - "9497:9497"   # metrics
  volumes:
    - immudb-data:/var/lib/immudb
```

## Spike Required (TB-028)

Before this ADR is accepted, the following must be validated:

1. **Platform support** — Confirm immudb runs on UFST HDP Kubernetes (resource limits,
   storage class, network policies).
2. **SDK integration** — Validate `immudb4j` with Spring Boot 3.3 / Java 21.
3. **Dual-write latency** — Measure async append overhead under realistic ledger load.
4. **Proof verification** — Demonstrate that tamper-evidence proof API can be called
   by an external auditor without application credentials.
5. **Backup and HA** — Assess immudb replication and backup options within existing
   DR strategy (ADR-0028).

Spike outcome determines whether this ADR moves to **Accepted** or **Rejected**.

## Related ADRs

- ADR-0013: Enterprise PostgreSQL with Audit and History
- ADR-0018: Double-Entry Bookkeeping
- ADR-0022: Shared Audit Infrastructure
- ADR-0028: Backup and Disaster Recovery

## References

- [immudb — codenotary/immudb](https://github.com/codenotary/immudb)
- [immudb4j Java SDK](https://github.com/codenotary/immudb4j)
- [immudb documentation](https://docs.immudb.io)
- Rigsrevisorernes retningslinjer for IT-revision
- ADR-0018: Double-Entry Bookkeeping (storno pattern)
