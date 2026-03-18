# Petition 040 Outcome Contract

## Acceptance criteria

### Part 1: Payment-service ledger query API

1. `GET /payment-service/api/v1/ledger/debt/{debtId}` returns paginated ledger entries for a debt, ordered by effective date then posting date.
2. `GET /payment-service/api/v1/ledger/case/{caseId}` resolves debt IDs from case-service and returns a merged ledger view across all debts in the case.
3. `GET /payment-service/api/v1/events/debt/{debtId}` returns the immutable event timeline for a debt.
4. `GET /payment-service/api/v1/events/case/{caseId}` returns a merged event timeline across all debts in a case.
5. `GET /payment-service/api/v1/ledger/debt/{debtId}/summary` returns computed balance summary.
6. All endpoints enforce `CASEWORKER` or `ADMIN` role authorization.
7. Ledger entries include the bi-temporal dates (effective date and posting date).
8. Storno entries include the `reversalOfTransactionId` reference.
9. Filtering by date range, entry category, and include/exclude storno works correctly.
10. Case-level endpoints do not access case-service database directly; they use the REST API.

### Part 2: Sagsbehandlerportal

11. A new module `opendebt-caseworker-portal` exists with Thymeleaf + HTMX rendering.
12. The portal is accessible on port 8087 under `/caseworker-portal`.
13. Authentication requires Keycloak login with `CASEWORKER` or `ADMIN` role.
14. The case list page shows cases assigned to the logged-in caseworker with pagination and filters.
15. The case detail page shows case header, fordringer tab, posteringslog tab, hændelseslog tab, and workflow tab.
16. The posteringslog tab displays ledger entries with vaerdidag, bogføringsdag, konto, debet/kredit, beløb, kategori, reference, and fordring columns.
17. Storno entries are visually distinguished (strikethrough or color).
18. Coverage reversal entries from crossing transactions are flagged with a visible indicator.
19. Debit/credit pairs are visually grouped by transaction ID.
20. HTMX filtering by category, date range, and debt works without full page reload.
21. The debt detail page (from case context) shows posteringslog, event timeline, and balance summary for a single debt.
22. Danish and English i18n message bundles are provided.

## Definition of done

- All five API endpoints respond correctly with test data.
- Authorization is enforced: unauthenticated or unauthorized requests return 401/403.
- Case-level endpoints correctly resolve debt IDs via case-service REST call.
- Pagination works for ledger endpoints with > 100 entries.
- The caseworker-portal starts, authenticates, and renders the case list page.
- The posteringslog tab renders correctly for a case with multiple debts and mixed entry categories.
- Storno and coverage reversal visual indicators render correctly.
- HTMX partial reload works for at least one filter (category or date range).
- Every acceptance criterion is covered by at least one unit test, integration test, or Gherkin scenario.

## Failure conditions

- Caseworkers cannot see individual ledger postings for a case.
- Ledger entries are missing bi-temporal dates (vaerdidag or bogføringsdag absent).
- Case-level endpoint directly queries case-service database instead of REST API.
- Storno entries are not distinguishable from regular entries in the UI.
- Coverage reversal entries from crossing transactions are not flagged.
- The posteringslog cannot handle a case with > 1000 entries (no pagination or timeout).
- Non-caseworker users can access the portal or the API endpoints.
- Debit/credit pairs appear as disconnected rows with no visual grouping.
