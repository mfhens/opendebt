-- ============================================================================
-- OpenDebt Payment Service - Consolidated Baseline Schema
-- ============================================================================
-- Database: PostgreSQL 16
-- Consolidates V1-V6 migrations into single baseline
-- Final state is identical to a fully-migrated database.
-- Dev databases must be dropped and recreated after this squash.
-- ============================================================================

-- ============================================================================
-- EXTENSIONS
-- ============================================================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================================
-- AUDIT INFRASTRUCTURE
-- ============================================================================

CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    table_name VARCHAR(100) NOT NULL,
    record_id UUID NOT NULL,
    operation VARCHAR(10) NOT NULL CHECK (operation IN ('INSERT', 'UPDATE', 'DELETE')),
    old_values JSONB,
    new_values JSONB,
    changed_fields TEXT[],
    db_user VARCHAR(100) NOT NULL DEFAULT current_user,
    application_user VARCHAR(100),
    client_ip INET,
    client_application VARCHAR(200),
    transaction_id BIGINT DEFAULT txid_current(),
    timestamp TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_log_table ON audit_log(table_name);
CREATE INDEX idx_audit_log_record ON audit_log(record_id);
CREATE INDEX idx_audit_log_timestamp ON audit_log(timestamp);

CREATE OR REPLACE FUNCTION set_audit_context(p_user VARCHAR, p_ip INET DEFAULT NULL, p_app VARCHAR DEFAULT NULL)
RETURNS VOID AS $$
BEGIN
    PERFORM set_config('opendebt.audit_user', p_user, true);
    PERFORM set_config('opendebt.audit_ip', COALESCE(p_ip::text, ''), true);
    PERFORM set_config('opendebt.audit_app', COALESCE(p_app, ''), true);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION audit_trigger_function()
RETURNS TRIGGER AS $$
DECLARE
    v_old_values JSONB; v_new_values JSONB; v_changed_fields TEXT[];
    v_record_id UUID; v_app_user VARCHAR(100); v_client_ip INET; v_client_app VARCHAR(200);
BEGIN
    v_app_user := NULLIF(current_setting('opendebt.audit_user', true), '');
    v_client_ip := NULLIF(current_setting('opendebt.audit_ip', true), '')::INET;
    v_client_app := NULLIF(current_setting('opendebt.audit_app', true), '');

    IF TG_OP = 'DELETE' THEN v_record_id := OLD.id; v_old_values := to_jsonb(OLD);
    ELSIF TG_OP = 'INSERT' THEN v_record_id := NEW.id; v_new_values := to_jsonb(NEW);
    ELSIF TG_OP = 'UPDATE' THEN
        v_record_id := NEW.id; v_old_values := to_jsonb(OLD); v_new_values := to_jsonb(NEW);
        SELECT array_agg(key) INTO v_changed_fields FROM jsonb_each(v_new_values) n
        FULL OUTER JOIN jsonb_each(v_old_values) o USING (key) WHERE n.value IS DISTINCT FROM o.value;
    END IF;

    INSERT INTO audit_log (table_name, record_id, operation, old_values, new_values,
                          changed_fields, application_user, client_ip, client_application)
    VALUES (TG_TABLE_NAME, v_record_id, TG_OP, v_old_values, v_new_values,
            v_changed_fields, v_app_user, v_client_ip, v_client_app);
    IF TG_OP = 'DELETE' THEN RETURN OLD; ELSE RETURN NEW; END IF;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ============================================================================
-- PAYMENTS (Main Entity)
-- V3: case_id nullable (unmatched CREMUL payments); debtor fields nullable;
--     ocr_line added for Betalingsservice payment matching
-- ============================================================================

CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id UUID,
    debt_id UUID,

    -- Debtor reference — nullable for incoming CREMUL payments pending matching
    debtor_identifier VARCHAR(10),
    debtor_identifier_type VARCHAR(3) CHECK (debtor_identifier_type IS NULL OR debtor_identifier_type IN ('CPR', 'CVR')),
    debtor_role VARCHAR(10) CHECK (debtor_role IS NULL OR debtor_role IN ('PERSONAL', 'BUSINESS')),

    amount NUMERIC(15, 2) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    transaction_reference VARCHAR(100),
    external_payment_id VARCHAR(100),
    payment_date TIMESTAMPTZ,
    processed_at TIMESTAMPTZ,
    processed_by VARCHAR(100),
    failure_reason VARCHAR(500),
    ocr_line VARCHAR(50),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    sys_period TSTZRANGE NOT NULL DEFAULT tstzrange(CURRENT_TIMESTAMP, NULL),

    CONSTRAINT chk_payment_method CHECK (payment_method IN (
        'BANK_TRANSFER', 'CARD_PAYMENT', 'WAGE_GARNISHMENT',
        'OFFSETTING', 'CASH', 'DIRECT_DEBIT'
    )),
    CONSTRAINT chk_payment_status CHECK (status IN (
        'PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'REFUNDED', 'CANCELLED'
    )),
    CONSTRAINT chk_amount_positive CHECK (amount > 0)
);

