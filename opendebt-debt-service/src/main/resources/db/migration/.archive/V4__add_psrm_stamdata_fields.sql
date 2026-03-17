-- V4: Add PSRM stamdata fields to debts table (W7-STAM-01)
-- Implements 22 stamdata field model from Gældsstyrelsen PSRM reference

-- Stamdata fields
ALTER TABLE debts ADD COLUMN hovedstol NUMERIC(15, 2);
ALTER TABLE debts ADD COLUMN fordringshaver_reference VARCHAR(50);
ALTER TABLE debts ADD COLUMN fordringsart VARCHAR(10);
ALTER TABLE debts ADD COLUMN fordring_kategori VARCHAR(5);
ALTER TABLE debts ADD COLUMN hovedfordrings_id UUID;
ALTER TABLE debts ADD COLUMN foraeldelsesdato DATE;
ALTER TABLE debts ADD COLUMN beskrivelse VARCHAR(100);
ALTER TABLE debts ADD COLUMN periode_fra DATE;
ALTER TABLE debts ADD COLUMN periode_til DATE;
ALTER TABLE debts ADD COLUMN stiftelsesdato DATE;
ALTER TABLE debts ADD COLUMN forfaldsdato DATE;
ALTER TABLE debts ADD COLUMN srb DATE;
ALTER TABLE debts ADD COLUMN bobehandling BOOLEAN;
ALTER TABLE debts ADD COLUMN domsdato DATE;
ALTER TABLE debts ADD COLUMN forligsdato DATE;
ALTER TABLE debts ADD COLUMN rente_regel VARCHAR(10);
ALTER TABLE debts ADD COLUMN rente_sats_kode VARCHAR(10);
ALTER TABLE debts ADD COLUMN mer_rente_sats NUMERIC(10, 4);
ALTER TABLE debts ADD COLUMN fordringsnote VARCHAR(500);
ALTER TABLE debts ADD COLUMN kundenote VARCHAR(500);
ALTER TABLE debts ADD COLUMN p_nummer VARCHAR(20);

-- Lifecycle state
ALTER TABLE debts ADD COLUMN lifecycle_state VARCHAR(20);
ALTER TABLE debts ADD COLUMN modtagelsestidspunkt TIMESTAMPTZ;

-- DebtType fields for PSRM
ALTER TABLE debt_types ADD COLUMN civilretlig BOOLEAN DEFAULT FALSE;
ALTER TABLE debt_types ADD COLUMN fordringstype_kode VARCHAR(20);

-- Indexes
CREATE INDEX idx_debt_hovedfordrings_id ON debts(hovedfordrings_id);
CREATE INDEX idx_debt_lifecycle_state ON debts(lifecycle_state);
CREATE INDEX idx_debt_srb ON debts(srb);
CREATE INDEX idx_debt_foraeldelsesdato ON debts(foraeldelsesdato);

-- Add same columns to history table
ALTER TABLE debts_history ADD COLUMN hovedstol NUMERIC(15, 2);
ALTER TABLE debts_history ADD COLUMN fordringshaver_reference VARCHAR(50);
ALTER TABLE debts_history ADD COLUMN fordringsart VARCHAR(10);
ALTER TABLE debts_history ADD COLUMN fordring_kategori VARCHAR(5);
ALTER TABLE debts_history ADD COLUMN hovedfordrings_id UUID;
ALTER TABLE debts_history ADD COLUMN foraeldelsesdato DATE;
ALTER TABLE debts_history ADD COLUMN beskrivelse VARCHAR(100);
ALTER TABLE debts_history ADD COLUMN periode_fra DATE;
ALTER TABLE debts_history ADD COLUMN periode_til DATE;
ALTER TABLE debts_history ADD COLUMN stiftelsesdato DATE;
ALTER TABLE debts_history ADD COLUMN forfaldsdato DATE;
ALTER TABLE debts_history ADD COLUMN srb DATE;
ALTER TABLE debts_history ADD COLUMN bobehandling BOOLEAN;
ALTER TABLE debts_history ADD COLUMN domsdato DATE;
ALTER TABLE debts_history ADD COLUMN forligsdato DATE;
ALTER TABLE debts_history ADD COLUMN rente_regel VARCHAR(10);
ALTER TABLE debts_history ADD COLUMN rente_sats_kode VARCHAR(10);
ALTER TABLE debts_history ADD COLUMN mer_rente_sats NUMERIC(10, 4);
ALTER TABLE debts_history ADD COLUMN fordringsnote VARCHAR(500);
ALTER TABLE debts_history ADD COLUMN kundenote VARCHAR(500);
ALTER TABLE debts_history ADD COLUMN p_nummer VARCHAR(20);
ALTER TABLE debts_history ADD COLUMN lifecycle_state VARCHAR(20);
ALTER TABLE debts_history ADD COLUMN modtagelsestidspunkt TIMESTAMPTZ;

