-- V6: Create collection_measures table for inddrivelsesskridt (petition007)
-- Begrebsmodel: Inddrivelsesskridt=Collection Measure, Modregning=Set-off,
-- Loenindeholdelse=Wage Garnishment, Udlaeg=Attachment

CREATE TABLE collection_measures (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    debt_id UUID NOT NULL REFERENCES debts(id),
    measure_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'INITIATED',
    initiated_by VARCHAR(100),
    initiated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    amount NUMERIC(15,2),
    note VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_measure_type CHECK (measure_type IN (
        'SET_OFF', 'WAGE_GARNISHMENT', 'ATTACHMENT'
    )),
    CONSTRAINT chk_measure_status CHECK (status IN (
        'INITIATED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'
    ))
);

CREATE INDEX idx_measure_debt_id ON collection_measures(debt_id);
CREATE INDEX idx_measure_type ON collection_measures(measure_type);
CREATE INDEX idx_measure_status ON collection_measures(status);

COMMENT ON TABLE collection_measures IS 'Inddrivelsesskridt: collection measure steps (modregning, loenindeholdelse, udlaeg)';
