# Petition 069 Outcome Contract

## Petition reference

**Petition 069:** Catala Compliance Spike — Dækningsrækkefølge GIL § 4 (companion to P057)  
**Type:** Research spike — no production code delivered.  
**Legal basis:** GIL § 4, stk. 1–4; GIL § 6a; GIL § 10b; Gæld.bekendtg. § 4, stk. 3;
Gæld.bekendtg. § 7; Retsplejelovens § 507; Lov nr. 288/2022  
**Time box:** 2 working days.  
**G.A. snapshot:** v3.16 (2026-03-28).  
**References:** Petition 057 (dækningsrækkefølge — GIL § 4, fuld spec), G.A.2.3.2.1

> **Spike note:** All deliverables for this petition are **files** — a Catala source program,
> a Catala test suite, and a markdown spike report. There is no runtime behaviour, no new API
> surface, no database migrations, and no portal changes. Acceptance criteria are therefore
> expressed as file-system and content assertions, plus a compilation exit-code check.

---

## Observable outcomes by functional requirement

### FR-1: Catala-kodning af prioritetskategorier — GIL § 4, stk. 1

**Leverance**
- Filen `catala/ga_2_3_2_1_daekningsraekkefoeigen.catala_da` er til stede i repositoriet.

**Forventet indhold**
- Filen koder alle fire prioritetskategorier i GIL § 4, stk. 1, i streng hierarkisk rækkefølge:
  - FR-1.1 Kategori 1: rimelige omkostninger ved udenretlig inddrivelse i udlandet
    (GIL § 6a, stk. 1; GIL § 4, stk. 12)
  - FR-1.2 Kategori 2: bøder, tvangsbøder og tilbagebetalingskrav (GIL § 10b),
    inkl. tvangsbøder tilføjet ved lov nr. 288/2022, § 2, nr. 1
  - FR-1.3 Kategori 3: underholdsbidrag — privatretlige dækkes forud for offentlige
    (GIL § 4, stk. 1, nr. 2)
  - FR-1.4 Kategori 4: andre fordringer under inddrivelse (GIL § 4, stk. 1, nr. 3)
- Hvert kategoriregel-blok er forankret til sin GIL § 4 / GIL § 6a / GIL § 10b artikel-citation
  via Catala's artikel-citations-syntaks.
- Kildesproget er dansk (`catala_da`).
- Alle G.A.-citater refererer snapshot v3.16 (2026-03-28).

**Hvad ser "succes" ud som**
- En reviewer kan entydigt identificere et Catala `scope`- eller `rule`-blok for hver af de
  fire kategorier med en klar artikel-forankring og uden workarounds.

---

### FR-2: Catala-kodning af FIFO-ordning og 6-niveauers rentesekvens — GIL § 4, stk. 2

**Leverance**
- `catala/ga_2_3_2_1_daekningsraekkefoeigen.catala_da` indeholder FR-2-reglerne.

**Forventet indhold**
- FR-2.1 FIFO-grundregel: fordringen med ældste modtagelsesdato dækkes først
  (GIL § 4, stk. 2, 1. pkt.)
- FR-2.2 Pre-2013-særregel: legacyModtagelsesdato anvendes som FIFO-nøgle
  (GIL § 4, stk. 2, 5. pkt.)
- FR-2.3 Renter-før-hovedkrav: rentekrav dækkes forud for hovedkravet inden for fordringen
  (GIL § 4, stk. 2, 2. pkt.)
- FR-2.4 Den 6-niveauers PSRM-rentesekvens (Gæld.bekendtg. § 4, stk. 3) i præcis rækkefølge:
  1. Opkrævningsrenter
  2. Inddrivelsesrenter beregnet af fordringshaver (Gæld.bekendtg. § 9, stk. 3, 2./4. pkt.)
  3. Inddrivelsesrenter påløbet inden tilbageførsel (Gæld.bekendtg. § 8, stk. 3)
  4. Inddrivelsesrenter under inddrivelsen (Gæld.bekendtg. § 9, stk. 1)
  5. Øvrige renter beregnet af RIM (Gæld.bekendtg. § 9, stk. 3, 1./3. pkt.)
  6. Hovedstol
- Alle seks under-positioner er forankret til Gæld.bekendtg. § 4, stk. 3 i Catala-kilden.

