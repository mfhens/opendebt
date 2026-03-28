# Petition 039: Krydsende handlinger — crossing financial transactions with cascading interest recalculation

## Summary

OpenDebt shall correctly handle krydsende handlinger (crossing financial transactions): situations where payments, dækninger, modregninger, opskrivninger, nedskrivninger, or other financial events have effective dates that overlap or precede previously posted transactions, requiring cascading recalculation of interest, dækningsrækkefølge (coverage priority), and dækningsophævelser (coverage reversals). The system shall produce correct saldi at any point in time and generate Allokeringsunderretninger that show the full breakdown including reversals caused by crossing transactions.

## Context and motivation

The PSRM domain explicitly acknowledges that financial transactions cross each other date-wise. The begrebsmodel v3 defines `Allokeringsunderretning` as including "dækningsophævelser ved krydsende finansielle transaktioner." The current interest rules (inddrivelsesrente 5.75%, simpel dag-til-dag rente, rente dækkes forud for hovedstol) mean that any change to the principal timeline cascades into interest recalculation, which cascades into dækning reallocation, which may cascade further.

**Example scenario:**

1. March 1: Debt registered, hovedstol 50.000 kr.
2. March 10: Interest accrued for March 1–10 based on 50.000 kr principal.
3. March 12: A CREMUL payment of 10.000 kr arrives with value date (vaerdidag) March 5. This *crosses* the interest accrual of March 10.
4. Correct handling requires:
   - Storno the March 1–10 interest accrual.
   - Recalculate: March 1–5 interest on 50.000 kr; apply 10.000 kr dækning as of March 5 (rente first, then hovedstol); March 5–10 interest on reduced principal.
   - If a modregning was also executed on March 8 based on the old balance, that modregning amount may now be incorrect and needs re-evaluation.

The current `RetroactiveCorrectionServiceImpl` handles a single correction event (e.g., udlaeg reduced) but does not:
- Detect that other previously posted transactions are affected by the correction.
- Re-apply dækningsrækkefølge (interest before principal) across the full recalculated timeline.
- Reverse and re-apply dækninger (dækningsophævelse) when the interest/principal split changes.
- Handle cascading corrections where fixing one crossing transaction reveals another.
- Generate the Allokeringsunderretning with the reversal details.

Without this capability, OpenDebt will produce incorrect saldi, incorrect interest amounts, and incorrect creditor notifications whenever transactions arrive out of chronological order — which is routine in Danish public debt collection (CREMUL files from SKB have value dates that can lag posting dates by days).

## Functional requirements

### FR-1: Crossing transaction detection

OpenDebt shall detect when a newly arrived financial event has an effective date that precedes any previously posted event on the same debt. This includes but is not limited to:
- Payments (CREMUL) with value date before the last interest accrual.
- Retroactive corrections (opskrivning, nedskrivning) with effective date before subsequent dækninger.
- Modregning with effective date before a payment already posted.

### FR-2: Full timeline replay with dækningsrækkefølge

When a crossing transaction is detected, OpenDebt shall replay the full financial timeline for the affected debt from the earliest crossing point:

1. Collect all events (registrations, payments, corrections, interest accruals, offsetting) ordered by effective date, with a deterministic tiebreaker for same-date events.
2. At each event point, apply the dækningsrækkefølge rule: inddrivelsesrente is dækket forud for hovedstol.
3. Recalculate interest for each period between balance-changing events using the corrected principal.
4. The result is a complete, consistent set of ledger postings from the crossing point to the present.

> **Alternative (G.A.1.4.4 / GIL § 18 l):** Per G.A.1.4.4 and GIL § 18 l, restanceinddrivelsesmyndighed may in certain cases choose to set the nedskrivning's effective date to the date of receipt (prospective, not retroactive) and instead pay the debtor a **rentegodtgørelse** (interest compensation) rather than performing a full retroactive timeline replay. This petition assumes full retroactive timeline replay as the primary implementation path; the rentegodtgørelse alternative is operationally valid under Danish law and may be preferred when retroactive replay is computationally expensive or would produce anomalous results. Future iterations may expose this as a caseworker-selectable option.

### FR-3: Dækningsophævelse and re-application

When the recalculated timeline differs from the previously posted timeline:

1. OpenDebt shall storno (reverse) all affected ledger entries from the crossing point forward.
2. OpenDebt shall post the recalculated entries.
3. OpenDebt shall track each dækningsophævelse (coverage reversal) as an auditable event, recording the original dækning, the reason for reversal (crossing transaction reference), and the replacement dækning.

### FR-4: Cascading correction resolution

OpenDebt shall handle cascading corrections: when replaying the timeline, if a recalculated amount affects another transaction that was previously posted (e.g., a modregning was based on a balance that has now changed), the system shall:

1. Flag the affected downstream transaction for review or automatic correction.
2. For automatic correction: include the downstream transaction in the same replay, producing a single consistent result.
3. For manual review: create a caseworker task (Flowable user task) with details of the discrepancy.

### FR-5: Same-effective-date ordering

OpenDebt shall define and enforce a deterministic ordering for events sharing the same effective date:

