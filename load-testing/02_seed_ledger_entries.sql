-- ============================================================
-- Load Test: Seed diverse bogføringsposter (ledger entries)
-- Target DB : opendebt_payment
-- Runtime   : ~3–8 min depending on disk/hardware
-- ============================================================
-- Prerequisites:
--   • 01_seed_debts.sql must have been applied to opendebt_debt.
--   • Flyway migrations already applied to opendebt_payment.
--
-- Design notes:
--   • debt_id values use the SAME deterministic formula as
--     01_seed_debts.sql: md5('loadtest-debt-'||i)::uuid.
--     No FK exists across databases, so this is safe.
--   • Every debt gets a DEBT_REGISTRATION pair.
--   • Additional transactions are layered by modulo to create
--     diverse booking histories:
--
--   Category          Filter          DEBIT/CREDIT pairs    Rows
--   DEBT_REGISTRATION all 1 000 000   1 000 000 pairs     2 000 000
--   PAYMENT           i%2=0             500 000 pairs     1 000 000
--   INTEREST_ACCRUAL  i%3=0             333 333 pairs       666 666
--   STORNO            i%10=0            100 000 pairs       200 000
--   CORRECTION        i%20=0             50 000 pairs       100 000
--                                                     ≈ 3 966 666
--
--   Each pair shares a transaction_id, giving valid double-entry
--   bookkeeping (every economic event has one DEBIT + one CREDIT).
--
--   Storno (i%10=0) reverses the PAYMENT of the same debt,
--   so reversal_of_transaction_id points to a real transaction.
--   i%10=0 ⇒ i%2=0 always, so the reversed transaction exists.
-- ============================================================

BEGIN;

SET synchronous_commit = OFF;
SET work_mem = '256MB';

-- ── Helper: principal amount matching 01_seed_debts.sql tiers ──
-- Used in several INSERTs below to keep amounts consistent.
-- Formula: tier = (i-1)%5
--   tier 0:  500 +  (((i-1)/5 % 901) *   5)   →    500 –   5 000
--   tier 1: 5000 +  (((i-1)/5 % 901) *  50)   →  5 000 –  50 000
--   tier 2: 50000 + (((i-1)/5 % 401) * 500)   → 50 000 – 250 000
--   tier 3: 250000 +(((i-1)/5 % 501) * 500)   →250 000 – 500 000
--   else:    100 +  (((i-1)/5 % 401) *   1)   →    100 –     500

-- ── 1a. DEBT_REGISTRATION — DEBIT side ───────────────────────
-- Account 1000 Fordringer (DR) / 3000 Indrivelsesindtægter (CR)
\echo 'Inserting DEBT_REGISTRATION DEBIT entries (1 000 000 rows)...'

INSERT INTO ledger_entries (
    id, transaction_id, debt_id,
    account_code, account_name, entry_type, amount,
    effective_date, posting_date,
    reference, description, entry_category, created_at
)
SELECT
    gen_random_uuid(),
    md5('loadtest-txn-reg-' || i)::uuid,
    md5('loadtest-debt-'    || i)::uuid,
    '1000', 'Fordringer', 'DEBIT',
    CASE ((i - 1) % 5)
        WHEN 0 THEN    500.00 + (((i - 1) / 5 % 901) *   5.00)
        WHEN 1 THEN   5000.00 + (((i - 1) / 5 % 901) *  50.00)
        WHEN 2 THEN  50000.00 + (((i - 1) / 5 % 401) * 500.00)
        WHEN 3 THEN 250000.00 + (((i - 1) / 5 % 501) * 500.00)
        ELSE          100.00 + (((i - 1) / 5 % 401) *   1.00)
    END,
    CURRENT_DATE - (60 + ((i - 1) % 1800))::int,
    CURRENT_DATE - (60 + ((i - 1) % 1800))::int,
    'LT-REG-' || LPAD(i::text, 10, '0'),
    'Fordring registreret – load test #' || i,
    'DEBT_REGISTRATION',
    NOW() - ((((i - 1) % 1800) || ' days')::interval)
