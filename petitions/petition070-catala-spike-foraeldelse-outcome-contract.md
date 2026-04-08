# Petition 070 Outcome Contract

## Petition reference

**Petition 070:** Catala Compliance Spike — Forældelse G.A.2.4 (companion to P059)  
**Type:** Research spike — no production code delivered.  
**Legal basis:** G.A.2.4.1–G.A.2.4.4.2 (v3.16, 2026-03-28), GIL §§ 18, 18a, Forældelsesl. §§ 3, 5, 18–19, SKM2015.718.ØLR  
**Time box:** 3 working days.  
**References:** Petition 059 (forældelse — fuld spec), G.A. snapshot v3.16 (2026-03-28)  
**Priority:** HIGHEST — commission immediately upon P059 approval.  
**depends_on:** Petition 059.

> **Spike note:** All deliverables for this petition are **files** — a Catala source program,
> a Catala test suite, and a markdown spike report. There is no runtime behaviour, no new API
> surface, no database migrations, and no portal changes. Acceptance criteria are therefore
> expressed as file-system and content assertions, plus a compilation exit-code check.
>
> The 3-day timebox (vs. 2 days for petition 054) is justified by the higher legal complexity
> of G.A.2.4: four interlocking temporal concepts, relational fordringskompleks propagation
> (GIL § 18a, stk. 2), a court precedent (SKM2015.718.ØLR) overriding statutory interpretation,
> and 29 companion Gherkin scenarios compared to ~12 in the P054 companion domain.

---

## Observable outcomes by functional requirement

### FR-1: Catala-indkodning af G.A.2.4.3 og G.A.2.4.1 — forældelsesfrist og udskydelse

**Deliverable**
- Filen `catala/ga_2_4_foraeldelse.catala_da` er til stede i repositoriet.

**Expected content**
- Filen indkoder alle fem sub-regler i FR-1:
  - FR-1.1 3-årig base frist for alle PSRM/DMI-fordringer (GIL § 18a, stk. 4)
  - FR-1.2 10-årig undtagelse ved udlæg + særligt retsgrundlag (Forældelsesl. § 5, stk. 1)
  - FR-1.3 PSRM-udskydelse: begyndelsestidspunkt tidligst 20-11-2021 (GIL § 18a, stk. 1, 1. pkt.)
  - FR-1.4 DMI/SAP38-udskydelse: begyndelsestidspunkt tidligst 20-11-2027 (GIL § 18a, stk. 1, 2. pkt.)
  - FR-1.5 Udskydelse er uforanderlig — kan ikke tilsidesættes af efterfølgende afbrydelse
- Hvert sub-regel-blok er forankret til sit kildelovafsnit med Catala's artikelcitationssyntaks.
- Kildesproget er dansk (`catala_da`).
- Alle G.A.-citater refererer til snapshot v3.16 (2026-03-28).

**What "success" looks like**
- En reviewer kan afbilde hvert af de fem FR-1-underregler til et tilsvarende Catala `scope`-
  eller `rule`-blok med en identificerbar artikelforankring, uden tvetydighed eller workaround.

---

### FR-2: Catala-indkodning af G.A.2.4.4.1 — afbrydelse

**Deliverable**
- Filen `catala/ga_2_4_foraeldelse.catala_da` indeholder afbrydelsesreglerne.

**Expected content**
- Filen indkoder alle tre G.A.2.4.4.1 afbrydelsestyper:
  - FR-2.1 Berostillelse: afgørelse afbryder; ny 3-årig frist; kun PSRM (GIL § 18a, stk. 8)
  - FR-2.2 Lønindeholdelse: afgørelse ved underretning afbryder; varsel alene afvises
    (GIL § 18, stk. 4 + SKM2015.718.ØLR); lønindeholdelse i bero 1 år → ny 3-årig frist
  - FR-2.3 Udlæg: forgæves udlæg ligestilles med vellykket udlæg (Forældelsesl. § 18, stk. 1);
    3-årig frist uden særligt retsgrundlag; 10-årig frist med særligt retsgrundlag
- SKM2015.718.ØLR er citeret i lønindeholdelsesblokken.
- Alle artikelforankringer er til stede med G.A.-version v3.16 (2026-03-28).

