@petition026
Feature: Citizen debt summary enrichment contract
  # Module scope: opendebt-debt-service (P026-DEBT-001; SA-026 slice S2)
  # Canonical source: petitions/petition026-citizen-debt-overview-page.feature
  # Specs: petitions/specs/petition026-specs.yaml
  # Architecture: design/solution-architecture-026.md

  # VAL-P026-005 — creditorDisplayName is mandatory and citizenStatus replaces legacy-only presentation.
  Scenario: Petition026 enriches an in-collection debt row with citizen-safe presentation data
    Given petition026 citizen "person-026A" has an in-collection debt that needs enrichment
    When petition026 debt-service builds the citizen debt summary for page 0 and size 20
    Then petition026 each returned debt includes creditorDisplayName
    And petition026 the first returned debt exposes citizenStatus "IN_COLLECTION"
    And petition026 the first returned debt omits conditional fields when they do not apply

  # VAL-P026-013 — paused interest uses structured reason codes.
  Scenario: Petition026 exposes paused-interest reason codes
    Given petition026 citizen "person-026B" has a paused-interest debt with reason code "CLAIM_UNCLEAR_DEBTOR_CANNOT_PAY"
    When petition026 debt-service builds the citizen debt summary for page 0 and size 20
    Then petition026 the first returned debt exposes interestAccrualState "PAUSED"
    And petition026 the first returned debt exposes interestPauseReasonCode "CLAIM_UNCLEAR_DEBTOR_CANNOT_PAY"
    And petition026 the first returned debt omits writtenOffReasonCode

  # VAL-P026-014 — interest metadata is row-level and page-level.
  Scenario: Petition026 exposes interest-rate metadata for interest-bearing debts
    Given petition026 citizen "person-026C" has an interest-bearing debt using interest rule "INDR_STD"
    When petition026 debt-service builds the citizen debt summary for page 0 and size 20
    Then petition026 the first returned debt exposes interestRuleCode "INDR_STD"
    And petition026 the first returned debt exposes currentInterestRate
    And petition026 the response exposes effectiveInterestRates metadata for rule "INDR_STD"

  # VAL-P026-015 — every written-off reason code is machine-readable.
  Scenario Outline: Petition026 exposes written-off reason codes
    Given petition026 citizen "person-026D" has a written-off debt with reason code "<reasonCode>"
    When petition026 debt-service builds the citizen debt summary for page 0 and size 20
    Then petition026 the first returned debt exposes citizenStatus "WRITTEN_OFF"
    And petition026 the first returned debt exposes writtenOffReasonCode "<reasonCode>"

    Examples:
      | reasonCode                     |
      | LIMITATION_EXPIRED             |
      | BANKRUPTCY                     |
      | ESTATE_OF_DECEASED             |
      | DEBT_RESTRUCTURING             |
      | RECOVERY_FUTILE                |
      | RECOVERY_COST_DISPROPORTIONATE |

  # P026-DEBT-001 — page metadata must remain truthful across multi-page datasets.
  Scenario: Petition026 preserves enrichment across paginated debt summaries
    Given petition026 citizen "person-026E" has 5 enriched debts
    When petition026 debt-service builds the citizen debt summary for page 1 and size 2
    Then petition026 the response echoes pageNumber 1 and pageSize 2
    And petition026 the response reports totalElements 5 and totalPages 3
    And petition026 the response contains 2 debts on the current page
    And petition026 each returned debt includes creditorDisplayName

  # VAL-P026-003 / NFR-GDPR-002 — the projection must stay scoped to the requested person_id only.
  Scenario: Petition026 scopes the citizen debt summary to the requested person_id
    Given petition026 citizen "person-026F" has 2 summary debts totaling "3500.00"
    And petition026 another citizen "person-026G" has 3 debts outside the summary scope
    When petition026 debt-service builds the citizen debt summary for page 0 and size 20
    Then petition026 the response contains only debts for citizen "person-026F"
    And petition026 the response totalDebtCount is 2
    And petition026 the response totalOutstandingAmount is "3500.00"
    And petition026 the response reports totalElements 2 and totalPages 1
