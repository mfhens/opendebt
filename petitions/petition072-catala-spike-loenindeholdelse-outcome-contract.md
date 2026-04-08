# Petition 072 Outcome Contract

## Petition reference

**Petition 072:** Catala Compliance Spike — Lønindeholdelsesprocent Gæld.bekendtg. § 14 (companion til P062)  
**Type:** Research spike — ingen produktionskode leveres.  
**Juridisk grundlag:** G.A.3.1.2.5, Gæld.bekendtg. § 14, stk. 2–3; GIL §§ 10, 10a; SKM2015.718.ØLR  
**Time box:** 2 arbejdsdage.  
**G.A.-snapshot:** v3.16 (2026-03-28).  
**Referencer:** Petition 062 (lønindeholdelse — fuld spec), Petition 054 (Catala-spike G.A.1.4.3/1.4.4)  
**Afhænger af:** Petition 062

> **Spike-note:** Alle leverancer for dette petition er **filer** — Catala-kildefiler, en
> Catala-testsuite og en markdown-spikerapport. Der er ingen runtime-adfærd, ingen ny API-overflade,
> ingen databasemigrationer og ingen portalændringer. Acceptancekriterier er derfor udtrykt som
> filsystem- og indholdspåstande samt et kompileringsecit-code-tjek.

---

## Observable outcomes per funktionelt krav

### FR-1: Catala-kodning af kerneformlen (Gæld.bekendtg. § 14, stk. 3, 2.–4. pkt.)

**Leverance**
- Filen `catala/ga_3_1_2_loenindeholdelse_pct.catala_da` er til stede i repositoriet.

**Forventet indhold**
- Filen koder omregningsformlen:
  ```
  lønindeholdelsesprocent = floor(
    (afdragsprocent × nettoindkomst)
    ──────────────────────────────────────────────────
    (nettoindkomst − fradragsbeløb) × (1 − trækprocent/100)
  )
  ```
- FR-1.1: Standardformlen er forankret til Gæld.bekendtg. § 14, stk. 3, 2.–4. pkt.
- FR-1.2: Nedrunding (floor) er forankret til Gæld.bekendtg. § 14, stk. 3, 5. pkt.
- FR-1.3: Cap-reglen (samlet indeholdelsesprocent ≤ 100 %) er forankret til Gæld.bekendtg. § 14,
  stk. 3, 9. pkt. og kildeskattelovens § 48, stk. 5.
- Filen erklærer den danske Catala-dialekt (`catala_da`).
- Alle G.A.-citationer refererer til snapshot v3.16 (2026-03-28).

**Hvad "succes" ser ud som**
- Revieweren kan, uden tvetydighed eller workaround, afbilde formlen, nedrundingsreglen og
  cap-reglen til separate, identificerbare Catala `scope`- eller `rule`-blokke med artikelankere.
- Catala-programmet returnerer 18 for input: afdragsprocent=10, nettoindkomst=400000,
  fradragsbeløb=48000, trækprocent=37.

---

### FR-2: Catala-kodning af frikort-kanttilfældet

**Leverance**
- Frikort-vagtkonditionerne er til stede i `catala/ga_3_1_2_loenindeholdelse_pct.catala_da`.

**Forventet indhold**
- FR-2.1: Fallback til `lønindeholdelsesprocent = afdragsprocent` når `(nettoindkomst − fradragsbeløb) ≤ 0`.
- FR-2.2: Standardformlen med denominator = 1 når `trækprocent = 0 %`.
- FR-2.3: Vagtkonditionerne er koded som separate Catala-regler; ingen division-med-nul-fejl opstår.
- Hvert vagtkodningsblok er forankret til G.A.3.1.2.5 (snapshot v3.16 2026-03-28).

**Hvad "succes" ser ud som**
- Et Catala-program med input nettoindkomst=48000 og fradragsbeløb=48000 returnerer
  `afdragsprocent` direkte (fallback) og kaster ikke et runtime-fejl.

---

### FR-3: Catala-kodning af reduceret sats og fordringtype-klassifikation

**Leverance**
- Klassifikationsprædikatet og reduceret-sats-reglen er til stede i kildefilen.

**Forventet indhold**
- FR-3.1: En Catala-enumeration af fordringtyper der kvalificerer til reduceret sats (GIL § 10a).
- FR-3.2: En Catala-betingelse der anvender reduceret sats ved kvalificerende fordringtype og
  standardsats ellers — forankret til GIL § 10a.
