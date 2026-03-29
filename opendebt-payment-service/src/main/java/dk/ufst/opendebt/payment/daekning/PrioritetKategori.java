package dk.ufst.opendebt.payment.daekning;

/** GIL § 4, stk. 1 — statutory priority categories for payment application order. */
public enum PrioritetKategori {
  /** GIL § 4, stk. 1, nr. 1 — rimelige inddrivelsesomkostninger (GIL § 6a stk. 1 og 12). */
  RIMELIGE_OMKOSTNINGER("GIL § 4, stk. 1, nr. 1"),

  /** GIL § 4, stk. 1, nr. 2 — bøder, tvangsbøder og tilbagebetalingskrav (lov nr. 288/2022). */
  BOEDER_TVANGSBOEEDER_TILBAGEBETALING("GIL § 4, stk. 1, nr. 2"),

  /** GIL § 4, stk. 1, nr. 3 — privatretlige underholdsbidrag (covered before offentlig). */
  UNDERHOLDSBIDRAG_PRIVATRETLIG("GIL § 4, stk. 1, nr. 3"),

  /** GIL § 4, stk. 1, nr. 3 — offentlige underholdsbidrag. */
  UNDERHOLDSBIDRAG_OFFENTLIG("GIL § 4, stk. 1, nr. 3"),

  /** GIL § 4, stk. 1, nr. 4 — andre fordringer. */
  ANDRE_FORDRINGER("GIL § 4, stk. 1, nr. 4");

  public final String gilParagraf;

  PrioritetKategori(String gilParagraf) {
    this.gilParagraf = gilParagraf;
  }
}
