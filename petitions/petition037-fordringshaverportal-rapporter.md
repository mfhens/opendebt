# Petition 037: Fordringshaverportal -- Rapporter

## Summary

The Fordringshaverportal shall allow creditors to list and download monthly reports, with secure access control ensuring creditors can only access their own reports.

## Context and motivation

Gaeldsstyrelsen generates monthly reports per creditor and stores them as zip files. The legacy portal used encrypted S3 keys and AES encryption to prevent cross-creditor access. In OpenDebt, reports are managed by a storage/reporting backend, and access control is enforced at the BFF and service level rather than through client-side key encryption.

**Supersedes:** Fordringshaverportal petition 011 (old).
**References:** Petition 008 (creditor data model), Petition 012 (BFF).

## Functional requirements

### List reports

1. The portal shall display a list of available reports for the acting creditor for a selected year and month.
2. The year/month selector shall allow the creditor to browse available periods.
3. Each report entry shall show: report name/type and availability status.
4. Reconciliation summary files shall be filtered out from the report list (these are accessed via the reconciliation module, petition 036).

### Download report

5. The creditor shall be able to download individual reports.
6. Downloaded files shall be served as `application/zip` with an appropriate `Content-Disposition` filename.
7. Download progress shall be indicated to the user.
8. All report downloads shall be logged to the audit log.

### Security

9. The BFF shall enforce that a creditor can only access reports belonging to their own creditor identity.
10. Report access shall be validated against the acting creditor context resolved through channel binding (petition 010).
11. No client-side key encryption scheme is needed -- access control is enforced server-side by the BFF and backend service.

### Access control

12. Reports shall be accessible to users with `CREDITOR_VIEWER`, `CREDITOR_EDITOR`, `CREDITOR_RECONCILIATION`, or `CREDITOR_SUPPORT` roles.

## Layout and accessibility

13. The reports page shall use the SKAT standardlayout.
14. Breadcrumb: Forside > Rapporter.
15. Year/month selector shall use standard HTML `<select>` elements with proper labels.
16. Download buttons shall be keyboard-accessible with appropriate `aria-label`.
17. Download status shall use `aria-live` to announce completion to screen readers.
18. All text shall use message bundles (petition 021).

## Data source mapping

| Old source | OpenDebt source |
|---|---|
| AWS S3 ListObjects | Reporting service GET /api/v1/reports?creditorId={id}&year=...&month=... |
| AWS S3 GetObject | Reporting service GET /api/v1/reports/{reportId}/download |
| PSRM TaxTypeClient | Not needed -- creditor context resolved via BFF |
| PSRM ReportDownloadLogClient | Audit log via OpenDebt audit infrastructure (ADR-0022) |

## Constraints and assumptions

- Report storage and generation is a backend concern outside this petition.
- The BFF handles creditor resolution and access control.
- The legacy AES key encryption for S3 keys is replaced by server-side access control.

## Out of scope

- Report generation pipeline
- Backend storage design for reports
- Report content format definitions
