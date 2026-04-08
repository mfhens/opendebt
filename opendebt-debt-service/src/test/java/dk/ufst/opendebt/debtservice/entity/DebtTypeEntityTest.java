package dk.ufst.opendebt.debtservice.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DebtTypeEntityTest {

  @Test
  void builderUsesDefaultFlags() {
    DebtTypeEntity entity = DebtTypeEntity.builder().code("600").name("Standard debt").build();

    assertThat(entity.isActive()).isTrue();
    assertThat(entity.isRequiresManualReview()).isFalse();
    assertThat(entity.isInterestApplicable()).isTrue();
  }

  @Test
  void builderAllowsOverridingFlagsAndMetadata() {
    DebtTypeEntity entity =
        DebtTypeEntity.builder()
            .code("700")
            .name("Manual review debt")
            .category("MANUAL")
            .description("Needs manual handling")
            .legalBasis("Act 1")
            .active(false)
            .requiresManualReview(true)
            .interestApplicable(false)
            .build();

    assertThat(entity.getCode()).isEqualTo("700");
    assertThat(entity.getCategory()).isEqualTo("MANUAL");
    assertThat(entity.getDescription()).isEqualTo("Needs manual handling");
    assertThat(entity.getLegalBasis()).isEqualTo("Act 1");
    assertThat(entity.isActive()).isFalse();
    assertThat(entity.isRequiresManualReview()).isTrue();
    assertThat(entity.isInterestApplicable()).isFalse();
  }

  @Test
  void constructorsAndSettersWork() {
    DebtTypeEntity entity = new DebtTypeEntity();
    entity.setCode("800");
    entity.setName("Interest bearing debt");
    entity.setCategory("STANDARD");
    entity.setDescription("Description");
    entity.setLegalBasis("Act 2");
    entity.setActive(true);
    entity.setRequiresManualReview(false);
    entity.setInterestApplicable(true);

    DebtTypeEntity copied =
        new DebtTypeEntity(
            entity.getCode(),
            entity.getName(),
            entity.getCategory(),
            entity.getDescription(),
            entity.getLegalBasis(),
            entity.isActive(),
            entity.isRequiresManualReview(),
            entity.isInterestApplicable(),
            entity.isCivilLaw(),
            entity.getClaimTypeCode());

    assertThat(copied.getCode()).isEqualTo("800");
    assertThat(copied.getName()).isEqualTo("Interest bearing debt");
    assertThat(copied.isInterestApplicable()).isTrue();
  }
}
