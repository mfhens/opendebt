# Petition 006 Outcome Contract

## Acceptance criteria

1. OpenDebt supports registration of an `indsigelse` against a specific `fordring` or `restance`.
2. A registered `indsigelse` records the relevant debtor, claim, time, and reason.
3. An active `indsigelse` blocks or pauses further collection progression for the affected claim.
4. The affected claim or related case can be identified as blocked, paused, or under appeal while the objection is active.
5. OpenDebt supports resolving an `indsigelse` as either upheld or rejected.
6. If the objection is upheld, normal collection continuation does not resume automatically as if nothing happened.
7. If the objection is rejected, OpenDebt allows normal collection progression to resume.
8. The objection history remains available for case handling and audit.

## Definition of done

- Registration, blocking effect, and resolution are testable.
- The workflow state impact is testable.
- The distinction between upheld and rejected outcomes is testable.
- The objection history is testable.
- Every acceptance criterion is covered by at least one Gherkin scenario.

## Failure conditions

- An objection can be registered without being linked to a claim.
- Collection progression continues as normal while an active objection exists.
- Upheld and rejected objections have the same practical outcome.
- Objection history is lost or cannot be audited.
