@petition059
Feature: Forældelse — prescription tracking and interruption (P059)

  # Legal basis: G.A.2.4 (v3.16, 2026-03-28), GIL §§ 18, 18a, Forældelsesl. §§ 3, 5, 18-19,
  #              Gæld.bekendtg., SKM2015.718.ØLR
  # Out of scope: G.A.2.4.5 strafbare forhold, international forældelses rules,
  #               Catala encoding (petition070), automatic bortfald trigger (petition065).
  # Contract guardrail: these scenarios specify an ownership-neutral public contract only and
  # must not be rewritten into case-service-only public endpoints during architecture work.
  # NFR-5 (p99 < 200 ms) is verified by a separate load-test suite, not by these scenarios.
  # ⚠️ Needs clarification: the petition states BEROSTILLELSE applies to PSRM-managed fordringer
  # only, but does not define the explicit failure mode for non-PSRM requests.

  # --- FR-1: Forældelsesfrist tracking (G.A.2.4.3) ---

  Scenario: FR-1.1 ForaeldelseRecord oprettes ved accept til inddrivelse
    Given en fordring "FDR-59000" accepteres til inddrivelse med registreringsdato "2022-06-01"
    And fordringen har retsgrundlag "ORDINARY"
    And kildesystem er "PSRM"
    When accepten registreres
    Then findes en "ForaeldelseRecord" for fordringen "FDR-59000"
    And er "status" lig med "ACTIVE"
    And er "retsgrundlag" lig med "ORDINARY"
    And er "afbrydelseHistory" tom
    And er "tillaegsfristHistory" tom

  Scenario: FR-1.2 GET returnerer komplet ForaeldelseStatusDto
    Given en fordring "FDR-59002" er under inddrivelse
    And følgende afbrydelseshændelser er registreret for "FDR-59002":
      | type           | eventDate  | legalReference                  | newFristExpires |
      | BEROSTILLELSE  | 2023-03-15 | GIL § 18a, stk. 8               | 2026-03-15      |
      | MODREGNING     | 2024-01-10 | Forældelsesl. § 18, stk. 4      | 2027-01-10      |
    And følgende tillægsfrist er registreret for "FDR-59002":
      | type               | appliedDate | extensionYears | newFristExpires |
      | INTERN_OPSKRIVNING | 2024-02-01  | 2              | 2029-01-10      |
    When der foretages et GET-kald til "/foraeldelse/FDR-59002"
    Then returneres HTTP 200
    And svaret indeholder feltet "fordringId" med værdien "FDR-59002"
    And svaret indeholder feltet "currentFristExpires" med værdien "2029-01-10"
    And svaret indeholder feltet "udskydelseDato"
    And svaret indeholder feltet "isInUdskydelse"
    And svaret indeholder feltet "retsgrundlag"
    And svaret indeholder feltet "afbrydelseHistory"
    And svaret indeholder feltet "tillaegsfristHistory"
    And svaret indeholder feltet "status" med værdien "ACTIVE"

  Scenario: FR-1.2b GET bevarer propagated metadata i afbrydelseHistory
    Given en fordring "FDR-59002B" er under inddrivelse
    And følgende propagerede afbrydelseshændelse er registreret for "FDR-59002B":
      | type          | eventDate  | legalReference    | newFristExpires | sourceFordringId | targetFordringId | propagationReason              |
      | BEROSTILLELSE | 2024-07-01 | GIL § 18a, stk. 2 | 2027-07-01      | FDR-59064        | FDR-59002B       | FORDRINGSKOMPLEKS_PROPAGATION  |
    When der foretages et GET-kald til "/foraeldelse/FDR-59002B"
    Then returneres HTTP 200
    And svaret indeholder feltet "fordringId" med værdien "FDR-59002B"
    And indeholder "afbrydelseHistory" en post med "sourceFordringId" = "FDR-59064"
    And indeholder "afbrydelseHistory" en post med "targetFordringId" = "FDR-59002B"
    And indeholder "afbrydelseHistory" en post med "propagationReason" = "FORDRINGSKOMPLEKS_PROPAGATION"

  Scenario: FR-1.3 3-årig forældelsesfrist gælder uden afbrydelse og uden udskydelse
    Given en fordring "FDR-59001" er under inddrivelse siden "2022-06-01"
    And fordringen har retsgrundlag "ORDINARY"
    And der er ingen afbrydelseshændelser registreret for "FDR-59001"
    And udskydelse gælder ikke for denne fordring
    When systemet beregner forældelsesstatus for fordringen "FDR-59001"
    Then er "currentFristExpires" lig med "2025-06-01"
    And er "status" lig med "ACTIVE"
    And er "afbrydelseHistory" tom

  Scenario: FR-1.3b PSRM-fordring med særligt retsgrundlag har stadig 3-årig basisfrist før udlæg
    Given en fordring "FDR-59001B" er under PSRM-inddrivelse siden "2022-06-01"
    And fordringen har retsgrundlag "SAERLIGT_RETSGRUNDLAG"
    And der er ingen afbrydelseshændelser registreret for "FDR-59001B"
    And udskydelse gælder ikke for denne fordring
    When systemet beregner forældelsesstatus for fordringen "FDR-59001B"
    Then er "currentFristExpires" lig med "2025-06-01"
    And er "status" lig med "ACTIVE"

  Scenario: FR-1.4 API returnerer 404 for ukendt fordringId
    When der foretages et GET-kald til "/foraeldelse/FDR-UKENDT"
    Then returneres HTTP 404

  # --- FR-2: Udskydelse — GIL § 18a, stk. 1 (G.A.2.4.1) ---

  Scenario: FR-2.1 PSRM-fordring fra 19-11-2015 har udskydelse til 20-11-2021
    Given en PSRM-fordring "FDR-59010" er registreret med registreringsdato "2018-04-05"
    And kildesystem er "PSRM"
    When fordringen accepteres til inddrivelse
    Then er "udskydelseDato" sat til "2021-11-20"
    And er "isInUdskydelse" sand på registreringstidspunktet
    And kan "currentFristExpires" tidligst være "2024-11-21"

  Scenario: FR-2.1b PSRM-fordring registreret dagen før grænsen får ingen PSRM-udskydelse
    Given en PSRM-fordring "FDR-59010B" er registreret med registreringsdato "2015-11-18"
    And kildesystem er "PSRM"
    When fordringen accepteres til inddrivelse
    Then er "udskydelseDato" lig med null
    And er "isInUdskydelse" lig med false

  Scenario: FR-2.1c PSRM-fordring registreret på grænsedatoen får PSRM-udskydelse
    Given en PSRM-fordring "FDR-59010C" er registreret med registreringsdato "2015-11-19"
    And kildesystem er "PSRM"
    When fordringen accepteres til inddrivelse
    Then er "udskydelseDato" sat til "2021-11-20"
    And er "isInUdskydelse" sand på registreringstidspunktet
    And kan "currentFristExpires" tidligst være "2024-11-21"

  Scenario: FR-2.2 DMI-fordring registreret 2024-01-01 har udskydelse til 20-11-2027
    Given en fordring "FDR-59011" er registreret i DMI/SAP38 med registreringsdato "2024-01-01"
    And kildesystem er "DMI_SAP38"
    When fordringen accepteres til inddrivelse
    Then er "udskydelseDato" sat til "2027-11-20"
    And er "isInUdskydelse" sand på registreringstidspunktet
    And kan "currentFristExpires" tidligst være "2030-11-21"

  Scenario: FR-2.2b DMI-fordring registreret dagen før grænsen får ingen DMI-udskydelse
    Given en fordring "FDR-59011B" er registreret i DMI/SAP38 med registreringsdato "2023-12-31"
    And kildesystem er "DMI_SAP38"
    When fordringen accepteres til inddrivelse
    Then er "udskydelseDato" lig med null
    And er "isInUdskydelse" lig med false

  Scenario: FR-2.3 Udskydelsesdato er uforanderlig og ændres ikke af efterfølgende afbrydelse
    Given en PSRM-fordring "FDR-59012" er registreret med registreringsdato "2020-01-01"
    And kildesystem er "PSRM"
    And "udskydelseDato" er sat til "2021-11-20"
    When en BEROSTILLELSE-hændelse registreres for "FDR-59012" med eventDate "2023-06-01"
    Then er "udskydelseDato" stadig "2021-11-20"
    And er "currentFristExpires" sat til "2026-06-01"

  Scenario Outline: FR-2.3b isInUdskydelse skifter ved udskydelsesdatoens grænse
    Given en fordring "<fordringId>" har "udskydelseDato" = "<udskydelseDato>"
    And systemdatoen er "<currentDate>"
    When systemet beregner om fordringen "<fordringId>" er i udskydelse
    Then er "isInUdskydelse" lig med <expectedInUdskydelse>

    Examples:
      | fordringId   | udskydelseDato | currentDate | expectedInUdskydelse |
      | FDR-59012B1  | 2021-11-20     | 2021-11-19  | true                 |
      | FDR-59012B2  | 2021-11-20     | 2021-11-20  | false                |
      | FDR-59012B3  | 2021-11-20     | 2021-11-21  | false                |
      | FDR-59012B4  | 2027-11-20     | 2027-11-19  | true                 |
      | FDR-59012B5  | 2027-11-20     | 2027-11-20  | false                |
      | FDR-59012B6  | 2027-11-20     | 2027-11-21  | false                |

  Scenario: FR-2.4 Fordring uden udskydelsesregel får ingen udskydelsesdato
    Given en fordring "FDR-59013" er registreret med registreringsdato "2014-06-01"
    And kildesystem er "PSRM"
    When fordringen accepteres til inddrivelse
    Then er "udskydelseDato" lig med null
    And er "isInUdskydelse" lig med false

  # --- FR-3: Afbrydelse event registration ---

  Scenario: FR-3.1 BEROSTILLELSE afbryder forældelsesfrist for PSRM-fordring
    Given en PSRM-fordring "FDR-59020" er under inddrivelse med "currentFristExpires" = "2025-09-01"
    When en BEROSTILLELSE-hændelse registreres for "FDR-59020" med eventDate "2024-02-15"
    Then returneres HTTP 201
    And er "currentFristExpires" for "FDR-59020" nu "2027-02-15"
    And afbrydelseloggen for "FDR-59020" indeholder en hændelse med:
      | type           | eventDate  | legalReference     |
      | BEROSTILLELSE  | 2024-02-15 | GIL § 18a, stk. 8 |

  Scenario: FR-3.2 Lønindeholdelsesvarsel alene afbryder ikke forældelsesfrist
    Given en fordring "FDR-59030" er under inddrivelse med "currentFristExpires" = "2026-03-01"
    When en LOENINDEHOLDELSE-afbrydelse registreres for "FDR-59030" med:
      | eventDate             | 2024-05-10 |
      | afgoerelseRegistreret | false      |
    Then returneres HTTP 422
    And svaret indeholder en problem-detail der angiver at varsel alene ikke afbryder
    And er "currentFristExpires" for "FDR-59030" stadig "2026-03-01"

  Scenario: FR-3.2b Manglende afgoerelseRegistreret for lønindeholdelse returnerer HTTP 422
    Given en fordring "FDR-59030B" er under inddrivelse med "currentFristExpires" = "2026-03-01"
    When en LOENINDEHOLDELSE-afbrydelse registreres for "FDR-59030B" med:
      | eventDate | 2024-05-10 |
    Then returneres HTTP 422
    And svaret indeholder en problem-detail der angiver at varsel alene ikke afbryder
    And er "currentFristExpires" for "FDR-59030B" stadig "2026-03-01"

  Scenario: FR-3.3 Afgørelse om lønindeholdelse afbryder ved underretning til debitor
    Given en fordring "FDR-59031" er under inddrivelse med "currentFristExpires" = "2025-11-01"
    When en LOENINDEHOLDELSE-afbrydelse registreres for "FDR-59031" med:
      | eventDate             | 2024-06-20 |
      | afgoerelseRegistreret | true       |
    Then returneres HTTP 201
    And er "currentFristExpires" for "FDR-59031" nu "2027-06-20"
    And afbrydelseloggen indeholder hændelsen med legalReference "GIL § 18, stk. 4 + Forældelsesl. § 18, stk. 4"

  Scenario: FR-3.4 Lønindeholdelse afbryder alle fordringer omfattet af samme afgørelse
    Given fordringerne "FDR-59032" og "FDR-59033" er omfattet af samme lønindeholdelsesafgørelse
    And begge fordringer har "currentFristExpires" = "2025-11-01"
    When afgørelsen om lønindeholdelse underrettes debitor den "2024-06-20"
    Then er "currentFristExpires" for "FDR-59032" nu "2027-06-20"
    And er "currentFristExpires" for "FDR-59033" nu "2027-06-20"

  Scenario: FR-3.5 Lønindeholdelse inaktiv i 1 år medfører at ny forældelsesfrist begynder
    Given en fordring "FDR-59034" har en aktiv lønindeholdelsesafbrydelse med dato "2023-01-10"
    And lønindeholdelsen har været inaktiv siden "2023-08-01"
    When inaktivitetsperioden overskrider 1 år den "2024-08-01"
    Then begynder en ny 3-årig forældelsesfrist fra "2024-08-01"
    And er "currentFristExpires" for "FDR-59034" nu "2027-08-01"

  Scenario: FR-3.6 Udlæg på fordring med almindeligt retsgrundlag sætter ny 3-årig frist
    Given en fordring "FDR-59040" har retsgrundlag "ORDINARY"
    And "currentFristExpires" er "2025-12-01"
    When en UDLAEG-afbrydelse registreres for "FDR-59040" med eventDate "2024-04-10"
    Then returneres HTTP 201
    And er "currentFristExpires" for "FDR-59040" nu "2027-04-10"

  Scenario: FR-3.7 Udlæg på fordring med særligt retsgrundlag sætter ny 10-årig frist
    Given en fordring "FDR-59041" har retsgrundlag "SAERLIGT_RETSGRUNDLAG"
    And "currentFristExpires" er "2025-12-01"
    When en UDLAEG-afbrydelse registreres for "FDR-59041" med eventDate "2024-04-10"
    Then returneres HTTP 201
    And er "currentFristExpires" for "FDR-59041" nu "2034-04-10"

  Scenario Outline: FR-3.8 Forgæves udlæg har samme afbrydelseseffekt som succesfuldt udlæg
    Given en fordring "<fordringId>" har retsgrundlag "<retsgrundlag>"
    And "currentFristExpires" er "<previousFrist>"
    When en UDLAEG-afbrydelse registreres for "<fordringId>" med eventDate "<eventDate>" og forgaevesUdlaeg = <forgaevesUdlaeg>
    Then returneres HTTP 201
    And er "currentFristExpires" for "<fordringId>" nu "<newFrist>"
    And afbrydelseloggen indeholder hændelsen med legalReference "Forældelsesl. § 18, stk. 1"

    Examples:
      | fordringId | retsgrundlag            | previousFrist | eventDate  | forgaevesUdlaeg | newFrist   |
      | FDR-59042  | ORDINARY                | 2025-07-15    | 2024-03-22 | true            | 2027-03-22 |
      | FDR-59043  | SAERLIGT_RETSGRUNDLAG   | 2025-07-15    | 2024-03-22 | true            | 2034-03-22 |

  Scenario: FR-3.9 Modregning afbryder forældelsesfrist
    Given en fordring "FDR-59050" er under inddrivelse med "currentFristExpires" = "2026-01-01"
    When en MODREGNING-afbrydelse registreres for "FDR-59050" med eventDate "2024-08-05"
    Then returneres HTTP 201
    And er "currentFristExpires" for "FDR-59050" nu "2027-08-05"
    And afbrydelseloggen indeholder hændelsen med legalReference "Forældelsesl. § 18, stk. 4"

  Scenario: FR-3.10 Ukendt afbrydelsestype returnerer HTTP 422
    Given en fordring "FDR-59051" er under inddrivelse
    When der registreres en afbrydelseshændelse for "FDR-59051" med ukendt type "UKENDT_TYPE"
    Then returneres HTTP 422

  Scenario: FR-3.11 Manglende eventDate returnerer HTTP 422
    Given en fordring "FDR-59052" er under inddrivelse
    When en MODREGNING-afbrydelse registreres for "FDR-59052" uden "eventDate"
    Then returneres HTTP 422

  Scenario: FR-3.12 Ukendt fordringId ved afbrydelse returnerer HTTP 404
    When en MODREGNING-afbrydelse registreres for "FDR-UKENDT" med eventDate "2024-08-05"
    Then returneres HTTP 404

  # --- FR-4: Fordringskompleks membership and afbrydelse propagation ---

  Scenario: FR-4.1 Nyt fordringskompleks oprettes med initiale medlemmer
    Given fordringerne "FDR-59060" og "FDR-59061" findes
    When der foretages et POST-kald til "/fordringskompleks" med medlemmerne "FDR-59060" og "FDR-59061"
    Then oprettes et fordringskompleks med et nyt "kompleksId"
    And er "FDR-59060" medlem af det nye kompleks
    And er "FDR-59061" medlem af det nye kompleks

  Scenario: FR-4.2 Medlem kan tilføjes og medlemslisten kan hentes
    Given komplekset "K-002" findes med medlemmet "FDR-59062"
    When der foretages et POST-kald til "/fordringskompleks/K-002/members/FDR-59063"
    Then er "FDR-59063" medlem af komplekset "K-002"
    When der foretages et GET-kald til "/fordringskompleks/K-002/members"
    Then returneres medlemmerne "FDR-59062" og "FDR-59063"

  Scenario: FR-4.3 Afbrydelse for én fordring i fordringskompleks propagerer til alle medlemmer med fuld metadata
    Given fordringerne "FDR-59064", "FDR-59065" og "FDR-59066" er medlemmer af kompleks "K-003"
    And alle tre fordringer har "currentFristExpires" = "2025-10-01"
    When en BEROSTILLELSE-afbrydelse registreres for "FDR-59064" med eventDate "2024-07-01"
    Then returneres HTTP 201
    And er "currentFristExpires" for "FDR-59064" nu "2027-07-01"
    And er "currentFristExpires" for "FDR-59065" nu "2027-07-01"
    And er "currentFristExpires" for "FDR-59066" nu "2027-07-01"
    And afbrydelseloggen for "FDR-59065" indeholder en propageret hændelse med:
      | type           | eventDate  | legalReference    | sourceFordringId | targetFordringId | propagationReason               |
      | BEROSTILLELSE  | 2024-07-01 | GIL § 18a, stk. 2 | FDR-59064        | FDR-59065        | FORDRINGSKOMPLEKS_PROPAGATION   |
    And afbrydelseloggen for "FDR-59066" indeholder en propageret hændelse med:
      | type           | eventDate  | legalReference    | sourceFordringId | targetFordringId | propagationReason               |
      | BEROSTILLELSE  | 2024-07-01 | GIL § 18a, stk. 2 | FDR-59064        | FDR-59066        | FORDRINGSKOMPLEKS_PROPAGATION   |
    When der foretages et GET-kald til "/foraeldelse/FDR-59065"
    Then returneres HTTP 200
    And indeholder "afbrydelseHistory" en post med "sourceFordringId" = "FDR-59064"
    And indeholder "afbrydelseHistory" en post med "targetFordringId" = "FDR-59065"
    And oprettes en CLS-revisionslogpost for den propagerede hændelse på "FDR-59065"
    And oprettes en CLS-revisionslogpost for den propagerede hændelse på "FDR-59066"

  Scenario: FR-4.4 Fordringskompleks-propagation er atomisk og ruller tilbage ved fejl
    Given fordringerne "FDR-59067" og "FDR-59068" er medlemmer af kompleks "K-004"
    And "FDR-59068" er i en tilstand der midlertidigt forhindrer opdatering
    When en MODREGNING-afbrydelse registreres for "FDR-59067" med eventDate "2024-09-01"
    Then returneres en fejl
    And er "currentFristExpires" for "FDR-59067" uændret
    And er "currentFristExpires" for "FDR-59068" uændret

  Scenario: FR-4.5 Modtagelse af tomt fordringskompleks udløser foreløbig afbrydelse
    Given fordringen "FDR-59069" modtages til inddrivelse som del af et tomt fordringskompleks den "2024-02-15"
    And der er ingen tidligere afbrydelseshændelser registreret for "FDR-59069"
    And udskydelse gælder ikke for denne modtagelse
    When modtagelsen registreres i forældelsessystemet
    Then oprettes en afbrydelseshændelse for "FDR-59069" med legalReference "GIL § 18a, stk. 7"
    And er "currentFristExpires" for "FDR-59069" nu "2027-02-15"
    When der foretages et GET-kald til "/foraeldelse/FDR-59069"
    Then returneres HTTP 200
    And indeholder "afbrydelseHistory" en post med legalReference "GIL § 18a, stk. 7"

  # --- FR-5: Tillægsfrister (G.A.2.4.4.2) ---

  Scenario: FR-5.1 Intern opskrivning tilføjer 2-årig tillægsfrist til forældelsesfrist
    Given en fordring "FDR-59070" er under inddrivelse med "currentFristExpires" = "2026-05-15"
    When en tillægsfrist af typen "INTERN_OPSKRIVNING" registreres for "FDR-59070" med appliedDate "2024-10-01"
    Then returneres HTTP 201
    And er "currentFristExpires" for "FDR-59070" nu "2028-05-15"
    And "tillaegsfristHistory" for "FDR-59070" indeholder:
      | type                | appliedDate | extensionYears | newFristExpires | legalReference |
      | INTERN_OPSKRIVNING  | 2024-10-01  | 2              | 2028-05-15      | G.A.2.4.4.2    |
    And oprettes en CLS-revisionslogpost for "FDR-59070" med hændelsestype og juridisk reference for tillægsfristregistrering

  Scenario: FR-5.2 Tillægsfrist beregnes fra max af currentFristExpires og appliedDate
    Given en fordring "FDR-59071" er under inddrivelse med "currentFristExpires" = "2024-03-01"
    When en tillægsfrist af typen "INTERN_OPSKRIVNING" registreres for "FDR-59071" med appliedDate "2024-06-01"
    Then er "currentFristExpires" for "FDR-59071" nu "2026-06-01"

  # --- FR-6: Forældelsesindsigelse workflow (G.A.2.4.6) ---

  Scenario: FR-6.1 Debitors forældelsesindsigelse registreres og udløser sagsbehandlerworkflow
    Given en fordring "FDR-59080" er under inddrivelse med status "ACTIVE"
    When en caseworker registrerer en forældelsesindsigelse for "FDR-59080" via "POST /foraeldelse/FDR-59080/indsigelse"
    Then returneres HTTP 201
    And indeholder svaret et unikt "indsigelsesId"
    And ændres status for "FDR-59080" til "INDSIGELSE_PENDING"
    And oprettes en CLS-revisionslogpost for "FDR-59080" med hændelsestype og juridisk reference for indsigelsesregistrering
    And er revisionsloggens identitet afledt af autentificeret serverkontekst og ikke af kommandoinput

  Scenario: FR-6.1b Offentlig indsigelsesregistrering afviser klientstyrede identitets- og skyldnerfelter
    Given en fordring "FDR-59080B" er under inddrivelse med status "ACTIVE"
    When en klient sender "POST /foraeldelse/FDR-59080B/indsigelse" med payload:
      | registeredBy  | spoofed-caseworker                        |
      | decidedBy     | spoofed-approver                          |
      | debtorPersonId| 11111111-1111-1111-1111-111111111111      |
    Then afvises anmodningen som ugyldigt offentligt kommandoinput
    And oprettes ingen indsigelse for "FDR-59080B"
    And forbliver status for "FDR-59080B" "ACTIVE"

  Scenario: FR-6.2 Gyldig forældelsesindsigelse fjerner fordringen fra inddrivelse
    Given en fordring "FDR-59081" har status "INDSIGELSE_PENDING"
    And indsigelsen har "indsigelsesId" = "INS-001"
    When en caseworker evaluerer indsigelsen "INS-001" for "FDR-59081" med:
      | outcome   | VALID                                                     |
      | rationale | Forældelsesfrist udløb 2023-11-21 uden afbrydelseshændelser |
    Then returneres HTTP 200
    And ændres status for "FDR-59081" til "FORAELDET"
    And fjernes fordringen "FDR-59081" fra aktiv inddrivelse
    And oprettes en CLS-revisionslogpost med udfald "VALID", caseworkers identitet, timestamp, fordringId og juridisk reference
    And er revisionsloggens identitet afledt af autentificeret serverkontekst og ikke af kommandoinput

  Scenario: FR-6.3 Ugyldig forældelsesindsigelse returnerer fordring til aktiv inddrivelse
    Given en fordring "FDR-59082" har status "INDSIGELSE_PENDING"
    And indsigelsen har "indsigelsesId" = "INS-002"
    When en caseworker evaluerer indsigelsen "INS-002" for "FDR-59082" med:
      | outcome   | INVALID                                               |
      | rationale | Lønindeholdelsesafgørelse afbrød fristen 2023-04-10  |
    Then returneres HTTP 200
    And ændres status for "FDR-59082" til "ACTIVE"
    And er afvisningsrationale gemt på indsigelsen "INS-002"
    And oprettes en CLS-revisionslogpost med udfald "INVALID", caseworkers identitet, timestamp, fordringId og juridisk reference
    And er revisionsloggens identitet afledt af autentificeret serverkontekst og ikke af kommandoinput

  Scenario: FR-6.3b Offentlig indsigelsesevaluering afviser klientstyrede identitets- og skyldnerfelter
    Given en fordring "FDR-59082B" har status "INDSIGELSE_PENDING"
    And indsigelsen har "indsigelsesId" = "INS-003"
    When en klient evaluerer indsigelsen "INS-003" for "FDR-59082B" med:
      | outcome       | INVALID                                   |
      | rationale     | Forsøgt spoofet payload                   |
      | registeredBy  | spoofed-caseworker                        |
      | decidedBy     | spoofed-approver                          |
      | debtorPersonId| 22222222-2222-2222-2222-222222222222      |
    Then afvises anmodningen som ugyldigt offentligt kommandoinput
    And ændres status for "FDR-59082B" ikke
    And opdateres indsigelsen "INS-003" ikke

  # --- FR-7: Sagsbehandlerportal synlighed ---

  Scenario: FR-7.1 Sagsbehandlerportalen viser forældelsesstatus med ISO-dato og udskydelse
    Given en sagsbehandler er autentificeret med rollen "CASEWORKER"
    And fordringen "FDR-59090" har følgende forældelsesstatus:
      | status              | ACTIVE      |
      | currentFristExpires | 2027-03-15  |
      | udskydelseDato      | 2021-11-20  |
      | isInUdskydelse      | false       |
    When sagsbehandleren navigerer til detaljevisningen for fordringen "FDR-59090"
    Then vises forældelsesstatus-panelet med:
      | Status               | Aktiv       |
      | Frist udløber        | 2027-03-15  |
      | Udskydelsesdato      | 2021-11-20  |
      | I udskydelsesvindue  | Nej         |

  Scenario: FR-7.2 Sagsbehandlerportalen viser afbrydelseshistorik med resulting new frist
    Given en fordring "FDR-59091" har to registrerede afbrydelseshændelser
    When sagsbehandleren navigerer til detaljevisningen for fordringen "FDR-59091"
    Then vises afbrydelseshistorik-tabellen med 2 rækker i kronologisk rækkefølge
    And indeholder hver række type, dato, juridisk reference og resulting new frist

  Scenario: FR-7.3 Sagsbehandlerportalen viser tillægsfristhistorik og fordringskompleks-medlemskab
    Given fordringen "FDR-59092" er medlem af kompleks "K-010" med medlemmet "FDR-59093"
    And fordringen "FDR-59092" har en registreret tillægsfrist
    And fordringen "FDR-59092" har en propageret afbrydelseshændelse med "sourceFordringId" = "FDR-59093"
    And fordringen "FDR-59092" har en propageret afbrydelseshændelse med "targetFordringId" = "FDR-59092"
    When sagsbehandleren navigerer til detaljevisningen for fordringen "FDR-59092"
    Then vises afsnittet "Fordringskompleks" i panelet
    And listes fordringen "FDR-59093" som medlem af komplekset
    And vises tillægsfristhistorikken med type, dato, extension og ny frist
    And vises "sourceFordringId" = "FDR-59093" for den propagerede afbrydelseshændelse
    And vises "targetFordringId" = "FDR-59092" for den propagerede afbrydelseshændelse

  Scenario: FR-7.4 Sagsbehandler med skriveadgang ser knap til registrering af forældelsesindsigelse
    Given en fordring "FDR-59094" har status "ACTIVE"
    And sagsbehandleren har rolle "CASEWORKER" med skriveadgang
    When sagsbehandleren navigerer til detaljevisningen for fordringen "FDR-59094"
    Then vises knappen "Registrer forældelsesindsigelse"

  Scenario: FR-7.5 Afventende indsigelse viser evalueringsformular med rationalefelt
    Given en fordring "FDR-59095" har status "INDSIGELSE_PENDING"
    When sagsbehandleren navigerer til detaljevisningen for fordringen "FDR-59095"
    Then vises evalueringsformularen med valg for "Gyldig" og "Ugyldig"
    And vises et tekstfelt til "rationale"
    And er registreringsknappen ikke tilgængelig

  Scenario: FR-7.6 Forældet fordring viser udfald og rationale uden registreringsknap
    Given en fordring "FDR-59096" har status "FORAELDET"
    And den seneste indsigelse blev vurderet som "VALID" med rationale "Forældelsesfrist udløb 2023-11-21"
    When sagsbehandleren navigerer til detaljevisningen for fordringen "FDR-59096"
    Then vises udfaldet "Forældet"
    And vises rationalet "Forældelsesfrist udløb 2023-11-21"
    And vises knappen "Registrer forældelsesindsigelse" ikke

  Scenario: FR-7.7 Read-only caseworker ser panelet men ingen skrivehandlinger
    Given en sagsbehandler er autentificeret med læseadgang til fordringen "FDR-59097"
    And fordringen "FDR-59097" har status "ACTIVE"
    When sagsbehandleren navigerer til detaljevisningen for fordringen "FDR-59097"
    Then vises forældelsesstatus-panelet
    And kan sagsbehandleren ikke registrere afbrydelseshændelser
    And kan sagsbehandleren ikke registrere eller evaluere indsigelser

  # --- NFR-3: GDPR / no PII ---

  Scenario: NFR-3.1 Forældelsesdata og API-svar refererer kun skyldner via person_id
    Given en fordring "FDR-59100" har skyldnerreference "person_id" = "550e8400-e29b-41d4-a716-446655440000"
    And der findes en "ForaeldelseRecord", en "AfbrydelseEvent", en "TillaegsfristEvent", en "FordringskompleksLink" og en "ForaeldelseIndsigelse" for fordringen
    When der foretages et GET-kald til "/foraeldelse/FDR-59100"
    Then indeholder svaret kun skyldnerreferencen "person_id"
    And indeholder svaret ikke CPR, navn, adresse, email eller telefon
    And indeholder de persistede forældelsesdata ikke CPR, navn, adresse, email eller telefon

  Scenario: NFR-2.1 Almindelig afbrydelsesregistrering logges til CLS med påkrævede felter
    Given en fordring "FDR-59101" er under inddrivelse
    When en UDLAEG-afbrydelse registreres for "FDR-59101" med eventDate "2024-11-01"
    Then returneres HTTP 201
    And oprettes en CLS-revisionslogpost for "FDR-59101" med caseworker eller system-identitet, timestamp, fordringId og juridisk reference "Forældelsesl. § 18, stk. 1"
    And beskriver revisionslogposten hændelsestypen som en afbrydelsesregistrering for "UDLAEG"

  Scenario Outline: NFR-2.2 Øvrige auditerede hændelser skriver CLS-payload med påkrævede felter
    Given den auditerede hændelse "<eventName>" er udført for fordringen "<fordringId>"
    When revisionsloggen for hændelsen hentes
    Then indeholder CLS-logposten caseworker eller system-identitet, timestamp, fordringId "<fordringId>" og juridisk reference "<legalReference>"
    And beskriver CLS-logposten hændelsestypen som "<eventDescription>"

    Examples:
      | eventName                         | eventDescription                         | legalReference     | fordringId |
      | fordringskompleks propagation     | afbrydelsepropagering                    | GIL § 18a, stk. 2  | FDR-59065  |
      | tillægsfrist registrering         | tillægsfristregistrering                 | G.A.2.4.4.2        | FDR-59070  |
      | indsigelse registrering           | indsigelsesregistrering                  | G.A.2.4.6          | FDR-59080  |
      | indsigelse evaluering VALID       | indsigelsesevaluering med VALID udfald   | G.A.2.4.6          | FDR-59081  |
      | indsigelse evaluering INVALID     | indsigelsesevaluering med INVALID udfald | G.A.2.4.6          | FDR-59082  |
