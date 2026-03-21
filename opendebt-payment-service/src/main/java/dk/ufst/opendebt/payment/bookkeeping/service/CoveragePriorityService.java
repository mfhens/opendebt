package dk.ufst.opendebt.payment.bookkeeping.service;

import java.math.BigDecimal;
import java.util.UUID;

import dk.ufst.opendebt.payment.bookkeeping.model.CoverageAllocation;

/**
 * Applies dækningsrækkefølge: inddrivelsesrente is always covered before hovedstol. Given a payment
 * amount, outstanding interest, and outstanding principal, computes how much goes to interest and
 * how much to principal.
 */
public interface CoveragePriorityService {

  /**
   * Allocates a payment between interest, fees, and principal according to dækningsrækkefølge:
   * inddrivelsesrente first, then gebyrer, then hovedstol.
   *
   * @param debtId the debt being paid
   * @param paymentAmount the total payment amount
   * @param accruedInterest outstanding accrued interest at the payment effective date
   * @param outstandingFees outstanding fees (gebyrer) at the payment effective date
   * @param principalBalance outstanding principal at the payment effective date
   * @return allocation showing interest, fees, and principal portions
   */
  CoverageAllocation allocatePayment(
      UUID debtId,
      BigDecimal paymentAmount,
      BigDecimal accruedInterest,
      BigDecimal outstandingFees,
      BigDecimal principalBalance);
}
