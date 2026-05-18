CREATE TABLE limitation_objection_workflow_record (
    id UUID PRIMARY KEY,
    fordring_id UUID NOT NULL,
    debtor_person_id UUID NOT NULL,
    workflow_case_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    registered_by VARCHAR(100),
    registered_at TIMESTAMP,
    decided_by VARCHAR(100),
    decided_at TIMESTAMP,
    rationale TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT
);
