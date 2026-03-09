# Petition 011 Outcome Contract

## Acceptance criteria

1. External creditor M2M calls enter OpenDebt through DUPLA and `integration-gateway`.
2. `integration-gateway` resolves the acting `fordringshaver` before routing the request.
3. Unauthorized or unresolvable M2M requests are rejected before they reach `debt-service` or the creditor master-data service.
4. `Fordring` submission requests are routed to `debt-service`.
5. Correlation and audit context are propagated to downstream services.

## Definition of done

- The M2M ingress point is testably located in `integration-gateway`.
- The routing split between gateway and owning business services is testable.
- Failed access resolution is testable as a pre-routing rejection.
- Every acceptance criterion is covered by at least one Gherkin scenario.

## Failure conditions

- External creditor systems call `debt-service` directly.
- `integration-gateway` forwards requests without resolving the acting `fordringshaver`.
- Unauthorized M2M calls reach the owning business service.
- Audit or correlation context is lost across the gateway boundary.
