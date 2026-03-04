-- OpenDebt Rules Engine - Schema for rule metadata and audit
-- Actual rules are in .drl files or Excel decision tables

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Rule execution audit log
CREATE TABLE rule_execution_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_type VARCHAR(50) NOT NULL,
    request_data JSONB NOT NULL,
    result_data JSONB NOT NULL,
    rules_fired TEXT[],
    execution_time_ms INTEGER,
    executed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    executed_by VARCHAR(100)
);

CREATE INDEX idx_rule_log_type ON rule_execution_log(rule_type);
CREATE INDEX idx_rule_log_executed_at ON rule_execution_log(executed_at);

-- Rule versions for tracking which rules were active
CREATE TABLE rule_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_name VARCHAR(100) NOT NULL,
    version VARCHAR(20) NOT NULL,
    description TEXT,
    effective_from DATE NOT NULL,
    effective_to DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    
    CONSTRAINT uk_rule_version UNIQUE (rule_name, version)
);

COMMENT ON TABLE rule_execution_log IS 'Audit log of all rule evaluations';
COMMENT ON TABLE rule_versions IS 'Version tracking for business rules';
