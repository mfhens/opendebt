# P070 Implementation Specification: Catala Compliance Spike — Forældelse G.A.2.4

**Petition:** 070 — Catala Compliance Spike — Forældelse G.A.2.4 (companion to P059)  
**Type:** Research spike — ingen produktionskode  
**G.A. snapshot:** v3.16 (2026-03-28)  
**Legal basis:** G.A.2.4.1–G.A.2.4.4.2, GIL §§ 18, 18a stk. 1–8, Forældelsesl. §§ 3, 5, 18–19, SKM2015.718.ØLR  
**Spec author:** Pipeline Conductor (P070)  
**Companion petition:** P059 — Forældelse (29 Gherkin scenarios)  
**Depends on:** P059  
**Reference spec:** design/specs-p069-catala-spike-daekningsraekkefoeigen.md  

---

## 1. Overview

This spike produces three file artifacts encoding the complete G.A.2.4 forældelses-ruleset in Catala:

| # | Deliverable | Path | FRs |
|---|-------------|------|-----|
| D-1 | Catala source — forældelse | `catala/ga_2_4_foraeldelse.catala_da` | FR-1–FR-4 |
| D-2 | Catala test suite | `catala/tests/ga_foraeldelse_tests.catala_da` | FR-5 |
| D-3 | Spike report + Go/No-Go | `catala/SPIKE-REPORT-070.md` | FR-6, FR-7 |

No production code, database migrations, API changes, or Spring Boot modules are produced.

---

## 2. Deliverable D-1: catala/ga_2_4_foraeldelse.catala_da

### 2.1 File Header

```catala
# G.A.2.4 — Forældelse — GIL §§ 18, 18a; Forældelsesl. §§ 3, 5, 18–19

(* Catala kildedialekt: catala_da
   G.A. snapshot v3.16, dated 2026-03-28
   Juridisk grundlag: GIL § 18 stk. 4, GIL § 18a stk. 1–8,
                      Forældelsesl. §§ 3, 5, 18–19, SKM2015.718.ØLR
   Formål: Formalisering af forældelses-reglerne i G.A.2.4.1–G.A.2.4.4.2.
   Petition: P070 — Catala Compliance Spike — Forældelse G.A.2.4
   Status: Research spike — ingen produktionskode. *)
```

### 2.2 Enumerationer

**ENUM-1: KildesystemType** — GIL § 18a, stk. 1
```catala
declaration enumeration KildesystemType:
  -- PSRM
  -- DMI_SAP38
```

**ENUM-2: RetsgrundlagType** — Forældelsesl. § 5, stk. 1
```catala
declaration enumeration RetsgrundlagType:
  -- ORDINARY
  -- SAERLIGT_RETSGRUNDLAG
```

**ENUM-3: AfbrydelseType** — G.A.2.4.4.1
```catala
declaration enumeration AfbrydelseType:
  -- BEROSTILLELSE
  -- LOENINDEHOLDELSE_AFGOERELSE
  -- LOENINDEHOLDELSE_VARSEL
  -- UDLAEG
```
(MODREGNING and FORELOEBIG_MODTAGELSE are out of scope for P070 — deferred to follow-up petition.)

**ENUM-4: AfbrydelseResultat** — for classification of interruption outcome
```catala
declaration enumeration AfbrydelseResultat:
  -- NY_FRIST_3_AAR
  -- NY_FRIST_10_AAR
  -- AFVIST_VARSEL_ALENE
  -- FORELOEBIG_AFBRYDELSE
```

### 2.3 Scope 1: UdskydelsesBeregning — FR-1.3/1.4/1.5 (GIL § 18a, stk. 1)

**Input fields:**
- `kildesystem`: KildesystemType
- `registreringsDato`: date (date the claim was registered in source system)
- `inddrivelsesdato`: date (date accepted into inddrivelse, ≥ 2015-11-19 for PSRM scope)

