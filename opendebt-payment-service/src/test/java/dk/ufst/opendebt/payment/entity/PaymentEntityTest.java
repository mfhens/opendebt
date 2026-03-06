package dk.ufst.opendebt.payment.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class PaymentEntityTest {

  @Test
  void builderAndAccessorsRoundTripAllFields() {
    UUID paymentId = UUID.randomUUID();
    UUID caseId = UUID.randomUUID();
    UUID debtId = UUID.randomUUID();
    LocalDateTime paymentDate = LocalDateTime.of(2026, 3, 6, 12, 0);
    LocalDateTime processedAt = LocalDateTime.of(2026, 3, 6, 12, 5);

    PaymentEntity entity =
        PaymentEntity.builder()
            .id(paymentId)
            .caseId(caseId)
            .debtId(debtId)
            .amount(new BigDecimal("100"))
            .paymentMethod(PaymentEntity.PaymentMethod.BANK_TRANSFER)
            .status(PaymentEntity.PaymentStatus.COMPLETED)
            .transactionReference("TX-1")
            .externalPaymentId("EXT-1")
            .ocrLine("OCR-123")
            .paymentDate(paymentDate)
            .processedAt(processedAt)
            .processedBy("system")
            .failureReason("none")
            .createdAt(paymentDate)
            .updatedAt(processedAt)
            .version(2L)
            .build();

    assertThat(entity.getId()).isEqualTo(paymentId);
    assertThat(entity.getCaseId()).isEqualTo(caseId);
    assertThat(entity.getDebtId()).isEqualTo(debtId);
    assertThat(entity.getAmount()).isEqualByComparingTo("100");
    assertThat(entity.getPaymentMethod()).isEqualTo(PaymentEntity.PaymentMethod.BANK_TRANSFER);
    assertThat(entity.getStatus()).isEqualTo(PaymentEntity.PaymentStatus.COMPLETED);
    assertThat(entity.getTransactionReference()).isEqualTo("TX-1");
    assertThat(entity.getExternalPaymentId()).isEqualTo("EXT-1");
    assertThat(entity.getOcrLine()).isEqualTo("OCR-123");
    assertThat(entity.getPaymentDate()).isEqualTo(paymentDate);
    assertThat(entity.getProcessedAt()).isEqualTo(processedAt);
    assertThat(entity.getProcessedBy()).isEqualTo("system");
    assertThat(entity.getFailureReason()).isEqualTo("none");
    assertThat(entity.getVersion()).isEqualTo(2L);
  }

  @Test
  void constructorsAndSettersWork() {
    PaymentEntity entity = new PaymentEntity();
    entity.setId(UUID.randomUUID());
    entity.setCaseId(UUID.randomUUID());
    entity.setDebtId(UUID.randomUUID());
    entity.setAmount(new BigDecimal("250"));
    entity.setPaymentMethod(PaymentEntity.PaymentMethod.DIRECT_DEBIT);
    entity.setStatus(PaymentEntity.PaymentStatus.PENDING);
    entity.setTransactionReference("TX-2");
    entity.setExternalPaymentId("EXT-2");
    entity.setOcrLine("OCR-456");
    entity.setProcessedBy("worker");
    entity.setFailureReason("timeout");

    PaymentEntity copied =
        new PaymentEntity(
            entity.getId(),
            entity.getCaseId(),
            entity.getDebtId(),
            entity.getAmount(),
            entity.getPaymentMethod(),
            entity.getStatus(),
            entity.getTransactionReference(),
            entity.getExternalPaymentId(),
            entity.getOcrLine(),
            entity.getPaymentDate(),
            entity.getProcessedAt(),
            entity.getProcessedBy(),
            entity.getFailureReason(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getVersion());

    assertThat(copied.getAmount()).isEqualByComparingTo("250");
    assertThat(copied.getPaymentMethod()).isEqualTo(PaymentEntity.PaymentMethod.DIRECT_DEBIT);
    assertThat(copied.getStatus()).isEqualTo(PaymentEntity.PaymentStatus.PENDING);
    assertThat(copied.getFailureReason()).isEqualTo("timeout");
  }
}
