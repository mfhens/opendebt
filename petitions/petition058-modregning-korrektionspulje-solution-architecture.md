# Solution Architecture — Petition 058: Modregning og Korrektionspulje

**Document ID:** SA-058  
**Petition:** P058 — Modregning i udbetalinger fra det offentlige + Korrektionspulje (G.A.2.3.3–2.3.4)  
**Status:** Approved for implementation  
**Service owner:** `opendebt-debt-service`  
**G.A. snapshot:** v3.16 (2026-03-28)  
**Depends on:** P057 (`DaekningsRaekkefoeigenService`), TB-040 (active fordringer internal API)  
**Depended on by:** P059 (forældelse — interruption by modregning), P062 (pro-rata distribution)  
**Legal basis:** GIL §§ 4 stk. 5–11, 7 stk. 1–2, 8b, 9a, 17 stk. 1; Nemkonto § 16 stk. 1; Gæld.bekendtg. § 7 stk. 4; Kildeskattelov §§ 62, 62A; Lov om børne- og ungeydelse § 11 stk. 2

---

## 1. Context and Scope

### 1.1 Purpose

Under GIL §§ 7–8b, when a public authority owes a debtor a payment (tax refund, social benefit, housing subsidy, etc.), RIM (Restanceinddrivelsesmyndighed / Gældsstyrelsen / OpenDebt) has a statutory obligation to intercept that payment via the Nemkonto system and apply it against the debtor's outstanding fordringer (*modregning i udbetalinger fra det offentlige*). Interception must follow a legally mandated three-tier priority sequence.

Petition 007 registered the `SET_OFF` collection-measure type and created the `OffsettingService` interface as a stub. P058 replaces that stub with a complete, legally grounded implementation and adds the korrektionspulje (correction pool) lifecycle (GIL § 4, stk. 5–10) that handles surplus amounts arising when previously offset debts are written down or cancelled.

### 1.2 In-Scope Functional Areas

| FR | Domain function |
|----|-----------------|
| FR-1 | Automatic three-tier payment interception workflow (GIL § 7, stk. 1, nr. 1–3) |
| FR-2 | Caseworker waiver of tier-2 priority (GIL § 4, stk. 11) |
| FR-3 | Korrektionspulje lifecycle: gendækning, pool entry creation, monthly/annual settlement |
| FR-4 | Rentegodtgørelse computation (GIL § 8b), including 5-banking-day and kildeskattelov exceptions |
| FR-5 | Klage (appeal) deadline tracking on every ModregningEvent (GIL § 17, stk. 1) |

### 1.3 Explicitly Out of Scope

| Item | Reason |
|------|--------|
| DMI korrektionspulje settlement (GIL § 4, stk. 9) | Modelled as `correctionPoolTarget = DMI` flag only; DMI pool mechanics are a future item |
| Tværgående lønindeholdelse korrektionspulje (G.A.2.3.4.3) | Separate petition |
| Konkurslov / gældsbrevslov exceptions to modregningsrækkefølge | Legal-team review required |
| Manual caseworker-initiated modregning | Future petition |
| Børne-og-ungeydelse restriction enforcement detail (§ 11, stk. 2) | Restriction flag is persisted; enforcement logic is a future petition |
| GIL § 49 DMI paralleldrift modregning rules | Out of scope |

### 1.4 Service Boundary

All P058 components are deployed within `opendebt-debt-service` (ADR-0027). The offsetting domain is co-located with the debt lifecycle domain because:
- The `SET_OFF` collection measure and debt balance write-down must be atomic within a single JPA `@Transactional` boundary.
- No external system integration justifies a separate service boundary (unlike `opendebt-wage-garnishment-service` which integrates with EINDKOMST/A-melding).
- All offsetting operations read from and write to the debt service's own PostgreSQL schema.

---

## 2. Key Architectural Decisions

### 2.1 Binding ADR Constraints

| ADR | Decision | Impact on P058 |
|-----|----------|----------------|
| ADR-0007 | No cross-service direct database access | All P057 delegation uses `DaekningsRaekkefoeigenServiceClient` (HTTP); debt service calls payment-service REST API. No shared schema. |
| ADR-0014 | GDPR data isolation — UUID-only person references | All P058 tables (`modregning_event`, `korrektionspulje_entry`, `rentegodt_rate_entry`) use `debtor_person_id UUID`. CPR numbers are never stored outside Person Registry. |
| ADR-0018 | Double-entry bookkeeping for all financial events | Every tier allocation produces a debit to debtor fordring account and a credit to fordringshaver account. All ledger entries reference `ModregningEvent.id` and the GIL § 7 tier applied. |
| ADR-0019 | Orchestration over event-driven | The FR-1 workflow is driven by an inbound event consumer but is orchestrated within a single transactional boundary. Digital Post is dispatched via transactional outbox after commit. |
| ADR-0027 | Modregning merged into debt-service | P058 components live in `dk.ufst.opendebt.debtservice.offsetting` package under `opendebt-debt-service`. |
| ADR-0029 | immudb for financial ledger integrity | Every `ModregningEvent` and every `CollectionMeasureEntity` with `SET_OFF` is dual-written to immudb after PostgreSQL commit (see ADR-0029 Exception in §2.2). CLS audit log entries are **not** written to immudb — ADR-0029 explicitly excludes audit log tables; CLS/Filebeat provides the external anchor. immudb failure does not roll back PostgreSQL. |
| ADR-0031 | Statutory codes as Java enums | Payment types (`OVERSKYDENDE_SKAT`, `BOERNE_OG_UNGEYDELSE`, etc.), `correctionPoolTarget` (`PSRM`, `DMI`), and `ExceptionType` (`NONE`, `FIVE_BANKING_DAY`, `KILDESKATTELOV`) are Java enums, not free-text configuration. |

### 2.2 Architectural Decisions Made by This Document

| Decision | Rationale |
|----------|-----------|
| **P058 does not re-implement GIL § 4 ordering** | `ModregningsRaekkefoeigenEngine` delegates all tier-2 partial-coverage logic to `DaekningsRaekkefoeigenService` (P057) via a single HTTP call. Re-implementing violates the single-responsibility principle and would create a maintenance divergence risk for a statutory algorithm. |
| **`RenteGodtgoerelseRateEntry` table is the authoritative rate store** | Rates seeded from `BusinessConfigService`-aware tooling; `computeRate()` queries `rentegodt_rate_entry` with effective-date ordering. This supports the 5-banking-day delayed rate-change effect without special-casing logic in the service. |
| **Korrektionspulje settlement re-enters FR-1** | `KorrektionspuljeService.settleEntry()` calls `ModregningService.initiateModregning()` with the settled amount. This ensures settled pool amounts are processed through the full three-tier ordering engine, including ledger entry generation and Digital Post notice, without duplicating that logic. |
| **Digital Post via transactional outbox** | The `notification_outbox` table write is within the `@Transactional` boundary of `initiateModregning`; actual dispatch by `NotificationService` outbox poller happens post-commit. This prevents dispatch before commit (a legally significant failure condition). |
| **immudb write is best-effort, not transactional** | The immudb append happens in a post-commit listener. A failed immudb write logs an alert metric but does not roll back the PostgreSQL transaction. immudb is a tamper-evidence audit trail, not the source of truth. |
| **`nemkontoReferenceId` as the idempotency key** | The UNIQUE constraint on `modregning_event.nemkonto_reference_id` and a pre-check SELECT guard ensure that replaying the same Nemkonto event produces no duplicate state. Race-condition concurrent inserts are caught via `DataIntegrityViolationException`. |
| **[ADR-0029 Exception] `ModregningEvent` and `SET_OFF` records written to immudb** | ADR-0029 defines immudb scope as "financial ledger entries only" in payment-service tables (`ledger_entries`, `debt_events`). Writing `ModregningEvent` and `SET_OFF` `CollectionMeasureEntity` records from debt-service to immudb constitutes a scope expansion. Legal justification: these are legally-binding offsetting decision records subject to Rigsrevisionen audit exposure under GIL § 7, stk. 1 legal-standing requirements, and Gæld.bekendtg. § 7 traceability obligations. CLS audit log entries remain excluded per ADR-0029. **Open item: an ADR-0029 amendment documenting this scope expansion and its legal justification must be approved before the P058 deployment gate.** |