CREATE TABLE payments_history (
    LIKE payments,
    PRIMARY KEY (id, sys_period)
);

-- ============================================================================
-- INDEXES (payments)
-- ============================================================================

CREATE INDEX idx_payment_case_id ON payments(case_id);
CREATE INDEX idx_payment_debt_id ON payments(debt_id);
CREATE INDEX idx_payment_debtor ON payments(debtor_identifier, debtor_identifier_type, debtor_role);
CREATE INDEX idx_payment_status ON payments(status);
CREATE INDEX idx_payment_date ON payments(payment_date);
CREATE INDEX idx_payment_created_at ON payments(created_at);
CREATE INDEX idx_payment_ocr_line ON payments(ocr_line);

CREATE INDEX idx_payments_history_id ON payments_history(id);
CREATE INDEX idx_payments_history_period ON payments_history USING GIST (sys_period);

-- ============================================================================
-- TRIGGERS (payments)
-- ============================================================================

CREATE OR REPLACE FUNCTION payments_versioning_trigger_function()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'UPDATE' THEN
        INSERT INTO payments_history SELECT OLD.*;
        UPDATE payments_history SET sys_period = tstzrange(lower(sys_period), CURRENT_TIMESTAMP)
        WHERE id = OLD.id AND upper(sys_period) IS NULL;
        NEW.sys_period := tstzrange(CURRENT_TIMESTAMP, NULL);
        NEW.updated_at := CURRENT_TIMESTAMP;
        NEW.version := OLD.version + 1;
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        INSERT INTO payments_history SELECT OLD.*;
        UPDATE payments_history SET sys_period = tstzrange(lower(sys_period), CURRENT_TIMESTAMP)
        WHERE id = OLD.id AND upper(sys_period) IS NULL;
        RETURN OLD;
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER payments_versioning_trigger
    BEFORE UPDATE OR DELETE ON payments FOR EACH ROW EXECUTE FUNCTION payments_versioning_trigger_function();

CREATE TRIGGER payments_audit_trigger
    AFTER INSERT OR UPDATE OR DELETE ON payments FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

COMMENT ON TABLE payments IS 'Payment transactions with full history and audit';
COMMENT ON TABLE payments_history IS 'Historical versions of payments';
COMMENT ON COLUMN payments.ocr_line IS 'Betalingsservice OCR-linje used for automatic matching';

-- ============================================================================
-- LEDGER ENTRIES (Immutable bi-temporal double-entry postings)
-- V4: entry_category includes COVERAGE_REVERSAL; amount constraint relaxed to >= 0
-- V5: accounting_target column added
-- See ADR-0018 for design rationale
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

    -- Category for filtering and reporting (V4: COVERAGE_REVERSAL added)
    entry_category VARCHAR(20) NOT NULL CHECK (entry_category IN (
        'DEBT_REGISTRATION', 'PAYMENT', 'INTEREST_ACCRUAL',
        'OFFSETTING', 'WRITE_OFF', 'REFUND', 'STORNO', 'CORRECTION',
        'COVERAGE_REVERSAL'
    )),

    -- Who receives the money: FORDRINGSHAVER or STATEN (V5)
    accounting_target VARCHAR(20),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_ledger_amount_non_negative CHECK (amount >= 0)
);

-- ============================================================================
-- DEBT EVENT TIMELINE
-- V4: COVERAGE_REVERSED event type added
-- ============================================================================

