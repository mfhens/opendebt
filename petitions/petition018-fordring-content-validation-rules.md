# Petition 018: Fordring Claim Content Validation Rules

## Summary

OpenDebt shall validate the content and business logic of claim actions in the Fordring system. These rules validate claim amounts, types, documents, notes, hovedstol (principal), hæftelse (liability), and routing constraints. Validation errors shall be returned with specific Fordring-compatible error codes.

## Context and motivation

Beyond structure, dates, authorization, and references, the Fordring system must validate the actual content of claims:

1. **Amount Validation**: Claim amounts, nedskrivning amounts, and opskrivning amounts must meet minimum thresholds and business rules.

2. **Type Validation**: Claim types, sub-claim types, interest codes, and reason codes must be valid and consistent.

3. **Document/Note Validation**: Documents and notes must meet size limits and business constraints.

4. **Hovedstol/Hæftelse Validation**: Principal amount changes and liability configurations must be valid.

5. **Routing Constraints**: Some rules apply only to claims routed to PSRM or DMI.

## Functional requirements

### Claim Amount Validation (Rules 201, 215, 227, 408, 425)

1. The system shall verify that the main claim (hovedfordring) has category HF; if not, reject with error code 201 (HOVEDFORDRING_TYPE_ERROR).
2. The system shall verify that the claim amount exceeds the lower limit; if not, reject with error code 215 (FORDRING_AMOUNT_TOO_SMALL).
3. The system shall verify that opskrivning/nedskrivning correction amounts are not zero; if zero, reject with error code 227 (FORDRING_OPSKRIVNING_AMOUNT_TOO_SMALL).
4. For NEDSKRIV actions, the system shall verify that NedskrivningBeløb is greater than 0; if not, reject with error code 408 (NEDSKRIVNING_TOO_SMALL).
5. The system shall verify that only one hovedfordring is created per action; if multiple, reject with error code 425 (TOO_MANY_HOVEDFORDRINGER).

### Sub-Claim Validation (Rules 270, 459, 461, 423)

6. The system shall verify that sub-claim art matches main claim art; if different, reject with error code 270 (SUB_FORDRING_ART_MISMATCH).
7. The system shall verify that sub-claim types are allowed for the fordringshaver; if not allowed, reject with error code 459 (EFTERSENDT_RELATERET_FORDRING_IKKE_UNDERFORDRING).
8. The system shall verify that HovedfordringID is provided and exists for related claims; if missing or non-existent, reject with error code 461 (HOVEDFORDRINGS_ID_IKKE_ANGIVET).
9. The system shall verify that HovedfordringID is not filled by fordringshaver for related claims; if filled, reject with error code 423 (HOVEDFORDRING_ID_ERROR).

### Interest Validation (Rules 436, 441, 442, 443)

10. For OPRETFORDRING actions, the system shall verify that MerRenteSats is only provided for RenteSatsKode 03, 04, or 07; if provided for other codes, reject with error code 436 (INTEREST_RATE_CODE_ERROR).
11. The system shall verify that RenteRegel 002 is only used with RenteSatsKode 99 and MerRenteSats 00 or empty; if not, reject with error code 441 (INTEREST_RATE_RULE_ERROR).
12. The system shall verify that the RenteSatsKode is valid for PSRM; if not valid, reject with error code 442 (PSRM_INVALID_RENTESATSKODE).
13. The system shall verify that the claim type is interest-bearing when interest is specified; if not interest-bearing, reject with error code 443 (NO_INTEREST_ON_FORDRING_TYPE).

### Nedskriv Reason Validation (Rules 410, 433, 519, 571)

14. For NEDSKRIV actions, the system shall verify that ÅrsagKode REGU is only used at fordring level, not hæftelse level; if used at hæftelse level, reject with error code 410 (CAUSE_CODE_ERROR).
15. The system shall verify that debtor identity is provided for certain nedskrivning types; if missing, reject with error code 433 (NEDSKRIV_DEBTOR_REQUIRED).
16. For nedskrivninger with ÅrsagKode REGU, the system shall verify that VirkningsDato is not provided; if provided, reject with error code 519 (NEDSKRIV_REGU_VIRKNING_DATO_NONEMPTY).
17. The system shall verify that ÅrsagKode FAST is not used for PSRM claims; if used, reject with error code 571 (AARSAGSKODE_FAST_NOT_ON_PSRM_CLAIMS).

### Document and Note Validation (Rules 164, 181, 220, 413, 415, 516)

18. The system shall verify that document file size does not exceed the maximum allowed; if exceeded, reject with error code 164 (DOCUMENT_TOO_BIG).
19. The system shall verify that document count per action does not exceed the maximum allowed; if exceeded, reject with error code 181 (TOO_MANY_DOCUMENTS_IN_AKTION).
20. The system shall verify that notes (sagsbemærkninger) have content; if empty, reject with error code 220 (FORDRING_NOTE_CONTENT_MISSING).
21. The system shall verify that note length does not exceed the maximum allowed; if exceeded, reject with error code 413 (REMARK_TOO_LONG).
22. The system shall verify that document file types are allowed; if not allowed, reject with error code 415 (DOCUMENT_TYPE_ERROR).
23. For OANI, OONR, and OpRegu actions, the system shall verify that the underlying fordring does not contain documents or notes; if it does, reject with error code 516.

