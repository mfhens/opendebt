Feature: Multi-regime interest compliance per debt type (CR-001 delta)

  # ── Pre-conditions ──────────────────────────────────────────────────────────
  # The rules engine receives an InterestCalculationRequest whose `interestRule`
  # and `annualRate` have been pre-resolved by the caller using
  # BusinessConfigService.getDecimalValue(configKey, calculationDate).
  # The DRL uses request.annualRate verbatim — it never overrides or resolves rates.
  # ── Formula (all rate rules): principal × (annualRate / 365) × daysPastDue ──

  # ---------------------------------------------------------------------------
  # Modified: replaces the placeholder "Standard Interest Rate" rule
  # ---------------------------------------------------------------------------

  Scenario: INDR_STD debt accrues interest at the pre-resolved standard rate
    Given an InterestCalculationRequest with interestRule "INDR_STD"
    And annualRate is 0.0575
    And principalAmount is 10000.00
    And daysPastDue is 365
    When the interest rules engine evaluates the request
    Then the interestAmount is 575.00 within 0.01
    And the rateType is "INDR_STD"
    And the legalBasis is "Gældsinddrivelsesloven § 5, stk. 1-2"

  # ---------------------------------------------------------------------------
  # Unchanged: carried forward from existing DRL with no logic change
  # ---------------------------------------------------------------------------

  Scenario: No interest accrues before the debt is due
    Given an InterestCalculationRequest with daysPastDue 0
    When the interest rules engine evaluates the request
    Then the interestAmount is 0.00
    And the rateType is "NOT_DUE"

  Scenario: No interest accrues on principal below 100 kr
    Given an InterestCalculationRequest with principalAmount 99.99
    And daysPastDue is 365
    When the interest rules engine evaluates the request
    Then the interestAmount is 0.00
    And the rateType is "SMALL_AMOUNT"

  # ---------------------------------------------------------------------------
  # New scenarios (CR-001)
  # ---------------------------------------------------------------------------

  Scenario: INDR_EXEMPT debt accrues zero interest regardless of principal
    Given an InterestCalculationRequest with interestRule "INDR_EXEMPT"
    And principalAmount is 50000.00
    And daysPastDue is 365
    When the interest rules engine evaluates the request
    Then the interestAmount is 0.00
    And the rateType is "INDR_EXEMPT"
    And the legalBasis is "Gældsinddrivelsesloven § 5, stk. 1; Retsplejeloven § 997, stk. 3"

  Scenario: INDR_TOLD debt uses pre-resolved told rate
    Given an InterestCalculationRequest with interestRule "INDR_TOLD"
    And annualRate is 0.0375
    And principalAmount is 10000.00
    And daysPastDue is 365
    When the interest rules engine evaluates the request
    Then the interestAmount is 375.00 within 0.01
    And the rateType is "INDR_TOLD"
    And the legalBasis is "EU-toldkodeks art. 114; Toldloven § 30a"

  Scenario: INDR_TOLD_AFD debt uses pre-resolved told-with-afdrag rate
    Given an InterestCalculationRequest with interestRule "INDR_TOLD_AFD"
    And annualRate is 0.0275
    And principalAmount is 10000.00
    And daysPastDue is 365
    When the interest rules engine evaluates the request
    Then the interestAmount is 275.00 within 0.01
    And the rateType is "INDR_TOLD_AFD"
    And the legalBasis is "EU-toldkodeks art. 114 (med afdragsordning)"

  Scenario: INDR_CONTRACT debt uses pre-resolved contractual rate
    Given an InterestCalculationRequest with interestRule "INDR_CONTRACT"
    And annualRate is 0.0800
    And principalAmount is 10000.00
    And daysPastDue is 365
    When the interest rules engine evaluates the request
    Then the interestAmount is 800.00 within 0.01
    And the rateType is "INDR_CONTRACT"
    And the legalBasis is "Gældsinddrivelsesbekendtgørelsen § 9, stk. 3"

  # ---------------------------------------------------------------------------
  # Time-aware acceptance criterion (non-regression guard)
  # ---------------------------------------------------------------------------

  Scenario: DRL uses annualRate verbatim — historical rate produces historically correct amount
    Given an InterestCalculationRequest with interestRule "INDR_STD"
    And annualRate is 0.0775
    And principalAmount is 10000.00
    And daysPastDue is 365
    When the interest rules engine evaluates the request
    Then the interestAmount is 775.00 within 0.01
    And the rateType is "INDR_STD"
