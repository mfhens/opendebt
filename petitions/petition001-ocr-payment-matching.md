# Petition 001: OCR-based matching of incoming payments

## Summary

OpenDebt shall support automatic matching of incoming payments for issued `påkrav` using the Betalingsservice `OCR-linje`. Incoming payments received from SKB/CREMUL shall be matched automatically when the `OCR-linje` uniquely identifies a debt, even when the paid amount differs from the expected amount. Payments that cannot be uniquely matched shall be handled manually on the case.

## Context and motivation

OpenDebt sends `påkrav` to debtors. These `påkrav` include a Betalingsservice `OCR-linje` intended to identify the payment when funds are received. The repository already models inbound payment delivery via SKB CREMUL through `opendebt-integration-gateway`; this petition defines the business behavior after such a payment has been received by OpenDebt.

The business needs are:

- straight-through processing when the `OCR-linje` uniquely identifies the debt
- write-down of the debt by the actual amount paid
- case-based manual handling when automatic matching is not possible
- rule-driven handling of overpayments based on `sagstype` and whether the payment is a `frivillig indbetaling`

## Functional requirements

1. An issued `påkrav` shall contain a Betalingsservice `OCR-linje`.
2. OpenDebt shall attempt automatic matching of each incoming payment using the `OCR-linje`.
3. If the `OCR-linje` uniquely identifies a debt, OpenDebt shall auto-match the payment to that debt even when the paid amount differs from the expected amount.
4. When a payment is auto-matched, the debt shall be written down by the actual amount paid.
5. If a payment cannot be uniquely matched using the `OCR-linje`, OpenDebt shall not auto-apply the payment and shall route it to manual matching on the case.
6. If an auto-matched payment is larger than the amount that can be handled as a straightforward write-down on the matched debt, the subsequent treatment of the excess amount shall be determined by rules based on:
   - `sagstype`
   - whether the payment is a `frivillig indbetaling`
7. At this requirement level, the allowed high-level outcomes for such an excess amount are:
   - payout
   - use to cover other debt posts

## Constraints and assumptions

- Automatic matching is allowed only when the `OCR-linje` yields a unique match.
- This petition does not define the detailed rules that decide whether an excess amount is paid out or used to cover other debt posts.
- Manual matching is handled on the case; the detailed caseworker workflow and UI are out of scope here.
- This petition defines business behavior, not the technical details of UN/EDIFACT parsing or file polling in the SKB integration.
- **Excess payment dækningsrækkefølge (G.A.2.3.2.1 / GIL § 4):** When an auto-matched payment exceeds the amount applied to the matched debt and the excess is used to cover other debt posts, the coverage order must follow the dækningsrækkefølge prescribed by GIL § 4 and G.A.2.3.2.1: (1) bøder/tvangsbøder → (2) underholdsbidrag → (3) andre fordringer (FIFO within each category). Within each category, inddrivelsesrenter are dækket forud for the associated hauptfordring. The sagstype/frivillig-indbetaling rules govern the high-level payout-vs-cover decision, but the inter-claim allocation of any covered amount must respect this legal priority order.

## Legal Basis / G.A. References

- **GIL § 4** — dækningsrækkefølge for overskydende beløb
- **G.A.2.3.2.1** — Dækningsrækkefølge (priority order: bøder/tvangsbøder → underholdsbidrag → andre fordringer; renter forud for hauptfordring within each category)

## Out of scope

- Detailed SKB CREMUL/DEBMUL transport behavior
- Detailed allocation rules across multiple debt posts
- Refund execution details
- Bookkeeping implementation details
- Caseworker UI design
