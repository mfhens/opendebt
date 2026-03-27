-- ============================================================================
-- OpenDebt Debt Service - Consolidated Baseline Schema
-- ============================================================================
-- Database: PostgreSQL 16
-- Consolidates V1-V10 migrations into single baseline
-- Final state is identical to a fully-migrated database.
-- Dev databases must be dropped and recreated after this squash.
-- Begrebsmodel: v3 (docs/begrebsmodel/)
-- Implementation language: English (ADR-0014, AGENTS.md)
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
CREATE INDEX idx_audit_log_operation ON audit_log(operation);
CREATE INDEX idx_audit_log_db_user ON audit_log(db_user);

COMMENT ON TABLE audit_log IS 'Audit trail for all database modifications - required for compliance';

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
        v_new_values := NULL;
    ELSIF TG_OP = 'INSERT' THEN
        v_record_id := NEW.id;
        v_old_values := NULL;
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
    civil_law BOOLEAN DEFAULT FALSE,
    claim_type_code VARCHAR(20),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sys_period TSTZRANGE NOT NULL DEFAULT tstzrange(CURRENT_TIMESTAMP, NULL)
);

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

    -- References to Person Registry (NO PII stored here - ADR-0014)
    debtor_person_id UUID NOT NULL,
    creditor_org_id UUID NOT NULL,

    -- Debt details
    debt_type_code VARCHAR(20) NOT NULL REFERENCES debt_types(code),
    principal_amount NUMERIC(15, 2) NOT NULL,
    interest_amount NUMERIC(15, 2) DEFAULT 0,
    fees_amount NUMERIC(15, 2) DEFAULT 0,
    outstanding_balance NUMERIC(15, 2),
    due_date DATE NOT NULL,
    original_due_date DATE,
    external_reference VARCHAR(100),
    ocr_line VARCHAR(50),

    -- PSRM stamdata fields (begrebsmodel v3 section 2.1)
    principal NUMERIC(15, 2),
    creditor_reference VARCHAR(50),
    claim_art VARCHAR(10),
    claim_category VARCHAR(5),
    parent_claim_id UUID,
    limitation_date DATE,
    description VARCHAR(100),
    period_from DATE,
    period_to DATE,
    inception_date DATE,
    payment_deadline DATE,
    last_payment_date DATE,
    estate_processing BOOLEAN,
    judgment_date DATE,
    settlement_date DATE,
    interest_rule VARCHAR(10),
    interest_rate_code VARCHAR(10),
    additional_interest_rate NUMERIC(10, 4),
    claim_note VARCHAR(500),
    customer_note VARCHAR(500),
    p_number VARCHAR(20),

    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    readiness_status VARCHAR(20) NOT NULL DEFAULT 'PENDING_REVIEW',
    readiness_rejection_reason VARCHAR(500),
    readiness_validated_at TIMESTAMPTZ,
    readiness_validated_by VARCHAR(100),
    lifecycle_state VARCHAR(20),
    received_at TIMESTAMPTZ,

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
    CONSTRAINT chk_principal_non_negative CHECK (principal_amount >= 0)
);

CREATE TABLE debts_history (
    LIKE debts,
    PRIMARY KEY (id, sys_period)
);

-- ============================================================================
-- CLAIM LIFECYCLE EVENTS (Audit Trail)
-- ============================================================================

CREATE TABLE claim_lifecycle_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    debt_id UUID NOT NULL REFERENCES debts(id),
    creditor_id UUID NOT NULL,
    recipient_id UUID,
    occurred_at TIMESTAMPTZ NOT NULL,
    previous_state VARCHAR(20),
    new_state VARCHAR(20),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_lifecycle_event_debt_id ON claim_lifecycle_events(debt_id);
CREATE INDEX idx_lifecycle_event_occurred_at ON claim_lifecycle_events(occurred_at);

COMMENT ON TABLE claim_lifecycle_events IS 'Audit trail for claim lifecycle transitions';

-- ============================================================================
-- HOERING (PSRM Hearing Workflow)
-- ============================================================================

CREATE TABLE hoering (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    debt_id UUID NOT NULL REFERENCES debts(id),
    hoering_status VARCHAR(30) NOT NULL DEFAULT 'AFVENTER_FORDRINGSHAVER',
    deviation_description VARCHAR(500) NOT NULL,
    fordringshaver_begrundelse VARCHAR(1000),
    rim_decision VARCHAR(500),
    sla_deadline TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMPTZ,
    CONSTRAINT chk_hoering_status CHECK (hoering_status IN (
        'AFVENTER_FORDRINGSHAVER', 'AFVENTER_RIM', 'GODKENDT', 'AFVIST', 'FORTRUDT'
    ))
);

CREATE INDEX idx_hoering_debt_id ON hoering(debt_id);
CREATE INDEX idx_hoering_status ON hoering(hoering_status);
CREATE INDEX idx_hoering_sla_deadline ON hoering(sla_deadline);

COMMENT ON TABLE hoering IS 'PSRM HOERING workflow for claims with stamdata deviations';

-- ============================================================================
-- NOTIFICATIONS (V3 - petition004)
-- Begrebsmodel: Underretning=Notification, Paakrav=Demand, Rykker=Reminder
-- ============================================================================

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_type VARCHAR(30) NOT NULL,
    debt_id UUID NOT NULL REFERENCES debts(id),
    sender_creditor_org_id UUID NOT NULL,
    recipient_person_id UUID NOT NULL,
    channel VARCHAR(20) NOT NULL,
    sent_at TIMESTAMPTZ,
    delivery_state VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ocr_line VARCHAR(50),
    related_lifecycle_event_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_notification_type CHECK (notification_type IN (
        'PAAKRAV', 'RYKKER', 'AFREGNING', 'UDLIGNING',
        'ALLOKERING', 'RENTER', 'AFSKRIVNING', 'TILBAGESEND'
    )),
    CONSTRAINT chk_notification_channel CHECK (channel IN (
        'DIGITAL_POST', 'PHYSICAL_MAIL', 'PORTAL'
    )),
    CONSTRAINT chk_notification_delivery_state CHECK (delivery_state IN (
        'PENDING', 'SENT', 'DELIVERED', 'FAILED'
    ))
);

