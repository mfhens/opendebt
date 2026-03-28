package dk.ufst.opendebt.debtservice.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class FeeEntityTest {

  @Test
  void builder_setsAllFields() {
    UUID debtId = UUID.randomUUID();
    LocalDate date = LocalDate.of(2026, 3, 19);

    FeeEntity fee =
        FeeEntity.builder()
            .debtId(debtId)
            .feeType(FeeEntity.FeeType.RYKKER)
            .amount(new BigDecimal("250.00"))
            .accrualDate(date)
            .legalBasis("Inddrivelsesloven §2")
            .paid(false)
            .build();

    assertThat(fee.getDebtId()).isEqualTo(debtId);
    assertThat(fee.getFeeType()).isEqualTo(FeeEntity.FeeType.RYKKER);
    assertThat(fee.getAmount()).isEqualByComparingTo(new BigDecimal("250.00"));
    assertThat(fee.getAccrualDate()).isEqualTo(date);
    assertThat(fee.getLegalBasis()).isEqualTo("Inddrivelsesloven §2");
    assertThat(fee.isPaid()).isFalse();
  }

  @Test
  void feeType_hasAllExpectedValues() {
    assertThat(FeeEntity.FeeType.values())
        .containsExactly(
            FeeEntity.FeeType.RYKKER,
            FeeEntity.FeeType.UDLAEG,
            FeeEntity.FeeType.LOENINDEHOLDELSE,
            FeeEntity.FeeType.TILSIGELSE,
            FeeEntity.FeeType.OTHER);
  }

  @Test
  void paidDefaults_toFalse() {
    FeeEntity fee =
        FeeEntity.builder()
            .debtId(UUID.randomUUID())
            .feeType(FeeEntity.FeeType.UDLAEG)
            .amount(BigDecimal.TEN)
            .accrualDate(LocalDate.now())
            .build();

    assertThat(fee.isPaid()).isFalse();
  }
}
