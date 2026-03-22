package dk.ufst.opendebt.common.security;

import java.util.UUID;

/**
 * Interface for checking creditor organization-scoped access. Implementation in creditor-service
 * (Rules 3.1, 3.2).
 *
 * <p>Per ADR-0007, each service must validate authorization at its boundary.
 */
public interface CreditorAccessChecker {

  /**
   * Check if the authenticated creditor can access a specific claim.
   *
   * @param claimId Claim UUID
   * @param authContext Current authentication context (must have organizationId)
   * @return true if authorized, false otherwise
   */
  boolean canAccessClaim(UUID claimId, AuthContext authContext);
}