- FR-3.3: `trækprocent` og `fradragsbeløb` injiceres som parametre; ingen CPR-persistering.

**Hvad "succes" ser ud som**
- For en kvalificerende fordringtype returnerer Catala-programmet den reducerede sats, og for
  en ikke-kvalificerende fordringtype returnerer det standardsatsen.

---

### FR-4: Catala-testsuite

**Leverance**
- Filen `catala/tests/ga_loenindeholdelse_tests.catala_da` er til stede i repositoriet.

**Forventet indhold**
- Mindst 8 individuelle testcases udtrykt med Catala's built-in `Test`-modul.
- Dækning inkluderer:
  - FR-4.1 Standardformel: input 10/400000/48000/37 → output 18
  - FR-4.2 Nedrunding: et resultat med decimaler nedrundes, aldrig oprundes
  - FR-4.3 Frikort (trækprocent=0): formel med denominator-faktor=1
  - FR-4.4 Frikort-kant: fallback aktiveres korrekt
  - FR-4.5 Cap: resultat over maksimumsgrænse → cap anvendes; nedrunding gælder det cappede resultat
  - FR-4.6 Reduceret sats: kvalificerende fordringtype → reduceret sats returneres
  - FR-4.7 Fast-punkt præcision: mellemresultater valideres
  - FR-4.8 Afdragsprocent-afhængighed: afdragsprocent fra P071-bracket injiceres korrekt
- Alle tests eksekverer og passerer med Catala CLI.

**Hvad "succes" ser ud som**
- `catala test-doc catala/tests/ga_loenindeholdelse_tests.catala_da` afslutter med exit 0
  og alle testcases rapporterer `PASS`.

---

### FR-5: Sammenligningsrapport mod P062 Gherkin-scenarier

**Leverance**
- Filen `catala/SPIKE-REPORT-072.md` er til stede i repositoriet.

**Forventet indhold**
- En markdowntabel der afbilder hvert P062 FR-2 og FR-3 Gherkin-scenarie (fra
  `petitions/petition062-loenindeholdelse-fuld-spec.feature`) til én af: **Dækket**, **Ikke dækket**
  eller **Uoverensstemmelse fundet**.
- Et **Huller**-afsnit der lister regelgrene kodet i Catala men ikke dækket af et P062-scenarie
  (eller eksplicit bekræftelse af "Ingen fundet").
- Et **Uoverensstemmelser**-afsnit der lister tilfælde hvor et P062-scenarie tilsyneladende
  modsiger G.A.-teksten som formaliseret (eller "Ingen fundet").
- Et **Diskrepanspunkter**-afsnit der eksplicit vurderer de fem identificerede risici:
  nedrunding, fast-punkt-aritmetik, frikort-vagtkondition, cap-rækkefølge og reduceret sats-klassifikation.
- Et **Indsatsskøn**-afsnit med persondag-estimat for kodning af det fulde G.A.3.1.2-kapitel og rationale.

**Hvad "succes" ser ud som**
- Hvert P062 FR-2 og FR-3 Gherkin-scenarie har en række i sammenligningstabellen.
- Alle fem afsnit (Sammenligning, Huller, Uoverensstemmelser, Diskrepanspunkter, Indsatsskøn)
  er til stede og indeholder substantielt indhold eller en eksplicit "Ingen fundet"-erklæring.

---

### FR-6: Go/No-Go-anbefaling

**Leverance**
- `catala/SPIKE-REPORT-072.md` indeholder et eksplicit Go/No-Go-anbefalingsafsnit.

**Forventet indhold**
- Anbefalingsafsnittet angiver enten **Go** eller **No-Go** utvetydigt.
- Evidens gives for hvert Go-kriterium:
  - Om § 14-formlen kodedes uden tvetydighed (ja/nej + evidens)
  - Om nedrunding og fast-punkt-aritmetik kan udtrykkes i Catala (ja/nej + evidens)
  - Om frikort-vagtkondition kan udtrykkes rent (ja/nej + evidens)
  - Om det konkrete 18 %-eksempel reproduceres korrekt i Catala-tests (ja/nej + output)
  - Om mindst 1 uoverensstemmelse fundet vs. P062 Gherkin (ja/nej + detalje)
  - Om Catala kompilerede uden fejl (ja/nej + kommandooutput)
