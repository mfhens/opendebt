@petition058
Feature: Modregning i udbetalinger fra det offentlige og korrektionspulje (G.A.2.3.3–2.3.4)
  # Legal basis: GIL §§ 4 stk. 5–11, 7 stk. 1–2, 8b, 9a, 17 stk. 1; Nemkonto § 16 stk. 1;
  #   Gæld.bekendtg. § 7 stk. 4; Kildeskattelov §§ 62, 62A; Lov om børne-og-ungeydelse § 11 stk. 2
  # G.A. snapshot: v3.16 (2026-03-28)
  # Out of scope: DMI korrektionspulje settlement (GIL § 4 stk. 9), tværgående
  #   lønindeholdelse korrektionspulje (G.A.2.3.4.3), konkurslov/gældsbrevslov exceptions,
  #   manual caseworker-initiated modregning.
  # AC-18 (i18n bundle coverage) is verified by CI bundle-lint tests, not by Gherkin scenarios.

  Background:
    Given the debt-service modregning workflow is active
    And the payment-service DaekningsRaekkefoeigenService is available
    And the caseworker portal is running
    And BusinessConfigService key "rentelov.refRate" is set to "9.0" percent
    And the DanishBankingCalendar is configured for the current test year
    And the debtorPersonId for any debtor is stored as a UUID never as CPR

  # ==============================================================================
  # FR-1: Automatic payment interception workflow — tier-1 full coverage
  # AC-2
  # ==============================================================================

  Scenario: Tier-1 fuld dækning — udbetalende myndighed dækker alle fordringer
    Given debtor "SKY-5801" has the following tier-1 fordringer registered by the paying authority:
      | fordringId | tilbaestaaendeBeloeb | registreringsdato |
      | FDR-58011  | 1200.00              | 2024-11-01        |
      | FDR-58012  | 800.00               | 2024-11-15        |
    And debtor "SKY-5801" has the following tier-2 fordringer under RIM inddrivelse:
      | fordringId | tilbaestaaendeBeloeb | modtagelsesdato |
      | FDR-58013  | 3000.00              | 2024-01-10      |
    When a PublicDisbursementEvent arrives for debtor "SKY-5801" with disbursementAmount 2000.00 DKK and nemkontoReferenceId "NKR-5801-001"
    And the ModregningService processes the event
    Then fordring "FDR-58011" is fully covered with 1200.00 DKK from tier-1
    And fordring "FDR-58012" is fully covered with 800.00 DKK from tier-1
    And fordring "FDR-58013" receives no dækning
    And the ModregningEvent for "NKR-5801-001" has tier1Amount 2000.00 DKK
    And the ModregningEvent for "NKR-5801-001" has tier2Amount 0.00 DKK
    And the ModregningEvent for "NKR-5801-001" has tier3Amount 0.00 DKK
    And DaekningsRaekkefoeigenService is not called for this event
    And a SET_OFF CollectionMeasureEntity referencing this ModregningEvent exists for "FDR-58011"
    And a SET_OFF CollectionMeasureEntity referencing this ModregningEvent exists for "FDR-58012"
    And the ModregningEvent has renteGodtgoerelseNonTaxable set to true

  # ==============================================================================
  # FR-1: Tier-2 full coverage — RIM offsets using dækningsrækkefølge
  # AC-1 (tier-2 only path), AC-4
  # ==============================================================================

  Scenario: Tier-2 fuld dækning — RIM dækker alle fordringer under inddrivelse
    Given debtor "SKY-5802" has no tier-1 fordringer
    And debtor "SKY-5802" has the following tier-2 fordringer under RIM inddrivelse:
      | fordringId | tilbaestaaendeBeloeb | modtagelsesdato |
      | FDR-58021  | 2000.00              | 2024-01-05      |
      | FDR-58022  | 1500.00              | 2024-03-01      |
    And debtor "SKY-5802" has no tier-3 fordringer
    When a PublicDisbursementEvent arrives for debtor "SKY-5802" with disbursementAmount 3500.00 DKK and nemkontoReferenceId "NKR-5802-001"
    And the ModregningService processes the event
    Then fordring "FDR-58021" is fully covered with 2000.00 DKK from tier-2
    And fordring "FDR-58022" is fully covered with 1500.00 DKK from tier-2
    And the ModregningEvent for "NKR-5802-001" has tier2Amount 3500.00 DKK
    And the ModregningEvent for "NKR-5802-001" has residualPayoutAmount 0.00 DKK
    And a SET_OFF CollectionMeasureEntity referencing this ModregningEvent exists for "FDR-58021"
    And a SET_OFF CollectionMeasureEntity referencing this ModregningEvent exists for "FDR-58022"
    And the ModregningEvent has renteGodtgoerelseNonTaxable set to true

  # ==============================================================================
  # FR-1: Tier-2 partial coverage — P057 DaekningsRaekkefoeigenService applied
  # AC-3
  # ==============================================================================

  Scenario: Tier-2 delvis dækning — P057 dækningsrækkefølge delegeres ved utilstrækkelig betaling
    Given debtor "SKY-5803" has no tier-1 fordringer
    And debtor "SKY-5803" has the following tier-2 fordringer under RIM inddrivelse:
      | fordringId | tilbaestaaendeBeloeb | modtagelsesdato |
      | FDR-58031  | 3000.00              | 2024-02-01      |
      | FDR-58032  | 2500.00              | 2024-04-10      |
    And debtor "SKY-5803" has no tier-3 fordringer
    When a PublicDisbursementEvent arrives for debtor "SKY-5803" with disbursementAmount 1800.00 DKK and nemkontoReferenceId "NKR-5803-001"
    And the ModregningService processes the event
    Then DaekningsRaekkefoeigenService is called once with residualAmount 1800.00 DKK and debtorPersonId for "SKY-5803"
    And fordring "FDR-58031" (oldest modtagelsesdato 2024-02-01) is partially covered with 1800.00 DKK per P057 ordering
    And fordring "FDR-58032" receives no dækning in this event
    And the ModregningEvent for "NKR-5803-001" has tier2Amount 1800.00 DKK
    And a SET_OFF CollectionMeasureEntity referencing this ModregningEvent exists for "FDR-58031"
    And the ModregningEvent has renteGodtgoerelseNonTaxable set to true

  # ==============================================================================
  # FR-1: Tier-3 remainder coverage
  # AC-1 (tier-3 path)
  # ==============================================================================

  Scenario: Tier-3 restdækning — restbeløb dækker andre fordringer efter tier-2
    Given debtor "SKY-5804" has no tier-1 fordringer
    And debtor "SKY-5804" has the following tier-2 fordringer under RIM inddrivelse:
      | fordringId | tilbaestaaendeBeloeb | modtagelsesdato |
      | FDR-58041  | 2000.00              | 2024-01-15      |
    And debtor "SKY-5804" has the following tier-3 fordringer in registration order:
      | fordringId | tilbaestaaendeBeloeb | registreringsdato |
      | FDR-58042  | 1000.00              | 2024-06-01        |
      | FDR-58043  | 800.00               | 2024-07-01        |
    When a PublicDisbursementEvent arrives for debtor "SKY-5804" with disbursementAmount 3200.00 DKK and nemkontoReferenceId "NKR-5804-001"
    And the ModregningService processes the event
    Then fordring "FDR-58041" is fully covered with 2000.00 DKK from tier-2
    And fordring "FDR-58042" is fully covered with 1000.00 DKK from tier-3
    And fordring "FDR-58043" is partially covered with 200.00 DKK from tier-3
    And the ModregningEvent for "NKR-5804-001" has tier2Amount 2000.00 DKK
    And the ModregningEvent for "NKR-5804-001" has tier3Amount 1200.00 DKK
    And the ModregningEvent has renteGodtgoerelseNonTaxable set to true

  # ==============================================================================
  # FR-1: Mixed tiers in a single disbursement event
  # AC-1
  # ==============================================================================

  Scenario: Blandet tre-tier modregning — udbetaling dækker alle tre tiers og giver restbetaling
    Given debtor "SKY-5805" has the following tier-1 fordringer registered by the paying authority:
      | fordringId | tilbaestaaendeBeloeb | registreringsdato |
      | FDR-58051  | 3000.00              | 2024-10-01        |
    And debtor "SKY-5805" has the following tier-2 fordringer under RIM inddrivelse:
      | fordringId | tilbaestaaendeBeloeb | modtagelsesdato |
      | FDR-58052  | 5000.00              | 2024-01-01      |
    And debtor "SKY-5805" has the following tier-3 fordringer in registration order:
      | fordringId | tilbaestaaendeBeloeb | registreringsdato |
      | FDR-58053  | 4000.00              | 2024-05-01        |
    When a PublicDisbursementEvent arrives for debtor "SKY-5805" with disbursementAmount 10000.00 DKK and nemkontoReferenceId "NKR-5805-001"
    And the ModregningService processes the event
    Then the ModregningEvent for "NKR-5805-001" has tier1Amount 3000.00 DKK
    And the ModregningEvent for "NKR-5805-001" has tier2Amount 5000.00 DKK
    And the ModregningEvent for "NKR-5805-001" has tier3Amount 2000.00 DKK
    And the ModregningEvent for "NKR-5805-001" has residualPayoutAmount 0.00 DKK
    And fordring "FDR-58051" is fully covered with 3000.00 DKK from tier-1
    And fordring "FDR-58052" is fully covered with 5000.00 DKK from tier-2
    And fordring "FDR-58053" is partially covered with 2000.00 DKK from tier-3
    And the ModregningEvent has renteGodtgoerelseNonTaxable set to true

  # ==============================================================================
  # FR-1: Idempotency — duplicate nemkontoReferenceId
  # AC-5
  # ==============================================================================

  Scenario: Idempotent genbehandling — duplikat nemkontoReferenceId opretter ikke nyt event
    Given a PublicDisbursementEvent with nemkontoReferenceId "NKR-5806-001" has already been processed for debtor "SKY-5806"
    And a ModregningEvent exists for "NKR-5806-001"
    When the same PublicDisbursementEvent with nemkontoReferenceId "NKR-5806-001" arrives again
    And the ModregningService processes the event
    Then no new ModregningEvent is created for "NKR-5806-001"
    And the system returns a reference to the existing ModregningEvent
    And no additional SET_OFF CollectionMeasureEntity is created
    And the ModregningEvent has renteGodtgoerelseNonTaxable set to true

  # ==============================================================================
  # FR-4: 5-banking-day exception — no rentegodtgørelse
  # AC-12
  # ==============================================================================

  Scenario: 5-bankdags-undtagelse — ingen rentegodtgørelse når beslutning inden 5 bankdage
    Given debtor "SKY-5807" has one active tier-2 fordring with tilbaestaaendeBeloeb 5000.00 DKK
    And a PublicDisbursementEvent arrives for debtor "SKY-5807" with receiptDate "2025-03-10"
    And the modregning decision is made on "2025-03-13" which is 3 banking days after receiptDate
    When the ModregningService processes the event
    Then the ModregningEvent has renteGodtgoerelseAccrued 0.00 DKK
    And the ModregningEvent has no renteGodtgoerelseStartDate recorded
    And the ModregningEvent has renteGodtgoerelseNonTaxable set to true

  # ==============================================================================
  # FR-4: Kildeskattelov § 62 special start date for income tax refund
  # AC-13
  # ==============================================================================

  Scenario: Kildeskattelov § 62 særlig startdato — overskydende skat starter ikke før 1. september år efter indkomstår
    Given debtor "SKY-5808" has one active tier-2 fordring with tilbaestaaendeBeloeb 8000.00 DKK
    And a PublicDisbursementEvent arrives for debtor "SKY-5808" with:
      | field              | value              |
      | paymentType        | OVERSKYDENDE_SKAT  |
      | indkomstAar        | 2024               |
      | receiptDate        | 2025-04-01         |
      | disbursementAmount | 8000.00            |
      | nemkontoReferenceId | NKR-5808-001      |
    When the ModregningService processes the event
    Then the ModregningEvent for "NKR-5808-001" has renteGodtgoerelseStartDate "2025-09-01"
    And the standard start date "2025-05-01" (1st of month after receipt) is not used
    And the ModregningEvent has renteGodtgoerelseNonTaxable set to true

  # ==============================================================================
  # FR-3 + FR-4: Børne-og-ungeydelse restriction preserved after korrektionspulje settlement
  # AC-11
  # ==============================================================================

  Scenario: Børne-og-ungeydelse begrænsning bevaret efter korrektionspulje-afregning
    Given debtor "SKY-5809" had a fordring "FDR-58091" previously offset by modregning
    And the original disbursement had paymentType "BOERNE_OG_UNGEYDELSE"
    And fordring "FDR-58091" has been written down generating an OffsettingReversalEvent with surplusAmount 200.00 DKK
    And gendækning exhausts 0.00 DKK of the surplus (no eligible fordringer exist)
    And a KorrektionspuljeEntry exists with surplusAmount 200.00 DKK and boerneYdelseRestriction true
    When the KorrektionspuljeSettlementJob runs its monthly settlement for debtor "SKY-5809"
    Then the settled amount of 200.00 DKK is NOT treated as an unrestricted Nemkonto payment
    And the boerneYdelseRestriction flag is true on the settled amount
    And the børne-og-ungeydelse modregning restrictions apply to the re-applied amount
    And the ModregningEvent has renteGodtgoerelseNonTaxable set to true

  # ==============================================================================
  # FR-3: Gendækning (re-coverage) after fordring write-down
  # AC-8
  # ==============================================================================

  Scenario: Gendækning efter fordring-nedskrivning — surplus dækker anden fordring via P057
    Given debtor "SKY-5810" had fordring "FDR-58101" with tilbaestaaendeBeloeb 3000.00 DKK previously offset
    And fordring "FDR-58101" is written down generating an OffsettingReversalEvent with surplusAmount 1500.00 DKK
    And fordring "FDR-58101" still has an uncovered portion of 200.00 DKK (renter)
    And debtor "SKY-5810" has another tier-2 fordring "FDR-58102" with tilbaestaaendeBeloeb 1000.00 DKK
    When the OffsettingReversalEventConsumer processes the reversal event
    Then Step 1: 200.00 DKK is applied to fordring "FDR-58101" uncovered renter portion
    And Step 2: DaekningsRaekkefoeigenService is called with remaining surplus 1300.00 DKK for gendækning
    And fordring "FDR-58102" is gendækket with 1000.00 DKK
    And a KorrektionspuljeEntry is created with surplusAmount 300.00 DKK
    And no Digital Post notice is sent for the gendækning
    And the ModregningEvent has renteGodtgoerelseNonTaxable set to true

  # ==============================================================================
  # FR-3: Korrektionspulje threshold — < 50 DKK skips monthly settlement
  # AC-9
  # ==============================================================================

  Scenario: Korrektionspulje beløb under 50 DKK — ingen månedlig afregning, kun årsafregning
    Given debtor "SKY-5811" has a KorrektionspuljeEntry with:
      | field                       | value      |
      | surplusAmount               | 45.00      |
      | correctionPoolTarget        | PSRM       |
      | boerneYdelseRestriction     | false      |
      | renteGodtgoerelseStartDate  | 2025-02-01 |
    When the KorrektionspuljeSettlementJob runs its monthly settlement
    Then the KorrektionspuljeEntry for debtor "SKY-5811" is NOT settled
    And the entry is marked for annual-only settlement
    And no new ModregningEvent is created from this pool entry in the monthly run
    And the ModregningEvent has renteGodtgoerelseNonTaxable set to true

  # ==============================================================================
  # FR-3: Korrektionspulje monthly settlement → re-applied as new Nemkonto payment
  # AC-10
  # ==============================================================================

  Scenario: Korrektionspulje månedlig afregning — surplus behandles som ny selvstændig Nemkonto-udbetaling
    Given debtor "SKY-5812" has a KorrektionspuljeEntry with:
      | field                       | value      |
      | surplusAmount               | 750.00     |
      | correctionPoolTarget        | PSRM       |
      | boerneYdelseRestriction     | false      |
      | renteGodtgoerelseAccrued    | 12.50      |
      | renteGodtgoerelseStartDate  | 2025-01-01 |
    And debtor "SKY-5812" has an active tier-2 fordring "FDR-58121" with tilbaestaaendeBeloeb 900.00 DKK
    When the KorrektionspuljeSettlementJob runs its monthly settlement
    Then the KorrektionspuljeEntry is settled with total amount 762.50 DKK (surplus + rentegodtgørelse)
    And a new PublicDisbursementEvent equivalent is created with disbursementAmount 762.50 DKK for debtor "SKY-5812"
    And the FR-1 three-tier modregning workflow is invoked for the settled amount
    And fordring "FDR-58121" receives dækning from the settled amount without transporter restrictions from the original payment
    And the KorrektionspuljeEntry is marked as settled
    And the ModregningEvent has renteGodtgoerelseNonTaxable set to true

  # ==============================================================================
  # FR-5: Klage deadline recorded on modregning event
  # AC-15, AC-16, AC-17
  # ==============================================================================

  Scenario: Klage-deadline registreres korrekt — underretning leveret og ikke leveret
    Given debtor "SKY-5813" has one active tier-2 fordring with tilbaestaaendeBeloeb 2000.00 DKK
    When a PublicDisbursementEvent with disbursementAmount 2000.00 DKK is processed for debtor "SKY-5813" with nemkontoReferenceId "NKR-5813-001"

    # Case A: notice delivered on 2025-03-15 → klage deadline = 2025-06-15
    And the Digital Post notice for "NKR-5813-001" is delivered on "2025-03-15"
    Then the ModregningEvent for "NKR-5813-001" has noticeDelivered true
    And the ModregningEvent for "NKR-5813-001" has klageFristDato "2025-06-15"

    # Case B: notice delivery fails for "NKR-5813-002" (same debtor, different event)
    When a second PublicDisbursementEvent is processed for debtor "SKY-5813" with nemkontoReferenceId "NKR-5813-002" and decisionDate "2025-03-15"
    And the Digital Post notice for "NKR-5813-002" cannot be delivered
    Then the ModregningEvent for "NKR-5813-002" has noticeDelivered false
    And the ModregningEvent for "NKR-5813-002" has klageFristDato "2026-03-15"

    # Read model
    When a caseworker calls GET /debtors/SKY-5813/modregning-events
    Then the response contains both modregning events with their klageFristDato values
    And each event in the response includes fields: eventId, decisionDate, totalOffsetAmount, tier1Amount, tier2Amount, tier3Amount, residualPayoutAmount, klageFristDato, noticeDelivered, tier2WaiverApplied

    # Portal highlighting
    And the caseworker portal displays the event for "NKR-5813-001" with an amber indicator if klageFristDato is within 14 days
    And the ModregningEvent has renteGodtgoerelseNonTaxable set to true

  # ==============================================================================
  # FR-2: Waiver of tier-2 by caseworker (GIL § 4, stk. 11)
  # AC-6, AC-7
  # ==============================================================================

  Scenario: Sagsbehandler fravælger tier-2 prioritet — GIL § 4 stk. 11 fravalg
    Given debtor "SKY-5814" has the following tier-1 fordringer registered by the paying authority:
      | fordringId | tilbaestaaendeBeloeb | registreringsdato |
      | FDR-58141  | 1000.00              | 2024-12-01        |
    And debtor "SKY-5814" has the following tier-2 fordringer under RIM inddrivelse:
      | fordringId | tilbaestaaendeBeloeb | modtagelsesdato |
      | FDR-58142  | 3000.00              | 2024-01-01      |
    And a ModregningEvent "EVT-5814-001" exists for debtor "SKY-5814" with tier2WaiverApplied false
    And caseworker "CSW-001" holds OAuth2 scope "modregning:waiver"
    When caseworker "CSW-001" calls POST /debtors/SKY-5814/modregning-events/EVT-5814-001/tier2-waiver with waiverReason "Særlige omstændigheder godkendt af leder"
    Then the ModregningEvent "EVT-5814-001" has tier2WaiverApplied set to true
    And the three-tier ordering engine re-runs for "EVT-5814-001" skipping tier-2
    And fordring "FDR-58142" receives no dækning in the re-processed event
    And each SET_OFF CollectionMeasureEntity for this event has waiverApplied set to true
    And the CLS audit log contains an entry with:
      | field             | value                |
      | gilParagraf       | GIL § 4, stk. 11    |
      | modregningEventId | EVT-5814-001         |
      | caseworkerId      | CSW-001              |
      | waiverReason      | Særlige omstændigheder godkendt af leder |
    And the HTTP response status is 200
    And the ModregningEvent has renteGodtgoerelseNonTaxable set to true

  Scenario: Manglende waiver-scope giver 403 — uautoriseret forsøg på tier-2 fravalg
    Given a ModregningEvent "EVT-5815-001" exists for debtor "SKY-5815"
    And caller "APP-CLIENT-001" does NOT hold OAuth2 scope "modregning:waiver"
    When "APP-CLIENT-001" calls POST /debtors/SKY-5815/modregning-events/EVT-5815-001/tier2-waiver
    Then the HTTP response status is 403
    And the ModregningEvent "EVT-5815-001" has tier2WaiverApplied unchanged as false
    And no CLS audit log entry is created for this request
    And the ModregningEvent has renteGodtgoerelseNonTaxable set to true

  # ==============================================================================
  # FR-4: Standard rentegodtgørelse start date — decision after 5 banking days
  # (implicit AC-14: renteGodtgoerelseNonTaxable always true)
  # ==============================================================================

  Scenario: Standard rentegodtgørelse startdato — beslutning efter 5 bankdage
    Given debtor "SKY-5816" has one active tier-2 fordring with tilbaestaaendeBeloeb 6000.00 DKK
    And a PublicDisbursementEvent arrives for debtor "SKY-5816" with receiptDate "2025-03-10"
    And the modregning decision is made on "2025-03-24" which is 10 banking days after receiptDate
    When the ModregningService processes the event
    Then the ModregningEvent has renteGodtgoerelseStartDate "2025-04-01"
      # 1st of month AFTER receiptDate 2025-03-10 = 2025-04-01 (GIL § 8b)
    And the ModregningEvent has renteGodtgoerelseNonTaxable set to true
    And the renteGodtgørelse rate is 5.0 percent
      # 9.0% (rentelov.refRate) MINUS 4.0 percentage points = 5.0%

  # ==============================================================================
  # FR-1 + FR-2 + FR-4: Mixed scenario — SET_OFF CollectionMeasure per covered fordring
  # AC-4
  # ==============================================================================

  Scenario: SET_OFF measure oprettes per dækket fordring med reference til ModregningEvent
    Given debtor "SKY-5817" has the following tier-2 fordringer under RIM inddrivelse:
      | fordringId | tilbaestaaendeBeloeb | modtagelsesdato |
      | FDR-58171  | 1000.00              | 2024-02-01      |
      | FDR-58172  | 500.00               | 2024-04-01      |
    When a PublicDisbursementEvent arrives for debtor "SKY-5817" with disbursementAmount 1500.00 DKK and nemkontoReferenceId "NKR-5817-001"
    And the ModregningService processes the event
    Then a ModregningEvent "EVT-5817-001" is persisted for "NKR-5817-001"
    And a SET_OFF CollectionMeasureEntity with measureType "SET_OFF" and modregningEventId "EVT-5817-001" exists for fordring "FDR-58171"
    And a SET_OFF CollectionMeasureEntity with measureType "SET_OFF" and modregningEventId "EVT-5817-001" exists for fordring "FDR-58172"
    And the CLS audit log contains entries for "FDR-58171" and "FDR-58172" with gilParagraf "GIL § 7, stk. 1, nr. 2"
    And the ModregningEvent has renteGodtgoerelseNonTaxable set to true

  # ==============================================================================
  # FR-3.2(b): Gendækning opt-out scenarios
  # ==============================================================================

  Scenario: Gendækning springes over ved DMI-target korrektionspulje
    # FR-3.2(b)(a): correctionPoolTarget = DMI → gendækning skipped
    Given debtor "SKY-5820" has a korrektionspulje entry with surplus 500.00 DKK
    And the korrektionspulje entry has correctionPoolTarget "DMI"
    And debtor "SKY-5820" has an uncovered fordring "FDR-58201" with 300.00 DKK
    When the korrektionspulje settlement process runs for debtor "SKY-5820"
    Then gendækning is skipped for debtor "SKY-5820"
    And fordring "FDR-58201" receives no gendækning coverage

  Scenario: Gendækning springes over ved modregningssurplus fra inkasso
    # FR-3.2(b)(b): surplus from offsetting against debt-under-collection → gendækning skipped
    Given debtor "SKY-5821" has a korrektionspulje entry with surplus 200.00 DKK
    And the surplus originated from modregning against a debt-under-collection inddrivelsesindsats
    And debtor "SKY-5821" has an uncovered fordring "FDR-58211" with 150.00 DKK
    When the korrektionspulje settlement process runs for debtor "SKY-5821"
    Then gendækning is skipped for debtor "SKY-5821"

  Scenario: Gendækning springes over for retroaktivt delvist dækkede fordringer
    # FR-3.2(b)(c): retroactively partially covered fordring → gendækning skipped
    Given debtor "SKY-5822" has a korrektionspulje entry with surplus 400.00 DKK
    And fordring "FDR-58221" was retroactively partially covered with 100.00 DKK
    When the korrektionspulje settlement process runs for debtor "SKY-5822"
    Then gendækning is skipped for fordring "FDR-58221"

  # ==============================================================================
  # FR-4.1: Rate-change effective-date delay — new rate not used until 5 banking days after publication
  # ==============================================================================

  Scenario: Ny rentesats træder i kraft efter 5 bankdage
    # FR-4.1: Rate-change effective-date — new rate not used until 5 banking days after publication
    Given a RenteGodtgoerelseRateEntry with referenceRatePercent 9.0 effective "2024-01-01"
    And a new RenteGodtgoerelseRateEntry with referenceRatePercent 11.0 published "2024-06-10" effective "2024-06-17"
    And today is "2024-06-12"
    When the rentegodtgoerelse rate is computed for reference date "2024-06-12"
    Then the applied rate is 5.0 percent (9.0 minus 4.0 using the prior entry)
    And the new rate of 7.0 percent (11.0 minus 4.0) is not yet in effect
