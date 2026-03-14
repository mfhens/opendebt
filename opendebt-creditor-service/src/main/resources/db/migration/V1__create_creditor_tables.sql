-- OpenDebt Creditor Service - Operational Creditor Model
-- Database: PostgreSQL (Enterprise Grade)
-- This service owns all operational creditor configuration and permissions.
-- Organization identity data (name, address, CVR) is stored in Person Registry.

-- ============================================================================
-- EXTENSIONS
-- ============================================================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================================
-- AUDIT INFRASTRUCTURE
-- ============================================================================

CREATE TABLE IF NOT EXISTS audit_log (
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

CREATE INDEX IF NOT EXISTS idx_audit_log_table ON audit_log(table_name);
CREATE INDEX IF NOT EXISTS idx_audit_log_record ON audit_log(record_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_timestamp ON audit_log(timestamp);
CREATE INDEX IF NOT EXISTS idx_audit_log_app_user ON audit_log(application_user);

-- Audit context function
CREATE OR REPLACE FUNCTION set_audit_context(p_user VARCHAR, p_ip INET DEFAULT NULL, p_app VARCHAR DEFAULT NULL)
RETURNS VOID AS $$
BEGIN
    PERFORM set_config('opendebt.audit_user', p_user, true);
    PERFORM set_config('opendebt.audit_ip', COALESCE(p_ip::text, ''), true);
    PERFORM set_config('opendebt.audit_app', COALESCE(p_app, ''), true);
END;
$$ LANGUAGE plpgsql;

-- Generic audit trigger
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
-- CREDITORS TABLE
-- ============================================================================

CREATE TABLE creditors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Organization reference (FK to Person Registry via technical UUID)
    creditor_org_id UUID NOT NULL,
    
    -- Core identity
    external_creditor_id VARCHAR(50) NOT NULL UNIQUE,
    auto_created BOOLEAN NOT NULL DEFAULT FALSE,
    unique_link_id VARCHAR(30),
    sorting_id VARCHAR(8) CHECK (sorting_id IS NULL OR sorting_id ~ '^[A-Z0-9]{1,8}$'),
    captia_id VARCHAR(15) CHECK (captia_id IS NULL OR captia_id ~ '^[0-9]{2}-[0-9]{7}$'),
    
    -- Parent-child hierarchy (self-referencing)
    parent_creditor_id UUID REFERENCES creditors(id),
    system_reporter_id VARCHAR(50),
    
    -- Notification preferences
    interest_notifications BOOLEAN NOT NULL DEFAULT FALSE,
    detailed_interest_notifications BOOLEAN NOT NULL DEFAULT FALSE,
    equalisation_notifications BOOLEAN NOT NULL DEFAULT FALSE,
    allocation_notifications BOOLEAN NOT NULL DEFAULT FALSE,
    settlement_notifications BOOLEAN NOT NULL DEFAULT FALSE,
    return_notifications BOOLEAN NOT NULL DEFAULT FALSE,
    write_off_notifications BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- Action permissions
    allow_portal_actions BOOLEAN NOT NULL DEFAULT FALSE,
    allow_write_down BOOLEAN NOT NULL DEFAULT FALSE,
    allow_create_recovery_claims BOOLEAN NOT NULL DEFAULT FALSE,
    allow_create_offset_claims BOOLEAN NOT NULL DEFAULT FALSE,
    allow_create_transports BOOLEAN NOT NULL DEFAULT FALSE,
    allow_withdraw BOOLEAN NOT NULL DEFAULT FALSE,
    allow_write_up_reversed_write_down_adjustment BOOLEAN NOT NULL DEFAULT FALSE,
    allow_write_up_cancelled_write_down_payment BOOLEAN NOT NULL DEFAULT FALSE,
    allow_write_down_cancelled_write_up_adjustment BOOLEAN NOT NULL DEFAULT FALSE,
    allow_write_down_cancelled_write_up_payment BOOLEAN NOT NULL DEFAULT FALSE,
    allow_incorrect_principal_report BOOLEAN NOT NULL DEFAULT FALSE,
    allow_write_up_adjustment BOOLEAN NOT NULL DEFAULT FALSE,
    allow_resubmit_claims BOOLEAN NOT NULL DEFAULT FALSE,
    allow_reject_offset_claims BOOLEAN NOT NULL DEFAULT FALSE,
    allow_auto_cancel_hearing BOOLEAN NOT NULL DEFAULT FALSE,
    auto_cancel_hearing_days INTEGER CHECK (auto_cancel_hearing_days IS NULL OR auto_cancel_hearing_days > 0),
    
    -- Settlement configuration
    currency_code VARCHAR(3) CHECK (currency_code ~ '^[A-Z]{3}$') DEFAULT 'DKK',
    settlement_frequency VARCHAR(20) DEFAULT 'MONTHLY',
    settlement_method VARCHAR(20) CHECK (settlement_method IN ('BANK_TRANSFER', 'NEM_KONTO', 'STATSREGNSKAB')),
    iban VARCHAR(34),
    swift_code VARCHAR(11),
    danish_account_number VARCHAR(14),
    
    -- Classification
    business_unit VARCHAR(10),
    creditor_type VARCHAR(30) CHECK (creditor_type IN ('OTHER_PUBLIC', 'MUNICIPAL', 'PRIVATE', 'REGIONAL', 'SKAT', 'STATE', 'FOREIGN')),
    payment_type VARCHAR(30) CHECK (payment_type IN ('EXTERNAL', 'EXTERNAL_WITH_INCOME', 'INTERNAL', 'RIM')),
    adjustment_type_profile VARCHAR(10) CHECK (adjustment_type_profile IN ('ER', 'EX', 'IN', 'RM', 'SP')),
    uses_dmi BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- Status and lifecycle
    dcs_status VARCHAR(30) CHECK (dcs_status IN ('SKAL_I_DCS', 'INDSENDT', 'OPRETTET', 'OPSAMLING_SL', 'SLETTET', 'INTERN', 'EJ_UDLAND')),
    system_agreement_active BOOLEAN NOT NULL DEFAULT FALSE,
    activity_status VARCHAR(40) CHECK (activity_status IN ('ACTIVE', 'DELETED', 'TEMPORARILY_CLOSED', 'NO_AGREEMENT', 'DEACTIVATED_CEASED', 'DEACTIVATED_NO_AGREEMENT', 'DELETED_CEASED', 'DELETED_NO_AGREEMENT')) DEFAULT 'ACTIVE',
    connection_type VARCHAR(20) CHECK (connection_type IN ('SYSTEM', 'HYBRID', 'PORTAL', 'PENDING', 'NONE')),
    ip_whitelisted BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- Audit fields
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    sys_period TSTZRANGE NOT NULL DEFAULT tstzrange(CURRENT_TIMESTAMP, NULL),
    
    -- Invariant constraints
    CONSTRAINT chk_interest_notifications_exclusivity 
        CHECK (NOT (interest_notifications = TRUE AND detailed_interest_notifications = TRUE)),
    CONSTRAINT chk_settlement_notifications_exclusivity 
        CHECK (NOT (equalisation_notifications = TRUE AND allocation_notifications = TRUE)),
    CONSTRAINT chk_auto_cancel_hearing_days 
        CHECK (allow_auto_cancel_hearing = FALSE OR auto_cancel_hearing_days IS NOT NULL),
    CONSTRAINT chk_nem_konto_no_bank_details 
        CHECK (settlement_method != 'NEM_KONTO' OR (iban IS NULL AND swift_code IS NULL AND danish_account_number IS NULL)),
    CONSTRAINT chk_statsregnskab_no_bank_details 
        CHECK (settlement_method != 'STATSREGNSKAB' OR (iban IS NULL AND swift_code IS NULL AND danish_account_number IS NULL)),
    CONSTRAINT chk_no_self_parent 
        CHECK (parent_creditor_id IS NULL OR parent_creditor_id != id)
);

-- History table for creditors
CREATE TABLE creditors_history (
    LIKE creditors,
    PRIMARY KEY (id, sys_period)
);

-- ============================================================================
-- INDEXES
-- ============================================================================

CREATE INDEX idx_creditor_org_id ON creditors(creditor_org_id);
CREATE INDEX idx_parent_creditor_id ON creditors(parent_creditor_id);
CREATE INDEX idx_creditor_type ON creditors(creditor_type);
CREATE INDEX idx_activity_status ON creditors(activity_status);
CREATE INDEX idx_dcs_status ON creditors(dcs_status);
CREATE INDEX idx_system_agreement_active ON creditors(system_agreement_active) WHERE system_agreement_active = TRUE;

CREATE INDEX idx_creditors_history_id ON creditors_history(id);
CREATE INDEX idx_creditors_history_period ON creditors_history USING GIST (sys_period);

-- ============================================================================
-- TEMPORAL TRIGGERS
-- ============================================================================

CREATE OR REPLACE FUNCTION creditors_versioning_trigger()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'UPDATE' THEN
        INSERT INTO creditors_history SELECT OLD.*;
        UPDATE creditors_history SET sys_period = tstzrange(lower(sys_period), CURRENT_TIMESTAMP)
        WHERE id = OLD.id AND upper(sys_period) IS NULL;
        NEW.sys_period := tstzrange(CURRENT_TIMESTAMP, NULL);
        NEW.updated_at := CURRENT_TIMESTAMP;
        NEW.version := OLD.version + 1;
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        INSERT INTO creditors_history SELECT OLD.*;
        UPDATE creditors_history SET sys_period = tstzrange(lower(sys_period), CURRENT_TIMESTAMP)
        WHERE id = OLD.id AND upper(sys_period) IS NULL;
        RETURN OLD;
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER creditors_versioning BEFORE UPDATE OR DELETE ON creditors
    FOR EACH ROW EXECUTE FUNCTION creditors_versioning_trigger();

CREATE TRIGGER creditors_audit AFTER INSERT OR UPDATE OR DELETE ON creditors
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

-- ============================================================================
-- COMMENTS
-- ============================================================================

COMMENT ON TABLE creditors IS 'Operational creditor (fordringshaver) configuration and permissions';
COMMENT ON COLUMN creditors.creditor_org_id IS 'Technical UUID referencing organization in Person Registry';
COMMENT ON COLUMN creditors.external_creditor_id IS 'Legacy unique creditor ID from AutoTool';
COMMENT ON COLUMN creditors.parent_creditor_id IS 'Self-referencing FK for umbrella creditor hierarchy';
COMMENT ON COLUMN creditors.interest_notifications IS 'Mutually exclusive with detailed_interest_notifications';
COMMENT ON COLUMN creditors.equalisation_notifications IS 'Mutually exclusive with allocation_notifications';
COMMENT ON COLUMN creditors.settlement_method IS 'NEM_KONTO and STATSREGNSKAB require null bank details';
COMMENT ON COLUMN creditors.sorting_id IS 'SVC_TYPE_CD, uppercase alphanumeric, max 8 chars';
