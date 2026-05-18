-- ============================================================================
-- Petition059 E2E seed — opendebt_case database
-- ============================================================================
-- Purpose : Seed 4 dedicated cases + case_debts links for the petition059
--           Playwright E2E harness (@backlog tag, manual execution only).
-- Idempotent: all INSERTs use ON CONFLICT DO NOTHING.
--
-- UUID namespace (prefix 05900000-e2e0-0059-*):
--   Cases      ca00  → 001/002/003/004
--   Fordringer fd00  → 001/002/003/004   (debt_id used in case_debts)
--   Case debts cc00  → 001/002/003/004
--
-- Branch coverage:
--   pairs[0] fd00-001 → FR-7.1 (structural), FR-7.2 (afbrydelse), FR-7.4 (write btn)
--   pairs[1] fd00-002 → FR-7.3 (fordringA complex+tillaegsfrist+afbrydelse) [exclusive]
--   pairs[0] fd00-001 → FR-7.3 (fordringB in complex)
--   pairs[2] fd00-003 → FR-7.5 (indsigelse → INDSIGELSE_PENDING) [exclusive]
--   pairs[3] fd00-004 → FR-7.6 (foraeldet via indsigelse→evaluate)
--   any pair          → FR-7.7 (read-only)
--
-- Note: fd00-002 and fd00-003 are now EXCLUSIVE to FR-7.3 and FR-7.5
-- respectively, preventing optimistic-lock conflicts during parallel test runs.
-- ============================================================================

BEGIN;

-- Disable versioning trigger to avoid sys_period friction during seed INSERT
ALTER TABLE cases DISABLE TRIGGER cases_versioning_trigger;

INSERT INTO cases (
    id, case_number,
    title,
    case_state, case_type,
    primary_caseworker_id,
    status,
    total_debt, total_paid, total_remaining,
    created_by
) VALUES
    -- pairs[0]: used by FR-7.1, FR-7.2 (afbrydelse events), FR-7.4 (write btn),
    --           and FR-7.3 as fordringB (complex member)
    (
        '05900000-e2e0-0059-ca00-000000000001',
        'P059-E2E-A',
        'E2E Limitation Test — Fordring A',
        'ASSESSED', 'DEBT_COLLECTION',
        'anna-jensen',
        'IN_PROGRESS',
        10000.00, 0.00, 10000.00,
        'e2e-seed-p059'
    ),
    -- pairs[1]: used by FR-7.3 (fordringA for complex+tillaegsfrist), FR-7.5 (indsigelse)
    (
        '05900000-e2e0-0059-ca00-000000000002',
        'P059-E2E-B',
        'E2E Limitation Test — Fordring B',
        'ASSESSED', 'DEBT_COLLECTION',
        'anna-jensen',
        'IN_PROGRESS',
        10000.00, 0.00, 10000.00,
        'e2e-seed-p059'
    ),
    -- pairs[2]: used by FR-7.5 (indsigelse → INDSIGELSE_PENDING) [exclusive]
    (
        '05900000-e2e0-0059-ca00-000000000003',
        'P059-E2E-C',
        'E2E Limitation Test — Fordring C',
        'ASSESSED', 'DEBT_COLLECTION',
        'anna-jensen',
        'IN_PROGRESS',
        10000.00, 0.00, 10000.00,
        'e2e-seed-p059'
    ),
    -- pairs[3]: used by FR-7.6 (indsigelse → evaluate → FORAELDET)
    (
        '05900000-e2e0-0059-ca00-000000000004',
        'P059-E2E-D',
        'E2E Limitation Test — Fordring D',
        'ASSESSED', 'DEBT_COLLECTION',
        'anna-jensen',
        'IN_PROGRESS',
        10000.00, 0.00, 10000.00,
        'e2e-seed-p059'
    )
ON CONFLICT (id) DO NOTHING;

ALTER TABLE cases ENABLE TRIGGER cases_versioning_trigger;

-- Link each case to its dedicated fordring (debt_id = fordringId in debt-service)
INSERT INTO case_debts (id, case_id, debt_id, added_by, notes)
VALUES
    ('05900000-e2e0-0059-cc00-000000000001',
     '05900000-e2e0-0059-ca00-000000000001',
     '05900000-e2e0-0059-fd00-000000000001',
     'e2e-seed-p059',
     'Fordring-A: pairs[0] — afbrydelse + write-btn + complex-member'),
    ('05900000-e2e0-0059-cc00-000000000002',
     '05900000-e2e0-0059-ca00-000000000002',
     '05900000-e2e0-0059-fd00-000000000002',
     'e2e-seed-p059',
     'Fordring-B: pairs[1] — complex-fordringA + indsigelse'),
    ('05900000-e2e0-0059-cc00-000000000003',
     '05900000-e2e0-0059-ca00-000000000003',
     '05900000-e2e0-0059-fd00-000000000003',
     'e2e-seed-p059',
     'Fordring-C: pairs[2] — indsigelse only (exclusive to FR-7.5)'),
    ('05900000-e2e0-0059-cc00-000000000004',
     '05900000-e2e0-0059-ca00-000000000004',
     '05900000-e2e0-0059-fd00-000000000004',
     'e2e-seed-p059',
     'Fordring-D: pairs[3] — foraeldet via indsigelse+evaluate (FR-7.6)')
ON CONFLICT (id) DO NOTHING;

COMMIT;
