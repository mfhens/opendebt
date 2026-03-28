-- ============================================================================
-- V2: Legal compliance corrections from G.A. Inddrivelse audit (2026-03-28)
-- ============================================================================
-- Applies corrections to existing deployments. V1__baseline.sql has also been
-- updated for fresh installs, so these changes are idempotent for new schemas.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- P045: Correct fee amounts per GIL § 6, stk. 1
-- Previously: FEE_RYKKER = 65 kr (Opkrævningsloven § 6) — WRONG
--             FEE_LOENINDEHOLDELSE = 100 kr (GIL § 10) — WRONG
-- Correct:    Both fees are governed by GIL § 6, stk. 1
-- ----------------------------------------------------------------------------

UPDATE business_config
SET config_value = '140.00',
    legal_basis   = 'Gaeldsinddrivelsesloven SS 6, stk. 1',
    description   = 'Rykkergebyr per erindringsskrivelse'
WHERE config_key = 'FEE_RYKKER'
  AND effective_to IS NULL;

UPDATE business_config
SET config_value = '300.00',
    legal_basis   = 'Gaeldsinddrivelsesloven SS 6, stk. 1',
    description   = 'Loenindeholdelsesgebyr'
WHERE config_key = 'FEE_LOENINDEHOLDELSE'
  AND effective_to IS NULL;

-- Insert FEE_TILSIGELSE (450 kr) — missing entirely from prior versions
INSERT INTO business_config (config_key, config_value, value_type, effective_from, effective_to,
                              description, legal_basis, created_by)
SELECT 'FEE_TILSIGELSE', '450.00', 'DECIMAL', '2024-01-01', NULL,
       'Tilsigelsesgebyr for indkaldelse til udlaegforretning',
       'Gaeldsinddrivelsesloven SS 6, stk. 1', 'system-seed'
WHERE NOT EXISTS (SELECT 1 FROM business_config WHERE config_key = 'FEE_TILSIGELSE');

-- Fix seed fee rows created by V1 (identified by their hardcoded UUIDs)
UPDATE fees
SET amount    = 140.00,
    legal_basis = 'Gældsinddrivelsesloven § 6, stk. 1 — rykkergebyr for 1. erindringsskrivelse'
WHERE id = '00000000-0000-0000-0000-00000FEE0D02'
  AND fee_type = 'RYKKER';

UPDATE fees
SET amount    = 300.00,
    legal_basis = 'Gældsinddrivelsesloven § 6, stk. 1 — gebyr for lønindeholdelse'
WHERE id = '00000000-0000-0000-0000-00000FEE0E01'
  AND fee_type = 'LOENINDEHOLDELSE';

-- ----------------------------------------------------------------------------
-- P043: Add ikkeinddrivelsesparat flag to debts
-- Debts temporarily exempt from interest accrual while awaiting stamdata
-- correction or other suspension condition (G.A.2.4.3).
-- Default FALSE — no existing debt is retroactively marked ikkeinddrivelsesparat.
-- ----------------------------------------------------------------------------

ALTER TABLE debts
    ADD COLUMN IF NOT EXISTS ikkeinddrivelsesparat BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN debts.ikkeinddrivelsesparat IS
    'When true, fordringen er midlertidigt ikkeinddrivelsesparat og undtages fra renteberegning (G.A.2.4.3 / P043)';
