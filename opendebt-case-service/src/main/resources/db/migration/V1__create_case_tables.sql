-- ============================================================================
-- OpenDebt Case Service - Consolidated Baseline Schema
-- ============================================================================
-- Database: PostgreSQL 16
-- Consolidates V1-V7 migrations into single baseline
-- Final state is identical to a fully-migrated database.
-- Dev databases must be dropped and recreated after this squash.
-- OIO Sag model (V3), sensitivity classification (V7)
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

COMMENT ON TABLE audit_log IS 'Audit trail for all database modifications';

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
-- V3: OIO Sag columns added; status/debtor_person_id made nullable (deprecated)
-- V7: sensitivity column added for VIP/PEP/CONFIDENTIAL access control
-- ============================================================================

CREATE TABLE cases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_number VARCHAR(20) NOT NULL UNIQUE,

    -- DEPRECATED (V3): moved to case_parties with role PRIMARY_DEBTOR
    debtor_person_id UUID,

    -- DEPRECATED (V3): replaced by case_state
    status VARCHAR(30),

    -- DEPRECATED (V3): computed on demand from debt-service
    total_debt NUMERIC(15, 2) DEFAULT 0,
    total_paid NUMERIC(15, 2) DEFAULT 0,
    total_remaining NUMERIC(15, 2) DEFAULT 0,

    -- DEPRECATED (V3): replaced by collection_measures table
    active_strategy VARCHAR(30),

    -- DEPRECATED (V3): replaced by primary_caseworker_id
    assigned_caseworker_id VARCHAR(100),

    -- DEPRECATED (V3): replaced by case_journal_notes table
    notes TEXT,

    -- DEPRECATED (V3): replaced by case_events table
    last_activity_at TIMESTAMPTZ,

    -- OIO Sag core fields (V3)
    title VARCHAR(200) NOT NULL,
    description TEXT,
    confidential_title VARCHAR(200),
    case_state VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    state_changed_at TIMESTAMPTZ,
    case_type VARCHAR(30) NOT NULL DEFAULT 'DEBT_COLLECTION',
    subject_classification VARCHAR(30),
    action_classification VARCHAR(30),
    precedent_indicator BOOLEAN NOT NULL DEFAULT FALSE,
    retention_override BOOLEAN,

    -- Organisation / ownership (V3)
    owner_organisation_id VARCHAR(100),
    responsible_unit_id VARCHAR(100),
    primary_caseworker_id VARCHAR(100),

    -- Hierarchy / workflow (V3)
    parent_case_id UUID,
    workflow_process_instance_id VARCHAR(100),

    -- Access control sensitivity (V7: ADR-0014, petition048)
    sensitivity VARCHAR(20) NOT NULL DEFAULT 'NORMAL',

    -- Metadata
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,

    -- Temporal support
    sys_period TSTZRANGE NOT NULL DEFAULT tstzrange(CURRENT_TIMESTAMP, NULL),

    -- Constraints (V3 replaces V1 chk_status/chk_strategy)
    CONSTRAINT chk_case_state CHECK (case_state IN (
        'CREATED', 'ASSESSED', 'DECIDED', 'SUSPENDED',
        'CLOSED_PAID', 'CLOSED_WRITTEN_OFF', 'CLOSED_WITHDRAWN', 'CLOSED_CANCELLED'
    )),
    CONSTRAINT chk_case_type CHECK (case_type IN (
        'DEBT_COLLECTION', 'HEARING', 'APPEAL', 'OBJECTION'
    )),
    CONSTRAINT chk_case_sensitivity CHECK (sensitivity IN (
        'NORMAL', 'VIP', 'PEP', 'CONFIDENTIAL'
    ))
);

-- History table for cases
CREATE TABLE cases_history (
    LIKE cases,
    PRIMARY KEY (id, sys_period)
);

-- Legacy debt ID mapping (kept for backward compatibility with V6 seed)
CREATE TABLE case_debt_ids (
    case_id UUID NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    debt_id UUID NOT NULL,
    added_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    added_by VARCHAR(100),
    PRIMARY KEY (case_id, debt_id)
);

-- ============================================================================
-- CASE PARTIES (V3)
-- ============================================================================

