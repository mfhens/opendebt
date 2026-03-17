package dk.ufst.opendebt.creditor.dto;

import java.math.BigDecimal;

import lombok.*;

/**
 * DTO representing a single row in the financial breakdown table on the claim detail view. Each row
 * corresponds to a debt category, recovery interest, collection charges, or the total balance.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialBreakdownRowDto {

  private String category;
  private BigDecimal originalAmount;
  private BigDecimal writeOffAmount;
  private BigDecimal paymentAmount;
  private BigDecimal balance;
}
