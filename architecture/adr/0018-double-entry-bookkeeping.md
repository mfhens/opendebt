# ADR 0018: Double-Entry Bookkeeping for Financial Accounting

## Status
Accepted — amended (see amendment below)

## Context
Debt collection involves financial transactions that must be accounted for correctly:

- Registering debts (fordringer) as receivables
- Recording payments received via CREMUL from Statens Koncernbetalinger
- Recording refunds/outgoing payments via DEBMUL
- Offsetting (modregning) transactions
- Wage garnishment (loenindeholdelse) deductions
- Interest accrual and write-offs
- Reconciliation with SKB bank statements

Danish public sector accounting (statsligt regnskab) requires double-entry bookkeeping (dobbelt bogholderi) with full audit trail. All financial movements must be traceable and reconcilable.

Requirements:
- Double-entry principle: every transaction debits one account and credits another
- Full audit trail of all postings (posteringer)
- Support for chart of accounts (kontoplan) aligned with Statens Kontoplan
- Multi-currency not required (DKK only)
- PostgreSQL as database backend
- Spring Boot integration
- Reconciliation support (afstemning) against SKB bank statements

## Amendment (2026-04-05, #3): Every financial transaction posts to the ledger

**Invariant:** Any behaviour that records a **financial effect** — including changes to amounts owed, received, cleared, accrued, written off, refunded, garnished, offset, or retroactively corrected — **must** result in a balanced **double-entry posting** to the canonical ledger in `opendebt-payment-service` (`BookkeepingService` / `ledger_entries` / `debt_events` timeline), unless this ADR is explicitly amended with a documented exception.

**Rationale:** Statsligt regnskab and audit require one reconcilable hovedbog. Service-local tables (for example `interest_journal_entries` in debt-service) may support calculation, idempotency, or display, but they **do not** replace ledger postings where this ADR applies.

**Cross-service flows:** Other services must reach the ledger via the supported API (HTTP internal routes, future `ufst-bookkeeping-core` ports, or approved events consumed by payment-service). Debt-service set-off currently invokes `LedgerServiceClient` toward payment-service; **TB-055** tracks replacing the stub with real HTTP.

**Process:** Pull requests that touch money-moving code paths should answer: *Where is the ledger posting (or the ADR exception)?* New petitions or major features should call out ledger impact in the solution architecture.

---

## Amendment (2026-04-01, #2): Implementation is seed for shared UFST library

**Decision:** The custom implementation in `opendebt-payment-service` will be extracted into a shared `dk.ufst:ufst-bookkeeping-core` library (see ADR-0033). OpenDebt remains the initial home of the module. The architecture and interface contracts described below are the basis for that library.

---

## Amendment (2026-04-01, #1): Library replaced by custom implementation

**Finding:** `com.yanimetaxas:bookkeeping` is only available at version `0.1.0` (the `4.3.0` version referenced here does not exist on Maven Central). The library has been unmaintained for ~9 years with no active community.

**Revised decision:** The bookkeeping module is implemented as a **bespoke in-house component** within `opendebt-payment-service` under the package `dk.ufst.opendebt.payment.bookkeeping`. No third-party bookkeeping library is used. Double-entry invariants (debit/credit balance enforcement, immutability, bi-temporal model, storno pattern) are enforced directly in `BookkeepingServiceImpl`. The `com.yanimetaxas:bookkeeping` dependency has been removed from the root `pom.xml`.

All architectural decisions below (account structure, bi-temporal model, storno pattern, interest calculation) remain in effect — only the implementation vehicle changed.

---

## Decision
We implement a **bespoke double-entry bookkeeping module** within `opendebt-payment-service`, under `dk.ufst.opendebt.payment.bookkeeping`.

