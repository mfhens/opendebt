# Petition 032: Fordringshaverportal -- Afviste fordringer

## Summary

The Fordringshaverportal shall provide a list of rejected claims (afviste fordringer) and a detailed view showing rejection reasons, error messages, and debtor information, enabling creditors to understand and correct failed submissions.

## Context and motivation

When a submitted claim fails validation or is rejected by a caseworker, the creditor must understand why. The legacy portal retrieved rejected claims from NyMF `AfvistAktionerSoeg`. In OpenDebt, validation is performed by `debt-service` using the rules defined in petitions 015-018, and rejection data is persisted in `debt-service`.

**Supersedes:** Fordringshaverportal petition 006 (old).
**References:** Petition 015-018 (validation rules), Petition 002 (fordring creation), Petition 012 (BFF).

## Functional requirements

### Rejected claims list

1. The portal shall display a paginated table of rejected claims for the acting creditor.
2. The list shall use the same tabular, search, sort, and pagination patterns as the other list views (petition 029).
3. Rejected claims data is retrieved from `debt-service` through the BFF.

### Rejected claim detail view

4. The detail page shall display:
   - Aktionsstatus (action status)
   - Afvisningsaarsag (rejection reason text)
   - ID-numre: fordrings-ID, fordringshaver-reference
   - Fordringsinformation: fordringstype, fordringshaver-beskrivelse, indberetningstidspunkt, periode, stiftelsesdato
   - Renteinformation: renteregelnummer, rentesatskode
   - Fordringshaver-info: fordringshaver-ID, fordringshaver-navn
   - Beloeb: oprindeligt beloeb, fordringsbeloeb
   - Fejlbeskrivelser (list of validation error descriptions with error codes)
   - Sagsbehandler-bemaerkning (caseworker remark) -- if present

### Debtor information in rejected claims

5. Each rejected claim shall display its debtor list with:
   - Skyldner-ID (CPR censored, CVR, SE, or AKR)
   - Forfaldsdato, sidste rettidige betalingsdato, foraeldelsesdato
   - Domsdato (court date), forligsdato (settlement date) -- if applicable
   - Bobehandling flag (estate processing)
   - Skyldner-note
6. CPR numbers shall be censored before display (first 6 digits only).
7. A configurable flag shall control whether debtor details are shown at all.

### Error code display

8. Validation errors shall display both the numeric error code and the Danish description.
9. Error codes correspond to the validation rules in petitions 015-018 (e.g., 152 = ugyldig valuta, 411 = forkert fordringsart).

### Access control

10. The rejected claims list and detail shall be accessible to all users with `CREDITOR_VIEWER` or `CREDITOR_EDITOR` roles.

## Layout and accessibility

11. All pages shall use the SKAT standardlayout.
12. The detail page breadcrumb: Forside > Afviste fordringer > [Fordrings-ID].
13. Error descriptions shall use an `skat-alert skat-alert--error` component or similar error styling.
14. All text shall use message bundles (petition 021).

## Data source mapping

| Old source | OpenDebt source |
|---|---|
| NyMF AfvistAktionerSoeg | `debt-service` GET /api/v1/debts?creditorId={id}&status=REJECTED |
| NyMF rejected claim detail | `debt-service` GET /api/v1/debts/{id}/rejection |

## Constraints and assumptions

- The BFF resolves debtor display information from person-registry.
- Rejection data is produced by the validation pipeline (petitions 015-018) within debt-service.
- This petition defines the portal view, not the backend API.

## Out of scope

- Resubmission flow from the rejected claim detail
- Backend validation rule implementation
- Caseworker rejection workflow
