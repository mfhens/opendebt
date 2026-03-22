package dk.ufst.opendebt.caseservice.service.impl;

import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import dk.ufst.opendebt.caseservice.entity.CaseSensitivity;
import dk.ufst.opendebt.caseservice.service.AssignmentGuardService;
import dk.ufst.opendebt.common.security.AuthContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of assignment guard service for capability-based validation (Petition048
 * W9-RBAC-02).
 *
 * <p>This implementation validates assignments based on:
 *
 * <ul>
 *   <li>Case sensitivity level (NORMAL, VIP, PEP, CONFIDENTIAL)
 *   <li>Target caseworker capabilities (from JWT token)
 *   <li>Assignment audit trail (who assigned whom to which case)
 * </ul>
 *
 * <p><b>Note:</b> This is a simplified implementation that extracts capabilities from the current
 * authentication context. A production implementation would query Keycloak or a user service to get
 * the target caseworker's capabilities from their profile, not from the current supervisor's token.
 *
 * <p><b>TODO (Future Enhancement):</b> Add UserService integration to fetch target caseworker
 * capabilities from Keycloak user attributes when assigning cases to other caseworkers.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssignmentGuardServiceImpl implements AssignmentGuardService {

  /**
   * Validates case assignment based on sensitivity and caseworker capabilities.
   *
   * <p><b>Simplified Logic (Current Implementation):</b>
   *
   * <ul>
   *   <li>CONFIDENTIAL cases: Reject all caseworker assignments
   *   <li>VIP cases: Require HANDLE_VIP_CASES capability
   *   <li>PEP cases: Require HANDLE_PEP_CASES capability
   *   <li>NORMAL cases: Allow all assignments
   * </ul>
   *
   * <p><b>Note:</b> This implementation assumes self-assignment (supervisor assigns themselves or
   * caseworker receives auto-assignment). For supervisor-to-caseworker assignments, a UserService
   * should be injected to check the target caseworker's capabilities.
   *
   * @param caseId Case UUID
   * @param targetCaseworkerId User ID of the caseworker to assign
   * @param sensitivity Case sensitivity level
   * @throws AccessDeniedException if assignment violates capability requirements
   */
  @Override
  public void validateAssignment(
      UUID caseId, String targetCaseworkerId, CaseSensitivity sensitivity) {

    // Extract authentication context (simplified: assumes self-assignment)
    // TODO: In production, query UserService to get target caseworker's capabilities
    AuthContext authContext = AuthContext.fromSecurityContext();

    log.debug(
        "Validating assignment: caseId={}, targetCaseworker={}, sensitivity={}",
        caseId,
        targetCaseworkerId,
        sensitivity);

    // Rule 5.3: CONFIDENTIAL cases cannot be assigned to caseworkers
    if (sensitivity == CaseSensitivity.CONFIDENTIAL) {
      log.warn(
          "Rejected CONFIDENTIAL case assignment: caseId={}, targetCaseworker={}",
          caseId,
          targetCaseworkerId);
      throw new AccessDeniedException(
          "CONFIDENTIAL cases cannot be assigned to caseworkers (supervisor/admin only)");
    }

    // Rule 5.2: VIP cases require HANDLE_VIP_CASES capability
    if (sensitivity == CaseSensitivity.VIP) {
      if (!authContext.hasCapability("HANDLE_VIP_CASES")) {
        log.warn(
            "Rejected VIP case assignment due to missing capability: caseId={}, targetCaseworker={}",
            caseId,
            targetCaseworkerId);
        throw new AccessDeniedException("CASEWORKER_LACKS_VIP_PERMISSION");
      }
    }

    // Rule 5.2: PEP cases require HANDLE_PEP_CASES capability
    if (sensitivity == CaseSensitivity.PEP) {
      if (!authContext.hasCapability("HANDLE_PEP_CASES")) {
        log.warn(
            "Rejected PEP case assignment due to missing capability: caseId={}, targetCaseworker={}",
            caseId,
            targetCaseworkerId);
        throw new AccessDeniedException("CASEWORKER_LACKS_PEP_PERMISSION");
      }
    }

    // NORMAL cases: no capability checks required
    log.info(
        "Assignment validated: caseId={}, targetCaseworker={}, sensitivity={}",
        caseId,
        targetCaseworkerId,
        sensitivity);

    // TODO: Log audit event for assignment (deferred to W9-RBAC-03)
    // auditService.logCaseAssignment(caseId, targetCaseworkerId, authContext.getUserId());
  }
}