- Evidens gives for hvert No-Go-trigger (hvis applicable):
  - Om fast-punkt-aritmetik kræver eksternt bibliotek ikke tilgængeligt i Catala (ja/nej + detalje)
  - Om frikort-kanttilfældet kræver workarounds (ja/nej + detalje)
  - Om indsatsen overstiger 4 persondage pr. G.A.-afsnit (ja/nej + estimat)

**Hvad "succes" ser ud som**
- En reviewer kan fastslå Go/No-Go-udfaldet og dets grundlag uden at referere til et eksternt
  dokument ud over spikerapporten selv.

---

## Acceptancekriterier

Følgende er binære pass/fail-tjek. Hvert skal bestå for at petition lukkes som Done.

**AC-1 (FR-1 — fil til stede)**  
`catala/ga_3_1_2_loenindeholdelse_pct.catala_da` eksisterer i repositoriet.

**AC-2 (FR-1 — standardformel til stede)**  
Filen indeholder et identificerbart Catala-regelblok for omregningsformlen forankret til
Gæld.bekendtg. § 14, stk. 3, 2.–4. pkt.

**AC-3 (FR-1 — konkret eksempel reproduceres)**  
Et Catala-test med input (afdragsprocent=10, nettoindkomst=400000, fradragsbeløb=48000,
trækprocent=37) returnerer lønindeholdelsesprocent=18.

**AC-4 (FR-1 — nedrunding)**  
Filen indeholder et Catala-regelblok for nedrunding forankret til Gæld.bekendtg. § 14, stk. 3, 5. pkt.
og SKM2015.718.ØLR. Et input der giver en formelværdi på 18,99 % returnerer 18, aldrig 19.

**AC-5 (FR-1 — cap-regel)**  
Filen indeholder et Catala-regelblok for maksimumsbegrænsning forankret til Gæld.bekendtg. § 14,
stk. 3, 9. pkt. og kildeskattelovens § 48, stk. 5. Nedrunding gælder det cappede resultat.

**AC-6 (FR-2 — frikort-vagtkondition til stede)**  
Filen indeholder separate Catala-regelblokke for frikort-fallback (FR-2.1) og nul-trækprocent-
håndtering (FR-2.2), begge med G.A.3.1.2.5-artikelankere.

**AC-7 (FR-2 — ingen division-med-nul)**  
Catala-programmet returnerer `afdragsprocent` (fallback-sats) — uden runtime-fejl — for input
nettoindkomst=48000, fradragsbeløb=48000.

**AC-8 (FR-3 — reduceret sats-klassifikation)**  
Filen indeholder en Catala-enumeration af fordringtyper der kvalificerer til reduceret sats,
forankret til GIL § 10a.

**AC-9 (FR-3 — reduceret sats-betingelse)**  
For en kvalificerende fordringtype returnerer Catala-programmet den reducerede sats; for en
ikke-kvalificerende returnerer det standardsatsen.

**AC-10 (FR-4 — testfil til stede)**  
`catala/tests/ga_loenindeholdelse_tests.catala_da` eksisterer i repositoriet.

**AC-11 (FR-4 — minimum testantal)**  
Testfilen indeholder mindst 8 distinkte testcases udbredt Catala's `Test`-modul, der dækker
alle regelgrene i FR-4.1–FR-4.8.

**AC-12 (FR-5 — rapportfil til stede)**  
`catala/SPIKE-REPORT-072.md` eksisterer i repositoriet.

**AC-13 (FR-5 — sammenligningstabel)**  
`catala/SPIKE-REPORT-072.md` indeholder en tabel der afbilder hvert P062 FR-2 og FR-3
Gherkin-scenarie til en dækningsstatus (Dækket / Ikke dækket / Uoverensstemmelse fundet).

**AC-14 (FR-5 — alle fem afsnit til stede)**  
`catala/SPIKE-REPORT-072.md` indeholder alle fem afsnit: Sammenligning, Huller,
Uoverensstemmelser, Diskrepanspunkter og Indsatsskøn — hvert med substantielt indhold
eller en eksplicit "Ingen fundet"-erklæring.

**AC-15 (FR-6 — Go/No-Go til stede)**  
`catala/SPIKE-REPORT-072.md` indeholder et eksplicit Go/No-Go-afsnit med et klart
**Go** eller **No-Go**-udfald.

