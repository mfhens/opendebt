# Petition 009: Dedicated `fordringshaver` master data service

## Summary

OpenDebt shall implement a dedicated backend service for operational `fordringshaver` master data. This service shall own the non-PII creditor model described in Petition 008, while `person-registry` remains the system of record for organization identity data such as name, address, and CVR/SE/AKR.

## Context and motivation

The target architecture requires a clear separation between:

- presentation (`creditor-portal`)
- external ingress (`integration-gateway`)
- debt lifecycle (`debt-service`)
- creditor master data ownership

If the operational creditor model is stored in the portal, the UI becomes the system of record. If it is stored in `debt-service`, creditor administration becomes coupled to debt lifecycle concerns. OpenDebt therefore needs a dedicated service that owns `fordringshaver` operational configuration and exposes it through internal APIs.

This petition operationalizes the architecture decision in ADR-0020 and the data model in Petition 008.

## Functional requirements

1. OpenDebt shall implement a dedicated backend service for operational `fordringshaver` master data.
2. The service shall store the non-PII operational creditor attributes described in Petition 008, including:
   - hierarchy
   - notification preferences
   - action permissions
   - settlement configuration
   - classification
   - lifecycle status
3. The service shall reference organization identity in `person-registry` using `creditor_org_id` and shall not duplicate organization PII locally.
4. The service shall expose internal APIs for:
   - creditor lookup
   - creditor status/permission validation
   - creditor administration
5. The service shall support parent-child `fordringshaver` relationships.
6. The service shall support audit logging and temporal history in line with ADR-0013.
7. Other services shall access creditor master data through the service API only and not through database access.
8. `debt-service` shall be able to validate whether a creditor is active and allowed to perform a requested action.
9. `creditor-portal` shall be able to read and update creditor master data through this service.
10. `integration-gateway` shall be able to resolve creditor master data needed for external channel handling through this service.

## Constraints and assumptions

- `person-registry` remains the source of truth for organization identity data.
- This petition does not require an immediate migration from `creditor_org_id` to a separate `creditor_id` reference in `debt-service`.
- This petition defines service ownership and API needs, not the final OpenAPI contract.
- This petition assumes synchronous REST between internal services.

## Out of scope

- Detailed onboarding workflow for new creditors
- External DUPLA API contract design
- Portal UI design
- Bulk migration/import from legacy AutoTool sources
