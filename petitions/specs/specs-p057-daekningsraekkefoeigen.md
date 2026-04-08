# Implementation Specification — P057: Dækningsrækkefølge (GIL § 4 Payment Application Order)

**Spec ID:** SPEC-P057  
**Petition:** `petitions/petition057-daekningsraekkefoeigen.md`  
**Outcome contract:** `petitions/petition057-daekningsraekkefoeigen-outcome-contract.md`  
**Feature file:** `petitions/petition057-daekningsraekkefoeigen.feature`  
**Solution architecture:** `design/solution-architecture-p057-daekningsraekkefoeigen.md`  
**Status:** Ready for implementation  
**Legal basis:** GIL § 4 stk. 1–4; GIL § 6a stk. 1, stk. 12; GIL § 9 stk. 1, stk. 3; GIL § 10b;
Gæld.bekendtg. § 4 stk. 3; Gæld.bekendtg. § 7; Retsplejelovens § 507; Lov nr. 288/2022  
**ADRs binding this document:** ADR-0004, ADR-0007, ADR-0011, ADR-0014, ADR-0022, ADR-0029,
ADR-0031, ADR-0032  
**Depended on by:** Petition 059 (forældelse), Petition 062 (pro-rata distribution)

---

## Module and package reference

| Module | Base package |
|--------|-------------|
| `opendebt-payment-service` | `dk.ufst.opendebt.payment` |
| `opendebt-caseworker-portal` | `dk.ufst.opendebt.caseworker` |
| `opendebt-rules-engine` | `opendebt-rules-engine/src/main/resources/rules/` |

All file paths in this document are relative to the repository root.

---

## SPEC-FR-1 — Priority categories — GIL § 4, stk. 1

**Source:** Petition 057 FR-1 · Outcome contract FR-1 · GIL § 4 stk. 1 · GIL § 6a stk. 1, 12 ·
GIL § 10b · Lov nr. 288/2022

### 1.1 `PrioritetKategori` enum

**Action:** CREATE  
**File:** `opendebt-payment-service/src/main/java/dk/ufst/opendebt/payment/domain/PrioritetKategori.java`

```java
package dk.ufst.opendebt.payment.domain;

/**
 * Statutory priority categories for GIL § 4, stk. 1 payment application order.
 * Enum ordinal defines sort order (ascending = covered first).
 * ADR-0031: statutory codes are Java enums, not DB/config tables.
 */
public enum PrioritetKategori {
    /** GIL § 4, stk. 1, nr. 1 / GIL § 6a, stk. 1 og stk. 12 */
    RIMELIGE_OMKOSTNINGER,
    /** GIL § 4, stk. 1, nr. 2 / GIL § 10b — includes tvangsbøder (lov nr. 288/2022) */
    BOEDER_TVANGSBOEEDER_TILBAGEBETALING,
    /** GIL § 4, stk. 1, nr. 3 — privatretlige underholdsbidrag (covered before offentlige) */
    UNDERHOLDSBIDRAG_PRIVATRETLIG,
    /** GIL § 4, stk. 1, nr. 3 — offentlige underholdsbidrag */
    UNDERHOLDSBIDRAG_OFFENTLIG,
    /** GIL § 4, stk. 1, nr. 4 — all remaining fordringer */
    ANDRE_FORDRINGER
}
```

**Invariants:**
- Enum ordinal is the sort key: `RIMELIGE_OMKOSTNINGER.ordinal() == 0`, `ANDRE_FORDRINGER.ordinal() == 4`.
- No fordring with a higher ordinal receives any dækning while any fordring with a lower ordinal has `tilbaestaaendeBeloeb > 0`.
- The 5-value split (privatretlig/offentlig for underholdsbidrag) directly encodes the within-category-3 ordering rule.
- Constants must not be extended without a legislative amendment.

> **Compile-time invariant:** `PrioritetKategori` contains exactly 5 constants in the order listed above. Any attempt to use a string value not matching a constant name fails at compile time (ADR-0031).

### 1.2 `collection-priority.drl` — tvangsbøder category fix

**Action:** MODIFY  
**File:** `opendebt-rules-engine/src/main/resources/rules/collection-priority.drl`

Replace the existing rule that assigns tvangsbøder to `ANDRE_FORDRINGER`:

```
// REMOVE (pre-lov nr. 288/2022 — legally incorrect):
rule "tvangsboeeder-category"
    when Fordring(type == "TVANGSBOEDE")
    then setPrioritetKategori(ANDRE_FORDRINGER)

// ADD (lov nr. 288/2022 — mandatory correction for AC-2):
rule "tvangsboeeder-category"
    when Fordring(type == "TVANGSBOEDE")
    then setPrioritetKategori(BOEDER_TVANGSBOEEDER_TILBAGEBETALING)
```

The `PrioritetKategori` enum imported into the DRL must be
`dk.ufst.opendebt.payment.domain.PrioritetKategori` (or a re-export from `opendebt-common`).
String-matching on category names is prohibited (ADR-0031).

### 1.3 Acceptance criteria (FR-1)

| AC | Criterion |
|----|-----------|
| AC-1 | When a debtor has fordringer in categories 1–4 and payment covers less than the total, category-1 fordringer are fully covered before any category-2 fordring is touched; category-2 before category-3; category-3 before category-4. |
| AC-2 | A fordring with `fordringType == "TVANGSBOEDE"` is assigned `prioritetKategori = BOEDER_TVANGSBOEEDER_TILBAGEBETALING`, and receives dækning before any `ANDRE_FORDRINGER` fordring. |

---

## SPEC-FR-2 — Within-category FIFO ordering — GIL § 4, stk. 2

**Source:** Petition 057 FR-2 · Outcome contract FR-2 · GIL § 4 stk. 2

### 2.1 Liquibase migration — `legacy_modtagelsesdato` column

**Action:** CREATE  
**File:** `opendebt-debt-service/src/main/resources/db/changelog/changes/V_P057_001__add_legacy_modtagelsesdato.xml`

```xml
<changeSet id="P057-001" author="p057-daekningsraekkefoelge">
    <addColumn tableName="fordring">
        <column name="legacy_modtagelsesdato" type="DATE">
            <constraints nullable="true"/>
        </column>
    </addColumn>
    <rollback>
        <dropColumn tableName="fordring" columnName="legacy_modtagelsesdato"/>
    </rollback>
</changeSet>
```

**Population rule:** A separate data-migration changeset (not part of this spec) populates
`legacy_modtagelsesdato` from the EFI legacy export for all rows where
`modtagelsesdato < '2013-09-01'`. It runs with `runOnChange="false"`. Rows with
`modtagelsesdato >= '2013-09-01'` receive `NULL`.

### 2.2 FIFO sort key resolution rule

**Scope:** Implemented inside `DaekningsRaekkefoeigenService` (see §5.1 SPEC-FR-6, step 4).

The `fifoSortKey` for a fordring is determined as follows:

```
if (fordring.legacyModtagelsesdato != null
        && fordring.modtagelsesdato.isBefore(LocalDate.of(2013, 9, 1))) {
    fifoSortKey = fordring.legacyModtagelsesdato;
} else {
    fifoSortKey = fordring.modtagelsesdato;
}
```

**Tie-breaking:** When two fordringer in the same `PrioritetKategori` have the same
`fifoSortKey`, they are sub-sorted by `sekvensNummer` ascending (per petition FR-2).
This is stable and reproducible (NFR-1 / ADR-0032).

### 2.3 `fifoSortKey` field in API responses

