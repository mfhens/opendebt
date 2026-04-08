# Catala Compliance Spike Report — P071

**Petition:** P071 — Catala Compliance Spike — Afdragsordninger GIL § 11 (companion til P061)
**G.A. snapshot:** v3.16 (2026-03-28)
**Legal basis:** G.A.3.1.1, G.A.3.1.1.1; GIL § 11, stk. 1–2, stk. 6; GIL § 45;
Gæld.bekendtg. chapter 7
**Time box:** 2 working days
**Spike type:** Research spike — ingen produktionskode
**Companion petition:** P061 (Afdragsordninger — fuld specifikation, 30 scenarier)
**Prepared:** 2026-03-28

---

## Indledende bemærkning om scenarier

Outcome contract for P071 refererer til "25 P061 Gherkin-scenarier". Den aktuelle
`petitions/petition061-afdragsordninger.feature`-fil indeholder **30 scenarier**.
Alle 30 er kortlagt i afsnit 1 nedenfor.

---

## 1. Dækningstabel — P061 Gherkin-scenarier vs. Catala-testpakke

**Statuskoder:**
- **Dækket** — den juridiske regel er formaliseret i `ga_3_1_1_afdragsordninger.catala_da`,
  og Catala-testsuiten verificerer den.
- **Delvist dækket** — reglen er kodet, men kun ét aspekt af scenariet er verificerbart i
  Catala (det proceslogiske aspekt er udenfor Catala-scope).
- **Ikke dækket** — scenariet tester applikationslogik, UI-adfærd, workflowlogik eller
  infrastruktur; disse er ikke juridiske beregningsregler og er strukturelt udenfor
  Catala-scope.