**Output fields:**
- `udskydelseDato`: date — the effective start date for frist computation (immutable after first set)
- `psrmUdskydelseDato`: date — 2021-11-20 constant (GIL § 18a, stk. 1, 1. pkt.)
- `dmiUdskydelseDato`: date — 2027-11-20 constant (GIL § 18a, stk. 1, 2. pkt.)

**Rules:**

Rule U-1 (PSRM udskydelse — GIL § 18a, stk. 1, 1. pkt.):
- Condition: kildesystem = PSRM AND inddrivelsesdato >= |2015-11-19|
- Output: udskydelseDato = |2021-11-20|
- Anchor: GIL § 18a, stk. 1, 1. pkt.

Rule U-2 (DMI/SAP38 udskydelse — GIL § 18a, stk. 1, 2. pkt.):
- Condition: kildesystem = DMI_SAP38 AND registreringsDato >= |2024-01-01|
- Output: udskydelseDato = |2027-11-20|
- Anchor: GIL § 18a, stk. 1, 2. pkt.

Rule U-3 (udskydelse immutabilitet — G.A.2.4.1):
- The udskydelseDato computed at intake is IMMUTABLE. No subsequent afbrydelse event can modify it.
- Catala encoding: udskydelseDato is an output of UdskydelsesBeregning computed once; AfbrydelseRegler scope MUST NOT output a modified udskydelseDato.
- Anchor: G.A.2.4.1

### 2.4 Scope 2: ForaeldelseFristBeregning — FR-1.1/1.2 (GIL § 18a, stk. 4 + Forældelsesl. § 5)

**Input fields:**
- `udskydelseDato`: date (from Scope 1)
- `afbrydelseDato`: date (date of most recent interruption event, or null if none)
- `retsgrundlag`: RetsgrundlagType
- `erUdlaeg`: boolean (whether the triggering afbrydelse is an udlæg)
- `currentFristExpires`: date (current expiry date before this computation)

**Output fields:**
- `fristStartDato`: date — effective start of frist (= max(afbrydelseDato ?? inddrivelsesdato, udskydelseDato))
- `fristVarighed`: integer — 3 or 10 (years)
- `nyFristUdloeber`: date — computed expiry date

**Rules:**

Rule F-1 (3-årig base frist — GIL § 18a, stk. 4):
- Condition: erUdlaeg = false OR retsgrundlag = ORDINARY
- Output: fristVarighed = 3
- Anchor: GIL § 18a, stk. 4

Rule F-2 (10-årig frist ved udlæg + særligt retsgrundlag — Forældelsesl. § 5, stk. 1):
- Condition: erUdlaeg = true AND retsgrundlag = SAERLIGT_RETSGRUNDLAG
- Output: fristVarighed = 10
- Override-priority: F-2 > F-1 (F-2 is exception to F-1)
- Anchor: Forældelsesl. § 5, stk. 1

Rule F-3 (udskydelse overtrumfer — G.A.2.4.1):
- fristStartDato = if afbrydelseDato exists then maximum of (afbrydelseDato, udskydelseDato) else udskydelseDato
- nyFristUdloeber = fristStartDato + fristVarighed years
- Anchor: GIL § 18a, stk. 1 (udskydelse immutability constraint applied at frist computation)

### 2.5 Scope 3: AfbrydelseValidering — FR-2.1/2.2/2.3 (G.A.2.4.4.1)

**Input fields:**
- `afbrydelseType`: AfbrydelseType
- `eventDate`: date
- `afgoerelseRegistreret`: boolean (for lønindeholdelse: is there a formal afgørelse?)
- `underretningsDato`: date (for lønindeholdelse: date debitor received notification)
- `kildesystem`: KildesystemType (for berostillelse: PSRM only)
- `retsgrundlag`: RetsgrundlagType
- `inaktivSiden`: date option (for lønindeholdelse: date inactivity began)
- `currentFristExpires`: date

