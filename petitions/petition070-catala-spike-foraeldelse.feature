@petition070
Feature: Catala Compliance Spike — G.A.2.4 Forældelse Indkodning (P070)

  # Type: Research spike — ingen produktionskode
  # Leverancer: Catala-kildefil, testsuite, spike-rapport
  # Verifikation: fil-system og indholdsassertioner på spike-leverancerne;
  #               ingen applikationsadfærd asserteres.
  # Juridisk grundlag: G.A.2.4.1–G.A.2.4.4.2 (v3.16, 2026-03-28),
  #                    GIL §§ 18, 18a, Forældelsesl. §§ 3, 5, 18–19, SKM2015.718.ØLR
  # G.A. snapshot-version: v3.16 (2026-03-28)
  # Tidsboks: 3 arbejdsdage (vs. standard 2 for P054 — begrundet i juridisk kompleksitet)
  # Prioritet: HØJESTE — igangsættes straks ved P059-godkendelse
  # Afhænger af: petition059 (forældelse)
  # Companion: 29 Gherkin-scenarier i petition059-foraeldelse.feature
  # Udenfor scope: G.A.2.4.5 (strafbare forhold), G.A.2.4.6 (indsigelse),
  #                runtime-integration, CI-pipeline, komplet G.A.-kapitelindkodning.

  # --- FR-1: Catala-indkodning af G.A.2.4.3 + G.A.2.4.1 — base frist og udskydelse ---

  Scenario: ga_2_4_foraeldelse.catala_da eksisterer og erklærer dansk Catala-dialekt
    Then filen "catala/ga_2_4_foraeldelse.catala_da" eksisterer i repositoriet
    And filen erklærer den danske Catala-dialekt "catala_da"
    And filen indeholder en versionsreference til "v3.16" og "2026-03-28"

  Scenario: Filen indkoder 3-årig base frist for alle PSRM- og DMI-fordringer
    Then filen "catala/ga_2_4_foraeldelse.catala_da" indeholder et Catala-regelblok
      for den 3-årige forældelsesfrist forankret til "GIL § 18a, stk. 4"
    And regelblokken gælder uanset retsgrundlag, herunder fordringer med dom eller forlig
    And filen indeholder et Catala-regelblok for den 10-årige undtagelsesfrist
      ved udlæg på fordring med særligt retsgrundlag forankret til "Forældelsesl. § 5, stk. 1"

  Scenario: Filen indkoder PSRM-udskydelse — forældelsesfrist tidligst fra 20-11-2021
    Then filen "catala/ga_2_4_foraeldelse.catala_da" indeholder et Catala-regelblok
      for PSRM-udskydelse forankret til "GIL § 18a, stk. 1, 1. pkt."
    And regelblokken angiver at forældelsesfristens begyndelsestidspunkt er tidligst "2021-11-20"
      for fordringer under inddrivelse fra "2015-11-19" eller senere
    And regelblokken angiver at forældelse tidligst kan indtræde den "2024-11-21"

  Scenario: Filen indkoder DMI/SAP38-udskydelse — forældelsesfrist tidligst fra 20-11-2027
    Then filen "catala/ga_2_4_foraeldelse.catala_da" indeholder et Catala-regelblok
      for DMI/SAP38-udskydelse forankret til "GIL § 18a, stk. 1, 2. pkt."
    And regelblokken angiver at forældelsesfristens begyndelsestidspunkt er tidligst "2027-11-20"
      for fordringer registreret i andet system end PSRM fra "2024-01-01" eller herefter
    And regelblokken angiver at forældelse tidligst kan indtræde den "2030-11-21"

  Scenario: Filen udtrykker at udskydelsesdatoen er uforanderlig og ikke kan tilsidesættes af afbrydelse
    Then filen "catala/ga_2_4_foraeldelse.catala_da" indeholder en Catala-betingelse eller constraint
      der eksplicit angiver at udskydelsesdatoen er en engangs-startdatopostponing
    And udskydelsesdatoen er ikke en afbrydelseshændelse og kan ikke nulstilles af efterfølgende hændelser
    And en afbrydelseshændelse der ellers ville give en kortere frist end udskydelsen
      medfører ikke at udskydelsesdatoen tilsidesættes

  # --- FR-2: Catala-indkodning af G.A.2.4.4.1 — afbrydelse ---

  Scenario: Filen indkoder berostillelse afgørelse som afbrydelse (GIL § 18a, stk. 8)
    Then filen "catala/ga_2_4_foraeldelse.catala_da" indeholder et Catala-regelblok
      for berostillelse-afbrydelse forankret til "GIL § 18a, stk. 8"
    And regelblokken angiver at ny forældelsesfrist er "eventDate + 3 år"
    And regelblokken angiver at berostillelse-afbrydelse kun gælder PSRM-forvaltede fordringer
      og ikke SAP38- eller DMI-fordringer

  Scenario: Filen afviser lønindeholdelsesvarsel som afbrydelsesgrundlag (SKM2015.718.ØLR)
    Then filen "catala/ga_2_4_foraeldelse.catala_da" indeholder et Catala-regelblok
      der eksplicit afviser lønindeholdelsesvarsel som gyldigt afbrydelsesgrundlag
    And regelblokket er forankret til "GIL § 18, stk. 4" og "SKM2015.718.ØLR"
    And en lønindeholdelsesafbrydelse uden "afgoerelseRegistreret = sand"
      resulterer i en afvisningsbetingelse — ikke en ny forældelsesfrist

  Scenario: Filen indkoder lønindeholdelse afgørelse som afbrydelse ved underretning til debitor
    Then filen "catala/ga_2_4_foraeldelse.catala_da" indeholder et Catala-regelblok
      for lønindeholdelse afgørelse som afbrydelse forankret til "GIL § 18, stk. 4"
    And regelblokket angiver at afbrydelsesdatoen er det tidspunkt, hvor underretning om afgørelsen
      når debitor — ikke varslingsdatoen
    And regelblokket angiver at afgørelsen skal angive fordringens art og størrelse
    And en lønindeholdelse der har været inaktiv i 1 år medfører at en ny 3-årig forældelsesfrist
      begynder fra berostillelsestidspunktet

  Scenario: Filen indkoder forgæves udlæg med samme afbrydelsesvirkning som vellykket udlæg
    Then filen "catala/ga_2_4_foraeldelse.catala_da" indeholder et Catala-regelblok
      for udlæg-afbrydelse forankret til "Forældelsesl. § 18, stk. 1"
    And regelblokket udtrykker eksplicit at forgæves udlæg (insolvenserklæring)
      ligestilles med vellykket udlæg i afbrydelsesvirkning
    And regelblokket skelner ikke mellem forgæves og vellykket udlæg
      i beregningen af den nye forældelsesfrist

  Scenario: Filen indkoder ny 10-årig forældelsesfrist ved udlæg på fordring med særligt retsgrundlag
    Then filen "catala/ga_2_4_foraeldelse.catala_da" indeholder et Catala-regelblok
      der angiver ny forældelsesfrist "eventDate + 10 år"
      for udlæg på fordring med "retsgrundlag = SAERLIGT_RETSGRUNDLAG"
    And regelblokket er forankret til "Forældelsesl. § 5, stk. 1"

  Scenario: Filen indkoder ny 3-årig forældelsesfrist ved udlæg uden særligt retsgrundlag
    Then filen "catala/ga_2_4_foraeldelse.catala_da" indeholder et Catala-regelblok
      der angiver ny forældelsesfrist "eventDate + 3 år"
      for udlæg på fordring med "retsgrundlag = ORDINARY"
    And regelblokket er forankret til "Forældelsesl. § 18, stk. 1"

  Scenario: Filen indkoder lønindeholdelse i bero i 1 år medfører ny 3-årig forældelsesfrist
    Then filen "catala/ga_2_4_foraeldelse.catala_da" indeholder et Catala-regelblok
      for den situation, at lønindeholdelse har været inaktiv i 1 år
    And regelblokket angiver at ny 3-årig forældelsesfrist begynder fra berostillelsestidspunktet
    And regelblokket er forankret til "GIL § 18, stk. 4"

  # --- FR-3: Catala-indkodning af G.A.2.4.2 — fordringskompleks-propagation ---

  Scenario: Filen indkoder fordringskompleks-propagation atomisk for alle kompleksmedlemmer
    Then filen "catala/ga_2_4_foraeldelse.catala_da" indeholder et Catala-regelblok
      for fordringskompleks-definition forankret til "GIL § 18a, stk. 2"
    And regelblokket udtrykker at afbrydelse af ét kompleksmedlem afbryder alle øvrige
      medlemmer af fordringskomplekset (GIL § 18a, stk. 2, 4. pkt.)
    And filen indeholder et Catala-udtryk der angiver atomicitetsforpligtelsen:
      partiel propagation er en fejlbetingelse — ikke en delvis succes

  Scenario: Filen afviser partiel fordringskompleks-propagation som fejlbetingelse
    Then filen "catala/ga_2_4_foraeldelse.catala_da" indeholder en Catala-constraint
      der angiver at en afbrydelsestransaktion der ikke propagerer til alle kompleksmedlemmer
      er ugyldig og skal rulles tilbage i sin helhed
    And atomicitetsforpligtelsen er forankret til "GIL § 18a, stk. 2"

  # --- FR-4: Catala-indkodning af G.A.2.4.4.2 — tillægsfrister ---

  Scenario: Filen indkoder tillægsfrist ved intern opskrivning (G.A.2.4.4.2.1)
    Then filen "catala/ga_2_4_foraeldelse.catala_da" indeholder et Catala-regelblok
      for tillægsfrist ved intern opskrivning forankret til "G.A.2.4.4.2.1"
    And regelblokket angiver at intern opskrivning tilføjer en 2-årig tillægsfrist
      til den løbende forældelsesfrist

  Scenario: Filen indkoder tillægsfrist-formlen max(currentFristExpires, eventDate) + 2 år
    Then filen "catala/ga_2_4_foraeldelse.catala_da" indeholder et Catala-udtryk
      der beregner tillægsfristen som "max(currentFristExpires, eventDate) + 2 år"
    And udtrykket er forankret til "G.A.2.4.4.2"
    And udtrykket anvender eksplicit "max()" over de to argumenter
      — ikke blot "eventDate + 2 år"

  Scenario: Udskydelsesgrænsen overtrumfer afbrydelse-forkortede frister og tillægsfrister
    Then filen "catala/ga_2_4_foraeldelse.catala_da" indeholder en Catala-betingelse
      der angiver at ingen hændelse — hverken afbrydelse eller tillægsfrist —
      kan sætte forældelsesfristens begyndelsestidspunkt tidligere end udskydelsesdatoen
    And betingelsen er forankret til "GIL § 18a, stk. 1"

  # --- FR-5: Catala-testsuite ---

  Scenario: ga_foraeldelse_tests.catala_da eksisterer og indeholder mindst 10 testcases
    Then filen "catala/tests/ga_foraeldelse_tests.catala_da" eksisterer i repositoriet
    And filen indeholder mindst 10 distinkte testcases udtrykt med Catala's indbyggede Test-modul
    And testcasene dækker den 3-årige base frist og den 10-årige undtagelsesfrist
    And testcasene dækker PSRM-udskydelsen med grænseværdidato "2021-11-20"
      (dagen før, på datoen og dagen efter)
    And testcasene dækker DMI/SAP38-udskydelsen med grænseværdidato "2027-11-20"
    And testcasene dækker udskydelsesimmutabilitet: en afbrydelse ændrer ikke udskydelsesdatoen
    And testcasene dækker berostillelse-afbrydelse (positiv case)
    And testcasene dækker den negative case: lønindeholdelsesvarsel afbryder IKKE (SKM2015.718.ØLR)
    And testcasene dækker lønindeholdelse afgørelse afbryder (positiv case)
    And testcasene dækker forgæves udlæg: ny frist identisk med vellykket udlæg
    And testcasene dækker fordringskompleks-propagation med mindst to kompleksmedlemmer
    And testcasene dækker tillægsfrist-formlen med begge forgreninger af max()-udtrykket:
      (a) currentFristExpires > eventDate og (b) eventDate > currentFristExpires

  # --- FR-6: Sammenligningsrapport mod P059 Gherkin (29 scenarier) ---

  Scenario: SPIKE-REPORT-070.md eksisterer og indeholder dækningstabel for alle 29 P059-scenarier
    Then filen "catala/SPIKE-REPORT-070.md" eksisterer i repositoriet
    And rapporten indeholder en tabel der afbilder hvert af P059's 29 Gherkin-scenarier
      fra "petitions/petition059-foraeldelse.feature" til en dækningsstatus
      af "Dækket", "Ikke dækket" eller "Uoverensstemmelse fundet"
    And alle 29 scenarier fra P059-featurefilen har en række i tabellen
    And rapporten indeholder et "Huller"-afsnit med regelgrene indkodet i Catala
      men ikke dækket af noget P059-scenarie, eller en eksplicit "Ingen fundet"-erklæring
    And rapporten indeholder et "Uoverensstemmelser"-afsnit med tilfælde, hvor et P059-scenarie
      modsiger G.A.-teksten som formaliseret, eller en eksplicit "Ingen fundet"-erklæring
    And rapporten indeholder et "Indsatsskøn"-afsnit med et person-dag-estimat og begrundelse
    And rapporten adresserer eksplicit alle fem discrepancy hotspots:
      (1) varsel vs afgørelse (SKM2015.718.ØLR),
      (2) fordringskompleks atomicitet,
      (3) udskydelse immutabilitet,
      (4) forgæves udlæg ligestilling og
      (5) tillægsfrist max()-formel

  # --- FR-7: Go/Nej-Go-anbefaling ---

  Scenario: SPIKE-REPORT-070.md indeholder eksplicit Go/Nej-Go-anbefaling med evidens for hvert kriterium
    Then filen "catala/SPIKE-REPORT-070.md" indeholder et "Go/Nej-Go"-afsnit
      med en entydig dom af "Go" eller "Nej-Go"
    And afsnittet indeholder evidens for om alle tre afbrydelsestyper
      indkodes uden tvetydighed
    And afsnittet indeholder evidens for om varsel/afgørelse-distinktionen
      (SKM2015.718.ØLR) indkodes rent i Catala
    And afsnittet indeholder evidens for om fordringskompleks-propagation
      er udtrykkelig i Catala uden workarounds
    And afsnittet indeholder evidens for om Catala-testkompilering lykkedes uden fejl
    And afsnittet indeholder evidens for om mindst én uoverensstemmelse eller hul
      er fundet relativt til P059 Gherkin
    And afsnittet indeholder evidens for hvert Nej-Go-trigger:
      temporale workarounds, fordringskompleks-workarounds og indsatsskøn per afsnit

  # --- NFR-3: G.A. citation snapshot-version ---

  Scenario: Alle G.A.-artikelcitater refererer til snapshot-version v3.16 dateret 2026-03-28
    Then filen "catala/ga_2_4_foraeldelse.catala_da" indeholder en versionsreference til "v3.16" og "2026-03-28"
    And ingen G.A.-citater i filen refererer til en anden snapshot-version

  # --- NFR-1: Catala CLI-kompilering afsluttes med kode 0 ---

  Scenario: ga_2_4_foraeldelse.catala_da kompilerer uden fejl
    Given Catala CLI er tilgængelig på eksekveringsstien
    When "catala ocaml ga_2_4_foraeldelse.catala_da" køres fra "catala/"-mappen
    Then kommandoen afsluttes med kode 0

  # --- NFR-4: Ingen produktionsartefakter modificeret ---

  Scenario: Ingen Java-kildefiler, databasemigreringer eller API-specifikationer er modificeret af spiket
    Then ingen Java-kildefiler er oprettet eller modificeret under nogen "src/main/java"-sti
    And ingen databasemigreringsscripts er tilføjet under nogen "src/main/resources/db/migration"-sti
    And ingen OpenAPI- eller Swagger-specifikationsfiler er modificeret
    And ingen Spring Boot-modulkonfigurationsfiler er oprettet eller modificeret
