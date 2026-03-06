package dk.ufst.opendebt.payment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dk.ufst.opendebt.common.dto.DebtDto;
import dk.ufst.opendebt.payment.bookkeeping.BookkeepingService;
import dk.ufst.opendebt.payment.client.DebtServiceClient;
import dk.ufst.opendebt.payment.dto.IncomingPaymentDto;
import dk.ufst.opendebt.payment.dto.OverpaymentOutcome;
import dk.ufst.opendebt.payment.dto.PaymentMatchResult;
import dk.ufst.opendebt.payment.entity.PaymentEntity;
import dk.ufst.opendebt.payment.repository.PaymentRepository;
import dk.ufst.opendebt.payment.service.OverpaymentRulesService;

/**
 * Unit tests for {@link PaymentMatchingServiceImpl}, covering all scenarios from
 * petitions/petition001.feature:
 *
 * <ol>
 *   <li>Unique OCR auto-match even when the amount differs
 *   <li>Debt is written down by the actual paid amount
 *   <li>Payment without a unique OCR match is routed to manual matching
 *   <li>Overpayment follows a rule-driven branch (payout)
 *   <li>Overpayment follows a rule-driven branch (use to cover other debt posts)
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class PaymentMatchingServiceImplTest {

  @Mock private DebtServiceClient debtServiceClient;
  @Mock private BookkeepingService bookkeepingService;
  @Mock private PaymentRepository paymentRepository;
  @Mock private OverpaymentRulesService overpaymentRulesService;

  private PaymentMatchingServiceImpl service;

  @BeforeEach
  void setUp() {
    service =
        new PaymentMatchingServiceImpl(
            debtServiceClient, bookkeepingService, paymentRepository, overpaymentRulesService);
  }

  // =========================================================================
  // Scenario 1: Unique OCR auto-match even when the amount differs
  // =========================================================================

  @Test
  void uniqueOcrAutoMatch_evenWhenAmountDiffers() {
    // Given: OCR-123 uniquely identifies debt D1 with outstanding balance 1000
    UUID debtId = UUID.randomUUID();
    DebtDto debt =
        DebtDto.builder()
            .id(debtId)
            .ocrLine("OCR-123")
            .outstandingBalance(new BigDecimal("1000"))
            .build();
    when(debtServiceClient.findByOcrLine("OCR-123")).thenReturn(List.of(debt));
    when(debtServiceClient.writeDown(any(), any())).thenReturn(debt);
    stubPaymentRepositorySave();

    // And: incoming payment references OCR-123 with amount 900 (differs from 1000)
    IncomingPaymentDto incoming = incomingPayment("OCR-123", "900");

    // When
    PaymentMatchResult result = service.processIncomingPayment(incoming);

    // Then: the payment is auto-matched to debt D1
    assertThat(result.isAutoMatched()).isTrue();
    assertThat(result.getMatchedDebtId()).isEqualTo(debtId);
    // And: the payment is not routed to manual matching
    assertThat(result.isRoutedToManualMatching()).isFalse();
  }

  // =========================================================================
  // Scenario 2: Debt is written down by the actual paid amount
  // =========================================================================

  @Test
  void debtIsWrittenDown_byActualPaidAmount() {
    // Given: OCR-456 uniquely identifies debt D2 with outstanding balance 1000
    UUID debtId = UUID.randomUUID();
    DebtDto debt =
        DebtDto.builder()
            .id(debtId)
            .ocrLine("OCR-456")
            .outstandingBalance(new BigDecimal("1000"))
            .build();
    when(debtServiceClient.findByOcrLine("OCR-456")).thenReturn(List.of(debt));
    when(debtServiceClient.writeDown(any(), any())).thenReturn(debt);
    stubPaymentRepositorySave();

    // And: incoming payment references OCR-456 with amount 600
    IncomingPaymentDto incoming = incomingPayment("OCR-456", "600");

    // When
    PaymentMatchResult result = service.processIncomingPayment(incoming);

    // Then: the payment is auto-matched
    assertThat(result.isAutoMatched()).isTrue();
    assertThat(result.getMatchedDebtId()).isEqualTo(debtId);

    // And: debt D2 is written down by 600 DKK (the actual paid amount)
    assertThat(result.getWriteDownAmount()).isEqualByComparingTo("600");
    verify(debtServiceClient).writeDown(debtId, new BigDecimal("600"));

    // And: bookkeeping records the full paid amount
    verify(bookkeepingService)
        .recordPaymentReceived(
            eq(debtId), eq(new BigDecimal("600")), any(LocalDate.class), any(String.class));

    // And: remaining is 400 (1000 - 600) - verified via write-down call
    assertThat(result.getExcessAmount()).isEqualByComparingTo("0");
  }

  // =========================================================================
  // Scenario 3: Payment without unique OCR match → manual matching
  // =========================================================================

  @Test
  void noUniqueOcrMatch_routesToManualMatching_whenNoDebtsFound() {
    // Given: OCR line does not match any debt
    when(debtServiceClient.findByOcrLine("OCR-UNKNOWN")).thenReturn(List.of());
    stubPaymentRepositorySave();

    IncomingPaymentDto incoming = incomingPayment("OCR-UNKNOWN", "500");

    // When
    PaymentMatchResult result = service.processIncomingPayment(incoming);

    // Then
    assertThat(result.isAutoMatched()).isFalse();
    assertThat(result.isRoutedToManualMatching()).isTrue();
    assertThat(result.getMatchedDebtId()).isNull();
  }

  @Test
  void noUniqueOcrMatch_routesToManualMatching_whenMultipleDebtsFound() {
    // Given: OCR line matches multiple debts (not unique)
    DebtDto debt1 = DebtDto.builder().id(UUID.randomUUID()).ocrLine("OCR-DUP").build();
    DebtDto debt2 = DebtDto.builder().id(UUID.randomUUID()).ocrLine("OCR-DUP").build();
    when(debtServiceClient.findByOcrLine("OCR-DUP")).thenReturn(List.of(debt1, debt2));
    stubPaymentRepositorySave();

    IncomingPaymentDto incoming = incomingPayment("OCR-DUP", "500");

    // When
    PaymentMatchResult result = service.processIncomingPayment(incoming);

    // Then
    assertThat(result.isAutoMatched()).isFalse();
    assertThat(result.isRoutedToManualMatching()).isTrue();
  }

  @Test
  void noUniqueOcrMatch_routesToManualMatching_whenOcrLineIsNull() {
    // Given: incoming payment has no OCR-linje
    stubPaymentRepositorySave();

    IncomingPaymentDto incoming =
        IncomingPaymentDto.builder()
            .ocrLine(null)
            .amount(new BigDecimal("500"))
            .valueDate(LocalDate.of(2025, 12, 1))
            .cremulReference("CREMUL-003")
            .build();

    // When
    PaymentMatchResult result = service.processIncomingPayment(incoming);

    // Then
    assertThat(result.isAutoMatched()).isFalse();
    assertThat(result.isRoutedToManualMatching()).isTrue();
    // No call to debt service when OCR is null
    verifyNoInteractions(debtServiceClient);
  }

  // =========================================================================
  // Scenario 4: Overpayment - payout outcome
  // =========================================================================

  @Test
  void overpayment_rulesResolveToPayout() {
    // Given: OCR-789 uniquely identifies debt D3 with outstanding balance 1000
    UUID debtId = UUID.randomUUID();
    DebtDto debt =
        DebtDto.builder()
            .id(debtId)
            .ocrLine("OCR-789")
            .outstandingBalance(new BigDecimal("1000"))
            .build();
    when(debtServiceClient.findByOcrLine("OCR-789")).thenReturn(List.of(debt));
    when(debtServiceClient.writeDown(any(), any())).thenReturn(debt);
    stubPaymentRepositorySave();

    // And: rules resolve excess amount outcome to "payout"
    when(overpaymentRulesService.resolveOutcome(debtId)).thenReturn(OverpaymentOutcome.PAYOUT);

    // And: incoming payment of 1400 (overpayment of 400)
    IncomingPaymentDto incoming = incomingPayment("OCR-789", "1400");

    // When
    PaymentMatchResult result = service.processIncomingPayment(incoming);

    // Then: the payment is auto-matched (not routed to manual despite overpayment)
    assertThat(result.isAutoMatched()).isTrue();
    assertThat(result.getMatchedDebtId()).isEqualTo(debtId);
    assertThat(result.isRoutedToManualMatching()).isFalse();

    // And: debt is written down by the outstanding balance (1000)
    verify(debtServiceClient).writeDown(debtId, new BigDecimal("1000"));

    // And: excess amount outcome is payout
    assertThat(result.getExcessAmount()).isEqualByComparingTo("400");
    assertThat(result.getExcessOutcome()).isEqualTo(OverpaymentOutcome.PAYOUT);

    // And: bookkeeping records the full paid amount (1400)
    verify(bookkeepingService)
        .recordPaymentReceived(
            eq(debtId), eq(new BigDecimal("1400")), any(LocalDate.class), any(String.class));
  }

  // =========================================================================
  // Scenario 5: Overpayment - use to cover other debt posts
  // =========================================================================

  @Test
  void overpayment_rulesResolveToCoverOtherDebts() {
    // Given: OCR-789 uniquely identifies debt D3 with outstanding balance 1000
    UUID debtId = UUID.randomUUID();
    DebtDto debt =
        DebtDto.builder()
            .id(debtId)
            .ocrLine("OCR-789")
            .outstandingBalance(new BigDecimal("1000"))
            .build();
    when(debtServiceClient.findByOcrLine("OCR-789")).thenReturn(List.of(debt));
    when(debtServiceClient.writeDown(any(), any())).thenReturn(debt);
    stubPaymentRepositorySave();

    // And: rules resolve excess amount outcome to "use to cover other debt posts"
    when(overpaymentRulesService.resolveOutcome(debtId))
        .thenReturn(OverpaymentOutcome.COVER_OTHER_DEBTS);

    // And: incoming payment of 1400 (overpayment of 400)
    IncomingPaymentDto incoming = incomingPayment("OCR-789", "1400");

    // When
    PaymentMatchResult result = service.processIncomingPayment(incoming);

    // Then: the payment is auto-matched
    assertThat(result.isAutoMatched()).isTrue();
    assertThat(result.getMatchedDebtId()).isEqualTo(debtId);

    // And: excess amount outcome is cover other debt posts
    assertThat(result.getExcessAmount()).isEqualByComparingTo("400");
    assertThat(result.getExcessOutcome()).isEqualTo(OverpaymentOutcome.COVER_OTHER_DEBTS);
  }

  // =========================================================================
  // Additional edge case: exact payment (no excess, no underpayment)
  // =========================================================================

  @Test
  void exactPayment_noExcessNoUnderpayment() {
    UUID debtId = UUID.randomUUID();
    DebtDto debt =
        DebtDto.builder()
            .id(debtId)
            .ocrLine("OCR-EXACT")
            .outstandingBalance(new BigDecimal("1000"))
            .build();
    when(debtServiceClient.findByOcrLine("OCR-EXACT")).thenReturn(List.of(debt));
    when(debtServiceClient.writeDown(any(), any())).thenReturn(debt);
    stubPaymentRepositorySave();

    IncomingPaymentDto incoming = incomingPayment("OCR-EXACT", "1000");

    PaymentMatchResult result = service.processIncomingPayment(incoming);

    assertThat(result.isAutoMatched()).isTrue();
    assertThat(result.getWriteDownAmount()).isEqualByComparingTo("1000");
    assertThat(result.getExcessAmount()).isEqualByComparingTo("0");
    assertThat(result.getExcessOutcome()).isNull();
    verify(debtServiceClient).writeDown(debtId, new BigDecimal("1000"));
    verifyNoInteractions(overpaymentRulesService);
  }

  // =========================================================================
  // Verify payment record creation
  // =========================================================================

  @Test
  void autoMatch_createsCompletedPaymentRecord() {
    UUID debtId = UUID.randomUUID();
    DebtDto debt =
        DebtDto.builder()
            .id(debtId)
            .ocrLine("OCR-REC")
            .outstandingBalance(new BigDecimal("1000"))
            .build();
    when(debtServiceClient.findByOcrLine("OCR-REC")).thenReturn(List.of(debt));
    when(debtServiceClient.writeDown(any(), any())).thenReturn(debt);
    stubPaymentRepositorySave();

    IncomingPaymentDto incoming = incomingPayment("OCR-REC", "800");

    service.processIncomingPayment(incoming);

    ArgumentCaptor<PaymentEntity> captor = ArgumentCaptor.forClass(PaymentEntity.class);
    verify(paymentRepository).save(captor.capture());
    PaymentEntity saved = captor.getValue();

    assertThat(saved.getDebtId()).isEqualTo(debtId);
    assertThat(saved.getAmount()).isEqualByComparingTo("800");
    assertThat(saved.getStatus()).isEqualTo(PaymentEntity.PaymentStatus.COMPLETED);
    assertThat(saved.getPaymentMethod()).isEqualTo(PaymentEntity.PaymentMethod.BANK_TRANSFER);
    assertThat(saved.getOcrLine()).isEqualTo("OCR-REC");
    assertThat(saved.getTransactionReference()).isEqualTo("CREMUL-001");
  }

  @Test
  void manualMatch_createsPendingPaymentRecord() {
    when(debtServiceClient.findByOcrLine("OCR-NONE")).thenReturn(List.of());
    stubPaymentRepositorySave();

    IncomingPaymentDto incoming = incomingPayment("OCR-NONE", "500");

    service.processIncomingPayment(incoming);

    ArgumentCaptor<PaymentEntity> captor = ArgumentCaptor.forClass(PaymentEntity.class);
    verify(paymentRepository).save(captor.capture());
    PaymentEntity saved = captor.getValue();

    assertThat(saved.getDebtId()).isNull();
    assertThat(saved.getStatus()).isEqualTo(PaymentEntity.PaymentStatus.PENDING);
    assertThat(saved.getOcrLine()).isEqualTo("OCR-NONE");
  }

  // =========================================================================
  // Helpers
  // =========================================================================

  private void stubPaymentRepositorySave() {
    when(paymentRepository.save(any(PaymentEntity.class)))
        .thenAnswer(
            invocation -> {
              PaymentEntity entity = invocation.getArgument(0);
              if (entity.getId() == null) {
                entity.setId(UUID.randomUUID());
              }
              return entity;
            });
  }

  private IncomingPaymentDto incomingPayment(String ocrLine, String amount) {
    return IncomingPaymentDto.builder()
        .ocrLine(ocrLine)
        .amount(new BigDecimal(amount))
        .valueDate(LocalDate.of(2025, 12, 1))
        .cremulReference("CREMUL-001")
        .build();
  }
}
