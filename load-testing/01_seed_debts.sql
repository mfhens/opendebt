-- ============================================================
-- Load Test: Seed 1,000,000 fordringer (debts)
-- Target DB : opendebt_debt
-- Runtime   : ~1–3 min depending on disk/hardware
-- ============================================================
-- Prerequisites:
--   Flyway migrations V1–V7 already applied to opendebt_debt.
--
-- Design notes:
--   • All debts get lifecycle_state = 'OVERDRAGET' so the
--     InterestAccrualJob picks them all up immediately.
--   • Debt IDs are deterministic: md5('loadtest-debt-'||i)::uuid
--     — the same formula is used in 02_seed_ledger_entries.sql
--     so the two scripts stay in sync without cross-DB joins.
--   • 6 debt types, 5 principal tiers, 7 outstanding-balance
--     patterns and dates spread over 5 years give a wide spread
--     for realistic batch processing behaviour.
--   • Run 00_cleanup.sql first if you need to re-seed.
-- ============================================================

BEGIN;

SET synchronous_commit = OFF;   -- major throughput boost for bulk inserts
SET work_mem = '256MB';

INSERT INTO debts (
    id,
    debtor_person_id,
    creditor_org_id,
    debt_type_code,
    principal_amount,
    principal,
    interest_amount,
    fees_amount,
    outstanding_balance,
    due_date,
    original_due_date,
    inception_date,
    period_from,
    period_to,
    status,
    readiness_status,
    lifecycle_state,
    claim_art,
    claim_category,
    creditor_reference,
    description,
    created_at,
    updated_at,
    created_by,
    version
)
SELECT
    -- Deterministic UUID — mirrors the formula in 02_seed_ledger_entries.sql
    md5('loadtest-debt-' || i)::uuid                                            AS id,

    -- 200 000 distinct debtors so some have multiple debts (realistic)
    md5('loadtest-debtor-' || ((i - 1) % 200000))::uuid                        AS debtor_person_id,

    -- 10 creditor organisations
    md5('loadtest-creditor-' || ((i - 1) % 10))::uuid                          AS creditor_org_id,

    -- Cycle through all six debt types
    (ARRAY['SKAT','MOMS','BOEDE','UNDERHOLDSBIDRAG','DAGINSTITUTION','EJENDOMSSKAT'])
        [1 + ((i - 1) % 6)]                                                    AS debt_type_code,

    -- principal_amount: five tiers (nano → large)
    CASE ((i - 1) % 5)
        WHEN 0 THEN    500.00 + (((i - 1) / 5 % 901)  *    5.00)  -- micro:   500 – 5 000
        WHEN 1 THEN   5000.00 + (((i - 1) / 5 % 901)  *   50.00)  -- small: 5 000 – 50 000
        WHEN 2 THEN  50000.00 + (((i - 1) / 5 % 401)  *  500.00)  -- medium: 50k – 250k
        WHEN 3 THEN 250000.00 + (((i - 1) / 5 % 501)  *  500.00)  -- large: 250k – 500k
        ELSE          100.00 + (((i - 1) / 5 % 401)  *    1.00)   -- nano:  100 – 500
    END                                                                         AS principal_amount,

    -- principal (PSRM stamdata — same value as at origination)
    CASE ((i - 1) % 5)
        WHEN 0 THEN    500.00 + (((i - 1) / 5 % 901)  *    5.00)
        WHEN 1 THEN   5000.00 + (((i - 1) / 5 % 901)  *   50.00)
        WHEN 2 THEN  50000.00 + (((i - 1) / 5 % 401)  *  500.00)
        WHEN 3 THEN 250000.00 + (((i - 1) / 5 % 501)  *  500.00)
        ELSE          100.00 + (((i - 1) / 5 % 401)  *    1.00)
    END                                                                         AS principal,

    0.00                                                                        AS interest_amount,

    -- fees: 0–2 000 DKK in steps of 25
    ((i - 1) % 81 * 25.00)                                                     AS fees_amount,

    -- outstanding_balance: seven payment-stage patterns (all > 0)
    -- This drives the daily interest calculation in the batch job.
    CASE ((i - 1) % 7)
        WHEN 0 THEN    500.00 + (((i - 1) / 7 % 901)  *    5.00)  -- effectively unpaid
        WHEN 1 THEN    100.00 + (((i - 1) / 7 % 200)  *    5.00)  -- heavily reduced
        WHEN 2 THEN   1000.00 + (((i - 1) / 7 % 601)  *   50.00)  -- medium remaining
        WHEN 3 THEN    200.00 + (((i - 1) / 7 % 401)  *    5.00)  -- low remaining
        WHEN 4 THEN   5000.00 + (((i - 1) / 7 % 801)  *  100.00)  -- large remaining
        WHEN 5 THEN     50.00 + (((i - 1) / 7 % 451)  *    1.00)  -- near-paid (50–500)
        ELSE          10000.00 + (((i - 1) / 7 % 501)  *  200.00)  -- very large
    END                                                                         AS outstanding_balance,

    -- due_date: spread 30 days to 5 years in the past
    CURRENT_DATE - (30 + ((i - 1) % 1796))::int                                AS due_date,
    CURRENT_DATE - (30 + ((i - 1) % 1796))::int                                AS original_due_date,
    CURRENT_DATE - (60 + ((i - 1) % 1800))::int                                AS inception_date,

    -- period_from/to: last 36 calendar months (full month boundaries)
    DATE_TRUNC('month',
        CURRENT_DATE - (((i - 1) % 36) || ' months')::interval
    )::date                                                                     AS period_from,
    (DATE_TRUNC('month',
        CURRENT_DATE - (((i - 1) % 36) || ' months')::interval
    ) + INTERVAL '1 month - 1 day')::date                                      AS period_to,

    -- status: 2/3 IN_COLLECTION, 1/3 PARTIALLY_PAID (both valid for OVERDRAGET)
    CASE ((i - 1) % 3)
        WHEN 0 THEN 'IN_COLLECTION'
        WHEN 1 THEN 'PARTIALLY_PAID'
        ELSE        'IN_COLLECTION'
    END                                                                         AS status,

    'READY_FOR_COLLECTION'                                                      AS readiness_status,
    'OVERDRAGET'                                                                AS lifecycle_state,
    'INDR'                                                                      AS claim_art,
    'HF'                                                                        AS claim_category,

    'LT-' || LPAD(i::text, 10, '0')                                            AS creditor_reference,

    'Load test: ' || (ARRAY['Skat','Moms','Bøde','Underholdsbidrag','Daginstitution','Ejendomsskat'])
        [1 + ((i - 1) % 6)]                                                    AS description,

    -- created_at: spread over past 3 years
    NOW() - (((i - 1) % 1095 || ' days')::interval)                            AS created_at,
    NOW() - (((i - 1) % 365  || ' days')::interval)                            AS updated_at,
    'load-test-seeder'                                                          AS created_by,
    0                                                                           AS version

FROM generate_series(1, 1000000) AS s(i);

COMMIT;

-- ── Verification ─────────────────────────────────────────────
\echo 'Debt verification:'
SELECT
    COUNT(*)                                                        AS total_seeded,
    COUNT(*) FILTER (WHERE lifecycle_state = 'OVERDRAGET')         AS eligible_for_interest,
    COUNT(DISTINCT debt_type_code)                                  AS distinct_debt_types,
    COUNT(DISTINCT debtor_person_id)                                AS distinct_debtors,
    COUNT(DISTINCT creditor_org_id)                                 AS distinct_creditors,
    MIN(outstanding_balance)                                        AS min_balance,
    ROUND(AVG(outstanding_balance), 2)                             AS avg_balance,
    MAX(outstanding_balance)                                        AS max_balance,
    TO_CHAR(SUM(outstanding_balance), 'FM999,999,999,999.00')      AS total_outstanding_dkk
FROM debts
WHERE creditor_reference LIKE 'LT-%';
