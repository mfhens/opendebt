# Petition 032 Outcome Contract

## Acceptance criteria

1. The creditor portal serves a paginated table of rejected claims (afviste fordringer) for the acting creditor.
2. The rejected claims list uses the same tabular, search, sort, and pagination patterns as the other list views (petition 029).
3. Rejected claims data is retrieved from `debt-service` through the BFF.
4. The rejected claim detail page displays: aktionsstatus (action status) and afvisningsaarsag (rejection reason text).
5. The rejected claim detail page displays ID-numre: fordrings-ID and fordringshaver-reference.
6. The rejected claim detail page displays fordringsinformation: fordringstype, fordringshaver-beskrivelse, indberetningstidspunkt, periode, and stiftelsesdato.
7. The rejected claim detail page displays renteinformation: renteregelnummer (interest rule number) and rentesatskode (interest rate code).
8. The rejected claim detail page displays fordringshaver-info: fordringshaver-ID and fordringshaver-navn.
9. The rejected claim detail page displays beloeb: oprindeligt beloeb (original amount) and fordringsbeloeb (claim amount).
10. The rejected claim detail page displays a list of fejlbeskrivelser (validation error descriptions) with error codes.
11. The rejected claim detail page displays sagsbehandler-bemaerkning (caseworker remark) when present.
12. Each rejected claim displays its debtor list with: skyldner-ID (CPR censored, CVR, SE, or AKR), forfaldsdato, sidste rettidige betalingsdato, foraeldelsesdato, domsdato (if applicable), forligsdato (if applicable), bobehandling flag, and skyldner-note.
13. CPR numbers are censored before display: first 6 digits followed by `****`.
14. A configurable flag controls whether debtor details are shown at all.
15. Validation errors display both the numeric error code and the Danish description.
16. Error codes correspond to the validation rules in petitions 015-018.
17. The rejected claims list and detail view are accessible to all users with `CREDITOR_VIEWER` or `CREDITOR_EDITOR` roles.
18. Unauthenticated users are redirected to the login page.
19. All pages use the SKAT standardlayout (skip link, header, breadcrumb, main content, footer) from `layout/default.html`.
20. The detail page breadcrumb shows: Forside > Afviste fordringer > [Fordrings-ID].
21. Error descriptions use an `skat-alert skat-alert--error` component or similar error styling.
22. All user-facing text uses message bundles (petition 021) with Danish and English translations.
23. Monetary amounts are formatted with 2 decimal places using comma as decimal separator (Danish locale).
24. Dates are formatted as `dd.MM.yyyy`.
25. Claim data is loaded from `debt-service` through the creditor-portal BFF; the portal does not query `debt-service` directly.
26. The BFF client uses injected `WebClient.Builder` (not `WebClient.create()`), verified by ArchUnit test.

## Definition of done

- The creditor portal renders a rejected claims list with pagination, sorting, search, and date range filtering, using the same patterns as petition 029.
- The rejected claims list loads data from `debt-service` through the BFF.
- Clicking a claim row navigates to the rejected claim detail page.
- The rejected claim detail page displays: aktionsstatus, afvisningsaarsag, ID-numre, fordringsinformation, renteinformation, fordringshaver-info, beloeb, fejlbeskrivelser with error codes, and sagsbehandler-bemaerkning (when present).
- Each error in the fejlbeskrivelser list displays both the numeric error code and the Danish description.
- The debtor list displays all required fields: skyldner-ID (CPR censored), forfaldsdato, sidste rettidige betalingsdato, foraeldelsesdato, domsdato, forligsdato, bobehandling flag, and skyldner-note.
- CPR numbers are censored (first 6 digits + `****`).
- A configurable flag controls debtor detail visibility.
- Role-based access control restricts visibility to `CREDITOR_VIEWER` and `CREDITOR_EDITOR` roles.
- Unauthenticated users are redirected to the login page.
- The SKAT standardlayout is applied on all pages.
- The detail page breadcrumb shows: Forside > Afviste fordringer > [Fordrings-ID].
- Error descriptions use error alert styling (`skat-alert skat-alert--error` or similar).
- Monetary amounts use Danish locale formatting (comma decimal separator, 2 decimal places).
- Dates are formatted as `dd.MM.yyyy`.
- All user-facing text is externalised to message bundles with Danish and English translations.
- The BFF client uses injected `WebClient.Builder` (not `WebClient.create()`), verified by ArchUnit test.
- Every acceptance criterion is covered by at least one Gherkin scenario.

