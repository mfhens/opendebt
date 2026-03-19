# Petition 043: Batch processing for daily lifecycle transitions, interest accrual, and deadline monitoring

## Summary

OpenDebt shall support scheduled batch processing to handle daily operations at scale: transitioning overdue claims to `restance`, accruing `inddrivelsesrente` on transferred claims, and monitoring deadlines for `forældelse`, `høring` SLA, and `betalingsfrist`. These operations shall execute as bulk database operations rather than per-entity REST calls to support a portfolio of approximately 1 million active debts across 150,000 debtors.

## Context and motivation

The current architecture processes claim lifecycle transitions and state evaluations one entity at a time via synchronous REST endpoints. At the expected production volume of ~1,000,000 active debts, this approach would require millions of database round-trips per daily run, making it operationally infeasible.

PSRM requires:

- **Daily interest accrual**: `inddrivelsesrente` at 5.75% p.a. (simpel dag-til-dag) is charged from the 1st of the month following `modtagelse` for all claims in state `OVERDRAGET`
- **Daily lifecycle evaluation**: claims where `betalingsfrist` has expired and `outstanding_balance > 0` must transition from `REGISTERED` to `RESTANCE`
- **Deadline monitoring**: `forældelsesfrist` approaching, `høring` SLA deadlines, and other time-sensitive business rules must be evaluated continuously

These operations are inherently batch-oriented and must complete within a defined processing window (typically overnight) without impacting portal response times.

## Functional requirements

### Daily RESTANCE transition

1. OpenDebt shall, on a daily schedule, identify all claims in state `REGISTERED` where the `betalingsfrist` (last payment date) has expired and the `outstanding_balance` is greater than zero.
2. OpenDebt shall transition all such claims to state `RESTANCE` in a single bulk operation.
3. OpenDebt shall record a `ClaimLifecycleEvent` for each transitioned claim.
4. Claims that are fully paid (`outstanding_balance` = 0) shall not transition regardless of deadline expiry.

### Daily interest accrual

5. OpenDebt shall, on a daily schedule, calculate `inddrivelsesrente` for all claims in state `OVERDRAGET` where the interest accrual start date has been reached.
6. The interest accrual start date is the 1st of the month following `received_at`.
7. Interest shall be calculated as simple daily interest: `outstanding_balance × annual_rate / 365`.
8. The annual interest rate shall be configurable (currently 5.75%).
9. Accrued interest shall be recorded as a separate journal entry, not by modifying the `principal_amount`.
10. Each interest journal entry shall record the accrual date, effective date, balance snapshot, and rate used, so that it can be stornoed and recalculated by petition039 (krydsende handlinger) when a crossing transaction is detected.
11. Interest shall not be accrued on claims in terminal states (`TILBAGEKALDT`, `AFSKREVET`, `INDFRIET`).

### Deadline monitoring

12. OpenDebt shall, on a daily schedule, identify claims where the `limitation_date` (forældelsesfrist) is within a configurable warning threshold (default: 90 days).
13. OpenDebt shall identify `høring` records where the `sla_deadline` has expired without resolution.
14. Deadline violations and approaching deadlines shall be logged and made available for caseworker review.

### Operational requirements

15. Batch jobs shall be idempotent: running the same job twice for the same date shall not produce duplicate transitions, interest entries, or events.
16. Batch jobs shall process claims in configurable page sizes (default: 1000) to manage memory and transaction scope.
17. Batch jobs shall record execution metadata: start time, end time, records processed, records failed, and outcome.
18. Batch jobs shall not block or degrade portal API response times during execution.

## Constraints and assumptions

- PostgreSQL 16 can handle bulk updates on 1M rows within acceptable time frames given proper indexes (which already exist on `lifecycle_state`, `last_payment_date`, `limitation_date`).
- The existing `claim_lifecycle_events` table can absorb batch-inserted audit records.
- Interest calculation produces journal entries; the bookkeeping model for interest journals is assumed to follow the existing double-entry pattern (ADR-0018) but detailed ledger integration is out of scope.
- The exact scheduling mechanism (Spring `@Scheduled`, Spring Batch, or Kubernetes CronJob) is an implementation decision, not a business requirement.
- **Krydsende handlinger (crossing transactions) and retroactive correction:** This petition accrues interest based on the current outstanding balance at batch execution time. When a crossing transaction arrives later (e.g., a CREMUL payment with a value date preceding the last accrual), the interest entries produced by this batch may become incorrect. Correction of such entries — including storno, full timeline replay, dækningsophævelse, and recalculation with dækningsrækkefølge (rente forud for hovedstol) — is handled by petition039 (krydsende handlinger). This matches PSRM's operational model: accrue daily, correct retroactively when crossings are detected.
- **Storno compatibility:** All interest journal entries produced by this batch must be storno-compatible (ADR-0018). Each entry must record the accrual date, the effective date, and the balance snapshot used for calculation, so that petition039's timeline replay can identify, reverse, and replace affected entries.

## PSRM reference context

**Sources:**
- [Renteregler](https://gaeldst.dk/fordringshaver/find-vejledning/renteregler)
- [Generelle krav til fordringer](https://gaeldst.dk/fordringshaver/find-vejledning/generelle-krav-til-fordringer)

### Inddrivelsesrente

- Rate: 5.75% p.a. (as of 2026), updated annually by Skatteministeriet
- Calculation: simpel dag-til-dag rente (simple daily interest, no compounding)
- Start: 1st of the month following `modtagelsestidspunktet` (received_at)
- Applies to: all claims in active collection (`OVERDRAGET`)
- Does not apply to: withdrawn, written-off, or fully paid claims

### Forældelse

- Standard forældelsesfrist: 3 years from betalingsfrist for most claim types
- Extended: 5 or 10 years for specific claim types (e.g., tax, judgments)
- If a claim reaches its forældelsesfrist without interruption, it must be written off

## Out of scope

- Detailed bookkeeping ledger entries for interest (deferred to double-entry bookkeeping refinement)
- Notification generation from deadline monitoring (covered by petition004)
- Rules engine integration for interest rate determination (covered by TB-008/Drools)
- Batch processing for payment matching (covered by petition001 and TB-004/Smooks)
- Retroactive interest correction due to krydsende handlinger (covered by petition039 -- storno, timeline replay, dækningsophævelse)
