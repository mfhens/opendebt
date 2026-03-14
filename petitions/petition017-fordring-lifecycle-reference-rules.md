# Petition 017: Fordring Claim Lifecycle and Reference Rules

## Summary

OpenDebt shall validate claim lifecycle transitions and cross-references between actions in the Fordring system. These rules ensure that claim resubmissions (genindsend), withdrawals (tilbagekald), and correction actions properly reference existing claims and maintain consistent state. Validation errors shall be returned with specific Fordring-compatible error codes.

## Context and motivation

Claims in Fordring go through various lifecycle states and can reference other claims or actions. The system must enforce rules around:

1. **Genindsend (Resubmission)**: When a claim is withdrawn with certain reason codes, it can be resubmitted. The resubmission must reference the original claim and maintain data consistency.

2. **Tilbagekald (Withdrawal)**: Claims can be withdrawn with various reason codes. Certain withdrawals have restrictions based on claim state, routing, and related claims.

3. **Reference Validation**: Correction actions (opskrivning, nedskrivning, annullering) must reference existing actions with correct types and matching data.

4. **State Consistency**: Actions cannot reference withdrawn, rejected, or pending claims inappropriately.

## Functional requirements

### Genindsend (Resubmit) Rules (Rules 539-544)

1. For GENINDSENDFORDRING actions, the system shall verify that the original claim was withdrawn with a valid reason code (HENS, KLAG, BORD, or HAFT); if not, reject with error code 539 (GENINDSEND_OPRET_NOT_WITHDRAWN_WITH_VALID_REASON).
2. For GENINDSENDFORDRING actions, the system shall verify that the original claim is actually withdrawn; if not, reject with error code 540 (GENINDSEND_CREATE_CLAIM_NOT_WITHDRAWN).
3. For GENINDSENDFORDRING actions, the system shall verify that the resubmission is from the same fordringshaver as the original withdrawal; if not, reject with error code 541 (GENINDSEND_MUST_HAVE_SAME_CLAIMANT).
4. For GENINDSENDFORDRING actions, the system shall verify that the stamdata (basic data) matches the original claim; if different, reject with error code 542 (GENINDSEND_BASIC_DATA_NOT_EQUAL_TO_CREATE_CLAIM).
5. For GENINDSENDFORDRING actions with ArtType 'MODR', the system shall reject with error code 544 (GENINDSEND_MODR_NOT_ALLOWED) as modregningsfordringer cannot be resubmitted.

### Tilbagekald (Withdrawal) Rules (Rules 434, 538, 546, 547, 570)

6. For claims being corrected that were withdrawn in the conversion process, the system shall reject with error code 434 (FORDRING_TILBAGEKALDT).
7. For TILBAGEKALD actions with reason code BORT targeting claims routed to DMI, the system shall reject with error code 538 (TILBAGEKALD_BORT_NOT_TO_PSRM) as BORT cannot be used for DMI claims.
8. For TILBAGEKALD actions with reason code FEJL, the system shall verify that VirkningsDato is empty; if filled, reject with error code 546 (TILBAGEKALD_FEJL_NO_VIRKNINGDATO).
9. For eftersendt (late-submitted) related claims, the system shall verify that the main claim is not withdrawn or returned; if it is, reject with error code 547 (TILBAGEKALD_NOT_REJECTED_ON_MAINCLAIM).
10. For TILBAGEKALD actions on hovedfordringer (main claims) with divided related claims, the system shall verify that the related divided claims are withdrawn first; if not, reject with error code 570 (TILBAGEKALD_NOT_UDFOERT_ON_DIVIDED_RELATED_CLAIMS).

### Action Reference Rules (Rules 418, 429, 526, 527, 530)

11. The system shall verify that when a previous action is pending (not UDFØRT), new actions shall wait; reject with error code 418 (WAIT_FOR_AKTION_TO_BE_UDFOERT).
12. The system shall verify that referenced AktionID exists in the system; if unknown, reject with error code 429 (UNKNOWN_AKTION_ID).
13. The system shall verify that MFAktionIDRef points to an existing action; if not, reject with error code 526 (REF_AKTION_MISMATCH).
14. For OANI actions where FordringID is known, the system shall verify that MFAktionIDRef is filled; if not, reject with error code 527 (AKTIONIDREF_NOT_FILLED).
15. The system shall verify that referenced claims/actions are not withdrawn; if withdrawn, reject with error code 530 (REJECTED_POINTING_AT_WITHDRAWAL).

