# ADR 0032: Catala as the Formal Compliance Verification Layer for Juridisk Vejledning

## Status
Accepted — amended (2026-04-02: Catala promoted from spike-on-demand to first-class pipeline stage)

## Context

OpenDebt implements Danish debt collection law as defined in:
- *Lov om inddrivelse af gæld til det offentlige* (LIG/GIL)
- *Gældsinddrivelesbekendtgørelsen* (Gæld.bekendtg.)
- *Forældelsesloven*
- The Danish Tax Agency's *Juridisk vejledning, afsnit G.A. Inddrivelse* (authoritative interpretation)

The current compliance chain is:

```
G.A. juridisk vejledning (natural language)
        ↓
Petition (narrative requirements, legal citations)
        ↓
Gherkin feature file (executable scenarios)
        ↓
Implementation specs
        ↓
Java / Spring Boot production code
```

This chain works but has a structural gap: **no layer in the pipeline formally verifies that the legal rules have been encoded correctly before they reach production code**. Errors in the translation from G.A. natural language to Gherkin scenarios can be:

- Silent — existing Gherkin scenarios pass even though a legal rule branch is missing
- Late — discovered during acceptance testing or legal review, which are expensive correction points
- Invisible to non-technical legal experts — Gherkin is readable but not formally verifiable

The rules most at risk are those with complex temporal semantics, exception hierarchies, and numerical calculations — precisely the rules covered by G.A.2 and G.A.3:

| Domain | Risk type | Examples |
|--------|-----------|---------|
| Dækningsrækkefølge (GIL § 4) | Priority ordering, 6-tier interest sequence | Incorrect tier ordering silently allocates payments to wrong component |
| Forældelse (GIL § 18a) | Temporal rules, afbrydelse semantics | Varsel treated as afbrydelse (refuted by SKM2015.718.ØLR) |
| Afdragsordninger (GIL § 11) | Numerical calculation, rounding | Floor rounding replaced with round-half-up |
| Lønindeholdelse (Gæld.bekendtg. § 14) | Formula encoding, fixed-point arithmetic | Division-by-zero guard missing for frikort case |

