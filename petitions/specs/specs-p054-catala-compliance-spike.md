# Implementation Specification — P054: Catala Compliance Spike

**Spec ID:** SPEC-P054  
**Petition:** `petitions/petition054-catala-compliance-spike.md`  
**Outcome contract:** `petitions/petition054-catala-compliance-spike-outcome-contract.md`  
**Feature file:** `petitions/petition054-catala-compliance-spike.feature`  
**Status:** Ready for implementation  
**Legal basis:** G.A.1.4.3, G.A.1.4.4, Gæld.bekendtg. § 7 stk. 1–2, GIL § 18 k  
**G.A. snapshot:** v3.16 (2026-01-30)  
**Spike type:** Research spike — no production code  
**Time box:** 2 working days  

> **Spike note:** All deliverables are files — Catala source programs, a Catala test suite, and
> a markdown spike report. No runtime behaviour, no new API surface, no database migrations,
> and no portal changes are introduced. Every specification element in this document maps to a
> named FR, NFR, or AC from Petition 054.

---

## §1 Overview

### 1.1 Spike type and time box

| Attribute | Value |
|-----------|-------|
| Spike type | Research spike |
| Time box | 2 working days |
| Output type | Files only (Catala source, test suite, markdown report) |
| Decision gate | Go/No-Go logged in `petitions/program-status.yaml` |

### 1.2 What Catala is and why it is relevant

Catala (from Inria and the French Ministry of Finance) is a domain-specific language for
encoding legislative and regulatory texts as executable programs. It uses a *literate
programming* style: natural-language article text and formal code blocks are interleaved in
the same source file, with each code block anchored to the specific article it formalises.
The source compiles to OCaml, Coq, or Python via the Catala CLI.

**Relevance to OpenDebt:** The juridisk vejledning (G.A. Inddrivelse) defines rules that
OpenDebt must implement. The current pipeline (petitions → Gherkin → specs) has no *formal,
executable representation of the law itself* — rules encoded in natural language and Gherkin
can silently diverge from the G.A. text. Catala programs anchored to G.A. article citations
can act as an *oracle*: generating authoritative test cases and flagging implementation gaps
that Gherkin alone cannot detect.

**Demonstrated feasibility:** Catala has been used to formalise the French Tax Code and UK
welfare rules. As noted in `docs/psrm-reference/Feasibility of Using Logic.md`, it
"represents legal norms in a formal syntax closely mirroring the statute text" and "bridges
the gap between legal text and algorithm, enabling lawyers to contribute to the code." The
spike hypothesis is that G.A.1.4.3 and G.A.1.4.4 — which contain exactly the exception
hierarchies and temporal rules where Catala provides the most value — can be encoded without
ambiguity at a sustainable effort level.

### 1.3 G.A. snapshot version

All G.A. article citations in Catala source files produced by this spike **must** reference:

```
G.A. snapshot v3.16, dated 2026-01-30
```

Both literal strings `v3.16` and `2026-01-30` must appear in the file header or in a comment
block at the top of each Catala source file. This matches the version referenced in Petition
053 (NFR-3; AC-15).

### 1.4 Deliverables

| ID | Artefact | Path | Petition FR |
|----|----------|------|-------------|
| D-1 | Catala source — opskrivning | `catala/ga_1_4_3_opskrivning.catala_da` | FR-1 |
| D-2 | Catala source — nedskrivning | `catala/ga_1_4_4_nedskrivning.catala_da` | FR-2 |
| D-3 | Catala test file | `catala/tests/ga_opskrivning_nedskrivning_tests.catala_da` | FR-3 |
| D-4 | Spike report | `catala/SPIKE-REPORT.md` | FR-4, FR-5 |

No file outside the `catala/` directory tree is created or modified by this spike (NFR-4).

---

## §2 FR-1 Specification — `catala/ga_1_4_3_opskrivning.catala_da`

**Source:** Petition 054 FR-1 · Outcome contract FR-1 · AC-1, AC-2, AC-3, AC-13, AC-14, AC-15  
**Legal basis:** G.A.1.4.3; Gæld.bekendtg. § 7, stk. 1, 3.–6. pkt.

### 2.1 File structure requirements

The file must:

1. Carry the Danish Catala dialect declaration as the first effective content (AC-3, AC-14).
2. Carry a header comment block that includes the literal strings `v3.16` and `2026-01-30`
   and identifies the G.A. section encoded (AC-15).
3. Declare a scope (or equivalent top-level construct) named `OpskrivningsfordringModtagelse`
   (or a Danish equivalent) containing the four modtagelsestidspunkt sub-rules as distinct,
   individually identifiable rule blocks.
4. Anchor each rule block to its exact Gæld.bekendtg. § 7 sub-paragraph citation using
   Catala's article-citation syntax (AC-2).

**Required file header pattern** (exact string content; comment delimiters may vary
[CATALA-SYNTAX-TBD] depending on installed Catala CLI version):

```
(* G.A.1.4.3 — Opskrivninger af fordringer — Modtagelsestidspunkt
   G.A. snapshot v3.16, dated 2026-01-30
   Legal basis: Gæld.bekendtg. § 7, stk. 1, 3.–6. pkt.
   Catala dialect: catala_da *)
```

**Dialect declaration** [CATALA-SYNTAX-TBD — verify exact form against installed CLI]:

```
> Module OpskrivningModtagelse
```

or via file extension `.catala_da`, which implicitly declares the Danish dialect. If the
installed Catala version requires an explicit `Language: Danish` pragma or similar header
line, the implementer must add it. The acceptance check (AC-3) is: the file, when passed to
`catala ocaml`, is parsed without a "wrong language" error.

### 2.2 Required scope declaration

The file must declare a computation scope capturing the inputs needed to determine
`modtagelsestidspunkt`. The following is the **suggested logical structure**; exact Catala
keyword spelling must be verified against the installed CLI version [CATALA-SYNTAX-TBD]:

```catala
(* [CATALA-SYNTAX-TBD] Verify 'declaration scope' vs 'scope' keyword *)
declaration scope OpskrivningsfordringModtagelse:
  context erFordringIHøring content boolean
  context erAnnulleretNedskrivning content boolean
  context erKrydssystemNedskrivning content boolean
  context originalFordringModtagelsestidspunkt content date
  context modtagelseISystemet content date
  context modtagelsestidspunkt content date   (* output — the legally-determined receipt timestamp *)
```

**Field semantics:**

