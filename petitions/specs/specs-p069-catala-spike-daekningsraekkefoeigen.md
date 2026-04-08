# Implementation Specification — P069: Catala Compliance Spike — Dækningsrækkefølge

**Spec ID:** SPEC-P069  
**Petition:** `petitions/petition069-catala-spike-daekningsraekkefoeigen.md`  
**Outcome contract:** `petitions/petition069-catala-spike-daekningsraekkefoeigen-outcome-contract.md`  
**Feature file:** `petitions/petition069-catala-spike-daekningsraekkefoeigen.feature`  
**Status:** Ready for implementation  
**Legal basis:** GIL § 4, stk. 1–4; GIL § 6a; GIL § 10b; Gæld.bekendtg. § 4, stk. 3;
Gæld.bekendtg. § 7; Retsplejelovens § 507; Lov nr. 288/2022  
**G.A. snapshot:** v3.16 (2026-03-28)  
**Companion petition:** P057 (dækningsrækkefølge — GIL § 4, fuld spec)  
**Spike type:** Research spike — no production code  
**Time box:** 2 working days  

> **Spike note:** All deliverables are files — a Catala source program, a Catala test suite,
> and a markdown spike report. No runtime behaviour, no new API surface, no database
> migrations, and no portal changes are introduced. Every specification element in this
> document maps to a named FR, NFR, or AC from Petition 069.

---

> **FLAG-B — Citation correction (D-1):** The feature file Scenario 1 (line 22) asserts
> `"GIL § 4, stk. 12"` for the Category 1 anchor citation. This is a petition typo inherited
> from petition069 (FLAG-B). The correct legal reference is `GIL § 6a, stk. 12`. D-1 must
> use the corrected citation `GIL § 6a, stk. 12` in all Catala article-anchor comments. The
> feature file step assertion retains the incorrect string and will be corrected in a
> follow-up petition amendment.

> **FLAG-A — P057 scenario count reconciliation (D-3):** AC-13 references "22 P057 scenarios"
> as stated in the outcome contract, which replicates the petition typo. The actual committed
> count in `petition057-daekningsraekkefoeigen.feature` is **26** scenarios. D-3 must contain
> a comparison table with 26 rows. AC-13 is satisfied by the 26-row table; the count
> discrepancy in the outcome contract is an acknowledged petition defect (FLAG-A) that will be
> corrected in a follow-up petition amendment.

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

**Why GIL § 4 (G.A.2.3.2.1)?** GIL § 4 is the most legally consequential rule engine in
OpenDebt. An incorrect dækningsrækkefølge distributes money to the wrong fordring or
creditor, creates legally invalid dækning records that must be reversed and replayed, causes
CLS audit failures, and exposes Gældsstyrelsen to liability under forvaltningslovens § 22.
Petition 057 implemented the full rule engine in Gherkin and Java. Catala captures the *law
itself*. These two layers should agree — and where they diverge, one of them is wrong.

**Discrepancy hotspots (highest value for the spike):**

| Hotspot | Risk | Legal source |
|---------|------|-------------|
| 6-tier PSRM interest ordering within a single fordring | PSRM implementations often collapse the 6 sub-positions into fewer tiers; any simplification is legally incorrect | Gæld.bekendtg. § 4, stk. 3 |
| Udlæg exception (Retsplejelovens § 507) | Many systems apply inddrivelsesindsats surplus universally; udlæg surplus must stay with the udlæg fordring only | GIL § 4, stk. 3 + Retsplejelovens § 507 |
| Opskrivningsfordring positioning | Whether the opskrivningsfordring truly follows its parent immediately vs. takes a new FIFO queue position based on its own modtagelsesdato | Gæld.bekendtg. § 7 |

### 1.3 G.A. snapshot version

All G.A. article citations in Catala source files produced by this spike **must** reference:

```
G.A. snapshot v3.16, dated 2026-03-28
```

Both literal strings `v3.16` and `2026-03-28` must appear in the file header comment block
of each Catala source file (NFR-3; AC-20).

### 1.4 Deliverables

| ID | Artefact | Path | Petition FR |
|----|----------|------|-------------|
| D-1 | Catala source — dækningsrækkefølge | `catala/ga_2_3_2_1_daekningsraekkefoeigen.catala_da` | FR-1 to FR-5 |
| D-2 | Catala test file | `catala/tests/ga_daekningsraekkefoeigen_tests.catala_da` | FR-6 |
| D-3 | Spike report | `catala/SPIKE-REPORT-069.md` | FR-7, FR-8 |

No file outside the `catala/` directory tree is created or modified by this spike (NFR-4).

---

## §2 D-1 Specification — `catala/ga_2_3_2_1_daekningsraekkefoeigen.catala_da`

**Source:** Petition 069 FR-1 to FR-5 · Outcome contract FR-1 to FR-5 ·
AC-1, AC-2, AC-3, AC-4, AC-5, AC-6, AC-7, AC-8, AC-9, AC-18, AC-19, AC-20  
**Legal basis:** GIL § 4, stk. 1–4; GIL § 6a, stk. 1 and stk. 12; GIL § 10b;
Gæld.bekendtg. § 4, stk. 3; Gæld.bekendtg. § 7; Gæld.bekendtg. § 8, stk. 3;
Gæld.bekendtg. § 9, stk. 1 and stk. 3; Retsplejelovens § 507; Lov nr. 288/2022

### 2.1 File structure requirements

The file must:

1. Carry the Danish Catala dialect declaration (AC-19; NFR-2). The `.catala_da` extension
   signals the Danish dialect to the CLI. Any additional in-file pragma required by the
   installed CLI version must also be present [CATALA-SYNTAX-TBD — verify against installed
   CLI].
2. Carry a header comment block including the literal strings `v3.16` and `2026-03-28` and
   identify the G.A. section encoded (AC-20; NFR-3).
3. Declare three enumeration types (ENUM-1, ENUM-2, ENUM-3) and six computation scopes
   (SCOPE-1 through SCOPE-6) as specified in §2.2–§2.8.
4. Anchor each rule block to its exact legal citation using Catala's article-citation syntax
   (AC-2 through AC-9).

**Required file header (exact string content):**

```
# G.A.2.3.2.1 — Dækningsrækkefølge — GIL § 4

(* Catala kildedialekt: catala_da
   G.A. snapshot v3.16, dated 2026-03-28
   Juridisk grundlag: GIL § 4 stk. 1–4, GIL § 6a, GIL § 10b, Gæld.bekendtg. § 4 stk. 3,
                      Gæld.bekendtg. § 7, Retsplejelovens § 507, Lov nr. 288/2022
   Formål: Formalisering af dækningsrækkefølge-reglerne i GIL § 4, stk. 1–4, jf. G.A.2.3.2.1.
   Petition: P069 — Catala Compliance Spike — Dækningsrækkefølge
   Status: Research spike — ingen produktionskode. *)
```

### 2.2 Required enumeration declarations

#### ENUM-1: PrioritetKategori

**Anchor:** GIL § 4, stk. 1  
**Five variants — exact names mandatory:**

