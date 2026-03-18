# Petition 041 — Outcome contract

## Petition

Sagsbehandlerportal demo access with crossing-transactions showcase

## Acceptance criteria

### AC-1: Demo login page

**Given** the caseworker-portal is started with `spring.profiles.active=dev`
**When** a user navigates to `http://localhost:8087/demo-login`
**Then** a login page is displayed listing at least three demo caseworker identities (Anna Jensen, Erik Sørensen, Mette Larsen) with their roles.

### AC-2: Demo session management

**Given** the user selects a caseworker on the demo login page
**When** the user submits the form
**Then** the selected identity is stored in the HTTP session and the user is redirected to the dashboard.

**Given** the user clicks "Log ud" / navigates to `/demo-logout`
**When** the logout completes
**Then** the session is cleared and the user is redirected to `/demo-login`.

### AC-3: Security bypass in dev profile

**Given** `spring.profiles.active` includes `dev`
**When** any HTTP request is made to the caseworker-portal
**Then** no OAuth2/OIDC authentication is required and the request is permitted.

**Given** `spring.profiles.active` does NOT include `dev`
**When** a request is made without a valid JWT
**Then** the request is rejected with 401 Unauthorized.

### AC-4: Demo case exists

**Given** the demo environment has been started with `dev` profile
**When** the case list page is loaded
**Then** case `SAG-2025-00042` appears in the list with status ACTIVE and assigned caseworker Anna Jensen.

### AC-5: Demo debts exist

**Given** case `SAG-2025-00042` is opened
**When** the debts tab is displayed
**Then** at least two debts are listed: one tax debt (45,000 DKK) and one fine debt (12,500 DKK).

### AC-6: Crossing-transaction posteringslog

**Given** the tax debt (Debt A, 45,000 DKK) posteringslog is opened
**When** the full ledger is displayed
**Then** the following entries are visible in chronological order:
- Initial registration (45,000 DKK debit)
- Original interest accruals (entries #2 and #3)
- Crossing payment (vaerdidag 2025-02-15, posted 2025-04-10)
- Storno of original interest entries (#5, #6)
- Recalculated interest entries (#7, #8)
- Coverage reversal marker (#9)
- Subsequent normal payment and interest (#10, #11)

### AC-7: Storno visual indicators

**Given** the posteringslog for Debt A is displayed
**When** storno entries are present
**Then** they are visually distinguished (strikethrough or red styling with "Storno" badge) and each links to or references the original reversed entry.

### AC-8: Recalculated entry indicators

**Given** the posteringslog contains recalculated entries from a crossing-transaction replay
**When** these entries are displayed
**Then** they carry an "Omberegnet" badge distinguishing them from original postings.

### AC-9: Coverage reversal indicator

**Given** a dækningsophævelse entry exists in the posteringslog
**When** it is displayed
**Then** an info banner or badge explains that the payment coverage was reallocated due to crossing-transaction replay.

### AC-10: Crossing-transaction grouping

**Given** multiple entries were posted as part of the same crossing-transaction replay (sharing a `transactionId`)
**When** the posteringslog is displayed
**Then** these entries are visually grouped (colored border, expandable section, or similar).

### AC-11: Normal debt comparison

**Given** the fine debt (Debt B, 12,500 DKK) posteringslog is opened
**When** the ledger is displayed
**Then** only normal entries are shown (registration, payment, interest) with no stornos, recalculations, or crossing indicators.

### AC-12: CSV export

**Given** the posteringslog is displayed for any debt
**When** the user clicks "Download CSV"
**Then** a semicolon-separated CSV file is downloaded with Danish column headers (Dato, Bogføringsdato, Type, Kategori, Beløb, Beskrivelse, Transaktion).

### AC-13: Start script

**Given** PostgreSQL is running
**When** the user runs `start-caseworker-demo.ps1`
**Then** case-service, debt-service, payment-service, and caseworker-portal start successfully, health checks pass within 90 seconds, and the browser opens `http://localhost:8087/demo-login`.

### AC-14: Idempotent seeding

**Given** the demo environment is restarted with `dev` profile
**When** the demo data seeder runs again
**Then** no duplicate data is created — the seeder detects existing demo data and skips insertion.

### AC-15: Profile isolation

**Given** the application is started without the `dev` profile
**When** the application context loads
**Then** the demo data seeder bean, demo login controller, and permissive security config are NOT instantiated.

## Deliverables

1. `DemoLoginController.java` in caseworker-portal with demo caseworker identities
2. `DemoSecurityConfig.java` in caseworker-portal with `@Profile("dev")` permissive security
3. Demo data seeder (`@Profile("dev")` ApplicationRunner) in payment-service for ledger/event seed data
4. SQL seed data additions in case-service and debt-service V1__baseline.sql for demo case and debts
5. Visual indicators in posteringslog Thymeleaf templates (storno, omberegnet, dækningsophævelse, grouping)
6. CSV export endpoint or controller action for posteringslog
7. `start-caseworker-demo.ps1` PowerShell script
8. `demo-login.html` Thymeleaf template for caseworker portal

## Constraints

- Demo mode features must be completely excluded from non-dev builds via `@Profile("dev")` or `@ConditionalOnProperty`.
- No demo credentials, seed data SQL, or permissive security configs may be active in production.
- The demo data must exercise all crossing-transaction code paths from petition 039.
- The posteringslog visual indicators (AC-7 through AC-10) apply in all profiles, not just demo — they are production features that the demo merely showcases.
