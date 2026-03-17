package dk.ufst.opendebt.creditor.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import lombok.*;

/** DTO representing a single claim row in the recovery or zero-balance claims list. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimListItemDto {

  private UUID claimId;
  private LocalDate receivedDate;
  private String debtorType;
  private String debtorIdentifier;
  private int debtorCount;
  private String creditorReference;
  private String claimTypeName;
  private String claimStatus;
  private LocalDate incorporationDate;
  private LocalDate periodFrom;
  private LocalDate periodTo;
  private BigDecimal amountSentForRecovery;
  private BigDecimal balance;
  private BigDecimal balanceWithInterestAndFees;
  private LocalDate zeroBalanceReachedDate;
}
