-- OpenDebt Payment Service - Bi-Temporal Ledger Tables for Double-Entry Bookkeeping
-- See ADR-0018 for design rationale
--
-- Bi-temporal model:
--   effective_date = when the economic event applies (value date)
--   posting_date   = when the entry was recorded in the system
--
-- Corrections use the storno pattern: reversal entries cancel the original,
-- then new correct entries are posted. Entries are never modified or deleted.

-- ============================================================================
-- LEDGER ENTRIES (Immutable bi-temporal double-entry postings)
-- ============================================================================

CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID NOT NULL,
    debt_id UUID NOT NULL,
    account_code VARCHAR(10) NOT NULL,
    account_name VARCHAR(100) NOT NULL,
    entry_type VARCHAR(10) NOT NULL CHECK (entry_type IN ('DEBIT', 'CREDIT')),
    amount NUMERIC(15, 2) NOT NULL,

    -- Bi-temporal dates
    effective_date DATE NOT NULL,
    posting_date DATE NOT NULL DEFAULT CURRENT_DATE,

    reference VARCHAR(200),
    description VARCHAR(500),

    -- Storno support: references the transaction being reversed
    reversal_of_transaction_id UUID,

    -- Category for filtering and reporting
    entry_category VARCHAR(20) NOT NULL CHECK (entry_category IN (
        'DEBT_REGISTRATION', 'PAYMENT', 'INTEREST_ACCRUAL',
        'OFFSETTING', 'WRITE_OFF', 'REFUND', 'STORNO', 'CORRECTION'
    )),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_ledger_amount_positive CHECK (amount > 0)
);

-- ============================================================================
-- DEBT EVENT TIMELINE (Source of truth for replaying corrections)
-- ============================================================================

CREATE TABLE debt_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    debt_id UUID NOT NULL,
    event_type VARCHAR(30) NOT NULL CHECK (event_type IN (
        'DEBT_REGISTERED', 'PAYMENT_RECEIVED', 'INTEREST_ACCRUED',
        'OFFSETTING_EXECUTED', 'WRITE_OFF', 'REFUND',
        'UDLAEG_REGISTERED', 'UDLAEG_CORRECTED', 'CORRECTION'
    )),
    effective_date DATE NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    corrects_event_id UUID REFERENCES debt_events(id),
    reference VARCHAR(200),
    description VARCHAR(500),
    ledger_transaction_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- INDEXES
-- ============================================================================

-- Ledger entry indexes
CREATE INDEX idx_ledger_transaction_id ON ledger_entries(transaction_id);
CREATE INDEX idx_ledger_debt_id ON ledger_entries(debt_id);
CREATE INDEX idx_ledger_account_code ON ledger_entries(account_code);
CREATE INDEX idx_ledger_created_at ON ledger_entries(created_at);
CREATE INDEX idx_ledger_entry_type ON ledger_entries(entry_type);
CREATE INDEX idx_ledger_effective_date ON ledger_entries(effective_date);
CREATE INDEX idx_ledger_posting_date ON ledger_entries(posting_date);
CREATE INDEX idx_ledger_reversal_of ON ledger_entries(reversal_of_transaction_id);
CREATE INDEX idx_ledger_category ON ledger_entries(entry_category);
CREATE INDEX idx_ledger_debt_effective ON ledger_entries(debt_id, effective_date);
CREATE INDEX idx_ledger_debt_category ON ledger_entries(debt_id, entry_category);

-- Debt event indexes
CREATE INDEX idx_debt_event_debt_id ON debt_events(debt_id);
CREATE INDEX idx_debt_event_effective_date ON debt_events(effective_date);
CREATE INDEX idx_debt_event_type ON debt_events(event_type);
CREATE INDEX idx_debt_event_debt_effective ON debt_events(debt_id, effective_date);

-- ============================================================================
-- AUDIT TRIGGERS (reuses existing audit infrastructure from V1)
-- ============================================================================

CREATE TRIGGER ledger_entries_audit_trigger
    AFTER INSERT ON ledger_entries
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

