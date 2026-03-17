# Petition 036 Outcome Contract

## Acceptance criteria

1. The creditor portal displays a searchable, filterable list of reconciliation periods for the acting creditor.
2. Filter parameters include: status (ACTIVE, CLOSED, etc.), period end date range (from, to), reconciliation start date range (from, to), and reconciliation end date range (from, to).
3. Each reconciliation entry shows summary information: status, period, and whether a response has been submitted.
4. The reconciliation detail view displays: reconciliation status, year and month of the reconciliation period, and previous response (if already submitted).
5. For ACTIVE reconciliations, the detail view additionally displays basis data: tilgang (influx amount), tilbagekaldt (recall amount), opskrevet (write-up amount), and nedskrevet (write-down amount).
6. Basis data is retrieved from OpenDebt's storage backend.
7. All amounts are displayed in DKK with 2 decimal places.
8. If no basis data exists for the period, the view displays zero amounts.
9. For ACTIVE reconciliations, the creditor can submit a response containing: forklaret difference (explained), uforklaret difference (unexplained), and total difference.
10. Validation enforces: forklaret + uforklaret == total.
11. The submitted basis data is tamper-protected; the BFF verifies basis data has not been modified client-side.
12. Only ACTIVE reconciliations may receive responses; the submit button is not available for CLOSED reconciliations.
13. Responses are submitted to `debt-service` through the BFF.
14. Service errors are propagated to the user as Danish messages.
15. The reconciliation list and detail are accessible to users with `CREDITOR_RECONCILIATION` or `CREDITOR_SUPPORT` roles.
16. Response submission requires `CREDITOR_RECONCILIATION` role; users with only `CREDITOR_SUPPORT` cannot submit responses.
17. All pages use the SKAT standardlayout.
18. Breadcrumb: Forside > Afstemning > [Period].
19. The response form uses accessible patterns with labels, validation feedback, and a confirmation step.
20. Financial tables use semantic HTML with proper `<th scope="col">` and `<th scope="row">` attributes.
21. All user-facing text uses message bundles (petition 021) with Danish and English translations.

## Definition of done

- The creditor portal renders a reconciliation list page with filters for status, period end date range, reconciliation start date range, and reconciliation end date range.
- Each reconciliation entry displays status, period, and whether a response has been submitted.
- The detail view displays reconciliation status, year/month, and previous response (if any).
- For ACTIVE reconciliations, basis data (tilgang, tilbagekaldt, opskrevet, nedskrevet) is displayed in DKK with 2 decimal places.
- If no basis data exists, zero amounts are shown.
- Creditors can submit a reconciliation response with forklaret, uforklaret, and total difference fields.
- Validation enforces forklaret + uforklaret == total; the form cannot be submitted if this condition is not met.
- Basis data is tamper-protected; the BFF verifies integrity before forwarding the response.
- Only ACTIVE reconciliations accept response submissions.
- Responses are submitted to `debt-service` through the BFF.
- Service errors are propagated to the user as localized Danish messages.
- The reconciliation list and detail are accessible to users with `CREDITOR_RECONCILIATION` or `CREDITOR_SUPPORT` roles.
- Response submission is restricted to users with `CREDITOR_RECONCILIATION` role.
- Users with only `CREDITOR_SUPPORT` can view but not submit responses.
- Unauthenticated users or users without the required roles are denied access.
- The SKAT standardlayout is applied with breadcrumb: Forside > Afstemning > [Period].
- The response form uses accessible patterns: visible labels, inline validation feedback, and a confirmation step.
- Financial tables use semantic HTML with proper scope attributes.
- All user-facing text is externalized to message bundles with Danish and English translations.
- Reconciliation data is loaded from `debt-service` through the BFF; the portal does not query `debt-service` directly.
- The BFF client uses injected `WebClient.Builder` (not `WebClient.create()`), verified by ArchUnit test.
- Every acceptance criterion is covered by at least one Gherkin scenario.

## Success metrics

