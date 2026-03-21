package dk.ufst.opendebt.debtservice.batch;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import dk.ufst.opendebt.debtservice.entity.BatchJobExecutionEntity;
import dk.ufst.opendebt.debtservice.entity.BatchJobExecutionEntity.BatchStatus;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.entity.InterestJournalEntry;
import dk.ufst.opendebt.debtservice.repository.BatchJobExecutionRepository;
import dk.ufst.opendebt.debtservice.repository.InterestJournalEntryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Transactional helper for {@link InterestAccrualJob}.
 *
 * <p>Each public method runs in its own {@code REQUIRES_NEW} transaction so that:
 *
 * <ul>
 *   <li>The execution record is visible to monitoring queries immediately after it is created.
 *   <li>Each page of interest entries is committed independently — a failure on one page does not
 *       roll back work already done on previous pages.
 *   <li>No single transaction ever holds more than {@code pageSize} rows open at once.
 * </ul>
 *
 * <p>This class must be a separate Spring bean (not an inner class or self-call) so that Spring's
 * AOP proxy can apply the {@code @Transactional} advice.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InterestAccrualJobHelper {

  private static final BigDecimal DAYS_IN_YEAR = new BigDecimal("365");

  private final BatchJobExecutionRepository batchRepository;
  private final InterestJournalEntryRepository interestRepository;

  /** Returns true if the job has already been executed for the given date. */
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public boolean alreadyExecuted(String jobName, java.time.LocalDate date) {
    return batchRepository.existsByJobNameAndExecutionDate(jobName, date);
  }

  /**
   * Persists a new execution record with status {@code RUNNING} and immediately commits so that the
   * record is visible to monitoring queries before the long-running page loop starts.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public BatchJobExecutionEntity createExecution(String jobName, java.time.LocalDate date) {
    BatchJobExecutionEntity execution =
        BatchJobExecutionEntity.builder()
            .jobName(jobName)
            .executionDate(date)
            .startedAt(Instant.now())
            .status(BatchStatus.RUNNING)
            .build();
    return batchRepository.save(execution);
  }

  /**
   * Processes one page of debts and commits the resulting interest entries in a single batch
   * insert. Returns an array {@code [processed, failed]}.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public int[] processPage(
      List<DebtEntity> debts, java.time.LocalDate accrualDate, BigDecimal annualRate) {

    List<InterestJournalEntry> toSave = new ArrayList<>(debts.size());
    int failed = 0;

    for (DebtEntity debt : debts) {
      try {
        if (interestRepository.existsByDebtIdAndAccrualDate(debt.getId(), accrualDate)) {
          continue;
        }
        BigDecimal balance = debt.getOutstandingBalance();
        BigDecimal dailyInterest =
            balance.multiply(annualRate).divide(DAYS_IN_YEAR, 2, RoundingMode.HALF_UP);

        toSave.add(
            InterestJournalEntry.builder()
                .debtId(debt.getId())
                .accrualDate(accrualDate)
                .effectiveDate(accrualDate)
                .balanceSnapshot(balance)
                .rate(annualRate)
                .interestAmount(dailyInterest)
                .build());
      } catch (Exception e) {
        log.warn("Failed to build interest entry for debt={}: {}", debt.getId(), e.getMessage());
        failed++;
      }
    }

    if (!toSave.isEmpty()) {
      interestRepository.saveAll(toSave);
    }

    return new int[] {toSave.size(), failed};
  }

  /**
   * Updates the execution record with the final counts and status and commits. Uses {@code
   * REQUIRES_NEW} so the update is persisted even if the caller catches an exception.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public BatchJobExecutionEntity finalizeExecution(
      BatchJobExecutionEntity execution, int processed, int failed) {
    execution.setRecordsProcessed(processed);
    execution.setRecordsFailed(failed);
    execution.setCompletedAt(Instant.now());
    execution.setStatus(failed > 0 ? BatchStatus.FAILED : BatchStatus.COMPLETED);
    return batchRepository.save(execution);
  }
}