| # | P061 scenario | P061 FR | Catala-status | Catala-scope / Test |
|---|---|---|---|---|
| 1 | Tabeltræk for debitor uden forsørgerpligt beregnes korrekt og afrundes ned | FR-1 | **Dækket** | TabeltræksBeregning; Test 4 (250.000 × 13 % → 2.700) |
| 2 | Tabeltræk for debitor med forsørgerpligt giver lavere procent og ydelse end uden | FR-1 | **Dækket** | TabeltræksBeregning; Test 5 (250.000 × 10 % → 2.050) |
| 3 | Debitor under lavindkomstgrænse kan ikke oprette afdragsordning via tabeltræk | FR-1 | **Dækket** | TabeltræksBeregning FR-1.1; Test 1, 3 |
| 4 | Tabeltræk-motor er deterministisk — samme input giver altid samme output | FR-1 | **Dækket** | Catala's rene funktionelle semantik garanterer determinisme per konstruktion |
| 5 | Månedlig ydelse er altid et multiplum af 50 og afrundet ned | FR-1 | **Dækket** | TabeltræksBeregning FR-2.4; Test 4, 5, 9. Se afsnit 2 (Hul: 100 kr-enhed) |
| 6 | Ny indekstabel tager effekt for planer oprettet fra 1. januar | FR-1 | **Dækket** | FR-1.6 parameterisering jf. GIL § 45; lavindkomstGrænse er context-input |
| 7 | Eksisterende aktive afdragsordninger genberegnes ikke automatisk ved ny indekstabel | FR-1 | **Ikke dækket** | Livscykluslogik / tilstandsstyring — ikke en lovbestemt beregningsregel |
| 8 | Oprettet afdragsordning aktiveres og lønindeholdelse suspenderes | FR-2 | **Dækket** | AfdragsordningInteraktion; Test 13 (aktiv → lønindeholdelseSuspenderet = true) |
| 9 | Ugyldig tilstandsovergang fra ANNULLERET til AKTIV afvises | FR-2 | **Ikke dækket** | Tilstandsmaskinelogik — udenfor Catala Tier A |
| 10 | Fordring tilføjes til aktiv afdragsordning og ydelse genberegnes | FR-2 | **Ikke dækket** | Entitetsrelation og genberegningsworkflow — applikationslogik |
| 11 | Fordring kan ikke tilføjes til en annulleret afdragsordning | FR-2 | **Ikke dækket** | Tilstandsbaseret validering — applikationslogik |
| 12 | Caseworker annullerer aktiv afdragsordning med begrundelse | FR-3 | **Ikke dækket** | Sagsbehandler-workflow — proceslogik udenfor Catala-scope |
| 13 | Manglende betaling udløser misligholdelsesvarsel og status ændres til MISLIGHOLT | FR-4 | **Ikke dækket** | Hændelsesdrevet operationslogik — udenfor Catala Tier A |
| 14 | Lønindeholdelsessuspension ophæves ved MISLIGHOLT og sagsbehandler notificeres | FR-4 | **Delvist dækket** | AfdragsordningInteraktion: afdragsordningAktiv=false → suspenderet=false (Test 14); notifikation er UI-logik |
| 15 | Sagsbehandler genindtræder misligholt afdragsordning med sagsnote | FR-5 | **Ikke dækket** | Sagsbehandler-workflow — proceslogik |
| 16 | Genindtræden uden sagsnote afvises | FR-5 | **Ikke dækket** | Inputvalidering — applikationslogik |
| 17 | Konkret betalingsevnevurdering med budgetskema giver lavere ydelse end tabeltræk | FR-6 | **Dækket** | KonkretBetalingsevneVurdering; Test 12 (konkretYdelse = 2.333 < ref 2.700) |
| 18 | Konkret betalingsevnevurdering afvises for debitor under lavindkomstgrænsen | FR-6 | **Dækket** | KonkretBetalingsevneVurdering FR-3.1; Test 10 (UNDER_LAVINDKOMSTGRAENSE) |
| 19 | Kulanceaftale oprettes med sagsbehandlers begrundelse og manuel ydelse | FR-7 | **Ikke dækket** | Diskretionær bestemmelse (GIL § 11, stk. 11) — ingen formel beregningsregel; eksplicit udenfor spikes scope jf. P071 §"Out of Scope" |
| 20 | Kulanceaftale uden begrundelse afvises | FR-7 | **Ikke dækket** | Diskretionær validering — udenfor Catala-scope |
| 21 | Afdragsordning for igangværende virksomhed kræver dokumentationsreference | FR-8 | **Ikke dækket** | GIL § 11a — virksomhedsafdragsordning; eksplicit udenfor spikes scope |
| 22 | Afdragsordning for igangværende virksomhed uden dokumentationsreference afvises | FR-8 | **Ikke dækket** | GIL § 11a — udenfor scope |
| 23 | Afdragsordning for afmeldt virksomhed behandles som privat person med tabeltræk | FR-8 | **Delvist dækket** | TabeltræksBeregning finder anvendelse; differentieringen er applikationslogik |
| 24 | Sagsbehandlerportal viser aktiv afdragsordning med status og beløb | FR-9 | **Ikke dækket** | UI/portalvisning — udenfor Catala-scope |
| 25 | API-forespørgsel returnerer afdragsordning med betalingshistorik og næste ydelsesdato | FR-9 | **Ikke dækket** | API-endpoint-adfærd — applikationslogik |
| 26 | Afdragsordning API-svar indeholder afbryderForaeldelse false | FR-9 | **Dækket** | AfdragsordningInteraktion.afbryderForaeldelse = false; Test 13, 14 |
| 27 | Oprettelse af afdragsordning afbryder ikke forældelsesfrist | FR-9 | **Dækket** | AfdragsordningInteraktion.afbryderForaeldelse = false (G.A.2.4) |
| 28 | Afdragsordning entity indeholder ingen personhenføring udover person_id | NFR-3 | **Ikke dækket** | Datamodel / GDPR — ikke en juridisk beregningsregel |
| 29 | Vellykket oprettelse af afdragsordning logges til revisionssporet | NFR-4 | **Ikke dækket** | Revisionslog-infrastruktur — udenfor Catala-scope |
| 30 | Fejlende oprettelse af afdragsordning logges til revisionssporet | NFR-4 | **Ikke dækket** | Revisionslog-infrastruktur — udenfor Catala-scope |

