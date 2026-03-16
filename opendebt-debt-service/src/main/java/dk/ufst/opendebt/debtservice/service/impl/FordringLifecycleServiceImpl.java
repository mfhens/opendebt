package dk.ufst.opendebt.debtservice.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.ufst.opendebt.common.exception.OpenDebtException;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.entity.FordringLifecycleState;
import dk.ufst.opendebt.debtservice.entity.OverdragelseEvent;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;
import dk.ufst.opendebt.debtservice.repository.OverdragelseEventRepository;
import dk.ufst.opendebt.debtservice.service.FordringLifecycleService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FordringLifecycleServiceImpl implements FordringLifecycleService {

  private final DebtRepository debtRepository;
  private final OverdragelseEventRepository overdragelseEventRepository;

  /** Terminal states that cannot transition to any other state. */
  private static final Set<FordringLifecycleState> TERMINAL_STATES =
      EnumSet.of(
          FordringLifecycleState.TILBAGEKALDT,
          FordringLifecycleState.AFSKREVET,
          FordringLifecycleState.INDFRIET);

  /** Allowed transitions: from-state → set of valid to-states. */
  private static final Map<FordringLifecycleState, Set<FordringLifecycleState>> ALLOWED_TRANSITIONS;

  static {
    ALLOWED_TRANSITIONS = new EnumMap<>(FordringLifecycleState.class);
    ALLOWED_TRANSITIONS.put(
        FordringLifecycleState.REGISTERED,
        EnumSet.of(
            FordringLifecycleState.RESTANCE,
            FordringLifecycleState.HOERING,
            FordringLifecycleState.TILBAGEKALDT,
            FordringLifecycleState.INDFRIET));
    ALLOWED_TRANSITIONS.put(
        FordringLifecycleState.RESTANCE,
        EnumSet.of(
            FordringLifecycleState.OVERDRAGET,
            FordringLifecycleState.TILBAGEKALDT,
            FordringLifecycleState.INDFRIET));
    ALLOWED_TRANSITIONS.put(
        FordringLifecycleState.HOERING,
        EnumSet.of(
            FordringLifecycleState.OVERDRAGET,
            FordringLifecycleState.REGISTERED,
            FordringLifecycleState.TILBAGEKALDT,
            FordringLifecycleState.INDFRIET));
    ALLOWED_TRANSITIONS.put(
        FordringLifecycleState.OVERDRAGET,
        EnumSet.of(
            FordringLifecycleState.TILBAGEKALDT,
            FordringLifecycleState.AFSKREVET,
            FordringLifecycleState.INDFRIET));
    // Terminal states have no outgoing transitions
    ALLOWED_TRANSITIONS.put(
        FordringLifecycleState.TILBAGEKALDT, EnumSet.noneOf(FordringLifecycleState.class));
    ALLOWED_TRANSITIONS.put(
        FordringLifecycleState.AFSKREVET, EnumSet.noneOf(FordringLifecycleState.class));
    ALLOWED_TRANSITIONS.put(
        FordringLifecycleState.INDFRIET, EnumSet.noneOf(FordringLifecycleState.class));
  }

  @Override
  @Transactional
  public DebtEntity transitionToRestance(UUID debtId) {
    DebtEntity debt = findDebt(debtId);
    validateTransition(debt, FordringLifecycleState.RESTANCE);

    if (debt.getSidsteRettigeBetalingsdato() == null) {
      throw new OpenDebtException(
          "SRB (sidste rettidige betalingsdato) must be set before transition to RESTANCE",
          "INVALID_LIFECYCLE_TRANSITION",
          OpenDebtException.ErrorSeverity.WARNING);
    }
    if (!debt.getSidsteRettigeBetalingsdato().isBefore(LocalDate.now())) {
      throw new OpenDebtException(
          "SRB has not expired yet — cannot transition to RESTANCE",
          "INVALID_LIFECYCLE_TRANSITION",
          OpenDebtException.ErrorSeverity.WARNING);
    }
    BigDecimal outstanding =
        debt.getOutstandingBalance() != null ? debt.getOutstandingBalance() : BigDecimal.ZERO;
    if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
      throw new OpenDebtException(
          "Outstanding balance must be > 0 to transition to RESTANCE",
          "INVALID_LIFECYCLE_TRANSITION",
          OpenDebtException.ErrorSeverity.WARNING);
    }

    FordringLifecycleState previous = debt.getLifecycleState();
    debt.setLifecycleState(FordringLifecycleState.RESTANCE);
    DebtEntity saved = debtRepository.save(debt);
    recordEvent(saved, previous, FordringLifecycleState.RESTANCE);
    log.info("Debt {} transitioned {} → RESTANCE", debtId, previous);
    return saved;
  }

  @Override
  @Transactional
  public DebtEntity overdragTilInddrivelse(UUID debtId) {
    DebtEntity debt = findDebt(debtId);

    if (debt.getLifecycleState() != FordringLifecycleState.RESTANCE) {
      throw new OpenDebtException(
          "Overdragelse requires state RESTANCE, current state: " + debt.getLifecycleState(),
          "INVALID_LIFECYCLE_TRANSITION",
          OpenDebtException.ErrorSeverity.WARNING);
    }
    if (debt.getSidsteRettigeBetalingsdato() == null
        || !debt.getSidsteRettigeBetalingsdato().isBefore(LocalDate.now())) {
      throw new OpenDebtException(
          "SRB must have expired before overdragelse",
          "INVALID_LIFECYCLE_TRANSITION",
          OpenDebtException.ErrorSeverity.WARNING);
    }
    BigDecimal outstanding =
        debt.getOutstandingBalance() != null ? debt.getOutstandingBalance() : BigDecimal.ZERO;
    if (outstanding.compareTo(BigDecimal.ZERO) == 0) {
      throw new OpenDebtException(
          "Cannot overdrage a 0-saldo fordring (unless rente reference holder)",
          "INVALID_LIFECYCLE_TRANSITION",
          OpenDebtException.ErrorSeverity.WARNING);
    }

    FordringLifecycleState previous = debt.getLifecycleState();
    debt.setLifecycleState(FordringLifecycleState.OVERDRAGET);
    debt.setModtagelsestidspunkt(LocalDateTime.now());
    DebtEntity saved = debtRepository.save(debt);
    recordEvent(saved, previous, FordringLifecycleState.OVERDRAGET);
    log.info("Debt {} overdraget til inddrivelse (RESTANCE → OVERDRAGET)", debtId);
    return saved;
  }

  @Override
  @Transactional
  public DebtEntity transitionToHoering(UUID debtId) {
    DebtEntity debt = findDebt(debtId);
    validateTransition(debt, FordringLifecycleState.HOERING);

    if (debt.getLifecycleState() != FordringLifecycleState.REGISTERED) {
      throw new OpenDebtException(
          "Transition to HOERING is only allowed from REGISTERED, current state: "
              + debt.getLifecycleState(),
          "INVALID_LIFECYCLE_TRANSITION",
          OpenDebtException.ErrorSeverity.WARNING);
    }

    FordringLifecycleState previous = debt.getLifecycleState();
    debt.setLifecycleState(FordringLifecycleState.HOERING);
    DebtEntity saved = debtRepository.save(debt);
    recordEvent(saved, previous, FordringLifecycleState.HOERING);
    log.info("Debt {} transitioned {} → HOERING", debtId, previous);
    return saved;
  }

  @Override
  @Transactional
  public DebtEntity resolveHoering(UUID debtId, boolean accepted) {
    DebtEntity debt = findDebt(debtId);

    if (debt.getLifecycleState() != FordringLifecycleState.HOERING) {
      throw new OpenDebtException(
          "resolveHoering requires state HOERING, current state: " + debt.getLifecycleState(),
          "INVALID_LIFECYCLE_TRANSITION",
          OpenDebtException.ErrorSeverity.WARNING);
    }

    FordringLifecycleState previous = debt.getLifecycleState();
    FordringLifecycleState target =
        accepted ? FordringLifecycleState.OVERDRAGET : FordringLifecycleState.REGISTERED;

    debt.setLifecycleState(target);
    if (accepted) {
      debt.setModtagelsestidspunkt(LocalDateTime.now());
    }
    DebtEntity saved = debtRepository.save(debt);
    recordEvent(saved, previous, target);
    log.info("Debt {} hearing resolved: HOERING → {} (accepted={})", debtId, target, accepted);
    return saved;
  }

  @Override
  @Transactional
  public DebtEntity tilbagekald(UUID debtId, String aarsagskode) {
    DebtEntity debt = findDebt(debtId);

    if (TERMINAL_STATES.contains(debt.getLifecycleState())) {
      throw new OpenDebtException(
          "Cannot tilbagekald from terminal state: " + debt.getLifecycleState(),
          "INVALID_LIFECYCLE_TRANSITION",
          OpenDebtException.ErrorSeverity.WARNING);
    }
    validateTransition(debt, FordringLifecycleState.TILBAGEKALDT);

    FordringLifecycleState previous = debt.getLifecycleState();
    debt.setLifecycleState(FordringLifecycleState.TILBAGEKALDT);
    DebtEntity saved = debtRepository.save(debt);
    recordEvent(saved, previous, FordringLifecycleState.TILBAGEKALDT);
    log.info(
        "Debt {} tilbagekaldt ({} → TILBAGEKALDT), årsagskode={}", debtId, previous, aarsagskode);
    return saved;
  }

  @Override
  @Transactional
  public DebtEntity afskriv(UUID debtId, String reasonCode) {
    DebtEntity debt = findDebt(debtId);

    if (debt.getLifecycleState() != FordringLifecycleState.OVERDRAGET) {
      throw new OpenDebtException(
          "Afskrivning requires state OVERDRAGET, current state: " + debt.getLifecycleState(),
          "INVALID_LIFECYCLE_TRANSITION",
          OpenDebtException.ErrorSeverity.WARNING);
    }

    FordringLifecycleState previous = debt.getLifecycleState();
    debt.setLifecycleState(FordringLifecycleState.AFSKREVET);
    DebtEntity saved = debtRepository.save(debt);
    recordEvent(saved, previous, FordringLifecycleState.AFSKREVET);
    log.info("Debt {} afskrevet (OVERDRAGET → AFSKREVET), reasonCode={}", debtId, reasonCode);
    return saved;
  }

  @Override
  @Transactional
  public DebtEntity markIndfriet(UUID debtId) {
    DebtEntity debt = findDebt(debtId);

    if (TERMINAL_STATES.contains(debt.getLifecycleState())) {
      throw new OpenDebtException(
          "Cannot mark as indfriet from terminal state: " + debt.getLifecycleState(),
          "INVALID_LIFECYCLE_TRANSITION",
          OpenDebtException.ErrorSeverity.WARNING);
    }
    validateTransition(debt, FordringLifecycleState.INDFRIET);

    FordringLifecycleState previous = debt.getLifecycleState();
    debt.setLifecycleState(FordringLifecycleState.INDFRIET);
    DebtEntity saved = debtRepository.save(debt);
    recordEvent(saved, previous, FordringLifecycleState.INDFRIET);
    log.info("Debt {} indfriet ({} → INDFRIET)", debtId, previous);
    return saved;
  }

  @Override
  public boolean canTransition(FordringLifecycleState from, FordringLifecycleState to) {
    Set<FordringLifecycleState> allowed = ALLOWED_TRANSITIONS.get(from);
    return allowed != null && allowed.contains(to);
  }

  // =========================================================================
  // Helpers
  // =========================================================================

  private DebtEntity findDebt(UUID debtId) {
    return debtRepository
        .findById(debtId)
        .orElseThrow(
            () ->
                new OpenDebtException(
                    "Debt not found: " + debtId,
                    "DEBT_NOT_FOUND",
                    OpenDebtException.ErrorSeverity.WARNING));
  }

  private void validateTransition(DebtEntity debt, FordringLifecycleState target) {
    FordringLifecycleState current = debt.getLifecycleState();
    if (current == null) {
      throw new OpenDebtException(
          "Debt " + debt.getId() + " has no lifecycle state set",
          "INVALID_LIFECYCLE_TRANSITION",
          OpenDebtException.ErrorSeverity.ERROR);
    }
    if (!canTransition(current, target)) {
      throw new OpenDebtException(
          "Invalid lifecycle transition: " + current + " → " + target,
          "INVALID_LIFECYCLE_TRANSITION",
          OpenDebtException.ErrorSeverity.WARNING);
    }
  }

  private void recordEvent(
      DebtEntity debt, FordringLifecycleState previous, FordringLifecycleState newState) {
    OverdragelseEvent event =
        OverdragelseEvent.builder()
            .debtId(debt.getId())
            .fordringshaverId(debt.getCreditorOrgId())
            .previousState(previous != null ? previous.name() : null)
            .newState(newState.name())
            .build();
    overdragelseEventRepository.save(event);
  }
}
