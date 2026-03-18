package dk.ufst.opendebt.caseservice.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class CaseStateTest {

  // ── Valid transitions ────────────────────────────────────────────────

  @Test
  void created_canTransitionToAssessed() {
    assertThat(CaseState.CREATED.canTransitionTo(CaseState.ASSESSED)).isTrue();
  }

  @Test
  void created_canTransitionToClosedCancelled() {
    assertThat(CaseState.CREATED.canTransitionTo(CaseState.CLOSED_CANCELLED)).isTrue();
  }

  @Test
  void assessed_canTransitionToDecided() {
    assertThat(CaseState.ASSESSED.canTransitionTo(CaseState.DECIDED)).isTrue();
  }

  @Test
  void assessed_canTransitionToSuspended() {
    assertThat(CaseState.ASSESSED.canTransitionTo(CaseState.SUSPENDED)).isTrue();
  }

  @Test
  void assessed_canTransitionToClosedCancelled() {
    assertThat(CaseState.ASSESSED.canTransitionTo(CaseState.CLOSED_CANCELLED)).isTrue();
  }

  @Test
  void decided_canTransitionToSuspended() {
    assertThat(CaseState.DECIDED.canTransitionTo(CaseState.SUSPENDED)).isTrue();
  }

  @Test
  void decided_canTransitionToClosedPaid() {
    assertThat(CaseState.DECIDED.canTransitionTo(CaseState.CLOSED_PAID)).isTrue();
  }

  @Test
  void decided_canTransitionToClosedWrittenOff() {
    assertThat(CaseState.DECIDED.canTransitionTo(CaseState.CLOSED_WRITTEN_OFF)).isTrue();
  }

  @Test
  void decided_canTransitionToClosedWithdrawn() {
    assertThat(CaseState.DECIDED.canTransitionTo(CaseState.CLOSED_WITHDRAWN)).isTrue();
  }

  @Test
  void decided_canTransitionToClosedCancelled() {
    assertThat(CaseState.DECIDED.canTransitionTo(CaseState.CLOSED_CANCELLED)).isTrue();
  }

  @Test
  void suspended_canTransitionToAssessed() {
    assertThat(CaseState.SUSPENDED.canTransitionTo(CaseState.ASSESSED)).isTrue();
  }

  @Test
  void suspended_canTransitionToDecided() {
    assertThat(CaseState.SUSPENDED.canTransitionTo(CaseState.DECIDED)).isTrue();
  }

  @Test
  void suspended_canTransitionToClosedCancelled() {
    assertThat(CaseState.SUSPENDED.canTransitionTo(CaseState.CLOSED_CANCELLED)).isTrue();
  }

  // ── Invalid transitions ──────────────────────────────────────────────

  @Test
  void created_cannotTransitionToClosedPaid() {
    assertThat(CaseState.CREATED.canTransitionTo(CaseState.CLOSED_PAID)).isFalse();
  }

  @Test
  void created_cannotTransitionToDecided() {
    assertThat(CaseState.CREATED.canTransitionTo(CaseState.DECIDED)).isFalse();
  }

  @Test
  void assessed_cannotTransitionToClosedPaid() {
    assertThat(CaseState.ASSESSED.canTransitionTo(CaseState.CLOSED_PAID)).isFalse();
  }

  @Test
  void closedPaid_cannotTransitionToAnything() {
    for (CaseState target : CaseState.values()) {
      assertThat(CaseState.CLOSED_PAID.canTransitionTo(target)).isFalse();
    }
  }

  @Test
  void closedWrittenOff_cannotTransitionToAnything() {
    for (CaseState target : CaseState.values()) {
      assertThat(CaseState.CLOSED_WRITTEN_OFF.canTransitionTo(target)).isFalse();
    }
  }

  @Test
  void closedWithdrawn_cannotTransitionToAnything() {
    for (CaseState target : CaseState.values()) {
      assertThat(CaseState.CLOSED_WITHDRAWN.canTransitionTo(target)).isFalse();
    }
  }

  @Test
  void closedCancelled_cannotTransitionToAnything() {
    for (CaseState target : CaseState.values()) {
      assertThat(CaseState.CLOSED_CANCELLED.canTransitionTo(target)).isFalse();
    }
  }

  // ── isClosed tests ───────────────────────────────────────────────────

  @ParameterizedTest
  @EnumSource(
      value = CaseState.class,
      names = {"CLOSED_PAID", "CLOSED_WRITTEN_OFF", "CLOSED_WITHDRAWN", "CLOSED_CANCELLED"})
  void closedStates_returnTrueForIsClosed(CaseState state) {
    assertThat(state.isClosed()).isTrue();
  }

  @ParameterizedTest
  @EnumSource(
      value = CaseState.class,
      names = {"CREATED", "ASSESSED", "DECIDED", "SUSPENDED"})
  void nonClosedStates_returnFalseForIsClosed(CaseState state) {
    assertThat(state.isClosed()).isFalse();
  }
}
