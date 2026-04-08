package dk.ufst.opendebt.debtservice.dto;

/**
 * Controlled reason codes for write-down (nedskrivning) adjustments at the debt-service API
 * boundary.
 *
 * <p>Values are identical to the portal enum {@code
 * dk.ufst.opendebt.creditor.dto.WriteDownReasonCode} per Decision 2 (dual enum, no shared
 * extraction). Parity is enforced by code review convention.
 *
 * <p>The three codes are exhaustive per Gæld.bekendtg. § 7, stk. 2. They must not be extended
 * without a legislative amendment.
 *
 * <ul>
 *   <li>{@link #NED_INDBETALING} — direct payment received by the creditor (nr. 1)
 *   <li>{@link #NED_FEJL_OVERSENDELSE} — erroneous referral for debt collection (nr. 2)
 *   <li>{@link #NED_GRUNDLAG_AENDRET} — assessment basis has changed (nr. 3)
 * </ul>
 *
 * <p>Spec reference: SPEC-P053 §1.3 / FR-1 / FR-9 / Gæld.bekendtg. § 7 stk. 2
 */
public enum WriteDownReasonCode {

  /** Direkte indbetaling til fordringshaver — Gæld.bekendtg. § 7 stk. 2 nr. 1 */
  NED_INDBETALING,

  /** Fejlagtig oversendelse til inddrivelse — Gæld.bekendtg. § 7 stk. 2 nr. 2 */
  NED_FEJL_OVERSENDELSE,

  /** Opkrævningsgrundlaget har ændret sig — Gæld.bekendtg. § 7 stk. 2 nr. 3 */
  NED_GRUNDLAG_AENDRET
}
