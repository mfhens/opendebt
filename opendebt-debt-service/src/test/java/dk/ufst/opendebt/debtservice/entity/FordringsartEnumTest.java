package dk.ufst.opendebt.debtservice.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FordringsartEnumTest {

  @Test
  void indrExists() {
    assertThat(FordringsartEnum.valueOf("INDR")).isEqualTo(FordringsartEnum.INDR);
  }

  @Test
  void modrExists() {
    assertThat(FordringsartEnum.valueOf("MODR")).isEqualTo(FordringsartEnum.MODR);
  }

  @Test
  void onlyTwoValues() {
    assertThat(FordringsartEnum.values()).hasSize(2);
  }
}
