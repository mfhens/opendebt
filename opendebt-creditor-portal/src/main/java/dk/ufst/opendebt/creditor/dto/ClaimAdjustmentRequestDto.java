package dk.ufst.opendebt.creditor.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.*;

/**
 * DTO representing a write-up or write-down adjustment request submitted via the creditor portal
 * (petition 034). Contains amount, effective date, reason, update type, and optional debtor index
 * for payment-related adjustments.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimAdjustmentRequestDto {

  @NotNull(message = "{adjustment.validation.type.required}")
  private ClaimAdjustmentType adjustmentType;

  @NotNull(message = "{adjustment.validation.amount.required}")
  @DecimalMin(value = "0.01", message = "{adjustment.validation.amount.min}")
  private BigDecimal amount;

  @NotNull(message = "{adjustment.validation.effectiveDate.required}")
  @DateTimeFormat(pattern = "yyyy-MM-dd")
  private LocalDate effectiveDate;

  /**
   * Write-down reason code (FR-1 / Gæld.bekendtg. § 7 stk. 2). Required for WRITE_DOWN direction;
   * null for WRITE_UP. Validated conditionally in {@code
   * ClaimAdjustmentController.submitAdjustment()}.
   */
  private WriteDownReasonCode writeDownReasonCode;

  /**
   * Index of the selected debtor in the claim's debtor list. Required for payment-related
   * adjustment types when the claim has multiple debtors.
   */
  private Integer debtorIndex;
}
