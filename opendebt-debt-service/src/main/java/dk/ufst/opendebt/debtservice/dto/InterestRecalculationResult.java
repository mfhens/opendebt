package dk.ufst.opendebt.debtservice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of retroactively recalculating interest journal entries for a debt after a crossing
 * transaction has been detected and processed by payment-service.
 *
 * <p>Returned by {@code POST /api/v1/debts/{id}/interest/recalculate?from={date}}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterestRecalculationResult {

  private UUID debtId;

  /** The crossing date from which entries were deleted and recalculated. */
  private LocalDate recalculatedFrom;

  /** Today's date (exclusive upper bound — today's accrual runs in the next batch window). */
  private LocalDate recalculatedTo;

  /** Number of journal entries deleted (covering the disrupted period). */
  private int entriesDeleted;

  /** Number of new journal entries written with the corrected balance. */
  private int entriesWritten;

  /** Outstanding balance used for recalculation (post write-down). */
  private BigDecimal balanceUsed;

  /** Total interest recalculated across the corrected period. */
  private BigDecimal totalInterestRecalculated;
}
