package dk.ufst.opendebt.payment.bookkeeping.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.*;

/**
 * Content model for an Allokeringsunderretning (Allocation Notification). Shows the allocation of
 * saldo changes between hovedstol and renter, including dækningsophævelser caused by crossing
 * transactions.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllocationNotification {

  private UUID debtId;
  private UUID creditorOrgId;
  private LocalDate notificationDate;
  private LocalDateTime generatedAt;
  private BigDecimal principalChange;
  private BigDecimal interestChange;
  private BigDecimal newPrincipalBalance;
  private BigDecimal newInterestBalance;
  private BigDecimal newTotalBalance;
  private List<AllocationLine> allocationLines;
  private List<ReversalLine> reversalLines;
  private boolean hasCrossingReversals;

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class AllocationLine {
    private LocalDate effectiveDate;
    private String transactionType;
    private BigDecimal totalAmount;
    private BigDecimal interestPortion;
    private BigDecimal principalPortion;
    private String reference;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class ReversalLine {
    private LocalDate effectiveDate;
    private BigDecimal originalInterestPortion;
    private BigDecimal originalPrincipalPortion;
    private BigDecimal correctedInterestPortion;
    private BigDecimal correctedPrincipalPortion;
    private String crossingTransactionReference;
    private String reason;
  }
}
