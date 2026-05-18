-- ============================================================================
-- Petition059 E2E seed — opendebt_debt database
-- ============================================================================
-- Purpose : Seed 4 ACTIVE foraeldelse_record rows for the petition059
--           Playwright E2E harness (@backlog tag, manual execution only).
-- Idempotent: DELETEs related rows first, then upserts foraeldelse_record.
--
-- fordring_id values MUST match the debt_id values seeded in opendebt_case
-- via petition059-case-seed.sql.
--
-- Branch coverage map (matches activePairs[] assignment in spec):
--   pairs[0] → 05900000-e2e0-0059-fd00-000000000001
--              FR-7.1 structural panel (udskydelseDato visible),
--              FR-7.2 afbrydelse events,
--              FR-7.4 write button, FR-7.3 fordringB (complex member)
--   pairs[1] → 05900000-e2e0-0059-fd00-000000000002
--              FR-7.3 fordringA (complex + tillaegsfrist + afbrydelse) [exclusive]
--   pairs[2] → 05900000-e2e0-0059-fd00-000000000003
--              FR-7.5 indsigelse → INDSIGELSE_PENDING [exclusive]
--   pairs[3] → 05900000-e2e0-0059-fd00-000000000004
--              FR-7.6 indsigelse → evaluate VALID → FORAELDET
--   static  → 05900000-e2e0-0059-fd00-000000000005  (fd00-005)
--              FR-7.3 fordringB: static complex-member UUID (not in activePairs).
--              Never mutated by any other test; eliminates optimistic-lock conflicts
--              when createComplex() and FR-7.2 concurrently save foraeldelse_record.
--
-- fd00-002 is EXCLUSIVE to FR-7.3. fd00-003 is EXCLUSIVE to FR-7.5.
-- fd00-005 is ONLY used as a static complex-member and never written to by
-- any other parallel test, so @Version conflicts cannot occur.
--
-- retsgrundlag: ORDINARY (3-year period under GFL § 18)
-- status:       ACTIVE (all 4 reset to ACTIVE on every seed run)
-- ============================================================================

BEGIN;

-- ── Reset related tables (safe: P059 e2e UUIDs only) ──────────────────────────
DELETE FROM limitation_objection_linkage
WHERE fordring_id IN (
    '05900000-e2e0-0059-fd00-000000000001',
    '05900000-e2e0-0059-fd00-000000000002',
    '05900000-e2e0-0059-fd00-000000000003',
    '05900000-e2e0-0059-fd00-000000000004',
    '05900000-e2e0-0059-fd00-000000000005'
);

DELETE FROM afbrydelse_event
WHERE fordring_id IN (
    '05900000-e2e0-0059-fd00-000000000001',
    '05900000-e2e0-0059-fd00-000000000002',
    '05900000-e2e0-0059-fd00-000000000003',
    '05900000-e2e0-0059-fd00-000000000004',
    '05900000-e2e0-0059-fd00-000000000005'
);

DELETE FROM tillaegsfrist_event
WHERE fordring_id IN (
    '05900000-e2e0-0059-fd00-000000000001',
    '05900000-e2e0-0059-fd00-000000000002',
    '05900000-e2e0-0059-fd00-000000000003',
    '05900000-e2e0-0059-fd00-000000000004',
    '05900000-e2e0-0059-fd00-000000000005'
);

DELETE FROM fordringskompleks_link
WHERE fordring_id IN (
    '05900000-e2e0-0059-fd00-000000000001',
    '05900000-e2e0-0059-fd00-000000000002',
    '05900000-e2e0-0059-fd00-000000000003',
    '05900000-e2e0-0059-fd00-000000000004',
    '05900000-e2e0-0059-fd00-000000000005'
);

