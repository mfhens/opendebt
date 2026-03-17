package dk.ufst.opendebt.debtservice.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ClaimLifecycleStateTest {

  @Test
  void allStatesExist() {
    assertThat(ClaimLifecycleState.values()).hasSize(7);
  }

  @Test
  void registeredIsInitialState() {
    assertThat(ClaimLifecycleState.valueOf("REGISTERED")).isEqualTo(ClaimLifecycleState.REGISTERED);
  }

  @Test
  void hoeringStateExists() {
    assertThat(ClaimLifecycleState.valueOf("HOERING")).isEqualTo(ClaimLifecycleState.HOERING);
  }
}
