package dk.ufst.opendebt.debtservice.batch;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import dk.ufst.opendebt.common.dto.AccountingTarget;
import dk.ufst.opendebt.debtservice.entity.BatchJobExecutionEntity;
import dk.ufst.opendebt.debtservice.entity.BatchJobExecutionEntity.BatchStatus;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.entity.InterestJournalEntry;
import dk.ufst.opendebt.debtservice.entity.InterestRuleCode;
import dk.ufst.opendebt.debtservice.entity.InterestSelectionEmbeddable;
import dk.ufst.opendebt.debtservice.repository.BatchJobExecutionRepository;
import dk.ufst.opendebt.debtservice.repository.InterestJournalEntryRepository;
import dk.ufst.opendebt.debtservice.service.BusinessConfigService;

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
 * <p>Performance optimizations (petition 045/046):
 *
 * <ul>
 *   <li>Batch idempotency: single IN-clause query replaces N per-debt exists calls.
 *   <li>Per-debt rate resolution via cached BusinessConfigService lookups.
 *   <li>Fee-inclusive balance: outstanding fees included in interest calculation base.
 *   <li>Accounting target tagging: FORDRINGSHAVER for interest on principal.
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InterestAccrualJobHelper {

  private static final BigDecimal DAYS_IN_YEAR = new BigDecimal("365");
  private static final InterestRuleCode DEFAULT_RULE = InterestRuleCode.INDR_STD;

  private final BatchJobExecutionRepository batchRepository;
  private final InterestJournalEntryRepository interestRepository;
  private final BusinessConfigService configService;

  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public boolean alreadyExecuted(String jobName, java.time.LocalDate date) {
    return batchRepository.existsByJobNameAndExecutionDate(jobName, date);
  }

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
   * Processes one page of debts with per-debt rate resolution and batch idempotency check.
   *
   * <p>DB queries per page: 1 (batch idempotency) + 1 (batch insert) = 2 total. Rate lookups are
   * served from the pre-loaded cache in BusinessConfigService.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public int[] processPage(List<DebtEntity> debts, java.time.LocalDate accrualDate) {
    if (debts.isEmpty()) {
      return new int[] {0, 0};
    }

    // Batch idempotency check: 1 query instead of N
    List<UUID> debtIds = debts.stream().map(DebtEntity::getId).toList();
    Set<UUID> alreadyAccrued = interestRepository.findAlreadyAccruedDebtIds(accrualDate, debtIds);

    List<InterestJournalEntry> toSave = new ArrayList<>(debts.size());
    int failed = 0;

    for (DebtEntity debt : debts) {
      if (alreadyAccrued.contains(debt.getId())) {
        continue;
      }
      try {
        BigDecimal rate = resolveRate(debt, accrualDate);
        if (rate.signum() == 0) {
          continue;
        }

        // Fee-inclusive balance per gældsinddrivelsesloven
        BigDecimal balance = debt.getOutstandingBalance();
        BigDecimal fees = debt.getFeesAmount();
        if (fees != null && fees.signum() > 0) {
          balance = balance.add(fees);
        }

        BigDecimal dailyInterest =
            balance.multiply(rate).divide(DAYS_IN_YEAR, 2, RoundingMode.HALF_UP);

        toSave.add(
            InterestJournalEntry.builder()
                .debtId(debt.getId())
                .accrualDate(accrualDate)
                .effectiveDate(accrualDate)
                .balanceSnapshot(balance)
                .rate(rate)
                .interestAmount(dailyInterest)
                .accountingTarget(AccountingTarget.FORDRINGSHAVER)
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
   * Resolves the applicable annual interest rate for a debt based on its interest_rule.
   *
   * <p>Resolution order:
   *
   * <ol>
   *   <li>If the debt has an explicit InterestSelectionEmbeddable.interestRule, use it.
   *   <li>If the rule is INDR_CONTRACT, use the additionalInterestRate from the embeddable.
   *   <li>If the rule is INDR_EXEMPT, return ZERO.
   *   <li>Otherwise, look up the rate from BusinessConfigService (cached).
   *   <li>Fallback: INDR_STD if no rule is set.
   * </ol>
   */
  BigDecimal resolveRate(DebtEntity debt, java.time.LocalDate accrualDate) {
    InterestRuleCode ruleCode = resolveRuleCode(debt);

    if (ruleCode.isExempt()) {
      return BigDecimal.ZERO;
    }

    if (ruleCode.usesContractualRate()) {
      InterestSelectionEmbeddable sel = debt.getInterestSelection();
      if (sel != null && sel.getAdditionalInterestRate() != null) {
        return sel.getAdditionalInterestRate();
      }
      log.warn(
          "Debt {} has INDR_CONTRACT rule but no additionalInterestRate, falling back to INDR_STD",
          debt.getId());
      ruleCode = DEFAULT_RULE;
    }

    String configKey = ruleCode.getConfigKey();
    if (configKey == null) {
      return BigDecimal.ZERO;
    }

    try {
      return configService.getDecimalValue(configKey, accrualDate);
    } catch (BusinessConfigService.ConfigurationNotFoundException e) {
      log.warn(
          "No config for key={} date={}, using ZERO for debt={}",
          configKey,
          accrualDate,
          debt.getId());
      return BigDecimal.ZERO;
    }
  }

  private InterestRuleCode resolveRuleCode(DebtEntity debt) {
    InterestSelectionEmbeddable sel = debt.getInterestSelection();
    if (sel != null && sel.getInterestRule() != null && !sel.getInterestRule().isBlank()) {
      try {
        return InterestRuleCode.valueOf(sel.getInterestRule());
      } catch (IllegalArgumentException e) {
        log.warn(
            "Unknown interest rule '{}' for debt={}, using default",
            sel.getInterestRule(),
            debt.getId());
      }
    }
    return DEFAULT_RULE;
  }

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
