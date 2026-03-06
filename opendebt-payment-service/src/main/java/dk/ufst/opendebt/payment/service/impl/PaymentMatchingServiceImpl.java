package dk.ufst.opendebt.payment.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.ufst.opendebt.common.dto.DebtDto;
import dk.ufst.opendebt.payment.bookkeeping.BookkeepingService;
import dk.ufst.opendebt.payment.client.DebtServiceClient;
import dk.ufst.opendebt.payment.dto.IncomingPaymentDto;
import dk.ufst.opendebt.payment.dto.OverpaymentOutcome;
import dk.ufst.opendebt.payment.dto.PaymentMatchResult;
import dk.ufst.opendebt.payment.entity.PaymentEntity;
import dk.ufst.opendebt.payment.repository.PaymentRepository;
import dk.ufst.opendebt.payment.service.OverpaymentRulesService;
import dk.ufst.opendebt.payment.service.PaymentMatchingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implements OCR-based automatic matching of incoming payments as defined by petition001.
 *
 * <p>Flow:
 *
 * <ol>
 *   <li>Look up debts by OCR-linje via debt-service REST API
 *   <li>If exactly one debt is found, auto-match the payment
 *   <li>Write down the debt by the actual paid amount (capped at outstanding balance)
 *   <li>If overpayment, delegate to rules for excess amount handling
 *   <li>If no unique match, route to manual matching on the case
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentMatchingServiceImpl implements PaymentMatchingService {

  private final DebtServiceClient debtServiceClient;
  private final BookkeepingService bookkeepingService;
  private final PaymentRepository paymentRepository;
  private final OverpaymentRulesService overpaymentRulesService;

  @Override
  @Transactional
  public PaymentMatchResult processIncomingPayment(IncomingPaymentDto incomingPayment) {
    String ocrLine = incomingPayment.getOcrLine();
    log.info(
        "Processing incoming payment: ocrLine={}, amount={}, ref={}",
        ocrLine,
        incomingPayment.getAmount(),
        incomingPayment.getCremulReference());

    // Step 1: Attempt OCR-based lookup
    List<DebtDto> matchingDebts = lookupDebtsByOcr(ocrLine);

    // Step 2: Check for unique match
    if (matchingDebts.size() != 1) {
      log.info(
          "OCR-linje '{}' did not uniquely identify a debt (found {}), routing to manual matching",
          ocrLine,
          matchingDebts.size());
      return routeToManualMatching(incomingPayment);
    }

    // Step 3: Unique match found - auto-match
    DebtDto matchedDebt = matchingDebts.get(0);
    log.info(
        "OCR-linje '{}' uniquely matched debt {}, proceeding with auto-match",
        ocrLine,
        matchedDebt.getId());

    return autoMatch(incomingPayment, matchedDebt);
  }

  private List<DebtDto> lookupDebtsByOcr(String ocrLine) {
    if (ocrLine == null || ocrLine.isBlank()) {
      return List.of();
    }
    // AIDEV-NOTE: Calls debt-service REST API — no shared DB (ADR-0007). Circuit-breaker/retry
    // should be added once Resilience4j is wired (currently fails fast on network errors).
    return debtServiceClient.findByOcrLine(ocrLine);
  }

  // AIDEV-NOTE: Manual matching routes to the case (sag) in opendebt-case-service.
  // Currently only records the payment entity with PENDING status; the case-service integration
  // (Flowable task creation) is not yet implemented.
  // AIDEV-TODO: Emit an event or call case-service to create a caseworker matching task.
  private PaymentMatchResult routeToManualMatching(IncomingPaymentDto incomingPayment) {
    PaymentEntity payment = createPaymentRecord(incomingPayment, null);
    payment.setStatus(PaymentEntity.PaymentStatus.PENDING);
    PaymentEntity saved = paymentRepository.save(payment);

    return PaymentMatchResult.builder()
        .paymentId(saved.getId())
        .autoMatched(false)
        .routedToManualMatching(true)
        .excessAmount(BigDecimal.ZERO)
        .build();
  }

  private PaymentMatchResult autoMatch(IncomingPaymentDto incomingPayment, DebtDto matchedDebt) {
    BigDecimal paidAmount = incomingPayment.getAmount();
    BigDecimal outstandingBalance = matchedDebt.getOutstandingBalance();

    // Determine write-down amount and excess
    BigDecimal writeDownAmount;
    BigDecimal excessAmount;
    OverpaymentOutcome excessOutcome = null;

    if (paidAmount.compareTo(outstandingBalance) <= 0) {
      // Normal or underpayment: write down by actual paid amount
      writeDownAmount = paidAmount;
      excessAmount = BigDecimal.ZERO;
    } else {
      // Overpayment: write down the full outstanding balance, handle excess via rules
      writeDownAmount = outstandingBalance;
      excessAmount = paidAmount.subtract(outstandingBalance);
      excessOutcome = overpaymentRulesService.resolveOutcome(matchedDebt.getId());
      log.info("Overpayment detected: excess={}, outcome={}", excessAmount, excessOutcome);
    }

    // Create payment record
    PaymentEntity payment = createPaymentRecord(incomingPayment, matchedDebt.getId());
    payment.setStatus(PaymentEntity.PaymentStatus.COMPLETED);
    payment.setProcessedAt(LocalDateTime.now());
    PaymentEntity saved = paymentRepository.save(payment);

    // AIDEV-NOTE: Bookkeeping is recorded before the write-down call so that a failed write-down
    // (network error to debt-service) does not leave an unrecorded ledger entry.
    // AIDEV-TODO: Both operations must eventually be wrapped in a saga/outbox pattern to guarantee
    // consistency across payment-service and debt-service (ADR-0019 orchestration).
    bookkeepingService.recordPaymentReceived(
        matchedDebt.getId(),
        paidAmount,
        incomingPayment.getValueDate(),
        incomingPayment.getCremulReference());

    // Write down the debt via debt-service API
    debtServiceClient.writeDown(matchedDebt.getId(), writeDownAmount);

    return PaymentMatchResult.builder()
        .paymentId(saved.getId())
        .autoMatched(true)
        .matchedDebtId(matchedDebt.getId())
        .writeDownAmount(writeDownAmount)
        .excessAmount(excessAmount)
        .excessOutcome(excessOutcome)
        .routedToManualMatching(false)
        .build();
  }

  private PaymentEntity createPaymentRecord(
      IncomingPaymentDto incomingPayment, java.util.UUID debtId) {
    return PaymentEntity.builder()
        .debtId(debtId)
        .amount(incomingPayment.getAmount())
        .paymentMethod(PaymentEntity.PaymentMethod.BANK_TRANSFER)
        .status(PaymentEntity.PaymentStatus.PENDING)
        .transactionReference(incomingPayment.getCremulReference())
        .ocrLine(incomingPayment.getOcrLine())
        .paymentDate(incomingPayment.getValueDate().atStartOfDay())
        .build();
  }
}