CREATE TABLE case_parties (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id UUID NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    person_id UUID NOT NULL,
    party_role VARCHAR(30) NOT NULL CHECK (party_role IN (
        'PRIMARY_DEBTOR', 'CO_DEBTOR', 'CREDITOR',
        'LEGAL_REPRESENTATIVE', 'CONTACT_PERSON', 'GUARANTOR'
    )),
    party_type VARCHAR(20) NOT NULL CHECK (party_type IN ('PERSON', 'ORGANISATION')),
    active_from DATE,
    active_to DATE,
    added_by VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_case_parties_case_id ON case_parties(case_id);
CREATE INDEX idx_case_parties_person_id ON case_parties(person_id);
COMMENT ON TABLE case_parties IS 'Parties (persons/orgs) associated with a case - NO PII STORED';
COMMENT ON COLUMN case_parties.person_id IS 'Reference to person-registry (UUID only)';

-- ============================================================================
-- CASE DEBTS (V3 - replaces case_debt_ids with soft-delete support)
-- ============================================================================

CREATE TABLE case_debts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id UUID NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    debt_id UUID NOT NULL,
    added_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    added_by VARCHAR(100),
    removed_at TIMESTAMPTZ,
    removed_by VARCHAR(100),
    transfer_reference VARCHAR(200),
    notes TEXT
);

CREATE INDEX idx_case_debts_case_id ON case_debts(case_id);
CREATE INDEX idx_case_debts_debt_id ON case_debts(debt_id);
CREATE UNIQUE INDEX uq_case_debts_active ON case_debts(case_id, debt_id) WHERE removed_at IS NULL;
COMMENT ON TABLE case_debts IS 'Debt-to-case links with soft-delete support';

-- ============================================================================
-- CASE JOURNAL ENTRIES (V3)
-- ============================================================================

CREATE TABLE case_journal_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id UUID NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    journal_entry_title VARCHAR(200) NOT NULL,
    journal_entry_time TIMESTAMPTZ NOT NULL,
    document_id UUID,
    document_direction VARCHAR(20) CHECK (document_direction IN ('INCOMING', 'OUTGOING', 'INTERNAL')),
    document_type VARCHAR(100),
    confidential_title VARCHAR(200),
    registered_by VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_journal_entries_case_id ON case_journal_entries(case_id);
COMMENT ON TABLE case_journal_entries IS 'OIO Journalpost entries on a case';

-- ============================================================================
-- CASE JOURNAL NOTES (V3)
-- ============================================================================

CREATE TABLE case_journal_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id UUID NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    note_title VARCHAR(200) NOT NULL,
    note_text TEXT NOT NULL,
    author_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_journal_notes_case_id ON case_journal_notes(case_id);
COMMENT ON TABLE case_journal_notes IS 'Free-text notes on a case (replaces flat notes column)';

-- ============================================================================
-- CASE LEGAL BASES (V3)
-- ============================================================================

CREATE TABLE case_legal_bases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id UUID NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    legal_source_uri VARCHAR(500),
    legal_source_title VARCHAR(300) NOT NULL,
    paragraph_reference VARCHAR(100),
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_legal_bases_case_id ON case_legal_bases(case_id);
COMMENT ON TABLE case_legal_bases IS 'Legal basis references for a case';

-- ============================================================================
-- CASE EVENTS (V3 - immutable audit trail)
-- ============================================================================

CREATE TABLE case_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id UUID NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    event_type VARCHAR(40) NOT NULL CHECK (event_type IN (
        'CASE_CREATED', 'STATE_CHANGED', 'CASEWORKER_ASSIGNED',
        'PARTY_ADDED', 'PARTY_REMOVED', 'DEBT_ADDED', 'DEBT_REMOVED',
        'JOURNAL_ENTRY_ADDED', 'NOTE_ADDED', 'STRATEGY_CHANGED',
        'COLLECTION_MEASURE_INITIATED', 'OBJECTION_RECEIVED',
        'HEARING_STARTED', 'HEARING_RESOLVED',
        'WORKFLOW_TASK_COMPLETED', 'CASE_CLOSED'
    )),
    description TEXT,
    metadata TEXT,
    performed_by VARCHAR(100),
    performed_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_case_events_case_performed ON case_events(case_id, performed_at);
COMMENT ON TABLE case_events IS 'Immutable event log for case lifecycle';

-- ============================================================================
-- CASE RELATIONS (V3)
-- ============================================================================

CREATE TABLE case_relations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_case_id UUID NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    target_case_id UUID NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    relation_type VARCHAR(20) NOT NULL CHECK (relation_type IN (
        'PARENT', 'RELATED', 'PRECEDENT', 'SPLIT_FROM', 'MERGED_INTO'
    )),
    description TEXT,
    created_by VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_case_relations_source ON case_relations(source_case_id);
CREATE INDEX idx_case_relations_target ON case_relations(target_case_id);
COMMENT ON TABLE case_relations IS 'Directed relationships between cases';

-- ============================================================================
-- COLLECTION MEASURES (V3)
-- ============================================================================

