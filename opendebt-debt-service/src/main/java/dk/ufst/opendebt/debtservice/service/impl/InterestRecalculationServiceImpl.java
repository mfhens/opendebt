package dk.ufst.opendebt.debtservice.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.ufst.opendebt.common.dto.AccountingTarget;
import dk.ufst.opendebt.debtservice.dto.InterestRecalculationResult;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.entity.InterestJournalEntry;
import dk.ufst.opendebt.debtservice.entity.InterestRuleCode;
import dk.ufst.opendebt.debtservice.entity.InterestSelectionEmbeddable;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;
import dk.ufst.opendebt.debtservice.repository.InterestJournalEntryRepository;
import dk.ufst.opendebt.debtservice.service.BusinessConfigService;
import dk.ufst.opendebt.debtservice.service.InterestRecalculationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Corrects {@code interest_journal_entries} after a crossing transaction. Implements petition039
 * §FR-2: "recalculate interest for each period with the corrected principal balance."
 *
 * <p>Design notes:
 *
 * <ul>
 *   <li>Uses the debt's current {@code outstanding_balance + fees_amount} (already reduced by the
 *       write-down that payment-service called before this endpoint) as the corrected base,
 *       matching the fee-inclusive balance used by the daily {@link
 *       dk.ufst.opendebt.debtservice.batch.InterestAccrualJobHelper}.
 *   <li>Resolves the per-debt rate key (RATE_INDR_STD, RATE_INDR_TOLD, etc.) from the debt's {@code
 *       interest_rule} field, matching the batch resolution order exactly.
 *   <li>Re-checks the rate at every month boundary (1st of each month) within the recalculation
 *       window, ensuring that mid-year NB-rate changes are captured correctly for long windows.
 *   <li>Runs in a single {@code @Transactional} — the delete and the re-inserts are atomic.
 *   <li>Idempotent: a second call with the same {@code from} date produces the same entries.
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterestRecalculationServiceImpl implements InterestRecalculationService {

  private static final BigDecimal DAYS_IN_YEAR = new BigDecimal("365");
  private static final BigDecimal FALLBACK_ANNUAL_RATE = new BigDecimal("0.0575");
  private static final InterestRuleCode DEFAULT_RULE = InterestRuleCode.INDR_STD;

  private final DebtRepository debtRepository;
  private final InterestJournalEntryRepository interestRepository;
  private final BusinessConfigService configService;

  @Override
  @Transactional
  public InterestRecalculationResult recalculateFromDate(UUID debtId, LocalDate from) {
    DebtEntity debt =
        debtRepository
            .findById(debtId)
            .orElseThrow(() -> new EntityNotFoundException("Debt not found: " + debtId));

    LocalDate today = LocalDate.now();

    // Guard: if crossing date is in the future or is today, there is nothing to recalculate.
    if (!from.isBefore(today)) {
      log.info(
          "Interest recalculation requested for debtId={} from={} but that is not in the past,"
              + " skipping",
          debtId,
          from);
      return InterestRecalculationResult.builder()
          .debtId(debtId)
          .recalculatedFrom(from)
          .recalculatedTo(today)
          .entriesDeleted(0)
          .entriesWritten(0)
          .balanceUsed(debt.getOutstandingBalance())
          .totalInterestRecalculated(BigDecimal.ZERO)
          .build();
    }

    // Fee-inclusive base balance, matching the daily batch calculation in InterestAccrualJobHelper.
    BigDecimal balance = debt.getOutstandingBalance();
    BigDecimal fees = debt.getFeesAmount();
    if (fees != null && fees.signum() > 0) {
      balance = balance.add(fees);
    }

    // Resolve per-debt rate key using the same order as the batch job.
    String configKey = resolveConfigKey(debt);
    BigDecimal initialRate = resolveInitialRate(configKey, from, debtId);

    // Step 1: delete all journal entries in the disrupted window [from, today)
    int deleted = interestRepository.deleteByDebtIdFromDate(debtId, from);
    log.info(
        "Crossing recalculation: debtId={}, deleted {} interest_journal_entries from {}",
        debtId,
        deleted,
        from);

    // Step 2: recalculate each day in [from, today).
    // Re-check the rate at the first day of each month so that mid-year NB-rate changes (e.g. a
    // Nationalbanken rate announcement on 2025-07-07) are applied on the correct day within a
    // multi-month or multi-year recalculation window.
    // today is excluded — it will be picked up by the next scheduled batch run.
    BigDecimal currentRate = initialRate;
    LocalDate currentRateDate = from;

    List<InterestJournalEntry> entries = new ArrayList<>();
    LocalDate cursor = from;
    while (cursor.isBefore(today)) {
      // Re-resolve at start of window and at every month boundary (1st of month).
      boolean isMonthBoundary = cursor.getDayOfMonth() == 1;
      if ((cursor.equals(from) || isMonthBoundary) && configKey != null) {
        try {
          BigDecimal resolvedRate = configService.getDecimalValue(configKey, cursor);
          if (!resolvedRate.equals(currentRate)) {
            log.debug(
                "Rate boundary crossed at {}: {} → {} for debtId={}",
                cursor,
                currentRate,
                resolvedRate,
                debtId);
            currentRate = resolvedRate;
            currentRateDate = cursor;
          }
        } catch (BusinessConfigService.ConfigurationNotFoundException e) {
          log.warn(
              "No rate found for key={} on {}, continuing with rate {} from {}",
              configKey,
              cursor,
              currentRate,
              currentRateDate);
        }
      }

      BigDecimal dailyInterest =
          balance.multiply(currentRate).divide(DAYS_IN_YEAR, 2, RoundingMode.HALF_UP);

      entries.add(
          InterestJournalEntry.builder()
              .debtId(debtId)
              .accrualDate(cursor)
              .effectiveDate(cursor)
              .balanceSnapshot(balance)
              .rate(currentRate)
              .interestAmount(dailyInterest)
              .accountingTarget(AccountingTarget.FORDRINGSHAVER)
              .build());
      cursor = cursor.plusDays(1);
    }

    interestRepository.saveAll(entries);

    BigDecimal total =
        entries.stream()
            .map(InterestJournalEntry::getInterestAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);

    log.info(
        "Crossing recalculation complete: debtId={}, from={}, to={}, written={}, total={}",
        debtId,
        from,
        today,
        entries.size(),
        total);

    return InterestRecalculationResult.builder()
        .debtId(debtId)
        .recalculatedFrom(from)
        .recalculatedTo(today)
        .entriesDeleted(deleted)
        .entriesWritten(entries.size())
        .balanceUsed(balance)
        .totalInterestRecalculated(total)
        .build();
  }

  /**
   * Resolves the {@code business_config} key for this debt's interest rule, using the same
   * resolution order as {@link dk.ufst.opendebt.debtservice.batch.InterestAccrualJobHelper}.
   * Returns {@code null} for exempt debts (zero interest).
   */
  private String resolveConfigKey(DebtEntity debt) {
    InterestSelectionEmbeddable sel = debt.getInterestSelection();
    InterestRuleCode ruleCode = DEFAULT_RULE;

    if (sel != null && sel.getInterestRule() != null && !sel.getInterestRule().isBlank()) {
      try {
        ruleCode = InterestRuleCode.valueOf(sel.getInterestRule());
      } catch (IllegalArgumentException e) {
        log.warn(
            "Unknown interest rule '{}' for debt={}, using default",
            sel.getInterestRule(),
            debt.getId());
      }
    }

    if (ruleCode.isExempt()) {
      return null;
    }
    if (ruleCode.usesContractualRate()) {
      // Contractual rates are fixed — no config key needed; caller handles ZERO.
      return null;
    }
    return ruleCode.getConfigKey();
  }

  /** Looks up the rate effective on {@code from} for the given key; falls back if not found. */
  private BigDecimal resolveInitialRate(String configKey, LocalDate from, UUID debtId) {
    if (configKey == null) {
      return BigDecimal.ZERO;
    }
    try {
      return configService.getDecimalValue(configKey, from);
    } catch (BusinessConfigService.ConfigurationNotFoundException e) {
      log.warn(
          "No business config found for key={} on {}, falling back to {} for debtId={}",
          configKey,
          from,
          FALLBACK_ANNUAL_RATE,
          debtId);
      return FALLBACK_ANNUAL_RATE;
    }
  }
}