---

## 3. Component Architecture

### 3.1 Component Responsibilities

All components reside in `opendebt-debt-service` under root package `dk.ufst.opendebt.debtservice`.

| Component | Package | Type | Responsibility |
|-----------|---------|------|----------------|
| `ModregningService` | `.service` | Spring `@Service` | Orchestrates the complete FR-1 workflow. Implements `OffsettingService` interface (P007 stub replaced). Handles idempotency, tier delegation, ledger posting (via `LedgerServiceClient`), outbox write. Entry point for FR-2 waiver. At initial persist, `klageFristDato = decisionDate + 1 year` (failure-case value); recomputed to `noticeDeliveryDate + 3 months` in the post-commit callback when notice delivery succeeds. |
| `ModregningsRaekkefoeigenEngine` | `.service` | Spring `@Service` | Executes the three-tier GIL § 7 allocation algorithm. Queries active fordringer via internal API. Delegates tier-2 partial allocation to `DaekningsRaekkefoeigenServiceClient`. Pure stateless computation — no persistence. |
| `KorrektionspuljeService` | `.service` | Spring `@Service` | Processes `OffsettingReversalEvent`: Step 1 (same-fordring residual), Step 2 (gendækning via P057), Step 3 (KorrektionspuljeEntry creation). Settles pool entries by re-invoking `ModregningService`. |
| `RenteGodtgoerelseService` | `.service` | Spring `@Service` | Computes rentegodtgørelse start date (with 5-banking-day and kildeskattelov exceptions) and rate (from `rentegodt_rate_entry` table). Pure date/rate computation — no side effects. |
| `KorrektionspuljeSettlementJob` | `.batch` | Spring `@Scheduled` | Monthly and annual scheduled jobs that iterate unsettled PSRM-target pool entries and invoke `KorrektionspuljeService.settleEntry()` per entry. Each settlement is its own transaction. |
| `PublicDisbursementEventConsumer` | `.consumer` | Event consumer | Receives `PublicDisbursementEvent` from Nemkonto. Validates required fields. Delegates to `ModregningService.initiateModregning()`. Dead-letters validation-failed events. |
| `OffsettingReversalEventConsumer` | `.consumer` | Event consumer | Receives `OffsettingReversalEvent` from P053. Validates idempotency (no existing `KorrektionspuljeEntry` for `originModregningEventId`). Delegates to `KorrektionspuljeService.processReversal()`. |
| `ModregningController` | `.controller` | Spring `@RestController` | Exposes: `POST .../tier2-waiver` (FR-2, scope `modregning:waiver`); `GET .../modregning-events` (FR-5, scope `modregning:read`). |
| `DaekningsRaekkefoeigenServiceClient` | `.client` | HTTP client | HTTP client for P057's internal allocation API in `opendebt-payment-service`. Called at most once per engine run. |
| `LedgerServiceClient` | `.client` | HTTP client | Posts double-entry debit/credit ledger entries to payment-service bookkeeping API (ADR-0018, ADR-0007). Called from `ModregningService` within the `@Transactional` boundary — one call per covered fordring allocation. Each entry references `ModregningEvent.id` and the GIL § 7 tier applied. HTTP failure propagates and triggers full `@Transactional` rollback. |
| `FordringQueryPort` | `.port` | Spring `@Component` | Internal adapter for TB-040 active-fordringer queries within `opendebt-debt-service` (same-service — no inter-service HTTP). Exposes typed Java API `getActiveFordringer(debtorPersonId, tier, payingAuthorityOrgId)` backed by JPA repository. Consumed exclusively by `ModregningsRaekkefoeigenEngine`. |

### 3.2 Component Dependency Graph

```
PublicDisbursementEventConsumer
  └─▶ ModregningService (initiateModregning)
        ├─▶ ModregningsRaekkefoeigenEngine (allocate)
        │     ├─▶ FordringQueryPort (active fordringer by tier — TB-040)
        │     └─▶ DaekningsRaekkefoeigenServiceClient (tier-2 partial only)
        ├─▶ RenteGodtgoerelseService (computeDecision)
        ├─▶ LedgerServiceClient (double-entry via payment-service — ADR-0018)
        ├─▶ notification_outbox (write)
        └─▶ immudb (post-commit audit append — ADR-0029)

OffsettingReversalEventConsumer
  └─▶ KorrektionspuljeService (processReversal)
        ├─▶ DaekningsRaekkefoeigenServiceClient (gendækning step 2)
        └─▶ ModregningService (settlement re-entry via settleEntry)

ModregningController
  ├─▶ ModregningService (applyTier2Waiver)
  └─▶ ModregningEventRepository (read model)

KorrektionspuljeSettlementJob
  └─▶ KorrektionspuljeService (settleEntry)
        ├─▶ RenteGodtgoerelseService (computeRate)
        └─▶ ModregningService (initiateModregning — re-entry)
```

---

## 4. Data Model

### 4.1 New Tables

#### `modregning_event` (FR-1, FR-2, FR-4, FR-5)

| Column | SQL type | Nullable | Constraint | Notes |
|--------|----------|----------|------------|-------|
| `id` | `uuid` | NO | PK | |
| `nemkonto_reference_id` | `varchar(100)` | NO | UNIQUE | Idempotency key (NFR-4) |
| `debtor_person_id` | `uuid` | NO | indexed | ADR-0014 — no CPR |
| `receipt_date` | `date` | NO | | Date RIM received disbursement |
| `decision_date` | `date` | NO | | Date of modregning decision |
| `payment_type` | `varchar(50)` | NO | | Enum: `OVERSKYDENDE_SKAT`, `BOERNE_OG_UNGEYDELSE`, etc. (ADR-0031) |
| `indkomst_aar` | `integer` | YES | | Set only for `OVERSKYDENDE_SKAT` (kildeskattelov § 62/62A) |
| `disbursement_amount` | `numeric(15,2)` | NO | ≥ 0 | Total intercepted |
| `tier1_amount` | `numeric(15,2)` | NO | ≥ 0 DEFAULT 0 | GIL § 7, stk. 1, nr. 1 |
| `tier2_amount` | `numeric(15,2)` | NO | ≥ 0 DEFAULT 0 | GIL § 7, stk. 1, nr. 2 |
| `tier3_amount` | `numeric(15,2)` | NO | ≥ 0 DEFAULT 0 | GIL § 7, stk. 1, nr. 3 |
| `residual_payout_amount` | `numeric(15,2)` | NO | ≥ 0 DEFAULT 0 | Surplus returned to debtor |
| `tier2_waiver_applied` | `boolean` | NO | DEFAULT false | GIL § 4, stk. 11 |
| `notice_delivered` | `boolean` | NO | | Digital Post delivery outcome |
| `notice_delivery_date` | `date` | YES | | Set when `notice_delivered = true` |
| `klage_frist_dato` | `date` | NO | NOT NULL | Appeal deadline (GIL § 17) |
| `rente_godtgoerelse_start_date` | `date` | YES | | Null when 5-banking-day exception |
| `rente_godtgoerelse_non_taxable` | `boolean` | NO | DEFAULT true | Always true (GIL § 8b, stk. 2, 3. pkt.) |

