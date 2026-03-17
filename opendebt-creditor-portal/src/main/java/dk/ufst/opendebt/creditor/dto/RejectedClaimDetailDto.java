package dk.ufst.opendebt.creditor.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import lombok.*;

/**
 * DTO representing the full rejected claim detail view data, including rejection reason, validation
 * errors, creditor info, financial amounts, interest info, and debtor list.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejectedClaimDetailDto {

  // --- Status ---
  private String actionStatus;
  private String rejectionReason;

  // --- ID numbers ---
  private UUID claimId;
  private String creditorReference;

  // --- Claim information ---
  private String claimType;
  private String creditorDescription;
  private LocalDate reportedDate;
  private LocalDate periodFrom;
  private LocalDate periodTo;
  private LocalDate incorporationDate;

  // --- Interest information ---
  private String interestRuleNumber;
  private String interestRateCode;

  // --- Creditor information ---
  private String creditorId;
  private String creditorName;

  // --- Amounts ---
  private BigDecimal originalAmount;
  private BigDecimal claimAmount;

  // --- Validation errors ---
  private List<ValidationErrorDto> validationErrors;

  // --- Caseworker remark ---
  private String caseworkerRemark;

  // --- Debtors ---
  private List<RejectedClaimDebtorDto> debtors;
}
