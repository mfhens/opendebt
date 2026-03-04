-- OpenDebt Person Registry - GDPR Data Store
-- Database: PostgreSQL (Enterprise Grade)
-- This is the SINGLE SOURCE OF TRUTH for all personal data (PII)

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
CREATE INDEX idx_audit_log_app_user ON audit_log(application_user);

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

    -- IMPORTANT: Do not log encrypted values in audit - log field names only
    INSERT INTO audit_log (table_name, record_id, operation, old_values, new_values, 
                          changed_fields, application_user, client_ip, client_application)
    VALUES (TG_TABLE_NAME, v_record_id, TG_OP, 
            -- Redact encrypted fields from audit log
            v_old_values - ARRAY['identifier_encrypted', 'name_encrypted', 'address_street_encrypted', 
                                  'address_city_encrypted', 'address_postal_code_encrypted', 
                                  'address_country_encrypted', 'email_encrypted', 'phone_encrypted',
                                  'cvr_encrypted', 'address_encrypted', 'contact_email_encrypted', 
                                  'contact_phone_encrypted'],
            v_new_values - ARRAY['identifier_encrypted', 'name_encrypted', 'address_street_encrypted', 
                                  'address_city_encrypted', 'address_postal_code_encrypted', 
                                  'address_country_encrypted', 'email_encrypted', 'phone_encrypted',
                                  'cvr_encrypted', 'address_encrypted', 'contact_email_encrypted', 
                                  'contact_phone_encrypted'],
            v_changed_fields, v_app_user, v_client_ip, v_client_app);
    IF TG_OP = 'DELETE' THEN RETURN OLD; ELSE RETURN NEW; END IF;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ============================================================================
-- PERSONS TABLE (Natural persons and sole proprietors)
-- ============================================================================

CREATE TABLE persons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Identification (encrypted + hashed for lookup)
    identifier_encrypted BYTEA NOT NULL,
    identifier_type VARCHAR(3) NOT NULL CHECK (identifier_type IN ('CPR', 'CVR')),
    identifier_hash VARCHAR(64) NOT NULL,
    role VARCHAR(10) NOT NULL CHECK (role IN ('PERSONAL', 'BUSINESS')),
    
    -- PII fields (all encrypted)
    name_encrypted BYTEA,
    address_street_encrypted BYTEA,
    address_city_encrypted BYTEA,
    address_postal_code_encrypted BYTEA,
    address_country_encrypted BYTEA,
    email_encrypted BYTEA,
    phone_encrypted BYTEA,
    
    -- Communication preferences (not PII)
    digital_post_enabled BOOLEAN,
    eboks_enabled BOOLEAN,
    
    -- GDPR tracking
    consent_given_at TIMESTAMPTZ,
    consent_type VARCHAR(50),
    data_retention_until DATE,
    deletion_requested_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    deletion_reason VARCHAR(200),
    
    -- Access tracking
    last_accessed_at TIMESTAMPTZ,
    access_count BIGINT DEFAULT 0,
    
    -- Metadata
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    sys_period TSTZRANGE NOT NULL DEFAULT tstzrange(CURRENT_TIMESTAMP, NULL),
    
    -- Unique constraint: same identifier can have PERSONAL and BUSINESS roles
    CONSTRAINT uk_person_identifier_role UNIQUE (identifier_hash, role)
);

-- History table for persons
CREATE TABLE persons_history (
    LIKE persons,
    PRIMARY KEY (id, sys_period)
);

-- ============================================================================
-- ORGANIZATIONS TABLE (Creditors / Fordringshavere)
-- ============================================================================

CREATE TABLE organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Identification (encrypted + hashed)
    cvr_encrypted BYTEA NOT NULL,
    cvr_hash VARCHAR(64) NOT NULL UNIQUE,
    
    -- Organization data (encrypted)
    name_encrypted BYTEA,
    address_encrypted BYTEA,
    contact_email_encrypted BYTEA,
    contact_phone_encrypted BYTEA,
    
    -- Organization type (not PII)
    organization_type VARCHAR(30) CHECK (organization_type IN (
        'MUNICIPALITY', 'REGION', 'STATE_AGENCY', 'PUBLIC_INSTITUTION', 'OTHER'
    )),
    
    -- Status
    active BOOLEAN NOT NULL DEFAULT TRUE,
    onboarded_at TIMESTAMPTZ,
    
    -- Metadata
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    sys_period TSTZRANGE NOT NULL DEFAULT tstzrange(CURRENT_TIMESTAMP, NULL)
);

