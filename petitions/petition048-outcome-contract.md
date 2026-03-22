# Petition 048 Outcome Contract: Role-Based Data Access Control (RBAC)

## Contract Header

| Field | Value |
|---|---|
| Petition ID | 048 |
| Title | Role-Based Data Access Control (RBAC) for Cases, Claims, and Notifications |
| Type | Security & Authorization |
| Scope | Cross-service authorization enforcement at HTTP, service, and query layers |
| Status | Not Started |
| Created | 2026-03-22 |

## Acceptance Criteria (Structured)

### Category A: Caseworker Access Control

#### AC-A1: Caseworker Case List Filtering
**Given** a caseworker user with ID `cw-001` assigned to cases `case-100`, `case-101`, and `case-102`  
**And** this caseworker makes a request without SUPERVISOR role  
**When** they call `GET /api/v1/cases` with authentication token  
**Then** the response status is 200  
**And** the response contains exactly 3 cases (IDs: `case-100`, `case-101`, `case-102`)  
**And** the response does NOT contain any cases assigned to other caseworkers

**Test Data**
- Caseworker 1: `cw-001`, assigned to [`case-100`, `case-101`, `case-102`]
- Caseworker 2: `cw-002`, assigned to [`case-103`, `case-104`]
- Supervisor: `sup-001`, assigned to none (can see all)

**Expected Behavior**
- `cw-001` list → sees 3 cases
- `cw-002` list → sees 2 cases (different cases)
- `sup-001` list → sees 5 cases (all)

---

#### AC-A2: Caseworker Cannot Query Other Caseworker's Cases
**Given** a caseworker `cw-001` with only CASEWORKER role  
**When** they call `GET /api/v1/cases?caseworkerId=cw-002` (attempting to filter by another caseworker)  
**Then** the response status is 403 Forbidden  
**And** the error code is `UNAUTHORIZED_QUERY` or similar

**Rationale**: Only SUPERVISOR or ADMIN roles may query cases filtered by a specific caseworker.

---

#### AC-A3: Caseworker Case Detail Access
**Given** caseworker `cw-001` assigned to `case-100`  
**And** caseworker `cw-002` assigned to `case-103`  
**When** `cw-001` calls `GET /api/v1/cases/case-100`  
**Then** the response status is 200 and returns case details  
**When** `cw-001` calls `GET /api/v1/cases/case-103`  
**Then** the response status is 403 Forbidden

---

#### AC-A4: Caseworker Cannot Reassign Cases
**Given** caseworker `cw-001`  
**When** they call `POST /api/v1/cases/{id}/assign` with body `{ "caseworkerId": "cw-002" }`  
**Then** the response status is 403 Forbidden  
**And** the case assignment is NOT changed

**Post-Condition**: Only SUPERVISOR or ADMIN roles may call the assign endpoint.

---

#### AC-A5: Caseworker Debt Visibility Must Follow Case Assignment
**Given** debt `debt-001` linked to case `case-100`  
**And** caseworker `cw-001` assigned to `case-100`  
**When** `cw-001` calls `GET /api/v1/debts/debt-001`  
**Then** the response status is 200 and returns debt details  
**When** debt `debt-002` is linked to case `case-103` (assigned to `cw-002`)  
**And** `cw-001` calls `GET /api/v1/debts/debt-002`  
**Then** the response status is 403 Forbidden

---

### Category B: Supervisor Access Control

#### AC-B1: Supervisor Unrestricted Case List
**Given** supervisor `sup-001` with SUPERVISOR role  
**When** they call `GET /api/v1/cases` without filters  
**Then** the response status is 200  
**And** the response contains ALL cases in the system regardless of assignment

**Data Assumption**: 10 total cases exist; supervisor response includes all 10.

---

#### AC-B2: Supervisor Case Filtering
**Given** supervisor `sup-001` with SUPERVISOR role  
**When** they call `GET /api/v1/cases?caseworkerId=cw-001`  
**Then** the response status is 200  
**And** returns only the 3 cases assigned to `cw-001`

---

#### AC-B3: Supervisor Case Reassignment
**Given** supervisor `sup-001` and case `case-100` assigned to `cw-001`  
**When** `sup-001` calls `POST /api/v1/cases/case-100/assign` with body `{ "caseworkerId": "cw-002" }`  
**Then** the response status is 200  
**And** case `case-100` is now assigned to `cw-002`  
**And** an audit log entry records the reassignment

---

#### AC-B4: Supervisor Readiness Approval
**Given** debt `debt-001` in state "PENDING_READINESS"  
**And** supervisor `sup-001`  
**When** `sup-001` calls `POST /api/v1/debts/debt-001/approve-readiness`  
**Then** the response status is 200  
**And** debt state transitions to "READY_FOR_COLLECTION"

---

