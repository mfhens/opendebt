package dk.ufst.opendebt.rules.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class InterestCalculationResultTest {

  @Test
  void noInterest_returnsZeroAmountWithReason() {
    InterestCalculationResult result = InterestCalculationResult.noInterest("Exempt from interest");

    assertThat(result.getInterestAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(result.getInterestRate()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(result.getRateType()).isEqualTo("NONE");
    assertThat(result.getLegalBasis()).isEqualTo("Exempt from interest");
    assertThat(result.getDaysCalculated()).isZero();
  }
}