-- History table for organizations
CREATE TABLE organizations_history (
    LIKE organizations,
    PRIMARY KEY (id, sys_period)
);

-- ============================================================================
-- INDEXES
-- ============================================================================

CREATE INDEX idx_person_identifier_hash ON persons(identifier_hash);
CREATE INDEX idx_person_identifier_role ON persons(identifier_hash, role);
CREATE INDEX idx_person_deleted ON persons(deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_person_retention ON persons(data_retention_until) WHERE deleted_at IS NULL;
CREATE INDEX idx_person_deletion_pending ON persons(deletion_requested_at) WHERE deleted_at IS NULL;

CREATE INDEX idx_persons_history_id ON persons_history(id);
CREATE INDEX idx_persons_history_period ON persons_history USING GIST (sys_period);

CREATE INDEX idx_org_cvr_hash ON organizations(cvr_hash);
CREATE INDEX idx_org_active ON organizations(active) WHERE active = TRUE;

CREATE INDEX idx_orgs_history_id ON organizations_history(id);
CREATE INDEX idx_orgs_history_period ON organizations_history USING GIST (sys_period);

-- ============================================================================
-- TEMPORAL TRIGGERS
-- ============================================================================

CREATE OR REPLACE FUNCTION persons_versioning_trigger()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'UPDATE' THEN
        INSERT INTO persons_history SELECT OLD.*;
        UPDATE persons_history SET sys_period = tstzrange(lower(sys_period), CURRENT_TIMESTAMP)
        WHERE id = OLD.id AND upper(sys_period) IS NULL;
        NEW.sys_period := tstzrange(CURRENT_TIMESTAMP, NULL);
        NEW.updated_at := CURRENT_TIMESTAMP;
        NEW.version := OLD.version + 1;
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        INSERT INTO persons_history SELECT OLD.*;
        UPDATE persons_history SET sys_period = tstzrange(lower(sys_period), CURRENT_TIMESTAMP)
        WHERE id = OLD.id AND upper(sys_period) IS NULL;
        RETURN OLD;
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION organizations_versioning_trigger()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'UPDATE' THEN
        INSERT INTO organizations_history SELECT OLD.*;
        UPDATE organizations_history SET sys_period = tstzrange(lower(sys_period), CURRENT_TIMESTAMP)
        WHERE id = OLD.id AND upper(sys_period) IS NULL;
        NEW.sys_period := tstzrange(CURRENT_TIMESTAMP, NULL);
        NEW.updated_at := CURRENT_TIMESTAMP;
        NEW.version := OLD.version + 1;
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        INSERT INTO organizations_history SELECT OLD.*;
        UPDATE organizations_history SET sys_period = tstzrange(lower(sys_period), CURRENT_TIMESTAMP)
        WHERE id = OLD.id AND upper(sys_period) IS NULL;
        RETURN OLD;
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER persons_versioning BEFORE UPDATE OR DELETE ON persons
    FOR EACH ROW EXECUTE FUNCTION persons_versioning_trigger();

CREATE TRIGGER organizations_versioning BEFORE UPDATE OR DELETE ON organizations
    FOR EACH ROW EXECUTE FUNCTION organizations_versioning_trigger();

CREATE TRIGGER persons_audit AFTER INSERT OR UPDATE OR DELETE ON persons
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

CREATE TRIGGER organizations_audit AFTER INSERT OR UPDATE OR DELETE ON organizations
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

-- ============================================================================
-- COMMENTS
-- ============================================================================

COMMENT ON TABLE persons IS 'GDPR data store - SINGLE SOURCE OF TRUTH for all personal data';
COMMENT ON TABLE organizations IS 'Creditor/fordringshaver registry';
COMMENT ON COLUMN persons.identifier_hash IS 'SHA-256 hash for lookup without exposing PII';
COMMENT ON COLUMN persons.identifier_encrypted IS 'AES-256-GCM encrypted CPR/CVR';
COMMENT ON COLUMN persons.role IS 'PERSONAL for private person, BUSINESS for sole proprietor or company';
COMMENT ON COLUMN persons.data_retention_until IS 'Date when data should be deleted per retention policy';
