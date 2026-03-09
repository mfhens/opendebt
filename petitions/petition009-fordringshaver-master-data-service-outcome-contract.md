# Petition 009 Outcome Contract

## Acceptance criteria

1. OpenDebt has a dedicated backend owner for operational `fordringshaver` master data.
2. The operational creditor model from Petition 008 is not owned by `creditor-portal` or `integration-gateway`.
3. Organization identity data remains owned by `person-registry`.
4. Internal services can look up and validate creditor status and permissions through a service API.
5. Parent-child creditor relationships are representable.
6. Audit logging and temporal history are supported for creditor master data changes.

## Definition of done

- The architecture assigns clear system-of-record ownership for operational creditor master data.
- The dependency from other services to creditor master data is API-based and testable.
- The separation between organization identity data and operational creditor data is testable.
- Every acceptance criterion is covered by at least one Gherkin scenario.

## Failure conditions

- Creditor master data is persisted in `creditor-portal`.
- Organization PII is duplicated into the operational creditor master-data store.
- Other services depend on direct database access to creditor master data.
- Creditor status or permissions cannot be resolved through a backend service API.