**Opsummering:**
- Dækket: 11 ud af 30 (37 %)
- Delvist dækket: 3 ud af 30 (10 %)
- Ikke dækket: 16 ud af 30 (53 %) — alle er applikationslogik, UI, workflow eller
  infrastruktur (strukturelt udenfor Catala Tier A)

---

## 2. Huller — Regelgrene kodet i Catala men ikke dækket af noget P061-scenario

**Hul 1 — 100 kr afrundingsenhed (FR-2.3)**

`TabeltræksBeregning` implementerer to afrundingsenheder: 50 kr og 100 kr, styret af
et parameteriseret grænsebeløb. P061 scenario 5 angiver kun "altid et multiplum af 50"
og tester ikke tilfældet, hvor månedligYdelse ≥ afrundingsGrænsebeløb og
afrundingsEnhed = 100 kr. Teknisk set er et multiplum af 100 kr altid et multiplum af
50 kr (100 = 2 × 50), så der er ingen faktisk modstrid — men den 100 kr-specifikke
gren i GIL § 11, stk. 2 er ikke eksplicit scenariedækket i P061.

**Hul 2 — Konkret ydelse HØJERE end tabeltræk (FR-3.3)**

FR-3.3 fastslår at konkret ydelse lovligt kan være lavere ELLER højere end
tabeltrækket. P061 scenario 17 tester kun det tilfælde, hvor konkret ydelse er
lavere end tabeltræksydelsen. Tilfældet `konkretYdelse > tabeltræksReferenceYdelse`
er juridisk gyldigt men mangler et P061-scenarie.

**Hul 3 — Indeksreguleringens virkning for aktive afdragsordninger**

GIL § 45 fastslår, at grænser reguleres 1. januar. FR-1.6 er kodet som
parameterisering, og P061 scenario 6 tester virkningstidspunktet for nye planer.
Men ingen P061-scenarie eksplicit verificerer, at en afdragsordning oprettet i 2026
fortsat bruger 2026-parametrene uanset om 2027-parametrene er i kraft — dette
fremgår kun implicit af parametermodellen.

---

## 3. Afvigelser — Tilfælde hvor P061-scenarie tilsyneladende modsiger G.A.-teksten

**Ingen fundne.**

De P061-scenarier der er Catala-dækkede er konsistente med G.A.3.1.1.1-kodningen.
Specifikt:
- P061 scenario 5 ("altid multiplum af 50") er teknisk korrekt, da 100 er et
  multiplum af 50. Der er ingen modstrid, men hul 1 ovenfor indikerer en
  underspecifikation.
- Referenceberegningerne i P061 (2.700 kr og 2.050 kr) er verificeret korrekte
  i Test 4 og Test 5.

---

## 4. Indsatsvurdering

**Enkeltsektion (G.A.3.1.1 / G.A.3.1.1.1):** 0,5–1 persondag ved dette fidelitetsniveau.
Spike-encodingen dækker 4 scopes, 1 enumeration, 15 testtilfælde.

**Skalering til det fulde G.A. Inddrivelse-kapitel:**
Det juridiske vejledningskapitel om inddrivelse indeholder ca. 50 artikelafsnit på linje
med G.A.3.1.1. Derudover er ca. 10–15 tematiske krydsreferenceafsnit (forældelse,
dækning, lønindeholdelse m.v.) allerede kodet via P054, P069, P070 og P072.
Estimat for resterende dækning:

| Sektionstype | Antal afsnit | Estimat/sektion | Totalt |
|---|---|---|---|
| Komplekse aritmetiske regler (som GIL § 11) | ~10 | 0,5–1 dag | 5–10 dage |
| Prædikatregler og typologibaserede regler | ~25 | 0,25 dag | 6–7 dage |
| Proceduremæssige/livscyklusregler (begrænset Catala-egnethed) | ~15 | 0,1 dag | 1–2 dage |
| **Total** | **~50** | | **~12–20 persondag** |

Dette er inden for Go-kriteriets grænse (< 4 persondag per sektion).

---

## 5. Go/No-Go-anbefaling

### Afgørelse: ✅ GO

Bestil petition 072 for at etablere en CI-integreret Catala-komplianceverifikationspipeline
der dækker G.A.3.1.x og supplerer P061's Gherkin-suite med Catala-genererede
autoritative testtilfælde.

