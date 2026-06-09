@petition060
Feature: Section 50 retskraft worklist portal view (P060)
  # Module scope: opendebt-caseworker-portal.section50
  # Canonical source: petitions/petition060-retskraftvurdering.feature
  # Petition: petitions/petition060-retskraftvurdering.md
  # Outcome contract: petitions/petition060-retskraftvurdering-outcome-contract.md
  # Architecture: design/solution-architecture-p060-retskraftvurdering.md

  Scenario: Caseworker can inspect override, deviation, modregning outcome, and audit details
    Given a caseworker is authenticated with write access
    And debtor "06000000-0000-0000-0000-000000000060" has a stored section 50 worklist "06000000-0000-0000-0000-000000000063" with:
      | orderingMode       | OVERRIDE                                   |
      | contextType        | MODREGNING                                 |
      | legalReference     | Section 50 override path                   |
      | amountWindow       | 500.00                                     |
      | generatedAt        | 2026-05-27T08:15:00Z                       |
      | overrideReason     | Urgent court deadline on C-06022           |
      | overrideLegalBasis | Section 50 subsection 5                    |
      | deviationReason    | Expedite to use voluntary payment surplus  |
      | modregningOutcome  | NO_MODREGNING                              |
      | selectedNextItemId | C-06022                                    |
    And the worklist contains these ranked entries:
      | rank | claimId  | itemType  | claimCategory | suspectedDataError | confirmedRetskraft | withinAmountWindow | selectionReason                  | prioritisationFactors | suppressedReason                 | amount |
      | 1    | C-06022  | PRINCIPAL | OTHER         | false              | false              | true               | Override selected claim first    | urgent,deadline       |                                  | 250.00 |
      | 2    | C-06021  | PRINCIPAL | FINE          | false              | true               | true               | Confirmed claim already settled  |                       |                                  | 700.00 |
      | 3    | A-06022  | ACCESSORY | OTHER         | true               | false              | false              | Accessory deferred until principal | complexity,timing   | Deferred for accessory sequencing | 40.00  |
    And the decision snapshot contains:
      | rulePath           | SECTION_50_OVERRIDE_PATH               |
      | legalReference     | Section 50 override path               |
      | origin             | CASEWORKER                             |
      | occurredAt         | 2026-05-27T08:15:00Z                   |
      | inputHash          | abc123hash                             |
      | notes              | Timing pressure before payout deadline |
      | selectedNextItemId | C-06022                                |
    When the caseworker opens the petition060 worklist page
    Then the page shows the override reason "Urgent court deadline on C-06022"
    And the page shows the deviation reason "Expedite to use voluntary payment surplus"
    And the page shows the modregning outcome "NO_MODREGNING"
    And the page shows the decision rule path "SECTION_50_OVERRIDE_PATH"
    And the page shows the technical identifier "C-06022"
