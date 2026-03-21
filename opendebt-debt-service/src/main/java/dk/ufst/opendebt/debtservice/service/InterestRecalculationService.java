package dk.ufst.opendebt.debtservice.service;

import java.time.LocalDate;
import java.util.UUID;

import dk.ufst.opendebt.debtservice.dto.InterestRecalculationResult;

/**
 * Retroactively corrects {@code interest_journal_entries} in the debt-service after a crossing
 * transaction has been detected and processed by the payment-service.
 *
 * <p>When payment-service detects that an incoming payment has an effective date earlier than
 * already-posted interest accruals, it:
 *
 * <ol>
 *   <li>Replays the ledger timeline internally (storno + recalculation of {@code ledger_entries}).
 *   <li>Calls {@code POST /api/v1/debts/{id}/write-down} to correct the outstanding balance.
 *   <li>Calls {@code POST /api/v1/debts/{id}/interest/recalculate?from={date}} (this service) to
 *       correct the {@code interest_journal_entries} that the daily batch accrual had already
 *       written for the disrupted period.
 * </ol>
 *
 * <p>This is the debt-service side of the crossing-transaction correction described in petition039,
 * and is orchestrated by Flowable in case-service (ADR-0019).
 */
public interface InterestRecalculationService {

  /**
   * Deletes all interest journal entries for the given debt on or after {@code from}, then
   * recalculates them using the debt's current outstanding balance (post write-down).
   *
   * <p>The recalculation window is {@code [from, today)} — today is excluded because the daily
   * batch job will handle it in the next scheduled run.
   *
   * <p>Idempotent: calling this method twice with the same arguments produces the same result.
   *
   * @param debtId the debt whose interest journal needs correction
   * @param from the earliest accrual date to delete and recalculate (inclusive)
   * @return result including entry counts and totals for audit
   */
  InterestRecalculationResult recalculateFromDate(UUID debtId, LocalDate from);
}
