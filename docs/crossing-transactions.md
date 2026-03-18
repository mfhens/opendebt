# Crossing Transactions (Krydsende Handlinger)

## What is a crossing transaction?

A crossing transaction occurs when a financial event arrives with an effective date (vaerdidag) that precedes events already posted to the ledger. This is routine in Danish public debt collection because CREMUL payment files from SKB carry value dates that can lag posting dates by days, corrections from fogedretten arrive weeks after the original action, and opskrivning/nedskrivning from creditors can be backdated.

When transactions cross, previously calculated interest amounts become wrong, and previously applied payment allocations between interest and principal (dækningsrækkefølge) become wrong. Fixing this requires a full timeline replay from the crossing point.

## Terminology

| Danish | English (code) | Definition |
|--------|---------------|------------|
| Krydsende handlinger | Crossing Transactions | Financial events whose effective dates overlap or precede posting dates |
| Dækningsrækkefølge | Coverage Priority Order | Interest is always covered before principal (GIL) |
| Dækningsophævelse | Coverage Reversal | Reversal of a previously applied recovery allocation |
| Allokeringsunderretning | Allocation Notification | Creditor notification showing interest/principal split incl. reversals |
| Vaerdidag | Effective Date | When the economic event applies |
| Bogføringsdag | Posting Date | When it was recorded in the system |

## Example scenario

```
Timeline (as events arrive):

Day 1:  Debt registered, principal 50.000 kr.
Day 10: Interest accrued for Day 1-10 based on 50.000 kr → 78,77 kr posted.
Day 12: CREMUL payment of 10.000 kr arrives, but value date = Day 5.
        This CROSSES the interest accrual of Day 10.

Without crossing support:
  - Payment of 10.000 kr reduces principal to 40.000 kr as of Day 12.
  - Interest from Day 1-10 was calculated on 50.000 kr. Wrong.

With crossing support (timeline replay):
  1. Storno the Day 1-10 interest accrual (78,77 kr reversed).
  2. Recalculate: Day 1-5 interest on 50.000 kr → 39,38 kr.
  3. Apply payment as of Day 5:
     - Dækningsrækkefølge: 39,38 kr to interest, 9.960,62 kr to principal.
     - Principal reduced to 40.039,38 kr as of Day 5.
  4. Day 5-today interest on 40.039,38 kr → recalculated.
  5. Allokeringsunderretning sent to creditor with the reversal details.
```

## Architecture

All crossing transaction logic lives in `opendebt-payment-service` within the `bookkeeping` package, extending the existing double-entry bookkeeping infrastructure (ADR-0018).

```
dk.ufst.opendebt.payment.bookkeeping/
├── service/
│   ├── CrossingTransactionDetector.java     ← FR-1: detects crossings
│   ├── CoveragePriorityService.java         ← FR-2: dækningsrækkefølge
│   ├── TimelineReplayService.java           ← FR-2/3/4: full replay engine
│   ├── AllocationNotificationService.java   ← FR-6: notification generation
│   ├── EventOrderComparator.java            ← FR-5: same-date ordering
│   └── impl/
│       ├── CrossingTransactionDetectorImpl.java
│       ├── CoveragePriorityServiceImpl.java
│       ├── TimelineReplayServiceImpl.java
│       └── AllocationNotificationServiceImpl.java
├── model/
│   ├── CrossingDetectionResult.java
│   ├── CoverageAllocation.java
│   ├── CoverageReversal.java
│   ├── TimelineReplayResult.java
│   └── AllocationNotification.java
└── (existing) entity/, repository/, AccountCode.java, BookkeepingService.java
```

## How it works

### 1. Crossing detection (CrossingTransactionDetector)

When a new financial event arrives, the detector checks whether its effective date precedes any existing event on the same debt. The lookback window is bounded (default: 12 months, configurable via `opendebt.bookkeeping.crossing.max-lookback-months`).

```java
CrossingDetectionResult result = detector.detectCrossing(debtId, paymentValueDate);
if (result.isCrossingDetected()) {
    // Trigger timeline replay from result.getCrossingPoint()
}
```

### 2. Coverage priority (CoveragePriorityService)

Implements dækningsrækkefølge per GIL: when a payment (dækning) is applied, accrued inddrivelsesrente is always covered first, then the remainder goes to hovedstol.

