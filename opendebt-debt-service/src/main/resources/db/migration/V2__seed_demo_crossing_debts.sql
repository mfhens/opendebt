-- V2: Seed demo debts for crossing-transaction demo (petition 041)
-- These debts match the payment-service DemoDataSeeder UUIDs

ALTER TABLE debts DISABLE TRIGGER debts_audit_trigger;

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
        'SKAT',
        45000.00, 951.03, 0.00, 30951.03, 45000.00,
        'INDR', 'HF', 'OVERDRAGET',
        '2025-06-01', '2025-06-01',
        'IN_COLLECTION', 'READY_FOR_COLLECTION',
        'seed-migration',
        'Demo tax debt with crossing'
    ),
    (
        '00000000-0000-0000-0000-000000000B01',
        'd0000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000001',
        'BOEDE',
        12500.00, 75.00, 0.00, 7575.00, 12500.00,
        'INDR', 'HF', 'OVERDRAGET',
        '2025-05-15', '2025-05-15',
        'IN_COLLECTION', 'READY_FOR_COLLECTION',
        'seed-migration',
        'Demo fine debt'
    )
ON CONFLICT (id) DO NOTHING;

ALTER TABLE debts ENABLE TRIGGER debts_audit_trigger;