**Hvad ser "succes" ud som**
- Alle seks under-positioner svarer til et distinkt, identificerbart Catala-regelblok.
- Ingen af de seks positioner er slået sammen eller udeladt.

---

### FR-3: Catala-kodning af inddrivelsesindsats-regel og udlæg-undtagelse — GIL § 4, stk. 3

**Leverance**
- `catala/ga_2_3_2_1_daekningsraekkefoeigen.catala_da` indeholder FR-3-reglerne.

**Forventet indhold**
- FR-3.1 Grundregel: beløb fra inddrivelsesindsats dækker indsats-fordringer først
  (GIL § 4, stk. 3, 1. pkt.)
- FR-3.2 Overskudsregel: residualbeløb dækker øvrige fordringer, der kan inddrives med samme
  indsatstype (GIL § 4, stk. 3, 2. pkt.)
- FR-3.3 Afdragsordning-undtagelse: overskudsbeløb fra afdragsordning kan dække tvangsbøder
  (GIL § 4, stk. 3, 2. pkt., 2. led)
- FR-3.4 Udlæg-undtagelse: GIL § 4, stk. 3 gælder **ikke** for udlæg-beløb; disse må kun
  dække fordringer encompassed af udlægget (Retsplejelovens § 507)
- Artikel-forankring til GIL § 4, stk. 3 og Retsplejelovens § 507 er til stede.

**Hvad ser "succes" ud som**
- Grundregel og udlæg-undtagelse er kodede som gensidigt eksklusive Catala-regler.
- En reviewer kan bekræfte, at udlæg-residualbeløb *aldrig* flyder til fordringer uden for
  udlægget i Catala-kodningen.

---

### FR-4: Catala-kodning af timingregel — GIL § 4, stk. 4

**Leverance**
- `catala/ga_2_3_2_1_daekningsraekkefoeigen.catala_da` indeholder FR-4-reglerne.

**Forventet indhold**
- FR-4.1 Applikationstidspunkt: rækkefølgen fastlægges på det tidspunkt, RIM anvender beløbet
  (GIL § 4, stk. 4, 1. pkt.)
- FR-4.2 Betalingstidspunkt: dækning har virkning fra det tidspunkt, skyldner mistede
  rådigheden over beløbet (GIL § 4, stk. 4, 2. pkt.)
- Artikel-forankring til GIL § 4, stk. 4, 1. og 2. pkt. er til stede.

**Hvad ser "succes" ud som**
- En reviewer kan entydigt identificere to adskilte Catala-regler: én for fastlæggelse af
  rækkefølgen og én for virkningstidspunktet.

---

### FR-5: Catala-kodning af opskrivningsfordring-placering — Gæld.bekendtg. § 7

**Leverance**
- `catala/ga_2_3_2_1_daekningsraekkefoeigen.catala_da` indeholder FR-5-reglerne.

**Forventet indhold**
- FR-5.1 En opskrivningsfordring placeres umiddelbart efter sin stamfordring i rækkefølgen;
  er stamfordringen dækket, arves stamfordringens FIFO-plads (Gæld.bekendtg. § 7)
- FR-5.2 Opskrivningsfordringen starter **ikke** en ny FIFO-position baseret på sin egen
  modtagelsesdato — dette kodede som en eksplicit undtagelse fra FR-2.1
- FR-5.3 Flere opskrivningsfordringer for samme stamfordring ordnes indbyrdes efter FIFO
  (Gæld.bekendtg. § 7; GIL § 4, stk. 2)
- Artikel-forankring til Gæld.bekendtg. § 7 er til stede.

**Hvad ser "succes" ud som**
- Opskrivningsfordring-placeringen er kodede som en undtagelse der tilsidesætter den generelle
  FIFO-regel, med klar artikel-forankring.

---

### FR-6: Catala-testsuite

**Leverance**
- Filen `catala/tests/ga_daekningsraekkefoeigen_tests.catala_da` er til stede i repositoriet.

