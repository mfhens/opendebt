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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dk.ufst.opendebt.common.dto.DebtDto;
import dk.ufst.opendebt.common.exception.OpenDebtException;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;

@ExtendWith(MockitoExtension.class)
class ReadinessValidationServiceImplTest {

  @Mock private DebtRepository debtRepository;
  @Mock private DebtServiceImpl debtService;

  private ReadinessValidationServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new ReadinessValidationServiceImpl(debtRepository, debtService);
  }

  @Test
  void validateReadiness_marksDebtReadyWhenMandatoryDataExists() {
    UUID debtId = UUID.randomUUID();
    DebtEntity entity = debtEntity(debtId);
    DebtDto expected =
        DebtDto.builder()
            .id(debtId)
            .readinessStatus(DebtDto.ReadinessStatus.READY_FOR_COLLECTION)
            .build();
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(entity));
    when(debtRepository.save(any(DebtEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(debtService.getDebtById(debtId)).thenReturn(expected);

    DebtDto result = service.validateReadiness(debtId);

    assertThat(result).isSameAs(expected);
    assertThat(entity.getReadinessStatus())
        .isEqualTo(DebtEntity.ReadinessStatus.READY_FOR_COLLECTION);
    assertThat(entity.getReadinessValidatedAt()).isNotNull();
    verify(debtRepository).save(entity);
  }

  @Test
  void validateReadiness_marksDebtNotReadyWhenMandatoryDataMissing() {
    UUID debtId = UUID.randomUUID();
    DebtEntity entity = debtEntity(debtId);
    entity.setDueDate(null);
    DebtDto expected =
        DebtDto.builder().id(debtId).readinessStatus(DebtDto.ReadinessStatus.NOT_READY).build();
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(entity));
    when(debtRepository.save(any(DebtEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(debtService.getDebtById(debtId)).thenReturn(expected);

    DebtDto result = service.validateReadiness(debtId);

    assertThat(result).isSameAs(expected);
    assertThat(entity.getReadinessStatus()).isEqualTo(DebtEntity.ReadinessStatus.NOT_READY);
    assertThat(entity.getReadinessRejectionReason()).isEqualTo("Missing mandatory claim data");
  }

  @Test
  void approveReadiness_marksDebtReady() {
    UUID debtId = UUID.randomUUID();
    DebtEntity entity = debtEntity(debtId);
    DebtDto expected = DebtDto.builder().id(debtId).build();
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(entity));
    when(debtRepository.save(any(DebtEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(debtService.getDebtById(debtId)).thenReturn(expected);

    DebtDto result = service.approveReadiness(debtId);

    assertThat(result).isSameAs(expected);
    assertThat(entity.getReadinessStatus())
        .isEqualTo(DebtEntity.ReadinessStatus.READY_FOR_COLLECTION);
    assertThat(entity.getReadinessValidatedAt()).isNotNull();
  }

  @Test
  void rejectReadiness_marksDebtNotReadyWithReason() {
    UUID debtId = UUID.randomUUID();
    DebtEntity entity = debtEntity(debtId);
    DebtDto expected = DebtDto.builder().id(debtId).build();
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(entity));
    when(debtRepository.save(any(DebtEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(debtService.getDebtById(debtId)).thenReturn(expected);

    DebtDto result = service.rejectReadiness(debtId, "Appeal pending");

    assertThat(result).isSameAs(expected);
    assertThat(entity.getReadinessStatus()).isEqualTo(DebtEntity.ReadinessStatus.NOT_READY);
    assertThat(entity.getReadinessRejectionReason()).isEqualTo("Appeal pending");
    assertThat(entity.getReadinessValidatedAt()).isNotNull();
  }

  @Test
  void validateBatchReadiness_validatesEachPendingDebt() {
    UUID creditorId = UUID.randomUUID();
    DebtEntity first = debtEntity(UUID.randomUUID());
    DebtEntity second = debtEntity(UUID.randomUUID());
    when(debtRepository.findByCreditorOrgIdAndReadinessStatus(
            creditorId, DebtEntity.ReadinessStatus.PENDING_REVIEW))
        .thenReturn(List.of(first, second));
    when(debtRepository.findById(first.getId())).thenReturn(Optional.of(first));
    when(debtRepository.findById(second.getId())).thenReturn(Optional.of(second));
    when(debtRepository.save(any(DebtEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(debtService.getDebtById(any(UUID.class))).thenReturn(DebtDto.builder().build());

    int validated = service.validateBatchReadiness(creditorId.toString());

    assertThat(validated).isEqualTo(2);
    assertThat(first.getReadinessStatus())
        .isEqualTo(DebtEntity.ReadinessStatus.READY_FOR_COLLECTION);
    assertThat(second.getReadinessStatus())
        .isEqualTo(DebtEntity.ReadinessStatus.READY_FOR_COLLECTION);
    verify(debtRepository, times(2)).save(any(DebtEntity.class));
  }

  @Test
  void validateReadiness_throwsWhenDebtDoesNotExist() {
    UUID debtId = UUID.randomUUID();
    when(debtRepository.findById(debtId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.validateReadiness(debtId))
        .isInstanceOf(OpenDebtException.class)
        .hasMessageContaining("Debt not found");
  }

  private DebtEntity debtEntity(UUID debtId) {
    return DebtEntity.builder()
        .id(debtId)
        .debtorPersonId(UUID.randomUUID())
        .creditorOrgId(UUID.randomUUID())
        .debtTypeCode("600")
        .principalAmount(new BigDecimal("1000"))
        .dueDate(LocalDate.of(2026, 4, 1))
        .outstandingBalance(new BigDecimal("1000"))
        .status(DebtEntity.DebtStatus.ACTIVE)
        .readinessStatus(DebtEntity.ReadinessStatus.PENDING_REVIEW)
        .build();
  }
}
