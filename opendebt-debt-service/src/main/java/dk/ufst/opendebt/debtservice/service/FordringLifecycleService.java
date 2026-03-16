package dk.ufst.opendebt.debtservice.service;

import java.util.UUID;

import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.entity.FordringLifecycleState;

/** Service managing fordring lifecycle state transitions (W7-LIFE-01). */
public interface FordringLifecycleService {

  /** Transition REGISTERED → RESTANCE when SRB has expired and outstanding balance > 0. */
  DebtEntity transitionToRestance(UUID debtId);

  /** Transition RESTANCE → OVERDRAGET with pre-condition checks for overdragelse. */
  DebtEntity overdragTilInddrivelse(UUID debtId);

  /** Transition REGISTERED → HOERING (indgangsfilter deviation). */
  DebtEntity transitionToHoering(UUID debtId);

  /** Resolve hearing: HOERING → OVERDRAGET (accepted) or HOERING → REGISTERED (rejected). */
  DebtEntity resolveHoering(UUID debtId, boolean accepted);

  /** Any active (non-terminal) state → TILBAGEKALDT. */
  DebtEntity tilbagekald(UUID debtId, String aarsagskode);

  /** OVERDRAGET → AFSKREVET. */
  DebtEntity afskriv(UUID debtId, String reasonCode);

  /** Any active (non-terminal) state → INDFRIET. */
  DebtEntity markIndfriet(UUID debtId);

  /** Check whether a lifecycle transition from {@code from} to {@code to} is valid. */
  boolean canTransition(FordringLifecycleState from, FordringLifecycleState to);
}