| Field | Type | Meaning |
|-------|------|---------|
| `erFordringIHøring` | `boolean` | True if the opskrivningsfordring is in høring state at submission |
| `erAnnulleretNedskrivning` | `boolean` | True if this opskrivning reverses a prior nedskrivning |
| `erKrydssystemNedskrivning` | `boolean` | True when the reversed nedskrivning crossed system boundaries (PSRM ↔ external) |
| `originalFordringModtagelsestidspunkt` | `date` | The modtagelsestidspunkt of the original fordring (used in FR-1.3) |
| `modtagelseISystemet` | `date` | The timestamp at which the fordring was registered in modtagelsessystemet |
| `modtagelsestidspunkt` | `date` | **Output:** the legally correct modtagelsestidspunkt for this fordring |

### 2.3 FR-1.1 — Default rule (Gæld.bekendtg. § 7, stk. 1, 3. pkt.)

**Article reference:** Gæld.bekendtg. § 7, stk. 1, 3. pkt.  
**Anchor string that must appear in source** (used by AC-2 check): `Gæld.bekendtg. § 7, stk. 1, 3. pkt.`

**Rule in natural language (Danish):** En opskrivningsfordring anses for modtaget, når den er
registreret i modtagelsessystemet — medmindre en af undtagelserne i 4.–6. pkt. finder
anvendelse.

**Catala encoding guidance:**

The default rule assigns `modtagelsestidspunkt = modtagelseISystemet`. It is the base case
in the exception hierarchy and must be defined *without* a condition (it fires whenever no
exception applies). The article text from § 7, stk. 1, 3. pkt. must appear as prose
immediately before the code block [CATALA-SYNTAX-TBD — exact Catala literate block syntax]:

```catala
## § 7, stk. 1, 3. pkt. — Gæld.bekendtg. § 7, stk. 1, 3. pkt.

En opskrivningsfordring anses for modtaget, når den er registreret i modtagelsessystemet.

```catala
scope OpskrivningsfordringModtagelse:
  rule modtagelsestidspunkt equals modtagelseISystemet
```

**Expected output for this rule:**

| Input | `erFordringIHøring` | `erAnnulleretNedskrivning` | `erKrydssystemNedskrivning` | `modtagelseISystemet` | Expected `modtagelsestidspunkt` |
|-------|---------------------|----------------------------|-----------------------------|----------------------|--------------------------------|
| Default case | `false` | `false` | `false` | `2025-03-10` | `2025-03-10` |

### 2.4 FR-1.2 — Høring exception (Gæld.bekendtg. § 7, stk. 1, 4. pkt.)

**Article reference:** Gæld.bekendtg. § 7, stk. 1, 4. pkt.  
**Anchor string that must appear in source:** `Gæld.bekendtg. § 7, stk. 1, 4. pkt.`

**Rule in natural language (Danish):** Er opskrivningsfordringen under høring, anses den for
modtaget, når bekræftelsen eller rettelsen er registreret i modtagelsessystemet — ikke på
tidspunktet for den oprindelige indsendelse.

**Catala encoding guidance:**

This rule *overrides* the FR-1.1 default when `erFordringIHøring = true`. In Catala's
exception model the overriding rule must be marked as an exception to the base rule
[CATALA-SYNTAX-TBD — verify `exception` keyword and label syntax]:

```catala
## § 7, stk. 1, 4. pkt. — Gæld.bekendtg. § 7, stk. 1, 4. pkt.

Er opskrivningsfordringen under høring, anses den for modtaget når høringen afsluttes.

```catala
scope OpskrivningsfordringModtagelse:
  exception [label for FR-1.1 default rule]  (* [CATALA-SYNTAX-TBD] *)
  rule modtagelsestidspunkt under condition erFordringIHøring
    consequence equals modtagelseISystemet
    (* modtagelseISystemet here holds the høring resolution registration timestamp *)
```

**Note for implementer:** In this rule, `modtagelseISystemet` must represent the timestamp
of the *høring resolution registration*, not the original submission timestamp. The scope
declaration may need a separate input field `høringsafslutningTidspunkt` to distinguish the
two. If the implementer determines a separate field is needed, the scope declaration in §2.2
must be extended accordingly. Document the decision in the spike report.

**Expected output for this rule:**

| Input | `erFordringIHøring` | `erAnnulleretNedskrivning` | `modtagelseISystemet` | Expected `modtagelsestidspunkt` |
|-------|---------------------|----------------------------|-----------------------|--------------------------------|
| Høring case | `true` | `false` | `2025-04-02` (høring resolved) | `2025-04-02` |

### 2.5 FR-1.3 — Annulleret nedskrivning, same system (Gæld.bekendtg. § 7, stk. 1, 5. pkt.)

**Article reference:** Gæld.bekendtg. § 7, stk. 1, 5. pkt.  
**Anchor string that must appear in source:** `Gæld.bekendtg. § 7, stk. 1, 5. pkt.`

**Rule in natural language (Danish):** Er opskrivningsfordringen en omgørelse af en
nedskrivning inden for samme system, anses fordringen for modtaget på samme tidspunkt som
den oprindelige fordring.

**Catala encoding guidance:**

This rule overrides FR-1.1 when `erAnnulleretNedskrivning = true` AND
`erKrydssystemNedskrivning = false`. The output is `originalFordringModtagelsestidspunkt`,
not `modtagelseISystemet`. The article text must appear as prose before the code block
[CATALA-SYNTAX-TBD]:

```catala
## § 7, stk. 1, 5. pkt. — Gæld.bekendtg. § 7, stk. 1, 5. pkt.

Er der tale om omgørelse af en nedskrivning inden for samme system, anses
opskrivningsfordringen for modtaget på samme tidspunkt som den oprindelige fordring.

```catala
scope OpskrivningsfordringModtagelse:
  exception [label for FR-1.1 default rule]  (* [CATALA-SYNTAX-TBD] *)
  rule modtagelsestidspunkt
    under condition erAnnulleretNedskrivning and (not erKrydssystemNedskrivning)
    consequence equals originalFordringModtagelsestidspunkt
```

**Expected output for this rule:**

| Input | `erAnnulleretNedskrivning` | `erKrydssystemNedskrivning` | `originalFordringModtagelsestidspunkt` | `modtagelseISystemet` | Expected `modtagelsestidspunkt` |
|-------|----------------------------|-----------------------------|----------------------------------------|-----------------------|--------------------------------|
| Same-system annullering | `true` | `false` | `2024-11-15` | `2025-03-10` | `2024-11-15` |

### 2.6 FR-1.4 — Annulleret nedskrivning, cross-system (Gæld.bekendtg. § 7, stk. 1, 6. pkt.)

**Article reference:** Gæld.bekendtg. § 7, stk. 1, 6. pkt.  
**Anchor string that must appear in source:** `Gæld.bekendtg. § 7, stk. 1, 6. pkt.`

**Rule in natural language (Danish):** Er opskrivningsfordringen en omgørelse af en
nedskrivning på tværs af systemer, anses fordringen *altid* for modtaget på
registreringstidspunktet i modtagelsessystemet — uanset hvornår den ursprunglige fordring
blev modtaget.

**Catala encoding guidance:**

This rule overrides FR-1.1 (and FR-1.3, which could otherwise partially apply) when
`erAnnulleretNedskrivning = true` AND `erKrydssystemNedskrivning = true`. The word "altid"
("always") in the G.A. text makes this a stronger exception than FR-1.3. In Catala's
exception hierarchy this must be modelled such that this rule takes priority over both the
default and the same-system rule [CATALA-SYNTAX-TBD — verify nested exception priority]:

```catala
## § 7, stk. 1, 6. pkt. — Gæld.bekendtg. § 7, stk. 1, 6. pkt.

