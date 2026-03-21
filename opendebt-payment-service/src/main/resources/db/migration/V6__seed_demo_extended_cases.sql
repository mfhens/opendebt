-- V6: Payment-service ledger entries for extended demo cases (petition 046/047)
--
-- Double-entry bookkeeping for 6 new debts across 3 cases.
-- Chart of accounts (from V2):
--   1000  Fordringer               (ASSET   — receivables)
--   1100  Renter tilgodehavende    (ASSET   — accrued interest receivable)
--   2000  SKB Bankkonto            (ASSET   — cash received from SKB)
--   3000  Indrivelsesindtaegter    (REVENUE — claim collection revenue)
--   3100  Renteindtaegter          (REVENUE — interest revenue)
--
-- Pattern per debt:
--   DEBT_REGISTRATION  → DEBIT 1000 / CREDIT 3000 (principal)
--   INTEREST_ACCRUAL   → DEBIT 1100 / CREDIT 3100  (per accrual batch)
--   PAYMENT            → DEBIT 2000 / CREDIT 1100 (interest) / CREDIT 1000 (principal)
--   FEE                → DEBIT 1000 / CREDIT 3000  (adds fee to receivable)

-- ============================================================================
-- DEBT EVENTS (source of truth for replay)
-- ============================================================================

INSERT INTO debt_events (id, debt_id, event_type, effective_date, amount, reference, description, created_at) VALUES

    -- C01: TOLD, principal 38500, no payments
    ('0C010000-0000-0000-0001-000000000001', '00000000-0000-0000-0000-000000000C01',
     'DEBT_REGISTERED',  '2025-04-15', 38500.00, 'TOLD-C01-2025-REG', 'Toldskyld registreret til inddrivelse', NOW()),
    ('0C010000-0000-0000-0001-000000000002', '00000000-0000-0000-0000-000000000C01',
     'INTEREST_ACCRUED', '2026-01-31', 122.67,   'TOLD-C01-INT-2026-01', 'Månedlig rentetilskrivning januar 2026', NOW()),
    ('0C010000-0000-0000-0001-000000000003', '00000000-0000-0000-0000-000000000C01',
     'INTEREST_ACCRUED', '2026-02-28', 110.82,   'TOLD-C01-INT-2026-02', 'Månedlig rentetilskrivning februar 2026', NOW()),

    -- C02: TOLD afdrag, principal 12800, payment 3000 on 2025-08-15
    ('0C020000-0000-0000-0002-000000000001', '00000000-0000-0000-0000-000000000C02',
     'DEBT_REGISTERED',  '2025-05-01', 12800.00, 'TOLD-C02-2025-REG', 'Toldskyld afdrag registreret til inddrivelse', NOW()),
    ('0C020000-0000-0000-0002-000000000002', '00000000-0000-0000-0000-000000000C02',
     'PAYMENT_RECEIVED', '2025-08-15', 3000.00,  'TOLD-C02-PAY-2025-08', 'Frivillig betaling via NemKonto', NOW()),
    ('0C020000-0000-0000-0002-000000000003', '00000000-0000-0000-0000-000000000C02',
     'INTEREST_ACCRUED', '2026-01-31', 22.94,    'TOLD-C02-INT-2026-01', 'Månedlig rentetilskrivning januar 2026', NOW()),
    ('0C020000-0000-0000-0002-000000000004', '00000000-0000-0000-0000-000000000C02',
     'INTEREST_ACCRUED', '2026-02-28', 20.72,    'TOLD-C02-INT-2026-02', 'Månedlig rentetilskrivning februar 2026', NOW()),

    -- D01: SU-gæld, principal 24750, payment 5000 on 2025-11-01
    ('0D010000-0000-0000-0003-000000000001', '00000000-0000-0000-0000-000000000D01',
     'DEBT_REGISTERED',  '2025-03-01', 24750.00, 'SU-D01-2025-REG', 'SU-gæld registreret til inddrivelse', NOW()),
    ('0D010000-0000-0000-0003-000000000002', '00000000-0000-0000-0000-000000000D01',
     'PAYMENT_RECEIVED', '2025-11-01', 5000.00,  'SU-D01-PAY-2025-11', 'Frivillig delvis betaling via NemKonto', NOW()),
    ('0D010000-0000-0000-0003-000000000003', '00000000-0000-0000-0000-000000000D01',
     'INTEREST_ACCRUED', '2026-01-31', 101.36,   'SU-D01-INT-2026-01', 'Månedlig rentetilskrivning januar 2026', NOW()),
    ('0D010000-0000-0000-0003-000000000004', '00000000-0000-0000-0000-000000000D01',
     'INTEREST_ACCRUED', '2026-02-28', 91.57,    'SU-D01-INT-2026-02', 'Månedlig rentetilskrivning februar 2026', NOW()),

    -- D02: Strafferetlig bøde, principal 5000, fee 65, no interest
    ('0D020000-0000-0000-0004-000000000001', '00000000-0000-0000-0000-000000000D02',
     'DEBT_REGISTERED',  '2025-04-01', 5000.00,  'STRAF-D02-2025-REG', 'Strafferetlig bøde registreret til inddrivelse', NOW()),

    -- E01: Underholdsbidrag, principal 18400, fee 100, no payments
    ('0E010000-0000-0000-0005-000000000001', '00000000-0000-0000-0000-000000000E01',
     'DEBT_REGISTERED',  '2025-07-01', 18400.00, 'BIDR-E01-2025-REG', 'Underholdsbidrag registreret til inddrivelse', NOW()),
    ('0E010000-0000-0000-0005-000000000002', '00000000-0000-0000-0000-000000000E01',
     'INTEREST_ACCRUED', '2026-01-31', 87.69,    'BIDR-E01-INT-2026-01', 'Månedlig rentetilskrivning januar 2026', NOW()),
    ('0E010000-0000-0000-0005-000000000003', '00000000-0000-0000-0000-000000000E01',
     'INTEREST_ACCRUED', '2026-02-28', 79.21,    'BIDR-E01-INT-2026-02', 'Månedlig rentetilskrivning februar 2026', NOW()),

    -- E02: Dagbøde, principal 3200, no payments
    ('0E020000-0000-0000-0006-000000000001', '00000000-0000-0000-0000-000000000E02',
     'DEBT_REGISTERED',  '2025-09-15', 3200.00,  'DAGB-E02-2025-REG', 'Dagbøde registreret til inddrivelse', NOW()),
    ('0E020000-0000-0000-0006-000000000002', '00000000-0000-0000-0000-000000000E02',
     'INTEREST_ACCRUED', '2026-01-31', 15.25,    'DAGB-E02-INT-2026-01', 'Månedlig rentetilskrivning januar 2026', NOW()),
    ('0E020000-0000-0000-0006-000000000003', '00000000-0000-0000-0000-000000000E02',
     'INTEREST_ACCRUED', '2026-02-28', 13.77,    'DAGB-E02-INT-2026-02', 'Månedlig rentetilskrivning februar 2026', NOW())

ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- LEDGER ENTRIES (double-entry)
-- Each INSERT block covers one transaction (transaction_id groups the entries)
-- ============================================================================

