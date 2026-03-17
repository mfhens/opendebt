package dk.ufst.opendebt.creditor.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import lombok.*;

/**
 * DTO representing the full claim detail view data, aggregating claim information, financial
 * breakdown, write-ups, write-downs, related claims, debtors, and decisions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimDetailDto {

  // --- Claim information ---
  private UUID claimId;
  private String claimType;
  private String claimCategory;
  private String creditorDescription;
  private LocalDate receivedDate;
  private LocalDate periodFrom;
  private LocalDate periodTo;
  private LocalDate incorporationDate;
  private LocalDate dueDate;
  private LocalDate limitationDate;
  private LocalDate courtDate;
  private LocalDate lastTimelyPaymentDate;
  private String obligationId;
  private String creditorReference;
  private String relatedObligationId;

  // --- Financial summary ---
  private String interestRule;
  private BigDecimal interestRate;
  private BigDecimal extraInterestRate;
  private BigDecimal totalDebt;
  private LocalDate latestInterestAccrualDate;
  private BigDecimal originalPrincipal;
  private BigDecimal receivedAmount;
  private BigDecimal claimBalance;
  private BigDecimal totalCreditorBalance;
  private BigDecimal amountSentForRecovery;
  private BigDecimal amountSentForRecoveryWithWriteUps;

  // --- Debtor count (used for conditional display) ---
  private int debtorCount;

  // --- Financial breakdown rows ---
  private List<FinancialBreakdownRowDto> financialBreakdown;

  // --- Write-ups ---
  private List<WriteUpDto> writeUps;

  // --- Write-downs ---
  private List<WriteDownDto> writeDowns;

  // --- Related claims (underfordringer) ---
  private List<RelatedClaimDto> relatedClaims;

  // --- Debtors (from haeftelsesstruktur) ---
  private List<DebtorInfoDto> debtors;

  // --- Decisions (afgoerelser) ---
  private List<DecisionDto> decisions;

  // --- Zero-balance metadata ---
  private boolean zeroBalanceExpired;
}
