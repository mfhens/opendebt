# Petition 035 Outcome Contract

## Acceptance criteria

1. The creditor portal provides a notification search page where creditors can check for available notifications.
2. Search parameters include: date range (from, to) and notification types (checkboxes for each type the creditor is configured to receive).
3. The search result displays the count of matching notifications.
4. Creditors can download notifications matching their search criteria.
5. Download format options include PDF and XML, selectable via checkboxes.
6. The download produces a zip file containing the matching notifications.
7. Download progress is indicated to the user (HTMX polling or progress indicator).
8. Available notification types are determined by the creditor's notification preferences in the creditor agreement (petition 008): Renteunderretninger (interest notifications), Detaljerede renteunderretninger (detailed interest notifications), Udligningsunderretninger (equalisation notifications), Allokeringsunderretninger (allocation notifications), Afregningsunderretninger (settlement notifications), Returunderretninger (return notifications), Afskrivningsunderretninger (write-off notifications).
9. Notifications are accessible to all users with `CREDITOR_VIEWER` or `CREDITOR_EDITOR` roles.
10. The notifications page uses the SKAT standardlayout.
11. Breadcrumb: Forside > Underretninger.
12. The date range picker uses standard HTML date inputs with proper labels.
13. Notification type checkboxes are grouped in a `<fieldset>` with a `<legend>`.
14. Download status is communicated to screen readers via `aria-live` region.
15. All user-facing text uses message bundles (petition 021) with Danish and English translations.

## Definition of done

- The creditor portal renders a notification search page with date range and notification type filters.
- The search returns and displays a count of matching notifications.
- Creditors can download matching notifications as a zip file containing PDF and/or XML files, selectable via checkboxes.
- Download progress is indicated to the user via HTMX polling or a progress indicator.
- Available notification types are filtered based on the creditor agreement's notification preferences.
- All seven notification types are supported: interest, detailed interest, equalisation, allocation, settlement, return, and write-off.
- The notifications page is accessible to users with `CREDITOR_VIEWER` or `CREDITOR_EDITOR` roles.
- Unauthenticated users or users without the required roles are denied access.
- The SKAT standardlayout is applied with breadcrumb: Forside > Underretninger.
- The date range picker uses standard HTML date inputs with visible labels.
- Notification type checkboxes are grouped in a `<fieldset>` with a `<legend>`.
- Download status is communicated to screen readers via `aria-live` region.
- All user-facing text is externalized to message bundles with Danish and English translations.
- Notification data is loaded from `debt-service` (or future notification service) through the BFF; the portal does not query the backend directly.
- The BFF client uses injected `WebClient.Builder` (not `WebClient.create()`), verified by ArchUnit test.
- Every acceptance criterion is covered by at least one Gherkin scenario.

## Success metrics

| Metric | Target |
|--------|--------|
| Notification search page renders with date range and type filters | 100% |
| Search returns correct count of matching notifications | 100% accuracy |
| Download produces zip file with selected formats (PDF, XML) | 100% |
| Download progress is indicated to the user | Every download |
| Notification types filtered by creditor agreement preferences | 100% |
| All 7 notification types supported | 100% |
| CREDITOR_VIEWER and CREDITOR_EDITOR can access notifications | 100% |
| Unauthenticated or unauthorized users denied access | 100% |
| SKAT standardlayout with correct breadcrumb | 100% |
| Date range picker uses standard HTML date inputs with labels | 100% |
| Checkboxes grouped in fieldset with legend | 100% |
| Download status communicated via aria-live | 100% |
| Message bundle coverage for DA and EN | All user-facing text |

## Deliverables

| Deliverable | Path / Location |
|-------------|-----------------|
| Notification search page (Thymeleaf template) | `creditor-portal/src/main/resources/templates/notifications/search.html` |
| Notification search results fragment | `creditor-portal/src/main/resources/templates/notifications/fragments/results.html` |
| Notification controller | `creditor-portal/src/main/java/.../controller/NotificationController.java` |
| BFF notification client | `creditor-portal/src/main/java/.../client/NotificationServiceClient.java` |
| Notification search DTOs | `creditor-portal/src/main/java/.../dto/NotificationSearchDto.java` |
| Notification search result DTO | `creditor-portal/src/main/java/.../dto/NotificationSearchResultDto.java` |
| Danish message bundle entries | `creditor-portal/src/main/resources/messages_da.properties` |
| English message bundle entries | `creditor-portal/src/main/resources/messages_en_GB.properties` |
| Gherkin feature file | `petitions/petition035-fordringshaverportal-underretninger.feature` |

## Failure conditions

- The notification search page does not render or is missing date range or type filters.
- The search does not return or display a count of matching notifications.
- Download does not produce a zip file with the selected formats (PDF, XML).
- Download produces files in a format the user did not select.
- Download progress is not indicated to the user.
- Notification types are not filtered by the creditor agreement's notification preferences.
- Any of the seven notification types is missing from the filter options when configured in the agreement.
- A notification type not configured in the creditor agreement is shown in the filter options.
- A user without `CREDITOR_VIEWER` or `CREDITOR_EDITOR` role can access the notifications page.
- An unauthenticated user can access the notifications page.
- The page does not use the SKAT standardlayout or the breadcrumb is incorrect.
- The date range picker does not use standard HTML date inputs or lacks visible labels.
- Notification type checkboxes are not grouped in a `<fieldset>` with a `<legend>`.
- Download status is not communicated to screen readers via `aria-live` region.
- Any user-facing text is hardcoded in templates rather than using message bundles.
- English translation is missing for any user-facing text on the notifications page.
- The BFF client uses `WebClient.create()` instead of injected `WebClient.Builder`.
- Notification data is fetched directly from the backend by the portal instead of going through the BFF.