## Success metrics

| Metric | Target |
|--------|--------|
| Rejected claims list renders correctly with pagination, sorting, and search | 100% |
| Rejected claims list follows same patterns as petition 029 list views | 100% |
| Search by Fordrings-ID, CPR, CVR, SE returns correct results | 100% accuracy |
| Rejected claim detail page displays all required sections | 100% |
| Aktionsstatus and afvisningsaarsag displayed on detail page | 100% |
| Fejlbeskrivelser show numeric error code and Danish description | 100% |
| Error codes correspond to validation rules in petitions 015-018 | 100% |
| Sagsbehandler-bemaerkning displayed when present, hidden when absent | 100% |
| Debtor list displays all required fields | 100% |
| CPR censoring: no full CPR displayed in any view | 100% |
| Configurable flag controls debtor detail visibility | 100% |
| CREDITOR_VIEWER and CREDITOR_EDITOR can access list and detail | 100% |
| Unauthenticated or unauthorised users denied access | 100% |
| SKAT standardlayout with correct breadcrumb | 100% |
| Error alert styling applied to error descriptions | 100% |
| Monetary amounts in Danish locale format | 100% |
| Dates formatted as dd.MM.yyyy | 100% |
| Message bundle coverage for DA and EN | All user-facing text |

## Deliverables

| Deliverable | Path / Location |
|-------------|-----------------|
| Rejected claims list page (Thymeleaf template) | `creditor-portal/src/main/resources/templates/claims/rejected-list.html` |
| Rejected claim detail page (Thymeleaf template) | `creditor-portal/src/main/resources/templates/claims/rejected-detail.html` |
| Rejected detail section fragments | `creditor-portal/src/main/resources/templates/claims/fragments/` |
| Rejected claims controller | `creditor-portal/src/main/java/.../controller/RejectedClaimsController.java` |
| BFF rejected claims client methods | `creditor-portal/src/main/java/.../client/DebtServiceClient.java` |
| Rejected claim list DTO | `creditor-portal/src/main/java/.../dto/RejectedClaimListItemDto.java` |
| Rejected claim detail DTO | `creditor-portal/src/main/java/.../dto/RejectedClaimDetailDto.java` |
| Validation error DTO | `creditor-portal/src/main/java/.../dto/ValidationErrorDto.java` |
| Debtor info DTO | `creditor-portal/src/main/java/.../dto/DebtorInfoDto.java` |
| Danish message bundle entries | `creditor-portal/src/main/resources/messages_da.properties` |
| English message bundle entries | `creditor-portal/src/main/resources/messages_en_GB.properties` |
| Gherkin feature file | `petitions/petition032-fordringshaverportal-afviste-fordringer.feature` |

## Failure conditions

- The rejected claims list does not follow the same tabular, search, sort, and pagination patterns as the other list views (petition 029).
- Any required section is missing from the rejected claim detail page (aktionsstatus, afvisningsaarsag, ID-numre, fordringsinformation, renteinformation, fordringshaver-info, beloeb, fejlbeskrivelser).
- Validation errors do not display both the numeric error code and the Danish description.
- Error codes do not correspond to the validation rules in petitions 015-018.
- Sagsbehandler-bemaerkning is not displayed when present, or is displayed when absent.
- Debtor list is missing any required field (skyldner-ID, forfaldsdato, sidste rettidige betalingsdato, foraeldelsesdato, domsdato, forligsdato, bobehandling flag, skyldner-note).
- CPR numbers are displayed in full (not censored) in any view.
- The configurable debtor detail visibility flag is not respected.
- Domsdato or forligsdato is displayed when not applicable, or missing when applicable.
- Bobehandling flag is missing from debtor entries.
- A user without `CREDITOR_VIEWER` or `CREDITOR_EDITOR` role can access the rejected claims list or detail.
- An unauthenticated user can access the rejected claims list or detail.
- The page does not use the SKAT standardlayout or the breadcrumb is incorrect.
- Error descriptions do not use error alert styling.
- Monetary amounts are not formatted with comma decimal separator and 2 decimal places.
- Dates are not formatted as `dd.MM.yyyy`.
- Any user-facing text is hardcoded in the template rather than using message bundles.
- English translation is missing for any user-facing text on rejected claims pages.
- The BFF client uses `WebClient.create()` instead of injected `WebClient.Builder`.
- Claim data is fetched directly from `debt-service` by the portal instead of going through the BFF.