Er der tale om omgørelse af en nedskrivning på tværs af systemer, anses
opskrivningsfordringen altid for modtaget ved registrering i modtagelsessystemet.

```catala
scope OpskrivningsfordringModtagelse:
  exception [label for FR-1.1 default rule]  (* [CATALA-SYNTAX-TBD] *)
  rule modtagelsestidspunkt
    under condition erAnnulleretNedskrivning and erKrydssystemNedskrivning
    consequence equals modtagelseISystemet
```

**Expected output for this rule:**

| Input | `erAnnulleretNedskrivning` | `erKrydssystemNedskrivning` | `originalFordringModtagelsestidspunkt` | `modtagelseISystemet` | Expected `modtagelsestidspunkt` |
|-------|----------------------------|-----------------------------|----------------------------------------|-----------------------|--------------------------------|
| Cross-system annullering | `true` | `true` | `2024-11-15` (ignored) | `2025-03-10` | `2025-03-10` |

---

## §3 FR-2 Specification — `catala/ga_1_4_4_nedskrivning.catala_da`

**Source:** Petition 054 FR-2 · Outcome contract FR-2 · AC-4, AC-5, AC-13, AC-14, AC-15  
**Legal basis:** G.A.1.4.4; Gæld.bekendtg. § 7, stk. 2, nr. 1–3; GIL § 18 k

### 3.1 File structure requirements

The file must:

1. Carry the Danish Catala dialect declaration (AC-14).
2. Carry a header comment block including `v3.16` and `2026-01-30` and the G.A.1.4.4 section
   identifier (AC-15).
3. Declare an enumeration type `NedskrivningsGrund` with exactly three variants (FR-2.1).
4. Declare a scope (or equivalent) named `Nedskrivning` containing rule blocks for each of
   FR-2.1 through FR-2.4, each anchored to its article (AC-5).

**Required file header pattern:**

```
(* G.A.1.4.4 — Nedskrivninger af fordringer
   G.A. snapshot v3.16, dated 2026-01-30
   Legal basis: Gæld.bekendtg. § 7, stk. 2, nr. 1–3; GIL § 18 k
   Catala dialect: catala_da *)
```

### 3.2 Required enumeration declaration — `NedskrivningsGrund`

**Anchor string:** `Gæld.bekendtg. § 7, stk. 2`

The three valid nedskrivningsgrunde must be declared as a Catala enumeration type. The
variant names must match the string constants used in the OpenDebt domain
(`NED_INDBETALING`, `NED_FEJL_OVERSENDELSE`, `NED_GRUNDLAG_AENDRET`), or map to them via
comment annotation [CATALA-SYNTAX-TBD — verify enumeration declaration syntax]:

```catala
## § 7, stk. 2 — Gæld.bekendtg. § 7, stk. 2

Nedskrivning kan ske på tre gyldige grundlag.

```catala
declaration enumeration NedskrivningsGrund:
  -- NedIndbetaling      (* Gæld.bekendtg. § 7, stk. 2, nr. 1: Direkte indbetaling til fordringshaver *)
  -- NedFejlOversendelse (* Gæld.bekendtg. § 7, stk. 2, nr. 2: Fejlagtig oversendelse til inddrivelse *)
  -- NedGrundlagAendret  (* Gæld.bekendtg. § 7, stk. 2, nr. 3: Opkrævningsgrundlaget har ændret sig *)
```

### 3.3 Required scope declaration

```catala
declaration scope Nedskrivning:
  context grund content NedskrivningsGrund    (* the submitted nedskrivningsgrund; may be absent *)
  context grundErGyldig content boolean       (* derived: true iff grund is one of the three legal variants *)
  context virkningsdato content date          (* the submitted effective date *)
  context fordringModtagelsestidspunkt content date  (* PSRM registration date of the fordring *)
  context erRetroaktiv content boolean        (* true iff virkningsdato < fordringModtagelsestidspunkt *)
  context gilParagraf18kSuspensionKrævet content boolean  (* output: GIL § 18 k suspension flag *)
  context nedskrivningGodkendt content boolean            (* output: overall acceptance flag *)
```

**Field semantics:**

| Field | Type | Meaning |
|-------|------|---------|
| `grund` | `NedskrivningsGrund` | Submitted reason code; validation checks this is populated |
| `grundErGyldig` | `boolean` | Derived: true when `grund` is a recognised `NedskrivningsGrund` variant |
| `virkningsdato` | `date` | Submitted effective date of the nedskrivning |
| `fordringModtagelsestidspunkt` | `date` | PSRM registration date (`DebtEntity.receivedAt`) |
| `erRetroaktiv` | `boolean` | Derived: `virkningsdato < fordringModtagelsestidspunkt` |
| `gilParagraf18kSuspensionKrævet` | `boolean` | Output: GIL § 18 k suspension required |
| `nedskrivningGodkendt` | `boolean` | Output: nedskrivning is valid for processing |

### 3.4 FR-2.1 — Three valid nedskrivningsgrunde (Gæld.bekendtg. § 7, stk. 2, nr. 1–3)

**Article references:**
- `Gæld.bekendtg. § 7, stk. 2, nr. 1` — Direkte indbetaling til fordringshaver
- `Gæld.bekendtg. § 7, stk. 2, nr. 2` — Fejlagtig oversendelse til inddrivelse
- `Gæld.bekendtg. § 7, stk. 2, nr. 3` — Opkrævningsgrundlaget har ændret sig

All three anchor strings must appear in the source (one per article sub-paragraph).

**Rule in natural language (Danish):** Nedskrivning kan alene ske på et af de tre grundlag
fastsat i Gæld.bekendtg. § 7, stk. 2, nr. 1–3. `grundErGyldig` er `sand` (true), når
`grund` er en af de tre lovlige varianter.

**Catala encoding guidance:** Each nr. should be a labelled prose section with its own code
block. The `grundErGyldig` derivation may be expressed as a match expression or three
separate rule clauses [CATALA-SYNTAX-TBD — verify pattern match syntax]:

```catala
## § 7, stk. 2, nr. 1 — Gæld.bekendtg. § 7, stk. 2, nr. 1

