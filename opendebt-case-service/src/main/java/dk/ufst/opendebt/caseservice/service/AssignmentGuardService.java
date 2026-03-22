package dk.ufst.opendebt.caseservice.service;

import java.util.UUID;

import dk.ufst.opendebt.caseservice.entity.CaseSensitivity;

/**
 * Assignment guard service for validating case assignment operations (Petition048 W9-RBAC-02).
 *
 * <p>Enforces capability-based assignment rules (Rules 5.2, 5.3):
 *
 * <ul>
 *   <li>VIP cases require HANDLE_VIP_CASES capability
 *   <li>PEP cases require HANDLE_PEP_CASES capability
 *   <li>CONFIDENTIAL cases cannot be assigned to caseworkers (supervisor/admin only)
 * </ul>
 *
 * <p>This guard prevents GDPR/compliance violations by blocking invalid assignments before they
 * occur. All assignment attempts are audited.
 */
public interface AssignmentGuardService {

  /**
   * Validate that a caseworker can be assigned to a case with given sensitivity.
   *
   * @param caseId Case UUID
   * @param targetCaseworkerId User ID of the caseworker to assign
   * @param sensitivity Case sensitivity level
   * @throws org.springframework.security.access.AccessDeniedException if assignment is not allowed
   */
  void validateAssignment(UUID caseId, String targetCaseworkerId, CaseSensitivity sensitivity);
}