```catala
declaration enumeration PrioritetKategori:
  -- RIMELIGE_OMKOSTNINGER
  -- BOEDER_TVANGSBOEEDER_TILBAGEBETALING
  -- UNDERHOLDSBIDRAG_PRIVATRETLIG
  -- UNDERHOLDSBIDRAG_OFFENTLIG
  -- ANDRE_FORDRINGER
```

**Field rationale:**

| Variant | Legal source | Notes |
|---------|-------------|-------|
| `RIMELIGE_OMKOSTNINGER` | GIL § 6a, stk. 1; GIL § 6a, stk. 12 | Kategori 1 |
| `BOEDER_TVANGSBOEEDER_TILBAGEBETALING` | GIL § 10b; Lov nr. 288/2022 § 2, nr. 1 | Kategori 2 — inkl. tvangsbøder tilføjet ved lov nr. 288/2022 |
| `UNDERHOLDSBIDRAG_PRIVATRETLIG` | GIL § 4, stk. 1, nr. 2 | Kategori 3a — privatretlig dækkes forud for offentlig |
| `UNDERHOLDSBIDRAG_OFFENTLIG` | GIL § 4, stk. 1, nr. 2 | Kategori 3b |
| `ANDRE_FORDRINGER` | GIL § 4, stk. 1, nr. 3 | Kategori 4 |

#### ENUM-2: RenteKomponent

**Anchor:** Gæld.bekendtg. § 4, stk. 3  
**Six variants — exact order is mandatory per Gæld.bekendtg. § 4, stk. 3:**

```catala
declaration enumeration RenteKomponent:
  -- OPKRAEVANINGSRENTER
  -- INDDRIVELSESRENTER_FORDRINGSHAVER
  -- INDDRIVELSESRENTER_FOER_TILBAGEFOERSEL
  -- INDDRIVELSESRENTER_STK1
  -- OEVRIGE_RENTER_PSRM
  -- HOVEDFORDRING
```

> **Token mismatch (spike discrepancy finding):** P057 uses the token
> `INDDRIVELSESRENTER_FORDRINGSHAVER_STK3` in Scenario FR-3. The Catala encoding uses
> `INDDRIVELSESRENTER_FORDRINGSHAVER` (position 2, Gæld.bekendtg. § 9, stk. 3, 2./4. pkt.).
> This token mismatch must be flagged in D-3 as a discrepancy finding in the
> Discrepancies section.

#### ENUM-3: InddrivelsesindsatsType

**Anchor:** GIL § 4, stk. 3  
**Four variants — all in petition vocabulary:**

```catala
declaration enumeration InddrivelsesindsatsType:
  -- LOENINDEHOLDELSE
  -- UDLAEG
  -- AFDRAGSORDNING
  -- INGEN_INDSATS
```

### 2.3 SCOPE-1: PrioritetKategoriRang — FR-1.1 to FR-1.4

**Source:** Petition 069 FR-1 · AC-2, AC-3  
**Article anchor:** GIL § 4, stk. 1; GIL § 6a, stk. 1; GIL § 6a, stk. 12; GIL § 10b;
Lov nr. 288/2022

**Scope declaration:**

```catala
declaration scope PrioritetKategoriRang:
  context kategori content PrioritetKategori           (* input *)
  context prioritetRang content integer                (* output: 1–4 *)
  context underholdsbidragOrdning content integer      (* output: 1=privatretlig, 2=offentlig; 0 for non-kat3 *)
```

**Context variable semantics:**

| Variable | Type | Direction | Semantics |
|----------|------|-----------|-----------|
| `kategori` | `PrioritetKategori` | input | Den fordringens prioritetskategori jf. GIL § 4, stk. 1 |
| `prioritetRang` | `integer` | output | 1 = højeste prioritet (RIMELIGE_OMKOSTNINGER) … 4 = laveste (ANDRE_FORDRINGER) |
| `underholdsbidragOrdning` | `integer` | output | 1 = privatretlig, 2 = offentlig; 0 for alle ikke-kat3-kategorier |

**Rule blocks (7 rules):**

- **R-1.1** `prioritetRang = 1` when `kategori = RIMELIGE_OMKOSTNINGER`  
  Anchor: GIL § 6a, stk. 1; GIL § 6a, stk. 12

- **R-1.2** `prioritetRang = 2` when `kategori = BOEDER_TVANGSBOEEDER_TILBAGEBETALING`  
  Anchor: GIL § 10b; Lov nr. 288/2022 § 2, nr. 1

- **R-1.3a** `prioritetRang = 3` when `kategori = UNDERHOLDSBIDRAG_PRIVATRETLIG`  
  Anchor: GIL § 4, stk. 1, nr. 2

- **R-1.3b** `prioritetRang = 3` when `kategori = UNDERHOLDSBIDRAG_OFFENTLIG`  
  Anchor: GIL § 4, stk. 1, nr. 2

- **R-1.3c** `underholdsbidragOrdning = 1` when `kategori = UNDERHOLDSBIDRAG_PRIVATRETLIG`  
  Rationale: privatretlig dækkes forud for offentlig inden for kategori 3

- **R-1.3d** `underholdsbidragOrdning = 2` when `kategori = UNDERHOLDSBIDRAG_OFFENTLIG`

- **R-1.4** `prioritetRang = 4` when `kategori = ANDRE_FORDRINGER`  
  Anchor: GIL § 4, stk. 1, nr. 3

**Expected outputs (key test vectors):**

| `kategori` | `prioritetRang` | `underholdsbidragOrdning` |
|------------|-----------------|--------------------------|
| `RIMELIGE_OMKOSTNINGER` | 1 | 0 |
| `BOEDER_TVANGSBOEEDER_TILBAGEBETALING` | 2 | 0 |
| `UNDERHOLDSBIDRAG_PRIVATRETLIG` | 3 | 1 |
| `UNDERHOLDSBIDRAG_OFFENTLIG` | 3 | 2 |
| `ANDRE_FORDRINGER` | 4 | 0 |

### 2.4 SCOPE-2: FifoSortNøgle — FR-2.1, FR-2.2

**Source:** Petition 069 FR-2 · AC-4  
**Article anchor:** GIL § 4, stk. 2, 1. pkt.; GIL § 4, stk. 2, 5. pkt.

**Scope declaration:**

```catala
declaration scope FifoSortNøgle:
  context modtagelsesdato content date                  (* input *)
  context harLegacyModtagelsesdato content boolean      (* input *)
  context legacyModtagelsesdato content date            (* input — conditional: used only when harLegacyModtagelsesdato = true *)
  context erFoerSeptember2013 content boolean           (* derived: legacyModtagelsesdato < |2013-09-01| *)
  context fifoSortKey content date                      (* output *)
```

**Context variable semantics:**

| Variable | Type | Direction | Semantics |
|----------|------|-----------|-----------|
| `modtagelsesdato` | `date` | input | Fordringens modtagelsesdato i inddrivelsessystemet |
| `harLegacyModtagelsesdato` | `boolean` | input | True for fordringer overdraget før 2013-09-01 med separat legacy-dato |
| `legacyModtagelsesdato` | `date` | input | Den registrerede legacy-modtagelsesdato (kun relevant når `harLegacyModtagelsesdato = true`) |
| `erFoerSeptember2013` | `boolean` | derived | `legacyModtagelsesdato < |2013-09-01|` |
| `fifoSortKey` | `date` | output | Den effektive FIFO-sorteringsnøgle |