**Invariant:** `tier1_amount + tier2_amount + tier3_amount + residual_payout_amount = disbursement_amount`

#### `korrektionspulje_entry` (FR-3)

| Column | SQL type | Nullable | Constraint | Notes |
|--------|----------|----------|------------|-------|
| `id` | `uuid` | NO | PK | |
| `debtor_person_id` | `uuid` | NO | indexed | ADR-0014 |
| `origin_event_id` | `uuid` | NO | FK → `modregning_event.id` | Reversed `ModregningEvent` |
| `surplus_amount` | `numeric(15,2)` | NO | > 0 | Amount placed in pool |
| `correction_pool_target` | `varchar(10)` | NO | `IN ('PSRM','DMI')` | Routing flag (ADR-0031) |
| `boerne_ydelse_restriction` | `boolean` | NO | DEFAULT false | GIL § 4, stk. 7, nr. 3 |
| `rente_godtgoerelse_start_date` | `date` | NO | | Day after origin event's decision date |
| `rente_godtgoerelse_accrued` | `numeric(15,2)` | NO | ≥ 0 DEFAULT 0 | Accrued interest |
| `annual_only_settlement` | `boolean` | NO | DEFAULT false | `surplus_amount < 50.00` |
| `settled_at` | `timestamptz` | YES | | Null until settled |
| `created_at` | `timestamptz` | NO | DEFAULT now() | |

**Partial index:** `idx_kpe_settled` on `settled_at` WHERE `settled_at IS NULL` (job query performance).

#### `rentegodt_rate_entry` (FR-4)

| Column | SQL type | Nullable | Constraint | Notes |
|--------|----------|----------|------------|-------|
| `id` | `uuid` | NO | PK | |
| `publication_date` | `date` | NO | UNIQUE | Rentelov § 5 published date |
| `effective_date` | `date` | NO | indexed | Publication + 5 banking days |
| `reference_rate_percent` | `numeric(6,4)` | NO | ≥ 0 | Raw rentelov § 5 rate |
| `godtgoerelse_rate_percent` | `numeric(6,4)` | NO | ≥ 0 | `MAX(0, reference − 4.0)` |
| `created_at` | `timestamptz` | NO | DEFAULT now() | |

**Invariant:** `godtgoerelse_rate_percent = MAX(0, reference_rate_percent - 4.0)` — computed and stored at insert time.

### 4.2 Extensions to `collection_measure`

| New Column | SQL type | Default | Notes |
|------------|----------|---------|-------|
| `modregning_event_id` | `uuid` | NULL | FK → `modregning_event.id`; MUST NOT be null when `measure_type = SET_OFF` |
| `waiver_applied` | `boolean` | false | GIL § 4, stk. 11; `true` on measures reversed or created by a tier-2 waiver |
| `caseworker_id` | `uuid` | NULL | Set when `waiver_applied = true` |

### 4.3 Migration Plan

| Migration | File | Applies |
|-----------|------|---------|
| V4 | `V4__modregning_event.sql` | CREATE TABLE `modregning_event`; indexes; UNIQUE on `nemkonto_reference_id` |
| V5 | `V5__korrektionspulje_entry.sql` | CREATE TABLE `korrektionspulje_entry`; partial index; FK |
| V6 | `V6__rentegodt_rate_entry.sql` | CREATE TABLE `rentegodt_rate_entry`; UNIQUE on `publication_date` |
| V7 | `V7__collection_measure_modregning_cols.sql` | ALTER TABLE `collection_measure` ADD three new columns; NOT NULL check for SET_OFF rows |

---

## 5. Data Flows

### 5.1 Three-Tier Offsetting Workflow (FR-1)

```
Nemkonto
   │ PublicDisbursementEvent
   ▼
PublicDisbursementEventConsumer
   │ validate fields; idempotency pre-check
   ▼
ModregningService.initiateModregning()              ──────── @Transactional ─────────────────────────┐
   │                                                                                                   │
   ├─▶ ModregningsRaekkefoeigenEngine.allocate()                                                       │
   │      │ Tier-1: GET /internal/debtors/{id}/fordringer/active?tier=1&payingAuthority={orgId}        │
   │      │         Apply disbursement to tier-1 fordringer                                            │
   │      │         [SHORT-CIRCUIT if remaining = 0 → skip tier-2/3 and P057 call]                    │
   │      │ Tier-2: GET /internal/debtors/{id}/fordringer/active?tier=2                               │
   │      │         If partial coverage → DaekningsRaekkefoeigenServiceClient.allocate() [ONCE]        │
   │      │ Tier-3: GET /internal/debtors/{id}/fordringer/active?tier=3 (ascending registreringsdato) │
   │      └─▶ TierAllocationResult                                                                     │
   │                                                                                                   │
   ├─▶ RenteGodtgoerelseService.computeDecision(receiptDate, decisionDate, paymentType, indkomstAar)  │
   │      └─▶ RenteGodtgoerelseDecision {startDate, exceptionApplied}                                 │
   │                                                                                                   │
   ├─▶ Persist ModregningEvent (all tier amounts, klageFristDato, renteGodtgoerelseStartDate)          │
   ├─▶ Persist CollectionMeasureEntity (SET_OFF, with modregning_event_id) per covered fordring        │
   ├─▶ Write double-entry ledger entries (ADR-0018) per fordring allocation                            │
   ├─▶ Write CLS audit log entry per allocation (gilParagraf = "GIL § 7, stk. 1, nr. {tier}")         │
   └─▶ Write to notification_outbox (transactional — post-commit dispatch)                             │
                                                                                                       │
──────────────── COMMIT ────────────────────────────────────────────────────────────────────────────────┘
   │
   ├─▶ immudb append (post-commit, best-effort): modregning:{eventId}, measure:{measureId}
   └─▶ NotificationService outbox poller:
          Dispatch Digital Post notice → on success/failure:
             Update ModregningEvent.noticeDelivered, noticeDeliveryDate
             Recompute klageFristDato (follow-up transaction)
```

**Key invariants enforced in this flow:**
- `DaekningsRaekkefoeigenService` is called at most once per invocation (AC-3)
- `DaekningsRaekkefoeigenService` is NOT called if tier-1 fully consumes disbursement (AC-2)
- Digital Post dispatch happens post-commit, never within the transaction (NFR-1)
- `renteGodtgoerelseNonTaxable = true` on every `ModregningEvent` (GIL § 8b, stk. 2)
- At initial persist, `klageFristDato = decisionDate + 1 year` (failure-case value); recomputed to `noticeDeliveryDate + 3 months` in the post-commit callback when notice delivery succeeds (GIL § 17, stk. 1)

### 5.2 Tier-2 Waiver Workflow (FR-2)

```
CaseworkerPortal
   │ POST /api/v1/debtors/{debtorId}/modregning-events/{eventId}/tier2-waiver
   │ OAuth2 scope: modregning:waiver (403 if absent)
   ▼
ModregningController
   ▼
ModregningService.applyTier2Waiver()                ──────── @Transactional ───────────────────────┐
   │                                                                                                 │
   ├─▶ Load ModregningEvent; assert tier2WaiverApplied = false (409 if already applied)             │
   ├─▶ Set tier2WaiverApplied = true                                                                 │
   ├─▶ Set waiver_applied = true, caseworker_id = caseworkerId on existing tier-2 CollectionMeasures │
   ├─▶ Reverse ledger entries for tier-2 allocations                                                 │
   ├─▶ ModregningsRaekkefoeigenEngine.allocate(skipTier2 = true)                                    │
   │      [Re-run with tier-1 amounts already fixed; residual applied to tier-3 only]               │
   ├─▶ Create new CollectionMeasureEntity for tier-3 fordringer (waiver_applied = true)              │
   ├─▶ Update ModregningEvent tier amounts (tier2Amount = 0)                                         │
   └─▶ Write CLS audit log (eventType = MODREGNING_TIER2_WAIVER, gilParagraf = "GIL § 4, stk. 11") │
                                                                                                     │
──────────────── COMMIT ──────────────────────────────────────────────────────────────────────────────┘
   └─▶ Return updated ModregningResult (HTTP 200)
```