**Output fields:**
- `afbrydelseResultat`: AfbrydelseResultat
- `nyAfbrydelseDato`: date option — date of new frist start (null if rejected)
- `nyFristUdloeber`: date option

**Rules:**

Rule A-1 (berostillelse — GIL § 18a, stk. 8):
- Condition: afbrydelseType = BEROSTILLELSE AND kildesystem = PSRM
- Output: afbrydelseResultat = NY_FRIST_3_AAR; nyAfbrydelseDato = eventDate; nyFristUdloeber = eventDate + 3yr
- Anchor: GIL § 18a, stk. 8

Rule A-2 (lønindeholdelse varsel AFVIST — SKM2015.718.ØLR):
- Condition: afbrydelseType = LOENINDEHOLDELSE_VARSEL OR (afbrydelseType = LOENINDEHOLDELSE_AFGOERELSE AND afgoerelseRegistreret = false)
- Output: afbrydelseResultat = AFVIST_VARSEL_ALENE; nyAfbrydelseDato = null; nyFristUdloeber = null
- CRITICAL: currentFristExpires is UNCHANGED
- Anchor: GIL § 18, stk. 4; SKM2015.718.ØLR

Rule A-3 (lønindeholdelse afgørelse — GIL § 18, stk. 4):
- Condition: afbrydelseType = LOENINDEHOLDELSE_AFGOERELSE AND afgoerelseRegistreret = true
- Output: afbrydelseResultat = NY_FRIST_3_AAR; nyAfbrydelseDato = underretningsDato; nyFristUdloeber = underretningsDato + 3yr
- Anchor: GIL § 18, stk. 4

Rule A-4 (lønindeholdelse inaktiv 1 år — GIL § 18, stk. 4):
- Condition: inaktivSiden is present AND (eventDate - inaktivSiden) >= 1yr
- Output: afbrydelseResultat = NY_FRIST_3_AAR; nyAfbrydelseDato = inaktivSiden; nyFristUdloeber = inaktivSiden + 3yr
- Anchor: GIL § 18, stk. 4
- Note: This replaces the active lønindeholdelse frist; the inaktivitetsdato is the new start

Rule A-5 (udlæg — Forældelsesl. § 18, stk. 1):
- Condition: afbrydelseType = UDLAEG (forgæves OR vellykket — NO DISTINCTION in interruption effect)
- Sub-rule A-5a (ordinary): retsgrundlag = ORDINARY → nyFristUdloeber = eventDate + 3yr; resultAt = NY_FRIST_3_AAR
- Sub-rule A-5b (særligt): retsgrundlag = SAERLIGT_RETSGRUNDLAG → nyFristUdloeber = eventDate + 10yr; resultAt = NY_FRIST_10_AAR
- Anchor: Forældelsesl. § 18, stk. 1 (forgæves udlæg = vellykket udlæg explicitly)

### 2.6 Scope 4: Fordringskompleks — FR-3 (GIL § 18a, stk. 2)

**Input:**
- `kompleksId`: integer
- `fordringIds`: list of integers (members of the complex — at least 1 = hovedkrav; rest = associated renter)
- `erForaeldet`: mapping of fordringId → boolean

**Rules:**

Rule K-1 (fordringskompleks definition — FR-3.1):
- A complex consists of exactly one hoofdkrav and zero or more associated renter sharing a forældelsesfrist.
- Anchor: GIL § 18a, stk. 2

Rule K-2 (afbrydelse propagation — FR-3.2):
- If any member of the complex has afbrydelse applied, ALL members receive the same nyFristUdloeber.
- Catala encoding: for all fordringId in fordringIds: nyFristUdloeber[fordringId] = same_date
- Anchor: GIL § 18a, stk. 2, 4. pkt.

