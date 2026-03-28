# Petition 031: Fordringshaverportal -- Fordringer i hoering

## Summary

The Fordringshaverportal shall provide a list of claims in hearing (hoering) and a detailed view for each, allowing creditors to review hearing details and either approve or withdraw claims.

## Context and motivation

When a submitted claim has stamdata deviations from the indgangsfilter rules, it enters hearing status rather than being accepted or rejected outright. The creditor must review the hearing, understand the deviations, and decide whether to approve (with justification) or withdraw the claim. The legacy portal used PSRM hearing services. In OpenDebt, this is handled by `debt-service` and potentially `case-service`.

**Supersedes:** Fordringshaverportal petition 005 (old).
**References:** Petition 002 (fordring creation), Petition 006 (indsigelse), Petition 012 (BFF), Petition 015-018 (validation rules).

## Functional requirements

### Hearing claims list

1. The portal shall display a paginated table of claims in hearing for the acting creditor.
2. Each row shall display:
   - Fordrings-ID
   - Indberetningstidspunkt (reporting timestamp)
   - Skyldner-type and skyldner-ID (CPR censored)
   - Antal skyldnere (number of debtors)
   - Fordringshaver-reference
   - Fordringstype (claim type name)
   - Fejl (error description -- single error text, or "N fejl" if multiple)
   - Hoeringsstatus (mapped to human-readable Danish text)
   - Sags-ID (case ID)
   - Aktionskode (action code)
3. The list shall support sorting, searching (by fordrings-ID, CPR, CVR, SE), and date range filtering.
4. Clicking a row shall navigate to the hearing detail view.

### Hearing detail view

5. The hearing detail page shall display:
   - Fordringsstatus (mapped from status code to Danish text)
   - ID-numre: fordrings-ID, sags-ID, aktions-ID, fordringshaver-reference, hovedfordrings-ID
   - Fordringsinformation: fordringstype, fordringshaver-beskrivelse, indberetningstidspunkt, periode, stiftelsesdato
   - Fordringshaver-info: fordringshaver-ID, fordringshaver-navn
   - Beloeb: oprindelig hovedstol, modtaget beloeb
   - Aktionskode
   - Skyldnerliste med fejltyper per skyldner

### Write-up hearing claims

6. If the action code indicates an opskrivning (write-up), the detail view shall additionally show:
   - Opskrivningsbeloeb (write-up amount)
   - Opskrivningsaarsag (write-up reason)
   - Reference-aktions-ID
   - Aendret oprindelig hovedstol (changed original principal) -- only for FEJLAGTIG_HOVEDSTOL_INDBERETNING
7. The following write-up action codes shall be recognized:
   - OPSKRIVNING_REGULERING
   - FEJLAGTIG_HOVEDSTOL_INDBERETNING
   - OPSKRIVNING_OMGJORT_NEDSKRIVNING_REGULERING
   - OPSKRIVNING_ANNULLERET_NEDSKRIVNING_INDBETALING

### Approve or withdraw hearing case

8. Editors shall be able to approve a hearing claim with a written justification (aarsag).
9. Editors shall be able to withdraw (fortryd) a hearing claim with a reason.
10. Both actions shall be submitted to `debt-service` through the BFF.
11. After approval, the status shall change to "Afventer Gaeldsstyrelsen" (pending review by Gaeldsstyrelsen).
12. All approve/withdraw actions shall be logged to the audit log.

### Hearing workflow context

13. A claim in hearing is NOT received for inddrivelse until approved and accepted -- the creditor's own foraeldelsesregler apply during this period.
14. Gaeldsstyrelsen treats all claims in hearing within 14 days of approval.
15. After Gaeldsstyrelsen review, the outcome is: godkendt (accepted), afvist (rejected), or tilpas indgangsfilter (adjusted).

### Access control

16. The hearing list and detail view shall be accessible to all users with `CREDITOR_VIEWER` or `CREDITOR_EDITOR` roles.
17. Approve and withdraw actions shall require `CREDITOR_EDITOR` role with `allow_portal_actions` permission from the creditor agreement (petition 008).

## Layout and accessibility

18. All pages shall use the SKAT standardlayout.
19. The hearing detail page breadcrumb: Forside > Fordringer i hoering > [Fordrings-ID].
20. Error descriptions shall be displayed in a clearly visible alert component.
21. The approve/withdraw form shall use accessible form patterns with labels, validation feedback, and confirmation dialog.
22. All text shall use message bundles (petition 021).

## Data source mapping

| Old source | OpenDebt source |
|---|---|
| PSRM DKGetHearingClaimsForClaimant | `debt-service` GET /api/v1/debts?creditorId={id}&status=HEARING |
| PSRM DKGetHearingClaimInfo | `debt-service` GET /api/v1/debts/{id}/hearing |
| PSRM DKApproveOrWithdrawHearingCase | `debt-service` POST /api/v1/debts/{id}/hearing/approve or /withdraw |

## Constraints and assumptions

- The hearing workflow is managed by `debt-service` (and potentially `case-service` for workflow orchestration via Flowable).
- This petition defines the portal views and user flows, not the backend API or workflow engine design.
- **G.A.1.4.3 (Opskrivning modtaget under høring):** If a fordringshaver submits an opskrivningsfordring while the related (main) claim is still in høring, the opskrivningsfordring is not considered received for inddrivelse until the høring is resolved (confirmed or corrected). Interest accrual on the opskrivningsbeløb begins at the høring resolution date, not at the opskrivning submission date. The portal must communicate this timing rule to fordringshavere — e.g., via an informational notice on the høring detail view when an opskrivning is pending for a claim currently in høring.

## Out of scope

- Backend hearing workflow implementation
- Caseworker review UI (internal system)
- Indgangsfilter rule definitions
