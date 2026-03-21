package dk.ufst.opendebt.debtservice.batch;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.ufst.opendebt.debtservice.entity.BatchJobExecutionEntity;
import dk.ufst.opendebt.debtservice.entity.ClaimLifecycleState;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class InterestAccrualJob {

  static final String JOB_NAME = "INTEREST_ACCRUAL";

  private final DebtRepository debtRepository;
  private final InterestAccrualJobHelper helper;

  @Value("${opendebt.batch.page-size:1000}")
  private int pageSize;

  @Value("${opendebt.interest.annual-rate:0.0575}")
  private BigDecimal annualRate;

  @Scheduled(cron = "${opendebt.batch.interest-accrual.cron:0 30 2 * * *}")
  public void run() {
    execute(LocalDate.now());
  }

  /**
   * Executes interest accrual for the given date. Each page of debts is processed and committed in
   * its own transaction via {@link InterestAccrualJobHelper} so that:
   *
   * <ul>
   *   <li>No single database transaction holds more than {@code pageSize} rows open at once.
   *   <li>Progress is durable — a failure mid-run does not discard completed pages.
   *   <li>The job is idempotent: re-running for the same date skips already-written entries.
   * </ul>
   */
  public BatchJobExecutionEntity execute(LocalDate accrualDate) {
    if (helper.alreadyExecuted(JOB_NAME, accrualDate)) {
      log.info("Interest accrual already executed for {}, skipping", accrualDate);
      return null;
    }

    // Commit the RUNNING record immediately so monitoring queries see it.
    BatchJobExecutionEntity execution = helper.createExecution(JOB_NAME, accrualDate);

    int totalProcessed = 0;
    int totalFailed = 0;
    int page = 0;

    try {
      Page<DebtEntity> debts;
      do {
        debts =
            debtRepository.findByLifecycleStateAndPositiveBalance(
                ClaimLifecycleState.OVERDRAGET, PageRequest.of(page, pageSize));

        int[] counts = helper.processPage(debts.getContent(), accrualDate, annualRate);
        totalProcessed += counts[0];
        totalFailed += counts[1];

        if (page % 100 == 0) {
          log.info(
              "Interest accrual progress: page={}, processed={}, failed={}",
              page,
              totalProcessed,
              totalFailed);
        }
        page++;
      } while (debts.hasNext());
    } catch (Exception e) {
      log.error("Interest accrual aborted at page {}: {}", page, e.getMessage(), e);
      return helper.finalizeExecution(execution, totalProcessed, totalFailed + 1);
    }

    execution = helper.finalizeExecution(execution, totalProcessed, totalFailed);
    log.info(
        "Interest accrual completed: date={}, processed={}, failed={}",
        accrualDate,
        totalProcessed,
        totalFailed);
    return execution;
  }
}
