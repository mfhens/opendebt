# Petition 024: Citizen-facing debt summary endpoint

## Summary

The `debt-service` shall expose a citizen-scoped REST endpoint that returns a paginated debt summary for the authenticated citizen. The endpoint resolves the citizen's `person_id` from the OAuth2 JWT token (set after MitID authentication via petition 023) and returns only debts belonging to that citizen — with no PII in the response and no creditor-oriented data leakage.

## Context and motivation

The existing `GET /api/v1/debts` endpoint in `debt-service` is creditor-oriented: it filters by `creditorId` (UUID) or `debtorId` (CPR/CVR string) and is secured with creditor/caseworker roles. Citizens should not query debts by CPR directly, and the existing endpoint exposes creditor-internal fields that are not appropriate for citizen consumption.

The citizen portal (`opendebt-citizen-portal`) needs a dedicated endpoint that:

1. Identifies the citizen from the authentication context (JWT `person_id` claim, resolved via person-registry in petition 023).
2. Returns only debts where the `debtorPersonId` matches the authenticated citizen's `person_id`.
3. Provides a summary view suitable for the "Mit gældsoverblik" self-service page: total outstanding amount, total number of debts, and a paginated list of individual debts with type, amounts, due date, and status.
4. Does not expose creditor-internal data, PII, or sensitive operational details.

This endpoint is the data backbone for the citizen debt overview page referenced in petition 022.

## Functional requirements

### Debt summary endpoint

1. `GET /api/v1/citizen/debts` shall return a paginated summary of debts belonging to the authenticated citizen.
2. The endpoint shall resolve the citizen's `person_id` from the JWT access token (e.g., a `person_id` claim or `sub` claim mapped to person_id).
3. If the JWT does not contain a valid `person_id`, the endpoint shall return `403 Forbidden`.
4. The endpoint shall query debts where the debtor's `person_id` matches the authenticated citizen's `person_id`.
5. The response shall include:
   - `debts`: paginated list of individual debt summaries
   - `totalOutstandingAmount`: sum of all outstanding amounts (principal + interest + fees) across all the citizen's debts
   - `totalDebtCount`: total number of debts for the citizen
6. Each debt summary in the list shall include:
   - `debtId` (UUID)
   - `debtTypeName` (human-readable debt type, e.g., "Parkeringsafgift")
   - `debtTypeCode`
   - `principalAmount` (original debt amount in DKK)
   - `outstandingAmount` (remaining amount including interest and fees)
   - `interestAmount` (accrued interest)
   - `feesAmount`
   - `dueDate`
   - `status` (mapped to citizen-friendly status, e.g., ACTIVE, IN_COLLECTION, PAID)
7. The response shall **not** include:
   - Debtor PII (CPR, name, address) — the debtor is implicit from the auth context
   - Creditor internal identifiers or creditor names (creditor information may be resolved separately if needed in a future petition)
   - Readiness status or readiness rejection reasons (creditor-internal concepts)
   - Internal timestamps (`createdAt`, `updatedAt`) unless needed for citizen display

### Pagination and filtering

8. The endpoint shall support standard Spring Data pagination parameters (`page`, `size`).
9. The endpoint shall optionally accept a `status` query parameter to filter debts by status.
10. Default page size shall be 20, maximum page size shall be 100.

### Security

11. The endpoint shall be secured with a citizen-scoped OAuth2 token (e.g., role `CITIZEN` or scope `citizen:debts:read`).
12. The endpoint shall reject requests with creditor-scoped or caseworker-scoped tokens (`403 Forbidden`).
13. A citizen shall only see their own debts. The `person_id` is derived from the token, not from a request parameter, preventing enumeration attacks.
14. No PII shall appear in the response body or in log output related to this endpoint.

### Data mapping

15. The debt-service currently stores `debtorId` as a CPR/CVR string. To support person_id-based queries, the debt-service shall maintain a `debtor_person_id` (UUID) column that references the person-registry. This column is populated when a debt is created (the creditor submission flow resolves the CPR to a person_id via person-registry).
16. If `debtor_person_id` is not yet populated for legacy debts, those debts shall be excluded from the citizen view until a backfill migration is completed (tracked separately).

## Technical approach