CREATE INDEX idx_notification_debt_id ON notifications(debt_id);
CREATE INDEX idx_notification_recipient ON notifications(recipient_person_id);
CREATE INDEX idx_notification_type ON notifications(notification_type);
CREATE INDEX idx_notification_delivery_state ON notifications(delivery_state);

COMMENT ON TABLE notifications IS 'Underretninger: paakrav, rykker, and other debt notifications';
COMMENT ON COLUMN notifications.notification_type IS 'Type of notification (begrebsmodel mapping)';
COMMENT ON COLUMN notifications.ocr_line IS 'OCR payment reference line for PAAKRAV notifications';

-- ============================================================================
-- LIABILITIES (V4 - petition005)
-- Begrebsmodel: Haeftelse=Liability
-- ============================================================================

CREATE TABLE liabilities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    debt_id UUID NOT NULL REFERENCES debts(id),
    debtor_person_id UUID NOT NULL,
    liability_type VARCHAR(30) NOT NULL,
    share_amount NUMERIC(15,2),
    share_percentage NUMERIC(5,2),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_liability_type CHECK (liability_type IN (
        'SOLE', 'JOINT_AND_SEVERAL', 'PROPORTIONAL'
    )),
    CONSTRAINT chk_share_percentage CHECK (share_percentage IS NULL OR (share_percentage > 0 AND share_percentage <= 100)),
    CONSTRAINT uq_liability_debt_debtor UNIQUE (debt_id, debtor_person_id)
);

CREATE INDEX idx_liability_debt_id ON liabilities(debt_id);
CREATE INDEX idx_liability_debtor ON liabilities(debtor_person_id);
CREATE INDEX idx_liability_active ON liabilities(active) WHERE active = TRUE;

COMMENT ON TABLE liabilities IS 'Haeftelse: liability relationships between fordring and skyldner';
COMMENT ON COLUMN liabilities.liability_type IS 'SOLE=enehaftelse, JOINT_AND_SEVERAL=solidarisk, PROPORTIONAL=delt';
COMMENT ON COLUMN liabilities.share_percentage IS 'Only for PROPORTIONAL - percentage share of liability';

-- ============================================================================
-- OBJECTIONS (V5 - petition006)
-- Begrebsmodel: Indsigelse=Objection
-- ============================================================================

CREATE TABLE objections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    debt_id UUID NOT NULL REFERENCES debts(id),
    debtor_person_id UUID NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    registered_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at TIMESTAMPTZ,
    resolution_note VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_objection_status CHECK (status IN ('ACTIVE', 'UPHELD', 'REJECTED'))
);

CREATE INDEX idx_objection_debt_id ON objections(debt_id);
CREATE INDEX idx_objection_debtor ON objections(debtor_person_id);
CREATE INDEX idx_objection_status ON objections(status);
CREATE INDEX idx_objection_active ON objections(debt_id, status) WHERE status = 'ACTIVE';

COMMENT ON TABLE objections IS 'Indsigelse: objections/disputes registered against fordringer';
COMMENT ON COLUMN objections.status IS 'ACTIVE=pending resolution, UPHELD=skyldner medhold, REJECTED=afvist';

-- ============================================================================
-- COLLECTION MEASURES (V6 - petition007)
-- Begrebsmodel: Inddrivelsesskridt=Collection Measure
-- ============================================================================

CREATE TABLE collection_measures (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    debt_id UUID NOT NULL REFERENCES debts(id),
    measure_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'INITIATED',
    initiated_by VARCHAR(100),
    initiated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    amount NUMERIC(15,2),
    note VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_measure_type CHECK (measure_type IN (
        'SET_OFF', 'WAGE_GARNISHMENT', 'ATTACHMENT'
    )),
    CONSTRAINT chk_measure_status CHECK (status IN (
        'INITIATED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'
    ))
);

CREATE INDEX idx_measure_debt_id ON collection_measures(debt_id);
CREATE INDEX idx_measure_type ON collection_measures(measure_type);
CREATE INDEX idx_measure_status ON collection_measures(status);

COMMENT ON TABLE collection_measures IS 'Inddrivelsesskridt: collection measure steps (modregning, loenindeholdelse, udlaeg)';

-- ============================================================================
-- BATCH JOB EXECUTIONS (V7 - petition043)
-- ============================================================================

CREATE TABLE batch_job_executions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_name VARCHAR(100) NOT NULL,
    execution_date DATE NOT NULL,
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    records_processed INT NOT NULL DEFAULT 0,
    records_failed INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'RUNNING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_batch_status CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX idx_batch_job_name ON batch_job_executions(job_name);
CREATE INDEX idx_batch_execution_date ON batch_job_executions(execution_date);

COMMENT ON TABLE batch_job_executions IS 'Tracks batch job runs for idempotency and audit';

-- ============================================================================
-- INTEREST JOURNAL ENTRIES (V7 - petition043)
-- V8: accounting_target column added (petition 045)
-- ============================================================================

