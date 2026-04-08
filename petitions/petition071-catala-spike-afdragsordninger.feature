@petition071
Feature: Catala Compliance Spike — Afdragsordninger GIL § 11 (P071)

  # Type: Research spike — no production code
  # Deliverables: Catala-kildefil, testsuite, spike-rapport
  # Verification: fil-system og indholdsassertioner på spike-leverancer;
  #               ingen applikationsadfærd verificeres.
  # Legal basis: GIL § 11 stk. 1–2, stk. 6; GIL § 45; Gæld.bekendtg. chapter 7;
  #              G.A.3.1.1, G.A.3.1.1.1
  # G.A. snapshot version: v3.16 (2026-03-28)
  # Companion petition: P061 (afdragsordninger — fuld specifikation, 25 scenarier)
  # Out of scope: runtime-integration, CI-pipeline, kulanceaftale, virksomhedsregler,
  #               livscyklus-tilstandsmaskine, misligholdelsesdetektion.

  # ===========================================================================
  # FR-1: Catala-kodning af tabeltræk og lavindkomstgrænse (GIL § 11, stk. 1)
  # ===========================================================================

  Scenario: ga_3_1_1_afdragsordninger.catala_da eksisterer og kodificerer alle tabeltræks-under-regler
    # AC-1, AC-2: Filen er til stede og indeholder alle seks under-regler med artikelankre
    Så filen "catala/ga_3_1_1_afdragsordninger.catala_da" eksisterer i repositoriet
    Og filen erklærer den danske Catala-dialekt "catala_da"
    Og filen indeholder et Catala-regelblok for lavindkomstgrænse-guard forankret til "GIL § 11, stk. 1"
    Og filen indeholder et Catala-regelblok for tabeltræk uden forsørgerpligt forankret til "GIL § 11, stk. 1"
    Og filen indeholder et Catala-regelblok for tabeltræk med forsørgerpligt forankret til "GIL § 11, stk. 1"
    Og filen indeholder et Catala-regelblok for minimumsafdragsprocent uden forsørgerpligt (4%) forankret til "GIL § 11, stk. 1"
    Og filen indeholder et Catala-regelblok for minimumsafdragsprocent med forsørgerpligt (3%) forankret til "GIL § 11, stk. 1"
    Og filen indeholder et Catala-regelblok for indeksparameterisering forankret til "GIL § 45"

  Scenario: Tabeltræk — indkomst over lavindkomstgrænse giver korrekt afdragsprocent
    # AC-6: Tabeltræk-opslag over grænsen giver den lovbestemte procent
    # Indkomst 250.000 kr, ingen forsørgerpligt, år 2026: afdragsprocent skal være 13%
    Givet Catala-scope "Tabeltræk" evalueres med:
      | felt                | værdi  |
      | nettoindkomst       | 250000 |
      | forsørgerpligt      | false  |
      | lavindkomstgrænse   | 138500 |
      | indeksår            | 2026   |
    Så scopet returnerer afdragsprocent 13%
    Og scopet returnerer monthlyYdelse 2700 kr

  Scenario: Tabeltræk — indkomst under lavindkomstgrænse afvises
    # AC-2 (FR-1.1): Nettoindkomst 120.000 kr er under grænsen 138.500 kr → tabeltræk ikke mulig
    Givet Catala-scope "Tabeltræk" evalueres med:
      | felt                | værdi  |
      | nettoindkomst       | 120000 |
      | forsørgerpligt      | false  |
      | lavindkomstgrænse   | 138500 |
      | indeksår            | 2026   |
    Så scopet returnerer tabeltræk_mulig = false
    Og scopet returnerer månedlig_ydelse = 0 kr

  Scenario: Tabeltræk — indkomst præcis på lavindkomstgrænse accepteres (grænsetilfælde)
    # AC-11: Grænsetilfælde — nettoindkomst = 138.500 kr er netop tilladt
    Givet Catala-scope "Tabeltræk" evalueres med:
      | felt                | værdi  |
      | nettoindkomst       | 138500 |
      | forsørgerpligt      | false  |
      | lavindkomstgrænse   | 138500 |
      | indeksår            | 2026   |
    Så scopet returnerer tabeltræk_mulig = true
    Og scopet returnerer en afdragsprocent over 0%

  # ===========================================================================
  # FR-2: Catala-kodning af månedlig ydelse og afrunding (GIL § 11, stk. 2)
  # ===========================================================================

  Scenario: Afrunding — 250.000 kr × 13% afrundes NED til 2.700 kr pr. måned
    # AC-4, AC-6: Referencetilfælde uden forsørgerpligt
    # 250.000 × 13% = 32.500 kr/år; 32.500 / 12 = 2.708,33 kr → floor til 50 kr = 2.700 kr
    Givet Catala-scope "MånedligYdelse" evalueres med:
      | felt              | værdi  |
      | nettoindkomst     | 250000 |
      | afdragsprocent    | 13%    |
      | afrundingsenhed   | 50     |
    Så scopet returnerer råydelse_måned = 2708.33 kr
    Og scopet returnerer månedlig_ydelse = 2700 kr
    Og månedlig_ydelse er et multiplum af 50
    Og månedlig_ydelse er mindre end eller lig med råydelse_måned

  Scenario: Afrunding — 250.000 kr × 10% med forsørgerpligt afrundes NED til 2.050 kr pr. måned
    # AC-6: Referencetilfælde med forsørgerpligt
    # 250.000 × 10% = 25.000 kr/år; 25.000 / 12 = 2.083,33 kr → floor til 50 kr = 2.050 kr
    Givet Catala-scope "MånedligYdelse" evalueres med:
      | felt              | værdi  |
      | nettoindkomst     | 250000 |
      | afdragsprocent    | 10%    |
      | afrundingsenhed   | 50     |
    Så scopet returnerer råydelse_måned = 2083.33 kr
    Og scopet returnerer månedlig_ydelse = 2050 kr
    Og månedlig_ydelse er et multiplum af 50
    Og månedlig_ydelse er mindre end eller lig med råydelse_måned

  Scenario: Afrundingsenhed — høj indkomst bruger 100 kr-enhed
    # AC-5: Verificerer at 100 kr afrundingsenhed anvendes ved høje ydelser
    Givet Catala-scope "AfrundingsEnhed" evalueres med:
      | felt              | værdi  |
      | råydelse_måned    | 8250.75 |
      | grænsebeløb       | 5000   |
    Så scopet returnerer afrundingsenhed = 100
    Og scopet returnerer månedlig_ydelse = 8200 kr
    Og månedlig_ydelse er et multiplum af 100
    Og månedlig_ydelse er mindre end eller lig med råydelse_måned

  Scenario: Forsørgerpligt minimum — indkomstinterval der ville give 2% giver minimum 3%
    # AC-2 (FR-1.5): Minimumsafdragsprocent med forsørgerpligt er 3%
    # Uanset indkomstinterval kan afdragsprocent ikke komme under 3% med forsørgerpligt
    Givet Catala-scope "Tabeltræk" evalueres med:
      | felt                | værdi  |
      | nettoindkomst       | 145000 |
      | forsørgerpligt      | true   |
      | lavindkomstgrænse   | 138500 |
      | rå_afdragsprocent   | 2%     |
    Så scopet returnerer afdragsprocent = 3%
    Og afdragsprocent er ikke lavere end 3%

  Scenario: Ingen forsørgerpligt minimum — indkomstinterval der ville give 3% giver minimum 4%
    # AC-2 (FR-1.4): Minimumsafdragsprocent uden forsørgerpligt er 4%
    Givet Catala-scope "Tabeltræk" evalueres med:
      | felt                | værdi  |
      | nettoindkomst       | 145000 |
      | forsørgerpligt      | false  |
      | lavindkomstgrænse   | 138500 |
      | rå_afdragsprocent   | 3%     |
    Så scopet returnerer afdragsprocent = 4%
    Og afdragsprocent er ikke lavere end 4%

  # ===========================================================================
  # FR-3: Catala-kodning af konkret betalingsevnevurdering (GIL § 11, stk. 6)
  # ===========================================================================

  Scenario: Konkret betalingsevnevurdering — lavere end tabeltræk accepteres
    # AC-7 (FR-3.3): Konkret ydelse lavere end tabeltræk er gyldig
    Givet Catala-scope "KonkretBetalingsevne" evalueres med:
      | felt                       | værdi  |
      | nettoindkomst              | 250000 |
      | lavindkomstgrænse          | 138500 |
      | budgetskema_indsendt       | true   |
      | månedlig_indkomst          | 20833  |
      | månedlige_udgifter         | 18500  |
      | tabeltræks_reference_ydelse | 2700  |
    Så scopet returnerer konkret_ydelse = 2333 kr
    Og konkret_ydelse er lavere end tabeltræks_reference_ydelse
    Og scopet returnerer vurdering_mulig = true

  Scenario: Konkret betalingsevnevurdering — ingen budgetskema afvises
    # AC-7 (FR-3.2): Uden budgetskema er konkret vurdering ikke mulig
    Givet Catala-scope "KonkretBetalingsevne" evalueres med:
      | felt                  | værdi  |
      | nettoindkomst         | 250000 |
      | lavindkomstgrænse     | 138500 |
      | budgetskema_indsendt  | false  |
    Så scopet returnerer vurdering_mulig = false
    Og scopet returnerer afvisningsårsag "BUDGETSKEMA_MANGLER"

  Scenario: Konkret betalingsevnevurdering under lavindkomstgrænsen afvises
    # AC-7 (FR-3.1): Forguard — under lavindkomstgrænsen er konkret vurdering ikke tilgængelig
    Givet Catala-scope "KonkretBetalingsevne" evalueres med:
      | felt                  | værdi  |
      | nettoindkomst         | 120000 |
      | lavindkomstgrænse     | 138500 |
      | budgetskema_indsendt  | true   |
    Så scopet returnerer vurdering_mulig = false
    Og scopet returnerer afvisningsårsag "UNDER_LAVINDKOMSTGRAENSE"

  # ===========================================================================
  # FR-1 / Interaktion: Aktiv afdragsordning suspenderer lønindeholdelse (P061 sc. 9)
  # ===========================================================================

  Scenario: Catala-kodning dokumenterer at aktiv afdragsordning suspenderer lønindeholdelse
    # AC-2: Filen kodificerer interaktionsreglen (GIL § 11, G.A.3.1.1)
    Så filen "catala/ga_3_1_1_afdragsordninger.catala_da" indeholder et regelblok
      der udtrykker at AKTIV afdragsordning medfører suspension af lønindeholdelse
    Og regelblokket er forankret til "G.A.3.1.1"

  # ===========================================================================
  # FR-1 / Forældelse: Afdragsordning afbryder ikke forældelsesfrist (P061 sc. 26)
  # ===========================================================================

  Scenario: Catala-kodning dokumenterer at afdragsordning ikke afbryder forældelsesfrist
    # AC-2: Filen kodificerer forældelses-interaktionsreglen (G.A.2.4)
    Så filen "catala/ga_3_1_1_afdragsordninger.catala_da" indeholder et regelblok
      der udtrykker at afdragsordning ikke udgør et forældelsesbrud
    Og regelblokket er forankret til "G.A.2.4"
    Og regelblokket returnerer afbryder_foraeldelse = false

  # ===========================================================================
  # FR-1.6 / Indeksregulering: Tabel i kraft ved oprettelsestidspunktet bruges
  # ===========================================================================

  Scenario: Indeksregulering — tabel gældende på oprettelsesdatoen anvendes
    # AC-3: Indeksår er en parameter; afdragsordninger oprettet i 2026 bruger 2026-tabellen
    Givet Catala-scope "Tabeltræk" evalueres med:
      | felt                | værdi  |
      | nettoindkomst       | 250000 |
      | forsørgerpligt      | false  |
      | lavindkomstgrænse   | 138500 |
      | indeksår            | 2026   |
    Og Catala-scope "Tabeltræk" evalueres parallelt med indeksår = 2027 og lavindkomstgrænse = 142000
    Så de to scope-evalueringer returnerer potentielt forskellige afdragsprocenter
    Og den 2026-bundne afdragsordning bruger konsekvent 2026-parametre uanset 2027-tabel

  # ===========================================================================
  # FR-4: Catala-testsuite (≥ 8 tests)
  # ===========================================================================

  Scenario: ga_afdragsordninger_tests.catala_da eksisterer og indeholder mindst 8 tests
    # AC-9, AC-10, AC-11
    Så filen "catala/tests/ga_afdragsordninger_tests.catala_da" eksisterer i repositoriet
    Og filen indeholder mindst 8 testtilfælde udtrykt via Catala's indbyggede Test-modul
    Og testtilfælde dækker lavindkomstgrænse-guard for nettoindkomst præcis på grænsen
    Og testtilfælde dækker lavindkomstgrænse-guard for nettoindkomst én krone under grænsen
    Og testtilfælde dækker referencetilfældet 250.000 kr uden forsørgerpligt (forventet 2.700 kr)
    Og testtilfælde dækker referencetilfældet 250.000 kr med forsørgerpligt (forventet 2.050 kr)
    Og testtilfælde dækker minimumsafdragsprocent 3% ved forsørgerpligt
    Og testtilfælde dækker minimumsafdragsprocent 4% uden forsørgerpligt
    Og testtilfælde dækker konkret betalingsevnevurdering afvist under lavindkomstgrænse
    Og testtilfælde dækker konkret betalingsevnevurdering afvist uden budgetskema

  # ===========================================================================
  # FR-5: Sammenligningstrapport (SPIKE-REPORT-071.md)
  # ===========================================================================

  Scenario: SPIKE-REPORT-071.md eksisterer og indeholder P061-dækningstabel samt indsatsvurdering
    # AC-12, AC-13, AC-14
    Så filen "catala/SPIKE-REPORT-071.md" eksisterer i repositoriet
    Og rapporten indeholder en tabel, der mapper hvert P061-scenario til dækningsstatus "Dækket", "Ikke dækket" eller "Afvigelse fundet"
    Og hvert af de 25 P061-scenarier fra "petitions/petition061-afdragsordninger.feature" har en række i tabellen
    Og rapporten indeholder et "Huller"-afsnit med regelgrene kodet i Catala men ikke dækket af et P061-scenario, eller teksten "Ingen fundet"
    Og rapporten indeholder et "Afvigelser"-afsnit med tilfælde, hvor et P061-scenario modsiger G.A.-teksten, eller teksten "Ingen fundet"
    Og rapporten indeholder et "Indsatsvurdering"-afsnit med persondag-estimat og begrundelse

  # ===========================================================================
  # FR-6: Go/No-Go-anbefaling
  # ===========================================================================

  Scenario: SPIKE-REPORT-071.md indeholder eksplicit Go/No-Go-anbefaling med evidens for hvert kriterium
    # AC-15, AC-16
    Så filen "catala/SPIKE-REPORT-071.md" indeholder et "Go/No-Go"-afsnit med en utvetydig afgørelse "Go" eller "No-Go"
    Og afsnittet angiver evidens for om tabeltræk-opslag kodificeres uden tvetydighed
    Og afsnittet angiver evidens for om floor-afrunding (stk. 2) er udtrykkelig i Catala
    Og afsnittet angiver evidens for om lavindkomstgrænse-guard fungerer som Catala-prædikatsguard
    Og afsnittet angiver evidens for om indeksparameterisering er udtrykkelig i Catala
    Og afsnittet angiver evidens for om mindst 1 hul eller afvigelse er fundet ift. P061 Gherkin
    Og afsnittet angiver evidens for om Catala-testkompilering lykkedes uden fejl
    Og afsnittet angiver evidens for hvert No-Go-udløser: indeksparameterisering, afrundingstvetydighed og indsats per G.A.-afsnit

  # ===========================================================================
  # NFR-3: G.A.-citaters snapshot-version
  # ===========================================================================

  Scenario: Alle G.A.-artikelcitater refererer til snapshot-version v3.16 dateret 2026-03-28
    # AC-19
    Så filen "catala/ga_3_1_1_afdragsordninger.catala_da" indeholder en versionreference til "v3.16" og "2026-03-28"

  # ===========================================================================
  # NFR-1: Catala CLI-kompilering afslutter 0
  # ===========================================================================

  Scenario: ga_3_1_1_afdragsordninger.catala_da kompilerer uden fejl
    # AC-17
    Givet Catala CLI er tilgængeligt på eksekveringsstien
    Når "catala ocaml ga_3_1_1_afdragsordninger.catala_da" eksekveres fra "catala/"-mappen
    Så kommandoen afslutter med exitkode 0

  # ===========================================================================
  # NFR-4: Ingen produktionsartefakter ændret
  # ===========================================================================

  Scenario: Ingen Java-kildefiler, databasemigrationer eller API-specifikationer er ændret af dette spike
    # AC-20
    Så ingen Java-kildefiler er oprettet eller ændret under nogen "src/main/java"-sti
    Og ingen databasemigrationsscripts er tilføjet under nogen "src/main/resources/db/migration"-sti
    Og ingen OpenAPI- eller Swagger-specifikationsfiler er ændret
    Og ingen Spring Boot-modulkonfigurationsfiler er oprettet eller ændret
