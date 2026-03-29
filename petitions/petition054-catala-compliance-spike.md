# Petition 054: Catala Compliance Spike — Formalisering af G.A.1.4.3 og G.A.1.4.4

## Summary

Conduct a time-boxed (2 working days) research spike to determine whether the open-source
legal programming language [Catala](https://catala-lang.org/) can serve as a formal
compliance specification layer for OpenDebt. The spike encodes the opskrivning and
nedskrivning rules from G.A.1.4.3 and G.A.1.4.4 — the same legal domain as petition 053 —
and produces a Go/No-Go recommendation with evidence.

**Type:** Research spike — no production code is delivered.  
**Time box:** 2 working days.  
**References:** Petition 053 (opskrivning og nedskrivning — fuld spec), G.A.1.4.3, G.A.1.4.4,
gældsinddrivelsesbekendtgørelsens § 7.

---

## Context and Motivation

The juridisk vejledning (G.A. Inddrivelse) defines the legal rules that OpenDebt must implement.
Today these rules are captured as:

1. **Narrative petitions** — human-readable requirements grounded in G.A. references.
2. **Gherkin feature files** — executable scenarios that express expected portal and
   backend behaviour.
3. **Specification documents** — class-level implementation contracts produced by the
   pipeline.

This chain works, but it has a gap: there is no *formal, executable representation of the law
itself*. A legal rule encoded only in natural language and Gherkin can silently diverge from
the G.A. text. Errors in this translation are discovered late — typically in acceptance testing
or by legal review — and are expensive to correct.

**Catala** is a domain-specific language developed by Inria and the French Ministry of Finance
specifically for encoding legislative and regulatory texts as executable programs. It has been
used to formalize the French tax code and UK welfare rules. Its key property is that each rule
block is *anchored to a specific article* of the source legislation, making the encoding
bidirectionally traceable and auditable.

The hypothesis is: if G.A.1.4.3 and G.A.1.4.4 can be encoded in Catala without ambiguity,
then the Catala program can act as an *oracle* for the OpenDebt test suite — generating
authoritative test cases and flagging implementation gaps that Gherkin alone cannot detect.

### Why G.A.1.4.3 and G.A.1.4.4?

These sections were the focus of petition 053, are well-bounded in scope, and contain the
kinds of rules where encoding errors are consequential:

- Temporal rules: exactly when an opskrivningsfordring is "modtaget" (3 distinct cases)
- Exception hierarchies: general rule → høring exception → annulleret nedskrivning exception
  → cross-system exception
- Retroactive consequences: dækning reassignment, GIL § 18 k suspension
- Multiple valid grounds with different legal effects (Gæld.bekendtg. § 7, stk. 2, nr. 1–3)

These are exactly the rule types where logic programming tools provide the most value and
where the risk of misimplementation is highest.

### Domain Terms

| Danish | English | Definition |
|--------|---------|------------|
| Catala | Catala | Open-source DSL for formalizing legislative text; produces OCaml/Python via compilation |
| Formalisering | Formalization | Encoding legal rules in a machine-executable language |
| Orakel | Oracle | An authoritative executable specification used to generate or validate test cases |
| Opskrivningsfordring | Write-up claim | A separate fordring submitted to increase the amount of an existing claim in PSRM |
| Modtagelsestidspunkt | Receipt timestamp | The legally-defined moment at which PSRM considers a claim received |
| Høring | Hearing | A state where PSRM seeks confirmation or correction of submitted claim data |
| Annulleret nedskrivning | Cancelled write-down | A write-down that the creditor reverses; triggers backdated receipt rule |
| Virkningsdato | Effective date | The date from which a write-down takes legal effect |
| Retroaktiv | Retroactive | Effective from a past date, triggering dækning reassignment |

---

## Legal Basis

| Reference | Content relevant to spike |
|-----------|--------------------------|
| G.A.1.4.3 | Opskrivninger af fordringer: definition, PSRM procedure, modtagelsestidspunkt (3 cases), høring rule, annulleret nedskrivning rule, cross-system exception |
| G.A.1.4.4 | Nedskrivninger af fordringer: three valid grounds (§ 7 stk. 2 nr. 1–3), virkningsdato rules, retroactive reassignment obligation, GIL § 18 k suspension |
| Gæld.bekendtg. § 7, stk. 1 | Modtagelsestidspunkt for opskrivningsfordringer (4 sub-rules) |
| Gæld.bekendtg. § 7, stk. 2 | Tre gyldige nedskrivningsgrunde |
| GIL § 18 k | RIM-suspension ved retroaktiv nedskrivning over systemgrænser |

---

## Functional Requirements

### FR-1: Catala encoding of G.A.1.4.3 modtagelsestidspunkt rules

Produce a Catala source file (`catala/ga_1_4_3_opskrivning.catala_da`) that formally encodes
the four sub-rules governing when an opskrivningsfordring is considered received:

- **FR-1.1** Default rule: received when registered in modtagelsessystemet
  (Gæld.bekendtg. § 7, stk. 1, 3. pkt.)
- **FR-1.2** Høring exception: received when confirmation/correction is registered
  (Gæld.bekendtg. § 7, stk. 1, 4. pkt.)
- **FR-1.3** Annulleret nedskrivning — same system: received at the same time as the
  original fordring (Gæld.bekendtg. § 7, stk. 1, 5. pkt.)
- **FR-1.4** Annulleret nedskrivning — cross-system: always received at modtagelsessystem
  registration (Gæld.bekendtg. § 7, stk. 1, 6. pkt.)

Each sub-rule must be anchored to its source article in the Catala source using Catala's
article-citation syntax.

### FR-2: Catala encoding of G.A.1.4.4 nedskrivning rules

Extend the Catala encoding (`catala/ga_1_4_4_nedskrivning.catala_da`) to cover:

- **FR-2.1** The three valid nedskrivningsgrunde (Gæld.bekendtg. § 7, stk. 2, nr. 1–3)
- **FR-2.2** Virkningsdato: retroactive if `virkningsdato < fordring.receivedAt`
- **FR-2.3** GIL § 18 k suspension flag: true when `virkningsdato < fordring.receivedAt`
  and the nedskrivning cannot be completed immediately
- **FR-2.4** Validation: a nedskrivning submitted without a valid grund is rejected

### FR-3: Catala test suite and generated test cases

Produce a Catala test file (`catala/tests/ga_opskrivning_nedskrivning_tests.catala_da`)
with at least one test per sub-rule (minimum 8 tests covering all rule branches in FR-1
and FR-2). Tests must:

- Be expressed using Catala's built-in `Test` module
- Cover all exception branches, including the cross-system case
- Cover boundary dates (same day as fordring.receivedAt, day before, day after)

### FR-4: Comparison report against P053 Gherkin scenarios

Produce a markdown report (`catala/SPIKE-REPORT.md`) comparing:

1. **Coverage**: Which of the P053 Gherkin scenarios (from
   `petitions/petition053-fordringshaverportal-opskrivning-nedskrivning-fuld-spec.feature`)
   are covered by the Catala test suite? Which are not?
2. **Gaps found**: Did the Catala encoding reveal any rule branches not covered by the
   P053 Gherkin scenarios?
3. **Discrepancies**: Did the Catala encoding surface any case where the P053 Gherkin
   scenario appears to contradict the G.A. text?
4. **Effort estimate**: How many person-days would it take to encode the full G.A.
   Inddrivelse chapter in Catala at this fidelity level?

### FR-5: Go/No-Go recommendation

The spike report must include an explicit Go/No-Go recommendation with evidence:

- **Go criteria** (all must be met):
  - All 4 modtagelsestidspunkt sub-rules encode without ambiguity
  - At least 1 gap or discrepancy found relative to P053 Gherkin (demonstrates value)
  - Catala test compilation succeeds without errors
  - OCaml/Python extraction produces runnable code
- **No-Go criteria** (any one triggers No-Go):
  - Temporal rules cannot be expressed without workarounds
  - Legal ambiguities in G.A. text block formal encoding (i.e., the rules are underspecified)
  - Encoding effort per G.A. section exceeds 4 person-days (unsustainable at scale)

---

## Non-Functional Requirements

- **NFR-1:** The Catala source must compile without errors using the Catala CLI
  (`catala ocaml` or `catala python` extraction).
- **NFR-2:** The Catala source must be in Danish (`catala_da`) to match the G.A. source
  language and support bidirectional traceability with the Danish legal text.
- **NFR-3:** All G.A. article citations in the Catala source must reference the same
  G.A. snapshot version used in petition 053 (version 3.16, dated 2026-01-30).
- **NFR-4:** No production code, database migrations, API changes, or Spring Boot modules
  are introduced by this spike.

---

## Deliverables

| # | Artefact | Path | Description |
|---|----------|------|-------------|
| D-1 | Catala source — opskrivning | `catala/ga_1_4_3_opskrivning.catala_da` | FR-1 rules with article anchors |
| D-2 | Catala source — nedskrivning | `catala/ga_1_4_4_nedskrivning.catala_da` | FR-2 rules with article anchors |
| D-3 | Catala test file | `catala/tests/ga_opskrivning_nedskrivning_tests.catala_da` | FR-3 test suite (≥ 8 tests) |
| D-4 | Spike report | `catala/SPIKE-REPORT.md` | FR-4 comparison + FR-5 Go/No-Go recommendation |

---

## Out of Scope

The following are explicitly excluded from this spike:

| Item | Reason |
|------|---------|
| Encoding any G.A. section other than G.A.1.4.3 and G.A.1.4.4 | Time-boxed; scope chosen for tractability and P053 overlap |
| Runtime integration with Spring Boot or OpenDebt services | Spike only; integration is a follow-on petition if Go |
| CI pipeline integration for Catala compilation | Follow-on if Go |
| Full G.A. Inddrivelse chapter encoding | Follow-on multi-petition programme if Go |
| Catala encoding of G.A.2.3.4.4 (interne opskrivninger) | RIM-internal; not fordringshaver-facing |
| Rentegodtgørelse rules (GIL § 18 l) | Tracked separately in TB-039 |
| Retroactive timeline replay implementation (G.A.1.4.4) | Tracked separately in TB-038 |

---

## Definition of Done

- [ ] D-1 and D-2 compile without errors (`catala ocaml ga_1_4_3_opskrivning.catala_da` and D-2 equivalent)
- [ ] D-3 test suite executes with all tests passing
- [ ] D-4 spike report contains explicit Go/No-Go with evidence for each criterion
- [ ] D-4 comparison table covers all P053 FR-1 and FR-2 Gherkin scenarios
- [ ] At least one gap or discrepancy finding documented (or explicitly confirmed as absent)
- [ ] No production files modified; no migrations, API specs, or source Java modified

---

## Decision Gate

This petition ends with a binary decision logged in `petitions/program-status.yaml`:

**If Go:** Commission petition 055 to establish a Catala compliance verification pipeline
(CI-integrated, covering a broader set of G.A. sections). The P053 Gherkin scenarios are
supplemented with Catala-generated cases as authoritative regression tests.

**If No-Go:** Document the blocking reason in `catala/SPIKE-REPORT.md` and close the
exploration. The current petition + Gherkin + specs pipeline remains the authoritative
compliance layer. No further investment in Catala is made without new evidence.
