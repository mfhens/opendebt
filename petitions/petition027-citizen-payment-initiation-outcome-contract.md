# Petition 027 Outcome Contract

## Acceptance criteria

1. `payment-service` exposes a citizen payment initiation endpoint (`POST /api/v1/citizen/payments`) that accepts `person_id`, `debt_id(s)`, `amount`, and `payment_method`.
2. The `person_id` is resolved from the authenticated citizen's session context, not from request input.
3. Card and MobilePay payments redirect the citizen to the external payment provider and handle success/failure callbacks.
4. Bank transfer payments generate a payment reference (OCR line) and display bank details to the citizen.
5. Successful payments are recorded as ledger entries using double-entry bookkeeping (ADR-0018).
6. Successful payments trigger debt write-down through the existing payment-matching flow.
7. The citizen portal displays a payment confirmation page on success and a failure page with retry options on failure.
8. Payment history for the authenticated citizen is visible on the debt overview page (petition 026).
9. A citizen can request an instalment plan, which creates a case in `case-service` for caseworker review.
10. All payment operations are audit-logged via the shared audit infrastructure (ADR-0022).

## Definition of done

- The citizen payment initiation endpoint is implemented, secured with OAuth2/OIDC, and reachable.
- Card/MobilePay payment flow (redirect → provider → callback → confirmation) is testable end-to-end with a mock provider.
- Bank transfer payment flow (generate OCR line → display bank details) is testable.
- Successful payments produce correct double-entry ledger entries.
- Successful payments trigger debt write-down and the debt balance reflects the payment.
- The instalment plan request creates a case in `case-service`.
- Payment confirmation and failure pages render correctly in the citizen portal.
- Payment history appears on the debt overview page.
- The `PaymentProviderClient` interface abstracts provider specifics so providers can be swapped.
- Every acceptance criterion is covered by at least one Gherkin scenario.

## Failure conditions

- The citizen payment endpoint is missing or returns errors for valid requests.
- The `person_id` is accepted from request input rather than resolved from the session context.
- Successful payments are not recorded as double-entry ledger entries.
- Successful payments do not trigger debt write-down.
- Payment callback from the provider is not verified or processed.
- Bank transfer payments do not generate a valid OCR line.
- Instalment plan request does not create a case in `case-service`.
- Payment operations are not audit-logged.
- The citizen portal does not display payment confirmation or failure feedback.
- Payment history is not visible on the debt overview page.
