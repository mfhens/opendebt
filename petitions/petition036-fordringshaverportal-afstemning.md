# Petition 036: Fordringshaverportal -- Afstemning (reconciliation)

## Summary

The Fordringshaverportal shall provide reconciliation functionality allowing creditors to view reconciliation periods, review basis data (tilgang, tilbagekaldt, opskrevet, nedskrevet), and submit reconciliation responses confirming agreement or documenting differences.

## Context and motivation

Periodically, Gaeldsstyrelsen performs reconciliation exercises with creditors to ensure both parties agree on outstanding amounts. Creditors must review the basis data and submit their response. The legacy portal used PSRM reconciliation services and S3 for basis data files. In OpenDebt, a reconciliation module in `debt-service` (or a future dedicated reconciliation service) manages this, with basis data stored in OpenDebt's own storage.

**Supersedes:** Fordringshaverportal petition 010 (old).
**References:** Petition 008 (creditor data model), Petition 012 (BFF).

## Functional requirements

### Reconciliation list

1. The portal shall display a searchable, filterable list of reconciliation periods for the acting creditor.
2. Filter parameters shall include:
   - Status (ACTIVE, CLOSED, etc.)
   - Period end date range (from, to)
   - Reconciliation start date range (from, to)
   - Reconciliation end date range (from, to)
3. Each reconciliation entry shall show summary information including status, period, and whether a response has been submitted.

### Reconciliation detail view

4. The detail view shall display:
   - Reconciliation status
   - Year and month of the reconciliation period
   - Previous response (if already submitted)
5. For ACTIVE reconciliations, the detail view shall additionally display the basis data:
   - Tilgang (influx amount)
   - Tilbagekaldt (recall amount)
   - Opskrevet (write-up amount)
   - Nedskrevet (write-down amount)

### Basis data

6. Basis data shall be retrieved from OpenDebt's storage backend.
7. All amounts shall be displayed in DKK with 2 decimal places.
8. If no basis data exists for the period, the view shall display zero amounts.

### Reconciliation response submission

9. For ACTIVE reconciliations, the creditor shall be able to submit a response containing:
   - Forklaret difference (difference explained)
   - Uforklaret difference (difference unexplained)
   - Total difference
10. Validation shall enforce: forklaret + uforklaret == total.
11. The submitted basis data shall be tamper-protected (the BFF verifies basis data has not been modified client-side).
12. Only ACTIVE reconciliations may receive responses.
13. Responses shall be submitted to `debt-service` through the BFF.
14. Service errors shall be propagated to the user as Danish messages.

### Access control

15. The reconciliation list and detail shall be accessible to users with `CREDITOR_RECONCILIATION` or `CREDITOR_SUPPORT` roles.
16. Response submission shall require `CREDITOR_RECONCILIATION` role.

## Layout and accessibility

17. All pages shall use the SKAT standardlayout.
18. Breadcrumb: Forside > Afstemning > [Period].
19. The response form shall use accessible patterns with labels, validation feedback, and a confirmation step.
20. Financial tables shall use semantic HTML with proper scope attributes.
21. All text shall use message bundles (petition 021).

## Data source mapping

| Old source | OpenDebt source |
|---|---|
| PSRM DKGetReconciliationList | `debt-service` GET /api/v1/reconciliations?creditorId={id} |
| PSRM DKGetReconciliationSingle | `debt-service` GET /api/v1/reconciliations/{id} |
| PSRM DKReconcPartResponse | `debt-service` POST /api/v1/reconciliations/{id}/response |
| AWS S3 (basis totals CSV) | OpenDebt storage GET /api/v1/reconciliations/{id}/basis |

## Constraints and assumptions

- Reconciliation module design (storage, basis data generation) is a backend concern outside this petition.
- The BFF resolves creditor context and enforces role checks.
- This petition defines the portal views and user flows.

## Out of scope

- Reconciliation basis data generation process
- Backend reconciliation storage design
- Reconciliation reporting and analytics
