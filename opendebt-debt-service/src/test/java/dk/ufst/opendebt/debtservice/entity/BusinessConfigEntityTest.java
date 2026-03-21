package dk.ufst.opendebt.debtservice.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class BusinessConfigEntityTest {

  @Test
  void getDecimalValue_parsesConfigValue() {
    BusinessConfigEntity entity =
        BusinessConfigEntity.builder()
            .configKey("RATE_INDR_STD")
            .configValue("0.0575")
            .valueType("DECIMAL")
            .validFrom(LocalDate.of(2025, 1, 1))
            .build();

    assertThat(entity.getDecimalValue()).isEqualByComparingTo(new BigDecimal("0.0575"));
  }

  @Test
  void builder_setsAllFields() {
    LocalDate from = LocalDate.of(2025, 1, 1);
    LocalDate to = LocalDate.of(2025, 12, 31);
    BusinessConfigEntity entity =
        BusinessConfigEntity.builder()
            .configKey("RATE_INDR_STD")
            .configValue("0.0575")
            .valueType("DECIMAL")
            .validFrom(from)
            .validTo(to)
            .description("Test desc")
            .legalBasis("§5")
            .createdBy("admin")
            .build();

    assertThat(entity.getConfigKey()).isEqualTo("RATE_INDR_STD");
    assertThat(entity.getConfigValue()).isEqualTo("0.0575");
    assertThat(entity.getValueType()).isEqualTo("DECIMAL");
    assertThat(entity.getValidFrom()).isEqualTo(from);
    assertThat(entity.getValidTo()).isEqualTo(to);
    assertThat(entity.getDescription()).isEqualTo("Test desc");
    assertThat(entity.getLegalBasis()).isEqualTo("§5");
    assertThat(entity.getCreatedBy()).isEqualTo("admin");
  }
}
