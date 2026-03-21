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

import dk.ufst.opendebt.debtservice.dto.InterestRecalculationResult;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.entity.InterestJournalEntry;
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
 *   <li>Uses the debt's current {@code outstanding_balance} (already reduced by the write-down that
 *       payment-service called before this endpoint) as the corrected balance for the entire
 *       disrupted period.
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

    BigDecimal annualRate;
    try {
      annualRate = configService.getDecimalValue("RATE_INDR_STD", from);
    } catch (BusinessConfigService.ConfigurationNotFoundException e) {
      log.warn(
          "No business config found for RATE_INDR_STD on {}, falling back to {}",
          from,
          FALLBACK_ANNUAL_RATE);
      annualRate = FALLBACK_ANNUAL_RATE;
    }

    // Step 1: delete all journal entries in the disrupted window [from, today)
    int deleted = interestRepository.deleteByDebtIdFromDate(debtId, from);
    log.info(
        "Crossing recalculation: debtId={}, deleted {} interest_journal_entries from {}",
        debtId,
        deleted,
        from);

    // Step 2: recalculate each day in [from, today) using the corrected balance.
    // today is excluded — it will be picked up by the next scheduled batch run.
    BigDecimal balance = debt.getOutstandingBalance();
    BigDecimal dailyInterest =
        balance.multiply(annualRate).divide(DAYS_IN_YEAR, 2, RoundingMode.HALF_UP);

    List<InterestJournalEntry> entries = new ArrayList<>();
    LocalDate cursor = from;
    while (cursor.isBefore(today)) {
      entries.add(
          InterestJournalEntry.builder()
              .debtId(debtId)
              .accrualDate(cursor)
              .effectiveDate(cursor)
              .balanceSnapshot(balance)
              .rate(annualRate)
              .interestAmount(dailyInterest)
              .build());
      cursor = cursor.plusDays(1);
    }

    interestRepository.saveAll(entries);

    BigDecimal total =
        dailyInterest.multiply(new BigDecimal(entries.size())).setScale(2, RoundingMode.HALF_UP);

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
}