`fifoSortKey` is a required field in both `DaekningsraekkefoelgePosition` and `SimulatePosition`
response schemas (see §7.1). Its value is the ISO-8601 date used for ordering (either
`legacyModtagelsesdato` or `modtagelsesdato`). It allows consumers to audit the ordering
decision without reconstructing the logic.

### 2.4 Acceptance criteria (FR-2)

| AC | Criterion |
|----|-----------|
| AC-3 | Within a priority category, the fordring with the earlier `fifoSortKey` is covered first. A fordring with `modtagelsesdato = 2024-01-15` is covered before one with `modtagelsesdato = 2024-03-01` in the same category. |
| AC-4 | A fordring with `modtagelsesdato < 2013-09-01` and a non-null `legacyModtagelsesdato` uses `legacyModtagelsesdato` as its `fifoSortKey`. The API response for that position carries `fifoSortKey = legacyModtagelsesdato`, not the overdragelse API timestamp. |
| AC-4b | A fordring with `modtagelsesdato >= 2013-09-01` uses `modtagelsesdato` as its `fifoSortKey`. |

---

## SPEC-FR-3 — Interest ordering within each fordring — Gæld.bekendtg. § 4, stk. 3

**Source:** Petition 057 FR-3 · Outcome contract FR-3 · Gæld.bekendtg. § 4 stk. 3 · GIL § 9 stk. 1, 3

### 3.1 `RenteKomponent` enum

**Action:** CREATE  
**File:** `opendebt-payment-service/src/main/java/dk/ufst/opendebt/payment/domain/RenteKomponent.java`

```java
package dk.ufst.opendebt.payment.domain;

/**
 * Statutory cost component sub-positions for interest-before-principal ordering.
 * Gæld.bekendtg. § 4, stk. 3.
 * Enum ordinal defines coverage order (ascending = covered first).
 * ADR-0031: statutory codes are Java enums, not DB/config tables.
 */
public enum RenteKomponent {
    /** Sub-position 1: Interest accrued during fordringshaver's collection period,
     *  before the fordring was transferred to inddrivelse. */
    OPKRAEVNINGSRENTER,
    /** Sub-position 2: Inddrivelsesrenter calculated by fordringshaver
     *  (GIL § 9, stk. 3, 2. or 4. pkt.). */
    INDDRIVELSESRENTER_FORDRINGSHAVER_STK3,
    /** Sub-position 3: Inddrivelsesrenter accrued before the fordring was returned to
     *  fordringshaver (GIL § 9, stk. 1 or stk. 3). */
    INDDRIVELSESRENTER_FOER_TILBAGEFOERSEL,
    /** Sub-position 4: Standard inddrivelsesrenter (GIL § 9, stk. 1). */
    INDDRIVELSESRENTER_STK1,
    /** Sub-position 5: Øvrige renter calculated by PSRM (GIL § 9, stk. 3, 1. or 3. pkt.). */
    OEVRIGE_RENTER_PSRM,
    /** Sub-position 6: Principal — covered only after all five rente sub-positions are
     *  exhausted. */
    HOVEDFORDRING
}
```

**Invariants:**
- Enum ordinals 0–5 map directly to sub-positions 1–6. Coverage order is `ordinal()` ascending.
- `HOVEDFORDRING` must have the highest ordinal. No principal dækning occurs while any
  lower-ordinal component has `tilbaestaaendeBeloeb > 0`.
- Within sub-positions 2–5, if multiple accrual periods exist for the same component,
  earlier periods are covered before later periods (ascending chronological order of the
  accrual period start date). The period ordering is implemented in the fordring-expansion
  step of the rule engine (§5.1, step 6).
- Constants must not be extended without a statutory change to Gæld.bekendtg. § 4, stk. 3.

### 3.2 Acceptance criteria (FR-3)

| AC | Criterion |
|----|-----------|
| AC-5 | A partial payment reaching a fordring with outstanding `OPKRAEVNINGSRENTER` and `INDDRIVELSESRENTER_STK1` and `HOVEDFORDRING` covers `OPKRAEVNINGSRENTER` first, then `INDDRIVELSESRENTER_STK1`, before any amount reaches `HOVEDFORDRING`. |
| AC-6 | A payment reaching a fordring with all six components outstanding covers sub-positions 1 → 2 → 3 → 4 → 5 in order; the `HOVEDFORDRING` receives dækning only after all five rente sub-positions are fully exhausted. Line-item allocation records appear in ascending sub-position order. |
| AC-6b | Within a single sub-position, an earlier accrual period is fully covered before a later accrual period. |

---

## SPEC-FR-4 — Inddrivelsesindsats rule — GIL § 4, stk. 3

**Source:** Petition 057 FR-4 · Outcome contract FR-4 · GIL § 4 stk. 3 · Retsplejelovens § 507

### 4.1 `InddrivelsesindsatsType` — permitted values

**Action:** CREATE
**File:** `opendebt-payment-service/src/main/java/dk/ufst/opendebt/payment/domain/InddrivelsesindsatsType.java`

`java
package dk.ufst.opendebt.payment.domain;

/**
 * Statutory collection-action types governing GIL § 4, stk. 3 payment ordering.
 * ADR-0031: statutory codes are Java enums, not DB/config tables.
 */
public enum InddrivelsesindsatsType {
    /** Retsplejelovens § 507 — udlæg exception; surplus is retained, not applied to other fordringer. */
    UDLAEG,
    /** GIL § 4, stk. 3 — lønindeholdelse indsats; surplus applied to same-type eligible fordringer. */
    LOENINDEHOLDELSE,
    /** GIL § 4, stk. 3 — modregning indsats; surplus applied to same-type eligible fordringer. */
    MODREGNING,
    /** GIL § 4, stk. 3 — frivillig betaling; no partitioning, normal GIL § 4 stk. 1 order. */
    FRIVILLIG
}
`

The `inddrivelsesindsatsType` field on `DaekningRecord` and on the simulate request carries one of:

| Value | Legal basis |
|-------|-------------|
| `UDLAEG` | Retsplejelovens § 507 — udlæg exception applies |
| `LOENINDEHOLDELSE` | GIL § 4, stk. 3 — indsats-first, surplus to same-type fordringer |
| `MODREGNING` | GIL § 4, stk. 3 — indsats-first, surplus to same-type fordringer |
| `FRIVILLIG` | GIL § 4, stk. 3 — normal ordering; value preserved on DaekningRecord for audit |
| `null` | Normal GIL § 4, stk. 1 ordering without indsats filter |

### 4.2 Inddrivelsesindsats filter — service algorithm

**Scope:** Step 2 of `DaekningsRaekkefoeigenService.computeOrdering()` (see §5.1).

When `inddrivelsesindsatsType` is non-null, the fordring set is partitioned before sorting:

```
IF inddrivelsesindsatsType == "FRIVILLIG":
    // No partition. Process all fordringer in normal GIL § 4 stk. 1 order.
    // Preserve inddrivelsesindsatsType on every resulting DaekningRecord.

ELSE IF inddrivelsesindsatsType != null:
    indsatsFordringer   ← fordringer where fordring.inddrivelsesindsatsType == inddrivelsesindsatsType
                          AND fordring is directly associated with this indsats run
    remainingFordringer ← all other fordringer eligible to receive the same indsats type's surplus

    IF inddrivelsesindsatsType == "UDLAEG":
        remainingFordringer ← []          // udlæg exception: surplus is retained, not applied

    // Process indsatsFordringer first (steps 3–6 of algorithm).
    // After indsatsFordringer exhausted:
    IF inddrivelsesindsatsType != "UDLAEG" AND surplus > 0:
        // Apply surplus to remainingFordringer (same steps 3–6).
    ELSE IF inddrivelsesindsatsType == "UDLAEG" AND surplus > 0:
        // Write a DaekningRecord with daekningBeloeb = 0 and udlaegSurplus = true.
        // No fordring receives the surplus amount.
```