### 5.3 Korrektionspulje Workflow (FR-3)

```
P053 (opskrivning/nedskrivning)
   │ OffsettingReversalEvent
   ▼
OffsettingReversalEventConsumer
   │ idempotency guard: no KorrektionspuljeEntry for originModregningEventId
   ▼
KorrektionspuljeService.processReversal()           ──────── @Transactional ───────────────────────┐
   │                                                                                                 │
   ├─▶ STEP 1: Residual same-fordring coverage (Gæld.bekendtg. § 7, stk. 4)                        │
   │      Apply surplus to uncovered portion of reversedFordringId (P057 sub-position order)        │
   │      Persist ledger adjustment for step1Consumed                                                │
   │                                                                                                 │
   ├─▶ STEP 2: Gendækning (GIL § 4, stk. 5–6)                                                      │
   │      IF optOut (correctionPoolTarget=DMI, debt-under-collection, retroactive partial): skip    │
   │      ELSE: DaekningsRaekkefoeigenServiceClient.allocate(debtorPersonId, remaining)             │
   │            [No Digital Post notice for gendækning]                                              │
   │                                                                                                 │
   └─▶ STEP 3: Create KorrektionspuljeEntry if remaining > 0                                        │
          {surplusAmount, correctionPoolTarget, boerneYdelseRestriction,                             │
           renteGodtgoerelseStartDate = originEvent.decisionDate + 1 day,                           │
           annualOnlySettlement = (remaining < 50.00 DKK)}                                          │
                                                                                                     │
──────────────── COMMIT ──────────────────────────────────────────────────────────────────────────────┘
```

### 5.4 Monthly and Annual Settlement (FR-3.4)

```
KorrektionspuljeSettlementJob.runMonthlySettlement()   [Cron: 3 AM on 1st of month]
   │ Query: settled_at IS NULL AND correctionPoolTarget = PSRM AND annualOnlySettlement = false
   │ [Entries with surplusAmount < 50 DKK are excluded — annualOnlySettlement = true]
   ▼
   FOR each unsettled entry:
      KorrektionspuljeService.settleEntry(entry, today)   ──── @Transactional (per entry) ──────────┐
         │                                                                                             │
         ├─▶ Compute renteGodtgoerelseAccrued:                                                        │
         │      rate = RenteGodtgoerelseService.computeRate(today)                                    │
         │      days = ChronoUnit.DAYS.between(entry.renteGodtgoerelseStartDate, today)              │
         │      accrued = surplusAmount × (rate/100) × days/365  [HALF_UP, 2 dp]                    │
         │      Persist accrued on entry                                                              │
         ├─▶ total = surplusAmount + renteGodtgoerelseAccrued                                         │
         ├─▶ ModregningService.initiateModregning(                                                    │
         │        debtorPersonId, total, paymentType, null,                                           │
         │        restrictedPayment = entry.boerneYdelseRestriction)                                  │
         │      [Note: origin-nature lost — transporter/udlæg do NOT carry over, except pre-2021]   │
         │      [boerneYdelseRestriction = true RETAINS the børne-og-ungeydelse restriction]         │
         └─▶ Set entry.settledAt = Instant.now()                                                      │
                                                                                                       │
         ──── COMMIT ────────────────────────────────────────────────────────────────────────────────── ┘
         [On failure: log error with entryId; continue to next entry — no full-job rollback]

KorrektionspuljeSettlementJob.runAnnualSettlement()   [Cron: 4 AM on 2 January]
   │ Query: settled_at IS NULL AND correctionPoolTarget = PSRM (includes annualOnlySettlement = true)
   │ [Processes all unsettled PSRM entries including sub-50 DKK entries]
   └─▶ Same settleEntry() loop as monthly
```

### 5.5 Rentegodtgørelse Decision (FR-4)

```
RenteGodtgoerelseService.computeDecision(receiptDate, decisionDate, paymentType, indkomstAar)

  1. bankingDays = DanishBankingCalendar.bankingDaysBetween(receiptDate, decisionDate)
     IF bankingDays ≤ 5:
        → return {startDate = null, exceptionApplied = FIVE_BANKING_DAY}
           [No rentegodtgørelse accrued; null start date on ModregningEvent]

  2. IF paymentType = OVERSKYDENDE_SKAT:
        kildeskatDate = LocalDate.of(indkomstAar + 1, 9, 1)    [1 Sep year+1]
        standardDate  = receiptDate.plusMonths(1).withDayOfMonth(1)
        → return {startDate = MAX(kildeskatDate, standardDate), exceptionApplied = KILDESKATTELOV if kildeskat later}

  3. Standard case:
        → return {startDate = receiptDate.plusMonths(1).withDayOfMonth(1), exceptionApplied = NONE}

RenteGodtgoerelseService.computeRate(referenceDate)
   SELECT godtgoerelse_rate_percent FROM rentegodt_rate_entry
   WHERE effective_date ≤ referenceDate ORDER BY effective_date DESC LIMIT 1
   [Rate = MAX(0, rentelov § 5 refRate − 4.0 pp); effective 5 banking days after publication]
```

---

## 6. Interface Specifications

### 6.1 REST API (ModregningController)

**Base path:** `/api/v1`  
**Authentication:** OAuth2 Bearer JWT (Keycloak — ADR-0005)

#### POST /api/v1/debtors/{debtorId}/modregning-events/{eventId}/tier2-waiver

| Attribute | Value |
|-----------|-------|
| Required scope | `modregning:waiver` |
| Request body | `{ "waiverReason": "string (1–500)", "caseworkerId": "uuid" }` |
| Success | HTTP 200 — updated `ModregningResult` |
| Error 400 | Missing/blank `waiverReason` or `caseworkerId` |
| Error 403 | Missing `modregning:waiver` scope |
| Error 404 | Debtor or event not found |
| Error 409 | `tier2WaiverApplied` already true |

#### GET /api/v1/debtors/{debtorId}/modregning-events

| Attribute | Value |
|-----------|-------|
| Required scope | `modregning:read` OR `modregning:waiver` |
| Query params | `page` (default 0), `size` (default 20, max 100), `sort` (default `decisionDate,desc`) |
| Success | HTTP 200 — paginated array of `ModregningEventSummary` |
| Mandatory response fields | `eventId`, `decisionDate`, `totalOffsetAmount`, `tier1Amount`, `tier2Amount`, `tier3Amount`, `residualPayoutAmount`, `klageFristDato`, `noticeDelivered`, `tier2WaiverApplied`, `renteGodtgoerelseNonTaxable`, `renteGodtgoerelseStartDate` |
| Error 403 | Missing required scope |
| Error 404 | `debtorId` not found in Person Registry |

### 6.2 Internal API Dependency — DaekningsRaekkefoeigenService (P057)

**Client:** `DaekningsRaekkefoeigenServiceClient` in `dk.ufst.opendebt.debtservice.client`  
**Invocation pattern:** HTTP REST (ADR-0007 — no direct DB access)  
**Host:** `opendebt-payment-service`

