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
import org.springframework.beans.factory.ObjectProvider;

import dk.ufst.opendebt.common.dto.DebtDto;
import dk.ufst.opendebt.common.exception.OpenDebtException;
import dk.ufst.opendebt.debtservice.dto.ClaimValidationResult;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;
import dk.ufst.opendebt.debtservice.service.ClaimValidationService;
import dk.ufst.opendebt.debtservice.service.ReadinessValidationService;

@ExtendWith(MockitoExtension.class)
class ReadinessValidationServiceImplTest {

  @Mock private DebtRepository debtRepository;
  @Mock private DebtServiceImpl debtService;
  @Mock private ClaimValidationService claimValidationService;
  @Mock private ObjectProvider<ReadinessValidationService> readinessValidationServiceProvider;

  private ReadinessValidationServiceImpl service;

  @BeforeEach
  void setUp() {
    service =
        new ReadinessValidationServiceImpl(
            debtRepository,
            debtService,
            claimValidationService,
            readinessValidationServiceProvider);
  }

  @Test
  void validateReadiness_marksDebtReadyWhenValidationPasses() {
    UUID debtId = UUID.randomUUID();
    DebtEntity entity = debtEntity(debtId);
    DebtDto dto = DebtDto.builder().id(debtId).build();
    DebtDto expected =
        DebtDto.builder()
            .id(debtId)
            .readinessStatus(DebtDto.ReadinessStatus.READY_FOR_COLLECTION)
            .build();
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(entity));
    when(debtRepository.save(any(DebtEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(debtService.getDebtById(debtId)).thenReturn(dto, expected);
    when(claimValidationService.validate(dto)).thenReturn(ClaimValidationResult.builder().build());

    DebtDto result = service.validateReadiness(debtId);

    assertThat(result).isSameAs(expected);
    assertThat(entity.getReadinessStatus())
        .isEqualTo(DebtEntity.ReadinessStatus.READY_FOR_COLLECTION);
    assertThat(entity.getReadinessValidatedAt()).isNotNull();
    verify(debtRepository).save(entity);
  }

  @Test
  void validateReadiness_marksDebtNotReadyWhenValidationFails() {
    UUID debtId = UUID.randomUUID();
    DebtEntity entity = debtEntity(debtId);
    DebtDto dto = DebtDto.builder().id(debtId).build();
    DebtDto expected =
        DebtDto.builder().id(debtId).readinessStatus(DebtDto.ReadinessStatus.NOT_READY).build();
    ClaimValidationResult failResult =
        ClaimValidationResult.builder()
            .errors(
                List.of(
                    ClaimValidationResult.ValidationError.builder()
                        .ruleId("R1")
                        .errorCode("MISSING_DUE_DATE")
                        .description("Due date is required")
                        .build()))
            .build();
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(entity));
    when(debtRepository.save(any(DebtEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(debtService.getDebtById(debtId)).thenReturn(dto, expected);
    when(claimValidationService.validate(dto)).thenReturn(failResult);

    DebtDto result = service.validateReadiness(debtId);

    assertThat(result).isSameAs(expected);
    assertThat(entity.getReadinessStatus()).isEqualTo(DebtEntity.ReadinessStatus.NOT_READY);
    assertThat(entity.getReadinessRejectionReason()).contains("MISSING_DUE_DATE");
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
    when(readinessValidationServiceProvider.getObject()).thenReturn(service);
    when(debtRepository.findByCreditorOrgIdAndReadinessStatus(
            creditorId, DebtEntity.ReadinessStatus.PENDING_REVIEW))
        .thenReturn(List.of(first, second));
    when(debtRepository.findById(first.getId())).thenReturn(Optional.of(first));
    when(debtRepository.findById(second.getId())).thenReturn(Optional.of(second));
    when(debtRepository.save(any(DebtEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(debtService.getDebtById(any(UUID.class))).thenReturn(DebtDto.builder().build());
    when(claimValidationService.validate(any(DebtDto.class)))
        .thenReturn(ClaimValidationResult.builder().build());

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
