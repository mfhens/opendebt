# Petition 069: Catala Compliance Spike — Dækningsrækkefølge GIL § 4 (companion to P057)

## Summary

Conduct a time-boxed (2 working days) research spike to determine whether the open-source
legal programming language [Catala](https://catala-lang.org/) can formally encode the
dækningsrækkefølge rules of GIL § 4, stk. 1–4, as specified in G.A.2.3.2.1 (v3.16,
2026-03-28). The spike produces a Go/No-Go recommendation with evidence and serves as the
Catala companion to petition 057.

**Type:** Research spike — no production code is delivered.  
**Time box:** 2 working days.  
**G.A. snapshot:** v3.16 (2026-03-28).  
**Companion:** Petition 057 (dækningsrækkefølge — GIL § 4, fuld spec).  
**References:** G.A.2.3.2.1, GIL § 4 stk. 1–4, GIL § 6a, GIL § 10b, Gæld.bekendtg. § 4 stk. 3,
Gæld.bekendtg. § 7, Retsplejelovens § 507, Lov nr. 288/2022.

---

## Context and Motivation

The juridisk vejledning (G.A. Inddrivelse) defines the legal rules that OpenDebt must implement.
Today these rules are captured as:

1. **Narrative petitions** — human-readable requirements grounded in G.A. references.
2. **Gherkin feature files** — executable scenarios that express expected backend and portal
   behaviour.
3. **Specification documents** — class-level implementation contracts produced by the pipeline.

This chain works, but it has a gap: there is no *formal, executable representation of the law
itself*. A legal rule encoded only in natural language and Gherkin can silently diverge from
the G.A. text. Errors in this translation are discovered late — typically in acceptance testing
or legal review — and are expensive to correct.

**Catala** is a domain-specific language developed by Inria and the French Ministry of Finance
specifically for encoding legislative and regulatory texts as executable programs. It has been
used to formalize the French tax code and UK welfare rules. Its key property is that each rule
block is *anchored to a specific article* of the source legislation, making the encoding
bidirectionally traceable and auditable.

The hypothesis is: if GIL § 4, stk. 1–4 can be encoded in Catala without ambiguity,
then the Catala program can act as an *oracle* for the OpenDebt test suite — generating
authoritative test cases and flagging implementation gaps that Gherkin alone cannot detect.

### Why GIL § 4 (G.A.2.3.2.1)?

GIL § 4 is the most legally consequential rule engine in OpenDebt. An incorrect dækningsrækkefølge:

- Distributes money to the wrong fordring or creditor.
- Creates legally invalid dækning records that must be reversed and replayed.
- Causes CLS audit failures — Gældsstyrelsen cannot document GIL § 4 compliance per dækning.
- Exposes Gældsstyrelsen to liability under forvaltningslovens § 22.

Petition 057 implemented the full rule engine in Gherkin (22 scenarios) and Java. The Gherkin
scenarios capture expected *system behaviour*. Catala captures the *law itself*. These two layers
should agree — and where they diverge, one of them is wrong.

### Discrepancy hotspots (highest value for the spike)

Three rule areas are known implementation risk zones where the Catala encoding is most likely
to surface discrepancies relative to P057 Gherkin:

| Hotspot | Risk | Legal source |
|---------|------|-------------|
| 6-tier PSRM interest ordering within a single fordring | PSRM implementations often collapse the 6 sub-positions into fewer tiers; any simplification is legally incorrect | Gæld.bekendtg. § 4, stk. 3 |
| Udlæg exception (retsplejelovens § 507) | Many systems apply inddrivelsesindsats surplus universally; udlæg surplus must stay with the udlæg fordring only | GIL § 4, stk. 3 + Retsplejelovens § 507 |
| Opskrivningsfordring positioning | Whether the opskrivningsfordring truly follows its parent immediately vs. takes a new FIFO queue position based on its own modtagelsesdato | Gæld.bekendtg. § 7 |

The spike must explicitly evaluate each hotspot and document its finding in `catala/SPIKE-REPORT-069.md`.

### Domain Terms

| Dansk | Engelsk | Definition |
|-------|---------|------------|
| Catala | Catala | Open-source DSL til formalisering af lovtekster; producerer OCaml/Python via kompilering |
| Dækningsrækkefølge | Payment application order | Den retligt fastsatte rækkefølge, hvori modtagne betalinger fordeles på skyldnerens udestående fordringer |
| Dækning | Coverage / allocation | Anvendelse af et modtaget beløb — helt eller delvist — på en specifik fordring eller omkostningskomponent |
| Fordring | Claim / debt | En gæld overdraget til Gældsstyrelsen til inddrivelse |
| Prioritetskategori | Priority category | En af de fire kategorier i GIL § 4, stk. 1, der definerer rækkefølgen på tværs af fordringer |
| FIFO | First In, First Out | Sorteringsprincip inden for en prioritetskategori: ældst modtagne fordring dækkes først |
| Modtagelsesdato | Receipt date | Dato for fordringens modtagelse i inddrivelsessystemet; FIFO-sorteringsnøgle |
| Betalingstidspunkt | Payment timestamp | Det tidspunkt, skyldner mistede rådigheden over beløbet; dækningen har virkning fra dette tidspunkt |
| Inddrivelsesindsats | Enforcement action | Et specifikt inddrivelsestiltag (fx udlæg, lønindeholdelse, modregning) |
| Udlæg | Attachment / levy | Fogedretlig inddrivelsesforanstaltning under retsplejelovens § 507 |
| Opskrivningsfordring | Write-up claim | Særskilt fordring oversendt af fordringshaver til forhøjelse af en eksisterende fordring i PSRM |
| Tvangsbøde | Coercive fine | Administrativ tvangsforanstaltning; tilføjet kategori 2 ved lov nr. 288/2022 |
| Orakel | Oracle | En autoritativ eksekverbar specifikation der anvendes til generering og validering af testcases |

---

## Legal Basis

| Reference | Indhold relevant for spike |
|-----------|---------------------------|
| GIL § 4, stk. 1 | Fire prioritetskategorier: (1) rimelige omkostninger ved udenretlig inddrivelse i udlandet (§ 6a), (2) bøder, tvangsbøder og tilbagebetalingskrav (§ 10b), (3) underholdsbidrag (privatretlige før offentlige), (4) andre fordringer |
| GIL § 4, stk. 2 | FIFO-ordning inden for kategori efter modtagelsesdato; renter dækkes før hovedkrav; særregel for pre-2013 fordringer |
| GIL § 4, stk. 3 | Inddrivelsesindsats-regel: beløb dækker indsats-fordringer først; overskud til øvrige indsats-fordringer af samme type; udlæg-undtagelse |
| GIL § 4, stk. 4 | Rækkefølgen fastlægges på applikationstidspunktet; dækning har virkning fra betalingstidspunktet |
| GIL § 6a, stk. 1 og stk. 12 | Definition af rimelige omkostninger ved udenretlig inddrivelse i udlandet (kategori 1) |
| GIL § 10b | Bøder, tvangsbøder og tilbagebetalingskrav (kategori 2) |
| Gæld.bekendtg. § 4, stk. 3 | Rækkefølge for dækning af rentekrav i PSRM: 6 under-positioner fra opkrævningsrenter til hovedstol |
| Gæld.bekendtg. § 7 | Opskrivningsfordringers plads i dækningsrækkefølgen umiddelbart efter stamfordringen; FIFO indbyrdes |
| Retsplejelovens § 507 | Udlæg-betaling må kun dække de fordringer, der er omfattet af udlægget |
| Lov nr. 288/2022 (§ 2, nr. 1) | Tilføjer tvangsbøder til GIL § 4, stk. 1, nr. 1 (kategori 2) |
| G.A.2.3.2.1 (v3.16, 2026-03-28) | G.A.-formulering af den fulde dækningsrækkefølge, inkl. PSRM-specifikke regler og opskrivningsfordringer |

---

## Functional Requirements

### FR-1: Catala-kodning af prioritetskategorier — GIL § 4, stk. 1

Producér en Catala-kildefil (`catala/ga_2_3_2_1_daekningsraekkefoeigen.catala_da`) der formelt
koder de fire prioritetskategorier i GIL § 4, stk. 1, i streng hierarkisk rækkefølge:

- **FR-1.1** Kategori 1 — Rimelige omkostninger ved udenretlig inddrivelse i udlandet
  (GIL § 6a, stk. 1; GIL § 4, stk. 12)
- **FR-1.2** Kategori 2 — Bøder, tvangsbøder og tilbagebetalingskrav efter GIL § 10b,
  inkl. tvangsbøder tilføjet ved lov nr. 288/2022 (§ 2, nr. 1)
- **FR-1.3** Kategori 3 — Underholdsbidrag: privatretlige underholdsbidrag dækkes forud for
  offentlige krav (GIL § 4, stk. 1, nr. 2)
- **FR-1.4** Kategori 4 — Andre fordringer: alle fordringer under inddrivelse, der ikke er
  omfattet af kategori 1–3 (GIL § 4, stk. 1, nr. 3)

Hvert kategoriregel-blok skal i Catala-kilden forankres til sin artikel via Catala's
artikel-citations-syntaks og referere G.A. snapshot v3.16 (2026-03-28).

### FR-2: Catala-kodning af FIFO-ordning og rentesekvens — GIL § 4, stk. 2

Udvid Catala-kildefilen med regler for ordning inden for de enkelte kategorier:

- **FR-2.1** FIFO-grundregel: fordringen med den ældste modtagelsesdato dækkes først
  (GIL § 4, stk. 2, 1. pkt.)
- **FR-2.2** Pre-2013-særregel: for fordringer overdraget til inddrivelse før 1. september 2013
  anvendes den i inddrivelsessystemet registrerede modtagelsesdato som FIFO-nøgle
  (GIL § 4, stk. 2, 5. pkt.)
- **FR-2.3** Renter dækkes forud for hovedkravet inden for samme fordring og kategori
  (GIL § 4, stk. 2, 2. pkt.)
- **FR-2.4** Rækkefølge for rentedækning i PSRM — 6 under-positioner i præcis rækkefølge
  (Gæld.bekendtg. § 4, stk. 3):
  1. Opkrævningsrenter (påløbet under opkrævning)
  2. Inddrivelsesrenter beregnet af fordringshaver (Gæld.bekendtg. § 9, stk. 3, 2. eller 4. pkt.)
  3. Inddrivelsesrenter påløbet inden tilbageførsel (Gæld.bekendtg. § 8, stk. 3)
  4. Inddrivelsesrenter påløbet under inddrivelsen (Gæld.bekendtg. § 9, stk. 1)
  5. Øvrige renter beregnet af RIM (Gæld.bekendtg. § 9, stk. 3, 1. eller 3. pkt.)
  6. Hovedstol

Dette er det primære diskrepans-hotspot: den 6-niveauers rentesekvens er kompleks, og PSRM-
implementeringer simplificerer den ofte til færre niveauer. Catala-kodningen skal eftervise
alle 6 under-positioner eksplicit med artikel-forankring.

### FR-3: Catala-kodning af inddrivelsesindsats-reglen og udlæg-undtagelsen — GIL § 4, stk. 3

- **FR-3.1** Grundregel: et beløb modtaget via en inddrivelsesindsats dækker indsats-fordringer
  først (GIL § 4, stk. 3, 1. pkt.)
- **FR-3.2** Overskudsregel: residualbeløb dækker andre fordringer, der kan inddrives med
  samme type indsats (GIL § 4, stk. 3, 2. pkt.)
- **FR-3.3** Afdragsordning-undtagelse: overskudsbeløb fra en afdragsordning kan også dække
  tvangsbøder, selvom afdragsordning ikke tillades til betaling af tvangsbøder
  (GIL § 4, stk. 3, 2. pkt., 2. led)
- **FR-3.4** Udlæg-undtagelse: GIL § 4, stk. 3 gælder **ikke** for beløb modtaget som følge
  af udlæg — udlægs-beløb må **kun** dække de fordringer, der er omfattet af udlægget
  (Retsplejelovens § 507)

FR-3.4 er det andet primære diskrepans-hotspot.

### FR-4: Catala-kodning af timingregel — GIL § 4, stk. 4

- **FR-4.1** Tidspunkt: dækningsrækkefølgen fastlægges på det tidspunkt, hvor RIM anvender
  beløbet til dækning (applikationstidspunktet), ikke på tidspunktet for fordringens
  registrering (GIL § 4, stk. 4, 1. pkt.)
- **FR-4.2** Virkningstidspunkt: dækningen har virkning fra betalingstidspunktet — det tidspunkt,
  skyldner mistede rådigheden over beløbet (GIL § 4, stk. 4, 2. pkt.)

### FR-5: Catala-kodning af opskrivningsfordring-placering — Gæld.bekendtg. § 7

- **FR-5.1** En opskrivningsfordring får plads i dækningsrækkefølgen umiddelbart efter den
  fordring, der opskrives. Er stamfordringen allerede dækket, arver opskrivningsfordringen
  stamfordringens FIFO-plads (Gæld.bekendtg. § 7; G.A.2.3.2.1)
- **FR-5.2** Opskrivningsfordringen starter **ikke** en ny selvstændig FIFO-køposition baseret
  på sin egen modtagelsesdato — dette er det tredje primære diskrepans-hotspot
- **FR-5.3** Er der flere opskrivningsfordringer for samme stamfordring, ordnes disse indbyrdes
  efter FIFO-princippet i GIL § 4, stk. 2 (G.A.2.3.2.1; Gæld.bekendtg. § 7)

### FR-6: Catala-testsuite og genererede testcases

Producér en Catala-testfil (`catala/tests/ga_daekningsraekkefoeigen_tests.catala_da`)
med mindst 8 tests, der dækker alle regelgrene i FR-1 til FR-5:

- Alle fire prioritetskategorier — inkl. tvangsbøde som kategori 2
- FIFO-ordning inden for kategori — ældre fordring dækkes før nyere
- Alle 6 under-positioner i rentesekvensen (FR-2.4)
- Inddrivelsesindsats-grundregel (FR-3.1) og overskudsregel (FR-3.2)
- Udlæg-undtagelse (FR-3.4): udlæg-overskud flyder **ikke** til andre fordringer
- Opskrivningsfordring-placering — umiddelbart efter stamfordring (FR-5.1)
- Timingregel — rækkefølgen fastlægges på applikationstidspunkt (FR-4.1)
- Boundary-case: enkelt fordring, fuld betaling

Tests skal udtrykkes via Catala's indbyggede `Test`-modul og alle bestå ved kørsel.

### FR-7: Sammenligningsrapport mod P057 Gherkin-scenarier

Producér en markdown-rapport (`catala/SPIKE-REPORT-069.md`) der sammenligner:

1. **Dækning**: Hvilke af P057's 22 Gherkin-scenarier (fra
   `petitions/petition057-daekningsraekkefoeigen.feature`) er dækket af Catala-testsuiten?
   Hvilke er ikke?