-- ─── C01 DEBT REGISTRATION (38500) ───────────────────────────────────────────
INSERT INTO ledger_entries (transaction_id, debt_id, account_code, account_name, entry_type, amount, effective_date, posting_date, reference, description, entry_category, accounting_target)
VALUES
    ('0C010000-0000-0000-0001-000000000001', '00000000-0000-0000-0000-000000000C01', '1000', 'Fordringer',            'DEBIT',  38500.00, '2025-04-15', '2025-04-15', 'TOLD-C01-2025-REG', 'Toldskyld registreret — EUTK art. 114',  'DEBT_REGISTRATION', 'FORDRINGSHAVER'),
    ('0C010000-0000-0000-0001-000000000001', '00000000-0000-0000-0000-000000000C01', '3000', 'Indrivelsesindtaegter', 'CREDIT', 38500.00, '2025-04-15', '2025-04-15', 'TOLD-C01-2025-REG', 'Toldskyld modtaget til inddrivelse',      'DEBT_REGISTRATION', 'FORDRINGSHAVER')
ON CONFLICT DO NOTHING;

-- ─── C01 INTEREST — Januar 2026 (122.67) ─────────────────────────────────────
INSERT INTO ledger_entries (transaction_id, debt_id, account_code, account_name, entry_type, amount, effective_date, posting_date, reference, description, entry_category, accounting_target)
VALUES
    ('0C010000-0000-0000-0001-000000000002', '00000000-0000-0000-0000-000000000C01', '1100', 'Renter tilgodehavende', 'DEBIT',  122.67, '2026-01-31', '2026-01-31', 'TOLD-C01-INT-2026-01', 'Inddrivelsesrente januar 2026 @ 3.75%', 'INTEREST_ACCRUAL', 'FORDRINGSHAVER'),
    ('0C010000-0000-0000-0001-000000000002', '00000000-0000-0000-0000-000000000C01', '3100', 'Renteindtaegter',       'CREDIT', 122.67, '2026-01-31', '2026-01-31', 'TOLD-C01-INT-2026-01', 'Renteindtægt januar 2026',              'INTEREST_ACCRUAL', 'FORDRINGSHAVER')
