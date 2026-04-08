package dk.ufst.opendebt.debtservice.service;

import java.util.UUID;

import dk.ufst.opendebt.debtservice.dto.ClaimAdjustmentRequestDto;
import dk.ufst.opendebt.debtservice.dto.ClaimAdjustmentResponseDto;

/**
 * Service interface for processing creditor-portal adjustment requests (write-ups and write-downs).
 *
 * <p>Enforces all FR-9 legal constraints independently of portal-side validation:
 *
 * <ul>
 *   <li>FR-1: WriteDownReasonCode required and validated at API boundary
 *   <li>FR-2: OPSKRIVNING_REGULERING on RENTE claim → 422
 *   <li>FR-3: Høring timing rule applied server-side
 *   <li>FR-4: Retroactive nedskrivning log marker (WARN level)
 *   <li>FR-6: GIL § 18 k cross-system flag evaluation
 *   <li>FR-7: DINDB/OMPL/AFSK write-up reason codes → 422
 *   <li>AC-16: CLS audit log for all outcomes (success and failure)
 * </ul>
 *
 * <p>Spec reference: SPEC-P053 §9.1
 */
public interface ClaimAdjustmentService {

  /**
   * Processes an adjustment request for the given claim.
   *
   * <p>The {@code request} parameter is the debt-service DTO ({@code
   * dk.ufst.opendebt.debtservice.dto.ClaimAdjustmentRequestDto}), not the portal's same-named
   * class.
   *
   * @param claimId the UUID of the claim to adjust
   * @param request the adjustment request
   * @return the adjustment response with processing status and GIL § 18 k flag
   * @throws dk.ufst.opendebt.debtservice.exception.CreditorValidationException on any FR-9
   *     validation failure (returned as HTTP 422)
   */
  ClaimAdjustmentResponseDto processAdjustment(UUID claimId, ClaimAdjustmentRequestDto request);
}