**Rule blocks (3 rules):**

- **R-2.1 (default)** `fifoSortKey = modtagelsesdato`  
  Anchor: GIL § 4, stk. 2, 1. pkt.

- **R-2.2b (derived)** `erFoerSeptember2013 consequence equals legacyModtagelsesdato < |2013-09-01|`

- **R-2.2 (exception to R-2.1)** `fifoSortKey = legacyModtagelsesdato`  
  `under condition harLegacyModtagelsesdato and erFoerSeptember2013`  
  Anchor: GIL § 4, stk. 2, 5. pkt.

**Expected outputs:**

| `harLegacyModtagelsesdato` | `modtagelsesdato` | `legacyModtagelsesdato` | Expected `fifoSortKey` |
|---------------------------|-------------------|------------------------|------------------------|
| `false` | `2024-01-15` | (irrelevant) | `2024-01-15` |
| `true` | `2024-01-15` | `2012-08-15` | `2012-08-15` |
| `true` | `2024-01-15` | `2013-09-15` (post-2013) | `2024-01-15` (default fires; legacy dato is NOT before 2013-09-01) |

### 2.5 SCOPE-3: RenteKomponentRang — FR-2.3, FR-2.4

**Source:** Petition 069 FR-2 · AC-5  
**Article anchor:** GIL § 4, stk. 2, 2. pkt.; Gæld.bekendtg. § 4, stk. 3;
Gæld.bekendtg. § 8, stk. 3; Gæld.bekendtg. § 9, stk. 1; Gæld.bekendtg. § 9, stk. 3

**Scope declaration:**

```catala
declaration scope RenteKomponentRang:
  context komponent content RenteKomponent     (* input *)
  context subPositionRang content integer      (* output: 1–6 *)
  context erRente content boolean              (* output: true for levels 1–5, false for HOVEDFORDRING *)
```

**Context variable semantics:**

| Variable | Type | Direction | Semantics |
|----------|------|-----------|-----------|
| `komponent` | `RenteKomponent` | input | Rentekomponen-typen som identificerer sub-positionen |
| `subPositionRang` | `integer` | output | 1 = første prioritet (OPKRAEVANINGSRENTER) … 6 = sidst (HOVEDFORDRING) |
| `erRente` | `boolean` | output | `true` for positionerne 1–5 (rentekrav); `false` for position 6 (hovedstol) |

**Rule blocks (8 rules — 6 for subPositionRang + 2 for erRente):**

- **R-2.4.1** `subPositionRang = 1` when `komponent = OPKRAEVANINGSRENTER`  
  Anchor: Gæld.bekendtg. § 4, stk. 3, pos. 1

- **R-2.4.2** `subPositionRang = 2` when `komponent = INDDRIVELSESRENTER_FORDRINGSHAVER`  
  Anchor: Gæld.bekendtg. § 9, stk. 3, 2./4. pkt.

- **R-2.4.3** `subPositionRang = 3` when `komponent = INDDRIVELSESRENTER_FOER_TILBAGEFOERSEL`  
  Anchor: Gæld.bekendtg. § 8, stk. 3

- **R-2.4.4** `subPositionRang = 4` when `komponent = INDDRIVELSESRENTER_STK1`  
  Anchor: Gæld.bekendtg. § 9, stk. 1

- **R-2.4.5** `subPositionRang = 5` when `komponent = OEVRIGE_RENTER_PSRM`  
  Anchor: Gæld.bekendtg. § 9, stk. 3, 1./3. pkt.

- **R-2.4.6** `subPositionRang = 6` when `komponent = HOVEDFORDRING`  
  Anchor: Gæld.bekendtg. § 4, stk. 3, pos. 6

- **R-2.3a** `erRente = true` when `komponent ≠ HOVEDFORDRING`  
  Anchor: GIL § 4, stk. 2, 2. pkt.

- **R-2.3b** `erRente = false` when `komponent = HOVEDFORDRING`

**Expected outputs (complete 6-position table):**

| `komponent` | `subPositionRang` | `erRente` |
|------------|-------------------|-----------|
| `OPKRAEVANINGSRENTER` | 1 | `true` |
| `INDDRIVELSESRENTER_FORDRINGSHAVER` | 2 | `true` |
| `INDDRIVELSESRENTER_FOER_TILBAGEFOERSEL` | 3 | `true` |
| `INDDRIVELSESRENTER_STK1` | 4 | `true` |
| `OEVRIGE_RENTER_PSRM` | 5 | `true` |
| `HOVEDFORDRING` | 6 | `false` |

### 2.6 SCOPE-4: InddrivelsesindsatsAnvendelse — FR-3.1 to FR-3.4

**Source:** Petition 069 FR-3 · AC-6, AC-7  
**Article anchor:** GIL § 4, stk. 3, 1. pkt.; GIL § 4, stk. 3, 2. pkt.; Retsplejelovens § 507

**Scope declaration:**

```catala
declaration scope InddrivelsesindsatsAnvendelse:
  context inddrivelsesindsatsType content InddrivelsesindsatsType  (* input *)
  context fordringErIndsatsFordring content boolean                 (* input *)
  context erUdlaeg content boolean                                  (* derived: inddrivelsesindsatsType = UDLAEG *)
  context gaelderGIL4Stk3 content boolean                          (* output: default true; false when erUdlaeg *)
  context modtagerIndsatsDaekning content boolean                   (* output *)
  context modtagerOverskudsDaekning content boolean                 (* output *)
  context udlaegSurplus content boolean                             (* output: true when erUdlaeg *)
```

**Context variable semantics:**

| Variable | Type | Direction | Semantics |
|----------|------|-----------|-----------|
| `inddrivelsesindsatsType` | `InddrivelsesindsatsType` | input | Type af inddrivelsesindsats for den modtagne betaling |
| `fordringErIndsatsFordring` | `boolean` | input | True hvis fordringen er omfattet af den pågældende inddrivelsesindsats |
| `erUdlaeg` | `boolean` | derived | `inddrivelsesindsatsType = UDLAEG` |
| `gaelderGIL4Stk3` | `boolean` | output | GIL § 4, stk. 3 finder anvendelse; default `true`; undtaget for udlæg |
| `modtagerIndsatsDaekning` | `boolean` | output | Fordringen modtager indsats-dækning (FR-3.1) |
| `modtagerOverskudsDaekning` | `boolean` | output | Fordringen kan modtage overskudsdækning (FR-3.2) |
| `udlaegSurplus` | `boolean` | output | `true` ved udlæg — overskud kan ikke omfordeles |

**Rule blocks (6 rules + 1 flagged annotation):**

- **R-3.erUdlaeg** `erUdlaeg consequence equals inddrivelsesindsatsType = UDLAEG`

- **R-3.1 (default)** `gaelderGIL4Stk3 = true`  
  Anchor: GIL § 4, stk. 3, 1. pkt.

