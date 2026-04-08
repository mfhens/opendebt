package dk.ufst.opendebt.payment.daekning;

/** Sub-positions within each fordring — Gæld.bekendtg. § 4, stk. 3. Ordinal = sub-position - 1. */
public enum RenteKomponent {
  /** Sub-position 1 — opkrævningsrenter. */
  OPKRAEVNINGSRENTER,

  /** Sub-position 2 — inddrivelsesrenter fordringshaver (Gæld.bekendtg. § 9). */
  INDDRIVELSESRENTER_FORDRINGSHAVER,

  /** Sub-position 3 — inddrivelsesrenter opstået før tilbageførselsperiode. */
  INDDRIVELSESRENTER_FOER_TILBAGEFOERSEL,

  /** Sub-position 4 — inddrivelsesrenter (GIL § 9, stk. 1). */
  INDDRIVELSESRENTER_STK1,

  /** Sub-position 5 — øvrige renter i PSRM. */
  OEVRIGE_RENTER_PSRM,

  /** Sub-position 6 — Hoofdfordring (principal, covered last). */
  HOOFDFORDRING
}
