# Petition 016: Fordring Claimant Authorization Rules

## Summary

OpenDebt shall validate that the fordringshaver (creditor) submitting a claim action has the required permissions to perform that specific action type. These authorization rules check permissions configured in the fordringhaveraftale (creditor agreement) and validate system reporter identity. Validation errors shall be returned with specific Fordring-compatible error codes.

## Context and motivation

Beyond basic data validation, the Fordring system must enforce authorization rules that determine whether a specific fordringshaver is permitted to submit certain types of actions. These permissions are typically configured in the fordringhaveraftale and include:

1. Permission to submit inddrivelsesfordringer (collection claims)
2. Permission to submit modregningsfordringer (offsetting claims)
3. Permission to perform nedskrivninger (write-downs)
4. Permission to perform tilbagekald (withdrawals)
5. Permission to perform various opskrivning/nedskrivning correction actions
6. Permission to resubmit claims (genindsend)
7. Permission to modify hovedstol (principal amount)

Additionally, for system-to-system integrations, the system must verify that the system reporter matches the fordringshaver they claim to represent.

## Functional requirements

### System Reporter Validation (Rule 400)

1. For system-to-system submissions, the system shall verify that the system reporter (identified by certificate) is authorized to submit on behalf of the specified fordringshaver; if not, reject with error code 400 (SYSTEM_FORDRINGSHAVER_MISMATCH).

### INDR/MODR Permission (Rules 416, 419)

2. For OPRETFORDRING and GENINDSENDFORDRING actions with ArtType 'INDR', the system shall verify that the fordringshaver has permission to submit inddrivelsesfordringer; if not, reject with error code 416 (FORDRINGSHAVER_CANNOT_COLLECT).
3. For OPRETFORDRING and GENINDSENDFORDRING actions with ArtType 'MODR', the system shall verify that the fordringshaver has permission to submit modregningsfordringer; if not, reject with error code 419 (FORDRINGSHAVER_CANNOT_DEDUCT).

### Nedskriv Permission (Rule 420)

4. For NEDSKRIV actions, the system shall verify that the fordringshaver has permission to perform nedskrivninger; if not, reject with error code 420 (FORDRINGSHAVER_CANNOT_REDUCE).

### Tilbagekald Permission (Rule 421)

5. For TILBAGEKALD actions, the system shall verify that the fordringshaver has permission to perform tilbagekald; if not, reject with error code 421 (FORDRINGSHAVER_CANNOT_CALL_BACK).

### Portal Submission Permission (Rule 437)

6. For submissions via the fordringshaverportal, the system shall verify that the fordringshaver has an agreement allowing portal submissions; if not, reject with error code 437 (PORTAL_ALLOWED_TO_SEND_ACTIONS_ERROR).

### Opskrivning/Nedskrivning Correction Permissions (Rules 465, 466, 497, 501, 508)

7. For OPSKRIVNINGANNULLERETNEDSKRIVNINGINDBETALING actions, the system shall verify that the fordringshaver has permission to submit OANI actions; if not, reject with error code 465 (ANNULLERE_NEDSKRIVNING_INDBETALING_NOT_ALLOWED).
8. For OPSKRIVNINGREGULERING actions, the system shall verify that the fordringshaver has permission to submit opskrivning regulering actions; if not, reject with error code 466 (OPSKRIVNING_REGULERING_NOT_ALLOWED).
9. For OPSKRIVNINGOMGJORTNEDSKRIVNINGREGULERING actions, the system shall verify that the fordringshaver has permission to submit OONR actions; if not, reject with error code 497 (OPSKRIVNING_OMGJORT_NEDSKRIVNING_REGULERING_NOT_ALLOWED).
10. For NEDSKRIVNINGANNULLERETOPSKRIVNINGREGULERING actions, the system shall verify that the fordringshaver has permission to submit NAOR actions; if not, reject with error code 501 (ANNULLERE_OPSKRIVNING_REGULERING_NOT_ALLOWED).
11. For NEDSKRIVNINGANNULLERETOPSKRIVNINGINDBETALING actions, the system shall verify that the fordringshaver has permission to submit NAOI actions; if not, reject with error code 508 (ANNULLERE_OPSKRIVNING_INDBETALING_NOT_ALLOWED).

### Hovedstol Change Permission (Rule 511)

12. For AENDRFORDRING actions that modify hovedstol (principal amount), the system shall verify that the fordringshaver has permission to change hovedstol; if not, reject with error code 511 (MAA_IKKE_AENDRE_HOVEDSTOL).

### Genindsend Permission (Rule 543)

13. For GENINDSENDFORDRING actions, the system shall verify that the fordringshaver has permission to resubmit claims; if not, reject with error code 543 (CLAIMANT_MAY_NOT_RESUBMIT_CLAIMS).

### SSO Access Validation (Rule 480)

14. For portal submissions, the system shall validate the SSO access token and verify valid caseworker or fordringshaver access; if invalid, reject with error code 480 (SSO_INVALID_ACCESS).

## Non-functional requirements

1. Authorization checks shall be implemented using Drools rules that query the ClaimantAgreementService.
2. Permission lookups shall be cached for performance (cache TTL: 5 minutes).
3. All authorization failures shall be logged for audit purposes with user/system identity.
4. Authorization validation shall complete within 100ms including service calls.

## Constraints and assumptions

- Permission configuration is stored in the fordringhaveraftale and retrieved via ClaimantAgreementService
- System reporter identity is extracted from the OCES3 certificate
- Portal user identity is extracted from the MitID Erhverv token
- These rules execute after core validation rules (petition015) pass

## Out of scope

- Core claim structure and data validation (covered in petition015)
- Claim lifecycle and reference validation (covered in petition017)
- Claim content/amount validation (covered in petition018)
- Configuration of permissions in the fordringhaveraftale
- Certificate and token validation mechanics
