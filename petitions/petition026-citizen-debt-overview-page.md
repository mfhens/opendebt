# Petition 026: "Mit gældsoverblik" — authenticated citizen debt overview page

## Summary

The citizen portal (`opendebt-citizen-portal`) shall provide an authenticated page at `/min-gaeld` ("My debt") where citizens can view their personal debt overview after logging in with MitID. This page is the core self-service feature, modeled after Gældsstyrelsen's existing "Mit gældsoverblik" at mitgaeldsoverblik.gaeldst.dk. It replaces the external link on the landing page (petition 022) with an internal page that retrieves real debt data from the `debt-service` backend (petition 024).

## Context and motivation

The citizen portal landing page (petition 022) currently links to Gældsstyrelsen's external "Mit gældsoverblik" URL as the MitID call-to-action destination. This petition replaces that external link with an internal page that:

1. Requires MitID authentication (petition 025).
2. Uses the `person_id` stored in the session after login to call the citizen debt summary endpoint in `debt-service` (petition 024).
3. Displays the citizen's actual debt data in a clear, accessible format following the SKAT design language (ADR-0023).

The page is the primary reason citizens visit the portal. Gældsstyrelsen's existing mitgaeldsoverblik.gaeldst.dk shows:
- Total outstanding debt amount.
- A list of individual debts grouped by creditor, showing type, principal amount, outstanding balance, and status.
- Interest information.
- Links to payment options.

OpenDebt's implementation replicates this functionality within the citizen portal, using the same Thymeleaf + HTMX technology stack and SKAT visual identity established in petition 022.

## Functional requirements

### Page and authentication

1. An authenticated page shall be served at `/min-gaeld` ("My debt").
2. Unauthenticated access to `/min-gaeld` shall redirect to the MitID/TastSelv login flow (petition 025).
3. After successful login, the page shall use the `person_id` from the HTTP session (stored by petition 025) to call the citizen debt summary endpoint.

### Debt data display

4. The page shall display the citizen's total outstanding debt amount prominently at the top.
5. The page shall display a table of individual debts with the following columns:
   a. Debt type (fordring art).
   b. Creditor name (fordringshaver).
   c. Principal amount (hovedstol).
   d. Outstanding balance (restance).
   e. Due date (forfaldsdato).
   f. Status (e.g., under inddrivelse, modregnet, betalt).
6. If the citizen has no outstanding debt, the page shall display a clear message stating that no debt was found.
7. Interest information shall be displayed, including an explanation that interest accrues daily and a note about the current interest rate (configurable or retrieved from debt-service).

### Actions and links