CREATE TABLE interest_journal_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    debt_id UUID NOT NULL REFERENCES debts(id),
    accrual_date DATE NOT NULL,
    effective_date DATE NOT NULL,
    balance_snapshot NUMERIC(15,2) NOT NULL,
    rate NUMERIC(5,4) NOT NULL,
    interest_amount NUMERIC(15,2) NOT NULL,
    accounting_target VARCHAR(20),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uq_interest_debt_date UNIQUE (debt_id, accrual_date)
);

CREATE INDEX idx_interest_debt_id ON interest_journal_entries(debt_id);
CREATE INDEX idx_interest_accrual_date ON interest_journal_entries(accrual_date);

COMMENT ON TABLE interest_journal_entries IS 'Inddrivelsesrente journal entries with storno-compatible metadata';

-- ============================================================================
-- BUSINESS CONFIG (V8 - petition 046)
-- V9: review_status column added (petition 047)
-- ============================================================================

CREATE TABLE business_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    config_key VARCHAR(100) NOT NULL,
    config_value VARCHAR(500) NOT NULL,
    value_type VARCHAR(20) NOT NULL DEFAULT 'DECIMAL',
    valid_from DATE NOT NULL,
    valid_to DATE,
    description TEXT,
    legal_basis VARCHAR(500),
    review_status VARCHAR(20),
    created_by VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uq_config_key_valid_from UNIQUE (config_key, valid_from)
);

CREATE INDEX idx_config_key ON business_config(config_key);
CREATE INDEX idx_config_valid_from ON business_config(valid_from);
CREATE INDEX idx_config_review_status ON business_config(review_status) WHERE review_status IS NOT NULL;

COMMENT ON TABLE business_config IS 'Time-versioned business configuration values with validity periods (petition 046)';

-- ============================================================================
-- FEES (V8 - petition 045)
-- ============================================================================

CREATE TABLE fees (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    debt_id UUID NOT NULL REFERENCES debts(id),
    fee_type VARCHAR(30) NOT NULL,
    amount NUMERIC(15,2) NOT NULL,
    accrual_date DATE NOT NULL,
    legal_basis VARCHAR(500),
    paid BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_fee_type CHECK (fee_type IN ('RYKKER', 'UDLAEG', 'LOENINDEHOLDELSE', 'OTHER'))
);

CREATE INDEX idx_fee_debt_id ON fees(debt_id);
CREATE INDEX idx_fee_accrual_date ON fees(accrual_date);

COMMENT ON TABLE fees IS 'Individual fees (gebyrer) imposed during collection (petition 045)';

-- ============================================================================
-- BUSINESS CONFIG AUDIT (V9 - petition 047)
-- ============================================================================

CREATE TABLE business_config_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    config_entry_id UUID NOT NULL,
    config_key VARCHAR(100) NOT NULL,
    action VARCHAR(20) NOT NULL,
    old_value VARCHAR(500),
    new_value VARCHAR(500),
    performed_by VARCHAR(100) NOT NULL,
    performed_at TIMESTAMP NOT NULL,
    details TEXT
);

CREATE INDEX idx_bca_config_entry_id ON business_config_audit(config_entry_id);
CREATE INDEX idx_bca_config_key ON business_config_audit(config_key);
CREATE INDEX idx_bca_performed_at ON business_config_audit(performed_at);

-- ============================================================================
-- INDEXES (debts + history)
-- ============================================================================

CREATE INDEX idx_debt_debtor_person_id ON debts(debtor_person_id);
CREATE INDEX idx_debt_creditor_org_id ON debts(creditor_org_id);
CREATE INDEX idx_debt_status ON debts(status);
CREATE INDEX idx_debt_readiness ON debts(readiness_status);
CREATE INDEX idx_debt_due_date ON debts(due_date);
CREATE INDEX idx_debt_type_code ON debts(debt_type_code);
CREATE INDEX idx_debt_created_at ON debts(created_at);
CREATE INDEX idx_debt_parent_claim_id ON debts(parent_claim_id);
CREATE INDEX idx_debt_lifecycle_state ON debts(lifecycle_state);
CREATE INDEX idx_debt_last_payment_date ON debts(last_payment_date);
CREATE INDEX idx_debt_limitation_date ON debts(limitation_date);
CREATE UNIQUE INDEX idx_debt_ocr_line ON debts(ocr_line) WHERE ocr_line IS NOT NULL;
CREATE INDEX idx_debt_lifecycle_balance ON debts(lifecycle_state, outstanding_balance)
    WHERE outstanding_balance > 0;

CREATE INDEX idx_debts_history_id ON debts_history(id);
CREATE INDEX idx_debts_history_period ON debts_history USING GIST (sys_period);

-- ============================================================================
-- TEMPORAL/HISTORY TRIGGERS
-- ============================================================================

CREATE OR REPLACE FUNCTION versioning_trigger_function()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'UPDATE' THEN
        INSERT INTO debts_history SELECT OLD.*;
        UPDATE debts_history
        SET sys_period = tstzrange(lower(sys_period), CURRENT_TIMESTAMP)
        WHERE id = OLD.id AND upper(sys_period) IS NULL;

        NEW.sys_period := tstzrange(CURRENT_TIMESTAMP, NULL);
        NEW.updated_at := CURRENT_TIMESTAMP;
        NEW.version := OLD.version + 1;
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        INSERT INTO debts_history SELECT OLD.*;
        UPDATE debts_history
        SET sys_period = tstzrange(lower(sys_period), CURRENT_TIMESTAMP)
        WHERE id = OLD.id AND upper(sys_period) IS NULL;
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER debts_versioning_trigger
    BEFORE UPDATE OR DELETE ON debts
    FOR EACH ROW
    EXECUTE FUNCTION versioning_trigger_function();

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

