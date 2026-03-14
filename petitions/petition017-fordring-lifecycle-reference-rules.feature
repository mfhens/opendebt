Feature: Fordring Claim Lifecycle and Reference Rules

  Background:
    Given fordringshaver "FH001" exists with valid agreement
    And the claim repository is available

  # Genindsend (Resubmit) Rules

  Scenario Outline: Resubmission with valid withdrawal reason passes
    Given claim "C001" was withdrawn by fordringshaver "FH001" with reason "<reason>"
    And fordringshaver "FH001" submits a GENINDSENDFORDRING for claim "C001"
    And the stamdata matches the original claim
    When the lifecycle rules are evaluated
    Then the action passes genindsend validation

    Examples:
      | reason |
      | HENS   |
      | KLAG   |
      | BORD   |
      | HAFT   |

  Scenario: Resubmission with invalid withdrawal reason is rejected
    Given claim "C001" was withdrawn by fordringshaver "FH001" with reason "FEJL"
    And fordringshaver "FH001" submits a GENINDSENDFORDRING for claim "C001"
    When the lifecycle rules are evaluated
    Then the action is rejected with error code 539
    And the error message contains "tilbagekaldt med andet end HENS KLAG BORD eller HAFT"

  Scenario: Resubmission of non-withdrawn claim is rejected
    Given claim "C001" is active and not withdrawn
    And fordringshaver "FH001" submits a GENINDSENDFORDRING for claim "C001"
    When the lifecycle rules are evaluated
    Then the action is rejected with error code 540
    And the error message contains "fordringen ikke er tilbagekaldt"

  Scenario: Resubmission from different fordringshaver is rejected
    Given claim "C001" was withdrawn by fordringshaver "FH001" with reason "HENS"
    And fordringshaver "FH002" submits a GENINDSENDFORDRING for claim "C001"
    When the lifecycle rules are evaluated
    Then the action is rejected with error code 541
    And the error message contains "genindsendes af samme fordringshaver"

  Scenario: Resubmission with different stamdata is rejected
    Given claim "C001" was withdrawn by fordringshaver "FH001" with reason "HENS"
    And fordringshaver "FH001" submits a GENINDSENDFORDRING for claim "C001"
    And the stamdata differs from the original claim
    When the lifecycle rules are evaluated
    Then the action is rejected with error code 542
    And the error message contains "Stamdata på aktionen der genindsendes er forskellig"

  Scenario: Resubmission of MODR claim is rejected
    Given claim "C001" with ArtType "MODR" was withdrawn with reason "HENS"
    And fordringshaver "FH001" submits a GENINDSENDFORDRING for claim "C001"
    When the lifecycle rules are evaluated
    Then the action is rejected with error code 544
    And the error message contains "kan ikke genindsendes modregningsfordringer"

  # Tilbagekald (Withdrawal) Rules

  Scenario: Correction of claim withdrawn in conversion is rejected
    Given claim "C001" was withdrawn during the conversion process
    And fordringshaver "FH001" submits a correction action for claim "C001"
    When the lifecycle rules are evaluated
    Then the action is rejected with error code 434
    And the error message contains "tilbagekaldt i konverteringsprocessen"

  Scenario: BORT withdrawal for DMI-routed claim is rejected
    Given claim "C001" is routed to DMI
    And fordringshaver "FH001" submits a TILBAGEKALD for claim "C001" with reason "BORT"
    When the lifecycle rules are evaluated
    Then the action is rejected with error code 538
    And the error message contains "BORT må ikke benyttes til fordringer i DMI"

  Scenario: FEJL withdrawal with VirkningsDato is rejected
    Given claim "C001" exists
    And fordringshaver "FH001" submits a TILBAGEKALD for claim "C001" with reason "FEJL"
    And the action has VirkningsDato "2024-03-01"
    When the lifecycle rules are evaluated
    Then the action is rejected with error code 546
    And the error message contains "Virkningsdato må ikke være udfyldt"

  Scenario: FEJL withdrawal without VirkningsDato passes
    Given claim "C001" exists
    And fordringshaver "FH001" submits a TILBAGEKALD for claim "C001" with reason "FEJL"
    And the action has no VirkningsDato
    When the lifecycle rules are evaluated
    Then the action passes withdrawal date validation

  Scenario: Eftersendt claim referencing withdrawn main claim is rejected
    Given hovedfordring "HF001" is withdrawn
    And fordringshaver "FH001" submits an eftersendt related claim referencing "HF001"
    When the lifecycle rules are evaluated
    Then the action is rejected with error code 547
    And the error message contains "relation til en tilbagekaldt fordring"

  Scenario: Withdrawal of hovedfordring with un-withdrawn divided claims is rejected
    Given hovedfordring "HF001" has divided related claims "RC001" and "RC002"
    And related claim "RC001" is not yet withdrawn
    And fordringshaver "FH001" submits a TILBAGEKALD for hovedfordring "HF001"
    When the lifecycle rules are evaluated
    Then the action is rejected with error code 570
    And the error message contains "opsplittede relaterede fordringer er tilbagekaldt"

  # Action Reference Rules

  Scenario: Action while previous action pending is rejected
    Given claim "C001" has a pending action that is not UDFØRT
    And fordringshaver "FH001" submits a new action for claim "C001"
    When the lifecycle rules are evaluated
    Then the action is rejected with error code 418
    And the error message contains "allerede indberettet en aktion der ikke er UDFØRT"

  Scenario: Reference to unknown AktionID is rejected
    Given action "A999" does not exist in the system
    And an action references AktionID "A999"
    When the lifecycle rules are evaluated
    Then the action is rejected with error code 429
    And the error message contains "aktionID som er ukendt for Fordring"

  Scenario: Invalid MFAktionIDRef is rejected
    Given action "A001" references MFAktionIDRef "REF999"
    And "REF999" does not exist
    When the lifecycle rules are evaluated
    Then the action is rejected with error code 526
    And the error message contains "AktionIDRef findes ikke"

  Scenario: OANI without AktionIDRef when FordringID known is rejected
    Given FordringID "F001" is known by Fordring
    And an OANI action for FordringID "F001" without MFAktionIDRef
    When the lifecycle rules are evaluated
    Then the action is rejected with error code 527
    And the error message contains "MFAktionIDRef er ikke udfyldt"

  Scenario: Reference to withdrawn claim is rejected
    Given claim "C001" is withdrawn
    And an action references claim "C001"
    When the lifecycle rules are evaluated
    Then the action is rejected with error code 530
    And the error message contains "peges på er tilbagekaldt"

  # Opskrivning/Nedskrivning Reference Rules

  Scenario: OANI with mismatched beløb is rejected
    Given nedskrivning action "N001" has NedskrivningBeløb 5000
    And an OANI action references "N001" with OpskrivningBeløb 4000
    When the lifecycle rules are evaluated
    Then the action is rejected with error code 469
    And the error message contains "FordringOpskrivningBeløb må ikke være forskellig"

  Scenario: Action with mismatched VirkningsDato is rejected
    Given referenced action "N001" has VirkningFra "2024-01-15"
    And the current action has VirkningsDato "2024-02-01"
    When the lifecycle rules are evaluated
    Then the action is rejected with error code 470
    And the error message contains "VirkningFra må ikke være forskellig"

  Scenario: OANI referencing nedskrivning without INDB reason is rejected
    Given nedskrivning action "N001" has ÅrsagKode "REGU"
    And an OANI action references "N001"
    When the lifecycle rules are evaluated
    Then the action is rejected with error code 471
    And the error message contains "FordringNedskrivningÅrsagKode være INDB"

  Scenario: OANI not referencing nedskriv action is rejected
    Given action "A001" is of type OPRETFORDRING
    And an OANI action references "A001"
    When the lifecycle rules are evaluated
    Then the action is rejected with error code 473
    And the error message contains "MFAktionIDRef pege på nedskriv aktion"

  Scenario: Opskrivning on interest claim is rejected
    Given claim "C001" is an interest claim (rente)
    And an opskrivning action targets claim "C001"
    When the lifecycle rules are evaluated
    Then the action is rejected with error code 474
    And the error message contains "opskrivningsfordringer på renter"

  Scenario: OONR referencing nedskrivning without REGU reason is rejected
    Given nedskrivning action "N001" has ÅrsagKode "INDB"
    And an OONR action references "N001"
    When the lifecycle rules are evaluated
    Then the action is rejected with error code 477
    And the error message contains "FordringNedskrivningÅrsagKode være REGU"

  Scenario: Reference to rejected opskrivning/nedskrivning is rejected
    Given nedskrivning action "N001" is rejected
    And an annullering action references "N001"
    When the lifecycle rules are evaluated
    Then the action is rejected with error code 493
    And the error message contains "aktion er afvist"

  Scenario: Action with mismatched FordringID is rejected
    Given action "A001" is for FordringID "F001"
    And a correction action for FordringID "F002" references "A001"
    When the lifecycle rules are evaluated
    Then the action is rejected with error code 494
    And the error message contains "fordringsID på aktionen matcher ikke"

  Scenario: NAOR not referencing OR or OONR type is rejected
    Given action "A001" is of type OANI
    And a NAOR action references "A001"
    When the lifecycle rules are evaluated
    Then the action is rejected with error code 502
    And the error message contains "skal være OpskrivningRegulering eller OONR"

  Scenario: Annullering when prior annullering exists is rejected
    Given nedskrivning action "N001" has a pending annullering "AN001"
    And a new annullering action targets "N001"
    When the lifecycle rules are evaluated
    Then the action is rejected with error code 503
    And the error message contains "allerede modtaget en annullering"

  Scenario: NAOR with mismatched beløb is rejected
    Given opskrivning action "O001" has OpskrivningsBeløb 5000
    And a NAOR action references "O001" with NedskrivningBeløb 4000
    When the lifecycle rules are evaluated
    Then the action is rejected with error code 504
    And the error message contains "FordringNedskrivningBeløb skal være lig med"

  Scenario: NAOI not referencing OANI type is rejected
    Given action "A001" is of type OONR
    And a NAOI action references "A001"
    When the lifecycle rules are evaluated
    Then the action is rejected with error code 506
    And the error message contains "skal være af typen OANI"

  # State Validation Rules

  Scenario: Reference to rejected FordringID is rejected
    Given FordringID "F001" is rejected
    And an action references FordringID "F001"
    When the lifecycle rules are evaluated
    Then the action is rejected with error code 428
    And the error message contains "fordringID eller hovedfordringID er afvist"

  Scenario: Annullering of DMI action not yet UDFØRT is rejected
    Given action "A001" is in DMI and not yet UDFØRT
    And an annullering action references "A001"
    When the lifecycle rules are evaluated
    Then the action is rejected with error code 488
    And the error message contains "endnu ikke udført i DMI"

  Scenario: Opskrivning on withdrawn original claim is rejected
    Given original claim "C001" is withdrawn
    And an opskrivning action targets claim "C001"
    When the lifecycle rules are evaluated
    Then the action is rejected with error code 496
    And the error message contains "oprindelig fordring som er tilbagesendt tilbagekaldt eller returneret"

  Scenario: Nedskrivning on withdrawn FordringID is rejected
    Given FordringID "F001" is withdrawn
    And a nedskrivning action targets FordringID "F001"
    When the lifecycle rules are evaluated
    Then the action is rejected with error code 498
    And the error message contains "fordringsID som er tilbagesendt tilbagekaldt eller returneret"
