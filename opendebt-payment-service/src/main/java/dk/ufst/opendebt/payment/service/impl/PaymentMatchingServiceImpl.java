package dk.ufst.opendebt.payment.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.ufst.bookkeeping.model.TimelineReplayResult;
import dk.ufst.bookkeeping.service.TimelineReplayService;
import dk.ufst.opendebt.common.dto.DebtDto;
import dk.ufst.opendebt.payment.bookkeeping.BookkeepingService;
import dk.ufst.opendebt.payment.bookkeeping.model.CrossingDetectionResult;
import dk.ufst.opendebt.payment.bookkeeping.service.CrossingTransactionDetector;
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
 *
 * <p>Crossing transactions (petition039): when the payment's {@code valueDate} (vaerdidag) precedes
 * previously posted events on the same debt, a full timeline replay is triggered internally (storno
 * + interest recalculation in payment-service ledger) and the debt-service interest journal is
 * corrected via a synchronous REST call (ADR-0019 orchestration).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentMatchingServiceImpl implements PaymentMatchingService {

  private final DebtServiceClient debtServiceClient;
  private final BookkeepingService bookkeepingService;
  private final PaymentRepository paymentRepository;
  private final OverpaymentRulesService overpaymentRulesService;
  private final CrossingTransactionDetector crossingTransactionDetector;
  private final TimelineReplayService timelineReplayService;

  @Value("${opendebt.interest.annual-rate:0.0575}")
  private BigDecimal annualInterestRate;

  @Override
  @Transactional
  public PaymentMatchResult processIncomingPayment(IncomingPaymentDto incomingPayment) {
    String ocrLine = incomingPayment.getOcrLine();
    log.info(
        "Processing incoming payment: ocrLine={}, amount={}, ref={}",
        ocrLine,
        incomingPayment.getAmount(),
        incomingPayment.getCremulReference());

    List<DebtDto> matchingDebts = lookupDebtsByOcr(ocrLine);

    if (matchingDebts.size() != 1) {
      log.info(
          "OCR-linje '{}' did not uniquely identify a debt (found {}), routing to manual matching",
          ocrLine,
          matchingDebts.size());
      return routeToManualMatching(incomingPayment);
    }

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

    BigDecimal writeDownAmount;
    BigDecimal excessAmount;
    OverpaymentOutcome excessOutcome = null;

    if (paidAmount.compareTo(outstandingBalance) <= 0) {
      writeDownAmount = paidAmount;
      excessAmount = BigDecimal.ZERO;
    } else {
      writeDownAmount = outstandingBalance;
      excessAmount = paidAmount.subtract(outstandingBalance);
      excessOutcome = overpaymentRulesService.resolveOutcome(matchedDebt.getId());
      log.info("Overpayment detected: excess={}, outcome={}", excessAmount, excessOutcome);
    }

    PaymentEntity payment = createPaymentRecord(incomingPayment, matchedDebt.getId());
    payment.setStatus(PaymentEntity.PaymentStatus.COMPLETED);
    payment.setProcessedAt(LocalDateTime.now());
    PaymentEntity saved = paymentRepository.save(payment);

    // ── Crossing transaction detection (petition039) ───────────────────────────
    // Detect BEFORE recording bookkeeping entries so the crossing check sees the
    // pre-payment event timeline. If a crossing is found, replay immediately after
    // recording the payment so storno and recalculation happen atomically within
    // this transaction.
    CrossingDetectionResult crossing =
        crossingTransactionDetector.detectCrossing(
            matchedDebt.getId(), incomingPayment.getValueDate());

    // AIDEV-NOTE: Bookkeeping is recorded before the write-down call so that a failed write-down
    // (network error to debt-service) does not leave an unrecorded ledger entry.
    // AIDEV-TODO: Both operations must eventually be wrapped in a saga/outbox pattern to guarantee
    // consistency across payment-service and debt-service (ADR-0019 orchestration).
    bookkeepingService.recordPaymentReceived(
        matchedDebt.getId(),
        paidAmount,
        incomingPayment.getValueDate(),
        incomingPayment.getCremulReference());

    // If a crossing was detected, replay the ledger timeline within payment-service
    // (storno of affected ledger_entries + recalculated interest entries).
    TimelineReplayResult replayResult = null;
    if (crossing.isCrossingDetected()) {
      log.info(
          "Crossing transaction detected for debt={}, crossingPoint={}, affectedEvents={}."
              + " Triggering timeline replay.",
          matchedDebt.getId(),
          crossing.getCrossingPoint(),
          crossing.getAffectedEvents().size());

      replayResult =
          timelineReplayService.replayTimeline(
              matchedDebt.getId(),
              crossing.getCrossingPoint(),
              annualInterestRate,
              incomingPayment.getCremulReference());

      log.info(
          "Timeline replay complete for debt={}: stornoEntries={}, newEntries={}, interestDelta={}",
          matchedDebt.getId(),
          replayResult.getStornoEntriesPosted(),
          replayResult.getNewEntriesPosted(),
          replayResult.getInterestDelta());
    }

    // Write down the debt via debt-service API
    debtServiceClient.writeDown(matchedDebt.getId(), writeDownAmount);

    // If crossing, also correct the interest_journal_entries in debt-service.
    // Must be called AFTER write-down so the corrected balance is already in place.
    if (crossing.isCrossingDetected()) {
      debtServiceClient.recalculateInterest(matchedDebt.getId(), crossing.getCrossingPoint());
    }

    return PaymentMatchResult.builder()
        .paymentId(saved.getId())
        .autoMatched(true)
        .matchedDebtId(matchedDebt.getId())
        .writeDownAmount(writeDownAmount)
        .excessAmount(excessAmount)
        .excessOutcome(excessOutcome)
        .routedToManualMatching(false)
        .crossingDetected(crossing.isCrossingDetected())
        .crossingPoint(crossing.isCrossingDetected() ? crossing.getCrossingPoint() : null)
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