ON CONFLICT DO NOTHING;

-- ─── C01 INTEREST — Februar 2026 (110.82) ────────────────────────────────────
INSERT INTO ledger_entries (transaction_id, debt_id, account_code, account_name, entry_type, amount, effective_date, posting_date, reference, description, entry_category, accounting_target)
VALUES
    ('0C010000-0000-0000-0001-000000000003', '00000000-0000-0000-0000-000000000C01', '1100', 'Renter tilgodehavende', 'DEBIT',  110.82, '2026-02-28', '2026-02-28', 'TOLD-C01-INT-2026-02', 'Inddrivelsesrente februar 2026 @ 3.75%', 'INTEREST_ACCRUAL', 'FORDRINGSHAVER'),
    ('0C010000-0000-0000-0001-000000000003', '00000000-0000-0000-0000-000000000C01', '3100', 'Renteindtaegter',       'CREDIT', 110.82, '2026-02-28', '2026-02-28', 'TOLD-C01-INT-2026-02', 'Renteindtægt februar 2026',               'INTEREST_ACCRUAL', 'FORDRINGSHAVER')
ON CONFLICT DO NOTHING;

-- ─── C02 DEBT REGISTRATION (12800) ───────────────────────────────────────────
INSERT INTO ledger_entries (transaction_id, debt_id, account_code, account_name, entry_type, amount, effective_date, posting_date, reference, description, entry_category, accounting_target)
VALUES
    ('0C020000-0000-0000-0002-000000000001', '00000000-0000-0000-0000-000000000C02', '1000', 'Fordringer',            'DEBIT',  12800.00, '2025-05-01', '2025-05-01', 'TOLD-C02-2025-REG', 'Toldskyld afdrag registreret — EUTK art. 114 stk. 2', 'DEBT_REGISTRATION', 'FORDRINGSHAVER'),
    ('0C020000-0000-0000-0002-000000000001', '00000000-0000-0000-0000-000000000C02', '3000', 'Indrivelsesindtaegter', 'CREDIT', 12800.00, '2025-05-01', '2025-05-01', 'TOLD-C02-2025-REG', 'Toldskyld afdrag modtaget til inddrivelse',              'DEBT_REGISTRATION', 'FORDRINGSHAVER')
ON CONFLICT DO NOTHING;

-- ─── C02 PAYMENT 3000 on 2025-08-15 (coverage: 115 interest + 2885 principal) ─
INSERT INTO ledger_entries (transaction_id, debt_id, account_code, account_name, entry_type, amount, effective_date, posting_date, reference, description, entry_category, accounting_target)
VALUES
    ('0C020000-0000-0000-0002-000000000002', '00000000-0000-0000-0000-000000000C02', '2000', 'SKB Bankkonto',         'DEBIT',  3000.00, '2025-08-15', '2025-08-15', 'TOLD-C02-PAY-2025-08', 'Betaling modtaget via NemKonto',               'PAYMENT', 'FORDRINGSHAVER'),
    ('0C020000-0000-0000-0002-000000000002', '00000000-0000-0000-0000-000000000C02', '1100', 'Renter tilgodehavende', 'CREDIT', 115.18,  '2025-08-15', '2025-08-15', 'TOLD-C02-PAY-2025-08', 'Dækning: renter (dækningsrækkefølge 1/3)',     'PAYMENT', 'FORDRINGSHAVER'),
    ('0C020000-0000-0000-0002-000000000002', '00000000-0000-0000-0000-000000000C02', '1000', 'Fordringer',            'CREDIT', 2884.82, '2025-08-15', '2025-08-15', 'TOLD-C02-PAY-2025-08', 'Dækning: hovedstol (dækningsrækkefølge 3/3)',  'PAYMENT', 'FORDRINGSHAVER')
