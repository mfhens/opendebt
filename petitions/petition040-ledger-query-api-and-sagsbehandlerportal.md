# Petition 040: Ledger query API and Sagsbehandlerportal accounting transaction view

## Summary

OpenDebt shall expose a read API on `payment-service` for querying ledger entries and debt events by debt ID and by case ID, and shall introduce a new `opendebt-caseworker-portal` (Sagsbehandlerportal) Spring Boot module providing caseworkers with a full accounting transaction log per case and per debt.

## Context and motivation

All financial transactions in OpenDebt are recorded as immutable double-entry ledger entries and debt events in `payment-service` (ADR-0018). However, there is currently no way to retrieve this data:

- `payment-service` has a single write endpoint (`POST /api/v1/payments/incoming`). No read endpoints exist.
- `case-service` knows which debts belong to a case (`CaseEntity.debtIds`) but has no integration with payment-service to fetch accounting data.
- The `creditor-portal` (petitions 029-038) shows a financial breakdown summary to creditors, but not the itemized transaction log. Creditors see totals; caseworkers need the individual postings.
- There is no caseworker-facing UI at all. Petition 031 explicitly listed "Caseworker review UI (internal system)" as out of scope.

Sagsbehandlere (caseworkers) at Gældsstyrelsen need to:
1. See every financial event on a debtor's case: registrations, payments, interest accruals, corrections, stornos, crossing-transaction replays, and coverage reversals.
2. Understand the chronological flow and the bi-temporal dimension (vaerdidag vs. bogføringsdag).
3. Verify that dækningsrækkefølge was applied correctly (interest before principal).
4. Audit dækningsophævelser from crossing transactions (petition 039).
5. Use this information when handling indsigelser (objections), klager (complaints), and høringer (hearings).

## Part 1: Payment-service ledger query API

### FR-1: Ledger entries by debt ID

`payment-service` shall expose an endpoint that returns all ledger entries for a given debt, ordered by effective date then posting date:

```
GET /payment-service/api/v1/ledger/debt/{debtId}
```

Query parameters:
- `fromDate` (optional): filter entries with `effective_date >= fromDate`
- `toDate` (optional): filter entries with `effective_date <= toDate`
- `category` (optional): filter by `entry_category` (e.g., PAYMENT, INTEREST_ACCRUAL, STORNO, CORRECTION, COVERAGE_REVERSAL)
- `includeStorno` (optional, default true): whether to include storno/reversal entries
- Pageable (page, size, sort)

Response: `Page<LedgerEntryDto>` where `LedgerEntryDto` contains:
- `id`, `transactionId`, `debtId`
- `accountCode`, `accountName`
- `entryType` (DEBIT/CREDIT)
- `amount`
- `effectiveDate`, `postingDate`
- `reference`, `description`
- `entryCategory`
- `reversalOfTransactionId` (null if not a storno)
- `createdAt`

Authorization: `CASEWORKER`, `ADMIN`, or `SERVICE` role.

### FR-2: Ledger entries by case ID

`payment-service` shall expose an endpoint that accepts a case ID, resolves the associated debt IDs (by calling `case-service`), and returns a merged, chronologically ordered ledger view across all debts in the case:

```
GET /payment-service/api/v1/ledger/case/{caseId}
```

Same query parameters and response structure as FR-1, plus:
- Each `LedgerEntryDto` includes the `debtId` so the caller can distinguish which debt each entry belongs to.

Implementation: `payment-service` calls `case-service` at `GET /api/v1/cases/{caseId}` to retrieve `CaseDto.debtIds`, then queries its own ledger for all those debts.

Authorization: `CASEWORKER`, `ADMIN` role.

### FR-3: Debt events by debt ID

`payment-service` shall expose an endpoint returning the immutable event timeline for a debt:

```
GET /payment-service/api/v1/events/debt/{debtId}
```

Response: `List<DebtEventDto>` where `DebtEventDto` contains:
- `id`, `debtId`, `eventType`, `effectiveDate`, `amount`
- `correctsEventId` (for correction events)
- `reference`, `description`
- `ledgerTransactionId`
- `createdAt`

Authorization: `CASEWORKER`, `ADMIN`, or `SERVICE` role.

### FR-4: Debt events by case ID

Same pattern as FR-2:

```
GET /payment-service/api/v1/events/case/{caseId}
```

Resolves debts from case-service, returns merged event timeline across all debts.

### FR-5: Ledger balance summary by debt ID

A convenience endpoint returning the current computed balances:

```
GET /payment-service/api/v1/ledger/debt/{debtId}/summary
```

Response: `LedgerSummaryDto` containing:
- `debtId`
- `principalBalance` (net receivables)
- `interestBalance` (net interest receivable)
- `totalBalance`
- `totalPayments`, `totalInterestAccrued`, `totalWriteOffs`, `totalCorrections`
- `lastEventDate`, `lastPostingDate`
- `entryCount`, `stornoCount`

## Part 2: Sagsbehandlerportal (Caseworker Portal)

