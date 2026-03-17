# Petition 031 Outcome Contract

## Acceptance criteria

1. The creditor portal serves a paginated table of claims in hearing for the acting creditor.
2. Each claim row displays all 10 required columns: Fordrings-ID, indberetningstidspunkt (reporting timestamp), skyldner-type and skyldner-ID (CPR censored), antal skyldnere (number of debtors), fordringshaver-reference, fordringstype (claim type name), fejl (error description — single error text, or "N fejl" if multiple), hoeringsstatus (mapped to human-readable Danish text), sags-ID (case ID), and aktionskode (action code).
3. The hearing claims list supports server-side sorting by any displayed column in ascending and descending order.
4. The hearing claims list supports server-side search by Fordrings-ID, CPR number, CVR number, and SE number.
5. The hearing claims list supports date range filtering on indberetningstidspunkt (from/to).
6. Clicking a claim row navigates to the hearing detail view for that claim.
7. The hearing detail page displays fordringsstatus (mapped from status code to Danish text).
8. The hearing detail page displays ID-numre: fordrings-ID, sags-ID, aktions-ID, fordringshaver-reference, and hovedfordrings-ID.
9. The hearing detail page displays fordringsinformation: fordringstype, fordringshaver-beskrivelse, indberetningstidspunkt, periode, and stiftelsesdato.
10. The hearing detail page displays fordringshaver-info: fordringshaver-ID and fordringshaver-navn.
11. The hearing detail page displays beloeb: oprindelig hovedstol (original principal) and modtaget beloeb (received amount).
12. The hearing detail page displays the aktionskode (action code).
13. The hearing detail page displays a skyldnerliste (debtor list) with fejltyper (error types) per skyldner.
14. When the action code indicates an opskrivning (write-up), the detail view additionally displays: opskrivningsbeloeb (write-up amount), opskrivningsaarsag (write-up reason), and reference-aktions-ID.
15. When the action code is FEJLAGTIG_HOVEDSTOL_INDBERETNING, the detail view also displays aendret oprindelig hovedstol (changed original principal).
16. The portal recognises the following write-up action codes: OPSKRIVNING_REGULERING, FEJLAGTIG_HOVEDSTOL_INDBERETNING, OPSKRIVNING_OMGJORT_NEDSKRIVNING_REGULERING, and OPSKRIVNING_ANNULLERET_NEDSKRIVNING_INDBETALING.
17. Editors can approve a hearing claim with a written justification (aarsag); the action is submitted to `debt-service` through the BFF.
18. Editors can withdraw (fortryd) a hearing claim with a reason; the action is submitted to `debt-service` through the BFF.
19. After approval, the claim status changes to "Afventer Gaeldsstyrelsen" (pending review by Gaeldsstyrelsen).
20. All approve and withdraw actions are logged to the audit log.
21. A claim in hearing is NOT received for inddrivelse until approved and accepted — the creditor's own foraeldelsesregler apply during this period.
22. Gaeldsstyrelsen treats all claims in hearing within 14 days of approval.
23. After Gaeldsstyrelsen review, the outcome is: godkendt (accepted), afvist (rejected), or tilpas indgangsfilter (adjusted).
24. The hearing list and detail view are accessible to all users with `CREDITOR_VIEWER` or `CREDITOR_EDITOR` roles.
25. Approve and withdraw actions require `CREDITOR_EDITOR` role with `allow_portal_actions` permission from the creditor agreement (petition 008).
26. Unauthenticated users are redirected to the login page.
27. All pages use the SKAT standardlayout (skip link, header, breadcrumb, main content, footer) from `layout/default.html`.
28. The hearing detail page breadcrumb shows: Forside > Fordringer i hoering > [Fordrings-ID].
29. Error descriptions are displayed in a clearly visible alert component.
30. The approve/withdraw form uses accessible form patterns with labels, validation feedback, and confirmation dialog.
31. All user-facing text uses message bundles (petition 021) with Danish and English translations.
32. CPR numbers are censored in display: first 6 digits followed by `****`.
33. Monetary amounts are formatted with 2 decimal places using comma as decimal separator (Danish locale).
34. Dates are formatted as `dd.MM.yyyy`.
35. Claim data is loaded from `debt-service` through the creditor-portal BFF; the portal does not query `debt-service` directly.
36. The BFF client uses injected `WebClient.Builder` (not `WebClient.create()`), verified by ArchUnit test.