**What "success" looks like**
- En reviewer kan identificere det Catala-regelblok, der eksplicit afviser varsel-baseret
  afbrydelse, og det Catala-regelblok, der ligestiller forgæves og vellykket udlæg, begge
  med entydige artikelforankringer og uden tvetydighed.

---

### FR-3: Catala-indkodning af G.A.2.4.2 — fordringskompleks-propagation

**Deliverable**
- Filen `catala/ga_2_4_foraeldelse.catala_da` indeholder fordringskompleks-reglerne.

**Expected content**
- Filen indkoder alle tre FR-3 sub-regler:
  - FR-3.1 Fordringskompleks-definition: hovedkrav + renter udgør kompleks med fælles forældelsesdato
  - FR-3.2 Propagation: afbrydelse for ét medlem afbryder alle i komplekset (GIL § 18a, stk. 2, 4. pkt.)
  - FR-3.3 Atomicitet: partiel propagation er en fejlbetingelse udtrykt eksplicit i indkodningen

**What "success" looks like**
- Propagationsreglen og dens atomicitetsforpligtelse korresponderer til to distinkte,
  identificerbare Catala-regler, begge forankret til GIL § 18a, stk. 2.

---

### FR-4: Catala-indkodning af G.A.2.4.4.2 — tillægsfrister

**Deliverable**
- Filen `catala/ga_2_4_foraeldelse.catala_da` indeholder tillægsfristreglerne.

**Expected content**
- Filen indkoder alle fire FR-4 sub-regler:
  - FR-4.1 Intern opskrivning → 2-årig tillægsfrist (G.A.2.4.4.2.1)
  - FR-4.2 Modtagelse til inddrivelse ved tomt kompleks → foreløbig afbrydelse + ny 3-årig frist
    (G.A.2.4.4.2.2, GIL § 18a, stk. 7)
  - FR-4.3 Tillægsfrist-formel: `max(currentFristExpires, eventDate) + 2 år`
  - FR-4.4 Udskydelsesgrænsen overtrumfer tillægsfristen: tillægsfristen kan ikke sætte
    begyndelsestidspunktet tidligere end udskydelsesdatoen

**What "success" looks like**
- Tillægsfrist-formlen svarer til et Catala-udtryk, der eksplicit anvender `max()` over
  de to argumenter, med et identificerbart artkelanchor til G.A.2.4.4.2.

---

### FR-5: Catala-testsuite

**Deliverable**
- Filen `catala/tests/ga_foraeldelse_tests.catala_da` er til stede i repositoriet.

**Expected content**
- Mindst **10** individuelle testcases udtrykt ved hjælp af Catala's indbyggede `Test`-modul.
- Dækning inkluderer:
  - FR-1.1 og FR-1.2: begge base frist-varianter (3 år og 10 år)
  - FR-1.3 og FR-1.4: begge udskydelsesregler (PSRM og DMI/SAP38)
  - FR-1.5: udskydelsesimmutabilitet — afbrydelse ændrer ikke udskydelsesdatoen
  - FR-2.1: berostillelse-afbrydelse
  - FR-2.2 positiv case: lønindeholdelse afgørelse afbryder
  - FR-2.2 negativ case: lønindeholdelse varsel afbryder IKKE (SKM2015.718.ØLR)
  - FR-2.3: forgæves udlæg ligestilles med vellykket udlæg
  - FR-3.2: fordringskompleks-propagation med mindst to kompleksmedlemmer
  - FR-4.3: tillægsfrist-formlen med begge forgreninger af max()-udtrykket
- Mindst én grænseværditest for udskydelsesdatoen (dagen før, på datoen, dagen efter).
- Alle tests kører og består, når Catala CLI eksekverer testfilen.

**What "success" looks like**
- `catala test-doc catala/tests/ga_foraeldelse_tests.catala_da` afsluttes med kode 0
  med alle testcases, der rapporterer `PASS`.

---

### FR-6: Sammenligningsrapport mod P059 Gherkin-scenarier (29 stk.)

**Deliverable**
- Filen `catala/SPIKE-REPORT-070.md` er til stede i repositoriet.