2. **Huller**: Afslører Catala-kodningen regelgrene, der ikke er dækket af noget P057
   Gherkin-scenarie?
3. **Diskrepanser**: Afslører Catala-kodningen tilfælde, hvor et P057 Gherkin-scenarie
   tilsyneladende modsiger G.A.-teksten?
4. **Diskrepans-hotspots**: Eksplicit vurdering af de tre identificerede hotspots:
   - 6-niveauers rentesekvens (FR-2.4): er alle 6 positioner korrekt repræsenteret i P057?
   - Udlæg-undtagelse (FR-3.4): er udlæg-residualbeløbet korrekt isoleret i P057?
   - Opskrivningsfordring-placering (FR-5): arver opskrivningsfordringen præcis FIFO-nøgle?
5. **Estimat**: Hvor mange personedage vil det kræve at kode det samlede G.A. Inddrivelse-kapitel
   i Catala på samme fidelitetsniveau?

### FR-8: Go/No-Go-anbefaling

Spike-rapporten skal indeholde en eksplicit Go/No-Go-anbefaling med dokumentation:

- **Go-kriterier** (alle skal være opfyldt):
  - Alle fire prioritetskategorier kodes uden tvetydighed
  - Mindst 1 diskrepans eller hul konstateres relativt til P057 Gherkin (demonstrerer værdien)
  - Catala-testkompilering lykkes uden fejl
  - OCaml-ekstraktion producerer kørbar kode
