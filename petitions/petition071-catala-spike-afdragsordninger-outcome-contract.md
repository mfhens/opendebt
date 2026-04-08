# Petition 071 Outcome Contract

## Petition reference

**Petition 071:** Catala Compliance Spike — Afdragsordninger GIL § 11 (companion til P061)  
**Type:** Research spike — no production code delivered.  
**Depends on:** Petition 061 (afdragsordninger — fuld specifikation)  
**Legal basis:** GIL § 11 stk. 1–2, stk. 6; GIL § 45; Gæld.bekendtg. chapter 7; G.A.3.1.1, G.A.3.1.1.1  
**Time box:** 2 working days.  
**References:** Petition 061 (afdragsordninger), G.A. snapshot v3.16 (2026-03-28)

> **Spike note:** All deliverables for this petition are **files** — a Catala source program,
> a Catala test suite, and a markdown spike report. There is no runtime behaviour, no new API
> surface, no database migrations, and no portal changes. Acceptance criteria are therefore
> expressed as file-system and content assertions, plus a compilation exit-code check.

---

## Observable outcomes by functional requirement

### FR-1: Catala-kodning af GIL § 11, stk. 1 — tabeltræk og lavindkomstgrænse

**Deliverable**
- File `catala/ga_3_1_1_afdragsordninger.catala_da` is present in the repository.

**Expected content**
- The file encodes all six sub-rules governing the tabeltræk lookup:
  - FR-1.1 Lavindkomstgrænse-guard: `nettoindkomst < lavindkomstgrænse` → 0 kr; parameterised threshold.
    (GIL § 11, stk. 1; G.A.3.1.1.1)
  - FR-1.2 Tabeltræk uden forsørgerpligt: bracket lookup → `afdragsprocent` 4–60%.
    (GIL § 11, stk. 1)
  - FR-1.3 Tabeltræk med forsørgerpligt: separate bracket column → `afdragsprocent` 3–60%.
    (GIL § 11, stk. 1)
  - FR-1.4 Minimum uden forsørgerpligt: `afdragsprocent ≥ 4%`.
    (GIL § 11, stk. 1)
  - FR-1.5 Minimum med forsørgerpligt: `afdragsprocent ≥ 3%`.
    (GIL § 11, stk. 1)
  - FR-1.6 Indeksparameterisering: lavindkomstgrænse og intervalgrænser er Catala-parametre;
    ingen grænseværdier er hardkodet. (GIL § 45)
- Each sub-rule block is anchored to its GIL § 11 article citation using Catala's article-citation syntax.
- The source language is Danish (`catala_da`).
- All G.A. citations reference G.A. snapshot v3.16 (2026-03-28).

**What "success" looks like**
- A reviewer can map each of the six sub-rules to a corresponding Catala `scope` or `rule`
  block with an identifiable article anchor, without ambiguity or workaround.

---

### FR-2: Catala-kodning af GIL § 11, stk. 2 — månedlig ydelse og afrunding

**Deliverable**
- File `catala/ga_3_1_1_afdragsordninger.catala_da` (same file, extended) encodes the
  monthly instalment and rounding rules.

**Expected content**
- The file encodes:
  - FR-2.1 Årlig betalingsevne: `betalingsevne_år = afdragsprocent × nettoindkomst`
  - FR-2.2 Månedlig råydelse: `råydelse_måned = betalingsevne_år / 12`
  - FR-2.3 Afrundingsenhed: 50 kr below the threshold; 100 kr at or above. Unit is a parameter.
  - FR-2.4 Floor-afrunding: `månedlig_ydelse = floor(råydelse / afrundingsenhed) × afrundingsenhed`
  - FR-2.5 Referencetilfælde uden forsørgerpligt: 250.000 kr × 13% / 12 = 2.708,33 → 2.700 kr
  - FR-2.6 Referencetilfælde med forsørgerpligt: 250.000 kr × 10% / 12 = 2.083,33 → 2.050 kr
- Each rule block is anchored to GIL § 11, stk. 2.
- All G.A. citations reference G.A. snapshot v3.16 (2026-03-28).