1. DEBT_REGISTERED before PAYMENT_RECEIVED.
2. PAYMENT_RECEIVED before INTEREST_ACCRUED.
3. CORRECTION events ordered by created_at (posting time).
4. The ordering shall be configurable via rules engine (Drools) to accommodate future business rule changes.

### FR-6: Allokeringsunderretning with crossing details

OpenDebt shall generate an Allokeringsunderretning that includes:

1. The original dækning allocation (before crossing correction).
2. The dækningsophævelse (reversal).
3. The new dækning allocation (after correction).
4. The net effect on hovedstol and rente separately.
5. Reference to the crossing transaction that triggered the recalculation.

### FR-7: Multi-claim awareness

When a debtor has multiple claims and a crossing transaction affects dækning allocation across claims (e.g., a payment was split across claims, but the recalculated interest on one claim changes the split):

1. The replay shall consider all affected claims for the debtor.
2. Dækningsrækkefølge shall be applied per claim and across claims according to the legal priority order (boernebidrag > skat > boeder, per collection-priority.drl).

## PSRM Reference Context

### Dækningsrækkefølge (coverage ordering)
Inddrivelsesrenten (5.75% per 1 January 2026) dækkes altid forud for hovedstol. Simple dag-til-dag rente. No compound interest (rentes rente). When transactions cross, the interest amount changes, which changes the interest/principal split of any dækning applied in the affected period.

_Source: `docs/psrm-reference/07-renteregler.md`_

### Allokeringsunderretning
"Som Udligningsunderretning, men viser derudover fordeling mellem afdrag på hovedstol og dækning af renter. Inkluderer dækningsophævelser ved krydsende finansielle transaktioner. Gensidigt eksklusiv med Udligningsunderretning."

_Source: `docs/begrebsmodel/Inddrivelse-begrebsmodel-UFST-v3.md` section 4.29_

### Underretningsmeddelelser — Allokering
"Daglig ved saldo-bevægelse. Som udligning MEN viser fordeling mellem afdrag på hovedstol og dækning af renter. Inkl. dækningsophævelser ved krydsende finansielle transaktioner."

_Source: `docs/psrm-reference/06-underretningsmeddelelser.md`_

### Bi-temporal bookkeeping (ADR-0018)
The double-entry bookkeeping system uses `effective_date` (vaerdidag) and `posting_date` (bogfoeringsdag). Retroactive corrections posted today can have an effective date in the past. The storno pattern ensures full auditability.

_Source: `docs/adr/0018-double-entry-bookkeeping.md`_

## Constraints and assumptions

- The existing bi-temporal event timeline (`debt_events`), storno pattern, and `InterestAccrualService` shall be extended, not replaced.
- The replay algorithm shall be idempotent: replaying the same timeline twice produces identical results.
- Performance: the replay window shall be bounded (e.g., max 12 months lookback) to prevent unbounded recalculation. Configurable via application property.
- The system shall log every crossing detection and replay for operational monitoring and audit.
- This petition assumes the dækningsrækkefølge engine (W7-INDR-02) is implemented or will be implemented concurrently.
- Interest calculation follows the simple dag-til-dag formula: `hovedstol * (rentesats / 365) * dage`. No compound interest.

## Existing system building blocks

| Component | Status | Relevance |
|-----------|--------|-----------|
| `InterestAccrualServiceImpl` | Done | Period-based interest calc with balance timeline — extend for full replay |
| `RetroactiveCorrectionServiceImpl` | Done | Single-event storno + recalc — extend for multi-event cascading |
| `BookkeepingServiceImpl` | Done | Double-entry posting with bi-temporal dates |
| `DebtEventEntity` | Done | Immutable event timeline with effective_date |
| `LedgerEntryEntity` | Done | Storno support, entry categories |
| `collection-priority.drl` | Done | Legal priority ordering for dækningsrækkefølge across claims |
| W7-INDR-02 (backlog) | Not started | Dækningsrækkefølge engine — needed as prerequisite or concurrent work |

## Out of scope

- Full CREMUL/DEBMUL file processing pipeline (covered by integration-gateway and petition001).
- Interest rate changes over time (annual rate update) — handled separately in rules engine.
- Rentestop for uafklaret gæld logic (separate business rule).
- Fordringshaver's own interest calculation (renteregel 002).
- Physical letter generation for Allokeringsunderretning (letter-service concern).
- Cross-debtor crossing (e.g., solidarisk hæftelse where one debtor's payment crosses another debtor's action on the same claim) — deferred to a later petition if needed.

## Terminology mapping (begrebsmodel v3)

| Danish | English (code) | Context |
|--------|---------------|---------|
| Krydsende handlinger | Crossing Transactions | When effective dates overlap/precede posting dates |
| Dækningsophævelse | Coverage Reversal | Reversal of a previously applied recovery |
| Dækningsrækkefølge | Coverage Priority Order | Interest before principal |
| Allokeringsunderretning | Allocation Notification | Notification showing interest/principal split incl. reversals |
| Vaerdidag | Effective Date | When the economic event applies |
| Bogføringsdag | Posting Date | When it was recorded in the system |
| Inddrivelsesrente | Collection Interest | 5.75% per 1 January 2026 |
| Hovedstol | Principal | The original claim amount |
| Dækning | Recovery/Coverage | Payment applied to a claim |
