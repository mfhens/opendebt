-- P057 Dækningsrækkefølge — immutable payment application records
CREATE TABLE daekning_record (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fordring_id VARCHAR(100) NOT NULL,
    debtor_id VARCHAR(100) NOT NULL,
    komponent VARCHAR(60) NOT NULL,
    daekning_beloeb NUMERIC(15,2) NOT NULL,
    betalingstidspunkt TIMESTAMP WITH TIME ZONE,
    application_timestamp TIMESTAMP WITH TIME ZONE,
    gil_paragraf VARCHAR(100),
    prioritet_kategori VARCHAR(60),
    fifo_sort_key DATE,
    udlaeg_surplus BOOLEAN NOT NULL DEFAULT FALSE,
    inddrivelsesindsats_type VARCHAR(30),
    opskrivning_af_fordring_id VARCHAR(100),
    created_by VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_daekning_record_fordring ON daekning_record(fordring_id);
CREATE INDEX idx_daekning_record_debtor ON daekning_record(debtor_id);
