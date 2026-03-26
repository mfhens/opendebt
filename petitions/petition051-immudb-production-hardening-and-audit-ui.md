# Petition 051: immudb Ledger Integrity — Production Hardening and Audit UI

## Summary

Harden the immudb tamper-evidence layer introduced in spike TB-028 for production readiness,
and add a role-restricted audit UI in the caseworker portal that allows authorised auditors to
inspect immudb ledger entries side-by-side with PostgreSQL ledger entries and verify their
cryptographic integrity. The petition is structured in two phases: Phase 1 covers the backend
hardening required for production safety; Phase 2 covers the audit UI surface.

## Context and Motivation

ADR-0029 (now conditionally accepted) establishes immudb as a cryptographic tamper-evidence
layer for the OpenDebt financial ledger. The spike TB-028 proved the integration works:
`immudb4j 1.0.1` integrates with Spring Boot 3.5, the dual-write pattern is operational, and
28 demo ledger entries are correctly stored in immudb.

However, the spike implementation has several known limitations that are not acceptable for
production:

| Issue | Risk |
|-------|------|
| Single shared `ImmuClient` session (not thread-safe) | Concurrent async writes corrupt the immudb session under load |
| `@Async` + `@Retryable` proxy ordering bug | Retries fire at submission time, not inside the async thread — entries are silently lost after 5 attempts |
| No health indicator | immudb connection failures are invisible to monitoring and platform SLAs |
| No reconciliation job | Entries that exhaust retries are permanently absent from immudb; no mechanism to detect or repair the gap |
| `contraAccountCode` is null in `LedgerImmuRecord` | Incomplete tamper-evidence records; audit proof is partial |

Beyond the hardening, the cryptographic integrity layer has no user-visible surface. Auditors
(Rigsrevisionen, internal finance) currently have no way to verify ledger integrity without
direct database access or running CLI tools. The audit UI closes this gap: it fetches entries
from both PostgreSQL and immudb, runs cryptographic verification, and presents the result to
authorised users within the existing caseworker portal.

### Domain Terms

| Danish | English | Definition |
|--------|---------|------------|
| Revisionslog | Audit log | Cryptographically verifiable record of financial postings |
| Verificering | Verification | Cryptographic proof that an immudb entry has not been tampered with |
| Sagsbehandler | Caseworker | Internal user processing debt collection cases |
| Revisor | Auditor | Role with read-only access to the tamper-evidence layer for compliance purposes |
| Posteringslog | Transaction log | PostgreSQL ledger entries (operational source of truth) |
| Integritetscheck | Integrity check | Comparison of PostgreSQL and immudb records; verification of cryptographic proof |

## Functional Requirements

### Phase 1: Backend Production Hardening

#### FR-1: Thread-Safe immudb Connection Management

- **FR-1.1**: Replace the single shared `ImmuClient` session in `ImmudbConfig` with a
  connection pool that supports concurrent async writes safely.
- **FR-1.2**: The pool shall open a new session per-call or maintain a fixed pool of
  authenticated sessions (minimum pool size: 2; maximum: configurable via
  `opendebt.immudb.pool.max-size`, default 5).
- **FR-1.3**: Pool exhaustion shall result in a timed wait (configurable, default 500 ms)
  followed by a fallback that logs a warning and skips the immudb write for that entry
  (never blocking or rolling back the PostgreSQL transaction).

#### FR-2: Correct Async Retry Behaviour

- **FR-2.1**: Remove the `@Retryable` annotation from `ImmuLedgerClient.appendAsync`.
- **FR-2.2**: Inject `RetryTemplate` with exponential backoff (initial delay 500 ms,
  multiplier 2, max attempts 5) into `ImmuLedgerClient`.
- **FR-2.3**: The `appendAsync` method shall call `retryTemplate.execute(...)` internally
  so that retries occur within the async thread, not at the proxy submission level.
- **FR-2.4**: After exhausting all retry attempts for a single entry, the entry UUID and
  transaction ID shall be written to the `immudb_pending_entries` table (see FR-4).

#### FR-3: immudb Health Indicator