## Definition of done

- The creditor portal renders a hearing claims list with all 10 columns, pagination, sorting, search, and date range filtering.
- The hearing claims list loads data from `debt-service` through the BFF.
- Clicking a claim row navigates to the hearing detail page.
- The hearing detail page displays: fordringsstatus, all ID-numre, fordringsinformation, fordringshaver-info, beloeb, aktionskode, and skyldnerliste with fejltyper.
- Write-up fields (opskrivningsbeloeb, opskrivningsaarsag, reference-aktions-ID, aendret oprindelig hovedstol) are conditionally displayed based on action code.
- All four write-up action codes (OPSKRIVNING_REGULERING, FEJLAGTIG_HOVEDSTOL_INDBERETNING, OPSKRIVNING_OMGJORT_NEDSKRIVNING_REGULERING, OPSKRIVNING_ANNULLERET_NEDSKRIVNING_INDBETALING) are recognised.
- Editors can approve a hearing claim with justification; status changes to "Afventer Gaeldsstyrelsen".
- Editors can withdraw a hearing claim with reason.
- Both approve and withdraw actions are submitted through the BFF to `debt-service` and logged to the audit log.
- The hearing workflow context (foraeldelsesregler, 14-day SLA, Gaeldsstyrelsen outcomes) is respected.
- Role-based access control restricts list/detail visibility to `CREDITOR_VIEWER` and `CREDITOR_EDITOR` roles.
- Approve/withdraw actions require `CREDITOR_EDITOR` role with `allow_portal_actions` permission.
- Unauthenticated users are redirected to the login page.
- The SKAT standardlayout is applied on all pages.
- The hearing detail breadcrumb shows: Forside > Fordringer i hoering > [Fordrings-ID].
- Error descriptions are displayed in alert components.
- The approve/withdraw form has accessible labels, validation feedback, and confirmation dialog.
- CPR numbers are censored (first 6 digits + `****`).
- Monetary amounts use Danish locale formatting (comma decimal separator, 2 decimal places).
- Dates are formatted as `dd.MM.yyyy`.
- All user-facing text is externalised to message bundles with Danish and English translations.
- The BFF client uses injected `WebClient.Builder` (not `WebClient.create()`), verified by ArchUnit test.
- Every acceptance criterion is covered by at least one Gherkin scenario.

## Success metrics

| Metric | Target |
|--------|--------|
| Hearing claims list renders correctly with all 10 columns | 100% |
| Server-side sorting works for all columns | All 10 columns |
| Search by Fordrings-ID, CPR, CVR, SE returns correct results | 100% accuracy |
| Date range filtering returns correct results | 100% accuracy |
| Hearing detail page displays all required sections | 100% |
| Write-up fields conditionally displayed based on action code | Correct for all 4 codes |
| FEJLAGTIG_HOVEDSTOL_INDBERETNING shows aendret oprindelig hovedstol | 100% |
| Approve action changes status to "Afventer Gaeldsstyrelsen" | 100% |
| Withdraw action submits reason to debt-service | 100% |
| Approve/withdraw actions logged to audit log | 100% |
| CREDITOR_VIEWER and CREDITOR_EDITOR can access list and detail | 100% |
| CREDITOR_EDITOR with allow_portal_actions can approve/withdraw | 100% |
| Users without allow_portal_actions cannot approve/withdraw | 100% |
| Unauthenticated or unauthorised users denied access | 100% |
| SKAT standardlayout with correct breadcrumb | 100% |
| Error descriptions in alert components | 100% |
| Approve/withdraw form accessibility (labels, validation, confirmation) | 100% |
| CPR censoring: no full CPR displayed in any view | 100% |
| Monetary amounts in Danish locale format | 100% |
| Dates formatted as dd.MM.yyyy | 100% |
| Message bundle coverage for DA and EN | All user-facing text |