#### AC-B5: Supervisor Receives Escalation Tasks
**Given** case `case-100` in the debt collection workflow  
**And** a task `escalateStrategy` with `flowable:candidateGroups="supervisors"`  
**When** the workflow reaches the escalation point  
**Then** the task appears in supervisor role task list (via Flowable API)  
**And** no caseworker (without SUPERVISOR role) can claim the task

---

### Category C: Creditor Access Control

#### AC-C1: Creditor Claims Scoped to Organization
**Given** creditor user from organization `ORG-A` (CVR: `12345678`)  
**And** 5 claims created by `ORG-A`, 3 claims created by `ORG-B`  
**When** creditor calls `GET /api/v1/creditors/claims` with authentication token  
**Then** the response status is 200  
**And** returns exactly 5 claims (only those from `ORG-A`)  
**And** claims from `ORG-B` are NOT visible

---

#### AC-C2: Creditor Cannot View Other Creditor's Details
**Given** creditor user from `ORG-A`  
**When** they call `GET /api/v1/claims/{claimId}` where `claimId` belongs to `ORG-B`  
**Then** the response status is 403 Forbidden  
**Or** the response status is 404 Not Found (claim not found from perspective of requester)

---

#### AC-C3: Creditor Portal Organization Switching
**Given** creditor user authorized for both `ORG-A` and `ORG-B`  
**When** they visit the creditor portal and select "Switch Organization" to `ORG-B`  
**Then** the portal calls an internal endpoint to validate authorization  
**And** if authorized, subsequent API calls use `ORG-B` as the organization context  
**And** all claims returned are scoped to `ORG-B`

---

#### AC-C4: Creditor Cannot Access Internal Cases
**Given** creditor user from `ORG-A`  
**When** they call `GET /api/v1/cases` (case-service endpoint)  
**Then** the response status is 403 Forbidden  
**Or** the response status is 404 Not Found (endpoint not exposed to creditors)

---

### Category D: Citizen Access Control

#### AC-D1: Citizen Debt List Filtered by Person ID
**Given** citizen with `person_id = uuid-alice`  
**And** 4 debts belonging to `uuid-alice`, 6 debts belonging to other persons  
**When** citizen calls `GET /api/v1/citizen/debts` with authenticated JWT  
**Then** the response status is 200  
**And** returns exactly 4 debts  
**And** debts from other persons are NOT visible (even if they exist in the system)

---

#### AC-D2: Citizen Cannot View Other Citizen's Debts
**Given** citizen 1 with `person_id = uuid-alice`  
**And** citizen 2 with `person_id = uuid-bob`  
**When** citizen 1 calls `GET /api/v1/citizen/debts`  
**Then** the JWT claim matches `uuid-alice`  
**And** no debts with `debtor_person_id = uuid-bob` are returned

---

#### AC-D3: Citizen Simplified Case Summary
**Given** citizen accessing a case detail via `/api/v1/citizen/cases/{id}`  
**When** the response is returned  
**Then** the response includes: `caseId`, `debtorPersonId`, `status`, `totalOutstandingAmount`, `nextPaymentDeadline`  
**And** the response EXCLUDES: `assignedCaseworkerId`, `primaryCaseworkerId`, `internalCollectionStrategy`, `internalNotes`

---

#### AC-D4: Citizen Cannot Access Other Portals
**Given** citizen with CITIZEN role token  
**When** they attempt to access `http://localhost:8087/caseworker-portal`  
**Then** OAuth2 login is triggered  
**And** token validation fails (scope or audience mismatch)  
**And** redirects to login or error page

---

### Category E: VIP/PEP Case Sensitivity

#### AC-E1: VIP Case Cannot Be Assigned to Unauthorized Caseworker
**Given** case `case-vip-001` marked with sensitivity = `VIP`  
**And** caseworker `cw-001` WITHOUT `HANDLE_VIP_CASES` capability  
**And** supervisor `sup-001`  
**When** `sup-001` calls `POST /api/v1/cases/case-vip-001/assign` with body `{ "caseworkerId": "cw-001" }`  
**Then** the response status is 403 Forbidden  
**And** the error message or code indicates capability mismatch  
**And** the assignment is NOT changed

---

#### AC-E2: VIP Case Not Visible to Unqualified Caseworker
**Given** caseworker `cw-001` WITHOUT `HANDLE_VIP_CASES`  
**And** 5 normal cases, 1 VIP case exist in system  
**When** `cw-001` calls `GET /api/v1/cases`  
**Then** the response contains exactly 5 cases (the normal ones)  
**And** the VIP case is filtered out (not returned)

---

#### AC-E3: VIP Case Visible to Qualified Caseworker
**Given** caseworker `cw-002` WITH `HANDLE_VIP_CASES` capability  
**And** case `case-vip-001` assigned to `cw-002`  
**When** `cw-002` calls `GET /api/v1/cases`  
**Then** the response includes `case-vip-001`

