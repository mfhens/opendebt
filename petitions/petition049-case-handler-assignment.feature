Feature: Case handler assignment within the operational caseworker portal

  # --- Workload Dashboard ---

  Scenario: Supervisor views workload overview with case counts per caseworker
    Given 3 active caseworkers: Anna with 12 open cases, Bo with 7 open cases, Clara with 0 open cases
    And the authenticated user has the SUPERVISOR role
    When the supervisor navigates to the workload overview page
    Then all 3 caseworkers are listed
    And Anna's row shows 12 open cases
    And Bo's row shows 7 open cases
    And Clara's row shows 0 open cases

  Scenario: Workload overview breaks down cases by sensitivity level
    Given caseworker Anna has 10 NORMAL cases, 1 VIP case, and 1 PEP case
    And the authenticated user has the SUPERVISOR role
    When the supervisor views the workload overview
    Then Anna's row shows NORMAL=10, VIP=1, PEP=1, CONFIDENTIAL=0

  Scenario: Caseworker role is denied access to the workload dashboard
    Given the authenticated user has only the CASEWORKER role
    When the user requests the workload overview page
    Then the system returns HTTP 403 Forbidden

  Scenario: Supervisor navigates from workload overview to a caseworker's case list
    Given the supervisor is on the workload overview page
    When the supervisor clicks caseworker Bo's row
    Then the case list is displayed filtered to show only Bo's assigned cases

  # --- Unassigned Cases Queue ---

  Scenario: Unassigned cases are listed for supervisor
    Given 5 cases exist with no primaryCaseworkerId and 10 cases are assigned
    And the authenticated user has the SUPERVISOR role
    When the supervisor navigates to the unassigned cases page
    Then exactly 5 cases are listed
    And each row shows case number, title, case type, sensitivity, creditor, creation date, and outstanding balance

  Scenario: Supervisor assigns an unassigned case to a caseworker
    Given case SAG-2026-0042 is unassigned with sensitivity NORMAL
    And caseworker Bo has the required capabilities for NORMAL cases
    And the authenticated user has the SUPERVISOR role
    When the supervisor selects Bo from the assignment picker on SAG-2026-0042
    Then SAG-2026-0042 is assigned to Bo as primary caseworker
    And a CASEWORKER_ASSIGNED event is recorded with method MANUAL and assignerId set to the supervisor
    And SAG-2026-0042 no longer appears in the unassigned queue

  Scenario: Caseworker self-assigns an unassigned NORMAL case
    Given case SAG-2026-0055 is unassigned with sensitivity NORMAL
    And the authenticated user is caseworker Clara
    When Clara clicks the self-assign action on SAG-2026-0055
    Then SAG-2026-0055 is assigned to Clara as primary caseworker
    And a CASEWORKER_ASSIGNED event is recorded with method SELF

  Scenario: Caseworker is denied self-assignment of a VIP case without capability
    Given case SAG-2026-0060 is unassigned with sensitivity VIP
    And caseworker Clara does not have the HANDLE_VIP_CASES capability
    When Clara attempts to self-assign SAG-2026-0060
    Then the assignment is rejected with reason CASEWORKER_LACKS_VIP_PERMISSION
    And an ASSIGNMENT_DENIED event is recorded
    And SAG-2026-0060 remains unassigned

  Scenario: Caseworker is denied self-assignment of a CONFIDENTIAL case
    Given case SAG-2026-0065 is unassigned with sensitivity CONFIDENTIAL
    And the authenticated user is caseworker Bo
    When Bo attempts to self-assign SAG-2026-0065
    Then the assignment is rejected with reason CONFIDENTIAL_CASE_RESTRICTED
    And SAG-2026-0065 remains unassigned

  Scenario: Unassigned cases queue supports pagination
    Given 45 unassigned cases exist
    And the default page size is 20
    When the supervisor opens the unassigned cases page
    Then 20 cases are displayed on page 1
    And pagination controls indicate 3 total pages

  # --- Single Case Reassignment ---

  Scenario: Supervisor reassigns a case from one caseworker to another
    Given case SAG-2026-0042 is assigned to caseworker Anna as primary
    And the authenticated user has the SUPERVISOR role
    When the supervisor reassigns SAG-2026-0042 to caseworker Bo
    Then SAG-2026-0042's primaryCaseworkerId changes from Anna to Bo
    And a CASEWORKER_ASSIGNED event is recorded with previousCaseworkerId Anna and method MANUAL

  Scenario: Reassignment of a CONFIDENTIAL case is rejected
    Given case SAG-2026-0070 has sensitivity CONFIDENTIAL
    And the authenticated user has the SUPERVISOR role
    When the supervisor attempts to assign SAG-2026-0070 to caseworker Bo
    Then the assignment is rejected with reason CONFIDENTIAL_CASE_RESTRICTED
    And the portal displays the denial reason to the supervisor
    And the case assignment remains unchanged

  Scenario: Case detail page shows current assignment
    Given case SAG-2026-0042 is assigned to Anna as primary and Bo as collaborator
    When a SUPERVISOR views the case detail page for SAG-2026-0042
    Then the page displays Anna as primary caseworker and Bo as collaborating caseworker

  # --- Bulk Assignment ---

  Scenario: Supervisor bulk-assigns multiple unassigned cases
    Given 5 unassigned cases with sensitivity NORMAL
    And the authenticated user has the SUPERVISOR role
    When the supervisor selects all 5 cases and assigns them to caseworker Bo
    Then all 5 cases are assigned to Bo
    And 5 CASEWORKER_ASSIGNED events are recorded with method BULK
    And the result summary shows 5 assigned and 0 rejected

  Scenario: Bulk assignment with mixed validation results
    Given 3 unassigned NORMAL cases and 1 unassigned VIP case
    And caseworker Clara does not have the HANDLE_VIP_CASES capability
    When the supervisor selects all 4 cases and assigns them to Clara
    Then 3 NORMAL cases are assigned to Clara
    And the VIP case is not assigned
    And the result summary shows 3 assigned and 1 rejected with reason CASEWORKER_LACKS_VIP_PERMISSION

  # --- Audit Trail ---

  Scenario: Assignment history is visible on case detail timeline
    Given case SAG-2026-0042 was assigned to Anna on 2026-03-20 by supervisor Erik
    And SAG-2026-0042 was reassigned to Bo on 2026-03-22 by supervisor Erik
    When a user views the case event timeline for SAG-2026-0042
    Then both CASEWORKER_ASSIGNED events are visible
    And each event shows the timestamp, assigner, previous and new caseworker, and method

  Scenario: Denied assignment attempt is recorded in audit trail
    Given a supervisor attempts to assign a VIP case to caseworker Clara without VIP capability
    When the assignment is denied
    Then an ASSIGNMENT_DENIED event is persisted with the case ID, target caseworker ID, reason code, supervisor ID, and timestamp

  # --- API Authorization ---

  Scenario: Caseworker role cannot call the workload API
    Given the authenticated user has only the CASEWORKER role
    When GET /api/v1/caseworkers/workload is called
    Then the response is HTTP 403 Forbidden

  Scenario: Caseworker role cannot call the bulk-assign API
    Given the authenticated user has only the CASEWORKER role
    When PUT /api/v1/cases/bulk-assign is called
    Then the response is HTTP 403 Forbidden

  # --- Concurrency ---

  Scenario: Concurrent assignment of the same case results in exactly one assignment
    Given case SAG-2026-0042 is unassigned
    And supervisor Erik and supervisor Freja both attempt to assign it at the same time
    When both assignment requests reach the server concurrently
    Then exactly one assignment succeeds
    And the other receives an optimistic locking conflict error
    And exactly one CASEWORKER_ASSIGNED event is recorded
