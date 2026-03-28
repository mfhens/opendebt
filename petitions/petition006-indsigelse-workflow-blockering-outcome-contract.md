# Petition 006 Outcome Contract

## Acceptance criteria

1. OpenDebt supports registration of an `indsigelse` against a specific `fordring` or `restance`.
2. A registered `indsigelse` records the relevant debtor, claim, time, and reason.
3. Registration of an `indsigelse` does **not** automatically block or suspend collection. Collection continues unless a KLAG tilbagekald is received from the fordringshaver.
4. The affected claim or related case surfaces the active indsigelse for caseworker visibility, but the claim remains in its current collection state.
5. A caseworker can manually put collection in bero for a specific claim (discretionary, not automatic).
6. OpenDebt supports resolving an `indsigelse` as either upheld or rejected.
7. If the objection is upheld, the corrective action (withdrawal, opskrivning, nedskrivning) must be initiated by the fordringshaver via the normal workflow — OpenDebt does not automatically modify the claim.
8. If the objection is rejected, no system action is needed; the claim continues without interruption.
9. The objection history remains available for case handling and audit.

## Definition of done

- Registration, objection visibility, and resolution are testable.
- Confirmed that registering an indsigelse alone does NOT block collection (negative test).
- Confirmed that a KLAG tilbagekald DOES suspend collection.
- The distinction between upheld and rejected outcomes is testable.
- The objection history is testable.
- Every acceptance criterion is covered by at least one Gherkin scenario.

## Failure conditions

- An objection can be registered without being linked to a claim.
- Registering an indsigelse automatically blocks collection (contradicts G.A.1.3.1).
- Upheld and rejected objections have the same practical outcome.
- Objection history is lost or cannot be audited.