**AC-16 (FR-6 — evidens for hvert kriterium)**  
Go/No-Go-afsnittet indeholder evidens (ja/nej med rationale) for alle seks Go-kriterier og
alle tre No-Go-triggere listet i FR-6.

**AC-17 (NFR-1 — kompilering exit 0)**  
`catala ocaml ga_3_1_2_loenindeholdelse_pct.catala_da` fra `catala/`-mappen afslutter
med exit-kode 0. Tilsvarende for testkørslen.

**AC-18 (NFR-2 — dansk dialekt)**  
Kildefilen erklærer den danske Catala-dialekt; ingen engelsksproget kildekode produceres.

**AC-19 (NFR-3 — G.A.-versionscitationer)**  
Alle G.A.-artikelcitationer i kildefilen refererer til G.A.-snapshot v3.16 (2026-03-28)
svarende til versionen brugt i P062.

**AC-20 (NFR-4 og NFR-5 — ingen produktionsartefakter modificeret)**  
Ingen Java-kildefiler er oprettet eller modificeret af dette spike.  
Ingen databasemigrationsscripts er tilføjet.  
Ingen OpenAPI/Swagger-specifikationsfiler er modificeret.  
Ingen Spring Boot-modulkonfigurationsfiler er oprettet eller modificeret.  
Ingen CPR-numre persisteres som følge af spiket.

---

## Definition of Done

*(Verbatim fra Petition 072)*

- [ ] D-1 kompilerer uden fejl (`catala ocaml ga_3_1_2_loenindeholdelse_pct.catala_da`)
- [ ] D-2 testsuite eksekverer med alle tests bestående
- [ ] D-3 spikerapport indeholder eksplicit Go/No-Go med evidens for hvert kriterium
- [ ] D-3 sammenligningstabel dækker alle P062 FR-2 og FR-3 Gherkin-scenarier
- [ ] Mindst én hul- eller uoverensstemmelsesfinding dokumenteret (eller eksplicit bekræftet som fraværende)
- [ ] Ingen produktionsfiler modificeret; ingen migrationer, API-specifikationer eller Java-kilde ændret

---

## Leverancer

| # | Artefakt | Sti | Verificeret af |
|---|----------|-----|----------------|
| D-1 | Catala-kilde — lønindeholdelsesprocent | `catala/ga_3_1_2_loenindeholdelse_pct.catala_da` | AC-1, AC-2, AC-3, AC-4, AC-5, AC-6, AC-7, AC-8, AC-9, AC-17, AC-18, AC-19 |
| D-2 | Catala-testfil | `catala/tests/ga_loenindeholdelse_tests.catala_da` | AC-10, AC-11, AC-17 |
| D-3 | Spikerapport | `catala/SPIKE-REPORT-072.md` | AC-12, AC-13, AC-14, AC-15, AC-16 |

---

## Fejlbetingelser

- `catala/ga_3_1_2_loenindeholdelse_pct.catala_da` er fraværende fra repositoriet.
- Omregningsformlen mangler eller mangler Gæld.bekendtg. § 14-artikelankere.
- Input (10, 400000, 48000, 37) returnerer ikke 18.
- Et input med formelværdi 18,99 returnerer 19 (opafrunding er non-compliant per SKM2015.718.ØLR).
- Cap-reglen mangler, eller nedrunding anvendes ikke på det cappede resultat.
- Frikort-vagtkondition mangler eller kaster en division-med-nul-fejl.
- Reduceret sats-klassifikation er fraværende eller mangler GIL § 10a-ankere.
- Testfilen indeholder færre end 8 testcases.
- Én eller flere testcases fejler ved Catala CLI-eksekvering.
- `catala/SPIKE-REPORT-072.md` er fraværende fra repositoriet.
- Sammenligningstabellen udelader et eller flere P062 FR-2 eller FR-3 Gherkin-scenarier.
- Go/No-Go-afsnittet er fraværende eller angiver ikke et klart udfald.
- Evidens mangler for et Go-kriterium eller No-Go-trigger i FR-6.
- `catala ocaml ga_3_1_2_loenindeholdelse_pct.catala_da` afslutter med en ikke-nul exit-kode.
- En Java-kildefil, databasemigration, OpenAPI-spec eller Spring Boot-modulkonfigurationsfil er
  oprettet eller modificeret som del af dette spike.
- G.A.-citationer refererer til et andet snapshot end v3.16 (2026-03-28).
- CPR persisteres i nogen form som del af spiket.
