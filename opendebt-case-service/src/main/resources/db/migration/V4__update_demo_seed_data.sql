-- V4: Update demo seed data for the OIO Sag model (Petition 042)
-- Updates the demo case SAG-2025-00042 created in V2 to use the new schema.

ALTER TABLE cases DISABLE TRIGGER cases_versioning_trigger;
ALTER TABLE cases DISABLE TRIGGER cases_audit_trigger;

-- Update the demo case with new OIO Sag fields (V3 migration already back-filled
-- some of these, but we set explicit demo-friendly values here).
UPDATE cases SET
    title = 'Inddrivelsessag SAG-2025-00042',
    case_state = 'ASSESSED',
    state_changed_at = CURRENT_TIMESTAMP,
    case_type = 'DEBT_COLLECTION',
    primary_caseworker_id = 'caseworker-demo',
    owner_organisation_id = 'UFST'
WHERE id = '00000000-0000-0000-0000-00000000C001';

-- Ensure case party exists for the demo debtor (V3 already migrated, but be safe)
INSERT INTO case_parties (id, case_id, person_id, party_role, party_type, active_from, added_by, created_at)
VALUES (
    '00000000-0000-0000-0000-00000000C501',
    '00000000-0000-0000-0000-00000000C001',
    'd0000000-0000-0000-0000-000000000001',
    'PRIMARY_DEBTOR', 'PERSON',
    '2025-01-01', 'v4-demo-seed', CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;

-- Ensure case_debts rows exist (V3 already migrated from case_debt_ids)
INSERT INTO case_debts (id, case_id, debt_id, added_by)
VALUES
    ('00000000-0000-0000-0000-0000000CD001', '00000000-0000-0000-0000-00000000C001', '00000000-0000-0000-0000-000000000A01', 'v4-demo-seed'),
    ('00000000-0000-0000-0000-0000000CD002', '00000000-0000-0000-0000-00000000C001', '00000000-0000-0000-0000-000000000B01', 'v4-demo-seed')
ON CONFLICT DO NOTHING;

-- Add sample case events for the demo case
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

ALTER TABLE cases ENABLE TRIGGER cases_versioning_trigger;
ALTER TABLE cases ENABLE TRIGGER cases_audit_trigger;