**Audit requirement:** Every `DaekningRecord` produced under an inddrivelsesindsats run
carries:
- `gilParagraf = "GIL § 4, stk. 3"` (in addition to the category basis from stk. 1)
- `inddrivelsesindsatsType` set to the triggering value (never coerced to `null`)

### 4.3 Acceptance criteria (FR-4)

| AC | Criterion |
|----|-----------|
| AC-7 | A `LOENINDEHOLDELSE` payment covers indsats-associated fordringer first (in GIL § 4 stk. 1 priority order among them), then applies surplus to other fordringer eligible for lønindeholdelse. Each `DaekningRecord` carries `gilParagraf = "GIL § 4, stk. 3"`. |
| AC-8 | A `UDLAEG` payment surplus after udlæg-fordringer are fully covered is flagged `udlaegSurplus = true` on the retained `DaekningRecord`. No `DaekningRecord` with `daekningBeloeb > 0` is written for any non-udlæg fordring. |
| AC-8b | `FRIVILLIG` inddrivelsesindsatsType produces no partition; fordringer are covered in normal GIL § 4 stk. 1 order. The value `FRIVILLIG` appears on every resulting `DaekningRecord.inddrivelsesindsatsType`. |

---

## SPEC-FR-5 — Opskrivningsfordring positioning

**Source:** Petition 057 FR-5 · Outcome contract FR-5 · Gæld.bekendtg. § 7 · G.A.2.3.2

### 5.1 Opskrivningsfordring positioning algorithm

**Scope:** Step 5 of `DaekningsRaekkefoeigenService.computeOrdering()` (see §6.1).

After the initial FIFO sort (step 4), opskrivningsfordringer are repositioned:

```
FOR each opskrivningsfordring O (where O.opskrivningAfFordringId != null):

    parent ← fordring with id == O.opskrivningAfFordringId

    IF parent is present in the sorted list (tilbaestaaendeBeloeb > 0):
        Insert O immediately after parent in the list.

    ELSE (parent is absent — fully covered, saldo == 0):
        Compute O.effectiveFifoSortKey = parent.fifoSortKey   // same key as parent
        Insert O at the position where parent would appear if still active
        (i.e., after all fordringer with fifoSortKey < parent.fifoSortKey,
         before all fordringer with fifoSortKey > parent.fifoSortKey).

    // Multiple opskrivningsfordringer for the same parent:
    // Sub-sort among siblings by ascending O.modtagelsesdato (FIFO on the opskrivningsfordring's
    // own receipt date). Within that sub-sort, ties broken by O.sekvensNummer ascending (per petition FR-2 tie-break convention).
```

**Constraint:** An opskrivningsfordring is never placed at the bottom of its priority category
regardless of its own `modtagelsesdato`. The parent-relative positioning rule overrides the
opskrivningsfordring's own FIFO position.

**Interest ordering on opskrivningsfordringer:** Each opskrivningsfordring is expanded into
`RenteKomponent` sub-positions following the same FR-3 rule as any other fordring (step 6 of
the algorithm). The `INDDRIVELSESRENTER_*` sub-positions of an opskrivningsfordring are covered
before its `HOVEDFORDRING`.

### 5.2 `opskrivningAfFordringId` in API response

Every position in the `GET` and `POST simulate` response includes:

```
opskrivningAfFordringId: UUID | null
```

- `null` for standard fordringer (not opskrivningsfordringer).
- UUID of the parent fordring for opskrivningsfordring positions.

This field is required in both `DaekningsraekkefoelgePosition` and `SimulatePosition` schemas
(see §7.1).

### 5.3 Acceptance criteria (FR-5)

| AC | Criterion |
|----|-----------|
| AC-9 | An opskrivningsfordring appears at rank `n+1` immediately after its parent fordring at rank `n` in the ordered list. The entry carries `opskrivningAfFordringId` set to the parent's UUID. |
| AC-10 | When a parent fordring has `tilbaestaaendeBeloeb == 0` (fully covered, absent from active list), the opskrivningsfordring occupies the position the parent would have had (same `fifoSortKey`), not the bottom of the category. |
| AC-11 | When a parent fordring has two opskrivningsfordringer, they appear at consecutive ranks after the parent, ordered by their own ascending `modtagelsesdato` (FIFO on opskrivningsfordring receipt date). |
| AC-11b | Inddrivelsesrenter on an opskrivningsfordring are fully covered before its `HOVEDFORDRING`, following the FR-3 interest sub-position sequence. |

---

## SPEC-FR-6 — Timing and `DaekningRecord` entity — GIL § 4, stk. 4

**Source:** Petition 057 FR-6 · Outcome contract FR-6, AC-12, AC-13 · GIL § 4 stk. 4

### 6.1 `DaekningsRaekkefoeigenService` — method signature and ordering algorithm

**Action:** CREATE  
**File:** `opendebt-payment-service/src/main/java/dk/ufst/opendebt/payment/service/DaekningsRaekkefoeigenService.java`

Public method signatures:

```java
package dk.ufst.opendebt.payment.service;

import dk.ufst.opendebt.payment.domain.InddrivelsesindsatsType;
import dk.ufst.opendebt.payment.dto.DaekningsraekkefoelgeResponse;
import dk.ufst.opendebt.payment.dto.SimulateResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public interface DaekningsRaekkefoeigenService {

    /**
     * Returns the current ordered list of active fordringer for a debtor.
     * No dækning is applied. No DaekningRecord is written.
     *
     * @param debtorId        Debtor person_id UUID (no CPR — ADR-0014)
     * @param asOf            Optional snapshot date; null = current state
     * @return                Ordered positions (GIL § 4 rule applied)
     */
    DaekningsraekkefoelgeResponse getOrdering(UUID debtorId, LocalDate asOf);

    /**
     * Simulates payment application. Returns projected allocation.
     * No DaekningRecord is written. No immudb append. (AC-15, FR-7)
     *
     * @param debtorId                  Debtor person_id UUID
     * @param beloeb                    Payment amount to simulate (must be > 0)
     * @param betalingstidspunkt        Legal effect timestamp of the payment
     * @param applicationTimestamp      Injected timestamp for determinism (NFR-1, ADR-0032)
     * @param inddrivelsesindsatsType   Optional; null = normal GIL § 4 stk. 1 order
     */
    SimulateResponse simulate(
        UUID debtorId,
        BigDecimal beloeb,
        Instant betalingstidspunkt,
        Instant applicationTimestamp,
        InddrivelsesindsatsType inddrivelsesindsatsType
    );

    /**
     * Applies payment. Writes DaekningRecord rows and appends to immudb.
     * This method is transactional — immudb failure rolls back PostgreSQL writes.
     *
     * @param debtorId                  Debtor person_id UUID
     * @param beloeb                    Payment amount
     * @param betalingstidspunkt        Legal effect timestamp
     * @param applicationTimestamp      Injected — must not be Instant.now() inside the method
     * @param inddrivelsesindsatsType   Optional
     */
    DaekningsraekkefoelgeResponse apply(
        UUID debtorId,
        BigDecimal beloeb,
        Instant betalingstidspunkt,
        Instant applicationTimestamp,
        InddrivelsesindsatsType inddrivelsesindsatsType
    );
}
```

