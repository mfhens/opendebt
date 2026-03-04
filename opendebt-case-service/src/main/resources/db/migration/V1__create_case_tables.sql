-- OpenDebt Case Service - Initial Schema
-- Database: PostgreSQL (Enterprise Grade)
-- Features: Temporal tables, audit logging, proper debtor identification

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

COMMENT ON TABLE audit_log IS 'Audit trail for all database modifications';

-- Audit context function
CREATE OR REPLACE FUNCTION set_audit_context(p_user VARCHAR, p_ip INET DEFAULT NULL, p_app VARCHAR DEFAULT NULL)
RETURNS VOID AS $$
BEGIN
    PERFORM set_config('opendebt.audit_user', p_user, true);
    PERFORM set_config('opendebt.audit_ip', COALESCE(p_ip::text, ''), true);
    PERFORM set_config('opendebt.audit_app', COALESCE(p_app, ''), true);
END;
$$ LANGUAGE plpgsql;

-- Generic audit trigger function
CREATE OR REPLACE FUNCTION audit_trigger_function()
RETURNS TRIGGER AS $$
DECLARE
    v_old_values JSONB;
    v_new_values JSONB;
    v_changed_fields TEXT[];
    v_record_id UUID;
    v_app_user VARCHAR(100);
    v_client_ip INET;
    v_client_app VARCHAR(200);
BEGIN
    v_app_user := NULLIF(current_setting('opendebt.audit_user', true), '');
    v_client_ip := NULLIF(current_setting('opendebt.audit_ip', true), '')::INET;
    v_client_app := NULLIF(current_setting('opendebt.audit_app', true), '');

    IF TG_OP = 'DELETE' THEN
        v_record_id := OLD.id;
        v_old_values := to_jsonb(OLD);
    ELSIF TG_OP = 'INSERT' THEN
        v_record_id := NEW.id;
        v_new_values := to_jsonb(NEW);
    ELSIF TG_OP = 'UPDATE' THEN
        v_record_id := NEW.id;
        v_old_values := to_jsonb(OLD);
        v_new_values := to_jsonb(NEW);
        SELECT array_agg(key) INTO v_changed_fields
        FROM jsonb_each(v_new_values) n
        FULL OUTER JOIN jsonb_each(v_old_values) o USING (key)
        WHERE n.value IS DISTINCT FROM o.value;
    END IF;

    INSERT INTO audit_log (table_name, record_id, operation, old_values, new_values, 
                          changed_fields, application_user, client_ip, client_application)
    VALUES (TG_TABLE_NAME, v_record_id, TG_OP, v_old_values, v_new_values,
            v_changed_fields, v_app_user, v_client_ip, v_client_app);

    IF TG_OP = 'DELETE' THEN RETURN OLD; ELSE RETURN NEW; END IF;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ============================================================================
-- CASES (Main Entity)
-- ============================================================================

CREATE TABLE cases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_number VARCHAR(20) NOT NULL UNIQUE,
    
    -- Reference to Person Registry (NO PII stored here)
    debtor_person_id UUID NOT NULL,      -- Reference to persons table in person-registry
    
    -- Case details
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    total_debt NUMERIC(15, 2) DEFAULT 0,
    total_paid NUMERIC(15, 2) DEFAULT 0,
    total_remaining NUMERIC(15, 2) DEFAULT 0,
    active_strategy VARCHAR(30),
    assigned_caseworker_id VARCHAR(100),
    notes TEXT,
    last_activity_at TIMESTAMPTZ,
    
    -- Metadata
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    
    -- Temporal support
    sys_period TSTZRANGE NOT NULL DEFAULT tstzrange(CURRENT_TIMESTAMP, NULL),
    
    -- Constraints
    CONSTRAINT chk_status CHECK (status IN (
        'OPEN', 'IN_PROGRESS', 'AWAITING_PAYMENT', 'PAYMENT_PLAN_ACTIVE',
        'WAGE_GARNISHMENT_ACTIVE', 'OFFSETTING_PENDING', 'UNDER_APPEAL',
        'CLOSED_PAID', 'CLOSED_WRITTEN_OFF', 'CLOSED_CANCELLED'
    )),
    CONSTRAINT chk_strategy CHECK (active_strategy IS NULL OR active_strategy IN (
        'VOLUNTARY_PAYMENT', 'PAYMENT_PLAN', 'WAGE_GARNISHMENT', 'OFFSETTING', 'LEGAL_ACTION'
    ))
);

