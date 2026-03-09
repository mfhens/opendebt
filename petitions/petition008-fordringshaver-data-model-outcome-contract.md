# Petition 008 Outcome Contract

## Acceptance criteria

1. OpenDebt distinguishes between organization identity data in `person-registry` and operational creditor data in the creditor domain model.
2. Every operational creditor record has a technical UUID identity and a unique `external_creditor_id`.
3. Parent-child `fordringshaver` relationships are representable as a self-referencing hierarchy.
4. Interest notifications and detailed interest notifications are mutually exclusive.
5. Equalisation notifications and allocation notifications are mutually exclusive.
6. Permission flags default to `false` unless explicitly enabled.
7. If `settlement_method` is `NEM_KONTO` or `STATSREGNSKAB`, bank-account fields are not populated.
8. If automatic hearing cancellation is enabled, `auto_cancel_hearing_days` is a positive integer.
9. The creditor model uses explicit value sets for registration type, settlement method, creditor type, payment type, adjustment type profile, lifecycle status, and connection type.
10. The creditor model supports audit logging and temporal history.

## Definition of done

- The creditor logical and physical model is testable through explicit outcome criteria.
- The invariant rules for exclusivity, settlement behavior, and field formats are testable.
- The authoritative value sets needed for W1 implementation are explicit.
- Every acceptance criterion is covered by at least one Gherkin scenario.

## Failure conditions

- Organization identity data is duplicated into the operational creditor model.
- `external_creditor_id` is not unique.
- Mutually exclusive notification settings can both be enabled.
- `NEM_KONTO` or `STATSREGNSKAB` creditors retain bank-account fields.
- Automatic hearing cancellation can be enabled without a positive day count.
- Required value sets remain ambiguous for W1 implementation.
