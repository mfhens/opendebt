package dk.ufst.opendebt.debtservice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for the {@code POST /api/v1/debts/{id}/adjustments} endpoint (SPEC-P053 §9.0).
 *
 * <p>This is the debt-service bounded-context DTO. It is separate from the portal's same-named
 * class in {@code dk.ufst.opendebt.creditor.dto}.
 *
 * <p>Field constraints per SPEC-P053 §9.0:
 *
 * <ul>
 *   <li>{@code adjustmentType} — required; must be a valid {@code ClaimAdjustmentType} name
 *   <li>{@code amount} — required; must be &gt; 0
 *   <li>{@code effectiveDate} — required; ISO-8601 date (virkningsdato)
 *   <li>{@code writeDownReasonCode} — required when direction is WRITE_DOWN (FR-1); validated in
 *       {@code ClaimAdjustmentServiceImpl}
 *   <li>{@code writeUpReasonCode} — must NOT be DINDB, OMPL, or AFSK (FR-7); validated in {@code
 *       ClaimAdjustmentServiceImpl}
 *   <li>{@code debtorId} — conditional; required for payment-related types with multiple debtors
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimAdjustmentRequestDto {

  /** Adjustment type name (e.g. "WRITE_DOWN", "OPSKRIVNING_REGULERING"). Required. */
  private String adjustmentType;

  /** Adjustment amount. Required; must be &gt; 0. */
  private BigDecimal amount;

  /**
   * Virkningsdato (effective date). Required. Used for retroactive log marker (FR-4) and
   * cross-system GIL § 18 k evaluation (FR-6).
   */
  private LocalDate effectiveDate;

  /**
   * Write-down reason code (FR-1 / Gæld.bekendtg. § 7 stk. 2). Required when {@code adjustmentType}
   * direction is WRITE_DOWN. Validated in {@code ClaimAdjustmentServiceImpl.processAdjustment()}.
   */
  private WriteDownReasonCode writeDownReasonCode;

  /**
   * Write-up reason code as free string. Must NOT be DINDB, OMPL, or AFSK (FR-7 / G.A.2.3.4.4).
   * Validated in {@code ClaimAdjustmentServiceImpl.processAdjustment()}.
   */
  private String writeUpReasonCode;

  /** Debtor UUID. Conditional; required for payment-related types with multiple debtors. */
  private String debtorId;
}
