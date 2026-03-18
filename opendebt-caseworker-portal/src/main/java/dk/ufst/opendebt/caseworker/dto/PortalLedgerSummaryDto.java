package dk.ufst.opendebt.caseworker.dto;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Portal-facing DTO representing a ledger balance summary for a debt. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalLedgerSummaryDto {

  private UUID debtId;
  private BigDecimal principalBalance;
  private BigDecimal interestBalance;
  private BigDecimal feesBalance;
  private BigDecimal totalBalance;
  private BigDecimal totalDebits;
  private BigDecimal totalCredits;
  private int entryCount;
}
