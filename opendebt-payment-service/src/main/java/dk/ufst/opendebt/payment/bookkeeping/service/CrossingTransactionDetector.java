package dk.ufst.opendebt.payment.bookkeeping.service;

import java.time.LocalDate;
import java.util.UUID;

import dk.ufst.opendebt.payment.bookkeeping.model.CrossingDetectionResult;

/**
 * Detects when a newly arrived financial event has an effective date that precedes previously
 * posted events on the same debt (a "crossing transaction").
 */
public interface CrossingTransactionDetector {

  /**
   * Checks whether a new event with the given effective date would cross any existing events.
   *
   * @param debtId the debt to check
   * @param newEventEffectiveDate the effective date of the incoming event
   * @return detection result with crossing point and affected events if crossing is found
   */
  CrossingDetectionResult detectCrossing(UUID debtId, LocalDate newEventEffectiveDate);
}
