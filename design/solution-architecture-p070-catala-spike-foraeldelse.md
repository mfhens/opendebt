# Solution Architecture — P070: Catala Compliance Spike — Forældelse G.A.2.4

**Document ID:** SA-P070
**Petition:** `petitions/petition070-catala-spike-foraeldelse.md`
**Status:** Approved for spike execution
**Legal basis:** GIL § 18a stk. 1–4; GIL § 18a stk. 7; Forældelsesl. § 5; Forældelsesl. §§ 14–18;
G.A.2.4 (G.A. snapshot v3.16, 2026-03-28); SKM2015.718.ØLR
**G.A. snapshot:** v3.16 (2026-03-28)
**Spike type:** Research spike — **no production code**
**Companion petition:** P059 (Forældelsesfrist og afbrydelse — implementation)
**Time box:** 3 working days (extended from 2-day standard per ADR 0032 §
"Spike pattern" — forældelse is the highest-complexity Tier A domain)
**ADRs binding this document:** ADR-0031, ADR-0032

---

## 1. Overview and Spike Purpose

### 1.1 Context

Forældelse (statutory prescription / limitation of debt claims) is designated a
**Tier A** domain in ADR 0032. Tier A means: before any implementation petition
(P059) can proceed, a Catala compliance spike must formally encode the G.A.2.4
rules and produce a Go/No-Go verdict.

ADR 0032 explicitly identifies forældelse as the **highest-priority** Tier A spike
because the relevant rules combine all three high-risk characteristics simultaneously:

| Risk characteristic | Forældelse manifestation |
|---------------------|--------------------------|
| Complex temporal rules | GIL § 18a, stk. 1 — effective start date varies by kildesystem (PSRM vs DMI/SAP38); stk. 4 — frist computed from a derived date, not the original stiftelse date |
| Exception hierarchies with legal precedent | SKM2015.718.ØLR refutes the intuitive reading that a lønindeholdelse-varsel constitutes afbrydelse — the ruling requires explicit modelling of the varsel/afbrydelse distinction |
| Numerical formulas | Tillaegsfrist calculation (G.A.2.4.4.2) combines base frist, afbrydelse periods, and udskydelse offsets |

The spike does **not** implement any production runtime component. It produces three
file artefacts (see §2) whose sole purpose is to:

1. Formally verify that G.A.2.4 rules can be expressed without ambiguity in Catala
2. Expose any discrepancy between G.A.2.4 natural-language text and the P059
   companion Gherkin scenarios
3. Emit a Go/No-Go verdict that gates the P059 implementation sprint

### 1.2 Spike Scope

| In scope | Out of scope |
|----------|--------------|
| Catala encoding of GIL § 18a stk. 1–4 | Any Java / Spring Boot implementation code |
| Catala encoding of Forældelsesl. § 5 (3-year basic period) | Drools rules engine changes |
| Catala encoding of Forældelsesl. §§ 14–18 (suspension grounds) | Database schema or REST API design |
| SKM2015.718.ØLR — varsel ≠ afbrydelse rule | Payment application logic (P057) |
| Fordringskompleks propagation and atomicity (GIL § 18a, stk. 2) | Pro-rata distribution (P062) |
| Tillaegsfrist calculation (G.A.2.4.4.2) | CI compilation of Catala artefacts (deferred, AC-18) |
| GIL § 18a stk. 7 — foreløbig afbrydelse (tomt fordringskompleks) | — |
| ≥8 Catala test cases covering all scopes | Deployment of any artefact to any environment |
| SPIKE-REPORT-070.md with explicit Go/No-Go verdict | Gherkin feature file authorship (owned by P059) |

### 1.3 Relationship to the OpenDebt Compliance Pipeline

```
G.A.2.4 juridisk vejledning (natural language, Danish)
           ↓
catala/ga_2_4_foraeldelse.catala_da         ← THIS SPIKE PRODUCES THIS
  - Encodes GIL § 18a stk. 1–4, Forældelsesl. § 5, §§ 14–18
  - Article-anchored rule blocks (bidirectional traceability)
  - Executable: Catala compiles to OCaml / Python
           ↓ oracle comparison
catala/tests/ga_foraeldelse_tests.catala_da ← THIS SPIKE PRODUCES THIS
  - ≥8 test cases, all scope branches covered
           ↓ Go/No-Go verdict
catala/SPIKE-REPORT-070.md                  ← THIS SPIKE PRODUCES THIS
  - Coverage table: P059 Gherkin scenarios vs Catala scopes
  - Gap analysis and discrepancy list
  - Explicit Go / No-Go recommendation
           ↓ gates
petitions/petition059 — P059 Implementation Sprint
  Gherkin feature file (validated against Catala oracle output)
           ↓
Implementation specs (debt-service, caseworker-portal)
           ↓
Java / Spring Boot production code
```

---

## 2. Deliverable Component Model

> **Critical note:** This is a research spike. The three artefacts below are
> **files**, not runtime components. No container, no deployable service, no
> Kubernetes workload, no API, and no database migration is produced by this
> petition. All three artefacts live entirely under `catala/` in the repository.

### 2.1 Artefact Inventory

| Artefact ID | File path | Type | Responsibility |
|-------------|-----------|------|----------------|
| ART-070-1 | `catala/ga_2_4_foraeldelse.catala_da` | Catala source | Formal encoding of G.A.2.4 rules (GIL § 18a stk. 1–4, GIL § 18a stk. 7, Forældelsesl. § 5, §§ 14–18). Six scopes, four enumeration types. Article-anchored rule blocks in Danish. |
| ART-070-2 | `catala/tests/ga_foraeldelse_tests.catala_da` | Catala test module | ≥8 test cases exercising all scope branches and boundary conditions. Uses Catala `Test` module with `assertion` blocks. |
| ART-070-3 | `catala/SPIKE-REPORT-070.md` | Markdown report | Coverage table (P059 Gherkin scenarios × Catala scopes), gap analysis, discrepancy list, Go/No-Go verdict. |