CREATE TRIGGER debt_events_audit_trigger
    AFTER INSERT ON debt_events
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

-- ============================================================================
-- CHART OF ACCOUNTS (reference table)
-- ============================================================================

CREATE TABLE chart_of_accounts (
    code VARCHAR(10) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    account_type VARCHAR(20) NOT NULL CHECK (account_type IN ('ASSET', 'LIABILITY', 'REVENUE', 'EXPENSE')),
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO chart_of_accounts (code, name, account_type, description) VALUES
    ('1000', 'Fordringer', 'ASSET', 'Udestaaende fordringer til inddrivelse'),
    ('1100', 'Renter tilgodehavende', 'ASSET', 'Tilskrevne renter endnu ikke betalt'),
    ('2000', 'SKB Bankkonto', 'ASSET', 'Statens Koncernbetalinger bankkonto'),
    ('3000', 'Indrivelsesindtaegter', 'REVENUE', 'Indtaegter fra inddrevne fordringer'),
    ('3100', 'Renteindtaegter', 'REVENUE', 'Indtaegter fra renter'),
    ('4000', 'Tab paa fordringer', 'EXPENSE', 'Afskrevne fordringer'),
    ('5000', 'Modregning clearing', 'LIABILITY', 'Clearingkonto for modregning');

-- ============================================================================
-- VIEWS
-- ============================================================================

-- Current balance per account (excludes reversed entries via storno netting)
CREATE VIEW ledger_balance AS
SELECT
    account_code,
    account_name,
    SUM(CASE WHEN entry_type = 'DEBIT' THEN amount ELSE 0 END) AS total_debit,
    SUM(CASE WHEN entry_type = 'CREDIT' THEN amount ELSE 0 END) AS total_credit,
    SUM(CASE WHEN entry_type = 'DEBIT' THEN amount ELSE -amount END) AS balance
FROM ledger_entries
GROUP BY account_code, account_name;

-- Balance per account as of a specific effective date (use with WHERE effective_date <= ?)
CREATE VIEW ledger_balance_by_effective_date AS
SELECT
    account_code,
    account_name,
    effective_date,
    SUM(CASE WHEN entry_type = 'DEBIT' THEN amount ELSE 0 END) AS total_debit,
    SUM(CASE WHEN entry_type = 'CREDIT' THEN amount ELSE 0 END) AS total_credit,
    SUM(CASE WHEN entry_type = 'DEBIT' THEN amount ELSE -amount END) AS balance
FROM ledger_entries
GROUP BY account_code, account_name, effective_date;

-- Debt balance timeline: shows running balance per debt over time
CREATE VIEW debt_balance_timeline AS
SELECT
    debt_id,
    effective_date,
    entry_category,
    SUM(CASE
        WHEN account_code = '1000' AND entry_type = 'DEBIT' THEN amount
        WHEN account_code = '1000' AND entry_type = 'CREDIT' THEN -amount
        ELSE 0
    END) AS receivable_change,
    SUM(CASE
        WHEN account_code = '1100' AND entry_type = 'DEBIT' THEN amount
        WHEN account_code = '1100' AND entry_type = 'CREDIT' THEN -amount
        ELSE 0
    END) AS interest_change
FROM ledger_entries
GROUP BY debt_id, effective_date, entry_category
ORDER BY debt_id, effective_date;

COMMENT ON TABLE ledger_entries IS 'Immutable bi-temporal double-entry postings with storno support (see ADR-0018)';
COMMENT ON TABLE debt_events IS 'Immutable event timeline for debt lifecycle, enables retroactive correction replay';
COMMENT ON TABLE chart_of_accounts IS 'Kontoplan aligned with statsligt regnskab';
COMMENT ON VIEW ledger_balance IS 'Current account balances derived from all ledger entries';
COMMENT ON VIEW ledger_balance_by_effective_date IS 'Account balances broken down by effective date';
COMMENT ON VIEW debt_balance_timeline IS 'Running balance changes per debt over time';
