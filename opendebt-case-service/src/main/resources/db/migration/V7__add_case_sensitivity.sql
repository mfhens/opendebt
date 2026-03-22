-- V7: Add case sensitivity classification for VIP/PEP/CONFIDENTIAL access control (W9-RBAC-02)
-- ADR-0014: GDPR data isolation - sensitivity metadata for access control
-- Petition048: Role-Based Data Access Control

-- Add sensitivity classification column with default NORMAL
ALTER TABLE cases
ADD COLUMN sensitivity VARCHAR(20) NOT NULL DEFAULT 'NORMAL';

-- Constraint: only allowed values (NORMAL, VIP, PEP, CONFIDENTIAL)
ALTER TABLE cases
ADD CONSTRAINT chk_case_sensitivity
CHECK (sensitivity IN ('NORMAL', 'VIP', 'PEP', 'CONFIDENTIAL'));

-- Index for sensitivity-based filtering queries
CREATE INDEX idx_cases_sensitivity ON cases(sensitivity);

-- Comment the column
COMMENT ON COLUMN cases.sensitivity IS 'Access control sensitivity level: NORMAL (no restrictions), VIP (requires HANDLE_VIP_CASES capability), PEP (requires HANDLE_PEP_CASES capability), CONFIDENTIAL (supervisor/admin only)';
