# Petition 023: Person Registry citizen (CPR) lookup API

## Summary

The `person-registry` service shall expose an internal REST API for citizen (CPR-based) person identity operations, mirroring the existing organization (CVR-based) API. This API enables the citizen portal and other internal services to resolve a MitID-authenticated CPR number to a technical `person_id` UUID, check person existence, and retrieve person details — all without leaking PII outside the person-registry boundary.

## Context and motivation

`person-registry` is the sole GDPR vault for personal data in OpenDebt (ADR-0014). It already stores citizen records in `PersonEntity` with CPR numbers encrypted and hashed for lookup. However, the current internal API (`openapi-person-registry-internal.yaml`) only exposes **organization** endpoints (CVR-based lookup, get-by-ID, exists-check). There are no REST endpoints for citizen/person operations.

The citizen portal (`opendebt-citizen-portal`) authenticates citizens via MitID, which yields a CPR number. After authentication the portal must resolve the CPR to a `person_id` UUID so that downstream services (debt-service, case-service, payment-service) can operate using only the technical UUID — never the CPR itself. Without this API, the citizen portal has no way to map an authenticated citizen to the internal data model.

The existing `PersonEntity` already supports encrypted CPR storage with hash-based lookup (`identifier_hash` index, `identifier_type = CPR`, `role = PERSONAL`). What is missing is the REST controller and service layer that exposes this capability to internal consumers.

## Functional requirements

### Person lookup

1. `POST /api/v1/persons/lookup` shall accept a CPR number and resolve it to an existing person record, or create a new person record if none exists (lookup-or-create pattern, identical to the existing organization lookup).
2. The request body shall contain the CPR number (`cpr`, 10-digit string) and optional metadata (name, address) that the registry may store encrypted for future use.
3. The response shall contain **only** the `personId` (UUID). The CPR itself shall never be returned in the response body.
4. If the person already exists (matched by identifier hash), the endpoint shall return the existing `personId` without creating a duplicate.
5. If the person does not exist, the endpoint shall create a new `PersonEntity` with `identifierType = CPR`, `role = PERSONAL`, store the CPR encrypted, compute the identifier hash, and return the new `personId`.

### Person retrieval

6. `GET /api/v1/persons/{personId}` shall return person details by technical UUID.
7. This endpoint is internal-only and may return decrypted PII (name, address) to authorized internal services (e.g., letter-service for Digital Post addressing). It shall **never** be exposed outside the cluster.
8. If the person does not exist, the endpoint shall return `404 Not Found`.
9. If the person has been soft-deleted (`deletedAt` is set), the endpoint shall return `410 Gone` or a response indicating the record is deleted.

### Person existence check

10. `GET /api/v1/persons/{personId}/exists` shall return a boolean indicating whether a person record with the given UUID exists and is not deleted.
11. This endpoint is lightweight and does not return any PII.

### Security and GDPR constraints

12. All three endpoints shall require a valid internal service-to-service bearer token (Keycloak client credentials).
13. No CPR number shall appear in API responses to calling services from the lookup endpoint — only the UUID is returned.
14. No CPR number shall appear in log output. Log messages shall reference `personId` (UUID) only.
15. CPR numbers shall be stored encrypted in `identifier_encrypted` (already implemented in `PersonEntity`).
16. CPR numbers shall be hashed for lookup in `identifier_hash` (already implemented in `PersonEntity`).
17. Access to `GET /api/v1/persons/{personId}` (which returns decryptable PII) shall be restricted to services with an explicit `read:persons` scope or equivalent role.

## Technical approach

- Add a `PersonController` annotated with `@RestController` and `@RequestMapping("/api/v1/persons")` in the `person-registry` service.
- Add a `PersonService` (interface + implementation) encapsulating lookup-or-create logic, encryption, and hashing.
- Reuse the existing `PersonRepository` (JPA) and `PersonEntity` — no schema changes required.
- The lookup-or-create operation shall be idempotent: concurrent requests for the same CPR must not create duplicates (enforced by the existing unique index on `identifier_hash` + `role`).
- Define request/response DTOs: `PersonLookupRequest`, `PersonLookupResponse`, `PersonDto`.
- Update `openapi-person-registry-internal.yaml` with the new Persons tag and endpoints.

## Configuration

```yaml
# No new configuration required — person-registry already has
# encryption keys and database connectivity configured.
# Security scopes are managed in Keycloak.
```

## Constraints and assumptions

- The citizen portal is the primary consumer but any internal service may call these endpoints.
- `PersonEntity` and its encryption/hashing infrastructure already exist and are not changed by this petition.
- The unique index `idx_person_identifier_role` on (`identifier_hash`, `role`) prevents duplicate CPR entries.
- This petition does not cover CPR validation against the Danish CPR Register (external integration) — the CPR is accepted as provided by MitID.
- Name and address metadata in the lookup request are optional and stored for future use (e.g., letter addressing).

## Out of scope

- External CPR Register (Det Centrale Personregister) integration for CPR validation or data enrichment.
- Citizen self-service endpoints (petition 024 and beyond).
- Bulk person import or migration tooling.
- Person data update or correction endpoints.
- GDPR data subject access request (DSAR) API.
- Audit logging to CLS for person data access (tracked separately).

## Dependencies

- None. This is a foundational petition. `PersonEntity` and `PersonRepository` already exist.
