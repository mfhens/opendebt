package dk.ufst.rules.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Builder;
import lombok.Data;

/** Request to calculate interest on a debt. */
@Data
@Builder
public class InterestCalculationRequest {

  private String debtTypeCode;
  private BigDecimal principalAmount;
  private LocalDate dueDate;
  private LocalDate calculationDate;
  private int daysPastDue;

  private boolean isPublicDebt;
  private String creditorType;

  /**
   * Interest regime code (e.g. "INDR_STD", "INDR_EXEMPT"). Pre-resolved by the caller; the DRL
   * branches on this value.
   */
  private String interestRule;

  /**
   * Annual interest rate as a decimal fraction (e.g. 0.0575 for 5.75%). Must be resolved by the
   * caller using BusinessConfigService.getDecimalValue(configKey, calculationDate) so that
   * retroactive recalculations use the historically correct rate.
   */
  private BigDecimal annualRate;
}
