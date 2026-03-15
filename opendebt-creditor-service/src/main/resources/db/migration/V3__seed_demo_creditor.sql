-- Fix: add updated_by column to channel_bindings (entity expects it via AuditableEntity)
ALTER TABLE channel_bindings ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE channel_bindings_history ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);

-- Fix: add updated_by column to creditors if missing
ALTER TABLE creditors ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);
ALTER TABLE creditors_history ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);

-- Seed data: demo fordringshaver for portal development
-- creditor_org_id matches DashboardController.DEMO_CREDITOR_ORG_ID

INSERT INTO creditors (
    id,
    creditor_org_id,
    external_creditor_id,
    activity_status,
    connection_type,
    creditor_type,
    payment_type,
    currency_code,
    settlement_method,
    settlement_frequency,
    allow_portal_actions,
    allow_create_recovery_claims,
    allow_write_down,
    allow_write_up_adjustment,
    created_by
) VALUES (
    'a0000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000001',
    'SKAT-DEMO-001',
    'ACTIVE',
    'PORTAL',
    'SKAT',
    'INTERNAL',
    'DKK',
    'STATSREGNSKAB',
    'MONTHLY',
    TRUE,
    TRUE,
    TRUE,
    TRUE,
    'seed-migration'
) ON CONFLICT (external_creditor_id) DO NOTHING;

-- A child creditor under the parent to demonstrate hierarchy
INSERT INTO creditors (
    id,
    creditor_org_id,
    external_creditor_id,
    parent_creditor_id,
    activity_status,
    connection_type,
    creditor_type,
    payment_type,
    currency_code,
    settlement_method,
    settlement_frequency,
    allow_portal_actions,
    allow_create_recovery_claims,
    created_by
) VALUES (
    'a0000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000002',
    'SKAT-DEMO-002',
    'a0000000-0000-0000-0000-000000000001',
    'ACTIVE',
    'PORTAL',
    'SKAT',
    'INTERNAL',
    'DKK',
    'STATSREGNSKAB',
    'MONTHLY',
    TRUE,
    TRUE,
    'seed-migration'
) ON CONFLICT (external_creditor_id) DO NOTHING;

-- Channel binding so the portal can resolve the demo creditor
INSERT INTO channel_bindings (
    id,
    channel_identity,
    channel_type,
    creditor_id,
    active,
    description,
    created_by
) VALUES (
    'b0000000-0000-0000-0000-000000000001',
    'demo-portal-user',
    'PORTAL',
    'a0000000-0000-0000-0000-000000000001',
    TRUE,
    'Demo portal user binding for development',
    'seed-migration'
) ON CONFLICT (channel_identity) DO NOTHING;