-- History table for cases (temporal/versioning)
CREATE TABLE cases_history (
    LIKE cases,
    PRIMARY KEY (id, sys_period)
);

-- Case to debt mapping
CREATE TABLE case_debt_ids (
    case_id UUID NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    debt_id UUID NOT NULL,
    added_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    added_by VARCHAR(100),
    PRIMARY KEY (case_id, debt_id)
);

-- ============================================================================
-- INDEXES
-- ============================================================================

CREATE INDEX idx_case_debtor_person_id ON cases(debtor_person_id);
CREATE INDEX idx_case_status ON cases(status);
CREATE INDEX idx_case_caseworker ON cases(assigned_caseworker_id);
CREATE INDEX idx_case_created_at ON cases(created_at);

CREATE INDEX idx_cases_history_id ON cases_history(id);
CREATE INDEX idx_cases_history_period ON cases_history USING GIST (sys_period);

-- ============================================================================
-- TEMPORAL/HISTORY TRIGGERS
-- ============================================================================

CREATE OR REPLACE FUNCTION cases_versioning_trigger_function()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'UPDATE' THEN
        INSERT INTO cases_history SELECT OLD.*;
        UPDATE cases_history 
        SET sys_period = tstzrange(lower(sys_period), CURRENT_TIMESTAMP)
        WHERE id = OLD.id AND upper(sys_period) IS NULL;
        
        NEW.sys_period := tstzrange(CURRENT_TIMESTAMP, NULL);
        NEW.updated_at := CURRENT_TIMESTAMP;
        NEW.version := OLD.version + 1;
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        INSERT INTO cases_history SELECT OLD.*;
        UPDATE cases_history 
        SET sys_period = tstzrange(lower(sys_period), CURRENT_TIMESTAMP)
        WHERE id = OLD.id AND upper(sys_period) IS NULL;
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER cases_versioning_trigger
    BEFORE UPDATE OR DELETE ON cases
    FOR EACH ROW
    EXECUTE FUNCTION cases_versioning_trigger_function();

CREATE TRIGGER cases_audit_trigger
    AFTER INSERT OR UPDATE OR DELETE ON cases
    FOR EACH ROW
    EXECUTE FUNCTION audit_trigger_function();

-- ============================================================================
-- HELPER FUNCTIONS
-- ============================================================================

CREATE OR REPLACE FUNCTION get_case_history(p_case_id UUID)
RETURNS TABLE (
    id UUID,
    case_number VARCHAR,
    status VARCHAR,
    total_debt NUMERIC,
    valid_from TIMESTAMPTZ,
    valid_to TIMESTAMPTZ
) AS $$
BEGIN
    RETURN QUERY
    SELECT h.id, h.case_number, h.status, h.total_debt,
           lower(h.sys_period), upper(h.sys_period)
    FROM cases_history h WHERE h.id = p_case_id
    UNION ALL
    SELECT c.id, c.case_number, c.status, c.total_debt,
           lower(c.sys_period), NULL
    FROM cases c WHERE c.id = p_case_id
    ORDER BY valid_from;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- COMMENTS
-- ============================================================================

COMMENT ON TABLE cases IS 'Debt collection cases with full history - NO PII STORED';
COMMENT ON TABLE cases_history IS 'Historical versions of cases (temporal table)';
COMMENT ON COLUMN cases.debtor_person_id IS 'Reference to person-registry.persons (UUID only, no PII)';
COMMENT ON COLUMN cases.case_number IS 'Unique case reference number';
