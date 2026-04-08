package dk.ufst.opendebt.debtservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import dk.ufst.opendebt.common.audit.cls.ClsAuditClient;
import dk.ufst.opendebt.debtservice.dto.ClaimAdjustmentRequestDto;
import dk.ufst.opendebt.debtservice.dto.ClaimAdjustmentResponseDto;
import dk.ufst.opendebt.debtservice.entity.ClaimCategory;
import dk.ufst.opendebt.debtservice.entity.ClaimLifecycleState;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.entity.HoeringEntity;
import dk.ufst.opendebt.debtservice.entity.HoeringStatus;
import dk.ufst.opendebt.debtservice.exception.CreditorValidationException;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;
import dk.ufst.opendebt.debtservice.repository.HoeringRepository;

@ExtendWith(MockitoExtension.class)
class ClaimAdjustmentServiceImplTest {

  @Mock private DebtRepository debtRepository;
  @Mock private HoeringRepository hoeringRepository;
  @Mock private ClsAuditClient clsAuditClient;

  private ClaimAdjustmentServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new ClaimAdjustmentServiceImpl(debtRepository, hoeringRepository, clsAuditClient);
  }

  // --- B4: adjustment type whitelist ---

  @Test
  void processAdjustment_unknownType_throwsInvalidAdjustmentType() {
    UUID claimId = UUID.randomUUID();
    ClaimAdjustmentRequestDto request =
        ClaimAdjustmentRequestDto.builder().adjustmentType("TOTALLY_UNKNOWN").build();

    assertThatThrownBy(() -> service.processAdjustment(claimId, request))
        .isInstanceOf(CreditorValidationException.class)
        .satisfies(
            e ->
                assertThat(((CreditorValidationException) e).getErrorCode())
                    .isEqualTo("INVALID_ADJUSTMENT_TYPE"));
  }

  @Test
  void processAdjustment_claimNotFound_throwsNotFound() {
    UUID claimId = UUID.randomUUID();
    when(debtRepository.findById(claimId)).thenReturn(Optional.empty());
    ClaimAdjustmentRequestDto request =
        ClaimAdjustmentRequestDto.builder().adjustmentType("WRITE_UP").build();

    assertThatThrownBy(() -> service.processAdjustment(claimId, request))
        .isInstanceOf(ResponseStatusException.class);
  }

  // --- FR-1: write-down reason code ---

  @Test
  void processAdjustment_writeDownNoReasonCode_throwsReasonRequired() {
    UUID claimId = UUID.randomUUID();
    when(debtRepository.findById(claimId))
        .thenReturn(Optional.of(debtInState(claimId, ClaimLifecycleState.OVERDRAGET)));
    ClaimAdjustmentRequestDto request =
        ClaimAdjustmentRequestDto.builder().adjustmentType("NEDSKRIV").build();

    assertThatThrownBy(() -> service.processAdjustment(claimId, request))
        .isInstanceOf(CreditorValidationException.class)
        .satisfies(
            e ->
                assertThat(((CreditorValidationException) e).getErrorCode())
                    .isEqualTo("WRITE_DOWN_REASON_REQUIRED"));
  }

  // --- FR-7: RIM-internal codes denylist ---

  @Test
  void processAdjustment_rimInternalWriteUpCode_throwsRimInternal() {
    UUID claimId = UUID.randomUUID();
    when(debtRepository.findById(claimId))
        .thenReturn(Optional.of(debtInState(claimId, ClaimLifecycleState.OVERDRAGET)));
    ClaimAdjustmentRequestDto request =
        ClaimAdjustmentRequestDto.builder()
            .adjustmentType("WRITE_UP")
            .writeUpReasonCode("DINDB")
            .build();

    assertThatThrownBy(() -> service.processAdjustment(claimId, request))
        .isInstanceOf(CreditorValidationException.class)
        .satisfies(
            e ->
                assertThat(((CreditorValidationException) e).getErrorCode())
                    .isEqualTo("RIM_INTERNAL_CODE"));
  }

  // --- FR-2: OPSKRIVNING_REGULERING on RENTE claim ---

  @Test
  void processAdjustment_opskrivningReguRenteClaim_throwsForbidden() {
    UUID claimId = UUID.randomUUID();
    DebtEntity debt =
        DebtEntity.builder()
            .id(claimId)
            .lifecycleState(ClaimLifecycleState.OVERDRAGET)
            .claimCategory(ClaimCategory.RENTE)
            .build();
    when(debtRepository.findById(claimId)).thenReturn(Optional.of(debt));
    ClaimAdjustmentRequestDto request =
        ClaimAdjustmentRequestDto.builder().adjustmentType("OPSKRIVNING_REGULERING").build();

    assertThatThrownBy(() -> service.processAdjustment(claimId, request))
        .isInstanceOf(CreditorValidationException.class)
        .satisfies(
            e ->
                assertThat(((CreditorValidationException) e).getErrorCode())
                    .isEqualTo("RENTE_OPSKRIVNING_FORBIDDEN"));
  }

  // --- FR-3: Høring timing rule ---

  @Test
  void processAdjustment_hoeringClaimOpskrivningReguResolved_useHoeringTimestamp() {
    UUID claimId = UUID.randomUUID();
    LocalDateTime resolvedAt = LocalDateTime.of(2024, 3, 10, 12, 0);
    DebtEntity debt =
        DebtEntity.builder()
            .id(claimId)
            .lifecycleState(ClaimLifecycleState.HOERING)
            .claimCategory(ClaimCategory.HF)
            .build();
    HoeringEntity hoering =
        HoeringEntity.builder()
            .id(UUID.randomUUID())
            .debtId(claimId)
            .hoeringStatus(HoeringStatus.GODKENDT)
            .resolvedAt(resolvedAt)
            .deviationDescription("stamdata deviation")
            .slaDeadline(LocalDateTime.now().plusDays(7))
            .build();

    when(debtRepository.findById(claimId)).thenReturn(Optional.of(debt));
    when(hoeringRepository.findTopByDebtIdOrderByResolvedAtDesc(claimId))
        .thenReturn(Optional.of(hoering));

    ClaimAdjustmentResponseDto response =
        service.processAdjustment(
            claimId,
            ClaimAdjustmentRequestDto.builder()
                .adjustmentType("OPSKRIVNING_REGULERING")
                .amount(BigDecimal.TEN)
                .build());

    assertThat(response.getStatus()).isEqualTo("PENDING_HOERING");
    assertThat(response.getReceiptTimestamp())
        .isEqualTo(resolvedAt.atZone(java.time.ZoneOffset.UTC).toInstant());
  }

  @Test
  void processAdjustment_hoeringClaimOpskrivningReguUnresolved_throwsNotYetResolved() {
    UUID claimId = UUID.randomUUID();
    DebtEntity debt =
        DebtEntity.builder()
            .id(claimId)
            .lifecycleState(ClaimLifecycleState.HOERING)
            .claimCategory(ClaimCategory.HF)
            .build();
    HoeringEntity hoering =
        HoeringEntity.builder()
            .id(UUID.randomUUID())
            .debtId(claimId)
            .hoeringStatus(HoeringStatus.AFVENTER_FORDRINGSHAVER)
            .resolvedAt(null)
            .deviationDescription("stamdata deviation")
            .slaDeadline(LocalDateTime.now().plusDays(14))
            .build();

    when(debtRepository.findById(claimId)).thenReturn(Optional.of(debt));
    when(hoeringRepository.findTopByDebtIdOrderByResolvedAtDesc(claimId))
        .thenReturn(Optional.of(hoering));

    assertThatThrownBy(
            () ->
                service.processAdjustment(
                    claimId,
                    ClaimAdjustmentRequestDto.builder()
                        .adjustmentType("OPSKRIVNING_REGULERING")
                        .build()))
        .isInstanceOf(CreditorValidationException.class)
        .satisfies(
            e ->
                assertThat(((CreditorValidationException) e).getErrorCode())
                    .isEqualTo("HOERING_NOT_YET_RESOLVED"));
  }

  @Test
  void processAdjustment_hoeringClaimNonOpskrivningType_returnsPendingHoering() {
    UUID claimId = UUID.randomUUID();
    when(debtRepository.findById(claimId))
        .thenReturn(
            Optional.of(
                DebtEntity.builder()
                    .id(claimId)
                    .lifecycleState(ClaimLifecycleState.HOERING)
                    .claimCategory(ClaimCategory.HF)
                    .build()));

    ClaimAdjustmentResponseDto response =
        service.processAdjustment(
            claimId,
            ClaimAdjustmentRequestDto.builder()
                .adjustmentType("WRITE_UP")
                .amount(BigDecimal.TEN)
                .build());

    assertThat(response.getStatus()).isEqualTo("PENDING_HOERING");
  }

  // --- Happy path ---

  @Test
  void processAdjustment_normalWriteUp_returnsAccepted() {
    UUID claimId = UUID.randomUUID();
    when(debtRepository.findById(claimId))
        .thenReturn(Optional.of(debtInState(claimId, ClaimLifecycleState.OVERDRAGET)));

    ClaimAdjustmentResponseDto response =
        service.processAdjustment(
            claimId,
            ClaimAdjustmentRequestDto.builder()
                .adjustmentType("WRITE_UP")
                .amount(new BigDecimal("500.00"))
                .build());

    assertThat(response.getStatus()).isEqualTo("ACCEPTED");
    assertThat(response.getAmount()).isEqualByComparingTo("500.00");
    assertThat(response.getActionId()).startsWith("ACT-");
  }

  private DebtEntity debtInState(UUID id, ClaimLifecycleState state) {
    return DebtEntity.builder()
        .id(id)
        .lifecycleState(state)
        .claimCategory(ClaimCategory.HF)
        .build();
  }
}
