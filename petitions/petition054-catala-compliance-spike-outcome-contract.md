# Petition 054 Outcome Contract

## Petition reference

**Petition 054:** Catala Compliance Spike — Formalisering af G.A.1.4.3 og G.A.1.4.4
**Type:** Research spike — no production code delivered.
**Legal basis:** G.A.1.4.3, G.A.1.4.4, Gæld.bekendtg. § 7, stk. 1–2, GIL § 18 k
**Time box:** 2 working days.
**References:** Petition 053 (opskrivning og nedskrivning — fuld spec), G.A. snapshot v3.16 (2026-01-30)

> **Spike note:** All deliverables for this petition are **files** — Catala source programs,
> a Catala test suite, and a markdown spike report. There is no runtime behaviour, no new API
> surface, no database migrations, and no portal changes. Acceptance criteria are therefore
> expressed as file-system and content assertions, plus a compilation exit-code check.

---

## Observable outcomes by functional requirement

### FR-1: Catala encoding of G.A.1.4.3 modtagelsestidspunkt rules

**Deliverable**
- File `catala/ga_1_4_3_opskrivning.catala_da` is present in the repository.

**Expected content**
- The file encodes all four sub-rules governing when an opskrivningsfordring is considered
  received:
  - FR-1.1 Default rule: registration in modtagelsessystemet (Gæld.bekendtg. § 7, stk. 1, 3. pkt.)
  - FR-1.2 Høring exception: confirmation/correction registration (§ 7, stk. 1, 4. pkt.)
  - FR-1.3 Annulleret nedskrivning — same system: at the same time as the original fordring
    (§ 7, stk. 1, 5. pkt.)
  - FR-1.4 Annulleret nedskrivning — cross-system: always at modtagelsessystem registration
    (§ 7, stk. 1, 6. pkt.)
- Each sub-rule block is anchored to its G.A.1.4.3 / Gæld.bekendtg. § 7 article citation
  using Catala's article-citation syntax.
- The source language is Danish (`catala_da`).
- All G.A. citations reference G.A. snapshot v3.16 (2026-01-30).

**What "success" looks like**
- A reviewer can map each of the four sub-rules to a corresponding Catala `scope` or `rule`
  block with an identifiable article anchor, without ambiguity or workaround.

---

### FR-2: Catala encoding of G.A.1.4.4 nedskrivning rules

**Deliverable**
- File `catala/ga_1_4_4_nedskrivning.catala_da` is present in the repository.

**Expected content**
- The file encodes:
  - FR-2.1 The three valid nedskrivningsgrunde: Gæld.bekendtg. § 7, stk. 2, nr. 1–3
    (`NED_INDBETALING`, `NED_FEJL_OVERSENDELSE`, `NED_GRUNDLAG_AENDRET`)
  - FR-2.2 Virkningsdato rule: retroactive if `virkningsdato < fordring.receivedAt`
  - FR-2.3 GIL § 18 k suspension flag: `true` when `virkningsdato < fordring.receivedAt`
    and the nedskrivning cannot be completed immediately
  - FR-2.4 Validation: a nedskrivning submitted without a valid grund is rejected
- All G.A.1.4.4 and GIL § 18 k article citations are anchored using Catala's
  article-citation syntax, referencing G.A. snapshot v3.16 (2026-01-30).
- The source language is Danish (`catala_da`).

**What "success" looks like**
- The three grounds, the virkningsdato retroactivity determination, and the GIL § 18 k
  suspension flag each correspond to a distinct, identifiable Catala rule with no ambiguity.

---

### FR-3: Catala test suite

**Deliverable**
- File `catala/tests/ga_opskrivning_nedskrivning_tests.catala_da` is present in the
  repository.

**Expected content**
- At least 8 individual test cases expressed using Catala's built-in `Test` module.
- Coverage includes:
  - All four FR-1 modtagelsestidspunkt sub-rules (FR-1.1 through FR-1.4)
  - All three FR-2.1 nedskrivningsgrunde
  - FR-2.3 GIL § 18 k suspension flag (true and false cases)
  - FR-2.4 invalid grund rejection
