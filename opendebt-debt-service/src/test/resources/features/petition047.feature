@petition047
Feature: Versioned business configuration administration (petition 047)

  Scenario: Create a new config entry with future valid-from date
    Given the business config table is empty
    When I create a config entry with key "RATE_INDR_STD" value "0.0575" valid from tomorrow
    Then the entry is stored with review status "PENDING_REVIEW"

  Scenario: Approve a pending config entry
    Given a PENDING_REVIEW config entry for key "RATE_INDR_STD"
    When I approve the entry
    Then the entry has review status "APPROVED"

  Scenario: Reject a pending config entry
    Given a PENDING_REVIEW config entry for key "FEE_STANDARDGEBYR"
    When I reject the entry
    Then the entry is deleted from the config table

  Scenario: List all config entries grouped by key
    Given config entries exist for keys "RATE_INDR_STD", "FEE_STANDARDGEBYR"
    When I list all config entries
    Then the result contains groups for "RATE_INDR_STD" and "FEE_STANDARDGEBYR"

  Scenario: Overlap detection prevents duplicate active periods
    Given an approved config entry for key "RATE_INDR_STD" valid from "2025-01-01" to "2025-12-31"
    When I try to create another entry for key "RATE_INDR_STD" valid from "2025-06-01"
    Then a validation error is returned indicating period overlap

  Scenario: Derived rates auto-computed when NB rate is created
    Given the business config table is empty
    When I create a config entry with key "RATE_NB_UDLAAN" value "5.0" valid from tomorrow
    Then derived entries are created for "RATE_INDR_STD", "RATE_INDR_TOLD", "RATE_INDR_TOLD_AFD"

  Scenario: Get version history for a config key
    Given 3 historical config entries exist for key "RATE_INDR_STD"
    When I request the history for key "RATE_INDR_STD"
    Then 3 entries are returned in the history

  Scenario: Effective value resolved correctly for a past date
    Given a config entry for key "RATE_INDR_STD" with value "0.08" valid from "2020-01-01" to "2022-12-31"
    And a config entry for key "RATE_INDR_STD" with value "0.0575" valid from "2023-01-01"
    When I request the effective value for key "RATE_INDR_STD" on date "2021-06-15"
    Then the returned value is "0.08"
