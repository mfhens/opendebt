# Petition 070: Catala Compliance Spike — Forældelse G.A.2.4 (companion to P059)

## Summary

Conduct a time-boxed (**3 working days**) research spike to determine whether the open-source
legal programming language [Catala](https://catala-lang.org/) can formally encode the complete
forældelses-ruleset from G.A.2.4 — the same legal domain as petition 059. The spike produces
a Catala source file, a Catala test suite, and a Go/No-Go spike report comparing coverage
against all 29 Gherkin scenarios in P059.

> **Timebox note:** This spike is allocated **3 working days** — one day more than the
> standard 2-day Catala spike (see petition 054). The additional day is justified by the
> substantially higher legal complexity of G.A.2.4 relative to G.A.1.4.3/1.4.4:
> (1) four interlocking legal concepts (base frist, udskydelse, afbrydelse, tillægsfrister)
> must all be encoded; (2) the fordringskompleks propagation rule (GIL § 18a, stk. 2) requires
> relational reasoning that is absent from the opskrivning/nedskrivning domain; and (3) the
> varsel/afgørelse distinction (SKM2015.718.ØLR) and udskydelse immutability are known
> implementation complexity hotspots without obvious analogues in G.A.1.4.
>
> **Priority: HIGHEST.** This spike shall be commissioned as soon as petition 059 is approved.

**Type:** Research spike — no production code is delivered.  
**Time box:** 3 working days.  
**Priority:** HIGHEST — commission immediately upon P059 approval.  
**G.A. snapshot:** v3.16 (2026-03-28).  
**Companion petition:** Petition 059 (Forældelse — prescription rules).  
**depends_on:** Petition 059.  
**References:** Petition 059 (forældelse), G.A.2.4.1–G.A.2.4.4.2, GIL §§ 18, 18a,
Forældelsesl. §§ 3, 5, 18–19, SKM2015.718.ØLR.

---

## Context and Motivation

The juridisk vejledning (G.A. Inddrivelse) defines the forældelses-ruleset that PSRM must
implement. Petition 059 captures these rules as narrative requirements and 29 Gherkin
scenarios. However, the same gap that motivated petition 054 for the opskrivning/nedskrivning
domain applies here with greater force: the G.A.2.4 forældelses-ruleset is substantially more
legally complex, involves deeply interlocking temporal rules, and has a documented history of
implementation errors in predecessor systems (EFI/DMI).

There is no formal, executable representation of the law itself for the forældelses domain.
Rules encoded only in natural language and Gherkin can silently diverge from the G.A. text.
Errors in this translation are discovered late — typically in acceptance testing or legal review
— and are materially expensive to correct given the legal consequences: continuation of
inddrivelse on a forældet fordring constitutes unlawful collection activity and exposes
Gældsstyrelsen to administrative fines, debtor compensation claims, and reputational damage.

**Catala** is a domain-specific language developed by Inria and the French Ministry of Finance
for encoding legislative and regulatory texts as executable programs. Each rule block is
*anchored to a specific article*, making the encoding bidirectionally traceable and auditable.

The hypothesis is: if the G.A.2.4 forældelses-ruleset can be encoded in Catala without
ambiguity, the Catala program can act as an *oracle* for the OpenDebt test suite — generating
authoritative test cases and flagging implementation gaps that the 29 P059 Gherkin scenarios
alone cannot detect. This is particularly valuable given the five known discrepancy hotspots
identified below.

### Why G.A.2.4 is the highest-priority Catala spike

| Complexity factor | G.A.1.4.3/1.4.4 (P054) | G.A.2.4 (P070) |
|---|---|---|
| Interlocking temporal concepts | 2 (modtagelse, virkningsdato) | 4 (base frist, udskydelse, afbrydelse, tillægsfrist) |
| Relational rule (propagation) | None | Fordringskompleks atomicity (GIL § 18a, stk. 2) |
| Court precedent overriding statutory text | None | SKM2015.718.ØLR (varsel ≠ afgørelse) |
| Known implementation errors in predecessor systems | Moderate | High (EFI/DMI forældelses tracking failures) |
| Companion Gherkin scenarios | ~12 | 29 |
| Estimated Catala rule blocks | 6–8 | 14–18 |

### Known discrepancy hotspots

The following five hotspots have the highest probability of revealing gaps or discrepancies
between the G.A.2.4 text and the P059 Gherkin scenarios. They are the primary value targets
for this spike:

1. **Varsel vs afgørelse (SKM2015.718.ØLR):** Many predecessor systems treated lønindeholdelses-
   varsel as afbrydelse. Only the confirmed afgørelse (at the moment underretning reaches the
   debtor) constitutes valid afbrydelse under GIL § 18, stk. 4 and the 2015 High Court ruling.
   Catala encoding makes this distinction formally testable.

2. **Fordringskompleks propagation atomicity (GIL § 18a, stk. 2):** Afbrydelse of one member
   propagates to ALL members. Partial propagation (some updated, some not) is a failure
   condition, not a partial success. Many implementations relax this constraint under load.
   Catala's functional model enforces all-or-nothing semantics explicitly.

3. **Udskydelse immutability:** The statutory floor dates (20-11-2021 for PSRM, 20-11-2027
   for DMI/SAP38) are not afbrydelse events — they are one-time postponements of the
   forældelsesfrist starting point. They cannot be reset or overridden by any subsequent event,
   including afbrydelse events that would otherwise shorten the frist. This distinction is
   frequently collapsed in implementations.

4. **Forgæves udlæg equals successful udlæg (Forældelsesl. § 18, stk. 1):** An unsuccessful
   levy resulting in an insolvenserklæring has the same afbrydelse effect as a successful udlæg.
   Implementations often treat forgæves udlæg as insufficient afbrydelse, which is legally wrong.

5. **Tillægsfrist formula: max(currentFristExpires, eventDate) + 2 years:** The tillægsfrist
   is computed from the maximum of the current frist expiry and the event date, then extended
   by 2 years. A simpler (incorrect) formula `eventDate + 2 years` is commonly implemented,
   which can produce a shorter effective frist than the pre-extension frist.

### Domain Terms

| Dansk | Engelsk | Definition |
|-------|---------|------------|
| Catala | Catala | Open-source DSL til formalisering af lovtekster; producerer OCaml/Python via kompilering |
| Formalisering | Formalization | Indkodning af juridiske regler i et maskineksekvérbart sprog |
| Orakel | Oracle | En autoritativ eksekverbar specifikation, der bruges til at generere eller validere testcases |
| Forældelsesfrist | Prescription period | Det tidsrum, efter hvis udløb en fordring forældes, hvis ingen afbrydelse er sket |
| Afbrydelse | Interruption | En inddrivelseshændelse, der nulstiller forældelsesfristuret og starter en ny periode |
| Udskydelse | Postponement | En lovbestemt regel, der udskyder begyndelsestidspunktet for forældelsesfristens løb |
| Tillægsfrist | Additional period | En 2-årig forlængelse af forældelsesfristens løb, der udløses af en intern opskrivning |
| Fordringskompleks | Claim complex | En gruppe af relaterede fordringer, for hvilke en afbrydelseshændelse for ét medlem propagerer til alle |
| Berostillelse | Suspension of collection | En administrativ afgørelse om at sætte inddrivelse af en fordring i bero; afbryder forældelsesfrist |
| Forgæves udlæg | Unsuccessful attachment | Et udlægsforsøg, der resulterer i insolvenserklæring; har samme afbrydelsesvirkning som vellykket udlæg |
| Varsel | Notice of intent | Forudgående underretning om påtænkt lønindeholdelse; afbryder IKKE forældelsesfrist alene |
| Afgørelse | Decision | Den formelle beslutning om lønindeholdelse, der — når underretning når debitor — afbryder forældelsesfrist |
| Underretning | Notification | Den formelle handling, der meddeler debitor om en inddrivelsesafgørelse |
| Særligt retsgrundlag | Special legal basis | Dom eller forlig (forældelsesl. § 5, stk. 1), der ved udlæg giver ny 10-årig frist |

---

## Legal Basis

| Reference | Indhold relevant for spike |
|-----------|--------------------------|
| G.A.2.4 (v3.16, 2026-03-28) | Komplet forældelses-ruleset for fordringer under inddrivelse i PSRM og DMI |
| G.A.2.4.1 | Udskydelse — GIL § 18a, stk. 1 startdatoregler for PSRM- og DMI/SAP38-fordringer |
| G.A.2.4.2 | Fordringskomplekser — definition, medlemsregler, afbrydelsespropagation |
| G.A.2.4.3 | 3-årig forældelsesfrist for alle PSRM/DMI-fordringer; 10-årig undtagelse ved udlæg + særligt retsgrundlag |
| G.A.2.4.4.1 | Egentlig afbrydelse — tre event-typer: berostillelse, lønindeholdelse, udlæg |
| G.A.2.4.4.2 | Tillægsfrister — 2-årig forlængelse udløst af interne opskrivninger og modtagelse til inddrivelse |
| GIL § 18, stk. 4 | Lønindeholdelse afbryder forældelsesfrist, når underretning om afgørelse når debitor |
| GIL § 18a, stk. 1 | Udskydelse — lovbestemt udskydelse af forældelsesfristens begyndelsestidspunkt |
| GIL § 18a, stk. 2 | Fordringskomplekser — grupperingsregel og afbrydelsespropagation på tværs af medlemmer |
| GIL § 18a, stk. 4 | 3-årig PSRM/DMI-inddrivelsesfrist — gælder selv ved særligt retsgrundlag |
| GIL § 18a, stk. 7 | Foreløbig afbrydelse ved modtagelse til inddrivelse af tomt fordringskompleks |
| GIL § 18a, stk. 8 | Afgørelse om berostillelse afbryder forældelsesfrist (kun PSRM) |
| Forældelsesl. § 3, stk. 1 | 3-årig ordinær forældelsesfrist |
| Forældelsesl. § 5, stk. 1 | 10-årig frist ved dom eller forlig (særligt retsgrundlag) |
| Forældelsesl. § 18, stk. 1 | Udlæg afbryder forældelsesfrist — gælder også forgæves udlæg |
| Forældelsesl. § 18, stk. 4 | Modregning afbryder forældelsesfrist |
| SKM2015.718.ØLR | Landsretsdom: afbrydelse ved lønindeholdelse kræver faktisk afgørelse, ikke blot varsel |

---

## Functional Requirements

### FR-1: Catala-indkodning af G.A.2.4.3 og G.A.2.4.1 — forældelsesfrist og udskydelse

Produce a Catala source file (`catala/ga_2_4_foraeldelse.catala_da`) that formally encodes
the G.A.2.4.3 base frist rules and the G.A.2.4.1 udskydelse rules:

- **FR-1.1** Base frist — 3 år: For alle fordringer under inddrivelse i PSRM og DMI er
  forældelsesfristens løbetid 3 år, uanset retsgrundlag (GIL § 18a, stk. 4).
- **FR-1.2** Base frist — 10 år: Undtagelse — udlæg på fordring med særligt retsgrundlag
  (dom/forlig efter Forældelsesl. § 5, stk. 1) giver ny 10-årig frist (G.A.2.4.3).
- **FR-1.3** PSRM-udskydelse: Fordringer under inddrivelse fra 19-11-2015 — forældelsesfristens
  begyndelsestidspunkt er tidligst 20-11-2021 (GIL § 18a, stk. 1, 1. pkt.).
- **FR-1.4** DMI/SAP38-udskydelse: Fordringer registreret i andet system end PSRM pr. 1-1-2024
  eller herefter — forældelsesfristens begyndelsestidspunkt er tidligst 20-11-2027
  (GIL § 18a, stk. 1, 2. pkt.).
- **FR-1.5** Udskydelse er uforanderlig: Udskydelsesdatoen fastsættes én gang ved fordringens
  modtagelse og kan ikke flyttes af efterfølgende hændelser, herunder afbrydelse.
  Afbrydelse har ingen virkning på udskydelsesdatoen (G.A.2.4.1).

Each sub-rule must be anchored to its source article in the Catala source using Catala's
article-citation syntax, with reference to G.A. v3.16 (2026-03-28).

### FR-2: Catala-indkodning af G.A.2.4.4.1 — afbrydelse

Extend the Catala source file to encode the three afbrydelse event types that reset the
forældelsesfrist under G.A.2.4.4.1:

- **FR-2.1** Berostillelse (GIL § 18a, stk. 8):
  - Afbrydelse sker ved afgørelses- og registreringsdato for afgørelse om berostillelse.
  - Ny frist: `eventDate + 3 år`.
  - Gælder kun PSRM-forvaltede fordringer — ikke SAP38/DMI.

- **FR-2.2** Lønindeholdelse (GIL § 18, stk. 4 + SKM2015.718.ØLR):
  - Afbrydelse kræver faktisk afgørelse om lønindeholdelse, der er kommet debitor til
    kundskab (underretning). Varsel alene er ikke tilstrækkeligt (SKM2015.718.ØLR).
  - Catala-indkodningen skal eksplicit afvise en afbrydelse, der alene er baseret på varsel.
  - Afgørelsen skal angive fordringens art og størrelse.
  - Lønindeholdelse i bero i 1 år medfører, at ny 3-årig forældelsesfrist begynder fra
    berostillelsestidspunktet.
  - Ny frist (ved aktiv lønindeholdelse): `underretningsDato + 3 år`.

- **FR-2.3** Udlæg (Forældelsesl. § 18, stk. 1):
  - Forgæves udlæg (insolvenserklæring) har **samme** afbrydelsesvirkning som vellykket udlæg.
    Catala-indkodningen må ikke skelne mellem disse i afbrydelsesvirkningen.
  - Ny frist (uden særligt retsgrundlag): `eventDate + 3 år`.
  - Ny frist (med særligt retsgrundlag — dom/forlig): `eventDate + 10 år`.

Each sub-rule must be anchored to its source article, citing both GIL/Forældelsesl. and
G.A.2.4.4.1 where applicable. SKM2015.718.ØLR must be cited in the lønindeholdelse block.

### FR-3: Catala-indkodning af G.A.2.4.2 — fordringskompleks-propagation

Extend the Catala source file to encode the fordringskompleks rules (GIL § 18a, stk. 2):

- **FR-3.1** Fordringskompleks-definition: Et hovedkrav og dets tilhørende renter under
  inddrivelse udgør et fordringskompleks med fælles forældelsesdato.
- **FR-3.2** Propagation: Afbrydelse af forældelsen for én fordring i komplekset afbryder
  forældelsen for alle fordringer i komplekset (GIL § 18a, stk. 2, 4. pkt.).
- **FR-3.3** Atomicitet: Propagationen er altomfattende — partiel propagation (hvor kun
  nogle medlemmer opdateres) er en fejlbetingelse, ikke en delvis succes. Catala-indkodningen
  skal udtrykke denne atomicitetsforpligtelse eksplicit.

### FR-4: Catala-indkodning af G.A.2.4.4.2 — tillægsfrister

Extend the Catala source file to encode the tillægsfrister rules:

- **FR-4.1** Intern opskrivning (G.A.2.4.4.2.1): En intern opskrivning tilføjer en 2-årig
  tillægsfrist til den løbende forældelsesfrist.
- **FR-4.2** Modtagelse til inddrivelse (G.A.2.4.4.2.2): Modtagelse til inddrivelse for
  et tomt fordringskompleks udløser foreløbig afbrydelse (GIL § 18a, stk. 7) og ny 3-årig
  frist fra modtagelsestidspunktet.
- **FR-4.3** Tillægsfrist-formel:
  ```
  nyFristUdløber = max(currentFristExpires, eventDate) + 2 år
  ```
  En tillægsfrist kan ikke udløbe før den basisfrist, der gjaldt uden tillægsfristen.
- **FR-4.4** Udskydelsesgrænsen overtrumfer: Selv en tillægsfrist kan ikke sætte
  forældelsesfristens begyndelsestidspunkt tidligere end udskydelsesdatoen.

### FR-5: Catala-testsuite og genererede testcases

Produce a Catala test file (`catala/tests/ga_foraeldelse_tests.catala_da`) with at least
**10 tests** covering all rule branches in FR-1 through FR-4. Tests must:

- Være udtrykt ved hjælp af Catala's indbyggede `Test`-modul.
- Dække alle varianter af FR-1 (base frist: 3 år og 10 år, PSRM-udskydelse, DMI-udskydelse,
  udskydelsesimmutabilitet).
- Dække alle tre afbrydelsestyper i FR-2, inklusive den negative case: varsel alene
  afbryder ikke (SKM2015.718.ØLR).
- Dække forgæves udlæg-ligestilling (FR-2.3).
- Dække fordringskompleks-propagation (FR-3.2) med mindst to kompleksmedlemmer.
- Dække tillægsfrist-formlen (FR-4.3) med begge forgreninger af max()-udtrykket.
- Inkludere grænseværditests for udskydelsesdatoen (dagen før, på datoen, dagen efter).

The minimum of 10 tests reflects the broader scope (4 rule clusters, 29 companion Gherkin
scenarios) compared to petition 054's minimum of 8 tests for a 2-day timebox.

### FR-6: Sammenligningsrapport mod P059 Gherkin-scenarier (29 stk.)

Produce a markdown report (`catala/SPIKE-REPORT-070.md`) comparing:

1. **Dækning:** Hvilke af P059's 29 Gherkin-scenarier (fra
   `petitions/petition059-foraeldelse.feature`) er dækket af Catala-testsuiten?
   Status for hvert scenarie: **Dækket**, **Ikke dækket** eller **Uoverensstemmelse fundet**.