**Forventet indhold**
- Mindst 8 individuelle testcases udtrykt via Catala's indbyggede `Test`-modul.
- Dækning inkluderer:
  - Alle fire FR-1 prioritetskategorier (inkl. tvangsbøde som kategori 2)
  - FR-2.1 FIFO-ordning: ældre fordring dækkes før nyere inden for samme kategori
  - FR-2.4 Alle 6 under-positioner i rentesekvensen — fuld sekvenstest
  - FR-3.1 Inddrivelsesindsats: grundregel og overskudsregel
  - FR-3.4 Udlæg-undtagelse: residualbeløb dækker ikke andre fordringer
  - FR-5.1 Opskrivningsfordring: placeret umiddelbart efter stamfordring
  - FR-4 Timingregel: rækkefølge fastlægges på applikationstidspunkt
  - Boundary-case: enkelt fordring, fuld betaling
- Alle tests eksekverer og består ved Catala CLI-kørsel.

**Hvad ser "succes" ud som**
- `catala test-doc catala/tests/ga_daekningsraekkefoeigen_tests.catala_da` afsluttes med
  exit-kode 0 og alle testcases rapporteret som `PASS`.

---

### FR-7: Sammenligningsrapport mod P057 Gherkin-scenarier

**Leverance**
- Filen `catala/SPIKE-REPORT-069.md` er til stede i repositoriet.

**Forventet indhold**
- En markdown-tabel der for hvert af P057's 22 Gherkin-scenarier (fra
  `petitions/petition057-daekningsraekkefoeigen.feature`) angiver én af:
  **Dækket**, **Ikke dækket** eller **Diskrepans fundet**.
- Et **Huller**-afsnit med regelgrene kodet i Catala men ikke dækket af noget P057-scenarie
  (eller eksplicit "Ingen fundet").
- Et **Diskrepanser**-afsnit med tilfælde, hvor et P057-scenarie tilsyneladende modsiger
  G.A.-teksten (eller eksplicit "Ingen fundet").
- Et **Diskrepans-hotspots**-afsnit med eksplicit vurdering af alle tre identificerede hotspots:
  6-niveauers rentesekvens, udlæg-undtagelse og opskrivningsfordring-placering.
- Et **Estimat**-afsnit med personedage-estimat for kodning af det samlede G.A. Inddrivelse-kapitel.

**Hvad ser "succes" ud som**
- Hvert af P057's 22 Gherkin-scenarier har en rad i sammenligningsoverdrevne.
- Alle fem afsnit (Dækning, Huller, Diskrepanser, Diskrepans-hotspots, Estimat) er til stede
  med substantielt indhold eller eksplicit "Ingen fundet"-erklæring.

---

### FR-8: Go/No-Go-anbefaling

**Leverance**
- `catala/SPIKE-REPORT-069.md` indeholder en eksplicit Go/No-Go-anbefaling.

**Forventet indhold**
- Anbefalingssektionen angiver entydigt enten **Go** eller **No-Go**.
- Dokumentation for hvert Go-kriterium:
  - Om alle fire prioritetskategorier er kodet uden tvetydighed (ja/nej + evidens)
  - Om mindst 1 diskrepans eller hul er fundet relativt til P057 Gherkin (ja/nej + detalje)
  - Om Catala-testkompilering lykkedes uden fejl (ja/nej + kommandoutput)
  - Om OCaml-ekstraktion producerede kørbar kode (ja/nej + evidens)
- Dokumentation for hvert No-Go-kriterium (hvis relevant):
  - Om 6-niveauers rentesekvens kunne udtrykkes uden workarounds (ja/nej + detalje)
  - Om juridiske tvetydigheder blokerede formel kodning (ja/nej + G.A.-reference)
  - Om kodningsindsats per G.A.-afsnit oversteg 4 personedage (ja/nej + estimat)

**Hvad ser "succes" ud som**
- En reviewer kan fastslå Go/No-Go-resultatet og dets grundlag alene ved læsning af
  spike-rapporten, uden at konsultere eksterne dokumenter.

---

## Acceptance criteria

Følgende er binære bestå/fejl-tjek. Samtlige skal bestås for at petitionen kan lukkes Done.

**AC-1 (FR-1 — fil til stede)**  
`catala/ga_2_3_2_1_daekningsraekkefoeigen.catala_da` eksisterer i repositoriet.

**AC-2 (FR-1 — fire kategorier med artikel-forankring)**  
Filen indeholder identificerbare Catala-regelblokke for alle fire prioritetskategorier
(FR-1.1 til FR-1.4), hver forankret til GIL § 4, stk. 1 / GIL § 6a / GIL § 10b.

