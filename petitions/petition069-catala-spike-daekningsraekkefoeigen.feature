@petition069
Feature: Catala Compliance Spike — GIL § 4 Dækningsrækkefølge-kodning (P069)

  # Type: Research spike — no production code
  # Leverancer: Catala-kildefil, Catala-testsuite, spike-rapport
  # Verificering: fil-system- og indholdsassertioner på spike-leverancerne;
  #               ingen applikationsadfærd asserteres.
  # Legal basis: GIL § 4 stk. 1–4, GIL § 6a, GIL § 10b, Gæld.bekendtg. § 4 stk. 3,
  #              Gæld.bekendtg. § 7, Retsplejelovens § 507, Lov nr. 288/2022
  # G.A. snapshot version: v3.16 (2026-03-28)
  # Companion petition: P057 (dækningsrækkefølge — GIL § 4, fuld spec)
  # Out of scope: runtime integration, CI pipeline, DMI-paralleldrift, P059, P062.

  # ==============================================================================
  # FR-1: Catala-kodning af prioritetskategorier — GIL § 4, stk. 1
  # AC-1, AC-2, AC-3
  # ==============================================================================

  Scenario: ga_2_3_2_1_daekningsraekkefoeigen.catala_da eksisterer og koder alle fire prioritetskategorier i streng hierarkisk rækkefølge
    Then filen "catala/ga_2_3_2_1_daekningsraekkefoeigen.catala_da" eksisterer i repositoriet
    And filen deklarerer den danske Catala-dialekt "catala_da"
    And filen indeholder et Catala-regelblok for kategori 1 (rimelige omkostninger ved udenretlig inddrivelse i udlandet) forankret til "GIL § 6a, stk. 1" og "GIL § 4, stk. 12"
    And filen indeholder et Catala-regelblok for kategori 2 (bøder, tvangsbøder og tilbagebetalingskrav) forankret til "GIL § 10b" og "GIL § 4, stk. 1, nr. 1"
    And filen indeholder et Catala-regelblok for kategori 3 (underholdsbidrag) med prioritering af privatretlige forud for offentlige, forankret til "GIL § 4, stk. 1, nr. 2"
    And filen indeholder et Catala-regelblok for kategori 4 (andre fordringer) forankret til "GIL § 4, stk. 1, nr. 3"
    And regelblokke for kategori 1 til 4 er ordnet i streng hierarkisk rækkefølge i kilden

  Scenario: Tvangsbøder kodes eksplicit som kategori 2 med reference til lov nr. 288/2022
    Then filen "catala/ga_2_3_2_1_daekningsraekkefoeigen.catala_da" indeholder en eksplicit Catala-undtagelse der koder tvangsbøder som kategori 2
    And undtagelsen er forankret til "lov nr. 288 af 7. marts 2022, § 2, nr. 1"
    And undtagelsen refererer "SKM2021.507.GÆLDST" som baggrund for lovændringen

  Scenario: Privatretlige underholdsbidrag kodes som højere prioritet end offentlige inden for kategori 3
    Then filen "catala/ga_2_3_2_1_daekningsraekkefoeigen.catala_da" indeholder et Catala-regelblok der rangordner underholdsbidragstyper
    And "UNDERHOLDSBIDRAG_PRIVATRETLIG" har højere prioritet end "UNDERHOLDSBIDRAG_OFFENTLIG" i Catala-kodningen
    And regelblokket er forankret til "GIL § 4, stk. 1, nr. 2"

  # ==============================================================================
  # FR-2: FIFO-ordning og 6-niveauers rentesekvens — GIL § 4, stk. 2 + Gæld.bekendtg. § 4, stk. 3
  # AC-4, AC-5
  # ==============================================================================

  Scenario: FIFO-grundregel og pre-2013-særregel kodes med modtagelsesdato som sorteringsnøgle
    Then filen "catala/ga_2_3_2_1_daekningsraekkefoeigen.catala_da" indeholder et Catala-regelblok for FIFO-grundreglen forankret til "GIL § 4, stk. 2, 1. pkt."
    And filen indeholder et Catala-regelblok for pre-2013-særreglen der anvender den registrerede modtagelsesdato som FIFO-nøgle, forankret til "GIL § 4, stk. 2, 5. pkt."
    And renter-før-hovedkrav-reglen er forankret til "GIL § 4, stk. 2, 2. pkt."

  Scenario: Den 6-niveauers PSRM-rentesekvens kodes komplet i korrekt rækkefølge uden sammenslåning af positioner
    Then filen "catala/ga_2_3_2_1_daekningsraekkefoeigen.catala_da" indeholder distinkte Catala-regelblokke for alle seks under-positioner i rentesekvensen forankret til "Gæld.bekendtg. § 4, stk. 3"
    And position 1 koder opkrævningsrenter (påløbet under opkrævning)
    And position 2 koder inddrivelsesrenter beregnet af fordringshaver i medfør af "Gæld.bekendtg. § 9, stk. 3, 2. eller 4. pkt."
    And position 3 koder inddrivelsesrenter påløbet inden tilbageførsel i medfør af "Gæld.bekendtg. § 8, stk. 3"
    And position 4 koder inddrivelsesrenter påløbet under inddrivelsen i medfør af "Gæld.bekendtg. § 9, stk. 1"
    And position 5 koder øvrige renter beregnet af RIM i medfør af "Gæld.bekendtg. § 9, stk. 3, 1. eller 3. pkt."
    And position 6 koder hovedstolen
    And ingen af de seks positioner er slået sammen eller udeladt i Catala-kodningen

  # ==============================================================================
  # FR-3: Inddrivelsesindsats-regel og udlæg-undtagelse — GIL § 4, stk. 3
  # AC-6, AC-7
  # ==============================================================================

  Scenario: Inddrivelsesindsats-grundregel og overskudsregel kodes med forholdsmæssig fordeling
    Then filen "catala/ga_2_3_2_1_daekningsraekkefoeigen.catala_da" indeholder et Catala-regelblok for inddrivelsesindsats-grundreglen forankret til "GIL § 4, stk. 3, 1. pkt."
    And filen indeholder et Catala-regelblok for overskudsreglen (residualbeløb til øvrige indsats-fordringer af samme type) forankret til "GIL § 4, stk. 3, 2. pkt."
    And afdragsordning-undtagelsen (overskud fra afdragsordning kan dække tvangsbøder) er kodet som undtagelse fra overskudsreglen

  Scenario: Udlæg-undtagelse kodes som gensidigt eksklusiv med inddrivelsesindsats-grundreglen med forankring til retsplejelovens § 507
    Then filen "catala/ga_2_3_2_1_daekningsraekkefoeigen.catala_da" indeholder en Catala-undtagelsesregel der koder udlæg-undtagelsen
    And undtagelsen er forankret til "Retsplejelovens § 507"
    And undtagelsesreglen er gensidigt eksklusiv med inddrivelsesindsats-grundreglen i kilden
    And Catala-kodningen udtrykker at udlæg-residualbeløb ikke kan flyde til fordringer uden for udlægget

  # ==============================================================================
  # FR-4: Timingregel — GIL § 4, stk. 4
  # AC-8
  # ==============================================================================

  Scenario: Timingregel kodes med adskillelse af applikationstidspunkt og betalingstidspunkt
    Then filen "catala/ga_2_3_2_1_daekningsraekkefoeigen.catala_da" indeholder et Catala-regelblok for applikationstidspunktet forankret til "GIL § 4, stk. 4, 1. pkt."
    And filen indeholder et Catala-regelblok for virkningstidspunktet (betalingstidspunktet) forankret til "GIL § 4, stk. 4, 2. pkt."
    And de to regelblokke er adskilte og uafhængige i Catala-kodningen

  # ==============================================================================
  # FR-5: Opskrivningsfordring-placering — Gæld.bekendtg. § 7
  # AC-9
  # ==============================================================================

  Scenario: Opskrivningsfordring kodes til at arve stamfordringens FIFO-nøgle og placeres umiddelbart efter stamfordringen
    Then filen "catala/ga_2_3_2_1_daekningsraekkefoeigen.catala_da" indeholder en Catala-undtagelsesregel for opskrivningsfordringers placering forankret til "Gæld.bekendtg. § 7"
    And undtagelsesreglen koder at opskrivningsfordringen placeres umiddelbart efter sin stamfordring
    And undtagelsesreglen koder at opskrivningsfordringen arver stamfordringens FIFO-sorteringsnøgle — ikke sin egen modtagelsesdato
    And undtagelsesreglen tilsidesætter FIFO-grundreglen i FR-2.1 for opskrivningsfordringer
    And filen indeholder en regel for indbyrdes FIFO-ordning af flere opskrivningsfordringer for samme stamfordring i medfør af "GIL § 4, stk. 2"

  # ==============================================================================
  # FR-6: Catala-testsuite — mindst 8 tests dækkende alle regelgrene
  # AC-10, AC-11
  # ==============================================================================

  Scenario: ga_daekningsraekkefoeigen_tests.catala_da eksisterer og indeholder mindst 8 tests der dækker alle regelgrene og diskrepans-hotspots
    Then filen "catala/tests/ga_daekningsraekkefoeigen_tests.catala_da" eksisterer i repositoriet
    And filen indeholder mindst 8 distinkte testcases udtrykt via Catala's indbyggede Test-modul
    And testcasene dækker alle fire FR-1 prioritetskategorier inklusiv tvangsbøde som kategori 2
    And testcasene dækker FR-2.1 FIFO-ordning: en fordring med ældste modtagelsesdato dækkes før en med nyere
    And testcasene dækker FR-2.4: alle seks under-positioner i rentesekvensen testes i komplet sekvens
    And testcasene dækker FR-3.1 inddrivelsesindsats-grundregel og FR-3.2 overskudsregel
    And testcasene dækker FR-3.4 udlæg-undtagelse: residualbeløb dækker ikke fordringer uden for udlægget
    And testcasene dækker FR-5.1 opskrivningsfordring-placering umiddelbart efter stamfordring
    And testcasene dækker FR-4 timingregel: rækkefølge fastlægges på applikationstidspunkt
    And testcasene inkluderer en boundary-case: enkelt fordring med fuld betaling

  # ==============================================================================
  # FR-7: Sammenligningsrapport mod P057 Gherkin-scenarier
  # AC-12, AC-13, AC-14, AC-15
  # ==============================================================================

  Scenario: SPIKE-REPORT-069.md eksisterer og indeholder en dækningsoversigt for alle 22 P057-scenarier samt alle fem rapportafsnit
    Then filen "catala/SPIKE-REPORT-069.md" eksisterer i repositoriet
    And rapporten indeholder en tabel der mapper hvert af P057's 22 Gherkin-scenarier fra "petitions/petition057-daekningsraekkefoeigen.feature" til en dækningsstatus af "Dækket", "Ikke dækket" eller "Diskrepans fundet"
    And hvert P057-scenarie i FR-1 til FR-8 har en rad i tabellen
    And rapporten indeholder et "Huller"-afsnit med regelgrene kodet i Catala men ikke dækket af noget P057-scenarie, eller eksplicit "Ingen fundet"
    And rapporten indeholder et "Diskrepanser"-afsnit med tilfælde hvor et P057-scenarie tilsyneladende modsiger G.A.-teksten, eller eksplicit "Ingen fundet"
    And rapporten indeholder et "Estimat"-afsnit med personedage-estimat for kodning af det samlede G.A. Inddrivelse-kapitel og en begrundelse

  Scenario: SPIKE-REPORT-069.md indeholder eksplicit vurdering af alle tre diskrepans-hotspots
    Then "catala/SPIKE-REPORT-069.md" indeholder et "Diskrepans-hotspots"-afsnit
    And afsnittet indeholder en eksplicit vurdering af hotspot 1: 6-niveauers rentesekvens (FR-2.4) — om alle seks positioner er korrekt repræsenteret i P057
    And afsnittet indeholder en eksplicit vurdering af hotspot 2: udlæg-undtagelse (FR-3.4) — om udlæg-residualbeløbet er korrekt isoleret i P057
    And afsnittet indeholder en eksplicit vurdering af hotspot 3: opskrivningsfordring-placering (FR-5.2) — om opskrivningsfordringen arver præcis FIFO-nøgle i P057
    And hvert hotspot har en klar finding: "Fund" eller "Ingen diskrepans"

  # ==============================================================================
  # FR-8: Go/No-Go-anbefaling
  # AC-16, AC-17
  # ==============================================================================

  Scenario: SPIKE-REPORT-069.md indeholder en eksplicit Go/No-Go-anbefaling med dokumentation for alle kriterier
    Then filen "catala/SPIKE-REPORT-069.md" indeholder et "Go/No-Go"-afsnit med en entydig verdict af "Go" eller "No-Go"
    And afsnittet giver dokumentation for om alle fire prioritetskategorier er kodet uden tvetydighed
    And afsnittet giver dokumentation for om mindst 1 diskrepans eller hul er fundet relativt til P057 Gherkin
    And afsnittet giver dokumentation for om Catala-testkompilering lykkedes uden fejl
    And afsnittet giver dokumentation for om OCaml-ekstraktion producerede kørbar kode
    And afsnittet giver dokumentation for hvert No-Go-trigger: om 6-niveauers rentesekvens kræver workarounds, om juridiske tvetydigheder blokerede formel kodning, og om kodningsindsats per G.A.-afsnit oversteg 4 personedage

  # ==============================================================================
  # NFR-3: G.A.-snapshot-versionscitater
  # AC-20
  # ==============================================================================

  Scenario: Alle G.A.-artikel-citater refererer snapshot version v3.16 dateret 2026-03-28
    Then filen "catala/ga_2_3_2_1_daekningsraekkefoeigen.catala_da" indeholder en versionreference til "v3.16" og "2026-03-28"
    And ingen G.A.-citater i kildefilen refererer et andet snapshot end v3.16 (2026-03-28)

  # ==============================================================================
  # NFR-1: Catala CLI-kompilering afsluttes med exit-kode 0
  # AC-18
  # ==============================================================================

  Scenario: ga_2_3_2_1_daekningsraekkefoeigen.catala_da kompilerer uden fejl
    Given Catala CLI er tilgængelig på eksekverings-PATH
    When "catala ocaml ga_2_3_2_1_daekningsraekkefoeigen.catala_da" eksekveres fra "catala/"-mappen
    Then kommandoen afsluttes med exit-kode 0

  Scenario: ga_daekningsraekkefoeigen_tests.catala_da eksekverer med alle tests bestående
    Given Catala CLI er tilgængelig på eksekverings-PATH
    When "catala test-doc catala/tests/ga_daekningsraekkefoeigen_tests.catala_da" eksekveres
    Then kommandoen afsluttes med exit-kode 0
    And alle testcases rapporteres som PASS

  # ==============================================================================
  # NFR-4: Ingen produktionsartefakter ændret
  # AC-21
  # ==============================================================================

  Scenario: Ingen Java-kildefiler, database-migrationsscripts eller API-specifikationer er ændret af spiket
    Then ingen Java-kildefiler er oprettet eller ændret under noget "src/main/java"-sti
    And ingen database-migrationsscripts er tilføjet under noget "src/main/resources/db/migration"-sti
    And ingen OpenAPI- eller Swagger-specifikationsfiler er ændret
    And ingen Spring Boot-modulkonfigurationsfiler er oprettet eller ændret