- Add a `CitizenDebtController` annotated with `@RestController` and `@RequestMapping("/api/v1/citizen/debts")` in `debt-service`.
- Add a `CitizenDebtService` (interface + implementation) that queries debts by `debtorPersonId` and computes summary aggregates.
- Extract `person_id` from the `SecurityContext` / JWT claims using a utility method or Spring Security `@AuthenticationPrincipal`.
- Define response DTOs: `CitizenDebtSummaryResponse`, `CitizenDebtItemDto`.
- Add a `debtor_person_id` UUID column to the debt table (if not already present) with an index for efficient citizen queries.
- Map `debtTypeCode` to `debtTypeName` using the existing debt-type reference data.

## Configuration

```yaml
# Keycloak realm configuration for citizen tokens
opendebt:
  security:
    citizen:
      required-role: CITIZEN           # or scope-based: citizen:debts:read
      person-id-claim: person_id       # JWT claim containing the person_id UUID
```

## Constraints and assumptions

- Citizens authenticate via MitID → citizen portal → Keycloak, which issues a JWT containing the `person_id` claim (resolved via petition 023's lookup API during the authentication flow).
- The `debtor_person_id` column in debt-service is the join key. New debts created after petition 023 will have this column populated. Legacy debts require a separate backfill.
- The endpoint does not provide write capabilities (payment, dispute). Those are separate petitions.
- Creditor names are not included in the initial response. If needed, a future petition can add creditor display name resolution via creditor-service.
- Interest and fees shown are as-of-last-calculation snapshots, not real-time computations. A note in the response or the citizen portal UI should indicate that interest accrues daily (consistent with petition 022's landing page messaging).

## PSRM Reference Context

### Debt data the citizen sees (interest rules)

From PSRM and [gaeldst.dk/fordringshaver/find-vejledning/renteregler](https://gaeldst.dk/fordringshaver/find-vejledning/renteregler):

- **Inddrivelsesrente**: 5.75% per 1. januar 2026, applied as simple day-to-day interest on the hovedstol (principal). No compound interest (rentes rente).
- **Dækningsrækkefølge**: Interest is covered before principal — when a payment (dækning) is applied, accrued rente is reduced first, then hovedstol. The `outstandingAmount` returned by this endpoint reflects this ordering.
- **Renter tilskrives** from the 1st of the month following Gældsstyrelsen's receipt of the claim.
- **Rentestop for uafklaret gæld**: Since 1. november 2024, no interest accrues on unclear claims where the debtor demonstrably cannot pay. Affected debts should have `interestAmount` frozen at the stop date.
- **No fradragsret**: Since 1. januar 2020, inddrivelsesrenter are not tax-deductible for citizens.

### Citizen-relevant underretning data

From PSRM underretningsmeddelelser (06-underretningsmeddelelser.md):

- **Afregning** (monthly settlement): Contains beløb, dato, and fordring identification. Maps to payments applied against the citizen's debts. The `outstandingAmount` and `interestAmount` fields reflect the latest afregning.
- **Afskrivning** (write-off): Triggered when a claim loses retskraft — forældelse (statute of limitations), konkurs (bankruptcy), gældssanering (debt restructuring), etc. Maps to debts transitioning from ACTIVE to a terminal status.

### Claim status mapping for citizen display

The endpoint's `status` field maps PSRM internal states to citizen-friendly values:

| PSRM State | Citizen Status | Notes |
|------------|---------------|-------|
| UDFØRT (executed/received) | `ACTIVE` | Claim is under inddrivelse |
| HØRING (hearing) | Not visible | Creditor-internal state, excluded from citizen view |
| Tilbagekaldt (recalled) | Depends on årsagskode | FEJL → removed from view; KLAG → paused/under review |
| Afskrevet (written off) | `WRITTEN_OFF` | Forældelse, konkurs, gældssanering, etc. |
| Fully paid via dækning | `PAID` | Outstanding amount = 0 after applied payments |

Source: [gaeldst.dk/fordringshaver/find-vejledning/renteregler](https://gaeldst.dk/fordringshaver/find-vejledning/renteregler), underretningsmeddelelser

## Out of scope

- Payment initiation or payment plan management from the citizen portal.
- Debt dispute or objection (indsigelse) submission.
- Creditor name resolution for display purposes.
- Real-time interest calculation at query time.
- Backfill migration of legacy debts to `debtor_person_id`.
- Detailed debt history or timeline view.
- PDF debt statement generation.

## Dependencies

- **Petition 023**: Person Registry CPR lookup API (provides the `person_id` resolution that populates `debtor_person_id` and the JWT `person_id` claim).
