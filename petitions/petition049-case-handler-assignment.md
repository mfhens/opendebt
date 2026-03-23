# Petition 049: Case Handler Assignment

## Summary

Move case handler (sagsbehandler) assignment from the external Data Warehouse into the
operational OpenDebt system. Provide supervisors with a workload dashboard and an
unassigned-cases queue so they can plan and execute assignments without leaving the
caseworker portal. Allow caseworkers to self-assign from the unassigned pool when
permitted. All assignments must respect the sensitivity-based capability rules
established in petition048 and produce a full audit trail.

## Context and Motivation

Today, case handler assignment is performed outside the transactional system:

1. A supervisor queries the Data Warehouse for a **workload report** (cases per
   caseworker) and a **new cases report** (recently registered, unassigned cases).
2. Based on those reports the supervisor decides which caseworker should handle which
   case and records the decision — often manually or via a separate tool.
3. The assignment eventually propagates back to the case-service.

This workflow is slow, error-prone, and disconnected from the operational state of
cases. Moving assignment into the caseworker portal eliminates the round-trip through
the DW, provides real-time workload visibility, and enforces capability and sensitivity
rules at the point of assignment.

### Domain Terms

| Danish | English | Definition |
|--------|---------|------------|
| Sagsbehandler | Caseworker | The person responsible for a case |
| Teamleder / Supervisor | Supervisor | Manages caseworkers, assigns and reassigns cases |
| Sagsfordeling | Case distribution | The act of assigning cases to caseworkers |
| Arbejdsbyrde | Workload | Number and complexity of cases assigned to a caseworker |
| Ufordelte sager | Unassigned cases | Cases with no `primaryCaseworkerId` |

## Functional Requirements

### FR-1: Workload Dashboard (Supervisor)

1. **FR-1.1** — The caseworker portal shall display a **workload overview page**
   accessible to users with the SUPERVISOR or ADMIN role.
2. **FR-1.2** — The workload overview shall list all active caseworkers with:
   - caseworker display name (resolved via person-registry or Keycloak)
   - number of open cases (state not in CLOSED_*)
   - number of cases by sensitivity level (NORMAL, VIP, PEP)
   - number of cases received in the last 7 days
3. **FR-1.3** — The workload overview shall be sortable by each column.
4. **FR-1.4** — Clicking a caseworker row shall navigate to a filtered case list
   showing only that caseworker's cases.

### FR-2: Unassigned Cases Queue (Supervisor + Caseworker)

5. **FR-2.1** — The caseworker portal shall display an **unassigned cases page**
   listing all cases where `primaryCaseworkerId` is null.
6. **FR-2.2** — The list shall show case number, title, case type, sensitivity,
   creditor name, creation date, and total outstanding balance.
7. **FR-2.3** — A SUPERVISOR or ADMIN may assign any unassigned case to a caseworker
   using an inline assignment control (caseworker picker).
8. **FR-2.4** — A CASEWORKER may self-assign an unassigned case to themselves,
   provided the case sensitivity allows it per petition048 rules.
9. **FR-2.5** — The list shall support pagination (default 20 per page) and filtering
   by case type and sensitivity.

### FR-3: Single Case Assignment and Reassignment

10. **FR-3.1** — The case detail page shall display the current primary caseworker and
    any collaborating caseworkers (`assignedCaseworkerIds`).
11. **FR-3.2** — A SUPERVISOR or ADMIN may reassign the primary caseworker of any case
    via a reassignment control on the case detail page.
12. **FR-3.3** — Reassignment shall validate the target caseworker's capabilities
    against the case sensitivity level using `AssignmentGuardService`.
13. **FR-3.4** — If validation fails, the portal shall display the denial reason
    (e.g., "Caseworker lacks VIP permission") without completing the reassignment.
14. **FR-3.5** — On successful assignment or reassignment, the case's
    `primaryCaseworkerId` shall be updated and a `CASEWORKER_ASSIGNED` event recorded.