- **R-3.4 (exception to R-3.1)** `gaelderGIL4Stk3 = false` when `erUdlaeg`  
  Anchor: Retsplejelovens § 507  
  Note: Mutually exclusive with R-3.1. Catala exception hierarchy must model R-3.4 as
  overriding R-3.1 when `erUdlaeg = true`. A reviewer must be able to confirm that
  udlæg-residualbeløb *never* flows to fordringer outside the udlæg in the Catala encoding.

- **R-3.1b** `modtagerIndsatsDaekning consequence equals fordringErIndsatsFordring and gaelderGIL4Stk3`

- **R-3.2** `modtagerOverskudsDaekning consequence equals (not fordringErIndsatsFordring) and gaelderGIL4Stk3`  
  Anchor: GIL § 4, stk. 3, 2. pkt.

- **R-3.3 (annotated, no AC)**  
  `# ⚠️ Ingen AC — afventer bekræftelse fra juridisk team (FLAG-C)`  
  Anchor: GIL § 4, stk. 3, 2. pkt., 2. led  
  Scope: Afdragsordning exception for tvangsbøder. Encode the rule with the FLAG-C
  annotation. No test case is required for this rule branch. The spike report must
  document FLAG-C explicitly under Gaps.

- **R-3.4b** `udlaegSurplus consequence equals erUdlaeg`

**Expected outputs (key test vectors):**

| `inddrivelsesindsatsType` | `fordringErIndsatsFordring` | `gaelderGIL4Stk3` | `modtagerIndsatsDaekning` | `modtagerOverskudsDaekning` | `udlaegSurplus` |
|--------------------------|---------------------------|-------------------|--------------------------|----------------------------|----------------|
| `LOENINDEHOLDELSE` | `true` | `true` | `true` | `false` | `false` |
| `LOENINDEHOLDELSE` | `false` | `true` | `false` | `true` | `false` |
| `UDLAEG` | `true` | `false` | `false` | `false` | `true` |
| `UDLAEG` | `false` | `false` | `false` | `false` | `true` |

### 2.7 SCOPE-5: TimingRegel — FR-4.1, FR-4.2

**Source:** Petition 069 FR-4 · AC-8  
**Article anchor:** GIL § 4, stk. 4, 1. pkt.; GIL § 4, stk. 4, 2. pkt.

**Scope declaration:**

```catala
declaration scope TimingRegel:
  context applikationstidspunkt content date    (* input *)
  context betalingstidspunkt content date       (* input *)
  context raekkefoelgeFastlagtVed content date  (* output = applikationstidspunkt *)
  context daekningVirkerFra content date        (* output = betalingstidspunkt *)
```

**Context variable semantics:**

| Variable | Type | Direction | Semantics |
|----------|------|-----------|-----------|
| `applikationstidspunkt` | `date` | input | Det tidspunkt RIM anvender beløbet til dækning |
| `betalingstidspunkt` | `date` | input | Det tidspunkt skyldner mistede rådigheden over beløbet |
| `raekkefoelgeFastlagtVed` | `date` | output | Tidspunktet dækningsrækkefølgen fastlægges (= `applikationstidspunkt`) |
| `daekningVirkerFra` | `date` | output | Tidspunktet dækningen har virkning fra (= `betalingstidspunkt`) |

> **FLAG-D — Catala `date` type limitation:** Catala's native `date` type does not carry
> time-of-day precision. FR-4 timestamps from P057 Gherkin are ISO-8601 datetimes (e.g.,
> `2025-01-10T09:00:00Z`). The Catala encoding uses `date`. This is a scope limitation to
> be evaluated explicitly in the spike report Go/No-Go section as a No-Go trigger N-1
> candidate. The spike report must document whether datetime precision is legally required
> for GIL § 4, stk. 4 compliance or whether date-level granularity is sufficient.

**Rule blocks (2 rules — both direct assignments):**

- **R-4.1** `raekkefoelgeFastlagtVed equals applikationstidspunkt`  
  Anchor: GIL § 4, stk. 4, 1. pkt.

- **R-4.2** `daekningVirkerFra equals betalingstidspunkt`  
  Anchor: GIL § 4, stk. 4, 2. pkt.

**Expected outputs:**

| `applikationstidspunkt` | `betalingstidspunkt` | `raekkefoelgeFastlagtVed` | `daekningVirkerFra` |
|------------------------|----------------------|--------------------------|---------------------|
| `2025-01-10` | `2025-01-09` | `2025-01-10` | `2025-01-09` |

### 2.8 SCOPE-6: OpskrivningFifoNøgle — FR-5.1 to FR-5.3

**Source:** Petition 069 FR-5 · AC-9  
**Article anchor:** Gæld.bekendtg. § 7; GIL § 4, stk. 2

**Scope declaration:**

```catala
declaration scope OpskrivningFifoNøgle:
  context erOpskrivningsfordring content boolean    (* input *)
  context egnModtagelsesdato content date           (* input — opskrivningsfordringens own receipt date *)
  context stamfordringFifoNoegle content date       (* input — parent fordring's effective FIFO key *)
  context harFlereOpskrivninger content boolean     (* input — multiple sibling write-ups exist *)
  context fifoSortKey content date                  (* output *)
```

**Context variable semantics:**

| Variable | Type | Direction | Semantics |
|----------|------|-----------|-----------|
| `erOpskrivningsfordring` | `boolean` | input | True hvis fordringen er en opskrivningsfordring jf. Gæld.bekendtg. § 7 |
| `egnModtagelsesdato` | `date` | input | Opskrivningsfordringens eget modtagelsesdato i inddrivelsessystemet |
| `stamfordringFifoNoegle` | `date` | input | Stamfordringens effektive FIFO-sorteringsnøgle |
| `harFlereOpskrivninger` | `boolean` | input | True hvis der eksisterer søsterfordringer for samme stamfordring |
| `fifoSortKey` | `date` | output | Den effektive FIFO-sorteringsnøgle for denne opskrivningsfordring |

**Rule blocks (2 rules + 2 annotations):**

- **R-5.default** `fifoSortKey = egnModtagelsesdato`  
  Fallback for non-opskrivnings-fordringer (anvendes ikke direkte for opskrivninger)

- **R-5.1+5.2 (exception to R-5.default and exception to FR-2.1)**  
  `fifoSortKey = stamfordringFifoNoegle` when `erOpskrivningsfordring = true`  
  Anchor: Gæld.bekendtg. § 7  
  `# FR-5.2: undtagelse til FR-2.1 — opskrivningsfordring starter IKKE ny FIFO-position fra sin egnModtagelsesdato`

- **R-5.3 annotation (no additional rule block required):**  
  `# FR-5.3: indbyrdes FIFO for søsterfordringer — GIL § 4, stk. 2 (Gæld.bekendtg. § 7)`  
  Comment: When `harFlereOpskrivninger = true`, sibling opskrivningsfordringer are ordered
  relative to each other using `egnModtagelsesdato` as tiebreaker, while all sharing
  `stamfordringFifoNoegle` as their primary FIFO key.

**Expected outputs:**

| `erOpskrivningsfordring` | `stamfordringFifoNoegle` | `egnModtagelsesdato` | Expected `fifoSortKey` |
|--------------------------|--------------------------|---------------------|------------------------|
| `true` | `2024-01-10` | `2024-06-01` | `2024-01-10` (inherits stamfordring key) |
| `false` | `2024-01-10` | `2024-06-01` | `2024-06-01` (uses own modtagelsesdato) |

