# Petition 058 — Implementation Specifications
## Modregning i udbetalinger fra det offentlige + Korrektionspulje (G.A.2.3.3–2.3.4)

**Specification ID:** SPEC-058  
**Petition:** petition058-modregning-korrektionspulje.md  
**Outcome contract:** petition058-modregning-korrektionspulje-outcome-contract.md  
**Feature file:** petition058-modregning-korrektionspulje.feature  
**Service:** `opendebt-debt-service`  
**Root package:** `dk.ufst.opendebt.debtservice`  
**G.A. snapshot:** v3.16 (2026-03-28)  
**Depends on:** P057 (`DaekningsRaekkefoeigenService`), TB-040 (`GET /internal/debtors/{debtorId}/fordringer/active`)  
**Depended on by:** P059 (forældelse interruption), P062 (pro-rata distribution)

---

## 1. Scope and FR Mapping

This specification covers exactly the five functional requirements stated in P058. Nothing beyond them is specified.

| FR | Title | AC coverage | Deliverables |
|----|-------|-------------|--------------|
| **FR-1** | Automatic payment interception workflow | AC-1, AC-2, AC-3, AC-4, AC-5, AC-14 | `ModregningService`, `ModregningsRaekkefoeigenEngine`, `PublicDisbursementEventConsumer`, `ModregningEvent`, `CollectionMeasureEntity` (extension) |
| **FR-2** | Modregningsrækkefølge waiver (GIL § 4, stk. 11) | AC-6, AC-7 | `ModregningController` (waiver endpoint), `ModregningService` (re-run) |
| **FR-3** | Korrektionspulje management | AC-8, AC-9, AC-10, AC-11 | `KorrektionspuljeService`, `KorrektionspuljeSettlementJob`, `OffsettingReversalEventConsumer`, `KorrektionspuljeEntry` |
| **FR-4** | Rentegodtgørelse computation | AC-12, AC-13, AC-14 | `RenteGodtgoerelseService`, `RenteGodtgoerelseRateEntry` |
| **FR-5** | Klage (appeal) deadline tracking | AC-15, AC-16, AC-17 | `ModregningController` (read-model endpoint), `ModregningEvent` (klageFristDato) |

**Out of scope (not specified here):**
- DMI korrektionspulje settlement logic (GIL § 4, stk. 9) — `correctionPoolTarget = DMI` is a persisted flag only
- Tværgående lønindeholdelse korrektionspulje (G.A.2.3.4.3)
- Konkurslov/gældsbrevslov exceptions to modregningsrækkefølge
- Manual caseworker-initiated modregning
- Børne-og-ungeydelse restriction enforcement rules beyond flag persistence

---

## 2. Data Model

### 2.1 New Entities

#### 2.1.1 `ModregningEvent`

**Package:** `dk.ufst.opendebt.debtservice.entity`  
**Table:** `modregning_event`  
**Legal basis:** GIL §§ 7 stk. 1, 8b, 9a, 17 stk. 1

| Column | Java type | SQL type | Nullable | Constraints | Notes |
|--------|-----------|----------|----------|-------------|-------|
| `id` | `UUID` | `uuid` | NO | PK | `@GeneratedValue` |
| `nemkonto_reference_id` | `String` | `varchar(100)` | NO | UNIQUE | Idempotency key (NFR-4); source: `PublicDisbursementEvent.nemkontoReferenceId` |
| `debtor_person_id` | `UUID` | `uuid` | NO | indexed | ADR-0014 — no CPR |
| `receipt_date` | `LocalDate` | `date` | NO | | Date RIM received the disbursement from Nemkonto |
| `decision_date` | `LocalDate` | `date` | NO | | Date the modregning decision was made |
| `payment_type` | `String` | `varchar(50)` | NO | | Enum value: `OVERSKYDENDE_SKAT`, `BOERNE_OG_UNGEYDELSE`, etc. (ADR-0031) |
| `indkomst_aar` | `Integer` | `integer` | YES | | Set only when `payment_type = OVERSKYDENDE_SKAT`; kildeskattelov § 62/62A |
| `disbursement_amount` | `BigDecimal` | `numeric(15,2)` | NO | ≥ 0 | Total amount intercepted from Nemkonto |
| `tier1_amount` | `BigDecimal` | `numeric(15,2)` | NO | ≥ 0, DEFAULT 0 | Amount allocated to tier-1 fordringer |
| `tier2_amount` | `BigDecimal` | `numeric(15,2)` | NO | ≥ 0, DEFAULT 0 | Amount allocated to tier-2 fordringer |
| `tier3_amount` | `BigDecimal` | `numeric(15,2)` | NO | ≥ 0, DEFAULT 0 | Amount allocated to tier-3 fordringer |
| `residual_payout_amount` | `BigDecimal` | `numeric(15,2)` | NO | ≥ 0, DEFAULT 0 | Surplus returned to debtor via Nemkonto |
| `tier2_waiver_applied` | `boolean` | `boolean` | NO | DEFAULT false | GIL § 4, stk. 11 waiver flag |
| `notice_delivered` | `boolean` | `boolean` | NO | | Digital Post delivery outcome (GIL § 9a) |
| `notice_delivery_date` | `LocalDate` | `date` | YES | | Set when `notice_delivered = true` |
| `klage_frist_dato` | `LocalDate` | `date` | NO | | Computed at decision time (FR-5) |
| `rente_godtgoerelse_start_date` | `LocalDate` | `date` | YES | | Null when 5-banking-day exception applies (FR-4) |
| `rente_godtgoerelse_non_taxable` | `boolean` | `boolean` | NO | DEFAULT true | Always true (GIL § 8b, stk. 2, 3. pkt.) |

**Indexes:**
- `idx_me_debtor` on `debtor_person_id`
- `idx_me_decision_date` on `decision_date`
- UNIQUE on `nemkonto_reference_id`

**Invariants:**
- `tier1_amount + tier2_amount + tier3_amount + residual_payout_amount = disbursement_amount`
- `rente_godtgoerelse_non_taxable` MUST be `true` on every row (enforced by database DEFAULT and NOT NULL)
- `klage_frist_dato` MUST NOT be null on any persisted row

---

#### 2.1.2 `KorrektionspuljeEntry`

**Package:** `dk.ufst.opendebt.debtservice.entity`  
**Table:** `korrektionspulje_entry`  
**Legal basis:** GIL § 4, stk. 5–10

| Column | Java type | SQL type | Nullable | Constraints | Notes |
|--------|-----------|----------|----------|-------------|-------|
| `id` | `UUID` | `uuid` | NO | PK | `@GeneratedValue` |
| `debtor_person_id` | `UUID` | `uuid` | NO | indexed | ADR-0014 — no CPR |
| `origin_event_id` | `UUID` | `uuid` | NO | FK → `modregning_event.id` | The reversed ModregningEvent |
| `surplus_amount` | `BigDecimal` | `numeric(15,2)` | NO | > 0 | Amount placed in pool after gendækning |
| `correction_pool_target` | `String` | `varchar(10)` | NO | `IN ('PSRM','DMI')` | Routing flag (ADR-0031) |
| `boerne_ydelse_restriction` | `boolean` | `boolean` | NO | DEFAULT false | GIL § 4, stk. 7, nr. 3 |
| `rente_godtgoerelse_start_date` | `LocalDate` | `date` | NO | | Day after origin event's decision date |
| `rente_godtgoerelse_accrued` | `BigDecimal` | `numeric(15,2)` | NO | ≥ 0, DEFAULT 0.00 | Accrued interest on pool balance |
| `annual_only_settlement` | `boolean` | `boolean` | NO | DEFAULT false | Set to true when `surplus_amount < 50.00` |
| `settled_at` | `Instant` | `timestamptz` | YES | | Null until settled |
| `created_at` | `Instant` | `timestamptz` | NO | DEFAULT now() | |

**Indexes:**
- `idx_kpe_debtor` on `debtor_person_id`
- `idx_kpe_settled` on `settled_at` WHERE `settled_at IS NULL` (partial — for job queries)

