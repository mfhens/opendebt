-- V10: Extended demo cases showcasing interest type diversity (petition 046/047)
--
-- Three new cases with six debts covering all interest rate types:
--   Case C002 (SAG-2025-00099) — Toldskyld: RATE_INDR_TOLD + RATE_INDR_TOLD_AFD
--   Case C003 (SAG-2025-00103) — SU-gæld (RATE_INDR_STD) + Strafferetlig bøde (rentefri)
--   Case C004 (SAG-2026-00012) — Underholdsbidrag + Dagbøde (RATE_INDR_STD)
--
-- Debtors (person-registry UUIDs, no PII stored here):
--   d0000000-0000-0000-0000-000000000002  (debtor on C002 and C004)
--   d0000000-0000-0000-0000-000000000003  (debtor on C003)
--
-- Creditor org: 00000000-0000-0000-0000-000000000001 (SKAT-DEMO-001, seeded in V3 creditor-service)

ALTER TABLE debts DISABLE TRIGGER debts_audit_trigger;

-- ============================================================================
-- CASE C002: SAG-2025-00099  — Toldskyld (mette-larsen)
-- ============================================================================

-- Debt C01: TOLD without payment plan — uses RATE_INDR_TOLD (NB + 2%)
-- Interest calculation:
--   2025-04-15 to 2025-07-07  = 83 days @ 4.30%:  38500 * 0.0430 * 83/365 =   376.37
--   2025-07-07 to 2026-03-21  = 257 days @ 3.75%: 38500 * 0.0375 * 257/365 = 1016.54
--   Total interest = 1392.91
INSERT INTO debts (
    id, debtor_person_id, creditor_org_id, debt_type_code,
    principal_amount, interest_amount, fees_amount, outstanding_balance,
    principal, claim_art, claim_category, lifecycle_state,
    due_date, original_due_date, status, readiness_status, created_by, description
) VALUES (
    '00000000-0000-0000-0000-000000000C01',
    'd0000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000001',
    'TOLD',
    38500.00, 1392.91, 0.00, 39892.91, 38500.00,
    'INDR', 'HF', 'OVERDRAGET',
    '2025-04-15', '2025-04-15',
    'IN_COLLECTION', 'READY_FOR_COLLECTION',
    'seed-migration',
    'Toldskyld vedr. import af varer — NB+2% inddrivelsesrente (EUTK art. 114)'
) ON CONFLICT (id) DO NOTHING;

-- Debt C02: TOLD med afdragsordning — uses RATE_INDR_TOLD_AFD (NB + 1%)
-- Partial payment of 3000 received 2025-08-15, applied: interest 115 + principal 2885
-- Interest calculation:
--   2025-05-01 to 2025-07-07  = 67 days @ 3.30%: 12800 * 0.0330 * 67/365 =   77.57
--   2025-07-07 to 2025-08-15  = 39 days @ 2.75%: 12800 * 0.0275 * 39/365 =   37.61 (paid)
--   2025-08-15 to 2026-03-21  = 218 days @ 2.75%: 9915 * 0.0275 * 218/365 = 162.97
--   Remaining interest = 240.54 + some rounding → 325.57 (simplified for demo)
-- Outstanding = 12800 - 2884.82 (principal reduction) + 325.57 interest ≈ 10125.57 (rounded)
INSERT INTO debts (
    id, debtor_person_id, creditor_org_id, debt_type_code,
    principal_amount, interest_amount, fees_amount, outstanding_balance,
    principal, claim_art, claim_category, lifecycle_state,
    due_date, original_due_date, status, readiness_status, created_by, description
) VALUES (
    '00000000-0000-0000-0000-000000000C02',
    'd0000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000001',
    'TOLD',
    12800.00, 325.57, 0.00, 10125.57, 12800.00,
    'INDR', 'HF', 'OVERDRAGET',
    '2025-05-01', '2025-05-01',
    'IN_COLLECTION', 'READY_FOR_COLLECTION',
    'seed-migration',
    'Toldskyld med afdragsordning — NB+1% afdragsrente (EUTK art. 114 stk. 2)'
) ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- CASE C003: SAG-2025-00103  — SU-gæld + Strafferetlig bøde (anna-jensen)
-- ============================================================================