**Algorithm — 8-step ordering (binding for the implementation class):**

**Step 1 — Fetch active fordringer at application time (GIL § 4, stk. 4 / FR-6)**  
Call `GET /internal/debtors/{debtorId}/fordringer/active` on `debt-service` using
service-to-service OAuth2 client credentials (scope: `debt-service:internal-read`).
The fetch happens at `applicationTimestamp` (T3), not at `betalingstidspunkt` (T1).
Fordringer that arrived between T1 and T3 are included. If `debt-service` is
unavailable, raise `DaekningUnavailableException` (HTTP 503); do not proceed with
stale or partial data.
When `asOf` is provided, forward it as a query parameter `asOf={date}` to the internal endpoint. `debt-service` must support this parameter (part of DEP-1).

**Step 2 — Apply inddrivelsesindsats filter (GIL § 4, stk. 3 / FR-4)**  
Partition fordringer per the algorithm in §4.2. If `inddrivelsesindsatsType == null`
or `"FRIVILLIG"`, skip partitioning and process all fordringer together.

**Step 3 — Sort by `PrioritetKategori` ordinal (GIL § 4, stk. 1 / FR-1)**  
Sort ascending by `PrioritetKategori.ordinal()`. No fordring at ordinal N+1 is
covered while any fordring at ordinal N has `tilbaestaaendeBeloeb > 0`.

**Step 4 — Sort within category by `fifoSortKey` (GIL § 4, stk. 2 / FR-2)**  
Within each `PrioritetKategori` group, sort ascending by `fifoSortKey` (see §2.2).
Ties broken by `sekvensNummer` ascending (per petition FR-2).

**Step 5 — Reposition opskrivningsfordringer (Gæld.bekendtg. § 7 / FR-5)**  
Apply the positioning algorithm in §5.1. This is a post-sort mutation of the list —
opskrivningsfordringer are extracted from their FIFO position and re-inserted after
their parent.

**Step 6 — Expand each fordring into `RenteKomponent` sub-positions (Gæld.bekendtg. § 4, stk. 3 / FR-3)**  
For each fordring in the sorted list, emit one `position` per `RenteKomponent` with
`tilbaestaaendeBeloeb > 0`, in ascending `RenteKomponent.ordinal()` order. Within
sub-positions 2–5, if multiple accrual periods exist for the same component, emit one
position per period in ascending chronological order of period start date. The resulting
flat list is the `positions` array returned by the GET endpoint.

**Step 7 — Apply payment amount sequentially**

```
availableBeloeb = beloeb
for position in positions:
    if availableBeloeb <= BigDecimal.ZERO:
        break
    allocated = min(availableBeloeb, position.tilbaestaaendeBeloeb)
    position.daekningBeloeb = allocated
    position.fullyCovers = (allocated.compareTo(position.tilbaestaaendeBeloeb) >= 0)
    availableBeloeb = availableBeloeb.subtract(allocated)
```

For `simulate`: return `SimulateResponse`. No DB write. No immudb append.

For `apply`: write one `DaekningRecord` per position where `daekningBeloeb > 0`,
then append all records to immudb in a single gRPC batch inside the `@Transactional` boundary.

**Step 8 — Assign `gilParagraf` per position**

| Position type | `gilParagraf` value |
|---------------|---------------------|
| Category 1 fordring | `"GIL § 4, stk. 1, nr. 1"` |
| Category 2 fordring | `"GIL § 4, stk. 1, nr. 2"` |
| Category 3 (privatretlig) | `"GIL § 4, stk. 1, nr. 3"` |
| Category 3 (offentlig) | `"GIL § 4, stk. 1, nr. 3"` |
| Category 4 fordring | `"GIL § 4, stk. 1, nr. 4"` |
| Inddrivelsesindsats-triggered allocation | `"GIL § 4, stk. 3"` |

**Determinism requirement (NFR-1 / ADR-0032):**
- `applicationTimestamp` is injected by the caller — never `Instant.now()` inside the method.
- All sort operations use enum ordinals or `sekvensNummer` ascending — no random tiebreakers.
- The algorithm is pure and stateless during steps 1–6 (no DB reads after step 1).

### 6.2 `DaekningRecord` JPA entity

**Action:** CREATE  
**File:** `opendebt-payment-service/src/main/java/dk/ufst/opendebt/payment/domain/DaekningRecord.java`

| Field | Java type | Column | Constraints |
|-------|-----------|--------|-------------|
| `id` | `UUID` | `id` | PK, NOT NULL, generated |
| `fordringId` | `UUID` | `fordring_id` | NOT NULL — logical reference only (ADR-0007) |
| `komponent` | `RenteKomponent` (enum, persisted as `VARCHAR(64)`) | `komponent` | NOT NULL |
| `daekningBeloeb` | `BigDecimal` | `daekning_beloeb` | `NUMERIC(19,2)`, NOT NULL, `>= 0` |
| `betalingstidspunkt` | `Instant` | `betalingstidspunkt` | `TIMESTAMPTZ`, NOT NULL |
| `applicationTimestamp` | `Instant` | `application_timestamp` | `TIMESTAMPTZ`, NOT NULL |
| `gilParagraf` | `String` | `gil_paragraf` | `VARCHAR(64)`, NOT NULL |
| `prioritetKategori` | `PrioritetKategori` (enum, persisted as `VARCHAR(64)`) | `prioritet_kategori` | NOT NULL |
| `fifoSortKey` | `LocalDate` | `fifo_sort_key` | `DATE`, NOT NULL |
| `udlaegSurplus` | `boolean` | `udlaeg_surplus` | `BOOLEAN`, default `false` |
| `inddrivelsesindsatsType` | `InddrivelsesindsatsType` (enum, persisted as `VARCHAR(32)`, `@Enumerated`) | `inddrivelsesindsats_type` | `VARCHAR(32)`, nullable |
| `opskrivningAfFordringId` | `UUID` | `opskrivning_af_fordring_id` | `UUID`, nullable |
| `createdBy` | `String` | `created_by` | `VARCHAR(128)`, NOT NULL — OAuth2 token subject |
| `createdAt` | `Instant` | `created_at` | `TIMESTAMPTZ`, NOT NULL, default `NOW()` |
**Entity invariants:**
- `daekningBeloeb >= 0`. A zero-beloeb record is only written when `udlaegSurplus = true`
  to represent retained udlæg surplus. Rows representing actual dækning have `daekningBeloeb > 0`.
- `betalingstidspunkt <= applicationTimestamp` (payment precedes or equals application).
- `fifoSortKey` equals `legacyModtagelsesdato` for pre-2013 fordringer; equals
  `modtagelsesdato` otherwise.
- No `@Column(foreignKey)` to `opendebt_debt`; `fordringId` is a logical reference only.

### 6.3 Liquibase migration — `daekning_record` table

**Action:** CREATE  
**File:** `opendebt-payment-service/src/main/resources/db/changelog/changes/V_P057_002__create_daekning_record.xml`

