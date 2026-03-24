package dk.ufst.opendebt.debtservice.offsetting;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Summary of a completed offsetting cycle.
 *
 * @param debtorPersonId the debtor for whom offsetting was performed
 * @param totalOffset total amount applied across all debts
 * @param remainder amount remaining after all eligible debts were processed
 * @param appliedMeasures individual set-off allocations, ordered by priority
 */
public record OffsettingResult(
    UUID debtorPersonId,
    BigDecimal totalOffset,
    BigDecimal remainder,
    List<OffsetAllocation> appliedMeasures) {

  /**
   * A single allocation of offset funds against one debt.
   *
   * @param debtId the debt that was offset
   * @param offsetAmount the amount applied to this debt
   * @param measureId the CollectionMeasure (SET_OFF) UUID recorded in the debt service
   */
  public record OffsetAllocation(UUID debtId, BigDecimal offsetAmount, UUID measureId) {}
}
