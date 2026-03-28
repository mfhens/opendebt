# Petition 015: Fordring Core Claim Validation Rules

## Summary

OpenDebt shall validate incoming claim actions (aktioner) from the fordring integration against a set of core validation rules before processing. These rules ensure that the fundamental structure, data formats, and dates of claims are correct before any business logic is applied. Validation errors shall be returned with specific fordring-compatible error codes.

## Context and motivation

The OpenDebt system receives claim submissions from fordringshavere (creditors) via the fordring API. Before processing these claims, the system must validate that:

1. The correct XML structure is present for each action type (AktionKode)
2. Currency is DKK (the only supported currency)
3. Claim art type is valid (INDR for collection, MODR for offsetting)
4. Interest rates are non-negative
5. Dates are within valid ranges and logical sequences
6. The fordringhaveraftale (creditor agreement) exists
7. The debtor can be identified

These are "static" validation rules that can be evaluated without complex service calls and form the first validation layer in the claim processing pipeline.

## Functional requirements

### Structure Validation (Rules 403, 404, 406, 407, 412, 444, 447, 448, 458, 505)

1. When AktionKode is GENINDSENDFORDRING, the system shall verify that MFGenindsendFordringStruktur is present; if missing, reject with error code 403 (GENINDSEND_STRUCTURE_MISSING).
2. When AktionKode is OPSKRIVNINGREGULERING, the system shall verify that MFOpskrivningReguleringStruktur is present; if missing, reject with error code 404 (OPSKRIV_REGULERING_STRUKTUR).
3. When AktionKode is OPSKRIVNINGANNULLERETNEDSKRIVNINGINDBETALING, the system shall verify that the required struktur is present; if missing, reject with error code 406.
4. When AktionKode is OPSKRIVNINGOMGJORTNEDSKRIVNINGREGULERING, the system shall verify that the required struktur is present; if missing, reject with error code 407.
5. When AktionKode is NEDSKRIVNINGANNULLERETOPSKRIVNINGREGULERING, the system shall verify that the required struktur is present; if missing, reject with error code 412.
6. When AktionKode is OPRETFORDRING, the system shall verify that MFOpretFordringStruktur is present; if missing, reject with error code 444 (OPRETFORDRING_STRUKTUR).
7. When AktionKode is NEDSKRIV, the system shall verify that MFNedskrivFordringStruktur is present; if missing, reject with error code 447 (NEDSKRIV_STRUKTUR_MISSING).
8. When AktionKode is TILBAGEKALD, the system shall verify that MFTilbagekaldFordringStruktur is present; if missing, reject with error code 448 (TILBAGEKALD_STRUKTUR_MISSING).
9. When AktionKode is AENDRFORDRING, the system shall verify that MFAendrFordringStruktur is present; if missing, reject with error code 458 (AENDRFORDRING_STRUKTUR_MISSING).
10. When AktionKode is NEDSKRIVNINGANNULLERETOPSKRIVNINGINDBETALING, the system shall verify that the required struktur is present; if missing, reject with error code 505.

### Currency Validation (Rule 152)

11. For action types GENINDSENDFORDRING, OPRETFORDRING, NEDSKRIV, OPSKRIVNINGREGULERING, and related opskrivning/nedskrivning types, the system shall verify that ValutaKode equals 'DKK'; if not, reject with error code 152 (INVALID_CURRENCY).

### Art Type Validation (Rule 411)

12. For OPRETFORDRING and GENINDSENDFORDRING actions, the system shall verify that ArtType is either 'INDR' (inddrivelse) or 'MODR' (modregning); if not, reject with error code 411 (FORDRING_TYPE_ERROR).

    > **Note on MODR:** `MODR` (Modregning) is a legacy DMI concept — it does not exist as a distinct ArtType in PSRM. Rule 411 accepts `MODR` for backwards compatibility with DMI-path submissions only. New PSRM-path fordringer must use `INDR`. If/when DMI-path support is removed, `MODR` should be dropped from the valid set. Reference: PSRM Reference Context (Fordringsart row) in this petition.

### Interest Rate Validation (Rule 438)

13. The system shall verify that MerRenteSats is not negative; if negative, reject with error code 438 (NEGATIVE_INTEREST_RATE).

### Date Validation (Rules 409, 464, 467, 548, 568, 569)

14. The system shall verify that VirkningsDato (effective date) is filled when required; if missing, reject with error code 409 (VIRKNINGSDATO_MISSING).
15. The system shall verify that VirkningsDato is not later than ModtagelsesTidspunkt (receipt timestamp); if later, reject with error code 464 (VIRKNINGSDATO_SENERE_END_MODTAGELSE).
16. For withdrawal actions, the system shall verify that VirkningsDato is not earlier than the ModtagelsesTidspunkt of the main claim being withdrawn; if earlier, reject with error code 467.
17. The system shall verify that VirkningsDato is not in the future; if future, reject with error code 548 (NO_FUTURE_VIRKNINGDATO).
18. The system shall verify that all dates are on or after 1900-01-01; if earlier, reject with error code 568 (TIDLIGST_MULIG_DATO).
19. The system shall verify that PeriodeFra is not after PeriodeTil; if after, reject with error code 569 (PERIODE_TIL_EFTER_PERIODE_FRA).

