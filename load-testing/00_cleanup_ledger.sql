-- ============================================================
-- Load Test: Cleanup — removes load test ledger entries
-- Target DB : opendebt_payment
-- ============================================================
\echo 'Deleting load test ledger entries...'
DELETE FROM ledger_entries WHERE reference LIKE 'LT-%';

SELECT
    entry_category,
    COUNT(*) AS remaining
FROM ledger_entries
WHERE reference LIKE 'LT-%'
GROUP BY entry_category;