### FR-6: New module `opendebt-caseworker-portal`

A new Spring Boot module shall be created following the same technology choices as the creditor-portal (ADR-0023): Thymeleaf + HTMX, SKAT design tokens, DKFDS patterns.

- Port: 8087
- Context path: `/caseworker-portal`
- Authentication: Keycloak OAuth2 login with `CASEWORKER` or `ADMIN` role.
- i18n: Danish (`da`) and English (`en_GB`) message bundles per petition 021.

### FR-7: Case list page

The portal shall display a paginated list of cases assigned to the logged-in caseworker, with columns:
- Sagsnummer, skyldner (person_id resolved to initials/pseudonym via person-registry), status, strategi, samlet restance, sidst aktivitet.
- Filters: status, assigned caseworker (supervisors see all), date range.
- Clicking a case navigates to the case detail page.

### FR-8: Case detail page

The case detail page shall display:
- Case header: sagsnummer, status, strategi, sagsbehandler, skyldner reference.
- Fordringer tab: list of debts in the case with principal, interest, balance per debt.
- **Posteringslog tab** (accounting transaction log): the full ledger view from FR-2, rendered as a sortable, filterable table.
- Hændelseslog tab (event timeline): the event timeline from FR-4, rendered chronologically.
- Workflow tab: current Flowable BPMN status and available user tasks.

### FR-9: Posteringslog (accounting transaction log) view

The posteringslog tab shall display ledger entries in a table with columns:
- Vaerdidag (effective date)
- Bogføringsdag (posting date)
- Konto (account code + name)
- Debet/Kredit
- Beløb
- Kategori (entry category, translated to Danish: Betaling, Rentetilskrivning, Storno, Korrektion, Dækningsophævelse)
- Reference
- Fordring (debt ID, for multi-debt cases)

Visual indicators:
- Storno entries displayed with strikethrough or red highlight.
- Coverage reversal entries (from crossing transactions) flagged with a "Krydsende" badge.
- Grouped by transaction ID so debit/credit pairs are visually linked.
- HTMX-powered filtering by category, date range, and debt without full page reload.

### FR-10: Debt detail page (from case context)

Clicking a specific debt from the case detail page shall navigate to a debt-specific view showing:
- Debt details (from debt-service).
- Financial breakdown (similar to creditor-portal petition 030, but with full detail).
- Posteringslog filtered to that single debt (FR-1).
- Event timeline filtered to that single debt (FR-3).
- Balance summary (FR-5).

## PSRM Reference Context

### Sagsbehandler role

Gældsstyrelsen sagsbehandlere manage inddrivelsessager (collection cases). They need visibility into all financial movements to handle:
- Indsigelser (objections) from skyldnere — petition 006.
- Høringer where PSRM stamdata deviates from expected values.
- Manual payment matching when automatic OCR matching fails — petition 001.
- Tilbagekald decisions (KLAG, HENS, BORD, BORT, FEJL) where dækninger and renter must be verified.
- Crossing transaction outcomes — petition 039.

### Audit requirements

Statsligt regnskab requires full traceability. Caseworkers must be able to explain every kroner in every direction. The bi-temporal model (vaerdidag/bogføringsdag) and the storno pattern ensure that no information is lost, but this is only useful if the information is visible.

## Constraints and assumptions

- `payment-service` shall not access `case-service`'s database directly (ADR-0007). It calls the case-service REST API to resolve case → debt ID mappings.
- The caseworker-portal shall follow the same client/BFF pattern as the creditor-portal: server-side Thymeleaf rendering, WebClient calls to backend services.
- Person-registry lookup for debtor display data follows the same GDPR pattern (ADR-0014): only `person_id` UUIDs are passed; display names resolved at render time.
- The caseworker-portal requires Keycloak roles `CASEWORKER` or `ADMIN`. It is not accessible to creditors or citizens.
- The posteringslog shall handle large volumes: a case with 5 debts and 12 months of daily interest accruals could have thousands of ledger entries. Pagination and date filtering are required.

## Out of scope

- Caseworker task execution (completing Flowable user tasks) — deferred to a separate petition.
- Manual payment matching UI — deferred; petition 001 defines the matching logic.
- Letter/underretning generation from the caseworker UI — handled by letter-service.
- Supervisor dashboards and management reporting.
- Reconciliation views (afstemning) — separate module per execution backlog.

## Terminology mapping (begrebsmodel v3)

| Danish | English (code) | Context |
|--------|---------------|---------|
| Sagsbehandler | Caseworker | Gældsstyrelsen employee managing collection cases |
| Posteringslog | Transaction Log | Chronological ledger entry listing |
| Hændelseslog | Event Timeline | Immutable debt event history |
| Vaerdidag | Effective Date | When the economic event applies |
| Bogføringsdag | Posting Date | When it was recorded in the system |
| Dækningsophævelse | Coverage Reversal | Reversal from crossing transactions |
| Sag | Case | Collection case grouping debts for one debtor |
| Fordring | Claim/Debt | Individual debt within a case |
