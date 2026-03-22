package dk.ufst.opendebt.caseservice.entity;

/**
 * Case sensitivity classification for access control (Petition048 W9-RBAC-02).
 *
 * <p>Determines which caseworkers can access a case based on their capabilities:
 *
 * <ul>
 *   <li><b>NORMAL</b>: Standard case - any assigned caseworker can access
 *   <li><b>VIP</b>: High-profile debtor - requires HANDLE_VIP_CASES capability
 *   <li><b>PEP</b>: Politically Exposed Person - requires HANDLE_PEP_CASES capability
 *   <li><b>CONFIDENTIAL</b>: Restricted to supervisors and admins only
 * </ul>
 *
 * <p>Sensitivity is enforced at three layers:
 *
 * <ul>
 *   <li>HTTP Layer: @PreAuthorize annotations on controller methods
 *   <li>Service Layer: CaseAccessChecker validates sensitivity + capabilities
 *   <li>Assignment Layer: AssignmentGuardService prevents invalid assignments
 * </ul>
 *
 * @see dk.ufst.opendebt.caseservice.service.impl.CaseAccessCheckerImpl
 * @see dk.ufst.opendebt.caseservice.service.AssignmentGuardService
 */
public enum CaseSensitivity {
  /** Standard case - no special access restrictions. */
  NORMAL,

  /** High-profile debtor case - requires HANDLE_VIP_CASES capability. */
  VIP,

  /** Politically Exposed Person case - requires HANDLE_PEP_CASES capability. */
  PEP,

  /** Confidential case - supervisors and admins only (no caseworker access). */
  CONFIDENTIAL
}
