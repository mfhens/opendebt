package dk.ufst.opendebt.rules.model;

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
}