**AC-3 (FR-1 — tvangsbøder eksplicit kodet)**  
Filen indeholder en eksplicit Catala-regel der koder tvangsbøder som kategori 2 med
reference til lov nr. 288/2022, § 2, nr. 1.

**AC-4 (FR-2 — FIFO-regel med modtagelsesdato og pre-2013-særregel)**  
Filen indeholder Catala-regelblokke for FIFO-grundregel (GIL § 4, stk. 2, 1. pkt.) og
pre-2013-særregel (GIL § 4, stk. 2, 5. pkt.) med artikel-forankring til begge.

**AC-5 (FR-2 — 6-niveauers rentesekvens komplet)**  
Filen indeholder identificerbare Catala-regelblokke for alle seks under-positioner i
rentesekvensen (FR-2.4, Gæld.bekendtg. § 4, stk. 3) i korrekt rækkefølge. Ingen af de
seks positioner er udeladt eller slået sammen.

**AC-6 (FR-3 — inddrivelsesindsats-grundregel og overskudsregel)**  
Filen indeholder Catala-regelblokke for GIL § 4, stk. 3, grundregel (FR-3.1) og
overskudsregel (FR-3.2) med artikel-forankring.

**AC-7 (FR-3 — udlæg-undtagelse)**  
Filen indeholder en Catala-regel der koder udlæg-undtagelsen (FR-3.4) som gensidigt eksklusiv
med grundreglen og forankret til Retsplejelovens § 507.

**AC-8 (FR-4 — timingregel)**  
Filen indeholder Catala-regelblokke for applikationstidspunkt (FR-4.1) og betalingstidspunkt
(FR-4.2), forankret til GIL § 4, stk. 4, 1. og 2. pkt.

**AC-9 (FR-5 — opskrivningsfordring-placering)**  
Filen indeholder en Catala-undtagelsesregel der koder opskrivningsfordring-placeringen som
arv fra stamfordringens FIFO-nøgle (FR-5.1–FR-5.2), forankret til Gæld.bekendtg. § 7.

**AC-10 (FR-6 — testfil til stede)**  
`catala/tests/ga_daekningsraekkefoeigen_tests.catala_da` eksisterer i repositoriet.

**AC-11 (FR-6 — minimum antal tests)**  
Testfilen indeholder mindst 8 distinkte testcases udtrykt via Catala's `Test`-modul og
dækker alle tre diskrepans-hotspot-regelgrene.

**AC-12 (FR-7 — spike-rapport til stede)**  
`catala/SPIKE-REPORT-069.md` eksisterer i repositoriet.

**AC-13 (FR-7 — dækningsoversigt for alle P057-scenarier)**  
`catala/SPIKE-REPORT-069.md` indeholder en tabel der mapper hvert af P057's 22 Gherkin-scenarier
(fra `petitions/petition057-daekningsraekkefoeigen.feature`) til en dækningsstatus
(Dækket / Ikke dækket / Diskrepans fundet).

**AC-14 (FR-7 — alle fem afsnit til stede)**  
`catala/SPIKE-REPORT-069.md` indeholder alle fem afsnit: Dækning, Huller, Diskrepanser,
Diskrepans-hotspots og Estimat — hvert med substantielt indhold eller eksplicit
"Ingen fundet"-erklæring.

**AC-15 (FR-7 — tre hotspots eksplicit vurderet)**  
`catala/SPIKE-REPORT-069.md` indeholder eksplicit vurdering af alle tre diskrepans-hotspots:
6-niveauers rentesekvens (FR-2.4), udlæg-undtagelse (FR-3.4) og opskrivningsfordring-placering
(FR-5.2). Hvert hotspot har en klar finding: **Fund** eller **Ingen diskrepans**.

**AC-16 (FR-8 — Go/No-Go til stede)**  
`catala/SPIKE-REPORT-069.md` indeholder en eksplicit Go/No-Go-sektion med en klar **Go** eller
**No-Go**-verdict.

**AC-17 (FR-8 — dokumentation for hvert kriterium)**  
Go/No-Go-sektionen indeholder dokumentation (ja/nej med begrundelse) for alle fire Go-kriterier
og alle tre No-Go-triggers som angivet i FR-8.

**AC-18 (NFR-1 — kompilering afsluttes med exit-kode 0)**  
`catala ocaml ga_2_3_2_1_daekningsraekkefoeigen.catala_da` kørt fra `catala/`-mappen afsluttes
med exit-kode 0.