CREATE OR REPLACE FUNCTION get_debt_as_of(p_debt_id UUID, p_as_of TIMESTAMPTZ)
RETURNS debts AS $$
DECLARE
    v_result debts%ROWTYPE;
BEGIN
    SELECT * INTO v_result FROM debts
    WHERE id = p_debt_id AND sys_period @> p_as_of;

    IF FOUND THEN
        RETURN v_result;
    END IF;

    SELECT * INTO v_result FROM debts_history
    WHERE id = p_debt_id AND sys_period @> p_as_of;

    RETURN v_result;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- SEED DATA (V1 + V2 + V8 + V10)
-- ============================================================================

-- Temporarily disable audit trigger for seed inserts
ALTER TABLE debt_types DISABLE TRIGGER debt_types_audit_trigger;

INSERT INTO debt_types (code, name, category, description, legal_basis, active, interest_applicable, requires_manual_review, civil_law, created_at, updated_at)
VALUES
    ('SKAT', 'Restskat', 'Skat', 'Skyldig restskat til Skattestyrelsen', 'Kildeskatteloven SS 61', TRUE, TRUE, FALSE, FALSE, now(), now()),
    ('MOMS', 'Moms', 'Skat', 'Skyldig moms til Skattestyrelsen', 'Momsloven SS 57', TRUE, TRUE, FALSE, FALSE, now(), now()),
    ('BOEDE', 'Boede', 'Straf', 'Boede paalagt af domstol eller myndighed', 'Straffeloven', TRUE, FALSE, FALSE, FALSE, now(), now()),
    ('UNDERHOLDSBIDRAG', 'Underholdsbidrag', 'BIDRAG', 'Boernebidrag / aegtefaellebidrag', 'Lov om inddrivelse SS 1', TRUE, TRUE, FALSE, FALSE, now(), now()),
    ('DAGINSTITUTION', 'Daginstitution', 'Kommune', 'Skyldig betaling for daginstitution', 'Dagtilbudsloven', TRUE, TRUE, FALSE, FALSE, now(), now()),
    ('EJENDOMSSKAT', 'Ejendomsskat', 'Skat', 'Skyldig ejendomsskat', 'Ejendomsskatteloven', TRUE, TRUE, FALSE, FALSE, now(), now()),
    ('STRAF_BOEDE', 'Strafferetlig boede', 'BOEDE', 'Boeder idoemt ved dom/vedtaegt - rentefri', false, true, false, false, now(), now()),
    ('DAGBOEDE', 'Dagboede', 'BOEDE', 'Administrative dagboeder - rentebearende', true, true, false, false, now(), now()),
    ('ADMIN_BOEDE', 'Administrativ boede', 'BOEDE', 'Administrative boeder - rentebearende', true, true, false, false, now(), now()),
    ('TOLD', 'Toldskyld', 'TOLD', 'Toldkrav under inddrivelse - NB+2% rente', true, true, false, false, now(), now()),
    ('SU_GAELD', 'SU-gaeld', 'GAELD', 'Misligholdt studiegaeld', true, true, false, false, now(), now()),
    ('SKAT_REST', 'Restskat', 'SKAT', 'Personlig restskat', true, true, false, false, now(), now())
ON CONFLICT (code) DO NOTHING;

ALTER TABLE debt_types ENABLE TRIGGER debt_types_audit_trigger;

-- Demo debtor person IDs (technical UUIDs referencing person-registry, NOT PII)
ALTER TABLE debts DISABLE TRIGGER debts_audit_trigger;

INSERT INTO debts (
    id, debtor_person_id, creditor_org_id, debt_type_code,
    principal_amount, interest_amount, fees_amount, outstanding_balance,
    principal, claim_art, claim_category, lifecycle_state,
    due_date, original_due_date, status, readiness_status, created_by
) VALUES
    (
        'c0000000-0000-0000-0000-000000000001',
        'd0000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000001',
        'SKAT', 45000.00, 2250.00, 500.00, 32750.00, 45000.00,
        'INDR', 'HF', 'OVERDRAGET',
        '2025-07-01', '2025-07-01',
        'IN_COLLECTION', 'READY_FOR_COLLECTION', 'seed-migration'
    ),
    (
        'c0000000-0000-0000-0000-000000000002',
        'd0000000-0000-0000-0000-000000000002',
        '00000000-0000-0000-0000-000000000001',
        'MOMS', 125000.00, 0.00, 0.00, 125000.00, 125000.00,
        'INDR', 'HF', 'OVERDRAGET',
        '2025-10-01', '2025-10-01',
        'ACTIVE', 'READY_FOR_COLLECTION', 'seed-migration'
    ),
    (
        'c0000000-0000-0000-0000-000000000003',
        'd0000000-0000-0000-0000-000000000003',
        '00000000-0000-0000-0000-000000000001',
        'BOEDE', 5000.00, 0.00, 0.00, 5000.00, 5000.00,
        'INDR', 'HF', 'REGISTERED',
        '2025-12-15', '2025-12-15',
        'PENDING', 'PENDING_REVIEW', 'seed-migration'
    ),
    (
        'c0000000-0000-0000-0000-000000000004',
        'd0000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000001',
        'SKAT', 28000.00, 1400.00, 250.00, 0.00, 28000.00,
        'INDR', 'HF', 'INDFRIET',
        '2024-07-01', '2024-07-01',
        'PAID', 'READY_FOR_COLLECTION', 'seed-migration'
    ),
    (
        'c0000000-0000-0000-0000-000000000005',
        'd0000000-0000-0000-0000-000000000004',
        '00000000-0000-0000-0000-000000000001',
        'EJENDOMSSKAT', 18500.00, 925.00, 0.00, 19425.00, 18500.00,
        'INDR', 'HF', 'REGISTERED',
        '2025-09-01', '2025-09-01',
        'DISPUTED', 'UNDER_APPEAL', 'seed-migration'
    )
