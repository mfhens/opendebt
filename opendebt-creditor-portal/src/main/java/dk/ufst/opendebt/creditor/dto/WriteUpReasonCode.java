package dk.ufst.opendebt.creditor.dto;

import java.util.Arrays;
import java.util.List;

/**
 * Allowed reason codes (årsagskoder) for write-up (opskrivning) adjustments submitted via the
 * creditor portal.
 *
 * <p>The active set of codes a creditor may use is controlled by {@link
 * CreditorAgreementDto#getAllowedWriteUpReasonCodes()}, which falls back to all codes when the
 * agreement does not restrict them.
 *
 * <p>Codes follow the PSRM short-code convention (4–5 uppercase ASCII characters):
 *
 * <ul>
 *   <li>{@link #DINDB} — uncovered payment (dækningsløs indbetaling)
 *   <li>{@link #OMPL} — reallocation of coverages (omplacering af dækninger)
 *   <li>{@link #AFSK} — reversal of a write-off (tilbageførsel af afskrivning)
 * </ul>
 */
public enum WriteUpReasonCode {

  /** Dækningsløs indbetaling – payment received without a matching coverage. */
  DINDB,

  /** Omplacering af dækninger – reallocation of existing coverage entries. */
  OMPL,

  /** Tilbageførsel af afskrivning – reversal of a previously written-off amount. */
  AFSK;

  /** Returns all reason codes as an unmodifiable list. */
  public static List<WriteUpReasonCode> allCodes() {
    return Arrays.asList(values());
  }
}
