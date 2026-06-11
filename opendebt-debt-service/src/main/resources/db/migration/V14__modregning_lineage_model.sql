ALTER TABLE modregning_event
    ADD COLUMN IF NOT EXISTS decision_reference varchar(180),
    ADD COLUMN IF NOT EXISTS lineage_reference varchar(180),
    ADD COLUMN IF NOT EXISTS decision_kind varchar(80),
    ADD COLUMN IF NOT EXISTS supersedes_event_id uuid,
    ADD COLUMN IF NOT EXISTS operative boolean NOT NULL DEFAULT true;

UPDATE modregning_event
SET decision_reference = 'DEC-' || nemkonto_reference_id
WHERE decision_reference IS NULL;

UPDATE modregning_event
SET lineage_reference = 'LIN-' || nemkonto_reference_id
WHERE lineage_reference IS NULL;

UPDATE modregning_event
SET decision_kind = 'EXTERNAL_DISBURSEMENT_DECISION'
WHERE decision_kind IS NULL;

ALTER TABLE modregning_event
    ALTER COLUMN decision_reference SET NOT NULL,
    ALTER COLUMN lineage_reference SET NOT NULL,
    ALTER COLUMN decision_kind SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_me_decision_reference
    ON modregning_event (decision_reference);

CREATE INDEX IF NOT EXISTS idx_me_lineage_reference
    ON modregning_event (lineage_reference);

CREATE INDEX IF NOT EXISTS idx_me_operative
    ON modregning_event (operative);