Rule K-3 (atomicity constraint — FR-3.3):
- Partial propagation (some members updated, others not) is a FAILURE CONDITION.
- Catala encoding: assertion — for all fordringId in fordringIds: (nyFristUdloeber[fordringId] = sharedNyFrist)
- This assertion must fail (not silently succeed) if any member is missing from the update.
- Anchor: GIL § 18a, stk. 2

### 2.7 Scope 5: TillaegsfristBeregning — FR-4 (G.A.2.4.4.2)

**Input:**
- `currentFristExpires`: date
- `eventDate`: date (date of opskrivning or modtagelse)
- `udskydelseDato`: date (from Scope 1 — immutable lower bound)
- `erTomt`: boolean (true = empty fordringskompleks for FR-4.2)

**Rules:**

Rule T-1 (intern opskrivning tillægsfrist — FR-4.1, G.A.2.4.4.2.1):
- Condition: event = InternOpskrivning AND NOT erTomt
- nyFristUdloeber = (maximum of (currentFristExpires, eventDate)) + 2yr
- Anchor: G.A.2.4.4.2.1

Rule T-2 (max() formula — FR-4.3, intern opskrivning only):
- nyFristUdloeber = (maximum of (currentFristExpires, eventDate)) + tillaegsFristAar
- tillaegsFristAar = 2 (for intern opskrivning — FR-4.1/FR-4.3)
- CRITICAL: do NOT use simplified formula (eventDate + 2yr) — always apply max()
- Note: Rule T-2 applies to intern opskrivning only. For foreløbig afbrydelse (empty fordringskompleks), Rule T-3 applies with eventDate directly (no max() since currentFristExpires is undefined for a new empty complex).
- Anchor: G.A.2.4.4.2

Rule T-3 (foreløbig afbrydelse for tomt kompleks — FR-4.2, GIL § 18a, stk. 7):
- Condition: erTomt = true (empty fordringskompleks received into inddrivelse)
- Output: afbrydelseResultat = FORELOEBIG_AFBRYDELSE; nyFristUdloeber = eventDate + 3yr
- Anchor: GIL § 18a, stk. 7

Rule T-4 (udskydelsesdato floor — FR-4.4):
- The frist computed by T-1/T-2/T-3 cannot have a start date earlier than udskydelseDato.
- fristStartDato = maximum of (computed start, udskydelseDato)
- Anchor: GIL § 18a, stk. 1 (udskydelse immutability extends to tillægsfrister)

---

## 3. Deliverable D-2: catala/tests/ga_foraeldelse_tests.catala_da

### 3.1 File Header

```
(* Catala kildedialekt: catala_da
   G.A. snapshot v3.16, dated 2026-03-28
   Testpakke for: ga_2_4_foraeldelse.catala_da
   Petition: P070 — Catala Compliance Spike — Forældelse G.A.2.4
   Status: Research spike — ingen produktionskode.
   
   Denne fil indeholder 16 testscopes der dækker FR-1–FR-4. *)
```

### 3.2 Test Cases (T-01 through T-16)

Each test follows the P069 pattern: Markdown header + background + catala code block.

**T-01: 3-årig base frist (FR-1.1)**
- Input: kildesystem = PSRM, inddrivelsesdato = |2022-06-01|, retsgrundlag = ORDINARY, no afbrydelse
- Expected: fristVarighed = 3, nyFristUdloeber = |2025-06-01|
- Anchor: GIL § 18a, stk. 4

**T-02: 10-årig frist ved udlæg med særligt retsgrundlag (FR-1.2)**
- Input: kildesystem = PSRM, erUdlaeg = true, retsgrundlag = SAERLIGT_RETSGRUNDLAG, eventDate = |2024-04-10|
- Expected: fristVarighed = 10, nyFristUdloeber = |2034-04-10|
- Anchor: Forældelsesl. § 5, stk. 1

