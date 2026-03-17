package dk.ufst.opendebt.debtservice.service;

import java.util.UUID;

import dk.ufst.opendebt.debtservice.entity.ClaimLifecycleState;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;

/** Service managing claim lifecycle state transitions (W7-LIFE-01). */
public interface ClaimLifecycleService {

  /**
   * Transition REGISTERED → RESTANCE when payment deadline has expired and outstanding balance > 0.
   */
  DebtEntity transitionToRestance(UUID debtId);

  /** Transition RESTANCE → OVERDRAGET with pre-condition checks for transfer for collection. */
  DebtEntity transferForCollection(UUID debtId);

  /** Transition REGISTERED → HOERING (entry filter deviation). */
  DebtEntity transitionToHearing(UUID debtId);

  /** Resolve hearing: HOERING → OVERDRAGET (accepted) or HOERING → REGISTERED (rejected). */
  DebtEntity resolveHearing(UUID debtId, boolean accepted);

  /** Any active (non-terminal) state → TILBAGEKALDT. */
  DebtEntity withdraw(UUID debtId, String reasonCode);

  /** OVERDRAGET → AFSKREVET. */
  DebtEntity writeOff(UUID debtId, String reasonCode);

  /** Any active (non-terminal) state → INDFRIET. */
  DebtEntity markFullyPaid(UUID debtId);

  /** Check whether a lifecycle transition from {@code from} to {@code to} is valid. */
  boolean canTransition(ClaimLifecycleState from, ClaimLifecycleState to);
}