---

## §3 D-2 Specification — `catala/tests/ga_daekningsraekkefoeigen_tests.catala_da`

**Source:** Petition 069 FR-6 · Outcome contract FR-6 · AC-10, AC-11  
**Verification command (from outcome contract):**

```bash
catala test-doc catala/tests/ga_daekningsraekkefoeigen_tests.catala_da
# Expected exit code: 0; all tests report PASS
```

### 3.1 Test file structure requirements

1. The file imports or references the D-1 source file [CATALA-SYNTAX-TBD — verify Catala
   module import syntax for test files].
2. All tests are expressed using Catala's built-in `Test` module or equivalent assertion
   mechanism [CATALA-SYNTAX-TBD — verify exact test declaration syntax for installed version].
3. The file contains **at minimum 10 distinct test cases** covering all three
   discrepancy-hotspot rule branches (AC-11).
4. The file header must include `v3.16` and `2026-03-28` (NFR-3).

**General test structure pattern** [CATALA-SYNTAX-TBD]:

```catala
(* Test file: ga_daekningsraekkefoeigen_tests.catala_da
   Tests for D-1 (G.A.2.3.2.1 dækningsrækkefølge)
   G.A. snapshot v3.16, dated 2026-03-28 *)

## Test: <TestName>

```catala
scope Test <ScopeName>:
  definition <field> equals <value>
  assertion <field> = <expected_value>