FROM generate_series(1, 1000000) AS s(i);

-- ── 1b. DEBT_REGISTRATION — CREDIT side ──────────────────────
\echo 'Inserting DEBT_REGISTRATION CREDIT entries (1 000 000 rows)...'

INSERT INTO ledger_entries (
    id, transaction_id, debt_id,
    account_code, account_name, entry_type, amount,
    effective_date, posting_date,
    reference, description, entry_category, created_at
)
SELECT
    gen_random_uuid(),
    md5('loadtest-txn-reg-' || i)::uuid,  -- same transaction_id as DEBIT above
    md5('loadtest-debt-'    || i)::uuid,
    '3000', 'Indrivelsesindtægter', 'CREDIT',
    CASE ((i - 1) % 5)
        WHEN 0 THEN    500.00 + (((i - 1) / 5 % 901) *   5.00)
        WHEN 1 THEN   5000.00 + (((i - 1) / 5 % 901) *  50.00)
        WHEN 2 THEN  50000.00 + (((i - 1) / 5 % 401) * 500.00)
        WHEN 3 THEN 250000.00 + (((i - 1) / 5 % 501) * 500.00)
        ELSE          100.00 + (((i - 1) / 5 % 401) *   1.00)
    END,
    CURRENT_DATE - (60 + ((i - 1) % 1800))::int,
    CURRENT_DATE - (60 + ((i - 1) % 1800))::int,
    'LT-REG-' || LPAD(i::text, 10, '0'),
    'Fordring registreret – load test #' || i,
    'DEBT_REGISTRATION',
    NOW() - ((((i - 1) % 1800) || ' days')::interval)
FROM generate_series(1, 1000000) AS s(i);

-- ── 2a. PAYMENT — DEBIT side (every 2nd debt) ────────────────
-- A partial payment was received; reduces the outstanding balance.
-- Payment amount = 10–60% of principal (multiplier varies by i%11).
\echo 'Inserting PAYMENT DEBIT entries (~500 000 rows)...'

INSERT INTO ledger_entries (
    id, transaction_id, debt_id,
    account_code, account_name, entry_type, amount,
    effective_date, posting_date,
    reference, description, entry_category, created_at
)
SELECT
    gen_random_uuid(),
    md5('loadtest-txn-pay-' || i)::uuid,
    md5('loadtest-debt-'    || i)::uuid,
    '2000', 'SKB Bankkonto', 'DEBIT',
    GREATEST(1.00, ROUND(
        CASE ((i - 1) % 5)
            WHEN 0 THEN    500.00 + (((i - 1) / 5 % 901) *   5.00)
            WHEN 1 THEN   5000.00 + (((i - 1) / 5 % 901) *  50.00)
            WHEN 2 THEN  50000.00 + (((i - 1) / 5 % 401) * 500.00)
            WHEN 3 THEN 250000.00 + (((i - 1) / 5 % 501) * 500.00)
            ELSE          100.00 + (((i - 1) / 5 % 401) *   1.00)
        END * (0.10 + ((i - 1) % 11 * 0.05)),
    2)),
    CURRENT_DATE - (10 + ((i - 1) % 400))::int,
    CURRENT_DATE - (10 + ((i - 1) % 400))::int,
    'LT-PAY-' || LPAD(i::text, 10, '0'),
    'Betaling modtaget – load test #' || i,
    'PAYMENT',
    NOW() - ((((i - 1) % 400) || ' days')::interval)
FROM generate_series(1, 1000000) AS s(i)
WHERE i % 2 = 0;

-- ── 2b. PAYMENT — CREDIT side ────────────────────────────────
\echo 'Inserting PAYMENT CREDIT entries (~500 000 rows)...'

