Feature: Dedicated fordringshaver master data service

  Scenario: Operational creditor master data is stored in a dedicated backend service
    Given OpenDebt manages operational fordringshaver data
    When the architecture assigns system-of-record ownership for that data
    Then the owner is the dedicated creditor master data service
    And the owner is not creditor-portal

  Scenario: Organization identity data remains in Person Registry
    Given a fordringshaver has name, address, and CVR information
    When OpenDebt stores operational creditor master data
    Then the organization identity data remains owned by person-registry
    And the operational creditor service stores only a reference to that organization

  Scenario: Debt service validates creditor permissions through an internal API
    Given debt-service needs to validate whether a fordringshaver may create a claim
    When debt-service asks for creditor status and permissions
    Then the validation is performed through the creditor master data service API
    And debt-service does not read the creditor database directly