-- Add to debt_types_history too
ALTER TABLE debt_types_history ADD COLUMN civilretlig BOOLEAN DEFAULT FALSE;
ALTER TABLE debt_types_history ADD COLUMN fordringstype_kode VARCHAR(20);

-- Disable versioning trigger to avoid PK conflicts from multiple updates on the same row
ALTER TABLE debts DISABLE TRIGGER debts_versioning_trigger;

-- Initialize lifecycle_state for existing debts
UPDATE debts SET lifecycle_state = 'OVERDRAGET' WHERE status IN ('IN_COLLECTION', 'ACTIVE');
UPDATE debts SET lifecycle_state = 'INDFRIET' WHERE status = 'PAID';
UPDATE debts SET lifecycle_state = 'AFSKREVET' WHERE status = 'WRITTEN_OFF';
UPDATE debts SET lifecycle_state = 'REGISTERED' WHERE status = 'PENDING';
UPDATE debts SET lifecycle_state = 'REGISTERED' WHERE lifecycle_state IS NULL;

-- Initialize hovedstol from principal_amount for existing debts
UPDATE debts SET hovedstol = principal_amount WHERE hovedstol IS NULL;

-- Initialize fordringsart as INDR for existing debts (PSRM only supports INDR)
UPDATE debts SET fordringsart = 'INDR' WHERE fordringsart IS NULL;

-- Initialize fordring_kategori as HF for existing debts
UPDATE debts SET fordring_kategori = 'HF' WHERE fordring_kategori IS NULL;

-- Re-enable versioning trigger
ALTER TABLE debts ENABLE TRIGGER debts_versioning_trigger;

-- Relax principal_amount constraint to allow 0-fordringer (W7-ZERO-01 preparation)
ALTER TABLE debts DROP CONSTRAINT IF EXISTS chk_principal_positive;
ALTER TABLE debts ADD CONSTRAINT chk_principal_non_negative CHECK (principal_amount >= 0);

-- Comments
COMMENT ON COLUMN debts.hovedstol IS 'Original principal amount (PSRM stamdata)';
COMMENT ON COLUMN debts.fordringshaver_reference IS 'Unique creditor reference number (PSRM stamdata)';
COMMENT ON COLUMN debts.fordringsart IS 'INDR (collection) or MODR (offsetting only)';
COMMENT ON COLUMN debts.fordring_kategori IS 'HF (hovedfordring) or UF (underfordring)';
COMMENT ON COLUMN debts.hovedfordrings_id IS 'Reference to parent hovedfordring for underfordringer';
COMMENT ON COLUMN debts.foraeldelsesdato IS 'Statute of limitations date';
COMMENT ON COLUMN debts.beskrivelse IS 'Free-text description max 100 chars, NO PII (GDPR)';
COMMENT ON COLUMN debts.srb IS 'Sidste rettidige betalingsdato';
COMMENT ON COLUMN debts.lifecycle_state IS 'PSRM fordring lifecycle state';
COMMENT ON COLUMN debts.modtagelsestidspunkt IS 'Timestamp when claim was received for collection';