```java
CoverageAllocation allocation = coveragePriorityService.allocatePayment(
    debtId, paymentAmount, accruedInterest, principalBalance);
// allocation.getInterestPortion() → amount applied to rente
// allocation.getPrincipalPortion() → amount applied to hovedstol
```

### 3. Timeline replay (TimelineReplayService)

The core engine. When a crossing is detected:

1. **Storno** all ledger entries from the crossing point forward.
2. **Load** all debt events and sort them deterministically (EventOrderComparator).
3. **Walk** the timeline from the crossing point:
   - Between each pair of events, calculate interest on the current principal.
   - At each recovery event (payment, offsetting), apply dækningsrækkefølge.
   - Compare new allocation against original allocation; if different, record a dækningsophævelse.
4. **Post** recalculated interest and recovery entries to the ledger.

```java
TimelineReplayResult result = replayService.replayTimeline(
    debtId, crossingPoint, annualRate, reference);
// result.getStornoEntriesPosted()          → entries reversed
// result.getRecalculatedInterestPeriods()  → new interest periods
// result.getCoverageReversals()            → dækningsophævelser
// result.getFinalPrincipalBalance()        → correct principal
// result.getFinalInterestBalance()         → correct accrued interest
```

The replay is **idempotent**: running it twice on the same input produces identical results.

### 4. Same-date ordering (EventOrderComparator)

Events sharing the same effective date are ordered deterministically:

| Priority | Event Type | Rationale |
|----------|-----------|-----------|
| 0 | DEBT_REGISTERED | Must establish principal before anything else |
| 1 | UDLAEG_REGISTERED | Increases principal |
| 2 | PAYMENT_RECEIVED | Balance-reducing |
| 3 | OFFSETTING_EXECUTED | Balance-reducing |
| 4 | WRITE_OFF | Balance-reducing |
| 5 | REFUND | Balance-increasing |
| 6 | UDLAEG_CORRECTED | Correction |
| 7 | CORRECTION | Correction |
| 8 | COVERAGE_REVERSED | Reversal artifact |
| 9 | INTEREST_ACCRUED | Calculated last, after balance is final |

Within the same priority, events are ordered by `created_at` (posting time).

### 5. Allocation notification (AllocationNotificationService)

Generates an Allokeringsunderretning from a replay result. The notification contains:

- **Allocation lines**: each dækning split into rente and hovedstol portions.
- **Reversal lines**: each dækningsophævelse showing original vs corrected allocation.
- **Crossing flag**: `hasCrossingReversals` indicates whether reversals were caused by crossing transactions.

This content is ready for dispatch via letter-service (Digital Post) or display in the Fordringshaverportal.

## Configuration

```yaml
opendebt:
  bookkeeping:
    crossing:
      max-lookback-months: 12    # Maximum replay window
```

## Relationship to existing components

| Existing Component | How Crossing Transactions Extend It |
|---|---|
| `BookkeepingServiceImpl` (ADR-0018) | Crossing logic stornos and reposts entries created by BookkeepingService |
| `RetroactiveCorrectionServiceImpl` | Handles single-event corrections; TimelineReplayService handles multi-event cascades |
| `InterestAccrualServiceImpl` | Replay engine performs its own period-based calculation inline during timeline walk |
| `DebtEventEntity` timeline | New event type `COVERAGE_REVERSED` added |
| `LedgerEntryEntity` categories | New category `COVERAGE_REVERSAL` added |
| `collection-priority.drl` | Used for multi-claim priority ordering (FR-7, future) |

## Legal basis

- **Dækningsrækkefølge**: Gældsinddrivelsesloven (GIL). Inddrivelsesrente covered before hovedstol.
- **Inddrivelsesrente**: 5,75% per 1 January 2026. Simple dag-til-dag rente. No compound interest.
- **Allokeringsunderretning**: PSRM requirement. "Inkluderer dækningsophævelser ved krydsende finansielle transaktioner" (begrebsmodel v3, section 4.29).
- **Immutable audit trail**: Statsligt regnskab requires all corrections to be traceable via storno pattern.

## Petition and test coverage

- **Petition**: `petitions/petition039-krydsende-handlinger-crossing-transactions.md`
- **Outcome contract**: `petitions/petition039-krydsende-handlinger-crossing-transactions-outcome-contract.md`
- **Unit tests**: `EventOrderComparatorTest`, `CoveragePriorityServiceImplTest`, `CrossingTransactionDetectorImplTest`, `TimelineReplayServiceImplTest`, `AllocationNotificationServiceImplTest`
