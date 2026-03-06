# Petition 006: Indsigelse and workflow blocking during dispute handling

## Summary

OpenDebt shall support `indsigelse` as a first-class business object linked to a specific `fordring` or `restance`. When an active `indsigelse` exists, OpenDebt shall block or pause further collection progression for the affected claim until the indsigelse is resolved according to the applicable process.

## Context and motivation

The begrebsmodel treats `indsigelse` as a separate reaction concept, not as a subtype of communication. That matters because an objection changes the legal and operational treatment of the claim.

Without an explicit objection model, OpenDebt cannot reliably express:

- that the debtor disputes the claim
- why collection progression has paused
- when collection may resume
- how the case and communication history relate to the objection

## Functional requirements

1. OpenDebt shall support registration of an `indsigelse` against a specific `fordring` or `restance`.
2. An `indsigelse` shall identify:
   - the relevant `skyldner`
   - the affected `fordring` or `restance`
   - the registration time
   - the stated reason or basis for objection
3. When an active `indsigelse` exists for a claim, OpenDebt shall prevent further collection progression for that claim unless a specific allowed exception applies.
4. While an active `indsigelse` exists, OpenDebt shall expose the claim or related case as blocked, paused, or under appeal for workflow purposes.
5. OpenDebt shall support resolving an `indsigelse` with at least these high-level outcomes:
   - objection upheld
   - objection rejected
6. If the objection is upheld, OpenDebt shall prevent normal collection continuation for the affected claim until the resulting claim adjustment or closure has been handled.
7. If the objection is rejected, OpenDebt shall allow the claim to resume normal collection progression.
8. OpenDebt shall preserve the objection history for later case handling, legal review, and audit.

## Constraints and assumptions

- This petition defines objection lifecycle and workflow effect, not every legal subtype of objection.
- This petition does not define final caseworker UI.
- This petition does not define detailed SLA rules for resolving objections.
- This petition assumes that later communication to the debtor about the objection outcome may be handled by the communication model.

## Out of scope

- Full appeals hierarchy beyond the objection step
- Detailed document templates for objection acknowledgment and resolution notices
- Detailed legal decision support rules
