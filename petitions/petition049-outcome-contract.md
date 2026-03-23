# Petition 049 Outcome Contract: Case Handler Assignment

## Contract Header

| Field | Value |
|-------|-------|
| Petition ID | 049 |
| Title | Case Handler Assignment |
| Type | Operational Workflow / Caseworker Portal |
| Scope | Workload dashboard, unassigned queue, single/bulk assignment, reassignment, audit trail |
| Status | Not Started |
| Created | 2026-03-23 |

## Acceptance Criteria

### Category A: Workload Dashboard

#### AC-A1: Supervisor sees workload overview

**Given** 3 active caseworkers (Anna with 12 open cases, Bo with 7 open cases, Clara with 0 open cases)
**And** the authenticated user has the SUPERVISOR role
**When** the supervisor navigates to the workload overview page
**Then** all 3 caseworkers are listed with their open case counts (12, 7, 0)
**And** the page renders within 2 seconds

#### AC-A2: Workload breakdown by sensitivity

**Given** caseworker Anna has 10 NORMAL cases, 1 VIP case, and 1 PEP case
**When** the supervisor views the workload overview
**Then** Anna's row shows: Open=12, NORMAL=10, VIP=1, PEP=0, CONFIDENTIAL=0
**Note:** PEP cases are counted under the PEP column, not VIP.

#### AC-A3: Caseworker role cannot access workload dashboard

**Given** the authenticated user has only the CASEWORKER role
**When** the user requests the workload overview page
**Then** the system returns HTTP 403 Forbidden

#### AC-A4: Navigate from workload to caseworker's cases

**Given** the supervisor is on the workload overview page
**When** the supervisor clicks caseworker Bo's row
**Then** the browser navigates to the case list filtered to show only Bo's cases

### Category B: Unassigned Cases Queue

#### AC-B1: Unassigned cases are listed

**Given** 5 cases exist with no `primaryCaseworkerId` and 10 cases are assigned
**When** a SUPERVISOR navigates to the unassigned cases page
**Then** exactly 5 cases are listed
**And** each row shows case number, title, case type, sensitivity, creditor, creation date, and outstanding balance

#### AC-B2: Supervisor assigns from queue

**Given** case SAG-2026-0042 is unassigned
**And** caseworker Bo has the required capabilities for the case sensitivity
**When** the supervisor selects Bo from the assignment picker on SAG-2026-0042
**Then** SAG-2026-0042's `primaryCaseworkerId` is set to Bo's user ID
**And** a `CASEWORKER_ASSIGNED` event is recorded with method=MANUAL
**And** SAG-2026-0042 no longer appears in the unassigned queue

#### AC-B3: Caseworker self-assigns from queue

**Given** case SAG-2026-0055 is unassigned with sensitivity NORMAL
**And** the authenticated user is caseworker Clara
**When** Clara clicks "Tildel mig" (Assign to me) on SAG-2026-0055
**Then** SAG-2026-0055's `primaryCaseworkerId` is set to Clara's user ID
**And** a `CASEWORKER_ASSIGNED` event is recorded with method=SELF

#### AC-B4: Caseworker cannot self-assign VIP case without capability

**Given** case SAG-2026-0060 is unassigned with sensitivity VIP
**And** caseworker Clara does NOT have the `HANDLE_VIP_CASES` capability
**When** Clara attempts to self-assign SAG-2026-0060
**Then** the assignment is rejected with reason `CASEWORKER_LACKS_VIP_PERMISSION`
**And** an `ASSIGNMENT_DENIED` event is recorded
**And** the case remains unassigned

#### AC-B5: Queue supports pagination

**Given** 45 unassigned cases exist
**When** a supervisor opens the unassigned cases page (default page size 20)
**Then** 20 cases are displayed on page 1
**And** pagination controls show 3 pages

### Category C: Single Case Assignment and Reassignment

#### AC-C1: Reassignment on case detail

**Given** case SAG-2026-0042 is assigned to caseworker Anna
**And** the authenticated user is a SUPERVISOR
**When** the supervisor reassigns the case to caseworker Bo
**Then** `primaryCaseworkerId` changes from Anna to Bo
**And** a `CASEWORKER_ASSIGNED` event is recorded with previousCaseworkerId=Anna, method=MANUAL

#### AC-C2: Reassignment blocked by sensitivity

**Given** case SAG-2026-0070 has sensitivity CONFIDENTIAL
**And** the supervisor attempts to assign it to caseworker Bo
**When** the assignment request is submitted
**Then** the system rejects the assignment with reason `CONFIDENTIAL_CASE_RESTRICTED`
**And** the case remains assigned to its current handler (or unassigned)
**And** the portal displays the denial reason to the supervisor

#### AC-C3: Case detail shows current assignment

**Given** case SAG-2026-0042 is assigned to Anna (primary) with Bo as collaborator
**When** a SUPERVISOR views the case detail page
**Then** the page shows "Primær sagsbehandler: Anna" and "Samarbejdende: Bo"

### Category D: Bulk Assignment

#### AC-D1: Bulk assign multiple cases