| Metric | Target |
|--------|--------|
| Reconciliation list renders with all filters (status, date ranges) | 100% |
| Each entry displays status, period, and response-submitted indicator | 100% |
| Detail view displays basis data for ACTIVE reconciliations | 100% |
| Amounts displayed in DKK with 2 decimal places | 100% |
| No basis data → zero amounts displayed | 100% |
| Response validation enforces forklaret + uforklaret == total | 100% |
| Tamper protection of basis data verified by BFF | 100% |
| Only ACTIVE reconciliations accept responses | 100% |
| Service errors displayed as Danish messages | 100% |
| CREDITOR_RECONCILIATION and CREDITOR_SUPPORT can view list/detail | 100% |
| Only CREDITOR_RECONCILIATION can submit responses | 100% |
| Unauthenticated or unauthorized users denied access | 100% |
| SKAT standardlayout with correct breadcrumb | 100% |
| Response form uses accessible patterns (labels, validation, confirmation) | 100% |
| Financial tables use semantic HTML with scope attributes | 100% |
| Message bundle coverage for DA and EN | All user-facing text |

## Deliverables

| Deliverable | Path / Location |
|-------------|-----------------|
| Reconciliation list page (Thymeleaf template) | `creditor-portal/src/main/resources/templates/reconciliation/list.html` |
| Reconciliation detail page (Thymeleaf template) | `creditor-portal/src/main/resources/templates/reconciliation/detail.html` |
| Reconciliation response form fragment | `creditor-portal/src/main/resources/templates/reconciliation/fragments/response-form.html` |
| Reconciliation controller | `creditor-portal/src/main/java/.../controller/ReconciliationController.java` |
| BFF reconciliation client | `creditor-portal/src/main/java/.../client/ReconciliationServiceClient.java` |
| Reconciliation list DTO | `creditor-portal/src/main/java/.../dto/ReconciliationListItemDto.java` |
| Reconciliation detail DTO | `creditor-portal/src/main/java/.../dto/ReconciliationDetailDto.java` |
| Reconciliation response DTO | `creditor-portal/src/main/java/.../dto/ReconciliationResponseDto.java` |
| Reconciliation basis data DTO | `creditor-portal/src/main/java/.../dto/ReconciliationBasisDto.java` |
| Danish message bundle entries | `creditor-portal/src/main/resources/messages_da.properties` |
| English message bundle entries | `creditor-portal/src/main/resources/messages_en_GB.properties` |
| Gherkin feature file | `petitions/petition036-fordringshaverportal-afstemning.feature` |

## Failure conditions

- The reconciliation list does not render or is missing any of the filter parameters (status, period end date, reconciliation start date, reconciliation end date).
- A reconciliation entry does not display status, period, or response-submitted indicator.
- The detail view does not display basis data (tilgang, tilbagekaldt, opskrevet, nedskrevet) for ACTIVE reconciliations.
- Amounts are not displayed in DKK with 2 decimal places.
- The view does not display zero amounts when no basis data exists.
- The response form does not validate forklaret + uforklaret == total.
- Basis data is not tamper-protected; the BFF does not verify basis data integrity.
- A CLOSED reconciliation accepts a response submission.
- Service errors are not propagated or not displayed in Danish.
- A user without `CREDITOR_RECONCILIATION` or `CREDITOR_SUPPORT` role can access the reconciliation list or detail.
- A user with only `CREDITOR_SUPPORT` role can submit a reconciliation response.
- An unauthenticated user can access the reconciliation pages.
- The page does not use the SKAT standardlayout or the breadcrumb is incorrect.
- The response form lacks visible labels, inline validation feedback, or a confirmation step.
- Financial tables lack semantic HTML with proper scope attributes.
- Any user-facing text is hardcoded in templates rather than using message bundles.
- English translation is missing for any user-facing text on reconciliation pages.
- The BFF client uses `WebClient.create()` instead of injected `WebClient.Builder`.
- Reconciliation data is fetched directly from `debt-service` by the portal instead of going through the BFF.