INSERT INTO ledger_entries (
    id, transaction_id, debt_id,
    account_code, account_name, entry_type, amount,
    effective_date, posting_date,
    reference, description, entry_category, created_at
)
SELECT
    gen_random_uuid(),
    md5('loadtest-txn-pay-' || i)::uuid,
    md5('loadtest-debt-'    || i)::uuid,
    '1000', 'Fordringer', 'CREDIT',
    GREATEST(1.00, ROUND(
        CASE ((i - 1) % 5)
            WHEN 0 THEN    500.00 + (((i - 1) / 5 % 901) *   5.00)
            WHEN 1 THEN   5000.00 + (((i - 1) / 5 % 901) *  50.00)
            WHEN 2 THEN  50000.00 + (((i - 1) / 5 % 401) * 500.00)
            WHEN 3 THEN 250000.00 + (((i - 1) / 5 % 501) * 500.00)
            ELSE          100.00 + (((i - 1) / 5 % 401) *   1.00)
        END * (0.10 + ((i - 1) % 11 * 0.05)),
    2)),
    CURRENT_DATE - (10 + ((i - 1) % 400))::int,
    CURRENT_DATE - (10 + ((i - 1) % 400))::int,
    'LT-PAY-' || LPAD(i::text, 10, '0'),
    'Betaling modtaget – load test #' || i,
    'PAYMENT',
    NOW() - ((((i - 1) % 400) || ' days')::interval)
FROM generate_series(1, 1000000) AS s(i)
WHERE i % 2 = 0;

-- ── 3a. INTEREST_ACCRUAL — DEBIT side (every 3rd debt) ───────
-- Historic interest accrual already posted before the batch test.
-- Amount = principal × 5.75% / 365 × (30–180 days elapsed).
\echo 'Inserting INTEREST_ACCRUAL DEBIT entries (~333 333 rows)...'

INSERT INTO ledger_entries (
    id, transaction_id, debt_id,
    account_code, account_name, entry_type, amount,
    effective_date, posting_date,
    reference, description, entry_category, created_at
)
SELECT
    gen_random_uuid(),
    md5('loadtest-txn-int-' || i)::uuid,
    md5('loadtest-debt-'    || i)::uuid,
    '1100', 'Renter tilgodehavende', 'DEBIT',
    GREATEST(1.00, ROUND(
        CASE ((i - 1) % 5)
            WHEN 0 THEN    500.00 + (((i - 1) / 5 % 901) *   5.00)
            WHEN 1 THEN   5000.00 + (((i - 1) / 5 % 901) *  50.00)
            WHEN 2 THEN  50000.00 + (((i - 1) / 5 % 401) * 500.00)
            WHEN 3 THEN 250000.00 + (((i - 1) / 5 % 501) * 500.00)
            ELSE          100.00 + (((i - 1) / 5 % 401) *   1.00)
        END * 0.0575 / 365.0 * (30 + ((i - 1) % 151)),
    2)),
    CURRENT_DATE - (5 + ((i - 1) % 200))::int,
    CURRENT_DATE - (5 + ((i - 1) % 200))::int,
    'LT-INT-' || LPAD(i::text, 10, '0'),
    'Renteoptjening – load test #' || i,
    'INTEREST_ACCRUAL',
    NOW() - ((((i - 1) % 200) || ' days')::interval)
FROM generate_series(1, 1000000) AS s(i)
WHERE i % 3 = 0;

-- ── 3b. INTEREST_ACCRUAL — CREDIT side ───────────────────────
\echo 'Inserting INTEREST_ACCRUAL CREDIT entries (~333 333 rows)...'

