package dk.ufst.opendebt.rules.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

/** Request to determine collection priority for offsetting/garnishment. */
@Data
@Builder
public class CollectionPriorityRequest {

  private UUID debtId;
  private String debtTypeCode;
  private String debtCategory;
  private BigDecimal totalAmount;
  private int daysPastDue;

  // -----------------------------------------------------------------------
  // GIL § 4 claim-type flags (TB-034: corrected inter-claim ordering)
  // -----------------------------------------------------------------------

  /** Rimelige omkostninger — GIL § 6a, stk. 1 (category 1, highest priority). */
  private boolean isRimeligOmkostning;

  /** Bøder, tvangsbøder og tilbagebetalingskrav — GIL § 10b (category 2). */
  private boolean isFine;

  /** Underholdsbidrag (barnebidrag m.v.) — GIL § 4, stk. 1, nr. 2 (category 3). */
  private boolean isChildSupport;

  /**
   * Within underholdsbidrag: true = privatretlig (ordning 1, covered first); false = offentlig
   * (ordning 2). Ignored unless {@code isChildSupport == true}.
   *
   * <p>Catala: UNDERHOLDSBIDRAG_PRIVATRETLIG → underholdsbidragOrdning = 1 (FR-1.3c).
   */
  private boolean isPrivatUnderholdsbidrag;

  /**
   * Tax debts — mapped to ANDRE_FORDRINGER (category 4) per GIL § 4, stk. 1, nr. 3.
   *
   * <p>Previously (incorrectly) assigned rank 2. Fixed by TB-034.
   */
  private boolean isTaxDebt;

  /** Court-ordered debts — mapped to ANDRE_FORDRINGER (category 4). */
  private boolean isCourtOrdered;

  // -----------------------------------------------------------------------
  // FIFO sort key fields — GIL § 4, stk. 2 (FR-2.1 / FR-2.2)
  // Catala: FifoSortNøgle scope (ga_2_3_2_1_daekningsraekkefoeigen.catala_da)
  // -----------------------------------------------------------------------

  /**
   * Claim creation / reception date used as FIFO sort key (FR-2.1).
   *
   * <p>Corresponds to Catala {@code modtagelsesdato}.
   */
  private LocalDate oprettetDato;

  /**
   * True when the claim has a legacy reception date from before the current system.
   *
   * <p>Catala: {@code harLegacyModtagelsesdato} (FR-2.2).
   */
  private boolean harLegacyOprettetDato;

  /**
   * Legacy reception date used as FIFO key when the claim was transferred to the collection system
   * before 1 September 2013 (FR-2.2).
   *
   * <p>Catala: {@code legacyModtagelsesdato}.
   */
  private LocalDate legacyOprettetDato;
}