ON CONFLICT DO NOTHING;

-- ─── C02 INTEREST — Januar 2026 (22.94) ──────────────────────────────────────
INSERT INTO ledger_entries (transaction_id, debt_id, account_code, account_name, entry_type, amount, effective_date, posting_date, reference, description, entry_category, accounting_target)
VALUES
    ('0C020000-0000-0000-0002-000000000003', '00000000-0000-0000-0000-000000000C02', '1100', 'Renter tilgodehavende', 'DEBIT',  22.94, '2026-01-31', '2026-01-31', 'TOLD-C02-INT-2026-01', 'Inddrivelsesrente januar 2026 @ 2.75%', 'INTEREST_ACCRUAL', 'FORDRINGSHAVER'),
    ('0C020000-0000-0000-0002-000000000003', '00000000-0000-0000-0000-000000000C02', '3100', 'Renteindtaegter',       'CREDIT', 22.94, '2026-01-31', '2026-01-31', 'TOLD-C02-INT-2026-01', 'Renteindtægt januar 2026',              'INTEREST_ACCRUAL', 'FORDRINGSHAVER')
ON CONFLICT DO NOTHING;

-- ─── C02 INTEREST — Februar 2026 (20.72) ─────────────────────────────────────
INSERT INTO ledger_entries (transaction_id, debt_id, account_code, account_name, entry_type, amount, effective_date, posting_date, reference, description, entry_category, accounting_target)
VALUES
    ('0C020000-0000-0000-0002-000000000004', '00000000-0000-0000-0000-000000000C02', '1100', 'Renter tilgodehavende', 'DEBIT',  20.72, '2026-02-28', '2026-02-28', 'TOLD-C02-INT-2026-02', 'Inddrivelsesrente februar 2026 @ 2.75%', 'INTEREST_ACCRUAL', 'FORDRINGSHAVER'),
    ('0C020000-0000-0000-0002-000000000004', '00000000-0000-0000-0000-000000000C02', '3100', 'Renteindtaegter',       'CREDIT', 20.72, '2026-02-28', '2026-02-28', 'TOLD-C02-INT-2026-02', 'Renteindtægt februar 2026',               'INTEREST_ACCRUAL', 'FORDRINGSHAVER')
ON CONFLICT DO NOTHING;

-- ─── D01 DEBT REGISTRATION (24750) ───────────────────────────────────────────
INSERT INTO ledger_entries (transaction_id, debt_id, account_code, account_name, entry_type, amount, effective_date, posting_date, reference, description, entry_category, accounting_target)
VALUES
    ('0D010000-0000-0000-0003-000000000001', '00000000-0000-0000-0000-000000000D01', '1000', 'Fordringer',            'DEBIT',  24750.00, '2025-03-01', '2025-03-01', 'SU-D01-2025-REG', 'SU-gæld registreret — Gældsinddrivelsesloven § 5', 'DEBT_REGISTRATION', 'FORDRINGSHAVER'),
    ('0D010000-0000-0000-0003-000000000001', '00000000-0000-0000-0000-000000000D01', '3000', 'Indrivelsesindtaegter', 'CREDIT', 24750.00, '2025-03-01', '2025-03-01', 'SU-D01-2025-REG', 'SU-gæld modtaget til inddrivelse',                 'DEBT_REGISTRATION', 'FORDRINGSHAVER')
ON CONFLICT DO NOTHING;

-- ─── D01 PAYMENT 5000 on 2025-11-01 (coverage: 1003.59 interest + 3996.41 principal) ─
INSERT INTO ledger_entries (transaction_id, debt_id, account_code, account_name, entry_type, amount, effective_date, posting_date, reference, description, entry_category, accounting_target)
VALUES
    ('0D010000-0000-0000-0003-000000000002', '00000000-0000-0000-0000-000000000D01', '2000', 'SKB Bankkonto',         'DEBIT',  5000.00,  '2025-11-01', '2025-11-01', 'SU-D01-PAY-2025-11', 'Delvis betaling modtaget via NemKonto',                 'PAYMENT', 'FORDRINGSHAVER'),
    ('0D010000-0000-0000-0003-000000000002', '00000000-0000-0000-0000-000000000D01', '1100', 'Renter tilgodehavende', 'CREDIT', 1003.59,  '2025-11-01', '2025-11-01', 'SU-D01-PAY-2025-11', 'Dækning: renter op til 2025-11-01 (dækningsrækkefølge 1/3)', 'PAYMENT', 'FORDRINGSHAVER'),
    ('0D010000-0000-0000-0003-000000000002', '00000000-0000-0000-0000-000000000D01', '1000', 'Fordringer',            'CREDIT', 3996.41,  '2025-11-01', '2025-11-01', 'SU-D01-PAY-2025-11', 'Dækning: hovedstol (dækningsrækkefølge 3/3)',                'PAYMENT', 'FORDRINGSHAVER')
