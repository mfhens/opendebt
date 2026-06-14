@petition066
Feature: Petition066 attachment workflow seams in debt-service
  As the petition066 test pack
  I want debt-service seam coverage for attachment workflow orchestration
  So that the approved requirements are executable as failing coverage before runtime implementation exists

  Scenario: Debt-service lacks a dedicated attachment workflow command surface
    Given petition066 debt-service internal API is expected at "/api/internal/v1/debtors/{debtorId}/attachment-workflows"
    When the petition066 debt-service executable seam inventory is inspected
    Then petition066 debt-service coverage is marked pending implementation
    And the missing debt-service surface list contains "POST /api/internal/v1/debtors/{debtorId}/attachment-workflows"
    And the missing debt-service surface list contains "POST /api/internal/v1/debtors/{debtorId}/attachment-workflows/{workflowId}/dispatch"
    And the missing debt-service surface list contains "POST /api/internal/v1/debtors/{debtorId}/attachment-workflows/{workflowId}/withdraw"
    And the missing debt-service surface list contains "POST /api/internal/v1/debtors/{debtorId}/attachment-workflows/callbacks"
    And the missing debt-service surface list contains "GET /api/internal/v1/debtors/{debtorId}/attachment-workflows"
    And the missing debt-service surface list contains "GET /api/internal/v1/debtors/{debtorId}/attachment-workflows/{workflowId}"

  Scenario: Debt-service lacks a dedicated attachment workflow domain package
    When the petition066 debt-service executable seam inventory is inspected
    Then petition066 debt-service coverage is marked pending implementation
    And the missing debt-service package list contains "dk.ufst.opendebt.debtservice.attachment"
    And the missing debt-service package list contains "AttachmentWorkflowApi"
    And the missing debt-service package list contains "AttachmentWorkflowApplicationService"
    And the missing debt-service package list contains "AttachmentCallbackValidator"
    And the missing debt-service package list contains "AttachmentInterruptionBridge"

  Scenario: Petition066 requirements remain bound to executable debt-service coverage targets
    Given petition066 feature and validation contract have been reopened for this run
    When the petition066 debt-service executable seam inventory is inspected
    Then petition066 debt-service trace summary mentions "AC-01"
    And petition066 debt-service trace summary mentions "AC-14"
    And petition066 debt-service trace summary mentions "type=UDLAEG"
    And petition066 debt-service trace summary mentions "workflowReference"
