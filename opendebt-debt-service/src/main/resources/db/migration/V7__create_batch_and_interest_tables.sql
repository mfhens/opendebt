-- V7: Create batch job execution + interest journal tables (petition043)

CREATE TABLE batch_job_executions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_name VARCHAR(100) NOT NULL,
    execution_date DATE NOT NULL,
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    records_processed INT NOT NULL DEFAULT 0,
    records_failed INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'RUNNING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_batch_status CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX idx_batch_job_name ON batch_job_executions(job_name);
CREATE INDEX idx_batch_execution_date ON batch_job_executions(execution_date);

CREATE TABLE interest_journal_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    debt_id UUID NOT NULL REFERENCES debts(id),
    accrual_date DATE NOT NULL,
    effective_date DATE NOT NULL,
    balance_snapshot NUMERIC(15,2) NOT NULL,
    rate NUMERIC(5,4) NOT NULL,
    interest_amount NUMERIC(15,2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uq_interest_debt_date UNIQUE (debt_id, accrual_date)
);

CREATE INDEX idx_interest_debt_id ON interest_journal_entries(debt_id);
CREATE INDEX idx_interest_accrual_date ON interest_journal_entries(accrual_date);

COMMENT ON TABLE batch_job_executions IS 'Tracks batch job runs for idempotency and audit';
COMMENT ON TABLE interest_journal_entries IS 'Inddrivelsesrente journal entries with storno-compatible metadata';
