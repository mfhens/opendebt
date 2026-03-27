package dk.ufst.opendebt.debtservice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Detail view DTO for a single claim, consumed by the creditor portal's ClaimDetailController.
 * Field names mirror the portal's ClaimDetailDto so Jackson deserialises cleanly.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimDetailResponseDto {

  // Claim information
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
  private String creditorReference;
  private String obligationId;
  private String relatedObligationId;

  // Financial summary
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

  // Debtor count
  private int debtorCount;

  // Sub-lists (empty for now — no PII, no write-up data in debt-service)
  @Builder.Default private List<Object> financialBreakdown = Collections.emptyList();
  @Builder.Default private List<Object> writeUps = Collections.emptyList();
  @Builder.Default private List<Object> writeDowns = Collections.emptyList();
  @Builder.Default private List<Object> relatedClaims = Collections.emptyList();
  @Builder.Default private List<Object> debtors = Collections.emptyList();
  @Builder.Default private List<Object> decisions = Collections.emptyList();

  private boolean zeroBalanceExpired;
}
