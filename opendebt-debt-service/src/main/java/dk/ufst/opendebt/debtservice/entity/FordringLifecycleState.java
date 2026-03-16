package dk.ufst.opendebt.debtservice.entity;

/** PSRM lifecycle states for a fordring (claim). */
public enum FordringLifecycleState {
  /** Fordring created but not overdue. */
  REGISTERED,
  /** SRB passed, not fully paid. */
  RESTANCE,
  /** Submitted but in hearing. */
  HOERING,
  /** Accepted for inddrivelse (slutstatus UDFØRT). */
  OVERDRAGET,
  /** Withdrawn (with årsagskode sub-state). */
  TILBAGEKALDT,
  /** Written off (with reason code). */
  AFSKREVET,
  /** Fully paid. */
  INDFRIET
}