CREATE TABLE collection_measures (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id UUID NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    debt_id UUID,
    measure_type VARCHAR(30) NOT NULL CHECK (measure_type IN (
        'VOLUNTARY_PAYMENT', 'PAYMENT_PLAN', 'WAGE_GARNISHMENT', 'OFFSETTING', 'ATTACHMENT'
    )),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PLANNED', 'ACTIVE', 'COMPLETED', 'CANCELLED')),
    start_date DATE,
    end_date DATE,
    amount NUMERIC(15, 2),
    reference VARCHAR(200),
    notes TEXT,
    created_by VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_collection_measures_case_id ON collection_measures(case_id);
CREATE INDEX idx_collection_measures_status ON collection_measures(status);
COMMENT ON TABLE collection_measures IS 'Collection measures (inddrivelsesskridt) applied to a case/debt';

-- ============================================================================
-- OBJECTIONS (V3)
-- ============================================================================

CREATE TABLE objections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id UUID NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    debt_id UUID,
    objection_type VARCHAR(20) NOT NULL CHECK (objection_type IN (
        'EXISTENCE', 'AMOUNT', 'BASIS', 'TREATMENT', 'LIMITATION'
    )),
    status VARCHAR(20) NOT NULL CHECK (status IN (
        'RECEIVED', 'UNDER_REVIEW', 'ACCEPTED', 'REJECTED'
    )),
    description TEXT,
    debtor_statement TEXT,
    caseworker_assessment TEXT,
    received_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ,
    resolved_by VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_objections_case_id ON objections(case_id);
CREATE INDEX idx_objections_status ON objections(status);
COMMENT ON TABLE objections IS 'Debtor objections (indsigelser) against cases/debts';

-- ============================================================================
-- HISTORY TABLES for mutable V3 tables
-- ============================================================================

CREATE TABLE case_journal_notes_history (
    LIKE case_journal_notes,
    sys_period TSTZRANGE NOT NULL DEFAULT tstzrange(CURRENT_TIMESTAMP, NULL)
);

CREATE TABLE collection_measures_history (
    LIKE collection_measures,
    sys_period TSTZRANGE NOT NULL DEFAULT tstzrange(CURRENT_TIMESTAMP, NULL)
);

CREATE TABLE objections_history (
    LIKE objections,
    sys_period TSTZRANGE NOT NULL DEFAULT tstzrange(CURRENT_TIMESTAMP, NULL)
);

-- ============================================================================
-- INDEXES (cases + history)
-- ============================================================================

CREATE INDEX idx_case_debtor_person_id ON cases(debtor_person_id);
CREATE INDEX idx_case_status ON cases(status);
CREATE INDEX idx_case_caseworker ON cases(assigned_caseworker_id);
CREATE INDEX idx_case_created_at ON cases(created_at);
CREATE INDEX idx_case_case_state ON cases(case_state);
CREATE INDEX idx_case_case_type ON cases(case_type);
CREATE INDEX idx_case_primary_caseworker ON cases(primary_caseworker_id);
CREATE INDEX idx_case_parent_case_id ON cases(parent_case_id);
CREATE INDEX idx_cases_sensitivity ON cases(sensitivity);

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

CREATE TRIGGER case_journal_notes_audit_trigger
    AFTER INSERT OR UPDATE OR DELETE ON case_journal_notes
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

CREATE TRIGGER collection_measures_audit_trigger
    AFTER INSERT OR UPDATE OR DELETE ON collection_measures
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

CREATE TRIGGER objections_audit_trigger
    AFTER INSERT OR UPDATE OR DELETE ON objections
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

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
COMMENT ON COLUMN cases.debtor_person_id IS 'DEPRECATED (V3): moved to case_parties with role PRIMARY_DEBTOR';
COMMENT ON COLUMN cases.status IS 'DEPRECATED (V3): replaced by case_state';
COMMENT ON COLUMN cases.total_debt IS 'DEPRECATED (V3): computed on demand from debt-service';
COMMENT ON COLUMN cases.total_paid IS 'DEPRECATED (V3): computed on demand from debt-service';
COMMENT ON COLUMN cases.total_remaining IS 'DEPRECATED (V3): computed on demand from debt-service';
COMMENT ON COLUMN cases.active_strategy IS 'DEPRECATED (V3): replaced by collection_measures table';
COMMENT ON COLUMN cases.assigned_caseworker_id IS 'DEPRECATED (V3): replaced by primary_caseworker_id';
COMMENT ON COLUMN cases.notes IS 'DEPRECATED (V3): replaced by case_journal_notes table';
COMMENT ON COLUMN cases.last_activity_at IS 'DEPRECATED (V3): replaced by case_events table';
COMMENT ON COLUMN cases.case_number IS 'Unique case reference number';
COMMENT ON COLUMN cases.title IS 'OIO Sag: case title';
COMMENT ON COLUMN cases.case_state IS 'OIO Sag: lifecycle state (replaces status)';
COMMENT ON COLUMN cases.case_type IS 'OIO Sag: case type classification';
COMMENT ON COLUMN cases.primary_caseworker_id IS 'OIO Sag: primary responsible caseworker (replaces assigned_caseworker_id)';
COMMENT ON COLUMN cases.sensitivity IS 'Access control sensitivity level: NORMAL, VIP, PEP, CONFIDENTIAL';

-- ============================================================================
-- SEED DATA
-- ============================================================================

ALTER TABLE cases DISABLE TRIGGER cases_versioning_trigger;
ALTER TABLE cases DISABLE TRIGGER cases_audit_trigger;

-- Demo case SAG-2025-00042: merged final state from V2 + V4 + V5
-- (V4 sets title/case_state/case_type/primary_caseworker_id='caseworker-demo',
--  V5 corrects primary_caseworker_id and assigned_caseworker_id to 'anna-jensen')
INSERT INTO cases (
    id, case_number, debtor_person_id,
    title, case_state, case_type,
    primary_caseworker_id, owner_organisation_id,
    status, total_debt, total_paid, total_remaining,
    active_strategy, assigned_caseworker_id,
    notes, created_by
) VALUES (
    '00000000-0000-0000-0000-00000000C001',
    'SAG-2025-00042',
    'd0000000-0000-0000-0000-000000000001',
    'Inddrivelsessag SAG-2025-00042',
    'ASSESSED', 'DEBT_COLLECTION',
    'anna-jensen', 'UFST',
    'IN_PROGRESS', 57500.00, 15000.00, 42500.00,
    'VOLUNTARY_PAYMENT', 'anna-jensen',
    'Demo case with crossing-transaction for tax and fine debts',
    'seed-migration'
) ON CONFLICT (id) DO NOTHING;

ALTER TABLE cases ENABLE TRIGGER cases_versioning_trigger;
ALTER TABLE cases ENABLE TRIGGER cases_audit_trigger;

-- case_debt_ids for C001 (V2 - kept for backward compat)
INSERT INTO case_debt_ids (case_id, debt_id, added_by)
VALUES
    ('00000000-0000-0000-0000-00000000C001', '00000000-0000-0000-0000-000000000A01', 'seed-migration'),
    ('00000000-0000-0000-0000-00000000C001', '00000000-0000-0000-0000-000000000B01', 'seed-migration')
ON CONFLICT (case_id, debt_id) DO NOTHING;

-- case_parties for C001 (V4)
INSERT INTO case_parties (id, case_id, person_id, party_role, party_type, active_from, added_by, created_at)
VALUES (
    '00000000-0000-0000-0000-00000000C501',
    '00000000-0000-0000-0000-00000000C001',
    'd0000000-0000-0000-0000-000000000001',
    'PRIMARY_DEBTOR', 'PERSON',
    '2025-01-01', 'v4-demo-seed', CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;

-- case_debts for C001 (V4)
INSERT INTO case_debts (id, case_id, debt_id, added_by)
VALUES
    ('00000000-0000-0000-0000-0000000CD001', '00000000-0000-0000-0000-00000000C001', '00000000-0000-0000-0000-000000000A01', 'v4-demo-seed'),
    ('00000000-0000-0000-0000-0000000CD002', '00000000-0000-0000-0000-00000000C001', '00000000-0000-0000-0000-000000000B01', 'v4-demo-seed')
ON CONFLICT DO NOTHING;

-- case_events for C001 (V4)
INSERT INTO case_events (id, case_id, event_type, description, performed_by, performed_at)
VALUES
    (gen_random_uuid(), '00000000-0000-0000-0000-00000000C001',
     'CASE_CREATED', 'Demo case created by seed migration', 'seed-migration', '2025-01-15 10:00:00+00'),
    (gen_random_uuid(), '00000000-0000-0000-0000-00000000C001',
     'STATE_CHANGED', 'State changed from CREATED to ASSESSED', 'caseworker-demo', '2025-01-16 09:30:00+00'),
    (gen_random_uuid(), '00000000-0000-0000-0000-00000000C001',
     'DEBT_ADDED', 'Tax debt added to case', 'caseworker-demo', '2025-01-15 10:05:00+00'),
    (gen_random_uuid(), '00000000-0000-0000-0000-00000000C001',
     'DEBT_ADDED', 'Fine debt added to case', 'caseworker-demo', '2025-01-15 10:06:00+00'),
    (gen_random_uuid(), '00000000-0000-0000-0000-00000000C001',
     'CASEWORKER_ASSIGNED', 'Assigned to caseworker-demo', 'system', '2025-01-15 10:00:00+00')
ON CONFLICT DO NOTHING;

-- V6: Extended demo cases (C002, C003, C004) — appended below
-- V6: Extended demo cases (3 new cases with diverse interest types)
--
-- Case C002: SAG-2025-00099 — Toldskyld (mette-larsen, ASSESSED)
--   - Debt C01: TOLD (RATE_INDR_TOLD, no payment)
--   - Debt C02: TOLD med afdragsordning (RATE_INDR_TOLD_AFD, 3000 kr betalt)
--   - Collection measure: PAYMENT_PLAN (active, debt C02)
--
-- Case C003: SAG-2025-00103 — SU-gæld + Straf (anna-jensen, ASSESSED)
--   - Debt D01: SU_GAELD (RATE_INDR_STD, 5000 kr betalt)
--   - Debt D02: STRAF_BOEDE (rentefri, rykkergebyr, indsigelse UNDER_REVIEW)
--
-- Case C004: SAG-2026-00012 — Underholdsbidrag + Dagbøde (erik-sorensen, DECIDED)
--   - Debt E01: UNDERHOLDSBIDRAG (RATE_INDR_STD, lønindeholdelse aktiv)
--   - Debt E02: DAGBOEDE (RATE_INDR_STD)

ALTER TABLE cases DISABLE TRIGGER cases_versioning_trigger;
ALTER TABLE cases DISABLE TRIGGER cases_audit_trigger;
ALTER TABLE collection_measures DISABLE TRIGGER collection_measures_audit_trigger;
ALTER TABLE objections DISABLE TRIGGER objections_audit_trigger;
ALTER TABLE case_journal_notes DISABLE TRIGGER case_journal_notes_audit_trigger;

-- ============================================================================
-- CASES
-- ============================================================================

INSERT INTO cases (
    id, case_number,
    title, description,
    case_state, case_type,
    primary_caseworker_id,
    debtor_person_id,
    status,
    total_debt, total_paid, total_remaining,
    created_by
) VALUES
    (
        '00000000-0000-0000-0000-00000000C002',
        'SAG-2025-00099',
        'Inddrivelse af toldskyld — SAG-2025-00099',
        'Toldskyld vedr. import fra tredjeländer. To fordringer: én uden afdragsordning og én med aktiv betalingsplan.',
        'ASSESSED', 'DEBT_COLLECTION',
        'mette-larsen',
        'd0000000-0000-0000-0000-000000000002',
        'IN_PROGRESS',
        51300.00, 3000.00, 48018.48,
        'seed-migration'
    ),
    (
        '00000000-0000-0000-0000-00000000C003',
        'SAG-2025-00103',
        'Inddrivelse af SU-gæld og strafferetlig bøde — SAG-2025-00103',
        'Misligholdt SU-gæld med delvis betaling modtaget. Strafferetlig bøde under indsigelse fra skyldner.',
        'ASSESSED', 'DEBT_COLLECTION',
        'anna-jensen',
        'd0000000-0000-0000-0000-000000000003',
        'IN_PROGRESS',
        29750.00, 5000.00, 26273.35,
        'seed-migration'
    ),
    (
        '00000000-0000-0000-0000-00000000C004',
        'SAG-2026-00012',
        'Inddrivelse af underholdsbidrag og dagbøde — SAG-2026-00012',
        'Underholdsbidrag under lønindeholdelse. Dagbøde fra administrativ afgørelse indgår i samme sag.',
        'DECIDED', 'DEBT_COLLECTION',
        'erik-sorensen',
        'd0000000-0000-0000-0000-000000000002',
        'IN_PROGRESS',
        21600.00, 0.00, 22558.12,
        'seed-migration'
    )
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- CASE DEBTS (new V3 table)
-- ============================================================================

INSERT INTO case_debts (id, case_id, debt_id, added_by, notes) VALUES
    -- C002: two TOLD debts
    (gen_random_uuid(), '00000000-0000-0000-0000-00000000C002', '00000000-0000-0000-0000-000000000C01', 'seed-migration', 'TOLD uden afdragsordning — NB+2% rente'),
    (gen_random_uuid(), '00000000-0000-0000-0000-00000000C002', '00000000-0000-0000-0000-000000000C02', 'seed-migration', 'TOLD med afdragsordning — NB+1% rente, betalingsplan aktiv'),
    -- C003: SU + straf
    (gen_random_uuid(), '00000000-0000-0000-0000-00000000C003', '00000000-0000-0000-0000-000000000D01', 'seed-migration', 'SU-gæld — delvis betalt 2025-11-01'),
    (gen_random_uuid(), '00000000-0000-0000-0000-00000000C003', '00000000-0000-0000-0000-000000000D02', 'seed-migration', 'Strafferetlig bøde — indsigelse modtaget'),
    -- C004: underholdsbidrag + dagbøde
    (gen_random_uuid(), '00000000-0000-0000-0000-00000000C004', '00000000-0000-0000-0000-000000000E01', 'seed-migration', 'Underholdsbidrag — lønindeholdelse iværksat 2026-01-15'),
    (gen_random_uuid(), '00000000-0000-0000-0000-00000000C004', '00000000-0000-0000-0000-000000000E02', 'seed-migration', 'Dagbøde — standard inddrivelse')
ON CONFLICT DO NOTHING;

-- Also seed old case_debt_ids for backward compatibility (V2 table still exists)
INSERT INTO case_debt_ids (case_id, debt_id, added_by) VALUES
    ('00000000-0000-0000-0000-00000000C002', '00000000-0000-0000-0000-000000000C01', 'seed-migration'),
    ('00000000-0000-0000-0000-00000000C002', '00000000-0000-0000-0000-000000000C02', 'seed-migration'),
    ('00000000-0000-0000-0000-00000000C003', '00000000-0000-0000-0000-000000000D01', 'seed-migration'),
    ('00000000-0000-0000-0000-00000000C003', '00000000-0000-0000-0000-000000000D02', 'seed-migration'),
    ('00000000-0000-0000-0000-00000000C004', '00000000-0000-0000-0000-000000000E01', 'seed-migration'),
    ('00000000-0000-0000-0000-00000000C004', '00000000-0000-0000-0000-000000000E02', 'seed-migration')
ON CONFLICT (case_id, debt_id) DO NOTHING;

-- ============================================================================
-- CASE PARTIES (debtors — person-registry UUIDs, no PII)
-- ============================================================================

INSERT INTO case_parties (id, case_id, person_id, party_role, party_type, active_from, added_by) VALUES
    (gen_random_uuid(), '00000000-0000-0000-0000-00000000C002', 'd0000000-0000-0000-0000-000000000002', 'PRIMARY_DEBTOR', 'PERSON', '2025-08-20', 'seed-migration'),
    (gen_random_uuid(), '00000000-0000-0000-0000-00000000C003', 'd0000000-0000-0000-0000-000000000003', 'PRIMARY_DEBTOR', 'PERSON', '2025-08-25', 'seed-migration'),
    (gen_random_uuid(), '00000000-0000-0000-0000-00000000C004', 'd0000000-0000-0000-0000-000000000002', 'PRIMARY_DEBTOR', 'PERSON', '2026-01-10', 'seed-migration');

-- ============================================================================
-- CASE JOURNAL NOTES
-- ============================================================================

INSERT INTO case_journal_notes (id, case_id, note_title, note_text, author_id) VALUES
    -- C002 notes
    (gen_random_uuid(), '00000000-0000-0000-0000-00000000C002',
     'Afdragsordning bekræftet',
     'Skyldner har accepteret afdragsordning for toldskyld C02. Aftale: 3 månedlige afdrag á 3000 kr. Første afdrag modtaget 2025-08-15.',
     'mette-larsen'),
    (gen_random_uuid(), '00000000-0000-0000-0000-00000000C002',
     'Told C01 — afventer frivillig betaling',
     'Skyldner er orienteret om den lave EUTK-rente (NB+2%) på told C01. Frivillig betaling forventes inden 2026-05-01, ellers iværksættes lønindeholdelse.',
     'mette-larsen'),
    -- C003 notes
    (gen_random_uuid(), '00000000-0000-0000-0000-00000000C003',
     'Delvis betaling modtaget — SU-gæld',
     '5000 kr modtaget 2025-11-01. Dækning: 1003,59 kr rente + 3996,41 kr af hovedstol. Resterende saldo 21.208,47 kr til videre inddrivelse.',
     'anna-jensen'),
    (gen_random_uuid(), '00000000-0000-0000-0000-00000000C003',
     'Indsigelse modtaget — strafferetlig bøde',
     'Skyldner bestrider bødens størrelse (5000 kr). Indsigelse sendt til vurdering hos Ankestyrelsen. Inddrivelse suspenderet for D02 indtil afklaring.',
     'anna-jensen'),
    -- C004 notes
    (gen_random_uuid(), '00000000-0000-0000-0000-00000000C004',
     'Lønindeholdelse iværksat',
     'Lønindeholdelse godkendt og iværksat 2026-01-15. Arbejdsgiver orienteret. Månedligt indeholdelsesbeløb: 2500 kr. Gebyr 100 kr bogført på E01.',
     'erik-sorensen'),
    (gen_random_uuid(), '00000000-0000-0000-0000-00000000C004',
     'Dagbøde E02 — sag forenet',
     'Dagbøde (3200 kr + 94,25 kr rente) forenet med underholdsbidragssagen for samlet inddrivelse via lønindeholdelse.',
     'erik-sorensen');

-- ============================================================================
-- CASE JOURNAL ENTRIES (OIO Journalpost)
-- ============================================================================

INSERT INTO case_journal_entries (id, case_id, journal_entry_title, journal_entry_time, document_direction, document_type, registered_by) VALUES
    -- C002
    (gen_random_uuid(), '00000000-0000-0000-0000-00000000C002', 'Overdraget fra SKAT — toldskyld', '2025-08-20 09:00:00+02', 'INCOMING', 'OVERDRAGELSE', 'seed-migration'),
    (gen_random_uuid(), '00000000-0000-0000-0000-00000000C002', 'Bekræftelse på afdragsordning sendt', '2025-08-22 10:30:00+02', 'OUTGOING', 'BREV', 'mette-larsen'),
    (gen_random_uuid(), '00000000-0000-0000-0000-00000000C002', 'Betaling modtaget — 3000 kr', '2025-08-15 14:00:00+02', 'INCOMING', 'BETALING', 'seed-migration'),
    -- C003
    (gen_random_uuid(), '00000000-0000-0000-0000-00000000C003', 'Overdraget fra Uddannelsesstyrelsen — SU-gæld', '2025-08-25 08:30:00+02', 'INCOMING', 'OVERDRAGELSE', 'seed-migration'),
    (gen_random_uuid(), '00000000-0000-0000-0000-00000000C003', 'Indsigelse modtaget fra skyldner', '2025-04-28 11:15:00+02', 'INCOMING', 'INDSIGELSE', 'anna-jensen'),
    (gen_random_uuid(), '00000000-0000-0000-0000-00000000C003', 'Betaling modtaget — 5000 kr', '2025-11-01 09:45:00+01', 'INCOMING', 'BETALING', 'seed-migration'),
    -- C004
    (gen_random_uuid(), '00000000-0000-0000-0000-00000000C004', 'Overdraget fra Familieretshuset', '2026-01-10 10:00:00+01', 'INCOMING', 'OVERDRAGELSE', 'seed-migration'),
    (gen_random_uuid(), '00000000-0000-0000-0000-00000000C004', 'Afgørelse om lønindeholdelse', '2026-01-14 14:30:00+01', 'OUTGOING', 'AFGOERELSE', 'erik-sorensen'),
    (gen_random_uuid(), '00000000-0000-0000-0000-00000000C004', 'Orientering til arbejdsgiver', '2026-01-15 09:00:00+01', 'OUTGOING', 'BREV', 'erik-sorensen');

-- ============================================================================
-- COLLECTION MEASURES
-- ============================================================================

-- C002: PAYMENT_PLAN for C02 (TOLD med afdragsordning)
INSERT INTO collection_measures (id, case_id, debt_id, measure_type, status, start_date, amount, reference, notes, created_by)
VALUES (
    gen_random_uuid(),
    '00000000-0000-0000-0000-00000000C002',
    '00000000-0000-0000-0000-000000000C02',
    'PAYMENT_PLAN', 'ACTIVE',
    '2025-08-22', 3000.00,
    'PLAN-C02-2025-001',
    '3 afdrag á 3000 kr, startende 2025-08-15. Skyldner har betalt første afdrag.',
    'mette-larsen'
);

-- C004: WAGE_GARNISHMENT for E01 (underholdsbidrag)
INSERT INTO collection_measures (id, case_id, debt_id, measure_type, status, start_date, amount, reference, notes, created_by)
VALUES (
    gen_random_uuid(),
    '00000000-0000-0000-0000-00000000C004',
    '00000000-0000-0000-0000-000000000E01',
    'WAGE_GARNISHMENT', 'ACTIVE',
    '2026-01-15', 2500.00,
    'LOENI-E01-2026-001',
    'Månedlig lønindeholdelse 2500 kr. Arbejdsgiver: Aarhus Kommune (CVR 55133018).',
    'erik-sorensen'
);

-- ============================================================================
-- OBJECTIONS
-- ============================================================================

-- C003: Indsigelse mod D02 (strafferetlig bøde, AMOUNT, UNDER_REVIEW)
INSERT INTO objections (id, case_id, debt_id, objection_type, status, description, debtor_statement, caseworker_assessment, received_at)
VALUES (
    gen_random_uuid(),
    '00000000-0000-0000-0000-00000000C003',
    '00000000-0000-0000-0000-000000000D02',
    'AMOUNT', 'UNDER_REVIEW',
    'Skyldner bestrider bødens størrelse på 5000 kr',
    'Skyldner anfører at bøden er udmålt uforholdsmæssigt højt i forhold til overtrædelsens grovhed og skyldners økonomi.',
    'Indsigelsen er sendt til vurdering hos Ankestyrelsen. Inddrivelse af D02 afventer afgørelse. SU-gæld D01 fortsætter uberørt.',
    '2025-04-28 11:15:00+02'
);

-- ============================================================================
-- CASE EVENTS (immutable audit trail)
-- ============================================================================

INSERT INTO case_events (id, case_id, event_type, description, performed_by, performed_at) VALUES
    -- C002 events
    (gen_random_uuid(), '00000000-0000-0000-0000-00000000C002', 'CASE_CREATED',         'Sag oprettet ved overdragelse fra SKAT', 'seed-migration', '2025-08-20 09:00:00+02'),
    (gen_random_uuid(), '00000000-0000-0000-0000-00000000C002', 'DEBT_ADDED',            'Fordring C01 (TOLD) tilknyttet', 'seed-migration', '2025-08-20 09:00:00+02'),
    (gen_random_uuid(), '00000000-0000-0000-0000-00000000C002', 'DEBT_ADDED',            'Fordring C02 (TOLD afdrag) tilknyttet', 'seed-migration', '2025-08-20 09:00:00+02'),
    (gen_random_uuid(), '00000000-0000-0000-0000-00000000C002', 'CASEWORKER_ASSIGNED',   'Tildelt til mette-larsen (TEAM_LEAD)', 'seed-migration', '2025-08-20 09:01:00+02'),
    (gen_random_uuid(), '00000000-0000-0000-0000-00000000C002', 'STATE_CHANGED',         'Status ændret til ASSESSED', 'mette-larsen', '2025-08-21 10:00:00+02'),
    (gen_random_uuid(), '00000000-0000-0000-0000-00000000C002', 'COLLECTION_MEASURE_INITIATED', 'Betalingsplan iværksat for C02', 'mette-larsen', '2025-08-22 10:30:00+02'),
    -- C003 events
    (gen_random_uuid(), '00000000-0000-0000-0000-00000000C003', 'CASE_CREATED',         'Sag oprettet ved overdragelse fra Uddannelsesstyrelsen', 'seed-migration', '2025-08-25 08:30:00+02'),
    (gen_random_uuid(), '00000000-0000-0000-0000-00000000C003', 'DEBT_ADDED',            'Fordring D01 (SU-gæld) tilknyttet', 'seed-migration', '2025-08-25 08:30:00+02'),
    (gen_random_uuid(), '00000000-0000-0000-0000-00000000C003', 'DEBT_ADDED',            'Fordring D02 (Strafferetlig bøde) tilknyttet', 'seed-migration', '2025-08-25 08:30:00+02'),
    (gen_random_uuid(), '00000000-0000-0000-0000-00000000C003', 'CASEWORKER_ASSIGNED',   'Tildelt til anna-jensen (CASEWORKER)', 'seed-migration', '2025-08-25 08:31:00+02'),
    (gen_random_uuid(), '00000000-0000-0000-0000-00000000C003', 'STATE_CHANGED',         'Status ændret til ASSESSED', 'anna-jensen', '2025-09-01 09:00:00+02'),
    (gen_random_uuid(), '00000000-0000-0000-0000-00000000C003', 'OBJECTION_RECEIVED',    'Indsigelse modtaget mod D02 (bestrider bødens størrelse)', 'anna-jensen', '2025-04-28 11:15:00+02'),
    -- C004 events
    (gen_random_uuid(), '00000000-0000-0000-0000-00000000C004', 'CASE_CREATED',         'Sag oprettet ved overdragelse fra Familieretshuset', 'seed-migration', '2026-01-10 10:00:00+01'),
    (gen_random_uuid(), '00000000-0000-0000-0000-00000000C004', 'DEBT_ADDED',            'Fordring E01 (Underholdsbidrag) tilknyttet', 'seed-migration', '2026-01-10 10:00:00+01'),
    (gen_random_uuid(), '00000000-0000-0000-0000-00000000C004', 'DEBT_ADDED',            'Fordring E02 (Dagbøde) tilknyttet', 'seed-migration', '2026-01-10 10:00:00+01'),
    (gen_random_uuid(), '00000000-0000-0000-0000-00000000C004', 'CASEWORKER_ASSIGNED',   'Tildelt til erik-sorensen (SENIOR_CASEWORKER)', 'seed-migration', '2026-01-10 10:01:00+01'),
    (gen_random_uuid(), '00000000-0000-0000-0000-00000000C004', 'STATE_CHANGED',         'Status ændret til DECIDED', 'erik-sorensen', '2026-01-14 14:30:00+01'),
    (gen_random_uuid(), '00000000-0000-0000-0000-00000000C004', 'COLLECTION_MEASURE_INITIATED', 'Lønindeholdelse iværksat for E01 — 2500 kr/mdr', 'erik-sorensen', '2026-01-15 09:00:00+01');

ALTER TABLE cases ENABLE TRIGGER cases_versioning_trigger;
ALTER TABLE cases ENABLE TRIGGER cases_audit_trigger;
ALTER TABLE collection_measures ENABLE TRIGGER collection_measures_audit_trigger;
ALTER TABLE objections ENABLE TRIGGER objections_audit_trigger;
ALTER TABLE case_journal_notes ENABLE TRIGGER case_journal_notes_audit_trigger;

