# Petition 004: Formalization of underretning, påkrav, and rykker

## Summary

OpenDebt shall model `underretning` as a common business object for formal communication to `skyldner` about a `fordring` or `restance`. `Påkrav` and `rykker` shall be represented as distinct specializations of `underretning`. The system shall record sender, recipient(s), channel, relation to the relevant claim, and delivery state.

## Context and motivation

The current solution uses letters and communication concepts informally, but the begrebsmodel requires a stricter distinction:

- `underretning` is the common concept
- `påkrav` is a specific payment demand
- `rykker` is a follow-up communication after non-payment

Without a shared model, OpenDebt risks scattering legal communication semantics across letters, payment flows, and case workflows without a consistent audit trail.

## Functional requirements

1. OpenDebt shall model `underretning` as a formal communication object linked to a specific `fordring` or `restance`.
2. OpenDebt shall distinguish at least these subtypes of `underretning`:
   - `påkrav`
   - `rykker`
3. Every `underretning` shall record:
   - the sending `fordringshaver` or sending authority
   - one or more recipient `skyldnere`
   - the related `fordring` or `restance`
   - the communication channel
   - the sending time
   - the delivery or processing state
4. OpenDebt shall support issuing a `påkrav` for a relevant `fordring`.
5. If a `påkrav` supports OCR-based payment handling, the `påkrav` shall carry the relevant `OCR-linje`.
6. OpenDebt shall support issuing a `rykker` when a `fordring` or `restance` has not been paid in time and a reminder is legally or operationally required.
7. A `rykker` shall be linked to the same `fordring` or `restance` as the underlying unpaid obligation.
8. OpenDebt shall preserve the communication history for each `underretning` so that later case handling and legal review can see what was sent and to whom.

## Constraints and assumptions

- This petition defines business semantics and audit needs, not final document template layout.
- This petition does not define every possible communication subtype beyond `påkrav` and `rykker`.
- This petition does not define channel-specific delivery integrations in detail.
- This petition assumes debtor identity is still handled through UUID references in person-registry.

## Out of scope

- Physical mail provider integration
- Digital Post transport details
- Detailed text template wording
- Detailed escalation rules after repeated reminders
