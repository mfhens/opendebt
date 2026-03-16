-- V6: HØRING workflow table (W7-HEAR-01)
CREATE TABLE hoering (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    debt_id UUID NOT NULL REFERENCES debts(id),
    hoering_status VARCHAR(30) NOT NULL DEFAULT 'AFVENTER_FORDRINGSHAVER',
    deviation_description VARCHAR(500) NOT NULL,
    fordringshaver_begrundelse VARCHAR(1000),
    rim_decision VARCHAR(500),
    sla_deadline TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMPTZ,
    CONSTRAINT chk_hoering_status CHECK (hoering_status IN (
        'AFVENTER_FORDRINGSHAVER', 'AFVENTER_RIM', 'GODKENDT', 'AFVIST', 'FORTRUDT'
    ))
);

CREATE INDEX idx_hoering_debt_id ON hoering(debt_id);
CREATE INDEX idx_hoering_status ON hoering(hoering_status);
CREATE INDEX idx_hoering_sla_deadline ON hoering(sla_deadline);

COMMENT ON TABLE hoering IS 'PSRM HØRING workflow for fordringer with stamdata deviations';