**What "success" looks like**
- The floor rounding rule and the rounding unit determination each correspond to a distinct,
  identifiable Catala rule with no ambiguity, and the two reference computations validate
  against the encoded rules.

---

### FR-3: Catala-kodning af GIL § 11, stk. 6 — konkret betalingsevnevurdering trigger

**Deliverable**
- File `catala/ga_3_1_1_afdragsordninger.catala_da` (same file, extended) encodes the
  trigger conditions for konkret betalingsevnevurdering.

**Expected content**
- The file encodes:
  - FR-3.1 Forguard: `nettoindkomst >= lavindkomstgrænse` is required for konkret vurdering
  - FR-3.2 Budgetskema-krav: without budgetskema, the request is rejected (Catala validation rule)
  - FR-3.3 Resultatinterval: both lower-than-tabeltræk and higher-than-tabeltræk outcomes are valid
  - FR-3.4 Referencepunkt: tabeltræk beregnes altid parallelt til sammenligning
- Each rule block is anchored to GIL § 11, stk. 6 and Gæld.bekendtg. chapter 7.

**What "success" looks like**
- The trigger condition, budgetskema guard, and the result comparison each correspond to
  a distinct, identifiable Catala rule. The forguard reuses the same `lavindkomstgrænse`
  parameter as FR-1.1 without duplication.

---

### FR-4: Catala-testsuite

**Deliverable**
- File `catala/tests/ga_afdragsordninger_tests.catala_da` is present in the repository.

**Expected content**
- At least 8 individual test cases expressed using Catala's built-in `Test` module.
- Coverage includes:
  - FR-1.1 Lavindkomstgrænse-guard: nettoindkomst præcis på grænsen (accepteret) og én kr under (afvist)
  - FR-1.4 / FR-1.5 Minimumsprocentregler: med og uden forsørgerpligt
  - FR-2.5 Referencetilfælde uden forsørgerpligt: 250.000 kr → 2.700 kr
  - FR-2.6 Referencetilfælde med forsørgerpligt: 250.000 kr → 2.050 kr
  - FR-2.3 Afrundingsenhed: testtilfælde der verificerer 50 kr-enhed og 100 kr-enhed
  - FR-2.4 Floor-afrunding: verificerer at afrunding aldrig er halvt-op
  - FR-3.1 Konkret vurdering under lavindkomstgrænsen (afvist)
  - FR-3.2 Konkret vurdering uden budgetskema (afvist)
- All tests execute and pass when the Catala CLI runs the test file.

**What "success" looks like**
- `catala test-doc catala/tests/ga_afdragsordninger_tests.catala_da` exits 0
  with all test cases reporting `PASS`.

---

### FR-5: Sammenligningstrapport mod P061 Gherkin-scenarier

**Deliverable**
- File `catala/SPIKE-REPORT-071.md` is present in the repository.

**Expected content**
- A markdown table mapping each of the 25 P061 Gherkin scenarios
  (from `petitions/petition061-afdragsordninger.feature`)
  to one of: **Dækket**, **Ikke dækket**, or **Afvigelse fundet**.
- A **Huller** section listing any rule branches encoded in Catala but not covered by any
  P061 Gherkin scenario (or explicitly stating "Ingen fundet").
- A **Afvigelser** section listing any case where a P061 Gherkin scenario appears to
  contradict the G.A. text as formalized (or explicitly stating "Ingen fundet").
- An **Indsatsvurdering** section providing a person-day estimate for encoding the full
  G.A. Inddrivelse chapter at the same fidelity level, with rationale.

**What "success" looks like**
- Every P061 Gherkin scenario has a row in the comparison table.
- All four sections (Dækning, Huller, Afvigelser, Indsatsvurdering) are present and
  contain substantive content or an explicit "Ingen fundet" statement.

---

### FR-6: Go/No-Go-anbefaling

**Deliverable**
- `catala/SPIKE-REPORT-071.md` contains an explicit Go/No-Go recommendation section.