### 2.2 Artefact Dependency Graph

```
ART-070-1 (ga_2_4_foraeldelse.catala_da)
    │
    ├──► ART-070-2 (tests/ga_foraeldelse_tests.catala_da)
    │       [imports ART-070-1; assertions verify ART-070-1 rule outputs]
    │
    └──► ART-070-3 (SPIKE-REPORT-070.md)
            [references ART-070-1 scope definitions;
             compares with P059 companion Gherkin;
             issues Go/No-Go verdict]
```

### 2.3 Fit Within catala/ Directory Convention (ADR 0032)

```
catala/
  ga_1_4_3_opskrivning.catala_da                    # P054
  ga_1_4_4_nedskrivning.catala_da                   # P054
  ga_2_3_2_1_daekningsraekkefoeigen.catala_da       # P069
  ga_2_4_foraeldelse.catala_da                      # P070 ← ART-070-1 (this spike)
  ga_3_1_1_afdragsordninger.catala_da               # P071
  ga_3_1_2_loenindeholdelse_pct.catala_da           # P072
  tests/
    ga_opskrivning_nedskrivning_tests.catala_da     # P054
    ga_daekningsraekkefoeigen_tests.catala_da       # P069
    ga_foraeldelse_tests.catala_da                  # P070 ← ART-070-2 (this spike)
    ga_afdragsordninger_tests.catala_da             # P071
    ga_loenindeholdelse_tests.catala_da             # P072
  SPIKE-REPORT.md                                   # P054
  SPIKE-REPORT-069.md                               # P069
  SPIKE-REPORT-070.md                               # P070 ← ART-070-3 (this spike)
  SPIKE-REPORT-071.md                               # P071
  SPIKE-REPORT-072.md                               # P072
```

All files cite G.A. snapshot v3.16 (2026-03-28) in their file header, consistent
with the P069 convention established by `ga_2_3_2_1_daekningsraekkefoeigen.catala_da`.

---

## 3. Catala Model Design

### 3.1 Enumeration Types

Four enumeration types must be declared in ART-070-1. All enumerations are in
Danish, consistent with the `.catala_da` dialect convention. Each maps to a concept
explicitly named in G.A.2.4 or GIL § 18a.

#### ENUM-1: KildesystemType — GIL § 18a, stk. 1

GIL § 18a, stk. 1 conditions the effective forældelsesfrist start date on the
kildesystem from which the fordring was received. Two systems are currently
enumerated in G.A.2.4.

| Variant | Legal basis | Meaning |
|---------|-------------|---------|
| `PSRM` | GIL § 18a, stk. 1 | Primary debt management system — uses modtagelsesdato as UdskydelsesBeregning input |
| `DMI_SAP38` | GIL § 18a, stk. 1 | Legacy DMI/SAP38 system — receives special udskydelse treatment per stk. 1, 2. pkt. |

```
declaration enumeration KildesystemType:
  -- PSRM
  -- DMI_SAP38
```

#### ENUM-2: RetsgrundlagType — Forældelsesl. § 5; GIL § 18a, stk. 4

The forældelsesfrist duration depends on whether the fordring is subject to the
ordinary 3-year period (Forældelsesl. § 5) or a special legal basis that prescribes
a different period.

| Variant | Legal basis | Frist |
|---------|-------------|-------|
| `ORDINARY` | Forældelsesl. § 5, stk. 1 | 3 years from fristStartDato |
| `SAERLIGT_RETSGRUNDLAG` | GIL § 18a, stk. 4, 2. pkt.; Forældelsesl. § 5, stk. 2 | Frist determined by the specific statute cited in the fordring |

```
declaration enumeration RetsgrundlagType:
  -- ORDINARY
  -- SAERLIGT_RETSGRUNDLAG
```

#### ENUM-3: AfbrydelseType — Forældelsesl. §§ 14–18; SKM2015.718.ØLR

Afbrydelse events reset or suspend the running of the forældelsesfrist. This
enumeration is the central modelling decision of the spike: it encodes the
SKM2015.718.ØLR-mandated distinction between varsel (which does **not** interrupt)
and the events that genuinely constitute afbrydelse under Forældelsesl. §§ 14–18.

| Variant | Legal basis | Effect | Note |
|---------|-------------|--------|------|
| `BEROSTILLELSE` | Forældelsesl. § 14 | Suspends frist during the berostillelse period | Classic suspension ground |
| `LOENINDEHOLDELSE_AFGOERELSE` | Forældelsesl. § 15; GIL § 18a, stk. 1 | Interrupts frist; resets running period | Formal enforcement decision |
| `LOENINDEHOLDELSE_VARSEL` | SKM2015.718.ØLR | **Does NOT constitute afbrydelse** | Varsel is a preparatory notice, not an enforcement act — see §3.4 FR-2 design note |
| `UDLAEG` | Forældelsesl. § 15; Retsplejelovens § 478 | Interrupts frist; resets running period | Court-ordered enforcement action |

```
declaration enumeration AfbrydelseType:
  -- BEROSTILLELSE
  -- LOENINDEHOLDELSE_AFGOERELSE
  -- LOENINDEHOLDELSE_VARSEL
  -- UDLAEG
```