Direkte indbetaling til fordringshaver.

```catala
scope Nedskrivning:
  rule grundErGyldig under condition grund = NedIndbetaling
    consequence equals true

## § 7, stk. 2, nr. 2 — Gæld.bekendtg. § 7, stk. 2, nr. 2

Fejlagtig oversendelse til inddrivelse.

```catala
scope Nedskrivning:
  exception [label for nr. 1 rule]  (* [CATALA-SYNTAX-TBD] *)
  rule grundErGyldig under condition grund = NedFejlOversendelse
    consequence equals true

## § 7, stk. 2, nr. 3 — Gæld.bekendtg. § 7, stk. 2, nr. 3

Opkrævningsgrundlaget har ændret sig.

```catala
scope Nedskrivning:
  exception [label for nr. 1 rule]  (* [CATALA-SYNTAX-TBD] *)
  rule grundErGyldig under condition grund = NedGrundlagAendret
    consequence equals true
```

**Default for `grundErGyldig`** (no explicit legal ground matched):

```catala
scope Nedskrivning:
  rule grundErGyldig equals false    (* base case: no valid ground → not valid *)
```

### 3.5 FR-2.2 — Virkningsdato retroactivity (G.A.1.4.4)

**Article reference (prose anchor in source):** `G.A.1.4.4` — virkningsdato-reglen

**Rule in natural language (Danish):** En nedskrivning er retroaktiv, når virkningsdato er
*tidligere end* fordringens modtagelsestidspunkt i PSRM. `erRetroaktiv` er `sand` (true)
når `virkningsdato < fordringModtagelsestidspunkt`.

**Edge cases that the encoding must handle:**

| Condition | Expected `erRetroaktiv` |
|-----------|------------------------|
| `virkningsdato < fordringModtagelsestidspunkt` | `true` |
| `virkningsdato = fordringModtagelsestidspunkt` (same day) | `false` |
| `virkningsdato > fordringModtagelsestidspunkt` | `false` |

**Catala encoding guidance** [CATALA-SYNTAX-TBD — verify date comparison operator]:

```catala
## Virkningsdato — G.A.1.4.4

Nedskrivningens virkningsdato afgør, om der er tale om en retroaktiv nedskrivning.

```catala
scope Nedskrivning:
  rule erRetroaktiv equals
    virkningsdato <@ fordringModtagelsestidspunkt
    (* [CATALA-SYNTAX-TBD] date comparison operator: <@, before, or < *)
```

**Boundary-date behaviour:** The same-day case (`virkningsdato = fordringModtagelsestidspunkt`)
must produce `erRetroaktiv = false`. The encoding must use a *strict* less-than comparison.

### 3.6 FR-2.3 — GIL § 18 k suspension flag

**Article reference (prose anchor in source):** `GIL § 18 k`

**Rule in natural language (Danish):** Er nedskrivningen retroaktiv, og kan den ikke
gennemføres øjeblikkeligt (fordi den overskrider systemgrænser), udløser den en suspension
i henhold til GIL § 18 k. `gilParagraf18kSuspensionKrævet` er `sand` (true) netop når
`erRetroaktiv = true`.

> **Implementer note:** The petition and outcome contract define the flag as `true` when
> `virkningsdato < fordring.receivedAt` and the nedskrivning cannot be completed immediately.
> For the purposes of the Catala encoding, the "cannot be completed immediately" condition is
> modelled as equivalent to the retroactivity condition itself — the spike does not receive
> a live completion-status signal from PSRM. Document this simplification explicitly in the
> spike report.

**Catala encoding guidance:**

```catala
## GIL § 18 k — GIL § 18 k

Suspension ved retroaktiv nedskrivning over systemgrænser.

```catala
scope Nedskrivning:
  rule gilParagraf18kSuspensionKrævet equals erRetroaktiv
```

**Expected outputs:**

| `erRetroaktiv` | Expected `gilParagraf18kSuspensionKrævet` |
|----------------|------------------------------------------|
| `true` | `true` |
| `false` | `false` |

### 3.7 FR-2.4 — Invalid grund rejection

**Article reference:** Implied by Gæld.bekendtg. § 7, stk. 2 (closed list of valid grounds)

**Rule in natural language (Danish):** En nedskrivning uden gyldigt grundlag skal afvises.
Når `grundErGyldig = false`, er `nedskrivningGodkendt = false`.

**Edge cases:**
- `grund` is absent / null equivalent (no value supplied) → `grundErGyldig = false` →
  `nedskrivningGodkendt = false`.
- `grund` is an unrecognised value → `grundErGyldig = false` → `nedskrivningGodkendt = false`.

> **Null-grund handling [CATALA-SYNTAX-TBD]:** Catala's type system may or may not support
> an optional/nullable type for enumeration values. If `NedskrivningsGrund` cannot represent
> absence natively, the implementer must either (a) add a `-- UkendelGrund` sentinel variant
> and treat it as invalid, or (b) add a separate boolean `grundErAngivet` context field. The
> chosen approach must be documented in the spike report.

**Catala encoding guidance:**

```catala
scope Nedskrivning:
  rule nedskrivningGodkendt equals grundErGyldig
  (* nedskrivningGodkendt is true iff a valid grund was supplied *)
```

**Expected outputs:**

| `grund` value | `grundErGyldig` | `nedskrivningGodkendt` |
|---------------|-----------------|------------------------|
| `NedIndbetaling` | `true` | `true` |
| `NedFejlOversendelse` | `true` | `true` |
| `NedGrundlagAendret` | `true` | `true` |
| absent / null / sentinel | `false` | `false` |

---

## §4 FR-3 Specification — `catala/tests/ga_opskrivning_nedskrivning_tests.catala_da`

**Source:** Petition 054 FR-3 · Outcome contract FR-3 · AC-6, AC-7  
**Verification command (AC from outcome contract):**

```bash
catala test-doc catala/tests/ga_opskrivning_nedskrivning_tests.catala_da
# Expected exit code: 0; all tests report PASS
```

### 4.1 Test file structure requirements

1. The file imports (or references) both D-1 and D-2 source files [CATALA-SYNTAX-TBD —
   verify Catala module import syntax for test files].
2. All tests are expressed using Catala's built-in `Test` module or equivalent assertion
   mechanism [CATALA-SYNTAX-TBD — verify exact test declaration syntax for installed version].
3. The file contains **at minimum 8 distinct test cases**, each named and covering a distinct
   rule branch.

**General test structure pattern** [CATALA-SYNTAX-TBD — exact syntax depends on CLI version]:

