package dk.ufst.opendebt.debtservice.service;

import java.util.UUID;

import dk.ufst.opendebt.debtservice.entity.HoeringEntity;

/**
 * Service for managing HØRING workflow when fordringer have stamdata deviations. A hearing allows
 * the fordringshaver to justify deviations and RIM caseworkers to accept or reject the fordring.
 */
public interface HoeringService {

  /**
   * Creates a hearing record for a debt with stamdata deviations.
   *
   * @param debtId the debt that requires hearing
   * @param deviationDescription description of what stamdata deviated
   * @return the created hearing entity
   */
  HoeringEntity createHoering(UUID debtId, String deviationDescription);

  /**
   * Fordringshaver approves the hearing with a justification.
   *
   * @param hoeringId the hearing to approve
   * @param begrundelse justification for the deviation
   * @return the updated hearing entity
   */
  HoeringEntity fordingshaverApprove(UUID hoeringId, String begrundelse);

  /**
   * Fordringshaver withdraws the fordring during hearing.
   *
   * @param hoeringId the hearing to withdraw
   * @return the updated hearing entity
   */
  HoeringEntity fordingshaverWithdraw(UUID hoeringId);

  /**
   * RIM caseworker decides on the hearing.
   *
   * @param hoeringId the hearing to decide on
   * @param accepted true if accepted, false if rejected
   * @param notes caseworker decision notes
   * @return the updated hearing entity
   */
  HoeringEntity rimDecide(UUID hoeringId, boolean accepted, String notes);
}
