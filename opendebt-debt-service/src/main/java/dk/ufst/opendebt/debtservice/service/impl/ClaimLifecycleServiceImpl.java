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
import dk.ufst.opendebt.debtservice.entity.ClaimLifecycleEvent;
import dk.ufst.opendebt.debtservice.entity.ClaimLifecycleState;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.repository.ClaimLifecycleEventRepository;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;
import dk.ufst.opendebt.debtservice.service.ClaimLifecycleService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimLifecycleServiceImpl implements ClaimLifecycleService {

  private final DebtRepository debtRepository;
  private final ClaimLifecycleEventRepository claimLifecycleEventRepository;

  /** Terminal states that cannot transition to any other state. */
  private static final Set<ClaimLifecycleState> TERMINAL_STATES =
      EnumSet.of(
          ClaimLifecycleState.TILBAGEKALDT,
          ClaimLifecycleState.AFSKREVET,
          ClaimLifecycleState.INDFRIET);

  /** Allowed transitions: from-state → set of valid to-states. */
  private static final Map<ClaimLifecycleState, Set<ClaimLifecycleState>> ALLOWED_TRANSITIONS;

  static {
    ALLOWED_TRANSITIONS = new EnumMap<>(ClaimLifecycleState.class);
    ALLOWED_TRANSITIONS.put(
        ClaimLifecycleState.REGISTERED,
        EnumSet.of(
            ClaimLifecycleState.RESTANCE,
            ClaimLifecycleState.HOERING,
            ClaimLifecycleState.TILBAGEKALDT,
            ClaimLifecycleState.INDFRIET));
    ALLOWED_TRANSITIONS.put(
        ClaimLifecycleState.RESTANCE,
        EnumSet.of(
            ClaimLifecycleState.OVERDRAGET,
            ClaimLifecycleState.TILBAGEKALDT,
            ClaimLifecycleState.INDFRIET));
    ALLOWED_TRANSITIONS.put(
        ClaimLifecycleState.HOERING,
        EnumSet.of(
            ClaimLifecycleState.OVERDRAGET,
            ClaimLifecycleState.REGISTERED,
            ClaimLifecycleState.TILBAGEKALDT,
            ClaimLifecycleState.INDFRIET));
    ALLOWED_TRANSITIONS.put(
        ClaimLifecycleState.OVERDRAGET,
        EnumSet.of(
            ClaimLifecycleState.TILBAGEKALDT,
            ClaimLifecycleState.AFSKREVET,
            ClaimLifecycleState.INDFRIET));
    // Terminal states have no outgoing transitions
    ALLOWED_TRANSITIONS.put(
        ClaimLifecycleState.TILBAGEKALDT, EnumSet.noneOf(ClaimLifecycleState.class));
    ALLOWED_TRANSITIONS.put(
        ClaimLifecycleState.AFSKREVET, EnumSet.noneOf(ClaimLifecycleState.class));
    ALLOWED_TRANSITIONS.put(
        ClaimLifecycleState.INDFRIET, EnumSet.noneOf(ClaimLifecycleState.class));
  }

  @Override
  @Transactional
  public DebtEntity transitionToRestance(UUID debtId) {
    DebtEntity debt = findDebt(debtId);
    validateTransition(debt, ClaimLifecycleState.RESTANCE);

    if (debt.getLastPaymentDate() == null) {
      throw new OpenDebtException(
          "Last payment date must be set before transition to RESTANCE",
          "INVALID_LIFECYCLE_TRANSITION",
          OpenDebtException.ErrorSeverity.WARNING);
    }
    if (!debt.getLastPaymentDate().isBefore(LocalDate.now())) {
      throw new OpenDebtException(
          "Last payment date has not expired yet — cannot transition to RESTANCE",
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

    ClaimLifecycleState previous = debt.getLifecycleState();
    debt.setLifecycleState(ClaimLifecycleState.RESTANCE);
    DebtEntity saved = debtRepository.save(debt);
    recordEvent(saved, previous, ClaimLifecycleState.RESTANCE);
    log.info("Debt {} transitioned {} → RESTANCE", debtId, previous);
    return saved;
  }

  @Override
  @Transactional
  public DebtEntity transferForCollection(UUID debtId) {
    DebtEntity debt = findDebt(debtId);

    if (debt.getLifecycleState() != ClaimLifecycleState.RESTANCE) {
      throw new OpenDebtException(
          "Transfer for collection requires state RESTANCE, current state: "
              + debt.getLifecycleState(),
          "INVALID_LIFECYCLE_TRANSITION",
          OpenDebtException.ErrorSeverity.WARNING);
    }
    if (debt.getLastPaymentDate() == null || !debt.getLastPaymentDate().isBefore(LocalDate.now())) {
      throw new OpenDebtException(
          "Last payment date must have expired before transfer for collection",
          "INVALID_LIFECYCLE_TRANSITION",
          OpenDebtException.ErrorSeverity.WARNING);
    }
    BigDecimal outstanding =
        debt.getOutstandingBalance() != null ? debt.getOutstandingBalance() : BigDecimal.ZERO;
    if (outstanding.compareTo(BigDecimal.ZERO) == 0) {
      throw new OpenDebtException(
          "Cannot transfer a zero-balance claim (unless interest reference holder)",
          "INVALID_LIFECYCLE_TRANSITION",
          OpenDebtException.ErrorSeverity.WARNING);
    }

    ClaimLifecycleState previous = debt.getLifecycleState();
    debt.setLifecycleState(ClaimLifecycleState.OVERDRAGET);
    debt.setReceivedAt(LocalDateTime.now());
    DebtEntity saved = debtRepository.save(debt);
    recordEvent(saved, previous, ClaimLifecycleState.OVERDRAGET);
    log.info("Debt {} transferred for collection (RESTANCE → OVERDRAGET)", debtId);
    return saved;
  }

  @Override
  @Transactional
  public DebtEntity transitionToHearing(UUID debtId) {
    DebtEntity debt = findDebt(debtId);
    validateTransition(debt, ClaimLifecycleState.HOERING);

    if (debt.getLifecycleState() != ClaimLifecycleState.REGISTERED) {
      throw new OpenDebtException(
          "Transition to HOERING is only allowed from REGISTERED, current state: "
              + debt.getLifecycleState(),
          "INVALID_LIFECYCLE_TRANSITION",
          OpenDebtException.ErrorSeverity.WARNING);
    }

    ClaimLifecycleState previous = debt.getLifecycleState();
    debt.setLifecycleState(ClaimLifecycleState.HOERING);
    DebtEntity saved = debtRepository.save(debt);
    recordEvent(saved, previous, ClaimLifecycleState.HOERING);
    log.info("Debt {} transitioned {} → HOERING", debtId, previous);
    return saved;
  }

  @Override
  @Transactional
  public DebtEntity resolveHearing(UUID debtId, boolean accepted) {
    DebtEntity debt = findDebt(debtId);

    if (debt.getLifecycleState() != ClaimLifecycleState.HOERING) {
      throw new OpenDebtException(
          "resolveHearing requires state HOERING, current state: " + debt.getLifecycleState(),
          "INVALID_LIFECYCLE_TRANSITION",
          OpenDebtException.ErrorSeverity.WARNING);
    }

    ClaimLifecycleState previous = debt.getLifecycleState();
    ClaimLifecycleState target =
        accepted ? ClaimLifecycleState.OVERDRAGET : ClaimLifecycleState.REGISTERED;

    debt.setLifecycleState(target);
    if (accepted) {
      debt.setReceivedAt(LocalDateTime.now());
    }
    DebtEntity saved = debtRepository.save(debt);
    recordEvent(saved, previous, target);
    log.info("Debt {} hearing resolved: HOERING → {} (accepted={})", debtId, target, accepted);
    return saved;
  }

  @Override
  @Transactional
  public DebtEntity withdraw(UUID debtId, String reasonCode) {
    DebtEntity debt = findDebt(debtId);

    if (TERMINAL_STATES.contains(debt.getLifecycleState())) {
      throw new OpenDebtException(
          "Cannot withdraw from terminal state: " + debt.getLifecycleState(),
          "INVALID_LIFECYCLE_TRANSITION",
          OpenDebtException.ErrorSeverity.WARNING);
    }
    validateTransition(debt, ClaimLifecycleState.TILBAGEKALDT);

    ClaimLifecycleState previous = debt.getLifecycleState();
    debt.setLifecycleState(ClaimLifecycleState.TILBAGEKALDT);
    DebtEntity saved = debtRepository.save(debt);
    recordEvent(saved, previous, ClaimLifecycleState.TILBAGEKALDT);
    log.info("Debt {} withdrawn ({} → TILBAGEKALDT), reasonCode={}", debtId, previous, reasonCode);
    return saved;
  }

  @Override
  @Transactional
  public DebtEntity writeOff(UUID debtId, String reasonCode) {
    DebtEntity debt = findDebt(debtId);

    if (debt.getLifecycleState() != ClaimLifecycleState.OVERDRAGET) {
      throw new OpenDebtException(
          "Write-off requires state OVERDRAGET, current state: " + debt.getLifecycleState(),
          "INVALID_LIFECYCLE_TRANSITION",
          OpenDebtException.ErrorSeverity.WARNING);
    }

    ClaimLifecycleState previous = debt.getLifecycleState();
    debt.setLifecycleState(ClaimLifecycleState.AFSKREVET);
    DebtEntity saved = debtRepository.save(debt);
    recordEvent(saved, previous, ClaimLifecycleState.AFSKREVET);
    log.info("Debt {} written off (OVERDRAGET → AFSKREVET), reasonCode={}", debtId, reasonCode);
    return saved;
  }

  @Override
  @Transactional
  public DebtEntity markFullyPaid(UUID debtId) {
    DebtEntity debt = findDebt(debtId);

    if (TERMINAL_STATES.contains(debt.getLifecycleState())) {
      throw new OpenDebtException(
          "Cannot mark as fully paid from terminal state: " + debt.getLifecycleState(),
          "INVALID_LIFECYCLE_TRANSITION",
          OpenDebtException.ErrorSeverity.WARNING);
    }
    validateTransition(debt, ClaimLifecycleState.INDFRIET);

    ClaimLifecycleState previous = debt.getLifecycleState();
    debt.setLifecycleState(ClaimLifecycleState.INDFRIET);
    DebtEntity saved = debtRepository.save(debt);
    recordEvent(saved, previous, ClaimLifecycleState.INDFRIET);
    log.info("Debt {} fully paid ({} → INDFRIET)", debtId, previous);
    return saved;
  }

  @Override
  public boolean canTransition(ClaimLifecycleState from, ClaimLifecycleState to) {
    Set<ClaimLifecycleState> allowed = ALLOWED_TRANSITIONS.get(from);
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

  private void validateTransition(DebtEntity debt, ClaimLifecycleState target) {
    ClaimLifecycleState current = debt.getLifecycleState();
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
      DebtEntity debt, ClaimLifecycleState previous, ClaimLifecycleState newState) {
    ClaimLifecycleEvent event =
        ClaimLifecycleEvent.builder()
            .debtId(debt.getId())
            .creditorId(debt.getCreditorOrgId())
            .previousState(previous != null ? previous.name() : null)
            .newState(newState.name())
            .build();
    claimLifecycleEventRepository.save(event);
  }
}
