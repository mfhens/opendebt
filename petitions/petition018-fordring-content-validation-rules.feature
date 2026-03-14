Feature: Fordring Claim Content Validation Rules

  Background:
    Given system configuration parameters are loaded
    And maximum document size is 10MB
    And maximum note length is 4000 characters
    And maximum documents per action is 10

  # Claim Amount Validation

  Scenario: Hovedfordring without category HF is rejected
    Given an action creates a hovedfordring with category "UF"
    When the content validation rules are evaluated
    Then the action is rejected with error code 201
    And the error message contains "hovedfordring skal have fordringtypekategori HF"

  Scenario: Claim with amount below lower limit is rejected
    Given an action with claim amount 0
    When the content validation rules are evaluated
    Then the action is rejected with error code 215
    And the error message contains "Fordringsbeløb ikke større end nedre grænse"

  Scenario: Opskrivning with zero correction amount is rejected
    Given an OPSKRIVNINGREGULERING action with correction amount 0
    When the content validation rules are evaluated
    Then the action is rejected with error code 227
    And the error message contains "Korrektion på kr. 0 ikke muligt"

  Scenario: NEDSKRIV with zero amount is rejected
    Given a NEDSKRIV action with NedskrivningBeløb 0
    When the content validation rules are evaluated
    Then the action is rejected with error code 408
    And the error message contains "Nedskrivningsbeløb skal være større end 0"

  Scenario: Multiple hovedfordringer in one action is rejected
    Given an action creates 2 hovedfordringer
    When the content validation rules are evaluated
    Then the action is rejected with error code 425
    And the error message contains "kun oprettes en hovedfordring per aktion"

  # Sub-Claim Validation

  Scenario: Sub-claim with different art type is rejected
    Given hovedfordring "HF001" has ArtType "INDR"
    And a sub-claim for "HF001" has ArtType "MODR"
    When the content validation rules are evaluated
    Then the action is rejected with error code 270
    And the error message contains "Underfordringen matcher ikke art med hovedfordring"

  Scenario: Sub-claim with matching art type passes
    Given hovedfordring "HF001" has ArtType "INDR"
    And a sub-claim for "HF001" has ArtType "INDR"
    When the sub-claim art type rules are evaluated
    Then the action passes sub-claim art validation

  Scenario: Sub-claim type not allowed for fordringshaver is rejected
    Given fordringshaver "FH001" is not allowed sub-claim type "REN01"
    And an action includes a sub-claim of type "REN01"
    When the content validation rules are evaluated
    Then the action is rejected with error code 459
    And the error message contains "Underfordringstypen er ikke tilladt"

  Scenario: Related claim without HovedfordringID is rejected
    Given a related claim without HovedfordringID
    When the content validation rules are evaluated
    Then the action is rejected with error code 461
    And the error message contains "HovedfordringsID ikke angivet eller eksisterende"

  # Interest Validation

  Scenario Outline: MerRenteSats allowed only for specific RenteSatsKode
    Given an OPRETFORDRING with RenteSatsKode "<code>" and MerRenteSats "5.0"
    When the content validation rules are evaluated
    Then the action <result>

    Examples:
      | code | result                                    |
      | 03   | passes interest validation                |
      | 04   | passes interest validation                |
      | 07   | passes interest validation                |
      | 01   | is rejected with error code 436           |
      | 02   | is rejected with error code 436           |

  Scenario: RenteRegel 002 with invalid combination is rejected
    Given an action with RenteRegel "002" and RenteSatsKode "03" and MerRenteSats "5.0"
    When the content validation rules are evaluated
    Then the action is rejected with error code 441
    And the error message contains "Rente regel 002 kan kun bruges"

  Scenario: RenteRegel 002 with valid combination passes
    Given an action with RenteRegel "002" and RenteSatsKode "99" and MerRenteSats "00"
    When the interest rule validation is evaluated
    Then the action passes interest rule validation

  Scenario: Invalid RenteSatsKode for PSRM is rejected
    Given an action targeting PSRM with RenteSatsKode "99"
    When the content validation rules are evaluated
    Then the action is rejected with error code 442
    And the error message contains "RenteSatsKode kan ikke benyttes i PSRM"

  Scenario: Interest on non-interest-bearing claim type is rejected
    Given an action with claim type that does not bear interest
    And the action specifies interest
    When the content validation rules are evaluated
    Then the action is rejected with error code 443
    And the error message contains "Fordringstypen er ikke rentebærende"

  # Nedskriv Reason Validation

  Scenario: ÅrsagKode REGU at hæftelse level is rejected
    Given a NEDSKRIV action with ÅrsagKode "REGU" at hæftelse level
    When the content validation rules are evaluated
    Then the action is rejected with error code 410
    And the error message contains "Årsagskoden REGU kan kun benyttes på fordringsniveau"

  Scenario: Nedskrivning requiring debtor without debtor is rejected
    Given a NEDSKRIV action requiring debtor identity
    And no debtor identity is provided
    When the content validation rules are evaluated
    Then the action is rejected with error code 433
    And the error message contains "skyldners identitet påkrævet"

  Scenario: REGU nedskrivning with VirkningsDato is rejected
    Given a NEDSKRIV action with ÅrsagKode "REGU"
    And the action has VirkningsDato "2024-03-01"
    When the content validation rules are evaluated
    Then the action is rejected with error code 519
    And the error message contains "må ikke medsendes en virkningsdato"

  # Document and Note Validation

  Scenario: Document exceeding max size is rejected
    Given an action with a document of size 15MB
    When the content validation rules are evaluated
    Then the action is rejected with error code 164
    And the error message contains "filstørrelse er større end den tilladte grænse"

  Scenario: Too many documents is rejected
    Given an action with 15 documents
    When the content validation rules are evaluated
    Then the action is rejected with error code 181
    And the error message contains "Antal dokumenter indsendt per aktion større end"

  Scenario: Empty note is rejected
    Given an action with an empty sagsbemærkning
    When the content validation rules are evaluated
    Then the action is rejected with error code 220
    And the error message contains "Sagsbemærkninger på fordringen har ikke noget indhold"

  Scenario: Note exceeding max length is rejected
    Given an action with a note of 5000 characters
    When the content validation rules are evaluated
    Then the action is rejected with error code 413
    And the error message contains "Sagsbemærkninger må max være på"

  Scenario: Disallowed document type is rejected
    Given an action with a document of type "executable"
    When the content validation rules are evaluated
    Then the action is rejected with error code 415
    And the error message contains "Dokumentets filtype er ikke tilladt"

  Scenario: OANI with documents on underlying fordring is rejected
    Given an OANI action where underlying fordring has documents
    When the content validation rules are evaluated
    Then the action is rejected with error code 516
    And the error message contains "underliggende fordring indeholder dokumenter"

  # Hovedstol Validation

  Scenario: New hovedstol not higher than previous is rejected
    Given claim "C001" has current hovedstol 10000
    And an AENDRFORDRING specifies new hovedstol 9000
    When the content validation rules are evaluated
    Then the action is rejected with error code 510
    And the error message contains "hovedstol er lavere eller ens"

  Scenario: New hovedstol higher than previous passes
    Given claim "C001" has current hovedstol 10000
    And an AENDRFORDRING specifies new hovedstol 12000
    When the hovedstol amount rules are evaluated
    Then the action passes hovedstol amount validation

  Scenario: Hovedstol change for DMI claim is rejected
    Given claim "C001" is routed to DMI
    And an AENDRFORDRING attempts hovedstol change on "C001"
    When the content validation rules are evaluated
    Then the action is rejected with error code 512
    And the error message contains "DMI understøtter ikke ændring af hovedstol"

  Scenario: AENDRFORDRING without FHI struktur is rejected
    Given an AENDRFORDRING for hovedstol change
    And FejlagtigHovedstolIndberetningStruktur is not present
    When the content validation rules are evaluated
    Then the action is rejected with error code 517
    And the error message contains "FejlagtigHovedstolIndberetningStruktur mangler"

  # Hæftelse Validation

  Scenario: Duplicate hæftere is rejected
    Given a claim with two hæftere having same ID "H001"
    When the content validation rules are evaluated
    Then the action is rejected with error code 528
    And the error message contains "hæfter hvor hæfteren er den samme"

  Scenario: HaeftelseDomId without date is rejected
    Given a claim with HaeftelseDomId "DOM001"
    And no HaeftelseDomDato is provided
    When the content validation rules are evaluated
    Then the action is rejected with error code 531
    And the error message contains "HaeftelseDomId men ingen HaeftelseDomDato"

  Scenario: HaeftelseDomDato without id is rejected
    Given a claim with HaeftelseDomDato "2024-01-15"
    And no HaeftelseDomId is provided
    When the content validation rules are evaluated
    Then the action is rejected with error code 532
    And the error message contains "ikke et HaeftelseDomId men har alligevel en HaeftelseDomDato"

  Scenario: Future DomsDato is rejected
    Given a claim with DomsDato set to tomorrow
    When the content validation rules are evaluated
    Then the action is rejected with error code 533
    And the error message contains "Domsdato eller Forligsdato kan ikke være i fremtiden"

  Scenario: DMI claim with mismatched SRB/Forfald is rejected
    Given a claim routed to DMI with two hæftere
    And hæfter 1 has SRB "2024-01-01" and Forfald "2024-02-01"
    And hæfter 2 has SRB "2024-01-15" and Forfald "2024-02-15"
    When the content validation rules are evaluated
    Then the action is rejected with error code 557
    And the error message contains "SRB og Forfald er forskellig på to hæftere"

  Scenario: DMI claim with hæftelse-level documents is rejected
    Given a claim routed to DMI
    And the claim has documents at hæftelse level
    When the content validation rules are evaluated
    Then the action is rejected with error code 559
    And the error message contains "dokumenter på hæftelsesniveau"

  # Routing Validation

  Scenario: Synchronous portal action for DMI claim is rejected
    Given a claim routed to DMI
    And a synchronous action submitted via portal
    When the content validation rules are evaluated
    Then the action is rejected with error code 422
    And the error message contains "via portalen på fordringer i DMI"

  Scenario: NAOR to DMI is rejected
    Given a NAOR action for a claim routed to DMI
    When the content validation rules are evaluated
    Then the action is rejected with error code 426
    And the error message contains "NedskrivningAnnulleretOpskrivningRegulering understøttes ikke af DMI"

  Scenario: DMI claim with invalid AKR length is rejected
    Given a claim routed to DMI
    And the debtor has AKR number exceeding allowed length
    When the content validation rules are evaluated
    Then the action is rejected with error code 565
    And the error message contains "skyldner har et PSRM AKR-nummer"

  Scenario: ForeløbigFastsat claim to PSRM is rejected
    Given a claim with ForeløbigFastsat set to true
    And the claim targets PSRM
    When the content validation rules are evaluated
    Then the action is rejected with error code 572
    And the error message contains "forløbig fastsat kan ikke sendes til PSRM"

  # Claim Type Validation

  Scenario: Unknown FordringID is rejected
    Given FordringID "F999" is not known by Fordring
    And an action references FordringID "F999"
    When the content validation rules are evaluated
    Then the action is rejected with error code 509
    And the error message contains "FordringID er ikke kendt af Fordring"

  Scenario: AENDRFORDRING on INDR claim is rejected
    Given claim "C001" has ArtType "INDR"
    And an AENDRFORDRING targets claim "C001"
    When the content validation rules are evaluated
    Then the action is rejected with error code 537
    And the error message contains "FordringAendr aktioner på inddrivelsesfordringer"

  Scenario: Missing required stamdata is rejected
    Given an action with missing required stamdata fields
    When the content validation rules are evaluated
    Then the action is rejected with error code 550
    And the error message contains "Stamdata felter på fordringsaktionen mangler"

  Scenario: Inactive claim type in PSRM is rejected
    Given an action with claim type "HF99" that is inactive in PSRM
    When the content validation rules are evaluated
    Then the action is rejected with error code 574
    And the error message contains "Fordringstypen er inaktiv i PSRM"

  Scenario: Missing required BFE field is rejected
    Given an action with claim type requiring BFE
    And BFE field is empty
    When the content validation rules are evaluated
    Then the action is rejected with error code 575
    And the error message contains "Feltet BFE skal være udfyldt"

  # Identifier Validation

  Scenario: Non-unique FordringshaverRefID is rejected
    Given FordringshaverRefID "REF001" already exists
    And an action uses FordringshaverRefID "REF001"
    When the content validation rules are evaluated
    Then the action is rejected with error code 486
    And the error message contains "FordringshaverRefID er ikke entydig"

  Scenario: Modification of error-withdrawn claim is rejected
    Given claim "C001" is already withdrawn with reason FEJL
    And an action attempts to modify claim "C001"
    When the content validation rules are evaluated
    Then the action is rejected with error code 602
    And the error message contains "allerede tilbagekaldt/tilbagesend med årsagskode FEJL"

  Scenario: Non-FEJL withdrawal after HENS/KLAG/BORD is rejected
    Given claim "C001" was withdrawn with reason "HENS"
    And a new withdrawal attempts reason "BORT"
    When the content validation rules are evaluated
    Then the action is rejected with error code 603
    And the error message contains "kun returneres eller tilbagesendes med FEJL"