**Expected content**
- The recommendation section states either **Go** or **No-Go** unambiguously.
- Evidence is provided for each Go criterion:
  - Whether tabeltræk bracket lookup encodes cleanly without ambiguity (ja/nej + evidens)
  - Whether floor rounding (stk. 2) is expressible in Catala (ja/nej + evidens)
  - Whether lavindkomstgrænse guard functions as a Catala predicate guard (ja/nej + evidens)
  - Whether annual index parameterization is expressible as a parameter (ja/nej + evidens)
  - Whether Catala compilation succeeded without errors (ja/nej + kommandooutput)
  - Whether at least 1 gap or discrepancy was found relative to P061 Gherkin (ja/nej + detalje)
- Evidence is provided for each No-Go trigger (if applicable):
  - Whether annual index parameterization required a workaround (ja/nej + detalje)
  - Whether rounding ambiguity in the G.A. text blocked formal encoding (ja/nej + G.A.-reference)
  - Whether encoding effort per G.A. section exceeded 4 person-days (ja/nej + estimat)

**What "success" looks like**
- A reviewer can determine the Go/No-Go outcome and its basis without referring to any
  external document other than the spike report itself.

---

## Acceptance criteria

The following are binary pass/fail checks. Each must pass for the petition to be closed Done.

**AC-1 (FR-1 — fil til stede)**
`catala/ga_3_1_1_afdragsordninger.catala_da` exists in the repository.

**AC-2 (FR-1 — seks under-regler til stede)**
The file contains identifiable Catala rule blocks for all six tabeltræk sub-rules
(FR-1.1 through FR-1.6), each with a GIL § 11 article anchor.

**AC-3 (FR-1 — indeksparameterisering)**
The lavindkomstgrænse threshold and income bracket boundaries appear as Catala parameters,
not as hardcoded literal values. The source file contains no literal year-specific threshold
value (e.g., 138.500 kr) without parameterisation.

**AC-4 (FR-2 — floor-afrunding kodificeret)**
The file contains a Catala rule block implementing floor division for the monthly instalment
calculation, anchored to GIL § 11, stk. 2. The rule uses `floor` (not `round`
or `ceiling`) semantics.

**AC-5 (FR-2 — afrundingsenhed kodificeret)**
The file contains a Catala rule block determining the rounding unit (50 kr or 100 kr) based
on a parameterised threshold, anchored to GIL § 11, stk. 2.

**AC-6 (FR-2 — referencetilfælde kan verificeres)**
Given the 2026 bracket values, evaluating the Catala scope with `nettoindkomst = 250.000`,
`forsørgerpligt = false` yields `månedlig_ydelse = 2.700`. Evaluating with
`forsørgerpligt = true` yields `månedlig_ydelse = 2.050`.

**AC-7 (FR-3 — konkret vurdering trigger til stede)**
The file contains identifiable Catala rule blocks for the FR-3.1 forguard, the FR-3.2
budgetskema validation, and the FR-3.4 tabeltræk reference computation, each with
appropriate article anchors.

**AC-8 (FR-1, FR-2 — dansk dialekt)**
The source file declares the Danish Catala dialect (`catala_da`); the language declaration
is present at the top of the file.

**AC-9 (FR-4 — testfil til stede)**
`catala/tests/ga_afdragsordninger_tests.catala_da` exists in the repository.

**AC-10 (FR-4 — minimum testantal)**
The test file contains at least 8 distinct test cases using Catala's `Test` module.

**AC-11 (FR-4 — grænsetilfælde dækket)**
The test file contains at least one test for `nettoindkomst` exactly equal to
`lavindkomstgrænse` (accepted) and at least one test for `nettoindkomst` one unit below
`lavindkomstgrænse` (rejected).

**AC-12 (FR-5 — rapport til stede)**
`catala/SPIKE-REPORT-071.md` exists in the repository.

**AC-13 (FR-5 — sammenligningstalel)**
`catala/SPIKE-REPORT-071.md` contains a table mapping every P061 Gherkin scenario
(alle 25 i `petitions/petition061-afdragsordninger.feature`) to a coverage status
(Dækket / Ikke dækket / Afvigelse fundet).

**AC-14 (FR-5 — alle fire sektioner til stede)**
`catala/SPIKE-REPORT-071.md` contains all four sections: Dækning, Huller, Afvigelser,
and Indsatsvurdering — each with substantive content or an explicit "Ingen fundet" statement.

