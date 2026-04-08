package dk.ufst.opendebt.creditor.dto;

import java.util.Arrays;
import java.util.List;

/**
 * Controlled reason codes for write-down (nedskrivning) adjustments submitted via the creditor
 * portal.
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
 * <p>Spec reference: SPEC-P053 §1.1 / FR-1 / Gæld.bekendtg. § 7 stk. 2
 */
public enum WriteDownReasonCode {

  /** Direkte indbetaling til fordringshaver — Gæld.bekendtg. § 7 stk. 2 nr. 1 */
  NED_INDBETALING,

  /** Fejlagtig oversendelse til inddrivelse — Gæld.bekendtg. § 7 stk. 2 nr. 2 */
  NED_FEJL_OVERSENDELSE,

  /** Opkrævningsgrundlaget har ændret sig — Gæld.bekendtg. § 7 stk. 2 nr. 3 */
  NED_GRUNDLAG_AENDRET;

  /** Returns all reason codes as an unmodifiable list in declaration order. */
  public static List<WriteDownReasonCode> allCodes() {
    return Arrays.asList(values());
  }
}
