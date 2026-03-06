# Petition 007: Formalization of inddrivelsesskridt for modregning, lønindeholdelse, and udlæg

## Summary

OpenDebt shall represent `inddrivelsesskridt` as a common business concept for collection actions performed after `overdragelse til inddrivelse`. The first supported subtypes shall be `modregning`, `lønindeholdelse`, and `udlæg`. Each collection step shall be linked to a `restance`, tracked through its own lifecycle, and visible in case handling and audit.

## Context and motivation

The current codebase contains partial terminology and bookkeeping support for some collection actions, but the begrebsmodel requires a stricter structure:

- `inddrivelsesskridt` is the common abstract concept
- `modregning`, `lønindeholdelse`, and `udlæg` are different subtypes
- each subtype has different rules and operational consequences

Without a first-class model, OpenDebt risks treating collection actions as loose flags, enum values, or side effects instead of traceable domain objects.

## Functional requirements

1. OpenDebt shall model `inddrivelsesskridt` as a business object linked to a specific `restance`.
2. OpenDebt shall distinguish at least these subtypes of `inddrivelsesskridt`:
   - `modregning`
   - `lønindeholdelse`
   - `udlæg`
3. An `inddrivelsesskridt` shall be created only for a `restance` that has been transferred to collection.
4. Every `inddrivelsesskridt` shall record at least:
   - the affected `restance`
   - the step type
   - the initiating authority or system actor
   - the creation time
   - the current status
5. OpenDebt shall allow case handling to see all `inddrivelsesskridt` associated with a `restance`.
6. `Modregning`, `lønindeholdelse`, and `udlæg` shall be trackable as distinct types even when they share a common case timeline.
7. When an `inddrivelsesskridt` produces a financial consequence, OpenDebt shall keep bookkeeping and payment history aligned with the step outcome.
8. OpenDebt shall preserve the audit trail of initiated, changed, and completed collection steps.

## Constraints and assumptions

- This petition defines the common structure and subtype distinction, not the full detailed legal rules for each subtype.
- This petition does not define every external integration required for executing each step.
- This petition assumes that later subtype-specific petitions or tasks may refine the rules further.

## Out of scope

- Full legal rule engine for selecting the best collection step
- External employer integration for lønindeholdelse
- External enforcement authority integration for udlæg
- Detailed prioritization between concurrent collection steps
