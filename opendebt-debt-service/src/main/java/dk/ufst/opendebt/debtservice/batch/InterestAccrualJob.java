package dk.ufst.opendebt.debtservice.batch;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.ufst.opendebt.debtservice.entity.BatchJobExecutionEntity;
import dk.ufst.opendebt.debtservice.entity.ClaimLifecycleState;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.entity.InterestRuleCode;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;
import dk.ufst.opendebt.debtservice.service.BusinessConfigService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class InterestAccrualJob {

  static final String JOB_NAME = "INTEREST_ACCRUAL";

  private final DebtRepository debtRepository;
  private final InterestAccrualJobHelper helper;
  private final BusinessConfigService configService;

  @Value("${opendebt.batch.page-size:1000}")
  private int pageSize;

  @Scheduled(cron = "${opendebt.batch.interest-accrual.cron:0 30 2 * * *}")
  public void run() {
    execute(LocalDate.now());
  }

  /**
   * Executes interest accrual for the given date. Uses per-debt rate resolution via
   * BusinessConfigService (petition 046) and filters out interest-exempt debt types at the query
   * level (petition 045).
   *
   * <p>Performance optimizations over the original implementation:
   *
   * <ul>
   *   <li>Batch idempotency check: 1 query per page instead of 1 per debt.
   *   <li>Interest-exempt filtering in SQL: straffebøder never fetched.
   *   <li>Rate pre-loading: all needed rates loaded once, cached for the run.
   *   <li>Fee-inclusive balance: feesAmount included in interest base per gældsinddrivelsesloven.
   * </ul>
   */
  public BatchJobExecutionEntity execute(LocalDate accrualDate) {
    if (helper.alreadyExecuted(JOB_NAME, accrualDate)) {
      log.info("Interest accrual already executed for {}, skipping", accrualDate);
      return null;
    }

    // Pre-load all rate config keys for this date (1 DB call total)
    List<String> rateKeys =
        java.util.Arrays.stream(InterestRuleCode.values())
            .map(InterestRuleCode::getConfigKey)
            .filter(java.util.Objects::nonNull)
            .distinct()
            .toList();
    configService.preloadRatesForDate(accrualDate, rateKeys);

    BatchJobExecutionEntity execution = helper.createExecution(JOB_NAME, accrualDate);

    int totalProcessed = 0;
    int totalFailed = 0;
    int page = 0;

    try {
      Page<DebtEntity> debts;
      do {
        // Uses findInterestEligibleDebts which JOINs debt_types to exclude interest-exempt types
        debts =
            debtRepository.findInterestEligibleDebts(
                ClaimLifecycleState.OVERDRAGET, PageRequest.of(page, pageSize));

        int[] counts = helper.processPage(debts.getContent(), accrualDate);
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
    } finally {
      configService.clearCache();
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
