Feature: Fordringshaver logical and physical data model

  Scenario: Operational creditor data references organization identity by technical ID only
    Given organization identity data for a fordringshaver is stored in person-registry
    When OpenDebt creates the operational creditor record
    Then the record stores `creditor_org_id` as the reference to that organization
    And the operational creditor record does not store name, address, or CVR/SE/AKR directly

  Scenario: A parent fordringshaver can be linked to a child fordringshaver
    Given fordringshaver "K_PARENT" exists
    And fordringshaver "K_CHILD" exists
    When OpenDebt records "K_PARENT" as parent of "K_CHILD"
    Then "K_CHILD" stores a self-referencing parent creditor relation

  Scenario: Interest notification modes are mutually exclusive
    Given a fordringshaver configuration enables interest notifications
    When the same configuration also enables detailed interest notifications
    Then the configuration is rejected

  Scenario: Equalisation and allocation notifications are mutually exclusive
    Given a fordringshaver configuration enables equalisation notifications
    When the same configuration also enables allocation notifications
    Then the configuration is rejected

  Scenario: Permission flags default to false
    Given a new fordringshaver configuration is created without explicit action permissions
    Then all `allow_*` permission flags are stored as `false`

  Scenario: NEM_KONTO settlement does not store bank account fields
    Given a fordringshaver has settlement method `NEM_KONTO`
    When the settlement configuration is stored
    Then `iban`, `swift_code`, and `danish_account_number` are `null`

  Scenario: Automatic hearing cancellation requires a positive day count
    Given a fordringshaver enables automatic hearing cancellation
    When `auto_cancel_hearing_days` is `0`
    Then the configuration is rejected

  Scenario: Sorting ID is constrained for operational matching
    Given a fordringshaver configuration contains sorting ID `abcdefghi`
    When OpenDebt validates the creditor configuration
    Then the configuration is rejected because sorting ID must be uppercase and at most 8 characters