**T-03: PSRM udskydelse — dagen FØR grænseværdidato (FR-1.3 boundary)**
- Input: kildesystem = PSRM, inddrivelsesdato = |2015-11-18| (one day BEFORE scope)
- Expected: udskydelseDato = NOT set by PSRM rule (no udskydelse applies)
- Anchor: GIL § 18a, stk. 1, 1. pkt.

**T-04: PSRM udskydelse — PÅ grænseværdidato (FR-1.3 boundary)**
- Input: kildesystem = PSRM, inddrivelsesdato = |2015-11-19|
- Expected: udskydelseDato = |2021-11-20|, tidligste fristUdloeb = |2024-11-21| (after 3yr)
- Anchor: GIL § 18a, stk. 1, 1. pkt.

**T-05: PSRM udskydelse — dagen EFTER grænseværdidato (FR-1.3 boundary)**
- Input: kildesystem = PSRM, inddrivelsesdato = |2015-11-20|
- Expected: udskydelseDato = |2021-11-20| (same — date is a lower bound, not a day-exact cutoff)
- Anchor: GIL § 18a, stk. 1, 1. pkt.

**T-06: DMI/SAP38 udskydelse (FR-1.4)**
- Input: kildesystem = DMI_SAP38, registreringsDato = |2024-03-01|
- Expected: udskydelseDato = |2027-11-20|, tidligste fristUdloeb = |2030-11-21|
- Anchor: GIL § 18a, stk. 1, 2. pkt.

**T-07: Udskydelse immutabilitet — afbrydelse ændrer IKKE udskydelseDato (FR-1.5)**
- Input: kildesystem = PSRM, inddrivelsesdato = |2020-01-01| → udskydelseDato = |2021-11-20|
- Event: BEROSTILLELSE with eventDate = |2023-06-01|
- Expected: udskydelseDato = STILL |2021-11-20| (unchanged); nyFristUdloeber = |2026-06-01|
- Anchor: G.A.2.4.1

**T-08: Berostillelse afbrydelse → ny 3-årig frist (FR-2.1)**
- Input: kildesystem = PSRM, afbrydelseType = BEROSTILLELSE, eventDate = |2024-02-15|
- Expected: afbrydelseResultat = NY_FRIST_3_AAR; nyAfbrydelseDato = |2024-02-15|; nyFristUdloeber = |2027-02-15|
- Anchor: GIL § 18a, stk. 8

**T-09: Lønindeholdelsesvarsel AFVIST — ingen ny frist (SKM2015.718.ØLR, FR-2.2 negative)**
- Input: afbrydelseType = LOENINDEHOLDELSE_VARSEL, eventDate = |2024-05-10|, afgoerelseRegistreret = false
- Expected: afbrydelseResultat = AFVIST_VARSEL_ALENE; nyFristUdloeber = null (currentFristExpires UNCHANGED)
- Anchor: GIL § 18, stk. 4; SKM2015.718.ØLR

**T-10: Lønindeholdelse afgørelse → underretningsDato + 3yr (FR-2.2 positive)**
- Input: afbrydelseType = LOENINDEHOLDELSE_AFGOERELSE, afgoerelseRegistreret = true, underretningsDato = |2024-06-20|
- Expected: afbrydelseResultat = NY_FRIST_3_AAR; nyAfbrydelseDato = |2024-06-20|; nyFristUdloeber = |2027-06-20|
- Anchor: GIL § 18, stk. 4

**T-11: Forgæves udlæg = vellykket udlæg — identisk ny frist (FR-2.3)**
- Input A (vellykket): afbrydelseType = UDLAEG, forgaevesUdlaeg = false, eventDate = |2024-03-22|, retsgrundlag = ORDINARY
- Input B (forgæves): afbrydelseType = UDLAEG, forgaevesUdlaeg = true, eventDate = |2024-03-22|, retsgrundlag = ORDINARY
- Expected BOTH: afbrydelseResultat = NY_FRIST_3_AAR; nyFristUdloeber = |2027-03-22|
- Assertion: result(A) = result(B) — no distinction
- Anchor: Forældelsesl. § 18, stk. 1

