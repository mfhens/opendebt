Feature: Collection measures for modregning, loenindeholdelse, and udlaeg

  Scenario: A transferred restance can receive a set-off (modregning) step
    Given a debt in OVERDRAGET state
    When a SET_OFF measure is initiated with amount 5000
    Then a collection measure record is created with type "SET_OFF"
    And the measure status is "INITIATED"

  Scenario: A transferred restance can receive a wage garnishment step
    Given a debt in OVERDRAGET state
    When a WAGE_GARNISHMENT measure is initiated
    Then a collection measure record is created with type "WAGE_GARNISHMENT"

  Scenario: A transferred restance can receive an attachment (udlaeg) step
    Given a debt in OVERDRAGET state
    When an ATTACHMENT measure is initiated
    Then a collection measure record is created with type "ATTACHMENT"

  Scenario: Collection step cannot be created before transfer
    Given a debt in REGISTERED state
    When a SET_OFF measure is attempted
    Then the initiation is rejected with "OVERDRAGET"

  Scenario: A collection measure can be completed
    Given an existing INITIATED collection measure
    When the measure is completed
    Then the measure status is "COMPLETED"
    And a completion timestamp is recorded

  Scenario: A collection measure can be cancelled
    Given an existing INITIATED collection measure
    When the measure is cancelled with reason "debtor paid"
    Then the measure status is "CANCELLED"

  Scenario: A completed measure cannot be cancelled
    Given an existing COMPLETED collection measure
    When cancellation is attempted
    Then the system rejects the cancellation
