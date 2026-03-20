-- ============================================================
-- Load Test: Cleanup — removes all seeded load test data
-- ============================================================
-- Run this before re-seeding to start fresh.
-- Matches rows by the 'LT-' creditor_reference prefix in the
-- debt DB and the 'LT-' reference prefix in the payment DB.
-- ============================================================

-- ── opendebt_debt ─────────────────────────────────────────────
\echo 'Deleting interest_journal_entries for load test debts...'
DELETE FROM interest_journal_entries
WHERE debt_id IN (
    SELECT id FROM debts WHERE creditor_reference LIKE 'LT-%'
);

\echo 'Deleting claim_lifecycle_events for load test debts...'
DELETE FROM claim_lifecycle_events
WHERE debt_id IN (
    SELECT id FROM debts WHERE creditor_reference LIKE 'LT-%'
);

\echo 'Deleting load test debts...'
DELETE FROM debts WHERE creditor_reference LIKE 'LT-%';

\echo 'Deleting load test batch executions...'
DELETE FROM batch_job_executions WHERE created_by = 'load-test-seeder';

SELECT COUNT(*) AS remaining_debts FROM debts WHERE creditor_reference LIKE 'LT-%';