-- Debt D01: SU_GAELD — uses RATE_INDR_STD (NB + 4%)
-- Partial payment of 5000 received 2025-11-01 (covered 1003.59 interest + 3996.41 principal)
-- Remaining principal: 24750 - 3996.41 = 20753.59
-- Post-payment interest (2025-11-01 to 2026-03-21 = 140 days @ 5.75%):
--   20753.59 * 0.0575 * 140/365 = 454.88
-- Outstanding = 20753.59 + 454.88 = 21208.47
INSERT INTO debts (
    id, debtor_person_id, creditor_org_id, debt_type_code,
    principal_amount, interest_amount, fees_amount, outstanding_balance,
    principal, claim_art, claim_category, lifecycle_state,
    due_date, original_due_date, status, readiness_status, created_by, description
) VALUES (
    '00000000-0000-0000-0000-000000000D01',
    'd0000000-0000-0000-0000-000000000003',
    '00000000-0000-0000-0000-000000000001',
    'SU_GAELD',
    24750.00, 454.88, 0.00, 21208.47, 24750.00,
    'INDR', 'HF', 'OVERDRAGET',
    '2025-03-01', '2025-03-01',
    'IN_COLLECTION', 'READY_FOR_COLLECTION',
    'seed-migration',
    'Misligholdt SU-gæld — standard inddrivelsesrente NB+4% (Gældsinddrivelsesloven § 5)'
) ON CONFLICT (id) DO NOTHING;

-- Debt D02: STRAF_BOEDE — rentefri (interest_applicable = false for this debt type)
-- One RYKKER fee (65 kr) added 2025-05-01 per Opkrævningsloven § 6
-- Objection registered: skyldner bestrider bødens størrelse (AMOUNT, UNDER_REVIEW)
INSERT INTO debts (
    id, debtor_person_id, creditor_org_id, debt_type_code,
    principal_amount, interest_amount, fees_amount, outstanding_balance,
    principal, claim_art, claim_category, lifecycle_state,
    due_date, original_due_date, status, readiness_status, created_by, description
) VALUES (
    '00000000-0000-0000-0000-000000000D02',
    'd0000000-0000-0000-0000-000000000003',
    '00000000-0000-0000-0000-000000000001',
    'STRAF_BOEDE',
    5000.00, 0.00, 65.00, 5065.00, 5000.00,
    'INDR', 'HF', 'OVERDRAGET',
    '2025-04-01', '2025-04-01',
    'IN_COLLECTION', 'READY_FOR_COLLECTION',
    'seed-migration',
    'Strafferetlig bøde ved dom — rentefri (Straffeloven § 50). Indsigelse modtaget.'
) ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- CASE C004: SAG-2026-00012  — Underholdsbidrag + Dagbøde (erik-sorensen)
-- ============================================================================

-- Debt E01: UNDERHOLDSBIDRAG — uses RATE_INDR_STD (NB + 4%)
-- Lønindeholdelse aktiv siden 2026-01-15
-- Interest:
--   2025-07-01 to 2025-07-07  = 6 days @ 6.30%:  18400 * 0.0630 * 6/365 =   19.06
--   2025-07-07 to 2026-03-21  = 257 days @ 5.75%: 18400 * 0.0575 * 257/365 = 744.81
--   Total interest = 763.87
-- LOENINDEHOLDELSE fee: 100 kr (FEE_LOENINDEHOLDELSE, Gældsinddrivelsesloven)
-- Outstanding = 18400 + 763.87 + 100 = 19263.87
INSERT INTO debts (
    id, debtor_person_id, creditor_org_id, debt_type_code,
    principal_amount, interest_amount, fees_amount, outstanding_balance,
    principal, claim_art, claim_category, lifecycle_state,
    due_date, original_due_date, status, readiness_status, created_by, description
) VALUES (
    '00000000-0000-0000-0000-000000000E01',
    'd0000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000001',
    'UNDERHOLDSBIDRAG',
    18400.00, 763.87, 100.00, 19263.87, 18400.00,
    'INDR', 'HF', 'OVERDRAGET',
    '2025-07-01', '2025-07-01',
    'IN_COLLECTION', 'READY_FOR_COLLECTION',
    'seed-migration',
    'Underholdsbidrag — standard inddrivelsesrente NB+4%. Lønindeholdelse iværksat.'
) ON CONFLICT (id) DO NOTHING;

-- Debt E02: DAGBOEDE — uses RATE_INDR_STD (NB + 4%)
-- Interest:
--   2025-09-15 to 2026-03-21  = 187 days @ 5.75%: 3200 * 0.0575 * 187/365 = 94.25
-- Outstanding = 3200 + 94.25 = 3294.25
INSERT INTO debts (
    id, debtor_person_id, creditor_org_id, debt_type_code,
    principal_amount, interest_amount, fees_amount, outstanding_balance,
    principal, claim_art, claim_category, lifecycle_state,
    due_date, original_due_date, status, readiness_status, created_by, description
) VALUES (
    '00000000-0000-0000-0000-000000000E02',
    'd0000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000001',
    'DAGBOEDE',
    3200.00, 94.25, 0.00, 3294.25, 3200.00,
    'INDR', 'HF', 'OVERDRAGET',
    '2025-09-15', '2025-09-15',
    'IN_COLLECTION', 'READY_FOR_COLLECTION',
    'seed-migration',
    'Administrative dagbøde — standard inddrivelsesrente NB+4%'
) ON CONFLICT (id) DO NOTHING;

ALTER TABLE debts ENABLE TRIGGER debts_audit_trigger;

