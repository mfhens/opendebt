# Petition 043 Outcome Contract

## Acceptance criteria

### Daily RESTANCE transition

1. OpenDebt transitions all eligible `REGISTERED` claims with expired `betalingsfrist` and `outstanding_balance > 0` to `RESTANCE` in a daily batch run.
2. Fully paid claims are not transitioned regardless of deadline expiry.
3. A `ClaimLifecycleEvent` is recorded for each transitioned claim.
4. Running the batch job twice for the same date does not produce duplicate transitions.

### Daily interest accrual

5. OpenDebt accrues daily `inddrivelsesrente` on all `OVERDRAGET` claims past their interest start date.
6. Interest is calculated as `outstanding_balance × annual_rate / 365` (simple daily interest).
7. Interest is recorded as a separate journal entry, not by modifying `principal_amount`.
8. Each interest journal entry records the accrual date, effective date, balance snapshot, and rate used, making it storno-compatible for retroactive correction by petition039 (krydsende handlinger).
9. Claims in terminal states do not accrue interest.
10. Running the batch job twice for the same date does not produce duplicate interest entries.

### Deadline monitoring

11. OpenDebt identifies claims approaching `forældelsesfrist` within a configurable threshold.
12. OpenDebt identifies expired `høring` SLA deadlines.
13. Approaching and violated deadlines are logged and available for caseworker review.

### Operational

14. Batch jobs record execution metadata (start, end, counts, outcome).
15. Batch processing does not degrade portal API response times.

## Definition of done

- A daily RESTANCE transition batch job processes 1M claims within an acceptable time window.
- Interest accrual produces correct journal entries for a representative dataset.
- Interest journal entries are storno-compatible (contain accrual date, effective date, balance snapshot, rate).
- Deadline monitoring identifies approaching and expired deadlines correctly.
- All batch jobs are idempotent.
- Every acceptance criterion is covered by at least one Gherkin scenario.
- Performance is validated against a dataset of at least 100,000 records.

## Failure conditions

- A claim is transitioned to `RESTANCE` when it should not be (paid, deadline not expired).
- Interest is accrued on a terminal-state claim.
- An interest journal entry does not contain the metadata required for storno by petition039.
- A duplicate transition or interest entry is created by re-running the batch for the same date.
- Batch execution takes longer than the overnight processing window (8 hours).
- Portal response times degrade during batch execution.
- An approaching `forældelsesfrist` is not detected within the warning threshold.
