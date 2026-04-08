-- ============================================================================
-- TB-040: Add PSRM interest-breakdown and dækningsrækkefølge fields to debts
-- ============================================================================
-- P057 (dækningsrækkefølge) requires these fields to be available in debt-service
-- so that GET /internal/debtors/{debtorId}/fordringer/active can return the full
-- set of financial components consumed by payment-service during payment application.
--
-- Fields are populated by the opret-fordring submission flow (creditor portal /
-- M2M intake) when a claim is registered.
-- All amounts in DKK, NUMERIC(15,2).
-- ============================================================================

ALTER TABLE debts
    ADD COLUMN IF NOT EXISTS sekvens_nummer                              INTEGER,
    ADD COLUMN IF NOT EXISTS gil_paragraf                                VARCHAR(100),
    ADD COLUMN IF NOT EXISTS beloeb_opkraevningsrenter                   NUMERIC(15, 2),
    ADD COLUMN IF NOT EXISTS beloeb_inddrivelsesrenter_fordringshaver    NUMERIC(15, 2),
    ADD COLUMN IF NOT EXISTS beloeb_inddrivelsesrenter_foer_tilbagefoersel NUMERIC(15, 2),
    ADD COLUMN IF NOT EXISTS beloeb_inddrivelsesrenter_stk1              NUMERIC(15, 2),
    ADD COLUMN IF NOT EXISTS beloeb_oevrige_renter_psrm                  NUMERIC(15, 2);

-- Index to support ordering by sekvens_nummer in the active-fordringer query
CREATE INDEX IF NOT EXISTS idx_debt_sekvens_nummer
    ON debts (debtor_person_id, sekvens_nummer ASC NULLS LAST);

COMMENT ON COLUMN debts.sekvens_nummer IS
    'Application order within a debtor portfolio for dækningsrækkefølge (P057)';
COMMENT ON COLUMN debts.gil_paragraf IS
    'GIL legal paragraph reference, e.g. "GIL § 4, stk. 1"';
COMMENT ON COLUMN debts.beloeb_opkraevningsrenter IS
    'Opkrævningsrenter component (STK2 rate) at time of receipt';
COMMENT ON COLUMN debts.beloeb_inddrivelsesrenter_fordringshaver IS
    'Inddrivelsesrenter – fordringshaver (previously labelled _STK3)';
COMMENT ON COLUMN debts.beloeb_inddrivelsesrenter_foer_tilbagefoersel IS
    'Inddrivelsesrenter before reversal/tilbageføring';
COMMENT ON COLUMN debts.beloeb_inddrivelsesrenter_stk1 IS
    'Inddrivelsesrenter – stk. 1 component';
COMMENT ON COLUMN debts.beloeb_oevrige_renter_psrm IS
    'Øvrige renter from PSRM legacy system';
