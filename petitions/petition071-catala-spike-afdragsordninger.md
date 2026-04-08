# Petition 071: Catala Compliance Spike — Afdragsordninger GIL § 11 (companion til P061)

## Summary

Conduct a time-boxed (2 working days) research spike to determine whether the open-source
legal programming language [Catala](https://catala-lang.org/) can formally encode the
afdragsordning calculation rules in GIL § 11 — the same legal domain as petition 061. The
spike produces a Catala source encoding the tabeltræk lookup, månedlig ydelse calculation,
and konkret betalingsevnevurdering trigger conditions, together with a test suite and a
Go/No-Go recommendation with evidence.

**Type:** Research spike — no production code is delivered.  
**Time box:** 2 working days.  
**Depends on:** Petition 061 (afdragsordninger — fuld specifikation).  
**References:** Petition 061 (afdragsordninger), G.A.3.1.1, G.A.3.1.1.1, GIL § 11 stk. 1–2,
stk. 6; GIL § 45; Gæld.bekendtg. chapter 7.  
**G.A. snapshot version:** v3.16 (2026-03-28).

---

## Context and Motivation

The juridisk vejledning (G.A. Inddrivelse) defines the legal rules that OpenDebt must
implement. Today these rules are captured as:

1. **Narrative petitions** — human-readable requirements grounded in G.A. references.
2. **Gherkin feature files** — executable scenarios that express expected portal and backend
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

The hypothesis is: if GIL § 11 stk. 1–2 and stk. 6 can be encoded in Catala without
ambiguity, then the Catala program can act as an *oracle* for the OpenDebt test suite —
generating authoritative test cases and flagging implementation gaps that Gherkin alone cannot
detect.

Petition 054 demonstrated the approach for G.A.1.4.3 and G.A.1.4.4 (opskrivning og
nedskrivning). Petition 071 applies the same methodology to G.A.3.1.1 — the afdragsordning
domain — where the calculation rules are of a different character: statutory arithmetic with
income bracket tables and explicit rounding rules. These are the rule types where off-by-one
errors and rounding divergences occur most frequently in practice.

### Why G.A.3.1.1 (GIL § 11)?

The tabeltræk rules (GIL § 11, stk. 1–2) and the konkret betalingsevnevurdering trigger
(GIL § 11, stk. 6) are **Catala Tier A targets**: they are arithmetically precise,
free of discretionary judgement, anchored to a published statutory table, and repeated many
thousands of times per year. The risk of implementation error is high because:

- Rounding rules are specified imprecisely ("rounded down to nearest 50 or 100 kr") without
  a clearly stated boundary for when the 100 kr unit applies instead of 50 kr.
- Two separate bracket tables coexist (with and without forsørgerpligt) with different
  minimum afdragsprocent values (3% vs 4%), and these are frequently conflated.
- The lavindkomstgrænse threshold is indexed annually (GIL § 45), creating a risk that
  implementations hardcode the current year's value.
- The konkret betalingsevnevurdering trigger condition (at or above lavindkomstgrænse) is the
  same threshold as the tabeltræk rejection condition — a subtle coupling that is easy to miss.

These are exactly the rule types where Catala adds the most value and where the risk of
misimplementation is highest.

### Domain Terms

| Dansk | Engelsk | Definition |
|-------|---------|------------|
| Catala | Catala | Open-source DSL til formalisering af lovtekster; kompilerer til OCaml/Python |
| Formalisering | Formalization | Kodning af retsregler i maskinekserverbart sprog |
| Orakel | Oracle | En autoritativ eksekverbar specifikation, der bruges til at generere eller validere testtilfælde |
| Tabeltræk | Table lookup | GIL § 11, stk. 1: automatisk opslag af afdragsprocent i lovbestemt tabel |
| Nettoindkomst | Annual net income | Årlig indkomst efter skat, grundlag for tabeltræk |
| Afdragsprocent | Repayment percentage | Procentdel af nettoindkomst, der udgør den årlige betalingsevne |
| Lavindkomstgrænse | Low-income threshold | Minimumsindkomstgrænse; under denne er tabeltræk ikke mulig |
| Månedlig ydelse | Monthly instalment | Beløb skyldner betaler månedligt; afrundes NED |
| Forsørgerpligt | Parental support obligation | Juridisk pligt til at forsørge børn; giver mere favorable tabeltræksprocenter |
| Betalingsevnevurdering | Payment-capacity assessment | GIL § 11, stk. 6: individuel vurdering baseret på budgetskema |
| Budgetskema | Budget form | Struktureret skema, debitor indsender til dokumentation af konkret betalingsevne |
| Indeksregulering | Annual index regulation | GIL § 45: indkomstgrænser justeres 1. januar hvert år via SKM-meddelelse |

---

## Legal Basis

| Reference | Indhold relevant for spike |
|-----------|---------------------------|
| G.A.3.1.1 | Afdragsordninger — primært juridisk grundlag og livscyklusoversigt |
| G.A.3.1.1.1 | Tabeltræk (GIL § 11 stk. 1–2) og konkret betalingsevnevurdering (stk. 6) |
| GIL § 11, stk. 1 | Tabeltræk: afdragsprocent baseret på årlig nettoindkomst og forsørgerpligt |
| GIL § 11, stk. 2 | Månedlig ydelse afrundet NED til nærmeste 50 eller 100 kr |
| GIL § 11, stk. 6 | Konkret betalingsevnevurdering: tilgængelig ved/over lavindkomstgrænse; kræver budgetskema |
| GIL § 45 | Indeksregulering: indkomstgrænser opdateres hvert år 1. januar |
| Gæld.bekendtg. chapter 7 | Konkret betalingsevnevurdering beregningsmetodik |

Alle citater refererer til G.A. snapshot v3.16 (2026-03-28).

---

## Functional Requirements

### FR-1: Catala-kodning af GIL § 11, stk. 1 — tabeltræk og lavindkomstgrænse

Produc en Catala-kildefil (`catala/ga_3_1_1_afdragsordninger.catala_da`), der formelt
kodificerer tabeltræksreglerne i GIL § 11, stk. 1:

- **FR-1.1** Lavindkomstgrænse-guard: hvis `nettoindkomst < lavindkomstgrænse`, er tabeltræk
  ikke mulig; resultatet er 0 kr/måned. Grænsen er en parameter (ikke hardkodet).
  (GIL § 11, stk. 1, G.A.3.1.1.1)
- **FR-1.2** Tabeltræk uden forsørgerpligt: opslag i den lovbestemte tabel giver
  `afdragsprocent` fra 4% op til 60% afhængigt af indkomstinterval.
  (GIL § 11, stk. 1)
- **FR-1.3** Tabeltræk med forsørgerpligt: separat tabelkolonne giver lavere
  `afdragsprocent` (mere favorabelt), fra 3% op til 60%.
  (GIL § 11, stk. 1)
- **FR-1.4** Minimum afdragsprocent med forsørgerpligt: hvis tabelopslaget giver < 3%,
  sættes `afdragsprocent` til 3%.
  (GIL § 11, stk. 1)
- **FR-1.5** Minimum afdragsprocent uden forsørgerpligt: hvis tabelopslaget giver < 4%,
  sættes `afdragsprocent` til 4%.
  (GIL § 11, stk. 1)
- **FR-1.6** Indeksparameterisering: lavindkomstgrænse og intervalgrænser er parametre, der
  sættes ud fra det gældende årsregulerede sæt. Ingen grænseværdier er hardkodet i Catala-kilden.
  (GIL § 45)

Hvert regelsæt skal forankres med Catala's artikel-citationssyntaks til den pågældende
paragraf i GIL § 11.

### FR-2: Catala-kodning af GIL § 11, stk. 2 — månedlig ydelse og afrunding

Udvid Catala-kildefilen med afrundingreglerne i GIL § 11, stk. 2:

- **FR-2.1** Årlig betalingsevne: `betalingsevne_år = afdragsprocent × nettoindkomst`.
  (GIL § 11, stk. 1)
- **FR-2.2** Månedlig råydelse (uafrundet): `råydelse_måned = betalingsevne_år / 12`.
  (GIL § 11, stk. 2)
- **FR-2.3** Afrundingsenhed: 50 kr for ydelser under et specificeret grænsebeløb; 100 kr
  for ydelser derover. Grænsebeløbet er en parameter.
  (GIL § 11, stk. 2)
- **FR-2.4** Floor-afrunding: `månedlig_ydelse = floor(råydelse_måned / afrundingsenhed) × afrundingsenhed`.
  Altid afrunding NED — aldrig op eller til nærmeste.
  (GIL § 11, stk. 2)
- **FR-2.5** Referencetilfælde (uden forsørgerpligt): 250.000 kr × 13% / 12 = 2.708,33 kr
  → afrundet NED til 2.700 kr (multiplum af 50).
  (GIL § 11, stk. 2)
- **FR-2.6** Referencetilfælde (med forsørgerpligt): 250.000 kr × 10% / 12 = 2.083,33 kr
  → afrundet NED til 2.050 kr (multiplum af 50).
  (GIL § 11, stk. 2)

### FR-3: Catala-kodning af GIL § 11, stk. 6 — konkret betalingsevnevurdering

Udvid Catala-kildefilen med trigger-betingelserne for konkret betalingsevnevurdering:

- **FR-3.1** Forguard: konkret betalingsevnevurdering er kun tilgængelig, hvis
  `nettoindkomst >= lavindkomstgrænse`. Under grænsen afvises anmodningen.
  (GIL § 11, stk. 6)
- **FR-3.2** Budgetskema-krav: uden indsendt budgetskema er konkret betalingsevnevurdering
  ikke mulig. Catala kodificerer dette som en valideringsbetingelse.
  (Gæld.bekendtg. chapter 7)
- **FR-3.3** Resultatinterval: den konkrete ydelse kan være lavere ELLER højere end
  tabeltrækket; begge tilfælde er gyldige.
  (GIL § 11, stk. 6)
- **FR-3.4** Referencepunkt: tabeltræksydelsen beregnes altid parallelt, så den konkrete
  ydelse kan sammenlignes med den.
  (GIL § 11, stk. 6)

### FR-4: Catala-testsuite og genererede testtilfælde

Produc en Catala-testfil (`catala/tests/ga_afdragsordninger_tests.catala_da`) med mindst
8 testtilfælde udtrykt via Catala's indbyggede `Test`-modul. Testtilfælde skal dække:

- Alle FR-1 under-regler (FR-1.1 til FR-1.6), inklusive lavindkomstgrænse-grænsetilfæld
  (præcis på grænsen, én kr under og én kr over).
- Begge referencetilfælde i FR-2 (med og uden forsørgerpligt; 250.000 kr).
- FR-2.3 afrundingsenhed: testtilfælde for indkomst, der udløser 50 kr-enhed, og testtilfælde
  der udløser 100 kr-enhed.
- FR-2.4 floor-afrunding: testtilfælde, der verificerer, at afrunding altid er ned og aldrig
  halvt-op.
- FR-3.1 konkret betalingsevnevurdering nedenfor lavindkomstgrænsen (afvist) og ved/over (accepteret).
- FR-3.2 budgetskema-krav: testtilfælde uden budgetskema (afvist).
- Minimum 2 grænsetilfælde: lavindkomstgrænse præcis (≥ og <).

### FR-5: Sammenligningstrapport mod P061 Gherkin-scenarier

Produc en markdown-rapport (`catala/SPIKE-REPORT-071.md`), der sammenligner:

1. **Dækning:** Hvilke af de 25 P061 Gherkin-scenarier er dækket af Catala-testsuiten?
   Hvilke er ikke? (Status: **Dækket**, **Ikke dækket**, **Afvigelse fundet**)
2. **Huller:** Afslører Catala-kodningen regel-grene, der ikke er dækket af et P061-scenario?
3. **Afvigelser:** Identificerer Catala-kodningen tilfælde, hvor P061-scenariet tilsyneladende
   modsiger G.A.-teksten?
4. **Indsatsvurdering:** Hvor mange persondag vil det kræve at kodificere det fulde
   G.A. Inddrivelse-kapitel i Catala med samme detaljegrad?

### FR-6: Go/No-Go-anbefaling

Rapporten skal indeholde en eksplicit Go/No-Go-anbefaling med evidens:

- **Go-kriterier (alle skal opfyldes):**
  - Tabeltræk-opslag kodificeres uden tvetydighed
  - Floor-afrunding (stk. 2) er udtrykkelig i Catala
  - Lavindkomstgrænse-guard fungerer som Catala-prædikatsguard
  - Indeksparameterisering er udtrykkelig i Catala (parameter, ikke hardkodet)
  - Mindst 1 afvigelse eller hul identificeret ift. P061 Gherkin
  - Catala-kilde kompilerer uden fejl
- **No-Go-kriterier (ét udløser No-Go):**
  - Indeksparameterisering kan ikke udtrykkes i Catala uden workaround
  - Afrundingstvetydighed i G.A.-teksten (50 kr vs. 100 kr-grænse) blokerer formel kodning
  - Indsats per G.A.-afsnit overstiger 4 persondag

---

## Non-Functional Requirements

- **NFR-1:** Catala-kilden skal kompilere uden fejl ved brug af Catala CLI
  (`catala ocaml` eller `catala python`-ekstraktion).
- **NFR-2:** Catala-kilden skal være på dansk (`catala_da`) for at matche G.A.-kildesprog
  og understøtte tovejssporing med den danske lovtekst.
- **NFR-3:** Alle G.A.-artikelcitater i Catala-kilden skal referere til samme G.A.-snapshot
  som petition 061: version 3.16, dateret 2026-03-28.
- **NFR-4:** Ingen produktionskode, databasemigrationer, API-ændringer eller Spring Boot-moduler
  introduceres af dette spike.

---

## Deliverables

| # | Artefakt | Sti | Beskrivelse |
|---|----------|-----|-------------|
| D-1 | Catala-kilde — afdragsordninger | `catala/ga_3_1_1_afdragsordninger.catala_da` | FR-1, FR-2, FR-3 regler med artikelankre |
| D-2 | Catala-testfil | `catala/tests/ga_afdragsordninger_tests.catala_da` | FR-4 testsuite (≥ 8 tests) |
| D-3 | Spike-rapport | `catala/SPIKE-REPORT-071.md` | FR-5 sammenligning + FR-6 Go/No-Go |

---

## Out of Scope

Følgende er eksplicit ekskluderet fra dette spike:

| Emne | Begrundelse |
|------|-------------|
| Kodning af andre G.A.-afsnit end G.A.3.1.1 | Time-boxed; scope valgt for sporbarhed og P061-overlap |
| Kulanceaftale (GIL § 11, stk. 11) | Diskretionær — ingen formel beregningsregel at kodificere |
| Livscyklus-tilstandsmaskine (OPRETTET → AKTIV → MISLIGHOLT) | Procedurel/workflowlogik, ikke lovbestemt beregning |
| Misligholdelsesdetektion (FR-4 i P061) | Operationel logik, ikke Catala Tier A |
| Virksomhedsafdragsordning (GIL § 11a) | Evidensregler, ikke tabeltræk; separat vurdering |
| Runtime-integration med Spring Boot eller OpenDebt-services | Spike only; integration er en follow-on-petition hvis Go |
| CI-pipeline-integration til Catala-kompilering | Follow-on hvis Go |
| Fuld G.A. Inddrivelse-kapitelkodning | Follow-on multi-petition-program hvis Go |
| Rentegodtgørelse (GIL § 18 l) | Spores separat i TB-039 |

---

## Definition of Done

- [ ] D-1 kompilerer uden fejl (`catala ocaml catala/ga_3_1_1_afdragsordninger.catala_da`)
- [ ] D-2 testsuite eksekverer med alle tests bestået
- [ ] D-3 spike-rapport indeholder eksplicit Go/No-Go med evidens for hvert kriterium
- [ ] D-3 sammenligningstalel dækker alle 25 P061-scenarier
- [ ] Mindst én hul- eller afvigelseskonstatering er dokumenteret (eller eksplicit bekræftet fraværende)
- [ ] Ingen produktionsfiler ændret; ingen migrationer, API-specifikationer eller Java-kilde ændret

---

## Decision Gate

Dette petition afsluttes med en binær afgørelse logget i `petitions/program-status.yaml`:

**Hvis Go:** Bestil petition 072 for at etablere en Catala-komplianceverifikationspipeline
(CI-integreret, dækkende et bredere sæt af G.A. 3.1.x-afsnit). P061 Gherkin-scenarierne
suppleres med Catala-genererede tilfælde som autoritative regressionstest.

**Hvis No-Go:** Dokumenter blokeringsårsagen i `catala/SPIKE-REPORT-071.md` og luk
udforskningen. Den nuværende petition + Gherkin + specs-pipeline forbliver det autoritative
compliance-lag. Ingen yderligere investering i Catala gøres uden ny evidens.
