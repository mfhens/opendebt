# Petition 030: Fordringshaverportal -- Fordring detaljevisning

## Summary

The Fordringshaverportal shall provide a detailed view for individual claims, showing claim information, financial breakdown, debtor information, write-ups, write-downs, related claims, and decision history.

## Context and motivation

When a creditor clicks on a claim from any list view, they need comprehensive information about that claim. The legacy portal fetched this from PSRM `DKClaimDetails` and NyMF for receipts. In OpenDebt, the data source is `debt-service`, with debtor display data resolved via `person-registry`.

**Supersedes:** Fordringshaverportal petition 003 (old).
**References:** Petition 003 (fordring lifecycle), Petition 012 (BFF), Petition 029 (claims lists).

## Functional requirements

### Claim information section

1. The detail view shall display:
   - Fordringstype and fordringskategori (debt category)
   - Fordringshaver-beskrivelse (creditor's description of the claim)
   - Modtagelsesdato (date received)
   - Periode (from-to dates)
   - Stiftelsesdato (date of incorporation)
   - Forfaldsdato (due date) -- only if single debtor
   - Foraeldelsesdato (expiry date) -- only if single debtor
   - Retsdato (court date) -- if applicable
   - Sidste rettidige betalingsdag (last timely payment date) -- only if single debtor
   - Fordrings-ID, obligations-ID, and fordringshaver-reference
   - Fordringskategori (HOVEDFORDRING or sub-category)
   - Related obligations-ID (for sub-claims)

### Financial information section

2. The detail view shall display a financial breakdown table showing:
   - Rows per debt category with: original amount, write-off amount, payment amount, and balance
   - Inddrivelsesrenter (recovery interest) row
   - Inddrivelsesomkostninger (collection charges) from related claims
   - Opkraevningsrenter sendt til inddrivelse (collection interest sent for recovery)
   - Total current balance row
3. The view shall display:
   - Renteregel (interest rule) and rentesats (interest rate)
   - Ekstra rentesats (extra interest rate) if applicable
   - Total gaeld (total debt: main claim + charges + interest)
   - Seneste rentetilskrivningsdato (latest interest accrual date)
   - Oprindelig hovedstol (original principal)
   - Modtaget beloeb (received amount)
   - Fordringssaldo (claim balance)
   - Samlet fordringshaver-saldo (total claimant balance)
   - Beloeb indsendt til inddrivelse (with and without write-ups)

### Write-ups section (opskrivninger)

4. The detail view shall display a list of all write-ups on the claim.
5. Each write-up shall show: aktions-ID, reference-aktions-ID, formtype, aarsag (reason), beloeb (amount), virkningsdato (effective date), skyldner-ID.
6. Annulled write-ups shall be visually flagged.
7. Write-ups shall be sorted by aktions-ID.

### Write-downs section (nedskrivninger)

8. The detail view shall display a list of all write-downs on the claim.
9. Each write-down shall show: aktions-ID, reference-aktions-ID, formtype, aarsagskode (reason code), beloeb, virkningsdato, skyldner-ID.

### Related claims section

10. If the claim has related claims (underfordringer), the detail view shall list them with summary information.
11. Each related claim shall be clickable to navigate to its own detail view.

### Debtor information section (skyldnere)

12. The detail view shall list all debtors associated with the claim via the haeftelsesstruktur.
13. Each debtor shall show their identifier (CPR censored, CVR, SE, or AKR).
14. CPR numbers shall always be censored before display.

### Decisions section (afgoerelser)

15. Court decisions (dom) and settlements (forlig) shall be displayed with their dates.
16. This section is only shown when there is a single debtor.

### Receipt retrieval

17. The portal shall provide a mechanism to fetch receipts (kvitteringer) for claim operations.
18. Receipts are retrieved from `debt-service` using the delivery ID.

### Error handling

19. If a zero-balance claim is past 60 days, a message shall inform the user that detailed data is no longer available.
20. Service errors from `debt-service` shall be displayed as user-friendly Danish error messages.

### Access control

21. The detail view shall be accessible to all users with `CREDITOR_VIEWER` or `CREDITOR_EDITOR` roles.

## Layout and accessibility

22. The detail page shall use the SKAT standardlayout with breadcrumb showing: Forside > Fordringer > [Fordrings-ID].
23. Sections shall use collapsible `<details>`/`<summary>` elements for long sections (write-ups, write-downs, related claims).
24. Financial tables shall use semantic HTML with proper scope attributes.
25. All monetary amounts shall use Danish formatting (comma decimal separator, 2 decimal places).
26. All dates shall be formatted as `dd.MM.yyyy`.
27. All text shall use message bundles (petition 021).

## Data source mapping

| Old source | OpenDebt source |
|---|---|
| PSRM DKClaimDetails | `debt-service` GET /api/v1/debts/{id}/details |
| PSRM DKGetAmountSumForClaimListBS | `debt-service` GET /api/v1/debts/{id}/financials |
| NyMF ReceiptFetcher | `debt-service` GET /api/v1/debts/{id}/receipts/{deliveryId} |
| Person display data | `person-registry` via BFF |

## Constraints and assumptions

- The BFF resolves debtor display information from person-registry and censors CPR numbers before rendering.
- This petition defines the portal view, not the debt-service API contract.

## Out of scope

- Write-up/write-down submission flows (petition 033)
- Claim creation (petition 032)
- Backend API contract for claim details