### Agreement Validation (Rules 002, 151, 156)

20. The system shall verify that the FordringhaveraftaleID exists in the system; if not found, reject with error code 2 (NO_AGREEMENT_FOUND).
21. The system shall verify that the claim type (DMIFordringTypeKode) is allowed per the creditor agreement; if not allowed, reject with error code 151 (TYPE_AGREEMENT_MISSING).
22. For system-to-system integrations, the system shall verify that MFAftaleSystemIntegration is true on the agreement; if false, reject with error code 156 (NO_SYSTEM_TO_SYSTEM_INTEGRATION).

### Debtor Validation (Rule 005)

23. For applicable action types (GENINDSENDFORDRING, OPRETFORDRING, AENDRFORDRING, NEDSKRIV, and related types), the system shall verify that the debtor ID is valid (not 0 or all zeros); if invalid, reject with error code 5 (DEBTOR_NOT_FOUND).

## Non-functional requirements

1. All validation rules shall be implemented using Drools decision tables for maintainability.
2. Validation errors shall return the fordring-compatible error code number and Danish description.
3. Structure validation shall complete within 50ms for typical payloads.
4. All validation rule changes shall be auditable with version history.

## Constraints and assumptions

- Rules are based on the fordring integration API specification version extracted from dk.rim.is.api.rules
- The system receives pre-parsed claim action objects, not raw XML
- FordringhaveraftaleID lookup requires a call to the fordringshaver master data service
- Debtor validation requires a call to the person registry service
- Error codes must be numeric and match fordring error code enumeration

## PSRM Reference Context

### PSRM Stamdata Field Definitions

The validation rules in this petition correspond to the following official PSRM stamdata field definitions (ref: [Generelle krav til fordringer](https://gaeldst.dk/fordringshaver/find-vejledning/generelle-krav-til-fordringer)):

| Stamdata Field | PSRM Definition | Petition Relevance |
|---|---|---|
| **Beløb** | Restgæld at overdragelse (numeric). The outstanding debt balance at the time of submission to Gældsstyrelsen. | Validated by amount rules (petition018), but core structure must carry the field. |
| **Hovedstol** | Original pålydende — the original face value of the claim as established by the creditor. | Referenced in opskrivning/nedskrivning structure validation (rules 404, 406, 407, 412). |
| **Fordringstype (kode)** | Describes the retligt grundlag (legal basis) for the claim, e.g. `PSRESTS` = restskat. Each code maps to specific validation rules and interest regimes. | Validated against fordringshaveraftale in rule 151 (TYPE_AGREEMENT_MISSING). |
| **Fordringsart** | `INDR` (inddrivelse/collection) or `MODR` (modregning/offsetting). Note: only `INDR` exists in PSRM; `MODR` is a legacy DMI concept. | Directly validated by rule 411 (FORDRING_TYPE_ERROR). |
| **SRB** (Seneste rettidige betalingsdato) | Seneste betalingstidspunkt uden misligholdelse. Henstand or betalingsordning changes SRB; rykkerskrivelser do NOT affect SRB. | Part of date validation logic; SRB consistency checked in downstream rules. |
| **Forældelsesdato** | Must reflect the current limitation date at the time of oversendelse to Gældsstyrelsen, not the original limitation date. | Validated by date range rules (rules 568, 569) ensuring dates are within valid bounds. |
| **Stiftelsesdato** | Tidspunkt for den retsstiftende begivenhed — the date of the legally constitutive event that gave rise to the claim. | Part of stamdata consistency checks; must be ≥ 1900-01-01 per rule 568. |
| **Beskrivelse** | Free-text description, max 100 characters, must not contain PII (GDPR compliance). | Validated by content rules in petition018; GDPR constraint aligns with ADR-0014. |

### PSRM Submission Outcomes

When a claim passes validation and is submitted to PSRM, the system returns a **kvittering** (receipt) with a unique fordrings-ID. The submission outcome status can be:

| Status | Meaning | Petition Mapping |
|---|---|---|
| **UDFØRT** | Accepted — the claim has been successfully registered in PSRM. | Corresponds to successful validation (no error codes triggered). |
| **AFVIST** | Rejected — the claim failed validation or business rules. | Corresponds to any of the error codes in this petition (e.g., 152, 411, 438, etc.) being triggered. |
| **HØRING** | Pending review — the claim requires manual caseworker review before acceptance. | Not directly mapped to validation rules; occurs post-validation for certain claim types. |

These outcomes map directly to the petition's error code return model: each rejected claim returns a specific numeric error code with a Danish description, mirroring PSRM's AFVIST response pattern.

## Out of scope

- Claimant permission/authorization validation (covered in petition016)
- Claim reference and lifecycle validation (covered in petition017)
- Claim content/amount validation (covered in petition018)
- Integration with external DMI system
- Detailed fordring API endpoint implementation
