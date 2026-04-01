package dk.ufst.opendebt.payment.bookkeeping.service;

import java.util.UUID;

import dk.ufst.bookkeeping.model.TimelineReplayResult;
import dk.ufst.opendebt.payment.bookkeeping.model.AllocationNotification;

/**
 * Generates Allokeringsunderretning (Allocation Notification) content from a timeline replay
 * result. The notification shows how saldo changes are distributed between hovedstol and renter,
 * including dækningsophævelser caused by crossing transactions.
 */
public interface AllocationNotificationService {

  /**
   * Generates an allocation notification from a timeline replay result.
   *
   * @param replayResult the result of a timeline replay
   * @param creditorOrgId the creditor receiving the notification
   * @return the notification content ready for dispatch via letter-service or portal
   */
  AllocationNotification generateFromReplayResult(
      TimelineReplayResult replayResult, UUID creditorOrgId);
}