```catala
(* Test file: ga_opskrivning_nedskrivning_tests.catala_da
   Tests for D-1 (GA 1.4.3 opskrivning) and D-2 (GA 1.4.4 nedskrivning)
   G.A. snapshot v3.16, dated 2026-01-30 *)

## Test: <TestName>

```catala
scope <ScopeName>:
  assertion <field> = <expected_value>
```

### 4.2 Required test cases (minimum 8)

The following 11 test cases are required. TC-1 through TC-4 cover FR-1 sub-rules;
TC-5 through TC-10 cover FR-2 sub-rules; TC-11 covers the mandated day-after boundary case
(AC-7; Gherkin FR-3 scenario explicitly requires same day, day before, and day after).
Implementing all 11 satisfies AC-7 (minimum 8) and the Gherkin's three-boundary requirement.

---

#### TC-1 — Default modtagelsestidspunkt (FR-1.1)

**Test name:** `TC-1_Default_Modtagelsestidspunkt`  
**FR covered:** FR-1.1 (Gæld.bekendtg. § 7, stk. 1, 3. pkt.)

| Input field | Value |
|-------------|-------|
| `erFordringIHøring` | `false` |
| `erAnnulleretNedskrivning` | `false` |
| `erKrydssystemNedskrivning` | `false` |
| `modtagelseISystemet` | `2025-03-10` |
| `originalFordringModtagelsestidspunkt` | `2025-03-10` (irrelevant, same) |

| Output field | Expected value |
|--------------|----------------|
| `modtagelsestidspunkt` | `2025-03-10` |

---

#### TC-2 — Høring exception (FR-1.2)

**Test name:** `TC-2_Hoering_Exception`  
**FR covered:** FR-1.2 (Gæld.bekendtg. § 7, stk. 1, 4. pkt.)

| Input field | Value |
|-------------|-------|
| `erFordringIHøring` | `true` |
| `erAnnulleretNedskrivning` | `false` |
| `erKrydssystemNedskrivning` | `false` |
| `modtagelseISystemet` | `2025-04-02` (høring resolution date) |

| Output field | Expected value |
|--------------|----------------|
| `modtagelsestidspunkt` | `2025-04-02` |

---

#### TC-3 — Annulleret nedskrivning, same system (FR-1.3)

**Test name:** `TC-3_AnnulleretNedskrivning_SammeSystem`  
**FR covered:** FR-1.3 (Gæld.bekendtg. § 7, stk. 1, 5. pkt.)

| Input field | Value |
|-------------|-------|
| `erFordringIHøring` | `false` |
| `erAnnulleretNedskrivning` | `true` |
| `erKrydssystemNedskrivning` | `false` |
| `originalFordringModtagelsestidspunkt` | `2024-11-15` |
| `modtagelseISystemet` | `2025-03-10` |

| Output field | Expected value |
|--------------|----------------|
| `modtagelsestidspunkt` | `2024-11-15` |

---

#### TC-4 — Annulleret nedskrivning, cross-system (FR-1.4)

**Test name:** `TC-4_AnnulleretNedskrivning_Krydssystem`  
**FR covered:** FR-1.4 (Gæld.bekendtg. § 7, stk. 1, 6. pkt.)

| Input field | Value |
|-------------|-------|
| `erFordringIHøring` | `false` |
| `erAnnulleretNedskrivning` | `true` |
| `erKrydssystemNedskrivning` | `true` |
| `originalFordringModtagelsestidspunkt` | `2024-11-15` (must be ignored) |
| `modtagelseISystemet` | `2025-03-10` |

| Output field | Expected value |
|--------------|----------------|
| `modtagelsestidspunkt` | `2025-03-10` |

---

#### TC-5 — Valid grund NED_INDBETALING (FR-2.1, nr. 1)

**Test name:** `TC-5_GyldigGrund_NedIndbetaling`  
**FR covered:** FR-2.1 (Gæld.bekendtg. § 7, stk. 2, nr. 1)

| Input field | Value |
|-------------|-------|
| `grund` | `NedIndbetaling` |
| `virkningsdato` | `2025-03-10` |
| `fordringModtagelsestidspunkt` | `2025-03-10` |

| Output field | Expected value |
|--------------|----------------|
| `grundErGyldig` | `true` |
| `nedskrivningGodkendt` | `true` |
| `erRetroaktiv` | `false` |

---

#### TC-6 — Valid grund NED_FEJL_OVERSENDELSE (FR-2.1, nr. 2)

**Test name:** `TC-6_GyldigGrund_NedFejlOversendelse`  
**FR covered:** FR-2.1 (Gæld.bekendtg. § 7, stk. 2, nr. 2)

| Input field | Value |
|-------------|-------|
| `grund` | `NedFejlOversendelse` |
| `virkningsdato` | `2025-05-01` |
| `fordringModtagelsestidspunkt` | `2025-03-10` |

| Output field | Expected value |
|--------------|----------------|
| `grundErGyldig` | `true` |
| `nedskrivningGodkendt` | `true` |
| `erRetroaktiv` | `false` |

---

#### TC-7 — Valid grund NED_GRUNDLAG_AENDRET (FR-2.1, nr. 3)

**Test name:** `TC-7_GyldigGrund_NedGrundlagAendret`  
**FR covered:** FR-2.1 (Gæld.bekendtg. § 7, stk. 2, nr. 3)

| Input field | Value |
|-------------|-------|
| `grund` | `NedGrundlagAendret` |
| `virkningsdato` | `2025-01-01` |
| `fordringModtagelsestidspunkt` | `2025-03-10` |

| Output field | Expected value |
|--------------|----------------|
| `grundErGyldig` | `true` |
| `nedskrivningGodkendt` | `true` |
| `erRetroaktiv` | `true` |
| `gilParagraf18kSuspensionKrævet` | `true` |

---

#### TC-8 — Invalid grund rejection (FR-2.4)

**Test name:** `TC-8_UgyldigGrund_Afvist`  
**FR covered:** FR-2.4 (validation; Gæld.bekendtg. § 7, stk. 2 closed list)

| Input field | Value |
|-------------|-------|
| `grund` | absent / null / sentinel `UkendelGrund` [CATALA-SYNTAX-TBD] |
| `virkningsdato` | `2025-03-10` |
| `fordringModtagelsestidspunkt` | `2025-03-10` |

| Output field | Expected value |
|--------------|----------------|
| `grundErGyldig` | `false` |
| `nedskrivningGodkendt` | `false` |

---

#### TC-9 — GIL § 18 k suspension flag, boundary: day before fordring.receivedAt (FR-2.2, FR-2.3)

**Test name:** `TC-9_Retroaktiv_DagenFoer_Modtagelse`  
**FR covered:** FR-2.2, FR-2.3 (boundary-date test: one day before)

| Input field | Value |
|-------------|-------|
| `grund` | `NedIndbetaling` |
| `virkningsdato` | `2025-03-09` |
| `fordringModtagelsestidspunkt` | `2025-03-10` |

| Output field | Expected value |
|--------------|----------------|
| `erRetroaktiv` | `true` (`2025-03-09 < 2025-03-10`) |
| `gilParagraf18kSuspensionKrævet` | `true` |

---

#### TC-10 — GIL § 18 k suspension flag, boundary: same day as fordring.receivedAt (FR-2.2, FR-2.3)

**Test name:** `TC-10_IkkeRetroaktiv_Sammedag_Modtagelse`  
**FR covered:** FR-2.2, FR-2.3 (boundary-date test: same day — must produce `false`)

| Input field | Value |
|-------------|-------|
| `grund` | `NedIndbetaling` |
| `virkningsdato` | `2025-03-10` |
| `fordringModtagelsestidspunkt` | `2025-03-10` |

| Output field | Expected value |
|--------------|----------------|
| `erRetroaktiv` | `false` (`2025-03-10` is not *before* `2025-03-10`) |
| `gilParagraf18kSuspensionKrævet` | `false` |

> **Boundary rationale:** TC-9 covers day-before (retroactive); TC-10 covers same day (not
> retroactive). TC-7 covers a further-before retroactive case — it is NOT a day-after boundary.
> TC-11 below covers the day-after case (`virkningsdato > fordringModtagelsestidspunkt`),
> which the Gherkin FR-3 scenario explicitly mandates.

---

#### TC-11 — GIL § 18 k suspension flag, boundary: day after fordring.receivedAt (FR-2.2, FR-2.3)

**Test name:** `TC-11_IkkeRetroaktiv_DagenEfter_Modtagelse`
**FR covered:** FR-2.2, FR-2.3 (boundary-date test: one day after — must produce `false`)

| Input field | Value |
|-------------|-------|
| `grund` | `NedIndbetaling` |
| `virkningsdato` | `2025-03-11` |
| `fordringModtagelsestidspunkt` | `2025-03-10` |

| Output field | Expected value |
|--------------|----------------|
| `erRetroaktiv` | `false` (`2025-03-11` is not before `2025-03-10`) |
| `gilParagraf18kSuspensionKrævet` | `false` |

### 4.3 Test naming convention

Each test case heading in the Catala file must include the TC identifier so that the
`catala test-doc` output can be matched to test names in the spike report. Pattern:
`TC-<N>_<CamelCaseName>`.

---

## §5 FR-4 Specification — `catala/SPIKE-REPORT.md`

**Source:** Petition 054 FR-4 · Outcome contract FR-4 · AC-8, AC-9, AC-10  
**Legal basis:** Comparison against P053 Gherkin scenarios from
`petitions/petition053-fordringshaverportal-opskrivning-nedskrivning-fuld-spec.feature`

### 5.1 Required sections

The spike report must contain the following sections. Sections 1–4 are required by AC-10
(FR-4). Section 5 is required by FR-5 / AC-11. Section numbering is a spec-level convention
to aid reviewer orientation.

1. **Coverage Table** — maps every in-scope P053 scenario to a coverage status (see §5.2).
2. **Gaps** — lists Catala rule branches not covered by any P053 scenario, or states
   "None found" (see §5.3).
3. **Discrepancies** — lists cases where a P053 scenario appears to contradict the G.A. text
   as formalised in Catala, or states "None found" (see §5.4).
4. **Effort Estimate** — person-day estimate with rationale (see §5.5).
5. **Go/No-Go** — verdict and evidence (specified in §6; required by FR-5 / AC-11).

### 5.2 Coverage table requirements (AC-9)

The coverage table must have exactly these columns:

| P053 scenario | P053 FR section | Catala coverage status | Notes |
|---------------|-----------------|------------------------|-------|

**Column definitions:**

- **P053 scenario:** The exact scenario name (or Scenario Outline title + example row) from
  `petition053-fordringshaverportal-opskrivning-nedskrivning-fuld-spec.feature`.
- **P053 FR section:** The FR label from that feature file (`FR-1`, `FR-2`, etc.).
- **Catala coverage status:** One of `Covered`, `Not covered`, or `Discrepancy found`.
- **Notes:** Brief explanation — which Catala test covers it, or why it is out of scope.

**Required rows (P053 FR-1 and FR-2 scenarios):**

The following P053 scenarios are in scope for the coverage table. The implementer must
include a row for each:

**From P053 FR-1 (Nedskrivning — kontrolleret årsagsvalg, Gæld.bekendtg. § 7, stk. 2):**

| Scenario name | Outline example (if applicable) |
|---------------|---------------------------------|
| Nedskrivningsformular viser præcis tre lovlige årsagskoder | — |
| Nedskrivning med gyldig årsagskode sendes korrekt til debt-service | reasonCode: NED_INDBETALING |
| Nedskrivning med gyldig årsagskode accepteres for alle tre koder | example: NED_FEJL_OVERSENDELSE |
| Nedskrivning med gyldig årsagskode accepteres for alle tre koder | example: NED_GRUNDLAG_AENDRET |
| Nedskrivning uden valgt årsagskode afvises af portalen inden BFF-kald | — |
| Nedskrivning med ugyldig årsagskode afvises af portalen | — |

**From P053 FR-2 (Opskrivning — rentefordring-undtagelse, G.A.1.4.3, 3. pkt.):**

| Scenario name |
|---------------|
| Opskrivning på rentefordring afvises med vejledningsbesked |
| Opskrivning på ikke-rentefordring tillades |

> **Coverage rationale note:** P053 FR-2 concerns the *rentefordring* exception (G.A.1.4.3,
> 3. pkt.), which is explicitly out of scope for the P054 Catala encoding (P054 encodes only
> the modtagelsestidspunkt rules, not the rentefordring exception). The implementer should
> mark these rows as `Not covered` with the note "Rentefordring exception is out of scope for
> P054 Catala encoding."

### 5.3 Gaps section requirements

For each Catala rule branch (scope rule, enumeration variant, or exception path) in D-1 or
D-2 that has **no corresponding P053 FR-1 or FR-2 Gherkin scenario**, the report must list:

- The Catala rule or variant identifier.
- The G.A. article citation it derives from.
- A one-sentence explanation of why the Gherkin does not cover it.

If no gaps exist, the section must state explicitly: "None found."

**Known candidate gaps to investigate** (implementer must confirm or refute during spike):

| Candidate gap | Catala rule | G.A. citation |
|---------------|-------------|---------------|
| Cross-system annulleret nedskrivning rule | FR-1.4 | Gæld.bekendtg. § 7, stk. 1, 6. pkt. |
| Same-system annulleret nedskrivning rule | FR-1.3 | Gæld.bekendtg. § 7, stk. 1, 5. pkt. |
| Høring modtagelsestidspunkt rule | FR-1.2 | Gæld.bekendtg. § 7, stk. 1, 4. pkt. |
| GIL § 18 k suspension — false case (virkningsdato ≥ receivedAt) | FR-2.3 | GIL § 18 k |

These are candidates because the P053 feature file does not contain scenarios that directly
assert the *modtagelsestidspunkt value* for these cases as a backend computation — it asserts
portal behaviour (banners, advisories) rather than the underlying date computation.

### 5.4 Discrepancies section requirements

A discrepancy is any case where the Catala formal encoding produces a result that *cannot* be
reconciled with a P053 Gherkin scenario without one of them being incorrect.

For each discrepancy, document:
- The P053 scenario name and the specific step that conflicts.
- The Catala rule and G.A. citation that contradicts it.
- The specific input values that expose the contradiction.
- A proposed resolution (amend the Gherkin, amend the Catala encoding, or escalate to legal
  review).

If no discrepancies are found, the section must state: "None found."

### 5.5 Effort estimate section requirements

The effort estimate must:

1. State the estimated encoding effort for D-1 and D-2.
2. State the **number of G.A. rule branches** encoded in D-1 and D-2 (4 + 4 = 8 minimum).
3. State the estimated number of G.A. Inddrivelse rule branches to be encoded (implementer
   must estimate this from the chapter structure during the spike).
4. Provide a person-day estimate with rationale explaining how the estimate was derived.
5. State whether the per-section effort exceeds the 4-person-day No-Go threshold (FR-5).

---

## §6 FR-5 Specification — Go/No-Go criteria

**Source:** Petition 054 FR-5 · Outcome contract FR-5 · AC-11, AC-12  
**Section title in report:** "Go/No-Go Recommendation"

The Go/No-Go section must contain:
- An unambiguous verdict: the word **Go** or **No-Go** in a heading or bold text (AC-11).
- Evidence for every criterion listed below (AC-12).

### 6.1 Go criteria — all four must be met for a Go verdict

#### Go criterion G-1: All 4 modtagelsestidspunkt sub-rules encoded without ambiguity

**Evidence required:**
- State `Yes` or `No`.
- If `Yes`: identify the Catala scope and rule names used for FR-1.1 through FR-1.4.
- If `No`: cite the specific sub-rule that could not be encoded without workaround, and
  describe the workaround and why it introduces ambiguity.

**Example of sufficient "Yes" evidence:**
> "All four modtagelsestidspunkt rules are encoded in scope `OpskrivningsfordringModtagelse`
> as rules `defaultModtagelse`, `hoeringUndtagelse`, `sammeSytemAnnullering`, and
> `krydssystemAnnullering`. No syntax placeholders remain. The Catala exception hierarchy
> handles the precedence of FR-1.4 over FR-1.3 via nested exception labels."

#### Go criterion G-2: At least 1 gap or discrepancy found relative to P053 Gherkin

**Evidence required:**
- State `Yes` or `No`.
- If `Yes`: cite at least one gap or discrepancy finding from the coverage table and describe
  what it demonstrates about the value of the Catala encoding.
- If `No`: state explicitly "No gaps or discrepancies were found" and assess whether this
  indicates (a) the Gherkin is already comprehensive, or (b) the Catala encoding is
  insufficiently precise to reveal gaps.

#### Go criterion G-3: Catala test compilation succeeds without errors

**Evidence required:**
- State `Yes` or `No`.
- Include the exact command run and its output (or last 20 lines if output is long).
- Include the exit code.

**Example of sufficient "Yes" evidence:**
```
Command: catala test-doc catala/tests/ga_opskrivning_nedskrivning_tests.catala_da
Exit code: 0
Output (last 5 lines):
  [PASS] TC-1_Default_Modtagelsestidspunkt
  [PASS] TC-2_Hoering_Exception
  ...
  10/10 tests passed