- **FR-3.1**: Implement `ImmudbHealthIndicator` implementing Spring Boot's `HealthIndicator`.
- **FR-3.2**: The indicator shall call `immuadmin status` equivalent (via immudb4j connection
  check) and report `UP` or `DOWN` with last-seen timestamp and database name.
- **FR-3.3**: The indicator shall be exposed via `GET /actuator/health/immudb` and included
  in the composite `GET /actuator/health` response.
- **FR-3.4**: When immudb is `DOWN`, the health status of payment-service shall be `DEGRADED`
  (not `DOWN`) — the service remains operational; only the tamper-evidence layer is unavailable.

#### FR-4: Reconciliation Table and Job

- **FR-4.1**: Add Flyway migration creating table `immudb_pending_entries` in the
  `opendebt_payment` database:
  - `id UUID PRIMARY KEY`
  - `ledger_entry_id UUID NOT NULL` (references `ledger_entries.id`)
  - `transaction_id UUID NOT NULL`
  - `payload JSONB NOT NULL` (the `LedgerImmuRecord` that failed to write)
  - `attempt_count INTEGER NOT NULL DEFAULT 0`
  - `last_attempt_at TIMESTAMPTZ`
  - `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`
  - `resolved_at TIMESTAMPTZ` (null = pending; set when successfully written to immudb)
- **FR-4.2**: Implement `ImmudbReconciliationJob` (`@Scheduled`, daily at 03:00 UTC) that
  fetches unresolved entries from `immudb_pending_entries` and retries writing them to immudb.
- **FR-4.3**: On successful reconciliation, set `resolved_at = now()` and log at INFO level.
- **FR-4.4**: After 10 failed reconciliation attempts (`attempt_count >= 10`), the entry
  shall be flagged for manual review via an application log at ERROR level with the
  `IMMUDB_INTEGRITY_GAP` marker.

#### FR-5: Complete LedgerImmuRecord

- **FR-5.1**: `LedgerImmuRecord` shall include `contraAccountCode`, populated from the
  paired entry (the CREDIT counterpart of a DEBIT, and vice versa).
- **FR-5.2**: The `appendAsync(LedgerEntryEntity... entries)` call shall receive both
  entries of a double-entry pair and cross-populate `contraAccountCode` before writing.

---

### Phase 2: Audit UI in Caseworker Portal

#### FR-6: Audit Endpoint in Payment-Service

- **FR-6.1**: Add `GET /api/v1/audit/ledger` to payment-service, restricted to
  `ROLE_AUDITOR` and `ROLE_SUPERVISOR`.
- **FR-6.2**: The endpoint shall accept query parameters: `debtId` (optional UUID),
  `fromDate` (optional ISO date), `toDate` (optional ISO date), `page` (default 0),
  `size` (default 25).
- **FR-6.3**: For each `LedgerEntryEntity` matching the filters, the endpoint shall:
  1. Fetch the corresponding entry from immudb using `verifiedGet(entryId)`.
  2. Compare the immudb payload against the PostgreSQL record.
  3. Return an `AuditEntryDto` containing: PostgreSQL fields, immudb presence flag,
     verification result (`VERIFIED` / `TAMPERED` / `MISSING`), and immudb transaction ID.
- **FR-6.4**: If immudb is unavailable, `verificationResult` shall be `UNAVAILABLE` for
  all entries; the endpoint shall not fail.
- **FR-6.5**: The endpoint shall not return any PII. All person references shall be `personId`
  (UUID) only, consistent with GDPR isolation (ADR data isolation principle).

#### FR-7: Audit Role in Keycloak

- **FR-7.1**: Add `ROLE_AUDITOR` to the Keycloak realm configuration (dev and demo realms).
- **FR-7.2**: The auditor role shall have read-only access to the audit endpoint and the
  caseworker portal audit page only. It shall not have access to case management actions,
  payment processing, or configuration.
- **FR-7.3**: Add a demo auditor user (`auditor-user` / `auditor-password`) for demo and
  dev environments.

#### FR-8: Audit Page in Caseworker Portal

- **FR-8.1**: Add a new page `/caseworker-portal/audit/ledger` in the caseworker portal,
  accessible only to users with `ROLE_AUDITOR` or `ROLE_SUPERVISOR`.
