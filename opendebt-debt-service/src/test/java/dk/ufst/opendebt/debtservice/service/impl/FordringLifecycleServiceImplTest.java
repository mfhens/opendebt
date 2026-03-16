package dk.ufst.opendebt.debtservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.entity.FordringLifecycleState;
import dk.ufst.opendebt.debtservice.entity.OverdragelseEvent;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;
import dk.ufst.opendebt.debtservice.repository.OverdragelseEventRepository;

@ExtendWith(MockitoExtension.class)
class FordringLifecycleServiceImplTest {

  @Mock private DebtRepository debtRepository;
  @Mock private OverdragelseEventRepository overdragelseEventRepository;

  @InjectMocks private FordringLifecycleServiceImpl service;

  // =========================================================================
  // transitionToRestance
  // =========================================================================

  @Test
  void transitionToRestance_success() {
    UUID debtId = UUID.randomUUID();
    DebtEntity debt = debtEntity(debtId, FordringLifecycleState.REGISTERED);
    debt.setSidsteRettigeBetalingsdato(LocalDate.now().minusDays(1));
    debt.setOutstandingBalance(new BigDecimal("1000"));
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(debt));
    when(debtRepository.save(any(DebtEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    when(overdragelseEventRepository.save(any(OverdragelseEvent.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    DebtEntity result = service.transitionToRestance(debtId);

    assertThat(result.getLifecycleState()).isEqualTo(FordringLifecycleState.RESTANCE);
    verify(debtRepository).save(debt);
    verify(overdragelseEventRepository).save(any(OverdragelseEvent.class));
  }

  @Test
  void transitionToRestance_srbNotExpired_throws() {
    UUID debtId = UUID.randomUUID();
    DebtEntity debt = debtEntity(debtId, FordringLifecycleState.REGISTERED);
    debt.setSidsteRettigeBetalingsdato(LocalDate.now().plusDays(10));
    debt.setOutstandingBalance(new BigDecimal("1000"));
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(debt));

    assertThatThrownBy(() -> service.transitionToRestance(debtId))
        .isInstanceOf(OpenDebtException.class)
        .hasMessageContaining("SRB has not expired");
  }

  @Test
  void transitionToRestance_alreadyPaid_throws() {
    UUID debtId = UUID.randomUUID();
    DebtEntity debt = debtEntity(debtId, FordringLifecycleState.REGISTERED);
    debt.setSidsteRettigeBetalingsdato(LocalDate.now().minusDays(1));
    debt.setOutstandingBalance(BigDecimal.ZERO);
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(debt));

    assertThatThrownBy(() -> service.transitionToRestance(debtId))
        .isInstanceOf(OpenDebtException.class)
        .hasMessageContaining("Outstanding balance must be > 0");
  }

  // =========================================================================
  // overdragTilInddrivelse
  // =========================================================================

  @Test
  void overdragTilInddrivelse_success() {
    UUID debtId = UUID.randomUUID();
    DebtEntity debt = debtEntity(debtId, FordringLifecycleState.RESTANCE);
    debt.setSidsteRettigeBetalingsdato(LocalDate.now().minusDays(5));
    debt.setOutstandingBalance(new BigDecimal("500"));
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(debt));
    when(debtRepository.save(any(DebtEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    when(overdragelseEventRepository.save(any(OverdragelseEvent.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    DebtEntity result = service.overdragTilInddrivelse(debtId);

    assertThat(result.getLifecycleState()).isEqualTo(FordringLifecycleState.OVERDRAGET);
    assertThat(result.getModtagelsestidspunkt()).isNotNull();
    verify(debtRepository).save(debt);
  }

  @Test
  void overdragTilInddrivelse_notRestance_throws() {
    UUID debtId = UUID.randomUUID();
    DebtEntity debt = debtEntity(debtId, FordringLifecycleState.REGISTERED);
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(debt));

    assertThatThrownBy(() -> service.overdragTilInddrivelse(debtId))
        .isInstanceOf(OpenDebtException.class)
        .hasMessageContaining("Overdragelse requires state RESTANCE");
  }

  // =========================================================================
  // transitionToHoering
  // =========================================================================

  @Test
  void transitionToHoering_success() {
    UUID debtId = UUID.randomUUID();
    DebtEntity debt = debtEntity(debtId, FordringLifecycleState.REGISTERED);
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(debt));
    when(debtRepository.save(any(DebtEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    when(overdragelseEventRepository.save(any(OverdragelseEvent.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    DebtEntity result = service.transitionToHoering(debtId);

    assertThat(result.getLifecycleState()).isEqualTo(FordringLifecycleState.HOERING);
  }

  @Test
  void transitionToHoering_fromRestance_throws() {
    UUID debtId = UUID.randomUUID();
    DebtEntity debt = debtEntity(debtId, FordringLifecycleState.RESTANCE);
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(debt));

    assertThatThrownBy(() -> service.transitionToHoering(debtId))
        .isInstanceOf(OpenDebtException.class)
        .hasMessageContaining("Invalid lifecycle transition");
  }

  // =========================================================================
  // resolveHoering
  // =========================================================================

  @Test
  void resolveHoering_accepted() {
    UUID debtId = UUID.randomUUID();
    DebtEntity debt = debtEntity(debtId, FordringLifecycleState.HOERING);
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(debt));
    when(debtRepository.save(any(DebtEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    when(overdragelseEventRepository.save(any(OverdragelseEvent.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    DebtEntity result = service.resolveHoering(debtId, true);

    assertThat(result.getLifecycleState()).isEqualTo(FordringLifecycleState.OVERDRAGET);
    assertThat(result.getModtagelsestidspunkt()).isNotNull();
  }

  @Test
  void resolveHoering_rejected() {
    UUID debtId = UUID.randomUUID();
    DebtEntity debt = debtEntity(debtId, FordringLifecycleState.HOERING);
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(debt));
    when(debtRepository.save(any(DebtEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    when(overdragelseEventRepository.save(any(OverdragelseEvent.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    DebtEntity result = service.resolveHoering(debtId, false);

    assertThat(result.getLifecycleState()).isEqualTo(FordringLifecycleState.REGISTERED);
    assertThat(result.getModtagelsestidspunkt()).isNull();
  }

  // =========================================================================
  // tilbagekald
  // =========================================================================

  @Test
  void tilbagekald_fromOverdraget_success() {
    UUID debtId = UUID.randomUUID();
    DebtEntity debt = debtEntity(debtId, FordringLifecycleState.OVERDRAGET);
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(debt));
    when(debtRepository.save(any(DebtEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    when(overdragelseEventRepository.save(any(OverdragelseEvent.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    DebtEntity result = service.tilbagekald(debtId, "FEJL");

    assertThat(result.getLifecycleState()).isEqualTo(FordringLifecycleState.TILBAGEKALDT);
  }

  @Test
  void tilbagekald_fromTerminal_throws() {
    UUID debtId = UUID.randomUUID();
    DebtEntity debt = debtEntity(debtId, FordringLifecycleState.INDFRIET);
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(debt));

    assertThatThrownBy(() -> service.tilbagekald(debtId, "FEJL"))
        .isInstanceOf(OpenDebtException.class)
        .hasMessageContaining("Cannot tilbagekald from terminal state");
  }

  // =========================================================================
  // afskriv
  // =========================================================================

  @Test
  void afskriv_success() {
    UUID debtId = UUID.randomUUID();
    DebtEntity debt = debtEntity(debtId, FordringLifecycleState.OVERDRAGET);
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(debt));
    when(debtRepository.save(any(DebtEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    when(overdragelseEventRepository.save(any(OverdragelseEvent.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    DebtEntity result = service.afskriv(debtId, "REASON-01");

    assertThat(result.getLifecycleState()).isEqualTo(FordringLifecycleState.AFSKREVET);
  }

  // =========================================================================
  // markIndfriet
  // =========================================================================

  @Test
  void markIndfriet_success() {
    UUID debtId = UUID.randomUUID();
    DebtEntity debt = debtEntity(debtId, FordringLifecycleState.OVERDRAGET);
    when(debtRepository.findById(debtId)).thenReturn(Optional.of(debt));
    when(debtRepository.save(any(DebtEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    when(overdragelseEventRepository.save(any(OverdragelseEvent.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    DebtEntity result = service.markIndfriet(debtId);

    assertThat(result.getLifecycleState()).isEqualTo(FordringLifecycleState.INDFRIET);
  }

  // =========================================================================
  // canTransition
  // =========================================================================

  @Test
  void canTransition_validPairs() {
    assertThat(
            service.canTransition(
                FordringLifecycleState.REGISTERED, FordringLifecycleState.RESTANCE))
        .isTrue();
    assertThat(
            service.canTransition(
                FordringLifecycleState.REGISTERED, FordringLifecycleState.HOERING))
        .isTrue();
    assertThat(
            service.canTransition(
                FordringLifecycleState.RESTANCE, FordringLifecycleState.OVERDRAGET))
        .isTrue();
    assertThat(
            service.canTransition(
                FordringLifecycleState.RESTANCE, FordringLifecycleState.TILBAGEKALDT))
        .isTrue();
    assertThat(
            service.canTransition(
                FordringLifecycleState.HOERING, FordringLifecycleState.OVERDRAGET))
        .isTrue();
    assertThat(
            service.canTransition(
                FordringLifecycleState.HOERING, FordringLifecycleState.REGISTERED))
        .isTrue();
    assertThat(
            service.canTransition(
                FordringLifecycleState.OVERDRAGET, FordringLifecycleState.TILBAGEKALDT))
        .isTrue();
    assertThat(
            service.canTransition(
                FordringLifecycleState.OVERDRAGET, FordringLifecycleState.AFSKREVET))
        .isTrue();
    assertThat(
            service.canTransition(
                FordringLifecycleState.OVERDRAGET, FordringLifecycleState.INDFRIET))
        .isTrue();
  }

  @Test
  void canTransition_invalidPairs() {
    assertThat(
            service.canTransition(
                FordringLifecycleState.REGISTERED, FordringLifecycleState.OVERDRAGET))
        .isFalse();
    assertThat(
            service.canTransition(
                FordringLifecycleState.REGISTERED, FordringLifecycleState.AFSKREVET))
        .isFalse();
    assertThat(
            service.canTransition(FordringLifecycleState.RESTANCE, FordringLifecycleState.HOERING))
        .isFalse();
    assertThat(
            service.canTransition(
                FordringLifecycleState.TILBAGEKALDT, FordringLifecycleState.REGISTERED))
        .isFalse();
    assertThat(
            service.canTransition(
                FordringLifecycleState.AFSKREVET, FordringLifecycleState.OVERDRAGET))
        .isFalse();
    assertThat(
            service.canTransition(
                FordringLifecycleState.INDFRIET, FordringLifecycleState.REGISTERED))
        .isFalse();
  }

  // =========================================================================
  // Helpers
  // =========================================================================

  private DebtEntity debtEntity(UUID id, FordringLifecycleState state) {
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
