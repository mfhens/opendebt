-- V3: OIO Sag-aligned case data model (Petition 042 - Phase 1)
-- Adds new OIO Sag fields to the cases table, creates supporting tables for
-- parties, debts, journal entries, notes, legal bases, events, relations,
-- collection measures, and objections.
--
-- IMPORTANT: Old columns are NOT dropped — they are kept for backward
-- compatibility and marked as deprecated via comments. A future migration
-- will drop them once all consumers have migrated.

-- ============================================================================
-- 1. DISABLE TRIGGERS ON CASES (prevent audit/history noise during migration)
-- ============================================================================
ALTER TABLE cases DISABLE TRIGGER cases_versioning_trigger;
ALTER TABLE cases DISABLE TRIGGER cases_audit_trigger;

-- ============================================================================
-- 2. ALTER TABLE cases — add new OIO Sag columns
-- ============================================================================

-- Core OIO Sag fields
ALTER TABLE cases ADD COLUMN IF NOT EXISTS title VARCHAR(200);
ALTER TABLE cases ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE cases ADD COLUMN IF NOT EXISTS confidential_title VARCHAR(200);
ALTER TABLE cases ADD COLUMN IF NOT EXISTS case_state VARCHAR(30);
ALTER TABLE cases ADD COLUMN IF NOT EXISTS state_changed_at TIMESTAMPTZ;
ALTER TABLE cases ADD COLUMN IF NOT EXISTS case_type VARCHAR(30);
ALTER TABLE cases ADD COLUMN IF NOT EXISTS subject_classification VARCHAR(30);
ALTER TABLE cases ADD COLUMN IF NOT EXISTS action_classification VARCHAR(30);
ALTER TABLE cases ADD COLUMN IF NOT EXISTS precedent_indicator BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE cases ADD COLUMN IF NOT EXISTS retention_override BOOLEAN;

-- Organisation / ownership
ALTER TABLE cases ADD COLUMN IF NOT EXISTS owner_organisation_id VARCHAR(100);
ALTER TABLE cases ADD COLUMN IF NOT EXISTS responsible_unit_id VARCHAR(100);
ALTER TABLE cases ADD COLUMN IF NOT EXISTS primary_caseworker_id VARCHAR(100);

-- Hierarchy / workflow
ALTER TABLE cases ADD COLUMN IF NOT EXISTS parent_case_id UUID;
ALTER TABLE cases ADD COLUMN IF NOT EXISTS workflow_process_instance_id VARCHAR(100);

-- ============================================================================
-- 3. MIGRATE EXISTING DATA (safe for both fresh and upgrade scenarios)
-- ============================================================================

-- Backfill title from case_number
UPDATE cases SET title = 'Inddrivelsessag ' || case_number WHERE title IS NULL;

-- Backfill primary_caseworker_id from assigned_caseworker_id
UPDATE cases SET primary_caseworker_id = assigned_caseworker_id
WHERE primary_caseworker_id IS NULL AND assigned_caseworker_id IS NOT NULL;

-- Map old status to new case_state
UPDATE cases SET case_state = CASE status
    WHEN 'OPEN' THEN 'CREATED'
    WHEN 'IN_PROGRESS' THEN 'ASSESSED'
    WHEN 'AWAITING_PAYMENT' THEN 'DECIDED'
    WHEN 'PAYMENT_PLAN_ACTIVE' THEN 'DECIDED'
    WHEN 'WAGE_GARNISHMENT_ACTIVE' THEN 'DECIDED'
    WHEN 'OFFSETTING_PENDING' THEN 'DECIDED'
    WHEN 'UNDER_APPEAL' THEN 'SUSPENDED'
    WHEN 'CLOSED_PAID' THEN 'CLOSED_PAID'
    WHEN 'CLOSED_WRITTEN_OFF' THEN 'CLOSED_WRITTEN_OFF'
    WHEN 'CLOSED_CANCELLED' THEN 'CLOSED_CANCELLED'
    ELSE 'CREATED'
END
WHERE case_state IS NULL AND status IS NOT NULL;

-- For fresh installs (no rows) ensure defaults
UPDATE cases SET case_state = 'CREATED' WHERE case_state IS NULL;

-- Backfill state_changed_at
UPDATE cases SET state_changed_at = updated_at WHERE state_changed_at IS NULL;

-- Backfill case_type
UPDATE cases SET case_type = 'DEBT_COLLECTION' WHERE case_type IS NULL;

