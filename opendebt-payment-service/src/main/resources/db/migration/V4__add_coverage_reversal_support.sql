-- V4: Add COVERAGE_REVERSED event type and COVERAGE_REVERSAL ledger category
-- Required for crossing-transaction detection (petition 041)

-- Extend debt_events event_type CHECK to include COVERAGE_REVERSED
ALTER TABLE debt_events DROP CONSTRAINT IF EXISTS debt_events_event_type_check;
ALTER TABLE debt_events ADD CONSTRAINT debt_events_event_type_check CHECK (event_type IN (
    'DEBT_REGISTERED', 'PAYMENT_RECEIVED', 'INTEREST_ACCRUED',
    'OFFSETTING_EXECUTED', 'WRITE_OFF', 'REFUND',
    'UDLAEG_REGISTERED', 'UDLAEG_CORRECTED', 'CORRECTION',
    'COVERAGE_REVERSED'
));

-- Extend ledger_entries entry_category CHECK to include COVERAGE_REVERSAL
ALTER TABLE ledger_entries DROP CONSTRAINT IF EXISTS ledger_entries_entry_category_check;
ALTER TABLE ledger_entries ADD CONSTRAINT ledger_entries_entry_category_check CHECK (entry_category IN (
    'DEBT_REGISTRATION', 'PAYMENT', 'INTEREST_ACCRUAL',
    'OFFSETTING', 'WRITE_OFF', 'REFUND', 'STORNO', 'CORRECTION',
    'COVERAGE_REVERSAL'
));

-- Relax positive-amount constraint to allow zero-amount marker entries (coverage reversal)
ALTER TABLE ledger_entries DROP CONSTRAINT IF EXISTS chk_ledger_amount_positive;
ALTER TABLE ledger_entries ADD CONSTRAINT chk_ledger_amount_non_negative CHECK (amount >= 0);
