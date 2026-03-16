package dk.ufst.opendebt.debtservice.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FordringLifecycleStateTest {

  @Test
  void allStatesExist() {
    assertThat(FordringLifecycleState.values()).hasSize(7);
  }

  @Test
  void registeredIsInitialState() {
    assertThat(FordringLifecycleState.valueOf("REGISTERED"))
        .isEqualTo(FordringLifecycleState.REGISTERED);
  }

  @Test
  void hoeringStateExists() {
    assertThat(FordringLifecycleState.valueOf("HOERING")).isEqualTo(FordringLifecycleState.HOERING);
  }
}
