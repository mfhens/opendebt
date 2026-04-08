@petition062
Feature: Lønindeholdelse fuld specifikation — GIL § 10/§ 14 (P062)

  # Legal basis: G.A.3.1.2 (v3.16 2026-03-28), GIL §§ 10, 10a; Gæld.bekendtg. §§ 11, 14;
  # Forældelsesl. §§ 18-19; SKM2015.718.ØLR.
  # Extends: Petition 007 (basic inddrivelsesskridt lifecycle — implemented).
  # Depends on: Petition 059 (forældelse model), Petition 061 (afdragsordning suspension model).
  # Catala companion: Petition 072 (Tier A — lønindeholdelsesprocent formula).
  # Out of scope: henstand automation (G.A.3.1.2.4.5), insolvens (G.A.3.1.2.6), P072 Catala encoding.
  # AC-6 (formula inputs audit): verified by unit tests, not re-tested in every Gherkin scenario.
  # AC-25 (CPR isolation): verified by GDPR audit scanner; scenario included for BDD coverage.

  # --- FR-1: Eligible fordringer — pre-initiation validation ---

  Scenario: Offentligretlig fordring accepteres ved initiering af lønindeholdelse
    Given sagen "SAG-62001" har debitoren "DBT-001" med person_id "uuid-debtor-001"
    And fordringen "FDR-62001" på sagen "SAG-62001" er en offentligretlig fordring
    And sagsbehandleren "SB-01" er autentificeret med rollen "CASEWORKER"
    When sagsbehandleren sender POST /api/v1/loenindeholdelse for debitor "DBT-001" med fordringen "FDR-62001"
    Then systemet accepterer anmodningen og opretter en loenindeholdelse-sag
    And loenindeholdelse-sagen returnerer en sags-UUID og en beregnet loenindeholdelsesprocent

  Scenario: Civilretlig fordring uden eksekutionsfundament afvises ved initiering
    Given sagen "SAG-62002" har debitoren "DBT-002" med person_id "uuid-debtor-002"
    And fordringen "FDR-62002" på sagen "SAG-62002" er en civilretlig fordring uden eksekutionsfundament
    And sagsbehandleren "SB-01" er autentificeret med rollen "CASEWORKER"
    When sagsbehandleren sender POST /api/v1/loenindeholdelse for debitor "DBT-002" med fordringen "FDR-62002"
    Then systemet returnerer HTTP 422
    And fejlresponsen identificerer fordringen "FDR-62002" som ikke-eligible med årsagen "INGEN_EKSEKUTIONSFUNDAMENT"

  Scenario: Sagsbehandlerportalen viser ikke-eligible fordringer som deaktiverede
    Given sagen "SAG-62003" indeholder:
      | fordring_id | type             | eksekutionsfundament |
      | FDR-62010   | offentligretlig  | N/A                  |
      | FDR-62011   | civilretlig      | nej                  |
    And sagsbehandleren "SB-01" navigerer til initiering af loenindeholdelse for sagen "SAG-62003"
    Then portalen viser "FDR-62010" som valgbar
    And portalen viser "FDR-62011" som deaktiveret med tooltip "Ingen eksekutionsfundament"

  # --- FR-2: Lønindeholdelsesprocent calculation (Gæld.bekendtg. § 14, stk. 2) ---

  Scenario: Korrekt beregning af loenindeholdelsesprocent med konkrete vaerdier
    # AC-3: (10% * 400000) / ((400000 - 48000) * (100% - 37%)) = 40000 / 221760 = 18.04% -> floor = 18%
    Given debitor "DBT-003" har følgende eSkattekortet-data:
      | felt              | vaerdi        |
      | nettoindkomst     | 400000        |
      | fradragsbeloeb    | 48000         |
      | traekprocent      | 37            |
    And tabeltræk for nettoindkomst 400000 DKK giver afdragsprocent 10
    When systemet beregner loenindeholdelsesprocenten for debitor "DBT-003"
    Then den beregnede loenindeholdelsesprocent er 18
    And beregningsresultatet er afrundet ned fra 18.04%

  Scenario Outline: Beregning af loenindeholdelsesprocent for forskellige indkomstprofiler (AC-3, AC-5)
    # All results rounded down (floor), never up.
    Given debitor "<debitor_id>" har eSkattekortet-data med nettoindkomst <nettoindkomst>, fradragsbeloeb <fradragsbeloeb>, traekprocent <traekprocent>
    And tabeltræk giver afdragsprocent <afdragsprocent> for denne indkomst
    When systemet beregner loenindeholdelsesprocenten for debitor "<debitor_id>"
    Then den beregnede loenindeholdelsesprocent er <forventet_procent>

    Examples:
      | debitor_id | nettoindkomst | fradragsbeloeb | traekprocent | afdragsprocent | forventet_procent |
      | DBT-010    | 300000        | 40000          | 35           | 8              | 14                |
      | DBT-011    | 500000        | 60000          | 40           | 12             | 22                |
      | DBT-012    | 250000        | 36000          | 32           | 7              | 12                |

  Scenario: Loenindeholdelsesprocent afrundes altid ned — aldrig op (AC-5, SKM2015.718.ØLR)
    # Formula result is 18.99% -> must yield 18, not 19.
    Given debitor "DBT-020" har eSkattekortet-data der giver en formel-vaerdi på 18.99%
    When systemet beregner loenindeholdelsesprocenten for debitor "DBT-020"
    Then den beregnede loenindeholdelsesprocent er 18
    And systemet har ikke afrundet opad til 19

  Scenario: Beregning er deterministisk — samme input giver altid samme resultat (AC-4)
    Given debitor "DBT-021" har nettoindkomst 400000, fradragsbeloeb 48000, traekprocent 37, afdragsprocent 10
    When systemet beregner loenindeholdelsesprocenten to gange med identiske input
    Then begge beregninger returnerer 18
    And resultaterne er identiske bit-for-bit

  # --- FR-3: Frikort threshold handling ---

  Scenario: Beregning falder tilbage til bruttoindkomstbasis for debitor med frikort (AC-7)
    # fradragsbeloeb >= nettoindkomst -> denominator <= 0 -> fallback to afdragsprocent directly
    Given debitor "DBT-030" har frikort med:
      | felt           | vaerdi |
      | nettoindkomst  | 48000  |
      | fradragsbeloeb | 48000  |
      | traekprocent   | 0      |
    And tabeltræk for nettoindkomst 48000 DKK giver afdragsprocent 5
    When systemet beregner loenindeholdelsesprocenten for debitor "DBT-030"
    Then systemet anvender bruttoindkomst-faldbasisberegning
    And den beregnede loenindeholdelsesprocent er 5
    And revisionssporet registrerer frikort-status og faldbasis-metode

  Scenario: System fejler ikke ved frikort-situation med noevnaer lig nul
    # Guards against division by zero
    Given debitor "DBT-031" har nettoindkomst 30000 og fradragsbeloeb 35000 (fradragsbeloeb > nettoindkomst)
    And tabeltræk giver afdragsprocent 4 for denne indkomst
    When systemet beregner loenindeholdelsesprocenten for debitor "DBT-031"
    Then systemet returnerer ikke en fejl
    And den beregnede loenindeholdelsesprocent er 4

  # --- FR-4: Statutory maximum and reduceret rate enforcement ---

  Scenario: Loenindeholdelsesprocent begrænses af den lovbestemte maksimumsprocent (AC-8)
    Given debitor "DBT-040" har eSkattekortet-data der giver en formel-vaerdi over den lovbestemte maksimum
    When systemet beregner loenindeholdelsesprocenten for debitor "DBT-040"
    Then den beregnede loenindeholdelsesprocent er lig den lovbestemte maksimumsprocent
    And revisionssporet registrerer en cap-begivenhed med:
      | felt                      | krav                                    |
      | formel_vaerdi             | den uafkortede formel-vaerdi             |
      | anvendt_procent           | den lovbestemte maksimumsprocent         |
      | retsgrundlag              | GIL § 10a                               |

  Scenario: Reduceret loenindeholdelsesprocent anvendes for lav indkomst efter fradrag (GIL § 10a)
    Given debitor "DBT-041" har en nettoindkomst efter fradrag inden for lavindkomstgraensen i GIL § 10a
    When systemet beregner loenindeholdelsesprocenten for debitor "DBT-041"
    Then systemet anvender den reducerede loenindeholdelsesprocent
    And revisionssporet registrerer retsgrundlaget "GIL § 10a reduceret rate"

  # --- FR-5: eSkattekortet dispatch ---

  Scenario: eSkattekortet-afsendelse sker efter afgoerelses-underretning er bekræftet (AC-9)
    Given loenindeholdelsessagen "LI-62001" har en bekraeftet afgoerelse-underretning
    When systemet afsender loenindeholdelsesprocenten til eSkattekortet
    Then afsendelsen inkluderer:
      | felt              | krav                              |
      | procent           | den beregnede procent             |
      | ikrafttraedelsesdato | dags dato fra afgoerelsenotification |
    And CPR hentes fra Person Registry ved afsendelse og gemmes ikke i inddrivelse-service
    And revisionssporet registrerer person_id + afsendelsestidspunkt (ingen CPR)

  Scenario: eSkattekortet-afsendelse er idempotent ved gentagne forsøg (AC-10)
    Given loenindeholdelsessagen "LI-62002" har en bekraeftet afgoerelse-underretning
    And en afsendelse til eSkattekortet er allerede gennemfoert med idempotency-noegle fra afgoerelse "AFG-62002"
    When systemet forsøger at afsende igen med den samme idempotency-noegle "AFG-62002"
    Then eSkattekortet returnerer succes uden at oprette en duplikat-post
    And revisionssporet registrerer kun én afsendelsesbegivenhed for "LI-62002"

  Scenario: Fejlet eSkattekortet-afsendelse placerer sagen i sagsbehandler-alarmkoen
    Given loenindeholdelsessagen "LI-62003" har en bekraeftet afgoerelse-underretning
    And alle afsendelsesforsoeg til eSkattekortet fejler med netvaerksfejl
    When systemets maksimale antal genforsøg er opbrugt
    Then sagen "LI-62003" placeres i sagsbehandler-alarmkoen
    And revisionssporet registrerer alle fejlede afsendelsesforsoeg

  # --- FR-6: Varsel generation and tracking ---

  Scenario: Varsel genereres med korrekt indhold (AC-11)
    Given sagsbehandleren "SB-01" initierer loenindeholdelse for debitor "DBT-050"
    And sagen dækker fordringerne "FDR-62020" (art: "parkeringsbøde", stoerrelse: 1200 DKK) og "FDR-62021" (art: "biblioteksbod", stoerrelse: 450 DKK)
    And den beregnede loenindeholdelsesprocent er 18
    When systemet genererer varslet
    Then varslet indeholder:
      | felt                          | krav                                          |
      | fordring FDR-62020 art        | parkeringsbøde                                |
      | fordring FDR-62020 stoerrelse | 1200 DKK                                      |
      | fordring FDR-62021 art        | biblioteksbod                                 |
      | fordring FDR-62021 stoerrelse | 450 DKK                                       |
      | foreslaaet procent            | 18%                                           |
      | debitorrettigheder            | inkluderer ret til at gore indsigelse         |
      | debitorrettigheder            | inkluderer ret til at anmode om afdragsordning|
      | svarfrist                     | en dato i fremtiden                           |
    And varslet afsendes via Digital Post

  Scenario: Varsel-afsendelse opretter IKKE en foraeidelsesbrud-begivenhed (AC-12, G.A.2.4.4.1.2)
    Given sagsbehandleren "SB-01" initierer loenindeholdelse for sagen "SAG-62010"
    And varslet er genereret og afsendt til debitor "DBT-051" via Digital Post
    When Digital Post bekraefter leveringen af varslet
    Then systemet opretter ingen foraeidelsesbrud-begivenhed for nogen fordring i sagen "SAG-62010"
    And revisionssporet registrerer varsel-afsendelsestidspunkt og svarfrist

  Scenario: Debitoren svarer inden varslets svarfrist — sagsbehandler notificeres
    Given varslet for sagen "SAG-62011" har en svarfrist om 14 dage
    When debitoren svarer inden svarfristen og anmoder om afdragsordning
    Then systemet registrerer debitorens svar
    And sagsbehandleren notificeres om anmodning om afdragsordning
    And loenindeholdelsesprocessen sættes i bero

  # --- FR-7: Afgørelse generation and Digital Post dispatch ---

  Scenario: Afgoerelse genereres med obligatorisk indhold efter varselfristens udloeb (AC-13)
    Given varslet for sagen "SAG-62020" er afsendt og svarfristen er udloebet
    And debitoren har ikke svaret
    When sagsbehandleren udsteder loenindeholdelsesafgoerelsen
    Then afgoerelsen indeholder for hver fordring:
      | felt            | krav                              |
      | fordringens art | angivet for hver dækket fordring  |
      | storrelse       | angivet for hver dækket fordring  |
    And afgoerelsen indeholder den fastsatte loenindeholdelsesprocent
    And afgoerelsen angiver retsgrundlaget "GIL § 10, Gæld.bekendtg. § 14"
    And afgoerelsen inkluderer debitorens klagerettighed

  Scenario: Afgoerelse uden fordringens art og storrelse afvises af backend foer afsendelse (AC-14)
    Given en anmodning om at udstede afgoerelse for sagen "SAG-62021"
    And anmodningen mangler fordringens art og storrelse for fordringen "FDR-62030"
    When anmodningen sendes til API-endepunktet
    Then systemet returnerer HTTP 422
    And afgoerelsen afsendes IKKE via Digital Post
    And fejlresponsen angiver "MANGLENDE_ART_OG_STOERRELSE" for fordringen "FDR-62030"

  Scenario: Afgoerelse afsendes via Digital Post og status trackes
    Given afgoerelsen "AFG-62001" for sagen "SAG-62022" er genereret med korrekt indhold
    When systemet afsender afgoerelsen via Digital Post
    Then systemet registrerer status "afsendt"
    And statusen opdateres til "bekraeftet" naar Digital Post returnerer leveringsbekraeftelse

  # --- FR-8: Underretning tracking and forældelsesbrud event ---

  Scenario: Afgoerelse-underretning bekraeftelse opretter foraeldelsesbrud for alle fordringer (AC-15, AC-16)
    Given afgoerelsen "AFG-62002" for sagen "SAG-62030" daekker fordringerne "FDR-62040" og "FDR-62041"
    And afgoerelsen er afsendt via Digital Post
    When Digital Post bekraefter leveringen af afgoerelses-underretningen med tidsstemplet "2026-06-15T14:23:00Z"
    Then systemet opretter en foraeidelsesbrud-begivenhed for fordringen "FDR-62040" med:
      | felt                          | vaerdi                                        |
      | fordring_uuid                 | FDR-62040                                     |
      | afgoerelse_reference          | AFG-62002                                     |
      | underretning_bekraeftelsestid | 2026-06-15T14:23:00Z                          |
      | ny_foraeildelsestidspunkt_start | 2026-06-15                                  |
    And systemet opretter en foraeidelsesbrud-begivenhed for fordringen "FDR-62041" med tilsvarende data
    And revisionssporet er uforanderligt opdateret med begge begivenheder

  Scenario: Varsel-levering opretter ingen foraeidelsesbrud — kun afgoerelse-underretning tæller (AC-12, AC-15)
    Given varslet "VAR-62001" for sagen "SAG-62031" daekker fordringerne "FDR-62050" og "FDR-62051"
    When Digital Post bekraefter leveringen af varslet "VAR-62001"
    Then systemet opretter ingen foraeidelsesbrud-begivenhed for hverken "FDR-62050" eller "FDR-62051"

  Scenario: Foraeildelsesbryd registreres ved alternativ levering naar Digital Post-indbakke er inaktiv
    Given debitor "DBT-060" har ingen aktiv Digital Post-indbakke
    And afgoerelsen "AFG-62003" afsendes som fysisk brev
    When det fysiske brev leveres og leveringstidspunktet registreres som "2026-06-20"
    Then systemet opretter foraeidelsesbrud-begivenhed for alle fordringer i "AFG-62003" med tidsstempel "2026-06-20"

  # --- FR-9: Betalingsevnevurdering concurrent support ---

  Scenario: Betalingsevnevurdering kan reducere loenindeholdelsesprocenten under aktiv loenindeholdelse (AC-17)
    Given loenindeholdelsessagen "LI-62010" er aktiv med loenindeholdelsesprocent 18
    And debitoren har indsendt et budgetskema
    When systemet kalder betalingsevnevurderingstjenesten og faar konkret afdragsbeloeb 12% svarende til lavere procent
    Then systemet udsteder en ny afgoerelse med reduceret loenindeholdelsesprocent 12
    And loenindeholdelsessagen forbliver aktiv (IKKE i bero) under vurderingsperioden
    And revisionssporet registrerer den foregaaende procent, vurderingsresultatet og den nye procent

  Scenario: AEgtefaelles indkomst medtages i betalingsevnevurderingen (G.A.3.1.2.4.3.2)
    Given loenindeholdelsessagen "LI-62011" er aktiv
    And debitoren "DBT-071" har en registreret aegtefaelle "DBT-072"
    And debitoren indsender et budgetskema med aegtefaellens indkomstoplysninger
    When systemet udforer betalingsevnevurderingen
    Then aegtefaellens indkomst er medtaget i kapacitetsvurderingen per G.A.3.1.2.4.3.2

  # --- FR-10: Ændring workflow ---

  Scenario: Aendring kraever ny afgoerelse og genererer ny foraeildelsesbrydningsbegivenhed (AC-18)
    Given loenindeholdelsessagen "LI-62020" er aktiv med loenindeholdelsesprocent 18
    And debitorens indkomst er aendret signifikant
    When sagsbehandleren sender PUT /api/v1/loenindeholdelse/LI-62020 med opdaterede indkomstdata
    Then systemet genberegner loenindeholdelsesprocenten med de nye eSkattekortet-data
    And systemet udsteder en ny afgoerelse "AFG-62010" med den opdaterede procent
    When Digital Post bekraefter leveringen af den nye afgoerelses-underretning
    Then systemet opretter en ny foraeidelsesbrud-begivenhed for alle fordringer i scope
    And revisionssporet registrerer: forudgaaende procent, udloesende indkomstaendring, ny procent, ny afgoerelse-reference

  Scenario: AEndring kan IKKE aendre den eksisterende afgoerelse — ny afgoerelse er paakreevet
    Given loenindeholdelsessagen "LI-62021" har afgoerelsen "AFG-62011"
    When sagsbehandleren forsoeger at aendre loenindeholdelsesprocenten i "AFG-62011" direkte
    Then systemet returnerer HTTP 405 eller HTTP 422
    And "AFG-62011" er uforandret

  # --- FR-11: Tværgående lønindeholdelse ---

  Scenario: Tvaergaaende loenindeholdelse afsendes til to arbejdsgivere i daekningsraekkefoelge (AC-19)
    Given debitor "DBT-080" har A-indkomst fra to arbejdsgivere:
      | arbejdsgiver_id | maanedlig_indkomst |
      | ARB-001         | 35000              |
      | ARB-002         | 15000              |
    And den beregnede loenindeholdelsesprocent er 18 samlet
    And afgoerelsen er bekraeftet med underretning
    When systemet afsender tvaergaaende loenindeholdelse til eSkattekortet
    Then afsendelsen til ARB-001 (primaer arbejdsgiver) afsendes foerst
    And afsendelsen til ARB-002 daekker det resterende beloeb op til den samlede procent
    And den kombinerede loenindeholdelsesprocent paa tvaers af arbejdsgivere overstiger ikke den lovbestemte maksimum

  Scenario: Afsendelse til sekundaer arbejdsgiver genforsoeges uafhaengigt ved fejl
    Given debitor "DBT-081" har to arbejdsgivere "ARB-010" og "ARB-011"
    And afsendelsen til "ARB-011" fejler paa grund af netvaerksfejl
    And afsendelsen til "ARB-010" lykkes
    When systemet genforsoeges paa afsendelsen til "ARB-011"
    Then afsendelsen til "ARB-010" forbliver uforandret
    And afsendelsen til "ARB-011" genforsoeges selvstaendigt

  # --- FR-12: Lønindeholdelse in bero and new forældelsesfrist ---

  Scenario: Loenindeholdelse sættes i bero naar afdragsordning er aktiv (AC-20)
    Given loenindeholdelsessagen "LI-62030" er aktiv for debitor "DBT-090"
    When afdragsordningen "AFO-62001" for debitor "DBT-090" aktiveres (P061)
    Then systemet skifter loenindeholdelsessagen "LI-62030" til status "I_BERO"
    And revisionssporet registrerer i-bero-startdato og aarsagen "AKTIV_AFDRAGSORDNING"

  Scenario: Loenindeholdelse genoptages IKKE automatisk efter afdragsordning-misligholdelse (AC-21)
    Given loenindeholdelsessagen "LI-62031" er i bero grundet aktiv afdragsordning "AFO-62002"
    When afdragsordningen "AFO-62002" registreres som misligholdt (P061)
    Then systemet placerer en notifikation i sagsbehandler-alarmkoen om at ny afgoerelse er paakreevet
    And loenindeholdelsessagen "LI-62031" forbliver i status "I_BERO"
    And loenindeholdelsen genoptages IKKE automatisk

  Scenario: Et aars i-bero starter ny foraeldelsesfrist for alle fordringer i scope (AC-22)
    Given loenindeholdelsessagen "LI-62032" gik i bero den "2025-01-15"
    And fordringerne "FDR-62070" og "FDR-62071" er i scope
    When 1 aar er forlobet siden i-bero-datoen (dvs. den "2026-01-15")
    Then systemet registrerer en ny foraeildelsesfristen-startbegivenhed for "FDR-62070" med dato "2026-01-15"
    And systemet registrerer en ny foraeildelsesfristen-startbegivenhed for "FDR-62071" med dato "2026-01-15"
    And begivenhederne er registreret i revisionssporet med reference til i-bero-startdatoen

  Scenario: Loenindeholdelse i bero i under 1 aar udloser ikke ny foraeildelsesfristen-start
    Given loenindeholdelsessagen "LI-62033" gik i bero den "2026-01-01"
    When der er forlobet 364 dage (det er den "2026-12-31")
    Then systemet opretter INGEN ny foraeildelsesfristen-startbegivenhed

  # --- FR-13: API endpoints ---

  Scenario: GET-statusendepunkt returnerer fuld status, procent og begivenhedshistorik (AC-23)
    Given loenindeholdelsessagen "LI-62040" er aktiv med loenindeholdelsesprocent 18
    And sagen har foelgende begivenheder i revisionssporet:
      | begivenhed              | tidsstempel          |
      | VARSEL_AFSENDT          | 2026-04-01T10:00:00Z |
      | AFGOERELSE_AFSENDT      | 2026-05-01T09:00:00Z |
      | UNDERRETNING_BEKRAEFTET | 2026-05-03T14:00:00Z |
      | FORAEILDELSESBRYD       | 2026-05-03T14:00:00Z |
    When sagsbehandleren sender GET /api/v1/loenindeholdelse/LI-62040
    Then svaret indeholder:
      | felt               | vaerdi                     |
      | status             | AKTIV                      |
      | loenindeholdelsesprocent | 18                   |
      | fordringer_i_scope | listen over fordring-UUIDs |
      | begivenheder       | alle 4 begivenheder        |

  Scenario: Uautoriseret kald til loenindeholdelse-API afvises
    Given en HTTP-klient uden "CASEWORKER"- eller "SUPERVISOR"-rolle
    When klienten sender GET /api/v1/loenindeholdelse/LI-62040
    Then systemet returnerer HTTP 403

  Scenario: POST /afgoerelse/confirm registrerer underretning og udloser foraeildelsesbryd
    Given afgoerelsen "AFG-62020" for sagen "LI-62041" daekker fordringen "FDR-62080"
    When Digital Post-callbacket sender POST /api/v1/loenindeholdelse/LI-62041/afgoerelse/confirm
    And callbacket angiver underretning-bekraeftelsestidsstemplet "2026-06-01T11:00:00Z"
    Then systemet registrerer underretning-status som "BEKRAEFTET"
    And systemet opretter foraeidelsesbrud-begivenhed for "FDR-62080" med tidsstempel "2026-06-01T11:00:00Z"

  # --- FR-14: Sagsbehandlerportal — varsel/afgørelse-tidslinje ---

  Scenario: Sagsbehandlerportalen viser fuld varsel/afgoerelse-tidslinje (AC-24)
    Given loenindeholdelsessagen "LI-62050" er aktiv
    And sagen har varsel afsendt, afgoerelse afsendt og underretning bekraeftet
    When sagsbehandleren "SB-01" navigerer til tidslinje-visningen for sagen "LI-62050"
    Then portalen viser:
      | element                       | vaerdi                                   |
      | Varsel afsendt                | dato for afsendelse                      |
      | Debitors svarfrist            | fristdato                                |
      | Debitorens svar               | "Intet svar" eller debitorens svar       |
      | Afgoerelse afsendt            | dato for afsendelse                      |
      | Underretning bekraeftet       | bekraeftelsestidsstempel fra Digital Post |
      | Foraeildelsesbryd-begivenhed  | kontroltidsstempel for hvert fordring    |

  Scenario: Portalen viser korrekt status-badge for aktiv loenindeholdelse
    Given loenindeholdelsessagen "LI-62051" har status "AKTIV" og loenindeholdelsesprocent 18
    When sagsbehandleren "SB-01" aabner sagens oversigt
    Then portalen viser status-badgen "AKTIV"
    And portalen viser loenindeholdelsesprocenten "18%"

  Scenario: Portalen viser tvaergaaende arbejdsgiverantal naar relevant
    Given loenindeholdelsessagen "LI-62052" koordinerer tvaergaaende loenindeholdelse for 2 arbejdsgivere
    When sagsbehandleren aabner sagens oversigt
    Then portalen viser "2 arbejdsgivere" i status-visningen

  Scenario: Portalen viser beregningsopstilling paa initierings-bekraeftelsessiden
    Given sagsbehandleren "SB-01" er ved at initiere loenindeholdelse for debitor "DBT-100"
    And systemet har beregnet loenindeholdelsesprocenten 18 baseret paa eSkattekortet-data
    When sagsbehandleren navigerer til bekraeftelsessiden
    Then portalen viser formelspecifikationen med:
      | felt           | vaerdi   |
      | nettoindkomst  | 400000   |
      | fradragsbeloeb | 48000    |
      | traekprocent   | 37%      |
      | afdragsprocent | 10%      |
      | beregnet procent | 18%    |

  # --- FR-15: GDPR — CPR only via Person Registry ---

  Scenario: CPR gemmes ikke i inddrivelse-service ved eSkattekortet-afsendelse (AC-25)
    Given loenindeholdelsessagen "LI-62060" er klar til eSkattekortet-afsendelse
    And debitor "DBT-110" har CPR-nummer registreret i Person Registry
    When systemet gennemfoerer eSkattekortet-afsendelsen
    Then CPR-nummeret er ikke gemt i nogen kolonne i inddrivelse-service-databasen
    And revisionssporet for afsendelsesbegivenheden indeholder person_id og tidsstempel — men ikke CPR
    And Person Registry-API-kaldet til at hente CPR forekommer kun under selve afsendelsen

  Scenario: Loenindeholdelse-entiteter refererer debitor via person_id UUID — ikke CPR
    Given en ny loenindeholdelse oprettes for debitor "DBT-111"
    When systemet gemmer loenindeholdelse-entiteten
    Then entiteten indeholder feltet "debtor_person_id" af typen UUID
    And entiteten indeholder IKKE noget felt navngivet "cpr", "cprNummer" eller tilsvarende
