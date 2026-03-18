# Petition 039 Outcome Contract

## Acceptance criteria

1. OpenDebt detects when a financial event's effective date precedes previously posted events on the same debt.
2. OpenDebt replays the full financial timeline from the crossing point, applying dækningsrækkefølge (interest before principal) at every step.
3. All affected ledger entries from the crossing point forward are stornoed and replaced with recalculated entries.
4. Each dækningsophævelse (coverage reversal) is recorded as an auditable event with original dækning reference, reversal reason, and replacement dækning.
5. Cascading corrections are resolved: when a recalculation changes a downstream transaction's basis, the downstream transaction is either auto-corrected in the same replay or flagged for manual caseworker review.
6. Events sharing the same effective date are ordered deterministically (registration before payment before interest, configurable via rules).
7. An Allokeringsunderretning is generated showing original allocation, reversal, new allocation, net effect on rente and hovedstol, and the crossing transaction reference.
8. Multi-claim crossing is handled: when a debtor's payment allocation across claims changes due to recalculated interest, all affected claims are replayed consistently.
9. The replay algorithm is idempotent: replaying the same timeline produces identical ledger state.
10. The replay window is bounded and configurable (default: 12 months lookback).

## Definition of done

- Crossing transaction detection is testable with known crossing scenarios.
- Timeline replay produces correct interest and dækning allocations for at least these scenarios:
  - Payment with vaerdidag before last interest accrual.
  - Retroactive opskrivning that increases principal in a past period.
  - Retroactive nedskrivning that decreases principal in a past period.
  - Two transactions crossing each other (payment and modregning with overlapping effective dates).
  - Same-day events ordered correctly.
- Dækningsophævelse events are auditable in the ledger.
- Allokeringsunderretning content includes reversal details.
- Multi-claim crossing scenario covered by at least one test.
- Idempotency verified: replay called twice on same input produces identical output.
- Performance: replay of 12-month timeline with 50+ events completes within 5 seconds.
- Every acceptance criterion is covered by at least one Gherkin scenario or unit test.

## Failure conditions

- A payment with a past vaerdidag does not trigger recalculation of interest for the affected period.
- Dækningsrækkefølge is not applied: principal is reduced before interest in any scenario.
- Previously posted dækninger remain in the ledger when the recalculation shows they should be different.
- No audit trail exists for dækningsophævelser.
- Cascading corrections are silently ignored, leaving inconsistent saldi.
- Same-day events produce non-deterministic results.
- Allokeringsunderretning does not mention reversals caused by crossing transactions.
- Replay produces different results when called twice on identical input.