2. **Huller:** Regelgrene indkodet i Catala, der ikke er dækket af noget P059-scenarie
   (eller eksplicit bekræftelse: "Ingen fundet").
3. **Uoverensstemmelser:** Tilfælde, hvor et P059 Gherkin-scenarie ser ud til at modsige
   G.A.-teksten som formaliseret (eller eksplicit bekræftelse: "Ingen fundet"). Rapporten
   skal eksplicit adressere de fem discrepancy hotspots listet i Context and Motivation.
4. **Indsatsskøn:** Estimat i person-dage for indkodning af det komplette G.A.2.4-kapitel
   i Catala med samme detaljeringsniveau, med begrundelse.

### FR-7: Go/Nej-Go-anbefaling

Spike-rapporten skal indeholde en eksplicit Go/Nej-Go-anbefaling med evidens:

- **Go-kriterier** (alle skal være opfyldt):
  - Alle tre afbrydelsestyper indkodes uden tvetydighed.
  - Varsel/afgørelse-distinktionen (SKM2015.718.ØLR) indkodes rent i Catala.
  - Fordringskompleks-propagation er udtrykkelig i Catala uden workarounds.
  - Catala-testkompilering lykkes uden fejl.
  - Mindst én uoverensstemmelse eller ét hul fundet relativt til P059 Gherkin (påviser
    merværdi af Catala-laget).

