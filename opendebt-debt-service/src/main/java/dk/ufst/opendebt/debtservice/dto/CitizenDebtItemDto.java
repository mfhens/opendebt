package dk.ufst.opendebt.debtservice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

/**
 * Citizen-facing debt item DTO. Contains NO PII, NO creditor internals, NO readinessStatus (those
 * are internal only).
 */
@Data
@Builder
public class CitizenDebtItemDto {

  private UUID debtId;
  private String debtTypeCode;
  private String debtTypeName;
  private BigDecimal principalAmount;
  private BigDecimal outstandingAmount;
  private BigDecimal interestAmount;
  private BigDecimal feesAmount;
  private LocalDate dueDate;
  private String status;
  private String lifecycleState;
}
