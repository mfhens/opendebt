# ADR 0018: Double-Entry Bookkeeping for Financial Accounting

## Status
Accepted

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

## Decision
We adopt **double-entry-bookkeeping-api** by imetaxas as the foundation for the bookkeeping module, embedded in the payment-service.

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

### Dependencies
```xml
<dependency>
    <groupId>com.yanimetaxas</groupId>
    <artifactId>bookkeeping</artifactId>
    <version>4.3.0</version>
</dependency>
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
- **Spring + PostgreSQL**: Matches OpenDebt's existing stack exactly
- **Maven Central**: Easy dependency management, no custom repository needed
- **MIT license**: Permissive, no restrictions for public sector use
- **Double-entry guarantee**: Library enforces debit/credit balance at API level
- **Audit-ready**: All postings are immutable and timestamped
- **Lightweight**: Focused library, not an entire ERP system
- **Bi-temporal**: Supports retroactive corrections without breaking immutability
- **Event-sourced timeline**: Full history enables replay and recalculation
- **Storno pattern**: Corrections are fully traceable and auditable

### Negative
- **Limited community**: Smaller project compared to enterprise accounting platforms
- **Customization needed**: Must extend for Statens Kontoplan structure and SKB reconciliation
- **No built-in reporting**: Must build financial reports (balance, trial balance) ourselves
- **No Danish localization**: Account names and reporting labels must be configured
- **Recalculation complexity**: Period-based interest replay requires careful testing

### Mitigations
- Wrap the library in an OpenDebt-specific `BookkeepingService` for domain-specific operations
- Build reconciliation service that matches CREMUL/DEBMUL entries against ledger postings
- Create reporting endpoints for balance and trial balance aligned with statsligt regnskab
- Comprehensive unit tests for storno and interest recalculation scenarios
- If the library proves too limited, the double-entry abstraction layer makes it replaceable without changing the rest of the system

## Alternatives Considered

| Option | Reason Not Chosen |
|--------|-------------------|
| ADORSYS ledgers | More enterprise-grade but heavier; good fallback option if bookkeeping-api proves insufficient |
| fintx-accounting | Apache 2.0, enterprise-level, but less Spring Boot integration out of the box |
| Custom implementation | Double-entry bookkeeping has subtle invariants (balance enforcement, immutability) that are easy to get wrong |
| External accounting system (e.g., Navision) | Against self-contained microservice principle; adds external dependency and vendor lock-in |
| Apache BookKeeper | Distributed log system, not a financial accounting library despite the name |
