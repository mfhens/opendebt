-- V5: Create objections table for indsigelse (petition006)
-- Begrebsmodel: Indsigelse=Objection

CREATE TABLE objections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    debt_id UUID NOT NULL REFERENCES debts(id),
    debtor_person_id UUID NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    registered_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at TIMESTAMPTZ,
    resolution_note VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_objection_status CHECK (status IN ('ACTIVE', 'UPHELD', 'REJECTED'))
);

CREATE INDEX idx_objection_debt_id ON objections(debt_id);
CREATE INDEX idx_objection_debtor ON objections(debtor_person_id);
CREATE INDEX idx_objection_status ON objections(status);
CREATE INDEX idx_objection_active ON objections(debt_id, status) WHERE status = 'ACTIVE';

COMMENT ON TABLE objections IS 'Indsigelse: objections/disputes registered against fordringer';
COMMENT ON COLUMN objections.status IS 'ACTIVE=pending resolution, UPHELD=skyldner medhold, REJECTED=afvist';