INSERT INTO ledger_entries (
    id, transaction_id, debt_id,
    account_code, account_name, entry_type, amount,
    effective_date, posting_date,
    reference, description, entry_category, created_at
)
SELECT
    gen_random_uuid(),
    md5('loadtest-txn-int-' || i)::uuid,
    md5('loadtest-debt-'    || i)::uuid,
    '3100', 'Renteindtægter', 'CREDIT',
    GREATEST(1.00, ROUND(
        CASE ((i - 1) % 5)
            WHEN 0 THEN    500.00 + (((i - 1) / 5 % 901) *   5.00)
            WHEN 1 THEN   5000.00 + (((i - 1) / 5 % 901) *  50.00)
            WHEN 2 THEN  50000.00 + (((i - 1) / 5 % 401) * 500.00)
            WHEN 3 THEN 250000.00 + (((i - 1) / 5 % 501) * 500.00)
            ELSE          100.00 + (((i - 1) / 5 % 401) *   1.00)
        END * 0.0575 / 365.0 * (30 + ((i - 1) % 151)),
    2)),
    CURRENT_DATE - (5 + ((i - 1) % 200))::int,
    CURRENT_DATE - (5 + ((i - 1) % 200))::int,
    'LT-INT-' || LPAD(i::text, 10, '0'),
    'Renteoptjening – load test #' || i,
    'INTEREST_ACCRUAL',
    NOW() - ((((i - 1) % 200) || ' days')::interval)
FROM generate_series(1, 1000000) AS s(i)
WHERE i % 3 = 0;

-- ── 4a. STORNO — DEBIT side (every 10th debt) ────────────────
-- Reverses the PAYMENT transaction of the same debt.
-- i%10=0 implies i%2=0, so the PAYMENT transaction always exists.
\echo 'Inserting STORNO DEBIT entries (~100 000 rows)...'

INSERT INTO ledger_entries (
    id, transaction_id, debt_id,
    account_code, account_name, entry_type, amount,
    effective_date, posting_date,
    reference, description,
    reversal_of_transaction_id,
    entry_category, created_at
)
SELECT
    gen_random_uuid(),
    md5('loadtest-txn-sto-' || i)::uuid,
    md5('loadtest-debt-'    || i)::uuid,
    '1000', 'Fordringer', 'DEBIT',
    GREATEST(1.00, ROUND(
        CASE ((i - 1) % 5)
            WHEN 0 THEN    500.00 + (((i - 1) / 5 % 901) *   5.00)
            WHEN 1 THEN   5000.00 + (((i - 1) / 5 % 901) *  50.00)
            WHEN 2 THEN  50000.00 + (((i - 1) / 5 % 401) * 500.00)
            WHEN 3 THEN 250000.00 + (((i - 1) / 5 % 501) * 500.00)
            ELSE          100.00 + (((i - 1) / 5 % 401) *   1.00)
        END * (0.10 + ((i - 1) % 11 * 0.05)),
    2)),
    CURRENT_DATE - (3 + ((i - 1) % 100))::int,
    CURRENT_DATE - (3 + ((i - 1) % 100))::int,
    'LT-STO-' || LPAD(i::text, 10, '0'),
    'Storno af betaling – load test #' || i,
    md5('loadtest-txn-pay-' || i)::uuid,   -- references the reversed PAYMENT
    'STORNO',
    NOW() - ((((i - 1) % 100) || ' days')::interval)
FROM generate_series(1, 1000000) AS s(i)
WHERE i % 10 = 0;

-- ── 4b. STORNO — CREDIT side ─────────────────────────────────
\echo 'Inserting STORNO CREDIT entries (~100 000 rows)...'

