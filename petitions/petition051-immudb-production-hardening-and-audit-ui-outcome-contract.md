# Outcome Contract: Petition 051 — immudb Ledger Integrity Production Hardening and Audit UI

## Acceptance Criteria

### Phase 1: Backend Production Hardening

#### AC-1: Thread-Safe Connection Management

- [ ] AC-1.1: Under 10 concurrent async ledger writes, no `IllegalStateException` or
  `ImmuServiceRuntimeException` is thrown due to session collision.
- [ ] AC-1.2: `ImmudbConfig` does not expose a single shared `ImmuClient` bean; a pool or
  per-call session strategy is in place.
- [ ] AC-1.3: `opendebt.immudb.pool.max-size` is configurable in `application.yml`; default is 5.
- [ ] AC-1.4: When the pool is exhausted, the PostgreSQL ledger write succeeds; the immudb
  write is skipped and a WARN log entry with text `immudb pool exhausted, skipping tamper-evidence write`
  is recorded.

#### AC-2: Correct Async Retry Behaviour

- [ ] AC-2.1: The `@Retryable` annotation is absent from `ImmuLedgerClient.appendAsync`.
- [ ] AC-2.2: When immudb is temporarily unavailable during an async write, `RetryTemplate`
  retries up to 5 times with exponential backoff before giving up.
- [ ] AC-2.3: After 5 failed attempts, the failed entry is inserted into `immudb_pending_entries`
  in PostgreSQL, not silently discarded.
- [ ] AC-2.4: Unit test covers: `RetryTemplate` is invoked inside the async thread, not at
  proxy submission time.

#### AC-3: immudb Health Indicator

- [ ] AC-3.1: `GET /actuator/health/immudb` returns HTTP 200 with `{"status": "UP"}` when
  immudb is reachable.
- [ ] AC-3.2: `GET /actuator/health/immudb` returns HTTP 200 with `{"status": "DOWN"}` when
  immudb is unreachable.
- [ ] AC-3.3: `GET /actuator/health` returns `{"status": "DEGRADED"}` (not `DOWN`) when
  immudb is `DOWN` and PostgreSQL is `UP`.
- [ ] AC-3.4: The composite health includes an `immudb` component key.

#### AC-4: Reconciliation Table and Job

- [ ] AC-4.1: Flyway migration creates `immudb_pending_entries` with all specified columns
  and runs without error on a clean database.
- [ ] AC-4.2: `ImmudbReconciliationJob` is annotated `@Scheduled(cron = "0 0 3 * * *")`.
- [ ] AC-4.3: Entries successfully reconciled have `resolved_at` set to a non-null timestamp.
- [ ] AC-4.4: Entries with `attempt_count >= 10` produce an ERROR log with marker
  `IMMUDB_INTEGRITY_GAP` and are not retried further.
- [ ] AC-4.5: The job does not execute when system clock is between 07:00 and 18:00 CET
  (configurable no-op window).

#### AC-5: Complete LedgerImmuRecord

- [ ] AC-5.1: `LedgerImmuRecord.contraAccountCode` is populated for all entries written
  after this petition is implemented.
- [ ] AC-5.2: When immudb is queried via the audit endpoint, the `contraAccountCode` field
  is present and matches the account code of the paired entry in PostgreSQL.
- [ ] AC-5.3: Existing entries written during the spike (without `contraAccountCode`) are
  handled gracefully by the audit endpoint (field shown as `null`, not causing an error).

---

### Phase 2: Audit UI

#### AC-6: Audit Endpoint

- [ ] AC-6.1: `GET /api/v1/audit/ledger` returns HTTP 403 for users with `ROLE_CASEWORKER`
  only.
- [ ] AC-6.2: `GET /api/v1/audit/ledger` returns HTTP 200 with a paginated list of
  `AuditEntryDto` for users with `ROLE_AUDITOR` or `ROLE_SUPERVISOR`.
- [ ] AC-6.3: Each `AuditEntryDto` contains `verificationResult` with one of:
  `VERIFIED`, `TAMPERED`, `MISSING`, `UNAVAILABLE`.
- [ ] AC-6.4: When immudb is stopped, all entries have `verificationResult = UNAVAILABLE`
  and the endpoint still returns HTTP 200 (not 500).
- [ ] AC-6.5: No CPR number, name, or address appears in the response; only `personId`
  (UUID) and `debtId` (UUID).
- [ ] AC-6.6: `debtId` filter works: `GET /api/v1/audit/ledger?debtId=<uuid>` returns only
  entries for that debt.