**Expected content**
- En markdown-tabel, der afbilder hvert af P059's 29 Gherkin-scenarier
  (fra `petitions/petition059-foraeldelse.feature`) til en dækningsstatus:
  **Dækket**, **Ikke dækket** eller **Uoverensstemmelse fundet**.
- Et **Huller**-afsnit med regelgrene indkodet i Catala, der ikke er dækket af noget P059-scenarie
  (eller eksplicit bekræftelse: "Ingen fundet").
- Et **Uoverensstemmelser**-afsnit med tilfælde, hvor et P059-scenarie ser ud til at modsige
  G.A.-teksten som formaliseret (eller eksplicit bekræftelse: "Ingen fundet").
  Rapporten skal eksplicit adressere de fem discrepancy hotspots fra petitionen.
- Et **Indsatsskøn**-afsnit med et person-dag-estimat for indkodning af det komplette G.A.2.4
  i Catala med tilsvarende detaljeringsniveau og begrundelse.

**What "success" looks like**
- Hvert af de 29 P059-scenarier har en række i sammenligningsabellen.
- Alle fem discrepancy hotspots er eksplicit adresseret — enten som uoverensstemmelse fundet
  eller bekræftet fraværende.
- Alle fire afsnit (Dækning, Huller, Uoverensstemmelser, Indsatsskøn) er til stede med
  substantielt indhold eller en eksplicit "Ingen fundet"-erklæring.

---

### FR-7: Go/Nej-Go-anbefaling

**Deliverable**
- `catala/SPIKE-REPORT-070.md` indeholder et eksplicit Go/Nej-Go-anbefalingsafsnit.

**Expected content**
- Anbefalingsafsnittet angiver entydigt enten **Go** eller **Nej-Go**.
- Der er evidens for hvert Go-kriterium:
  - Om alle tre afbrydelsestyper indkodes uden tvetydighed (ja/nej + evidens)
  - Om varsel/afgørelse-distinktionen (SKM2015.718.ØLR) indkodes rent (ja/nej + evidens)
  - Om fordringskompleks-propagation er udtrykkelig uden workarounds (ja/nej + evidens)
  - Om Catala-testkompilering lykkedes uden fejl (ja/nej + kommandooutput)
  - Om mindst én uoverensstemmelse eller hul er fundet relativt til P059 (ja/nej + detalje)
- Der er evidens for hvert Nej-Go-trigger, hvis relevant:
  - Om temporale regler kræver workarounds (ja/nej + detalje)
  - Om fordringskompleks-propagation kræver workarounds uden for Catala's model (ja/nej + detalje)
  - Om indkodningsindsats per G.A.-afsnit overstiger 4 person-dage (ja/nej + skøn)

**What "success" looks like**
- En reviewer kan afgøre Go/Nej-Go-resultatet og dets grundlag uden at referere til noget
  eksternt dokument ud over spike-rapporten selv.

---

## Acceptance criteria

The following are binary pass/fail checks. Each must pass for the petition to be closed Done.

**AC-1 (FR-1 — fil til stede)**  
`catala/ga_2_4_foraeldelse.catala_da` eksisterer i repositoriet.

**AC-2 (FR-1 — dansk dialekt)**  
Filen erklærer den danske Catala-dialekt (`catala_da`); sprogdeklarationen er til stede øverst
i filen.

**AC-3 (FR-1.1/1.2 — base frist-regler til stede)**  
Filen indeholder identificerbare Catala-regelblokke for den 3-årige base frist og den 10-årige
undtagelse ved udlæg + særligt retsgrundlag, begge forankret til GIL § 18a, stk. 4 og
Forældelsesl. § 5, stk. 1 henholdsvis.

**AC-4 (FR-1.3 — PSRM-udskydelsesregel til stede)**  
Filen indeholder et identificerbart Catala-regelblok for PSRM-udskydelsen, der fastsætter
begyndelsestidspunktet til tidligst 20-11-2021, forankret til GIL § 18a, stk. 1, 1. pkt.

**AC-5 (FR-1.4 — DMI/SAP38-udskydelsesregel til stede)**  
Filen indeholder et identificerbart Catala-regelblok for DMI/SAP38-udskydelsen, der fastsætter
begyndelsestidspunktet til tidligst 20-11-2027, forankret til GIL § 18a, stk. 1, 2. pkt.

