Feature: Fordring Core Claim Validation Rules

  Background:
    Given a valid fordringhaveraftale "FA001" exists for fordringshaver "FH001"
    And the agreement allows claim types "HF01, UF01"
    And the agreement has MFAftaleSystemIntegration set to true

  # Structure Validation Rules

  Scenario: OPRETFORDRING without struktur is rejected
    Given an action with AktionKode "OPRETFORDRING"
    And the action does not include MFOpretFordringStruktur
    When the core validation rules are evaluated
    Then the action is rejected with error code 444
    And the error message contains "MFOpretFordringStruktur mangler"

  Scenario: OPRETFORDRING with struktur passes structure validation
    Given an action with AktionKode "OPRETFORDRING"
    And the action includes MFOpretFordringStruktur
    When the structure validation rules are evaluated
    Then the action passes structure validation

  Scenario: GENINDSENDFORDRING without struktur is rejected
    Given an action with AktionKode "GENINDSENDFORDRING"
    And the action does not include MFGenindsendFordringStruktur
    When the core validation rules are evaluated
    Then the action is rejected with error code 403
    And the error message contains "MFGenindsendFordringStruktur mangler"

  Scenario: NEDSKRIV without struktur is rejected
    Given an action with AktionKode "NEDSKRIV"
    And the action does not include MFNedskrivFordringStruktur
    When the core validation rules are evaluated
    Then the action is rejected with error code 447
    And the error message contains "MFNedskrivFordringStruktur mangler"

  Scenario: TILBAGEKALD without struktur is rejected
    Given an action with AktionKode "TILBAGEKALD"
    And the action does not include MFTilbagekaldFordringStruktur
    When the core validation rules are evaluated
    Then the action is rejected with error code 448
    And the error message contains "MFTilbagekaldFordringStruktur mangler"

  Scenario: AENDRFORDRING without struktur is rejected
    Given an action with AktionKode "AENDRFORDRING"
    And the action does not include MFAendrFordringStruktur
    When the core validation rules are evaluated
    Then the action is rejected with error code 458
    And the error message contains "MFAendrFordringStruktur mangler"

  Scenario Outline: Complex action types require correct struktur
    Given an action with AktionKode "<action_type>"
    And the action does not include the required struktur
    When the core validation rules are evaluated
    Then the action is rejected with error code <error_code>

    Examples:
      | action_type                                   | error_code |
      | OPSKRIVNINGREGULERING                         | 404        |
      | OPSKRIVNINGANNULLERETNEDSKRIVNINGINDBETALING  | 406        |
      | OPSKRIVNINGOMGJORTNEDSKRIVNINGREGULERING      | 407        |
      | NEDSKRIVNINGANNULLERETOPSKRIVNINGREGULERING   | 412        |
      | NEDSKRIVNINGANNULLERETOPSKRIVNINGINDBETALING  | 505        |

  # Currency Validation (Rule 152)

  Scenario: Claim with DKK currency passes validation
    Given an action with AktionKode "OPRETFORDRING"
    And the action has ValutaKode "DKK"
    When the currency validation rules are evaluated
    Then the action passes currency validation

  Scenario: Claim with non-DKK currency is rejected
    Given an action with AktionKode "OPRETFORDRING"
    And the action has ValutaKode "EUR"
    When the core validation rules are evaluated
    Then the action is rejected with error code 152
    And the error message contains "ValutaKode ifølge fordringhaveraftale"

  Scenario Outline: Currency validation applies to applicable action types
    Given an action with AktionKode "<action_type>"
    And the action has ValutaKode "USD"
    When the core validation rules are evaluated
    Then the action is rejected with error code 152

    Examples:
      | action_type                                  |
      | OPRETFORDRING                                |
      | GENINDSENDFORDRING                           |
      | NEDSKRIV                                     |
      | OPSKRIVNINGREGULERING                        |
      | OPSKRIVNINGOMGJORTNEDSKRIVNINGREGULERING     |
      | OPSKRIVNINGANNULLERETNEDSKRIVNINGINDBETALING |

  # Art Type Validation (Rule 411)

  Scenario: OPRETFORDRING with art type INDR passes validation
    Given an action with AktionKode "OPRETFORDRING"
    And the action has ArtType "INDR"
    When the art type validation rules are evaluated
    Then the action passes art type validation

  Scenario: OPRETFORDRING with art type MODR passes validation
    Given an action with AktionKode "OPRETFORDRING"
    And the action has ArtType "MODR"
    When the art type validation rules are evaluated
    Then the action passes art type validation

  Scenario: OPRETFORDRING with invalid art type is rejected
    Given an action with AktionKode "OPRETFORDRING"
    And the action has ArtType "INVALID"
    When the core validation rules are evaluated
    Then the action is rejected with error code 411
    And the error message contains "Fordringsart må være inddrivelse"

  Scenario: GENINDSENDFORDRING with invalid art type is rejected
    Given an action with AktionKode "GENINDSENDFORDRING"
    And the action has ArtType "OTHER"
    When the core validation rules are evaluated
    Then the action is rejected with error code 411

  # Interest Rate Validation (Rule 438)

  Scenario: Claim with positive interest rate passes validation
    Given an action with MerRenteSats 5.5
    When the interest validation rules are evaluated
    Then the action passes interest validation

  Scenario: Claim with zero interest rate passes validation
    Given an action with MerRenteSats 0
    When the interest validation rules are evaluated
    Then the action passes interest validation

  Scenario: Claim with negative interest rate is rejected
    Given an action with MerRenteSats -2.0
    When the core validation rules are evaluated
    Then the action is rejected with error code 438
    And the error message contains "MerRenteSats kan ikke være negativ"

  # Date Validation Rules

  Scenario: Claim with missing VirkningsDato is rejected
    Given an action that requires VirkningsDato
    And VirkningsDato is not provided
    When the core validation rules are evaluated
    Then the action is rejected with error code 409
    And the error message contains "Virkningsdato skal være udfyldt"

  Scenario: Claim with VirkningsDato later than receipt is rejected
    Given an action with ModtagelsesTidspunkt "2024-03-01"
    And the action has VirkningsDato "2024-03-15"
    When the core validation rules are evaluated
    Then the action is rejected with error code 464
    And the error message contains "Virkningsdato må ikke være senere end modtagelsestidspunktet"

  Scenario: Claim with future VirkningsDato is rejected
    Given an action with VirkningsDato set to tomorrow
    When the core validation rules are evaluated
    Then the action is rejected with error code 548
    And the error message contains "virkningsdato må ikke være fremtidig"

  Scenario: Claim with date before 1900 is rejected
    Given an action with VirkningsDato "1899-12-31"
    When the core validation rules are evaluated
    Then the action is rejected with error code 568
    And the error message contains "datoer der ligger før år 1900"

  Scenario: Claim with PeriodeFra after PeriodeTil is rejected
    Given an action with PeriodeFra "2024-06-01"
    And the action has PeriodeTil "2024-03-01"
    When the core validation rules are evaluated
    Then the action is rejected with error code 569
    And the error message contains "PeriodeFra må ikke være efter PeriodeTil"

  Scenario: Claim with valid date range passes validation
    Given an action with PeriodeFra "2024-01-01"
    And the action has PeriodeTil "2024-06-30"
    And the action has VirkningsDato "2024-03-15"
    And the action has ModtagelsesTidspunkt "2024-03-15"
    When the date validation rules are evaluated
    Then the action passes date validation

  # Agreement Validation Rules

  Scenario: Claim with non-existent agreement is rejected
    Given an action with FordringhaveraftaleID "NONEXISTENT"
    When the core validation rules are evaluated
    Then the action is rejected with error code 2
    And the error message contains "Fordringhaveraftale findes ikke"

  Scenario: Claim with disallowed claim type is rejected
    Given an action with AktionKode "OPRETFORDRING"
    And the action has DMIFordringTypeKode "HF99"
    And the agreement does not allow claim type "HF99"
    When the core validation rules are evaluated
    Then the action is rejected with error code 151
    And the error message contains "må ikke indberettes på denne DMIFordringTypeKode"

  Scenario: System-to-system submission without integration flag is rejected
    Given an action submitted via system-to-system integration
    And the agreement has MFAftaleSystemIntegration set to false
    When the core validation rules are evaluated
    Then the action is rejected with error code 156
    And the error message contains "MFAftaleSystemIntegration på fordringhaveraftale er falsk"

  # Debtor Validation (Rule 005)

  Scenario: Claim with valid debtor ID passes validation
    Given an action with AktionKode "OPRETFORDRING"
    And the action has a valid debtor ID "12345678"
    When the debtor validation rules are evaluated
    Then the action passes debtor validation

  Scenario: Claim with zero debtor ID is rejected
    Given an action with AktionKode "OPRETFORDRING"
    And the action has debtor ID "0"
    When the core validation rules are evaluated
    Then the action is rejected with error code 5
    And the error message contains "Skyldner der er angivet findes ikke"

  Scenario: Claim with all-zeros debtor ID is rejected
    Given an action with AktionKode "OPRETFORDRING"
    And the action has debtor ID "00000000-0000-0000-0000-000000000000"
    When the core validation rules are evaluated
    Then the action is rejected with error code 5

  # Combined Validation

  Scenario: Valid OPRETFORDRING passes all core validations
    Given an action with AktionKode "OPRETFORDRING"
    And the action includes MFOpretFordringStruktur
    And the action has ValutaKode "DKK"
    And the action has ArtType "INDR"
    And the action has valid dates
    And the action references a valid agreement
    And the action has a valid debtor ID
    When the core validation rules are evaluated
    Then the action passes all core validation rules
