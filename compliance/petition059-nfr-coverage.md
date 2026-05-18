# NFR Coverage Report — Petition 059

**Petition:** Forældelse — prescription rules (G.A.2.4)  
**Coverage basis:** `compliance/nfr-register.yaml`, `petitions/petition059-foraeldelse.md`, `petitions/petition059-foraeldelse-outcome-contract.md`  
**Detected petition tags:** `api`, `audit`, `gdpr`, `integration`, `performance`, `portal`, `service`, `ui`

## Applicable NFRs

### Must

| ID | Title | Reason | Validation hook |
|---|---|---|---|
| NFR-SEC-001 | Authentication required on all non-public endpoints | all | code-reviewer-strict |
| NFR-SEC-002 | Role-based authorisation enforced at method level | all | code-reviewer-strict |
| NFR-SEC-003 | No secrets in source code or application logs | all | code-reviewer-strict |
| NFR-SEC-004 | All inter-service and external communication over TLS | all | c4-architecture-governor |
| NFR-SEC-005 | Input validation on all API boundaries | all | bdd-test-generator |
| NFR-SEC-006 | Dependency vulnerability scan passes in CI | all | _none defined_ |
| NFR-GDPR-001 | PII must not appear in application logs | all | gdpr-database-compliance-auditor |
| NFR-GDPR-002 | PII isolated in the designated PII silo; all others use UUID | tags:service | gdpr-database-compliance-auditor |
| NFR-GDPR-003 | Lawful basis documented for every personal data category | all | gdpr-database-compliance-auditor |
| NFR-GDPR-004 | Data retention policy enforced at the storage layer | all | gdpr-database-compliance-auditor |
| NFR-GDPR-005 | Data subject access and erasure APIs available | tags:service | bdd-test-generator |
| NFR-AUDIT-001 | All state-changing operations emit a structured audit event | all | code-reviewer-strict |
| NFR-RIGS-001 | Public administrative decisions preserved for archival | all | rigsarkivet-compliance-data-assessor |
| NFR-RIGS-002 | Archival metadata complete for all statutory records | all | rigsarkivet-compliance-data-assessor |
| NFR-PERF-002 | Database queries use indexed columns and include pagination | all | code-reviewer-strict |
| NFR-RES-001 | Circuit breaker on all inter-service HTTP calls | tags:service | code-reviewer-strict |
| NFR-RES-002 | Retry with exponential backoff on transient failures | tags:service | code-reviewer-strict |
| NFR-RES-003 | Health and readiness endpoints on every service | tags:service | code-reviewer-strict |
| NFR-OBS-001 | Structured JSON logging at all log levels | all | code-reviewer-strict |
| NFR-OBS-002 | Distributed tracing headers propagated across all calls | tags:service | code-reviewer-strict |
| NFR-API-001 | API-first: OpenAPI 3.1 spec committed alongside code | all | code-reviewer-strict |
| NFR-API-002 | Breaking changes require a new API version | all | code-reviewer-strict |
| NFR-ACC-001 | Public portals meet WCAG 2.1 Level AA | tags:portal | _none defined_ |
| NFR-ARCH-001 | No direct cross-service database access | tags:service | c4-architecture-governor |
| NFR-ARCH-002 | Fællesoffentlige Arkitekturprincipper compliance | all | c4-architecture-governor |
| NFR-ARCH-003 | Business rules expressed as .drl files in ufst-rules-lib | tags:service | code-reviewer-strict |
| NFR-ARCH-004 | Statutory calculations with a legal mandate must have a Catala formal spec | tags:service | catala-encoder |

### Should

| ID | Title | Reason | Validation hook |
|---|---|---|---|
| NFR-PERF-001 | Synchronous API response time: p95 < 500 ms under normal load | all | _none defined_ |
| NFR-PERF-003 | Long-running operations must be asynchronous | all | code-reviewer-strict |
| NFR-OBS-003 | Key business events exposed as Prometheus metrics | tags:service | _none defined_ |
| NFR-ACC-002 | Form labels, error messages, and ARIA attributes present | tags:portal | _none defined_ |

## Register gaps

The following **must** NFRs are applicable but currently have **no automated validation hook** in the register:

| ID | Title | Implication |
|---|---|---|
| NFR-SEC-006 | Dependency vulnerability scan passes in CI | Needs explicit CI/review evidence outside the normal stage hooks. |
| NFR-ACC-001 | Public portals meet WCAG 2.1 Level AA | Needs an accessibility validation plan or dedicated auditor evidence. |

## Skipped NFRs

2 NFRs were not selected by the current petition text/component scope filter.

## Summary

- Applicable NFRs: 31
- Must NFRs: 27
- Should NFRs: 4
- Must-gap count (no automated hook): 2

## Handoff notes

1. Pass this report and `domain/petition059-domain-alignment.md` to `solution-architect`.
2. Pass this report to `specs-translator`.
3. Carry the two must-gap warnings into the completion report unless later stages attach explicit evidence for them.
