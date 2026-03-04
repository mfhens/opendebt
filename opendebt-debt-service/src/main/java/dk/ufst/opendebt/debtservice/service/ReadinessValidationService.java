package dk.ufst.opendebt.debtservice.service;

import java.util.UUID;

import dk.ufst.opendebt.common.dto.DebtDto;

/**
 * Service for validating debt readiness for collection (indrivelsesparathed). A debt must meet
 * certain criteria before it can be collected, including:
 *
 * <ul>
 *   <li>Valid debtor identification (CPR/CVR)
 *   <li>Correct debt type classification
 *   <li>Proper documentation
 *   <li>Passed due date
 *   <li>No active disputes or appeals
 *   <li>Compliance with legal requirements
 * </ul>
 */
public interface ReadinessValidationService {

  /**
   * Validates if a debt is ready for collection.
   *
   * @param debtId The ID of the debt to validate
   * @return Updated debt with readiness status
   */
  DebtDto validateReadiness(UUID debtId);

  /**
   * Manually approves a debt as ready for collection.
   *
   * @param debtId The ID of the debt to approve
   * @return Updated debt with approved readiness status
   */
  DebtDto approveReadiness(UUID debtId);

  /**
   * Rejects a debt as not ready for collection.
   *
   * @param debtId The ID of the debt to reject
   * @param reason The reason for rejection
   * @return Updated debt with rejected readiness status
   */
  DebtDto rejectReadiness(UUID debtId, String reason);

  /**
   * Validates a batch of debts for readiness.
   *
   * @param creditorId The creditor whose debts to validate
   * @return Number of debts validated
   */
  int validateBatchReadiness(String creditorId);
}