### FR-4: Bulk Assignment (Supervisor)

15. **FR-4.1** — On the unassigned cases page, a SUPERVISOR may select multiple cases
    via checkboxes and assign them to a single caseworker in one action.
16. **FR-4.2** — Bulk assignment shall validate each case individually against the
    target caseworker's capabilities. Cases that fail validation shall be skipped and
    reported to the supervisor with the denial reason.
17. **FR-4.3** — The result summary shall show: N assigned, M skipped (with reasons).

### FR-5: Assignment Audit Trail

18. **FR-5.1** — Every assignment and reassignment shall produce a `CaseEvent` of type
    `CASEWORKER_ASSIGNED` with metadata including: previous caseworker ID (if any),
    new caseworker ID, assigner ID, assignment method (MANUAL, SELF, BULK), and
    timestamp.
19. **FR-5.2** — Every denied assignment shall produce a `CaseEvent` of type
    `ASSIGNMENT_DENIED` with the denial reason code.
20. **FR-5.3** — The case detail page shall display assignment history as part of the
    case event timeline.

### FR-6: API Endpoints (Case-Service)

21. **FR-6.1** — `PUT /api/v1/cases/{id}/assign` — Assign or reassign the primary
    caseworker. Request body: `{ "targetCaseworkerId": "..." }`. Returns 200 on
    success, 403 if capability validation fails, 404 if case not found.
22. **FR-6.2** — `PUT /api/v1/cases/bulk-assign` — Bulk assign multiple cases.
    Request body: `{ "caseIds": [...], "targetCaseworkerId": "..." }`. Returns a
    result object with per-case success/failure status.
23. **FR-6.3** — `GET /api/v1/cases/unassigned` — List unassigned cases with
    pagination, sorting, and filtering. Respects RBAC rules (petition048).
24. **FR-6.4** — `GET /api/v1/caseworkers/workload` — Return workload summary for
    all active caseworkers. Accessible to SUPERVISOR and ADMIN roles only.

## Non-Functional Requirements

| NFR | Specification |
|-----|---------------|
| **Performance** | Workload dashboard must render within 2 seconds for up to 200 caseworkers |
| **Consistency** | Assignment must be atomic — a case cannot be simultaneously assigned to two caseworkers |
| **Audit** | All assignment events must be queryable for 5 years (Rigsarkivet) |
| **Accessibility** | All new UI pages must meet WCAG 2.1 AA (lang, title, labels, keyboard navigation) |

## Constraints and Assumptions

- The CaseEntity already has `primaryCaseworkerId` and `assignedCaseworkerIds` fields
  (no schema migration required for the core assignment model).
- The `AssignmentGuardService` and `CaseEvent` infrastructure from petition048 are
  available and tested.
- Caseworker identity and display names are available via Keycloak user attributes or
  the person-registry service.
- TB-023 (target-caseworker capability lookup) must be completed before FR-3.3 and
  FR-4.2 can enforce real capability checks on the target caseworker.

## Dependencies

| Dependency | Status | Impact |
|------------|--------|--------|
| Petition048 (RBAC) | **Done** | Foundation for sensitivity validation |
| TB-023 (target-caseworker lookup) | Not started | Blocks real capability validation on reassignment |
| Person-registry / Keycloak user attributes | Partial | Needed for caseworker display names on workload dashboard |

## Out of Scope

- **Automatic assignment rules** (round-robin, workload-balanced auto-distribution) —
  deferred to a future petition if demand materialises.
- **Case complexity scoring** — workload is measured by case count, not weighted
  complexity. Complexity weighting is a future enhancement.
- **Cross-team assignment** — this petition assumes a flat caseworker pool; team/unit
  structures are not modelled.
- **Notification of assignment** — caseworkers are not notified of new assignments via
  email or push. They see new cases on login. Notification is a separate concern.
- **Data Warehouse retirement** — this petition adds operational assignment capability
  but does not mandate removing DW-based reports.
