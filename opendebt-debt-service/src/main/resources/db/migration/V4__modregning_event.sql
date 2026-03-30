CREATE TABLE IF NOT EXISTS modregning_event (
    id uuid PRIMARY KEY,
    nemkonto_reference_id varchar(100) NOT NULL,
    debtor_person_id uuid NOT NULL,
    receipt_date date NOT NULL,
    decision_date date NOT NULL,
    payment_type varchar(50) NOT NULL,
    indkomst_aar integer,
    disbursement_amount numeric(15,2) NOT NULL,
    tier1_amount numeric(15,2) NOT NULL DEFAULT 0,
    tier2_amount numeric(15,2) NOT NULL DEFAULT 0,
    tier3_amount numeric(15,2) NOT NULL DEFAULT 0,
    residual_payout_amount numeric(15,2) NOT NULL DEFAULT 0,
    tier2_waiver_applied boolean NOT NULL DEFAULT false,
    notice_delivered boolean NOT NULL DEFAULT false,
    notice_delivery_date date,
    klage_frist_dato date NOT NULL,
    rente_godtgoerelse_start_date date,
    rente_godtgoerelse_non_taxable boolean NOT NULL DEFAULT true,
    CONSTRAINT uq_modregning_nemkonto UNIQUE (nemkonto_reference_id)
);
CREATE INDEX IF NOT EXISTS idx_me_debtor ON modregning_event (debtor_person_id);
CREATE INDEX IF NOT EXISTS idx_me_decision_date ON modregning_event (decision_date);
