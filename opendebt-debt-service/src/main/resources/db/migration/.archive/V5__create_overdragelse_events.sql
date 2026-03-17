-- V5: Overdragelse audit trail table (W7-LIFE-01)
CREATE TABLE overdragelse_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    debt_id UUID NOT NULL REFERENCES debts(id),
    fordringshaver_id UUID NOT NULL,
    modtager_id UUID,
    tidspunkt TIMESTAMPTZ NOT NULL,
    previous_state VARCHAR(20),
    new_state VARCHAR(20),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_overdragelse_debt_id ON overdragelse_events(debt_id);
CREATE INDEX idx_overdragelse_tidspunkt ON overdragelse_events(tidspunkt);

COMMENT ON TABLE overdragelse_events IS 'Audit trail for fordring lifecycle transitions';