- **Nej-Go-kriterier** (ét er nok til Nej-Go):
  - Temporale regler kan ikke udtrykkes uden workarounds i Catala.
  - Fordringskompleks-propagation kræver workarounds uden for Catala's funktionelle model.
  - Indkodningsindsats per G.A.-afsnit overstiger 4 person-dage (uholdbart i skala).

---

## Non-Functional Requirements

- **NFR-1:** Catala-kildefilen skal kompilere uden fejl ved kørsel af Catala CLI
  (`catala ocaml` eller `catala python`-ekstraktion).
- **NFR-2:** Catala-kilden skal være på dansk (`catala_da`) for at matche kildesproget i G.A.
  og understøtte tovejssporbarhed med den danske lovtekst.
- **NFR-3:** Alle G.A.-artikelcitater i Catala-kilden skal referere til G.A.-snapshot-versionen
  v3.16 (2026-03-28) — samme version som bruges i petition 059.
- **NFR-4:** Ingen produktionskode, databasemigreringer, API-ændringer eller Spring Boot-moduler
  introduceres af dette spike.

---

## Deliverables

| # | Artefakt | Sti | Beskrivelse |
|---|----------|-----|-------------|
| D-1 | Catala-kildefil — forældelse | `catala/ga_2_4_foraeldelse.catala_da` | FR-1 til FR-4 regler med artikelankre |
| D-2 | Catala-testfil | `catala/tests/ga_foraeldelse_tests.catala_da` | FR-5 testsuite (≥ 10 tests) |
| D-3 | Spike-rapport | `catala/SPIKE-REPORT-070.md` | FR-6 sammenligning + FR-7 Go/Nej-Go-anbefaling |

