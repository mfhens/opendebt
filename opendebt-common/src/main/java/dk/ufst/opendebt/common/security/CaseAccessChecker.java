package dk.ufst.opendebt.common.security;

import java.util.UUID;

/**
 * Interface for checking case access authorization. Implementation in case-service (Rules 1.1, 1.2,
 * 2.1, 2.2, 5.3, 5.4).
 *
 * <p>Per ADR-0007, each service must validate authorization at its boundary.
 */
public interface CaseAccessChecker {

  /**
   * Check if the authenticated user can access a specific case.
   *
   * @param caseId Case UUID
   * @param authContext Current authentication context
   * @return true if authorized, false otherwise
   */
  boolean canAccessCase(UUID caseId, AuthContext authContext);
}
