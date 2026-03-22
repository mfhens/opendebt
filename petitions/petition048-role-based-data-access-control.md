# Petition 048: Role-Based Data Access Control (RBAC) for Cases, Claims, and Notifications

## Summary

OpenDebt shall enforce granular role-based access control (RBAC) to ensure that users can only view and act upon data they have authorization to access. This petition specifies authorization rules for the five primary user roles:

- **Caseworker** (CASEWORKER): Internal debt collection staff member
- **Supervisor** (SUPERVISOR): Team lead with approval authority and case oversight
- **Creditor** (CREDITOR): External organization submitting claims (fordringer)
- **Citizen** (CITIZEN): Denmark resident viewing their own debts and case status
- **Administrator** (ADMIN): System administrator with full access

Access control shall be enforced at three layers: HTTP request level (Spring Security), service/repository level (business logic validation), and database query level (filtering). All authorization shall comply with ADR-0014 (GDPR data isolation) and ADR-0007 (no cross-service database access).

## Context and Motivation

### Current State

The OpenDebt platform currently implements basic role checking via `@PreAuthorize` annotations on REST controllers (see SecurityConfig, CaseController, DebtController). However, the current implementation has gaps:

1. **Incomplete caseworker case filtering** — The `listCases()` endpoint accepts an optional `caseworkerId` parameter but does not enforce that non-supervisors can only query their own assigned cases.
2. **Missing creditor data isolation** — The creditor-service resolves acting creditor but portals do not yet enforce that creditors see only their own claims (fordringer).
3. **No citizen case filtering** — The citizen-portal does not yet filter cases by the authenticated citizen's person_id.
4. **No VIP/PEP sensitivity handling** — There is no mechanism to restrict cases marked with elevated sensitivity (VIP, PEP) to authorized personnel only.
5. **Supervisor case visibility** — While supervisors should oversee all cases in principle, there is no explicit rule defining supervisor access.

### Regulatory and Business Drivers

- **GDPR (ADR-0014)**: Personal data (CPR, names, addresses) is isolated in Person Registry; however, **case metadata like case status, assigned caseworker, and collection strategy contains quasi-sensitive information** about debt collection activities that must be access-controlled.
- **Fællesoffentlige Architecture Principles**: Principle 6 (Privacy by design) requires that data access be minimized and role-role.
- **Danish Public Debt Collection Practice**: Case assignment and caseworker confidentiality are standard operational concerns in public collection agencies (e.g., KKIK, kommuner).
- **Creditor Transparency**: Commercial creditors must not see cases or collection activities for claims not belonging to them.

## Functional Requirements

### 1. CASEWORKER Role Requirements

#### Rule 1.1: Caseworker Case List Visibility
When a CASEWORKER user requests the case list (`GET /api/v1/cases`), the system shall return **only cases assigned to that caseworker's ID**, unless the request includes a `caseworkerId` query parameter **and the requesting user has SUPERVISOR or ADMIN role**.

- **Verification**: In production (non-dev profile), if the authenticated user holds only CASEWORKER role and requests `GET /api/v1/cases?caseworkerId=other-caseworker-id`, the system shall return HTTP 403 Forbidden.
- **Dev/Demo exception**: In `dev` or `local` profiles, `@PreAuthorize` is disabled and all cases are visible (for demo/testing purposes).

#### Rule 1.2: Caseworker Case Detail Access
When a CASEWORKER requests a specific case (`GET /api/v1/cases/{id}`), the system shall verify that the case's `primaryCaseworkerId` matches the authenticated user's ID or that the case has the requesting user in its list of assignedCaseworkerIds. If not, return HTTP 403 Forbidden.

**Exception**: A CASEWORKER may view a case if:
- They are the primary assigned caseworker, OR
- They are in the case's `assignedCaseworkerIds` list (for collaborative case handling), OR
- A SUPERVISOR or ADMIN explicitly grants them access via case delegation.

#### Rule 1.3: Caseworker Debt Detail Access
When a CASEWORKER requests debt details (`GET /api/v1/debts/{id}`), the system shall verify that the debt is linked to a case assigned to the caseworker (via case assignment). If the debt exists but is not linked to an accessible case, return HTTP 403 Forbidden.