**Given** 5 unassigned cases with sensitivity NORMAL
**And** the supervisor selects all 5 and chooses caseworker Bo as target
**When** the bulk assignment is submitted
**Then** all 5 cases are assigned to Bo
**And** 5 `CASEWORKER_ASSIGNED` events are recorded with method=BULK
**And** a summary shows "5 tildelt, 0 afvist" (5 assigned, 0 rejected)

#### AC-D2: Bulk assign with mixed validation results

**Given** 3 unassigned NORMAL cases and 1 unassigned VIP case
**And** the supervisor selects all 4 and targets caseworker Clara (no VIP capability)
**When** the bulk assignment is submitted
**Then** 3 NORMAL cases are assigned to Clara
**And** the VIP case is NOT assigned
**And** the summary shows "3 tildelt, 1 afvist (CASEWORKER_LACKS_VIP_PERMISSION)"

### Category E: Assignment Audit Trail

#### AC-E1: Assignment events appear in case timeline

**Given** case SAG-2026-0042 was assigned to Anna on 2026-03-20
**And** reassigned to Bo on 2026-03-22
**When** a user views the case event timeline
**Then** both assignment events are visible with timestamps, assigner, and method

#### AC-E2: Denied assignment events are recorded

**Given** a supervisor attempts to assign a VIP case to a caseworker without VIP capability
**When** the assignment is denied
**Then** an `ASSIGNMENT_DENIED` event is persisted with:
  - caseId
  - targetCaseworkerId
  - reason code
  - supervisorId (assigner)
  - timestamp

### Category F: API Endpoints

#### AC-F1: PUT /api/v1/cases/{id}/assign returns 200 on success

**Given** a valid case ID and a target caseworker with required capabilities
**When** `PUT /api/v1/cases/{id}/assign` is called with `{ "targetCaseworkerId": "..." }`
**Then** the response is HTTP 200 with the updated case representation

#### AC-F2: PUT /api/v1/cases/{id}/assign returns 403 on capability failure

**Given** a VIP case and a target caseworker without `HANDLE_VIP_CASES`
**When** `PUT /api/v1/cases/{id}/assign` is called
**Then** the response is HTTP 403 with error code `CASEWORKER_LACKS_VIP_PERMISSION`

#### AC-F3: GET /api/v1/cases/unassigned returns paginated results

**Given** 45 unassigned cases
**When** `GET /api/v1/cases/unassigned?page=0&size=20` is called by a SUPERVISOR
**Then** the response contains 20 cases and pagination metadata (totalElements=45, totalPages=3)

#### AC-F4: GET /api/v1/caseworkers/workload is restricted to SUPERVISOR/ADMIN

**Given** the authenticated user has only the CASEWORKER role
**When** `GET /api/v1/caseworkers/workload` is called
**Then** the response is HTTP 403 Forbidden

## Non-Functional Acceptance Criteria

| NFR | Acceptance Criterion |
|-----|---------------------|
| Performance | Workload dashboard renders in < 2s with 200 caseworkers and 50,000 open cases |
| Atomicity | Concurrent assignment of the same case by two supervisors results in exactly one assignment (optimistic locking) |
| Audit retention | Assignment events are queryable for at least 5 years |
| Accessibility | All new pages pass axe-core automated WCAG 2.1 AA checks |

## Implementation Dependencies

### Prerequisite Features
- Petition048 (RBAC sensitivity validation) — **Done**
- CaseEntity with `primaryCaseworkerId` / `assignedCaseworkerIds` — **Done**
- CaseEvent infrastructure (`CASEWORKER_ASSIGNED`, `ASSIGNMENT_DENIED`) — **Done**
- TB-023 (target-caseworker capability lookup from Keycloak) — **Required for full capability enforcement**

### Integration Points
1. **AssignmentGuardService** — Validate capability on every assignment
2. **CaseEventRepository** — Persist audit trail events
3. **Keycloak / person-registry** — Resolve caseworker display names and capabilities
4. **Caseworker portal BFF** — New client methods for assignment endpoints

## Risk and Mitigation

| Risk | Mitigation |
|------|------------|
| TB-023 not completed before implementation | Phase implementation: deploy UI and API first with self-assignment validation only; add target-caseworker validation when TB-023 lands |
| Caseworker list grows large | Add search/filter to caseworker picker; paginate workload dashboard |
| Concurrent assignment race conditions | Use optimistic locking (`@Version`) on CaseEntity |
| Display name resolution latency | Cache person-registry responses with short TTL (5 min) |

## Verification and Testing Strategy

### Unit Tests
- `AssignmentGuardServiceImplTest`: Validate all sensitivity × capability combinations
- `CaseServiceImplTest`: Assignment/reassignment updates `primaryCaseworkerId`, produces events
- `BulkAssignmentServiceTest`: Mixed validation results (partial success)

### Integration Tests (TestContainers)
- Full assign/reassign cycle through REST API with Keycloak token
- Concurrent assignment (optimistic lock conflict returns 409)
- Unassigned cases query respects RBAC filtering

### E2E Tests
- Supervisor assigns case from unassigned queue in browser
- Caseworker self-assigns NORMAL case
- Caseworker denied self-assignment of VIP case

## Sign-Off

| Role | Name | Date | Status |
|------|------|------|--------|
| Product Owner | — | — | Pending |
| Technical Lead | — | — | Pending |
