-- P058: Add modregning_tier to debts for three-tier offsetting allocation
ALTER TABLE debts ADD COLUMN IF NOT EXISTS modregning_tier integer;
CREATE INDEX IF NOT EXISTS idx_debt_modregning_tier ON debts (debtor_person_id, modregning_tier) WHERE modregning_tier IS NOT NULL;