ON CONFLICT DO NOTHING;

-- ─── D01 INTEREST — Januar 2026 (101.36) ─────────────────────────────────────
INSERT INTO ledger_entries (transaction_id, debt_id, account_code, account_name, entry_type, amount, effective_date, posting_date, reference, description, entry_category, accounting_target)
VALUES
    ('0D010000-0000-0000-0003-000000000003', '00000000-0000-0000-0000-000000000D01', '1100', 'Renter tilgodehavende', 'DEBIT',  101.36, '2026-01-31', '2026-01-31', 'SU-D01-INT-2026-01', 'Inddrivelsesrente januar 2026 @ 5.75%', 'INTEREST_ACCRUAL', 'FORDRINGSHAVER'),
    ('0D010000-0000-0000-0003-000000000003', '00000000-0000-0000-0000-000000000D01', '3100', 'Renteindtaegter',       'CREDIT', 101.36, '2026-01-31', '2026-01-31', 'SU-D01-INT-2026-01', 'Renteindtægt januar 2026',              'INTEREST_ACCRUAL', 'FORDRINGSHAVER')
ON CONFLICT DO NOTHING;

-- ─── D01 INTEREST — Februar 2026 (91.57) ─────────────────────────────────────
INSERT INTO ledger_entries (transaction_id, debt_id, account_code, account_name, entry_type, amount, effective_date, posting_date, reference, description, entry_category, accounting_target)
VALUES
    ('0D010000-0000-0000-0003-000000000004', '00000000-0000-0000-0000-000000000D01', '1100', 'Renter tilgodehavende', 'DEBIT',  91.57, '2026-02-28', '2026-02-28', 'SU-D01-INT-2026-02', 'Inddrivelsesrente februar 2026 @ 5.75%', 'INTEREST_ACCRUAL', 'FORDRINGSHAVER'),
    ('0D010000-0000-0000-0003-000000000004', '00000000-0000-0000-0000-000000000D01', '3100', 'Renteindtaegter',       'CREDIT', 91.57, '2026-02-28', '2026-02-28', 'SU-D01-INT-2026-02', 'Renteindtægt februar 2026',               'INTEREST_ACCRUAL', 'FORDRINGSHAVER')
ON CONFLICT DO NOTHING;

-- ─── D02 DEBT REGISTRATION (5000, strafferetlig bøde) ────────────────────────
INSERT INTO ledger_entries (transaction_id, debt_id, account_code, account_name, entry_type, amount, effective_date, posting_date, reference, description, entry_category, accounting_target)
VALUES
    ('0D020000-0000-0000-0004-000000000001', '00000000-0000-0000-0000-000000000D02', '1000', 'Fordringer',            'DEBIT',  5000.00, '2025-04-01', '2025-04-01', 'STRAF-D02-2025-REG', 'Strafferetlig bøde registreret — rentefri', 'DEBT_REGISTRATION', 'FORDRINGSHAVER'),
    ('0D020000-0000-0000-0004-000000000001', '00000000-0000-0000-0000-000000000D02', '3000', 'Indrivelsesindtaegter', 'CREDIT', 5000.00, '2025-04-01', '2025-04-01', 'STRAF-D02-2025-REG', 'Bøde modtaget til inddrivelse',             'DEBT_REGISTRATION', 'FORDRINGSHAVER')
ON CONFLICT DO NOTHING;