**T-12: Fordringskompleks propagation med 2 kompleksmedlemmer (FR-3.2)**
- Input: kompleksId = 1, fordringIds = [101, 102] (101=hovedkrav, 102=rente)
- Event: BEROSTILLELSE with eventDate = |2024-04-01| applied to fordring 101
- Expected: BOTH 101 and 102 get nyFristUdloeber = |2027-04-01|
- Anchor: GIL § 18a, stk. 2, 4. pkt.

**T-13: Tillægsfrist max() gren A — currentFristExpires > eventDate (FR-4.3)**
- Input: currentFristExpires = |2026-01-01|, eventDate = |2025-03-15|, tillaegsFristAar = 2
- max(|2026-01-01|, |2025-03-15|) = |2026-01-01|
- Expected: nyFristUdloeber = |2028-01-01|
- Anchor: G.A.2.4.4.2

**T-14: Tillægsfrist max() gren B — eventDate > currentFristExpires (FR-4.3)**
- Input: currentFristExpires = |2025-01-01|, eventDate = |2025-09-30|, tillaegsFristAar = 2
- max(|2025-01-01|, |2025-09-30|) = |2025-09-30|
- Expected: nyFristUdloeber = |2027-09-30|
- Anchor: G.A.2.4.4.2

**T-15: Foreløbig afbrydelse for tomt kompleks → ny 3-årig frist (FR-4.2, GIL § 18a, stk. 7)**
- Input: erTomt = true, eventDate = |2024-07-01| (modtagelsesdato for empty complex)
- Expected: afbrydelseResultat = FORELOEBIG_AFBRYDELSE; nyFristUdloeber = |2027-07-01|
- Anchor: GIL § 18a, stk. 7

**T-16: Lønindeholdelse inaktiv 1 år → ny frist fra inaktivitetsdato (FR-2.2, Rule A-4)**
- Input: afbrydelseType = LOENINDEHOLDELSE_AFGOERELSE, afgoerelseRegistreret = true, underretningsDato = |2023-01-10|, inaktivSiden = |2023-08-01|, eventDate = |2024-08-01| ((eventDate - inaktivSiden) = 1yr ≥ threshold)
- Expected: afbrydelseResultat = NY_FRIST_3_AAR; nyAfbrydelseDato = |2023-08-01| (inaktivSiden, NOT underretningsDato); nyFristUdloeber = |2026-08-01|
- Anchor: GIL § 18, stk. 4
- Note: Verifies that A-4 (inaktivitetsregel) produces fristStart = inaktivSiden, which is different from A-3 (active afgørelse → underretningsDato)

---

## 4. Deliverable D-3: catala/SPIKE-REPORT-070.md

### 4.1 Coverage Table Structure (29 P059 scenarios)

The spike report must contain a table with columns:

| # | P059 scenario name | P059 FR | Catala dækningsstatus | Noter |
|---|--------------------|---------|-----------------------|-------|

Status values: **Dækket** | **Ikke dækket** | **Diskrepans fundet**

### 4.2 Required Sections

1. Coverage table (all 29 P059 scenarios)
2. Gaps section (Catala rules with no P059 scenario)
3. Discrepancies section (explicitly address 5 hotspots)
4. Effort estimate (person-days for full G.A.2.4 encoding)
5. Go/No-Go verdict (with evidence for each criterion)

### 4.3 Five Hotspots to Address Explicitly

1. Varsel vs afgørelse (SKM2015.718.ØLR) — encoded in A-2 (T-09)
2. Fordringskompleks atomicitet — encoded in K-3 (T-12)
3. Udskydelse immutabilitet — encoded in U-3 (T-07)
4. Forgæves udlæg ligestilling — encoded in A-5 (T-11)
5. Tillægsfrist max()-formel — encoded in T-2 (T-13/T-14)