### Hovedstol Validation (Rules 510, 512, 517, 518)

24. The system shall verify that the new hovedstol amount is higher than the previous amount; if not higher, reject with error code 510 (FEJLAGTIG_HOVEDSTOL_INDBETALING_BELOEB_FOR_LAVT).
25. For claims routed to DMI, the system shall reject hovedstol changes with error code 512 (FEJLAGTIG_HOVEDSTOL_HF_ROUTED_TIL_EXML).
26. For AENDRFORDRING with FHI (fejlagtig hovedstol indberetning), the system shall verify the FHI struktur is present; if missing, reject with error code 517 (FEJLAGTIG_HOVEDSTOL_INDBERETNING_STRUKTUR_MISSING).
27. The system shall verify that hovedstol changes are not made on withdrawn claims; if attempted, reject with error code 518 (FHI_TILBAGEKALD_NOT_REJECTED_ON_RELATEDCLAIM).

### Hæftelse Validation (Rules 528, 531, 532, 533, 557, 559)

28. The system shall verify that a claim does not have multiple hæftere with the same identity; if duplicate, reject with error code 528 (TO_HAEFTERE_MED_SAMME_ID).
29. The system shall verify that HaeftelseDomId is accompanied by HaeftelseDomDato; if date missing, reject with error code 531 (HAEFTELSEDOMIDTRUE_BUT_DATE_NOT_PRESENT).
30. The system shall verify that HaeftelseDomDato is not present without HaeftelseDomId; if present, reject with error code 532 (HAEFTELSEDOMIDFALSE_BUT_DATE_IS_PRESENT).
31. The system shall verify that DomsDato/ForligsDato is not in the future; if future, reject with error code 533 (HAEFTELSEDOMDATE_IS_BEFORE_MODTAGELSESTIDSPUNKT).
32. For claims routed to DMI, the system shall verify that SRB and Forfald are consistent across hæftere; if inconsistent, reject with error code 557 (SRB_OR_FORFALD_MISMATCH).
33. For claims routed to DMI, the system shall verify no documents are on hæftelse level; if present, reject with error code 559 (NO_DOCUMENTS_ON_HAEFTELSENIVEAU_DMI).

### Routing Validation (Rules 422, 426, 565, 572)

34. For synchronous portal actions, the system shall verify the claim is not routed to DMI; if routed to DMI, reject with error code 422 (SYNCHRONOUS_ROUTING_TO_DMI_NOT_SUPPORTED).
35. For NAOR actions, the system shall reject routing to DMI with error code 426 (REJECT_NAOR_TO_EXMF).
36. For claims routed to DMI, the system shall verify AKR length is valid; if invalid, reject with error code 565 (AKR_LENGTH_TOO_LONG).
37. The system shall verify that claims with foreløbig fastsat (provisionally determined) are not sent to PSRM; if attempted, reject with error code 572 (FORELOEBIGFASTSAT_TARGETING_PSRM).

### Claim Type Validation (Rules 509, 537, 550, 574, 575)

38. The system shall verify that referenced FordringID is known by Fordring; if unknown, reject with error code 509 (FEJLAGTIG_HOVEDSTOL_INDBETALING_FORDRING_UKENDT).
39. For AENDRFORDRING on INDR claims, the system shall reject with error code 537 (MAA_IKKE_AENDRE_FORDRING_MED_ART_INDR).
40. The system shall verify that required stamdata fields are present; if missing, reject with error code 550 (STAMDATA_MANGLER).
41. The system shall verify that the claim type is active in PSRM; if inactive, reject with error code 574 (CLAIM_TYPE_IS_INACTIVE).
42. The system shall verify that BFE field is filled when required for the claim type; if missing, reject with error code 575 (BFE_IS_REQUIRED_FOR_CLAIMTYPE).

### Identifier Validation (Rules 486, 602, 603)

43. The system shall verify that FordringshaverRefID is unique; if not unique, reject with error code 486 (FORDRINGSHAVER_REF_ID_IKKE_UNIKT).
44. The system shall verify that claims already error-withdrawn cannot be modified; if attempted, reject with error code 602 (PSRM_CLAIM_ALREADY_ERROR_WITHDRAWN).
45. For claims withdrawn with HENS/KLAG/BORD, the system shall only allow FEJL withdrawal; if other reason, reject with error code 603 (PSRM_ONLY_ERROR_REASON_IS_ALLOWED).

## Non-functional requirements

1. All content validation rules shall be implemented using Drools rules.
2. Amount thresholds and size limits shall be configurable.
3. Validation shall log all rejected claims with reasons for audit.
4. Content validation shall complete within 100ms.

## Constraints and assumptions

- Configuration parameters for limits (max document size, max note length, etc.) are loaded from system configuration
- PSRM and DMI routing status is determined by claim type and fordringshaver configuration
- These rules execute after lifecycle rules (petition017) pass

## Out of scope

- Core claim structure validation (covered in petition015)
- Claimant authorization validation (covered in petition016)
- Claim lifecycle and reference validation (covered in petition017)
- Configuration management for validation parameters
- PSRM and DMI integration mechanics
