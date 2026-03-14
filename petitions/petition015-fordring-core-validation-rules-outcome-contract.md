# Petition 015 Outcome Contract

## Acceptance criteria

### Structure Validation
1. A GENINDSENDFORDRING action without MFGenindsendFordringStruktur is rejected with error code 403.
2. An OPSKRIVNINGREGULERING action without MFOpskrivningReguleringStruktur is rejected with error code 404.
3. An OPSKRIVNINGANNULLERETNEDSKRIVNINGINDBETALING action without required struktur is rejected with error code 406.
4. An OPSKRIVNINGOMGJORTNEDSKRIVNINGREGULERING action without required struktur is rejected with error code 407.
5. A NEDSKRIVNINGANNULLERETOPSKRIVNINGREGULERING action without required struktur is rejected with error code 412.
6. An OPRETFORDRING action without MFOpretFordringStruktur is rejected with error code 444.
7. A NEDSKRIV action without MFNedskrivFordringStruktur is rejected with error code 447.
8. A TILBAGEKALD action without MFTilbagekaldFordringStruktur is rejected with error code 448.
9. An AENDRFORDRING action without MFAendrFordringStruktur is rejected with error code 458.
10. A NEDSKRIVNINGANNULLERETOPSKRIVNINGINDBETALING action without required struktur is rejected with error code 505.

### Currency Validation
11. A claim action with ValutaKode other than 'DKK' for applicable action types is rejected with error code 152.
12. A claim action with ValutaKode 'DKK' passes currency validation.

### Art Type Validation
13. An OPRETFORDRING or GENINDSENDFORDRING action with ArtType other than 'INDR' or 'MODR' is rejected with error code 411.
14. An OPRETFORDRING action with ArtType 'INDR' passes art type validation.
15. A GENINDSENDFORDRING action with ArtType 'MODR' passes art type validation.

### Interest Rate Validation
16. A claim action with negative MerRenteSats is rejected with error code 438.
17. A claim action with zero or positive MerRenteSats passes interest validation.

### Date Validation
18. A claim action with missing VirkningsDato when required is rejected with error code 409.
19. A claim action with VirkningsDato later than ModtagelsesTidspunkt is rejected with error code 464.
20. A withdrawal action with VirkningsDato earlier than main claim's ModtagelsesTidspunkt is rejected with error code 467.
21. A claim action with future VirkningsDato is rejected with error code 548.
22. A claim action with any date before 1900-01-01 is rejected with error code 568.
23. A claim action with PeriodeFra after PeriodeTil is rejected with error code 569.

### Agreement Validation
24. A claim action with non-existent FordringhaveraftaleID is rejected with error code 2.
25. A claim action with claim type not allowed by agreement is rejected with error code 151.
26. A system-to-system submission with MFAftaleSystemIntegration=false is rejected with error code 156.

### Debtor Validation
27. A claim action with invalid debtor ID (0 or all zeros) is rejected with error code 5.
28. A claim action with valid debtor ID passes debtor validation.

## Definition of done

- All 23 functional requirements are implemented as Drools rules.
- Each rule returns the correct Fordring error code and Danish description.
- Unit tests cover all rule conditions with positive and negative cases.
- Integration tests verify rule evaluation against sample Fordring payloads.
- Rules are documented in the rules-engine service.
- Structure validation completes within 50ms performance target.

## Failure conditions

- A claim with missing required struktur is accepted.
- A claim with non-DKK currency is accepted.
- A claim with invalid art type is accepted.
- A claim with negative interest rate is accepted.
- A claim with invalid dates is accepted.
- A claim with non-existent agreement is accepted.
- A claim with invalid debtor ID is accepted.
- Error codes returned do not match Fordring specification.
- Error descriptions are not in Danish.
