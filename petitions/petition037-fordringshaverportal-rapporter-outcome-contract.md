# Petition 037 Outcome Contract

## Acceptance criteria

1. The creditor portal displays a list of available reports for the acting creditor for a selected year and month.
2. A year/month selector allows the creditor to browse available periods.
3. Each report entry shows: report name/type and availability status.
4. Reconciliation summary files are filtered out from the report list (these are accessed via the reconciliation module, petition 036).
5. The creditor can download individual reports.
6. Downloaded files are served as `application/zip` with an appropriate `Content-Disposition` filename.
7. Download progress is indicated to the user.
8. All report downloads are logged to the audit log (ADR-0022).
9. The BFF enforces that a creditor can only access reports belonging to their own creditor identity.
10. Report access is validated against the acting creditor context resolved through channel binding (petition 010).
11. No client-side key encryption scheme is used; access control is enforced server-side by the BFF and backend service.
12. Reports are accessible to users with `CREDITOR_VIEWER`, `CREDITOR_EDITOR`, `CREDITOR_RECONCILIATION`, or `CREDITOR_SUPPORT` roles.
13. The reports page uses the SKAT standardlayout.
14. Breadcrumb: Forside > Rapporter.
15. The year/month selector uses standard HTML `<select>` elements with proper labels.
16. Download buttons are keyboard-accessible with appropriate `aria-label`.
17. Download status uses `aria-live` to announce completion to screen readers.
18. All user-facing text uses message bundles (petition 021) with Danish and English translations.

## Definition of done

- The creditor portal renders a reports page with a year/month selector for browsing available periods.
- A list of available reports is displayed for the selected year and month, showing report name/type and availability status.
- Reconciliation summary files are excluded from the report list.
- Creditors can download individual reports as `application/zip` with appropriate `Content-Disposition` filename.
- Download progress is indicated to the user.
- All report downloads are logged to the audit log.
- The BFF enforces that creditors can only access their own reports, validated against the acting creditor context via channel binding.
- No client-side key encryption is used; access control is purely server-side.
- Reports are accessible to users with `CREDITOR_VIEWER`, `CREDITOR_EDITOR`, `CREDITOR_RECONCILIATION`, or `CREDITOR_SUPPORT` roles.
- Unauthenticated users or users without any of the required roles are denied access.
- The SKAT standardlayout is applied with breadcrumb: Forside > Rapporter.
- The year/month selector uses standard HTML `<select>` elements with visible labels.
- Download buttons are keyboard-accessible with appropriate `aria-label`.
- Download status is communicated to screen readers via `aria-live` region.
- All user-facing text is externalized to message bundles with Danish and English translations.
- Report data is loaded from the reporting service through the BFF; the portal does not query the backend directly.
- The BFF client uses injected `WebClient.Builder` (not `WebClient.create()`), verified by ArchUnit test.
- Every acceptance criterion is covered by at least one Gherkin scenario.

## Success metrics

| Metric | Target |
|--------|--------|
| Reports page renders with year/month selector | 100% |
| Report list displays name/type and availability for selected period | 100% |
| Reconciliation summary files excluded from report list | 100% |
| Download serves application/zip with Content-Disposition filename | 100% |
| Download progress indicated to the user | Every download |
| All downloads logged to audit log | 100% |
| Creditor can only access their own reports | 100% |
| Access validated via channel binding creditor context | 100% |
| No client-side key encryption used | 100% |
| CREDITOR_VIEWER, CREDITOR_EDITOR, CREDITOR_RECONCILIATION, CREDITOR_SUPPORT can access reports | 100% |
| Unauthenticated or unauthorized users denied access | 100% |
| SKAT standardlayout with correct breadcrumb | 100% |
| Year/month selector uses HTML select elements with labels | 100% |
| Download buttons keyboard-accessible with aria-label | 100% |
| Download status communicated via aria-live | 100% |
| Message bundle coverage for DA and EN | All user-facing text |

## Deliverables

| Deliverable | Path / Location |
|-------------|-----------------|
| Reports page (Thymeleaf template) | `creditor-portal/src/main/resources/templates/reports/list.html` |
| Report list fragment | `creditor-portal/src/main/resources/templates/reports/fragments/report-list.html` |
| Reports controller | `creditor-portal/src/main/java/.../controller/ReportsController.java` |
| BFF reporting client | `creditor-portal/src/main/java/.../client/ReportingServiceClient.java` |
| Report list DTO | `creditor-portal/src/main/java/.../dto/ReportListItemDto.java` |
| Danish message bundle entries | `creditor-portal/src/main/resources/messages_da.properties` |
| English message bundle entries | `creditor-portal/src/main/resources/messages_en_GB.properties` |
| Gherkin feature file | `petitions/petition037-fordringshaverportal-rapporter.feature` |

## Failure conditions

- The reports page does not render or is missing the year/month selector.
- The report list does not display report name/type or availability status.
- Reconciliation summary files appear in the report list.
- Download does not serve `application/zip` or lacks a `Content-Disposition` filename.
- Download progress is not indicated to the user.
- Report downloads are not logged to the audit log.
- A creditor can access reports belonging to another creditor.
- Access is not validated against the acting creditor context via channel binding.
- Client-side key encryption is used instead of server-side access control.
- A user without any of `CREDITOR_VIEWER`, `CREDITOR_EDITOR`, `CREDITOR_RECONCILIATION`, or `CREDITOR_SUPPORT` roles can access reports.
- An unauthenticated user can access the reports page.
- The page does not use the SKAT standardlayout or the breadcrumb is incorrect.
- The year/month selector does not use standard HTML `<select>` elements or lacks visible labels.
- Download buttons are not keyboard-accessible or lack `aria-label`.
- Download status is not communicated to screen readers via `aria-live`.
- Any user-facing text is hardcoded in templates rather than using message bundles.
- English translation is missing for any user-facing text on the reports page.
- The BFF client uses `WebClient.create()` instead of injected `WebClient.Builder`.
- Report data is fetched directly from the reporting service by the portal instead of going through the BFF.