| Field | Value |
|-------|-------|
| Input | `UUID debtorPersonId`, `BigDecimal amount` |
| Output | `List<FordringAllocation>` — `{fordringId, amountCovered}` per fordring |
| Sum invariant | `SUM(amountCovered) = input amount` (P057 contract) |
| Empty result | Returns `[]` if no eligible tier-2 fordringer (full amount unallocated) |
| Call constraint | At most once per `ModregningsRaekkefoeigenEngine.allocate()` invocation |

**Call sites and conditions:**

| Call site | Condition |
|-----------|-----------|
| `ModregningsRaekkefoeigenEngine` | Tier-2 partial coverage only (residual < tier-2 total) |
| `KorrektionspuljeService` | Gendækning step 2 (when opt-out conditions are absent) |

### 6.3 Active Fordringer Internal API (TB-040)

**Host:** `opendebt-debt-service` internal endpoint  
**Purpose:** Query debtor's active fordringer by tier and optionally by paying authority

| Endpoint | Used by | Condition |
|----------|---------|-----------|
| `GET /internal/debtors/{id}/fordringer/active?tier=1&payingAuthority={orgId}` | `ModregningsRaekkefoeigenEngine` | Tier-1 resolution (always) |
| `GET /internal/debtors/{id}/fordringer/active?tier=2` | `ModregningsRaekkefoeigenEngine` | Tier-2 resolution (if remaining > 0) |
| `GET /internal/debtors/{id}/fordringer/active?tier=3` | `ModregningsRaekkefoeigenEngine` | Tier-3 resolution (if remaining > 0 after tier-2) |

### 6.4 Event Consumer Contracts

#### PublicDisbursementEventConsumer

| Field | Type | Required | Source |
|-------|------|----------|--------|
| `nemkontoReferenceId` | String | YES | Idempotency key |
| `debtorPersonId` | UUID | YES | Forwarded to service |
| `disbursementAmount` | BigDecimal | YES | |
| `paymentType` | String (enum) | YES | ADR-0031 |
| `indkomstAar` | Integer | NO | Only for `OVERSKYDENDE_SKAT` |
| `payingAuthorityOrgId` | UUID | YES | For tier-1 fordring resolution |
| `receiptDate` | LocalDate | YES | |

**Failure handling:** Validation failures → DLQ (no retry). Transient failures → broker retry (idempotency guard ensures safe replay).

#### OffsettingReversalEventConsumer

| Field | Type | Required | Source |
|-------|------|----------|--------|
| `originModregningEventId` | UUID | YES | |
| `reversedFordringId` | UUID | YES | |
| `surplusAmount` | BigDecimal | YES | |
| `debtorPersonId` | UUID | YES | |
| `correctionPoolTarget` | String (enum) | YES | ADR-0031 |
| `originalPaymentType` | String (enum) | YES | |

### 6.5 Digital Post Outbox Contract

**Table:** `notification_outbox` (existing schema from P052)  
**Write timing:** Within `@Transactional` boundary of `ModregningService.initiateModregning()`  
**Dispatch:** Post-commit by `NotificationService` outbox poller

**Outbox message payload:**
```json
{
  "notificationType": "MODREGNING_NOTICE",
  "debtorPersonId": "uuid",
  "modregningEventId": "uuid",
  "totalOffsetAmount": 10000.00,
  "coverages": [
    { "fordringshaver": "string", "fordringId": "uuid", "amount": 5000.00, "tier": 2 }
  ],
  "renteGodtgoerelseStartDate": "2025-04-01",
  "renteGodtgoerelseNonTaxable": true,
  "klageFristDato": "2025-06-15"
}
```

**Post-delivery callback:** `NotificationService` updates `ModregningEvent.noticeDelivered`, `noticeDeliveryDate` in a follow-up transaction; `klageFristDato` is recomputed.

**No Digital Post for gendækning:** `KorrektionspuljeService.processReversal()` MUST NOT enqueue any outbox message for Step 2 gendækning allocations.

### 6.6 LedgerServiceClient — Payment-Service Bookkeeping API (ADR-0018)

**Client:** `LedgerServiceClient` in `dk.ufst.opendebt.debtservice.client`  
**Invocation pattern:** HTTP REST (ADR-0007 — no direct DB access)  
**Host:** `opendebt-payment-service`  
**Called from:** `ModregningService.initiateModregning()` within the `@Transactional` boundary — one call per covered fordring allocation.

| Field | Value |
|-------|-------|
| Endpoint | `POST /internal/ledger/double-entry` |
| Input | `{ "debitAccount": "uuid", "creditAccount": "uuid", "amount": "BigDecimal", "currency": "DKK", "referenceId": "uuid", "referenceType": "MODREGNING_EVENT", "gilParagraf": "GIL § 7, stk. 1, nr. {tier}", "timestamp": "Instant" }` |
| Output | `{ "ledgerEntryId": "uuid" }` |
| Failure behaviour | Throws `LedgerServiceException`; propagates through `@Transactional` boundary causing full FR-1 rollback |
| Idempotency | `referenceId` + `referenceType` uniqueness enforced by payment-service ledger |
| Call constraint | One call per covered fordring (not once per `ModregningEvent`) |

**Distributed transaction note:** This HTTP call is within the Spring `@Transactional` scope but not within the PostgreSQL transaction boundary. A payment-service ledger success followed by a debt-service PostgreSQL rollback will leave an orphaned ledger entry recoverable via reconciliation against `ModregningEvent` state. This is a known and accepted limitation of the ADR-0007/ADR-0018 separation.

---

## 7. Integration Patterns

### 7.1 P057 Delegation Pattern

**Constraint (ADR-0007):** `ModregningsRaekkefoeigenEngine` does NOT re-implement GIL § 4 ordering logic. It delegates exclusively to `DaekningsRaekkefoeigenServiceClient` for all tier-2 partial-coverage scenarios.

**Invocation guard:** The engine tracks whether P057 has been called per execution. If called once and additional residual remains, it is treated as unallocated (P057 contract violation is surfaced as `InsufficientFundsException`).

**When P057 is NOT called:**
- Tier-1 fully consumes the disbursement (short-circuit to zero residual)
- Tier-2 full coverage (residual ≥ sum of all tier-2 outstanding amounts)
- Tier-2 waiver applied (`skipTier2 = true`)
- Gendækning opt-out conditions present

### 7.2 immudb Write Pattern (ADR-0029)

**Dual-write, post-commit, best-effort:**

1. `ModregningService.initiateModregning()` commits to PostgreSQL within the `@Transactional` boundary.
2. A Spring `@TransactionalEventListener(phase = AFTER_COMMIT)` appends to immudb with keys:
   - `modregning:{eventId}` — serialised `ModregningEvent`
   - `measure:{measureId}` — per SET_OFF `CollectionMeasureEntity`
3. If the immudb write fails: PostgreSQL is NOT rolled back; failure is logged and an alert metric is emitted.

**Key format convention:**
```
modregning:{UUID}     → ModregningEvent record
measure:{UUID}        → CollectionMeasureEntity (SET_OFF)
```

> **Note:** CLS audit log entries are explicitly excluded from immudb per ADR-0029 ("audit_log tables: CLS/Filebeat provides sufficient external anchor"). Only `ModregningEvent` and `SET_OFF` `CollectionMeasureEntity` records are appended to immudb (see ADR-0029 Exception in §2.2).

### 7.3 Transactional Outbox Pattern (ADR-0019)

The Digital Post notice for modregning decisions follows the outbox pattern established in P052:

