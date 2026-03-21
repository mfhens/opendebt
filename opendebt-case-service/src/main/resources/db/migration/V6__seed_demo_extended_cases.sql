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
