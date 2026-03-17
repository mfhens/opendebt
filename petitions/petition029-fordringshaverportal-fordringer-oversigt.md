# Petition 029: Fordringshaverportal -- Fordringer i inddrivelse, nulfordringer og optaellinger

## Summary

The Fordringshaverportal shall provide paginated, sortable, searchable list views for claims in recovery (fordringer i inddrivelse) and zero-balance claims (nulfordringer), as well as count views for each category. These views allow creditors to monitor the status of their submitted claims.

## Context and motivation

Creditors need a primary overview of all their claims currently under collection by Gaeldsstyrelsen. The legacy Fordringshaverportal provided these views via PSRM and NyMF SOAP services. In OpenDebt, the data source is `debt-service`, accessed through the creditor-portal BFF (petition 012). The portal uses Thymeleaf + HTMX (ADR-0023) with the SKAT standardlayout.

**Supersedes:** Fordringshaverportal petitions 002, 004, 012 (old).
**References:** Petition 003 (fordring lifecycle), Petition 012 (BFF), Petition 013 (accessibility).

## Functional requirements

### Claims in recovery list

1. The portal shall display a paginated table of claims currently in recovery for the acting creditor.
2. Claims with a balance of 0,00 kr. shall be excluded from the recovery list (shown separately in zero-balance view).
3. Each claim row shall display:
   - Fordrings-ID
   - Modtagelsesdato (date received)
   - Skyldner-type (CPR, CVR, SE, or AKR)
   - Skyldner-ID (censored for CPR numbers -- first 6 digits only)
   - Antal skyldnere (number of debtors, if multiple)
   - Fordringshaver-reference
   - Fordringstype (claim type name)
   - Fordringsstatus
   - Stiftelsesdato (date of incorporation)
   - Periode (from-to dates)
   - Beloeb indsendt til inddrivelse (amount sent for recovery)
   - Saldo (balance)
   - Saldo med renter og gebyrer (balance including interest and fees)
4. The list shall support server-side sorting by any displayed column, in ascending and descending order.
5. The list shall support server-side search by:
   - Fordrings-ID
   - CPR number
   - CVR number
   - SE number
6. The list shall support date range filtering (modtagelsesdato from/to).
7. Clicking a claim row shall navigate to the claim detail page (petition 030).

### Zero-balance claims list

8. The portal shall display a paginated table of zero-balance claims using the same column layout as the recovery list.
9. Zero-balance claims are claims where the outstanding balance has been reduced to zero through payments, write-offs, or other actions.
10. The list shall support the same sorting, search, and date range filtering as the recovery list.
11. Clicking a zero-balance claim shall navigate to the claim detail page. If the claim is older than 60 days since reaching zero balance, the detail page shall display an appropriate message.

### Claims counts

12. The portal shall display the count of active claims in recovery for the acting creditor within a given date range.
13. The portal shall display the count of zero-balance claims for the acting creditor within a given date range.
14. Both count views shall allow the user to select a date range.

### Data loading

15. List data shall be loaded from `debt-service` through the creditor-portal BFF.
16. HTMX shall be used for progressive loading: the table body is loaded asynchronously after the page shell renders, with a loading indicator shown during fetch.
17. Pagination controls shall use HTMX to swap table content without full page reload.

### Access control

18. The claims lists and counts shall be accessible to all authenticated portal users with `CREDITOR_VIEWER` or `CREDITOR_EDITOR` roles.
19. Users with only `CREDITOR_RECONCILIATION` role shall not see claims lists.

## Layout and accessibility

20. All list pages shall use the SKAT standardlayout (skip link, header, breadcrumb, main content, footer) from `layout/default.html`.
21. Data tables shall use semantic HTML (`<table>`, `<thead>`, `<tbody>`, `<th scope="col">`) with proper caption or `aria-label`.
22. Pagination controls shall be keyboard-accessible and announce page changes to screen readers.
23. Monetary amounts shall be formatted with 2 decimal places using comma as decimal separator (Danish locale).
24. Dates shall be formatted as `dd.MM.yyyy`.
25. CPR numbers shall be censored (show only first 6 digits followed by `****`).
26. All user-facing text shall use message bundles (petition 021) with Danish and English translations.

## Data source mapping

| Old source | OpenDebt source |
|---|---|
| PSRM DKGetClaimsForClaimant | `debt-service` GET /api/v1/debts?creditorId={id}&status=IN_RECOVERY |
| PSRM DKGetZeroClaimsCountForClaimant | `debt-service` GET /api/v1/debts/count?creditorId={id}&balance=0 |
| PSRM DKGetClaimsCountForClaimant | `debt-service` GET /api/v1/debts/count?creditorId={id}&status=IN_RECOVERY |

## Constraints and assumptions

- The BFF handles creditor resolution and injects the acting creditor ID into backend calls (petition 010, petition 012).
- Person Registry is called by the BFF to resolve censored debtor display names when needed.
- This petition defines portal views, not the debt-service API contract.

## Out of scope

- Claim detail page (petition 030)
- Claim creation (petition 032)
- Backend debt-service API design
