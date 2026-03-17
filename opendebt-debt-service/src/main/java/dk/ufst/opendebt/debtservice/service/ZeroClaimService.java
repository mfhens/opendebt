package dk.ufst.opendebt.debtservice.service;

import dk.ufst.opendebt.debtservice.entity.DebtEntity;

/**
 * Validates zero-principal claim patterns for interest on fully paid main claims.
 *
 * <p>A zero-principal claim is a main claim (HF) with principalAmount == 0 that serves as a
 * reference anchor for sub-claims (UF) such as interest claims after the original main claim has
 * been fully paid.
 */
public interface ZeroClaimService {

  /**
   * Validate that a zero-principal claim has all required master data.
   *
   * @param entity the debt entity to validate
   */
  void validateZeroClaim(DebtEntity entity);

  /**
   * Validate that a sub-claim (UF / interest) correctly references an existing main claim via
   * parentClaimId.
   *
   * @param subClaim the sub-claim entity to validate
   */
  void validateSubClaimReference(DebtEntity subClaim);
}
