-- OpenDebt Debt Service - Initial Schema
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

-- Audit log table for tracking all direct database modifications
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
CREATE INDEX idx_audit_log_operation ON audit_log(operation);
CREATE INDEX idx_audit_log_db_user ON audit_log(db_user);

COMMENT ON TABLE audit_log IS 'Audit trail for all database modifications - required for compliance';

-- Function to capture audit context from application
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
    -- Get audit context
    v_app_user := NULLIF(current_setting('opendebt.audit_user', true), '');
    v_client_ip := NULLIF(current_setting('opendebt.audit_ip', true), '')::INET;
    v_client_app := NULLIF(current_setting('opendebt.audit_app', true), '');

    IF TG_OP = 'DELETE' THEN
        v_record_id := OLD.id;
        v_old_values := to_jsonb(OLD);
        v_new_values := NULL;
    ELSIF TG_OP = 'INSERT' THEN
        v_record_id := NEW.id;
        v_old_values := NULL;
        v_new_values := to_jsonb(NEW);
    ELSIF TG_OP = 'UPDATE' THEN
        v_record_id := NEW.id;
        v_old_values := to_jsonb(OLD);
        v_new_values := to_jsonb(NEW);
        -- Identify changed fields
        SELECT array_agg(key) INTO v_changed_fields
        FROM jsonb_each(v_new_values) n
        FULL OUTER JOIN jsonb_each(v_old_values) o USING (key)
        WHERE n.value IS DISTINCT FROM o.value;
    END IF;

    INSERT INTO audit_log (table_name, record_id, operation, old_values, new_values, 
                          changed_fields, application_user, client_ip, client_application)
    VALUES (TG_TABLE_NAME, v_record_id, TG_OP, v_old_values, v_new_values,
            v_changed_fields, v_app_user, v_client_ip, v_client_app);

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    ELSE
        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ============================================================================
-- DEBT TYPES (Reference Data)
-- ============================================================================

CREATE TABLE debt_types (
    code VARCHAR(20) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    category VARCHAR(50),
    description TEXT,
    legal_basis VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    requires_manual_review BOOLEAN DEFAULT FALSE,
    interest_applicable BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sys_period TSTZRANGE NOT NULL DEFAULT tstzrange(CURRENT_TIMESTAMP, NULL)
);

-- History table for debt_types
CREATE TABLE debt_types_history (
    LIKE debt_types,
    PRIMARY KEY (code, sys_period)
);

COMMENT ON TABLE debt_types IS 'Reference data for ~600 debt types';
COMMENT ON TABLE debt_types_history IS 'Historical versions of debt_types';

-- ============================================================================
-- DEBTS (Main Entity)
-- ============================================================================

CREATE TABLE debts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- References to Person Registry (NO PII stored here)
    debtor_person_id UUID NOT NULL,      -- Reference to persons table in person-registry
    creditor_org_id UUID NOT NULL,       -- Reference to organizations table in person-registry
    
    -- Debt details
    debt_type_code VARCHAR(20) NOT NULL REFERENCES debt_types(code),
    principal_amount NUMERIC(15, 2) NOT NULL,
    interest_amount NUMERIC(15, 2) DEFAULT 0,
    fees_amount NUMERIC(15, 2) DEFAULT 0,
    due_date DATE NOT NULL,
    original_due_date DATE,
    external_reference VARCHAR(100),
    
    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    readiness_status VARCHAR(20) NOT NULL DEFAULT 'PENDING_REVIEW',
    readiness_rejection_reason VARCHAR(500),
    readiness_validated_at TIMESTAMPTZ,
    readiness_validated_by VARCHAR(100),
    
    -- Metadata
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    
    -- Temporal support
    sys_period TSTZRANGE NOT NULL DEFAULT tstzrange(CURRENT_TIMESTAMP, NULL),
    
    -- Constraints
    CONSTRAINT chk_debt_status CHECK (status IN (
        'PENDING', 'ACTIVE', 'IN_COLLECTION', 'PARTIALLY_PAID',
        'PAID', 'WRITTEN_OFF', 'DISPUTED', 'CANCELLED'
    )),
    CONSTRAINT chk_readiness_status CHECK (readiness_status IN (
        'PENDING_REVIEW', 'READY_FOR_COLLECTION', 'NOT_READY', 'UNDER_APPEAL'
    )),
    CONSTRAINT chk_principal_positive CHECK (principal_amount > 0)
);

-- History table for debts (temporal/versioning)
CREATE TABLE debts_history (
    LIKE debts,
    PRIMARY KEY (id, sys_period)
);

-- ============================================================================
-- INDEXES
-- ============================================================================

