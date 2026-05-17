CREATE TABLE foraeldelse_record (
    id UUID PRIMARY KEY,
    fordring_id UUID NOT NULL UNIQUE,
    debtor_person_id UUID NOT NULL,
    retsgrundlag VARCHAR(30) NOT NULL,
    udskydelse_dato DATE,
    is_in_udskydelse BOOLEAN NOT NULL,
    current_frist_expires DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    kompleks_id UUID,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT
);

CREATE TABLE afbrydelse_event (
    id UUID PRIMARY KEY,
    fordring_id UUID NOT NULL,
    type VARCHAR(40) NOT NULL,
    event_date DATE NOT NULL,
    legal_reference VARCHAR(255) NOT NULL,
    new_frist_expires DATE NOT NULL,
    source_fordring_id UUID,
    target_fordring_id UUID,
    propagation_reason VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT
);

CREATE TABLE tillaegsfrist_event (
    id UUID PRIMARY KEY,
    fordring_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    applied_date DATE NOT NULL,
    extension_years INT NOT NULL,
    new_frist_expires DATE NOT NULL,
    legal_reference VARCHAR(255) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT
);

CREATE TABLE fordringskompleks_link (
    kompleks_id UUID NOT NULL,
    fordring_id UUID NOT NULL,
    linked_at TIMESTAMP NOT NULL,
    PRIMARY KEY (kompleks_id, fordring_id)
);

CREATE TABLE limitation_objection_linkage (
    id UUID PRIMARY KEY,
    fordring_id UUID NOT NULL,
    indsigelse_id UUID NOT NULL UNIQUE,
    workflow_case_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    rationale TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT
);

CREATE INDEX idx_foraeldelse_record_fordring_id ON foraeldelse_record (fordring_id);
CREATE INDEX idx_foraeldelse_record_kompleks_id ON foraeldelse_record (kompleks_id);
CREATE INDEX idx_afbrydelse_event_fordring_id ON afbrydelse_event (fordring_id);
CREATE INDEX idx_tillaegsfrist_event_fordring_id ON tillaegsfrist_event (fordring_id);
CREATE INDEX idx_fordringskompleks_link_kompleks_id ON fordringskompleks_link (kompleks_id);
CREATE INDEX idx_limitation_objection_linkage_fordring_id ON limitation_objection_linkage (fordring_id);