- [ ] AC-6.7: `fromDate` / `toDate` filters apply to `postingDate` on `LedgerEntryEntity`.
- [ ] AC-6.8: Pagination parameters `page` and `size` are respected; default page size is 25.

#### AC-7: Keycloak Audit Role

- [ ] AC-7.1: `ROLE_AUDITOR` exists in the dev and demo Keycloak realm configurations.
- [ ] AC-7.2: `auditor-user` / `auditor-password` credentials are available in dev and demo
  environments.
- [ ] AC-7.3: `auditor-user` can authenticate and receive a token that contains
  `ROLE_AUDITOR` in the roles claim.
- [ ] AC-7.4: `auditor-user` cannot create, update, or delete cases, payments, or ledger
  entries (read-only access enforced at endpoint level).

#### AC-8: Caseworker Portal Audit Page

- [ ] AC-8.1: Navigating to `/audit/ledger` as `auditor-user` shows the ledger audit table.
- [ ] AC-8.2: Navigating to `/audit/ledger` as `caseworker-user` (no `ROLE_AUDITOR`) shows
  a "Ingen adgang" (Access denied) page or redirects to the dashboard.
- [ ] AC-8.3: The table shows at minimum: Tx#, posting date, type, account code, amount,
  debt ID, and immudb status indicator.
- [ ] AC-8.4: Verified entries show a green `✓ Verificeret` badge; missing entries show
  amber `⚠ Mangler`; tampered entries show red `✗ Manipuleret`.
- [ ] AC-8.5: Clicking "Kør integritetscheck" triggers a fresh fetch from the audit endpoint
  and updates all status badges; the button is disabled while the request is in flight.
- [ ] AC-8.6: If any entry has `TAMPERED` status, a red warning banner is shown at the top
  of the page.
- [ ] AC-8.7: Summary counters (total / verified / missing / tampered) are accurate and
  update after each integrity check.
- [ ] AC-8.8: "Eksporter rapport" downloads a CSV containing all columns including
  verification status.
- [ ] AC-8.9: The "Revision" nav link is hidden for users without `ROLE_AUDITOR` or
  `ROLE_SUPERVISOR`.

#### AC-9: Audit Trail

- [ ] AC-9.1: Every call to `GET /api/v1/audit/ledger` inserts a row into `audit_log` with
  `action = 'IMMUDB_AUDIT_QUERY'`, `actor` = authenticated user ID, and `details` containing
  the query parameters.
- [ ] AC-9.2: Any response containing `verificationResult = TAMPERED` triggers an ERROR log
  with marker `IMMUDB_TAMPER_DETECTED` and a corresponding `audit_log` row with
  `action = 'IMMUDB_TAMPER_DETECTED'`.

---

### Non-Functional Acceptance Criteria

- [ ] AC-10: `verifiedGet` for a single entry completes in under 500 ms (p95) measured in
  integration test with immudb running locally.
- [ ] AC-11: Batch of 25 `verifiedGet` calls completes under 5 seconds total.
- [ ] AC-12: No PII appears in any log entry, API response, or audit export.
- [ ] AC-13: Payment-service JaCoCo line coverage remains ≥ 70 % and branch coverage ≥ 50 %
  after implementation of Phase 1.

---

## Definition of Done

1. All acceptance criteria above are passing in the CI pipeline.
2. Flyway migration(s) have been reviewed for correctness and do not break an existing
   populated database.
3. Keycloak realm JSON files for dev and demo have been updated and the demo startup script
   picks them up.
4. `architecture/overview.md` updated with the new audit endpoint and `ROLE_AUDITOR`.
5. `agents.md` updated to reflect the `immudb_pending_entries` table and
   `ImmudbReconciliationJob`.
6. ADR-0029 moved to Fully Accepted once TB-028-a (HDP platform validation) is also complete.
7. Snyk code scan shows no new high or critical issues in changed code.
8. PR reviewed and approved before merge to `main`.

## Risks

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| TB-028-a fails (HDP cannot run immudb) | Medium | High | Phase 1 and Phase 2 are blocked; ADR-0029 must be revised. Do not invest in Phase 2 UI until TB-028-a is confirmed. |
| `verifiedGet` latency exceeds 500 ms under load | Low | Medium | Introduce async pre-fetch on page load; cache verification results for 5 minutes (invalidated on integrity check button click). |
| immudb schema drift (KV key format changes) | Low | Medium | Key format documented in `LedgerImmuRecord`; migration script required if format changes. |
| Auditor role too broad / too narrow | Medium | Low | Define role in Keycloak with explicit permission set; iterate in UAT with a caseworker representative. |