-- Indexes for person/org lookups (technical IDs only - NO PII)
CREATE INDEX idx_debt_debtor_person_id ON debts(debtor_person_id);
CREATE INDEX idx_debt_creditor_org_id ON debts(creditor_org_id);
CREATE INDEX idx_debt_status ON debts(status);
CREATE INDEX idx_debt_readiness ON debts(readiness_status);
CREATE INDEX idx_debt_due_date ON debts(due_date);
CREATE INDEX idx_debt_type_code ON debts(debt_type_code);
CREATE INDEX idx_debt_created_at ON debts(created_at);

-- History table indexes
CREATE INDEX idx_debts_history_id ON debts_history(id);
CREATE INDEX idx_debts_history_period ON debts_history USING GIST (sys_period);

-- ============================================================================
-- TEMPORAL/HISTORY TRIGGERS
-- ============================================================================

-- Function to manage temporal versioning
CREATE OR REPLACE FUNCTION versioning_trigger_function()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'UPDATE' THEN
        -- Close the current version in history
        INSERT INTO debts_history SELECT OLD.*;
        UPDATE debts_history 
        SET sys_period = tstzrange(lower(sys_period), CURRENT_TIMESTAMP)
        WHERE id = OLD.id AND upper(sys_period) IS NULL;
        
        -- Update the new record
        NEW.sys_period := tstzrange(CURRENT_TIMESTAMP, NULL);
        NEW.updated_at := CURRENT_TIMESTAMP;
        NEW.version := OLD.version + 1;
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        -- Archive the deleted record
        INSERT INTO debts_history SELECT OLD.*;
        UPDATE debts_history 
        SET sys_period = tstzrange(lower(sys_period), CURRENT_TIMESTAMP)
        WHERE id = OLD.id AND upper(sys_period) IS NULL;
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Apply versioning trigger
CREATE TRIGGER debts_versioning_trigger
    BEFORE UPDATE OR DELETE ON debts
    FOR EACH ROW
    EXECUTE FUNCTION versioning_trigger_function();

-- Apply audit trigger
CREATE TRIGGER debts_audit_trigger
    AFTER INSERT OR UPDATE OR DELETE ON debts
    FOR EACH ROW
    EXECUTE FUNCTION audit_trigger_function();

CREATE TRIGGER debt_types_audit_trigger
    AFTER INSERT OR UPDATE OR DELETE ON debt_types
    FOR EACH ROW
    EXECUTE FUNCTION audit_trigger_function();

-- ============================================================================
-- HELPER FUNCTIONS
-- ============================================================================

-- Function to get debt history for a specific record
CREATE OR REPLACE FUNCTION get_debt_history(p_debt_id UUID)
RETURNS TABLE (
    id UUID,
    debtor_person_id UUID,
    status VARCHAR,
    principal_amount NUMERIC,
    valid_from TIMESTAMPTZ,
    valid_to TIMESTAMPTZ
) AS $$
BEGIN
    RETURN QUERY
    SELECT h.id, h.debtor_person_id, h.status, h.principal_amount,
           lower(h.sys_period) as valid_from, upper(h.sys_period) as valid_to
    FROM debts_history h
    WHERE h.id = p_debt_id
    UNION ALL
    SELECT d.id, d.debtor_person_id, d.status, d.principal_amount,
           lower(d.sys_period) as valid_from, NULL as valid_to
    FROM debts d
    WHERE d.id = p_debt_id
    ORDER BY valid_from;
END;
$$ LANGUAGE plpgsql;

-- Function to get debt as of a specific point in time
CREATE OR REPLACE FUNCTION get_debt_as_of(p_debt_id UUID, p_as_of TIMESTAMPTZ)
RETURNS debts AS $$
DECLARE
    v_result debts%ROWTYPE;
BEGIN
    -- Check current table first
    SELECT * INTO v_result FROM debts 
    WHERE id = p_debt_id AND sys_period @> p_as_of;
    
    IF FOUND THEN
        RETURN v_result;
    END IF;
    
    -- Check history
    SELECT * INTO v_result FROM debts_history 
    WHERE id = p_debt_id AND sys_period @> p_as_of;
    
    RETURN v_result;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- COMMENTS
-- ============================================================================

COMMENT ON TABLE debts IS 'Registered debts for collection with full history - NO PII STORED';
COMMENT ON TABLE debts_history IS 'Historical versions of debts (temporal table)';
COMMENT ON COLUMN debts.debtor_person_id IS 'Reference to person-registry.persons (UUID only, no PII)';
COMMENT ON COLUMN debts.creditor_org_id IS 'Reference to person-registry.organizations (UUID only, no PII)';
COMMENT ON COLUMN debts.sys_period IS 'System time period for temporal versioning';
COMMENT ON FUNCTION get_debt_history IS 'Returns complete history of a debt record';
COMMENT ON FUNCTION get_debt_as_of IS 'Returns debt state at a specific point in time';
COMMENT ON COLUMN debts.readiness_status IS 'Indrivelsesparathed status';
