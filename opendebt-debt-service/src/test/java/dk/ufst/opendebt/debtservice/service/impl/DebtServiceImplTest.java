package dk.ufst.opendebt.debtservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import dk.ufst.opendebt.common.dto.DebtDto;
import dk.ufst.opendebt.common.exception.OpenDebtException;
import dk.ufst.opendebt.debtservice.client.CreditorServiceClient;
import dk.ufst.opendebt.debtservice.client.ValidateActionRequest;
import dk.ufst.opendebt.debtservice.client.ValidateActionResponse;
import dk.ufst.opendebt.debtservice.config.FordringMetrics;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.exception.CreditorValidationException;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
class DebtServiceImplTest {

  @Mock private DebtRepository debtRepository;
  @Mock private CreditorServiceClient creditorServiceClient;

  private DebtServiceImpl service;

  @BeforeEach
  void setUp() {
    FordringMetrics fordringMetrics = new FordringMetrics(new SimpleMeterRegistry());
    service = new DebtServiceImpl(debtRepository, creditorServiceClient, fordringMetrics);
  }

  @Test
  void listDebts_mapsAllFiltersAndResults() {
    UUID creditorId = UUID.randomUUID();
    UUID debtorId = UUID.randomUUID();
    Pageable pageable = PageRequest.of(0, 10);
    DebtEntity entity = debtEntity("OCR-LIST", new BigDecimal("1000"));
    entity.setCreditorOrgId(creditorId);
    entity.setDebtorPersonId(debtorId);
    when(debtRepository.findByFilters(
            creditorId,
            debtorId,
            DebtEntity.DebtStatus.ACTIVE,
            DebtEntity.ReadinessStatus.READY_FOR_COLLECTION,
            pageable))
        .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));

    Page<DebtDto> result =
        service.listDebts(
            creditorId.toString(),
            debtorId.toString(),
            DebtDto.DebtStatus.ACTIVE,
            DebtDto.ReadinessStatus.READY_FOR_COLLECTION,
            pageable);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getCreditorId()).isEqualTo(creditorId.toString());
    assertThat(result.getContent().get(0).getDebtorId()).isEqualTo(debtorId.toString());
  }

  @Test
  void listDebts_allowsNullFilters() {
    Pageable pageable = PageRequest.of(0, 5);
    when(debtRepository.findByFilters(null, null, null, null, pageable))
        .thenReturn(Page.empty(pageable));

    Page<DebtDto> result = service.listDebts(null, null, null, null, pageable);

    assertThat(result.getContent()).isEmpty();
    verify(debtRepository).findByFilters(null, null, null, null, pageable);
  }

  @Test
  void getDebtById_returnsMappedDebt() {
    UUID debtId = UUID.randomUUID();
    DebtEntity entity = debtEntity("OCR-ID", new BigDecimal("1200"));
    entity.setId(debtId);
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(entity));

    DebtDto result = service.getDebtById(debtId);

    assertThat(result.getId()).isEqualTo(debtId);
    assertThat(result.getOcrLine()).isEqualTo("OCR-ID");
  }

  @Test
  void getDebtById_mapsNullOptionalFieldsToNull() {
    UUID debtId = UUID.randomUUID();
    DebtEntity entity = debtEntity("OCR-NULL", new BigDecimal("200"));
    entity.setId(debtId);
    entity.setDebtorPersonId(null);
    entity.setCreditorOrgId(null);
    entity.setStatus(null);
    entity.setReadinessStatus(null);
    entity.setCreatedAt(LocalDateTime.of(2026, 3, 6, 12, 0));
    entity.setUpdatedAt(LocalDateTime.of(2026, 3, 6, 12, 30));
    entity.setCreatedBy("caseworker");
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(entity));

    DebtDto result = service.getDebtById(debtId);

    assertThat(result.getDebtorId()).isNull();
    assertThat(result.getCreditorId()).isNull();
    assertThat(result.getStatus()).isNull();
    assertThat(result.getReadinessStatus()).isNull();
    assertThat(result.getCreatedBy()).isEqualTo("caseworker");
  }

  @Test
  void getDebtsByDebtor_parsesUuidAndMapsResults() {
    UUID debtorId = UUID.randomUUID();
    DebtEntity entity = debtEntity("OCR-DEBTOR", new BigDecimal("300"));
    entity.setDebtorPersonId(debtorId);
    when(debtRepository.findByDebtorPersonId(debtorId)).thenReturn(List.of(entity));

    List<DebtDto> result = service.getDebtsByDebtor(debtorId.toString());

    assertThat(result)
        .singleElement()
        .extracting(DebtDto::getDebtorId)
        .isEqualTo(debtorId.toString());
  }

  @Test
  void getDebtsByCreditor_parsesUuidAndMapsResults() {
    UUID creditorId = UUID.randomUUID();
    DebtEntity entity = debtEntity("OCR-CREDITOR", new BigDecimal("300"));
    entity.setCreditorOrgId(creditorId);
    when(debtRepository.findByCreditorOrgId(creditorId)).thenReturn(List.of(entity));

    List<DebtDto> result = service.getDebtsByCreditor(creditorId.toString());

    assertThat(result)
        .singleElement()
        .extracting(DebtDto::getCreditorId)
        .isEqualTo(creditorId.toString());
  }

  @Test
  void createDebt_persistsPendingDebtWithCalculatedOutstandingBalance() {
    UUID creditorId = UUID.randomUUID();
    DebtDto input =
        debtDto()
            .creditorId(creditorId.toString())
            .principalAmount(new BigDecimal("1000"))
            .interestAmount(new BigDecimal("50"))
            .feesAmount(new BigDecimal("25"))
            .dueDate(LocalDate.of(2026, 4, 1))
            .originalDueDate(LocalDate.of(2026, 3, 1))
            .externalReference("EXT-1")
            .ocrLine("OCR-CREATE")
            .build();
    when(creditorServiceClient.validateAction(eq(creditorId), any(ValidateActionRequest.class)))
        .thenReturn(ValidateActionResponse.builder().allowed(true).build());
    when(debtRepository.save(any(DebtEntity.class)))
        .thenAnswer(
            invocation -> {
              DebtEntity saved = invocation.getArgument(0);
              saved.setId(UUID.randomUUID());
              return saved;
            });

    DebtDto result = service.createDebt(input);

    ArgumentCaptor<DebtEntity> captor = ArgumentCaptor.forClass(DebtEntity.class);
    verify(debtRepository).save(captor.capture());
    assertThat(captor.getValue().getOutstandingBalance()).isEqualByComparingTo("1075");
    assertThat(captor.getValue().getStatus()).isEqualTo(DebtEntity.DebtStatus.PENDING);
    assertThat(captor.getValue().getReadinessStatus())
        .isEqualTo(DebtEntity.ReadinessStatus.PENDING_REVIEW);
    assertThat(result.getOutstandingBalance()).isEqualByComparingTo("1075");
  }

  @Test
  void createDebt_usesZeroWhenAllAmountsAreMissing() {
    UUID creditorId = UUID.randomUUID();
    DebtDto input =
        debtDto()
            .creditorId(creditorId.toString())
            .principalAmount(null)
            .interestAmount(null)
            .feesAmount(null)
            .build();
    when(creditorServiceClient.validateAction(eq(creditorId), any(ValidateActionRequest.class)))
        .thenReturn(ValidateActionResponse.builder().allowed(true).build());
    when(debtRepository.save(any(DebtEntity.class)))
        .thenAnswer(
            invocation -> {
              DebtEntity saved = invocation.getArgument(0);
              saved.setId(UUID.randomUUID());
              return saved;
            });

    DebtDto result = service.createDebt(input);

    assertThat(result.getOutstandingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  void createDebt_throwsWhenCreditorNotAllowed() {
    UUID creditorId = UUID.randomUUID();
    DebtDto input = debtDto().creditorId(creditorId.toString()).build();
    when(creditorServiceClient.validateAction(eq(creditorId), any(ValidateActionRequest.class)))
        .thenReturn(
            ValidateActionResponse.builder()
                .allowed(false)
                .reasonCode("CREDITOR_INACTIVE")
                .message("Creditor is not active")
                .build());

    assertThatThrownBy(() -> service.createDebt(input))
        .isInstanceOf(CreditorValidationException.class)
        .hasMessageContaining("Creditor is not active");
    verify(debtRepository, never()).save(any());
  }

  @Test
  void updateDebt_updatesMutableFields() {
    UUID debtId = UUID.randomUUID();
    UUID creditorId = UUID.randomUUID();
    DebtEntity entity = debtEntity("OCR-OLD", new BigDecimal("900"));
    entity.setId(debtId);
    entity.setCreditorOrgId(creditorId);
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(entity));
    when(creditorServiceClient.validateAction(eq(creditorId), any(ValidateActionRequest.class)))
        .thenReturn(ValidateActionResponse.builder().allowed(true).build());
    when(debtRepository.save(any(DebtEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    DebtDto update =
        debtDto()
            .debtTypeCode("700")
            .principalAmount(new BigDecimal("800"))
            .interestAmount(new BigDecimal("20"))
            .feesAmount(new BigDecimal("10"))
            .dueDate(LocalDate.of(2026, 5, 1))
            .externalReference("EXT-UPDATED")
            .ocrLine("OCR-UPDATED")
            .build();

    DebtDto result = service.updateDebt(debtId, update);

    assertThat(result.getDebtTypeCode()).isEqualTo("700");
    assertThat(result.getPrincipalAmount()).isEqualByComparingTo("800");
    assertThat(result.getOcrLine()).isEqualTo("OCR-UPDATED");
  }

  @Test
  void updateDebt_throwsWhenCreditorNotAllowed() {
    UUID debtId = UUID.randomUUID();
    UUID creditorId = UUID.randomUUID();
    DebtEntity entity = debtEntity("OCR-OLD", new BigDecimal("900"));
    entity.setId(debtId);
    entity.setCreditorOrgId(creditorId);
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(entity));
    when(creditorServiceClient.validateAction(eq(creditorId), any(ValidateActionRequest.class)))
        .thenReturn(
            ValidateActionResponse.builder()
                .allowed(false)
                .reasonCode("ACTION_NOT_PERMITTED")
                .message("Creditor does not have permission for action")
                .build());

    DebtDto update = debtDto().build();

    assertThatThrownBy(() -> service.updateDebt(debtId, update))
        .isInstanceOf(CreditorValidationException.class)
        .hasMessageContaining("Creditor does not have permission");
    verify(debtRepository, never()).save(any());
  }

  @Test
  void cancelDebt_marksDebtCancelled() {
    UUID debtId = UUID.randomUUID();
    DebtEntity entity = debtEntity("OCR-CANCEL", new BigDecimal("900"));
    entity.setId(debtId);
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(entity));

    service.cancelDebt(debtId);

    assertThat(entity.getStatus()).isEqualTo(DebtEntity.DebtStatus.CANCELLED);
    verify(debtRepository).save(entity);
  }

  @Test
  void getDebtTypes_returnsStandardCode() {
    assertThat(service.getDebtTypes()).containsExactly("600");
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
    BigDecimal amount = new BigDecimal("100");
    when(debtRepository.findById(debtId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.writeDown(debtId, amount))
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

  @Test
  void writeDown_usesTotalAmountWhenOutstandingBalanceIsMissing() {
    UUID debtId = UUID.randomUUID();
    DebtEntity entity = debtEntity("OCR-TOTAL", null);
    entity.setId(debtId);
    entity.setPrincipalAmount(new BigDecimal("1000"));
    entity.setInterestAmount(new BigDecimal("50"));
    entity.setFeesAmount(new BigDecimal("25"));
    entity.setOutstandingBalance(null);
    entity.setStatus(DebtEntity.DebtStatus.ACTIVE);
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(entity));
    when(debtRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    DebtDto result = service.writeDown(debtId, new BigDecimal("75"));

    assertThat(result.getOutstandingBalance()).isEqualByComparingTo("1000");
    assertThat(result.getStatus()).isEqualTo(DebtDto.DebtStatus.PARTIALLY_PAID);
  }

  // =========================================================================
  // Helpers
  // =========================================================================

  private DebtDto.DebtDtoBuilder debtDto() {
    return DebtDto.builder()
        .debtorId(UUID.randomUUID().toString())
        .creditorId(UUID.randomUUID().toString())
        .debtTypeCode("600")
        .dueDate(LocalDate.of(2025, 12, 1));
  }

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
