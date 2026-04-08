CREATE TABLE IF NOT EXISTS notification_outbox (
    id uuid PRIMARY KEY,
    modregning_event_id uuid NOT NULL,
    debtor_person_id uuid NOT NULL,
    payload text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    dispatched boolean NOT NULL DEFAULT false
);
CREATE INDEX IF NOT EXISTS idx_notif_outbox_event ON notification_outbox (modregning_event_id);
CREATE INDEX IF NOT EXISTS idx_notif_outbox_dispatched ON notification_outbox (dispatched) WHERE dispatched = false;
