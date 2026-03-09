# Petition 010 Outcome Contract

## Acceptance criteria

1. M2M and portal identities are resolved to an acting `fordringshaver` through a backend capability.
2. The same access-resolution logic is used across gateway, portal, and debt-service.
3. Acting-on-behalf-of requires an allowed parent-child creditor relationship.
4. Unbound identities are rejected.
5. Access-resolution outcomes are auditable.

## Definition of done

- Channel binding exists as a first-class backend concept.
- Access resolution returns enough data to authorize downstream business operations.
- The hierarchy-based acting-on-behalf-of rule is testable.
- Every acceptance criterion is covered by at least one Gherkin scenario.

## Failure conditions

- Channel identity binding is hardcoded separately in multiple services.
- An unbound identity can submit or manage data.
- A parent/umbrella relationship is ignored when evaluating acting-on-behalf-of.
- Access resolution cannot be audited.
