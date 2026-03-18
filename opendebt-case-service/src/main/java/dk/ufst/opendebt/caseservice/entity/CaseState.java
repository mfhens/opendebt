package dk.ufst.opendebt.caseservice.entity;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * OIO Sag-aligned case lifecycle states. Replaces the flat CaseStatus enum.
 *
 * <p>Valid transitions are enforced via {@link #canTransitionTo(CaseState)}.
 */
public enum CaseState {
  CREATED,
  ASSESSED,
  DECIDED,
  SUSPENDED,
  CLOSED_PAID,
  CLOSED_WRITTEN_OFF,
  CLOSED_WITHDRAWN,
  CLOSED_CANCELLED;

  private static final Map<CaseState, Set<CaseState>> VALID_TRANSITIONS =
      Map.of(
          CREATED, EnumSet.of(ASSESSED, CLOSED_CANCELLED),
          ASSESSED, EnumSet.of(DECIDED, SUSPENDED, CLOSED_CANCELLED),
          DECIDED,
              EnumSet.of(
                  SUSPENDED, CLOSED_PAID, CLOSED_WRITTEN_OFF, CLOSED_WITHDRAWN, CLOSED_CANCELLED),
          SUSPENDED, EnumSet.of(ASSESSED, DECIDED, CLOSED_CANCELLED),
          CLOSED_PAID, EnumSet.noneOf(CaseState.class),
          CLOSED_WRITTEN_OFF, EnumSet.noneOf(CaseState.class),
          CLOSED_WITHDRAWN, EnumSet.noneOf(CaseState.class),
          CLOSED_CANCELLED, EnumSet.noneOf(CaseState.class));

  /**
   * Returns {@code true} if a transition from this state to {@code target} is allowed.
   *
   * @param target the desired target state
   * @return whether the transition is valid
   */
  public boolean canTransitionTo(CaseState target) {
    return VALID_TRANSITIONS.getOrDefault(this, EnumSet.noneOf(CaseState.class)).contains(target);
  }

  /** Returns {@code true} if this state represents a terminal (closed) state. */
  public boolean isClosed() {
    return name().startsWith("CLOSED_");
  }
}