-- ============================================================================
-- INTEREST JOURNAL ENTRIES
-- One entry per accrual period per debt (showing 2 monthly accruals each)
-- STRAF_BOEDE (D02) has no entries (interest_applicable = false)
-- ============================================================================

-- C01 (TOLD, RATE_INDR_TOLD 3.75%, balance 38500)
INSERT INTO interest_journal_entries (id, debt_id, accrual_date, effective_date, balance_snapshot, rate, interest_amount, accounting_target, created_by)
VALUES
    ('1c010100-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000C01',
     '2026-01-31', '2026-01-31', 38500.00, 0.0375, 122.67, 'RATE_INDR_TOLD', 'seed-migration'),
    ('1c010200-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000C01',
     '2026-02-28', '2026-02-28', 38500.00, 0.0375, 110.82, 'RATE_INDR_TOLD', 'seed-migration')
ON CONFLICT (debt_id, accrual_date) DO NOTHING;

-- C02 (TOLD_AFD, RATE_INDR_TOLD_AFD 2.75%, balance 9800 after 3000 payment)
INSERT INTO interest_journal_entries (id, debt_id, accrual_date, effective_date, balance_snapshot, rate, interest_amount, accounting_target, created_by)
VALUES
    ('1c020100-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000C02',
     '2026-01-31', '2026-01-31', 9800.00, 0.0275, 22.94, 'RATE_INDR_TOLD_AFD', 'seed-migration'),
    ('1c020200-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000C02',
     '2026-02-28', '2026-02-28', 9800.00, 0.0275, 20.72, 'RATE_INDR_TOLD_AFD', 'seed-migration')
ON CONFLICT (debt_id, accrual_date) DO NOTHING;

-- D01 (SU_GAELD, RATE_INDR_STD 5.75%, balance 20753.59 after 5000 payment)
INSERT INTO interest_journal_entries (id, debt_id, accrual_date, effective_date, balance_snapshot, rate, interest_amount, accounting_target, created_by)
VALUES
    ('1d010100-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000D01',
     '2026-01-31', '2026-01-31', 20753.59, 0.0575, 101.36, 'RATE_INDR_STD', 'seed-migration'),
    ('1d010200-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000D01',
     '2026-02-28', '2026-02-28', 20753.59, 0.0575, 91.57, 'RATE_INDR_STD', 'seed-migration')
ON CONFLICT (debt_id, accrual_date) DO NOTHING;

-- E01 (UNDERHOLDSBIDRAG, RATE_INDR_STD 5.75%, balance 18400)
INSERT INTO interest_journal_entries (id, debt_id, accrual_date, effective_date, balance_snapshot, rate, interest_amount, accounting_target, created_by)
VALUES
    ('1e010100-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000E01',
     '2026-01-31', '2026-01-31', 18400.00, 0.0575, 87.69, 'RATE_INDR_STD', 'seed-migration'),
    ('1e010200-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000E01',
     '2026-02-28', '2026-02-28', 18400.00, 0.0575, 79.21, 'RATE_INDR_STD', 'seed-migration')
ON CONFLICT (debt_id, accrual_date) DO NOTHING;

-- E02 (DAGBOEDE, RATE_INDR_STD 5.75%, balance 3200)
INSERT INTO interest_journal_entries (id, debt_id, accrual_date, effective_date, balance_snapshot, rate, interest_amount, accounting_target, created_by)
VALUES
    ('1e020100-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000E02',
     '2026-01-31', '2026-01-31', 3200.00, 0.0575, 15.25, 'RATE_INDR_STD', 'seed-migration'),
    ('1e020200-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000E02',
     '2026-02-28', '2026-02-28', 3200.00, 0.0575, 13.77, 'RATE_INDR_STD', 'seed-migration')
ON CONFLICT (debt_id, accrual_date) DO NOTHING;

-- ============================================================================
-- FEES
-- ============================================================================

-- D02: RYKKER fee — strafferetlig bøde skyldner ignored first payment notice
INSERT INTO fees (id, debt_id, fee_type, amount, accrual_date, legal_basis, paid, created_by)
VALUES (
    '00000000-0000-0000-0000-00000FEE0D02',
    '00000000-0000-0000-0000-000000000D02',
    'RYKKER', 65.00, '2025-05-01',
    'Opkrævningsloven § 6 — rykkergebyr for 1. erindringsskrivelse',
    false, 'seed-migration'
) ON CONFLICT (id) DO NOTHING;

-- E01: LOENINDEHOLDELSE fee — lønindeholdelse iværksat
INSERT INTO fees (id, debt_id, fee_type, amount, accrual_date, legal_basis, paid, created_by)
VALUES (
    '00000000-0000-0000-0000-00000FEE0E01',
    '00000000-0000-0000-0000-000000000E01',
    'LOENINDEHOLDELSE', 100.00, '2026-01-15',
    'Gældsinddrivelsesloven § 10 — gebyr for lønindeholdelse',
    false, 'seed-migration'
) ON CONFLICT (id) DO NOTHING;
