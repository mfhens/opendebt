-- V5: Add accounting_target to ledger entries (petition 045)

ALTER TABLE ledger_entries
    ADD COLUMN IF NOT EXISTS accounting_target VARCHAR(20);

COMMENT ON COLUMN ledger_entries.accounting_target IS 'Who receives the money: FORDRINGSHAVER (interest on principal) or STATEN (fees and interest on fees)';
