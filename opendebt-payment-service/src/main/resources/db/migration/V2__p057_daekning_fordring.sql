-- P057 Dækningsrækkefølge — fordring data copy for payment ordering
CREATE TABLE daekning_fordring (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fordring_id VARCHAR(100) NOT NULL,
    debtor_id VARCHAR(100) NOT NULL,
    prioritet_kategori VARCHAR(60) NOT NULL,
    tilbaestaaende_beloeb NUMERIC(15,2) NOT NULL,
    modtagelsesdato DATE NOT NULL,
    legacy_modtagelsesdato DATE,
    sekvens_nummer INTEGER,
    opskrivning_af_fordring_id VARCHAR(100),
    fordring_type VARCHAR(60),
    in_loenindeholdelse_indsats BOOLEAN NOT NULL DEFAULT FALSE,
    in_udlaeg_forretning BOOLEAN NOT NULL DEFAULT FALSE,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    beloeb_opkraevningsrenter NUMERIC(15,2),
    beloeb_inddrivelsesrenter_fordringshaver NUMERIC(15,2),
    beloeb_inddrivelsesrenter_foer_tilbagefoersel NUMERIC(15,2),
    beloeb_inddrivelsesrenter_stk1 NUMERIC(15,2),
    beloeb_oevrige_renter_psrm NUMERIC(15,2),
    beloeb_hoofdfordring NUMERIC(15,2)
);

CREATE INDEX idx_daekning_fordring_debtor ON daekning_fordring(debtor_id);
CREATE INDEX idx_daekning_fordring_fordring ON daekning_fordring(fordring_id);
