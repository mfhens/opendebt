-- Seed data: demo debt types and fordringer for portal development
-- creditor_org_id matches DashboardController.DEMO_CREDITOR_ORG_ID

-- Temporarily disable the audit trigger on debt_types (it assumes UUID id column)
ALTER TABLE debt_types DISABLE TRIGGER debt_types_audit_trigger;

INSERT INTO debt_types (code, name, category, description, legal_basis, active, interest_applicable)
VALUES
    ('SKAT', 'Restskat', 'Skat', 'Skyldig restskat til Skattestyrelsen', 'Kildeskatteloven § 61', TRUE, TRUE),
    ('MOMS', 'Moms', 'Skat', 'Skyldig moms til Skattestyrelsen', 'Momsloven § 57', TRUE, TRUE),
    ('BOEDE', 'Bøde', 'Straf', 'Bøde pålagt af domstol eller myndighed', 'Straffeloven', TRUE, FALSE),
    ('UNDERHOLDSBIDRAG', 'Underholdsbidrag', 'Social', 'Skyldig underholdsbidrag', 'Lov om inddrivelse § 1', TRUE, FALSE),
    ('DAGINSTITUTION', 'Daginstitution', 'Kommune', 'Skyldig betaling for daginstitution', 'Dagtilbudsloven', TRUE, TRUE),
    ('EJENDOMSSKAT', 'Ejendomsskat', 'Skat', 'Skyldig ejendomsskat', 'Ejendomsskatteloven', TRUE, TRUE)
ON CONFLICT (code) DO NOTHING;

ALTER TABLE debt_types ENABLE TRIGGER debt_types_audit_trigger;

-- Demo debtor person IDs (technical UUIDs referencing person-registry, NOT PII)
INSERT INTO debts (
    id, debtor_person_id, creditor_org_id, debt_type_code,
    principal_amount, interest_amount, fees_amount, outstanding_balance,
    due_date, original_due_date, status, readiness_status, created_by
) VALUES
    (
        'c0000000-0000-0000-0000-000000000001',
        'd0000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000001',
        'SKAT',
        45000.00, 2250.00, 500.00, 32750.00,
        '2025-07-01', '2025-07-01',
        'IN_COLLECTION', 'READY_FOR_COLLECTION',
        'seed-migration'
    ),
    (
        'c0000000-0000-0000-0000-000000000002',
        'd0000000-0000-0000-0000-000000000002',
        '00000000-0000-0000-0000-000000000001',
        'MOMS',
        125000.00, 0.00, 0.00, 125000.00,
        '2025-10-01', '2025-10-01',
        'ACTIVE', 'READY_FOR_COLLECTION',
        'seed-migration'
    ),
    (
        'c0000000-0000-0000-0000-000000000003',
        'd0000000-0000-0000-0000-000000000003',
        '00000000-0000-0000-0000-000000000001',
        'BOEDE',
        5000.00, 0.00, 0.00, 5000.00,
        '2025-12-15', '2025-12-15',
        'PENDING', 'PENDING_REVIEW',
        'seed-migration'
    ),
    (
        'c0000000-0000-0000-0000-000000000004',
        'd0000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000001',
        'SKAT',
        28000.00, 1400.00, 250.00, 0.00,
        '2024-07-01', '2024-07-01',
        'PAID', 'READY_FOR_COLLECTION',
        'seed-migration'
    ),
    (
        'c0000000-0000-0000-0000-000000000005',
        'd0000000-0000-0000-0000-000000000004',
        '00000000-0000-0000-0000-000000000001',
        'EJENDOMSSKAT',
        18500.00, 925.00, 0.00, 19425.00,
        '2025-09-01', '2025-09-01',
        'DISPUTED', 'UNDER_APPEAL',
        'seed-migration'
    )
ON CONFLICT (id) DO NOTHING;