```xml
<changeSet id="P057-002" author="p057-daekningsraekkefoelge">
    <createTable tableName="daekning_record">
        <column name="id" type="UUID">
            <constraints primaryKey="true" nullable="false"/>
        </column>
        <column name="fordring_id" type="UUID">
            <constraints nullable="false"/>
        </column>
        <column name="komponent" type="VARCHAR(64)">
            <constraints nullable="false"/>
        </column>
        <column name="daekning_beloeb" type="NUMERIC(19,2)">
            <constraints nullable="false"/>
        </column>
        <column name="betalingstidspunkt" type="TIMESTAMPTZ">
            <constraints nullable="false"/>
        </column>
        <column name="application_timestamp" type="TIMESTAMPTZ">
            <constraints nullable="false"/>
        </column>
        <column name="gil_paragraf" type="VARCHAR(64)">
            <constraints nullable="false"/>
        </column>
        <column name="prioritet_kategori" type="VARCHAR(64)">
            <constraints nullable="false"/>
        </column>
        <column name="fifo_sort_key" type="DATE">
            <constraints nullable="false"/>
        </column>
        <column name="udlaeg_surplus" type="BOOLEAN" defaultValueBoolean="false"/>
        <column name="inddrivelsesindsats_type" type="VARCHAR(32)"/>
        <column name="opskrivning_af_fordring_id" type="UUID"/>
        <column name="created_by" type="VARCHAR(128)">
            <constraints nullable="false"/>
        </column>
        <column name="created_at" type="TIMESTAMPTZ" defaultValueComputed="NOW()">
            <constraints nullable="false"/>
        </column>
    </createTable>

    <sql>
        ALTER TABLE daekning_record
            ADD CONSTRAINT chk_inddrivelsesindsats_type
            CHECK (inddrivelsesindsats_type IS NULL OR
                   inddrivelsesindsats_type IN ('UDLAEG', 'LOENINDEHOLDELSE', 'MODREGNING', 'FRIVILLIG'));
    </sql>

    <createIndex tableName="daekning_record" indexName="idx_daekning_fordring_id">
        <column name="fordring_id"/>
    </createIndex>

    <createIndex tableName="daekning_record" indexName="idx_daekning_betalingstidspunkt">
        <column name="betalingstidspunkt"/>
    </createIndex>

    <rollback>
        <dropTable tableName="daekning_record"/>
    </rollback>
</changeSet>
```

### 6.4 immudb audit append — required fields per entry

Every actual payment application (not `simulate`) appends to immudb via gRPC.
Each entry must carry all eight fields listed in AC-13:

| immudb field | Source |
|--------------|--------|
| `fordringId` | `DaekningRecord.fordringId` |
| `komponent` | `DaekningRecord.komponent.name()` |
| `daekningBeloeb` | `DaekningRecord.daekningBeloeb` |
| `betalingstidspunkt` | `DaekningRecord.betalingstidspunkt` (ISO-8601) |
| `applicationTimestamp` | `DaekningRecord.applicationTimestamp` (ISO-8601) |
| `gilParagraf` | `DaekningRecord.gilParagraf` |
| `prioritetKategori` | `DaekningRecord.prioritetKategori.name()` |
| `fifoSortKey` | `DaekningRecord.fifoSortKey` (ISO-8601 date) |

**Transactional boundary:** The Spring `@Transactional` annotation wraps both the PostgreSQL
`INSERT` of `DaekningRecord` rows and the immudb gRPC batch append. If the immudb call
fails, the transaction is rolled back and a `DaekningAuditException` is thrown. A dækning
record without a tamper-proof audit entry is legally unacceptable.

`simulate` does NOT append to immudb. The service method invoked by the simulate endpoint
(`simulate()`) has no immudb dependency. The controller layer enforces this by calling
`service.simulate()` for `POST .../simulate` and `service.apply()` for actual application.

### 6.5 Acceptance criteria (FR-6)

| AC | Criterion |
|----|-----------|
| AC-12 | A fordring that arrives at time T2, where `betalingstidspunkt T1 < T2 < applicationTimestamp T3`, is included in the ordered list when the rule engine runs at T3. The fordring is not excluded on the grounds that it post-dates T1. |
| AC-13 | Every `DaekningRecord` carries both `betalingstidspunkt` and `applicationTimestamp` as non-null fields. The CLS (immudb) audit log entry for each dækning event includes all eight required fields: `fordringId`, `komponent`, `daekningBeloeb`, `betalingstidspunkt`, `applicationTimestamp`, `gilParagraf`, `prioritetKategori`, `fifoSortKey`. |
| AC-13b | `simulate` produces no `DaekningRecord` rows and appends nothing to immudb. |

---

## SPEC-FR-7 — Payment application API

**Source:** Petition 057 FR-7 · Outcome contract FR-7, AC-14, AC-15

### 7.1 `DaekningsRaekkefoeigenController` — endpoints

**Action:** CREATE  
**File:** `opendebt-payment-service/src/main/java/dk/ufst/opendebt/payment/controller/DaekningsRaekkefoeigenController.java`

```java
@RestController
@RequestMapping("/debtors/{debtorId}")
@SecurityRequirement(name = "bearerAuth", scopes = {"payment-service:read"})
public class DaekningsRaekkefoeigenController {

    @GetMapping("/daekningsraekkefoelge")
    @ResponseStatus(HttpStatus.OK)
    public DaekningsraekkefoelgeResponse getOrdering(
        @PathVariable UUID debtorId,
        @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate asOf
    );

    @PostMapping("/daekningsraekkefoelge/simulate")
    @ResponseStatus(HttpStatus.OK)
    public SimulateResponse simulate(
        @PathVariable UUID debtorId,
        @RequestBody @Valid SimulateRequest request
    );
}
```

**Auth:** Keycloak Bearer token, scope `payment-service:read`. Role must grant access to
the debtor. Debtor-level access is delegated to `debt-service`: if `debt-service` returns
HTTP 403 for the fordring fetch, `payment-service` propagates HTTP 403.

**GET `/debtors/{debtorId}/daekningsraekkefoelge` — response `DaekningsraekkefoelgeResponse`:**

```json
{
  "debtorId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "positions": [
    {
      "rank": 1,
      "fordringId": "a1b2c3d4-...",
      "fordringshaverId": "f0e1d2c3-...",
      "prioritetKategori": "RIMELIGE_OMKOSTNINGER",
      "gilParagraf": "GIL § 4, stk. 1, nr. 1",
      "komponent": "OPKRAEVNINGSRENTER",
      "tilbaestaaendeBeloeb": 1250.00,
      "modtagelsesdato": "2022-03-15",
      "fifoSortKey": "2022-03-15",
      "opskrivningAfFordringId": null
    }
  ]
}
```

Required fields per position (every field is required; absence = failure condition from AC-14):

| Field | Type | Legal basis |
|-------|------|-------------|
| `rank` | integer (1-based) | — |
| `fordringId` | UUID | — |
| `fordringshaverId` | UUID | ADR-0014: no CPR |
| `prioritetKategori` | `PrioritetKategori` enum name | GIL § 4, stk. 1 |
| `gilParagraf` | string | GIL § 4, stk. 1–4 |
| `komponent` | `RenteKomponent` enum name | Gæld.bekendtg. § 4, stk. 3 |
| `tilbaestaaendeBeloeb` | decimal | — |
| `modtagelsesdato` | ISO-8601 date | GIL § 4, stk. 2 |
| `fifoSortKey` | ISO-8601 date | GIL § 4, stk. 2 (FR-2) |
| `opskrivningAfFordringId` | UUID or null | Gæld.bekendtg. § 7 (FR-5) |

**GET error responses:**

| HTTP | Condition |
|------|-----------|
| 400 | `asOf` is not a valid ISO-8601 date |
| 403 | Token present but caller lacks access to debtor |
| 404 | Debtor UUID not found in `debt-service` |
| 503 | `debt-service` unavailable (`DaekningUnavailableException`) |