**Invariants:**
- `surplus_amount > 0` always (zero-surplus entries are never persisted)
- When `correction_pool_target = DMI`, `settled_at` remains null indefinitely (DMI settlement is out of scope)

---

#### 2.1.3 `RenteGodtgoerelseRateEntry`

**Package:** `dk.ufst.opendebt.debtservice.entity`  
**Table:** `rentegodt_rate_entry`  
**Legal basis:** GIL § 8b, stk. 1; rentelov § 5, stk. 1+2

| Column | Java type | SQL type | Nullable | Constraints | Notes |
|--------|-----------|----------|----------|-------------|-------|
| `id` | `UUID` | `uuid` | NO | PK | `@GeneratedValue` |
| `publication_date` | `LocalDate` | `date` | NO | UNIQUE | Date the rentelov § 5 rate was published |
| `effective_date` | `LocalDate` | `date` | NO | | Publication date + 5 banking days (computed, stored) |
| `reference_rate_percent` | `BigDecimal` | `numeric(6,4)` | NO | ≥ 0 | Rentelov § 5 published rate |
| `godtgoerelse_rate_percent` | `BigDecimal` | `numeric(6,4)` | NO | ≥ 0 | MAX(0, reference_rate_percent − 4.0) |
| `created_at` | `Instant` | `timestamptz` | NO | DEFAULT now() | |

**Indexes:**
- `idx_rgre_effective_date` on `effective_date`

**Invariants:**
- `godtgoerelse_rate_percent = MAX(0, reference_rate_percent - 4.0)` — computed and stored at insert time
- `effective_date` is always ≥ `publication_date + 5 banking days` (validated at insertion, not by DB constraint)
- Rates are published semi-annually (1 January, 1 July); two rows per year at steady state

**Population:** Rows in this table are seeded by a `BusinessConfigService`-aware component that reads `rentelov.refRate` (FR-4.1), or by a developer-managed DB migration/seed. The seeding mechanism is a developer decision and is not prescribed by this specification. The `computeRate` method queries this table as the authoritative source for effective-dated rates.

---

#### 2.1.4 Extensions to Existing Entities

**`CollectionMeasureEntity`** — add three columns:

| New Column | Java type | SQL type | Nullable | Default | Notes |
|------------|-----------|----------|----------|---------|-------|
| `modregning_event_id` | `UUID` | `uuid` | YES | NULL | FK → `modregning_event.id`; set when `measure_type = SET_OFF` |
| `waiver_applied` | `boolean` | `boolean` | NO | false | GIL § 4, stk. 11; set `true` on (a) original tier-2 measures whose ledger entries are reversed by the waiver re-run, and (b) new tier-3 measures created by the waiver re-run |
| `caseworker_id` | `UUID` | `uuid` | YES | NULL | Set to the caseworker's UUID when a tier-2 waiver is applied (FR-2.2); null for all non-waiver measures |

**Constraint:** When `measure_type = SET_OFF`, `modregning_event_id` MUST NOT be null.  
Enforced by: application-level guard in `ModregningService` before persist.

---

### 2.2 Flyway Migrations

**Naming convention:** `V{N}__{snake_description}.sql`  
**Location:** `opendebt-debt-service/src/main/resources/db/migration/`  
**Existing migrations:** V1, V2, V3 (active); V4–V7 archived and NOT in active migration path.  
**New migrations start at V4** (archived V4–V7 are not in `db/migration/` and will not conflict).

| Migration | File | Content |
|-----------|------|---------|
| **V4** | `V4__modregning_event.sql` | CREATE TABLE `modregning_event`; indexes; UNIQUE on `nemkonto_reference_id` |
| **V5** | `V5__korrektionspulje_entry.sql` | CREATE TABLE `korrektionspulje_entry`; partial index on unsettled entries; FK to `modregning_event` |
| **V6** | `V6__rentegodt_rate_entry.sql` | CREATE TABLE `rentegodt_rate_entry`; UNIQUE on `publication_date` |
| **V7** | `V7__collection_measure_modregning_cols.sql` | ALTER TABLE `collection_measure` ADD COLUMN `modregning_event_id uuid`, ADD COLUMN `waiver_applied boolean NOT NULL DEFAULT false`, ADD COLUMN `caseworker_id uuid`; FK constraint on `modregning_event_id`; partial NOT NULL check constraint for SET_OFF rows |

Each migration file contains only DDL. No DML seeding. Each file is idempotent via `IF NOT EXISTS` guards.

---

## 3. Service Design

### 3.1 `ModregningService`

**Package:** `dk.ufst.opendebt.debtservice.service`  
**Implements:** `OffsettingService` (existing interface in `dk.ufst.opendebt.debtservice.offsetting`)  
**Source requirement:** FR-1, FR-2, FR-5

```
ModregningService implements OffsettingService {

  // Replaces OffsettingService stub (P007).
  // Implements OffsettingService.initiateOffsetting(...) as the full three-tier workflow.
  OffsettingResult initiateOffsetting(UUID debtorPersonId, BigDecimal availableAmount, String paymentType)
    // → delegates to initiateModregning(debtorPersonId, availableAmount, paymentType, null, false)

  ModregningResult initiateModregning(
      UUID debtorPersonId,
      BigDecimal availableAmount,
      String paymentType,
      PublicDisbursementEvent sourceEvent,   // null when called from korrektionspulje settlement
      boolean restrictedPayment             // true when børne-og-ungeydelse restriction applies (FR-3)
  )

  // Called by ModregningController for FR-2 waiver.
  ModregningResult applyTier2Waiver(
      UUID debtorPersonId,
      UUID modregningEventId,
      String waiverReason,
      UUID caseworkerId
  )
}
```

**Method contracts:**

`initiateModregning`:
- **Input:** `debtorPersonId` (non-null UUID), `availableAmount` (> 0), `paymentType` (non-null, must be in configured eligible-payment-types), `sourceEvent` (nullable), `restrictedPayment` (boolean; `true` when called from korrektionspulje settlement with `boerneYdelseRestriction = true`, bypassing the "origin nature lost" rule for børne-og-ungeydelse payments; `false` in all other cases including direct disbursement events)
- **Idempotency:** if `sourceEvent.nemkontoReferenceId` already exists in `modregning_event.nemkonto_reference_id`, return the existing `ModregningResult` without executing any further logic
- **Execution:** delegates tier ordering to `ModregningsRaekkefoeigenEngine`; delegates rentegodtgørelse start date to `RenteGodtgoerelseService`; persists `ModregningEvent`; creates `CollectionMeasureEntity` per covered fordring; generates ledger entries; enqueues Digital Post outbox message
- **Transaction:** entire method runs in a single `@Transactional` boundary; Digital Post outbox write is part of the same transaction (outbox pattern)
- **Output:** `ModregningResult` (see §5 for DTO definition)
- **Error:** throws `DuplicateNemkontoReferenceException` (HTTP 409) if idempotency guard triggered mid-transaction (race condition); throws `EligiblePaymentTypeException` (HTTP 422) if payment type is not in eligible set

`applyTier2Waiver`:
- **Input:** `debtorPersonId`, `modregningEventId`, `waiverReason` (non-null, max 500 chars), `caseworkerId` (non-null UUID)
- **Precondition:** `ModregningEvent` with given id exists and belongs to `debtorPersonId`; `tier2WaiverApplied = false`
- **Execution — state transitions (AC-6):**
  1. Sets `tier2WaiverApplied = true` on the `ModregningEvent`.
  2. For each existing tier-2 `CollectionMeasureEntity` linked to this event: sets `waiver_applied = true` and `caseworker_id = caseworkerId`; reverses the corresponding ledger entries in the same transaction. These rows are NOT deleted — they remain as the historical record of the reversed tier-2 measures.
  3. Re-runs `ModregningsRaekkefoeigenEngine.allocate` with `skipTier2 = true`, using the event's original `disbursementAmount` minus the tier-1 amounts already allocated as the available residual.
  4. Creates new `CollectionMeasureEntity` rows for any tier-3 fordringer covered in the re-run; each new row has `modregning_event_id` set, `waiver_applied = true`, and `caseworker_id = caseworkerId`.
  5. Updates `ModregningEvent.tier2Amount = 0.00`; updates `tier3Amount` = sum of newly covered tier-3 allocations; updates `residualPayoutAmount` = any uncovered residual.
  6. Writes CLS audit entry with `gilParagraf = "GIL § 4, stk. 11"`, `caseworkerId`, and `waiverReason`.
