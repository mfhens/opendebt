Feature: OCR-based matching of incoming payments

  Background:
    Given incoming payments are received by OpenDebt from SKB as CREMUL payment entries

  Scenario: Unique OCR auto-match even when the amount differs
    Given an issued påkrav contains Betalingsservice OCR-linje "OCR-123"
    And OCR-linje "OCR-123" uniquely identifies debt "D1"
    And debt "D1" has an outstanding balance of 1000 DKK
    And an incoming payment references OCR-linje "OCR-123" with amount 900 DKK
    When the payment is processed
    Then the payment is auto-matched to debt "D1"
    And the payment is not routed to manual matching on the case

  Scenario: Debt is written down by the actual paid amount
    Given an issued påkrav contains Betalingsservice OCR-linje "OCR-456"
    And OCR-linje "OCR-456" uniquely identifies debt "D2"
    And debt "D2" has an outstanding balance of 1000 DKK
    And an incoming payment references OCR-linje "OCR-456" with amount 600 DKK
    When the payment is processed
    Then the payment is auto-matched to debt "D2"
    And debt "D2" is written down by 600 DKK
    And debt "D2" has 400 DKK remaining

  Scenario: Payment without a unique OCR match is routed to manual matching on the case
    Given an incoming payment does not contain an OCR-linje that uniquely identifies a debt
    When the payment is processed
    Then the payment is not auto-matched
    And the payment is routed to manual matching on the case

  Scenario Outline: Overpayment follows a rule-driven branch after OCR auto-match
    Given an issued påkrav contains Betalingsservice OCR-linje "OCR-789"
    And OCR-linje "OCR-789" uniquely identifies debt "D3"
    And debt "D3" has an outstanding balance of 1000 DKK
    And an incoming payment references OCR-linje "OCR-789" with amount 1400 DKK
    And rules for sagstype and frivillig indbetaling resolve the excess amount outcome to "<outcome>"
    When the payment is processed
    Then the payment is auto-matched to debt "D3"
    And debt "D3" is written down by the actual paid amount according to the applicable payment rules
    And the excess amount outcome is "<outcome>"

    Examples:
      | outcome                        |
      | payout                         |
      | use to cover other debt posts  |
