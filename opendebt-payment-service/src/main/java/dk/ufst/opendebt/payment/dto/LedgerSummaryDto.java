package dk.ufst.opendebt.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO representing a summary of all ledger entries for a given debt. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerSummaryDto {

  private UUID debtId;
  private BigDecimal principalBalance;
  private BigDecimal interestBalance;
  private BigDecimal totalBalance;
  private BigDecimal totalPayments;
  private BigDecimal totalInterestAccrued;
  private BigDecimal totalWriteOffs;
  private BigDecimal totalCorrections;
  private LocalDate lastEventDate;
  private LocalDate lastPostingDate;
  private long entryCount;
  private long stornoCount;
}
