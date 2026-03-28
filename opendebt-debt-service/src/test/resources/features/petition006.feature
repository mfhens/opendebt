Feature: Indsigelse and workflow blocking during dispute handling

  Scenario: A skyldner registers an objection against a claim
    Given a debt exists for objection testing
    When an objection is registered with reason "claim amount disputed"
    Then the objection status is "ACTIVE"
    And the debt readiness status is "READY_FOR_COLLECTION"

  Scenario: Registering an objection does not block collection
    Given a debt exists for objection testing
    When an objection is registered with reason "claim amount disputed"
    Then the debt readiness status is "READY_FOR_COLLECTION"

  Scenario: An active objection blocks collection progression
    Given a debt exists with an active objection
    When the system checks for active objections
    Then hasActiveObjection returns true

  Scenario: A rejected objection allows collection to resume
    Given a debt exists with an active objection
    When the objection is resolved as "REJECTED" with note "insufficient evidence"
    Then the objection status is "REJECTED"
    And the debt readiness status is "READY_FOR_COLLECTION"

  Scenario: An upheld objection marks the indsigelse as upheld but does not auto-block collection
    Given a debt exists with an active objection
    When the objection is resolved as "UPHELD" with note "valid dispute"
    Then the objection status is "UPHELD"
    And the debt readiness status is "READY_FOR_COLLECTION"

  Scenario: Duplicate active objection is rejected
    Given a debt exists with an active objection
    When a second objection is attempted
    Then the objection is rejected with message "Active objection already exists"

  Scenario: Objection history is preserved for audit
    Given a debt exists with a resolved objection
    When the objection history is queried
    Then the resolved objection appears in the history