- **Transaction:** `@Transactional`; all steps 1–6 execute within a single transaction boundary; CLS audit write is within the same boundary
- **Output:** updated `ModregningResult`
- **Error:** throws `ModregningEventNotFoundException` (HTTP 404) if event not found; throws `WaiverAlreadyAppliedException` (HTTP 409) if `tier2WaiverApplied` already true

---

### 3.2 `ModregningsRaekkefoeigenEngine`

**Package:** `dk.ufst.opendebt.debtservice.service`  
**Source requirement:** FR-1.1–FR-1.4, FR-2

```
ModregningsRaekkefoeigenEngine {

  TierAllocationResult allocate(
      UUID debtorPersonId,
      BigDecimal availableAmount,
      boolean skipTier2,            // true when tier-2 waiver applied
      UUID payingAuthorityOrgId     // for tier-1 fordring resolution
  )
}
```

**Method contracts:**

`allocate`:
- **Input:** all four parameters required; `availableAmount` ≥ 0
- **Output:** `TierAllocationResult` — lists of `FordringAllocation` per tier (fordringId, amountCovered, tier) and `residualPayoutAmount`
- **Tier-1 resolution:** queries `GET /internal/debtors/{debtorId}/fordringer/active?tier=1&payingAuthority={payingAuthorityOrgId}` (TB-040 API); applies disbursement to tier-1 fordringer first; stops tier-2/tier-3 processing if available amount reaches zero after tier-1
- **Tier-2 resolution:** if `skipTier2 = false` and residual > 0, queries `GET /internal/debtors/{debtorId}/fordringer/active?tier=2`; if residual ≥ sum of all tier-2 outstanding amounts, covers each in full without calling `DaekningsRaekkefoeigenService`; if residual < sum, delegates exactly once to `DaekningsRaekkefoeigenService` (P057 client call) for GIL § 4 partial allocation
- **Tier-3 resolution:** if residual > 0, queries `GET /internal/debtors/{debtorId}/fordringer/active?tier=3`; applies residual in ascending `registreringsdato` order; partial coverage on last fordring if residual exhausted
- **Residual payout:** any amount remaining after all tiers are fully covered
- **Constraint:** `DaekningsRaekkefoeigenService` MUST NOT be called when tier-1 fully consumes the available amount (AC-2); MUST be called at most once per `allocate` invocation (AC-3)

---

### 3.3 `KorrektionspuljeService`

**Package:** `dk.ufst.opendebt.debtservice.service`  
**Source requirement:** FR-3

```
KorrektionspuljeService {

  KorrektionspuljeResult processReversal(
      OffsettingReversalEvent reversalEvent
  )

  void settleEntry(KorrektionspuljeEntry entry, LocalDate settlementDate)
}
```

**Method contracts:**

`processReversal`:
- **Input:** `reversalEvent` (non-null); contains `originModregningEventId`, `surplusAmount`, `debtorPersonId`, `reversedFordringId`
- **Step 1 — residual same-fordring coverage:** applies `surplusAmount` to uncovered portion of `reversedFordringId` (including renter sub-positions in P057 order); any amount consumed here is persisted as a ledger adjustment; remaining = surplus − step1Consumed
- **Step 2 — gendækning:** if remaining > 0 and gendækning is not opted-out (see opt-out rules below), calls `DaekningsRaekkefoeigenService` to allocate remaining against other active tier-2 fordringer; gendækning opt-out applies when: `correctionPoolTarget = DMI`, OR original payment was from `inddrivelsesindsats` of type debt-under-collection with a debt-under-collection opt-out flag, OR fordring was partially covered retroactively; no Digital Post notice is sent for gendækning
- **Step 3 — pool entry creation:** if remaining > 0 after gendækning (or gendækning skipped), persists `KorrektionspuljeEntry` with `surplusAmount = remaining`, `correctionPoolTarget` derived from origin event, `boerneYdelseRestriction` derived from origin event's `paymentType = BOERNE_OG_UNGEYDELSE`, `renteGodtgoerelseStartDate = origin event decisionDate + 1 day`, `annualOnlySettlement = (remaining < 50.00)`
- **Transaction:** `@Transactional`
- **Output:** `KorrektionspuljeResult` (stepConsumed, gendaekketAmount, poolEntryId, poolAmount)
- **Error:** throws `OriginEventNotFoundException` (HTTP 404) if `originModregningEventId` not found; throws `InvalidReversalAmountException` (HTTP 422) if `surplusAmount ≤ 0`

`settleEntry`:
- **Input:** `entry` (non-null, `settled_at = null`, `correctionPoolTarget = PSRM`); `settlementDate` (non-null)
- **Accrual computation (FR-3.6):** Before computing the total, compute and persist `renteGodtgoerelseAccrued` on the entry:
  - Only when `entry.renteGodtgoerelseStartDate IS NOT NULL` (i.e., the 5-banking-day exception did not apply to the origin event)
  - Rate: `godtgoerelseRatePercent = RenteGodtgoerelseService.computeRate(settlementDate)`
  - Period: `days = ChronoUnit.DAYS.between(entry.renteGodtgoerelseStartDate, settlementDate)`
  - Formula: `renteGodtgoerelseAccrued = entry.surplusAmount × (godtgoerelseRatePercent / 100) × days / 365`, rounded to 2 decimal places using `HALF_UP`
  - When `renteGodtgoerelseStartDate IS NULL`: `renteGodtgoerelseAccrued` remains `0.00`
  - The accrual update is persisted within the same `@Transactional` boundary as settlement
  - **Schedule alignment:** this computation runs at settlement time, which is driven by the monthly (`runMonthlySettlement`) and annual (`runAnnualSettlement`) job schedules (FR-3.4)
- **Execution:** computes `total = entry.surplusAmount + entry.renteGodtgoerelseAccrued`; invokes `ModregningService.initiateModregning(entry.debtorPersonId, total, paymentType, null, entry.boerneYdelseRestriction)` — `restrictedPayment` is passed as `entry.boerneYdelseRestriction`, bypassing the "origin nature lost" rule when `true`; transporter notified before 2021-10-01 is preserved via a flag on the P057 call; sets `settledAt = Instant.now()` on the entry
- **Constraint:** MUST NOT be called for entries with `correctionPoolTarget = DMI`

---

### 3.4 `RenteGodtgoerelseService`

**Package:** `dk.ufst.opendebt.debtservice.service`  
**Source requirement:** FR-4

```
RenteGodtgoerelseService {

  RenteGodtgoerelseDecision computeDecision(
      LocalDate receiptDate,
      LocalDate decisionDate,
      String paymentType,
      Integer indkomstAar       // nullable; used only when paymentType = OVERSKYDENDE_SKAT
  )

  BigDecimal computeRate(LocalDate referenceDate)
}
```

**Method contracts:**

`computeDecision`:
- **Input:** `receiptDate` and `decisionDate` non-null; `paymentType` non-null; `indkomstAar` nullable
- **5-banking-day exception (AC-12):** if `DanishBankingCalendar.bankingDaysBetween(receiptDate, decisionDate) ≤ 5`, return `RenteGodtgoerelseDecision { startDate = null, exceptionApplied = FIVE_BANKING_DAY }` — no rentegodtgørelse accrues
- **Kildeskattelov § 62/62A exception (AC-13):** if `paymentType = OVERSKYDENDE_SKAT`, candidate start date = `LocalDate.of(indkomstAar + 1, 9, 1)`; standard start date = `receiptDate.plusMonths(1).withDayOfMonth(1)`; `startDate = max(candidate, standardStartDate)` where the maximum is determined by whichever date is later; return `RenteGodtgoerelseDecision { startDate = max(candidate, standard), exceptionApplied = KILDESKATTELOV }` when candidate is later than standard
- **Standard case:** if neither exception applies, `startDate = receiptDate.plusMonths(1).withDayOfMonth(1)`; return `RenteGodtgoerelseDecision { startDate = startDate, exceptionApplied = NONE }`
- **Output:** `RenteGodtgoerelseDecision` record: `{ LocalDate startDate, ExceptionType exceptionApplied }`