**AC-15 (FR-6 — Go/No-Go til stede)**
`catala/SPIKE-REPORT-071.md` contains an explicit Go/No-Go section with a clear
**Go** or **No-Go** verdict.

**AC-16 (FR-6 — evidens for hvert kriterium)**
The Go/No-Go section contains evidence (ja/nej with rationale) for every criterion listed
in FR-6 (six Go criteria; three No-Go triggers).

**AC-17 (NFR-1 — kompilering afslutter 0)**
Running `catala ocaml ga_3_1_1_afdragsordninger.catala_da` from the `catala/` directory
exits with code 0.

**AC-18 (NFR-2 — dansk dialekt)**
The source file declares the Danish Catala dialect; no English-dialect source files are
produced by this spike.

**AC-19 (NFR-3 — G.A.-versionscitater)**
All G.A. article citations in the source file reference the G.A. snapshot v3.16
(dated 2026-03-28), matching the version used in petition 061.

**AC-20 (NFR-4 — ingen produktionsartefakter ændret)**
No Java source files are modified or created by this spike.
No database migration scripts are added.
No OpenAPI/Swagger specification files are modified.
No Spring Boot module configuration files are changed.

---

## Definition of Done

*(Verbatim fra Petition 071)*

- [ ] D-1 kompilerer uden fejl (`catala ocaml catala/ga_3_1_1_afdragsordninger.catala_da`)
- [ ] D-2 testsuite eksekverer med alle tests bestået
- [ ] D-3 spike-rapport indeholder eksplicit Go/No-Go med evidens for hvert kriterium
- [ ] D-3 sammenligningstatabel dækker alle 25 P061 Gherkin-scenarier
- [ ] Mindst én hul- eller afvigelseskonstatering er dokumenteret (eller eksplicit bekræftet fraværende)
- [ ] Ingen produktionsfiler ændret; ingen migrationer, API-specifikationer eller Java-kilde ændret

---

## Deliverables

| # | Artefakt | Sti | Verificeret ved |
|---|----------|-----|-----------------|
| D-1 | Catala-kilde — afdragsordninger | `catala/ga_3_1_1_afdragsordninger.catala_da` | AC-1, AC-2, AC-3, AC-4, AC-5, AC-6, AC-7, AC-8, AC-17, AC-18, AC-19 |
| D-2 | Catala-testfil | `catala/tests/ga_afdragsordninger_tests.catala_da` | AC-9, AC-10, AC-11 |
| D-3 | Spike-rapport | `catala/SPIKE-REPORT-071.md` | AC-12, AC-13, AC-14, AC-15, AC-16 |

---

## Failure conditions

- `catala/ga_3_1_1_afdragsordninger.catala_da` is absent from the repository.
- Any of the six FR-1 sub-rules (FR-1.1–FR-1.6) is missing or lacks a GIL § 11 article anchor.
- Lavindkomstgrænse threshold appears as a hardcoded literal value (not a parameter).
- The monthly ydelse rounding rule does not implement floor semantics.
- The rounding unit determination is absent or lacks a parameterised boundary.
- Given the 2026 bracket values, the Catala scope does not yield 2.700 kr for 250.000 kr
  without forsørgerpligt, or does not yield 2.050 kr with forsørgerpligt.
- `catala/tests/ga_afdragsordninger_tests.catala_da` is absent from the repository.
- The test file contains fewer than 8 test cases.
- No boundary test for `nettoindkomst` exactly at `lavindkomstgrænse` is present.
- Any test case in D-2 fails when the Catala CLI executes the test file.
- `catala/SPIKE-REPORT-071.md` is absent from the repository.
- The comparison table omits one or more P061 Gherkin scenarios.
- The Go/No-Go section is absent or does not state a clear verdict.
- Evidence is missing for any Go criterion or No-Go trigger in FR-6.
- `catala ocaml ga_3_1_1_afdragsordninger.catala_da` exits with a non-zero code.
- Any Java source file, database migration, OpenAPI spec, or Spring Boot module file is
  modified or created as part of this spike.
- G.A. citations reference a snapshot version other than v3.16 (2026-03-28).