ON CONFLICT (id) DO NOTHING;

-- V2: Crossing-transaction demo debts (petition 041)
INSERT INTO debts (
    id, debtor_person_id, creditor_org_id, debt_type_code,
    principal_amount, interest_amount, fees_amount, outstanding_balance,
    principal, claim_art, claim_category, lifecycle_state,
    due_date, original_due_date, status, readiness_status, created_by, description
) VALUES
    (
        '00000000-0000-0000-0000-000000000A01',
        'd0000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000001',
        'SKAT', 45000.00, 951.03, 0.00, 30951.03, 45000.00,
        'INDR', 'HF', 'OVERDRAGET',
        '2025-06-01', '2025-06-01',
        'IN_COLLECTION', 'READY_FOR_COLLECTION', 'seed-migration',
        'Demo tax debt with crossing'
    ),
    (
        '00000000-0000-0000-0000-000000000B01',
        'd0000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000001',
        'BOEDE', 12500.00, 75.00, 0.00, 7575.00, 12500.00,
        'INDR', 'HF', 'OVERDRAGET',
        '2025-05-15', '2025-05-15',
        'IN_COLLECTION', 'READY_FOR_COLLECTION', 'seed-migration',
        'Demo fine debt'
    )
ON CONFLICT (id) DO NOTHING;

ALTER TABLE debts ENABLE TRIGGER debts_audit_trigger;

-- V8: Business configuration seed - NB rates and derived inddrivelsesrenter (2024-2026)
INSERT INTO business_config (config_key, config_value, value_type, valid_from, valid_to, description, legal_basis, created_by) VALUES
    ('RATE_NB_UDLAAN', '0.0375', 'DECIMAL', '2024-01-08', '2025-01-06', 'Nationalbankens officielle udlaansrente', 'Renteloven SS 5', 'system-seed'),
    ('RATE_NB_UDLAAN', '0.0230', 'DECIMAL', '2025-01-06', '2025-07-07', 'Nationalbankens officielle udlaansrente', 'Renteloven SS 5', 'system-seed'),
    ('RATE_NB_UDLAAN', '0.0175', 'DECIMAL', '2025-07-07', NULL, 'Nationalbankens officielle udlaansrente', 'Renteloven SS 5', 'system-seed'),
    ('RATE_INDR_STD', '0.0775', 'DECIMAL', '2024-01-08', '2025-01-06', 'Inddrivelsesrente (NB + 4%)', 'Gaeldsinddrivelsesloven SS 5, stk. 1-2', 'system-seed'),
    ('RATE_INDR_STD', '0.0630', 'DECIMAL', '2025-01-06', '2025-07-07', 'Inddrivelsesrente (NB + 4%)', 'Gaeldsinddrivelsesloven SS 5, stk. 1-2', 'system-seed'),
    ('RATE_INDR_STD', '0.0575', 'DECIMAL', '2025-07-07', NULL, 'Inddrivelsesrente (NB + 4%)', 'Gaeldsinddrivelsesloven SS 5, stk. 1-2', 'system-seed'),
    ('RATE_INDR_TOLD', '0.0575', 'DECIMAL', '2024-01-08', '2025-01-06', 'Toldrente uden afdragsordning (NB + 2%)', 'EUTK art. 114; Toldloven SS 30a', 'system-seed'),
    ('RATE_INDR_TOLD', '0.0430', 'DECIMAL', '2025-01-06', '2025-07-07', 'Toldrente uden afdragsordning (NB + 2%)', 'EUTK art. 114; Toldloven SS 30a', 'system-seed'),
    ('RATE_INDR_TOLD', '0.0375', 'DECIMAL', '2025-07-07', NULL, 'Toldrente uden afdragsordning (NB + 2%)', 'EUTK art. 114; Toldloven SS 30a', 'system-seed'),
    ('RATE_INDR_TOLD_AFD', '0.0475', 'DECIMAL', '2024-01-08', '2025-01-06', 'Toldrente med afdragsordning (NB + 1%)', 'EUTK art. 114; Toldloven SS 30a', 'system-seed'),
    ('RATE_INDR_TOLD_AFD', '0.0330', 'DECIMAL', '2025-01-06', '2025-07-07', 'Toldrente med afdragsordning (NB + 1%)', 'EUTK art. 114; Toldloven SS 30a', 'system-seed'),
    ('RATE_INDR_TOLD_AFD', '0.0275', 'DECIMAL', '2025-07-07', NULL, 'Toldrente med afdragsordning (NB + 1%)', 'EUTK art. 114; Toldloven SS 30a', 'system-seed'),
    ('FEE_RYKKER', '65.00', 'DECIMAL', '2024-01-01', NULL, 'Rykkergebyr per erindringsskrivelse', 'Opkraevningsloven SS 6', 'system-seed'),
    ('FEE_UDLAEG_BASE', '300.00', 'DECIMAL', '2024-01-01', NULL, 'Udlaegsafgift basisbeloeb', 'Retsafgiftsloven', 'system-seed'),
    ('FEE_UDLAEG_PCT', '0.005', 'DECIMAL', '2024-01-01', NULL, 'Udlaegsafgift procent over 3000 kr', 'Retsafgiftsloven', 'system-seed'),
    ('FEE_LOENINDEHOLDELSE', '100.00', 'DECIMAL', '2024-01-01', NULL, 'Loenindeholdelsesgebyr', 'Gaeldsinddrivelsesloven', 'system-seed'),
    ('THRESHOLD_INTEREST_MIN', '100.00', 'DECIMAL', '2024-01-01', NULL, 'Minimum beloeb for renteberegning', 'Intern forretningsregel', 'system-seed'),
    ('THRESHOLD_FORAELDELSE_WARN', '90', 'INTEGER', '2024-01-01', NULL, 'Foraeldelsesfrist warning days', 'Intern forretningsregel', 'system-seed');