#### Rule 1.4: Caseworker Cannot Reassign
A CASEWORKER cannot assign a case to another caseworker (`POST /api/v1/cases/{id}/assign`). This operation requires SUPERVISOR or ADMIN role.

### 2. SUPERVISOR Role Requirements

#### Rule 2.1: Supervisor Case List Visibility
When a SUPERVISOR requests the case list (`GET /api/v1/cases`), the system shall return **all cases** (no filtering by assigned caseworker). The supervisor may optionally filter by status, assigned caseworker, or other criteria to support oversight and reassignment workflows.

#### Rule 2.2: Supervisor Case Detail Access
A SUPERVISOR may view **all cases** without restriction.

#### Rule 2.3: Supervisor Case Assignment Authority
A SUPERVISOR may assign a case to a caseworker (`POST /api/v1/cases/{id}/assign`) and may reassign cases between caseworkers. The system shall log such assignments for audit purposes.

#### Rule 2.4: Supervisor Readiness Approval
A SUPERVISOR may approve or reject debt readiness validation (`POST /api/v1/debts/{id}/approve-readiness`, `POST /api/v1/debts/{id}/reject-readiness`). A CASEWORKER may only **request** readiness validation; approval is reserved for supervisors.

#### Rule 2.5: Supervisor Escalation and Strategy Review
A SUPERVISOR is the assigned actor for escalation and strategy review tasks in the debt collection workflow (BPMN process `escalateStrategy` task with `flowable:candidateGroups="supervisors"`).

### 3. CREDITOR (Fordringshaver) Role Requirements