**AC-19 (NFR-2 — dansk dialekt)**  
Kildefilen deklarerer den danske Catala-dialekt (`catala_da`); ingen engelsksprogede kildefiler
produceres.

**AC-20 (NFR-3 — G.A.-versionscitater)**  
Alle G.A.-artikel-citater i kildefilen refererer G.A. snapshot v3.16 (2026-03-28), svarende til
den version der anvendes i petition 057.

**AC-21 (NFR-4 — ingen produktionsartefakter ændret)**  
Ingen Java-kildefiler oprettes eller ændres som del af denne spike.  
Ingen database-migrationsscripts tilføjes.  
Ingen OpenAPI/Swagger-specifikationsfiler ændres.  
Ingen Spring Boot-modulkonfigurationsfiler oprettes eller ændres.

---

## Definition of Done

*(Ordret fra Petition 069)*

- [ ] D-1 kompilerer uden fejl (`catala ocaml ga_2_3_2_1_daekningsraekkefoeigen.catala_da`)
- [ ] D-2 testsuite eksekverer med alle tests bestående
- [ ] D-3 spike-rapport indeholder eksplicit Go/No-Go med dokumentation for hvert kriterium
- [ ] D-3 sammenligningsoversigt dækker alle 22 P057 Gherkin-scenarier
- [ ] Alle tre diskrepans-hotspots er eksplicit vurderet i D-3
- [ ] Mindst ét hul- eller diskrepans-fund er dokumenteret (eller eksplicit bekræftet som fraværende)
- [ ] Ingen produktionsfiler ændret; ingen migrationer, API-specs eller Java-kilder ændret

---

## Deliverables

| # | Artefakt | Sti | Verificeret af |
|---|----------|-----|----------------|
| D-1 | Catala-kilde — dækningsrækkefølge | `catala/ga_2_3_2_1_daekningsraekkefoeigen.catala_da` | AC-1, AC-2, AC-3, AC-4, AC-5, AC-6, AC-7, AC-8, AC-9, AC-18 |
| D-2 | Catala-testfil | `catala/tests/ga_daekningsraekkefoeigen_tests.catala_da` | AC-10, AC-11 |
| D-3 | Spike-rapport | `catala/SPIKE-REPORT-069.md` | AC-12, AC-13, AC-14, AC-15, AC-16, AC-17 |

---

## Failure conditions

- `catala/ga_2_3_2_1_daekningsraekkefoeigen.catala_da` mangler i repositoriet.
- En eller flere af de fire prioritetskategorier (FR-1.1–FR-1.4) mangler eller mangler
  artikel-forankring.
- Tvangsbøder-reglen (lov nr. 288/2022) er ikke eksplicit kodet som kategori 2.
- En eller flere af de seks under-positioner i rentesekvensen (FR-2.4) mangler eller er slået
  sammen — dette er den kritiske testbetingelse for det primære hotspot.
- Udlæg-undtagelsen (FR-3.4) er ikke kodet som gensidigt eksklusiv med inddrivelsesindsats-
  grundreglen.
- Opskrivningsfordring-placeringen (FR-5) mangler eller koder opskrivningsfordringen til at
  starte en ny selvstændig FIFO-position.
- `catala/tests/ga_daekningsraekkefoeigen_tests.catala_da` mangler eller indeholder færre end
  8 testcases.
- En eller flere testcases i D-2 fejler ved Catala CLI-kørsel.
- `catala/SPIKE-REPORT-069.md` mangler i repositoriet.
- Sammenligningsoversigten udelader et eller flere af P057's 22 Gherkin-scenarier.
- Et eller flere af de tre diskrepans-hotspots mangler eksplicit vurdering.
- Go/No-Go-sektionen mangler eller angiver ikke en klar verdict.
- Dokumentation mangler for ét eller flere Go-kriterier eller No-Go-triggers i FR-8.
- `catala ocaml ga_2_3_2_1_daekningsraekkefoeigen.catala_da` afsluttes med en exit-kode
  forskellig fra 0.
- En Java-kildefil, database-migrationsscript, OpenAPI-spec eller Spring Boot-modulkonfiguration
  oprettes eller ændres som del af denne spike.
- G.A.-citater refererer et andet snapshot end v3.16 (2026-03-28).