> **Design note — LOENINDEHOLDELSE_VARSEL:** This variant is included in the
> enumeration so that the Catala model can receive it as an input and explicitly
> return `afbryderForaeldelse = false`. If the variant were absent, a consumer
> could pass an unclassified varsel event and receive no output. Including it
> forces the rule to be explicit about the negative case. This is the key
> compliance insight that P070 must validate against P059 Gherkin.

### 3.2 Scope Definitions

Six scopes encode the full G.A.2.4 rule set. Each scope has a single,
well-bounded responsibility. Scopes are sequentially composable: the output of
`UdskydelsesBeregning` feeds `ForaeldelseFristBeregning`; the output of
`AfbrydelseValidering` is consumed by `ForaeldelseFristBeregning` and
`Fordringskompleks`.

#### SCOPE-1: UdskydelsesBeregning — GIL § 18a, stk. 1

**Responsibility:** Compute the effective start date (`fristStartDato`) for the
forældelsesfrist, accounting for kildesystem-conditional udskydelse.

| Context field | Direction | Type | Description |
|---------------|-----------|------|-------------|
| `kildesystem` | input | `KildesystemType` | The system from which the fordring was received |
| `stiftelsesDato` | input | `date` | Original date of the debt claim's creation |
| `modtagelsesDato` | input | `date` | Date the fordring was received in the current inddrivelsessystem |
| `fristStartDato` | output | `date` | Effective start date for forældelsesfrist computation |

**Rules:**
- FR-1.1 (PSRM): `fristStartDato = modtagelsesDato` — Ankerpunkt: GIL § 18a, stk. 1, 1. pkt.
- FR-1.2 (DMI_SAP38): `fristStartDato = stiftelsesDato` (special udskydelse) — Ankerpunkt: GIL § 18a, stk. 1, 2. pkt.

**Spike validation target:** Verify whether the G.A.2.4 text sufficiently specifies
which date applies for each kildesystem without ambiguity.

#### SCOPE-2: ForaeldelseFristBeregning — GIL § 18a, stk. 4; Forældelsesl. § 5

**Responsibility:** Compute the forældelsesfrist expiry date (`udloebsDato`) given
the effective start date and the applicable retsgrundlag.

| Context field | Direction | Type | Description |
|---------------|-----------|------|-------------|
| `fristStartDato` | input | `date` | Output of `UdskydelsesBeregning` |
| `retsgrundlag` | input | `RetsgrundlagType` | Ordinary 3-year or saerligt retsgrundlag |
| `saerligFristAar` | input | `integer` | Frist in years when `retsgrundlag = SAERLIGT_RETSGRUNDLAG`; 0 otherwise |
| `afbrydelseAdjusteringDage` | input | `integer` | Net adjustment days from `AfbrydelseValidering` (berostillelse periods + resets) |
| `udloebsDato` | output | `date` | Computed expiry date of the forældelsesfrist |
| `erForaeldet` | output | `boolean` | `true` if assessment date ≥ `udloebsDato` |
| `vurderingsDato` | input | `date` | The date as-of which forældelsesstatus is assessed |

**Rules:**
- FR-2.1 (ORDINARY): `udloebsDato = fristStartDato + 3 years + afbrydelseAdjusteringDage days` — Ankerpunkt: Forældelsesl. § 5, stk. 1; GIL § 18a, stk. 4
- FR-2.2 (SAERLIGT): `udloebsDato = fristStartDato + saerligFristAar years + afbrydelseAdjusteringDage days` — Ankerpunkt: GIL § 18a, stk. 4, 2. pkt.
- FR-2.3: `erForaeldet = vurderingsDato >= udloebsDato`

**Spike validation target:** Verify Catala's date arithmetic handles the year-based
addition correctly, and that the adjustment from `AfbrydelseValidering` composes
cleanly.

#### SCOPE-3: AfbrydelseValidering — Forældelsesl. §§ 14–18; SKM2015.718.ØLR

> **Naming note:** Also known as `AfbrydelseRegler` in earlier drafts; the canonical name is `AfbrydelseValidering` (chosen for precision — the scope validates the legal classification of each event, not just applies rules). All references in this document and in ART-070-1 use `AfbrydelseValidering`.

**Responsibility:** Validate whether a given afbrydelse event constitutes a genuine
interruption/suspension of the forældelsesfrist, and compute the net adjustment in
days to apply to `ForaeldelseFristBeregning`.

| Context field | Direction | Type | Description |
|---------------|-----------|------|-------------|
| `afbrydelseType` | input | `AfbrydelseType` | The type of the event being assessed |
| `afbrydelseDato` | input | `date` | Date the event occurred |
| `berostillelseSlutDato` | input | `date` | Relevant only when `afbrydelseType = BEROSTILLELSE`; the date the berostillelse ended |
| `afbryderForaeldelse` | output | `boolean` | `true` if the event interrupts or suspends the frist |
| `suspensionsDage` | output | `integer` | Number of days the frist is extended (berostillelse duration) |
| `nulstillerFrist` | output | `boolean` | `true` if the frist resets to a new start date (AFGOERELSE, UDLAEG) |

**Rules (critical — this is where SKM2015.718.ØLR is enforced):**
- FR-3.1 (BEROSTILLELSE): `afbryderForaeldelse = true`; `suspensionsDage = berostillelseSlutDato - afbrydelseDato`; `nulstillerFrist = false` — Ankerpunkt: Forældelsesl. § 14
- FR-3.2 (LOENINDEHOLDELSE_AFGOERELSE): `afbryderForaeldelse = true`; `suspensionsDage = 0`; `nulstillerFrist = true` — Ankerpunkt: Forældelsesl. § 15; GIL § 18a, stk. 1
- FR-3.3 (LOENINDEHOLDELSE_VARSEL): **`afbryderForaeldelse = false`**; `suspensionsDage = 0`; `nulstillerFrist = false` — Ankerpunkt: SKM2015.718.ØLR (explicit negative rule)
- FR-3.4 (UDLAEG): `afbryderForaeldelse = true`; `suspensionsDage = 0`; `nulstillerFrist = true` — Ankerpunkt: Forældelsesl. § 15; Retsplejelovens § 478

