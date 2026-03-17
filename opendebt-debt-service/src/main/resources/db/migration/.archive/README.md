# Archived Migrations

This folder contains the original V1-V7 migrations that were consolidated into `V1__baseline.sql` on 2026-03-17.

## Migration History

- **V1** - Initial schema with audit infrastructure, debt_types, debts tables, temporal versioning
- **V2** - Added OCR line and outstanding_balance for payment matching
- **V3** - Seed demo debt types and sample debts
- **V4** - Added PSRM stamdata fields (22 fields from Gældsstyrelsen reference)
- **V5** - Created overdragelse_events audit trail table
- **V6** - Created høring workflow table
- **V7** - Renamed Danish columns to English (begrebsmodel v3 compliance)

## Why Consolidated?

During rapid development phase, we accumulated 7 migrations quickly. For cleaner baseline going forward, these were merged into a single V1__baseline.sql that represents the complete current schema.

## For Reference Only

These files are kept for historical reference. Do not attempt to run them - they are superseded by V1__baseline.sql.
