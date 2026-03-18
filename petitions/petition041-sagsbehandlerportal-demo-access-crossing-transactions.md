# Petition 041: Sagsbehandlerportal demo access with crossing-transactions showcase

## Summary

OpenDebt shall provide a development/demo mode for the `opendebt-caseworker-portal` module (petition 040) that bypasses Keycloak authentication, seeds realistic crossing-transaction data (petition 039), and allows developers and reviewers to explore the posteringslog (accounting transaction log) without any external dependencies.

## Context and motivation

The creditor-portal already has a working demo mode (DemoLoginController, `spring.profiles.active=dev`, seed data in V1__baseline.sql) that allows local exploration of the portal without Keycloak, external databases, or third-party services. The caseworker-portal (petition 040) needs the same capability, with a particular focus on demonstrating crossing-transaction handling (petition 039).

Crossing transactions — where a payment's value date (vaerdidag) precedes already-posted interest or other events — produce stornos, recalculated interest, dækningsrækkefølge re-application, and dækningsophævelser. These are complex accounting artifacts that must be visible and understandable in the posteringslog. A curated demo dataset that exercises all these code paths is essential for:

1. **Developer onboarding** — new team members can see crossing-transaction behavior without manually constructing test data.
2. **Stakeholder review** — product owners and caseworker subject-matter experts can validate the UI before production deployment.
3. **Regression testing** — a known demo dataset provides a stable baseline for visual/manual testing.
4. **Demo presentations** — the system can be shown at internal reviews and external audits.

## Dependencies

| Petition | Dependency |
|----------|-----------|
| 039 | Crossing-transaction detection, timeline replay, dækningsrækkefølge, dækningsophævelse, allokeringsunderretning |
| 040 | Caseworker-portal module, ledger query API, posteringslog view |

## Functional requirements

### FR-1: Demo security configuration

When `spring.profiles.active` includes `dev`, the caseworker-portal shall:
- Disable OAuth2/OIDC resource server and client configuration.
- Permit all HTTP requests without authentication.
- Expose a `/demo-login` page that allows the user to select a caseworker identity from a list of demo caseworkers.
- Store the selected identity in the HTTP session.
- Expose `/demo-logout` to clear the session and redirect to `/demo-login`.

This follows the same pattern as `creditor-portal`'s `DemoLoginController` and `DemoSecurityConfig`.

### FR-2: Demo caseworker identities

The demo mode shall provide at least three selectable caseworker identities:

| Name | Role | Description |
|------|------|-------------|
| Anna Jensen | CASEWORKER | Standard sagsbehandler |
| Erik Sørensen | SENIOR_CASEWORKER | Senior sagsbehandler with extended permissions |
| Mette Larsen | TEAM_LEAD | Teamleder with supervisory access |

These are hard-coded in the demo login controller (not stored in a database).

### FR-3: Demo data — case with crossing transactions

The demo data seeder shall create a realistic case that demonstrates crossing-transaction behavior. The seed data must be inserted into the correct service databases:

**Case (case-service database):**
- Case number: `SAG-2025-00042`
- Status: ACTIVE
- Assigned caseworker: Anna Jensen
- Debtor: reference to a demo person in person-registry

**Debts (debt-service database):**
At least two debts for the case:
1. **Debt A — Skattegæld (tax debt)**: principal 45,000.00 DKK, period 2024-01-01 to 2024-12-31, created 2025-02-01
2. **Debt B — Bødegæld (fine debt)**: principal 12,500.00 DKK, period 2024-06-01 to 2024-06-01, created 2025-03-15

**Ledger entries and events (payment-service database):**
For Debt A, the timeline must include:

| # | Event type | Effective date | Posting date | Amount | Description |
|---|-----------|---------------|-------------|--------|-------------|
| 1 | REGISTRATION | 2025-02-01 | 2025-02-01 | 45,000.00 | Initial registration of tax debt |
| 2 | INTEREST_ACCRUAL | 2025-03-01 | 2025-03-01 | 375.00 | Monthly interest (1% of 45,000 — 28 days) |
| 3 | INTEREST_ACCRUAL | 2025-04-01 | 2025-04-01 | 453.75 | Monthly interest (1% of 45,375) |
| 4 | PAYMENT | 2025-02-15 | 2025-04-10 | -10,000.00 | Payment with vaerdidag 15 Feb, registered 10 Apr → **CROSSING** |
| 5 | STORNO | 2025-03-01 | 2025-04-10 | -375.00 | Reversal of entry #2 (interest must be recalculated from crossing point) |
| 6 | STORNO | 2025-04-01 | 2025-04-10 | -453.75 | Reversal of entry #3 |
| 7 | INTEREST_ACCRUAL | 2025-03-01 | 2025-04-10 | 291.67 | Recalculated interest on 35,000 (after 10k payment on Feb 15) — 28 days |
| 8 | INTEREST_ACCRUAL | 2025-04-01 | 2025-04-10 | 352.92 | Recalculated interest on 35,291.67 — 31 days |
| 9 | COVERAGE_REVERSAL | 2025-02-15 | 2025-04-10 | 0.00 | Dækningsophævelse marker: payment coverage changed from pre-crossing to post-crossing allocation |
| 10 | PAYMENT | 2025-05-01 | 2025-05-01 | -5,000.00 | Normal payment (no crossing) |
| 11 | INTEREST_ACCRUAL | 2025-05-01 | 2025-05-01 | 306.44 | Interest accrual after normal payment |

