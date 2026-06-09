CREATE TABLE section50_candidate_item (
    id UUID PRIMARY KEY,
    debtor_person_id UUID NOT NULL,
    claim_id VARCHAR(100) NOT NULL UNIQUE,
    item_type VARCHAR(20) NOT NULL,
    claim_category VARCHAR(50) NOT NULL,
    amount NUMERIC(15,2) NOT NULL,
    suspected_data_error BOOLEAN NOT NULL DEFAULT FALSE,
    confirmed_retskraft BOOLEAN NOT NULL DEFAULT FALSE,
    accessory_of_claim_id VARCHAR(100),
    disproportionate_write_off BOOLEAN NOT NULL DEFAULT FALSE,
    error_type VARCHAR(100),
    complexity VARCHAR(20),
    payment_opportunity VARCHAR(20),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT
);

CREATE INDEX idx_s50_candidate_debtor ON section50_candidate_item (debtor_person_id);

CREATE TABLE section50_worklist (
    id UUID PRIMARY KEY,
    debtor_person_id UUID NOT NULL,
    context_type VARCHAR(50) NOT NULL,
    ordering_mode VARCHAR(50) NOT NULL,
    legal_reference VARCHAR(200) NOT NULL,
    amount_window NUMERIC(15,2),
    generated_at TIMESTAMP NOT NULL,
    selected_next_item_id VARCHAR(100),
    override_reason VARCHAR(500),
    override_legal_basis VARCHAR(500),
    deviation_reason VARCHAR(500),
    modregning_outcome VARCHAR(50),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT
);

CREATE INDEX idx_s50_worklist_debtor ON section50_worklist (debtor_person_id);

CREATE TABLE section50_worklist_entry (
    id UUID PRIMARY KEY,
    worklist_id UUID NOT NULL,
    rank_order INTEGER NOT NULL,
    claim_id VARCHAR(100) NOT NULL,
    item_type VARCHAR(20) NOT NULL,
    claim_category VARCHAR(50) NOT NULL,
    suspected_data_error BOOLEAN NOT NULL DEFAULT FALSE,
    confirmed_retskraft BOOLEAN NOT NULL DEFAULT FALSE,
    within_amount_window BOOLEAN NOT NULL DEFAULT FALSE,
    selection_reason VARCHAR(500),
    prioritisation_factors VARCHAR(1000),
    suppressed_reason VARCHAR(500),
    amount NUMERIC(15,2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT,
    CONSTRAINT fk_s50_entry_worklist FOREIGN KEY (worklist_id) REFERENCES section50_worklist (id)
);

CREATE INDEX idx_s50_entry_worklist ON section50_worklist_entry (worklist_id);

CREATE TABLE section50_decision_snapshot (
    id UUID PRIMARY KEY,
    worklist_id UUID NOT NULL UNIQUE,
    rule_path VARCHAR(100) NOT NULL,
    input_hash VARCHAR(128) NOT NULL,
    selected_next_item_id VARCHAR(100),
    legal_reference VARCHAR(200) NOT NULL,
    audit_event_id UUID NOT NULL,
    origin VARCHAR(100) NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    notes VARCHAR(1000),
    prioritisation_factors VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT,
    CONSTRAINT fk_s50_snapshot_worklist FOREIGN KEY (worklist_id) REFERENCES section50_worklist (id)
);
