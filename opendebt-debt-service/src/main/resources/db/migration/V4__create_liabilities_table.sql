-- V4: Create liabilities table for haeftelse (petition005)
-- Begrebsmodel: Hæftelse=Liability, Enehæftelse=Sole, Solidarisk=Joint and Several, Delt=Proportional

CREATE TABLE liabilities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    debt_id UUID NOT NULL REFERENCES debts(id),
    debtor_person_id UUID NOT NULL,
    liability_type VARCHAR(30) NOT NULL,
    share_amount NUMERIC(15,2),
    share_percentage NUMERIC(5,2),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_liability_type CHECK (liability_type IN (
        'SOLE', 'JOINT_AND_SEVERAL', 'PROPORTIONAL'
    )),
    CONSTRAINT chk_share_percentage CHECK (share_percentage IS NULL OR (share_percentage > 0 AND share_percentage <= 100)),
    CONSTRAINT uq_liability_debt_debtor UNIQUE (debt_id, debtor_person_id)
);

CREATE INDEX idx_liability_debt_id ON liabilities(debt_id);
CREATE INDEX idx_liability_debtor ON liabilities(debtor_person_id);
CREATE INDEX idx_liability_active ON liabilities(active) WHERE active = TRUE;

COMMENT ON TABLE liabilities IS 'Hæftelse: liability relationships between fordring and skyldner';
COMMENT ON COLUMN liabilities.liability_type IS 'SOLE=enehæftelse, JOINT_AND_SEVERAL=solidarisk, PROPORTIONAL=delt';
COMMENT ON COLUMN liabilities.share_percentage IS 'Only for PROPORTIONAL - percentage share of liability';
