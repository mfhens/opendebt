-- V8: Business config, fees, and interest journal enhancements (petition 045/046)

-- Time-versioned business configuration
CREATE TABLE business_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    config_key VARCHAR(100) NOT NULL,
    config_value VARCHAR(500) NOT NULL,
    value_type VARCHAR(20) NOT NULL DEFAULT 'DECIMAL',
    valid_from DATE NOT NULL,
    valid_to DATE,
    description TEXT,
    legal_basis VARCHAR(500),
    created_by VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uq_config_key_valid_from UNIQUE (config_key, valid_from)
);

CREATE INDEX idx_config_key ON business_config(config_key);
CREATE INDEX idx_config_valid_from ON business_config(valid_from);

-- Fee entity for individual fee tracking (gebyrer)
CREATE TABLE fees (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    debt_id UUID NOT NULL REFERENCES debts(id),
    fee_type VARCHAR(30) NOT NULL,
    amount NUMERIC(15,2) NOT NULL,
    accrual_date DATE NOT NULL,
    legal_basis VARCHAR(500),
    paid BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_fee_type CHECK (fee_type IN ('RYKKER', 'UDLAEG', 'LOENINDEHOLDELSE', 'OTHER'))
);

CREATE INDEX idx_fee_debt_id ON fees(debt_id);
CREATE INDEX idx_fee_accrual_date ON fees(accrual_date);

-- Add accounting_target to interest journal entries
ALTER TABLE interest_journal_entries
    ADD COLUMN accounting_target VARCHAR(20);

-- Composite index for interest-eligible debt query performance
CREATE INDEX idx_debt_lifecycle_balance ON debts(lifecycle_state, outstanding_balance)
    WHERE outstanding_balance > 0;

-- Seed: Historical NB rates and derived inddrivelsesrenter (2024-2026)
INSERT INTO business_config (config_key, config_value, value_type, valid_from, valid_to, description, legal_basis, created_by) VALUES
    ('RATE_NB_UDLAAN', '0.0375', 'DECIMAL', '2024-01-08', '2025-01-06', 'Nationalbankens officielle udlånsrente', 'Renteloven § 5', 'system-seed'),
    ('RATE_NB_UDLAAN', '0.0230', 'DECIMAL', '2025-01-06', '2025-07-07', 'Nationalbankens officielle udlånsrente', 'Renteloven § 5', 'system-seed'),
    ('RATE_NB_UDLAAN', '0.0175', 'DECIMAL', '2025-07-07', NULL, 'Nationalbankens officielle udlånsrente', 'Renteloven § 5', 'system-seed'),

    ('RATE_INDR_STD', '0.0775', 'DECIMAL', '2024-01-08', '2025-01-06', 'Inddrivelsesrente (NB + 4%)', 'Gældsinddrivelsesloven § 5, stk. 1-2', 'system-seed'),
    ('RATE_INDR_STD', '0.0630', 'DECIMAL', '2025-01-06', '2025-07-07', 'Inddrivelsesrente (NB + 4%)', 'Gældsinddrivelsesloven § 5, stk. 1-2', 'system-seed'),
    ('RATE_INDR_STD', '0.0575', 'DECIMAL', '2025-07-07', NULL, 'Inddrivelsesrente (NB + 4%)', 'Gældsinddrivelsesloven § 5, stk. 1-2', 'system-seed'),

    ('RATE_INDR_TOLD', '0.0575', 'DECIMAL', '2024-01-08', '2025-01-06', 'Toldrente uden afdragsordning (NB + 2%)', 'EUTK art. 114; Toldloven § 30a', 'system-seed'),
    ('RATE_INDR_TOLD', '0.0430', 'DECIMAL', '2025-01-06', '2025-07-07', 'Toldrente uden afdragsordning (NB + 2%)', 'EUTK art. 114; Toldloven § 30a', 'system-seed'),
    ('RATE_INDR_TOLD', '0.0375', 'DECIMAL', '2025-07-07', NULL, 'Toldrente uden afdragsordning (NB + 2%)', 'EUTK art. 114; Toldloven § 30a', 'system-seed'),

    ('RATE_INDR_TOLD_AFD', '0.0475', 'DECIMAL', '2024-01-08', '2025-01-06', 'Toldrente med afdragsordning (NB + 1%)', 'EUTK art. 114; Toldloven § 30a', 'system-seed'),
    ('RATE_INDR_TOLD_AFD', '0.0330', 'DECIMAL', '2025-01-06', '2025-07-07', 'Toldrente med afdragsordning (NB + 1%)', 'EUTK art. 114; Toldloven § 30a', 'system-seed'),
    ('RATE_INDR_TOLD_AFD', '0.0275', 'DECIMAL', '2025-07-07', NULL, 'Toldrente med afdragsordning (NB + 1%)', 'EUTK art. 114; Toldloven § 30a', 'system-seed'),

    ('FEE_RYKKER', '65.00', 'DECIMAL', '2024-01-01', NULL, 'Rykkergebyr per erindringsskrivelse', 'Opkrævningsloven § 6', 'system-seed'),
    ('FEE_UDLAEG_BASE', '300.00', 'DECIMAL', '2024-01-01', NULL, 'Udlægsafgift basisbeløb', 'Retsafgiftsloven', 'system-seed'),
    ('FEE_UDLAEG_PCT', '0.005', 'DECIMAL', '2024-01-01', NULL, 'Udlægsafgift procent over 3000 kr', 'Retsafgiftsloven', 'system-seed'),
    ('FEE_LOENINDEHOLDELSE', '100.00', 'DECIMAL', '2024-01-01', NULL, 'Lønindeholdelsesgebyr', 'Gældsinddrivelsesloven', 'system-seed'),
    ('THRESHOLD_INTEREST_MIN', '100.00', 'DECIMAL', '2024-01-01', NULL, 'Minimum beløb for renteberegning', 'Intern forretningsregel', 'system-seed'),
    ('THRESHOLD_FORAELDELSE_WARN', '90', 'INTEGER', '2024-01-01', NULL, 'Forældelsesfrist warning days', 'Intern forretningsregel', 'system-seed');

