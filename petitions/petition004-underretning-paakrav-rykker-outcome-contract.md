# Petition 004 Outcome Contract

## Acceptance criteria

1. OpenDebt models `underretning` as a business object linked to a specific `fordring` or `restance`.
2. OpenDebt distinguishes `påkrav` and `rykker` as separate kinds of `underretning`.
3. Every stored `underretning` records sender, recipient(s), related claim, channel, sending time, and status.
4. OpenDebt can issue a `påkrav` for a `fordring`.
5. A `påkrav` used for OCR-based payment carries the relevant `OCR-linje`.
6. OpenDebt can issue a `rykker` for an unpaid `fordring` or `restance`.
7. A `rykker` remains linked to the underlying unpaid claim.
8. The communication history is queryable for later case handling or audit.

## Definition of done

- The shared `underretning` structure is testable.
- The subtype distinction between `påkrav` and `rykker` is testable.
- The OCR dependency from `påkrav` to payment matching is testable.
- The communication history is testable.
- Every acceptance criterion is covered by at least one Gherkin scenario.

## Failure conditions

- `Påkrav` and `rykker` are stored without a shared communication structure.
- A stored `underretning` lacks sender, recipients, or related claim.
- An OCR-based `påkrav` does not carry the `OCR-linje` needed for payment matching.
- A `rykker` cannot be traced to the unpaid claim it concerns.
- Sent communications cannot be reconstructed for case handling or audit.