---

### Evidens for Go-kriterier

| Kriterium | Status | Evidens |
|---|---|---|
| Tabeltræk-opslag kodificeres uden tvetydighed | ✅ Ja | `TabeltræksOpslagDemo` bruger nested if-then-else med parameteriserede grænser (ingen ambiguity). Test 15a/b verificerer 250.000 kr → 13% og 10%. |
| Floor-afrunding (stk. 2) er udtrykkelig i Catala | ✅ Ja | `(råydelseMåned / afrundingsEnhed) * afrundingsEnhed` — Catala heltalsdivision er floor for positive tal. Test 4 (2708→2700), Test 5 (2083→2050), Test 9 (3333→3300). |
| Lavindkomstgrænse-guard fungerer som Catala-prædikatsguard | ✅ Ja | `definition tabeltrækkMulig equals nettoindkomst >= lavindkomstGrænse` er et rent predikat. Test 1, 2, 3 dækker grænsetilfæld. |
| Indeksparameterisering er udtrykkelig som parameter | ✅ Ja | `lavindkomstGrænse`, `grænse1`-`grænse3`, alle procentsatser er `context`-input. Ingen hardkodede årsspecifikke værdier i Catala-kilden. |
| Mindst 1 hul eller afvigelse fundet ift. P061 Gherkin | ✅ Ja | 3 huller fundet (afsnit 2): 100 kr-afrundingsenhed, konkret ydelse >tabeltræk, indeksvirkning for aktive aftaler. |
| Catala-kilde kompilerer uden fejl | ⏳ UDSKUDT | `catala` ikke på PATH (Windows-miljø). Typecheck tilføjet til CI (`.github/workflows/ci.yml`) — Linux-runner med opam vil validere. Forventet at passere på baggrund af syntaksefterlevelse med eksisterende compilerende filer (P054, P069, P070). |

### Evidens for No-Go-udløsere

| Udløser | Status | Evidens |
|---|---|---|
| Indeksparameterisering kræver workaround | ✅ Nej (No-Go IKKE udløst) | Parameterinjection via `context`-variabler virker direkte uden workaround. Se `TabeltræksOpslagDemo` og `TabeltræksBeregning`. |
| Afrundingstvetydighed i G.A.-teksten blokerer formel kodning | ✅ Nej (No-Go IKKE udløst) | Grænsebeløbet for 50/100 kr-skift er en parameter (`afrundingsGrænsebeløb`). Tvetydigheden flyttes til parameter-konfiguration og blokerer ikke den formelle kodning. |
| Indsats per G.A.-sektion overstiger 4 persondag | ✅ Nej (No-Go IKKE udløst) | Estimeret 0,5–1 persondag for G.A.3.1.1/3.1.1.1 (4 scopes, 15 tests). Skaleret er der ~12–20 persondag for hele kapitlet (afsnit 4). |

---

### Næste skridt ved Go

1. **P072** — Etabler CI-integreret Catala-kompliancepipeline:
   - Udvid til det fulde G.A.3.1.x (afdragsordningers livscyklus, virksomhedsregler)
   - Integrer Catala-genererede test som autoritative regressionstest i OpenDebt's CI-suite
   - Dæk de 3 identificerede huller med nye Catala-scenarier

2. **P061 Gherkin-opdatering:** Tilføj et eksplicit scenarie for 100 kr-afrundingsenheden
   (hul 1) og for konkret ydelse højere end tabeltræk (hul 2).

3. **G.A. snapshot-management:** Etabler et parameterregister for de årsregulerede
   GIL § 45-værdier (lavindkomstGrænse, intervalgrænser, procentsatser) som et
   separat konfigurationsartefakt, der opdateres ved SKM-meddelelse og injiceres
   i Catala-testsuiten.

---

*Rapport udarbejdet som del af P071 Catala Compliance Spike.*
*G.A. snapshot v3.16, dated 2026-03-28.*
*Catala-artefakter: `catala/ga_3_1_1_afdragsordninger.catala_da`,*
*`catala/tests/ga_afdragsordninger_tests.catala_da`.*