-- Seed: Debt types with interest applicability flags
-- Disable audit trigger (it assumes UUID id column; debt_types uses code as PK)
ALTER TABLE debt_types DISABLE TRIGGER debt_types_audit_trigger;

INSERT INTO debt_types (code, name, category, description, interest_applicable, active, requires_manual_review, civil_law, created_at, updated_at) VALUES
    ('STRAF_BOEDE', 'Strafferetlig bøde', 'BOEDE', 'Bøder idømt ved dom/vedtægt — rentefri', false, true, false, false, now(), now()),
    ('DAGBOEDE', 'Dagbøde', 'BOEDE', 'Administrative dagbøder — rentebærende', true, true, false, false, now(), now()),
    ('ADMIN_BOEDE', 'Administrativ bøde', 'BOEDE', 'Administrative bøder — rentebærende', true, true, false, false, now(), now()),
    ('TOLD', 'Toldskyld', 'TOLD', 'Toldkrav under inddrivelse — NB+2% rente', true, true, false, false, now(), now()),
    ('SU_GAELD', 'SU-gæld', 'GAELD', 'Misligholdt studiegæld', true, true, false, false, now(), now()),
    ('SKAT_REST', 'Restskat', 'SKAT', 'Personlig restskat', true, true, false, false, now(), now()),
    ('UNDERHOLDSBIDRAG', 'Underholdsbidrag', 'BIDRAG', 'Børnebidrag / ægtefællebidrag', true, true, false, false, now(), now())
ON CONFLICT (code) DO UPDATE SET
    interest_applicable = EXCLUDED.interest_applicable,
    updated_at = now();

ALTER TABLE debt_types ENABLE TRIGGER debt_types_audit_trigger;

COMMENT ON TABLE business_config IS 'Time-versioned business configuration values with validity periods (petition 046)';
COMMENT ON TABLE fees IS 'Individual fees (gebyrer) imposed during collection (petition 045)';