- **No-Go-kriterier** (ét enkelt trigger No-Go):
  - Den 6-niveauers rentesekvens kan ikke udtrykkes i Catala uden workarounds
  - G.A.-teksten er underspecificeret på en eller flere regelgrene (blokerer formel kodning)
  - Kodningsindsats per G.A.-afsnit overstiger 4 personedage (uholdbart i stor skala)

---

## Non-Functional Requirements

- **NFR-1:** Catala-kilden skal kompilere uden fejl ved brug af Catala CLI
  (`catala ocaml ga_2_3_2_1_daekningsraekkefoeigen.catala_da`).
- **NFR-2:** Catala-kilden skal være på dansk (`catala_da`) for at matche G.A.-kildesproget
  og understøtte tovejssporbarhed med den danske lovtekst.
- **NFR-3:** Alle G.A.-artikel-citater i Catala-kilden skal referere det samme G.A.-snapshot
  som P057 (version 3.16, dateret 2026-03-28).
- **NFR-4:** Ingen produktionskode, database-migrationer, API-ændringer eller Spring Boot-moduler
  introduceres af denne spike.

---

## Deliverables

| # | Artefakt | Sti | Beskrivelse |
|---|----------|-----|-------------|
| D-1 | Catala-kilde — dækningsrækkefølge | `catala/ga_2_3_2_1_daekningsraekkefoeigen.catala_da` | FR-1 til FR-5 regler med artikel-forankring |
| D-2 | Catala-testfil | `catala/tests/ga_daekningsraekkefoeigen_tests.catala_da` | FR-6 testsuite (≥ 8 tests) |
| D-3 | Spike-rapport | `catala/SPIKE-REPORT-069.md` | FR-7 sammenligning + FR-8 Go/No-Go-anbefaling |

