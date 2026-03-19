package dk.ufst.opendebt.debtservice.batch;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import dk.ufst.opendebt.debtservice.entity.BatchJobExecutionEntity;
import dk.ufst.opendebt.debtservice.entity.BatchJobExecutionEntity.BatchStatus;
import dk.ufst.opendebt.debtservice.entity.ClaimLifecycleState;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.entity.InterestJournalEntry;
import dk.ufst.opendebt.debtservice.repository.BatchJobExecutionRepository;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;
import dk.ufst.opendebt.debtservice.repository.InterestJournalEntryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class InterestAccrualJob {

  static final String JOB_NAME = "INTEREST_ACCRUAL";
  private static final BigDecimal DAYS_IN_YEAR = new BigDecimal("365");

  private final DebtRepository debtRepository;
  private final InterestJournalEntryRepository interestRepository;
  private final BatchJobExecutionRepository batchRepository;

  @Value("${opendebt.batch.page-size:1000}")
  private int pageSize;

  @Value("${opendebt.interest.annual-rate:0.0575}")
  private BigDecimal annualRate;

  @Scheduled(cron = "${opendebt.batch.interest-accrual.cron:0 30 2 * * *}")
  public void run() {
    execute(LocalDate.now());
  }

  @Transactional
  public BatchJobExecutionEntity execute(LocalDate accrualDate) {
    if (batchRepository.existsByJobNameAndExecutionDate(JOB_NAME, accrualDate)) {
      log.info("Interest accrual already executed for {}, skipping", accrualDate);
      return null;
    }

    BatchJobExecutionEntity execution =
        BatchJobExecutionEntity.builder()
            .jobName(JOB_NAME)
            .executionDate(accrualDate)
            .startedAt(Instant.now())
            .status(BatchStatus.RUNNING)
            .build();
    execution = batchRepository.save(execution);

    int processed = 0;
    int failed = 0;
    int page = 0;

    Page<DebtEntity> debts;
    do {
      debts =
          debtRepository.findByLifecycleStateAndPositiveBalance(
              ClaimLifecycleState.OVERDRAGET, PageRequest.of(page, pageSize));

      for (DebtEntity debt : debts.getContent()) {
        try {
          if (interestRepository.existsByDebtIdAndAccrualDate(debt.getId(), accrualDate)) {
            continue;
          }

          BigDecimal balance = debt.getOutstandingBalance();
          BigDecimal dailyInterest =
              balance.multiply(annualRate).divide(DAYS_IN_YEAR, 2, RoundingMode.HALF_UP);

          InterestJournalEntry entry =
              InterestJournalEntry.builder()
                  .debtId(debt.getId())
                  .accrualDate(accrualDate)
                  .effectiveDate(accrualDate)
                  .balanceSnapshot(balance)
                  .rate(annualRate)
                  .interestAmount(dailyInterest)
                  .build();

          interestRepository.save(entry);
          processed++;
        } catch (Exception e) {
          log.warn("Failed to accrue interest for debt={}: {}", debt.getId(), e.getMessage());
          failed++;
        }
      }
      page++;
    } while (debts.hasNext());

    execution.setRecordsProcessed(processed);
    execution.setRecordsFailed(failed);
    execution.setCompletedAt(Instant.now());
    execution.setStatus(failed > 0 ? BatchStatus.FAILED : BatchStatus.COMPLETED);
    batchRepository.save(execution);

    log.info(
        "Interest accrual completed: date={}, processed={}, failed={}",
        accrualDate,
        processed,
        failed);

    return execution;
  }
}
