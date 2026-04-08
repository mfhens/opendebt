CREATE TABLE IF NOT EXISTS korrektionspulje_entry (
    id uuid PRIMARY KEY,
    debtor_person_id uuid NOT NULL,
    origin_event_id uuid NOT NULL REFERENCES modregning_event(id),
    surplus_amount numeric(15,2) NOT NULL,
    correction_pool_target varchar(10) NOT NULL CHECK (correction_pool_target IN ('PSRM','DMI')),
    boerne_ydelse_restriction boolean NOT NULL DEFAULT false,
    rente_godtgoerelse_start_date date,
    rente_godtgoerelse_accrued numeric(15,2) NOT NULL DEFAULT 0.00,
    annual_only_settlement boolean NOT NULL DEFAULT false,
    settled_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_kpe_debtor ON korrektionspulje_entry (debtor_person_id);
CREATE INDEX IF NOT EXISTS idx_kpe_settled ON korrektionspulje_entry (settled_at) WHERE settled_at IS NULL;
