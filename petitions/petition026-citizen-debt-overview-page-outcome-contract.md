# Petition 026 Outcome Contract

## Acceptance criteria

1. An authenticated page is served at `/min-gaeld` displaying the citizen's debt overview.
2. Unauthenticated access to `/min-gaeld` redirects to the MitID/TastSelv login flow.
3. The page calls the citizen debt summary endpoint using the `person_id` from the HTTP session.
4. The total outstanding debt amount is displayed prominently at the top of the page.
5. A table of individual debts is displayed with columns: debt type, creditor, principal amount, outstanding balance, due date, and status.
6. If no debt is found, a clear "no debt found" message is displayed instead of an empty table.
7. Interest information is displayed with an explanation that interest accrues daily.
8. A link to the payment page is present (configurable URL, initially external).
9. A placeholder link to PDF download is present (future enhancement indicator).
10. The page explains that the overview is a snapshot and the actual balance may differ.
11. Contact information (phone number) is displayed for citizens with questions.
12. All user-facing text is externalized to message bundles with Danish and English translations.
13. The debt table uses proper HTML table semantics (`<thead>`, `<th scope="col">`, `<caption>`) for accessibility.
14. The page is keyboard-navigable and screen-reader compatible (WCAG 2.1 AA).
15. The landing page (petition 022) MitID call-to-action links to `/min-gaeld` instead of an external URL.
16. Error states (debt-service unavailable, empty results) are communicated with user-friendly messages.

## Definition of done

- The citizen portal serves `/min-gaeld` as an authenticated page requiring MitID login.
- After login, the page displays debt data retrieved from `debt-service` using the session `person_id`.
- The debt table renders correctly with all required columns and proper semantic HTML.
- The page renders correctly in both Danish and English.
- No authentication or debt retrieval errors result in stack traces visible to the user.
- The `DebtServiceClient` uses injected `WebClient.Builder` (not `WebClient.create()`), verified by ArchUnit test.
- The landing page MitID CTA navigates to `/min-gaeld`, triggering login if unauthenticated.
- The page passes basic accessibility checks (heading structure, table semantics, keyboard navigation).
- Currency amounts are formatted according to the active locale (DKK).
- Every acceptance criterion is covered by at least one Gherkin scenario or unit test.

## Failure conditions

- `/min-gaeld` is accessible without authentication.
- The page does not display debt data or displays incorrect data.
- The debt table lacks proper semantic HTML (missing `<thead>`, `<th>`, or `<caption>`).
- Any user-facing text is hardcoded in the template rather than using message bundles.
- English translation is missing or incomplete for the debt overview page.
- The landing page MitID CTA still links to an external URL instead of `/min-gaeld`.
- Debt-service unavailability results in an unhandled error or stack trace.
- The `DebtServiceClient` uses `WebClient.create()` instead of the injected `WebClient.Builder`.
- The page is not keyboard-navigable or fails basic screen-reader compatibility.
- The "no debt found" state is not handled (empty table displayed instead of a message).