`computeRate`:
- **Input:** `referenceDate` non-null
- **Execution:** queries `rentegodt_rate_entry` for the latest row where `effective_date ≤ referenceDate`; returns `godtgoerelse_rate_percent` of that row; the `rentegodt_rate_entry` table stores historical rate periods with effective dates to support the rate-change 5-banking-day delay requirement (FR-4.1)
- **Error:** throws `NoRenteGodtgoerelseRateException` (HTTP 500) if no rate entry covers `referenceDate` (configuration gap)

---

### 3.5 `KorrektionspuljeSettlementJob`

**Package:** `dk.ufst.opendebt.debtservice.batch`  
**Source requirement:** FR-3.4

```
KorrektionspuljeSettlementJob {

  void runMonthlySettlement()    // scheduled
  void runAnnualSettlement()     // scheduled
}
```

**Method contracts:**

`runMonthlySettlement`:
- **Execution:** queries `korrektionspulje_entry` WHERE `settled_at IS NULL AND correction_pool_target = 'PSRM' AND annual_only_settlement = false`; for each entry: calls `KorrektionspuljeService.settleEntry(entry, today)`
- **Threshold enforcement (AC-9):** entries with `surplus_amount < 50.00` have `annual_only_settlement = true` at creation time and are therefore excluded from this query
- **Scheduling:** configurable via `opendebt.batch.korrektionspulje-settlement.monthly.cron` in `application.yml`; default `0 0 3 1 * ?` (3 AM on the 1st of each month)
- **Error handling:** if settlement of one entry fails, log error with entry id, continue processing remaining entries (no full-job rollback); each entry settlement is its own transaction

`runAnnualSettlement`:
- **Execution:** queries `korrektionspulje_entry` WHERE `settled_at IS NULL AND correction_pool_target = 'PSRM'` (includes annual-only entries)
- **Scheduling:** configurable via `opendebt.batch.korrektionspulje-settlement.annual.cron` in `application.yml`; default `0 0 4 2 1 ?` (4 AM on 2 January)

---

### 3.6 Event Consumers

#### `PublicDisbursementEventConsumer`

**Package:** `dk.ufst.opendebt.debtservice.consumer`  
**Source requirement:** FR-1

| Aspect | Specification |
|--------|---------------|
| Event type | `PublicDisbursementEvent` |
| Event fields consumed | `nemkontoReferenceId` (String), `debtorPersonId` (UUID), `disbursementAmount` (BigDecimal), `paymentType` (String), `indkomstAar` (Integer, nullable), `payingAuthorityOrgId` (UUID), `receiptDate` (LocalDate) |
| Processing | Calls `ModregningService.initiateModregning(...)` |
| Idempotency | `nemkontoReferenceId` uniqueness check in `ModregningService` before any state mutation |
| Dead-letter | Events that fail validation (missing required fields, invalid paymentType) are sent to DLQ; not retried |
| Retry | Transient failures (DB connectivity, downstream service unavailability) are retried per broker configuration; idempotency guard ensures safe replay |

#### `OffsettingReversalEventConsumer`

**Package:** `dk.ufst.opendebt.debtservice.consumer`  
**Source requirement:** FR-3

| Aspect | Specification |
|--------|---------------|
| Event type | `OffsettingReversalEvent` (emitted by P053) |
| Event fields consumed | `originModregningEventId` (UUID), `reversedFordringId` (UUID), `surplusAmount` (BigDecimal), `debtorPersonId` (UUID), `correctionPoolTarget` (String), `originalPaymentType` (String) |
| Processing | Calls `KorrektionspuljeService.processReversal(event)` |
| Idempotency | Guard: check that no `KorrektionspuljeEntry` with `origin_event_id = originModregningEventId` already exists; if found, return without creating duplicate |

---

## 4. Algorithms

### 4.1 Three-Tier Ordering Algorithm (FR-1)

```
INPUT:  debtorPersonId, availableAmount, skipTier2, payingAuthorityOrgId
OUTPUT: TierAllocationResult

remaining ← availableAmount

// TIER 1
tier1Fordringer ← GET /internal/debtors/{debtorId}/fordringer/active?tier=1&payingAuthority={payingAuthorityOrgId}
tier1Allocations ← []
// Tier-1 internal ordering is not specified by GIL § 7; implementation order is at developer discretion.
FOR each fordring f IN tier1Fordringer:
    covered ← MIN(remaining, f.tilbaestaaendeBeloeb)
    IF covered > 0:
        tier1Allocations.add({fordringId: f.id, amountCovered: covered, tier: 1})
        remaining ← remaining − covered
    IF remaining = 0: BREAK

// SHORT-CIRCUIT: if remaining = 0 after tier-1, DO NOT call DaekningsRaekkefoeigenService
IF remaining = 0:
    RETURN TierAllocationResult(tier1=tier1Allocations, tier2=[], tier3=[], residual=0)

// TIER 2
tier2Allocations ← []
IF NOT skipTier2:
    tier2Fordringer ← GET /internal/debtors/{debtorId}/fordringer/active?tier=2
    tier2Total ← SUM(f.tilbaestaaendeBeloeb FOR f IN tier2Fordringer)
    IF remaining >= tier2Total:
        // Full coverage — no P057 delegation needed
        FOR each fordring f IN tier2Fordringer:
            tier2Allocations.add({fordringId: f.id, amountCovered: f.tilbaestaaendeBeloeb, tier: 2})
        remaining ← remaining − tier2Total
    ELSE:
        // Partial coverage — delegate to DaekningsRaekkefoeigenService EXACTLY ONCE
        p057Result ← DaekningsRaekkefoeigenService.allocate(debtorPersonId, remaining)
        tier2Allocations ← p057Result.allocations   // fordringId + amountCovered per allocation
        remaining ← 0   // p057 allocates all of remaining

IF remaining = 0:
    RETURN TierAllocationResult(tier1, tier2=tier2Allocations, tier3=[], residual=0)

// TIER 3
tier3Fordringer ← GET /internal/debtors/{debtorId}/fordringer/active?tier=3
                  sorted by registreringsdato ASC
tier3Allocations ← []
FOR each fordring f IN tier3Fordringer:
    covered ← MIN(remaining, f.tilbaestaaendeBeloeb)
    IF covered > 0:
        tier3Allocations.add({fordringId: f.id, amountCovered: covered, tier: 3})
        remaining ← remaining − covered
    IF remaining = 0: BREAK

residualPayout ← remaining   // may be > 0 if all tiers exhausted

RETURN TierAllocationResult(tier1=tier1Allocations, tier2=tier2Allocations,
                             tier3=tier3Allocations, residual=residualPayout)
```

**Constraints on the algorithm:**
- All `covered` values use `BigDecimal` arithmetic with `HALF_UP` rounding at 2 decimal places
- `DaekningsRaekkefoeigenService` is invoked at most once per execution (AC-3)
- The `remaining` counter never goes below zero

---

### 4.2 Partial Allocation (Tier-2 Delegation)

Tier-2 partial coverage is fully delegated to `DaekningsRaekkefoeigenService` (P057). The engine does NOT re-implement GIL § 4 allocation logic. The engine only invokes P057's service with the residual amount; it receives back a list of `{fordringId, amountCovered}` pairs and treats them as the tier-2 allocations.

---

### 4.3 Korrektionspulje Settlement Algorithm (FR-3)

