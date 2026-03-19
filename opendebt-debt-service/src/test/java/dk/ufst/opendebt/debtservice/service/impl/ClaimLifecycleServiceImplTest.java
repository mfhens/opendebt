package dk.ufst.opendebt.debtservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dk.ufst.opendebt.common.exception.OpenDebtException;
import dk.ufst.opendebt.debtservice.entity.ClaimLifecycleEvent;
import dk.ufst.opendebt.debtservice.entity.ClaimLifecycleState;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.repository.ClaimLifecycleEventRepository;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;

@ExtendWith(MockitoExtension.class)
class ClaimLifecycleServiceImplTest {

  @Mock private DebtRepository debtRepository;
  @Mock private ClaimLifecycleEventRepository claimLifecycleEventRepository;

  @InjectMocks private ClaimLifecycleServiceImpl service;

  // =========================================================================
  // transitionToRestance
  // =========================================================================

  @Test
  void transitionToRestance_success() {
    UUID debtId = UUID.randomUUID();
    DebtEntity debt = debtEntity(debtId, ClaimLifecycleState.REGISTERED);
    debt.setLastPaymentDate(LocalDate.now().minusDays(1));
    debt.setOutstandingBalance(new BigDecimal("1000"));
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(debt));
    when(debtRepository.save(any(DebtEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    when(claimLifecycleEventRepository.save(any(ClaimLifecycleEvent.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    DebtEntity result = service.transitionToRestance(debtId);

    assertThat(result.getLifecycleState()).isEqualTo(ClaimLifecycleState.RESTANCE);
    verify(debtRepository).save(debt);
    verify(claimLifecycleEventRepository).save(any(ClaimLifecycleEvent.class));
  }

  @Test
  void transitionToRestance_srbNotExpired_throws() {
    UUID debtId = UUID.randomUUID();
    DebtEntity debt = debtEntity(debtId, ClaimLifecycleState.REGISTERED);
    debt.setLastPaymentDate(LocalDate.now().plusDays(10));
    debt.setOutstandingBalance(new BigDecimal("1000"));
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(debt));

    assertThatThrownBy(() -> service.transitionToRestance(debtId))
        .isInstanceOf(OpenDebtException.class)
        .hasMessageContaining("has not expired");
  }

  @Test
  void transitionToRestance_alreadyPaid_throws() {
    UUID debtId = UUID.randomUUID();
    DebtEntity debt = debtEntity(debtId, ClaimLifecycleState.REGISTERED);
    debt.setLastPaymentDate(LocalDate.now().minusDays(1));
    debt.setOutstandingBalance(BigDecimal.ZERO);
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(debt));

    assertThatThrownBy(() -> service.transitionToRestance(debtId))
        .isInstanceOf(OpenDebtException.class)
        .hasMessageContaining("Outstanding balance must be > 0");
  }

  // =========================================================================
  // transferForCollection
  // =========================================================================

  @Test
  void transferForCollection_success() {
    UUID debtId = UUID.randomUUID();
    DebtEntity debt = debtEntity(debtId, ClaimLifecycleState.RESTANCE);
    debt.setLastPaymentDate(LocalDate.now().minusDays(5));
    debt.setOutstandingBalance(new BigDecimal("500"));
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(debt));
    when(debtRepository.save(any(DebtEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    when(claimLifecycleEventRepository.save(any(ClaimLifecycleEvent.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    DebtEntity result = service.transferForCollection(debtId);

    assertThat(result.getLifecycleState()).isEqualTo(ClaimLifecycleState.OVERDRAGET);
    assertThat(result.getReceivedAt()).isNotNull();
    verify(debtRepository).save(debt);
  }

  @Test
  void transferForCollection_notRestance_throws() {
    UUID debtId = UUID.randomUUID();
    DebtEntity debt = debtEntity(debtId, ClaimLifecycleState.REGISTERED);
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(debt));

    assertThatThrownBy(() -> service.transferForCollection(debtId))
        .isInstanceOf(OpenDebtException.class)
        .hasMessageContaining("requires state RESTANCE");
  }

  // =========================================================================
  // transitionToHearing
  // =========================================================================

  @Test
  void transitionToHearing_success() {
    UUID debtId = UUID.randomUUID();
    DebtEntity debt = debtEntity(debtId, ClaimLifecycleState.REGISTERED);
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(debt));
    when(debtRepository.save(any(DebtEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    when(claimLifecycleEventRepository.save(any(ClaimLifecycleEvent.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    DebtEntity result = service.transitionToHearing(debtId);

    assertThat(result.getLifecycleState()).isEqualTo(ClaimLifecycleState.HOERING);
  }

  @Test
  void transitionToHearing_fromRestance_throws() {
    UUID debtId = UUID.randomUUID();
    DebtEntity debt = debtEntity(debtId, ClaimLifecycleState.RESTANCE);
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(debt));

    assertThatThrownBy(() -> service.transitionToHearing(debtId))
        .isInstanceOf(OpenDebtException.class)
        .hasMessageContaining("Invalid lifecycle transition");
  }

  // =========================================================================
  // resolveHearing
  // =========================================================================

  @Test
  void resolveHearing_accepted() {
    UUID debtId = UUID.randomUUID();
    DebtEntity debt = debtEntity(debtId, ClaimLifecycleState.HOERING);
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(debt));
    when(debtRepository.save(any(DebtEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    when(claimLifecycleEventRepository.save(any(ClaimLifecycleEvent.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    DebtEntity result = service.resolveHearing(debtId, true);

    assertThat(result.getLifecycleState()).isEqualTo(ClaimLifecycleState.OVERDRAGET);
    assertThat(result.getReceivedAt()).isNotNull();
  }

  @Test
  void resolveHearing_rejected() {
    UUID debtId = UUID.randomUUID();
    DebtEntity debt = debtEntity(debtId, ClaimLifecycleState.HOERING);
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(debt));
    when(debtRepository.save(any(DebtEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    when(claimLifecycleEventRepository.save(any(ClaimLifecycleEvent.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    DebtEntity result = service.resolveHearing(debtId, false);

    assertThat(result.getLifecycleState()).isEqualTo(ClaimLifecycleState.REGISTERED);
    assertThat(result.getReceivedAt()).isNull();
  }

  // =========================================================================
  // withdraw
  // =========================================================================

  @Test
  void withdraw_fromOverdraget_success() {
    UUID debtId = UUID.randomUUID();
    DebtEntity debt = debtEntity(debtId, ClaimLifecycleState.OVERDRAGET);
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(debt));
    when(debtRepository.save(any(DebtEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    when(claimLifecycleEventRepository.save(any(ClaimLifecycleEvent.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    DebtEntity result = service.withdraw(debtId, "FEJL");

    assertThat(result.getLifecycleState()).isEqualTo(ClaimLifecycleState.TILBAGEKALDT);
  }

  @Test
  void withdraw_fromTerminal_throws() {
    UUID debtId = UUID.randomUUID();
    DebtEntity debt = debtEntity(debtId, ClaimLifecycleState.INDFRIET);
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(debt));

    assertThatThrownBy(() -> service.withdraw(debtId, "FEJL"))
        .isInstanceOf(OpenDebtException.class)
        .hasMessageContaining("Cannot withdraw from terminal state");
  }

  // =========================================================================
  // writeOff
  // =========================================================================

  @Test
  void writeOff_success() {
    UUID debtId = UUID.randomUUID();
    DebtEntity debt = debtEntity(debtId, ClaimLifecycleState.OVERDRAGET);
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(debt));
    when(debtRepository.save(any(DebtEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    when(claimLifecycleEventRepository.save(any(ClaimLifecycleEvent.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    DebtEntity result = service.writeOff(debtId, "REASON-01");

    assertThat(result.getLifecycleState()).isEqualTo(ClaimLifecycleState.AFSKREVET);
  }

  // =========================================================================
  // markFullyPaid
  // =========================================================================

  @Test
  void markFullyPaid_success() {
    UUID debtId = UUID.randomUUID();
    DebtEntity debt = debtEntity(debtId, ClaimLifecycleState.OVERDRAGET);
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(debt));
    when(debtRepository.save(any(DebtEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    when(claimLifecycleEventRepository.save(any(ClaimLifecycleEvent.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    DebtEntity result = service.markFullyPaid(debtId);

    assertThat(result.getLifecycleState()).isEqualTo(ClaimLifecycleState.INDFRIET);
  }

  // =========================================================================
  // evaluateClaimState
  // =========================================================================

  @Test
  void evaluateClaimState_transitionsToRestance_whenDeadlineExpiredAndUnpaid() {
    UUID debtId = UUID.randomUUID();
    DebtEntity debt = debtEntity(debtId, ClaimLifecycleState.REGISTERED);
    debt.setLastPaymentDate(LocalDate.of(2026, 3, 1));
    debt.setOutstandingBalance(new BigDecimal("1000"));
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(debt));
    when(debtRepository.save(any(DebtEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    when(claimLifecycleEventRepository.save(any(ClaimLifecycleEvent.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    DebtEntity result = service.evaluateClaimState(debtId, LocalDate.of(2026, 3, 2));

    assertThat(result.getLifecycleState()).isEqualTo(ClaimLifecycleState.RESTANCE);
    verify(debtRepository).save(debt);
  }

  @Test
  void evaluateClaimState_noTransition_whenFullyPaid() {
    UUID debtId = UUID.randomUUID();
    DebtEntity debt = debtEntity(debtId, ClaimLifecycleState.REGISTERED);
    debt.setLastPaymentDate(LocalDate.of(2026, 3, 1));
    debt.setOutstandingBalance(BigDecimal.ZERO);
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(debt));

    DebtEntity result = service.evaluateClaimState(debtId, LocalDate.of(2026, 3, 2));

    assertThat(result.getLifecycleState()).isEqualTo(ClaimLifecycleState.REGISTERED);
    verify(debtRepository, never()).save(any());
  }

  @Test
  void evaluateClaimState_noTransition_whenDeadlineNotExpired() {
    UUID debtId = UUID.randomUUID();
    DebtEntity debt = debtEntity(debtId, ClaimLifecycleState.REGISTERED);
    debt.setLastPaymentDate(LocalDate.of(2026, 3, 5));
    debt.setOutstandingBalance(new BigDecimal("1000"));
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(debt));

    DebtEntity result = service.evaluateClaimState(debtId, LocalDate.of(2026, 3, 2));

    assertThat(result.getLifecycleState()).isEqualTo(ClaimLifecycleState.REGISTERED);
    verify(debtRepository, never()).save(any());
  }

  @Test
  void evaluateClaimState_noTransition_whenAlreadyRestance() {
    UUID debtId = UUID.randomUUID();
    DebtEntity debt = debtEntity(debtId, ClaimLifecycleState.RESTANCE);
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(debt));

    DebtEntity result = service.evaluateClaimState(debtId, LocalDate.of(2026, 3, 2));

    assertThat(result.getLifecycleState()).isEqualTo(ClaimLifecycleState.RESTANCE);
    verify(debtRepository, never()).save(any());
  }

  // =========================================================================
  // transferForCollection with recipientId
  // =========================================================================

  @Test
  void transferForCollection_withRecipient_recordsRecipientInEvent() {
    UUID debtId = UUID.randomUUID();
    UUID recipientId = UUID.randomUUID();
    DebtEntity debt = debtEntity(debtId, ClaimLifecycleState.RESTANCE);
    debt.setLastPaymentDate(LocalDate.now().minusDays(5));
    debt.setOutstandingBalance(new BigDecimal("500"));
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(debt));
    when(debtRepository.save(any(DebtEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    when(claimLifecycleEventRepository.save(any(ClaimLifecycleEvent.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    DebtEntity result = service.transferForCollection(debtId, recipientId);

    assertThat(result.getLifecycleState()).isEqualTo(ClaimLifecycleState.OVERDRAGET);
    verify(claimLifecycleEventRepository)
        .save(
            argThat(
                event ->
                    recipientId.equals(event.getRecipientId())
                        && "OVERDRAGET".equals(event.getNewState())));
  }

  // =========================================================================
  // canTransition
  // =========================================================================

  @Test
  void canTransition_validPairs() {
    assertThat(service.canTransition(ClaimLifecycleState.REGISTERED, ClaimLifecycleState.RESTANCE))
        .isTrue();
    assertThat(service.canTransition(ClaimLifecycleState.REGISTERED, ClaimLifecycleState.HOERING))
        .isTrue();
    assertThat(service.canTransition(ClaimLifecycleState.RESTANCE, ClaimLifecycleState.OVERDRAGET))
        .isTrue();
    assertThat(
            service.canTransition(ClaimLifecycleState.RESTANCE, ClaimLifecycleState.TILBAGEKALDT))
        .isTrue();
    assertThat(service.canTransition(ClaimLifecycleState.HOERING, ClaimLifecycleState.OVERDRAGET))
        .isTrue();
    assertThat(service.canTransition(ClaimLifecycleState.HOERING, ClaimLifecycleState.REGISTERED))
        .isTrue();
    assertThat(
            service.canTransition(ClaimLifecycleState.OVERDRAGET, ClaimLifecycleState.TILBAGEKALDT))
        .isTrue();
    assertThat(service.canTransition(ClaimLifecycleState.OVERDRAGET, ClaimLifecycleState.AFSKREVET))
        .isTrue();
    assertThat(service.canTransition(ClaimLifecycleState.OVERDRAGET, ClaimLifecycleState.INDFRIET))
        .isTrue();
  }

  @Test
  void canTransition_invalidPairs() {
    assertThat(
            service.canTransition(ClaimLifecycleState.REGISTERED, ClaimLifecycleState.OVERDRAGET))
        .isFalse();
    assertThat(service.canTransition(ClaimLifecycleState.REGISTERED, ClaimLifecycleState.AFSKREVET))
        .isFalse();
    assertThat(service.canTransition(ClaimLifecycleState.RESTANCE, ClaimLifecycleState.HOERING))
        .isFalse();
    assertThat(
            service.canTransition(ClaimLifecycleState.TILBAGEKALDT, ClaimLifecycleState.REGISTERED))
        .isFalse();
    assertThat(service.canTransition(ClaimLifecycleState.AFSKREVET, ClaimLifecycleState.OVERDRAGET))
        .isFalse();
    assertThat(service.canTransition(ClaimLifecycleState.INDFRIET, ClaimLifecycleState.REGISTERED))
        .isFalse();
  }

  // =========================================================================
  // Helpers
  // =========================================================================

  private DebtEntity debtEntity(UUID id, ClaimLifecycleState state) {
    return DebtEntity.builder()
        .id(id)
        .debtorPersonId(UUID.randomUUID())
        .creditorOrgId(UUID.randomUUID())
        .debtTypeCode("600")
        .principalAmount(new BigDecimal("1000"))
        .dueDate(LocalDate.of(2025, 12, 1))
        .outstandingBalance(new BigDecimal("1000"))
        .status(DebtEntity.DebtStatus.ACTIVE)
        .readinessStatus(DebtEntity.ReadinessStatus.READY_FOR_COLLECTION)
        .lifecycleState(state)
        .build();
  }
}
