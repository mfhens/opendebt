package dk.ufst.opendebt.debtservice.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ClaimArtEnumTest {

  @Test
  void indrExists() {
    assertThat(ClaimArtEnum.valueOf("INDR")).isEqualTo(ClaimArtEnum.INDR);
  }

  @Test
  void modrExists() {
    assertThat(ClaimArtEnum.valueOf("MODR")).isEqualTo(ClaimArtEnum.MODR);
  }

  @Test
  void onlyTwoValues() {
    assertThat(ClaimArtEnum.values()).hasSize(2);
  }
}