> **⚠️ FLAG-A — Varsel/afbrydelse distinction:** The SKM2015.718.ØLR negative
> rule for LOENINDEHOLDELSE_VARSEL (FR-3.3) is the highest-priority discrepancy
> risk. The spike must explicitly test whether any P059 Gherkin scenario treats
> a varsel event as afbrydelse. If such a scenario is found, it is a discrepancy
> and must be reported in SPIKE-REPORT-070.md before P059 implementation proceeds.

#### SCOPE-4: Fordringskompleks — GIL § 18a, stk. 2

**Responsibility:** Model the propagation and atomicity rules for
fordringskompleks — the rule that afbrydelse of one fordring within a complex
propagates to all related fordringer, and that forældelsesstatus is computed
atomically for the entire kompleks.

| Context field | Direction | Type | Description |
|---------------|-----------|------|-------------|
| `fordringIds` | input | `list of integer` | IDs of all fordringer in the kompleks |
| `erAfbrudt` | input | `list of boolean` | Per-fordring afbrydelse status (from `AfbrydelseValidering`) |
| `kompleksAfbrudt` | output | `boolean` | `true` if any fordring in the kompleks has been interrupted — propagated to all members |
| `alleForaeldet` | output | `boolean` | `true` only if all fordringer in the kompleks are individually forældet |

**Rules:**
- FR-4.1: `kompleksAfbrudt = exists fordring in fordringIds such that erAfbrudt[fordring] = true` — Ankerpunkt: GIL § 18a, stk. 2
- FR-4.1b (atomicity): `alleForaeldet = for all fordring in fordringIds: erForaeldet[fordring] = true` — Ankerpunkt: GIL § 18a, stk. 2

**Spike validation target:** Verify that Catala's list/aggregate semantics can
express the existential and universal quantification required by stk. 2 without
workarounds.

#### SCOPE-5: TillaegsfristBeregning — G.A.2.4.4.2

**Responsibility:** Compute any additional frist periods (tillægsfrist) that
extend the basic forældelsesfrist under G.A.2.4.4.2. Tillægsfrist applies when
specific conditions delay the effective knowledge of the debt's existence.

| Context field | Direction | Type | Description |
|---------------|-----------|------|-------------|
| `grundForaeldelsesUdloebsDato` | input | `date` | Basic udloebsDato from `ForaeldelseFristBeregning` |
| `harTillaegsbetingelse` | input | `boolean` | Whether the tillægs conditions in G.A.2.4.4.2 apply |
| `tillaegsfristDage` | input | `integer` | Additional days granted by G.A.2.4.4.2 when `harTillaegsbetingelse = true` |
| `effektivUdloebsDato` | output | `date` | Final expiry date after all adjustments |

**Rules:**
- FR-5.1 (base): `effektivUdloebsDato = grundForaeldelsesUdloebsDato` — default rule
- FR-5.2 (tillæg): exception — when `harTillaegsbetingelse = true`: `effektivUdloebsDato = grundForaeldelsesUdloebsDato + tillaegsfristDage days` — Ankerpunkt: G.A.2.4.4.2

**Spike validation target:** Verify that G.A.2.4.4.2 text is sufficiently precise
to parameterise `tillaegsfristDage` without ambiguity. If it is not (i.e. the
number of days is underspecified), this must be flagged as a No-Go trigger.

#### SCOPE-6: ForeløbigAfbrydelse — GIL § 18a, stk. 7

**Responsibility:** Model the foreløbig (provisional) afbrydelse that arises when
a fordringskompleks with no existing members carrying an active frist is received
into inddrivelse. GIL § 18a stk. 7 mandates that in this situation a new,
provisional 3-year frist begins from modtagelsesdato, even though no prior
afbrydelse event has occurred. This scope is distinct from `AfbrydelseValidering`
(SCOPE-3), which classifies ongoing interruption events; SCOPE-6 handles the
receipt-triggered provisional frist for an initially empty kompleks.

| Context field | Direction | Type | Description |
|---------------|-----------|------|-------------|
| `erTomtKompleks` | input | `boolean` | `true` if the fordringskompleks has no existing members with an active frist at time of modtagelse |
| `modtagelsesDato` | input | `date` | Date the (empty) fordringskompleks was received into inddrivelse |
| `foreløbigAfbrydelse` | output | `boolean` | `true` if the foreløbige afbrydelse rule applies (stk. 7 trigger met) |
| `foreløbigFristUdloebsDato` | output | `date` | Provisional frist expiry date (`modtagelsesDato + 3 years`) when `foreløbigAfbrydelse = true`; undefined otherwise |

**Rules:**
- FR-4.2 (foreløbig afbrydelse — tomt kompleks): when `erTomtKompleks = true`:
  `foreløbigAfbrydelse = true`; `foreløbigFristUdloebsDato = modtagelsesDato + 3 years`
  — Ankerpunkt: GIL § 18a, stk. 7
- FR-4.2 (base — kompleks med medlemmer): when `erTomtKompleks = false`:
  `foreløbigAfbrydelse = false` — stk. 7 trigger does not apply

