-- V2: Support OCR-based payment matching (petition001)
-- Adds OCR-linje for matching incoming payments to debts via Betalingsservice
-- Adds outstanding_balance for tracking remaining debt amount after payments

-- Add OCR-linje column with unique index (allows multiple NULLs)
ALTER TABLE debts ADD COLUMN ocr_line VARCHAR(50);
CREATE UNIQUE INDEX idx_debt_ocr_line ON debts(ocr_line) WHERE ocr_line IS NOT NULL;

-- Add outstanding balance for tracking remaining debt amount
ALTER TABLE debts ADD COLUMN outstanding_balance NUMERIC(15, 2);

-- Initialize outstanding_balance from current amounts
UPDATE debts SET outstanding_balance = COALESCE(principal_amount, 0)
    + COALESCE(interest_amount, 0)
    + COALESCE(fees_amount, 0)
WHERE outstanding_balance IS NULL;

-- Add columns to history table as well
ALTER TABLE debts_history ADD COLUMN ocr_line VARCHAR(50);
ALTER TABLE debts_history ADD COLUMN outstanding_balance NUMERIC(15, 2);

COMMENT ON COLUMN debts.ocr_line IS 'Betalingsservice OCR-linje for automatic payment matching';
COMMENT ON COLUMN debts.outstanding_balance IS 'Remaining balance after payments (write-downs)';
