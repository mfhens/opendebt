package dk.ufst.opendebt.debtservice.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class DebtEntityTest {

  @Test
  void builderAndAccessorsRoundTripAllFields() {
    UUID debtId = UUID.randomUUID();
    UUID debtorId = UUID.randomUUID();
    UUID creditorId = UUID.randomUUID();
    LocalDateTime createdAt = LocalDateTime.of(2026, 3, 6, 12, 0);
    LocalDateTime updatedAt = LocalDateTime.of(2026, 3, 6, 12, 30);

    DebtEntity entity =
        DebtEntity.builder()
            .id(debtId)
            .debtorPersonId(debtorId)
            .creditorOrgId(creditorId)
            .debtTypeCode("600")
            .principalAmount(new BigDecimal("1000"))
            .interestAmount(new BigDecimal("50"))
            .feesAmount(new BigDecimal("25"))
            .dueDate(LocalDate.of(2026, 4, 1))
            .originalDueDate(LocalDate.of(2026, 3, 1))
            .externalReference("EXT-1")
            .ocrLine("OCR-123")
            .outstandingBalance(new BigDecimal("1075"))
            .status(DebtEntity.DebtStatus.ACTIVE)
            .readinessStatus(DebtEntity.ReadinessStatus.PENDING_REVIEW)
            .readinessRejectionReason("reason")
            .readinessValidatedAt(createdAt)
            .readinessValidatedBy("worker")
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .createdBy("system")
            .version(3L)
            .build();

    assertThat(entity.getId()).isEqualTo(debtId);
    assertThat(entity.getDebtorPersonId()).isEqualTo(debtorId);
    assertThat(entity.getCreditorOrgId()).isEqualTo(creditorId);
    assertThat(entity.getDebtTypeCode()).isEqualTo("600");
    assertThat(entity.getInterestAmount()).isEqualByComparingTo("50");
    assertThat(entity.getFeesAmount()).isEqualByComparingTo("25");
    assertThat(entity.getOriginalDueDate()).isEqualTo(LocalDate.of(2026, 3, 1));
    assertThat(entity.getExternalReference()).isEqualTo("EXT-1");
    assertThat(entity.getOcrLine()).isEqualTo("OCR-123");
    assertThat(entity.getOutstandingBalance()).isEqualByComparingTo("1075");
    assertThat(entity.getStatus()).isEqualTo(DebtEntity.DebtStatus.ACTIVE);
    assertThat(entity.getReadinessStatus()).isEqualTo(DebtEntity.ReadinessStatus.PENDING_REVIEW);
    assertThat(entity.getReadinessRejectionReason()).isEqualTo("reason");
    assertThat(entity.getReadinessValidatedAt()).isEqualTo(createdAt);
    assertThat(entity.getReadinessValidatedBy()).isEqualTo("worker");
    assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
    assertThat(entity.getUpdatedAt()).isEqualTo(updatedAt);
    assertThat(entity.getCreatedBy()).isEqualTo("system");
    assertThat(entity.getVersion()).isEqualTo(3L);
  }

  @Test
  void constructorsAndSettersWork() {
    DebtEntity entity = new DebtEntity();
    entity.setId(UUID.randomUUID());
    entity.setDebtorPersonId(UUID.randomUUID());
    entity.setCreditorOrgId(UUID.randomUUID());
    entity.setDebtTypeCode("700");
    entity.setPrincipalAmount(new BigDecimal("500"));
    entity.setInterestAmount(new BigDecimal("20"));
    entity.setFeesAmount(new BigDecimal("5"));
    entity.setDueDate(LocalDate.of(2026, 5, 1));
    entity.setOutstandingBalance(new BigDecimal("525"));
    entity.setStatus(DebtEntity.DebtStatus.PARTIALLY_PAID);
    entity.setReadinessStatus(DebtEntity.ReadinessStatus.NOT_READY);

    DebtEntity copied =
        new DebtEntity(
            entity.getId(),
            entity.getDebtorPersonId(),
            entity.getCreditorOrgId(),
            entity.getDebtTypeCode(),
            entity.getPrincipalAmount(),
            entity.getInterestAmount(),
            entity.getFeesAmount(),
            entity.getDueDate(),
            entity.getOriginalDueDate(),
            entity.getExternalReference(),
            entity.getOcrLine(),
            entity.getOutstandingBalance(),
            entity.getStatus(),
            entity.getReadinessStatus(),
            entity.getReadinessRejectionReason(),
            entity.getReadinessValidatedAt(),
            entity.getReadinessValidatedBy(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getCreatedBy(),
            entity.getVersion());

    assertThat(copied.getDebtTypeCode()).isEqualTo("700");
    assertThat(copied.getOutstandingBalance()).isEqualByComparingTo("525");
    assertThat(copied.getStatus()).isEqualTo(DebtEntity.DebtStatus.PARTIALLY_PAID);
    assertThat(copied.getReadinessStatus()).isEqualTo(DebtEntity.ReadinessStatus.NOT_READY);
  }

  @Test
  void getTotalAmountHandlesPresentAndMissingValues() {
    DebtEntity fullAmounts = new DebtEntity();
    fullAmounts.setPrincipalAmount(new BigDecimal("100"));
    fullAmounts.setInterestAmount(new BigDecimal("10"));
    fullAmounts.setFeesAmount(new BigDecimal("5"));

    DebtEntity missingAmounts = new DebtEntity();
    missingAmounts.setPrincipalAmount(null);
    missingAmounts.setInterestAmount(null);
    missingAmounts.setFeesAmount(null);

    assertThat(fullAmounts.getTotalAmount()).isEqualByComparingTo("115");
    assertThat(missingAmounts.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
  }
}