1. **Within transaction:** `notification_outbox` row is inserted with `status = PENDING`.
2. **After commit:** `NotificationService` poller picks up PENDING rows and dispatches via Digital Post gateway.
3. **On success:** Row updated to `SENT`; `ModregningEvent.noticeDelivered = true`.
4. **On failure:** Row updated to `FAILED`; `ModregningEvent.noticeDelivered = false`; `klageFristDato` recomputed as `decisionDate + 1 year` (GIL § 17).

**Failure condition (legally significant):** Digital Post MUST NOT be dispatched before the PostgreSQL transaction commits. Any violation means the disbursement record may be missing in case of rollback, yet the debtor has been notified.

### 7.4 BusinessConfigService — Rate Seeding

`RenteGodtgoerelseService.computeRate()` queries `rentegodt_rate_entry` as the authoritative source for the effective-dated godtgørelse rate. The `rentegodt_rate_entry` table is populated by a `BusinessConfigService`-aware seeding mechanism (either a DB migration for known rates, or a developer-operated admin tool when new semi-annual rates are published).

`BusinessConfigService` key: `rentelov.refRate` — the raw rentelov § 5, stk. 1+2 reference rate (percentage). The `godtgoerelse_rate_percent` stored in the table is pre-computed as `MAX(0, refRate − 4.0)` at insertion time and not recalculated at query time.

**Rate change effective date:** New rows inserted with `effective_date = publication_date + 5 banking days` (computed via `DanishBankingCalendar`). Rate queries always use the latest row where `effective_date ≤ referenceDate`.

---

## 8. Non-Functional Architecture Decisions

### 8.1 Atomicity (NFR-1)

The full FR-1 workflow executes within a single Spring `@Transactional(rollbackOn = Exception.class)` boundary:
- `ModregningEvent` creation
- `CollectionMeasureEntity` creation (one per covered fordring)
- Double-entry ledger entry generation (ADR-0018)
- CLS audit log write per allocation
- `notification_outbox` row insert

**If any step fails:** Full rollback — no partial state is persisted. Digital Post dispatch is structurally impossible within the transaction boundary (it is outbox-mediated).

**Settlement transactions:** Each `KorrektionspuljeSettlementJob` entry settlement is its own `@Transactional` boundary. Failure of one entry does not roll back other entries in the same job run.

### 8.2 Auditability (NFR-2)

Every modregning decision writes to two audit channels:

1. **CLS audit log** (within `@Transactional`):
   - Per fordring allocation: `{eventType: MODREGNING_ALLOCATION, gilParagraf: "GIL § 7, stk. 1, nr. {tier}", modregningEventId, debtorPersonId, fordringId, amountCovered, tier, timestamp}`
   - Per tier-2 waiver: `{eventType: MODREGNING_TIER2_WAIVER, gilParagraf: "GIL § 4, stk. 11", modregningEventId, caseworkerId, waiverReason, timestamp}`

2. **immudb** (post-commit, tamper-evident): `ModregningEvent`, `CollectionMeasureEntity` records appended as immutable entries with key-based retrieval.

Both channels are mandatory; neither can substitute for the other.

### 8.3 GDPR (NFR-3, ADR-0014)

All P058 domain tables reference debtors exclusively via `debtor_person_id UUID`. CPR numbers, names, addresses, and any other direct personal identifiers MUST NOT appear in:
- Any P058 table column
- Any index
- Any CLS log entry
- Any outbox message payload
- Any immudb key or value

Debtor personal details are resolved exclusively via `PersonRegistry` service (ADR-0014) at query time — never stored alongside financial data.

### 8.4 Idempotency (NFR-4)

| Scenario | Guard | Response |
|----------|-------|----------|
| Same `nemkontoReferenceId` replayed | UNIQUE constraint + pre-check SELECT | HTTP 200 — returns existing `ModregningResult`; no new state |
| Race-condition concurrent insert | Catch `DataIntegrityViolationException`; re-query | HTTP 200 — returns existing event |
| Same `OffsettingReversalEvent` replayed | Pre-check: `KorrektionspuljeEntry` for `origin_event_id` exists | Return without duplicate entry |
| Same waiver replayed | Check `tier2WaiverApplied = true` | HTTP 409 |

### 8.5 Performance (NFR-5)

The three-tier ordering engine must resolve and persist a `ModregningEvent` covering up to 500 active fordringer within 2 seconds at p99 under normal load (measured from event ingestion to transaction commit).

**Architecture enablers:**
- All fordringer queries use indexed `debtor_person_id` + `tier` columns.
- `DaekningsRaekkefoeigenService` is called at most once per run (not once per fordring).
- No cross-service database joins — all data fetched via indexed REST calls.
- `ModregningsRaekkefoeigenEngine` is a pure in-memory algorithm after the three HTTP calls return.

---

## 9. Traceability Matrix

### 9.1 Requirement → Component

| FR | Component(s) | Table(s) |
|----|-------------|---------|
| FR-1 (three-tier workflow) | `ModregningService`, `ModregningsRaekkefoeigenEngine`, `FordringQueryPort`, `LedgerServiceClient`, `PublicDisbursementEventConsumer` | `modregning_event`, `collection_measure` |
| FR-2 (tier-2 waiver) | `ModregningService.applyTier2Waiver()`, `ModregningController` | `modregning_event`, `collection_measure` |
| FR-3 (korrektionspulje) | `KorrektionspuljeService`, `KorrektionspuljeSettlementJob`, `OffsettingReversalEventConsumer` | `korrektionspulje_entry`, `modregning_event` |
| FR-4 (rentegodtgørelse) | `RenteGodtgoerelseService` | `rentegodt_rate_entry` |
| FR-5 (klage deadline) | `ModregningService` (compute at persist), `ModregningController` (read model) | `modregning_event` |

### 9.2 Requirement → ADR

| Requirement | ADR(s) Applied |
|-------------|---------------|
| Atomic FR-1 workflow | ADR-0018 (double-entry), ADR-0019 (outbox) |
| No cross-service DB | ADR-0007 |
| UUID-only person refs | ADR-0014 |
| immudb audit | ADR-0029 |
| Statutory code types | ADR-0031 |
| Modregning in debt service | ADR-0027 |
| OAuth2 enforcement | ADR-0005 |

### 9.3 Acceptance Criteria → Component

| AC | Component(s) |
|----|-------------|
| AC-1 (three-tier mixed coverage) | `ModregningsRaekkefoeigenEngine`, `ModregningService` |
| AC-2 (tier-1 full — no P057 call) | `ModregningsRaekkefoeigenEngine` |
| AC-3 (tier-2 partial — P057 once) | `ModregningsRaekkefoeigenEngine`, `DaekningsRaekkefoeigenServiceClient` |
| AC-4 (SET_OFF measure per fordring) | `ModregningService` |
| AC-5 (idempotent replay) | `ModregningService` (idempotency guard) |
| AC-6 (waiver re-run) | `ModregningService.applyTier2Waiver()`, `ModregningController` |
| AC-7 (waiver 403) | `ModregningController` (`@PreAuthorize`) |
| AC-8 (gendækning after reversal) | `KorrektionspuljeService` |
| AC-9 (< 50 DKK annual-only) | `KorrektionspuljeService` (annualOnlySettlement flag), `KorrektionspuljeSettlementJob` |
| AC-10 (monthly settlement via FR-1) | `KorrektionspuljeSettlementJob`, `ModregningService` (re-entry) |
| AC-11 (børneydelse restriction preserved) | `KorrektionspuljeService`, `ModregningService` (restrictedPayment flag) |
| AC-12 (5-banking-day exception) | `RenteGodtgoerelseService` |
| AC-13 (kildeskattelov exception) | `RenteGodtgoerelseService` |
| AC-14 (renteGodtgoerelseNonTaxable = true) | `ModregningService` (always set on persist) |
| AC-15/AC-16 (klageFristDato read model) | `ModregningController` |
| AC-17 (amber/red portal indicator) | Caseworker portal `ModregningViewController` |