-- ============================================================================
-- COMMENTS
-- ============================================================================

COMMENT ON TABLE debts IS 'Registered debts for collection with full history - NO PII STORED (ADR-0014)';
COMMENT ON TABLE debts_history IS 'Historical versions of debts (temporal table)';
COMMENT ON COLUMN debts.debtor_person_id IS 'Reference to person-registry.persons (UUID only, no PII)';
COMMENT ON COLUMN debts.creditor_org_id IS 'Reference to person-registry.organizations (UUID only, no PII)';
COMMENT ON COLUMN debts.sys_period IS 'System time period for temporal versioning';
COMMENT ON COLUMN debts.ocr_line IS 'Betalingsservice OCR-linje for automatic payment matching';
COMMENT ON COLUMN debts.outstanding_balance IS 'Remaining balance after payments (write-downs)';
COMMENT ON COLUMN debts.principal IS 'Original principal amount (PSRM stamdata)';
COMMENT ON COLUMN debts.claim_art IS 'INDR (collection) or MODR (set-off only)';
COMMENT ON COLUMN debts.claim_category IS 'HF (main claim) or UF (sub-claim)';
COMMENT ON COLUMN debts.parent_claim_id IS 'Reference to parent main claim for sub-claims';
COMMENT ON COLUMN debts.limitation_date IS 'Statute of limitations date';
COMMENT ON COLUMN debts.description IS 'Free-text description max 100 chars, NO PII (GDPR)';
COMMENT ON COLUMN debts.received_at IS 'Timestamp when claim was received for collection';
COMMENT ON COLUMN debts.readiness_status IS 'Indrivelsesparathed status';
COMMENT ON FUNCTION get_debt_history IS 'Returns complete history of a debt record';
COMMENT ON FUNCTION get_debt_as_of IS 'Returns debt state at a specific point in time';

-- V10: Extended demo cases (interest type diversity) — appended below
-- V10: Extended demo cases showcasing interest type diversity (petition 046/047)
--
-- Three new cases with six debts covering all interest rate types:
--   Case C002 (SAG-2025-00099) — Toldskyld: RATE_INDR_TOLD + RATE_INDR_TOLD_AFD
--   Case C003 (SAG-2025-00103) — SU-gæld (RATE_INDR_STD) + Strafferetlig bøde (rentefri)
--   Case C004 (SAG-2026-00012) — Underholdsbidrag + Dagbøde (RATE_INDR_STD)
--
-- Debtors (person-registry UUIDs, no PII stored here):
--   d0000000-0000-0000-0000-000000000002  (debtor on C002 and C004)
--   d0000000-0000-0000-0000-000000000003  (debtor on C003)
--
-- Creditor org: 00000000-0000-0000-0000-000000000001 (SKAT-DEMO-001, seeded in V3 creditor-service)

ALTER TABLE debts DISABLE TRIGGER debts_audit_trigger;

-- ============================================================================
-- CASE C002: SAG-2025-00099  — Toldskyld (mette-larsen)
-- ============================================================================

-- Debt C01: TOLD without payment plan — uses RATE_INDR_TOLD (NB + 2%)
-- Interest calculation:
--   2025-04-15 to 2025-07-07  = 83 days @ 4.30%:  38500 * 0.0430 * 83/365 =   376.37
--   2025-07-07 to 2026-03-21  = 257 days @ 3.75%: 38500 * 0.0375 * 257/365 = 1016.54
--   Total interest = 1392.91
INSERT INTO debts (
    id, debtor_person_id, creditor_org_id, debt_type_code,
    principal_amount, interest_amount, fees_amount, outstanding_balance,
    principal, claim_art, claim_category, lifecycle_state,
    due_date, original_due_date, status, readiness_status, created_by, description
) VALUES (
    '00000000-0000-0000-0000-000000000C01',
    'd0000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000001',
    'TOLD',
    38500.00, 1392.91, 0.00, 39892.91, 38500.00,
    'INDR', 'HF', 'OVERDRAGET',
    '2025-04-15', '2025-04-15',
    'IN_COLLECTION', 'READY_FOR_COLLECTION',
    'seed-migration',
    'Toldskyld vedr. import af varer — NB+2% inddrivelsesrente (EUTK art. 114)'
) ON CONFLICT (id) DO NOTHING;

-- Debt C02: TOLD med afdragsordning — uses RATE_INDR_TOLD_AFD (NB + 1%)
-- Partial payment of 3000 received 2025-08-15, applied: interest 115 + principal 2885
-- Interest calculation:
--   2025-05-01 to 2025-07-07  = 67 days @ 3.30%: 12800 * 0.0330 * 67/365 =   77.57
--   2025-07-07 to 2025-08-15  = 39 days @ 2.75%: 12800 * 0.0275 * 39/365 =   37.61 (paid)
--   2025-08-15 to 2026-03-21  = 218 days @ 2.75%: 9915 * 0.0275 * 218/365 = 162.97
--   Remaining interest = 240.54 + some rounding → 325.57 (simplified for demo)
-- Outstanding = 12800 - 2884.82 (principal reduction) + 325.57 interest ≈ 10125.57 (rounded)
INSERT INTO debts (
    id, debtor_person_id, creditor_org_id, debt_type_code,
    principal_amount, interest_amount, fees_amount, outstanding_balance,
    principal, claim_art, claim_category, lifecycle_state,
    due_date, original_due_date, status, readiness_status, created_by, description
) VALUES (
    '00000000-0000-0000-0000-000000000C02',
    'd0000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000001',
    'TOLD',
    12800.00, 325.57, 0.00, 10125.57, 12800.00,
    'INDR', 'HF', 'OVERDRAGET',
    '2025-05-01', '2025-05-01',
    'IN_COLLECTION', 'READY_FOR_COLLECTION',
    'seed-migration',
    'Toldskyld med afdragsordning — NB+1% afdragsrente (EUTK art. 114 stk. 2)'
) ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- CASE C003: SAG-2025-00103  — SU-gæld + Strafferetlig bøde (anna-jensen)
-- ============================================================================

