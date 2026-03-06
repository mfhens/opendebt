-- V3: Support OCR-based payment matching (petition001)
-- Adds OCR-linje for tracking the payment reference used for matching.
-- Makes case_id and debtor fields nullable for incoming CREMUL payments
-- that have not yet been associated with a case.

-- Add OCR-linje column for payment matching
ALTER TABLE payments ADD COLUMN ocr_line VARCHAR(50);
CREATE INDEX idx_payment_ocr_line ON payments(ocr_line);

-- Make case_id nullable for incoming payments pending matching
ALTER TABLE payments ALTER COLUMN case_id DROP NOT NULL;

-- Make debtor fields nullable for incoming CREMUL payments
ALTER TABLE payments ALTER COLUMN debtor_identifier DROP NOT NULL;
ALTER TABLE payments ALTER COLUMN debtor_identifier_type DROP NOT NULL;
ALTER TABLE payments ALTER COLUMN debtor_role DROP NOT NULL;

-- Add column to history table
ALTER TABLE payments_history ADD COLUMN ocr_line VARCHAR(50);

COMMENT ON COLUMN payments.ocr_line IS 'Betalingsservice OCR-linje used for automatic matching';
