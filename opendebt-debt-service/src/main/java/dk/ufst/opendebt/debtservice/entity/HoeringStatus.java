package dk.ufst.opendebt.debtservice.entity;

/** HØRING workflow statuses for fordringer with stamdata deviations. */
public enum HoeringStatus {
  /** Awaiting fordringshaver response. */
  AFVENTER_FORDRINGSHAVER,
  /** Fordringshaver approved; awaiting RIM caseworker decision. */
  AFVENTER_RIM,
  /** Hearing approved — fordring accepted for inddrivelse. */
  GODKENDT,
  /** Hearing rejected — fordring returned to REGISTERED. */
  AFVIST,
  /** Fordringshaver withdrew the fordring during hearing. */
  FORTRUDT
}
