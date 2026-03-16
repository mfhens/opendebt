package dk.ufst.opendebt.debtservice.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

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
    LocalDateTime createdAt = LocalDateTime.of(2026, 3, 6, 12, 0);
    LocalDateTime updatedAt = LocalDateTime.of(2026, 3, 6, 12, 30);
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
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .build();

    assertThat(entity.getCode()).isEqualTo("700");
    assertThat(entity.getCategory()).isEqualTo("MANUAL");
    assertThat(entity.getDescription()).isEqualTo("Needs manual handling");
    assertThat(entity.getLegalBasis()).isEqualTo("Act 1");
    assertThat(entity.isActive()).isFalse();
    assertThat(entity.isRequiresManualReview()).isTrue();
    assertThat(entity.isInterestApplicable()).isFalse();
    assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
    assertThat(entity.getUpdatedAt()).isEqualTo(updatedAt);
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
            entity.isCivilretlig(),
            entity.getFordringstypeKode(),
            entity.getCreatedAt(),
            entity.getUpdatedAt());

    assertThat(copied.getCode()).isEqualTo("800");
    assertThat(copied.getName()).isEqualTo("Interest bearing debt");
    assertThat(copied.isInterestApplicable()).isTrue();
  }
}
