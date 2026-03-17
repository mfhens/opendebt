# Petition 029 Outcome Contract

## Acceptance criteria

1. The creditor portal serves a paginated table of claims in recovery for the acting creditor, excluding zero-balance claims.
2. The creditor portal serves a separate paginated table of zero-balance claims using the same column layout as the recovery list.
3. Each claim row displays all 13 required columns: Fordrings-ID, modtagelsesdato, skyldner-type, skyldner-ID (censored for CPR), antal skyldnere, fordringshaver-reference, fordringstype, fordringsstatus, stiftelsesdato, periode, amount sent for recovery, saldo, and saldo with interest and fees.
4. Both claim lists support server-side sorting by any displayed column in ascending and descending order.
5. Both claim lists support server-side search by Fordrings-ID, CPR number, CVR number, and SE number.
6. Both claim lists support date range filtering on modtagelsesdato (from/to).
7. Clicking a claim row navigates to the claim detail page (petition 030); for zero-balance claims older than 60 days since reaching zero balance, the detail page displays an appropriate message.
8. The portal displays the count of active claims in recovery and the count of zero-balance claims, each filterable by a user-selected date range.
9. Claim data is loaded from `debt-service` through the creditor-portal BFF; the portal does not query `debt-service` directly.
10. HTMX is used for progressive loading (table body loaded asynchronously after the page shell renders) with a loading indicator shown during fetch.
11. Pagination controls use HTMX to swap table content without a full page reload.
12. Claims lists and counts are accessible only to authenticated users with `CREDITOR_VIEWER` or `CREDITOR_EDITOR` roles.
13. Users with only `CREDITOR_RECONCILIATION` role cannot see claims lists or counts.
14. All list pages use the SKAT standardlayout (skip link, header, breadcrumb, main content, footer) from `layout/default.html`.
15. Data tables use semantic HTML (`<table>`, `<thead>`, `<tbody>`, `<th scope="col">`) with proper caption or `aria-label`.
16. Pagination controls are keyboard-accessible and announce page changes to screen readers.
17. Monetary amounts are formatted with 2 decimal places using comma as decimal separator (Danish locale).
18. Dates are formatted as `dd.MM.yyyy`.
19. CPR numbers are censored: first 6 digits followed by `****`.
20. All user-facing text uses message bundles (petition 021) with Danish and English translations.

## Definition of done

- The creditor portal renders a claims-in-recovery list and a zero-balance claims list, each with 13 columns, pagination, sorting, search, and date range filtering.
- Both lists load data from `debt-service` through the BFF using HTMX progressive loading.
- A loading indicator is shown while table data is being fetched.
- Pagination uses HTMX partial page updates (no full page reload).
- Claims counts are displayed and filterable by date range.
- Role-based access control restricts visibility to `CREDITOR_VIEWER` and `CREDITOR_EDITOR` roles; `CREDITOR_RECONCILIATION` users are denied access.
- The SKAT standardlayout is applied on all list and count pages.
- Tables use semantic HTML with accessible captions and column headers.
- Pagination controls are keyboard-navigable and screen-reader announced.
- CPR numbers are censored in display (first 6 digits + `****`).
- Monetary amounts use Danish locale formatting (comma decimal separator, 2 decimal places).
- Dates are formatted as `dd.MM.yyyy`.
- All user-facing text is externalized to message bundles with Danish and English translations.
- Clicking a claim row navigates to the claim detail page.
- Zero-balance claims older than 60 days since reaching zero balance show an appropriate message on the detail page.
- The BFF client uses injected `WebClient.Builder` (not `WebClient.create()`), verified by ArchUnit test.
- Every acceptance criterion is covered by at least one Gherkin scenario.

## Success metrics

| Metric | Target |
|--------|--------|
| Claims-in-recovery list renders correctly with all 13 columns | 100% |
| Zero-balance claims list renders correctly with all 13 columns | 100% |
| Server-side sorting works for all columns | All 13 columns |
| Search by Fordrings-ID, CPR, CVR, SE returns correct results | 100% accuracy |
| Date range filtering returns correct results | 100% accuracy |
| HTMX progressive loading shows loading indicator during fetch | Every load |
| Pagination does not trigger full page reload | Every page change |
| CREDITOR_VIEWER and CREDITOR_EDITOR can access lists | 100% |
| CREDITOR_RECONCILIATION is denied access to lists | 100% |
| Accessibility: tables pass semantic HTML validation | All tables |
| CPR censoring: no full CPR displayed in any view | 100% |
| Message bundle coverage for DA and EN | All user-facing text |

## Deliverables

| Deliverable | Path / Location |
|-------------|-----------------|
| Claims-in-recovery list page (Thymeleaf template) | `creditor-portal/src/main/resources/templates/claims/recovery-list.html` |
| Zero-balance claims list page (Thymeleaf template) | `creditor-portal/src/main/resources/templates/claims/zero-balance-list.html` |
| Claims count view (Thymeleaf template) | `creditor-portal/src/main/resources/templates/claims/counts.html` |
| HTMX table fragment templates | `creditor-portal/src/main/resources/templates/claims/fragments/` |
| Claims list controller | `creditor-portal/src/main/java/.../controller/ClaimsListController.java` |
| BFF claims client | `creditor-portal/src/main/java/.../client/DebtServiceClient.java` |
| Claims list DTOs | `creditor-portal/src/main/java/.../dto/ClaimListItemDto.java` |
| Danish message bundle entries | `creditor-portal/src/main/resources/messages_da.properties` |
| English message bundle entries | `creditor-portal/src/main/resources/messages_en_GB.properties` |
| Gherkin feature file | `petitions/petition029-fordringshaverportal-fordringer-oversigt.feature` |

## Failure conditions

- The recovery list displays zero-balance claims or the zero-balance list displays non-zero claims.
- Any of the 13 required columns is missing from either list view.
- Sorting does not work for one or more columns.
- Search by Fordrings-ID, CPR, CVR, or SE returns incorrect or no results.
- Date range filtering does not correctly filter by modtagelsesdato.
- HTMX progressive loading is not used; the page blocks on data fetch without showing a loading indicator.
- Pagination triggers a full page reload instead of HTMX content swap.
- A user with only `CREDITOR_RECONCILIATION` role can view claims lists or counts.
- An unauthenticated user can access claims lists or counts.
- Data tables lack semantic HTML (`<table>`, `<thead>`, `<th scope="col">`) or accessible captions.
- Pagination controls are not keyboard-accessible or do not announce page changes to screen readers.
- CPR numbers are displayed in full (not censored) in any view.
- Monetary amounts are not formatted with comma decimal separator and 2 decimal places.
- Dates are not formatted as `dd.MM.yyyy`.
- Any user-facing text is hardcoded in the template rather than using message bundles.
- English translation is missing for any user-facing text on claims list pages.
- The BFF client uses `WebClient.create()` instead of injected `WebClient.Builder`.
- Claims data is fetched directly from `debt-service` by the portal instead of going through the BFF.
- Clicking a claim row does not navigate to the claim detail page.