---

#### AC-E4: Supervisor Can View and Assign VIP Cases
**Given** supervisor `sup-001`  
**And** case `case-vip-001` with sensitivity = `VIP`  
**When** `sup-001` calls `GET /api/v1/cases`  
**Then** the response includes `case-vip-001`  
**When** `sup-001` calls `POST /api/v1/cases/case-vip-001/assign` with a qualified caseworker  
**Then** the response status is 200 and assignment succeeds

---

#### AC-E5: CONFIDENTIAL Cases Supervisor-Only
**Given** case `case-conf-001` with sensitivity = `CONFIDENTIAL`  
**And** caseworker `cw-001` WITH `HANDLE_VIP_CASES` and `HANDLE_PEP_CASES`  
**When** `cw-001` calls `GET /api/v1/cases/case-conf-001`  
**Then** the response status is 403 Forbidden  
**When** supervisor `sup-001` calls the same endpoint  
**Then** the response status is 200 and returns case details

---

### Category F: Administrator Access

#### AC-F1: Admin Unrestricted Case Access
**Given** admin user with ADMIN role  
**When** admin calls `GET /api/v1/cases`  
**Then** the response returns ALL cases (no filtering)

---

#### AC-F2: Admin Can View VIP/PEP/CONFIDENTIAL
**Given** admin user  
**And** cases with all sensitivity levels (NORMAL, VIP, PEP, CONFIDENTIAL)  
**When** admin calls `GET /api/v1/cases`  
**Then** all cases are visible

---

#### AC-F3: Admin Audit Logging
**Given** admin `admin-001`  
**When** admin performs action (e.g., case assignment override)  
**Then** audit log record is created with fields: `{userId: 'admin-001', action: 'CASE_ASSIGN', resourceId: 'case-xx', resourceType: 'Case', timestamp: ..., beforeState: {...}, afterState: {...}}`

---

### Category G: Cross-Service Authorization (ADR-0007)

#### AC-G1: Service Independence (Debt-Service Revalidates)
**Given** citizen `uuid-alice` authenticated with case-service  
**And** case-service calls debt-service with `GET /api/v1/debts/debt-001?debtorPersonId=uuid-alice`  
**When** debt-service receives the request  
**Then** debt-service independently verifies that `debt-001.debtor_person_id == uuid-alice`  
**And** does NOT trust case-service's pre-filtering

**Scenario**: If debt-service allowed spoofing via custom headers, this would fail.

---

#### AC-G2: JWT Context Propagation
**Given** a user request with JWT token to case-service  
**When** case-service calls debt-service  
**Then** case-service includes the original JWT (or derives a service-to-service token) in the Authorization header  
**And** debt-service extracts claims from the token to revalidate authorization independently

---

### Category H: Audit & Compliance

#### AC-H1: Case Assignment Audit Trail
**Given** supervisor `sup-001` reassigns case `case-100` from `cw-001` to `cw-002`  
**When** the assignment completes  
**Then** an audit log entry is created:
```json
{
  "auditLogId": "audit-xxxx",
  "userId": "sup-001",
  "action": "CASE_ASSIGN",
  "resourceType": "Case",
  "resourceId": "case-100",
  "beforeState": { "assignedCaseworkerId": "cw-001" },
  "afterState": { "assignedCaseworkerId": "cw-002" },
  "timestamp": "2026-03-22T14:30:00Z"
}
```

---

#### AC-H2: Authorization Denial Logging
**Given** caseworker `cw-001` attempts to access `GET /api/v1/cases/case-103` (not assigned to them)  
**When** the request is denied with 403  
**Then** an audit log entry records: `{userId: 'cw-001', action: 'UNAUTHORIZED_ACCESS_ATTEMPT', resourceId: 'case-103', timestamp: ...}`

---

## Non-Functional Requirements (NFRs)

| NFR | Specification | Acceptance Criterion |
|---|---|---|
| **Performance** | Authorization checks shall not increase API response time by >50ms per individual rule. | Benchmark: `GET /api/v1/cases` with 1000 cases and complex filtering returns in <500ms. |
| **Scalability** | Authorization logic shall scale to 50,000+ cases and 500+ active caseworkers. | Query execution plan does not degrade beyond O(log n) with proper indexing. |
| **Audit Completeness** | All authorization decisions (allow/deny) shall be logged. | 100% of case assignments, claim accesses, debt views captured in audit table. |
| **Token Validation** | JWT validation and claim extraction must happen before authorization checks. | Malformed or expired tokens result in 401 Unauthorized before any business logic. |
| **GDPR Compliance (ADR-0014)** | No PII (CPR, names, addresses) shall be leaked through authorization bypasses. | All sensitive data references use person_id UUIDs; no CPR in case/claim/debt objects. |

## Implementation Dependencies