---

## 5. Acceptance Criteria Traceability

NOTE: This table is a reviewer aid only — it is not a required deliverable from any petition FR. It maps acceptance criteria from the outcome contract to specification rules for review convenience.

| AC | FR | Deliverable | Section/Rule |
|----|-----|-------------|-------------|
| AC-1: catala/ga_2_4_foraeldelse.catala_da exists | FR-1 | D-1 | File |
| AC-2: dansk catala_da dialect | NFR-2 | D-1 | §2.1 header |
| AC-3: 3yr + 10yr base frist rules | FR-1.1/1.2 | D-1 | F-1, F-2 |
| AC-4: PSRM udskydelse ≥ 2021-11-20 | FR-1.3 | D-1 | U-1 |
| AC-5: DMI/SAP38 udskydelse ≥ 2027-11-20 | FR-1.4 | D-1 | U-2 |
| AC-6: Udskydelse immutable | FR-1.5 | D-1 | U-3 |
| AC-7: Berostillelse → 3yr, PSRM only | FR-2.1 | D-1 | A-1 |
| AC-8: Varsel REJECTED (SKM2015.718.ØLR) | FR-2.2 | D-1 | A-2 |
| AC-9: Lønindeholdelse afgørelse → underretningsDato | FR-2.2 | D-1 | A-3 |
| AC-10: Forgæves = vellykket udlæg | FR-2.3 | D-1 | A-5 |
| AC-11: 3yr + 10yr udlæg frister | FR-2.3 | D-1 | A-5a, A-5b |
| AC-12: Fordringskompleks propagation + atomicity | FR-3 | D-1 | K-1, K-2, K-3 |
| AC-13: Tillægsfrist max() formula | FR-4.3 | D-1 | T-2 |
| AC-14: catala/tests/ga_foraeldelse_tests.catala_da exists | FR-5 | D-2 | File |
| AC-15: ≥ 10 test cases | FR-5 | D-2 | T-01 to T-16 (16 tests) |
| AC-16: All tests PASS | FR-5 | D-2 | All T-xx |
| AC-17+: SPIKE-REPORT-070.md exists with all required sections | FR-6/FR-7 | D-3 | §4 |
| AC-23: catala ocaml compilation exits with code 0 | NFR-1 | D-1 | §2.1 (file header / compile constraint per ADR-0032) |
| AC-24: No Java source, migration scripts, OpenAPI specs, or Spring Boot configs modified | NFR-4 | D-1, D-2, D-3 | §1 (scope boundary — research spike only) |

---

## 6. Phase 1 Review Gaps (Addressed in Spec)

The following gaps were identified by Phase 1 reviewers and are addressed here:

1. **FR-4.2 (GIL §18a stk.7) absent from feature file** → Added as Rule T-3 and T-15 test case in this spec.
2. **FR-2.2 active lønindeholdelse frist formula not asserted** → Specified as Rule A-3 output: `nyFristUdloeber = underretningsDato + 3yr` (T-10).
3. **Step-level redundancies** (Scenarios 1/21, 8/12, 13/14) → Noted; cosmetic only; not blocking implementation.

---

## 7. No-Go Triggers (Pre-Spike Context — Not Binding)

NOTE: The assessments below are informed pre-spike context only. Binding evidence for each Go/No-Go trigger will be documented in D-3 (SPIKE-REPORT-070.md) as part of FR-7. The spike work itself will determine the actual verdict.

| Trigger | Assessment | Mitigation |
|---------|------------|------------|
| Temporal rules require workarounds | LOW — Catala handles date arithmetic natively | None needed |
| Fordringskompleks propagation requires workarounds | LOW — Catala's universal quantification handles all-or-nothing | K-3 assertion pattern |
| Encoding effort > 4 person-days per G.A. section | MEDIUM — forældelse has 5 interlocking scopes | 3-day timebox; scope boundary pruning |
