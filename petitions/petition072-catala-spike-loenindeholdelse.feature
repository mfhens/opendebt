@petition072
Feature: Catala Compliance Spike — Lønindeholdelsesprocent Gæld.bekendtg. § 14 (P072)

  # Type: Research spike — ingen produktionskode
  # Leverancer: Catala-kildefiler, testsuite, spikerapport
  # Verifikation: filsystem- og indholdspåstande på spikeleverancerne;
  #               ingen applikationsadfærd påstås.
  # Juridisk grundlag: G.A.3.1.2.5 (v3.16 2026-03-28), Gæld.bekendtg. §§ 11, 14;
  #                    GIL §§ 10, 10a; kildeskattelovens § 48, stk. 5; SKM2015.718.ØLR
  # G.A.-snapshot: v3.16 (2026-03-28)
  # Companion til: P062 (lønindeholdelse — fuld spec)
  # Afhænger af: P062 (Gherkin-scenarier er sammenligningsgrundlaget)
  # Uden for scope: eSkattekortet HTTP-integration, runtime, CI-pipeline, fuld G.A.3.1.2-dækning.

  # --- FR-1 og FR-4: Standardformel og konkret eksempelreproduktion ---

  Scenario: ga_3_1_2_loenindeholdelse_pct.catala_da eksisterer og koder standardformlen med artikelankere
    Then filen "catala/ga_3_1_2_loenindeholdelse_pct.catala_da" eksisterer i repositoriet
    And filen erklærer den danske Catala-dialekt "catala_da"
    And filen indeholder et Catala-regelblok for standardformlen forankret til "Gæld.bekendtg. § 14, stk. 3, 2.–4. pkt."
    And filen indeholder et Catala-regelblok for nedrunding (floor) forankret til "Gæld.bekendtg. § 14, stk. 3, 5. pkt."
    And filen indeholder et Catala-regelblok for maksimumsbegrænsning forankret til "Gæld.bekendtg. § 14, stk. 3, 9. pkt."
    And alle G.A.-citationer refererer til snapshot "v3.16" og "2026-03-28"

  Scenario: Catala-programmet reproducerer det konkrete G.A.3.1.2.5-eksempel med 18 % (AC-3)
    # Formel: (10 × 400.000) / ((400.000 − 48.000) × (1 − 0,37)) = 4.000.000 / 221.760 = 18,04 % → 18 %
    # Juridisk kilde: G.A.3.1.2.5 (v3.16 2026-03-28), Gæld.bekendtg. § 14, stk. 3
    Given Catala-programmet er kompileret fra "catala/ga_3_1_2_loenindeholdelse_pct.catala_da"
    When programmet eksekveres med input:
      | felt            | værdi   |
      | afdragsprocent  | 10      |
      | nettoindkomst   | 400000  |
      | fradragsbeloeb  | 48000   |
      | traekprocent    | 37      |
    Then outputtet er loenindeholdelsesprocent = 18
    And beregningsresultatet er nedrundes fra 18,04 %

  Scenario: Nedrunding er altid floor — aldrig standard-afrunding (AC-4, SKM2015.718.ØLR)
    # Et resultat på 18,99 % skal give 18, ikke 19.
    # SKM2015.718.ØLR fastslår: lønindeholdelsesprocent nedrundes altid.
    Given Catala-programmet modtager input der giver en formelværdi på 18,99 %
    When programmet eksekveres
    Then outputtet er loenindeholdelsesprocent = 18
    And filen indeholder et Catala-regelblok der bekræfter "aldrig afrunding opad"
    And testsuiten "catala/tests/ga_loenindeholdelse_tests.catala_da" indeholder en testcase der verificerer floor-semantik

  Scenario: Fast-punkt-præcision — mellemresultater valideres mod loveksemplet (diskrepanspunkt 2)
    # Fast-punkt-aritmetik er lovpåkrævet; floating-point kan give ±1 % fejl.
    Given Catala-programmet er kompileret med fast-punkt-aritmetik for alle mellemresultater
    When programmet eksekveres med input afdragsprocent=10, nettoindkomst=400000, fradragsbeloeb=48000, traekprocent=37
    Then den interne repræsentation af "4.000.000 / 221.760" er fastpunkts-præcis
    And outputtet stemmer overens med loveksemplet til nærmeste hele procent
    And spikerapporten "catala/SPIKE-REPORT-072.md" dokumenterer fast-punkt vs. floating-point-diskrepanspunktet

  # --- FR-2: Frikort-kanttilfælde ---

  Scenario: Beregning med trækprocent lig 0 — frikort uden skatteforpligtelse (AC-7 variant)
    # Denominator-faktor = (1 − 0) = 1; standardformlen anvendes uændret.
    # Juridisk kilde: G.A.3.1.2.5 (v3.16 2026-03-28)
    Given Catala-programmet modtager input:
      | felt            | værdi  |
      | afdragsprocent  | 5      |
      | nettoindkomst   | 200000 |
      | fradragsbeloeb  | 30000  |
      | traekprocent    | 0      |
    When programmet eksekveres
    Then standardformlen anvendes med denominator-faktor = 1
    And outputtet er loenindeholdelsesprocent = floor(5 × 200000 / (200000 − 30000))
    And testsuiten indeholder en testcase for trækprocent = 0

  Scenario: Frikort-kant — nettoindkomst lig fradragsbeløb udløser fallback til bruttoindkomstbasis (AC-7)
    # (nettoindkomst − fradragsbeløb) = 0 → denominator ≤ 0 → fallback: lønindeholdelsesprocent = afdragsprocent
    # Juridisk kilde: G.A.3.1.2.5 (v3.16 2026-03-28) — beregning ved bruttoindkomstbasis
    Given Catala-programmet modtager input:
      | felt            | værdi |
      | afdragsprocent  | 5     |
      | nettoindkomst   | 48000 |
      | fradragsbeloeb  | 48000 |
      | traekprocent    | 0     |
    When programmet eksekveres
    Then systemet aktiverer bruttoindkomst-fallback
    And outputtet er loenindeholdelsesprocent = 5
    And ingen division-med-nul-fejl opstår
    And filen indeholder en Catala-vagtkondition forankret til "G.A.3.1.2.5"

  Scenario: Frikort-vagtkonditionerne er til stede i Catala-kildefilen (diskrepanspunkt 3)
    # Frikort-denominatorguard er ofte udeladt i implementationer — spiket verificerer den er kodet.
    Then filen "catala/ga_3_1_2_loenindeholdelse_pct.catala_da" indeholder en Catala-regel for fallback ved "(nettoindkomst − fradragsbeløb) ≤ 0"
    And filen indeholder en Catala-regel for trækprocent = 0-håndtering med denominator-faktor = 1
    And spikerapporten dokumenterer frikort-vagtkonditionens tilstedeværelse som diskrepanspunkt 3-vurdering

  # --- FR-1 (cap) og FR-3: Maksimumsbegrænsning og reduceret sats ---

  Scenario: Lønindeholdelsesprocent begrænses af den lovbestemte maksimumsprocent (AC-5, diskrepanspunkt 4)
    # Cap: trækprocent + lønindeholdelsesprocent ≤ 100 % (kildeskattelovens § 48, stk. 5)
    # Eksempel: trækprocent = 40 % → maksimal lønindeholdelsesprocent = 60 %
    # Nedrunding gælder det cappede resultat.
    Given Catala-programmet modtager input der giver en formelværdi over 60 % med traekprocent = 40
    When programmet eksekveres
    Then outputtet er loenindeholdelsesprocent = 60
    And filen indeholder et Catala-regelblok der anvender cap FØR outputtet returneres
    And nedrunding gælder det cappede resultat, ikke det uafkortede
    And spikerapporten dokumenterer cap-rækkefølge (før/efter nedrunding) som diskrepanspunkt 4-vurdering

  Scenario: Høj indkomst — resultat korrekt cappes til maksimum (AC-5 variant)
    # nettoindkomst = 800.000 kr., fradragsbeloeb = 40.000 kr., traekprocent = 38 %, afdragsprocent = 15 %
    # Ufiltret resultat kan overskride (100 − 38) % = 62 % maksimum.
    Given Catala-programmet modtager input:
      | felt            | værdi  |
      | afdragsprocent  | 15     |
      | nettoindkomst   | 800000 |
      | fradragsbeloeb  | 40000  |
      | traekprocent    | 38     |
    When programmet eksekveres
    Then outputtet er lønindeholdelsesprocent ≤ 62
    And hvis formelværdien oversteg 62, er outputtet præcis 62

  Scenario: Kvalificerende fordringtype giver reduceret lønindeholdelsesprocent (AC-9, diskrepanspunkt 5)
    # GIL § 10a — reduceret sats for bestemte fordringtyper.
    Given Catala-programmet modtager en fordringtype klassificeret som "kvalificerende_reduceret_sats" under "GIL § 10a"
    And standardberegningen giver en lønindeholdelsesprocent på 18
    When programmet eksekveres med reduceret-sats-flag = sand
    Then outputtet er den reducerede loenindeholdelsesprocent (lavere end 18)
    And filen indeholder et Catala-klassifikationspredikat forankret til "GIL § 10a"

  Scenario: Ikke-kvalificerende fordringtype giver standardlønindeholdelsesprocent (AC-9 negativ)
    # Standardsatsen gælder for fordringer der ikke er oplistet under GIL § 10a.
    Given Catala-programmet modtager en fordringtype klassificeret som "ikke_kvalificerende"
    When programmet eksekveres med reduceret-sats-flag = falsk
    Then outputtet er standardloenindeholdelsesprocenten (18 for standardeksempel-input)
    And den reducerede sats er ikke anvendt

  # --- FR-3 og NFR-5: eSkattekortet-parameter-injektion og CPR-isolation ---

  Scenario: Ny trækprocent fra eSkattekortet giver ny beregning uden CPR-persistering (NFR-5)
    # Trækprocent injiceres som parameter — ingen HTTP-kald i Catala.
    # CPR behandles som ephemært input og persisteres ikke i resultatet.
    Given Catala-programmet er parametriseret med trækprocent = 37 og returnerer 18
    When Catala-programmet eksekveres påny med opdateret trækprocent = 30 og identiske øvrige input
    Then det nye output er loenindeholdelsesprocent = floor((10 × 400000) / ((400000 − 48000) × 0,70))
    And CPR er ikke inkluderet i Catala-programmets output eller mellemresultater
    And spikerapporten bekræfter at "trækprocent injiceres som parameter — ingen CPR-persistering"

  Scenario: CPR-isolation verificeret — personen refereres ved UUID, CPR persisteres ikke
    # GDPR-krav: CPR-nummer må aldrig persisteres uden for Person Registry.
    # Catala-programmet modtager person_id (UUID), ikke CPR.
    Then filen "catala/ga_3_1_2_loenindeholdelse_pct.catala_da" indeholder ikke variabelnavnet "cpr"
    And filen refererer til debitor via person_id (UUID) i alle scopes
    And spikerapporten bekræfter CPR-isolation som en formel Catala-modelleret egenskab

  # --- FR-4: Afdragsprocent fra forsørgerpligt-bracket (P071-afhængighed) ---

  Scenario: Afdragsprocent fra forsørgerpligt-bracket (3 %) injiceres korrekt i formlen
    # Lavindkomstgrænse-bracket: afdragsprocent = 3 (forsørger) eller 4 (ikke-forsørger).
    # P072 modtager afdragsprocent som parameter fra P071-tabeltræk.
    Given Catala-programmet modtager afdragsprocent = 3 (forsørgerpligt-bracket) fra P071-output
    And nettoindkomst = 150000, fradragsbeloeb = 35000, traekprocent = 32
    When programmet eksekveres
    Then outputtet er loenindeholdelsesprocent = floor((3 × 150000) / ((150000 − 35000) × (1 − 0,32)))
    And testsuiten indeholder en testcase for forsørgerpligt-bracket-afdragsprocent

  # --- FR-5 og FR-6: Spikerapport og kompilering ---

  Scenario: ga_loenindeholdelse_tests.catala_da eksisterer og indeholder mindst 8 testcases
    Then filen "catala/tests/ga_loenindeholdelse_tests.catala_da" eksisterer i repositoriet
    And filen indeholder mindst 8 testcases udtrykt med Catala's built-in Test-modul
    And testcasene dækker standardformlen (FR-4.1), nedrunding (FR-4.2), frikort-trækprocent=0 (FR-4.3),
      frikort-fallback (FR-4.4), cap (FR-4.5), reduceret sats (FR-4.6), fast-punkt (FR-4.7) og
      forsørgerpligt-afdragsprocent (FR-4.8)

  Scenario: SPIKE-REPORT-072.md eksisterer og indeholder P062-sammenligningstabell og indsatsskøn
    Then filen "catala/SPIKE-REPORT-072.md" eksisterer i repositoriet
    And rapporten indeholder en tabel der afbilder hvert P062 FR-2 og FR-3 Gherkin-scenarie til
      dækningsstatus "Dækket", "Ikke dækket" eller "Uoverensstemmelse fundet"
    And hvert scenarie fra "petitions/petition062-loenindeholdelse-fuld-spec.feature" der omhandler
      formelberegning eller frikort har en række i tabellen
    And rapporten indeholder et "Huller"-afsnit med regelgrene kodet i Catala men ikke i P062, eller "Ingen fundet"
    And rapporten indeholder et "Uoverensstemmelser"-afsnit med tilfælde hvor P062 modsiger G.A.-teksten, eller "Ingen fundet"
    And rapporten indeholder et "Diskrepanspunkter"-afsnit der eksplicit vurderer alle fem identificerede risici
    And rapporten indeholder et "Indsatsskøn"-afsnit med persondag-estimat og rationale

  Scenario: SPIKE-REPORT-072.md indeholder eksplicit Go/No-Go-anbefaling med evidens
    Then filen "catala/SPIKE-REPORT-072.md" indeholder et "Go/No-Go"-afsnit med et utvetydigt udfald af "Go" eller "No-Go"
    And afsnittet giver evidens for om § 14-formlen kodedes uden tvetydighed
    And afsnittet giver evidens for om nedrunding og fast-punkt-aritmetik kan udtrykkes i Catala
    And afsnittet giver evidens for om frikort-vagtkondition kan udtrykkes rent
    And afsnittet giver evidens for om det konkrete 18 %-eksempel reproduceres korrekt i Catala-tests
    And afsnittet giver evidens for om mindst 1 uoverensstemmelse fundet vs. P062 Gherkin
    And afsnittet giver evidens for om Catala kompilerede uden fejl
    And afsnittet giver evidens for hvert No-Go-trigger: fast-punkt-bibliotekskrav, frikort-workarounds og indsatsoverskridelse

  # --- NFR-1 og NFR-3: Kompilering og G.A.-versionscitationer ---

  Scenario: ga_3_1_2_loenindeholdelse_pct.catala_da kompilerer uden fejl
    Given Catala CLI er tilgængeligt på eksekveringsstien
    When "catala ocaml ga_3_1_2_loenindeholdelse_pct.catala_da" eksekveres fra "catala/"-mappen
    Then kommandoen afslutter med exit-kode 0

  Scenario: Alle G.A.-artikelcitationer refererer til snapshot v3.16 dateret 2026-03-28
    Then filen "catala/ga_3_1_2_loenindeholdelse_pct.catala_da" indeholder en versionsreference til "v3.16" og "2026-03-28"
    And filen "catala/tests/ga_loenindeholdelse_tests.catala_da" indeholder en versionsreference til "v3.16" og "2026-03-28"

  # --- NFR-4: Ingen produktionsartefakter modificeret ---

  Scenario: Ingen Java-kildefiler, databasemigrationer eller API-specifikationer modificeres af spiket
    Then ingen Java-kildefiler er oprettet eller modificeret under en "src/main/java"-sti
    And ingen databasemigrationsscripts er tilføjet under en "src/main/resources/db/migration"-sti
    And ingen OpenAPI- eller Swagger-specifikationsfiler er modificeret
    And ingen Spring Boot-modulkonfigurationsfiler er oprettet eller modificeret
    And ingen CPR-numre er persisteret i nogen fil produceret af spiket
