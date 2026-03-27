-- ============================================================================
-- V2: Drop deprecated debtor_person_id column from cases table
-- ============================================================================
-- The debtor_person_id column was deprecated in v2 (OIO Sag migration).
-- Debtor data is now stored in the case_parties table with role PRIMARY_DEBTOR.
-- The column has been read-only (insertable=false, updatable=false) since V3
-- of the original migration sequence and all active rows have been migrated.
-- ============================================================================

DROP INDEX IF EXISTS idx_case_debtor_person_id;

ALTER TABLE cases DROP COLUMN IF EXISTS debtor_person_id;
