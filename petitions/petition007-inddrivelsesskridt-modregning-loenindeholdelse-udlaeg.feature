Feature: Formalization of collection steps for modregning, lønindeholdelse, and udlæg

  Scenario: A transferred restance can receive a modregning step
    Given restance "R1" has been transferred to collection
    When OpenDebt initiates a modregning step for restance "R1"
    Then an inddrivelsesskridt record is created for restance "R1"
    And the step type is "modregning"
    And the step has a recorded initiator and creation time

  Scenario: A transferred restance can receive a lønindeholdelse step
    Given restance "R2" has been transferred to collection
    When OpenDebt initiates a lønindeholdelse step for restance "R2"
    Then an inddrivelsesskridt record is created for restance "R2"
    And the step type is "lønindeholdelse"
    And the case timeline for restance "R2" includes the step

  Scenario: A collection step cannot be created before transfer to collection
    Given restance "R3" has not been transferred to collection
    When OpenDebt attempts to initiate an udlæg step for restance "R3"
    Then the step initiation is rejected
    And no inddrivelsesskridt record is created for restance "R3"

  Scenario: A completed collection step remains aligned with bookkeeping and audit
    Given restance "R4" has an active modregning step
    When the modregning step for restance "R4" is completed with a financial effect
    Then the step status is updated to completed
    And the financial consequence is reflected in bookkeeping or payment history
    And the audit trail preserves the step lifecycle