-- Now apply NOT NULL constraints after backfill
ALTER TABLE cases ALTER COLUMN title SET NOT NULL;
ALTER TABLE cases ALTER COLUMN case_state SET NOT NULL;
ALTER TABLE cases ALTER COLUMN case_state SET DEFAULT 'CREATED';
ALTER TABLE cases ALTER COLUMN case_type SET NOT NULL;
ALTER TABLE cases ALTER COLUMN case_type SET DEFAULT 'DEBT_COLLECTION';

-- Drop old constraints (safe with IF EXISTS pattern)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_status') THEN
        ALTER TABLE cases DROP CONSTRAINT chk_status;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_strategy') THEN
        ALTER TABLE cases DROP CONSTRAINT chk_strategy;
    END IF;
END $$;

-- Add new constraints
ALTER TABLE cases ADD CONSTRAINT chk_case_state CHECK (case_state IN (
    'CREATED', 'ASSESSED', 'DECIDED', 'SUSPENDED',
    'CLOSED_PAID', 'CLOSED_WRITTEN_OFF', 'CLOSED_WITHDRAWN', 'CLOSED_CANCELLED'
));
ALTER TABLE cases ADD CONSTRAINT chk_case_type CHECK (case_type IN (
    'DEBT_COLLECTION', 'HEARING', 'APPEAL', 'OBJECTION'
));

-- New indexes for new columns
CREATE INDEX IF NOT EXISTS idx_case_case_state ON cases(case_state);
CREATE INDEX IF NOT EXISTS idx_case_case_type ON cases(case_type);
CREATE INDEX IF NOT EXISTS idx_case_primary_caseworker ON cases(primary_caseworker_id);
CREATE INDEX IF NOT EXISTS idx_case_parent_case_id ON cases(parent_case_id);

-- ============================================================================
-- 4. CREATE NEW TABLES
-- ============================================================================

