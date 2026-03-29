@petition061
Feature: Afdragsordninger — instalment plan management (P061)

  # Legal basis: GIL §§ 11 stk. 1–2, 11 stk. 6, 11 stk. 11, 11a, 45; Gæld.bekendtg. chapter 7;
  # G.A.3.1.1, G.A.3.1.1.1–3, G.A.2.4
  # G.A. snapshot version: v3.16 (2026-03-28)
  # Out of scope: Catala encoding (P071), automatiseret modregning (G.A.3.1.4),
  # udlægsforretning logic (P066), kulanceaftale supervisory approval.
  # AC-5 (tabeltræk determinism) is verified by unit tests, not Gherkin scenarios.
  # AC-22 (CLS audit log) is covered by the two audit-log scenarios below.
  # AC-23: every other acceptance criterion is covered by at least one scenario below.

  # ===========================================================================
  # FR-1: Tabeltræk calculation engine (GIL § 11, stk. 1–2)
  # ===========================================================================

  Scenario: Tabeltræk for debitor uden forsørgerpligt beregnes korrekt og afrundes ned
    # AC-1: 250,000 kr × 13% = 32,500 kr / 12 = 2,708.33 kr → afrundet NED til 2,700 kr
    Given the afdragsordning service has an index table for year 2026 loaded
    And the lavindkomstgrænse for 2026 is 138,500 kr/year
    And the afdragsprocent for 250,000 kr/year without forsørgerpligt in 2026 is 13%
    When the tabeltræk engine is invoked with:
      | field               | value   |
      | annualNettoindkomst | 250000  |
      | forsørgerpligt      | false   |
      | indexYear           | 2026    |
    Then the engine returns:
      | field                   | value    |
      | afdragsprocent          | 13%      |
      | annualBetalingsevne     | 32500    |
      | monthlyYdelseUnrounded  | 2708.33  |
      | monthlyYdelse           | 2700     |
    And monthlyYdelse is a multiple of 50
    And monthlyYdelse is less than or equal to monthlyYdelseUnrounded

  Scenario: Tabeltræk for debitor med forsørgerpligt giver lavere procent og ydelse end uden
    # AC-2 and AC-3: 250,000 kr × 10% = 25,000 kr / 12 = 2,083.33 kr → 2,050 kr
    # Forsørgerpligtig debitor får lavere afdragsprocent (10% vs 13%) ved samme indkomst
    Given the afdragsordning service has an index table for year 2026 loaded
    And the afdragsprocent for 250,000 kr/year with forsørgerpligt in 2026 is 10%
    When the tabeltræk engine is invoked with:
      | field               | value   |
      | annualNettoindkomst | 250000  |
      | forsørgerpligt      | true    |
      | indexYear           | 2026    |
    Then the engine returns:
      | field                   | value    |
      | afdragsprocent          | 10%      |
      | annualBetalingsevne     | 25000    |
      | monthlyYdelseUnrounded  | 2083.33  |
      | monthlyYdelse           | 2050     |
    And the monthly ydelse for forsørgerpligtig debitor (2050 kr) is lower than for debitor without forsørgerpligt (2700 kr) at the same income level

  Scenario: Debitor under lavindkomstgrænse kan ikke oprette afdragsordning via tabeltræk
    # AC-4: Nettoindkomst 120,000 kr er under lavindkomstgrænse 138,500 kr → 0 kr/month → afvist
    Given the afdragsordning service has an index table for year 2026 loaded
    And the lavindkomstgrænse for 2026 is 138,500 kr/year
    And a fordring "FDR-61001" is under inddrivelse for debtor "P-61001"
    When a CreateAfdragsordningRequest is submitted for debtor "P-61001" with:
      | field               | value   |
      | method              | TABELTRÆK |
      | annualNettoindkomst | 120000  |
      | forsørgerpligt      | false   |
      | indexYear           | 2026    |
    Then the service returns HTTP 422
    And the error code is "BELOW_LAVINDKOMSTGRAENSE"
    And no afdragsordning is created

  Scenario: Tabeltræk-motor er deterministisk — samme input giver altid samme output
    # AC-5: Covered by unit tests; this scenario verifies the observable contract
    Given the afdragsordning service has an index table for year 2026 loaded
    When the tabeltræk engine is invoked twice with identical inputs:
      | field               | value   |
      | annualNettoindkomst | 250000  |
      | forsørgerpligt      | false   |
      | indexYear           | 2026    |
    Then both invocations return identical monthlyYdelse values
    And both invocations return identical afdragsprocent values

  Scenario: Månedlig ydelse er altid et multiplum af 50 og afrundet ned
    # AC-6: Verify rounding invariant for a non-round income amount
    Given the afdragsordning service has an index table for year 2026 loaded
    And the afdragsprocent for 185,000 kr/year without forsørgerpligt in 2026 is 11%
    When the tabeltræk engine is invoked with annualNettoindkomst 185000, forsørgerpligt false, indexYear 2026
    Then the returned monthlyYdelse is a multiple of 50
    And the returned monthlyYdelse is less than or equal to (185000 * 0.11 / 12)

  # ===========================================================================
  # FR-2: Yearly index table management (GIL § 45)
  # ===========================================================================

  Scenario: Ny indekstabel tager effekt for planer oprettet fra 1. januar
    # AC-7: Plans created before 1 Jan use old table; plans created on/after use new table
    Given the afdragsordning service has an index table for year 2026 loaded
    And an existing AKTIV afdragsordning "AFD-62001" was created in 2026 with index year 2026
    When an admin loads a new index table for year 2027 via POST /admin/index-tables
    And a new CreateAfdragsordningRequest is submitted after 1 January 2027
    Then the new afdragsordning uses the 2027 index table for its calculation
    And afdragsordning "AFD-62001" still uses the 2026 index table

  Scenario: Eksisterende aktive afdragsordninger genberegnes ikke automatisk ved ny indekstabel
    # AC-8: No automatic recalculation of existing plans
    Given an existing AKTIV afdragsordning "AFD-62002" with monthlyYdelse 2700 kr (index year 2026)
    When an admin loads a new index table for year 2027 with different bracket values
    Then afdragsordning "AFD-62002" still has monthlyYdelse 2700 kr
    And no YDELSE_AENDRET lifecycle event is recorded for "AFD-62002"

  # ===========================================================================
  # FR-3: Afdragsordning lifecycle management (G.A.3.1.1)
  # ===========================================================================

  Scenario: Oprettet afdragsordning aktiveres og lønindeholdelse suspenderes
    # AC-9 and AC-10: OPRETTET → AKTIV transition; lønindeholdelse is suspended
    Given a fordring "FDR-61010" is under inddrivelse for debtor "P-61010"
    And an existing lønindeholdelsesafgørelse "LOENI-61010" is AKTIV for debtor "P-61010"
    And a new afdragsordning "AFD-61010" has been created in state OPRETTET for debtor "P-61010"
    When the caseworker confirms afdragsordning "AFD-61010"
    Then afdragsordning "AFD-61010" transitions to state AKTIV
    And lønindeholdelsesafgørelse "LOENI-61010" is suspended
    And a suspension event is recorded in the lønindeholdelse service with the afdragsordning reference
    And a lifecycle event is recorded for "AFD-61010" with:
      | field      | value     |
      | from_state | OPRETTET  |
      | to_state   | AKTIV     |
      | actor      | caseworker |

  Scenario: Ugyldig tilstandsovergang fra ANNULLERET til AKTIV afvises
    # AC-9: Illegal state transitions rejected
    Given an afdragsordning "AFD-61011" is in state ANNULLERET
    When the caseworker attempts to confirm afdragsordning "AFD-61011"
    Then the service returns HTTP 409
    And the error message describes the invalid state transition
    And afdragsordning "AFD-61011" remains in state ANNULLERET

  Scenario: Fordring tilføjes til aktiv afdragsordning og ydelse genberegnes
    # AC-11: Adding a fordring to AKTIV plan succeeds; MISLIGHOLT/ANNULLERET rejected
    Given an AKTIV afdragsordning "AFD-61020" for debtor "P-61020"
    And a new fordring "FDR-61021" is under inddrivelse for debtor "P-61020"
    When the caseworker submits AddFordringToAfdragsordningRequest for "AFD-61020" adding "FDR-61021"
    Then "FDR-61021" appears in the included fordringer list of "AFD-61020"
    And if the total outstanding amount changed materially, a YDELSE_AENDRET lifecycle event is recorded

  Scenario: Fordring kan ikke tilføjes til en annulleret afdragsordning
    # AC-11: Adding fordring to ANNULLERET plan fails
    Given an afdragsordning "AFD-61022" is in state ANNULLERET
    And a fordring "FDR-61023" is under inddrivelse
    When the caseworker submits AddFordringToAfdragsordningRequest for "AFD-61022" adding "FDR-61023"
    Then the service returns HTTP 409

  Scenario: Caseworker annullerer aktiv afdragsordning med begrundelse
    Given an AKTIV afdragsordning "AFD-61030" for debtor "P-61030"
    When the caseworker submits AnnullerAfdragsordningRequest for "AFD-61030" with reason "Fordringen er bortfaldet"
    Then afdragsordning "AFD-61030" transitions to state ANNULLERET
    And a lifecycle event is recorded with from_state AKTIV, to_state ANNULLERET, and the provided reason
    And lønindeholdelse for debtor "P-61030" may resume at caseworker discretion

  # ===========================================================================
  # FR-4: Misligholdelse detection (G.A.3.1.1)
  # ===========================================================================

  Scenario: Manglende betaling udløser misligholdelsesvarsel og status ændres til MISLIGHOLT
    # AC-12: Missed payment → varsel sent → after notice period → MISLIGHOLT
    Given an AKTIV afdragsordning "AFD-61040" for debtor "P-61040" with monthlyYdelse 2700 kr
    And the payment due date for "AFD-61040" has passed 30 days ago with no payment registered
    When the scheduled misligholdelse detection job runs
    Then a misligholdelsesvarsel is sent to debtor "P-61040" via Digital Post
    And the varsel sent timestamp is recorded on afdragsordning "AFD-61040"
    When the statutory notice period elapses without payment
    Then afdragsordning "AFD-61040" transitions to state MISLIGHOLT
    And a MISLIGHOLT lifecycle event is recorded with timestamp and actor "system"

  Scenario: Lønindeholdelsessuspension ophæves ved MISLIGHOLT og sagsbehandler notificeres
    # AC-13: Lønindeholdelse suspension lifted on MISLIGHOLT
    Given an AKTIV afdragsordning "AFD-61041" for debtor "P-61041"
    And lønindeholdelsesafgørelse "LOENI-61041" is suspended due to "AFD-61041"
    When afdragsordning "AFD-61041" transitions to state MISLIGHOLT
    Then lønindeholdelsesafgørelse "LOENI-61041" suspension is lifted
    And the lønindeholdelse service receives a MISLIGHOLT notification for debtor "P-61041"
    And a caseworker notification is created indicating that lønindeholdelse may resume

  Scenario: Sagsbehandler genindtræder misligholt afdragsordning med sagsnote
    # AC-14: Reinstatement requires non-empty note
    Given an afdragsordning "AFD-61042" is in state MISLIGHOLT
    When the caseworker submits ReinstateAfdragsordningRequest for "AFD-61042" with note "Skyldner har betalt restancen"
    Then afdragsordning "AFD-61042" transitions to state AKTIV
    And lønindeholdelse for debtor is suspended again
    And a lifecycle event is recorded with from_state MISLIGHOLT, to_state AKTIV, and the provided note

  Scenario: Genindtræden uden sagsnote afvises
    # AC-14: Reinstatement without note → HTTP 422
    Given an afdragsordning "AFD-61043" is in state MISLIGHOLT
    When the caseworker submits ReinstateAfdragsordningRequest for "AFD-61043" with an empty note
    Then the service returns HTTP 422
    And afdragsordning "AFD-61043" remains in state MISLIGHOLT

  # ===========================================================================
  # FR-5: Konkret betalingsevnevurdering (GIL § 11, stk. 6)
  # ===========================================================================

  Scenario: Konkret betalingsevnevurdering med budgetskema giver lavere ydelse end tabeltræk
    # AC-15 and AC-16: Debtor at/above lavindkomstgrænse → budgetskema → lower konkret ydelse
    Given the lavindkomstgrænse for 2026 is 138,500 kr/year
    And a fordring "FDR-61050" is under inddrivelse for debtor "P-61050"
    And debtor "P-61050" has annual nettoindkomst 250,000 kr (at or above lavindkomstgrænse)
    And the tabeltræk reference ydelse for debtor "P-61050" is 2,700 kr/month
    When the caseworker submits a CreateAfdragsordningRequest with method KONKRET and BudgetskemaDto:
      | field              | value  |
      | monthlyIncome      | 20833  |
      | monthlyExpenses    | 18500  |
      | numberOfDependents | 2      |
    Then the service computes a konkret monthly ydelse of 2,333 kr (income 20,833 − expenses 18,500 = 2,333 disposable)
    And 2,333 kr is lower than the tabeltræk reference ydelse of 2,700 kr
    And the caseworker is informed that the konkret ydelse is lower than the tabeltræk ydelse
    And after caseworker confirmation, the afdragsordning is created with:
      | field              | value     |
      | method             | KONKRET   |
      | monthlyYdelse      | 2333      |
      | tabeltræksReference | 2700     |
    And the full budgetskema inputs are stored on the afdragsordning entity

  Scenario: Konkret betalingsevnevurdering afvises for debitor under lavindkomstgrænsen
    # AC-15: Debtor below lavindkomstgrænse → rejected with HTTP 422
    Given the lavindkomstgrænse for 2026 is 138,500 kr/year
    And a fordring "FDR-61051" is under inddrivelse for debtor "P-61051"
    And debtor "P-61051" has annual nettoindkomst 120,000 kr (below lavindkomstgrænse)
    When the caseworker submits a CreateAfdragsordningRequest with method KONKRET for debtor "P-61051"
    Then the service returns HTTP 422
    And the error code is "BELOW_LAVINDKOMSTGRAENSE_KONKRET_NOT_AVAILABLE"
    And no afdragsordning is created

  # ===========================================================================
  # FR-6: Kulanceaftale workflow (GIL § 11, stk. 11)
  # ===========================================================================

  Scenario: Kulanceaftale oprettes med sagsbehandlers begrundelse og manuel ydelse
    # AC-17 and AC-18: Kulanceaftale requires justification; no tabeltræk calculation
    Given a fordring "FDR-61060" is under inddrivelse for debtor "P-61060"
    And caseworker "SB-61060" holds role SAGSBEHANDLER with kulanceaftale permission
    When caseworker "SB-61060" submits a CreateKulanceaftaleRequest with:
      | field         | value                                              |
      | monthlyYdelse | 500                                                |
      | justification | Skyldner er alvorligt syg og har ingen indkomst.   |
    Then the service creates an afdragsordning with type KULANCE and monthlyYdelse 500 kr
    And the justification text is stored on the afdragsordning entity
    And caseworker identity "SB-61060" is recorded on the entity
    And no tabeltræk or konkret calculation is performed

  Scenario: Kulanceaftale uden begrundelse afvises
    # AC-17: Kulanceaftale without justification → HTTP 422
    Given a fordring "FDR-61061" is under inddrivelse for debtor "P-61061"
    And caseworker "SB-61061" holds role SAGSBEHANDLER with kulanceaftale permission
    When caseworker "SB-61061" submits a CreateKulanceaftaleRequest with an empty justification
    Then the service returns HTTP 422
    And the error message indicates that justification is required

  # ===========================================================================
  # FR-7: Virksomhed afdragsordning (G.A.3.1.1.2)
  # ===========================================================================

  Scenario: Afdragsordning for igangværende virksomhed kræver dokumentationsreference
    # AC-19: Igangværende virksomhed → evidenceReference required
    Given a fordring "FDR-61070" is under inddrivelse for virksomhed "ORG-61070" (igangværende)
    And caseworker "SB-61070" holds role SAGSBEHANDLER
    When caseworker "SB-61070" submits a CreateAfdragsordningRequest with:
      | field               | value                      |
      | entityType          | VIRKSOMHED_IGANGVAERENDE   |
      | monthlyYdelse       | 3000                       |
      | evidenceReference   | DOC-61070-bankstatement    |
    Then the service creates an afdragsordning with type VIRKSOMHED_IGANGVAERENDE
    And the evidence reference "DOC-61070-bankstatement" is stored on the entity
    And no tabeltræk calculation is performed

  Scenario: Afdragsordning for igangværende virksomhed uden dokumentationsreference afvises
    # AC-19: Missing evidenceReference → HTTP 422
    Given a fordring "FDR-61071" is under inddrivelse for virksomhed "ORG-61071" (igangværende)
    When the caseworker submits a CreateAfdragsordningRequest with entityType VIRKSOMHED_IGANGVAERENDE and no evidenceReference
    Then the service returns HTTP 422
    And the error message indicates that evidenceReference is required

  Scenario: Afdragsordning for afmeldt virksomhed behandles som privat person med tabeltræk
    # AC-19: Afmeldt virksomhed → tabeltræk applies
    Given the afdragsordning service has an index table for year 2026 loaded
    And a fordring "FDR-61072" is under inddrivelse for virksomhed "ORG-61072" (afmeldt)
    When the caseworker submits a CreateAfdragsordningRequest with:
      | field               | value                  |
      | entityType          | VIRKSOMHED_AFMELDT     |
      | method              | TABELTRÆK              |
      | annualNettoindkomst | 250000                 |
      | forsørgerpligt      | false                  |
    Then the service processes the request using the standard tabeltræk engine
    And the afdragsordning is created with the tabeltræk-computed monthlyYdelse

  # ===========================================================================
  # FR-8: Sagsbehandler portal UI
  # ===========================================================================

  Scenario: Sagsbehandlerportal viser aktiv afdragsordning med status og beløb
    # AC-20: Portal detail view shows state, fordringer, ydelse, method, next payment date
    Given an AKTIV afdragsordning "AFD-61080" exists for debtor "P-61080" with:
      | field             | value       |
      | monthlyYdelse     | 2700        |
      | method            | TABELTRÆK   |
      | afdragsprocent    | 13%         |
      | indexYear         | 2026        |
      | nextPaymentDate   | 2026-05-01  |
    And the fordring "FDR-61080" is included in "AFD-61080" with outstanding balance 54,000 kr
    When caseworker "SB-61080" navigates to the afdragsordning detail page for "AFD-61080"
    Then the portal displays:
      | field           | value       |
      | state           | AKTIV       |
      | monthlyYdelse   | 2700 kr     |
      | method          | TABELTRÆK   |
      | afdragsprocent  | 13%         |
      | indexYear       | 2026        |
      | nextPaymentDate | 2026-05-01  |
    And the portal displays the included fordring "FDR-61080" with outstanding balance 54,000 kr
    And the portal displays the payment history for "AFD-61080"
    And the debtor CPR number is displayed (resolved from Person Registry; not from afdragsordning DB)

  # ===========================================================================
  # FR-8 / API: Query endpoint returns payment history and next ydelse date
  # ===========================================================================

  Scenario: API-forespørgsel returnerer afdragsordning med betalingshistorik og næste ydelsesdato
    Given an AKTIV afdragsordning "AFD-61090" exists with payment history and next ydelse date 2026-06-01
    When a GET request is made to /afdragsordning/AFD-61090
    Then the response status is 200
    And the response body includes:
      | field              | value      |
      | id                 | AFD-61090  |
      | state              | AKTIV      |
      | nextPaymentDate    | 2026-06-01 |
      | afbryderForaeldelse | false     |
    And the response body includes a non-empty paymentHistory array with at least one entry
    And the response body does not contain any CPR number, CVR number, name, or address

  # ===========================================================================
  # FR-9: Forældelse interaction (G.A.2.4)
  # ===========================================================================

  Scenario: Afdragsordning API-svar indeholder afbryderForaeldelse false
    # AC-21: afbryderForaeldelse: false in every response
    Given an AKTIV afdragsordning "AFD-61100" exists for debtor "P-61100"
    When a GET request is made to /afdragsordning/AFD-61100
    Then the response body contains "afbryderForaeldelse": false
    And no AFBRYDELSE event is recorded on the forældelse entity for debtor "P-61100" due to the afdragsordning

  Scenario: Oprettelse af afdragsordning afbryder ikke forældelsesfrist
    Given a fordring "FDR-61101" is under inddrivelse with a running forældelsesfrist
    And the current forældelsesfrist expiry date for "FDR-61101" is "2030-01-01"
    When an afdragsordning is successfully created for fordring "FDR-61101"
    Then the forældelsesfrist expiry date for "FDR-61101" remains "2030-01-01"
    And no AFBRYDELSE event is recorded in the forældelse log for "FDR-61101"

  # ===========================================================================
  # NFR-3: GDPR — ingen CPR/CVR i afdragsordning-service
  # ===========================================================================

  Scenario: Afdragsordning entity indeholder ingen personhenføring udover person_id
    # AC-23: No CPR, CVR, name, or address in afdragsordning DB or API response
    Given an AKTIV afdragsordning "AFD-61110" exists for debtor "P-61110"
    When the afdragsordning entity is retrieved directly from the database
    Then the entity does not contain any CPR number field
    And the entity does not contain any CVR number field
    And the entity does not contain any name field
    And the entity does not contain any address field
    And the debtor is referenced only via person_id "P-61110"

  # ===========================================================================
  # NFR-2: Audit log (CLS) — all lifecycle events are logged
  # ===========================================================================

  Scenario: Vellykket oprettelse af afdragsordning logges til revisionssporet
    # AC-22: All lifecycle events logged to CLS
    Given a fordring "FDR-61120" is under inddrivelse for debtor "P-61120"
    When a valid CreateAfdragsordningRequest is submitted and succeeds
    Then the CLS audit log contains an entry for the OPRETTET event with:
      | field      | value      |
      | person_id  | P-61120    |
      | actor      | caseworker |
      | to_state   | OPRETTET   |
      | outcome    | SUCCESS    |

  Scenario: Fejlende oprettelse af afdragsordning logges til revisionssporet
    # AC-22: Failed operations also logged
    Given a fordring "FDR-61121" is under inddrivelse
    When an invalid CreateAfdragsordningRequest is submitted (missing required fields)
    Then the service returns HTTP 422
    And the CLS audit log contains an entry for the failed creation attempt with outcome FAILURE

  # ===========================================================================
  # FR-9 / Backend enforcement: afbryderForaeldelse always false
  # ===========================================================================

  Scenario Outline: Alle afdragsordning-svar inkluderer afbryderForaeldelse false uanset tilstand
    Given an afdragsordning "AFD-61130" is in state <state>
    When a GET request is made to /afdragsordning/AFD-61130
    Then the response body contains "afbryderForaeldelse": false

    Examples:
      | state        |
      | OPRETTET     |
      | AKTIV        |
      | MISLIGHOLT   |
      | AFVIKLET     |
      | ANNULLERET   |
