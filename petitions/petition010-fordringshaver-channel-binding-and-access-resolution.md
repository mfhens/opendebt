# Petition 010: `Fordringshaver` channel binding and access resolution

## Summary

OpenDebt shall centralize channel binding and access resolution for `fordringshavere`. Both M2M identities and portal user identities shall resolve through a backend service to an acting `fordringshaver`, optionally with acting-on-behalf-of rights through parent-child hierarchy.

## Context and motivation

The target architecture has two creditor-facing channels:

- M2M via DUPLA and `integration-gateway`
- human interaction via `creditor-portal`

Both channels must apply the same access logic. Without a central resolution model, creditor permissions would be duplicated in the gateway, portal, and debt-service. OpenDebt therefore needs a shared backend capability that maps channel identities to the correct acting creditor and validates whether acting-on-behalf-of is allowed.

## Functional requirements

1. OpenDebt shall maintain backend bindings from external M2M identities to a `fordringshaver`.
2. OpenDebt shall maintain backend bindings from portal user identities to a `fordringshaver`.
3. The binding model shall support parent-child `fordringshaver` relationships where a parent may act on behalf of a child.
4. Access resolution shall return at least:
   - the acting `fordringshaver`
   - the represented `fordringshaver`, if different
   - the channel type
   - whether the requested operation is allowed
5. If no binding exists for the presented identity, OpenDebt shall reject the request.
6. If acting-on-behalf-of is requested without an allowed hierarchy relationship, OpenDebt shall reject the request.
7. The binding and access resolution logic shall be owned by the creditor master-data backend service.
8. `integration-gateway`, `creditor-portal`, and `debt-service` shall use the shared resolution capability rather than duplicating channel-specific access rules.
9. Binding changes and access-resolution outcomes shall be auditable.

## Constraints and assumptions

- This petition defines the binding and resolution responsibility, not the final identity token or certificate schema.
- This petition assumes `fordringshaver` hierarchy is available from the creditor master-data model.
- This petition does not define the full agreement lifecycle with DUPLA.

## Out of scope

- OCES3 certificate issuance and lifecycle management
- Detailed MitID Erhverv federation behavior
- Portal UI for managing bindings
- Detailed DUPLA agreement administration workflow