- At least one boundary-date test for the retroactivity determination
  (same day as `fordring.receivedAt`, day before, day after).
- All tests execute and pass when the Catala CLI runs the test file.

**What "success" looks like**
- `catala test-doc catala/tests/ga_opskrivning_nedskrivning_tests.catala_da` exits 0
  with all test cases reporting `PASS`.

---

### FR-4: Comparison report against P053 Gherkin scenarios

**Deliverable**
- File `catala/SPIKE-REPORT.md` is present in the repository.

**Expected content**
- A markdown table mapping each P053 FR-1 and FR-2 Gherkin scenario
  (from `petitions/petition053-fordringshaverportal-opskrivning-nedskrivning-fuld-spec.feature`)
  to one of: **Covered**, **Not covered**, or **Discrepancy found**.
- A **Gaps** section listing any rule branches encoded in Catala that are not covered by
  any P053 Gherkin scenario (or explicitly stating "None found" if absent).
- A **Discrepancies** section listing any case where the P053 Gherkin scenario appears to
  contradict the G.A. text as formalized (or explicitly stating "None found").
- An **Effort estimate** section providing a person-day estimate for encoding the full
  G.A. Inddrivelse chapter at the same fidelity level, with rationale.

**What "success" looks like**
- Every P053 FR-1 and FR-2 Gherkin scenario has a row in the comparison table.
- All four sections (Coverage, Gaps, Discrepancies, Effort estimate) are present and
  contain substantive content or an explicit "None found" statement.

---

### FR-5: Go/No-Go recommendation

**Deliverable**
- `catala/SPIKE-REPORT.md` contains an explicit Go/No-Go recommendation section.

**Expected content**
- The recommendation section states either **Go** or **No-Go** unambiguously.
- Evidence is provided for each Go criterion:
  - Whether all 4 modtagelsestidspunkt sub-rules encoded without ambiguity (yes/no + evidence)
  - Whether at least 1 gap or discrepancy was found relative to P053 Gherkin (yes/no + detail)
  - Whether Catala test compilation succeeded without errors (yes/no + command output)
  - Whether OCaml/Python extraction produced runnable code (yes/no + evidence)
- Evidence is provided for each No-Go trigger (if applicable):
  - Whether temporal rules required workarounds (yes/no + detail)
  - Whether legal ambiguities blocked formal encoding (yes/no + G.A. reference)
  - Whether encoding effort per G.A. section exceeded 4 person-days (yes/no + estimate)

**What "success" looks like**
- A reviewer can determine the Go/No-Go outcome and its basis without referring to any
  external document other than the spike report itself.

---

## Acceptance criteria

The following are binary pass/fail checks. Each must pass for the petition to be closed Done.

**AC-1 (FR-1 — file present)**
`catala/ga_1_4_3_opskrivning.catala_da` exists in the repository.

**AC-2 (FR-1 — four sub-rules present)**
The file contains identifiable Catala rule blocks for all four modtagelsestidspunkt cases
(FR-1.1 through FR-1.4), each with a Gæld.bekendtg. § 7 article anchor.

**AC-3 (FR-1 — language)**
The source file uses the Danish Catala dialect (`catala_da`); the language declaration is
present at the top of the file.

**AC-4 (FR-2 — file present)**
`catala/ga_1_4_4_nedskrivning.catala_da` exists in the repository.

**AC-5 (FR-2 — three grounds + virkningsdato + GIL § 18 k)**
The file contains identifiable Catala rule blocks for all three nedskrivningsgrunde,
the virkningsdato retroactivity determination, and the GIL § 18 k suspension flag,
each with the appropriate article anchor.

**AC-6 (FR-3 — test file present)**
`catala/tests/ga_opskrivning_nedskrivning_tests.catala_da` exists in the repository.

**AC-7 (FR-3 — minimum test count)**
The test file contains at least 8 distinct test cases using Catala's `Test` module.

**AC-8 (FR-4 — report present)**
`catala/SPIKE-REPORT.md` exists in the repository.

**AC-9 (FR-4 — comparison table)**
`catala/SPIKE-REPORT.md` contains a table mapping every P053 FR-1 and FR-2 Gherkin
scenario to a coverage status (Covered / Not covered / Discrepancy found).

