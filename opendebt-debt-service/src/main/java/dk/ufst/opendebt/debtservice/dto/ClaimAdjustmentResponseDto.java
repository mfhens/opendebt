package dk.ufst.opendebt.debtservice.dto;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for the {@code POST /api/v1/debts/{id}/adjustments} endpoint (SPEC-P053 §6.1).
 *
 * <p>Returned with HTTP 201 on successful adjustment processing.
 *
 * <p>Fields per SPEC-P053 §6.1:
 *
 * <ul>
 *   <li>{@code actionId} — PSRM action identifier
 *   <li>{@code status} — processing status (e.g., {@code ACCEPTED}, {@code PENDING_HOERING})
 *   <li>{@code amount} — adjustment amount processed
 *   <li>{@code crossSystemRetroactiveApplies} — GIL § 18 k flag (FR-6)
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimAdjustmentResponseDto {

  /** PSRM action identifier. */
  private String actionId;

  /** Processing status (e.g., ACCEPTED, PENDING_HOERING). */
  private String status;

  /** Adjustment amount processed. */
  private BigDecimal amount;

  /**
   * GIL § 18 k flag (FR-6 / SPEC-P053 §6.2). {@code true} when {@code
   * request.getEffectiveDate().isBefore(debt.getReceivedAt().toLocalDate())}, indicating a
   * cross-system retroactive nedskrivning requiring possible temporary suspension.
   */
  private boolean crossSystemRetroactiveApplies;

  /**
   * Authoritative receipt timestamp for the adjustment (FR-3 / B1 fix). For HOERING claims with
   * OPSKRIVNING_REGULERING, this is set to the høring resolution time; otherwise it is the portal
   * submission time ({@code Instant.now()}).
   */
  private Instant receiptTimestamp;
}