---

## Out of Scope

Følgende er eksplicit udelukket fra dette spike:

| Emne | Begrundelse |
|------|-------------|
| Indkodning af andre G.A.-afsnit end G.A.2.4.1–G.A.2.4.4.2 | Tidsafgrænset; scope valgt for P059-overlap |
| G.A.2.4.5 (forældelse af strafbare forhold og forvandlingsstraf) | Kræver retsintegrationslogik; udelukket i P059 |
| G.A.2.4.6 (indsigelse over forældelse) | Caseworker-workflow; ingen Catala-modelleringsværdi for juridisk logik |
| Internationale forældelsesregler | Udenfor PSRM's primære scope |
| Runtime-integration med Spring Boot eller OpenDebt-tjenester | Spike only; integration er en opfølgningspetition ved Go |
| CI-pipeline-integration til Catala-kompilering | Opfølgning ved Go |
| Komplet G.A. Inddrivelse-kapitel-indkodning | Opfølgning fler-petition-program ved Go |
| Indsigelse- og caseworker-workflow (FR-6, FR-7 i P059) | Procedurel logik; ingen Catala-indkodningsværdi |
| Automatisk bortfald-trigger (P065) | Spores separat i petition 065 |
| Rentegodtgørelse (GIL § 18 l) | Spores separat i TB-039 |