8. The page shall include a link or button to make a payment. Initially this links to an external payment page (configurable URL, defaulting to gaeldst.dk's payment page). This will be replaced by an internal payment flow in petition 027.
9. The page shall include a placeholder link to download or view the debt overview as PDF. This is a future enhancement and shall display a "coming soon" indicator or link to the existing gaeldst.dk PDF feature.
10. The page shall include a link back to the landing page.

### Information and disclaimers

11. The page shall explain that the debt overview is a snapshot taken at the time of page load and that interest accrues daily, so the total may differ slightly from the actual balance.
12. The page shall explain how to contact Gældsstyrelsen for questions about specific debts, displaying the configurable phone number and contact information.

### Internationalization and accessibility

13. All user-facing text on the page shall be externalized to message bundles (`messages_da.properties`, `messages_en_GB.properties`) per petition 021.
14. The page shall be keyboard-navigable and screen-reader compatible per WCAG 2.1 AA (petition 013).
15. The debt table shall use proper `<table>` semantics with `<thead>`, `<th scope="col">`, and `<caption>` for accessibility.
16. Error states (service unavailable, empty results) shall be communicated accessibly with appropriate ARIA roles.

### Landing page update

17. The landing page (petition 022) MitID call-to-action button shall be updated to link to `/min-gaeld` instead of the external mitgaeldsoverblik URL.
18. When the user clicks the MitID CTA on the landing page and is not yet authenticated, they shall be redirected through the MitID login flow and then arrive at `/min-gaeld`.

## Technical approach

- **Controller**: `DebtOverviewController` serving `GET /min-gaeld`. Retrieves `person_id` from session, calls `DebtServiceClient`, and populates the Thymeleaf model.
- **Service client**: `DebtServiceClient` (or `CitizenDebtServiceClient`) calling the citizen debt summary endpoint in `debt-service` using injected `WebClient.Builder` (ADR-0024 trace propagation).
- **Template**: `min-gaeld.html` using Thymeleaf layout decorator with the citizen portal layout, rendering the debt table via `th:each` iteration.
- **Error handling**: If `debt-service` is unavailable, display a user-friendly error explaining the service is temporarily unavailable and suggesting the citizen try again later.
- **HTMX**: Optional HTMX enhancement for refreshing the debt overview without full page reload (e.g., a "Refresh" button that calls an HTMX endpoint returning the updated debt fragment).

## Configuration example

```yaml
opendebt:
  citizen:
    external-links:
      mit-gaeldsoverblik: /min-gaeld  # Updated from external URL to internal path
      payment-page: ${PAYMENT_PAGE_URL:https://gaeldst.dk/borger/saadan-betaler-du-din-gaeld}
      debt-pdf: ${DEBT_PDF_URL:https://mitgaeldsoverblik.gaeldst.dk/pdf}
      phone-number: "70 15 73 04"
      phone-international: "+45 70 15 73 04"
  services:
    debt-service:
      url: ${DEBT_SERVICE_URL:http://localhost:8082}
```

## Constraints and assumptions

- This petition assumes the citizen debt summary endpoint exists in `debt-service` (petition 024 dependency). If the endpoint is not yet available, the controller can use a stubbed `DebtServiceClient` returning mock data for development and testing.
- The debt data model returned by `debt-service` is assumed to include: debt ID, debt type, creditor reference, principal amount, outstanding balance, due date, and status.
- The `person_id` is available in the HTTP session after MitID login (petition 025 dependency).
- Currency amounts are displayed in DKK formatted according to the active locale.
- The page does not perform any write operations (no payment processing, no debt modification).
- HTMX refresh is an enhancement, not a requirement. The page must work without JavaScript as a baseline.

## PSRM Reference Context

### Interest display for citizens

From PSRM renteregler (07-renteregler.md) and [gaeldst.dk/fordringshaver/find-vejledning/renteregler](https://gaeldst.dk/fordringshaver/find-vejledning/renteregler):

- **Current inddrivelsesrente**: 5.75% per 1. januar 2026.
- **Simple day-to-day interest** on hovedstol — no compound interest (rentes rente). The page should display interest as a separate line item per debt.
- **Dækningsrækkefølge**: Rente dækkes forud for hovedstol. Citizens should understand that payments reduce accrued interest first, then principal. Consider a tooltip or info text explaining this ordering.
- **Rentestop for uafklaret gæld** (since 1. november 2024): For affected fordringer, display a note indicating that interest accrual is paused because the claim is unclear and the debtor demonstrably cannot pay.
- **No fradragsret since 2020**: Citizens should be informed that inddrivelsesrenter are not tax-deductible. Include this as a footnote or info box on the page.

### Collection step visibility

From PSRM civilretlige fordringer (11-civilretlige-fordringer.md):

- Citizens may see the following inddrivelsesskridt reflected in their debt overview:
  - **Afdragsordning** (installment plan): Active payment agreement with scheduled amounts.
  - **Modregning** (offsetting): Deduction from tax refunds or other government payments.
  - **Lønindeholdelse** (wage garnishment): Deduction from salary.
- For **civilretlige fordringer** (civil law claims): limited inddrivelsesskridt are available compared to public-law claims.
- **Betalingspåkrav** may have been filed for claims under 100,000 kr — citizens may see this as a collection action taken.

### Afskrivning scenarios

From PSRM underretningsmeddelelser (06-underretningsmeddelelser.md):

When a debt has been written off (afskrevet), citizens should see a clear status. The afskrivning årsagskoder include:

| Årsag | Description |
|-------|-------------|
| Forældelse | Statute of limitations expired |
| Konkurs | Bankruptcy — claim written off as part of estate proceedings |
| Dødsbo | Estate of deceased — claim cannot be recovered |
| Gældssanering | Debt restructuring — claim reduced or eliminated by court order |
| Åbenbart formålsløs inddrivelse | Collection deemed obviously futile |
| Uforholdsmæssigt omkostningsfuld | Collection costs would be disproportionate to recoverable amount |

Each scenario should map to a `WRITTEN_OFF` status with an explanatory sub-text so citizens understand why their debt was closed.

### Afstemning context

From PSRM afstemning (08-afstemning.md):

- The debt amounts displayed on this page represent the **inddrivelsesgrundlag**: tilgang − tilbagekald − nedskrivning + opskrivning.
- **Snapshot nature**: Saldi change daily due to renter (interest accrual), dækninger (payments applied), and reguleringer (adjustments). The page disclaimer ("amounts may differ slightly") maps directly to this reconciliation reality.
- The total outstanding amount shown is as-of the last calculation run, not a real-time computation. This aligns with the constraint noted in petition 024.

Source: [gaeldst.dk/fordringshaver/find-vejledning/renteregler](https://gaeldst.dk/fordringshaver/find-vejledning/renteregler), [civilretlige-fordringer](https://gaeldst.dk/fordringshaver/find-vejledning/civilretlige-fordringer), underretningsmeddelelser, afstemning

## Out of scope

- Payment processing (petition 027).
- PDF generation of debt overview (future enhancement).
- Instalment plan management.
- Debt dispute/objection submission (petition 006).
- Detailed debt drilldown pages (individual debt detail view).
- Creditor name resolution (the debt table may display creditor reference IDs initially; creditor name lookup is a future enhancement).

## Dependencies

- Petition 022: Citizen portal landing page (provides layout infrastructure, static resources, i18n setup).
- Petition 024: Citizen debt summary endpoint in `debt-service` (provides the backend data API).
- Petition 025: MitID/TastSelv OAuth2 browser flow (provides authentication and `person_id` session attribute).
- Petition 021: Internationalization infrastructure (message bundles, language selector).
- Petition 013: UI webtilgængelighed compliance (accessibility patterns).
- Petition 014: Accessibility statements and feedback.