**Spike validation target:** Verify that the "tomt kompleks" trigger condition is
unambiguous in G.A.2.4 / GIL § 18a stk. 7 text, and that Catala's date arithmetic
handles the 3-year addition from modtagelsesdato cleanly. If the legal text is
underspecified about what constitutes "tomt" (e.g. kompleks with inactive or
historically forældet members), flag as a No-Go trigger.

### 3.3 Scope Composition Diagram

```
                ┌──────────────────────────┐
                │   UdskydelsesBeregning   │  SCOPE-1
                │ kildesystem, stiftelse,  │  GIL § 18a, stk. 1
                │ modtagelse               │
                │  → fristStartDato        │
                └─────────────┬────────────┘
                              │ fristStartDato
                              ▼
  AfbrydelseValidering ──────►│
  SCOPE-3                     │
  afbrydelseType,             │  ┌───────────────────────────────┐
  afbrydelseDato,             ├─►│  ForaeldelseFristBeregning    │  SCOPE-2
  berostillelseSlutDato       │  │  fristStartDato +             │  GIL § 18a stk. 4
  → afbryderForaeldelse       │  │  retsgrundlag +               │  Forældelsesl. § 5
  → suspensionsDage           │  │  afbrydelseAdjustering        │
  → nulstillerFrist           │  │  → udloebsDato, erForaeldet   │
  [SKM2015.718.ØLR FR-3.3]    │  └───────────────┬───────────────┘
                              │                  │ udloebsDato
                              │                  ▼
  AfbrydelseValidering ──────►│  ┌───────────────────────────────┐
  (per fordring i kompleks)   │  │  TillaegsfristBeregning       │  SCOPE-5
                              │  │  grundUdloebsDato +           │  G.A.2.4.4.2
                              │  │  tillæg (conditional)         │
                              │  │  → effektivUdloebsDato        │
                              │  └───────────────────────────────┘
                              │
  Fordringskompleks ◄─────────┘
  SCOPE-4
  fordringIds, erAfbrudt
  → kompleksAfbrudt, alleForaeldet
  GIL § 18a, stk. 2

  ForeløbigAfbrydelse                            SCOPE-6
  erTomtKompleks, modtagelsesDato                GIL § 18a, stk. 7
  → foreløbigAfbrydelse
  → foreløbigFristUdloebsDato
  [Parallel path — triggered at receipt of empty kompleks]
```

### 3.4 Critical Design Decision: LOENINDEHOLDELSE_VARSEL in AfbrydelseType

Including `LOENINDEHOLDELSE_VARSEL` as an explicit variant of `AfbrydelseType`
(rather than excluding it from the enumeration) is an intentional design decision
with direct compliance impact.

**Rationale:**
- If varsel were absent from the enumeration, a system feeding events into the
  Catala model would have no type-safe way to represent a varsel event. It would
  either map it to `LOENINDEHOLDELSE_AFGOERELSE` (incorrect — constitutes an
  afbrydelse per the oracle) or drop it silently.
- Including it forces Catala's scope rules to explicitly return
  `afbryderForaeldelse = false` for FR-3.3, making the oracle's negative ruling
  visible and testable.
- The Catala test suite (ART-070-2) must include at least one test asserting
  FR-3.3 = false, which directly validates that P059 Gherkin has not treated
  a varsel as an afbrydelse event.

**Legal anchor:** SKM2015.718.ØLR — the Eastern High Court ruling that a
lønindeholdelse-varsel does not constitute afbrydelse of the forældelsesfrist.
This ruling post-dates the basic forældelsesl. § 15 framework and is the
authoritative interpretation under Danish administrative law.

---

## 4. Legal Basis Traceability

### 4.1 Statutory Requirement → Catala Scope Mapping

| Statutory basis | G.A. section | Catala scope | Rule ID |
|-----------------|-------------|--------------|---------|
| GIL § 18a, stk. 1 (udskydelse by kildesystem) | G.A.2.4.1 | `UdskydelsesBeregning` | FR-1.1, FR-1.2 |
| GIL § 18a, stk. 2 (fordringskompleks propagation) | G.A.2.4.2 | `Fordringskompleks` | FR-4.1, FR-4.1b |
| GIL § 18a, stk. 4 (frist expiry from derived start) | G.A.2.4.4 | `ForaeldelseFristBeregning` | FR-2.1, FR-2.2 |
| GIL § 18a, stk. 7 (foreløbig afbrydelse — tomt fordringskompleks) | G.A.2.4.2 | `ForeløbigAfbrydelse` | FR-4.2 |
| Forældelsesl. § 5, stk. 1 (3-year ordinary period) | G.A.2.4.4.1 | `ForaeldelseFristBeregning` | FR-2.1 |
| Forældelsesl. § 5, stk. 2 (special period) | G.A.2.4.4.1 | `ForaeldelseFristBeregning` | FR-2.2 |
| Forældelsesl. § 14 (berostillelse / suspension) | G.A.2.4.3 | `AfbrydelseValidering` | FR-3.1 |
| Forældelsesl. § 15 (formal afbrydelse by enforcement) | G.A.2.4.3 | `AfbrydelseValidering` | FR-3.2, FR-3.4 |
| SKM2015.718.ØLR (varsel ≠ afbrydelse) | G.A.2.4.3 (interpretation) | `AfbrydelseValidering` | FR-3.3 |
| Retsplejelovens § 478 (udlæg as enforcement act) | G.A.2.4.3 | `AfbrydelseValidering` | FR-3.4 |
| G.A.2.4.4.2 (tillægsfrist) | G.A.2.4.4.2 | `TillaegsfristBeregning` | FR-5.1, FR-5.2 |

### 4.2 Functional Requirements → Artefact Traceability