### Architecture
```
┌──────────────────────────────────────────────────────────┐
│                   PAYMENT SERVICE                         │
│                     (Port 8085)                           │
├──────────────────────────────────────────────────────────┤
│                                                          │
│  ┌────────────────────────────────────────────────────┐  │
│  │          Bookkeeping Module                        │  │
│  │    (double-entry-bookkeeping-api)                  │  │
│  │                                                    │  │
│  │  ┌──────────────┐  ┌───────────────────────────┐  │  │
│  │  │ Chart of     │  │ Ledger                    │  │  │
│  │  │ Accounts     │  │ (Hovedbog)                │  │  │
│  │  │ (Kontoplan)  │  │                           │  │  │
│  │  │              │  │  Debit    │  Credit        │  │  │
│  │  │ - Receivables│  │  ─────────┼──────────      │  │  │
│  │  │ - Bank (SKB) │  │  Fordring │  Skyldner      │  │  │
│  │  │ - Revenue    │  │  Bank     │  Fordring      │  │  │
│  │  │ - Write-offs │  │  Tab      │  Fordring      │  │  │
│  │  └──────────────┘  └───────────────────────────┘  │  │
│  └────────────────────────────────────────────────────┘  │
│                          │                                │
│              PostgreSQL (posteringer)                      │
└──────────────────────────────────────────────────────────┘
         │                    │                    │
    debt-service     integration-gateway    case-service
```

### Account Structure (Kontoplan)

| Account Code | Account Name | Type | Usage |
|-------------|--------------|------|-------|
| 1000 | Fordringer (Receivables) | Asset | Outstanding debts owed to the state |
| 1100 | Renter tilgodehavende | Asset | Accrued interest receivable |
| 2000 | SKB Bankkonto | Asset | Statens Koncernbetalinger bank |
| 3000 | Indrivelsesindtaegter | Revenue | Collection revenue |
| 3100 | Renteindtaegter | Revenue | Interest revenue |
| 4000 | Tab paa fordringer | Expense | Written-off debts |
| 5000 | Modregning (clearing) | Liability | Offsetting clearing account |

### Transaction Examples

| Event | Debit | Credit | Amount |
|-------|-------|--------|--------|
| Debt registered | 1000 Fordringer | 3000 Indrivelsesindtaegter | Principal |
| Payment received (CREMUL) | 2000 SKB Bank | 1000 Fordringer | Payment |
| Interest accrued | 1100 Renter | 3100 Renteindtaegter | Interest |
| Offsetting executed | 5000 Modregning | 1000 Fordringer | Offset amount |
| Debt written off | 4000 Tab | 1000 Fordringer | Remaining |
| Refund issued (DEBMUL) | 1000 Fordringer | 2000 SKB Bank | Refund |

### Bi-Temporal Model

Every ledger entry carries two dates:
- **`effective_date`** (vaerdidag): when the economic event applies
- **`posting_date`** (bogfoeringsdag): when it was recorded in the system

This enables retroactive corrections: a correction posted today can have an effective date months in the past, and the system can reconstruct what the correct state should have been at any point.

### Storno Pattern for Corrections

Ledger entries are immutable -- they are never modified or deleted. Corrections use the **storno** (reversal) pattern:

1. Post reversal entries that cancel the original (DEBIT becomes CREDIT and vice versa)
2. Reversal entries reference the original via `reversal_of_transaction_id`
3. Post new correct entries with the correct `effective_date`

```
Original (3 months ago):
  DEBIT  1100 Renter tilgodehavende  500 kr  effective=2025-12-01
  CREDIT 3100 Renteindtaegter        500 kr  effective=2025-12-01

Storno (today, reversing the above):
  CREDIT 1100 Renter tilgodehavende  500 kr  effective=2025-12-01  posting=2026-03-05
  DEBIT  3100 Renteindtaegter        500 kr  effective=2025-12-01  posting=2026-03-05

Corrected (today, with recalculated amount):
  DEBIT  1100 Renter tilgodehavende  300 kr  effective=2025-12-01  posting=2026-03-05
  CREDIT 3100 Renteindtaegter        300 kr  effective=2025-12-01  posting=2026-03-05
```

### Debt Event Timeline

An immutable event log (`debt_events`) records all facts about a debt's lifecycle:
- `DEBT_REGISTERED`, `PAYMENT_RECEIVED`, `INTEREST_ACCRUED`
- `UDLAEG_REGISTERED`, `UDLAEG_CORRECTED`
- `OFFSETTING_EXECUTED`, `WRITE_OFF`, `REFUND`, `CORRECTION`

