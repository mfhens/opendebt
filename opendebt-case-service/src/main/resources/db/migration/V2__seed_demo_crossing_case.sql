-- V2: Seed demo case for crossing-transaction demo (petition 041)
-- Links to the two demo debts in debt-service

ALTER TABLE cases DISABLE TRIGGER cases_audit_trigger;

INSERT INTO cases (
    id, case_number, debtor_person_id,
    status, total_debt, total_paid, total_remaining,
    active_strategy, assigned_caseworker_id,
    notes, created_by
) VALUES (
    '00000000-0000-0000-0000-00000000C001',
    'SAG-2025-00042',
    'd0000000-0000-0000-0000-000000000001',
    'IN_PROGRESS',
    57500.00, 15000.00, 42500.00,
    'VOLUNTARY_PAYMENT',
    'caseworker-demo',
    'Demo case with crossing-transaction for tax and fine debts',
    'seed-migration'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO case_debt_ids (case_id, debt_id, added_by)
VALUES
    ('00000000-0000-0000-0000-00000000C001', '00000000-0000-0000-0000-000000000A01', 'seed-migration'),
    ('00000000-0000-0000-0000-00000000C001', '00000000-0000-0000-0000-000000000B01', 'seed-migration')
ON CONFLICT (case_id, debt_id) DO NOTHING;

ALTER TABLE cases ENABLE TRIGGER cases_audit_trigger;
