package dk.ufst.opendebt.payment.service;

import dk.ufst.opendebt.payment.dto.IncomingPaymentDto;
import dk.ufst.opendebt.payment.dto.PaymentMatchResult;

/**
 * Service for automatic matching of incoming payments using Betalingsservice OCR-linje. Implements
 * the business rules defined in petition001.
 */
public interface PaymentMatchingService {

  /**
   * Processes an incoming payment by attempting OCR-based automatic matching.
   *
   * <ul>
   *   <li>If the OCR-linje uniquely identifies a debt, the payment is auto-matched and the debt is
   *       written down by the actual paid amount.
   *   <li>If the OCR-linje does not uniquely identify a debt, the payment is routed to manual
   *       matching on the case.
   *   <li>If the paid amount exceeds the outstanding balance, a rule-driven overpayment branch
   *       determines the excess amount outcome.
   * </ul>
   *
   * @param incomingPayment the incoming payment data from CREMUL
   * @return the result of the matching attempt
   */
  PaymentMatchResult processIncomingPayment(IncomingPaymentDto incomingPayment);
}
