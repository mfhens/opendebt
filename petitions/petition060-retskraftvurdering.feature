@petition060
Feature: Retskraft evaluation ordering under section 50

  # Legal basis: gaeldsinddrivelsesbekendtgoerelsens section 50, GIL section 18c,
  # GIL section 4 subsection 2-5, GIL section 7 subsection 1-2, GIL section 16 subsection 1,
  # G.A.2.5 (v3.16, 2026-03-28)
  # Out of scope: substantive legal criteria for whether an individual claim is retskraftig,
  # automated correction of data errors, new creditor-facing portal behaviour

  Scenario: Default ordering without suspected data error
    Given debtor "D-06001" has these doubtful claims under collection:
      | claimId | category             | suspectedDataError |
      | C-06011 | FINE                 | false              |
      | C-06012 | PRIVATE_MAINTENANCE  | false              |
      | C-06013 | OTHER                | false              |
    When the system generates the default retskraft evaluation worklist
    Then the ranked order is:
      | rank | claimId  |
      | 1    | C-06011  |
      | 2    | C-06012  |
      | 3    | C-06013  |
    And the result identifies that the default section-50 ordering path was used

  Scenario: Special circumstances override records why default order was changed
    Given debtor "D-06002" has these doubtful claims under collection:
      | claimId | category             | suspectedDataError |
      | C-06021 | FINE                 | false              |
      | C-06022 | OTHER                | false              |
    And a caseworker records override reason "Urgent court deadline on C-06022"
    When the system generates the retskraft evaluation worklist with override enabled
    Then claim "C-06022" may appear ahead of the default section-50 order
    And the result includes override reason "Urgent court deadline on C-06022"
    And the result includes the legal basis for the override

  Scenario: Suspected data error uses discretionary ordering instead of default ranking
    Given debtor "D-06003" has these doubtful claims under collection:
      | claimId | category | suspectedDataError | amount | complexity | paymentOpportunity |
      | C-06031 | OTHER    | true               | 900    | low        | medium             |
      | C-06032 | OTHER    | true               | 300    | high       | high               |
      | C-06033 | OTHER    | true               | 700    | medium     | low                |
    When the system generates the retskraft evaluation worklist for suspected data error
    Then the result identifies that a discretionary data-error ordering path was used
    And each ranked item includes the factors used for prioritisation
    And the result does not claim that the default section-50 order was applied

  Scenario: Accessory amounts stay behind a principal claim until the principal is retskraftig
    Given principal claim "C-06041" is not yet established as retskraftig
    And accessory amount "A-06041" belongs to principal claim "C-06041"
    When the system generates the retskraft evaluation worklist
    Then accessory amount "A-06041" is not ranked ahead of principal claim "C-06041"
    When principal claim "C-06041" becomes established as retskraftig
    Then accessory amount "A-06041" may be included after principal claim "C-06041"

  Scenario: Disproportionate accessory evaluation removes the accessory amount from the worklist
    Given principal claim "C-06042" is established as retskraftig
    And accessory amount "A-06042" belongs to principal claim "C-06042"
    And accessory amount "A-06042" is written off because evaluation would be disproportionate
    When the system generates the retskraft evaluation worklist
    Then accessory amount "A-06042" is excluded from the worklist

  Scenario: Voluntary payment surplus limits which doubtful claims are selected for evaluation
    Given debtor "D-06005" has already covered retskraftige claims for 600 DKK from a 1000 DKK payment
    And debtor "D-06005" has these remaining doubtful items:
      | itemId   | itemType    | category | amount |
      | C-06051  | PRINCIPAL   | OTHER    | 200    |
      | C-06052  | PRINCIPAL   | OTHER    | 300    |
      | A-06052  | ACCESSORY   | OTHER    | 50     |
    When the system generates a retskraft worklist for the remaining voluntary-payment surplus
    Then the remaining amount window is 400 DKK
    And selected principal items follow the applicable GIL section 4 ordering
    And accessory item "A-06052" is ranked after principal items
    And the selected doubtful amount does not exceed 400 DKK

  Scenario: Expedited voluntary-payment deviation is logged when normal ordering would delay coverage
    Given debtor "D-06006" has a voluntary-payment surplus waiting to be applied
    And the normal section-50 ordering would delay application of that surplus
    When the system generates the expedited retskraft worklist for that payment context
    Then the worklist records that an expedited deviation was used
    And the result explains why quicker-to-apply claims were prioritised

  Scenario: Modregning uses confirmed claims first and evaluates doubtful claims within the remaining amount window
    Given debtor "D-06007" has an overskydende beloeb of 1200 DKK for modregning
    And debtor "D-06007" has confirmed retskraftige claims without suspected data error totalling 700 DKK
    And debtor "D-06007" has these remaining doubtful items:
      | itemId   | itemType    | amount |
      | C-06071  | PRINCIPAL   | 300    |
      | C-06072  | PRINCIPAL   | 250    |
      | A-06072  | ACCESSORY   | 40     |
    When the system generates the modregning retskraft worklist
    Then confirmed retskraftige claims are used before doubtful items
    And the remaining amount window for doubtful items is 500 DKK
    And accessory item "A-06072" is ranked after principal items

  Scenario: Partial or no modregning can be chosen for operational reasons and must be visible
    Given debtor "D-06008" is in a modregning context with payout deadline pressure
    And the required investigations are too complex to complete before the payout deadline
    When the system records a decision to perform no modregning for the current payout
    Then the decision is visible to the caseworker
    And the reason references timing or complexity constraints

  Scenario: Caseworkers can inspect the rule path and audit details for an ordering decision
    Given a retskraft evaluation worklist already exists for debtor "D-06009"
    When a caseworker inspects the ranking details
    Then the result shows the ordering mode used
    And the result shows the legal reference for the rule path
    And the result shows the actor or system origin and timestamp
    And the result uses technical identifiers only
