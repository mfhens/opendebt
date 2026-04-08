ALTER TABLE collection_measures
    ADD COLUMN IF NOT EXISTS modregning_event_id uuid REFERENCES modregning_event(id),
    ADD COLUMN IF NOT EXISTS waiver_applied boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS caseworker_id uuid;
