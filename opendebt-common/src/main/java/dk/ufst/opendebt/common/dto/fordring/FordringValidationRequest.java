package dk.ufst.opendebt.common.dto.fordring;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request object for fordring (claim action) validation.
 *
 * <p>Carries the pre-parsed claim action data needed for Drools rule evaluation. Structure presence
 * flags indicate which MF*Struktur elements were present in the original submission.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FordringValidationRequest {

  // ===== Action identification =====

  /** The action type code (e.g., OPRETFORDRING, GENINDSENDFORDRING, NEDSKRIV). */
  private String aktionKode;

  // ===== Structure presence flags =====

  private boolean mfOpretFordringStrukturPresent;
  private boolean mfGenindsendFordringStrukturPresent;
  private boolean mfNedskrivFordringStrukturPresent;
  private boolean mfTilbagekaldFordringStrukturPresent;
  private boolean mfAendrFordringStrukturPresent;
  private boolean mfOpskrivningReguleringStrukturPresent;
  private boolean mfOpskrivningAnnulleretNedskrivningIndbetalingStrukturPresent;
  private boolean mfOpskrivningOmgjortNedskrivningReguleringStrukturPresent;
  private boolean mfNedskrivningAnnulleretOpskrivningReguleringStrukturPresent;
  private boolean mfNedskrivningAnnulleretOpskrivningIndbetalingStrukturPresent;

  // ===== Currency and type =====

  /** Currency code, must be DKK. */
  private String valutaKode;

  /** Art type: INDR (inddrivelse) or MODR (modregning). */
  private String artType;

  // ===== Interest =====

  /** Additional interest rate (MerRenteSats). Must be non-negative. */
  private BigDecimal merRenteSats;

  // ===== Dates =====

  /** Effective date (VirkningsDato). */
  private LocalDate virkningsDato;

  /** Receipt timestamp (ModtagelsesTidspunkt). */
  private LocalDateTime modtagelsesTidspunkt;

  /** Period start date. */
  private LocalDate periodeFra;

  /** Period end date. */
  private LocalDate periodeTil;

  /** Whether VirkningsDato is required for this action type. */
  private boolean virkningsDatoRequired;

  // ===== Agreement =====

  /** Creditor agreement ID. */
  private String fordringhaveraftaleId;

  /** Whether the agreement was found in the system. */
  private boolean agreementFound;

  /** Claim type code from DMI. */
  private String dmiFordringTypeKode;

  /** Whether the claim type is allowed by the agreement. */
  private boolean claimTypeAllowedByAgreement;

  /** Whether MFAftaleSystemIntegration is set on the agreement. */
  private boolean mfAftaleSystemIntegration;

  // ===== Submission context =====

  /** Whether this is a system-to-system (M2M) submission. */
  private boolean systemToSystem;

  // ===== Debtor =====

  /** Debtor identifier string. */
  private String debtorId;

  // ===== Withdrawal context (for rule 467) =====

  /** Receipt timestamp of the main claim being withdrawn, if applicable. */
  private LocalDateTime hovedfordringModtagelsesTidspunkt;
}