| FR ID | Functional requirement | Artefact | Scope / Rule |
|-------|------------------------|----------|--------------|
| FR-1 | Effective forældelsesfrist start date conditioned on kildesystem | ART-070-1 | `UdskydelsesBeregning` FR-1.1, FR-1.2 |
| FR-2 | Forældelsesfrist expiry computed from derived start date | ART-070-1 | `ForaeldelseFristBeregning` FR-2.1–2.3 |
| FR-3 | Afbrydelse events validated; varsel explicitly excluded (SKM2015.718.ØLR) | ART-070-1 | `AfbrydelseValidering` FR-3.1–3.4 |
| FR-4 | Fordringskompleks propagation and atomicity | ART-070-1 | `Fordringskompleks` FR-4.1, FR-4.1b |
| FR-4.2 | Foreløbig afbrydelse — new 3-year frist when tomt fordringskompleks received into inddrivelse (GIL § 18a stk. 7) | ART-070-1 | `ForeløbigAfbrydelse` FR-4.2 |
| FR-5 | Tillægsfrist conditional extension | ART-070-1 | `TillaegsfristBeregning` FR-5.1–5.2 |
| FR-T | ≥8 test cases covering all FR IDs | ART-070-2 | All scopes |
| FR-R | Go/No-Go verdict with coverage table | ART-070-3 | Report document |

### 4.3 Minimum Test Case Coverage (ART-070-2)

The test suite must cover the following boundary conditions as a minimum:

| Test ID | Description | Scope tested | FR covered |
|---------|-------------|-------------|------------|
| T-01 | PSRM fordring — fristStartDato = modtagelsesDato | `UdskydelsesBeregning` | FR-1.1 |
| T-02 | DMI_SAP38 fordring — fristStartDato = stiftelsesDato | `UdskydelsesBeregning` | FR-1.2 |
| T-03 | ORDINARY retsgrundlag — udloebsDato = fristStartDato + 3 years | `ForaeldelseFristBeregning` | FR-2.1 |
| T-04 | SAERLIGT retsgrundlag — udloebsDato uses saerligFristAar | `ForaeldelseFristBeregning` | FR-2.2 |
| T-05 | BEROSTILLELSE — suspends frist; suspensionsDage > 0; nulstillerFrist = false | `AfbrydelseValidering` | FR-3.1 |
| T-06 | LOENINDEHOLDELSE_AFGOERELSE — afbryderForaeldelse = true; nulstillerFrist = true | `AfbrydelseValidering` | FR-3.2 |
| T-07 | **LOENINDEHOLDELSE_VARSEL — afbryderForaeldelse = false** (SKM2015.718.ØLR) | `AfbrydelseValidering` | FR-3.3 |
| T-08 | UDLAEG — afbryderForaeldelse = true; nulstillerFrist = true | `AfbrydelseValidering` | FR-3.4 |
| T-09 | Fordringskompleks — one AFGOERELSE propagates kompleksAfbrudt = true to all | `Fordringskompleks` | FR-4.1 |
| T-10 | TillaegsfristBeregning — harTillaegsbetingelse = true extends effektivUdloebsDato | `TillaegsfristBeregning` | FR-5.2 |
| T-15 | ForeløbigAfbrydelse — tomt fordringskompleks received into inddrivelse → foreløbigAfbrydelse = true; foreløbigFristUdloebsDato = modtagelsesDato + 3 years (GIL § 18a stk. 7) | `ForeløbigAfbrydelse` | FR-4.2 |

---

## 5. Relationship to Existing OpenDebt Components

### 5.1 Oracle Role

The Catala spike acts as an **oracle** to the P059 implementation, not as a
runtime dependency. No existing OpenDebt container calls the Catala model at
runtime. The relationship is specification-time only.

```
                    SPECIFICATION TIME ONLY
                    ┌─────────────────────────────────────┐
                    │                                     │
  ART-070-1 ────► Catala oracle                          │
  (ga_2_4_foraeldelse.catala_da)                         │
       │                                                  │
       │ Go/No-Go verdict                                 │
       ▼                                                  │
  SPIKE-REPORT-070.md ─────────────────────────────────► P059 Gherkin review
                    │                                     │
                    └─────────────────────────────────────┘

                    RUNTIME (unchanged — existing OpenDebt containers)
                    ┌─────────────────────────────────────┐
                    │  debt-service        (P059 target)  │
                    │  payment-service     (P057 — live)  │
                    │  caseworker-portal   (views owner)  │
                    └─────────────────────────────────────┘
```

### 5.2 Affected Existing Containers (P059 target — not in scope for this spike)

The following OpenDebt containers will be **modified by the P059 implementation
petition** (not by this spike). They are listed here to clarify the architectural
boundary and confirm the oracle's relevance.

| Container | Current responsibility | P059 expected change | Catala scope that validates |
|-----------|----------------------|----------------------|-----------------------------|
| `debt-service` | Fordring (claim) management, interest calculation | Add forældelsesfrist computation and status tracking; GIL § 18a enforcement | `UdskydelsesBeregning`, `ForaeldelseFristBeregning`, `Fordringskompleks` |
| `caseworker-portal` | Case overview, manual interventions | Add forældelsesstatus display and tillægsfrist UI indicator | `TillaegsfristBeregning` output display |
| `payment-service` | GIL § 4 payment application | Forældelses-check before applying payment to a forældet fordring | `ForaeldelseFristBeregning` (erForaeldet flag) |

> **Non-goal:** This spike does not design, specify, or constrain the P059
> implementation of these containers. The spike's only influence on these
> containers is the Go/No-Go verdict: if Go, P059 proceeds; if No-Go, P059 is
> blocked pending resolution.

### 5.3 ADR Compliance Checklist

