-- V7: Rename Danish database columns and tables to English
-- Aligns with begrebsmodel v3 section 2.1 (docs/begrebsmodel/)
-- Implementation language rule: all code and DB schemas in English.

-- Disable versioning trigger during column renames
ALTER TABLE debts DISABLE TRIGGER debts_versioning_trigger;

-- ============================================================================
-- debts table: rename Danish columns to English
-- ============================================================================
ALTER TABLE debts RENAME COLUMN hovedstol TO principal;
ALTER TABLE debts RENAME COLUMN fordringshaver_reference TO creditor_reference;
ALTER TABLE debts RENAME COLUMN fordringsart TO claim_art;
ALTER TABLE debts RENAME COLUMN fordring_kategori TO claim_category;
ALTER TABLE debts RENAME COLUMN hovedfordrings_id TO parent_claim_id;
ALTER TABLE debts RENAME COLUMN foraeldelsesdato TO limitation_date;
ALTER TABLE debts RENAME COLUMN beskrivelse TO description;
ALTER TABLE debts RENAME COLUMN periode_fra TO period_from;
ALTER TABLE debts RENAME COLUMN periode_til TO period_to;
ALTER TABLE debts RENAME COLUMN stiftelsesdato TO inception_date;
ALTER TABLE debts RENAME COLUMN forfaldsdato TO payment_deadline;
ALTER TABLE debts RENAME COLUMN srb TO last_payment_date;
ALTER TABLE debts RENAME COLUMN bobehandling TO estate_processing;
ALTER TABLE debts RENAME COLUMN domsdato TO judgment_date;
ALTER TABLE debts RENAME COLUMN forligsdato TO settlement_date;
ALTER TABLE debts RENAME COLUMN rente_regel TO interest_rule;
ALTER TABLE debts RENAME COLUMN rente_sats_kode TO interest_rate_code;
ALTER TABLE debts RENAME COLUMN mer_rente_sats TO additional_interest_rate;
ALTER TABLE debts RENAME COLUMN fordringsnote TO claim_note;
ALTER TABLE debts RENAME COLUMN kundenote TO customer_note;
ALTER TABLE debts RENAME COLUMN p_nummer TO p_number;
ALTER TABLE debts RENAME COLUMN modtagelsestidspunkt TO received_at;

-- ============================================================================
-- debts_history table: same renames
-- ============================================================================
ALTER TABLE debts_history RENAME COLUMN hovedstol TO principal;
ALTER TABLE debts_history RENAME COLUMN fordringshaver_reference TO creditor_reference;
ALTER TABLE debts_history RENAME COLUMN fordringsart TO claim_art;
ALTER TABLE debts_history RENAME COLUMN fordring_kategori TO claim_category;
ALTER TABLE debts_history RENAME COLUMN hovedfordrings_id TO parent_claim_id;
ALTER TABLE debts_history RENAME COLUMN foraeldelsesdato TO limitation_date;
ALTER TABLE debts_history RENAME COLUMN beskrivelse TO description;
ALTER TABLE debts_history RENAME COLUMN periode_fra TO period_from;
ALTER TABLE debts_history RENAME COLUMN periode_til TO period_to;
ALTER TABLE debts_history RENAME COLUMN stiftelsesdato TO inception_date;
ALTER TABLE debts_history RENAME COLUMN forfaldsdato TO payment_deadline;
ALTER TABLE debts_history RENAME COLUMN srb TO last_payment_date;
ALTER TABLE debts_history RENAME COLUMN bobehandling TO estate_processing;
ALTER TABLE debts_history RENAME COLUMN domsdato TO judgment_date;
ALTER TABLE debts_history RENAME COLUMN forligsdato TO settlement_date;
ALTER TABLE debts_history RENAME COLUMN rente_regel TO interest_rule;
ALTER TABLE debts_history RENAME COLUMN rente_sats_kode TO interest_rate_code;
ALTER TABLE debts_history RENAME COLUMN mer_rente_sats TO additional_interest_rate;
ALTER TABLE debts_history RENAME COLUMN fordringsnote TO claim_note;
ALTER TABLE debts_history RENAME COLUMN kundenote TO customer_note;
ALTER TABLE debts_history RENAME COLUMN p_nummer TO p_number;
ALTER TABLE debts_history RENAME COLUMN modtagelsestidspunkt TO received_at;

-- ============================================================================
-- debt_types table: rename Danish columns
-- ============================================================================
ALTER TABLE debt_types RENAME COLUMN civilretlig TO civil_law;
ALTER TABLE debt_types RENAME COLUMN fordringstype_kode TO claim_type_code;

-- debt_types_history
ALTER TABLE debt_types_history RENAME COLUMN civilretlig TO civil_law;
ALTER TABLE debt_types_history RENAME COLUMN fordringstype_kode TO claim_type_code;

-- ============================================================================
-- overdragelse_events table: rename to English
-- ============================================================================
ALTER TABLE overdragelse_events RENAME COLUMN fordringshaver_id TO creditor_id;
ALTER TABLE overdragelse_events RENAME COLUMN modtager_id TO recipient_id;
ALTER TABLE overdragelse_events RENAME COLUMN tidspunkt TO occurred_at;
ALTER TABLE overdragelse_events RENAME TO claim_lifecycle_events;

-- Rename indexes to match new column names
ALTER INDEX IF EXISTS idx_debt_hovedfordrings_id RENAME TO idx_debt_parent_claim_id;
ALTER INDEX IF EXISTS idx_overdragelse_debt_id RENAME TO idx_lifecycle_event_debt_id;
ALTER INDEX IF EXISTS idx_overdragelse_tidspunkt RENAME TO idx_lifecycle_event_occurred_at;

-- Re-enable versioning trigger
ALTER TABLE debts ENABLE TRIGGER debts_versioning_trigger;

-- Update comments
COMMENT ON COLUMN debts.principal IS 'Original principal amount (PSRM stamdata)';
COMMENT ON COLUMN debts.creditor_reference IS 'Unique creditor reference number';
COMMENT ON COLUMN debts.claim_art IS 'INDR (collection) or MODR (set-off only)';
COMMENT ON COLUMN debts.claim_category IS 'HF (main claim) or UF (sub-claim)';
COMMENT ON COLUMN debts.parent_claim_id IS 'Reference to parent main claim for sub-claims';
COMMENT ON COLUMN debts.limitation_date IS 'Statute of limitations date';
COMMENT ON COLUMN debts.description IS 'Free-text description max 100 chars, NO PII (GDPR)';
COMMENT ON COLUMN debts.last_payment_date IS 'Last due payment date (Payment Deadline)';
COMMENT ON COLUMN debts.received_at IS 'Timestamp when claim was received for collection';
COMMENT ON TABLE claim_lifecycle_events IS 'Audit trail for claim lifecycle transitions';
