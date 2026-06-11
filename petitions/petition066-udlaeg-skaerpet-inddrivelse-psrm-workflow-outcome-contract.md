---
petition_id: petition066
status: draft
---

## Acceptance Criteria

- AC-01: Given a debtor and covered claims where all claims are eligible, when workflow creation is requested, then a new workflow is stored in debt-service `attachment_workflow` with status `REQUESTED` and unique `workflowReference`.
- AC-02: Given a creation request where at least one covered claim is ineligible, when workflow creation is submitted, then the request is rejected and no workflow is created.
- AC-03: Given a workflow in status `REQUESTED`, when dispatch is requested and accepted, then status becomes `IN_COURT_PROCESS` and dispatch metadata is persisted.
- AC-04: Given a workflow already dispatched, when dispatch is requested again, then no new outbound dispatch is sent and existing dispatch metadata is returned.
- AC-05: Given a workflow in `REQUESTED` or `IN_COURT_PROCESS`, when scope mutation is requested, then mutation is rejected and caller is instructed to use `WITHDRAWN` + recreate.
- AC-06: Given callback payload with debtor scope and matching `workflowReference`, when transition is legal, then callback is applied; otherwise callback is rejected with no state change.
- AC-07: Given callback with terminal `COMPLETED` and court outcome date, when transition is persisted, then `UDLAEG` interruption is registered atomically in the same transaction.
- AC-08: Given callback with terminal `UNSUCCESSFUL` and court outcome date, when transition is persisted, then `UDLAEG` interruption is registered atomically in the same transaction.
- AC-09: Given `UNSUCCESSFUL` callback without standardized reason code, when callback is processed, then request is rejected.
- AC-10: Given workflow transition to `WITHDRAWN` with mandatory caseworker reason, when transition is persisted, then no interruption is emitted.
- AC-11: Given covered claims spanning one or more claim complexes, when terminal interruption is emitted, then exactly one emission per complex group/standalone claim occurs.
- AC-12: Given terminal callback replay with same workflow and terminal status, when replay is processed, then processing is idempotent no-op and no duplicate interruption is emitted.
- AC-13: Given interruption emission from petition066, when request is sent to petition059 handling, then `type=UDLAEG` is used for both `COMPLETED` and `UNSUCCESSFUL` and policy-engine legal reference is applied.
- AC-14: Given caseworker read request on debtor scope, when workflows are returned, then response includes current status, chronological history, outcome qualifier, and interruption linkage metadata.
- AC-15: Given external fogedret callback traffic, when callback is received, then integration-gateway terminates external request and calls internal debtor-scoped debt-service API.
- AC-16: Given callback ingress at integration-gateway, when callback identity tuple (`workflowReference`, `outcomeDate`, `callbackMessageId`) has already been processed, then callback is rejected as replay and no downstream state change occurs.

## Definition of Done

- [ ] All acceptance criteria pass
- [ ] Gherkin scenarios covering AC-01 through AC-16 pass
- [ ] Fogedret dispatch and callback contract shape is documented and versioned
- [ ] Petition059 prescription-interruption linkage is observable in integration tests
- [ ] Debtor-scoped API surface is documented with workflow-reference correlation rules

## Success Conditions

PSRM can run attachment workflows end-to-end with strict state integrity, deterministic correlation, and legally correct interruption coupling for terminal court outcomes without duplicate emissions.

## Failure Conditions

Delivery fails if workflow ownership is split across writers, if scope mutates mid-flight, if callback mismatch/illegal transitions are accepted, if terminal outcomes drift from interruption records, or if duplicate dispatch/terminal replays produce duplicate legal effects.