**AC-6 (FR-1.5 — udskydelse er uforanderlig)**  
Filen indeholder et Catala-udtryk eller en constraint, der eksplicit angiver, at
udskydelsesdatoen ikke kan ændres af efterfølgende afbrydelseshændelser.

**AC-7 (FR-2.1 — berostillelse til stede)**  
Filen indeholder et identificerbart Catala-regelblok for berostillelse-afbrydelse, forankret
til GIL § 18a, stk. 8, der angiver ny 3-årig frist og begrænsning til PSRM-fordringer.

**AC-8 (FR-2.2 — varsel/afgørelse-distinktion)**  
Filen indeholder et Catala-regelblok, der eksplicit afviser lønindeholdelsesvarsel som
afbrydelsesgrundlag, forankret til GIL § 18, stk. 4 og SKM2015.718.ØLR.

**AC-9 (FR-2.2 — lønindeholdelse afgørelse til stede)**  
Filen indeholder et identificerbart Catala-regelblok for lønindeholdelse afgørelse som
afbrydelse ved underretning til debitor, forankret til GIL § 18, stk. 4.

**AC-10 (FR-2.3 — forgæves udlæg ligestillet)**  
Filen indeholder et Catala-regelblok for udlæg-afbrydelse, der eksplicit udtrykker ligestillingen
af forgæves og vellykket udlæg i afbrydelsesvirkningen, forankret til Forældelsesl. § 18, stk. 1.

**AC-11 (FR-2.3 — udlæg med og uden særligt retsgrundlag)**  
Filen indeholder identificerbare Catala-regelblokke for henholdsvis 3-årig ny frist (uden
særligt retsgrundlag) og 10-årig ny frist (med særligt retsgrundlag) ved udlæg.

**AC-12 (FR-3 — fordringskompleks-propagation til stede)**  
Filen indeholder identificerbare Catala-regelblokke for fordringskompleks-definition,
afbrydelsespropagation og atomicitetsforpligtelse, forankret til GIL § 18a, stk. 2.

**AC-13 (FR-4 — tillægsfrist-formel til stede)**  
Filen indeholder et Catala-udtryk for tillægsfrist-beregning, der eksplicit anvender
`max(currentFristExpires, eventDate) + 2 år`, forankret til G.A.2.4.4.2.

**AC-14 (FR-5 — testfil til stede)**  
`catala/tests/ga_foraeldelse_tests.catala_da` eksisterer i repositoriet.

**AC-15 (FR-5 — minimum testantal)**  
Testfilen indeholder mindst **10** distinkte testcases udtrykt med Catala's `Test`-modul.

**AC-16 (FR-5 — testkompilering lykkes)**  
`catala test-doc catala/tests/ga_foraeldelse_tests.catala_da` afsluttes med kode 0, og alle
testcases rapporterer `PASS`.

**AC-17 (FR-6 — spike-rapport til stede)**  
`catala/SPIKE-REPORT-070.md` eksisterer i repositoriet.

**AC-18 (FR-6 — sammenligningstabell dækker alle 29 scenarier)**  
`catala/SPIKE-REPORT-070.md` indeholder en tabel, der afbilder hvert af de 29 P059-scenarier
fra `petitions/petition059-foraeldelse.feature` til en dækningsstatus.

**AC-19 (FR-6 — alle fire rapportafsnit til stede)**  
`catala/SPIKE-REPORT-070.md` indeholder alle fire afsnit: Dækning, Huller, Uoverensstemmelser
og Indsatsskøn — hvert med substantielt indhold eller en eksplicit "Ingen fundet"-erklæring.

**AC-20 (FR-6 — discrepancy hotspots adresseret)**  
`catala/SPIKE-REPORT-070.md` adresserer eksplicit alle fem discrepancy hotspots fra
petitionen: (1) varsel vs afgørelse, (2) fordringskompleks atomicitet, (3) udskydelse
immutabilitet, (4) forgæves udlæg-ligestilling, (5) tillægsfrist max()-formel.

