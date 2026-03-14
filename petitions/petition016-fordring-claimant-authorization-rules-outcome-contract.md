# Petition 016 Outcome Contract

## Acceptance criteria

### System Reporter Validation
1. A system-to-system submission where the system reporter is not authorized for the fordringshaver is rejected with error code 400.
2. A system-to-system submission where the system reporter is authorized for the fordringshaver passes validation.

### INDR Permission
3. An OPRETFORDRING action with ArtType 'INDR' from a fordringshaver without INDR permission is rejected with error code 416.
4. An OPRETFORDRING action with ArtType 'INDR' from a fordringshaver with INDR permission passes validation.
5. A GENINDSENDFORDRING action with ArtType 'INDR' from a fordringshaver without INDR permission is rejected with error code 416.

### MODR Permission
6. An OPRETFORDRING action with ArtType 'MODR' from a fordringshaver without MODR permission is rejected with error code 419.
7. An OPRETFORDRING action with ArtType 'MODR' from a fordringshaver with MODR permission passes validation.
8. A GENINDSENDFORDRING action with ArtType 'MODR' from a fordringshaver without MODR permission is rejected with error code 419.

### Nedskriv Permission
9. A NEDSKRIV action from a fordringshaver without nedskriv permission is rejected with error code 420.
10. A NEDSKRIV action from a fordringshaver with nedskriv permission passes validation.

### Tilbagekald Permission
11. A TILBAGEKALD action from a fordringshaver without tilbagekald permission is rejected with error code 421.
12. A TILBAGEKALD action from a fordringshaver with tilbagekald permission passes validation.

### Portal Permission
13. A portal submission from a fordringshaver without portal agreement is rejected with error code 437.
14. A portal submission from a fordringshaver with portal agreement passes validation.

### Complex Action Permissions
15. An OPSKRIVNINGANNULLERETNEDSKRIVNINGINDBETALING action without OANI permission is rejected with error code 465.
16. An OPSKRIVNINGREGULERING action without opskrivning regulering permission is rejected with error code 466.
17. An OPSKRIVNINGOMGJORTNEDSKRIVNINGREGULERING action without OONR permission is rejected with error code 497.
18. A NEDSKRIVNINGANNULLERETOPSKRIVNINGREGULERING action without NAOR permission is rejected with error code 501.
19. A NEDSKRIVNINGANNULLERETOPSKRIVNINGINDBETALING action without NAOI permission is rejected with error code 508.

### Hovedstol Permission
20. An AENDRFORDRING action modifying hovedstol without hovedstol permission is rejected with error code 511.
21. An AENDRFORDRING action modifying hovedstol with hovedstol permission passes validation.

### Genindsend Permission
22. A GENINDSENDFORDRING action from a fordringshaver without resubmit permission is rejected with error code 543.
23. A GENINDSENDFORDRING action from a fordringshaver with resubmit permission passes validation.

### SSO Access
24. A portal submission with invalid SSO access is rejected with error code 480.
25. A portal submission with valid SSO access passes validation.

## Definition of done

- All 14 functional requirements are implemented as Drools rules.
- Rules query ClaimantAgreementService for permission checks.
- Permission lookups are cached with 5-minute TTL.
- All authorization failures are logged with identity information.
- Unit tests cover all permission combinations.
- Integration tests verify authorization against sample agreements.
- Authorization validation completes within 100ms.

## Failure conditions

- A submission from unauthorized system reporter is accepted.
- A fordringshaver without INDR permission can submit INDR claims.
- A fordringshaver without MODR permission can submit MODR claims.
- A fordringshaver without nedskriv permission can perform nedskrivning.
- A fordringshaver without tilbagekald permission can withdraw claims.
- A fordringshaver without portal agreement can submit via portal.
- A fordringshaver without permission can submit complex correction actions.
- A fordringshaver without hovedstol permission can modify principal.
- A fordringshaver without resubmit permission can resubmit claims.
- Authorization failures are not logged.
- Authorization validation exceeds 100ms.