```

#### Go criterion G-4: OCaml or Python extraction produces runnable code

**Evidence required:**
- State `Yes` or `No`.
- Include the exact extraction command(s) run:

```bash
catala ocaml catala/ga_1_4_3_opskrivning.catala_da
catala ocaml catala/ga_1_4_4_nedskrivning.catala_da
# or
catala python catala/ga_1_4_3_opskrivning.catala_da
catala python catala/ga_1_4_4_nedskrivning.catala_da
```

- State the exit code of each command.
- Confirm whether the generated output file (`.ml`, `.mli`, or `.py`) is syntactically valid
  (e.g., by running `ocaml` or `python -c` against it).

### 6.2 No-Go criteria — any one triggers a No-Go verdict

#### No-Go trigger N-1: Temporal rules cannot be expressed without workarounds

**Trigger condition:** Any of the four modtagelsestidspunkt sub-rules in FR-1 or the
`virkningsdato` retroactivity rule in FR-2.2 required an encoding workaround that introduces
ambiguity or requires runtime logic outside of Catala.

**Evidence required:**
- State `Triggered` or `Not triggered`.
- If `Triggered`: describe the specific temporal rule, the attempted encoding, the reason it
  fails or requires a workaround, and describe the specific temporal rule, the attempted
  encoding, and the reason it fails.

#### No-Go trigger N-2: Legal ambiguities in G.A. text block formal encoding

**Trigger condition:** Any G.A.1.4.3 or G.A.1.4.4 rule is underspecified in the G.A. text
to the point that multiple distinct Catala encodings are equally valid and produce different
outputs — and the G.A. text provides no basis for choosing between them.

**Evidence required:**
- State `Triggered` or `Not triggered`.
- If `Triggered`: cite the specific G.A. reference (article, paragraph, and sentence),
  describe the ambiguity, and document the two or more conflicting encodings with their
  different outputs.

#### No-Go trigger N-3: Encoding effort per G.A. section exceeds 4 person-days

**Trigger condition:** The effort estimate (§5.6) produces a per-G.A.-section estimate that
exceeds 4 person-days (8 sections × 4 pd = 32 pd total threshold for the chapter).

**Evidence required:**
- State `Triggered` or `Not triggered`.
- Include the per-section effort figure from the effort estimate and the threshold comparison.

---

## §7 NFR Specifications

**Source:** Petition 054 NFR-1 through NFR-4 · AC-13 through AC-16

### 7.1 NFR-1 — Compilation

**Requirement (from petition):** The Catala source must compile without errors using the
Catala CLI (`catala ocaml` or `catala python` extraction).

**Compilation commands and expected exit codes:**

```bash
# Run from repository root or from catala/ directory:
cd catala
catala ocaml ga_1_4_3_opskrivning.catala_da   # must exit 0
catala ocaml ga_1_4_4_nedskrivning.catala_da   # must exit 0
catala test-doc tests/ga_opskrivning_nedskrivning_tests.catala_da  # must exit 0
```

**Non-zero exit code is a hard failure** (AC-13). The spike report must record the actual
exit codes as evidence for Go criterion G-3 and G-4.

If the `catala` CLI is not on PATH, the implementer must install it (see
[https://catala-lang.org/en/install](https://catala-lang.org/en/install)) before starting
the spike. The spike does not manage CLI installation; the installed CLI version must be
documented in the spike report Introduction.

### 7.2 NFR-2 — Danish dialect declaration

**Requirement (from petition):** The Catala source must be in Danish (`catala_da`) to match
the G.A. source language.

**Compliance definition:** A source file satisfies NFR-2 when the Catala CLI parses it
without a dialect mismatch error. The `.catala_da` file extension signals the Danish dialect
to the CLI. Any additional in-file dialect pragma required by the installed version must
also be present [CATALA-SYNTAX-TBD — verify against installed CLI].

**Prohibited:** No English-dialect Catala source file (`.catala_en`) is produced by this
spike (AC-14).

**Verification:** Both D-1 and D-2 files must carry the `.catala_da` extension. Both must
compile without a dialect error.

### 7.3 NFR-3 — G.A. snapshot version citation format

**Requirement (from petition):** All G.A. article citations must reference the G.A. snapshot
version used in Petition 053 (version 3.16, dated 2026-01-30).

**Required literal strings:** Both `v3.16` and `2026-01-30` must appear as character
sequences in the header comment of each Catala source file (D-1, D-2, D-3). They need not
appear in every article anchor, but the header establishes the version context for the entire
file.

**Recommended header format:**

```
(* G.A. snapshot v3.16, dated 2026-01-30 *)
```

**Verification (AC-15):** A string search on each file must find both `v3.16` and `2026-01-30`.

```bash
grep -l "v3.16" catala/ga_1_4_3_opskrivning.catala_da catala/ga_1_4_4_nedskrivning.catala_da
grep -l "2026-01-30" catala/ga_1_4_3_opskrivning.catala_da catala/ga_1_4_4_nedskrivning.catala_da
```

Both commands must return the file names (i.e., the pattern must match).

### 7.4 NFR-4 — No production files modified

**Requirement (from petition):** No production code, database migrations, API changes, or
Spring Boot modules are introduced by this spike.

**Definition of "production files" in this project:**

| Category | Path pattern | Prohibited action |
|----------|-------------|-------------------|
| Java source code | `src/main/java/**/*.java` (in any module) | Create or modify |
| Database migrations | `src/main/resources/db/migration/**/*.sql` (in any module) | Create or modify |
| OpenAPI specifications | `api-specs/openapi-*.yaml` | Create or modify |
| Spring Boot config | `src/main/resources/application*.{yml,yaml,properties}` (in any module) | Create or modify |

**Concrete paths for this project that must not be touched:**

- `opendebt-creditor-portal/src/main/java/**`
- `opendebt-debt-service/src/main/java/**`
- `opendebt-creditor-service/src/main/java/**`
- `opendebt-case-service/src/main/java/**`
- `opendebt-*/src/main/resources/db/migration/**`
- `api-specs/openapi-creditor-service.yaml`
- `api-specs/openapi-debt-service.yaml`
- `api-specs/openapi-case-service.yaml`
- `api-specs/openapi-person-registry-internal.yaml`

**Verification (AC-16):** A git diff of the branch against `main` must show zero changes to
any of the above paths:

```bash
git diff main --name-only | grep -E '^(opendebt-[^/]+/src/main/java|opendebt-[^/]+/src/main/resources/db/migration|api-specs/)'
# Expected output: empty (no matches)
```

**Permitted new paths:**

```
catala/
catala/ga_1_4_3_opskrivning.catala_da
catala/ga_1_4_4_nedskrivning.catala_da
catala/tests/
catala/tests/ga_opskrivning_nedskrivning_tests.catala_da
catala/SPIKE-REPORT.md
design/specs-p054-catala-compliance-spike.md   (this document)
```

---

## Acceptance criteria traceability matrix

| AC | Petition 054 AC | Spec section |
|----|-----------------|--------------|
| AC-1 | FR-1 — D-1 file present | §2.1 |
| AC-2 | FR-1 — four sub-rule blocks with article anchors | §2.3–§2.6 |
| AC-3 | FR-1 — Danish dialect declared | §2.1, §7.2 |
| AC-4 | FR-2 — D-2 file present | §3.1 |
| AC-5 | FR-2 — three grounds + virkningsdato + GIL § 18 k with anchors | §3.2–§3.7 |
| AC-6 | FR-3 — D-3 test file present | §4.1 |
| AC-7 | FR-3 — minimum 8 test cases | §4.2 (10 specified) |
| AC-8 | FR-4 — D-4 report present | §5.1 |
| AC-9 | FR-4 — coverage table with all P053 FR-1 and FR-2 rows | §5.3 |
| AC-10 | FR-4 — all four sections present | §5.1–§5.5 |
| AC-11 | FR-5 — Go/No-Go section with unambiguous verdict | §6 |
| AC-12 | FR-5 — evidence for every criterion | §6.1–§6.2 |
| AC-13 | NFR-1 — compilation exits 0 for D-1 and D-2 | §7.1 |
| AC-14 | NFR-2 — Danish dialect, no English-dialect files | §7.2 |
| AC-15 | NFR-3 — G.A. snapshot v3.16 / 2026-01-30 in all source files | §7.3 |
| AC-16 | NFR-4 — no production artefacts modified | §7.4 |

---

## Out-of-scope boundary enforcement

The following are explicitly NOT specified in this document and must not be implemented:

| Item | Reason |
|------|--------|
| Encoding of any G.A. section other than G.A.1.4.3 and G.A.1.4.4 | Time-boxed; out of scope per petition |
| Runtime integration with Spring Boot or OpenDebt services | Follow-on petition if Go |
| CI pipeline integration for Catala compilation | Follow-on if Go |
| Full G.A. Inddrivelse chapter encoding | Multi-petition programme if Go |
| Catala encoding of G.A.2.3.4.4 (interne opskrivninger) | RIM-internal; excluded per petition |
| Rentegodtgørelse rules (GIL § 18 l) | Tracked in TB-039 |
| Retroactive timeline replay (G.A.1.4.4) | Tracked in TB-038 |
| Any Java class, DTO, service, or repository | NFR-4 hard constraint |
| Any database migration | NFR-4 hard constraint |
| Any OpenAPI specification change | NFR-4 hard constraint |