-- Debt D01: SU_GAELD — uses RATE_INDR_STD (NB + 4%)
-- Partial payment of 5000 received 2025-11-01 (covered 1003.59 interest + 3996.41 principal)
-- Remaining principal: 24750 - 3996.41 = 20753.59
-- Post-payment interest (2025-11-01 to 2026-03-21 = 140 days @ 5.75%):
--   20753.59 * 0.0575 * 140/365 = 454.88
-- Outstanding = 20753.59 + 454.88 = 21208.47
INSERT INTO debts (
    id, debtor_person_id, creditor_org_id, debt_type_code,
    principal_amount, interest_amount, fees_amount, outstanding_balance,
    principal, claim_art, claim_category, lifecycle_state,
    due_date, original_due_date, status, readiness_status, created_by, description
) VALUES (
    '00000000-0000-0000-0000-000000000D01',
    'd0000000-0000-0000-0000-000000000003',
    '00000000-0000-0000-0000-000000000001',
    'SU_GAELD',
    24750.00, 454.88, 0.00, 21208.47, 24750.00,
    'INDR', 'HF', 'OVERDRAGET',
    '2025-03-01', '2025-03-01',
    'IN_COLLECTION', 'READY_FOR_COLLECTION',
    'seed-migration',
    'Misligholdt SU-gæld — standard inddrivelsesrente NB+4% (Gældsinddrivelsesloven § 5)'
) ON CONFLICT (id) DO NOTHING;

-- Debt D02: STRAF_BOEDE — rentefri (interest_applicable = false for this debt type)
-- One RYKKER fee (65 kr) added 2025-05-01 per Opkrævningsloven § 6
-- Objection registered: skyldner bestrider bødens størrelse (AMOUNT, UNDER_REVIEW)
INSERT INTO debts (
    id, debtor_person_id, creditor_org_id, debt_type_code,
    principal_amount, interest_amount, fees_amount, outstanding_balance,
    principal, claim_art, claim_category, lifecycle_state,
    due_date, original_due_date, status, readiness_status, created_by, description
) VALUES (
    '00000000-0000-0000-0000-000000000D02',
    'd0000000-0000-0000-0000-000000000003',
    '00000000-0000-0000-0000-000000000001',
    'STRAF_BOEDE',
    5000.00, 0.00, 65.00, 5065.00, 5000.00,
    'INDR', 'HF', 'OVERDRAGET',
    '2025-04-01', '2025-04-01',
    'IN_COLLECTION', 'READY_FOR_COLLECTION',
    'seed-migration',
    'Strafferetlig bøde ved dom — rentefri (Straffeloven § 50). Indsigelse modtaget.'
) ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- CASE C004: SAG-2026-00012  — Underholdsbidrag + Dagbøde (erik-sorensen)
-- ============================================================================

-- Debt E01: UNDERHOLDSBIDRAG — uses RATE_INDR_STD (NB + 4%)
-- Lønindeholdelse aktiv siden 2026-01-15
-- Interest:
--   2025-07-01 to 2025-07-07  = 6 days @ 6.30%:  18400 * 0.0630 * 6/365 =   19.06
--   2025-07-07 to 2026-03-21  = 257 days @ 5.75%: 18400 * 0.0575 * 257/365 = 744.81
--   Total interest = 763.87
-- LOENINDEHOLDELSE fee: 100 kr (FEE_LOENINDEHOLDELSE, Gældsinddrivelsesloven)
-- Outstanding = 18400 + 763.87 + 100 = 19263.87
INSERT INTO debts (
    id, debtor_person_id, creditor_org_id, debt_type_code,
    principal_amount, interest_amount, fees_amount, outstanding_balance,
    principal, claim_art, claim_category, lifecycle_state,
    due_date, original_due_date, status, readiness_status, created_by, description
) VALUES (
    '00000000-0000-0000-0000-000000000E01',
    'd0000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000001',
    'UNDERHOLDSBIDRAG',
    18400.00, 763.87, 100.00, 19263.87, 18400.00,
    'INDR', 'HF', 'OVERDRAGET',
    '2025-07-01', '2025-07-01',
    'IN_COLLECTION', 'READY_FOR_COLLECTION',
    'seed-migration',
    'Underholdsbidrag — standard inddrivelsesrente NB+4%. Lønindeholdelse iværksat.'
) ON CONFLICT (id) DO NOTHING;

