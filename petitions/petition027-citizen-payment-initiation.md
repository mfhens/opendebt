# Petition 027: Citizen payment initiation API and self-service payment

## Summary

OpenDebt shall expose a citizen-facing payment initiation API in `payment-service` and integrate it with the citizen portal so that citizens can pay their debt directly via Mit gældsoverblik. Citizens shall be able to pay a full amount, pay a partial amount, or request an instalment plan. Payments shall be recorded using the existing double-entry bookkeeping model (ADR-0018) and matched to the correct debt(s) to trigger write-down.

## Context and motivation

`payment-service` currently handles incoming CREMUL/DEBMUL bank payments and OCR-based matching (petition 001). There is no citizen-initiated payment flow. The gaeldst.dk self-service portal ("Mit gældsoverblik") allows citizens to view their debt (petition 026) but cannot yet accept payments.

Citizens need a way to pay directly from the debt overview page. This requires:

- An API endpoint in `payment-service` that accepts citizen payment requests.
- Integration with an external payment provider (Nets/Dankort, MobilePay) for card and mobile payments.
- A bank-transfer path that generates a payment reference (OCR line) so citizens can pay via their own bank.
- Recording successful payments as ledger entries in the double-entry bookkeeping system.
- Triggering debt write-down through the existing payment-matching flow.
- An instalment plan request path that creates a case in `case-service` for caseworker review.

## Functional requirements

1. `payment-service` shall expose a citizen payment initiation endpoint: `POST /api/v1/citizen/payments`.
2. The endpoint shall accept the following input:
   - `person_id` (resolved from the authenticated citizen's session context)
   - `debt_id` or list of `debt_ids` to pay
   - `amount` to pay
   - `payment_method` (card, MobilePay, bank transfer)
3. For card and MobilePay payments, the service shall redirect the citizen to an external payment provider and receive a callback on success or failure.
4. For bank transfer payments, the service shall generate a payment reference (OCR line) and display the bank details to the citizen.
5. Successful payments shall be recorded as ledger entries using the double-entry bookkeeping model (ADR-0018).
6. Successful payments shall trigger debt write-down via the existing payment-matching flow in `payment-service`.
7. The citizen portal shall display a payment confirmation page on success or a failure page with retry options on failure.
8. Payment history shall be visible on the citizen debt overview page (petition 026).
9. A citizen shall be able to request an instalment plan (afdragsordning). The request shall create a case in `case-service` for caseworker review and approval.
10. All payment operations shall be audit-logged via the shared audit infrastructure (ADR-0022).

## Technical approach

- The citizen payment endpoint is secured with OAuth2/OIDC; the `person_id` is resolved from the access token (petition 025 MitID authentication).
- The payment provider integration is abstracted behind a `PaymentProviderClient` interface so providers can be swapped without changing business logic.
- Payment callback handling uses a dedicated webhook endpoint (`POST /api/v1/citizen/payments/callback`) that verifies provider signatures and updates payment status.
- Bank transfer references are generated using the existing OCR line generation logic in `payment-service`.
- The instalment plan request endpoint (`POST /api/v1/citizen/instalment-requests`) creates a case in `case-service` via the service client pattern (ADR-0024 trace propagation).
- The citizen portal payment pages use Thymeleaf + HTMX (ADR-0023), following the same design patterns as the creditor portal and citizen landing page (petition 022).

## Configuration example

```yaml
opendebt:
  payment:
    citizen:
      enabled: true
      callback-base-url: ${PAYMENT_CALLBACK_BASE_URL:https://mitgaeldsoverblik.gaeldst.dk/api/v1/citizen/payments/callback}
    provider:
      type: ${PAYMENT_PROVIDER_TYPE:nets}
      nets:
        merchant-id: ${NETS_MERCHANT_ID:}
        api-url: ${NETS_API_URL:https://api.nets.eu/v1}
        secret-key: ${NETS_SECRET_KEY:}
      mobilepay:
        merchant-id: ${MOBILEPAY_MERCHANT_ID:}
        api-url: ${MOBILEPAY_API_URL:https://api.mobilepay.dk/v1}
  instalment:
    min-amount: 200
    max-duration-months: 36
```

## Constraints and assumptions

- The citizen is authenticated via MitID (petition 025) and their `person_id` is available from the session context.
- The citizen debt overview (petition 026) provides the debt data and links to the payment flow.
- Payment provider integration details (Nets, MobilePay) require separate contract and onboarding with the provider; this petition defines the integration interface, not the provider-specific setup.
- Instalment plan approval is a caseworker decision; this petition only covers the citizen request path.
- The existing double-entry bookkeeping and OCR matching logic in `payment-service` is reused for recording and matching citizen-initiated payments.

## Out of scope

- Payment provider contract negotiation and onboarding.
- Bank integration setup for direct debit (Betalingsservice).
- Automatic instalment plan approval (requires caseworker review).
- Refund processing.
- Payment dispute handling.

## Dependencies

- Petition 001: OCR payment matching (existing payment-service foundation).
- Petition 024: Citizen debt endpoint (provides debt data for the payment flow).
- Petition 025: MitID authentication (citizen identity and session).
- Petition 026: Citizen debt overview page (links to payment and displays payment history).
- ADR-0018: Double-entry bookkeeping.
- ADR-0022: Shared audit infrastructure.
- ADR-0023: Thymeleaf + HTMX frontend.
- ADR-0024: Observability and trace propagation.