CREATE TABLE debt_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    debt_id UUID NOT NULL,
    event_type VARCHAR(30) NOT NULL CHECK (event_type IN (
        'DEBT_REGISTERED', 'PAYMENT_RECEIVED', 'INTEREST_ACCRUED',
        'OFFSETTING_EXECUTED', 'WRITE_OFF', 'REFUND',
        'UDLAEG_REGISTERED', 'UDLAEG_CORRECTED', 'CORRECTION',
        'COVERAGE_REVERSED'
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
-- INDEXES (ledger + debt_events)
-- ============================================================================

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

CREATE INDEX idx_debt_event_debt_id ON debt_events(debt_id);
CREATE INDEX idx_debt_event_effective_date ON debt_events(effective_date);
CREATE INDEX idx_debt_event_type ON debt_events(event_type);
CREATE INDEX idx_debt_event_debt_effective ON debt_events(debt_id, effective_date);

-- ============================================================================
-- AUDIT TRIGGERS (ledger + debt_events)
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

CREATE VIEW ledger_balance AS
SELECT
    account_code,
    account_name,
    SUM(CASE WHEN entry_type = 'DEBIT' THEN amount ELSE 0 END) AS total_debit,
    SUM(CASE WHEN entry_type = 'CREDIT' THEN amount ELSE 0 END) AS total_credit,
    SUM(CASE WHEN entry_type = 'DEBIT' THEN amount ELSE -amount END) AS balance
FROM ledger_entries
GROUP BY account_code, account_name;

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
COMMENT ON COLUMN ledger_entries.accounting_target IS 'Who receives the money: FORDRINGSHAVER (interest on principal) or STATEN (fees and interest on fees)';

-- ============================================================================
-- SEED DATA (V6: Extended demo cases — debt events + ledger entries)
-- ============================================================================

-- V6: Payment-service ledger entries for extended demo cases (petition 046/047)
--
-- Double-entry bookkeeping for 6 new debts across 3 cases.
-- Chart of accounts (from V2):
--   1000  Fordringer               (ASSET   — receivables)
--   1100  Renter tilgodehavende    (ASSET   — accrued interest receivable)
--   2000  SKB Bankkonto            (ASSET   — cash received from SKB)
--   3000  Indrivelsesindtaegter    (REVENUE — claim collection revenue)
--   3100  Renteindtaegter          (REVENUE — interest revenue)
--
-- Pattern per debt:
--   DEBT_REGISTRATION  → DEBIT 1000 / CREDIT 3000 (principal)
--   INTEREST_ACCRUAL   → DEBIT 1100 / CREDIT 3100  (per accrual batch)
--   PAYMENT            → DEBIT 2000 / CREDIT 1100 (interest) / CREDIT 1000 (principal)
--   FEE                → DEBIT 1000 / CREDIT 3000  (adds fee to receivable)

-- ============================================================================
-- DEBT EVENTS (source of truth for replay)
-- ============================================================================

INSERT INTO debt_events (id, debt_id, event_type, effective_date, amount, reference, description, created_at) VALUES

    -- C01: TOLD, principal 38500, no payments
    ('0C010000-0000-0000-0001-000000000001', '00000000-0000-0000-0000-000000000C01',
     'DEBT_REGISTERED',  '2025-04-15', 38500.00, 'TOLD-C01-2025-REG', 'Toldskyld registreret til inddrivelse', NOW()),
    ('0C010000-0000-0000-0001-000000000002', '00000000-0000-0000-0000-000000000C01',
     'INTEREST_ACCRUED', '2026-01-31', 122.67,   'TOLD-C01-INT-2026-01', 'Månedlig rentetilskrivning januar 2026', NOW()),
    ('0C010000-0000-0000-0001-000000000003', '00000000-0000-0000-0000-000000000C01',
     'INTEREST_ACCRUED', '2026-02-28', 110.82,   'TOLD-C01-INT-2026-02', 'Månedlig rentetilskrivning februar 2026', NOW()),

    -- C02: TOLD afdrag, principal 12800, payment 3000 on 2025-08-15
    ('0C020000-0000-0000-0002-000000000001', '00000000-0000-0000-0000-000000000C02',
     'DEBT_REGISTERED',  '2025-05-01', 12800.00, 'TOLD-C02-2025-REG', 'Toldskyld afdrag registreret til inddrivelse', NOW()),
    ('0C020000-0000-0000-0002-000000000002', '00000000-0000-0000-0000-000000000C02',
     'PAYMENT_RECEIVED', '2025-08-15', 3000.00,  'TOLD-C02-PAY-2025-08', 'Frivillig betaling via NemKonto', NOW()),
    ('0C020000-0000-0000-0002-000000000003', '00000000-0000-0000-0000-000000000C02',
     'INTEREST_ACCRUED', '2026-01-31', 22.94,    'TOLD-C02-INT-2026-01', 'Månedlig rentetilskrivning januar 2026', NOW()),
    ('0C020000-0000-0000-0002-000000000004', '00000000-0000-0000-0000-000000000C02',
     'INTEREST_ACCRUED', '2026-02-28', 20.72,    'TOLD-C02-INT-2026-02', 'Månedlig rentetilskrivning februar 2026', NOW()),

    -- D01: SU-gæld, principal 24750, payment 5000 on 2025-11-01
    ('0D010000-0000-0000-0003-000000000001', '00000000-0000-0000-0000-000000000D01',
     'DEBT_REGISTERED',  '2025-03-01', 24750.00, 'SU-D01-2025-REG', 'SU-gæld registreret til inddrivelse', NOW()),
    ('0D010000-0000-0000-0003-000000000002', '00000000-0000-0000-0000-000000000D01',
     'PAYMENT_RECEIVED', '2025-11-01', 5000.00,  'SU-D01-PAY-2025-11', 'Frivillig delvis betaling via NemKonto', NOW()),
    ('0D010000-0000-0000-0003-000000000003', '00000000-0000-0000-0000-000000000D01',
     'INTEREST_ACCRUED', '2026-01-31', 101.36,   'SU-D01-INT-2026-01', 'Månedlig rentetilskrivning januar 2026', NOW()),
    ('0D010000-0000-0000-0003-000000000004', '00000000-0000-0000-0000-000000000D01',
     'INTEREST_ACCRUED', '2026-02-28', 91.57,    'SU-D01-INT-2026-02', 'Månedlig rentetilskrivning februar 2026', NOW()),

    -- D02: Strafferetlig bøde, principal 5000, fee 65, no interest
    ('0D020000-0000-0000-0004-000000000001', '00000000-0000-0000-0000-000000000D02',
     'DEBT_REGISTERED',  '2025-04-01', 5000.00,  'STRAF-D02-2025-REG', 'Strafferetlig bøde registreret til inddrivelse', NOW()),

    -- E01: Underholdsbidrag, principal 18400, fee 100, no payments
    ('0E010000-0000-0000-0005-000000000001', '00000000-0000-0000-0000-000000000E01',
     'DEBT_REGISTERED',  '2025-07-01', 18400.00, 'BIDR-E01-2025-REG', 'Underholdsbidrag registreret til inddrivelse', NOW()),
    ('0E010000-0000-0000-0005-000000000002', '00000000-0000-0000-0000-000000000E01',
     'INTEREST_ACCRUED', '2026-01-31', 87.69,    'BIDR-E01-INT-2026-01', 'Månedlig rentetilskrivning januar 2026', NOW()),
    ('0E010000-0000-0000-0005-000000000003', '00000000-0000-0000-0000-000000000E01',
     'INTEREST_ACCRUED', '2026-02-28', 79.21,    'BIDR-E01-INT-2026-02', 'Månedlig rentetilskrivning februar 2026', NOW()),

    -- E02: Dagbøde, principal 3200, no payments
    ('0E020000-0000-0000-0006-000000000001', '00000000-0000-0000-0000-000000000E02',
     'DEBT_REGISTERED',  '2025-09-15', 3200.00,  'DAGB-E02-2025-REG', 'Dagbøde registreret til inddrivelse', NOW()),
    ('0E020000-0000-0000-0006-000000000002', '00000000-0000-0000-0000-000000000E02',
     'INTEREST_ACCRUED', '2026-01-31', 15.25,    'DAGB-E02-INT-2026-01', 'Månedlig rentetilskrivning januar 2026', NOW()),
    ('0E020000-0000-0000-0006-000000000003', '00000000-0000-0000-0000-000000000E02',
     'INTEREST_ACCRUED', '2026-02-28', 13.77,    'DAGB-E02-INT-2026-02', 'Månedlig rentetilskrivning februar 2026', NOW())

ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- LEDGER ENTRIES (double-entry)
-- Each INSERT block covers one transaction (transaction_id groups the entries)
-- ============================================================================

-- ─── C01 DEBT REGISTRATION (38500) ───────────────────────────────────────────
INSERT INTO ledger_entries (transaction_id, debt_id, account_code, account_name, entry_type, amount, effective_date, posting_date, reference, description, entry_category, accounting_target)
VALUES
    ('0C010000-0000-0000-0001-000000000001', '00000000-0000-0000-0000-000000000C01', '1000', 'Fordringer',            'DEBIT',  38500.00, '2025-04-15', '2025-04-15', 'TOLD-C01-2025-REG', 'Toldskyld registreret — EUTK art. 114',  'DEBT_REGISTRATION', 'FORDRINGSHAVER'),
    ('0C010000-0000-0000-0001-000000000001', '00000000-0000-0000-0000-000000000C01', '3000', 'Indrivelsesindtaegter', 'CREDIT', 38500.00, '2025-04-15', '2025-04-15', 'TOLD-C01-2025-REG', 'Toldskyld modtaget til inddrivelse',      'DEBT_REGISTRATION', 'FORDRINGSHAVER')
ON CONFLICT DO NOTHING;

-- ─── C01 INTEREST — Januar 2026 (122.67) ─────────────────────────────────────
INSERT INTO ledger_entries (transaction_id, debt_id, account_code, account_name, entry_type, amount, effective_date, posting_date, reference, description, entry_category, accounting_target)
VALUES
    ('0C010000-0000-0000-0001-000000000002', '00000000-0000-0000-0000-000000000C01', '1100', 'Renter tilgodehavende', 'DEBIT',  122.67, '2026-01-31', '2026-01-31', 'TOLD-C01-INT-2026-01', 'Inddrivelsesrente januar 2026 @ 3.75%', 'INTEREST_ACCRUAL', 'FORDRINGSHAVER'),
    ('0C010000-0000-0000-0001-000000000002', '00000000-0000-0000-0000-000000000C01', '3100', 'Renteindtaegter',       'CREDIT', 122.67, '2026-01-31', '2026-01-31', 'TOLD-C01-INT-2026-01', 'Renteindtægt januar 2026',              'INTEREST_ACCRUAL', 'FORDRINGSHAVER')
ON CONFLICT DO NOTHING;

-- ─── C01 INTEREST — Februar 2026 (110.82) ────────────────────────────────────
INSERT INTO ledger_entries (transaction_id, debt_id, account_code, account_name, entry_type, amount, effective_date, posting_date, reference, description, entry_category, accounting_target)
VALUES
    ('0C010000-0000-0000-0001-000000000003', '00000000-0000-0000-0000-000000000C01', '1100', 'Renter tilgodehavende', 'DEBIT',  110.82, '2026-02-28', '2026-02-28', 'TOLD-C01-INT-2026-02', 'Inddrivelsesrente februar 2026 @ 3.75%', 'INTEREST_ACCRUAL', 'FORDRINGSHAVER'),
    ('0C010000-0000-0000-0001-000000000003', '00000000-0000-0000-0000-000000000C01', '3100', 'Renteindtaegter',       'CREDIT', 110.82, '2026-02-28', '2026-02-28', 'TOLD-C01-INT-2026-02', 'Renteindtægt februar 2026',               'INTEREST_ACCRUAL', 'FORDRINGSHAVER')
ON CONFLICT DO NOTHING;

-- ─── C02 DEBT REGISTRATION (12800) ───────────────────────────────────────────
INSERT INTO ledger_entries (transaction_id, debt_id, account_code, account_name, entry_type, amount, effective_date, posting_date, reference, description, entry_category, accounting_target)
VALUES
    ('0C020000-0000-0000-0002-000000000001', '00000000-0000-0000-0000-000000000C02', '1000', 'Fordringer',            'DEBIT',  12800.00, '2025-05-01', '2025-05-01', 'TOLD-C02-2025-REG', 'Toldskyld afdrag registreret — EUTK art. 114 stk. 2', 'DEBT_REGISTRATION', 'FORDRINGSHAVER'),
    ('0C020000-0000-0000-0002-000000000001', '00000000-0000-0000-0000-000000000C02', '3000', 'Indrivelsesindtaegter', 'CREDIT', 12800.00, '2025-05-01', '2025-05-01', 'TOLD-C02-2025-REG', 'Toldskyld afdrag modtaget til inddrivelse',              'DEBT_REGISTRATION', 'FORDRINGSHAVER')
ON CONFLICT DO NOTHING;

-- ─── C02 PAYMENT 3000 on 2025-08-15 (coverage: 115 interest + 2885 principal) ─
INSERT INTO ledger_entries (transaction_id, debt_id, account_code, account_name, entry_type, amount, effective_date, posting_date, reference, description, entry_category, accounting_target)
VALUES
    ('0C020000-0000-0000-0002-000000000002', '00000000-0000-0000-0000-000000000C02', '2000', 'SKB Bankkonto',         'DEBIT',  3000.00, '2025-08-15', '2025-08-15', 'TOLD-C02-PAY-2025-08', 'Betaling modtaget via NemKonto',               'PAYMENT', 'FORDRINGSHAVER'),
    ('0C020000-0000-0000-0002-000000000002', '00000000-0000-0000-0000-000000000C02', '1100', 'Renter tilgodehavende', 'CREDIT', 115.18,  '2025-08-15', '2025-08-15', 'TOLD-C02-PAY-2025-08', 'Dækning: renter (dækningsrækkefølge 1/3)',     'PAYMENT', 'FORDRINGSHAVER'),
    ('0C020000-0000-0000-0002-000000000002', '00000000-0000-0000-0000-000000000C02', '1000', 'Fordringer',            'CREDIT', 2884.82, '2025-08-15', '2025-08-15', 'TOLD-C02-PAY-2025-08', 'Dækning: hovedstol (dækningsrækkefølge 3/3)',  'PAYMENT', 'FORDRINGSHAVER')
ON CONFLICT DO NOTHING;

-- ─── C02 INTEREST — Januar 2026 (22.94) ──────────────────────────────────────
INSERT INTO ledger_entries (transaction_id, debt_id, account_code, account_name, entry_type, amount, effective_date, posting_date, reference, description, entry_category, accounting_target)
VALUES
    ('0C020000-0000-0000-0002-000000000003', '00000000-0000-0000-0000-000000000C02', '1100', 'Renter tilgodehavende', 'DEBIT',  22.94, '2026-01-31', '2026-01-31', 'TOLD-C02-INT-2026-01', 'Inddrivelsesrente januar 2026 @ 2.75%', 'INTEREST_ACCRUAL', 'FORDRINGSHAVER'),
    ('0C020000-0000-0000-0002-000000000003', '00000000-0000-0000-0000-000000000C02', '3100', 'Renteindtaegter',       'CREDIT', 22.94, '2026-01-31', '2026-01-31', 'TOLD-C02-INT-2026-01', 'Renteindtægt januar 2026',              'INTEREST_ACCRUAL', 'FORDRINGSHAVER')
ON CONFLICT DO NOTHING;

-- ─── C02 INTEREST — Februar 2026 (20.72) ─────────────────────────────────────
INSERT INTO ledger_entries (transaction_id, debt_id, account_code, account_name, entry_type, amount, effective_date, posting_date, reference, description, entry_category, accounting_target)
VALUES
    ('0C020000-0000-0000-0002-000000000004', '00000000-0000-0000-0000-000000000C02', '1100', 'Renter tilgodehavende', 'DEBIT',  20.72, '2026-02-28', '2026-02-28', 'TOLD-C02-INT-2026-02', 'Inddrivelsesrente februar 2026 @ 2.75%', 'INTEREST_ACCRUAL', 'FORDRINGSHAVER'),
    ('0C020000-0000-0000-0002-000000000004', '00000000-0000-0000-0000-000000000C02', '3100', 'Renteindtaegter',       'CREDIT', 20.72, '2026-02-28', '2026-02-28', 'TOLD-C02-INT-2026-02', 'Renteindtægt februar 2026',               'INTEREST_ACCRUAL', 'FORDRINGSHAVER')
ON CONFLICT DO NOTHING;

-- ─── D01 DEBT REGISTRATION (24750) ───────────────────────────────────────────
INSERT INTO ledger_entries (transaction_id, debt_id, account_code, account_name, entry_type, amount, effective_date, posting_date, reference, description, entry_category, accounting_target)
VALUES
    ('0D010000-0000-0000-0003-000000000001', '00000000-0000-0000-0000-000000000D01', '1000', 'Fordringer',            'DEBIT',  24750.00, '2025-03-01', '2025-03-01', 'SU-D01-2025-REG', 'SU-gæld registreret — Gældsinddrivelsesloven § 5', 'DEBT_REGISTRATION', 'FORDRINGSHAVER'),
    ('0D010000-0000-0000-0003-000000000001', '00000000-0000-0000-0000-000000000D01', '3000', 'Indrivelsesindtaegter', 'CREDIT', 24750.00, '2025-03-01', '2025-03-01', 'SU-D01-2025-REG', 'SU-gæld modtaget til inddrivelse',                 'DEBT_REGISTRATION', 'FORDRINGSHAVER')
ON CONFLICT DO NOTHING;

-- ─── D01 PAYMENT 5000 on 2025-11-01 (coverage: 1003.59 interest + 3996.41 principal) ─
INSERT INTO ledger_entries (transaction_id, debt_id, account_code, account_name, entry_type, amount, effective_date, posting_date, reference, description, entry_category, accounting_target)
VALUES
    ('0D010000-0000-0000-0003-000000000002', '00000000-0000-0000-0000-000000000D01', '2000', 'SKB Bankkonto',         'DEBIT',  5000.00,  '2025-11-01', '2025-11-01', 'SU-D01-PAY-2025-11', 'Delvis betaling modtaget via NemKonto',                 'PAYMENT', 'FORDRINGSHAVER'),
    ('0D010000-0000-0000-0003-000000000002', '00000000-0000-0000-0000-000000000D01', '1100', 'Renter tilgodehavende', 'CREDIT', 1003.59,  '2025-11-01', '2025-11-01', 'SU-D01-PAY-2025-11', 'Dækning: renter op til 2025-11-01 (dækningsrækkefølge 1/3)', 'PAYMENT', 'FORDRINGSHAVER'),
    ('0D010000-0000-0000-0003-000000000002', '00000000-0000-0000-0000-000000000D01', '1000', 'Fordringer',            'CREDIT', 3996.41,  '2025-11-01', '2025-11-01', 'SU-D01-PAY-2025-11', 'Dækning: hovedstol (dækningsrækkefølge 3/3)',                'PAYMENT', 'FORDRINGSHAVER')
ON CONFLICT DO NOTHING;

-- ─── D01 INTEREST — Januar 2026 (101.36) ─────────────────────────────────────
INSERT INTO ledger_entries (transaction_id, debt_id, account_code, account_name, entry_type, amount, effective_date, posting_date, reference, description, entry_category, accounting_target)
VALUES
    ('0D010000-0000-0000-0003-000000000003', '00000000-0000-0000-0000-000000000D01', '1100', 'Renter tilgodehavende', 'DEBIT',  101.36, '2026-01-31', '2026-01-31', 'SU-D01-INT-2026-01', 'Inddrivelsesrente januar 2026 @ 5.75%', 'INTEREST_ACCRUAL', 'FORDRINGSHAVER'),
    ('0D010000-0000-0000-0003-000000000003', '00000000-0000-0000-0000-000000000D01', '3100', 'Renteindtaegter',       'CREDIT', 101.36, '2026-01-31', '2026-01-31', 'SU-D01-INT-2026-01', 'Renteindtægt januar 2026',              'INTEREST_ACCRUAL', 'FORDRINGSHAVER')
ON CONFLICT DO NOTHING;

-- ─── D01 INTEREST — Februar 2026 (91.57) ─────────────────────────────────────
INSERT INTO ledger_entries (transaction_id, debt_id, account_code, account_name, entry_type, amount, effective_date, posting_date, reference, description, entry_category, accounting_target)
VALUES
    ('0D010000-0000-0000-0003-000000000004', '00000000-0000-0000-0000-000000000D01', '1100', 'Renter tilgodehavende', 'DEBIT',  91.57, '2026-02-28', '2026-02-28', 'SU-D01-INT-2026-02', 'Inddrivelsesrente februar 2026 @ 5.75%', 'INTEREST_ACCRUAL', 'FORDRINGSHAVER'),
    ('0D010000-0000-0000-0003-000000000004', '00000000-0000-0000-0000-000000000D01', '3100', 'Renteindtaegter',       'CREDIT', 91.57, '2026-02-28', '2026-02-28', 'SU-D01-INT-2026-02', 'Renteindtægt februar 2026',               'INTEREST_ACCRUAL', 'FORDRINGSHAVER')
ON CONFLICT DO NOTHING;

-- ─── D02 DEBT REGISTRATION (5000, strafferetlig bøde) ────────────────────────
INSERT INTO ledger_entries (transaction_id, debt_id, account_code, account_name, entry_type, amount, effective_date, posting_date, reference, description, entry_category, accounting_target)
VALUES
    ('0D020000-0000-0000-0004-000000000001', '00000000-0000-0000-0000-000000000D02', '1000', 'Fordringer',            'DEBIT',  5000.00, '2025-04-01', '2025-04-01', 'STRAF-D02-2025-REG', 'Strafferetlig bøde registreret — rentefri', 'DEBT_REGISTRATION', 'FORDRINGSHAVER'),
    ('0D020000-0000-0000-0004-000000000001', '00000000-0000-0000-0000-000000000D02', '3000', 'Indrivelsesindtaegter', 'CREDIT', 5000.00, '2025-04-01', '2025-04-01', 'STRAF-D02-2025-REG', 'Bøde modtaget til inddrivelse',             'DEBT_REGISTRATION', 'FORDRINGSHAVER')
ON CONFLICT DO NOTHING;

-- ─── D02 FEE — RYKKER 65 kr (2025-05-01) ─────────────────────────────────────
INSERT INTO ledger_entries (transaction_id, debt_id, account_code, account_name, entry_type, amount, effective_date, posting_date, reference, description, entry_category, accounting_target)
VALUES
    ('0D020000-0000-0000-0004-000000000002', '00000000-0000-0000-0000-000000000D02', '1000', 'Fordringer',            'DEBIT',  65.00, '2025-05-01', '2025-05-01', 'STRAF-D02-FEE-RYKKER', 'Rykkergebyr tilføjet — Opkrævningsloven § 6', 'DEBT_REGISTRATION', 'STATEN'),
    ('0D020000-0000-0000-0004-000000000002', '00000000-0000-0000-0000-000000000D02', '3000', 'Indrivelsesindtaegter', 'CREDIT', 65.00, '2025-05-01', '2025-05-01', 'STRAF-D02-FEE-RYKKER', 'Rykkergebyr — statslig indtægt',              'DEBT_REGISTRATION', 'STATEN')
ON CONFLICT DO NOTHING;

-- ─── E01 DEBT REGISTRATION (18400, underholdsbidrag) ─────────────────────────
INSERT INTO ledger_entries (transaction_id, debt_id, account_code, account_name, entry_type, amount, effective_date, posting_date, reference, description, entry_category, accounting_target)
VALUES
    ('0E010000-0000-0000-0005-000000000001', '00000000-0000-0000-0000-000000000E01', '1000', 'Fordringer',            'DEBIT',  18400.00, '2025-07-01', '2025-07-01', 'BIDR-E01-2025-REG', 'Underholdsbidrag registreret — Gældsinddrivelsesloven § 5', 'DEBT_REGISTRATION', 'FORDRINGSHAVER'),
    ('0E010000-0000-0000-0005-000000000001', '00000000-0000-0000-0000-000000000E01', '3000', 'Indrivelsesindtaegter', 'CREDIT', 18400.00, '2025-07-01', '2025-07-01', 'BIDR-E01-2025-REG', 'Underholdsbidrag modtaget til inddrivelse',                 'DEBT_REGISTRATION', 'FORDRINGSHAVER')
ON CONFLICT DO NOTHING;

-- ─── E01 FEE — LOENINDEHOLDELSE 100 kr (2026-01-15) ─────────────────────────
INSERT INTO ledger_entries (transaction_id, debt_id, account_code, account_name, entry_type, amount, effective_date, posting_date, reference, description, entry_category, accounting_target)
VALUES
    ('0E010000-0000-0000-0005-000000000004', '00000000-0000-0000-0000-000000000E01', '1000', 'Fordringer',            'DEBIT',  100.00, '2026-01-15', '2026-01-15', 'BIDR-E01-FEE-LOENI', 'Lønindeholdelsesgebyr — Gældsinddrivelsesloven § 10', 'DEBT_REGISTRATION', 'STATEN'),
    ('0E010000-0000-0000-0005-000000000004', '00000000-0000-0000-0000-000000000E01', '3000', 'Indrivelsesindtaegter', 'CREDIT', 100.00, '2026-01-15', '2026-01-15', 'BIDR-E01-FEE-LOENI', 'Lønindeholdelsesgebyr — statslig indtægt',                    'DEBT_REGISTRATION', 'STATEN')
ON CONFLICT DO NOTHING;

-- ─── E01 INTEREST — Januar 2026 (87.69) ──────────────────────────────────────
INSERT INTO ledger_entries (transaction_id, debt_id, account_code, account_name, entry_type, amount, effective_date, posting_date, reference, description, entry_category, accounting_target)
VALUES
    ('0E010000-0000-0000-0005-000000000002', '00000000-0000-0000-0000-000000000E01', '1100', 'Renter tilgodehavende', 'DEBIT',  87.69, '2026-01-31', '2026-01-31', 'BIDR-E01-INT-2026-01', 'Inddrivelsesrente januar 2026 @ 5.75%', 'INTEREST_ACCRUAL', 'FORDRINGSHAVER'),
    ('0E010000-0000-0000-0005-000000000002', '00000000-0000-0000-0000-000000000E01', '3100', 'Renteindtaegter',       'CREDIT', 87.69, '2026-01-31', '2026-01-31', 'BIDR-E01-INT-2026-01', 'Renteindtægt januar 2026',              'INTEREST_ACCRUAL', 'FORDRINGSHAVER')
ON CONFLICT DO NOTHING;

-- ─── E01 INTEREST — Februar 2026 (79.21) ─────────────────────────────────────
INSERT INTO ledger_entries (transaction_id, debt_id, account_code, account_name, entry_type, amount, effective_date, posting_date, reference, description, entry_category, accounting_target)
VALUES
    ('0E010000-0000-0000-0005-000000000003', '00000000-0000-0000-0000-000000000E01', '1100', 'Renter tilgodehavende', 'DEBIT',  79.21, '2026-02-28', '2026-02-28', 'BIDR-E01-INT-2026-02', 'Inddrivelsesrente februar 2026 @ 5.75%', 'INTEREST_ACCRUAL', 'FORDRINGSHAVER'),
    ('0E010000-0000-0000-0005-000000000003', '00000000-0000-0000-0000-000000000E01', '3100', 'Renteindtaegter',       'CREDIT', 79.21, '2026-02-28', '2026-02-28', 'BIDR-E01-INT-2026-02', 'Renteindtægt februar 2026',               'INTEREST_ACCRUAL', 'FORDRINGSHAVER')
ON CONFLICT DO NOTHING;

-- ─── E02 DEBT REGISTRATION (3200, dagbøde) ───────────────────────────────────
INSERT INTO ledger_entries (transaction_id, debt_id, account_code, account_name, entry_type, amount, effective_date, posting_date, reference, description, entry_category, accounting_target)
VALUES
    ('0E020000-0000-0000-0006-000000000001', '00000000-0000-0000-0000-000000000E02', '1000', 'Fordringer',            'DEBIT',  3200.00, '2025-09-15', '2025-09-15', 'DAGB-E02-2025-REG', 'Dagbøde registreret — administrativ afgørelse', 'DEBT_REGISTRATION', 'FORDRINGSHAVER'),
    ('0E020000-0000-0000-0006-000000000001', '00000000-0000-0000-0000-000000000E02', '3000', 'Indrivelsesindtaegter', 'CREDIT', 3200.00, '2025-09-15', '2025-09-15', 'DAGB-E02-2025-REG', 'Dagbøde modtaget til inddrivelse',               'DEBT_REGISTRATION', 'FORDRINGSHAVER')
ON CONFLICT DO NOTHING;

-- ─── E02 INTEREST — Januar 2026 (15.25) ──────────────────────────────────────
INSERT INTO ledger_entries (transaction_id, debt_id, account_code, account_name, entry_type, amount, effective_date, posting_date, reference, description, entry_category, accounting_target)
VALUES
    ('0E020000-0000-0000-0006-000000000002', '00000000-0000-0000-0000-000000000E02', '1100', 'Renter tilgodehavende', 'DEBIT',  15.25, '2026-01-31', '2026-01-31', 'DAGB-E02-INT-2026-01', 'Inddrivelsesrente januar 2026 @ 5.75%', 'INTEREST_ACCRUAL', 'FORDRINGSHAVER'),
    ('0E020000-0000-0000-0006-000000000002', '00000000-0000-0000-0000-000000000E02', '3100', 'Renteindtaegter',       'CREDIT', 15.25, '2026-01-31', '2026-01-31', 'DAGB-E02-INT-2026-01', 'Renteindtægt januar 2026',              'INTEREST_ACCRUAL', 'FORDRINGSHAVER')
ON CONFLICT DO NOTHING;

-- ─── E02 INTEREST — Februar 2026 (13.77) ─────────────────────────────────────
INSERT INTO ledger_entries (transaction_id, debt_id, account_code, account_name, entry_type, amount, effective_date, posting_date, reference, description, entry_category, accounting_target)
VALUES
    ('0E020000-0000-0000-0006-000000000003', '00000000-0000-0000-0000-000000000E02', '1100', 'Renter tilgodehavende', 'DEBIT',  13.77, '2026-02-28', '2026-02-28', 'DAGB-E02-INT-2026-02', 'Inddrivelsesrente februar 2026 @ 5.75%', 'INTEREST_ACCRUAL', 'FORDRINGSHAVER'),
    ('0E020000-0000-0000-0006-000000000003', '00000000-0000-0000-0000-000000000E02', '3100', 'Renteindtaegter',       'CREDIT', 13.77, '2026-02-28', '2026-02-28', 'DAGB-E02-INT-2026-02', 'Renteindtægt februar 2026',               'INTEREST_ACCRUAL', 'FORDRINGSHAVER')
ON CONFLICT DO NOTHING;

