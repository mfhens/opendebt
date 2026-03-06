# Petition 003 Outcome Contract

## Acceptance criteria

1. OpenDebt distinguishes between a `fordring` and a `restance`.
2. A registered claim with a betalingsfrist is not treated as a `restance` before the betalingsfrist is exceeded.
3. A claim becomes a `restance` when the betalingsfrist is exceeded and the claim remains unpaid or underpaid.
4. A claim that is fully paid before or by the betalingsfrist does not become a `restance`.
5. OpenDebt supports an explicit `overdragelse til inddrivelse` action for a `restance`.
6. OpenDebt rejects an attempt to transfer a claim that is not a `restance`.
7. A successful transfer records the `fordringshaver`, the receiving `restanceinddrivelsesmyndighed`, the transferred `restance`, and the transfer time.
8. A successful transfer makes the `restance` eligible for subsequent collection handling.

## Definition of done

- The lifecycle distinction between `fordring` and `restance` is testable.
- The transfer precondition (`only restance may be transferred`) is testable.
- The transfer audit trail is testable.
- The handoff from transfer to later collection handling is testable at a high level.
- Every acceptance criterion is covered by at least one Gherkin scenario.

## Failure conditions

- A non-overdue claim is treated as a `restance`.
- A fully paid claim becomes a `restance`.
- A non-`restance` claim can be transferred to collection.
- A successful transfer does not record the required actors and timestamp.
- A successful transfer does not make the `restance` available for further collection handling.
