# Petition 009: Fordringshaver Master Data Service
# Outcome Contract: Dedicated backend owner for operational fordringshaver master data
# Requirement traceability: petition009-fordringshaver-master-data-service-outcome-contract.md

Feature: Creditor Service API for operational fordringshaver master data

  Background:
    Given the creditor service is available
    And organization identity data is owned by person-registry

  # GET /api/v1/creditors/{creditorOrgId}
  Scenario: Resolve creditor master data by organization reference succeeds
    Given a creditor exists with creditorOrgId "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
    And the creditor has externalCreditorId "FH-001"
    And the creditor has activityStatus "ACTIVE"
    And the creditor has connectionType "SYSTEM"
    When a service requests the creditor by creditorOrgId "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
    Then the creditor is returned successfully
    And the response contains creditorOrgId "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
    And the response contains externalCreditorId "FH-001"
    And the response contains activityStatus "ACTIVE"

  Scenario: Resolve creditor by organization reference returns 404 when not found
    Given no creditor exists with creditorOrgId "00000000-0000-0000-0000-000000000000"
    When a service requests the creditor by creditorOrgId "00000000-0000-0000-0000-000000000000"
    Then the request fails with status 404
    And the error response contains code "CREDITOR_NOT_FOUND"

  # GET /api/v1/creditors/by-external-id/{externalCreditorId}
  Scenario: Resolve creditor by legacy external identifier succeeds
    Given a creditor exists with externalCreditorId "LEGACY-FH-42"
    And the creditor has creditorOrgId "b2c3d4e5-f6a7-8901-bcde-f12345678901"
    And the creditor has activityStatus "ACTIVE"
    When a service requests the creditor by externalCreditorId "LEGACY-FH-42"
    Then the creditor is returned successfully
    And the response contains externalCreditorId "LEGACY-FH-42"
    And the response contains creditorOrgId "b2c3d4e5-f6a7-8901-bcde-f12345678901"

  Scenario: Resolve creditor by external identifier returns 404 when not found
    Given no creditor exists with externalCreditorId "NONEXISTENT-FH"
    When a service requests the creditor by externalCreditorId "NONEXISTENT-FH"
    Then the request fails with status 404
    And the error response contains code "CREDITOR_NOT_FOUND"

  # POST /api/v1/creditors/{creditorOrgId}/validate-action
  Scenario: Validate action succeeds for active creditor with permission
    Given a creditor exists with creditorOrgId "c3d4e5f6-a7b8-9012-cdef-123456789012"
    And the creditor has activityStatus "ACTIVE"
    And the creditor has permission to perform action "CREATE_CLAIM"
    When a service validates action "CREATE_CLAIM" for creditorOrgId "c3d4e5f6-a7b8-9012-cdef-123456789012"
    Then the validation response indicates allowed is true
    And the validation response contains requestedAction "CREATE_CLAIM"

  Scenario: Validate action fails for inactive creditor
    Given a creditor exists with creditorOrgId "d4e5f6a7-b8c9-0123-def1-234567890123"
    And the creditor has activityStatus "TEMPORARILY_CLOSED"
    When a service validates action "CREATE_CLAIM" for creditorOrgId "d4e5f6a7-b8c9-0123-def1-234567890123"
    Then the validation response indicates allowed is false
    And the validation response contains reasonCode "CREDITOR_INACTIVE"
    And the validation response contains message explaining the creditor is not active

  Scenario: Validate action fails when creditor lacks permission
    Given a creditor exists with creditorOrgId "e5f6a7b8-c9d0-1234-ef12-345678901234"
    And the creditor has activityStatus "ACTIVE"
    And the creditor does not have permission to perform action "ADMINISTER_CREDITOR"
    When a service validates action "ADMINISTER_CREDITOR" for creditorOrgId "e5f6a7b8-c9d0-1234-ef12-345678901234"
    Then the validation response indicates allowed is false
    And the validation response contains reasonCode "PERMISSION_DENIED"

  Scenario: Validate action returns 404 for non-existent creditor
    Given no creditor exists with creditorOrgId "00000000-0000-0000-0000-000000000000"
    When a service validates action "CREATE_CLAIM" for creditorOrgId "00000000-0000-0000-0000-000000000000"
    Then the request fails with status 404
    And the error response contains code "CREDITOR_NOT_FOUND"

  # Acceptance criterion: Parent-child creditor relationships are representable
  Scenario: Creditor has parent-child relationship
    Given a parent creditor exists with creditorOrgId "f6a7b8c9-d0e1-2345-f123-456789012345"
    And a child creditor exists with creditorOrgId "a7b8c9d0-e1f2-3456-1234-567890123456"
    And the child creditor has parentCreditorId "f6a7b8c9-d0e1-2345-f123-456789012345"
    When a service requests the creditor by creditorOrgId "a7b8c9d0-e1f2-3456-1234-567890123456"
    Then the creditor is returned successfully
    And the response contains parentCreditorId "f6a7b8c9-d0e1-2345-f123-456789012345"

  # Acceptance criterion: Organization PII is NOT duplicated
  Scenario: Creditor record contains only technical UUID reference to organization
    Given a creditor exists with creditorOrgId "b8c9d0e1-f2a3-4567-2345-678901234567"
    And the person-registry organization has CVR "12345678" for creditorOrgId "b8c9d0e1-f2a3-4567-2345-678901234567"
    When a service requests the creditor by creditorOrgId "b8c9d0e1-f2a3-4567-2345-678901234567"
    Then the creditor is returned successfully
    And the response contains creditorOrgId "b8c9d0e1-f2a3-4567-2345-678901234567"
    And the response does not contain CVR number directly
    And the response does not contain organization name directly
    And the response does not contain organization address directly

  # Acceptance criterion: API-based dependency, no direct database access
  Scenario: Services access creditor master data through API
    Given the debt-service needs to validate a creditor before creating a claim
    And the creditor with creditorOrgId "c9d0e1f2-a3b4-5678-3456-789012345678" exists
    When the debt-service calls the creditor-service API to validate the creditor
    Then the creditor-service resolves the creditor from its own database
    And the debt-service does not access the creditor database directly
    And the validation result is returned through the API

  # Acceptance criterion: Audit logging and temporal history support
  Scenario: Creditor changes are audit logged
    Given a creditor exists with creditorOrgId "d0e1f2a3-b4c5-6789-4567-890123456789"
    And the creditor has activityStatus "ACTIVE"
    When an administrator updates the creditor activityStatus to "TEMPORARILY_CLOSED"
    Then the change is recorded in the audit log
    And the audit log contains the timestamp of the change
    And the audit log contains the user who made the change
    And the audit log contains the previous activityStatus value
    And the audit log contains the new activityStatus value
