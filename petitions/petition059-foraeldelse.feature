@petition059
Feature: Forældelse — prescription tracking and interruption (P059)

  # Legal basis: G.A.2.4 (v3.16, 2026-03-28), GIL §§ 18, 18a, Forældelsesl. §§ 3, 5, 18–19,
  #              Gæld.bekendtg., SKM2015.718.ØLR
  # Out of scope: G.A.2.4.5 strafbare forhold, international forældelses rules,
  #               Catala encoding (petition070), automatic bortfald trigger (petition065).
  # NFR-3 (no PII in entities): verified by separate integration test suite, not Gherkin.
  # NFR-5 (p99 < 200 ms): verified by load test suite, not Gherkin.
  # AC-21 (no PII in responses): verified by response-inspection integration tests.
  # AC-22 (every AC covered by Gherkin): this file satisfies that requirement.

  # --- FR-1: Forældelsesfrist tracking (G.A.2.4.3) ---

  Scenario: 3-årig forældelsesfrist gælder for PSRM-fordring uden afbrydelse
    Given en fordring "FDR-59001" er under PSRM-inddrivelse siden "2022-06-01"
    And fordringen har retsgrundlag "ORDINARY"
    And der er ingen afbrydelseshændelser registreret for "FDR-59001"
    And udskydelse gælder ikke for denne fordring
    When systemet beregner forældelsesstatus for fordringen "FDR-59001"
    Then er "currentFristExpires" lig med "2025-06-01"
    And er "status" lig med "ACTIVE"
    And er "afbrydelseHistory" tom

  Scenario: API returnerer komplet forældelsesstatus med næste udløbsdato og afbrydelseshistorik
    Given en fordring "FDR-59002" er under inddrivelse
    And følgende afbrydelseshændelser er registreret for "FDR-59002":
      | type         | eventDate  | legalReference           |
      | BEROSTILLELSE | 2023-03-15 | GIL § 18a, stk. 8       |
      | MODREGNING   | 2024-01-10 | Forældelsesl. § 18, stk. 4 |
    When der foretages et GET-kald til "/foraeldelse/FDR-59002"
    Then returneres HTTP 200
    And svaret indeholder feltet "currentFristExpires" med værdien "2027-01-10"
    And svaret indeholder feltet "status" med værdien "ACTIVE"
    And "afbrydelseHistory" indeholder 2 hændelser i kronologisk rækkefølge
    And den seneste hændelse i historikken er af typen "MODREGNING" med dato "2024-01-10"

  Scenario: API returnerer 404 for ukendt fordringId
    When der foretages et GET-kald til "/foraeldelse/FDR-UKENDT"
    Then returneres HTTP 404

  # --- FR-2: Udskydelse — GIL § 18a, stk. 1 (G.A.2.4.1) ---

  Scenario: PSRM-fordring fra 19-11-2015 har tidligst forældelsesfrist fra 20-11-2021
    Given en PSRM-fordring "FDR-59010" er registreret med registreringsdato "2018-04-05"
    And kildesystem er "PSRM"
    When fordringen accepteres til inddrivelse
    Then er "udskydelseDato" sat til "2021-11-20"
    And er "isInUdskydelse" sand på registreringstidspunktet
    And kan "currentFristExpires" tidligst være "2024-11-21"

  Scenario: DMI-fordring registreret 1-1-2024 har tidligst forældelsesfrist fra 20-11-2027
    Given en fordring "FDR-59011" er registreret i DMI/SAP38 med registreringsdato "2024-01-01"
    And kildesystem er "DMI_SAP38"
    When fordringen accepteres til inddrivelse
    Then er "udskydelseDato" sat til "2027-11-20"
    And er "isInUdskydelse" sand på registreringstidspunktet
    And kan "currentFristExpires" tidligst være "2030-11-21"

  Scenario: Udskydelsesdato er uforanderlig og ændres ikke af efterfølgende afbrydelse
    Given en PSRM-fordring "FDR-59012" er registreret med registreringsdato "2020-01-01"
    And kildesystem er "PSRM"
    And "udskydelseDato" er sat til "2021-11-20"
    When en BEROSTILLELSE-hændelse registreres for "FDR-59012" med eventDate "2023-06-01"
    Then er "udskydelseDato" stadig "2021-11-20"
    And er "currentFristExpires" sat til "2026-06-01"

  # --- FR-3: Afbrydelse — BEROSTILLELSE (GIL § 18a, stk. 8) ---

  Scenario: Afgørelse om berostillelse afbryder forældelsesfrist (PSRM only)
    Given en fordring "FDR-59020" er under inddrivelse med "currentFristExpires" = "2025-09-01"
    When en BEROSTILLELSE-hændelse registreres for "FDR-59020" med eventDate "2024-02-15"
    Then returneres HTTP 201
    And er "currentFristExpires" for "FDR-59020" nu "2027-02-15"
    And afbrydelseloggen for "FDR-59020" indeholder en hændelse med:
      | type          | eventDate  | legalReference     |
      | BEROSTILLELSE | 2024-02-15 | GIL § 18a, stk. 8 |

  # --- FR-3: Afbrydelse — LOENINDEHOLDELSE (GIL § 18, stk. 4 + SKM2015.718.ØLR) ---

  Scenario: Lønindeholdelsesvarsel alene afbryder IKKE forældelsesfrist
    Given en fordring "FDR-59030" er under inddrivelse med "currentFristExpires" = "2026-03-01"
    When en LOENINDEHOLDELSE-afbrydelse registreres for "FDR-59030" med:
      | eventDate            | 2024-05-10 |
      | afgoerelseRegistreret | false      |
    Then returneres HTTP 422
    And svaret indeholder en problem-detail der angiver at varsel alene ikke afbryder
    And er "currentFristExpires" for "FDR-59030" stadig "2026-03-01"
    And er der ingen ny afbrydelseshændelse logget for "FDR-59030"

  Scenario: Afgørelse om lønindeholdelse afbryder ved underretning til debitor
    Given en fordring "FDR-59031" er under inddrivelse med "currentFristExpires" = "2025-11-01"
    When en LOENINDEHOLDELSE-afbrydelse registreres for "FDR-59031" med:
      | eventDate            | 2024-06-20 |
      | afgoerelseRegistreret | true       |
    Then returneres HTTP 201
    And er "currentFristExpires" for "FDR-59031" nu "2027-06-20"
    And afbrydelseloggen indeholder hændelsen med legalReference "GIL § 18, stk. 4"

  Scenario: Lønindeholdelse inaktiv i 1 år medfører at ny forældelsesfrist begynder
    Given en fordring "FDR-59032" har en aktiv lønindeholdelsesafbrydelse med dato "2023-01-10"
    And lønindeholdelsen har været inaktiv siden "2023-08-01"
    When inaktivitetsperioden overskrider 1 år den "2024-08-01"
    Then begynder en ny 3-årig forældelsesfrist fra "2024-08-01"
    And er "currentFristExpires" for "FDR-59032" nu "2027-08-01"

  # --- FR-3: Afbrydelse — UDLAEG (Forældelsesl. § 18, stk. 1) ---

  Scenario: Forgæves udlæg (insolvenserklæring) afbryder forældelsesfrist
    Given en fordring "FDR-59040" er under inddrivelse med retsgrundlag "ORDINARY"
    And "currentFristExpires" er "2025-07-15"
    When en UDLAEG-afbrydelse registreres for "FDR-59040" med eventDate "2024-03-22" og forgaevesUdlaeg = true
    Then returneres HTTP 201
    And er "currentFristExpires" for "FDR-59040" nu "2027-03-22"
    And afbrydelseloggen indeholder hændelsen med legalReference "Forældelsesl. § 18, stk. 1"

  Scenario: Udlæg på fordring med særligt retsgrundlag (dom) sætter ny 10-årig forældelsesfrist
    Given en fordring "FDR-59041" har retsgrundlag "SAERLIGT_RETSGRUNDLAG"
    And "currentFristExpires" er "2025-12-01"
    When en UDLAEG-afbrydelse registreres for "FDR-59041" med eventDate "2024-04-10"
    Then returneres HTTP 201
    And er "currentFristExpires" for "FDR-59041" nu "2034-04-10"

  Scenario: Udlæg på fordring med almindeligt retsgrundlag sætter ny 3-årig frist
    Given en fordring "FDR-59042" har retsgrundlag "ORDINARY"
    And "currentFristExpires" er "2025-12-01"
    When en UDLAEG-afbrydelse registreres for "FDR-59042" med eventDate "2024-04-10"
    Then returneres HTTP 201
    And er "currentFristExpires" for "FDR-59042" nu "2027-04-10"

  # --- FR-3: Afbrydelse — MODREGNING (Forældelsesl. § 18, stk. 4) ---

  Scenario: Modregning afbryder forældelsesfrist
    Given en fordring "FDR-59050" er under inddrivelse med "currentFristExpires" = "2026-01-01"
    When en MODREGNING-afbrydelse registreres for "FDR-59050" med eventDate "2024-08-05"
    Then returneres HTTP 201
    And er "currentFristExpires" for "FDR-59050" nu "2027-08-05"
    And afbrydelseloggen indeholder hændelsen med legalReference "Forældelsesl. § 18, stk. 4"

  # --- FR-4: Fordringskompleks propagation (GIL § 18a, stk. 2 / G.A.2.4.2) ---

  Scenario: Afbrydelse for én fordring i fordringskompleks propagerer til alle medlemmer
    Given fordringerne "FDR-59060", "FDR-59061" og "FDR-59062" er medlemmer af kompleks "K-001"
    And alle tre fordringer har "currentFristExpires" = "2025-10-01"
    When en BEROSTILLELSE-afbrydelse registreres for "FDR-59060" med eventDate "2024-07-01"
    Then returneres HTTP 201
    And er "currentFristExpires" for "FDR-59060" nu "2027-07-01"
    And er "currentFristExpires" for "FDR-59061" nu "2027-07-01"
    And er "currentFristExpires" for "FDR-59062" nu "2027-07-01"
    And afbrydelseloggen for "FDR-59061" indeholder en propageret hændelse med:
      | type          | eventDate  | legalReference    | sourceFordringId |
      | BEROSTILLELSE | 2024-07-01 | GIL § 18a, stk. 2 | FDR-59060        |
    And afbrydelseloggen for "FDR-59062" indeholder en propageret hændelse med:
      | type          | eventDate  | legalReference    | sourceFordringId |
      | BEROSTILLELSE | 2024-07-01 | GIL § 18a, stk. 2 | FDR-59060        |

  Scenario: Fordringskompleks-propagation er atomisk — fejl i ét led ruller hele transaktionen tilbage
    Given fordringerne "FDR-59063" og "FDR-59064" er medlemmer af kompleks "K-002"
    And "FDR-59064" er i en tilstand der midlertidigt forhindrer opdatering
    When en MODREGNING-afbrydelse registreres for "FDR-59063" med eventDate "2024-09-01"
    Then returneres en fejl
    And er "currentFristExpires" for "FDR-59063" uændret
    And er "currentFristExpires" for "FDR-59064" uændret

  # --- FR-5: Tillægsfrister (G.A.2.4.4.2) ---

  Scenario: Intern opskrivning tilføjer 2-årig tillægsfrist til forældelsesfrist
    Given en fordring "FDR-59070" er under inddrivelse med "currentFristExpires" = "2026-05-15"
    When en tillægsfrist af typen "INTERN_OPSKRIVNING" registreres for "FDR-59070" med appliedDate "2024-10-01"
    Then returneres HTTP 201
    And er "currentFristExpires" for "FDR-59070" nu "2028-05-15"
    And "tillaegsfristHistory" for "FDR-59070" indeholder:
      | type              | appliedDate | extensionYears | legalReference  |
      | INTERN_OPSKRIVNING | 2024-10-01 | 2              | G.A.2.4.4.2     |

  Scenario: Tillægsfrist beregnes fra max af currentFristExpires og appliedDate
    Given en fordring "FDR-59071" er under inddrivelse med "currentFristExpires" = "2024-03-01"
    When en tillægsfrist af typen "INTERN_OPSKRIVNING" registreres for "FDR-59071" med appliedDate "2024-06-01"
    Then er "currentFristExpires" for "FDR-59071" nu "2026-06-01"

  # --- FR-6: Forældelsesindsigelse (G.A.2.4.6) ---

  Scenario: Debitors forældelsesindsigelse registreres og udløser sagsbehandler-evalueringsworkflow
    Given en fordring "FDR-59080" er under inddrivelse med status "ACTIVE"
    When en caseworker registrerer en forældelsesindsigelse for "FDR-59080" via
      "POST /foraeldelse/FDR-59080/indsigelse"
    Then returneres HTTP 201
    And indeholder svaret et unikt "indsigelsesId"
    And ændres status for "FDR-59080" til "INDSIGELSE_PENDING"
    And oprettes en CLS-revisionslogpost for "FDR-59080" med hændelsestypen "INDSIGELSE_REGISTRERET"

  Scenario: Gyldig forældelsesindsigelse fjerner fordringen fra inddrivelse
    Given en fordring "FDR-59081" har status "INDSIGELSE_PENDING"
    And indsigelsen har "indsigelsesId" = "INS-001"
    When en caseworker evaluerer indsigelsen "INS-001" for "FDR-59081" med:
      | outcome   | VALID                                                  |
      | rationale | Forældelsesfrist udløb 2023-11-21 uden afbrydelseshændelser |
    Then returneres HTTP 200
    And ændres status for "FDR-59081" til "FORAELDET"
    And fjernes fordringen "FDR-59081" fra aktiv inddrivelse
    And oprettes en CLS-revisionslogpost med udfald "VALID" og caseworkers identitet

  Scenario: Ugyldig forældelsesindsigelse returnerer fordring til aktiv inddrivelse
    Given en fordring "FDR-59082" har status "INDSIGELSE_PENDING"
    And indsigelsen har "indsigelsesId" = "INS-002"
    When en caseworker evaluerer indsigelsen "INS-002" for "FDR-59082" med:
      | outcome   | INVALID                                              |
      | rationale | Lønindeholdelsesafgørelse afbrød fristen 2023-04-10 |
    Then returneres HTTP 200
    And ændres status for "FDR-59082" til "ACTIVE"
    And er afvisningsrationale gemt på indsigelsen "INS-002"
    And oprettes en CLS-revisionslogpost med udfald "INVALID" og caseworkers identitet

  # --- FR-7: Sagsbehandlerportal synlighed ---

  Scenario: Sagsbehandlerportalen viser forældelsesstatus for en fordring
    Given en sagsbehandler er autentificeret med rollen "CASEWORKER"
    And fordringen "FDR-59090" har følgende forældelsesstatus:
      | status             | ACTIVE           |
      | currentFristExpires | 2027-03-15      |
      | udskydelseDato     | 2021-11-20       |
      | isInUdskydelse     | false            |
    When sagsbehandleren navigerer til detaljevisningen for fordringen "FDR-59090"
    Then vises forældelsesstatus-panelet med:
      | Status             | Aktiv            |
      | Frist udløber      | 15. marts 2027   |
      | Udskydelsesdato    | 20. november 2021 |
      | I udskydelsesvindue | Nej             |

  Scenario: Sagsbehandlerportalen viser afbrydelseshistorik i kronologisk rækkefølge
    Given en fordring "FDR-59091" har to registrerede afbrydelseshændelser
    When sagsbehandleren navigerer til detaljevisningen for fordringen "FDR-59091"
    Then vises afbrydelseshistorik-tabellen med 2 rækker i kronologisk rækkefølge
    And indeholder hver række type, dato og juridisk reference

  Scenario: Sagsbehandlerportalen viser knap til registrering af forældelsesindsigelse
    Given en fordring "FDR-59092" har status "ACTIVE"
    And sagsbehandleren har rolle "CASEWORKER" med skriveadgang
    When sagsbehandleren navigerer til detaljevisningen for fordringen "FDR-59092"
    Then vises knappen "Registrer forældelsesindsigelse"

  Scenario: Sagsbehandlerportalen viser evalueringsformular for afventende indsigelse
    Given en fordring "FDR-59093" har status "INDSIGELSE_PENDING"
    When sagsbehandleren navigerer til detaljevisningen for fordringen "FDR-59093"
    Then vises evalueringsformularen med valg for "Gyldig" og "Ugyldig"
    And er registreringsknappen ikke tilgængelig

  Scenario: Sagsbehandlerportalen viser fordringskompleks-medlemskab
    Given fordringen "FDR-59094" er medlem af kompleks "K-010" med medl. "FDR-59095"
    When sagsbehandleren navigerer til detaljevisningen for fordringen "FDR-59094"
    Then vises afsnittet "Fordringskompleks" i panelet
    And listes fordringen "FDR-59095" som medkonteret kompleksmedlem

  # --- NFR-2: Revisionslog for alle hændelser ---

  Scenario: Alle afbrydelseshændelser logges til revisionsloggen (CLS)
    Given en fordring "FDR-59100" er under inddrivelse
    When en UDLAEG-afbrydelse registreres for "FDR-59100" med eventDate "2024-11-01"
    Then returneres HTTP 201
    And oprettes en CLS-revisionslogpost for "FDR-59100" indeholdende:
      | hændelsestype    | AFBRYDELSE_REGISTRERET |
      | afbrydelsestype  | UDLAEG                 |
      | juridiskReference | Forældelsesl. § 18, stk. 1 |
      | fordringId       | FDR-59100              |

  Scenario: Tillægsfrister logges til revisionsloggen
    Given en fordring "FDR-59101" er under inddrivelse
    When en tillægsfrist af typen "INTERN_OPSKRIVNING" registreres for "FDR-59101"
    Then returneres HTTP 201
    And oprettes en CLS-revisionslogpost for "FDR-59101" med hændelsestypen "TILLAEGSFRIST_REGISTRERET"

  Scenario: Fejlende afbrydelsesregistrering logges ikke til revisionsloggen
    Given en fordring "FDR-59102" er under inddrivelse
    When en LOENINDEHOLDELSE-afbrydelse registreres for "FDR-59102" med afgoerelseRegistreret = false
    Then returneres HTTP 422
    And oprettes ingen CLS-revisionslogpost med hændelsestypen "AFBRYDELSE_REGISTRERET" for "FDR-59102"
