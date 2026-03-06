# Petition 007 Outcome Contract

## Acceptance criteria

1. OpenDebt models `inddrivelsesskridt` as a business object linked to a specific `restance`.
2. OpenDebt distinguishes `modregning`, `lønindeholdelse`, and `udlæg` as separate kinds of `inddrivelsesskridt`.
3. A collection step can be created only for a `restance` that has been transferred to collection.
4. Every collection step records the related `restance`, step type, initiating actor, creation time, and status.
5. Case handling can list the collection steps associated with a `restance`.
6. Financial consequences from a collection step remain aligned with bookkeeping and payment history.
7. The lifecycle and audit trail of each collection step are preserved.

## Definition of done

- The shared collection-step structure is testable.
- The subtype distinction is testable.
- The transfer precondition is testable.
- Case visibility is testable.
- Financial and audit alignment is testable at a high level.
- Every acceptance criterion is covered by at least one Gherkin scenario.

## Failure conditions

- Collection actions are still represented only as loose flags or enums.
- A collection step can be created for a claim that has not been transferred to collection.
- Different step types cannot be distinguished in audit or case handling.
- Financial consequences from a step are not reflected in bookkeeping or payment history.
