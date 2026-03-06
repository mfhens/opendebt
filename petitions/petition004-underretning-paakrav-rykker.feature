Feature: Formalization of underretning, påkrav, and rykker

  Scenario: A påkrav is issued as a formal underretning for a fordring
    Given fordring "F1" belongs to fordringshaver "K1"
    And skyldner "S1" is the recipient for fordring "F1"
    When OpenDebt issues a påkrav for fordring "F1"
    Then an underretning record is created
    And the underretning type is "påkrav"
    And the underretning is linked to fordring "F1"
    And the underretning identifies fordringshaver "K1"
    And the underretning identifies skyldner "S1"

  Scenario: An OCR-based påkrav carries the OCR-linje used for payment matching
    Given fordring "F2" shall be paid through a Betalingsservice OCR-linje
    When OpenDebt issues a påkrav for fordring "F2"
    Then the påkrav contains an OCR-linje
    And the OCR-linje can be used by later incoming payment matching

  Scenario: A rykker is issued for an unpaid claim
    Given fordring "F3" has not been paid by its betalingsfrist
    When OpenDebt issues a rykker for fordring "F3"
    Then an underretning record is created
    And the underretning type is "rykker"
    And the underretning is linked to fordring "F3"

  Scenario: An underretning can have multiple skyldner recipients
    Given fordring "F4" is addressed to skyldner "S1" and skyldner "S2"
    When OpenDebt issues a påkrav for fordring "F4"
    Then the underretning identifies skyldner "S1" as a recipient
    And the underretning identifies skyldner "S2" as a recipient
    And the communication history preserves both recipients
