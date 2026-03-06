package dk.ufst.opendebt.payment.service;

import java.util.UUID;

import dk.ufst.opendebt.payment.dto.OverpaymentOutcome;

/**
 * Service for determining the outcome of excess amounts when an auto-matched payment exceeds the
 * outstanding debt balance. The outcome is determined by rules based on sagstype and whether the
 * payment is a frivillig indbetaling.
 */
public interface OverpaymentRulesService {

  /**
   * Resolves the overpayment outcome for the given debt.
   *
   * @param debtId the ID of the matched debt
   * @return the rule-driven outcome (PAYOUT or COVER_OTHER_DEBTS)
   */
  OverpaymentOutcome resolveOutcome(UUID debtId);
}
