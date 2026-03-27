package dk.ufst.opendebt.debtservice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditorClaimListItemDto {
  // Common
  private UUID claimId;
  private String creditorReference;
  private String claimTypeName; // = debtTypeCode
  private String debtorType; // null (PII in Person Registry)
  private String debtorIdentifier; // null (PII in Person Registry)
  private int debtorCount; // 1

  // For ClaimListItemDto (recovery/zero-balance/rejected)
  private LocalDate receivedDate;
  private String claimStatus; // = lifecycleState.name()
  private LocalDate incorporationDate;
  private LocalDate periodFrom;
  private LocalDate periodTo;
  private BigDecimal amountSentForRecovery; // = principalAmount
  private BigDecimal balance; // = outstandingBalance
  private BigDecimal balanceWithInterestAndFees; // = principalAmount + interestAmount + feesAmount
  private LocalDate zeroBalanceReachedDate; // null

  // For HearingClaimListItemDto
  private LocalDateTime reportingTimestamp;
  private String errorDescription; // null
  private int errorCount; // 0
  private String hearingStatus; // = lifecycleState.name()
  private UUID caseId; // null
  private String actionCode; // null
}
