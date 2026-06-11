# Attachment workflow boundary and prescription linkage

## Status

Accepted

## Date

2026-06-11

Petition066 models attachment as a dedicated debt-service aggregate (not generic collection-measure overload), owned by debt-service as single writer with case-service as projection consumer. The aggregate is debtor/case-scoped with immutable `coveredFordringIds` from REQUESTED onward, asynchronous fogedret dispatch/callback correlation by OpenDebt workflow reference, strict callback state validation, and terminal idempotency. Workflow statuses are REQUESTED, IN_COURT_PROCESS, COMPLETED, UNSUCCESSFUL, and WITHDRAWN (mandatory withdrawal reason). External court traffic terminates at integration-gateway, which invokes internal debtor-scoped debt-service APIs.

Terminal COMPLETED and UNSUCCESSFUL transitions commit atomically with petition059 interruption registration, using court outcome date as interruption event date, one UDLAEG interruption emission per covered complex group, and policy-engine legal references (no override). WITHDRAWN emits no interruption. UNSUCCESSFUL requires a static enum reason code (optional free-text detail), keeping outcome semantics queryable and stable across releases. Callback ingress requires OCES3 mTLS and replay protection on callback identity tuples.