**AC-21 (FR-7 — Go/Nej-Go til stede)**  
`catala/SPIKE-REPORT-070.md` indeholder et eksplicit Go/Nej-Go-afsnit med en klar **Go**-
eller **Nej-Go**-dom.

**AC-22 (FR-7 — evidens for hvert kriterium)**  
Go/Nej-Go-afsnittet indeholder evidens (ja/nej med begrundelse) for hvert kriterium listet
i FR-7 (fem Go-kriterier; tre Nej-Go-triggere).

**AC-23 (NFR-1 — kompilering med kode 0)**  
Kørsel af `catala ocaml ga_2_4_foraeldelse.catala_da` fra `catala/`-mappen afsluttes med
kode 0.

**AC-24 (NFR-4 — ingen produktionsartefakter modificeret)**  
Ingen Java-kildefiler er oprettet eller ændret af dette spike.  
Ingen databasemigreringsscripts er tilføjet under nogen `src/main/resources/db/migration`-sti.  
Ingen OpenAPI/Swagger-specifikationsfiler er ændret.  
Ingen Spring Boot-modulkonfigurationsfiler er oprettet eller ændret.

---

## Definition of Done

*(Verbatim fra Petition 070)*

- [ ] D-1 kompilerer uden fejl (`catala ocaml ga_2_4_foraeldelse.catala_da`)
- [ ] D-2 testsuite kører med alle tests bestående
- [ ] D-3 spike-rapport indeholder eksplicit Go/Nej-Go med evidens for hvert kriterium
- [ ] D-3 sammenligningstabell dækker alle 29 P059-scenarier
- [ ] Alle fem discrepancy hotspots er eksplicit adresseret i D-3
- [ ] Mindst én hul- eller uoverensstemmelse-finding dokumenteret (eller eksplicit bekræftet fraværende)
- [ ] Ingen produktionsfiler modificerede; ingen migreringer, API-specs eller Java-kilde ændret

---

## Deliverables

| # | Artefakt | Sti | Verificeret af |
|---|----------|-----|----------------|
| D-1 | Catala-kildefil — forældelse | `catala/ga_2_4_foraeldelse.catala_da` | AC-1, AC-2, AC-3, AC-4, AC-5, AC-6, AC-7, AC-8, AC-9, AC-10, AC-11, AC-12, AC-13, AC-23 |
| D-2 | Catala-testfil | `catala/tests/ga_foraeldelse_tests.catala_da` | AC-14, AC-15, AC-16 |
| D-3 | Spike-rapport | `catala/SPIKE-REPORT-070.md` | AC-17, AC-18, AC-19, AC-20, AC-21, AC-22 |

---

## Failure conditions

- `catala/ga_2_4_foraeldelse.catala_da` er fraværende fra repositoriet.
- Nogen af de fem FR-1-underregler mangler eller mangler en artikelforankring.
- Det Catala-regelblok, der afviser lønindeholdelsesvarsel, er fraværende eller citerer ikke
  SKM2015.718.ØLR.
- Forgæves udlæg og vellykket udlæg behandles forskelligt i afbrydelsesvirkningen.
- Fordringskompleks-propagationens atomicitetsforpligtelse er fraværende.
- Tillægsfrist-formlen anvender `eventDate + 2 år` i stedet for `max(currentFristExpires, eventDate) + 2 år`.
- Testfilen indeholder færre end 10 testcases.
- Nogen testcase i D-2 fejler, når Catala CLI kører testfilen.
- `catala/SPIKE-REPORT-070.md` er fraværende fra repositoriet.
- Sammenligningsabellen udelader et eller flere af de 29 P059-scenarier.
- Et eller flere af de fem discrepancy hotspots er ikke adresseret i D-3.
- Go/Nej-Go-afsnittet er fraværende eller angiver ikke en klar dom.
- Evidens mangler for nogen Go-kriterium eller Nej-Go-trigger i FR-7.
- `catala ocaml ga_2_4_foraeldelse.catala_da` afsluttes med en kode forskellig fra 0.
- Nogen Java-kildefil, databasemigrering, OpenAPI-spec eller Spring Boot-modulkonfiguration
  er modificeret eller oprettet som del af dette spike.
- G.A.-citater refererer til en anden snapshot-version end v3.16 (2026-03-28).
