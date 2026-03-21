package dk.ufst.opendebt.payment.dto;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Result of processing an incoming payment through OCR-based matching. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMatchResult {

  private UUID paymentId;

  /** True when the OCR-linje uniquely identified a debt and the payment was auto-matched. */
  private boolean autoMatched;

  /** The debt the payment was matched to (null when not auto-matched). */
  private UUID matchedDebtId;

  /** The amount by which the debt was written down. */
  private BigDecimal writeDownAmount;

  /** The excess amount beyond the outstanding balance (zero when no overpayment). */
  private BigDecimal excessAmount;

  /** Rule-driven outcome for the excess amount (null when no overpayment). */
  private OverpaymentOutcome excessOutcome;

  /** True when the payment could not be auto-matched and was routed to manual matching. */
  private boolean routedToManualMatching;

  /**
   * True when the payment's effective date preceded previously posted events and a full timeline
   * replay (storno + interest recalculation) was triggered (petition039).
   */
  private boolean crossingDetected;

  /**
   * The earliest date from which the timeline was replayed (null when no crossing). Interest
   * journal entries in debt-service are recalculated from this date forward.
   */
  private java.time.LocalDate crossingPoint;
}
