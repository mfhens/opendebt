@petition066
Feature: Petition066 fogedret gateway seams in integration-gateway
  As the petition066 test pack
  I want integration-gateway seam coverage for callback ingress and replay protection
  So that the approved requirements are executable as failing coverage before runtime implementation exists

  Scenario: Integration-gateway lacks a fogedret callback ingress surface
    Given petition066 gateway callback API is expected at "/api/external/v1/fogedret/attachment-callbacks"
    When the petition066 gateway executable seam inventory is inspected
    Then petition066 gateway coverage is marked pending implementation
    And the missing gateway surface list contains "POST /api/external/v1/fogedret/attachment-callbacks"
    And the missing gateway surface list contains "POST /api/external/v1/fogedret/attachment-dispatch"

  Scenario: Integration-gateway lacks callback replay and mTLS handling seams
    When the petition066 gateway executable seam inventory is inspected
    Then petition066 gateway coverage is marked pending implementation
    And the missing gateway package list contains "dk.ufst.opendebt.gateway.fogedret"
    And the missing gateway package list contains "FogedretCallbackController"
    And the missing gateway package list contains "FogedretReplayGuard"
    And the missing gateway package list contains "AttachmentGatewayClient"

  Scenario: Petition066 gateway requirements remain bound to executable coverage targets
    Given petition066 feature and validation contract have been reopened for this run in gateway coverage
    When the petition066 gateway executable seam inventory is inspected
    Then petition066 gateway trace summary mentions "AC-15"
    And petition066 gateway trace summary mentions "AC-17"
    And petition066 gateway trace summary mentions "OCES3 mTLS"
    And petition066 gateway trace summary mentions "workflowReference"