**POST `/debtors/{debtorId}/daekningsraekkefoelge/simulate` — request `SimulateRequest`:**

```json
{
  "beloeb": 5000.00,
  "inddrivelsesindsatsType": "LOENINDEHOLDELSE"
}
```

| Field | Type | Required | Constraint |
|-------|------|----------|------------|
| `beloeb` | `BigDecimal` | Yes | Must be `> 0`; HTTP 422 if zero or negative |
| `inddrivelsesindsatsType` | string | No | One of `UDLAEG`, `LOENINDEHOLDELSE`, `MODREGNING`, `FRIVILLIG`; null = normal ordering |

**POST response `SimulateResponse`** — same fields as GET positions plus:

| Additional field | Type | Description |
|-----------------|------|-------------|
| `daekningBeloeb` | decimal | Projected amount allocated to this position |
| `fullyCovers` | boolean | `true` iff `daekningBeloeb >= tilbaestaaendeBeloeb` |

**POST success:** HTTP **200** (not 201 — no resource is created).

**POST error responses:**

| HTTP | Condition |
|------|-----------|
| 422 | `beloeb` is zero or negative — RFC 7807 ProblemDetail |
| 403 | Caller lacks access to debtor |
| 404 | Debtor not found |

**HTTP 422 error shape:**

```json
{
  "type": "https://opendebt.ufst.dk/problems/validation-failure",
  "title": "Unprocessable Entity",
  "status": 422,
  "detail": "beloeb must be greater than zero"
}
```

### 7.2 OpenAPI 3.1 specification

**Action:** CREATE  
**File:** `opendebt-payment-service/src/main/resources/openapi/daekningsraekkefoelge.yaml`

Required additions (ADR-0004 — API-first: spec must be committed before implementation):

1. **New path:** `GET /debtors/{debtorId}/daekningsraekkefoelge`
2. **New path:** `POST /debtors/{debtorId}/daekningsraekkefoelge/simulate`
3. **New schemas:**
   - `DaekningsraekkefoelgeResponse` — GET response wrapper
   - `DaekningsraekkefoelgePosition` — position element with all required fields
   - `SimulateRequest` — simulate request body
   - `SimulateResponse` — simulate response wrapper
   - `SimulatePosition` — extends `DaekningsraekkefoelgePosition` with `daekningBeloeb`, `fullyCovers`
4. **New enum schemas:**
   - `PrioritetKategoriDto` — 5 values
   - `RenteKomponentDto` — 6 values
   - `InddrivelsesindsatsTypeDto` — 4 values: `UDLAEG`, `LOENINDEHOLDELSE`, `MODREGNING`, `FRIVILLIG`
5. **Security requirement:** `payment-service:read` scope on both paths
6. **`description` fields** on all schema properties that have a legal basis, citing the
   specific GIL § 4 article (UFST documentation standard).

### 7.3 Debt-service internal endpoint dependency

`DaekningsRaekkefoeigenService` fetches fordring data from `debt-service` via REST (ADR-0007).
The following endpoint is a hard dependency — P057 cannot go live without it:

**`GET /internal/debtors/{debtorId}/fordringer/active`** — owned by `opendebt-debt-service`.  
Not exposed through the public API gateway. Called with service-to-service OAuth2 client
credentials (scope: `debt-service:internal-read`).

**Response schema (OpenAPI fragment, owned by `debt-service`):**

```yaml
FordringForDaekningDto:
  type: object
  required: [id, fordringshaverId, prioritetKategori, modtagelsesdato, sekvensNummer, komponenter]
  properties:
    id:
      type: string
      format: uuid
    fordringshaverId:
      type: string
      format: uuid
    prioritetKategori:
      $ref: '#/components/schemas/PrioritetKategoriDto'
    modtagelsesdato:
      type: string
      format: date
    sekvensNummer:
      type: integer
      format: int64
      description: "Internal system sequence number for FIFO tie-breaking within a priority category (petition FR-2)"
    legacyModtagelsesdato:
      type: string
      format: date
      nullable: true
      description: "FIFO sort date for fordringer received before 2013-09-01 (FR-2, AC-4)"
    opskrivningAfFordringId:
      type: string
      format: uuid
      nullable: true
    inddrivelsesindsatsType:
      type: string
      nullable: true
    komponenter:
      type: array
      items:
        $ref: '#/components/schemas/KomponentSaldoDto'

KomponentSaldoDto:
  type: object
  required: [komponent, tilbaestaaendeBeloeb]
  properties:
    komponent:
      $ref: '#/components/schemas/RenteKomponentDto'
    tilbaestaaendeBeloeb:
      type: number
      format: decimal
```

This endpoint must be tracked as a sub-petition or technical backlog item against
`opendebt-debt-service`. Its implementation is a blocker for P057.

### 7.4 Acceptance criteria (FR-7)

| AC | Criterion |
|----|-----------|
| AC-14 | `GET /debtors/{debtorId}/daekningsraekkefoelge` returns HTTP 200 with an ordered array; each position includes `fordringId`, `fordringshaverId`, `prioritetKategori`, `gilParagraf`, `komponent`, `tilbaestaaendeBeloeb`, `modtagelsesdato`, `fifoSortKey`, `opskrivningAfFordringId`. |
| AC-14b | `GET` with a valid `asOf` date returns the ordering as it would have been at that date. |
| AC-14c | `GET` without `payment-service:read` scope returns HTTP 403. |
| AC-14d | `GET` for an unknown `debtorId` returns HTTP 404. |
| AC-15 | `POST .../simulate` returns HTTP 200 with projected `daekningBeloeb` and `fullyCovers` per position. No `DaekningRecord` is persisted. The sum of all `daekningBeloeb` values in the response equals the lesser of `beloeb` and the total outstanding balance. |
| AC-15b | `POST .../simulate` with `beloeb <= 0` returns HTTP 422 with RFC 7807 ProblemDetail body. |

---

## SPEC-FR-8 — Sagsbehandler portal — dækningsrækkefølge view

**Source:** Petition 057 FR-8 · Outcome contract FR-8, AC-16

### 8.1 `DaekningsRaekkefoeigenViewController`

**Action:** CREATE  
**File:** `opendebt-caseworker-portal/src/main/java/dk/ufst/opendebt/caseworker/controller/DaekningsRaekkefoeigenViewController.java`

```java
@Controller
@RequestMapping("/cases/{caseId}/daekningsraekkefoelge")
public class DaekningsRaekkefoeigenViewController {

    @GetMapping
    public String show(
        @PathVariable String caseId,
        Model model,
        OAuth2AuthorizedClient paymentServiceClient
    ) {
        // 1. Resolve debtorId from caseId via case-service (existing pattern).
        // 2. Call payment-service GET /debtors/{debtorId}/daekningsraekkefoelge
        //    using the caseworker's on-behalf-of token (payment-service:read scope).
        // 3. Add response as "positions" model attribute.
        // 4. Return "debtor/daekningsraekkefoelge" (template name).
    }
}
```

**Constraints:**
- The view is read-only. No `POST` handler. No dækning or payment actions may be initiated
  from this controller or its template.
- The controller is reachable from the debtor case overview via a link added to the
  existing case overview template (see §8.2).
- Token forwarding: the caseworker session token is forwarded to `payment-service` using the
  on-behalf-of pattern (existing portal OAuth2 client configuration). The portal does not
  use its own client credentials for this call.
- If `payment-service` returns HTTP 403, the portal returns HTTP 403 to the caseworker.
- If `payment-service` returns HTTP 404, the portal shows an empty state with an
  informational message (no stack trace).