- **FR-8.2**: The page shall display a searchable, filterable table of ledger entries with
  the following columns:
  - Tx# (immudb transaction number)
  - Posting date
  - Type (DEBIT / CREDIT)
  - Account code
  - Amount (DKK)
  - Debt ID (UUID, not name — no PII)
  - immudb status: `✓ Verificeret` (green) / `⚠ Mangler` (amber) / `✗ Manipuleret` (red) / `– Utilgængelig` (grey)
- **FR-8.3**: Each row shall have an expandable detail panel showing the full
  `LedgerImmuRecord` JSON payload from immudb alongside the PostgreSQL record, with
  field-level diff highlighting for any discrepancies.
- **FR-8.4**: A "Kør integritetscheck" (Run integrity check) button at the top of the page
  shall trigger a fresh `verifiedGet` scan for all visible entries and refresh the status
  column. The button shall be disabled during an active scan.
- **FR-8.5**: Summary counters at the top of the page shall show: total entries, verified
  count, missing count, tampered count. A tampered count > 0 shall render in red with a
  prominent warning banner.
- **FR-8.6**: The page shall include a "Eksporter rapport" (Export report) button that
  downloads a CSV of the current view including verification status, for inclusion in
  audit documentation.
- **FR-8.7**: The page shall be linked from the caseworker portal navigation under
  "Administration" and shall be hidden for users without `ROLE_AUDITOR` or `ROLE_SUPERVISOR`.

#### FR-9: Audit Trail for Audit Actions

- **FR-9.1**: Every call to `GET /api/v1/audit/ledger` shall be recorded in the existing
  `audit_log` table with: user ID, timestamp, action `IMMUDB_AUDIT_QUERY`, and the
  query parameters used.
- **FR-9.2**: Any entry where `verificationResult = TAMPERED` shall trigger an immediate
  log entry at `ERROR` level with marker `IMMUDB_TAMPER_DETECTED` and be written to
  `audit_log` with action `IMMUDB_TAMPER_DETECTED`.

## Non-Functional Requirements

- **NFR-1**: `verifiedGet` shall complete within 500 ms per entry (p95). Batch verification
  of 25 entries (one page) shall complete within 5 seconds total.
- **NFR-2**: The connection pool (FR-1) shall not increase synchronous ledger posting latency
  (PostgreSQL `@Transactional` path) by more than 5 ms p95.
- **NFR-3**: The audit page shall be accessible per WCAG 2.1 AA (consistent with ADR-0013
  and petition 013).
- **NFR-4**: No PII shall pass through the audit endpoint or appear in the audit UI. Debt
  ID (UUID) is the only identifying reference (GDPR data isolation principle).
- **NFR-5**: The reconciliation job (FR-4.2) shall not run during business hours (07:00–18:00
  CET) to avoid contention with live ledger writes.

## Out of Scope

- Full immudb High Availability (replication, follower nodes) — covered by TB-028-f.
- UFST HDP Kubernetes deployment of immudb — covered by TB-028-a.
- Load testing of the dual-write path under production traffic — covered by TB-028-d.
- Rigsrevisionen integration or external API access for auditors — future petition.
- Modifications to the citizen or creditor portals.

## Dependencies

| Dependency | Type | Notes |
|------------|------|-------|
| ADR-0029 | Architecture decision | Conditionally accepted; full acceptance requires TB-028-a (HDP validation) |
| Spike TB-028 | Technical prerequisite | Provides `RealImmudbAdapter`, `ImmuLedgerClient`, `LedgerImmuRecord`, `ImmudbConfig` |
| Petition 040 | Functional prerequisite | Ledger query API used by the audit endpoint |
| Petition 048 | Functional prerequisite | Role-based access control patterns used for `ROLE_AUDITOR` |
| TB-028-a | Platform prerequisite | HDP validation; if this fails, Phase 1 and Phase 2 both block |

## Related ADRs

- ADR-0029: immudb for Financial Ledger Integrity
- ADR-0018: Double-Entry Bookkeeping (storno pattern)
- ADR-0022: Shared Audit Infrastructure
- ADR-0013: Enterprise PostgreSQL with Audit and History