-- ── Upsert foraeldelse_record (ACTIVE, udskydelse_dato set for FR-7.1) ────────
INSERT INTO foraeldelse_record (
    id,
    fordring_id,
    debtor_person_id,
    retsgrundlag,
    udskydelse_dato,
    is_in_udskydelse,
    current_frist_expires,
    status,
    kompleks_id,
    created_at,
    created_by,
    version
)
VALUES
    -- Fordring-A  (activePairs[0])
    -- udskydelse_dato is non-null so the <dd> renders visibly (FR-7.1).
    -- Stays ACTIVE throughout; FR-7.2 appends afbrydelse events.
    -- FR-7.4 asserts the registration button is visible.
    (
        '05900000-e2e0-0059-fa00-000000000001',
        '05900000-e2e0-0059-fd00-000000000001',
        '05900000-e2e0-0059-de00-000000000001',
        'ORDINARY',
        '2031-03-15',
        true,
        '2030-03-15',
        'ACTIVE',
        NULL,
        NOW(),
        'e2e-seed-p059',
        0
    ),
    -- Fordring-B  (activePairs[1])
    -- FR-7.3 fordringA: paired with fordring-A to form a fordringskompleks;
    --         also receives a tillaegsfrist event and an afbrydelse event
    --         (the afbrydelse event ensures the afbrydelse table section renders).
    -- Exclusive to FR-7.3 to avoid optimistic-lock conflicts with FR-7.5.
    (
        '05900000-e2e0-0059-fa00-000000000002',
        '05900000-e2e0-0059-fd00-000000000002',
        '05900000-e2e0-0059-de00-000000000001',
        'ORDINARY',
        '2031-06-30',
        true,
        '2030-06-30',
        'ACTIVE',
        NULL,
        NOW(),
        'e2e-seed-p059',
        0
    ),
    -- Fordring-C  (activePairs[2])
    -- FR-7.5: receives an indsigelse → status transitions to INDSIGELSE_PENDING.
    -- Exclusive to FR-7.5 to avoid optimistic-lock conflicts with FR-7.3.
    (
        '05900000-e2e0-0059-fa00-000000000003',
        '05900000-e2e0-0059-fd00-000000000003',
        '05900000-e2e0-0059-de00-000000000001',
        'ORDINARY',
        '2031-12-31',
        true,
        '2030-12-31',
        'ACTIVE',
        NULL,
        NOW(),
        'e2e-seed-p059',
        0
    ),
    -- Fordring-D  (activePairs[3])
    -- FR-7.6: receives an indsigelse then evaluate(VALID) →
    --         status transitions to FORAELDET.
    (
        '05900000-e2e0-0059-fa00-000000000004',
        '05900000-e2e0-0059-fd00-000000000004',
        '05900000-e2e0-0059-de00-000000000001',
        'ORDINARY',
        '2031-09-15',
        true,
        '2030-09-15',
        'ACTIVE',
        NULL,
        NOW(),
        'e2e-seed-p059',
        0
    ),
    -- Fordring-E  (static complex-member, not in activePairs)
    -- FR-7.3 fordringB: used as the second member in the fordringskompleks.
    -- Static UUID (not discovered via findActiveFordringer). Exclusive to FR-7.3,
    -- ensuring createComplex() does not race with FR-7.2 on fd00-001's record.
    (
        '05900000-e2e0-0059-fa00-000000000005',
        '05900000-e2e0-0059-fd00-000000000005',
        '05900000-e2e0-0059-de00-000000000001',
        'ORDINARY',
        '2032-01-01',
        false,
        '2031-01-01',
        'ACTIVE',
        NULL,
        NOW(),
        'e2e-seed-p059',
        0
    )
ON CONFLICT (id) DO UPDATE SET
    status           = EXCLUDED.status,
    udskydelse_dato  = EXCLUDED.udskydelse_dato,
    is_in_udskydelse = EXCLUDED.is_in_udskydelse,
    kompleks_id      = EXCLUDED.kompleks_id,
    updated_at       = NOW(),
    version          = foraeldelse_record.version + 1;

COMMIT;
