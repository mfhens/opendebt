package dk.ufst.opendebt.debtservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dk.ufst.opendebt.common.exception.OpenDebtException;
import dk.ufst.opendebt.debtservice.entity.ClaimLifecycleState;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.entity.HoeringEntity;
import dk.ufst.opendebt.debtservice.entity.HoeringStatus;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;
import dk.ufst.opendebt.debtservice.repository.HoeringRepository;

@ExtendWith(MockitoExtension.class)
class HoeringServiceImplTest {

  @Mock private HoeringRepository hoeringRepository;
  @Mock private DebtRepository debtRepository;

  private HoeringServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new HoeringServiceImpl(hoeringRepository, debtRepository);
  }

  // =========================================================================
  // createHoering
  // =========================================================================

  @Test
  void createHoering_success() {
    UUID debtId = UUID.randomUUID();
    DebtEntity debt = registeredDebt(debtId);
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(debt));
    when(hoeringRepository.save(any(HoeringEntity.class)))
        .thenAnswer(
            invocation -> {
              HoeringEntity saved = invocation.getArgument(0);
              saved.setId(UUID.randomUUID());
              return saved;
            });
    when(debtRepository.save(any(DebtEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    LocalDateTime before = LocalDateTime.now();
    HoeringEntity result = service.createHoering(debtId, "Hovedstol afviger");
    LocalDateTime after = LocalDateTime.now();

    assertThat(result.getDebtId()).isEqualTo(debtId);
    assertThat(result.getHoeringStatus()).isEqualTo(HoeringStatus.AFVENTER_FORDRINGSHAVER);
    assertThat(result.getDeviationDescription()).isEqualTo("Hovedstol afviger");
    assertThat(result.getSlaDeadline()).isAfter(before.plusDays(13));
    assertThat(result.getSlaDeadline()).isBefore(after.plusDays(15));

    assertThat(debt.getLifecycleState()).isEqualTo(ClaimLifecycleState.HOERING);
    verify(debtRepository).save(debt);
  }

  @Test
  void createHoering_debtNotRegistered_throws() {
    UUID debtId = UUID.randomUUID();
    DebtEntity debt = registeredDebt(debtId);
    debt.setLifecycleState(ClaimLifecycleState.OVERDRAGET);
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(debt));

    assertThatThrownBy(() -> service.createHoering(debtId, "Afvigelse"))
        .isInstanceOf(OpenDebtException.class)
        .hasMessageContaining("REGISTERED");

    verify(hoeringRepository, never()).save(any());
  }

  // =========================================================================
  // creditorApprove
  // =========================================================================

  @Test
  void creditorApprove_success() {
    UUID hoeringId = UUID.randomUUID();
    HoeringEntity hoering = pendingHoering(hoeringId);
    when(hoeringRepository.findById(hoeringId)).thenReturn(Optional.of(hoering));
    when(hoeringRepository.save(any(HoeringEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    HoeringEntity result = service.creditorApprove(hoeringId, "Begrundelse for afvigelsen");

    assertThat(result.getHoeringStatus()).isEqualTo(HoeringStatus.AFVENTER_RIM);
    assertThat(result.getCreditorJustification()).isEqualTo("Begrundelse for afvigelsen");
  }

  @Test
  void creditorApprove_wrongStatus_throws() {
    UUID hoeringId = UUID.randomUUID();
    HoeringEntity hoering = pendingHoering(hoeringId);
    hoering.setHoeringStatus(HoeringStatus.AFVENTER_RIM);
    when(hoeringRepository.findById(hoeringId)).thenReturn(Optional.of(hoering));

    assertThatThrownBy(() -> service.creditorApprove(hoeringId, "Begrundelse"))
        .isInstanceOf(OpenDebtException.class)
        .hasMessageContaining("AFVENTER_FORDRINGSHAVER");

    verify(hoeringRepository, never()).save(any());
  }

  // =========================================================================
  // creditorWithdraw
  // =========================================================================

  @Test
  void creditorWithdraw_success() {
    UUID hoeringId = UUID.randomUUID();
    UUID debtId = UUID.randomUUID();
    HoeringEntity hoering = pendingHoering(hoeringId);
    hoering.setDebtId(debtId);
    DebtEntity debt = registeredDebt(debtId);
    debt.setLifecycleState(ClaimLifecycleState.HOERING);

    when(hoeringRepository.findById(hoeringId)).thenReturn(Optional.of(hoering));
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(debt));
    when(hoeringRepository.save(any(HoeringEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(debtRepository.save(any(DebtEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    HoeringEntity result = service.creditorWithdraw(hoeringId);

    assertThat(result.getHoeringStatus()).isEqualTo(HoeringStatus.FORTRUDT);
    assertThat(result.getResolvedAt()).isNotNull();
    assertThat(debt.getLifecycleState()).isEqualTo(ClaimLifecycleState.REGISTERED);
  }

  // =========================================================================
  // rimDecide
  // =========================================================================

  @Test
  void rimDecide_accepted() {
    UUID hoeringId = UUID.randomUUID();
    UUID debtId = UUID.randomUUID();
    HoeringEntity hoering = pendingHoering(hoeringId);
    hoering.setHoeringStatus(HoeringStatus.AFVENTER_RIM);
    hoering.setDebtId(debtId);
    DebtEntity debt = registeredDebt(debtId);
    debt.setLifecycleState(ClaimLifecycleState.HOERING);

    when(hoeringRepository.findById(hoeringId)).thenReturn(Optional.of(hoering));
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(debt));
    when(hoeringRepository.save(any(HoeringEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(debtRepository.save(any(DebtEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    HoeringEntity result = service.rimDecide(hoeringId, true, "Godkendt af sagsbehandler");

    assertThat(result.getHoeringStatus()).isEqualTo(HoeringStatus.GODKENDT);
    assertThat(result.getRimDecision()).isEqualTo("Godkendt af sagsbehandler");
    assertThat(result.getResolvedAt()).isNotNull();
    assertThat(debt.getLifecycleState()).isEqualTo(ClaimLifecycleState.OVERDRAGET);
  }

  @Test
  void rimDecide_rejected() {
    UUID hoeringId = UUID.randomUUID();
    UUID debtId = UUID.randomUUID();
    HoeringEntity hoering = pendingHoering(hoeringId);
    hoering.setHoeringStatus(HoeringStatus.AFVENTER_RIM);
    hoering.setDebtId(debtId);
    DebtEntity debt = registeredDebt(debtId);
    debt.setLifecycleState(ClaimLifecycleState.HOERING);

    when(hoeringRepository.findById(hoeringId)).thenReturn(Optional.of(hoering));
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(debt));
    when(hoeringRepository.save(any(HoeringEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(debtRepository.save(any(DebtEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    HoeringEntity result = service.rimDecide(hoeringId, false, "Afvist - manglende dokumentation");

    assertThat(result.getHoeringStatus()).isEqualTo(HoeringStatus.AFVIST);
    assertThat(result.getRimDecision()).isEqualTo("Afvist - manglende dokumentation");
    assertThat(result.getResolvedAt()).isNotNull();
    assertThat(debt.getLifecycleState()).isEqualTo(ClaimLifecycleState.REGISTERED);
  }

  // =========================================================================
  // Helpers
  // =========================================================================

  private DebtEntity registeredDebt(UUID debtId) {
    return DebtEntity.builder()
        .id(debtId)
        .debtorPersonId(UUID.randomUUID())
        .creditorOrgId(UUID.randomUUID())
        .debtTypeCode("600")
        .lifecycleState(ClaimLifecycleState.REGISTERED)
        .status(DebtEntity.DebtStatus.PENDING)
        .readinessStatus(DebtEntity.ReadinessStatus.PENDING_REVIEW)
        .build();
  }

  private HoeringEntity pendingHoering(UUID hoeringId) {
    return HoeringEntity.builder()
        .id(hoeringId)
        .debtId(UUID.randomUUID())
        .hoeringStatus(HoeringStatus.AFVENTER_FORDRINGSHAVER)
        .deviationDescription("Test afvigelse")
        .slaDeadline(LocalDateTime.now().plusDays(14))
        .build();
  }
}
