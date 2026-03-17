package dk.ufst.opendebt.debtservice.entity;

/** PSRM lifecycle states for a claim. */
public enum ClaimLifecycleState {
  /** Claim created but not overdue. */
  REGISTERED,
  /** Last payment date passed, not fully paid. */
  RESTANCE,
  /** Submitted but in hearing. */
  HOERING,
  /** Accepted for collection. */
  OVERDRAGET,
  /** Withdrawn (with reason code sub-state). */
  TILBAGEKALDT,
  /** Written off (with reason code). */
  AFSKREVET,
  /** Fully paid. */
  INDFRIET
}
