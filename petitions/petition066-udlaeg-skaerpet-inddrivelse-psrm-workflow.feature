@petition066
Feature: PSRM-side attachment workflow (G.A.3.2)
  As a caseworker
  I want debt-service to own and orchestrate attachment workflows against fogedret
  So that court outcomes and prescription interruption are legally consistent and auditable

  # AC-01
  Scenario: Create workflow with eligible covered claims
    Given debtor "P-66001" has eligible covered claims "F-66001,F-66002"
    When the caseworker creates an attachment workflow for debtor "P-66001" with covered claims "F-66001,F-66002"
    Then a new workflow is stored in the attachment_workflow aggregate
    And workflow status is "REQUESTED"
    And the workflow has a unique workflowReference

  # AC-02
  Scenario: Reject creation when any covered claim is ineligible
    Given debtor "P-66002" has covered claims "F-66003,F-66004"
    And claim "F-66004" is ineligible for attachment workflow
    When the caseworker creates an attachment workflow for debtor "P-66002" with covered claims "F-66003,F-66004"
    Then the request is rejected
    And no workflow is created
    And the response includes ineligibility reason for "F-66004"

  # AC-03
  Scenario: Accepted dispatch moves workflow to IN_COURT_PROCESS
    Given workflow "AW-66003" for debtor "P-66003" is in status "REQUESTED"
    When dispatch is accepted for workflow "AW-66003"
    Then workflow "AW-66003" status becomes "IN_COURT_PROCESS"
    And dispatch metadata is stored
    And dispatch metadata includes workflowReference "WR-66003"
    And dispatch metadata includes external case number "FG-2026-66003" when supplied by the court channel

  # AC-04
  Scenario: Repeated dispatch command is idempotent
    Given workflow "AW-66004" for debtor "P-66004" is already dispatched
    When dispatch is requested again for workflow "AW-66004"
    Then no new outbound dispatch command is issued
    And existing dispatch metadata is returned

  # AC-05
  Scenario: Covered claim scope is immutable after REQUESTED
    Given workflow "AW-66005" for debtor "P-66005" is in status "REQUESTED" with covered claims "F-66005,F-66006"
    When the caseworker attempts to update covered claims to "F-66005"
    Then the request is rejected
    And workflow "AW-66005" keeps covered claims "F-66005,F-66006"
    And the response instructs to withdraw and recreate

  # AC-06
  Scenario: Callback requires both debtor scope and workflow reference match
    Given workflow "AW-66006" belongs to debtor "P-66006" and has workflowReference "WR-66006"
    When callback is received on debtor "P-OTHER" with workflowReference "WR-66006"
    Then callback is rejected
    And workflow "AW-66006" state is unchanged

  # AC-06
  Scenario: Callback with mismatched workflow reference is rejected
    Given workflow "AW-66006B" belongs to debtor "P-66006B" and has workflowReference "WR-66006B"
    When callback is received on debtor "P-66006B" with workflowReference "WR-OTHER"
    Then callback is rejected
    And workflow "AW-66006B" state is unchanged

  # AC-07 and AC-13
  Scenario: COMPLETED callback persists terminal state and policy-derived interruption atomically
    Given workflow "AW-66007" for debtor "P-66007" is in status "IN_COURT_PROCESS"
    When callback "COMPLETED" is received with court outcome date "2026-07-10"
    Then workflow "AW-66007" status becomes "COMPLETED"
    And a petition059 interruption is registered in the same transaction
    And interruption type is "UDLAEG"
    And interruption event date is "2026-07-10"
    And interruption uses the policy-derived legal reference for "UDLAEG"

  # AC-08 and AC-09
  Scenario: UNSUCCESSFUL callback requires reason code and registers interruption
    Given workflow "AW-66008" for debtor "P-66008" is in status "IN_COURT_PROCESS"
    When callback "UNSUCCESSFUL" is received with court outcome date "2026-07-11" and reason code "NO_ATTACHABLE_ASSETS"
    Then workflow "AW-66008" status becomes "UNSUCCESSFUL"
    And unsuccessful reason code is stored as "NO_ATTACHABLE_ASSETS"
    And interruption type "UDLAEG" is registered in the same transaction

  # AC-09
  Scenario: UNSUCCESSFUL callback without reason code is rejected
    Given workflow "AW-66009" for debtor "P-66009" is in status "IN_COURT_PROCESS"
    When callback "UNSUCCESSFUL" is received with court outcome date "2026-07-12" and no reason code
    Then callback is rejected
    And workflow "AW-66009" remains in status "IN_COURT_PROCESS"

  # AC-10
  Scenario: WITHDRAWN requires reason and emits no interruption
    Given workflow "AW-66010" for debtor "P-66010" is in status "REQUESTED"
    When the caseworker withdraws workflow "AW-66010" with reason "duplicate case intake"
    Then workflow "AW-66010" status becomes "WITHDRAWN"
    And no petition059 interruption is emitted

  # AC-11
  Scenario: Interruption emission occurs once per covered complex group
    Given workflow "AW-66011" covers claims "F-66011,F-66012,F-66013"
    And claims "F-66011" and "F-66012" belong to the same fordringskompleks
    And claim "F-66013" is standalone
    When workflow "AW-66011" transitions to "COMPLETED" with court outcome date "2026-07-13"
    Then interruption emission count is 2
    And petition059 propagation expands complex members internally

  # AC-12
  Scenario: Duplicate terminal callback is idempotent no-op
    Given workflow "AW-66012" is already in terminal status "COMPLETED"
    And interruption has already been registered for "AW-66012"
    When the same "COMPLETED" callback is received again
    Then no new interruption is emitted
    And workflow history records callback replay as idempotent no-op

  # AC-13
  Scenario: Legal reference is policy-derived and not caller-overridden
    Given workflow "AW-66013" for debtor "P-66013" is in status "IN_COURT_PROCESS"
    When terminal callback "UNSUCCESSFUL" is received with court outcome date "2026-07-14" and reason code "COURT_REJECTION"
    Then interruption is registered with policy-derived legal reference for "UDLAEG"
    And caller-provided legal reference overrides are ignored

  # AC-14
  Scenario: Debtor-scoped read returns status history and interruption linkage
    Given debtor "P-66014" has attachment workflows "AW-66014" and "AW-66015"
    When the caseworker requests attachment workflows for debtor "P-66014"
    Then each workflow includes current status
    And each workflow includes chronological status history
    And each workflow includes outcome qualifier
    And each terminal workflow includes interruption linkage metadata

  # AC-15
  Scenario: Integration-gateway is the external callback boundary
    Given an external fogedret callback for workflowReference "WR-66015"
    When the callback is received from an external court channel
    Then integration-gateway terminates the external request
    And the callback is delivered to the internal attachment-workflow handling boundary

  # AC-16
  Scenario: Callback ingress requires OCES3 mTLS at gateway
    Given an external fogedret callback for workflowReference "WR-66016"
    When the callback is received without valid OCES3 mTLS client authentication
    Then integration-gateway rejects the callback
    And no downstream workflow state is changed

  # AC-17
  Scenario: Callback replay is blocked at gateway
    Given callback message "CB-66017" for workflowReference "WR-66017" and outcome date "2026-07-15" was already processed
    When the same callback tuple is received again
    Then callback is rejected as replay
    And no downstream workflow state is changed
