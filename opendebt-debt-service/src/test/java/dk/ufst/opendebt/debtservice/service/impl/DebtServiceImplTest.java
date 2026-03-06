package dk.ufst.opendebt.debtservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dk.ufst.opendebt.common.dto.DebtDto;
import dk.ufst.opendebt.common.exception.OpenDebtException;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;

@ExtendWith(MockitoExtension.class)
class DebtServiceImplTest {

  @Mock private DebtRepository debtRepository;

  private DebtServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new DebtServiceImpl(debtRepository);
  }

  // =========================================================================
  // findByOcrLine - petition001 requirement
  // =========================================================================

  @Test
  void findByOcrLine_returnsMatchingDebts() {
    DebtEntity entity = debtEntity("OCR-123", new BigDecimal("1000"));
    when(debtRepository.findByOcrLine("OCR-123")).thenReturn(List.of(entity));

    List<DebtDto> result = service.findByOcrLine("OCR-123");

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getOcrLine()).isEqualTo("OCR-123");
    assertThat(result.get(0).getOutstandingBalance()).isEqualByComparingTo("1000");
  }

  @Test
  void findByOcrLine_returnsEmptyWhenNoMatch() {
    when(debtRepository.findByOcrLine("OCR-UNKNOWN")).thenReturn(List.of());

    List<DebtDto> result = service.findByOcrLine("OCR-UNKNOWN");

    assertThat(result).isEmpty();
  }

  @Test
  void findByOcrLine_returnsMultipleWhenNotUnique() {
    DebtEntity e1 = debtEntity("OCR-DUP", new BigDecimal("500"));
    DebtEntity e2 = debtEntity("OCR-DUP", new BigDecimal("700"));
    when(debtRepository.findByOcrLine("OCR-DUP")).thenReturn(List.of(e1, e2));

    List<DebtDto> result = service.findByOcrLine("OCR-DUP");

    assertThat(result).hasSize(2);
  }

  // =========================================================================
  // writeDown - petition001 requirement
  // =========================================================================

  @Test
  void writeDown_reducesOutstandingBalance() {
    UUID debtId = UUID.randomUUID();
    DebtEntity entity = debtEntity("OCR-456", new BigDecimal("1000"));
    entity.setId(debtId);
    entity.setStatus(DebtEntity.DebtStatus.ACTIVE);
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(entity));
    when(debtRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    DebtDto result = service.writeDown(debtId, new BigDecimal("600"));

    assertThat(result.getOutstandingBalance()).isEqualByComparingTo("400");
    assertThat(result.getStatus()).isEqualTo(DebtDto.DebtStatus.PARTIALLY_PAID);
  }

  @Test
  void writeDown_setsStatusToPaidWhenFullyPaid() {
    UUID debtId = UUID.randomUUID();
    DebtEntity entity = debtEntity("OCR-789", new BigDecimal("1000"));
    entity.setId(debtId);
    entity.setStatus(DebtEntity.DebtStatus.ACTIVE);
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(entity));
    when(debtRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    DebtDto result = service.writeDown(debtId, new BigDecimal("1000"));

    assertThat(result.getOutstandingBalance()).isEqualByComparingTo("0");
    assertThat(result.getStatus()).isEqualTo(DebtDto.DebtStatus.PAID);
  }

  @Test
  void writeDown_clampsToZeroWhenOverpaying() {
    UUID debtId = UUID.randomUUID();
    DebtEntity entity = debtEntity("OCR-OVER", new BigDecimal("1000"));
    entity.setId(debtId);
    entity.setStatus(DebtEntity.DebtStatus.ACTIVE);
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(entity));
    when(debtRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    DebtDto result = service.writeDown(debtId, new BigDecimal("1500"));

    assertThat(result.getOutstandingBalance()).isEqualByComparingTo("0");
    assertThat(result.getStatus()).isEqualTo(DebtDto.DebtStatus.PAID);
  }

  @Test
  void writeDown_throwsWhenDebtNotFound() {
    UUID debtId = UUID.randomUUID();
    when(debtRepository.findById(debtId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.writeDown(debtId, new BigDecimal("100")))
        .isInstanceOf(OpenDebtException.class)
        .hasMessageContaining("Debt not found");
  }

  @Test
  void writeDown_persistsEntity() {
    UUID debtId = UUID.randomUUID();
    DebtEntity entity = debtEntity("OCR-SAVE", new BigDecimal("1000"));
    entity.setId(debtId);
    entity.setStatus(DebtEntity.DebtStatus.ACTIVE);
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(entity));
    when(debtRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    service.writeDown(debtId, new BigDecimal("300"));

    ArgumentCaptor<DebtEntity> captor = ArgumentCaptor.forClass(DebtEntity.class);
    verify(debtRepository).save(captor.capture());
    assertThat(captor.getValue().getOutstandingBalance()).isEqualByComparingTo("700");
  }

  // =========================================================================
  // Helpers
  // =========================================================================

  private DebtEntity debtEntity(String ocrLine, BigDecimal outstandingBalance) {
    return DebtEntity.builder()
        .id(UUID.randomUUID())
        .debtorPersonId(UUID.randomUUID())
        .creditorOrgId(UUID.randomUUID())
        .debtTypeCode("600")
        .principalAmount(outstandingBalance)
        .dueDate(LocalDate.of(2025, 12, 1))
        .ocrLine(ocrLine)
        .outstandingBalance(outstandingBalance)
        .status(DebtEntity.DebtStatus.ACTIVE)
        .readinessStatus(DebtEntity.ReadinessStatus.READY_FOR_COLLECTION)
        .build();
  }
}