```
INPUT:  reversalEvent {originModregningEventId, reversedFordringId, surplusAmount,
                       debtorPersonId, correctionPoolTarget, originalPaymentType}
OUTPUT: KorrektionspuljeResult

remaining ← surplusAmount

// STEP 1: Residual same-fordring coverage (Gæld.bekendtg. § 7, stk. 4)
sameFordring ← getFordring(reversedFordringId)
IF sameFordring.uncoveredPortion > 0:
    step1Covered ← MIN(remaining, sameFordring.uncoveredPortion)
    applyLedgerAdjustment(sameFordring, step1Covered)   // sub-position order per P057
    remaining ← remaining − step1Covered

// STEP 2: Gendækning (GIL § 4, stk. 5–6)
gendaekketAmount ← 0
optOut ← isGendaekningOptOut(correctionPoolTarget, reversalEvent)
IF remaining > 0 AND NOT optOut:
    p057Result ← DaekningsRaekkefoeigenService.allocate(debtorPersonId, remaining)
    gendaekketAmount ← SUM(p057Result.allocations.amountCovered)
    remaining ← remaining − gendaekketAmount
    // No Digital Post notice for gendækning

// STEP 3: KorrektionspuljeEntry creation
IF remaining > 0:
    entry ← new KorrektionspuljeEntry {
        debtorPersonId:            reversalEvent.debtorPersonId,
        originEventId:             originModregningEventId,
        surplusAmount:             remaining,
        correctionPoolTarget:      correctionPoolTarget,
        boerneYdelseRestriction:   (originalPaymentType = 'BOERNE_OG_UNGEYDELSE'),
        renteGodtgoerelseStartDate: originEvent.decisionDate + 1 day,
        renteGodtgoerelseAccrued:  0.00,
        annualOnlySettlement:      (remaining < 50.00)
    }
    persist(entry)

RETURN KorrektionspuljeResult(step1Consumed, gendaekketAmount, entry.id, remaining)
```

**Opt-out rule (`isGendaekningOptOut`):**
Returns `true` if ANY of the following:
- `correctionPoolTarget = 'DMI'`
- Origin event's `inddrivelsesindsats` is of a type designated as debt-under-collection opt-out (configuration key `opendebt.korrektionspulje.gendaekning-opt-out-indsats-types`)
- The original fordring was partially covered retroactively (flag on the origin `CollectionMeasureEntity`)

---

### 4.4 Rentegodtgørelse Decision Algorithm (FR-4)

```
INPUT:  receiptDate, decisionDate, paymentType, indkomstAar

// Check 5-banking-day exception FIRST
bankingDays ← DanishBankingCalendar.bankingDaysBetween(receiptDate, decisionDate)
IF bankingDays <= 5:
    RETURN { startDate: null, exceptionApplied: FIVE_BANKING_DAY }

// Check kildeskattelov § 62/62A exception
IF paymentType = 'OVERSKYDENDE_SKAT':
    kildeskatDate ← LocalDate.of(indkomstAar + 1, 9, 1)
    standardDate  ← receiptDate.plusMonths(1).withDayOfMonth(1)
    startDate ← if kildeskatDate > standardDate then kildeskatDate else standardDate
    exceptionApplied ← if kildeskatDate > standardDate then KILDESKATTELOV else NONE
    RETURN { startDate: startDate, exceptionApplied: exceptionApplied }

// Standard case
startDate ← receiptDate.plusMonths(1).withDayOfMonth(1)
RETURN { startDate: startDate, exceptionApplied: NONE }
```

**Rate lookup:**

```
computeRate(referenceDate):
    entry ← SELECT * FROM rentegodt_rate_entry
             WHERE effective_date <= referenceDate
             ORDER BY effective_date DESC
             LIMIT 1
    IF no entry found: THROW NoRenteGodtgoerelseRateException
    RETURN entry.godtgoerelse_rate_percent
```

---

### 4.5 Klage Deadline Algorithm (FR-5)

```
computeKlageFristDato(noticeDelivered, noticeDeliveryDate, decisionDate):
    IF noticeDelivered = true:
        RETURN noticeDeliveryDate.plusMonths(3)   // 3 calendar months from delivery date
    ELSE:
        RETURN decisionDate.plusYears(1)           // 1 year from decision date (GIL § 17)
```

This computation MUST be executed and `klageFristDato` MUST be stored on the `ModregningEvent` within the same transaction as the event creation. It MUST be updated after Digital Post delivery outcome is known (within the post-commit outbox processing, the event is updated with `noticeDelivered` and `noticeDeliveryDate`, then `klageFristDato` is recomputed and persisted in a follow-up transaction).

---

## 5. REST API

**Base path:** `/api/v1`  
**Authentication:** OAuth2 Bearer token (JWT)  
**Content-Type:** `application/json`

### 5.1 Endpoint: Apply Tier-2 Waiver (FR-2)

```
POST /api/v1/debtors/{debtorId}/modregning-events/{eventId}/tier2-waiver
```

**Required OAuth2 scope:** `modregning:waiver`  
**Required role check:** `@PreAuthorize("hasAuthority('SCOPE_modregning:waiver')")`

**Path parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `debtorId` | UUID | Debtor's `person_id` (ADR-0014) |
| `eventId` | UUID | `ModregningEvent.id` |

**Request body:**

```json
{
  "waiverReason": "string (1–500 chars, required)",
  "caseworkerId": "uuid (required)"
}
```

**Success response: HTTP 200**

```json
{
  "eventId": "uuid",
  "debtorPersonId": "uuid",
  "decisionDate": "2025-03-15",
  "tier1Amount": 0.00,
  "tier2Amount": 0.00,
  "tier3Amount": 3000.00,
  "residualPayoutAmount": 0.00,
  "tier2WaiverApplied": true,
  "klageFristDato": "2025-06-15",
  "noticeDelivered": true,
  "renteGodtgoerelseNonTaxable": true
}
```

**Error responses:**

| HTTP Status | Condition |
|-------------|-----------|
| 400 | `waiverReason` missing or blank; `caseworkerId` missing |
| 403 | Caller lacks `modregning:waiver` scope |
| 404 | `debtorId` or `eventId` not found |
| 409 | `tier2WaiverApplied` already `true` on this event |

---

### 5.2 Endpoint: List Modregning Events (FR-5)

```
GET /api/v1/debtors/{debtorId}/modregning-events
```

**Required OAuth2 scope:** `modregning:read` OR `modregning:waiver`  
**Required role check:** `@PreAuthorize("hasAuthority('SCOPE_modregning:read') or hasAuthority('SCOPE_modregning:waiver')")`

**Path parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `debtorId` | UUID | Debtor's `person_id` |

**Query parameters:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | integer | 0 | Zero-based page number |
| `size` | integer | 20 | Page size (max 100) |
| `sort` | string | `decisionDate,desc` | Sort field and direction |

**Success response: HTTP 200**