INSERT INTO ledger_entries (
    id, transaction_id, debt_id,
    account_code, account_name, entry_type, amount,
    effective_date, posting_date,
    reference, description,
    reversal_of_transaction_id,
    entry_category, created_at
)
SELECT
    gen_random_uuid(),
    md5('loadtest-txn-sto-' || i)::uuid,
    md5('loadtest-debt-'    || i)::uuid,
    '2000', 'SKB Bankkonto', 'CREDIT',
    GREATEST(1.00, ROUND(
        CASE ((i - 1) % 5)
            WHEN 0 THEN    500.00 + (((i - 1) / 5 % 901) *   5.00)
            WHEN 1 THEN   5000.00 + (((i - 1) / 5 % 901) *  50.00)
            WHEN 2 THEN  50000.00 + (((i - 1) / 5 % 401) * 500.00)
            WHEN 3 THEN 250000.00 + (((i - 1) / 5 % 501) * 500.00)
            ELSE          100.00 + (((i - 1) / 5 % 401) *   1.00)
        END * (0.10 + ((i - 1) % 11 * 0.05)),
    2)),
    CURRENT_DATE - (3 + ((i - 1) % 100))::int,
    CURRENT_DATE - (3 + ((i - 1) % 100))::int,
    'LT-STO-' || LPAD(i::text, 10, '0'),
    'Storno af betaling – load test #' || i,
    md5('loadtest-txn-pay-' || i)::uuid,
    'STORNO',
    NOW() - ((((i - 1) % 100) || ' days')::interval)
FROM generate_series(1, 1000000) AS s(i)
WHERE i % 10 = 0;

-- ── 5a. CORRECTION — DEBIT side (every 20th debt) ────────────
\echo 'Inserting CORRECTION DEBIT entries (~50 000 rows)...'

INSERT INTO ledger_entries (
    id, transaction_id, debt_id,
    account_code, account_name, entry_type, amount,
    effective_date, posting_date,
    reference, description, entry_category, created_at
)
SELECT
    gen_random_uuid(),
    md5('loadtest-txn-cor-' || i)::uuid,
    md5('loadtest-debt-'    || i)::uuid,
    '1000', 'Fordringer', 'DEBIT',
    GREATEST(25.00, ROUND((((i - 1) % 200) * 10.00), 2)),
    CURRENT_DATE - ((i - 1) % 30)::int,
    CURRENT_DATE - ((i - 1) % 30)::int,
    'LT-COR-' || LPAD(i::text, 10, '0'),
    'Korrektionspostering – load test #' || i,
    'CORRECTION',
    NOW() - ((((i - 1) % 30) || ' days')::interval)
FROM generate_series(1, 1000000) AS s(i)
WHERE i % 20 = 0;

-- ── 5b. CORRECTION — CREDIT side ─────────────────────────────
\echo 'Inserting CORRECTION CREDIT entries (~50 000 rows)...'

INSERT INTO ledger_entries (
    id, transaction_id, debt_id,
    account_code, account_name, entry_type, amount,
    effective_date, posting_date,
    reference, description, entry_category, created_at
)
SELECT
    gen_random_uuid(),
    md5('loadtest-txn-cor-' || i)::uuid,
    md5('loadtest-debt-'    || i)::uuid,
    '3000', 'Indrivelsesindtægter', 'CREDIT',
    GREATEST(25.00, ROUND((((i - 1) % 200) * 10.00), 2)),
    CURRENT_DATE - ((i - 1) % 30)::int,
    CURRENT_DATE - ((i - 1) % 30)::int,
    'LT-COR-' || LPAD(i::text, 10, '0'),
    'Korrektionspostering – load test #' || i,
    'CORRECTION',
    NOW() - ((((i - 1) % 30) || ' days')::interval)
FROM generate_series(1, 1000000) AS s(i)
WHERE i % 20 = 0;

COMMIT;

-- ── Verification ─────────────────────────────────────────────
\echo 'Ledger entry verification by category:'
SELECT
    entry_category,
    entry_type,
    COUNT(*)                                AS row_count,
    TO_CHAR(SUM(amount), 'FM999,999,999,999.00')    AS total_amount,
    TO_CHAR(AVG(amount), 'FM999,999,990.00')        AS avg_amount
FROM ledger_entries
WHERE reference LIKE 'LT-%'
GROUP BY entry_category, entry_type
ORDER BY entry_category, entry_type;