---

## 10. Assumptions and Open Items

### 10.1 Assumptions

| # | Assumption | Risk if wrong |
|---|-----------|---------------|
| A-1 | `notification_outbox` table from P052 is present and compatible with P058's outbox message schema | If schema differs, a migration V7 extension or adapter is needed |
| A-2 | `BusinessConfigService` provides `rentelov.refRate` and the seeding tooling populates `rentegodt_rate_entry` before any modregning event is processed | If no rate exists, `NoRenteGodtgoerelseRateException` (HTTP 500) is thrown — operational incident |
| A-3 | `DanishBankingCalendar` (used in P057) is accessible as a shared utility within `opendebt-debt-service` | If not, a local copy or adapter must be introduced |
| A-4 | `DaekningsRaekkefoeigenService` (P057) is deployed and healthy before any `PublicDisbursementEvent` with partial tier-2 coverage arrives | If P057 is unavailable, tier-2 partial events fail; idempotency guard allows safe retry on P057 recovery |
| A-5 | The `correctionPoolTarget = DMI` flag is sufficient to prevent DMI-routed entries from being settled — no DMI settlement workflow is triggered | If DMI pool settlement is needed before a future petition, a separate ADR is required |

### 10.2 Flagged for Human Review

| Item | Reason |
|------|--------|
| Konkurslov / gældsbrevslov exceptions to modregningsrækkefølge | Legal-team review required before these can be modelled; flagged in petition as future item |
| Pre-2021 transporter preservation in korrektionspulje settlement | Spec references "transporter notified before 1 October 2021" preserved via a flag on the P057 call; the precise mechanism for passing this flag to `DaekningsRaekkefoeigenService` must be confirmed with P057 team |
| GIL § 4, stk. 6 partial retroactive coverage opt-out | The flag detecting "partially covered retroactively" on the origin `CollectionMeasureEntity` must be established as part of P053 implementation; P058 reads this flag but does not set it |
| **ADR-0029 amendment required before deployment gate** | P058 writes `ModregningEvent` and `SET_OFF` `CollectionMeasureEntity` records to immudb, expanding ADR-0029's defined scope (currently limited to payment-service `ledger_entries` and `debt_events` tables). An explicit ADR-0029 amendment documenting the legal justification (Rigsrevisionen audit exposure under GIL § 7, GIL § 7 stk. 1 legal standing, Gæld.bekendtg. § 7 traceability obligations) must be approved before the P058 deployment gate. |

---

## 11. Structurizr DSL Block

The following DSL is directly mergeable into `architecture/workspace.dsl`. It adds P058 component declarations inside the `debtService` container definition, declares new relationships, and provides a component view.

**Merge instruction:**

**PREREQUISITE — Step 0: Refactor `debtService` declaration in `workspace.dsl`**

`workspace.dsl` currently declares `debtService` as a single-line container with no `{}` block. Structurizr DSL requires a `{}` block to nest component declarations. Before adding any P058 components, refactor the existing single-line declaration:

```
debtService = container "Debt Service" "Fordring (debt claim) management. Handles claim registration, prioritisation, interest calculation, and offsetting (modregning)." "Java 21 / Spring Boot 3.3, PostgreSQL" "Service"
```

to a block form:

```
debtService = container "Debt Service" "Fordring (debt claim) management. Handles claim registration, prioritisation, interest calculation, and offsetting (modregning)." "Java 21 / Spring Boot 3.3, PostgreSQL" "Service" {
    // P058 components go here — see Section A below
}
```

**Step 1:** Add the component declarations (Section A) inside the `debtService { ... }` block.

**Step 2:** Add the following container-level relationships to the `model { ... }` block after the existing `debtService` service-to-service relationships:

```
debtService -> letterService "Sends notification outbox events to" "PostgreSQL / JPA (outbox write, ADR-0019)"
debtService -> keycloak "Validates tokens via" "OAuth2/OIDC"
```

**Step 3:** Add the component-level relationship declarations (Section B) to the `model { ... }` block within the `openDebt` software system scope.

**Step 4:** Add the component view (Section C) to the `views { ... }` block.

---