## Deliverables

| Deliverable | Path / Location |
|-------------|-----------------|
| Hearing claims list page (Thymeleaf template) | `creditor-portal/src/main/resources/templates/claims/hearing-list.html` |
| Hearing claim detail page (Thymeleaf template) | `creditor-portal/src/main/resources/templates/claims/hearing-detail.html` |
| Hearing detail section fragments | `creditor-portal/src/main/resources/templates/claims/fragments/` |
| Hearing claims list controller | `creditor-portal/src/main/java/.../controller/HearingClaimsController.java` |
| BFF hearing client methods | `creditor-portal/src/main/java/.../client/DebtServiceClient.java` |
| Hearing claim list DTO | `creditor-portal/src/main/java/.../dto/HearingClaimListItemDto.java` |
| Hearing claim detail DTO | `creditor-portal/src/main/java/.../dto/HearingClaimDetailDto.java` |
| Write-up info DTO | `creditor-portal/src/main/java/.../dto/WriteUpInfoDto.java` |
| Approve/withdraw request DTOs | `creditor-portal/src/main/java/.../dto/HearingApproveRequestDto.java`, `HearingWithdrawRequestDto.java` |
| Danish message bundle entries | `creditor-portal/src/main/resources/messages_da.properties` |
| English message bundle entries | `creditor-portal/src/main/resources/messages_en_GB.properties` |
| Gherkin feature file | `petitions/petition031-fordringshaverportal-fordringer-i-hoering.feature` |

## Failure conditions

- Any of the 10 required columns is missing from the hearing claims list.
- Sorting does not work for one or more columns.
- Search by Fordrings-ID, CPR, CVR, or SE returns incorrect or no results.
- Date range filtering does not correctly filter by indberetningstidspunkt.
- Clicking a claim row does not navigate to the hearing detail page.
- The hearing detail page is missing any required section (fordringsstatus, ID-numre, fordringsinformation, fordringshaver-info, beloeb, aktionskode, skyldnerliste).
- Write-up fields are shown when the action code does not indicate an opskrivning, or omitted when it does.
- The aendret oprindelig hovedstol field is shown for action codes other than FEJLAGTIG_HOVEDSTOL_INDBERETNING, or missing when that code is present.
- Any of the four write-up action codes is not recognised.
- Approve or withdraw actions are available to users without `CREDITOR_EDITOR` role or without `allow_portal_actions` permission.
- Approve or withdraw actions are not submitted through the BFF to `debt-service`.
- After approval, the status does not change to "Afventer Gaeldsstyrelsen".
- Approve or withdraw actions are not logged to the audit log.
- A user with only `CREDITOR_VIEWER` role (without `CREDITOR_EDITOR`) can perform approve/withdraw actions.
- An unauthenticated user can access the hearing list or detail view.
- The page does not use the SKAT standardlayout or the breadcrumb is incorrect.
- Error descriptions are not displayed in alert components.
- The approve/withdraw form lacks labels, validation feedback, or confirmation dialog.
- CPR numbers are displayed in full (not censored) in any view.
- Monetary amounts are not formatted with comma decimal separator and 2 decimal places.
- Dates are not formatted as `dd.MM.yyyy`.
- Any user-facing text is hardcoded in the template rather than using message bundles.
- English translation is missing for any user-facing text on hearing pages.
- The BFF client uses `WebClient.create()` instead of injected `WebClient.Builder`.
- Claim data is fetched directly from `debt-service` by the portal instead of going through the BFF.