-- ─── D02 FEE — RYKKER 65 kr (2025-05-01) ─────────────────────────────────────
INSERT INTO ledger_entries (transaction_id, debt_id, account_code, account_name, entry_type, amount, effective_date, posting_date, reference, description, entry_category, accounting_target)
VALUES
    ('0D020000-0000-0000-0004-000000000002', '00000000-0000-0000-0000-000000000D02', '1000', 'Fordringer',            'DEBIT',  65.00, '2025-05-01', '2025-05-01', 'STRAF-D02-FEE-RYKKER', 'Rykkergebyr tilføjet — Opkrævningsloven § 6', 'DEBT_REGISTRATION', 'STATEN'),
    ('0D020000-0000-0000-0004-000000000002', '00000000-0000-0000-0000-000000000D02', '3000', 'Indrivelsesindtaegter', 'CREDIT', 65.00, '2025-05-01', '2025-05-01', 'STRAF-D02-FEE-RYKKER', 'Rykkergebyr — statslig indtægt',              'DEBT_REGISTRATION', 'STATEN')
ON CONFLICT DO NOTHING;

-- ─── E01 DEBT REGISTRATION (18400, underholdsbidrag) ─────────────────────────
INSERT INTO ledger_entries (transaction_id, debt_id, account_code, account_name, entry_type, amount, effective_date, posting_date, reference, description, entry_category, accounting_target)
VALUES
    ('0E010000-0000-0000-0005-000000000001', '00000000-0000-0000-0000-000000000E01', '1000', 'Fordringer',            'DEBIT',  18400.00, '2025-07-01', '2025-07-01', 'BIDR-E01-2025-REG', 'Underholdsbidrag registreret — Gældsinddrivelsesloven § 5', 'DEBT_REGISTRATION', 'FORDRINGSHAVER'),
    ('0E010000-0000-0000-0005-000000000001', '00000000-0000-0000-0000-000000000E01', '3000', 'Indrivelsesindtaegter', 'CREDIT', 18400.00, '2025-07-01', '2025-07-01', 'BIDR-E01-2025-REG', 'Underholdsbidrag modtaget til inddrivelse',                 'DEBT_REGISTRATION', 'FORDRINGSHAVER')
ON CONFLICT DO NOTHING;

-- ─── E01 FEE — LOENINDEHOLDELSE 100 kr (2026-01-15) ─────────────────────────
INSERT INTO ledger_entries (transaction_id, debt_id, account_code, account_name, entry_type, amount, effective_date, posting_date, reference, description, entry_category, accounting_target)
VALUES
    ('0E010000-0000-0000-0005-000000000004', '00000000-0000-0000-0000-000000000E01', '1000', 'Fordringer',            'DEBIT',  100.00, '2026-01-15', '2026-01-15', 'BIDR-E01-FEE-LOENI', 'Lønindeholdelsesgebyr — Gældsinddrivelsesloven § 10', 'DEBT_REGISTRATION', 'STATEN'),
    ('0E010000-0000-0000-0005-000000000004', '00000000-0000-0000-0000-000000000E01', '3000', 'Indrivelsesindtaegter', 'CREDIT', 100.00, '2026-01-15', '2026-01-15', 'BIDR-E01-FEE-LOENI', 'Lønindeholdelsesgebyr — statslig indtægt',                    'DEBT_REGISTRATION', 'STATEN')
ON CONFLICT DO NOTHING;

-- ─── E01 INTEREST — Januar 2026 (87.69) ──────────────────────────────────────
INSERT INTO ledger_entries (transaction_id, debt_id, account_code, account_name, entry_type, amount, effective_date, posting_date, reference, description, entry_category, accounting_target)
VALUES
    ('0E010000-0000-0000-0005-000000000002', '00000000-0000-0000-0000-000000000E01', '1100', 'Renter tilgodehavende', 'DEBIT',  87.69, '2026-01-31', '2026-01-31', 'BIDR-E01-INT-2026-01', 'Inddrivelsesrente januar 2026 @ 5.75%', 'INTEREST_ACCRUAL', 'FORDRINGSHAVER'),
    ('0E010000-0000-0000-0005-000000000002', '00000000-0000-0000-0000-000000000E01', '3100', 'Renteindtaegter',       'CREDIT', 87.69, '2026-01-31', '2026-01-31', 'BIDR-E01-INT-2026-01', 'Renteindtægt januar 2026',              'INTEREST_ACCRUAL', 'FORDRINGSHAVER')
ON CONFLICT DO NOTHING;

