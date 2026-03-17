package dk.ufst.opendebt.creditor.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.*;

/**
 * DTO representing the full hearing claim detail view, including all sections required by petition
 * 031: status, IDs, fordringsinformation, fordringshaver-info, amounts, action code, write-up info,
 * and debtor list with error types.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HearingClaimDetailDto {

  // --- Claim status ---
  private String claimStatusCode;
  private String claimStatusText;

  // --- ID numbers ---
  private UUID claimId;
  private UUID caseId;
  private String actionId;
  private String creditorReference;
  private UUID mainClaimId;

  // --- Claim information ---
  private String claimTypeName;
  private String creditorDescription;
  private LocalDateTime reportingTimestamp;
  private LocalDate periodFrom;
  private LocalDate periodTo;
  private LocalDate incorporationDate;

  // --- Creditor info ---
  private UUID creditorOrgId;
  private String creditorName;

  // --- Amounts ---
  private BigDecimal originalPrincipal;
  private BigDecimal receivedAmount;

  // --- Action code ---
  private String actionCode;

  // --- Write-up info (conditionally shown based on action code) ---
  private BigDecimal writeUpAmount;
  private String writeUpReason;
  private String referenceActionId;
  private BigDecimal changedOriginalPrincipal;

  // --- Debtor list with error types ---
  private List<HearingDebtorErrorDto> debtorsWithErrors;
}