```

### 3.2 Required test cases (minimum 10)

The following test cases are required. **Implementing all cases satisfies AC-10 and AC-11.** The test cases cover:
- TC-1 (`DaekningTest1`): FR-1.1 — Kategori 1 (RIMELIGE_OMKOSTNINGER) → prioritetRang = 1
- TC-2 (`DaekningTest2`): FR-1.2 — Kategori 2 (BOEDER_TVANGSBOEEDER_TILBAGEBETALING) → prioritetRang = 2; tvangsbøder explicit
- TC-3 (`DaekningTest3`): FR-1.3 — Kategori 3 privatretlig → underholdsbidragOrdning = 1 (before offentlig)
- TC-4 (`DaekningTest11`): FR-1.4 — Kategori 4 (ANDRE_FORDRINGER) → prioritetRang = 4
- TC-5 (`DaekningTest4`): FR-2.1 FIFO grundregel
- TC-6 (`DaekningTest5`): FR-2.2 pre-2013 FIFO undtagelse
- TC-7 (`DaekningTest6_FullSekvens`): FR-2.4 — complete 6-position interest sequence (6 scope blocks)
- TC-8 (`DaekningTest7`): FR-3.1 inddrivelsesindsats grundregel
- TC-9 (`DaekningTest8`): FR-3.4 udlæg undtagelse — gaelderGIL4Stk3=false, udlaegSurplus=true
- TC-10 (`DaekningTest9`): FR-4 timingregel — applikationstidspunkt and betalingstidspunkt
- TC-11 (`DaekningTest10`): FR-5.1+5.2 opskrivningsfordring FIFO arv

Total: 11 numbered test cases (TC-7 expands to 6 scope blocks for the complete interest sequence). After adding the 6 interest-sequence blocks, the D-2 file will contain at minimum **16 distinct scope blocks** — well above the AC-11 minimum of 8.

---

#### TC-1 — Kategori 1: RIMELIGE_OMKOSTNINGER (FR-1.1)

**Test name:** `DaekningTest1`  
**FR covered:** FR-1.1 (GIL § 6a, stk. 1; GIL § 6a, stk. 12)  
**Scope:** `PrioritetKategoriRang`

| Input field | Value |
|-------------|-------|
| `kategori` | `RIMELIGE_OMKOSTNINGER` |

| Output field | Expected value |
|--------------|----------------|
| `prioritetRang` | `1` |

---

#### TC-2 — Kategori 2: BOEDER_TVANGSBOEEDER_TILBAGEBETALING (FR-1.2)

**Test name:** `DaekningTest2`  
**FR covered:** FR-1.2 (GIL § 10b; Lov nr. 288/2022 § 2, nr. 1)  
**Scope:** `PrioritetKategoriRang`

| Input field | Value |
|-------------|-------|
| `kategori` | `BOEDER_TVANGSBOEEDER_TILBAGEBETALING` |

| Output field | Expected value |
|--------------|----------------|
| `prioritetRang` | `2` |

---

#### TC-3 — Kategori 3: UNDERHOLDSBIDRAG_PRIVATRETLIG før offentlig (FR-1.3)

**Test name:** `DaekningTest3`  
**FR covered:** FR-1.3 (GIL § 4, stk. 1, nr. 2)  
**Scope:** `PrioritetKategoriRang`

| Input field | Value |
|-------------|-------|
| `kategori` | `UNDERHOLDSBIDRAG_PRIVATRETLIG` |

| Output field | Expected value |
|--------------|----------------|
| `prioritetRang` | `3` |
| `underholdsbidragOrdning` | `1` |

---

#### TC-4 — FIFO-grundregel: ingen legacy dato (FR-2.1)

**Test name:** `DaekningTest4`  
**FR covered:** FR-2.1 (GIL § 4, stk. 2, 1. pkt.)  
**Scope:** `FifoSortNøgle`

| Input field | Value |
|-------------|-------|
| `harLegacyModtagelsesdato` | `false` |
| `modtagelsesdato` | `2024-01-15` |

| Output field | Expected value |
|--------------|----------------|
| `fifoSortKey` | `2024-01-15` |

---

#### TC-5 — FIFO pre-2013 særregel: legacy dato < 2013-09-01 (FR-2.2)

**Test name:** `DaekningTest5`  
**FR covered:** FR-2.2 (GIL § 4, stk. 2, 5. pkt.)  
**Scope:** `FifoSortNøgle`

| Input field | Value |
|-------------|-------|
| `harLegacyModtagelsesdato` | `true` |
| `legacyModtagelsesdato` | `2012-08-15` |
| `modtagelsesdato` | `2024-01-15` |

| Output field | Expected value |
|--------------|----------------|
| `fifoSortKey` | `2012-08-15` |

---

#### TC-7 — 6-position rentesekvens: komplet sekvens (FR-2.4, alle positioner)

**Test name:** `DaekningTest6_FullSekvens`  
**FR covered:** FR-2.4 (Gæld.bekendtg. § 4, stk. 3); FR-2.3 erRente  
**Scope:** `RenteKomponentRang`

**Required:** The test file MUST contain explicit test cases asserting all six `subPositionRang` values (1 through 6) with the correct `erRente` value for each. This satisfies the feature file requirement 'alle seks under-positioner i rentesekvensen testes i komplet sekvens' (AC-11).

Because Catala test scopes test one set of inputs, this is implemented as SIX individual test scopes (`DaekningTest6a` through `DaekningTest6f`), each asserting one position. They collectively constitute the mandatory complete-sequence assertion.

| Scope | Input: `komponent` | Expected: `subPositionRang` | Expected: `erRente` |
|-------|-------------------|----------------------------|---------------------|
| `DaekningTest6a` | `OPKRAEVANINGSRENTER` | `1` | `true` |
| `DaekningTest6b` | `INDDRIVELSESRENTER_FORDRINGSHAVER` | `2` | `true` |
| `DaekningTest6c` | `INDDRIVELSESRENTER_FOER_TILBAGEFOERSEL` | `3` | `true` |
| `DaekningTest6d` | `INDDRIVELSESRENTER_STK1` | `4` | `true` |
| `DaekningTest6e` | `OEVRIGE_RENTER_PSRM` | `5` | `true` |
| `DaekningTest6f` | `HOVEDFORDRING` | `6` | `false` |

---

#### TC-8 — Inddrivelsesindsats-grundregel: lønindeholdelse, indsatsfordring (FR-3.1)

**Test name:** `DaekningTest7`  
**FR covered:** FR-3.1 (GIL § 4, stk. 3, 1. pkt.)  
**Scope:** `InddrivelsesindsatsAnvendelse`

| Input field | Value |
|-------------|-------|
| `inddrivelsesindsatsType` | `LOENINDEHOLDELSE` |
| `fordringErIndsatsFordring` | `true` |

| Output field | Expected value |
|--------------|----------------|
| `modtagerIndsatsDaekning` | `true` |
| `gaelderGIL4Stk3` | `true` |

---

#### TC-9 — Udlæg-undtagelse: GIL § 4, stk. 3 gælder ikke (FR-3.4)

**Test name:** `DaekningTest8`  
**FR covered:** FR-3.4 (Retsplejelovens § 507)  
**Scope:** `InddrivelsesindsatsAnvendelse`

| Input field | Value |
|-------------|-------|
| `inddrivelsesindsatsType` | `UDLAEG` |
| `fordringErIndsatsFordring` | `true` |

| Output field | Expected value |
|--------------|----------------|
| `gaelderGIL4Stk3` | `false` |
| `udlaegSurplus` | `true` |

---

#### TC-10 — Timingregel: applikationstidspunkt og betalingstidspunkt (FR-4)

**Test name:** `DaekningTest9`  
**FR covered:** FR-4.1 and FR-4.2 (GIL § 4, stk. 4, 1. og 2. pkt.)  
**Scope:** `TimingRegel`

| Input field | Value |
|-------------|-------|
| `applikationstidspunkt` | `2025-01-10` |
| `betalingstidspunkt` | `2025-01-09` |

| Output field | Expected value |
|--------------|----------------|
| `raekkefoelgeFastlagtVed` | `2025-01-10` |
| `daekningVirkerFra` | `2025-01-09` |

> **FLAG-D note:** If the Catala CLI cannot represent datetime precision
> (`2025-01-10T09:00:00Z`), this test uses date-level precision as a documented limitation.
> The spike report must evaluate whether this limitation triggers No-Go criterion N-1.

---

#### TC-11 — Opskrivningsfordring arver stamfordringens FIFO-nøgle (FR-5.1+5.2)

**Test name:** `DaekningTest10`  
**FR covered:** FR-5.1 and FR-5.2 (Gæld.bekendtg. § 7)  
**Scope:** `OpskrivningFifoNøgle`

| Input field | Value |
|-------------|-------|
| `erOpskrivningsfordring` | `true` |
| `stamfordringFifoNoegle` | `2024-01-10` |
| `egnModtagelsesdato` | `2024-06-01` |
| `harFlereOpskrivninger` | `false` |

| Output field | Expected value |
|--------------|----------------|
| `fifoSortKey` | `2024-01-10` (arver stamfordring — IKKE egnModtagelsesdato) |

---

#### TC-12 — Kategori 4: ANDRE_FORDRINGER (FR-1.4)

**Test name:** `DaekningTest11`  
**FR covered:** FR-1.4 (GIL § 4, stk. 1, nr. 3)  
**Scope:** `PrioritetKategoriRang`

| Input field | Value |
|-------------|-------|
| `kategori` | `ANDRE_FORDRINGER` |

| Output field | Expected value |
|--------------|----------------|
| `prioritetRang` | `4` |

**Justification:** Ensures all four priority categories are explicitly tested per AC-11 ("testcasene dækker alle fire FR-1 prioritetskategorier").

### 3.3 Test naming convention

Each test case heading in the Catala file must include the TC identifier so that
`catala test-doc` output can be matched to test names in the spike report. Pattern:
`DaekningTest<N>` (or `DaekningTest<N><suffix>` for compound tests).

---

## §4 D-3 Specification — `catala/SPIKE-REPORT-069.md`

**Source:** Petition 069 FR-7, FR-8 · Outcome contract FR-7, FR-8 ·
AC-12, AC-13, AC-14, AC-15, AC-16, AC-17  
**Legal basis:** Comparison against P057 Gherkin scenarios from
`petitions/petition057-daekningsraekkefoeigen.feature`

### 4.1 Required sections

The spike report must contain exactly these six sections. AC-14 requires sections 1–5;
AC-16/AC-17 require section 6 as a **standalone identifiable section** (not embedded in
section 5).

1. **Coverage Table** — maps every P057 scenario to a coverage status (§4.2)
2. **Gaps** — Catala rule branches not covered by any P057 scenario (§4.3)
3. **Discrepancies** — P057 scenarios contradicting G.A. text (§4.4)
4. **Discrepancy Hotspots** — explicit evaluation of all three known hotspots (§4.5)
5. **Effort Estimate** — person-day estimate for full G.A. Inddrivelse chapter (§4.6)
6. **Go/No-Go** — explicit verdict + evidence for all criteria (§4.7)

### 4.2 Coverage table requirements (AC-13)

> **FLAG-A reconciliation:** AC-13 in the outcome contract states "22 P057 scenarios". The
> actual committed count in `petition057-daekningsraekkefoeigen.feature` (HEAD) is **26**
> scenarios. The coverage table must contain **26 rows** — one per committed scenario. The
> outcome contract figure "22" is an acknowledged petition defect (FLAG-A) that will be
> corrected in a follow-up petition amendment. AC-13 is satisfied by the 26-row table.

The coverage table must have exactly these columns:

| P057 scenario | P057 FR section | Catala coverage status | Notes |
|---------------|-----------------|------------------------|-------|
| (one row per committed scenario) | | Covered / Not covered / Discrepancy found | |

**Column definitions:**

- **P057 scenario:** The exact scenario name from
  `petitions/petition057-daekningsraekkefoeigen.feature`.
- **P057 FR section:** The FR label from that feature file.
- **Catala coverage status:** Exactly one of: `Covered`, `Not covered`, or
  `Discrepancy found`.
- **Notes:** Brief explanation — which Catala test or rule covers it, or why it is out of
  scope for this spike.

The implementer must count all 26 scenarios in the HEAD of
`petitions/petition057-daekningsraekkefoeigen.feature` and produce a row for each.

### 4.3 Gaps section requirements (AC-14)

For each Catala rule branch (scope rule, enumeration variant, or exception path) in D-1
that has **no corresponding P057 Gherkin scenario**, the report must list:

- The Catala scope and rule identifier.
- The G.A. article citation it derives from.
- A one-sentence explanation of why the Gherkin does not cover it.

If no gaps exist, the section must state: "None found."

**Known candidate gaps to investigate (implementer must confirm or refute):**

| Candidate gap | Catala scope / rule | G.A. citation |
|---------------|---------------------|---------------|
| FR-3.3 afdragsordning-undtagelse for tvangsbøder | SCOPE-4, R-3.3 | GIL § 4, stk. 3, 2. pkt., 2. led |
| FR-5.3 indbyrdes FIFO for søsterfordringer | SCOPE-6, R-5.3 annotation | Gæld.bekendtg. § 7 |
| FIFO default for non-opskrivning (R-5.default) | SCOPE-6 | GIL § 4, stk. 2 |

FLAG-C must be listed explicitly: "FR-3.3 has no AC — encoded but not tested (FLAG-C:
awaiting legal team confirmation)."

### 4.4 Discrepancies section requirements (AC-14)

A discrepancy is any case where the Catala formal encoding produces a result that *cannot*
be reconciled with a P057 Gherkin scenario without one of them being incorrect.

For each discrepancy, document:
- The P057 scenario name and specific step that conflicts.
- The Catala rule and G.A. citation that contradicts it.
- The specific input values that expose the contradiction.
- A proposed resolution.

If no discrepancies are found, the section must state: "None found."

**Known discrepancy to document:**

| Discrepancy | P057 reference | Catala token | Resolution |
|-------------|---------------|--------------|------------|
| Token mismatch: P057 uses `INDDRIVELSESRENTER_FORDRINGSHAVER_STK3`; Catala uses `INDDRIVELSESRENTER_FORDRINGSHAVER` | Scenario FR-3 (approximate) | SCOPE-3, ENUM-2 | Flag for petition amendment; Catala token follows Gæld.bekendtg. § 9, stk. 3 labelling |

### 4.5 Discrepancy hotspots section requirements (AC-15)

The report must contain an explicit subsection for each of the three identified hotspots.
Each hotspot must state a clear finding: **Fund** or **Ingen diskrepans**.

**Hotspot 1 — 6-tier PSRM interest ordering (FR-2.4)**

Evaluation must confirm:
- Whether all 6 positions in Gæld.bekendtg. § 4, stk. 3 are correctly represented in P057.
- Whether any P057 scenario collapses two or more positions into a single step.
- Finding: **Fund** or **Ingen diskrepans**, with specific scenario reference if Fund.

**Hotspot 2 — Udlæg exception (FR-3.4)**

Evaluation must confirm:
- Whether udlæg-residualbeløbet is correctly isolated in P057 (does not flow to fordringer
  outside the udlæg).
- Finding: **Fund** or **Ingen diskrepans**, with specific scenario reference if Fund.

**Hotspot 3 — Opskrivningsfordring FIFO positioning (FR-5.2)**

Evaluation must confirm:
- Whether opskrivningsfordringen in P057 inherits the precise FIFO key from its
  stamfordring, or whether it takes a new independent position.
- Finding: **Fund** or **Ingen diskrepans**, with specific scenario reference if Fund.

### 4.6 Effort estimate section requirements (AC-14)

The effort estimate must:

1. State the actual encoding effort expended during the spike (in person-days or person-hours).
2. State the number of G.A. rule branches encoded in D-1 (across all 6 scopes).
3. Estimate the number of G.A. Inddrivelse rule branches remaining to be encoded (based on
   chapter structure surveyed during the spike).
4. Provide a per-section effort estimate with rationale.
5. State explicitly whether the per-section effort exceeds the 4-person-day No-Go threshold
   (No-Go trigger N-3).

### 4.7 Go/No-Go section requirements (AC-16, AC-17)

The Go/No-Go section must be a **standalone section** (section 6 of the report), not embedded
within the effort estimate. It must contain:

- An unambiguous verdict: the word **Go** or **No-Go** in a heading or bold text (AC-16).
- Evidence for every criterion listed below (AC-17).

#### Go criteria — all four must be met for a Go verdict

**G-1: All four priority categories encoded without ambiguity**

Evidence required:
- State `Yes` or `No`.
- If `Yes`: identify the Catala scope and rule names used for FR-1.1 through FR-1.4.
- If `No`: cite the specific category that could not be encoded without workaround.

**G-2: At least 1 discrepancy or gap found relative to P057 Gherkin**

Evidence required:
- State `Yes` or `No`.
- If `Yes`: cite at least one finding and describe what it demonstrates about the value of
  the Catala encoding.
- If `No`: state explicitly "No gaps or discrepancies were found" and assess whether this
  indicates the Gherkin is already comprehensive or the Catala encoding is insufficiently
  precise.

**G-3: Catala test compilation succeeds without errors**

Evidence required:
- State `Yes` or `No`.
- Include exact command run, exit code, and last 20 lines of output.

**G-4: OCaml extraction produces runnable code**

Evidence required:
- State `Yes` or `No`.
- Include the exact extraction command:

```bash
catala ocaml ga_2_3_2_1_daekningsraekkefoeigen.catala_da
```

- State the exit code and confirm whether the generated `.ml` file is syntactically valid.

#### No-Go criteria — any one triggers No-Go

**N-1: Temporal/datetime rules cannot be expressed without workarounds**

Trigger condition: Any FR-4 timestamp rule or date comparison in FR-2.2 required an
encoding workaround that introduces ambiguity or requires runtime logic outside Catala.
FLAG-D (Catala `date` type lacking time-of-day precision) must be explicitly evaluated here.

Evidence required:
- State `Triggered` or `Not triggered`.
- If `Triggered`: describe the specific rule, attempted encoding, and reason it fails or
  requires a workaround.

**N-2: Legal ambiguities in G.A. text block formal encoding**

Trigger condition: Any G.A.2.3.2.1 rule is underspecified such that multiple distinct
Catala encodings are equally valid and produce different outputs, with no G.A. basis to
choose between them.

Evidence required:
- State `Triggered` or `Not triggered`.
- If `Triggered`: cite the specific G.A. reference, describe the ambiguity, and document
  the conflicting encodings with their different outputs.

**N-3: Encoding effort per G.A. section exceeds 4 person-days**

Trigger condition: The effort estimate (§4.6) produces a per-G.A.-section figure exceeding
4 person-days.

Evidence required:
- State `Triggered` or `Not triggered`.
- Include the per-section effort figure and threshold comparison.

---

## §5 NFR Specifications

**Source:** Petition 069 NFR-1 through NFR-4 · AC-18 through AC-21

### 5.1 NFR-1 — Compilation (AC-18)

**Requirement:** The Catala source must compile without errors using the Catala CLI.

**Compilation command and expected exit code:**

```bash
# Run from catala/ directory:
cd catala
catala ocaml ga_2_3_2_1_daekningsraekkefoeigen.catala_da   # must exit 0
catala test-doc tests/ga_daekningsraekkefoeigen_tests.catala_da  # must exit 0
```

**Non-zero exit code is a hard failure (AC-18).** The spike report must record the actual
exit codes as evidence for Go criteria G-3 and G-4.

### 5.2 NFR-2 — Danish dialect declaration (AC-19)

**Requirement:** The Catala source must be in Danish (`catala_da`) to match the G.A. source
language and support bidirectional traceability with the Danish legislative text.

**Compliance definition:** A source file satisfies NFR-2 when the Catala CLI parses it
without a dialect mismatch error. The `.catala_da` file extension signals the Danish dialect.
Any additional in-file dialect pragma required by the installed CLI version must also be
present [CATALA-SYNTAX-TBD — verify against installed CLI].

**Prohibited:** No English-dialect Catala source file (`.catala_en`) is produced by this
spike (AC-19).

**Verification:** D-1 and D-2 files must carry the `.catala_da` extension and compile
without a dialect error.

### 5.3 NFR-3 — G.A. snapshot version citation (AC-20)

**Requirement:** All G.A. article citations must reference G.A. snapshot version 3.16,
dated 2026-03-28.

**Required literal strings:** Both `v3.16` and `2026-03-28` must appear as character
sequences in the header comment of D-1 and D-2.

**Verification:**

```bash
grep "v3.16" catala/ga_2_3_2_1_daekningsraekkefoeigen.catala_da
grep "2026-03-28" catala/ga_2_3_2_1_daekningsraekkefoeigen.catala_da
# Both must return matches
```

### 5.4 NFR-4 — No production files modified (AC-21)

**Requirement:** No production code, database migrations, API changes, or Spring Boot
modules are introduced by this spike.

**Definition of "production files":**

| Category | Path pattern | Prohibited action |
|----------|-------------|-------------------|
| Java source code | `src/main/java/**/*.java` (any module) | Create or modify |
| Database migrations | `src/main/resources/db/migration/**/*.sql` (any module) | Create or modify |
| OpenAPI specifications | `api-specs/openapi-*.yaml` | Create or modify |
| Spring Boot config | `src/main/resources/application*.{yml,yaml,properties}` (any module) | Create or modify |

**Concrete prohibited paths:**

- `opendebt-creditor-portal/src/main/java/**`
- `opendebt-debt-service/src/main/java/**`
- `opendebt-creditor-service/src/main/java/**`
- `opendebt-case-service/src/main/java/**`
- `opendebt-*/src/main/resources/db/migration/**`
- `api-specs/openapi-creditor-service.yaml`
- `api-specs/openapi-debt-service.yaml`
- `api-specs/openapi-case-service.yaml`
- `api-specs/openapi-person-registry-internal.yaml`

**Permitted new paths:**

```
catala/ga_2_3_2_1_daekningsraekkefoeigen.catala_da
catala/tests/ga_daekningsraekkefoeigen_tests.catala_da
catala/SPIKE-REPORT-069.md
design/specs-p069-catala-spike-daekningsraekkefoeigen.md   (this document)
```

**Verification (AC-21):**

```bash
git diff main --name-only | grep -E \
  '^(opendebt-[^/]+/src/main/java|opendebt-[^/]+/src/main/resources/db/migration|api-specs/)'
# Expected output: empty (no matches)
```

---

## §6 Known flags

| Flag | Location | Description |
|------|----------|-------------|
| FLAG-A | D-3, §4.2 | P057 scenario count discrepancy: outcome contract states 22, committed HEAD contains 26. D-3 must contain 26 rows. Count acknowledged as petition defect; to be corrected in follow-up amendment. |
| FLAG-B | D-1, §2.1 | Feature file Scenario 1 (line 22) asserts `"GIL § 4, stk. 12"`. Correct citation is `GIL § 6a, stk. 12`. D-1 must use the corrected citation. Feature file step assertion retains incorrect string pending follow-up amendment. |
| FLAG-C | D-1, §2.6 (R-3.3) | FR-3.3 (afdragsordning exception for tvangsbøder) has no AC. Encode the rule with annotation `# ⚠️ Ingen AC — afventer bekræftelse fra juridisk team (FLAG-C)`. No test required for this branch. Report in D-3 Gaps section. |
| FLAG-D | D-1, §2.7; D-3, §4.7 | Catala `date` type lacks time-of-day precision. FR-4 timestamps in P057 Gherkin are ISO-8601 datetimes. This is a scope limitation; must be evaluated in D-3 Go/No-Go section under No-Go trigger N-1. |

---

## Acceptance criteria traceability matrix

| AC | Petition 069 AC | Spec section |
|----|-----------------|--------------|
| AC-1 | FR-1 — D-1 file present | §2.1 |
| AC-2 | FR-1 — four category rule blocks with article anchors | §2.3 |
| AC-3 | FR-1 — tvangsbøder explicitly encoded | §2.3 (R-1.2), §2.2 (ENUM-1) |
| AC-4 | FR-2 — FIFO and pre-2013 rules with article anchors | §2.4 |
| AC-5 | FR-2 — 6-position interest sequence complete | §2.5 |
| AC-6 | FR-3 — inddrivelsesindsats basic and surplus rules | §2.6 |
| AC-7 | FR-3 — udlæg exception mutually exclusive with basic rule | §2.6 (R-3.4) |
| AC-8 | FR-4 — timing rule with article anchors | §2.7 |
| AC-9 | FR-5 — opskrivningsfordring FIFO inheritance | §2.8 |
| AC-10 | FR-6 — D-2 test file present | §3.1 |
| AC-11 | FR-6 — minimum 8 test cases covering all hotspot branches | §3.2 (10 specified) |
| AC-12 | FR-7 — D-3 spike report present | §4.1 |
| AC-13 | FR-7 — coverage table with all 26 P057 scenarios (FLAG-A) | §4.2 |
| AC-14 | FR-7 — all five content sections present | §4.3–§4.6 |
| AC-15 | FR-7 — three hotspots explicitly evaluated | §4.5 |
| AC-16 | FR-8 — Go/No-Go section with unambiguous verdict | §4.7 |
| AC-17 | FR-8 — evidence for all 4 Go criteria and 3 No-Go triggers | §4.7 |
| AC-18 | NFR-1 — compilation exits 0 | §5.1 |
| AC-19 | NFR-2 — Danish dialect, no English-dialect files | §5.2 |
| AC-20 | NFR-3 — G.A. snapshot v3.16 / 2026-03-28 in source files | §5.3 |
| AC-21 | NFR-4 — no production artefacts created or modified | §5.4 |

---

## Out-of-scope boundary enforcement

The following are explicitly NOT specified in this document and must not be implemented:

| Item | Reason |
|------|--------|
| Encoding of any G.A. section other than G.A.2.3.2.1 | Time-boxed; scope selected for overlap with P057 |
| DMI parallel operation rules (GIL § 49) | Separate rule set; not PSRM-primary |
| Runtime integration with Spring Boot or OpenDebt services | Spike only; follow-on petition if Go |
| CI pipeline integration for Catala compilation | Follow-on if Go |
| Full G.A. Inddrivelse chapter in Catala | Follow-on multi-petition programme if Go |
| G.A.2.3.2.2 deviation from dækningsrækkefølge | Separate follow-on petition |
| G.A.2.3.2.3 parallel operation period | DMI-specific; not PSRM focus |
| Pro-rata allocation for co-debtors (P062) | Separate petition |
| Prescription interruption (P059) | Separate petition |
| Any Java class, DTO, service, or repository | NFR-4 hard constraint |
| Any database migration | NFR-4 hard constraint |
| Any OpenAPI specification change | NFR-4 hard constraint |