```structurizr
// ═══════════════════════════════════════════════════════════════════════
// P058: Modregning og Korrektionspulje — Structurizr DSL additions
// ═══════════════════════════════════════════════════════════════════════

// ── SECTION A: Components within debtService ────────────────────────────
// Add inside: debtService = container "Debt Service" ... { ... }

publicDisbursementEventConsumer = component "PublicDisbursementEventConsumer" "Receives PublicDisbursementEvent from Nemkonto. Validates fields and idempotency. Delegates to ModregningService.initiateModregning(). Dead-letters validation failures." "Java 21, Spring @Component, Event Consumer" "Component"

offsettingReversalEventConsumer = component "OffsettingReversalEventConsumer" "Receives OffsettingReversalEvent from P053. Guards against duplicate KorrektionspuljeEntry. Delegates to KorrektionspuljeService.processReversal()." "Java 21, Spring @Component, Event Consumer" "Component"

modregningService = component "ModregningService" "Orchestrates the complete three-tier modregning workflow (FR-1). Implements OffsettingService (replaces P007 stub). Handles idempotency, ledger posting via LedgerServiceClient, Digital Post outbox write, and tier-2 waiver re-run (FR-2)." "Java 21, Spring @Service, @Transactional" "Component"

modregningsRaekkefoeigenEngine = component "ModregningsRaekkefoeigenEngine" "Executes the GIL § 7, stk. 1 three-tier allocation algorithm. Delegates tier-2 partial allocation to DaekningsRaekkefoeigenServiceClient (P057) at most once per invocation. Queries fordringer via FordringQueryPort." "Java 21, Spring @Service" "Component"

korrektionspuljeService = component "KorrektionspuljeService" "Processes OffsettingReversalEvent: Step 1 same-fordring residual, Step 2 gendaenkning via P057, Step 3 KorrektionspuljeEntry creation. Settles pool entries by re-invoking ModregningService." "Java 21, Spring @Service, @Transactional" "Component"

renteGodtgoerelseService = component "RenteGodtgoerelseService" "Computes rentegodtgoerelse start date (5-banking-day exception, kildeskattelov § 62/62A exception) and effective rate from rentegodt_rate_entry table (GIL § 8b)." "Java 21, Spring @Service" "Component"

korrektionspuljeSettlementJob = component "KorrektionspuljeSettlementJob" "Scheduled monthly (3 AM, 1st of month) and annual (4 AM, 2 Jan) jobs. Queries unsettled PSRM-target pool entries and invokes KorrektionspuljeService.settleEntry() per entry. Each settlement is its own transaction." "Java 21, Spring @Scheduled" "Component"

daekningsRaekkefoeigenServiceClient = component "DaekningsRaekkefoeigenServiceClient" "HTTP client for P057 DaekningsRaekkefoeigenService in opendebt-payment-service. Invoked at most once per tier ordering run for tier-2 partial allocation and gendaenkning." "Java 21, Spring RestClient, HTTP/REST" "Component"

modregningController = component "ModregningController" "REST controller. Exposes POST tier2-waiver (scope modregning:waiver, FR-2) and GET modregning-events read model (scope modregning:read, FR-5). Enforces OAuth2 scopes via @PreAuthorize." "Java 21, Spring @RestController" "Component"

fordringQueryPort = component "FordringQueryPort" "Internal adapter for TB-040 active-fordringer queries within opendebt-debt-service (same-service, no inter-service HTTP). Exposes typed Java API getActiveFordringer(debtorPersonId, tier, payingAuthorityOrgId) backed by JPA repository. Consumed exclusively by ModregningsRaekkefoeigenEngine." "Java 21, Spring @Component, JPA" "Component"

ledgerServiceClient = component "LedgerServiceClient" "HTTP client for payment-service bookkeeping API (ADR-0018). Posts double-entry debit/credit ledger entries for each tier allocation. Each entry references ModregningEvent.id and the GIL § 7 tier applied. Called from ModregningService within the @Transactional boundary; failure propagates and triggers rollback." "Java 21, Spring RestClient, HTTP/REST" "Component"

// ── SECTION B: Component-level Relationships ─────────────────────────────
// Add to model { ... } block, within openDebt softwareSystem scope

// Event consumers → services
publicDisbursementEventConsumer -> modregningService "Delegates PublicDisbursementEvent to" "Java method call"
offsettingReversalEventConsumer -> korrektionspuljeService "Delegates OffsettingReversalEvent to" "Java method call"

// ModregningService orchestration
modregningService -> modregningsRaekkefoeigenEngine "Delegates three-tier allocation to" "Java method call"
modregningService -> renteGodtgoerelseService "Computes rentegodtgoerelse start date via" "Java method call"
modregningService -> ledgerServiceClient "Posts SET_OFF ledger entries (ADR-0018)" "HTTPS/REST"
modregningService -> immudb "Appends ModregningEvent and SET_OFF CollectionMeasure records (post-commit, ADR-0029)" "gRPC"

// ModregningsRaekkefoeigenEngine — fordring query (same-service port, no self-ref) and P057 delegation
modregningsRaekkefoeigenEngine -> fordringQueryPort "Queries active fordringer by tier (TB-040)" "Java method call"
modregningsRaekkefoeigenEngine -> daekningsRaekkefoeigenServiceClient "Delegates tier-2 partial allocation (at most once per run)" "Java method call"

// HTTP clients → payment-service
daekningsRaekkefoeigenServiceClient -> paymentService "Calls DaekningsRaekkefoeigenService for GIL § 4 allocation (ADR-0007)" "HTTPS/REST"
ledgerServiceClient -> paymentService "Posts double-entry ledger entries" "HTTPS/REST"

// KorrektionspuljeService
korrektionspuljeService -> daekningsRaekkefoeigenServiceClient "Delegates gendaenkning Step 2 allocation to" "Java method call"
korrektionspuljeService -> modregningService "Re-enters initiateModregning for pool settlement" "Java method call"
korrektionspuljeService -> renteGodtgoerelseService "Computes accrued rentegodtgoerelse rate at settlement" "Java method call"

// KorrektionspuljeSettlementJob
korrektionspuljeSettlementJob -> korrektionspuljeService "Invokes settleEntry() per unsettled PSRM pool entry" "Java method call"

// ModregningController
modregningController -> modregningService "Invokes applyTier2Waiver() for FR-2 waiver" "Java method call"
caseworkerPortal -> modregningController "Reads modregning-events and submits tier-2 waiver via" "HTTPS/REST"

// ── SECTION C: Component View ─────────────────────────────────────────────
// Add to views { ... } block

component debtService "DebtService_P058_Components" "P058 component view — Modregning og Korrektionspulje. Shows all new components within opendebt-debt-service and their relationships to P057, immudb, and BusinessConfigService." {
    include publicDisbursementEventConsumer
    include offsettingReversalEventConsumer
    include modregningService
    include modregningsRaekkefoeigenEngine
    include korrektionspuljeService
    include renteGodtgoerelseService
    include korrektionspuljeSettlementJob
    include daekningsRaekkefoeigenServiceClient
    include modregningController
    include fordringQueryPort
    include ledgerServiceClient
    include paymentService
    include immudb
    include caseworkerPortal
    autoLayout lr
}
```

---

## Appendix A: Error Catalogue

| Exception | HTTP | When |
|-----------|------|------|
| `DuplicateNemkontoReferenceException` | 409 | Concurrent duplicate `nemkontoReferenceId` insert |
| `EligiblePaymentTypeException` | 422 | `paymentType` not in configured eligible-payment-types |
| `ModregningEventNotFoundException` | 404 | Event id not found for given debtor |
| `WaiverAlreadyAppliedException` | 409 | `tier2WaiverApplied` already true |
| `OriginEventNotFoundException` | 404 | `originModregningEventId` not found in `OffsettingReversalEventConsumer` |
| `InvalidReversalAmountException` | 422 | `surplusAmount ≤ 0` in reversal event |
| `NoRenteGodtgoerelseRateException` | 500 | No `RenteGodtgoerelseRateEntry` covers the reference date |
| `InsufficientFundsException` | 422 | P057 returns allocations summing to less than input amount (P057 contract violation) |

---

## Appendix B: OAuth2 Scope Matrix

| Scope | Operation | Enforcement |
|-------|-----------|-------------|
| `modregning:read` | `GET /api/v1/debtors/{debtorId}/modregning-events` | `@PreAuthorize("hasAuthority('SCOPE_modregning:read') or hasAuthority('SCOPE_modregning:waiver')")` |
| `modregning:waiver` | `POST /api/v1/debtors/{debtorId}/modregning-events/{eventId}/tier2-waiver` | `@PreAuthorize("hasAuthority('SCOPE_modregning:waiver')")` |

Callers without the required scope receive HTTP 403 with no body details (AC-7). No audit log entry is created for rejected unauthorised requests.

---

## Appendix C: GIL § Reference Quick Map

| GIL Reference | P058 Implementation |
|---------------|---------------------|
| GIL § 7, stk. 1, nr. 1 | Tier-1 allocation in `ModregningsRaekkefoeigenEngine` |
| GIL § 7, stk. 1, nr. 2 | Tier-2 allocation, with P057 delegation for partial coverage |
| GIL § 7, stk. 1, nr. 3 | Tier-3 allocation in ascending `registreringsdato` order |
| GIL § 4, stk. 5 | Gendækning in `KorrektionspuljeService.processReversal()` Step 2 |
| GIL § 4, stk. 6 | Gendækning opt-out rules in `isGendaekningOptOut()` |
| GIL § 4, stk. 7 | `KorrektionspuljeEntry` creation; 50 DKK threshold; `boerneYdelseRestriction` flag |
| GIL § 4, stk. 7, nr. 2 | Rentegodtgørelse on pool balance — computed by `RenteGodtgoerelseService` |
| GIL § 4, stk. 7, nr. 3 | `boerneYdelseRestriction = true` preserved through settlement |
| GIL § 4, stk. 7, nr. 4 | Transporter/udlæg do NOT carry over to settled amount (except pre-2021) |
| GIL § 4, stk. 9 | `correctionPoolTarget = DMI` flag — no DMI settlement implemented |
| GIL § 4, stk. 11 | Tier-2 waiver in `ModregningService.applyTier2Waiver()` |
| GIL § 8b | `RenteGodtgoerelseService`; `renteGodtgoerelseNonTaxable = true` always |
| GIL § 9a | Digital Post notice via transactional outbox |
| GIL § 17, stk. 1 | `klageFristDato` computation: +3 months (notice delivered) / +1 year (notice failed) |
| Gæld.bekendtg. § 7, stk. 4 | Step 1 same-fordring residual coverage in `KorrektionspuljeService` |
| Kildeskattelov § 62/62A | `OVERSKYDENDE_SKAT` start-date exception in `RenteGodtgoerelseService` |