### Prerequisite Features
- **Keycloak realm** with roles defined: CASEWORKER, SUPERVISOR, CREDITOR, CITIZEN, ADMIN
- **JWT token structure** with claims: `sub`, `roles`, `organization`, `person_id`, `capabilities` (for VIP/PEP)
- **Case entity** with fields: `primaryCaseworkerId`, `assignedCaseworkerIds` (list), `sensitivity` (enum)
- **Claim entity** with field: `creditorOrgId`
- **Debt entity** with field: `debtorPersonId`
- **User capabilities entity** linking users to `HANDLE_VIP_CASES`, `HANDLE_PEP_CASES` flags

### Integration Points
1. **Spring Security** — `@PreAuthorize` configuration with custom accessors
2. **Flowable BPMN** — Task assignment via `candidateGroups` and `assignee` variables
3. **Audit framework** (already in place via `AuditableEntity`) — Extend for authorization events
4. **JWT claim mapping** (via `AuthenticationConverter`) — Ensure `capabilities` claim is extracted

## Risk & Mitigation

| Risk | Mitigation |
|---|---|
| Caseworker gains access to other caseworker's cases via direct case ID | Validation in `CaseService.getCaseById()` before returning; never rely on controller-level @PreAuthorize alone. |
| Creditor by-passes organization scoping via raw SQL or API manipulation | All repository methods use parameterized queries/JPA predicates with organization context baked in; no string concatenation. |
| Performance degradation from authorization checks on large datasets | Index `(caseId, assignedCaseworkerId)`, `(claimId, creditorOrgId)`, `(debtId, debtorPersonId)`. Cache user capabilities at AuthContext level. |
| VIP/PEP sensitivity levels are not enforced consistently | Unit tests for each sensitivity level; ArchUnit rule ensures `@PreAuthorize` checks sensitivity before returning cases. |
| Audit logs grow unbounded and impact database | Implement log retention policy (e.g., 1 year) and archive older logs to cold storage. |

## Verification & Testing Strategy

### Unit Tests
- **CaseServiceImplTest**: Verify `listCases()` filters by caseworker ID; verify `getCaseById()` denies unauthorized access.
- **CreditorServiceImplTest**: Verify claim queries filter by organization.
- **DebtServiceImplTest**: Verify citizen debt queries filter by person_id.
- **SensitivityFilterTest**: Verify VIP/PEP/CONFIDENTIAL cases are excluded from unqualified users' responses.

### Integration Tests
- Deploy case-service, debt-service, creditor-service in TestContainers.
- Create test fixtures: 5 cases, 10 debts, 8 claims across 3 caseworkers and 2 creditor orgs.
- Run requests as each role type; assert visibility matches rules.

### Security Tests (ArchUnit)
- Rule: All public controller methods must have `@PreAuthorize` annotation.
- Rule: All repository methods that return sensitive data must filter by authenticated user context.

### E2E Tests
- Use demo profile with seeded Keycloak users.
- Log in as caseworker, supervisor, creditor, citizen.
- Navigate portals; verify case lists, claim lists, debt lists match expected filtering.

---

## Appendix: Test Data Template

### Test Fixtures

```yaml
caseworkers:
  - id: cw-001
    name: Anna Hansen
    capabilities: []
    assignedCases: [case-100, case-101, case-102]

  - id: cw-002
    name: Bent Nielsen
    capabilities: [HANDLE_VIP_CASES]
    assignedCases: [case-103, case-104, case-vip-001]

supervisors:
  - id: sup-001
    name: Clara Larsen
    assignedCases: []

creditors:
  - orgId: ORG-A
    cvr: "12345678"
    name: "City Municipality"
    claims: [claim-001, claim-002, claim-003, claim-004, claim-005]

  - orgId: ORG-B
    cvr: "87654321"
    name: "Tax Authority"
    claims: [claim-006, claim-007, claim-008]

citizens:
  - personId: uuid-alice
    cprEncrypted: "***"
    debts: [debt-001, debt-002, debt-003, debt-004]

  - personId: uuid-bob
    cprEncrypted: "***"
    debts: [debt-005, debt-006, debt-007, debt-008, debt-009, debt-010]

cases:
  - id: case-100
    debtor: uuid-charlie
    assignedCaseworker: cw-001
    sensitivity: NORMAL
    status: ONGOING

  - id: case-vip-001
    debtor: uuid-dave
    assignedCaseworker: cw-002
    sensitivity: VIP
    status: ONGOING

  - id: case-conf-001
    debtor: uuid-eve
    sensitivity: CONFIDENTIAL
    status: RESTRICTED
```

---

## Sign-Off

| Role | Name | Date | Status |
|---|---|---|---|
| Product Owner | — | — | Pending |
| Technical Lead | — | — | Pending |
| Security & Privacy | — | — | Pending |

