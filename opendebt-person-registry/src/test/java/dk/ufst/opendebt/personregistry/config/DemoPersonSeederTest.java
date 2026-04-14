package dk.ufst.opendebt.personregistry.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DemoPersonSeederTest {

  @Test
  void seededPersonIds_coverSeededCasePartyDebtors() {
    assertThat(DemoPersonSeeder.seededPersonIds())
        .contains(
            "d0000000-0000-0000-0000-000000000001",
            "d0000000-0000-0000-0000-000000000002",
            "d0000000-0000-0000-0000-000000000003");
  }
}