| ADR | Requirement | How this spike complies |
|-----|-------------|------------------------|
| ADR-0031 | Statutory codes as enums, not configuration | All enumeration types (`KildesystemType`, `RetsgrundlagType`, `AfbrydelseType`) are Catala enumeration declarations, consistent with the Java enum pattern the ADR governs. The Catala variants will be the specification-time counterparts of the Java enums P059 implements. |
| ADR-0032 | Catala spike gates Tier A implementation petitions | This document is the architecture artefact for the P070 spike. SPIKE-REPORT-070.md (ART-070-3) will carry the Go/No-Go verdict that gates P059. |
| ADR-0032 (AC-18) | No CI compilation of Catala files | No CI job, build step, or pipeline stage is added by this spike. Catala files are committed as source only. |

---

## 6. Non-Functional Requirements

| NFR ID | Requirement | Application to this spike |
|--------|-------------|---------------------------|
| NFR-1 | **No production deployment** — spike artefacts are files only | All three deliverables are Markdown or `.catala_da` files. No JAR, no Docker image, no Kubernetes manifest. |
| NFR-2 | **G.A. version pinning** — all files cite snapshot v3.16 (2026-03-28) | File headers in ART-070-1 and ART-070-2 must cite the G.A. snapshot version, consistent with the P069 convention. |
| NFR-3 | **Article anchoring** — every Catala rule block must cite the specific GIL / Forældelsesl. article it encodes | Rule comments in ART-070-1 must include an `# Ankerpunkt:` citation, consistent with the P069 pattern. |
| NFR-4 | **Test coverage** — ≥8 test cases, all 6 scopes exercised | ART-070-2 minimum test inventory is specified in §4.3. T-07 (varsel negative rule) and T-15 (foreløbig afbrydelse — stk. 7) are mandatory. |
| NFR-5 | **Go/No-Go binary verdict** — SPIKE-REPORT-070.md must emit exactly one of Go or No-Go | ART-070-3 must contain an unambiguous verdict per ADR 0032 §"Go/No-Go gate" criteria. |
| NFR-6 | **3-day time box** — spike must complete within the forældelse-extended time allowance | The three deliverables are the completion criterion. No further work is in scope. |

---

## 7. Known Risks and Escalation Flags

| Flag ID | Risk | Trigger condition | Escalation |
|---------|------|-------------------|------------|
| FLAG-A | LOENINDEHOLDELSE_VARSEL treated as afbrydelse in P059 Gherkin | If T-07 (FR-3.3) reveals a P059 scenario that passes a varsel event and expects `afbryderForaeldelse = true` | Report as Discrepans Fundet in SPIKE-REPORT-070.md §Coverage Table; P059 Gherkin must be corrected before implementation sprint |
| FLAG-B | G.A.2.4.4.2 tillægsfrist underspecified | If `tillaegsfristDage` value cannot be derived from G.A.2.4.4.2 text without external consultation | No-Go trigger per ADR 0032 §"Go/No-Go gate" criterion 3 (legal text underspecified) |
| FLAG-C | Catala list/aggregate semantics insufficient for Fordringskompleks | If SCOPE-4 existential quantification cannot be expressed in Catala without workarounds | No-Go trigger per ADR 0032 §"Go/No-Go gate" criterion 1 (temporal rules cannot be expressed) |
| FLAG-D | Date arithmetic precision — Catala `date` type lacks time-of-day | Identified in SPIKE-REPORT-069.md §"FLAG-D" for dækningsrækkefølge; may recur for forældelsesfrist if timestamps are used | Accepted limitation — all forældelsesfrist calculations in G.A.2.4 use calendar dates, not datetimes. Confirm in spike. |

---

## 8. Rationale and Assumptions

### 8.1 Architectural Decisions

| Decision | Rationale |
|----------|-----------|
| Three-artefact spike structure (source, tests, report) | Consistent with P054 and P069 spike pattern (ADR 0032 §"Spike pattern"). Deviating would require ADR amendment. |
| Five scopes, not a single monolithic scope | Each G.A.2.4 sub-section has a distinct computational concern. Monolithic encoding would make rule exceptions untraceable and prevent scoped test assertions. |
| `LOENINDEHOLDELSE_VARSEL` included in `AfbrydelseType` as an explicit variant | Forces the negative rule (FR-3.3) to be modelled and testable, surfacing the SKM2015.718.ØLR finding at the type level. |
| `saerligFristAar` as an integer input to `ForaeldelseFristBeregning` | G.A.2.4 defers the specific year count for special retsgrundlag to the individual statute. Parameterising it keeps the Catala model generic while allowing P059 Gherkin to supply concrete values. |
| No Structurizr container added to `workspace.dsl` | Catala spike artefacts are not runtime containers. The DSL block (§9) adds a documentation-level spike element using the workspace extension pattern, consistent with how CatalaCompliance spikes are tracked across P054 and P069. |

### 8.2 Assumptions

| Assumption ID | Assumption | Impact if false |
|---------------|-----------|-----------------|
| A-1 | G.A. snapshot v3.16 (2026-03-28) is the authoritative version for P059 | If G.A. is updated before P059 implementation, SPIKE-REPORT-070.md and ART-070-1/2 must be reviewed and potentially revised |
| A-2 | SKM2015.718.ØLR remains the controlling interpretation of Forældelsesl. § 15 for varsel events | If new case law or a legislative amendment alters this, FR-3.3 must be revised |
| A-3 | Catala's `date` type is sufficient for all G.A.2.4 temporal computations (no intra-day timestamps needed) | FLAG-D — if datetime precision is required, an encoding workaround or No-Go recommendation is needed |
| A-4 | G.A.2.4.4.2 text is sufficiently specific to parameterise `tillaegsfristDage` without legal clarification | FLAG-B — if underspecified, No-Go for this domain |
| A-5 | The P059 companion Gherkin has at least one scenario covering the varsel/afbrydelse distinction | If P059 has no varsel scenario, the spike cannot demonstrate discrepancy-detection value; must note this in SPIKE-REPORT-070.md |