**AC-10 (FR-4 — all four sections present)**
`catala/SPIKE-REPORT.md` contains all four sections: Coverage table, Gaps, Discrepancies,
and Effort estimate — each with substantive content or an explicit "None found" statement.

**AC-11 (FR-5 — Go/No-Go present)**
`catala/SPIKE-REPORT.md` contains an explicit Go/No-Go section with a clear **Go** or
**No-Go** verdict.

**AC-12 (FR-5 — evidence for each criterion)**
The Go/No-Go section contains evidence (yes/no with rationale) for every criterion listed
in FR-5 (four Go criteria; three No-Go triggers).

**AC-13 (NFR-1 — compilation exits 0)**
Running `catala ocaml ga_1_4_3_opskrivning.catala_da` from the `catala/` directory exits
with code 0. The equivalent command for `ga_1_4_4_nedskrivning.catala_da` also exits 0.

**AC-14 (NFR-2 — Danish dialect)**
Both source files declare the Danish Catala dialect; no English-dialect source files are
produced.

**AC-15 (NFR-3 — G.A. version citations)**
All G.A. article citations in both source files reference the G.A. snapshot v3.16
(dated 2026-01-30), matching the version used in petition 053.

**AC-16 (NFR-4 — no production artefacts modified)**
No Java source files are modified or created by this spike.
No database migration scripts are added.
No OpenAPI/Swagger specification files are modified.
No Spring Boot module configuration files are changed.

---

## Definition of Done

*(Verbatim from Petition 054)*

- [ ] D-1 and D-2 compile without errors (`catala ocaml ga_1_4_3_opskrivning.catala_da` and D-2 equivalent)
- [ ] D-3 test suite executes with all tests passing
- [ ] D-4 spike report contains explicit Go/No-Go with evidence for each criterion
- [ ] D-4 comparison table covers all P053 FR-1 and FR-2 Gherkin scenarios
- [ ] At least one gap or discrepancy finding documented (or explicitly confirmed as absent)
- [ ] No production files modified; no migrations, API specs, or source Java modified

---

## Deliverables

| # | Artefact | Path | Verified by |
|---|----------|------|-------------|
| D-1 | Catala source — opskrivning | `catala/ga_1_4_3_opskrivning.catala_da` | AC-1, AC-2, AC-3, AC-13 |
| D-2 | Catala source — nedskrivning | `catala/ga_1_4_4_nedskrivning.catala_da` | AC-4, AC-5, AC-13, AC-14, AC-15 |
| D-3 | Catala test file | `catala/tests/ga_opskrivning_nedskrivning_tests.catala_da` | AC-6, AC-7 |
| D-4 | Spike report | `catala/SPIKE-REPORT.md` | AC-8, AC-9, AC-10, AC-11, AC-12 |

---

## Failure conditions

- `catala/ga_1_4_3_opskrivning.catala_da` is absent from the repository.
- Any of the four modtagelsestidspunkt sub-rules (FR-1.1–FR-1.4) is missing or lacks an
  article anchor.
- `catala/ga_1_4_4_nedskrivning.catala_da` is absent from the repository.
- Any of the three nedskrivningsgrunde, the virkningsdato rule, or the GIL § 18 k flag is
  absent from D-2 or lacks an article anchor.
- The test file contains fewer than 8 test cases.
- Any test case in D-3 fails when the Catala CLI executes the test file.
- `catala/SPIKE-REPORT.md` is absent from the repository.
- The comparison table omits one or more P053 FR-1 or FR-2 Gherkin scenarios.
- The Go/No-Go section is absent or does not state a clear verdict.
- Evidence is missing for any Go criterion or No-Go trigger in FR-5.
- `catala ocaml ga_1_4_3_opskrivning.catala_da` exits with a non-zero code.
- `catala ocaml ga_1_4_4_nedskrivning.catala_da` exits with a non-zero code.
- Any Java source file, database migration, OpenAPI spec, or Spring Boot module file is
  modified or created as part of this spike.
- G.A. citations reference a snapshot version other than v3.16 (2026-01-30).
