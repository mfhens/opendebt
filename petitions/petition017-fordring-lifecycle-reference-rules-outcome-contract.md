# Petition 017 Outcome Contract

## Acceptance criteria

### Genindsend (Resubmit) Rules
1. A GENINDSENDFORDRING for a claim withdrawn with reason HENS passes validation.
2. A GENINDSENDFORDRING for a claim withdrawn with reason KLAG passes validation.
3. A GENINDSENDFORDRING for a claim withdrawn with reason BORD passes validation.
4. A GENINDSENDFORDRING for a claim withdrawn with reason HAFT passes validation.
5. A GENINDSENDFORDRING for a claim withdrawn with another reason is rejected with error code 539.
6. A GENINDSENDFORDRING for a claim that is not withdrawn is rejected with error code 540.
7. A GENINDSENDFORDRING from a different fordringshaver than the original is rejected with error code 541.
8. A GENINDSENDFORDRING with different stamdata than the original is rejected with error code 542.
9. A GENINDSENDFORDRING for a MODR claim is rejected with error code 544.

### Tilbagekald (Withdrawal) Rules
10. An action on a claim withdrawn during conversion is rejected with error code 434.
11. A TILBAGEKALD with reason BORT for a DMI-routed claim is rejected with error code 538.
12. A TILBAGEKALD with reason FEJL and VirkningsDato filled is rejected with error code 546.
13. An eftersendt claim referencing a withdrawn main claim is rejected with error code 547.
14. A TILBAGEKALD on a hovedfordring with un-withdrawn divided claims is rejected with error code 570.

### Action Reference Rules
15. An action submitted while a previous action is pending is rejected with error code 418.
16. An action referencing an unknown AktionID is rejected with error code 429.
17. An action with invalid MFAktionIDRef is rejected with error code 526.
18. An OANI action without MFAktionIDRef when FordringID is known is rejected with error code 527.
19. An action referencing a withdrawn claim/action is rejected with error code 530.

### Opskrivning/Nedskrivning Reference Rules
20. An OANI/OONR action with mismatched beløb is rejected with error code 469.
21. An action with mismatched VirkningsDato is rejected with error code 470.
22. An OANI action referencing nedskrivning without INDB reason is rejected with error code 471.
23. An OANI/OONR action not referencing a nedskriv action is rejected with error code 473.
24. An opskrivning on an interest claim is rejected with error code 474.
25. An OONR action referencing nedskrivning without REGU reason is rejected with error code 477.
26. An action referencing a rejected opskrivning/nedskrivning is rejected with error code 493.
27. An action with mismatched FordringID is rejected with error code 494.
28. A NAOR action not referencing OR or OONR type is rejected with error code 502.
29. An annullering for an action with existing non-rejected annullering is rejected with error code 503.
30. A NAOR action with mismatched beløb is rejected with error code 504.
31. A NAOI action not referencing OANI type is rejected with error code 506.

### State Validation Rules
32. An action referencing a rejected FordringID is rejected with error code 428.
33. An annullering of a DMI action not yet UDFØRT is rejected with error code 488.
34. An opskrivning on a withdrawn original claim is rejected with error code 496.
35. A nedskrivning on a withdrawn FordringID is rejected with error code 498.

## Definition of done

- All 31 functional requirements are implemented as Drools rules.
- Rules properly query claim and action repositories.
- All state transitions are logged for audit.
- Unit tests cover all lifecycle scenarios.
- Integration tests verify reference validation against test data.
- Validation completes within 150ms.

## Failure conditions

- A claim resubmission with invalid withdrawal reason is accepted.
- A claim resubmission for a non-withdrawn claim is accepted.
- A claim resubmission from different claimant is accepted.
- A MODR claim can be resubmitted.
- BORT withdrawal works for DMI claims.
- FEJL withdrawal accepts VirkningsDato.
- Actions reference non-existent or withdrawn claims.
- Beløb or dates mismatch between actions is accepted.
- Opskrivning on interest claims is accepted.
- Invalid action type references are accepted.
- Multiple annulleringer for same action is accepted.
