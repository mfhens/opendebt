# Petition 012 Outcome Contract

## Acceptance criteria

1. `creditor-portal` is treated as a user interaction layer, not a master-data owner.
2. Manual creditor actions in the portal call the owning backend services.
3. Portal access is resolved to an acting `fordringshaver` through the shared backend resolution model.
4. Portal users cannot act for unrelated creditors.
5. The portal is not the primary M2M entry point.

## Definition of done

- The portal/backend responsibility split is testable.
- The manual debt-submission path from portal to `debt-service` is testable.
- The restriction to bound or represented creditors is testable.
- Every acceptance criterion is covered by at least one Gherkin scenario.

## Failure conditions

- The portal persists creditor master data as its own system of record.
- The portal accepts M2M traffic as the default creditor integration path.
- A portal user can act for an unrelated creditor.
- Manual debt submission bypasses `debt-service` ownership.