**Catala** is a domain-specific programming language developed by Inria and the French Ministry of Finance specifically for encoding legislative and regulatory texts as executable, formally-verifiable programs. It has been used to formalize the French income tax code and UK welfare legislation (Merigoux et al., 2021; DOI: [10.1007/s10506-022-09328-5](https://dx.doi.org/10.1007/s10506-022-09328-5)).

Key properties relevant to OpenDebt:

| Property | Relevance |
|----------|-----------|
| **Article-anchored rule blocks** | Each Catala rule is anchored to the specific G.A. or statutory article it encodes — bidirectional traceability |
| **Executable specification** | Catala compiles to OCaml or Python; the encoding is runnable, not just readable |
| **Literate programming style** | Legal text and code interleaved — a lawyer can read the source alongside the rule |
| **Formal type system** | Enumeration types, date arithmetic, and monetary amounts are first-class citizens — the same types that appear in GIL and Gæld.bekendtg. |
| **Test module** | Built-in `Test` module for asserting rule outputs against concrete inputs |
| **Danish language support** | `.catala_da` files allow the encoding to be in Danish, matching the G.A. source language |

The P054 Catala compliance spike (petition054) validated this approach against G.A.1.4.3 and G.A.1.4.4 (opskrivning/nedskrivning rules). The spike confirmed:
- All modtagelsestidspunkt sub-rules encode without ambiguity in Catala
- The `WriteDownReasonCode` Java enum maps directly to Catala's enumeration model (see ADR 0031)
- At least one gap was found relative to P053 Gherkin scenarios, demonstrating the value of the layer

The P054 outcome was **Go**. Catala Tier A spikes (P069–P072) are now commissioned for the four highest-risk G.A.2/G.A.3 domains.

Subsequent spike results:
- **P069 (Go)** — G.A.2.3.2.1 Dækningsrækkefølge (GIL § 4 stk. 1–4). All six priority tiers encoded without ambiguity; 16 Catala test scopes. P057 implementation sprint unblocked.
- **P070 (Go)** — G.A.2.4 Forældelse (GIL § 18a stk. 1–4, stk. 7; Forældelsesl. §§ 3, 5, 14–18; SKM2015.718.ØLR). All three afbrydelse-types encoded without ambiguity; varsel/afgørelse distinction structurally enforced via Catala type system; fordringskompleks atomicity formalised as a Catala assertion; 16 test cases across 5 scopes. Two P059 Gherkin coverage gaps surfaced. P059 implementation sprint unblocked.

## Decision

OpenDebt adopts **Catala as a formal compliance verification layer** positioned between the juridisk vejledning and the Gherkin test suite. Catala does not replace the existing pipeline — it augments it as an oracle.

### Role of Catala in the pipeline

```
G.A. juridisk vejledning (natural language, Danish)
        ↓
Catala source (.catala_da) ← NEW LAYER
  - Encodes legal rules with article anchors
  - Runs as executable specification
  - Generates test cases (Catala Test module)
        ↓ oracle output
Gherkin feature file (validated against Catala)
        ↓
Implementation specs
        ↓
Java / Spring Boot production code
```

Catala acts as an **oracle**: when a Gherkin scenario contradicts a Catala-encoded rule, the Gherkin is wrong, not the Catala. Discrepancies identified by a Catala spike must be resolved before the implementation petition proceeds.

### Scope and tiers

Not all G.A. sections warrant Catala encoding. Sections are classified into three tiers:

| Tier | Criteria | Treatment |
|------|----------|-----------|
| **Tier A** | Complex temporal rules, numerical formulas, exception hierarchies with legal precedent | Catala spike commissioned before implementation. Go/No-Go gates the implementation petition. |
| **Tier B** | Enumerated state machines, routing logic, simple validation rules | Gherkin + outcome contract sufficient. Catala encoding optional, deferred. |
| **Tier C** | Reference-only sections (klager, særlige hæftelsesformer, international inddrivelse) | No petition, no Catala. Informational only. |

Current Tier A assignments:
- G.A.2.3.2.1 Dækningsrækkefølge → petition069 (companion to P057)
- G.A.2.4 Forældelse → petition070 (companion to P059) — highest priority
- G.A.3.1.1 Afdragsordninger → petition071 (companion to P061)
- G.A.3.1.2 Lønindeholdelse → petition072 (companion to P062)

### Spike pattern

Each Catala spike follows the research-spike pattern established by P054:

1. **Catala source** (`.catala_da`) — legal rules encoded in Danish with article anchors referencing the G.A. snapshot version in use
2. **Catala test suite** — ≥8 tests per spike, covering all rule branches and boundary cases
3. **SPIKE-REPORT.md** — comparison against companion petition's Gherkin scenarios, gap analysis, and explicit **Go/No-Go recommendation**

A spike is time-boxed (2 working days standard; 3 days for forældelse due to complexity). No production code is produced by a spike.

### Go/No-Go gate

The Go/No-Go recommendation is a binary gate:

- **Go**: All priority rules encode without ambiguity; ≥1 discrepancy found relative to companion Gherkin (demonstrates value); Catala compiles; OCaml/Python extraction produces runnable code. → Implementation petition proceeds.
- **No-Go**: Temporal rules cannot be expressed without workarounds; legal text is underspecified; encoding effort exceeds 4 person-days per section. → Document the blocker; do not invest further until new evidence is available.

### What Catala is NOT used for

| Excluded | Reason |
|----------|--------|
| Runtime execution in production services | Catala compiles to OCaml/Python; the Java implementation is independent. The oracle is used at specification time, not runtime. |
| Replacing Gherkin | Gherkin remains the primary executable test format. Catala generates and validates test cases; it does not replace them. |
| All G.A. sections | Only Tier A sections receive Catala treatment. Tier B and C sections rely on the existing petition + Gherkin pipeline. |
| Drools rules engine | ADR 0015 governs runtime rule evaluation. Catala is a specification-time compliance tool, not a runtime rules engine. |
| Configuration management | ADR 0031 governs statutory enums. Catala validates the legal correctness of those enums but does not manage them. |

### Repository conventions

Catala source files are stored under `catala/`:

```
catala/
  ga_1_4_3_opskrivning.catala_da        # P054 — opskrivning
  ga_1_4_4_nedskrivning.catala_da       # P054 — nedskrivning
  ga_2_3_2_1_daekningsraekkefoeigen.catala_da   # P069 — GIL § 4
  ga_2_4_foraeldelse.catala_da          # P070 — GIL § 18a
  ga_3_1_1_afdragsordninger.catala_da   # P071 — GIL § 11
  ga_3_1_2_loenindeholdelse_pct.catala_da       # P072 — Gæld.bekendtg. § 14
  tests/
    ga_opskrivning_nedskrivning_tests.catala_da
    ga_daekningsraekkefoeigen_tests.catala_da
    ga_foraeldelse_tests.catala_da
    ga_afdragsordninger_tests.catala_da
    ga_loenindeholdelse_tests.catala_da
  SPIKE-REPORT.md       # P054
  SPIKE-REPORT-069.md
  SPIKE-REPORT-070.md
  SPIKE-REPORT-071.md
  SPIKE-REPORT-072.md
```

All Catala files cite the G.A. snapshot version used (currently v3.16, 2026-03-28). When the G.A. is updated, affected Catala files must be reviewed and updated before the corresponding implementation petition is renewed.

## Consequences

**Improved:**
- Legal encoding errors are detected at specification time, before any Java code is written
- Discrepancies between G.A. natural language and Gherkin scenarios are surfaced with concrete evidence (test failures, not assertions)
- The compliance chain becomes bidirectionally traceable: every Catala rule block cites the G.A. article it encodes
- Algorithmic transparency is improved: a non-technical legal expert can read the `.catala_da` source alongside the G.A. text
- Aligns with the Danish public sector's *Fællesoffentlige Arkitekturprincipper* principle of privacy by design and documented, auditable decision logic

**Accepted costs:**
- Each Tier A petition requires a preceding 2–3 day research spike, adding elapsed time before the implementation sprint starts
- Catala expertise must be maintained in the team; the language has a learning curve
- When the G.A. is amended, both the Catala source and the Gherkin scenarios must be updated (two change points instead of one)
- No-Go outcomes from a spike may block an implementation petition pending legal clarification

**Unchanged:**
- The Java implementation remains the authoritative production runtime; Catala does not run in production
- Gherkin remains the primary BDD/test artifact; Catala is a specification oracle, not a test runner
- ADR 0015 (Drools), ADR 0031 (statutory enums), and the existing pipeline governance are unaffected

## References

- Merigoux, D., Chataing, N., & Protzenko, J. (2021). *Catala: A Programming Language for the Law*. Inria / Microsoft Research. HAL: [hal-02936606](https://inria.hal.science/hal-02936606). DOI: 10.1007/s10506-022-09328-5
- Petition 054: Catala Compliance Spike — G.A.1.4.3 og G.A.1.4.4 (Go outcome)
- Petition 069: Catala spike — Dækningsrækkefølge GIL § 4
- Petition 070: Catala spike — Forældelse G.A.2.4 (highest priority)
- Petition 071: Catala spike — Afdragsordninger GIL § 11
- Petition 072: Catala spike — Lønindeholdelsesprocent Gæld.bekendtg. § 14
- ADR 0015: Drools for Business Rules Engine
- ADR 0031: Statutory Codes Are Defined as Enums, Not Configuration
- G.A. Inddrivelse snapshot v3.16 (2026-03-28)

---

## Amendment: 2026-04-02 — Catala as First-Class Pipeline Stage

### Context of amendment

All four Tier A spikes (P069–P072) returned **Go**. The spike pattern has been validated across four distinct legal domains. Keeping Catala as a manual, petition-specific research activity creates a risk that future legal-footprint petitions are processed without Catala encoding simply because no spike was explicitly commissioned.

### Decision

Catala encoding is now a **mandatory pipeline phase** (Phase 3.7) for any petition with a legal footprint. The phase runs automatically between Gherkin (Phase 1) and Specifications (Phase 4) in the `pipeline-conductor` agent.

A petition has a legal footprint if **any** of the following is true:

- References a G.A. article, Gæld.bekendtg. paragraph, GIL paragraph, or Forældelsesl. section
- Encodes a calculation, date rule, or priority ordering derived from statute
- Depends on a legally-defined enumeration, threshold, or statutory rate
- Is in a financial or entitlement domain where silent divergence from law creates liability

### Mechanism

A new `catala-encoder` agent (`~/.claude/agents/catala-encoder.agent.md`) executes Phase 3.7. It:

1. Determines whether the petition has a legal footprint
2. Encodes the legal rules into `.catala_da` scope files with statute anchors
3. Produces a companion test suite in `catala/tests/`
4. Typechecks both files via `catala typecheck --language en --no-stdlib`
5. Updates `petitions/program-status.yaml` with `legal_footprint: true` and `catala_files:` list
6. Adds the new files to the `catala typecheck` step in `.github/workflows/ci.yml`

The `pipeline-conductor` agent has been updated to invoke `catala-encoder` at Phase 3.7 and to pass Catala artefact paths to `specs-translator` as mandatory context. Specs must cross-reference encoded FR-IDs.

### program-status.yaml convention

Each petition entry gains two optional fields:

```yaml
legal_footprint: true | false     # required for all petitions going forward
catala_files:                     # required when legal_footprint: true
  - catala/<scope>.catala_da
```

### Unchanged

- The spike pattern (P054, P069–P072) remains valid for exploratory work on new legal domains
- Tier B and Tier C classifications from the original decision are unchanged
- Catala does not run in production; the Java implementation remains the authoritative runtime