```json
{
  "content": [
    {
      "eventId": "uuid",
      "decisionDate": "2025-03-15",
      "totalOffsetAmount": 10000.00,
      "tier1Amount": 3000.00,
      "tier2Amount": 5000.00,
      "tier3Amount": 2000.00,
      "residualPayoutAmount": 0.00,
      "klageFristDato": "2025-06-15",
      "noticeDelivered": true,
      "tier2WaiverApplied": false,
      "renteGodtgoerelseNonTaxable": true,
      "renteGodtgoerelseStartDate": "2025-04-01"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

**Field definitions:**

| Field | Source | Mandatory in response |
|-------|--------|----------------------|
| `eventId` | `ModregningEvent.id` | YES |
| `decisionDate` | `ModregningEvent.decisionDate` | YES |
| `totalOffsetAmount` | `tier1Amount + tier2Amount + tier3Amount` | YES |
| `tier1Amount` | `ModregningEvent.tier1Amount` | YES |
| `tier2Amount` | `ModregningEvent.tier2Amount` | YES |
| `tier3Amount` | `ModregningEvent.tier3Amount` | YES |
| `residualPayoutAmount` | `ModregningEvent.residualPayoutAmount` | YES |
| `klageFristDato` | `ModregningEvent.klageFristDato` | YES |
| `noticeDelivered` | `ModregningEvent.noticeDelivered` | YES |
| `tier2WaiverApplied` | `ModregningEvent.tier2WaiverApplied` | YES |
| `renteGodtgoerelseNonTaxable` | `ModregningEvent.renteGodtgoerelseNonTaxable` | YES |
| `renteGodtgoerelseStartDate` | `ModregningEvent.renteGodtgoerelseStartDate` | YES (null if 5-banking-day exception) |

**Error responses:**

| HTTP Status | Condition |
|-------------|-----------|
| 403 | Caller lacks required scope |
| 404 | `debtorId` not found in Person Registry |

---

### 5.3 DTO Definitions

**`ModregningResult`** (returned by service, mapped to response body by controller):

```java
record ModregningResult(
    UUID eventId,
    UUID debtorPersonId,
    LocalDate decisionDate,
    BigDecimal disbursementAmount,
    BigDecimal tier1Amount,
    BigDecimal tier2Amount,
    BigDecimal tier3Amount,
    BigDecimal residualPayoutAmount,
    boolean tier2WaiverApplied,
    boolean noticeDelivered,
    LocalDate noticeDeliveryDate,       // nullable
    LocalDate klageFristDato,
    LocalDate renteGodtgoerelseStartDate, // nullable
    boolean renteGodtgoerelseNonTaxable,
    List<FordringCoverageDto> coverages
)
```

**`FordringCoverageDto`:**

```java
record FordringCoverageDto(
    UUID fordringId,
    BigDecimal amountCovered,
    int tier                // 1, 2, or 3
)
```

**`TierAllocationResult`** (internal, not serialised to API):

```java
record TierAllocationResult(
    List<FordringAllocation> tier1Allocations,
    List<FordringAllocation> tier2Allocations,
    List<FordringAllocation> tier3Allocations,
    BigDecimal residualPayoutAmount
)
```

**`RenteGodtgoerelseDecision`** (internal):

```java
record RenteGodtgoerelseDecision(
    LocalDate startDate,              // null when exceptionApplied = FIVE_BANKING_DAY
    ExceptionType exceptionApplied    // enum: NONE, FIVE_BANKING_DAY, KILDESKATTELOV
)
```

---

## 6. Integrations

### 6.1 `DaekningsRaekkefoeigenService` (P057)

**Invocation pattern:** Internal HTTP call (no direct DB access — ADR-0007)  
**Client class:** `DaekningsRaekkefoeigenServiceClient` in `dk.ufst.opendebt.debtservice.client`

| Call site | Method | When called |
|-----------|--------|-------------|
| `ModregningsRaekkefoeigenEngine.allocate` | `allocate(debtorPersonId, remaining)` | Tier-2 partial coverage only (AC-3); never called when tier-1 fully consumes disbursement |
| `KorrektionspuljeService.processReversal` | `allocate(debtorPersonId, remaining)` | Step 2 gendækning only |

**Contract for `DaekningsRaekkefoeigenServiceClient.allocate`:**
- Input: `UUID debtorPersonId`, `BigDecimal amount`
- Output: `List<FordringAllocation>` — list of `{fordringId, amountCovered}`
- The sum of `amountCovered` across all allocations MUST equal the input `amount`
- If no eligible tier-2 fordringer exist, returns an empty list (full amount unallocated)

### 6.2 immudb (ADR-0029)

**Pattern:** Dual-write — primary write to PostgreSQL; secondary write to immudb for tamper-evidence.  
**What is written to immudb:** Each `ModregningEvent` persisted, each `CollectionMeasureEntity` created with `measureType = SET_OFF`, and each CLS audit log entry.  
**Write timing:** immudb write happens within the same logical transaction window as the PostgreSQL commit; if the immudb write fails, the PostgreSQL transaction is NOT rolled back (immudb is an audit trail, not a source of truth); the failure is logged and surfaced as an alert metric.  
**Key format:** `modregning:{eventId}` for `ModregningEvent` records; `measure:{measureId}` for `CollectionMeasureEntity` records.

### 6.3 Digital Post Outbox (Post-Commit)

**Pattern:** Transactional outbox (same as P052).  
**Table:** `notification_outbox` (existing table; check P052 migration for schema).  
**Write timing:** The outbox message is written WITHIN the `@Transactional` boundary of `ModregningService.initiateModregning`. The actual Digital Post dispatch happens AFTER commit, by the `NotificationService` outbox poller.  
**Violation:** Dispatching Digital Post before transaction commit is a failure condition (outcome contract, FR-1 failure conditions).

**Outbox message payload:**
```json
{
  "notificationType": "MODREGNING_NOTICE",
  "debtorPersonId": "uuid",
  "modregningEventId": "uuid",
  "totalOffsetAmount": 10000.00,
  "coverages": [ { "fordringshaver": "string", "fordringId": "uuid", "amount": 5000.00 } ],
  "renteGodtgoerelseStartDate": "2025-04-01",
  "renteGodtgoerelseNonTaxable": true,
  "klageFristDato": "2025-06-15"
}
```

**Post-delivery callback:** When `NotificationService` completes dispatch (success or failure), it updates `ModregningEvent.noticeDelivered` and `ModregningEvent.noticeDeliveryDate` in a follow-up transaction, and recomputes `klageFristDato`.

**No Digital Post for gendækning** (FR-3.2): The `KorrektionspuljeService.processReversal` MUST NOT enqueue any outbox message for Step 2 gendækning allocations.

---

## 7. Error Handling and Idempotency

### 7.1 Idempotency

| Scenario | Guard | Response |
|----------|-------|----------|
| Same `nemkontoReferenceId` replayed | UNIQUE constraint on `modregning_event.nemkonto_reference_id` + pre-check in `ModregningService` | Return HTTP 200 referencing existing event; no new state created (AC-5) |
| Same `OffsettingReversalEvent` replayed | Pre-check: `KorrektionspuljeEntry` with `origin_event_id` already exists | Return without creating duplicate entry |
| Same waiver request replayed | Pre-check: `tier2WaiverApplied = true` | Return HTTP 409 with message "Waiver already applied" |

**Implementation note on `nemkontoReferenceId` idempotency:**
1. Before any processing: `SELECT id FROM modregning_event WHERE nemkonto_reference_id = ?`
2. If found: return `ModregningResult` built from the existing `ModregningEvent`; do not proceed
3. If not found: proceed with full workflow
4. On `DataIntegrityViolationException` (UNIQUE constraint violation from concurrent insert): catch, re-query and return existing event

### 7.2 Error Catalogue

| Exception Class | HTTP Status | When thrown |
|-----------------|-------------|-------------|
| `DuplicateNemkontoReferenceException` | 409 | Concurrent duplicate `nemkontoReferenceId` insert |
| `EligiblePaymentTypeException` | 422 | `paymentType` not in configured eligible-payment-types |
| `ModregningEventNotFoundException` | 404 | Event id not found for given debtor |
| `WaiverAlreadyAppliedException` | 409 | `tier2WaiverApplied` already true |
| `OriginEventNotFoundException` | 404 | `originModregningEventId` not found in `OffsettingReversalEventConsumer` |
| `InvalidReversalAmountException` | 422 | `surplusAmount ≤ 0` in reversal event |
| `NoRenteGodtgoerelseRateException` | 500 | No `RenteGodtgoerelseRateEntry` covers the reference date |
| `InsufficientFundsException` | 422 | Tier-2 delegation returns allocations summing to less than input amount (P057 contract violation) |

All exceptions are handled by the existing `GlobalExceptionHandler` (or its extension). Error response format follows the existing API error body convention.

---

## 8. Security: OAuth2 Scopes

| Scope | Operations permitted |
|-------|---------------------|
| `modregning:read` | `GET /api/v1/debtors/{debtorId}/modregning-events` |
| `modregning:waiver` | `POST /api/v1/debtors/{debtorId}/modregning-events/{eventId}/tier2-waiver`; also grants read access |

**Enforcement mechanism:** `@PreAuthorize("hasAuthority('SCOPE_{scope}')")` annotation on each controller method, consistent with existing `SecurityConfig` pattern.

**Authorization failure:** Callers without required scope receive HTTP 403 with no body details (AC-7). No audit log entry is created for rejected unauthorized requests.

**Waiver CLS audit:** The CLS audit entry for a waiver MUST include the `caseworkerId` from the request body.

---

## 9. Non-Functional Requirements

### NFR-1: Atomicity (Rollback on Failure)

- The full FR-1 workflow (tier ordering, `ModregningEvent` persist, `CollectionMeasureEntity` creation per covered fordring, double-entry ledger entries, outbox message write) executes within a single Spring `@Transactional(rollbackOn = Exception.class)` boundary
- If ANY step fails (including the outbox write), the entire transaction rolls back; no partial state is persisted
- Digital Post dispatch NEVER occurs within the transaction — it is triggered by the outbox poller after commit
- **Verification:** Atomicity is verified by the acceptance tests for AC-1 through AC-4.

### NFR-2: Auditability — CLS Per-Tier Log

Every modregning decision writes a CLS audit log entry for each fordring covered. The CLS write MUST occur within the same `@Transactional` boundary as the `ModregningEvent` persist.

**CLS log entry schema (per fordring allocation):**

| Field | Value | Source |
|-------|-------|--------|
| `eventType` | `MODREGNING_ALLOCATION` | constant |
| `gilParagraf` | `"GIL § 7, stk. 1, nr. {tier}"` | tier number 1, 2, or 3 |
| `modregningEventId` | `ModregningEvent.id` | |
| `debtorPersonId` | `UUID` | ADR-0014 — no CPR |
| `fordringId` | `UUID` | |
| `amountCovered` | `BigDecimal` | |
| `tier` | `int` (1, 2, or 3) | |
| `timestamp` | `Instant.now()` | |

**CLS log entry for FR-2 waiver:**

| Field | Value |
|-------|-------|
| `eventType` | `MODREGNING_TIER2_WAIVER` |
| `gilParagraf` | `"GIL § 4, stk. 11"` |
| `modregningEventId` | `UUID` |
| `caseworkerId` | `UUID` |
| `waiverReason` | `String` |
| `timestamp` | `Instant.now()` |

### NFR-3: GDPR — No PII in P058 Tables

All P058 domain tables (`modregning_event`, `korrektionspulje_entry`, `rentegodt_rate_entry`) reference debtors exclusively via `debtor_person_id` (UUID). CPR numbers, names, addresses, and any other direct personal identifiers MUST NOT appear in any P058 table column, index, log entry, or outbox message payload. The `debtorPersonId` UUID is resolved to debtor details exclusively via the Person Registry service (ADR-0014).

### NFR-4: Idempotency (specified in §7.1 above)

### NFR-5: Performance

- `ModregningsRaekkefoeigenEngine.allocate` covering up to 500 active fordringer MUST complete and commit within 2 seconds at p99 under normal load (steady state), measured from event ingestion to transaction commit
- Tier-1 and tier-3 fordring queries use indexed `debtor_person_id` columns
- Tier-2 partial coverage makes exactly one network call to `DaekningsRaekkefoeigenService` (AC-3)
- The `KorrektionspuljeSettlementJob` processes entries in page-size batches of 1 000 (consistent with `opendebt.batch.page-size` config key) to avoid OOM on large debtor populations

---

## 10. Testing Requirements (AC-1 through AC-18)

All tests reside in `opendebt-debt-service/src/test/` following existing test conventions. JaCoCo line coverage ≥ 80%; branch coverage ≥ 70% for all new classes.

### AC-1: Three-Tier Mixed Disbursement

**Test class:** `ModregningServiceIntegrationTest`  
**Test type:** Integration (Spring slice or full context, in-memory H2/Testcontainers PostgreSQL)  
**Scenario:** 10 000 DKK disbursement; debtor has 3 000 DKK tier-1, 5 000 DKK tier-2, 4 000 DKK tier-3  
**Assertions:**
- `ModregningEvent.tier1Amount = 3 000.00`
- `ModregningEvent.tier2Amount = 5 000.00`
- `ModregningEvent.tier3Amount = 2 000.00`
- `ModregningEvent.residualPayoutAmount = 0.00`
- Three `CollectionMeasureEntity` rows exist with `modregning_event_id` set
- `DaekningsRaekkefoeigenService` mock is NOT called (tier-2 total 5 000 DKK ≤ remaining 7 000 DKK after tier-1, so full tier-2 coverage applies without P057 delegation)

### AC-2: Tier-1 Full Coverage — No P057 Call

**Test class:** `ModregningsRaekkefoeigenEngineTest`  
**Test type:** Unit  
**Scenario:** Disbursement fully consumed by tier-1  
**Assertions:**
- `DaekningsRaekkefoeigenService` mock is NEVER invoked
- `tier2Amount = 0.00`, `tier3Amount = 0.00`
- Exactly as many `CollectionMeasureEntity` rows as tier-1 fordringen

### AC-3: Tier-2 Partial Coverage — P057 Delegated Once

**Test class:** `ModregningsRaekkefoeigenEngineTest`  
**Test type:** Unit with `DaekningsRaekkefoeigenService` mock  
**Scenario:** Residual 1 800 DKK; tier-2 fordringer total 5 500 DKK → partial  
**Assertions:**
- `DaekningsRaekkefoeigenService.allocate` called exactly once with `(debtorPersonId, 1800.00)`
- Returned allocations are persisted as tier-2 `CollectionMeasureEntity` rows
- `tier2Amount = 1 800.00`

### AC-4: SET_OFF Measure Per Covered Fordring

**Test class:** `ModregningServiceIntegrationTest`  
**Test type:** Integration  
**Scenario:** Any multi-fordring disbursement event  
**Assertions:**
- For each fordring with `amountCovered > 0`: exactly one `CollectionMeasureEntity` exists with `measureType = SET_OFF` AND `modregningEventId = ModregningEvent.id`
- No `CollectionMeasureEntity` with `measureType = SET_OFF` exists without a `modregningEventId`

### AC-5: Idempotency — Duplicate nemkontoReferenceId

**Test class:** `ModregningServiceIdempotencyTest`  
**Test type:** Integration  
**Scenario:** Same `PublicDisbursementEvent` processed twice  
**Assertions:**
- Exactly one `ModregningEvent` row in DB after two calls
- Second call returns the same `eventId` as the first
- No additional `CollectionMeasureEntity` created on the second call

### AC-6: Tier-2 Waiver Applied by Authorized Caseworker

**Test class:** `ModregningControllerSecurityTest`  
**Test type:** MockMvc slice with Spring Security  
**Scenario:** Caller JWT contains `modregning:waiver` scope  
**Assertions:**
- HTTP 200
- `ModregningEvent.tier2WaiverApplied = true` in DB
- `ModregningEvent.tier2Amount = 0.00` in DB
- CLS audit entry exists with `gilParagraf = "GIL § 4, stk. 11"`, `caseworkerId`, `waiverReason`
- All original tier-2 `CollectionMeasureEntity` rows linked to this event have `waiver_applied = true` and `caseworker_id` set; they are NOT deleted
- New `CollectionMeasureEntity` rows exist for any tier-3 fordringer covered in the re-run, each with `waiver_applied = true` and `caseworker_id` set
- `DaekningsRaekkefoeigenService` is NOT called in the re-run (tier-2 is skipped; tier-3 does not use P057)

### AC-7: Missing Scope Returns HTTP 403

**Test class:** `ModregningControllerSecurityTest`  
**Test type:** MockMvc slice  
**Scenario:** Caller JWT does NOT contain `modregning:waiver`  
**Assertions:**
- HTTP 403
- `ModregningEvent.tier2WaiverApplied` remains `false` in DB
- No CLS audit entry created

### AC-8: Gendækning After Fordring Write-Down

**Test class:** `KorrektionspuljeServiceTest`  
**Test type:** Integration  
**Scenario:** `OffsettingReversalEvent` with 1 500 DKK surplus; same fordring has 200 DKK uncovered renter; other fordring has 1 000 DKK outstanding  
**Assertions:**
- Step 1: 200 DKK applied to same fordring's renter portion
- Step 2: `DaekningsRaekkefoeigenService` called with 1 300 DKK; 1 000 DKK gendækket to other fordring
- Step 3: `KorrektionspuljeEntry.surplusAmount = 300.00`
- No Digital Post outbox message enqueued

### AC-9: Pool Entry Under 50 DKK Skipped in Monthly Run

**Test class:** `KorrektionspuljeSettlementJobTest`  
**Test type:** Unit with mocked repository  
**Scenario:** One entry with `surplusAmount = 45.00`, `annualOnlySettlement = true`  
**Assertions:**
- `runMonthlySettlement` does NOT call `KorrektionspuljeService.settleEntry` for this entry
- Entry `settled_at` remains null
- `runAnnualSettlement` DOES settle the entry

### AC-10: Monthly Settlement Re-Applies via FR-1

**Test class:** `KorrektionspuljeSettlementJobIntegrationTest`  
**Test type:** Integration  
**Scenario:** Entry `surplusAmount = 750.00`, `renteGodtgoerelseAccrued = 12.50`, `boerneYdelseRestriction = false`; debtor has 900 DKK tier-2 fordring  
**Assertions:**
- `ModregningService.initiateModregning` called with `availableAmount = 762.50`
- Tier-2 fordring receives 762.50 DKK dækning
- No transporter/udlæg restriction applied
- `KorrektionspuljeEntry.settledAt` is non-null after job run

### AC-11: Børneydelse Restriction Preserved After Settlement

**Test class:** `KorrektionspuljeSettlementJobTest`  
**Test type:** Unit  
**Scenario:** Entry `boerneYdelseRestriction = true`, `surplusAmount = 200.00`  
**Assertions:**
- `KorrektionspuljeService.settleEntry` is called with `restrictedPayment = true`
- `ModregningService.initiateModregning` receives a flag indicating børne-og-ungeydelse restriction applies
- The settled amount is NOT treated as an unrestricted Nemkonto payment

### AC-12: 5-Banking-Day Exception — Zero Rentegodtgørelse

**Test class:** `RenteGodtgoerelseServiceTest`  
**Test type:** Unit with `DanishBankingCalendar` mock  
**Scenario:** `receiptDate = 2025-03-10`, `decisionDate = 2025-03-13` (3 banking days)  
**Assertions:**
- `computeDecision` returns `{ startDate: null, exceptionApplied: FIVE_BANKING_DAY }`
- `ModregningEvent.renteGodtgoerelseStartDate` is null after full modregning workflow

### AC-13: Kildeskattelov § 62 Special Start Date

**Test class:** `RenteGodtgoerelseServiceTest`  
**Test type:** Unit  
**Scenario:** `paymentType = OVERSKYDENDE_SKAT`, `indkomstAar = 2024`, `receiptDate = 2025-04-01`  
**Assertions:**
- `computeDecision` returns `{ startDate: 2025-09-01, exceptionApplied: KILDESKATTELOV }`
- Standard start date `2025-05-01` (1st of month after receipt) is NOT used

### AC-14: `renteGodtgoerelseNonTaxable = true` on Every Event

**Test class:** `ModregningServiceIntegrationTest` (verified across all AC-1 through AC-13 scenarios)  
**Assertion added to all FR-1 integration tests:** `modregningEvent.renteGodtgoerelseNonTaxable = true`  
**Additional test:** `ModregningEventInvariantTest` — queries all persisted `ModregningEvent` rows and asserts none has `rente_godtgoerelse_non_taxable = false`

### AC-15: Klage Deadline Computation

**Test class:** `KlageFristDatoComputationTest`  
**Test type:** Unit  
**Scenarios:**
- `noticeDelivered = true`, `noticeDeliveryDate = 2025-03-15` → `klageFristDato = 2025-06-15`
- `noticeDelivered = false`, `decisionDate = 2025-03-15` → `klageFristDato = 2026-03-15`
**Assertions:** exact date match; no off-by-one errors

### AC-16: GET Modregning Events Returns All Required Fields

**Test class:** `ModregningControllerTest`  
**Test type:** MockMvc slice  
**Scenario:** Debtor with two `ModregningEvent` rows  
**Assertions:**
- HTTP 200
- Response body contains both events
- Each event object in `content` array contains ALL of: `eventId`, `decisionDate`, `totalOffsetAmount`, `tier1Amount`, `tier2Amount`, `tier3Amount`, `residualPayoutAmount`, `klageFristDato`, `noticeDelivered`, `tier2WaiverApplied`
- No field is absent or null when it should be non-null

### AC-17: Portal Amber/Red Highlighting

**Test class:** `ModregningViewControllerTest`  
**Test type:** Thymeleaf rendering test (Spring MVC test with template engine)  
**Scenarios:**
- Event with `klageFristDato` 10 days in the future → rendered HTML contains CSS class `klage-frist-amber`
- Event with `klageFristDato` in the past → rendered HTML contains CSS class `klage-frist-red`
- Event with `klageFristDato` 30 days in the future → rendered HTML contains neither class

### AC-18: i18n Bundle Coverage

**Verification method:** CI bundle-lint check (build-time, not Gherkin).  
**Required keys in `messages_da.properties` and `messages_en_GB.properties`:**

| Key | Purpose |
|-----|---------|
| `modregning.tier.label.1` | Tier-1 label |
| `modregning.tier.label.2` | Tier-2 label |
| `modregning.tier.label.3` | Tier-3 label |
| `modregning.klage.frist.amber` | Amber indicator label |
| `modregning.klage.frist.red` | Red/past indicator label |
| `korrektionspulje.status.pending` | Pool entry pending settlement |
| `korrektionspulje.status.settled` | Pool entry settled |
| `korrektionspulje.status.annual.only` | Annual-only threshold label |
| `modregning.notice.rente.non.taxable` | Digital Post notice text for non-taxable rentegodtgørelse |

**Test:** The CI bundle-lint check (`BundleKeyLintTest` or Maven enforcer) fails the build if any key present in `messages_da.properties` is absent from `messages_en_GB.properties` and vice versa.

---

## Validation Checklist

- [x] Every FR (FR-1 through FR-5) has at least one specification
- [x] Every specification traces to petition/requirements (references included inline)
- [x] All interfaces are testable and unambiguous (typed inputs/outputs; no "should" or "might")
- [x] Non-functional requirements NFR-1 through NFR-5 are included only because they are explicitly stated in P058
- [x] Zero items beyond petition058-modregning-korrektionspulje.md, the outcome contract, and the feature file
- [x] Every specification enables implementation or testing
- [x] No vague language
- [x] No invented features or constraints
- [x] DMI korrektionspulje settlement (GIL § 4, stk. 9) NOT specified — only the `correctionPoolTarget = DMI` persistence flag
- [x] Manual caseworker-initiated modregning NOT specified
- [x] Børne-og-ungeydelse restriction enforcement logic beyond flag persistence NOT specified
- [x] All 18 ACs have a corresponding test specification in §10
- [x] AC-18 correctly identified as CI bundle-lint, not Gherkin
- [x] `modregning:admin` scope and `registerRatePublication` endpoint NOT specified — rate table population is a developer decision (FR-4.1 prescribes `BusinessConfigService` as source)
- [x] `created_by` column NOT included on `rentegodt_rate_entry` — not petitioned
- [x] JWT `sub` cross-validation on waiver endpoint NOT specified — not petitioned
- [x] Tier-1 internal ordering NOT mandated — GIL § 7 does not specify it
- [x] NFR-1 atomicity verification technique NOT mandated — left to developer discretion
