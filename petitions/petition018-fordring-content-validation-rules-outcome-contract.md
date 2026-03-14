# Petition 018 Outcome Contract

## Acceptance criteria

### Claim Amount Validation
1. A claim without hovedfordring category HF is rejected with error code 201.
2. A claim with amount below lower limit is rejected with error code 215.
3. An opskrivning/nedskrivning with zero correction amount is rejected with error code 227.
4. A NEDSKRIV with NedskrivningBeløb of 0 or negative is rejected with error code 408.
5. An action creating multiple hovedfordringer is rejected with error code 425.

### Sub-Claim Validation
6. A sub-claim with art type different from main claim is rejected with error code 270.
7. A sub-claim with type not allowed for fordringshaver is rejected with error code 459.
8. A related claim without HovedfordringID is rejected with error code 461.
9. A related claim with HovedfordringID filled by fordringshaver is rejected with error code 423.

### Interest Validation
10. An OPRETFORDRING with MerRenteSats for invalid RenteSatsKode is rejected with error code 436.
11. RenteRegel 002 with invalid RenteSatsKode/MerRenteSats combination is rejected with error code 441.
12. An invalid RenteSatsKode for PSRM is rejected with error code 442.
13. Interest on non-interest-bearing claim type is rejected with error code 443.

### Nedskriv Reason Validation
14. ÅrsagKode REGU at hæftelse level is rejected with error code 410.
15. Nedskrivning type requiring debtor without debtor is rejected with error code 433.
16. REGU nedskrivning with VirkningsDato is rejected with error code 519.
17. ÅrsagKode FAST for PSRM claim is rejected with error code 571.

### Document and Note Validation
18. A document exceeding max size is rejected with error code 164.
19. An action with too many documents is rejected with error code 181.
20. An empty note is rejected with error code 220.
21. A note exceeding max length is rejected with error code 413.
22. A disallowed document type is rejected with error code 415.
23. OANI/OONR/OpRegu with docs/notes on underlying fordring is rejected with error code 516.

### Hovedstol Validation
24. New hovedstol not higher than previous is rejected with error code 510.
25. Hovedstol change for DMI claim is rejected with error code 512.
26. AENDRFORDRING without FHI struktur is rejected with error code 517.
27. Hovedstol change on withdrawn claim is rejected with error code 518.

### Hæftelse Validation
28. Claim with duplicate hæftere is rejected with error code 528.
29. HaeftelseDomId without date is rejected with error code 531.
30. HaeftelseDomDato without id is rejected with error code 532.
31. Future DomsDato/ForligsDato is rejected with error code 533.
32. DMI claim with mismatched SRB/Forfald across hæftere is rejected with error code 557.
33. DMI claim with hæftelse-level documents is rejected with error code 559.

### Routing Validation
34. Synchronous portal action for DMI claim is rejected with error code 422.
35. NAOR to DMI is rejected with error code 426.
36. DMI claim with invalid AKR length is rejected with error code 565.
37. ForeløbigFastsat claim to PSRM is rejected with error code 572.

### Claim Type Validation
38. Unknown FordringID is rejected with error code 509.
39. AENDRFORDRING on INDR claim is rejected with error code 537.
40. Missing required stamdata is rejected with error code 550.
41. Inactive claim type in PSRM is rejected with error code 574.
42. Missing required BFE field is rejected with error code 575.

### Identifier Validation
43. Non-unique FordringshaverRefID is rejected with error code 486.
44. Modification of error-withdrawn claim is rejected with error code 602.
45. Non-FEJL withdrawal after HENS/KLAG/BORD is rejected with error code 603.

## Definition of done

- All 45 functional requirements are implemented as Drools rules.
- Configurable parameters are externalized to system configuration.
- All rejections are logged for audit.
- Unit tests cover all content validation scenarios.
- Integration tests verify against sample payloads.
- Validation completes within 100ms.

## Failure conditions

- Claims with invalid amounts are accepted.
- Sub-claims with mismatched art types are accepted.
- Invalid interest configurations are accepted.
- Oversized documents or notes are accepted.
- Invalid hæftelse configurations are accepted.
- DMI-incompatible claims are routed to DMI.
- PSRM-incompatible claims are routed to PSRM.
- Inactive claim types are accepted.
- Required fields are missing but accepted.