When a retroactive correction arrives, the `RetroactiveCorrectionService`:
1. Records the correction event in the timeline
2. Posts the principal correction to the ledger
3. Finds and stornos all interest accruals after the effective date
4. Replays the event timeline to build a balance-over-time curve
5. Recalculates interest per period with correct principal balances
6. Posts new interest accrual entries

### Period-Based Interest Calculation

Interest is calculated per period, not as a single shot. Each period has a stable principal balance:

```
Period 1: 2025-10-01 to 2025-12-01  principal=50.000 kr  → interest=821,92 kr
Period 2: 2025-12-01 to 2026-01-15  principal=30.000 kr  → interest=369,86 kr  (after udlaeg correction)
Period 3: 2026-01-15 to 2026-03-05  principal=25.000 kr  → interest=336,99 kr  (after partial payment)
```

### Retroactive Correction Example (Udlaeg)

```
Scenario: Court reduces udlaeg from 50.000 to 30.000 kr, effective 3 months ago.

RetroactiveCorrectionService.applyRetroactiveCorrection(
    debtId, LocalDate.of(2025, 12, 1),
    originalAmount=50000, correctedAmount=30000,
    annualRate=0.10, reference="RET-2026-001",
    reason="Fogedrettens afgoerelse - udlaeg nedsat"
);

Result:
  - principalDelta: -20.000 kr
  - stornoEntriesPosted: 6 (3 months of interest reversed)
  - newInterestEntriesPosted: 6 (3 months recalculated)
  - oldInterestTotal: 1.232,88 kr
  - newInterestTotal: 739,73 kr
  - interestDelta: -493,15 kr
```

### Integration Pattern
```java
@Service
public class BookkeepingService {

    // Record with bi-temporal dates
    public void recordPaymentReceived(
            UUID debtId, BigDecimal amount,
            LocalDate effectiveDate, String cremulRef) {
        // Posts double-entry: DEBIT SKB Bank, CREDIT Fordringer
        // Records event in debt timeline
        // Both with effectiveDate (value date) and postingDate (today)
    }
}

@Service
public class RetroactiveCorrectionService {

    // Full retroactive correction with interest recalculation
    public CorrectionResult applyRetroactiveCorrection(
            UUID debtId, LocalDate effectiveDate,
            BigDecimal originalAmount, BigDecimal correctedAmount,
            BigDecimal annualInterestRate,
            String reference, String reason) {
        // 1. Record correction event
        // 2. Post principal correction (storno + new)
        // 3. Storno affected interest accruals
        // 4. Recalculate interest per period
        // 5. Post new interest entries
    }
}
```

## Consequences

### Positive
- **Spring + PostgreSQL**: Custom implementation uses the existing stack natively
- **No external dependency**: Eliminates the stale/phantom `yanimetaxas` library risk
- **Double-entry guarantee**: `BookkeepingServiceImpl` enforces debit/credit balance at service level
- **Audit-ready**: All postings are immutable and timestamped
- **Lightweight**: Focused library, not an entire ERP system
- **Bi-temporal**: Supports retroactive corrections without breaking immutability
- **Event-sourced timeline**: Full history enables replay and recalculation
- **Storno pattern**: Corrections are fully traceable and auditable

### Negative
- **No off-the-shelf audit**: Must ensure double-entry invariants are covered by unit tests
- **Maintenance burden**: Balance enforcement logic is owned by the team

### Mitigations
- Comprehensive unit tests for storno and interest recalculation scenarios (`BookkeepingServiceImplTest`, `RetroactiveCorrectionServiceImplTest`)
- If the custom module proves insufficient, the `BookkeepingService` interface makes it replaceable without changing the rest of the system

## Alternatives Considered

| Option | Reason Not Chosen |
|--------|-------------------|
| ADORSYS ledgers | More enterprise-grade but heavier; good fallback option if bookkeeping-api proves insufficient |
| fintx-accounting | Apache 2.0, enterprise-level, but less Spring Boot integration out of the box |
| Custom implementation | Double-entry bookkeeping has subtle invariants (balance enforcement, immutability) that are easy to get wrong |
| External accounting system (e.g., Navision) | Against self-contained microservice principle; adds external dependency and vendor lock-in |
| Apache BookKeeper | Distributed log system, not a financial accounting library despite the name |
