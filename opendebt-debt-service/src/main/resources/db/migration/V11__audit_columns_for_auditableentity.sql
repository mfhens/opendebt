-- TB-001: Add missing audit columns so DebtEntity and DebtTypeEntity can safely extend
-- AuditableEntity (audit-trail-commons). AuditableEntity maps to: created_at, updated_at,
-- created_by, updated_by, version.
--
-- debts already has created_at, updated_at, created_by, version → only updated_by is missing.
-- debt_types already has created_at, updated_at → created_by, updated_by, version are missing.

ALTER TABLE debts
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100);

ALTER TABLE debt_types
    ADD COLUMN IF NOT EXISTS created_by  VARCHAR(100),
    ADD COLUMN IF NOT EXISTS updated_by  VARCHAR(100),
    ADD COLUMN IF NOT EXISTS version     BIGINT NOT NULL DEFAULT 0;