-- Case Parties
CREATE TABLE IF NOT EXISTS case_parties (
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
CREATE INDEX IF NOT EXISTS idx_case_parties_case_id ON case_parties(case_id);
CREATE INDEX IF NOT EXISTS idx_case_parties_person_id ON case_parties(person_id);
COMMENT ON TABLE case_parties IS 'Parties (persons/orgs) associated with a case — NO PII STORED';
COMMENT ON COLUMN case_parties.person_id IS 'Reference to person-registry (UUID only)';

-- Case Debts (replaces case_debt_ids)
CREATE TABLE IF NOT EXISTS case_debts (
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
CREATE INDEX IF NOT EXISTS idx_case_debts_case_id ON case_debts(case_id);
CREATE INDEX IF NOT EXISTS idx_case_debts_debt_id ON case_debts(debt_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_case_debts_active ON case_debts(case_id, debt_id) WHERE removed_at IS NULL;
COMMENT ON TABLE case_debts IS 'Debt-to-case links with soft-delete support';

-- Case Journal Entries
CREATE TABLE IF NOT EXISTS case_journal_entries (
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
CREATE INDEX IF NOT EXISTS idx_journal_entries_case_id ON case_journal_entries(case_id);
COMMENT ON TABLE case_journal_entries IS 'OIO Journalpost entries on a case';

-- Case Journal Notes
CREATE TABLE IF NOT EXISTS case_journal_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id UUID NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    note_title VARCHAR(200) NOT NULL,
    note_text TEXT NOT NULL,
    author_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_journal_notes_case_id ON case_journal_notes(case_id);
COMMENT ON TABLE case_journal_notes IS 'Free-text notes on a case (replaces flat notes column)';

-- Case Legal Bases
CREATE TABLE IF NOT EXISTS case_legal_bases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id UUID NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    legal_source_uri VARCHAR(500),
    legal_source_title VARCHAR(300) NOT NULL,
    paragraph_reference VARCHAR(100),
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_legal_bases_case_id ON case_legal_bases(case_id);
COMMENT ON TABLE case_legal_bases IS 'Legal basis references for a case';

-- Case Events (immutable audit trail)
CREATE TABLE IF NOT EXISTS case_events (
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
CREATE INDEX IF NOT EXISTS idx_case_events_case_performed ON case_events(case_id, performed_at);
COMMENT ON TABLE case_events IS 'Immutable event log for case lifecycle';

-- Case Relations
CREATE TABLE IF NOT EXISTS case_relations (
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
CREATE INDEX IF NOT EXISTS idx_case_relations_source ON case_relations(source_case_id);
CREATE INDEX IF NOT EXISTS idx_case_relations_target ON case_relations(target_case_id);
COMMENT ON TABLE case_relations IS 'Directed relationships between cases';

-- Collection Measures
CREATE TABLE IF NOT EXISTS collection_measures (
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
CREATE INDEX IF NOT EXISTS idx_collection_measures_case_id ON collection_measures(case_id);
CREATE INDEX IF NOT EXISTS idx_collection_measures_status ON collection_measures(status);
COMMENT ON TABLE collection_measures IS 'Collection measures (inddrivelsesskridt) applied to a case/debt';

-- Objections
CREATE TABLE IF NOT EXISTS objections (
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
CREATE INDEX IF NOT EXISTS idx_objections_case_id ON objections(case_id);
CREATE INDEX IF NOT EXISTS idx_objections_status ON objections(status);
COMMENT ON TABLE objections IS 'Debtor objections (indsigelser) against cases/debts';

-- ============================================================================
-- 5. MIGRATE EXISTING DATA INTO NEW TABLES
-- ============================================================================

-- Migrate debtor_person_id -> case_parties (PRIMARY_DEBTOR)
INSERT INTO case_parties (id, case_id, person_id, party_role, party_type, active_from, added_by, created_at)
SELECT gen_random_uuid(), id, debtor_person_id, 'PRIMARY_DEBTOR', 'PERSON', created_at::date, 'v3-migration', CURRENT_TIMESTAMP
FROM cases
WHERE debtor_person_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM case_parties cp
      WHERE cp.case_id = cases.id AND cp.party_role = 'PRIMARY_DEBTOR'
  );

-- Migrate case_debt_ids -> case_debts
INSERT INTO case_debts (id, case_id, debt_id, added_at, added_by)
SELECT gen_random_uuid(), cdi.case_id, cdi.debt_id, COALESCE(cdi.added_at, CURRENT_TIMESTAMP), COALESCE(cdi.added_by, 'v3-migration')
FROM case_debt_ids cdi
WHERE NOT EXISTS (
    SELECT 1 FROM case_debts cd
    WHERE cd.case_id = cdi.case_id AND cd.debt_id = cdi.debt_id AND cd.removed_at IS NULL
);

-- Migrate notes -> case_journal_notes (only non-empty notes)
INSERT INTO case_journal_notes (id, case_id, note_title, note_text, author_id, created_at, updated_at)
SELECT gen_random_uuid(), id, 'Migrated note', notes, COALESCE(created_by, 'v3-migration'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM cases
WHERE notes IS NOT NULL AND TRIM(notes) <> ''
  AND NOT EXISTS (
      SELECT 1 FROM case_journal_notes cjn
      WHERE cjn.case_id = cases.id AND cjn.note_title = 'Migrated note'
  );

-- Migrate active_strategy -> collection_measures
INSERT INTO collection_measures (id, case_id, measure_type, status, start_date, created_by, created_at, updated_at)
SELECT gen_random_uuid(), id,
    CASE active_strategy
        WHEN 'LEGAL_ACTION' THEN 'ATTACHMENT'
        ELSE active_strategy
    END,
    CASE
        WHEN status LIKE 'CLOSED_%' THEN 'COMPLETED'
        ELSE 'ACTIVE'
    END,
    created_at::date, 'v3-migration', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM cases
WHERE active_strategy IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM collection_measures cm
      WHERE cm.case_id = cases.id
  );

-- ============================================================================
-- 6. HISTORY TABLES FOR NEW MUTABLE TABLES
-- ============================================================================

-- Case Journal Notes history
CREATE TABLE IF NOT EXISTS case_journal_notes_history (
    LIKE case_journal_notes,
    sys_period TSTZRANGE NOT NULL DEFAULT tstzrange(CURRENT_TIMESTAMP, NULL)
);

-- Collection Measures history
CREATE TABLE IF NOT EXISTS collection_measures_history (
    LIKE collection_measures,
    sys_period TSTZRANGE NOT NULL DEFAULT tstzrange(CURRENT_TIMESTAMP, NULL)
);

-- Objections history
CREATE TABLE IF NOT EXISTS objections_history (
    LIKE objections,
    sys_period TSTZRANGE NOT NULL DEFAULT tstzrange(CURRENT_TIMESTAMP, NULL)
);

-- ============================================================================
-- 7. AUDIT TRIGGERS FOR NEW MUTABLE TABLES
-- ============================================================================

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
-- 8. UPDATE CASES HISTORY TABLE (add new columns)
-- ============================================================================

ALTER TABLE cases_history ADD COLUMN IF NOT EXISTS title VARCHAR(200);
ALTER TABLE cases_history ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE cases_history ADD COLUMN IF NOT EXISTS confidential_title VARCHAR(200);
ALTER TABLE cases_history ADD COLUMN IF NOT EXISTS case_state VARCHAR(30);
ALTER TABLE cases_history ADD COLUMN IF NOT EXISTS state_changed_at TIMESTAMPTZ;
ALTER TABLE cases_history ADD COLUMN IF NOT EXISTS case_type VARCHAR(30);
ALTER TABLE cases_history ADD COLUMN IF NOT EXISTS subject_classification VARCHAR(30);
ALTER TABLE cases_history ADD COLUMN IF NOT EXISTS action_classification VARCHAR(30);
ALTER TABLE cases_history ADD COLUMN IF NOT EXISTS precedent_indicator BOOLEAN;
ALTER TABLE cases_history ADD COLUMN IF NOT EXISTS retention_override BOOLEAN;
ALTER TABLE cases_history ADD COLUMN IF NOT EXISTS owner_organisation_id VARCHAR(100);
ALTER TABLE cases_history ADD COLUMN IF NOT EXISTS responsible_unit_id VARCHAR(100);
ALTER TABLE cases_history ADD COLUMN IF NOT EXISTS primary_caseworker_id VARCHAR(100);
ALTER TABLE cases_history ADD COLUMN IF NOT EXISTS parent_case_id UUID;
ALTER TABLE cases_history ADD COLUMN IF NOT EXISTS workflow_process_instance_id VARCHAR(100);

-- ============================================================================
-- 9. MARK DEPRECATED COLUMNS (comments only — no DROP)
-- ============================================================================

COMMENT ON COLUMN cases.debtor_person_id IS 'DEPRECATED (V3): moved to case_parties with role PRIMARY_DEBTOR';
COMMENT ON COLUMN cases.status IS 'DEPRECATED (V3): replaced by case_state';
COMMENT ON COLUMN cases.total_debt IS 'DEPRECATED (V3): computed on demand from debt-service';
COMMENT ON COLUMN cases.total_paid IS 'DEPRECATED (V3): computed on demand from debt-service';
COMMENT ON COLUMN cases.total_remaining IS 'DEPRECATED (V3): computed on demand from debt-service';
COMMENT ON COLUMN cases.active_strategy IS 'DEPRECATED (V3): replaced by collection_measures table';
COMMENT ON COLUMN cases.assigned_caseworker_id IS 'DEPRECATED (V3): replaced by primary_caseworker_id';
COMMENT ON COLUMN cases.notes IS 'DEPRECATED (V3): replaced by case_journal_notes table';
COMMENT ON COLUMN cases.last_activity_at IS 'DEPRECATED (V3): replaced by case_events table';

-- Make deprecated columns nullable (they were NOT NULL before for debtor_person_id)
-- This allows new rows to be inserted without providing deprecated values
DO $$
BEGIN
    -- debtor_person_id was NOT NULL — make it nullable for new inserts
    ALTER TABLE cases ALTER COLUMN debtor_person_id DROP NOT NULL;
    -- status was NOT NULL — make it nullable
    ALTER TABLE cases ALTER COLUMN status DROP NOT NULL;
    -- Drop default on status so new rows don't get stale defaults
    ALTER TABLE cases ALTER COLUMN status DROP DEFAULT;
EXCEPTION
    WHEN others THEN NULL; -- ignore if already done
END $$;

-- ============================================================================
-- 10. RE-ENABLE TRIGGERS
-- ============================================================================

ALTER TABLE cases ENABLE TRIGGER cases_versioning_trigger;
ALTER TABLE cases ENABLE TRIGGER cases_audit_trigger;

-- ============================================================================
-- COMMENTS
-- ============================================================================

COMMENT ON COLUMN cases.title IS 'OIO Sag: case title';
COMMENT ON COLUMN cases.case_state IS 'OIO Sag: lifecycle state (replaces status)';
COMMENT ON COLUMN cases.case_type IS 'OIO Sag: case type classification';
COMMENT ON COLUMN cases.primary_caseworker_id IS 'OIO Sag: primary responsible caseworker (replaces assigned_caseworker_id)';
