package dk.ufst.opendebt.rules.model;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

/** Result of interest calculation. */
@Data
@Builder
public class InterestCalculationResult {

  private BigDecimal interestAmount;
  private BigDecimal interestRate;
  private String rateType;
  private String legalBasis;
  private int daysCalculated;

  public static InterestCalculationResult noInterest(String reason) {
    return InterestCalculationResult.builder()
        .interestAmount(BigDecimal.ZERO)
        .interestRate(BigDecimal.ZERO)
        .rateType("NONE")
        .legalBasis(reason)
        .daysCalculated(0)
        .build();
  }
}
