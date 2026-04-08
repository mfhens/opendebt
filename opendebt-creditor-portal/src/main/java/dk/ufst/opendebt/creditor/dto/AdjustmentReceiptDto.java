package dk.ufst.opendebt.creditor.dto;

import java.math.BigDecimal;

import lombok.*;

/**
 * DTO representing the receipt returned after a successful claim adjustment (petition 034). Shows
 * the action ID, status, and amount of the processed write-up or write-down.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdjustmentReceiptDto {

  private String actionId;
  private String status;
  private BigDecimal amount;
  private String debtorIdentifier;
  private String adjustmentType;

  /**
   * GIL § 18 k flag (FR-6 / SPEC-P053 §6.3). True when virkningsdato is before the PSRM
   * registration date of the claim, indicating a cross-system retroactive nedskrivning.
   */
  private boolean crossSystemRetroactiveApplies;
}
