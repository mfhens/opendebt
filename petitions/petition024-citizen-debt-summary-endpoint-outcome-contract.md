# Petition 024 Outcome Contract

## Acceptance criteria

1. `GET /api/v1/citizen/debts` returns a paginated list of debts belonging to the authenticated citizen, identified by the `person_id` claim in the JWT token.
2. The response includes `totalOutstandingAmount` and `totalDebtCount` aggregates across all the citizen's debts.
3. Each debt item in the response includes: `debtId`, `debtTypeName`, `debtTypeCode`, `principalAmount`, `outstandingAmount`, `interestAmount`, `feesAmount`, `dueDate`, and `status`.
4. The endpoint does not return debtor PII (CPR, name, address) in the response body.
5. The endpoint does not return creditor-internal fields (readiness status, rejection reasons, internal timestamps) in the response body.
6. The `person_id` is resolved from the JWT token, not from a request parameter — citizens cannot query other citizens' debts.
7. The endpoint rejects requests without a valid citizen-scoped OAuth2 token with `401 Unauthorized`.
8. The endpoint rejects requests with creditor-scoped or caseworker-scoped tokens with `403 Forbidden`.
9. If the JWT does not contain a valid `person_id` claim, the endpoint returns `403 Forbidden`.
10. Pagination works correctly: `page` and `size` parameters control result pages, default page size is 20, maximum is 100.
11. The optional `status` query parameter filters debts by status.
12. Only debts with a populated `debtor_person_id` matching the citizen's `person_id` are returned.
13. No PII appears in log output related to this endpoint.

## Definition of done

- `CitizenDebtController` and `CitizenDebtService` are implemented in `opendebt-debt-service`.
- Response DTOs (`CitizenDebtSummaryResponse`, `CitizenDebtItemDto`) are defined and used.
- The `debtor_person_id` column exists in the debt table with an index for efficient querying.
- Unit tests verify: successful debt retrieval for authenticated citizen, empty result for citizen with no debts, pagination, status filtering, token rejection scenarios (no token, wrong scope, missing person_id claim).
- Security is verified: only citizen-scoped tokens are accepted, creditor/caseworker tokens are rejected, person_id is derived from token not request.
- No PII leaks in responses or logs are verified by test assertions.
- Every acceptance criterion is covered by at least one Gherkin scenario.

## Failure conditions

- The endpoint allows a citizen to query another citizen's debts by accepting `person_id` as a request parameter.
- Debtor PII (CPR, name, address) appears in the response body.
- Creditor-internal fields (readiness status, rejection reasons) appear in the response body.
- The endpoint accepts creditor-scoped or caseworker-scoped tokens.
- The `person_id` is not resolved from the JWT token.
- Pagination does not function correctly or exceeds the maximum page size.
- PII appears in application log output for this endpoint.
- The endpoint directly accesses person-registry's database instead of using the person_id from the JWT (violating ADR-0007).