---

## Out of Scope

Følgende er eksplicit udelukket fra denne spike:

| Element | Begrundelse |
|---------|-------------|
| Kodning af G.A.-afsnit udover G.A.2.3.2.1 | Tidsbokset; omfang valgt for overlapning med P057 |
| DMI-paralleldriftsregler (GIL § 49) | Separat regelsæt; ikke PSRM-primær |
| Runtime-integration med Spring Boot eller OpenDebt-services | Spike kun; integration er opfølgningspetition hvis Go |
| CI-pipeline-integration for Catala-kompilering | Opfølgning hvis Go |
| Fuldt G.A. Inddrivelse-kapitel i Catala | Opfølgende multi-petition-program hvis Go |
| G.A.2.3.2.2 fravigelse af dækningsrækkefølge | Behandles i separat opfølgningspetition |
| G.A.2.3.2.3 paralleldriftsperiode | DMI-specifik; ikke PSRM-fokus |
| Pro-rata fordeling på samhæftere (P062) | Separat petition |
| Forældelsesfrist-afbrydelse (P059) | Separat petition |

---

## Definition of Done

- [ ] D-1 kompilerer uden fejl (`catala ocaml ga_2_3_2_1_daekningsraekkefoeigen.catala_da`)
- [ ] D-2 testsuite eksekverer med alle tests bestående
- [ ] D-3 spike-rapport indeholder eksplicit Go/No-Go med dokumentation for hvert kriterium
- [ ] D-3 sammenligningsoversigt dækker alle 22 P057 FR-Gherkin-scenarier
- [ ] Alle tre diskrepans-hotspots er eksplicit vurderet i D-3
- [ ] Mindst ét hul- eller diskrepans-fund er dokumenteret (eller eksplicit bekræftet som fraværende)
- [ ] Ingen produktionsfiler ændret; ingen migrationer, API-specs eller Java-kilder ændret

---

## Decision Gate

Denne petition afsluttes med en binær beslutning logget i `petitions/program-status.yaml`:

**Hvis Go:** Bestil petition 070 om etablering af en Catala compliance verification pipeline
(CI-integreret, dækkende et bredere sæt G.A.-afsnit). P057 Gherkin-scenarier suppleres med
Catala-genererede cases som autoritative regressionstests.

**Hvis No-Go:** Dokumentér den blokerende årsag i `catala/SPIKE-REPORT-069.md` og luk
udforskningen. Den nuværende petition + Gherkin + specs pipeline forbliver det autoritative
compliance-lag. Ingen yderligere investering i Catala foretages uden ny evidens.