---

## Definition of Done

- [ ] D-1 kompilerer uden fejl (`catala ocaml ga_2_4_foraeldelse.catala_da`)
- [ ] D-2 testsuite kører med alle tests bestående
- [ ] D-3 spike-rapport indeholder eksplicit Go/Nej-Go med evidens for hvert kriterium
- [ ] D-3 sammenligningstabell dækker alle 29 P059-scenarier
- [ ] Alle fem discrepancy hotspots er eksplicit adresseret i D-3
- [ ] Mindst én hul- eller uoverensstemmelse-finding dokumenteret
  (eller eksplicit bekræftet som fraværende)
- [ ] Ingen produktionsfiler modificerede; ingen migreringer, API-specs eller Java-kilde ændret

---

## Decision Gate

Dette spike afsluttes med en binær beslutning logget i `petitions/program-status.yaml`:

**Ved Go:** Igangsæt opfølgningspetition for at etablere en Catala-overensstemmelsesverifikationspipeline
(CI-integreret, dækkende et bredere sæt af G.A.-afsnit). P059 Gherkin-scenarierne suppleres med
Catala-genererede cases som autoritative regressionstests. G.A.2.4 Catala-oraklet integreres med
petition 062 (lønindeholdelse fuld spec) og petition 065 (bortfald og afskrivning).

**Ved Nej-Go:** Dokumenter den blokerende årsag i `catala/SPIKE-REPORT-070.md` og afslut
udforskningen. Det nuværende petition + Gherkin + specs-pipeline forbliver det autoritative
overensstemmelseslag. Ingen yderligere investering i Catala foretages uden ny evidens.
