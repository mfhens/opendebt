# Petition 011: `Fordringshaver` M2M ingress via `integration-gateway`

## Summary

OpenDebt shall expose creditor system-to-system APIs through DUPLA and `integration-gateway`, not directly from `debt-service` or `creditor-portal`. `integration-gateway` shall authenticate the external caller, resolve the acting `fordringshaver`, normalize the request, and route it to the owning internal service.

## Context and motivation

Straight-through processing is the primary creditor interaction model. The default path for `fordringshaver` submission is therefore machine-to-machine, not portal-driven. OpenDebt already uses ADR-0009 to place external APIs behind DUPLA and an internal `integration-gateway`.

To keep service boundaries clear:

- `integration-gateway` owns external ingress concerns
- `debt-service` owns the `fordring` lifecycle
- `creditor-service` owns creditor master data and channel access resolution

This petition formalizes that split for creditor M2M traffic.

## Functional requirements

1. OpenDebt shall expose creditor M2M APIs through DUPLA and `integration-gateway`.
2. External creditor systems shall not call `debt-service` directly.
3. `integration-gateway` shall authenticate and validate the external channel context before forwarding a business request.
4. `integration-gateway` shall resolve the acting `fordringshaver` through the creditor master-data backend service.
5. For `fordring` submission, `integration-gateway` shall forward the normalized request to `debt-service`.
6. For creditor master-data operations intended for external system use, `integration-gateway` shall forward the normalized request to the creditor master-data backend service.
7. If the external caller cannot be resolved or is not allowed to perform the requested operation, `integration-gateway` shall reject the request before it reaches the owning business service.
8. `integration-gateway` shall propagate correlation and audit context to downstream services.
9. `integration-gateway` shall map downstream errors to a stable external API contract.

## Constraints and assumptions

- DUPLA remains the mandated external API exposure path.
- This petition does not define the full external OpenAPI contract.
- This petition assumes synchronous REST calls to internal services.
- This petition does not define every future creditor-facing API; it defines the ingress pattern.

## Out of scope

- Detailed DUPLA agreement management
- Low-level certificate validation implementation
- Internal debt lifecycle rules
- Portal-based manual submission flow
