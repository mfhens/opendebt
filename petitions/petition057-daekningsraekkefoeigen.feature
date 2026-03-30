@petition057
Feature: Dækningsrækkefølge — GIL § 4 payment application order (G.A.2.3.2)

  # Legal basis: GIL § 4 stk. 1–4, GIL § 6a stk. 1 og stk. 12, GIL § 9 stk. 1 og stk. 3,
  # GIL § 10b, Gæld.bekendtg. § 4 stk. 3, Retsplejelovens § 507, Lov nr. 288/2022
  # G.A. snapshot: v3.16 (2026-03-28)
  # Out of scope: DMI paralleldrift rules (GIL § 49), Catala encoding (P069),
  #   pro-rata joint-debtor distribution (P062), forældelsesfrist interruption (P059).
  # AC-17 (i18n bundle coverage) is verified by CI bundle-lint tests, not by Gherkin scenarios.

  Background:
    Given the payment-service rule engine is active
    And the sagsbehandler portal is running

  # ==============================================================================
  # FR-1: Priority categories — GIL § 4, stk. 1
  # AC-1, AC-2
  # ==============================================================================

  Scenario: Prioritetsrækkefølge — bøder dækkes før underholdsbidrag og andre fordringer
    Given debtor "SKY-3001" has the following active fordringer:
      | fordringId | kategori                              | tilbaestaaendeBeloeb | modtagelsesdato |
      | FDR-30011  | ANDRE_FORDRINGER                      | 800.00               | 2024-01-15      |
      | FDR-30012  | UNDERHOLDSBIDRAG_PRIVATRETLIG         | 400.00               | 2024-02-01      |
      | FDR-30013  | BOEDER_TVANGSBOEEDER_TILBAGEBETALING  | 300.00               | 2024-03-10      |
    When a payment of 500.00 DKK is received for debtor "SKY-3001" with betalingstidspunkt "2025-01-10T14:00:00Z"
    And the dækningsrækkefølge rule engine applies the payment
    Then fordring "FDR-30013" (bøder, kategori 2) is fully covered with 300.00 DKK
    And fordring "FDR-30012" (underholdsbidrag, kategori 3) is covered with 200.00 DKK
    And fordring "FDR-30011" (andre fordringer, kategori 4) receives no dækning
    And the dækning record for "FDR-30013" carries gilParagraf "GIL § 4, stk. 1, nr. 2"
    And the dækning record for "FDR-30012" carries gilParagraf "GIL § 4, stk. 1, nr. 3"

  Scenario: Prioritetsrækkefølge — alle fire kategorier — kun kategori 1 dækkes ved lille betaling
    Given debtor "SKY-3002" has the following active fordringer:
      | fordringId | kategori                              | tilbaestaaendeBeloeb | modtagelsesdato |
      | FDR-30021  | RIMELIGE_OMKOSTNINGER                 | 200.00               | 2024-01-05      |
      | FDR-30022  | BOEDER_TVANGSBOEEDER_TILBAGEBETALING  | 500.00               | 2024-01-10      |
      | FDR-30023  | UNDERHOLDSBIDRAG_OFFENTLIG            | 300.00               | 2024-02-01      |
      | FDR-30024  | ANDRE_FORDRINGER                      | 1000.00              | 2024-03-01      |
    When a payment of 150.00 DKK is received for debtor "SKY-3002"
    And the dækningsrækkefølge rule engine applies the payment
    Then fordring "FDR-30021" (kategori 1) is partially covered with 150.00 DKK
    And fordringer "FDR-30022", "FDR-30023", and "FDR-30024" receive no dækning

  Scenario: Tvangsbøder klassificeres som kategori 2 (GIL § 10b, lov nr. 288/2022)
    Given debtor "SKY-3003" has the following active fordringer:
      | fordringId | kategori                              | tilbaestaaendeBeloeb | modtagelsesdato |
      | FDR-30031  | BOEDER_TVANGSBOEEDER_TILBAGEBETALING  | 600.00               | 2024-06-01      |
      | FDR-30032  | ANDRE_FORDRINGER                      | 400.00               | 2024-01-01      |
    And fordring "FDR-30031" has fordringType "TVANGSBOEEDE"
    When a payment of 300.00 DKK is received for debtor "SKY-3003"
    And the dækningsrækkefølge rule engine applies the payment
    Then fordring "FDR-30031" (tvangsbøde, kategori 2) is partially covered with 300.00 DKK
    And fordring "FDR-30032" (kategori 4) receives no dækning
    And the dækning record for "FDR-30031" carries prioritetKategori "BOEDER_TVANGSBOEEDER_TILBAGEBETALING"

  Scenario: Privatretlige underholdsbidrag dækkes før offentlige inden for kategori 3
    Given debtor "SKY-3004" has the following active fordringer:
      | fordringId | kategori                      | tilbaestaaendeBeloeb | modtagelsesdato |
      | FDR-30041  | UNDERHOLDSBIDRAG_OFFENTLIG    | 500.00               | 2024-01-01      |
      | FDR-30042  | UNDERHOLDSBIDRAG_PRIVATRETLIG | 300.00               | 2024-06-01      |
    When a payment of 400.00 DKK is received for debtor "SKY-3004"
    And the dækningsrækkefølge rule engine applies the payment
    Then fordring "FDR-30042" (privatretligt underholdsbidrag) is fully covered with 300.00 DKK
    And fordring "FDR-30041" (offentligt underholdsbidrag) is covered with 100.00 DKK

  # ==============================================================================
  # FR-2: Within-category FIFO ordering — GIL § 4, stk. 2
  # AC-3, AC-4
  # ==============================================================================

  Scenario: FIFO inden for kategori — ældst modtagen fordring dækkes først
    Given debtor "SKY-3005" has the following active fordringer in the same priority category:
      | fordringId | kategori         | tilbaestaaendeBeloeb | modtagelsesdato |
      | FDR-30051  | ANDRE_FORDRINGER | 600.00               | 2024-03-01      |
      | FDR-30052  | ANDRE_FORDRINGER | 400.00               | 2024-01-15      |
      | FDR-30053  | ANDRE_FORDRINGER | 300.00               | 2024-06-10      |
    When a payment of 700.00 DKK is received for debtor "SKY-3005"
    And the dækningsrækkefølge rule engine applies the payment
    Then fordring "FDR-30052" (earliest modtagelsesdato 2024-01-15) is fully covered with 400.00 DKK
    And fordring "FDR-30051" (modtagelsesdato 2024-03-01) is partially covered with 300.00 DKK
    And fordring "FDR-30053" (latest modtagelsesdato 2024-06-10) receives no dækning

  Scenario: Pre-2013 fordring — legacyModtagelsesdato bruges som FIFO-nøgle
    Given debtor "SKY-3006" has the following active fordringer:
      # Note: modtagelsesdato is the overdragelse API timestamp ("opret" date).
      # FDR-30062 has no legacyModtagelsesdato (received after the 2013-09-01 migration cutoff).
      | fordringId | kategori         | tilbaestaaendeBeloeb | modtagelsesdato | legacyModtagelsesdato |
      | FDR-30061  | ANDRE_FORDRINGER | 500.00               | 2024-05-01      | 2012-08-15            |
      | FDR-30062  | ANDRE_FORDRINGER | 400.00               | 2024-04-01      |                       |
    And fordring "FDR-30061" has a legacyModtagelsesdato of "2012-08-15" (before 1 September 2013)
    When a payment of 600.00 DKK is received for debtor "SKY-3006"
    And the dækningsrækkefølge rule engine applies the payment
    Then fordring "FDR-30061" is covered first using legacyModtagelsesdato "2012-08-15" as the sort key
    And fordring "FDR-30062" is covered second using its overdragelse modtagelsesdato
    And the API response for "FDR-30061" contains fifoSortKey "2012-08-15"

  @petition057
  Scenario: SKY-3027 Uafgjort FIFO: samme modtagelsesdato — laveste sekvensnummer dækkes først
    Given debtor "SKY-3027" has the following active fordringer in the same priority category:
      | fordringId | kategori         | tilbaestaaendeBeloeb | modtagelsesdato | sekvensNummer |
      | FDR-30271  | ANDRE_FORDRINGER | 500.00               | 2024-01-15      | 1001          |
      | FDR-30272  | ANDRE_FORDRINGER | 400.00               | 2024-01-15      | 1002          |
    When a payment of 300.00 DKK is received for debtor "SKY-3027"
    And the dækningsrækkefølge rule engine applies the payment
    Then fordring "FDR-30271" (sekvensNummer 1001) is partially covered with 300.00 DKK
    And fordring "FDR-30272" (sekvensNummer 1002) receives no dækning

  # ==============================================================================
  # FR-3: Interest ordering within each fordring — Gæld.bekendtg. § 4, stk. 3
  # AC-5, AC-6
  # ==============================================================================

  Scenario: Renter dækkes før Hovedfordring ved delvis betaling
    Given debtor "SKY-3007" has fordring "FDR-30071" with the following outstanding components:
      | komponent                | beloeb |
      | OPKRAEVNINGSRENTER       | 50.00  |
      | INDDRIVELSESRENTER_STK1  | 30.00  |
      | OEVRIGE_RENTER_PSRM      | 20.00  |
      | HOVEDFORDRING            | 500.00 |
    When a payment of 80.00 DKK is applied to fordring "FDR-30071"
    Then opkrævningsrenter (50.00) are fully covered first
    And inddrivelsesrenter_stk1 (30.00) are fully covered second
    And Hovedfordring receives no dækning
    And no dækning record has komponent "HOVEDFORDRING" with beloeb > 0

  Scenario: Fuld rentesekvens — alle seks under-positioner dækkes i korrekt rækkefølge
    Given debtor "SKY-3008" has fordring "FDR-30081" with all six cost components outstanding:
      | sub-position | komponent                                  | beloeb |
      | 1            | OPKRAEVNINGSRENTER                         | 10.00  |
      | 2            | INDDRIVELSESRENTER_FORDRINGSHAVER_STK3     | 15.00  |
      | 3            | INDDRIVELSESRENTER_FOER_TILBAGEFOERSEL     | 20.00  |
      | 4            | INDDRIVELSESRENTER_STK1                    | 25.00  |
      | 5            | OEVRIGE_RENTER_PSRM                        | 30.00  |
      | 6            | HOVEDFORDRING                              | 800.00 |
    When a payment of 110.00 DKK is applied to fordring "FDR-30081"
    Then sub-positions 1 through 5 are fully covered in ascending order (total 100.00 DKK)
    And the Hovedfordring receives 10.00 DKK dækning (the remaining amount after renter)
    And the line-item allocation records are ordered by sub-position 1 → 2 → 3 → 4 → 5 → 6

  Scenario: Tidligere renteperiode dækkes inden for samme sub-position
    Given debtor "SKY-3009" has fordring "FDR-30091" with two INDDRIVELSESRENTER_STK1 periods:
      | periode   | beloeb |
      | 2023-Q1   | 40.00  |
      | 2023-Q2   | 60.00  |
    When a payment of 50.00 DKK reaches fordring "FDR-30091" after opkrævningsrenter are covered
    Then the 2023-Q1 inddrivelsesrente period (40.00) is fully covered first
    And the 2023-Q2 period is partially covered with 10.00 DKK

  # ==============================================================================
  # FR-4: Inddrivelsesindsats rule — GIL § 4, stk. 3
  # AC-7, AC-8
  # ==============================================================================

  Scenario: Lønindeholdelse-betaling dækker indsats-fordringer først — surplus til øvrige lønindeholdelses-fordringer
    Given debtor "SKY-3010" has the following active fordringer:
      # FDR-30102: not in this indsats (false) but eligible to receive lønindeholdelse surplus.
      # FDR-30103: not in this indsats (false) and not eligible for lønindeholdelse surplus.
      | fordringId | kategori         | tilbaestaaendeBeloeb | inLoenindeholdelsesIndsats |
      | FDR-30101  | ANDRE_FORDRINGER | 200.00               | true                       |
      | FDR-30102  | ANDRE_FORDRINGER | 300.00               | false                      |
      | FDR-30103  | ANDRE_FORDRINGER | 400.00               | false                      |
    When a lønindeholdelse payment of 450.00 DKK is received with inddrivelsesindsatsType "LOENINDEHOLDELSE"
    And the dækningsrækkefølge rule engine applies the payment
    Then fordring "FDR-30101" (indsats-fordring) is fully covered with 200.00 DKK first
    And surplus 250.00 DKK is applied to fordring "FDR-30102" (same-type-eligible)
    And fordring "FDR-30103" (not loenindeholdelse-eligible) receives no dækning from this payment
    And each dækning record carries gilParagraf "GIL § 4, stk. 3"

  Scenario: Udlæg-undtagelse — surplus fra udlæg-betaling flyder ikke til andre fordringer
    Given debtor "SKY-3011" has the following active fordringer:
      | fordringId | kategori         | tilbaestaaendeBeloeb | inUdlaegForretning |
      | FDR-30111  | ANDRE_FORDRINGER | 300.00               | true               |
      | FDR-30112  | ANDRE_FORDRINGER | 500.00               | false              |
    When an udlæg payment of 500.00 DKK is received with inddrivelsesindsatsType "UDLAEG"
    And the dækningsrækkefølge rule engine applies the payment
    Then fordring "FDR-30111" (udlæg-fordring) is fully covered with 300.00 DKK
    And fordring "FDR-30112" (non-udlæg) receives no dækning
    And the remaining 200.00 DKK surplus is flagged as udlaegSurplus = true
    And no dækning record exists for fordring "FDR-30112"
    And the dækning record for "FDR-30111" carries gilParagraf "GIL § 4, stk. 3"

  # ==============================================================================
  # FR-5: Opskrivningsfordring positioning
  # AC-9, AC-10, AC-11
  # ==============================================================================

  Scenario: Opskrivningsfordring placeres umiddelbart efter sin stamfordring
    Given debtor "SKY-3013" has the following active fordringer:
      # opskrivningAfFordringId is "" (empty string) for fordringer that are not opskrivningsfordringer.
      | fordringId | kategori         | tilbaestaaendeBeloeb | modtagelsesdato | opskrivningAfFordringId |
      | FDR-30131  | ANDRE_FORDRINGER | 400.00               | 2024-01-10      | ""                      |
      | FDR-30132  | ANDRE_FORDRINGER | 200.00               | 2024-01-10      | FDR-30131               |
      | FDR-30133  | ANDRE_FORDRINGER | 300.00               | 2024-02-01      | ""                      |
    When the dækningsrækkefølge ordered list is retrieved for debtor "SKY-3013"
    Then the ordered list is:
      | rank | fordringId | note                              |
      | 1    | FDR-30131  | stamfordring (FIFO 2024-01-10)    |
      | 2    | FDR-30132  | opskrivningsfordring after parent |
      | 3    | FDR-30133  | independent (FIFO 2024-02-01)     |
    And the entry for "FDR-30132" carries opskrivningAfFordringId "FDR-30131"

  Scenario: Opskrivningsfordring placeres korrekt når stamfordring allerede er fuldt dækket
    Given debtor "SKY-3014" has the following fordringer:
      # FDR-30141 is fully covered (tilbaestaaendeBeloeb = 0.00); opskrivningAfFordringId "" = not an opskrivningsfordring.
      | fordringId | kategori         | tilbaestaaendeBeloeb | modtagelsesdato | opskrivningAfFordringId |
      | FDR-30141  | ANDRE_FORDRINGER | 0.00                 | 2024-01-10      | ""                      |
      | FDR-30142  | ANDRE_FORDRINGER | 150.00               | 2024-01-10      | FDR-30141               |
      | FDR-30143  | ANDRE_FORDRINGER | 500.00               | 2024-02-01      | ""                      |
    When the dækningsrækkefølge ordered list is retrieved for debtor "SKY-3014"
    Then the ordered list includes "FDR-30142" at rank 1 (inheriting parent's FIFO sort key 2024-01-10)
    And "FDR-30143" is at rank 2 (FIFO 2024-02-01)
    And "FDR-30141" is not present (fully covered, saldo = 0)

  Scenario: Flere opskrivningsfordringer for samme stamfordring ordnes indbyrdes ved FIFO
    Given debtor "SKY-3015" has the following fordringer:
      | fordringId | kategori         | tilbaestaaendeBeloeb | modtagelsesdato | opskrivningAfFordringId |
      | FDR-30151  | ANDRE_FORDRINGER | 400.00               | 2024-01-10      | ""                      |
      | FDR-30152  | ANDRE_FORDRINGER | 100.00               | 2024-06-01      | FDR-30151               |
      | FDR-30153  | ANDRE_FORDRINGER | 80.00                | 2024-04-15      | FDR-30151               |
    When the dækningsrækkefølge ordered list is retrieved for debtor "SKY-3015"
    Then the ordered list is:
      | rank | fordringId | note                                                  |
      | 1    | FDR-30151  | stamfordring                                          |
      | 2    | FDR-30153  | opskrivning (earlier modtagelsesdato 2024-04-15)      |
      | 3    | FDR-30152  | opskrivning (later modtagelsesdato 2024-06-01)        |

  @petition057
  Scenario: SKY-3029 Opskrivningsfordring — inddrivelsesrenter dækkes inden Hovedfordring ved delvis betaling (FR-5)
    # FR-5 (last bullet): inddrivelsesrenter accrued on an opskrivningsfordring are covered
    # before its Hovedfordring, following the same FR-3 interest sequence.
    Given debtor "SKY-3029" has the following active fordringer:
      | fordringId | kategori         | tilbaestaaendeBeloeb | modtagelsesdato | opskrivningAfFordringId |
      | FDR-30291  | ANDRE_FORDRINGER | 500.00               | 2024-01-10      | ""                      |
      | FDR-30292  | ANDRE_FORDRINGER | 240.00               | 2024-01-10      | FDR-30291               |
    And fordring "FDR-30292" has the following outstanding components:
      | komponent                | beloeb |
      | INDDRIVELSESRENTER_STK1  | 40.00  |
      | HOVEDFORDRING            | 200.00 |
    When a payment of 30.00 DKK is applied to opskrivningsfordring "FDR-30292"
    And the dækningsrækkefølge rule engine applies the payment
    Then INDDRIVELSESRENTER_STK1 on fordring "FDR-30292" receives 30.00 DKK dækning
    And HOVEDFORDRING on fordring "FDR-30292" receives no dækning

  # ==============================================================================
  # FR-6: Timing — GIL § 4, stk. 4
  # AC-12, AC-13
  # ==============================================================================

  Scenario: Sen-ankommet fordring medtages i rækkefølgen ved applikationstidspunktet
    Given debtor "SKY-3016" has fordring "FDR-30161" received at "2025-01-10T08:00:00Z"
    And a payment is received for debtor "SKY-3016" at betalingstidspunkt "2025-01-10T09:00:00Z"
    And fordring "FDR-30162" arrives at "2025-01-10T10:00:00Z" (after betalingstidspunkt but before application)
    When the rule engine applies the payment at applicationTimestamp "2025-01-10T11:00:00Z"
    Then both "FDR-30161" and "FDR-30162" are included in the ordering
    And the dækning records carry betalingstidspunkt "2025-01-10T09:00:00Z"
    And the dækning records carry applicationTimestamp "2025-01-10T11:00:00Z"

  Scenario: Dækning logges med GIL § 4-reference, betalingstidspunkt og applikationstidspunkt
    Given debtor "SKY-3017" has fordring "FDR-30171" with tilbaestaaendeBeloeb 500.00
    When a payment of 200.00 DKK is applied to debtor "SKY-3017" at betalingstidspunkt "2025-02-01T12:00:00Z"
    Then a dækning record is created for fordring "FDR-30171" with:
      | field                | value                      |
      | daekningBeloeb       | 200.00                     |
      | betalingstidspunkt   | 2025-02-01T12:00:00Z       |
      | applicationTimestamp | 2025-02-01T12:05:00Z       |
      | gilParagraf          | GIL § 4, stk. 2            |
      | prioritetKategori    | ANDRE_FORDRINGER           |
      | fifoSortKey          | 2025-02-01                 |
    And the CLS audit log contains an entry for fordring "FDR-30171" with all eight required fields: fordringId, komponent, daekningBeloeb, betalingstidspunkt, applicationTimestamp, gilParagraf, prioritetKategori, fifoSortKey

  # ==============================================================================
  # FR-7: Payment application API
  # AC-14, AC-15
  # ==============================================================================

  Scenario: GET /daekningsraekkefoelge returnerer ordnet liste med gilParagraf for alle positioner
    Given debtor "SKY-3018" has three active fordringer in the same priority category with different modtagelsesdatoer
    When an authenticated sagsbehandler calls GET "/debtors/SKY-3018/daekningsraekkefoelge"
    Then the response status is 200
    And the response body is an ordered array of positions
    And each position includes fields: fordringId, fordringshaverId, prioritetKategori, komponent, tilbaestaaendeBeloeb, modtagelsesdato, fifoSortKey, gilParagraf
    And the array is ordered by prioritetKategori ascending, then by fifoSortKey ascending within each category

  Scenario: GET /daekningsraekkefoelge med asOf-parameter returnerer historisk rækkefølge
    Given debtor "SKY-3019" had fordring "FDR-30191" outstanding on "2024-06-01" but fully covered before today
    When an authenticated sagsbehandler calls GET "/debtors/SKY-3019/daekningsraekkefoelge?asOf=2024-06-01"
    Then the response status is 200
    And the response includes fordring "FDR-30191" with its historical tilbaestaaendeBeloeb as of 2024-06-01

  Scenario: POST /simulate returnerer beregnet dækningsplan uden at persistere ændringer
    Given debtor "SKY-3020" has fordringer with total outstanding 2000.00 DKK
    When an authenticated sagsbehandler calls POST "/debtors/SKY-3020/daekningsraekkefoelge/simulate"
    With body:
      """
      { "beloeb": 600.00 }
      """
    Then the response status is 200
    And each position in the response includes daekningBeloeb and fullyCovers
    And no DaekningRecord is persisted to the database
    And the total of all daekningBeloeb values equals 600.00

  Scenario: POST /simulate med beloeb = 0 returnerer HTTP 422
    Given debtor "SKY-3020" exists
    When an authenticated sagsbehandler calls POST "/debtors/SKY-3020/daekningsraekkefoelge/simulate"
    With body:
      """
      { "beloeb": 0.00 }
      """
    Then the response status is 422
    And the response body contains a problem-detail with description of the validation failure

  @petition057
  Scenario: SKY-3028 Simulering afviser negativt beloeb med HTTP 422
    Given debtor "SKY-3020" exists
    When an authenticated sagsbehandler calls POST "/debtors/SKY-3020/daekningsraekkefoelge/simulate"
    With body:
      """
      { "beloeb": -1 }
      """
    Then the response status is 422
    # FR-7 mandates only HTTP 422 — no specific error code constant is specified in the petition or outcome contract

  Scenario: GET /daekningsraekkefoelge returnerer HTTP 403 for uautoriseret bruger
    Given debtor "SKY-3021" exists
    When a caller without payment-service:read scope calls GET "/debtors/SKY-3021/daekningsraekkefoelge"
    Then the response status is 403

  Scenario: GET /daekningsraekkefoelge returnerer HTTP 404 for ukendt debtor
    When an authenticated sagsbehandler calls GET "/debtors/UNKNOWN-9999/daekningsraekkefoelge"
    Then the response status is 404

  # ==============================================================================
  # FR-8: Sagsbehandler portal — dækningsrækkefølge view
  # AC-16
  # ==============================================================================

  Scenario: Sagsbehandlerportal viser ordnet liste med GIL § 4-kategorietiketter
    Given debtor "SKY-3022" has active fordringer in categories UNDERHOLDSBIDRAG_PRIVATRETLIG and ANDRE_FORDRINGER
    And a sagsbehandler is authenticated with access to debtor "SKY-3022"
    When the sagsbehandler navigates to the dækningsrækkefølge view for debtor "SKY-3022"
    Then the portal displays a ranked list ordered by GIL § 4 priority
    And each row shows the translated Danish category label (e.g. "Underholdsbidrag — privatretlig")
    And each row shows the cost component type in Danish (e.g. "Opkrævningsrenter", "Inddrivelsesrenter § 9, stk. 1", "Hovedfordring")
    And the view is read-only (no dækning actions available)
    And the view is reachable from the debtor's case overview page

  Scenario: Sagsbehandlerportal markerer opskrivningsfordringer med link til stamfordring
    Given debtor "SKY-3023" has a fordring "FDR-30231" with an associated opskrivningsfordring "FDR-30232"
    And a sagsbehandler navigates to the dækningsrækkefølge view for debtor "SKY-3023"
    Then the row for "FDR-30232" is visually marked as an opskrivningsfordring
    And the row displays a reference linking "FDR-30232" to its parent "FDR-30231"
    And the row for "FDR-30232" appears immediately after the row for "FDR-30231" in the list
