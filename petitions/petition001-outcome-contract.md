# Petition 001 Outcome Contract

## Acceptance criteria

1. Given an incoming payment with an `OCR-linje` that uniquely identifies a debt, OpenDebt auto-matches the payment to that debt.
2. A unique `OCR-linje` match is auto-matched even when the paid amount differs from the expected amount on the `påkrav`.
3. When a payment is auto-matched, the matched debt is written down by the actual amount paid.
4. Given an incoming payment that cannot be uniquely matched using the `OCR-linje`, OpenDebt does not auto-apply the payment and routes it to manual matching on the case.
5. Given an auto-matched payment where the paid amount creates an excess amount beyond straightforward write-down of the matched debt, OpenDebt enters a rule-driven overpayment branch rather than sending the payment to manual matching solely because of the amount difference.
6. At this requirement level, the overpayment branch supports only these high-level outcomes:
   - payout
   - use to cover other debt posts
7. The choice between those overpayment outcomes is determined by rules based on `sagstype` and whether the payment is a `frivillig indbetaling`.

## Definition of done

- The requirements distinguish clearly between:
  - unique OCR-based auto-matching
  - case-based manual matching when unique matching is not possible
  - rule-driven overpayment handling after successful auto-match
- The conditions for amount-mismatch auto-matching are testable.
- The write-down by actual paid amount is testable.
- The manual matching trigger is testable.
- The allowed high-level overpayment outcomes are testable without inventing detailed allocation logic.
- Every acceptance criterion is covered by at least one Gherkin scenario.

## Failure conditions

- A payment with a unique `OCR-linje` is sent to manual matching solely because the amount differs from the expected amount.
- A matched debt is written down by the expected amount instead of the actual paid amount.
- A payment that cannot be uniquely matched is auto-applied.
- Detailed overpayment rules are introduced that are not justified by the petition.