#### Rule 3.1: Creditor Ford ringshaver-Scoped Claims View
When a CREDITOR requests the claims list (`GET /api/v1/claims` or `/api/v1/creditors/claims`), the system shall return **only claims (fordringer) created by or assigned to that creditor organization** (identified by the authenticated user's `organization` claim, typically a CVR).

- **Verification**: If creditor A tries to view claims submitted by creditor B, the system shall return HTTP 403 Forbidden or an empty list (depending on API design).
- **Creditor Agreement Scope**: The returned claims must also respect the creditor agreement (fordringhaveraftale) permissions; claims subject to agreement-level authorization rules (petition016) must be filtered accordingly.

#### Rule 3.2: Creditor Individual Claim Detail Access
When a CREDITOR requests a specific claim (`GET /api/v1/claims/{id}`), the system shall verify that the claim belongs to the creditor's organization. If not, return HTTP 403 Forbidden.

#### Rule 3.3: Creditor Cannot View Internal Case Details
A CREDITOR cannot directly access internal case details (`GET /api/v1/cases`, `GET /api/v1/cases/{id}`). Case management is internal to OpenDebt. Creditors may be notified of case status changes via creditor notifications (underretningsmeddelelser), but they do not see the case object itself.

#### Rule 3.4: Creditor Portal Organization Switching
If a CREDITOR is authorized to act on behalf of multiple organizations (via the creditor agreement), the creditor-portal shall allow the user to switch the acting organization, and all subsequent API calls shall use the switched organization's context. The system shall verify that the user is authorized for the acting organization before allowing the switch.

### 4. CITIZEN (Skyldner) Role Requirements

#### Rule 4.1: Citizen Debt List Visibility
When a CITIZEN requests the debt list (`GET /api/v1/citizen/debts`), the system shall return **only debts where the authenticated citizen's `person_id` (from JWT) matches the debtor_person_id**. No debts from other citizens shall be visible.

- **Verification**: If citizen with person_id `uuid-1111` tries to view debts for person_id `uuid-2222`, the system shall return HTTP 403 Forbidden or an empty list.

#### Rule 4.2: Citizen Power of Attorney Delegation
A citizen may grant power of attorney to another party (e.g., a social worker, family member, lawyer). If a citizen has been granted power of attorney by debtor D, and the POA includes the right to view cases, that citizen may view debtor D's cases and debts.

- **Implementation**: The Person Registry (or a dedicated POA service) maintains a registry of power-of-attorney relationships. When a citizen requests debts, the system shall query the POA registry to determine if the requesting user has authority over the debtor's data.
- **Future enhancement**: Not required in MVP; defer to next iteration (petition032).

#### Rule 4.3: Citizen Cannot View Internal Case State
A CITIZEN views a **simplified case summary** that excludes internal case fields (caseworker assignment, collection strategy, internal notes). The citizen sees their debts, payment deadlines, and current balance, but not which caseworker is handling the case.

#### Rule 4.4: Citizen Cannot Access Other Portals
A CITIZEN cannot access the caseworker-portal or creditor-portal. Portal-level OAuth2 client registration and scopes enforce this separation. If a citizen attempts to access the caseworker-portal with a CITIZEN token, the portal redirect-uri mismatch or token validation failure shall result in an error.

### 5. VIP / PEP (High-Sensitivity) Cases

#### Rule 5.1: VIP/PEP Case Sensitivity Classification
Cases may be marked with a sensitivity level indicating elevated complexity or public interest:

- **NORMAL** (default): Standard case, visible to assigned caseworker and supervisors.
- **VIP**: Involves a public figure or politically exposed person. Restricted to caseworkers with explicit `HANDLE_VIP_CASES` permission and supervisors.
- **PEP**: Politically Exposed Person (per AML/CFT regulations). Restricted to caseworkers with explicit `HANDLE_PEP_CASES` permission and supervisors.
- **CONFIDENTIAL**: Legal escalations or settlement negotiations. Restricted to supervisors and admins only.

#### Rule 5.2: VIP/PEP Case Assignment Restriction
A SUPERVISOR cannot assign a VIP or PEP case to a CASEWORKER unless the caseworker's user record includes the `HANDLE_VIP_CASES` or `HANDLE_PEP_CASES` capability flag respectively. The system shall prevent assignment by returning HTTP 403 Forbidden with error code `CASEWORKER_LACKS_VIP_PERMISSION`.

#### Rule 5.3: VIP/PEP Case List Filtering
When listing cases, non-supervisors shall **not see VIP/PEP cases in the list** unless they have explicit permission. A caseworker with `HANDLE_VIP_CASES` permission shall see only the VIP cases they are assigned to; all others shall be filtered from the list.

#### Rule 5.4: CONFIDENTIAL Cases (Supervisor-Only)
Cases marked as CONFIDENTIAL are visible **only to SUPERVISORS and ADMINS**. No caseworker, regardless of permissions, may view or handle a CONFIDENTIAL case.

### 6. ADMIN Role Requirements

#### Rule 6.1: Administrator Unrestricted Access
An ADMIN role shall have unrestricted read and write access to all resources (cases, debts, claims, creditor records) across the system. This includes access to all VIP/PEP/CONFIDENTIAL cases.

#### Rule 6.2: Administrator Audit Logging
All administrative actions (case modification, claim approval overrides, user permission changes) shall be logged with the admin's user ID, timestamp, and action details for audit and compliance purposes.

### 7. Cross-Service Authorization (ADR-0007 Compliance)

#### Rule 7.1: No Service-to-Service Data Leakage
Each microservice (case-service, debt-service, creditor-service, payment-service) shall enforce authorization at its own API boundary. Services shall not assume that a request has been pre-filtered by an upstream service.

- **Example**: The debt-service shall verify that the requesting JWT's person_id matches the debtor_person_id of the debt being queried, even if the case-service has already filtered cases for that citizen.

#### Rule 7.2: Inter-Service Client Filtering
When one service calls another (e.g., case-service calling debt-service), the calling service shall pass the authenticated user's context (via JWT or service-to-service propagation) so the called service can re-validate authorization.

## Outcome Contract

### Acceptance Criteria

**As-Is State (Before Implementation)**
- Cases are returned without filtering for caseworker assignment (Rule 1.1 not enforced).
- Creditors can theoretically view claims from other creditors (if authorization rules.parameters are missing).
- Citizens have no portal endpoint to view their debts filtered by person_id.
- No sensitivity classification or VIP/PEP handling exists.

**To-Be State (After Implementation)**
The system shall meet **all** of the following criteria for the petition to be considered complete:

1. **Caseworker Case Filtering** 
   - A caseworker requesting case list without SUPERVISOR role sees only their assigned cases.
   - Test: `GET /api/v1/cases` returns status 200 with 3 cases assigned to authenticated caseworker; same request as different caseworker returns 2 different cases.

2. **Caseworker Cannot Reassign**
   - A caseworker attempting `POST /api/v1/cases/{id}/assign` receives HTTP 403 Forbidden.
   - Supervisors and admins can reassign.

3. **Creditor Organization Scoping**
   - A creditor organization requests `GET /api/v1/creditors/claims` and receives only claims created by that organization.
   - Test: Creditor A's request returns 5 claims; Creditor B's request returns 3 different claims.

4. **Citizen Debt Filtering**
   - A citizen calls `GET /api/v1/citizen/debts` and receives only debts matching their person_id.
   - Test: Citizen with person_id `uuid-aaa` sees 4 debts; citizen with person_id `uuid-bbb` sees 6 different debts.

5. **VIP/PEP Case Restriction**
   - A case marked as VIP is not assigned to caseworkers without `HANDLE_VIP_CASES` flag.
   - When a caseworker without the flag lists cases, VIP cases are excluded from the response.
   - Test: Case list for unqualified caseworker excludes 1 VIP case; case list for qualified caseworker includes it.

6. **Supervisor Full Visibility**
   - A supervisor's `GET /api/v1/cases` response includes all cases (no caseworker filtering).
   - A supervisor can view and reassign VIP/PEP cases.

7. **Admin Unrestricted Access**
   - An admin can view all cases, all claims, and all citizen debts without restriction.

8. **Audit Logging**
   - All case assignments or reassignments are logged with admin user ID and timestamp.
   - Test: Check audit table for assignment records; verify correlation with case update audit trail.

9. **Inter-Service Verification**
   - When case-service calls debt-service on behalf of a citizen, debt-service re-validates the person_id independently.
   - Test: A debt belonging to citizen A is protected from viewing by citizen B even if case-service allows it.

### Definition of Done

- [ ] All authorization rules (Rules 1.1–7.2) are implemented and tested.
- [ ] Unit tests exist for each rule with positive and negative cases.
- [ ] Integration tests verify end-to-end filtering across services.
- [ ] Audit logs capture case assignments and administrative actions.
- [ ] Code review confirms ADR-0014 (GDPR isolation) and ADR-0007 (no cross-service DB access) compliance.
- [ ] Security and privacy team review completed.
- [ ] Documentation updated (architecture overview, API specs, ADRs if needed).
- [ ] Performance tests confirm authorization checks do not degrade query performance.

## Implementation Notes

### Reference Architecture

Authorization enforcement shall follow this layered approach:

```
┌─────────────────────────────────────────────────────┐
│ HTTP Request Layer (Spring Security Filters)       │
│ - Role-based method security (@PreAuthorize)       │
│ - OAuth2 JWT validation                            │
└──────────────────┬──────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────┐
│ Service Layer (Business Logic)                     │
│ - CaseService: Filter by assigned caseworker      │
│ - CreditorService: Filter by organization         │
│ - DebtService: Filter by person_id (citizen)      │
│ - VIP/PEP capability checks                       │
└──────────────────┬──────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────┐
│ Repository Layer (Database Queries)                │
│ - JPA/QueryDSL predicates with user context        │
│ - Case.assignedCaseworkerIds filtering             │
│ - Claim.creditorOrgId filtering                    │
│ - Debt.debtorPersonId filtering                    │
└─────────────────────────────────────────────────────┘
```

### Key Implementation Components

1. **AuthContext Bean** (`DtoAuthContext`)
   - Extracted from JWT claims: `userId`, `organizationId`, `personId`, `roles`, `capabilities` (for VIP/PEP flags)
   - Injected into services for authorization logic

2. **CaseAccessChecker** Service
   - Method: `boolean canAccessCase(UUID caseId, AuthContext requester)`
   - Implements Rules 1.1–1.3, 2.1–2.2, 5.4

3. **CreditorAccessChecker** Service
   - Method: `boolean canAccessCreditorClaim(UUID claimId, UUID creditorOrgId)`
   - Implements Rule 3.1–3.4

4. **CitizenDebtAccessChecker** Service
   - Method: `boolean canAccessDebt(UUID debtId, UUID personId)`
   - Implements Rule 4.1

5. **SensitivityClassification Entity** (on Case)
   - Enum: `{NORMAL, VIP, PEP, CONFIDENTIAL}`
   - Optional `allowedCapabilities: Set<String>` for granular role binding

6. **AuditLog Entity**
   - Fields: `userId, action, resourceId, resourceType, timestamp, beforeState, afterState`
   - Captures all case assignments, reassignments, and administrative changes

### Testing Strategy

- **Unit Tests**: Validate each rule in isolation (mock AccessChecker beans).
- **Integration Tests**: Use TestContainers for postgres; create test fixtures (cases, claims, debts with different assignments); verify filtering.
- **Security Tests** (ArchUnit): Ensure `@PreAuthorize` is present on all public endpoints.
- **E2E Tests**: Run against demo profile with seeded Keycloak users; verify caseworker, supervisor, creditor, and citizen see appropriate data.

### Performance Considerations

- **Index Strategy**: Add composite indexes on (caseId, assignedCaseworkerId), (claimId, creditorOrgId), (debtId, debtorPersonId) to support fast filtering.
- **Caching**: Consider caching user capabilities (VIP/PEP flags) at AuthContext level with TTL to avoid repeated DB lookups.
- **Query Optimization**: Use JPA `@Query` with custom WHERE predicates rather than in-memory filtering.

## References

### ADRs
- **ADR-0005**: Keycloak Authentication — Defines OAuth2 token structure and role claims.
- **ADR-0007**: No Cross-Service Database Connections — Services must validate authorization independently.
- **ADR-0014**: GDPR Data Isolation — Person Registry holds PII; other services reference by UUID only.
- **ADR-0024**: Observability (Distributed Tracing) — Ensure authorization checks are observable.

### Related Petitions
- **Petition 016**: Fordring Claimant Authorization Rules — Specifies permission checks for creditor actions on claims.
- **Petition 010**: Creditor Access Resolution — Defines how acting creditor is resolved from channel requests.
- **Petition 021**: Case Assignment and Caseworker Workflow — Defines case lifecycle and assignment events.

### Existing Code References
- `opendebt-case-service/src/main/java/dk/ufst/opendebt/caseservice/controller/CaseController.java` — Current @PreAuthorize stubs.
- `opendebt-case-service/src/main/resources/processes/debt-collection-case.bpmn20.xml` — Workflow task assignments (flowable:assignee, flowable:candidateGroups).
- `config/keycloak/opendebt-realm.json` — Role definitions and client configurations.
- `docs/begrebsmodel/Inddrivelse-begrebsmodel-UFST-v3.md` — Section 2.1 domain terminology mapping (Caseworker → Sagsbehandler, Supervisor → Supervisor, Citizen → Skyldner, Creditor → Fordringshaver).

### Domain Model
- **Skyldner (Debtor / Citizen)**: Danish resident owing a debt; identified by CPR (encrypted in Person Registry), referenced by `person_id` (UUID) in other services.
- **Fordringshaver (Creditor)**: Public or commercial organization owed money; identified by CVR (encrypted in Person Registry), referenced by `organization_id` (UUID) or CVR in claims and creditor records.
- **Sagsbehandler (Caseworker)**: Internal staff member assigned responsibility for a collection case.
- **Supervisor (Supervisor)**: Team lead with authority to assign cases, approve readiness, and escalate strategy.
- **Sag (Case)**: Collection case grouping one or more fordringer (claims) for a single debtor.
- **Fordring (Claim)**: Formal monetary demand submitted by a creditor.

## Changelog

| Version | Date | Change |
|---|---|---|
| 1.0 | 2026-03-22 | Initial petition with rules for caseworker, supervisor, creditor, and citizen access control; VIP/PEP case sensitivity. |