### Opskrivning/Nedskrivning Reference Rules (Rules 469-477, 493-494, 502-506)

16. For OANI and OONR actions, the system shall verify that FordringOpskrivningBeløb matches FordringNedskrivningBeløb on the referenced action; if different, reject with error code 469 (OPSKRIVNING_BELOEB_DOESNT_MATCH_NEDSKRIV).
17. The system shall verify that VirkningsDato matches VirkningFra on the referenced action; if different, reject with error code 470 (VIRKNINGSDATO_MISMATCH).
18. For OANI actions, the system shall verify that the referenced nedskrivning has ÅrsagKode INDB; if not, reject with error code 471 (FORDRING_NEDSKRIV_AARSAGSKODE_NOT_INDB).
19. For OANI and OONR actions, the system shall verify that MFAktionIDRef points to a nedskriv action; if not, reject with error code 473 (REF_AKTION_NEDSKRIV).
20. The system shall verify that opskrivning is not performed on interest claims (renter); if attempted, reject with error code 474 (OPSKRIVNING_ON_RENTE_NOT_ALLOWED).
21. For OONR actions, the system shall verify that the referenced nedskrivning has ÅrsagKode REGU; if not, reject with error code 477 (FORDRING_NEDSKRIV_AARSAGSKODE_NOT_REGU).
22. The system shall verify that referenced opskrivning/nedskrivning actions are not rejected; if rejected, reject with error code 493 (OPSKRIVNING_RELATERET_AKTION_AFVIST).
23. The system shall verify that FordringID on the action matches FordringID on the referenced action; if different, reject with error code 494 (FORDRINGS_ID_MISMATCH).
24. For NAOR actions, the system shall verify that the referenced action is of type OR (OpskrivningRegulering) or OONR; if not, reject with error code 502 (NAOR_KRAEVER_TYPE_OR_ELLER_OONR).
25. The system shall verify that no prior non-rejected annullering exists for the referenced action; if exists, reject with error code 503 (PRIOR_ANNULLERING_NOT_AFVIST).
26. For NAOR actions, the system shall verify that FordringNedskrivningBeløb equals FordringOpskrivningsBeløb in the referenced action; if different, reject with error code 504 (NEDSKRIVNINGSBELOEB_MISMATCH).
27. For NAOI actions, the system shall verify that the referenced action is of type OANI; if not, reject with error code 506 (NEDSKRIVNING_ANNULLERET_OPSKRIVNING_INDBETALING_INVALID_REFERENCE_TYPE).

### State Validation Rules (Rules 428, 488, 496, 498)

28. The system shall verify that referenced FordringID or HovedfordringID is not rejected; if rejected, reject with error code 428 (ANGIVNE_FORDRING_AFVIST).
29. For annullering actions referencing actions in DMI, the system shall verify that the action is UDFØRT; if not yet UDFØRT, reject with error code 488 (ANNULLERET_AKTION_I_DMI_ER_IKKE_UDFOERT).
30. For opskrivning actions, the system shall verify that the original claim is not withdrawn, returned, or rejected; if it is, reject with error code 496 (OPSKRIV_TILBAGEKALD_NOT_REJECTED_ON_RELATEDCLAIM).
31. For nedskrivning actions, the system shall verify that the FordringID is not withdrawn, returned, or rejected; if it is, reject with error code 498 (NEDSKRIV_TILBAGEKALD_NOT_REJECTED_ON_RELATEDCLAIM).

## Non-functional requirements

1. All lifecycle rules shall be implemented using Drools rules.
2. Reference validation shall query claim and action repositories efficiently.
3. Lifecycle state changes shall be logged for audit purposes.
4. Reference validation shall complete within 150ms including lookups.

## Constraints and assumptions

- Claim state information is available from the claim/action repository
- Referenced actions can be looked up by AktionID or FordringID
- DMI routing status is tracked per claim
- These rules execute after authorization rules (petition016) pass

## Out of scope

- Core claim structure validation (covered in petition015)
- Claimant authorization validation (covered in petition016)
- Claim amount/content validation (covered in petition018)
- Workflow and case management state transitions
- DMI synchronization mechanics
