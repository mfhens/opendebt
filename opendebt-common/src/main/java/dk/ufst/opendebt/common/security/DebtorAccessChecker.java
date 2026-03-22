package dk.ufst.opendebt.common.security;

import java.util.UUID;

/**
 * Interface for checking citizen person_id-scoped access. Implementation in debt-service (Rule
 * 4.1).
 *
 * <p>Per ADR-0007, each service must validate authorization at its boundary.
 */
public interface DebtorAccessChecker {

  /**
   * Check if the authenticated citizen can access a specific debt.
   *
   * @param debtId Debt UUID
   * @param authContext Current authentication context (must have personId for CITIZEN)
   * @return true if authorized, false otherwise
   */
  boolean canAccessDebt(UUID debtId, AuthContext authContext);
}
