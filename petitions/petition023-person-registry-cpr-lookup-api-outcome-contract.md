# Petition 023 Outcome Contract

## Acceptance criteria

1. `POST /api/v1/persons/lookup` accepts a CPR number and returns a `personId` (UUID) without including the CPR in the response body.
2. If a person with the given CPR already exists, the lookup endpoint returns the existing `personId` without creating a duplicate record.
3. If no person with the given CPR exists, the lookup endpoint creates a new `PersonEntity` with `identifierType = CPR` and `role = PERSONAL`, stores the CPR encrypted, and returns the new `personId`.
4. Concurrent lookup requests for the same CPR do not produce duplicate person records (idempotency enforced by unique index).
5. `GET /api/v1/persons/{personId}` returns person details for a valid, non-deleted person UUID.
6. `GET /api/v1/persons/{personId}` returns `404 Not Found` when the person UUID does not exist.
7. `GET /api/v1/persons/{personId}` returns an appropriate error (e.g., `410 Gone`) when the person has been soft-deleted.
8. `GET /api/v1/persons/{personId}/exists` returns `true` for an existing, non-deleted person and `false` otherwise.
9. All three endpoints require a valid internal service-to-service bearer token; unauthenticated requests are rejected with `401`.
10. No CPR number appears in any API response from the lookup endpoint — only the `personId` UUID is returned.
11. No CPR number appears in application log output; logs reference `personId` (UUID) only.
12. The `openapi-person-registry-internal.yaml` specification is updated with the new Persons endpoints.
13. Access to `GET /api/v1/persons/{personId}` (PII retrieval) is restricted to services with the appropriate scope or role.

## Definition of done

- `PersonController`, `PersonService`, and associated DTOs are implemented in `opendebt-person-registry`.
- The lookup-or-create flow is verified with unit tests covering: new person creation, existing person retrieval, and concurrent-request idempotency.
- The get-by-ID and exists endpoints are verified with unit tests covering: found, not found, and soft-deleted cases.
- Security is verified: unauthenticated calls return `401`, unauthorized scope returns `403`.
- No CPR leaks in responses or logs are verified by test assertions.
- `openapi-person-registry-internal.yaml` includes the Persons tag with all three endpoints and their schemas.
- Every acceptance criterion is covered by at least one Gherkin scenario.

## Failure conditions

- The lookup endpoint returns the CPR number in the response body.
- Duplicate person records are created for the same CPR due to missing idempotency.
- The get-by-ID endpoint returns PII for a soft-deleted person without indicating deletion.
- CPR numbers appear in application log output.
- The endpoints are accessible without a valid service-to-service bearer token.
- The OpenAPI specification is not updated with the new Persons endpoints.
- The implementation introduces direct database access from another service into person-registry's database (violating ADR-0007).
