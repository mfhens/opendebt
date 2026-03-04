-- OpenDebt Payment Service - Initial Schema
-- Database: PostgreSQL (Enterprise Grade)
-- Features: Temporal tables, audit logging

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
-- ============================================================================

CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id UUID NOT NULL,
    debt_id UUID,
    
    -- Debtor reference for direct lookups
    debtor_identifier VARCHAR(10) NOT NULL,
    debtor_identifier_type VARCHAR(3) NOT NULL CHECK (debtor_identifier_type IN ('CPR', 'CVR')),
    debtor_role VARCHAR(10) NOT NULL CHECK (debtor_role IN ('PERSONAL', 'BUSINESS')),
    
    amount NUMERIC(15, 2) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    transaction_reference VARCHAR(100),
    external_payment_id VARCHAR(100),
    payment_date TIMESTAMPTZ,
    processed_at TIMESTAMPTZ,
    processed_by VARCHAR(100),
    failure_reason VARCHAR(500),
    
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
-- INDEXES
-- ============================================================================

CREATE INDEX idx_payment_case_id ON payments(case_id);
CREATE INDEX idx_payment_debt_id ON payments(debt_id);
CREATE INDEX idx_payment_debtor ON payments(debtor_identifier, debtor_identifier_type, debtor_role);
CREATE INDEX idx_payment_status ON payments(status);
CREATE INDEX idx_payment_date ON payments(payment_date);
CREATE INDEX idx_payment_created_at ON payments(created_at);

CREATE INDEX idx_payments_history_id ON payments_history(id);
CREATE INDEX idx_payments_history_period ON payments_history USING GIST (sys_period);

-- ============================================================================
-- TRIGGERS
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
