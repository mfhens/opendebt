package dk.ufst.opendebt.debtservice.dto;

/** Slutstatus for a kvittering response after fordring submission. */
public enum SlutstatusEnum {
  /** Fordring successfully accepted for inddrivelse. */
  UDFOERT,
  /** Fordring rejected due to validation errors. */
  AFVIST,
  /** Fordring requires HØRING due to stamdata deviations. */
  HOERING
}