### 8.2 Thymeleaf view template — `daekningsraekkefoelge.html`

**Action:** CREATE  
**File:** `opendebt-caseworker-portal/src/main/resources/templates/debtor/daekningsraekkefoelge.html`

**Required rendering elements:**

1. **Numbered table** of positions, ordered by `rank` as returned by the API (ascending).

2. **Per-row columns:**

   | Column | Source field | Notes |
   |--------|-------------|-------|
   | Rank | `position.rank` | 1-based integer |
   | Fordring reference | `position.fordringId` | Displayed as UUID or short reference |
   | Fordringshaver | `position.fordringshaverId` | Resolved to creditor name via existing creditor lookup; UUID fallback if lookup fails |
   | Kategori | `position.prioritetKategori` | **Translated Danish label** via i18n key (see §9); must not display enum name |
   | Komponent | `position.komponent` | **Translated Danish label** via i18n key (see §9); must not display enum name |
   | Udestående beløb | `position.tilbaestaaendeBeloeb` | Formatted as Danish decimal |
   | Modtagelsesdato | `position.fifoSortKey` | ISO-8601 date displayed in Danish locale format |

3. **Opskrivningsfordring visual indicator:**
   - When `position.opskrivningAfFordringId != null`, the row is visually distinguished
     (e.g., indented or marked with a badge).
   - The row displays a reference linking it to the parent fordring at rank `n-1`
     (the parent's `fordringId` is displayed as a non-clickable reference or anchor).
   - Failure condition: the indicator is absent, or the row does not display the parent
     reference.

4. **Read-only enforcement:** No form, button, or link on this page initiates a dækning,
   payment, or any state-changing action. The template is GET-only; no `th:action` or
   `<form method="post">` elements are present.

5. **Navigation:** The caseworker reaches this view from a link on the existing debtor case
   overview page. A `<a th:href="@{/cases/{id}/daekningsraekkefoelge(id=${caseId})}">` link
   must be added to the case overview template.

### 8.3 Acceptance criteria (FR-8)

| AC | Criterion |
|----|-----------|
| AC-16 | The sagsbehandler portal displays the ordered list with translated Danish GIL § 4 priority category labels (e.g., `"Underholdsbidrag — privatretlig"`, not `"UNDERHOLDSBIDRAG_PRIVATRETLIG"`) and translated cost component labels (e.g., `"Opkrævningsrenter"`, `"Inddrivelsesrenter § 9, stk. 1"`, `"Hovedfordring"`). |
| AC-16b | Opskrivningsfordring rows display a visual indicator and a reference to the parent fordring. |
| AC-16c | The view is read-only — no dækning or payment action is accessible from this view. |
| AC-16d | The dækningsrækkefølge view is reachable from the debtor case overview page via a navigable link. |
| AC-16e | The priority category is displayed as a translated label, not as the enum constant name. |

---

## SPEC-i18n — New message keys

**Source:** Outcome contract AC-17 · FR-8

**Files to modify:**
- `opendebt-caseworker-portal/src/main/resources/messages_da.properties`
- `opendebt-caseworker-portal/src/main/resources/messages_en_GB.properties`

Both files must receive all 11 new keys. The CI bundle-lint check fails the build if any key
present in `messages_da.properties` is absent from `messages_en_GB.properties` or vice versa.

**`PrioritetKategori` label keys:**

| Key | DA value | EN value |
|-----|----------|----------|
| `daekningsraekkefoelge.kategori.RIMELIGE_OMKOSTNINGER` | `Rimelige omkostninger (udenretlig inddrivelse i udlandet)` | `Reasonable costs (non-judicial recovery abroad)` |
| `daekningsraekkefoelge.kategori.BOEDER_TVANGSBOEEDER_TILBAGEBETALING` | `Bøder, tvangsbøder og tilbagebetalingskrav` | `Fines, coercive fines and repayment claims` |
| `daekningsraekkefoelge.kategori.UNDERHOLDSBIDRAG_PRIVATRETLIG` | `Underholdsbidrag — privatretlig` | `Maintenance payments — private law` |
| `daekningsraekkefoelge.kategori.UNDERHOLDSBIDRAG_OFFENTLIG` | `Underholdsbidrag — offentlig` | `Maintenance payments — public` |
| `daekningsraekkefoelge.kategori.ANDRE_FORDRINGER` | `Andre fordringer` | `Other claims` |

**`RenteKomponent` label keys:**

| Key | DA value | EN value |
|-----|----------|----------|
| `daekningsraekkefoelge.komponent.OPKRAEVNINGSRENTER` | `Opkrævningsrenter` | `Collection-period interest` |
| `daekningsraekkefoelge.komponent.INDDRIVELSESRENTER_FORDRINGSHAVER_STK3` | `Inddrivelsesrenter (fordringshaver, § 9, stk. 3)` | `Recovery interest (creditor, § 9, para. 3)` |
| `daekningsraekkefoelge.komponent.INDDRIVELSESRENTER_FOER_TILBAGEFOERSEL` | `Inddrivelsesrenter (før tilbageførelse)` | `Recovery interest (before return to creditor)` |
| `daekningsraekkefoelge.komponent.INDDRIVELSESRENTER_STK1` | `Inddrivelsesrenter § 9, stk. 1` | `Recovery interest § 9, para. 1` |
| `daekningsraekkefoelge.komponent.OEVRIGE_RENTER_PSRM` | `Øvrige renter (PSRM)` | `Other interest (PSRM)` |
| `daekningsraekkefoelge.komponent.HOVEDFORDRING` | `Hovedfordring` | `Principal` |

**Key resolution in template:**

The Thymeleaf template resolves translated labels using:
```html
th:text="#{|daekningsraekkefoelge.kategori.${position.prioritetKategori}|}"
th:text="#{|daekningsraekkefoelge.komponent.${position.komponent}|}"
```

**AC-17 verification:** AC-17 is verified by the CI bundle-lint check (build fails if any
key in `messages_da.properties` is absent from `messages_en_GB.properties` and vice versa),
not by a Gherkin scenario.

---

## Liquibase migration sequencing

| Order | Changeset | Service DB | Blocking dependency |
|-------|-----------|------------|---------------------|
| 1 | `V_P057_001` — add `legacy_modtagelsesdato` | `opendebt_debt` | None — additive column |
| 2 | EFI data migration — populate pre-2013 dates | `opendebt_debt` | Must run after V_P057_001 |
| 3 | `V_P057_002` — create `daekning_record` table | `opendebt_payment` | None — new table |

Migrations 1 and 3 are safe to deploy before application code is active (additive, no
breaking changes to existing tables). Migration 2 is a one-time historical data load and
must be verified against the available EFI export before execution.

---

## API interface contract summary

### GET `/debtors/{debtorId}/daekningsraekkefoelge`

| Property | Value |
|----------|-------|
| Auth | Bearer token, scope `payment-service:read` |
| Success | HTTP 200 + `DaekningsraekkefoelgeResponse` |
| Errors | 400 (bad `asOf`), 403 (access denied), 404 (debtor not found), 503 (debt-service unavailable) |

### POST `/debtors/{debtorId}/daekningsraekkefoelge/simulate`

| Property | Value |
|----------|-------|
| Auth | Bearer token, scope `payment-service:read` |
| Consumes | `application/json` — `SimulateRequest` |
| Produces | `application/json` — `SimulateResponse` |
| Success | HTTP **200** (not 201 — no resource created) |
| Errors | 422 (`beloeb <= 0`), 403, 404 |
| Side effects | None — no DB write, no immudb append |

---

## Deliverables checklist

| # | File | Action | FR |
|---|------|--------|----|
| D-1 | `opendebt-payment-service/src/main/java/dk/ufst/opendebt/payment/domain/PrioritetKategori.java` | CREATE | FR-1 |
| D-2 | `opendebt-payment-service/src/main/java/dk/ufst/opendebt/payment/domain/RenteKomponent.java` | CREATE | FR-3 |
| D-3 | `opendebt-payment-service/src/main/java/dk/ufst/opendebt/payment/domain/DaekningRecord.java` | CREATE | FR-6 |
| D-4 | `opendebt-payment-service/src/main/java/dk/ufst/opendebt/payment/service/DaekningsRaekkefoeigenService.java` | CREATE (interface) | FR-1–FR-6 |
| D-5 | `opendebt-payment-service/src/main/java/dk/ufst/opendebt/payment/service/impl/DaekningsRaekkefoeigenServiceImpl.java` | CREATE (implementation) | FR-1–FR-6 |
| D-6 | `opendebt-payment-service/src/main/java/dk/ufst/opendebt/payment/controller/DaekningsRaekkefoeigenController.java` | CREATE | FR-7 |
| D-7 | `opendebt-payment-service/src/main/resources/openapi/daekningsraekkefoelge.yaml` | CREATE | FR-7 (ADR-0004) |
| D-8 | `opendebt-debt-service/src/main/resources/db/changelog/changes/V_P057_001__add_legacy_modtagelsesdato.xml` | CREATE | FR-2 |
| D-9 | `opendebt-payment-service/src/main/resources/db/changelog/changes/V_P057_002__create_daekning_record.xml` | CREATE | FR-6 |
| D-10 | `opendebt-rules-engine/src/main/resources/rules/collection-priority.drl` | MODIFY — tvangsbøder → `BOEDER_TVANGSBOEEDER_TILBAGEBETALING` | FR-1, AC-2 |
| D-11 | `opendebt-caseworker-portal/src/main/java/dk/ufst/opendebt/caseworker/controller/DaekningsRaekkefoeigenViewController.java` | CREATE | FR-8 |
| D-12 | `opendebt-caseworker-portal/src/main/resources/templates/debtor/daekningsraekkefoelge.html` | CREATE | FR-8 |
| D-13 | `opendebt-caseworker-portal/src/main/resources/templates/debtor/case-overview.html` | MODIFY — add link to dækningsrækkefølge view | FR-8, AC-16d |
| D-14 | `opendebt-caseworker-portal/src/main/resources/messages_da.properties` | MODIFY — add 11 new keys | FR-8, AC-17 |
| D-15 | `opendebt-caseworker-portal/src/main/resources/messages_en_GB.properties` | MODIFY — add 11 new keys | FR-8, AC-17 |

**Blocked dependency (not in P057 scope — must be tracked separately):**

| # | File | Action | Owner |
|---|------|--------|-------|
| DEP-1 | `opendebt-debt-service` — `GET /internal/debtors/{debtorId}/fordringer/active` | CREATE — new internal endpoint | `opendebt-debt-service` team |

---

## Traceability matrix

| FR / NFR | Requirement | Deliverables | AC |
|----------|-------------|----------|----|
| FR-1 | Priority categories — GIL § 4, stk. 1 | D-1, D-5, D-10 | AC-1, AC-2 |
| FR-2 | Within-category FIFO — GIL § 4, stk. 2 | D-5, D-8 | AC-3, AC-4 |
| FR-3 | Interest ordering — Gæld.bekendtg. § 4, stk. 3 | D-2, D-5 | AC-5, AC-6 |
| FR-4 | Inddrivelsesindsats rule — GIL § 4, stk. 3 | D-5 | AC-7, AC-8 |
| FR-5 | Opskrivningsfordring positioning — Gæld.bekendtg. § 7 | D-5 | AC-9, AC-10, AC-11 |
| FR-6 | Timing + DaekningRecord — GIL § 4, stk. 4 | D-3, D-5, D-9 | AC-12, AC-13 |
| FR-7 | Payment application API | D-6, D-7 | AC-14, AC-15 |
| FR-8 | Sagsbehandler portal view | D-11, D-12, D-13, D-14, D-15 | AC-16, AC-17 |
| NFR-1 | Determinism | D-5 (injected timestamp, pure ordering function) | AC-13 (completeness) |
| NFR-2 | Audit completeness | D-3, D-5 (immudb append with 8 required fields) | AC-13 |
| NFR-3 | GDPR isolation — no CPR in `opendebt_payment` | D-3 (UUID-only references) | — |
| NFR-4 | API-first | D-7 (OpenAPI spec before implementation) | — |
| NFR-5 | No cross-service DB (ADR-0007) | D-5 (REST-only fordring fetch), D-9 (no FK across DBs) | — |
| NFR-6 | Liquibase migrations (ADR-0011) | D-8, D-9 | AC-4 |
| NFR-7 | Statutory enums (ADR-0031) | D-1, D-2 | AC-1, AC-2, AC-5, AC-6 |

---

## Out of scope

The following items are explicitly excluded from this specification:

| Item | Reference | Tracked in |
|------|-----------|------------|
| DMI paralleldrift dækning logic (GIL § 49) | Different statutory rules; DMI mechanics not replicated in PSRM | Excluded by petition |
| Catala encoding of GIL § 4 | P069 companion petition (Tier A spike) | P069 |
| Pro-rata distribution across joint debtors | Depends on P057 ordering model | P062 |
| Forældelsesfrist interruption rules | Depends on P057 for ordering | P059 |
| Automatic modregning triggering | Modelled in P007; FR-4 governs ordering within an existing indsats payment only | P007 |
| Rentegodtgørelse (GIL § 18 l) | Out of scope for this petition | TB-039 |
| Full retroactive timeline replay | Out of scope for this petition | TB-038 |
| Betalingsflow changes (OCR matching, NemKonto) | This petition adds ordering within the existing flow, not a new flow | NFR-3 |
| Citizen portal or creditor portal payment views | FR-8 targets sagsbehandler portal only | Excluded by petition |

---

## Validation checklist

- [x] Every FR (FR-1 through FR-8) has at least one specification section
- [x] Every specification traces to petition FR and outcome-contract AC
- [x] All interfaces are testable and unambiguous (exact field names, types, comparison expressions, HTTP codes)
- [x] Non-functional requirements included only where petition/SA explicitly specifies them (NFR-1 through NFR-7)
- [x] Zero items beyond petition, outcome-contract, feature file, and solution architecture
- [x] Every specification enables implementation or testing
- [x] No vague language ("should", "might", "could") — all requirements use "must" or exact code
- [x] No invented features or constraints
- [x] `simulate` returns HTTP 200 specified (not 201); no DB write or immudb append specified
- [x] `DaekningRecord` entity invariants specified (beloeb >= 0, betalingstidspunkt <= applicationTimestamp)
- [x] Determinism preconditions specified (injected timestamp, no random tiebreakers, enum ordinals for sorting)
- [x] GDPR constraint specified (UUID-only references in payment-service; no CPR)
- [x] Blocked dependency (debt-service internal endpoint) explicitly called out
- [x] All 11 i18n keys specified for both DA and EN bundles
- [x] Tvangsbøder DRL correction specified with exact before/after rule text (AC-2)
- [x] `behave --dry-run` on feature file is not a spec item — it is a DoD item owned by the feature file itself
