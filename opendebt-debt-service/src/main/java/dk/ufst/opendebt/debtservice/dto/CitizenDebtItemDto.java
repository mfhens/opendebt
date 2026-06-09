package dk.ufst.opendebt.debtservice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Citizen-facing debt item DTO. Contains NO PII, NO creditor internals, NO readinessStatus (those
 * are internal only).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CitizenDebtItemDto {

  private UUID debtId;
  private String debtTypeCode;
  private String debtTypeName;
  private String creditorDisplayName;
  private BigDecimal principalAmount;
  private BigDecimal outstandingAmount;
  private BigDecimal interestAmount;
  private BigDecimal feesAmount;
  private LocalDate dueDate;
  private String status;
  private CitizenDebtStatus citizenStatus;
  private String statusReasonCode;
  private InterestAccrualState interestAccrualState;
  private InterestPauseReasonCode interestPauseReasonCode;
  private String interestRuleCode;
  private BigDecimal currentInterestRate;
  private WrittenOffReasonCode writtenOffReasonCode;
}
