package dk.ufst.opendebt.payment.bookkeeping.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import dk.ufst.opendebt.payment.bookkeeping.model.TimelineReplayResult;

/**
 * Replays the full financial timeline for a debt from a crossing point, recalculating interest,
 * re-applying dækningsrækkefølge, generating dækningsophævelser, and posting corrected ledger
 * entries.
 */
public interface TimelineReplayService {

  /**
   * Replays the timeline from the given crossing point to today.
   *
   * @param debtId the debt to replay
   * @param crossingPoint the earliest date from which to replay
   * @param annualInterestRate the interest rate for recalculation
   * @param triggeringReference reference to the event that caused the crossing
   * @return result containing all storno, recalculated, and new entries
   */
  TimelineReplayResult replayTimeline(
      UUID debtId,
      LocalDate crossingPoint,
      BigDecimal annualInterestRate,
      String triggeringReference);
}