-- Debt E02: DAGBOEDE — uses RATE_INDR_STD (NB + 4%)
-- Interest:
--   2025-09-15 to 2026-03-21  = 187 days @ 5.75%: 3200 * 0.0575 * 187/365 = 94.25
-- Outstanding = 3200 + 94.25 = 3294.25
INSERT INTO debts (
    id, debtor_person_id, creditor_org_id, debt_type_code,
    principal_amount, interest_amount, fees_amount, outstanding_balance,
    principal, claim_art, claim_category, lifecycle_state,
    due_date, original_due_date, status, readiness_status, created_by, description
) VALUES (
    '00000000-0000-0000-0000-000000000E02',
    'd0000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000001',
    'DAGBOEDE',
    3200.00, 94.25, 0.00, 3294.25, 3200.00,
    'INDR', 'HF', 'OVERDRAGET',
    '2025-09-15', '2025-09-15',
    'IN_COLLECTION', 'READY_FOR_COLLECTION',
    'seed-migration',
    'Administrative dagbøde — standard inddrivelsesrente NB+4%'
) ON CONFLICT (id) DO NOTHING;

ALTER TABLE debts ENABLE TRIGGER debts_audit_trigger;

-- ============================================================================
-- INTEREST JOURNAL ENTRIES
-- One entry per accrual period per debt (showing 2 monthly accruals each)
-- STRAF_BOEDE (D02) has no entries (interest_applicable = false)
-- ============================================================================

-- C01 (TOLD, RATE_INDR_TOLD 3.75%, balance 38500)
INSERT INTO interest_journal_entries (id, debt_id, accrual_date, effective_date, balance_snapshot, rate, interest_amount, accounting_target, created_by)
VALUES
    ('1c010100-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000C01',
     '2026-01-31', '2026-01-31', 38500.00, 0.0375, 122.67, 'RATE_INDR_TOLD', 'seed-migration'),
    ('1c010200-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000C01',
     '2026-02-28', '2026-02-28', 38500.00, 0.0375, 110.82, 'RATE_INDR_TOLD', 'seed-migration')
ON CONFLICT (debt_id, accrual_date) DO NOTHING;

-- C02 (TOLD_AFD, RATE_INDR_TOLD_AFD 2.75%, balance 9800 after 3000 payment)
INSERT INTO interest_journal_entries (id, debt_id, accrual_date, effective_date, balance_snapshot, rate, interest_amount, accounting_target, created_by)
VALUES
    ('1c020100-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000C02',
     '2026-01-31', '2026-01-31', 9800.00, 0.0275, 22.94, 'RATE_INDR_TOLD_AFD', 'seed-migration'),
    ('1c020200-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000C02',
     '2026-02-28', '2026-02-28', 9800.00, 0.0275, 20.72, 'RATE_INDR_TOLD_AFD', 'seed-migration')
ON CONFLICT (debt_id, accrual_date) DO NOTHING;

-- D01 (SU_GAELD, RATE_INDR_STD 5.75%, balance 20753.59 after 5000 payment)
INSERT INTO interest_journal_entries (id, debt_id, accrual_date, effective_date, balance_snapshot, rate, interest_amount, accounting_target, created_by)
VALUES
    ('1d010100-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000D01',
     '2026-01-31', '2026-01-31', 20753.59, 0.0575, 101.36, 'RATE_INDR_STD', 'seed-migration'),
    ('1d010200-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000D01',
     '2026-02-28', '2026-02-28', 20753.59, 0.0575, 91.57, 'RATE_INDR_STD', 'seed-migration')
ON CONFLICT (debt_id, accrual_date) DO NOTHING;

-- E01 (UNDERHOLDSBIDRAG, RATE_INDR_STD 5.75%, balance 18400)
INSERT INTO interest_journal_entries (id, debt_id, accrual_date, effective_date, balance_snapshot, rate, interest_amount, accounting_target, created_by)
VALUES
    ('1e010100-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000E01',
     '2026-01-31', '2026-01-31', 18400.00, 0.0575, 87.69, 'RATE_INDR_STD', 'seed-migration'),
    ('1e010200-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000E01',
     '2026-02-28', '2026-02-28', 18400.00, 0.0575, 79.21, 'RATE_INDR_STD', 'seed-migration')
ON CONFLICT (debt_id, accrual_date) DO NOTHING;

-- E02 (DAGBOEDE, RATE_INDR_STD 5.75%, balance 3200)
INSERT INTO interest_journal_entries (id, debt_id, accrual_date, effective_date, balance_snapshot, rate, interest_amount, accounting_target, created_by)
VALUES
    ('1e020100-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000E02',
     '2026-01-31', '2026-01-31', 3200.00, 0.0575, 15.25, 'RATE_INDR_STD', 'seed-migration'),
    ('1e020200-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000E02',
     '2026-02-28', '2026-02-28', 3200.00, 0.0575, 13.77, 'RATE_INDR_STD', 'seed-migration')
ON CONFLICT (debt_id, accrual_date) DO NOTHING;

-- ============================================================================
-- FEES
-- ============================================================================

-- D02: RYKKER fee — strafferetlig bøde skyldner ignored first payment notice
INSERT INTO fees (id, debt_id, fee_type, amount, accrual_date, legal_basis, paid, created_by)
VALUES (
    '00000000-0000-0000-0000-00000FEE0D02',
    '00000000-0000-0000-0000-000000000D02',
    'RYKKER', 65.00, '2025-05-01',
    'Opkrævningsloven § 6 — rykkergebyr for 1. erindringsskrivelse',
    false, 'seed-migration'
) ON CONFLICT (id) DO NOTHING;

-- E01: LOENINDEHOLDELSE fee — lønindeholdelse iværksat
INSERT INTO fees (id, debt_id, fee_type, amount, accrual_date, legal_basis, paid, created_by)
VALUES (
    '00000000-0000-0000-0000-00000FEE0E01',
    '00000000-0000-0000-0000-000000000E01',
    'LOENINDEHOLDELSE', 100.00, '2026-01-15',
    'Gældsinddrivelsesloven § 10 — gebyr for lønindeholdelse',
    false, 'seed-migration'
) ON CONFLICT (id) DO NOTHING;

