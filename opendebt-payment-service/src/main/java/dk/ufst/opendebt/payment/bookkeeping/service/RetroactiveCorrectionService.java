package dk.ufst.opendebt.payment.bookkeeping.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import dk.ufst.opendebt.payment.bookkeeping.model.CorrectionResult;

/**
 * Service for handling retroactive corrections that require storno of existing ledger entries and
 * recalculation of downstream effects (interest).
 *
 * <p>Example: An udlaeg is retroactively reduced. This service will:
 *
 * <ol>
 *   <li>Record the correction event in the debt timeline
 *   <li>Storno (reverse) all interest accrual ledger entries after the effective date
 *   <li>Recalculate interest for each period with the corrected principal balance
 *   <li>Post new interest accrual entries with correct amounts
 * </ol>
 */
public interface RetroactiveCorrectionService {

  /**
   * Applies a retroactive correction to a debt and recalculates all downstream effects.
   *
   * @param debtId the debt being corrected
   * @param effectiveDate when the correction economically applies
   * @param originalAmount the original amount that was recorded
   * @param correctedAmount the corrected amount
   * @param annualInterestRate the interest rate for recalculation
   * @param reference external reference for the correction
   * @param reason human-readable reason for the correction
   * @return result containing details of all storno and new entries posted
   */
  CorrectionResult applyRetroactiveCorrection(
      UUID debtId,
      LocalDate effectiveDate,
      BigDecimal originalAmount,
      BigDecimal correctedAmount,
      BigDecimal annualInterestRate,
      String reference,
      String reason);
}