For Debt B, a simpler timeline with no crossing:
| # | Event type | Effective date | Posting date | Amount |
|---|-----------|---------------|-------------|--------|
| 1 | REGISTRATION | 2025-03-15 | 2025-03-15 | 12,500.00 |
| 2 | PAYMENT | 2025-04-15 | 2025-04-15 | -5,000.00 |
| 3 | INTEREST_ACCRUAL | 2025-05-01 | 2025-05-01 | 75.00 |

This contrast lets reviewers compare a crossing-affected debt (A) with a normal debt (B) side by side.

### FR-4: Visual indicators for crossing-transaction artifacts

The posteringslog view (from petition 040) shall, in demo mode and production mode alike, visually distinguish:

1. **Storno entries** — strikethrough or red text with a "Storno" badge, linking to the original reversed entry.
2. **Recalculated entries** — an "Omberegnet" (recalculated) badge showing these replaced the stornoed originals.
3. **Coverage reversals (dækningsophævelse)** — an info banner explaining the coverage reallocation.
4. **Crossing-transaction group** — entries posted as part of a crossing-transaction replay share a common `transactionId`; the UI shall visually group them (e.g., a colored left border or expandable section).

### FR-5: Start script integration

A `start-caseworker-demo.ps1` PowerShell script shall be provided that:

1. Ensures PostgreSQL is running (checks `pg_isready`).
2. Starts `opendebt-case-service` on port 8083 with `spring.profiles.active=local`.
3. Starts `opendebt-debt-service` on port 8082 with `spring.profiles.active=local`.
4. Starts `opendebt-payment-service` on port 8084 with `spring.profiles.active=local`.
5. Starts `opendebt-caseworker-portal` on port 8087 with `spring.profiles.active=dev`.
6. Waits for health checks on all four services.
7. Opens `http://localhost:8087/demo-login` in the default browser.

The script follows the same pattern as `start-portal-demo.ps1`.

### FR-6: Demo data seeder as conditional Spring bean

The demo data seeder shall:
- Be annotated with `@Profile("dev")` so it only runs in demo mode.
- Implement `ApplicationRunner` to seed data on startup.
- Be idempotent: check if demo data already exists before inserting.
- Use the payment-service's internal repositories directly (not the API), since the seeder runs inside the same process.
- Log a summary of seeded data at INFO level.

For case-service and debt-service demo data, use SQL migration seed data in `V1__baseline.sql` (same pattern as existing debt-service seed data), conditioned on demo profile via a Spring bean that runs additional SQL.

### FR-7: Posteringslog export

The posteringslog page shall include a "Download CSV" button that exports the currently displayed ledger entries as a semicolon-separated CSV file with Danish column headers:

```
Dato;Bogføringsdato;Type;Kategori;Beløb;Beskrivelse;Transaktion
2025-02-01;2025-02-01;DEBIT;REGISTRATION;45000.00;Registrering af skattegæld;txn-001
```

This supports caseworkers who need to share accounting data with external auditors or legal counsel.

## Out of scope

- Production authentication and authorization (handled by petition 040 and Keycloak configuration).
- Caseworker user management and role assignment (separate petition).
- Write operations from the caseworker portal (objections, adjustments, etc.).
- Load testing or performance benchmarking of demo data seeding.
- Automated end-to-end tests of the demo flow (manual testing is sufficient for demo mode).

## Non-functional requirements

| Requirement | Target |
|------------|--------|
| Demo startup time | All 4 services healthy within 90 seconds |
| Demo data volume | Sufficient to demonstrate all crossing-transaction artifacts without overwhelming the UI |
| Browser support | Same as creditor-portal (Chrome, Firefox, Edge — latest 2 versions) |
| Accessibility | Same WCAG 2.1 AA compliance as creditor-portal (petition 013) |

## References

- Petition 039: Krydsende handlinger (crossing transactions)
- Petition 040: Ledger query API and Sagsbehandlerportal
- `docs/crossing-transactions.md`: Technical documentation of crossing-transaction handling
- ADR-0018: Double-Entry Bookkeeping
- Creditor-portal `DemoLoginController.java`: Reference implementation for demo login pattern
- GIL § 4, stk. 1 (dækningsrækkefølge)
- Inddrivelsesloven § 8 (rente)