-- ─── E01 INTEREST — Februar 2026 (79.21) ─────────────────────────────────────
INSERT INTO ledger_entries (transaction_id, debt_id, account_code, account_name, entry_type, amount, effective_date, posting_date, reference, description, entry_category, accounting_target)
VALUES
    ('0E010000-0000-0000-0005-000000000003', '00000000-0000-0000-0000-000000000E01', '1100', 'Renter tilgodehavende', 'DEBIT',  79.21, '2026-02-28', '2026-02-28', 'BIDR-E01-INT-2026-02', 'Inddrivelsesrente februar 2026 @ 5.75%', 'INTEREST_ACCRUAL', 'FORDRINGSHAVER'),
    ('0E010000-0000-0000-0005-000000000003', '00000000-0000-0000-0000-000000000E01', '3100', 'Renteindtaegter',       'CREDIT', 79.21, '2026-02-28', '2026-02-28', 'BIDR-E01-INT-2026-02', 'Renteindtægt februar 2026',               'INTEREST_ACCRUAL', 'FORDRINGSHAVER')
ON CONFLICT DO NOTHING;

-- ─── E02 DEBT REGISTRATION (3200, dagbøde) ───────────────────────────────────
INSERT INTO ledger_entries (transaction_id, debt_id, account_code, account_name, entry_type, amount, effective_date, posting_date, reference, description, entry_category, accounting_target)
VALUES
    ('0E020000-0000-0000-0006-000000000001', '00000000-0000-0000-0000-000000000E02', '1000', 'Fordringer',            'DEBIT',  3200.00, '2025-09-15', '2025-09-15', 'DAGB-E02-2025-REG', 'Dagbøde registreret — administrativ afgørelse', 'DEBT_REGISTRATION', 'FORDRINGSHAVER'),
    ('0E020000-0000-0000-0006-000000000001', '00000000-0000-0000-0000-000000000E02', '3000', 'Indrivelsesindtaegter', 'CREDIT', 3200.00, '2025-09-15', '2025-09-15', 'DAGB-E02-2025-REG', 'Dagbøde modtaget til inddrivelse',               'DEBT_REGISTRATION', 'FORDRINGSHAVER')
ON CONFLICT DO NOTHING;

-- ─── E02 INTEREST — Januar 2026 (15.25) ──────────────────────────────────────
INSERT INTO ledger_entries (transaction_id, debt_id, account_code, account_name, entry_type, amount, effective_date, posting_date, reference, description, entry_category, accounting_target)
VALUES
    ('0E020000-0000-0000-0006-000000000002', '00000000-0000-0000-0000-000000000E02', '1100', 'Renter tilgodehavende', 'DEBIT',  15.25, '2026-01-31', '2026-01-31', 'DAGB-E02-INT-2026-01', 'Inddrivelsesrente januar 2026 @ 5.75%', 'INTEREST_ACCRUAL', 'FORDRINGSHAVER'),
    ('0E020000-0000-0000-0006-000000000002', '00000000-0000-0000-0000-000000000E02', '3100', 'Renteindtaegter',       'CREDIT', 15.25, '2026-01-31', '2026-01-31', 'DAGB-E02-INT-2026-01', 'Renteindtægt januar 2026',              'INTEREST_ACCRUAL', 'FORDRINGSHAVER')
ON CONFLICT DO NOTHING;

-- ─── E02 INTEREST — Februar 2026 (13.77) ─────────────────────────────────────
INSERT INTO ledger_entries (transaction_id, debt_id, account_code, account_name, entry_type, amount, effective_date, posting_date, reference, description, entry_category, accounting_target)
VALUES
    ('0E020000-0000-0000-0006-000000000003', '00000000-0000-0000-0000-000000000E02', '1100', 'Renter tilgodehavende', 'DEBIT',  13.77, '2026-02-28', '2026-02-28', 'DAGB-E02-INT-2026-02', 'Inddrivelsesrente februar 2026 @ 5.75%', 'INTEREST_ACCRUAL', 'FORDRINGSHAVER'),
    ('0E020000-0000-0000-0006-000000000003', '00000000-0000-0000-0000-000000000E02', '3100', 'Renteindtaegter',       'CREDIT', 13.77, '2026-02-28', '2026-02-28', 'DAGB-E02-INT-2026-02', 'Renteindtægt februar 2026',               'INTEREST_ACCRUAL', 'FORDRINGSHAVER')
ON CONFLICT DO NOTHING;