---

## 9. Structurizr DSL Block

This block is directly mergeable into `architecture/workspace.dsl`. It adds the
P070 Catala spike as a documentation-level element within the existing workspace
extension pattern. It does **not** add any new runtime container to the `openDebt`
software system.

```dsl
workspace extends architecture/workspace.dsl {

  model {

    # P070: Catala Compliance Spike — Forældelse G.A.2.4
    # Three research artifact file outputs — no runtime deployment

    catalaForaeldelse = softwareSystem "Catala Forældelse Spike (P070)" "Catala DSL" {
      tags "Spike" "CatalaCompliance" "ResearchArtifact" "internal"
      description "Formal Catala encoding of G.A.2.4 forældelses-regler. Six scopes: UdskydelsesBeregning (GIL §18a stk.1), ForaeldelseFristBeregning (GIL §18a stk.4 + FL §5), AfbrydelseValidering (GIL §18 stk.4 + §18a stk.8 + FL §18 stk.1 + SKM2015.718.ØLR), Fordringskompleks (GIL §18a stk.2), TillaegsfristBeregning (G.A.2.4.4.2), ForeløbigAfbrydelse (GIL §18a stk.7). Oracle artifact — no production deployment."
    }

    catalaForaeldelseTests = softwareSystem "Catala Forældelse Tests (P070)" "Catala DSL" {
      tags "Spike" "CatalaCompliance" "ResearchArtifact" "internal"
      description "Test suite for ga_2_4_foraeldelse.catala_da. 15 test cases covering FR-1 through FR-4 boundary cases, SKM2015.718.ØLR negative case, fordringskompleks propagation, and both max() formula branches."
    }

    catalaForaeldelseReport = softwareSystem "Catala Forældelse Spike Report (P070)" "Markdown" {
      tags "Spike" "CatalaCompliance" "ResearchArtifact" "internal"
      description "SPIKE-REPORT-070.md. Coverage table for all 29 P059 Gherkin scenarios. Gaps, discrepancies (5 hotspots), effort estimate, and Go/No-Go verdict."
    }

    # Oracle relationships to existing OpenDebt containers (specification-time only)
    catalaForaeldelse -> debtService "Provides prescription calculation oracle for" "Specification-time"
    catalaForaeldelse -> paymentService "Provides prescription-aware payment oracle for" "Specification-time"
    catalaForaeldelse -> caseworkerPortal "Provides caseworker prescription rules oracle for" "Specification-time"

    # Intra-spike artifact relationships
    catalaForaeldelseTests -> catalaForaeldelse "Tests and validates" "Catala test-doc"
    catalaForaeldelseReport -> catalaForaeldelse "Analyses and documents" "Go/No-Go verdict"
    catalaForaeldelseReport -> catalaForaeldelseTests "References test results from" "Coverage evidence"

  }

  views {

    filtered "SystemContext" include tag == "CatalaCompliance" "CatalaSpikes" "Catala Compliance Spike Artifacts (all petitions)"

    filtered "SystemContext" include tag == "Spike" "CatalaP070" "Catala P070 Forældelse Spike"

    styles {
      element "Spike" {
        background #c0e0ff
        shape Component
      }
      element "ResearchArtifact" {
        border dashed
      }
    }

  }

}
```

---

## Appendix A: File Header Convention (ART-070-1)

The following header comment block must appear at the top of
`catala/ga_2_4_foraeldelse.catala_da`, consistent with the P069 pattern
in `ga_2_3_2_1_daekningsraekkefoeigen.catala_da`:

```
# G.A.2.4 — Forældelse — GIL § 18a
(* Catala kildedialekt: catala_da
   G.A. snapshot v3.16, dated 2026-03-28
   Juridisk grundlag: GIL § 18a stk. 1–4, GIL § 18a stk. 7, Forældelsesl. § 5,
                      Forældelsesl. §§ 14–18, G.A.2.4.4.2,
                      SKM2015.718.ØLR (varsel ≠ afbrydelse)
   Formål: Formalisering af forældelsesfrist-reglerne i G.A.2.4,
            jf. GIL § 18a og Forældelsesl.
   Petition: P070 — Catala Compliance Spike — Forældelse G.A.2.4
   Status: Research spike — ingen produktionskode. *)
```

---

## Appendix B: Go/No-Go Gate Criteria (per ADR 0032)

The spike must emit **Go** if all of the following are true:
1. All five scopes encode without ambiguity in Catala DSL
2. ≥1 discrepancy found relative to P059 companion Gherkin (demonstrates oracle value)
3. ART-070-2 compiles without errors (`catala typecheck`)
4. OCaml or Python extraction produces runnable code (`catala ocaml` / `catala python`)
5. G.A.2.4.4.2 `tillaegsfristDage` is sufficiently specified (FLAG-B not triggered)

The spike must emit **No-Go** if any of the following are true:
1. Temporal rules cannot be expressed without workarounds in Catala
2. Legal text in G.A.2.4 is underspecified for a mandatory rule branch
3. Catala encoding effort exceeds 4 person-days per section (per ADR 0032)
4. FLAG-C: Fordringskompleks aggregate semantics cannot be expressed

A No-Go finding must document the specific blocker and block the P059
implementation petition until the blocker is resolved (legal clarification,
alternative encoding approach, or ADR amendment).
