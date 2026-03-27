package dk.ufst.opendebt.creditor.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalDebtDto {

  // Identity
  private UUID id;
  private UUID debtorPersonId;
  private UUID creditorOrgId;

  // Core amounts
  private BigDecimal principalAmount;
  private BigDecimal outstandingBalance;

  // Status (read-only; not sent on submission)
  private String status;

  // Dates
  private LocalDate dueDate;
  private LocalDate periodFrom;
  private LocalDate periodTo;
  private LocalDate inceptionDate;
  private LocalDate limitationDate;
  private LocalDate judgmentDate;
  private LocalDate settlementDate;

  // Classification
  private String debtTypeCode;
  private String creditorReference;

  // Text
  private String description;
  private String claimNote;
  private String customerNote;

  // Interest
  private String interestRule;
  private String interestRateCode;
  private BigDecimal additionalInterestRate;

  // Flags
  private Boolean estateProcessing;
}
