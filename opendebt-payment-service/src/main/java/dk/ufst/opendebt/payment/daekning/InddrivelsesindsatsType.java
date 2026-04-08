package dk.ufst.opendebt.payment.daekning;

/** Inddrivelsesindsats type — determines surplus routing (GIL § 4, stk. 3). */
public enum InddrivelsesindsatsType {
  /** Udlæg — Retsplejelovens § 507: surplus is isolated, NOT applied to other fordringer. */
  UDLAEG,

  /** Lønindeholdelse — GIL § 4, stk. 3: surplus applied to same-type eligible fordringer. */
  LOENINDEHOLDELSE,

  /** Modregning — GIL § 4, stk. 3: surplus applied to same-type eligible fordringer. */
  MODREGNING,

  /** Frivillig betaling — normal GIL § 4, stk. 1 ordering, all fordringer eligible. */
  FRIVILLIG
}
