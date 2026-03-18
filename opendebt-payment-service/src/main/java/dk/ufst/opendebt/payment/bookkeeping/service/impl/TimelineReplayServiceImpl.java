package dk.ufst.opendebt.payment.bookkeeping.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.ufst.opendebt.payment.bookkeeping.AccountCode;
import dk.ufst.opendebt.payment.bookkeeping.entity.DebtEventEntity;
import dk.ufst.opendebt.payment.bookkeeping.entity.LedgerEntryEntity;
import dk.ufst.opendebt.payment.bookkeeping.model.*;
import dk.ufst.opendebt.payment.bookkeeping.repository.DebtEventRepository;
import dk.ufst.opendebt.payment.bookkeeping.repository.LedgerEntryRepository;
import dk.ufst.opendebt.payment.bookkeeping.service.CoveragePriorityService;
import dk.ufst.opendebt.payment.bookkeeping.service.EventOrderComparator;
import dk.ufst.opendebt.payment.bookkeeping.service.TimelineReplayService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimelineReplayServiceImpl implements TimelineReplayService {

  private static final BigDecimal DAYS_PER_YEAR = new BigDecimal("365");
  private static final int CALC_SCALE = 10;

  private final DebtEventRepository debtEventRepository;
  private final LedgerEntryRepository ledgerEntryRepository;
  private final CoveragePriorityService coveragePriorityService;

  @Override
  @Transactional
  public TimelineReplayResult replayTimeline(
      UUID debtId,
      LocalDate crossingPoint,
      BigDecimal annualInterestRate,
      String triggeringReference) {

    log.info(
        "TIMELINE REPLAY: debtId={}, crossingPoint={}, rate={}",
        debtId,
        crossingPoint,
        annualInterestRate);

    // 1. Storno all ledger entries from crossing point forward
    int stornoCount = stornoEntriesFromDate(debtId, crossingPoint, triggeringReference);

    // 2. Load all events and sort deterministically
    List<DebtEventEntity> allEvents =
        new ArrayList<>(
            debtEventRepository.findByDebtIdOrderByEffectiveDateAscCreatedAtAsc(debtId));
    allEvents.sort(EventOrderComparator.INSTANCE);

    // 3. Replay: walk the timeline computing balances and interest periods
    LocalDate replayEnd = LocalDate.now();
    ReplayState state = new ReplayState();
    List<InterestPeriod> interestPeriods = new ArrayList<>();
    List<CoverageAllocation> allocations = new ArrayList<>();
    List<CoverageReversal> reversals = new ArrayList<>();

    // Build balance up to crossing point first
    for (DebtEventEntity event : allEvents) {
      if (event.getEffectiveDate().isBefore(crossingPoint)) {
        applyEventToState(event, state);
      }
    }

    // Collect events from crossing point, grouped by date for interest calculation
    List<DebtEventEntity> replayEvents =
        allEvents.stream()
            .filter(
                e ->
                    !e.getEffectiveDate().isBefore(crossingPoint)
                        && !e.getEffectiveDate().isAfter(replayEnd))
            .filter(e -> e.getEventType() != DebtEventEntity.EventType.INTEREST_ACCRUED)
            .filter(e -> e.getEventType() != DebtEventEntity.EventType.COVERAGE_REVERSED)
            .toList();

    // 4. Walk timeline: for each event, compute interest from previous point, then apply event
    LocalDate previousDate = crossingPoint;
    int newEntryCount = 0;

    for (DebtEventEntity event : replayEvents) {
      LocalDate eventDate = event.getEffectiveDate();

      // Calculate interest for the gap between previous date and this event
      if (eventDate.isAfter(previousDate)
          && state.principalBalance.compareTo(BigDecimal.ZERO) > 0) {
        InterestPeriod period =
            calculateInterestForPeriod(
                previousDate, eventDate, state.principalBalance, annualInterestRate);
        if (period.getInterestAmount().compareTo(BigDecimal.ZERO) > 0) {
          interestPeriods.add(period);
          state.accruedInterest = state.accruedInterest.add(period.getInterestAmount());
          newEntryCount += postInterestEntries(debtId, period, triggeringReference);
        }
      }

      // Apply the event with coverage priority if it is a payment/recovery
      if (isRecoveryEvent(event)) {
        CoverageAllocation allocation =
            coveragePriorityService.allocatePayment(
                debtId, event.getAmount(), state.accruedInterest, state.principalBalance);
        allocation.setEffectiveDate(eventDate);
        allocation.setSourceEventId(event.getId());
        allocations.add(allocation);

        // Check if allocation changed vs original (detect dækningsophævelse)
        checkForCoverageReversal(event, allocation, state, reversals, triggeringReference);

        state.accruedInterest = state.accruedInterest.subtract(allocation.getInterestPortion());
        state.principalBalance = state.principalBalance.subtract(allocation.getPrincipalPortion());
        newEntryCount += postRecoveryEntries(debtId, eventDate, allocation, event.getReference());
      } else {
        applyEventToState(event, state);
        newEntryCount += repostEventEntries(debtId, event);
      }

      previousDate = eventDate;
    }

    // Final interest period from last event to today
    if (replayEnd.isAfter(previousDate) && state.principalBalance.compareTo(BigDecimal.ZERO) > 0) {
      InterestPeriod finalPeriod =
          calculateInterestForPeriod(
              previousDate, replayEnd, state.principalBalance, annualInterestRate);
      if (finalPeriod.getInterestAmount().compareTo(BigDecimal.ZERO) > 0) {
        interestPeriods.add(finalPeriod);
        state.accruedInterest = state.accruedInterest.add(finalPeriod.getInterestAmount());
        newEntryCount += postInterestEntries(debtId, finalPeriod, triggeringReference);
      }
    }

    BigDecimal newInterestTotal =
        interestPeriods.stream()
            .map(InterestPeriod::getInterestAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    log.info(
        "TIMELINE REPLAY COMPLETE: debtId={}, storno={}, newEntries={}, "
            + "interestPeriods={}, reversals={}, finalPrincipal={}, finalInterest={}",
        debtId,
        stornoCount,
        newEntryCount,
        interestPeriods.size(),
        reversals.size(),
        state.principalBalance,
        state.accruedInterest);

    return TimelineReplayResult.builder()
        .debtId(debtId)
        .replayFromDate(crossingPoint)
        .replayToDate(replayEnd)
        .stornoEntriesPosted(stornoCount)
        .newEntriesPosted(newEntryCount)
        .recalculatedInterestPeriods(interestPeriods)
        .recalculatedAllocations(allocations)
        .coverageReversals(reversals)
        .newInterestTotal(newInterestTotal)
        .finalPrincipalBalance(state.principalBalance)
        .finalInterestBalance(state.accruedInterest)
        .build();
  }

  private int stornoEntriesFromDate(UUID debtId, LocalDate fromDate, String reason) {
    List<LedgerEntryEntity> activeEntries =
        ledgerEntryRepository.findActiveEntriesByDebtId(debtId).stream()
            .filter(e -> !e.getEffectiveDate().isBefore(fromDate))
            .toList();

    Set<UUID> transactionIds = new LinkedHashSet<>();
    for (LedgerEntryEntity entry : activeEntries) {
      transactionIds.add(entry.getTransactionId());
    }

    int count = 0;
    LocalDate today = LocalDate.now();

    for (UUID originalTxnId : transactionIds) {
      if (ledgerEntryRepository.existsByReversalOfTransactionId(originalTxnId)) {
        continue;
      }

      List<LedgerEntryEntity> txnEntries = ledgerEntryRepository.findByTransactionId(originalTxnId);
      UUID stornoTxnId = UUID.randomUUID();

      for (LedgerEntryEntity original : txnEntries) {
        LedgerEntryEntity.EntryType reversedType =
            original.getEntryType() == LedgerEntryEntity.EntryType.DEBIT
                ? LedgerEntryEntity.EntryType.CREDIT
                : LedgerEntryEntity.EntryType.DEBIT;

        LedgerEntryEntity stornoEntry =
            LedgerEntryEntity.builder()
                .transactionId(stornoTxnId)
                .debtId(debtId)
                .accountCode(original.getAccountCode())
                .accountName(original.getAccountName())
                .entryType(reversedType)
                .amount(original.getAmount())
                .effectiveDate(original.getEffectiveDate())
                .postingDate(today)
                .reference(original.getReference())
                .description("STORNO (crossing): " + reason)
                .reversalOfTransactionId(originalTxnId)
                .entryCategory(LedgerEntryEntity.EntryCategory.STORNO)
                .build();

        ledgerEntryRepository.save(stornoEntry);
        count++;
      }
    }

    log.info(
        "STORNO: reversed {} entries across {} transactions from {} for debtId={}",
        count,
        transactionIds.size(),
        fromDate,
        debtId);
    return count;
  }

  private InterestPeriod calculateInterestForPeriod(
      LocalDate from, LocalDate to, BigDecimal principal, BigDecimal annualRate) {

    long days = ChronoUnit.DAYS.between(from, to);
    BigDecimal dailyRate = annualRate.divide(DAYS_PER_YEAR, CALC_SCALE, RoundingMode.HALF_UP);
    BigDecimal interest =
        principal
            .multiply(dailyRate)
            .multiply(BigDecimal.valueOf(days))
            .setScale(2, RoundingMode.HALF_UP);

    return InterestPeriod.builder()
        .periodStart(from)
        .periodEnd(to)
        .principalBalance(principal)
        .annualRate(annualRate)
        .days(days)
        .interestAmount(interest)
        .build();
  }

  private int postInterestEntries(UUID debtId, InterestPeriod period, String reference) {
    UUID txnId = UUID.randomUUID();
    LocalDate today = LocalDate.now();
    String desc =
        String.format(
            "Rente recalc: %s to %s, principal=%s, days=%d",
            period.getPeriodStart(),
            period.getPeriodEnd(),
            period.getPrincipalBalance().toPlainString(),
            period.getDays());

    ledgerEntryRepository.save(
        LedgerEntryEntity.builder()
            .transactionId(txnId)
            .debtId(debtId)
            .accountCode(AccountCode.INTEREST_RECEIVABLE.getCode())
            .accountName(AccountCode.INTEREST_RECEIVABLE.getName())
            .entryType(LedgerEntryEntity.EntryType.DEBIT)
            .amount(period.getInterestAmount())
            .effectiveDate(period.getPeriodStart())
            .postingDate(today)
            .reference(reference)
            .description(desc)
            .entryCategory(LedgerEntryEntity.EntryCategory.INTEREST_ACCRUAL)
            .build());

    ledgerEntryRepository.save(
        LedgerEntryEntity.builder()
            .transactionId(txnId)
            .debtId(debtId)
            .accountCode(AccountCode.INTEREST_REVENUE.getCode())
            .accountName(AccountCode.INTEREST_REVENUE.getName())
            .entryType(LedgerEntryEntity.EntryType.CREDIT)
            .amount(period.getInterestAmount())
            .effectiveDate(period.getPeriodStart())
            .postingDate(today)
            .reference(reference)
            .description(desc)
            .entryCategory(LedgerEntryEntity.EntryCategory.INTEREST_ACCRUAL)
            .build());

    return 2;
  }

  private int postRecoveryEntries(
      UUID debtId, LocalDate effectiveDate, CoverageAllocation allocation, String reference) {

    int count = 0;
    UUID txnId = UUID.randomUUID();
    LocalDate today = LocalDate.now();

    // Interest portion: debit bank, credit interest receivable
    if (allocation.getInterestPortion().compareTo(BigDecimal.ZERO) > 0) {
      ledgerEntryRepository.save(
          LedgerEntryEntity.builder()
              .transactionId(txnId)
              .debtId(debtId)
              .accountCode(AccountCode.SKB_BANK.getCode())
              .accountName(AccountCode.SKB_BANK.getName())
              .entryType(LedgerEntryEntity.EntryType.DEBIT)
              .amount(allocation.getInterestPortion())
              .effectiveDate(effectiveDate)
              .postingDate(today)
              .reference(reference)
              .description("Daekning: rente")
              .entryCategory(LedgerEntryEntity.EntryCategory.PAYMENT)
              .build());
      ledgerEntryRepository.save(
          LedgerEntryEntity.builder()
              .transactionId(txnId)
              .debtId(debtId)
              .accountCode(AccountCode.INTEREST_RECEIVABLE.getCode())
              .accountName(AccountCode.INTEREST_RECEIVABLE.getName())
              .entryType(LedgerEntryEntity.EntryType.CREDIT)
              .amount(allocation.getInterestPortion())
              .effectiveDate(effectiveDate)
              .postingDate(today)
              .reference(reference)
              .description("Daekning: rente")
              .entryCategory(LedgerEntryEntity.EntryCategory.PAYMENT)
              .build());
      count += 2;
    }

    // Principal portion: debit bank, credit receivables
    if (allocation.getPrincipalPortion().compareTo(BigDecimal.ZERO) > 0) {
      UUID txnId2 = UUID.randomUUID();
      ledgerEntryRepository.save(
          LedgerEntryEntity.builder()
              .transactionId(txnId2)
              .debtId(debtId)
              .accountCode(AccountCode.SKB_BANK.getCode())
              .accountName(AccountCode.SKB_BANK.getName())
              .entryType(LedgerEntryEntity.EntryType.DEBIT)
              .amount(allocation.getPrincipalPortion())
              .effectiveDate(effectiveDate)
              .postingDate(today)
              .reference(reference)
              .description("Daekning: hovedstol")
              .entryCategory(LedgerEntryEntity.EntryCategory.PAYMENT)
              .build());
      ledgerEntryRepository.save(
          LedgerEntryEntity.builder()
              .transactionId(txnId2)
              .debtId(debtId)
              .accountCode(AccountCode.RECEIVABLES.getCode())
              .accountName(AccountCode.RECEIVABLES.getName())
              .entryType(LedgerEntryEntity.EntryType.CREDIT)
              .amount(allocation.getPrincipalPortion())
              .effectiveDate(effectiveDate)
              .postingDate(today)
              .reference(reference)
              .description("Daekning: hovedstol")
              .entryCategory(LedgerEntryEntity.EntryCategory.PAYMENT)
              .build());
      count += 2;
    }

    return count;
  }

  private int repostEventEntries(UUID debtId, DebtEventEntity event) {
    UUID txnId = UUID.randomUUID();
    LocalDate today = LocalDate.now();

    AccountCode debitAccount;
    AccountCode creditAccount;
    LedgerEntryEntity.EntryCategory category;

    switch (event.getEventType()) {
      case DEBT_REGISTERED:
      case UDLAEG_REGISTERED:
        debitAccount = AccountCode.RECEIVABLES;
        creditAccount = AccountCode.COLLECTION_REVENUE;
        category = LedgerEntryEntity.EntryCategory.DEBT_REGISTRATION;
        break;
      case OFFSETTING_EXECUTED:
        debitAccount = AccountCode.OFFSETTING_CLEARING;
        creditAccount = AccountCode.RECEIVABLES;
        category = LedgerEntryEntity.EntryCategory.OFFSETTING;
        break;
      case WRITE_OFF:
        debitAccount = AccountCode.WRITE_OFF_EXPENSE;
        creditAccount = AccountCode.RECEIVABLES;
        category = LedgerEntryEntity.EntryCategory.WRITE_OFF;
        break;
      case REFUND:
        debitAccount = AccountCode.RECEIVABLES;
        creditAccount = AccountCode.SKB_BANK;
        category = LedgerEntryEntity.EntryCategory.REFUND;
        break;
      case UDLAEG_CORRECTED:
      case CORRECTION:
        if (event.getAmount().compareTo(BigDecimal.ZERO) >= 0) {
          debitAccount = AccountCode.RECEIVABLES;
          creditAccount = AccountCode.COLLECTION_REVENUE;
        } else {
          debitAccount = AccountCode.COLLECTION_REVENUE;
          creditAccount = AccountCode.RECEIVABLES;
        }
        category = LedgerEntryEntity.EntryCategory.CORRECTION;
        break;
      default:
        return 0;
    }

    BigDecimal amount = event.getAmount().abs();

    ledgerEntryRepository.save(
        LedgerEntryEntity.builder()
            .transactionId(txnId)
            .debtId(debtId)
            .accountCode(debitAccount.getCode())
            .accountName(debitAccount.getName())
            .entryType(LedgerEntryEntity.EntryType.DEBIT)
            .amount(amount)
            .effectiveDate(event.getEffectiveDate())
            .postingDate(today)
            .reference(event.getReference())
            .description("REPLAY: " + event.getDescription())
            .entryCategory(category)
            .build());

    ledgerEntryRepository.save(
        LedgerEntryEntity.builder()
            .transactionId(txnId)
            .debtId(debtId)
            .accountCode(creditAccount.getCode())
            .accountName(creditAccount.getName())
            .entryType(LedgerEntryEntity.EntryType.CREDIT)
            .amount(amount)
            .effectiveDate(event.getEffectiveDate())
            .postingDate(today)
            .reference(event.getReference())
            .description("REPLAY: " + event.getDescription())
            .entryCategory(category)
            .build());

    return 2;
  }

  private boolean isRecoveryEvent(DebtEventEntity event) {
    return event.getEventType() == DebtEventEntity.EventType.PAYMENT_RECEIVED
        || event.getEventType() == DebtEventEntity.EventType.OFFSETTING_EXECUTED;
  }

  private void applyEventToState(DebtEventEntity event, ReplayState state) {
    switch (event.getEventType()) {
      case DEBT_REGISTERED:
      case UDLAEG_REGISTERED:
        state.principalBalance = state.principalBalance.add(event.getAmount());
        break;
      case WRITE_OFF:
        state.principalBalance = state.principalBalance.subtract(event.getAmount());
        break;
      case UDLAEG_CORRECTED:
      case CORRECTION:
        state.principalBalance = state.principalBalance.add(event.getAmount());
        break;
      case REFUND:
        state.principalBalance = state.principalBalance.add(event.getAmount());
        break;
      default:
        break;
    }
  }

  private void checkForCoverageReversal(
      DebtEventEntity event,
      CoverageAllocation newAllocation,
      ReplayState state,
      List<CoverageReversal> reversals,
      String triggeringReference) {

    // If the event had a previous ledger transaction, compare old vs new allocation
    if (event.getLedgerTransactionId() != null) {
      List<LedgerEntryEntity> originalEntries =
          ledgerEntryRepository.findByTransactionId(event.getLedgerTransactionId());

      if (!originalEntries.isEmpty()) {
        BigDecimal originalInterestPortion = BigDecimal.ZERO;
        BigDecimal originalPrincipalPortion = BigDecimal.ZERO;

        for (LedgerEntryEntity entry : originalEntries) {
          if (entry.getEntryType() == LedgerEntryEntity.EntryType.CREDIT) {
            if (AccountCode.INTEREST_RECEIVABLE.getCode().equals(entry.getAccountCode())) {
              originalInterestPortion = originalInterestPortion.add(entry.getAmount());
            } else if (AccountCode.RECEIVABLES.getCode().equals(entry.getAccountCode())) {
              originalPrincipalPortion = originalPrincipalPortion.add(entry.getAmount());
            }
          }
        }

        boolean allocationChanged =
            newAllocation.getInterestPortion().compareTo(originalInterestPortion) != 0
                || newAllocation.getPrincipalPortion().compareTo(originalPrincipalPortion) != 0;

        if (allocationChanged) {
          CoverageAllocation original =
              CoverageAllocation.builder()
                  .debtId(event.getDebtId())
                  .totalAmount(event.getAmount())
                  .interestPortion(originalInterestPortion)
                  .principalPortion(originalPrincipalPortion)
                  .build();

          CoverageReversal reversal =
              CoverageReversal.builder()
                  .debtId(event.getDebtId())
                  .originalTransactionId(event.getLedgerTransactionId())
                  .crossingEventId(event.getId())
                  .effectiveDate(event.getEffectiveDate())
                  .originalAllocation(original)
                  .replacementAllocation(newAllocation)
                  .interestDelta(
                      newAllocation.getInterestPortion().subtract(originalInterestPortion))
                  .principalDelta(
                      newAllocation.getPrincipalPortion().subtract(originalPrincipalPortion))
                  .reason("Daekningsophaevelse: krydsende transaktion " + triggeringReference)
                  .build();

          reversals.add(reversal);

          log.info(
              "DAEKNINGSOPHAEVELSE: debtId={}, eventDate={}, "
                  + "oldInterest={}, newInterest={}, oldPrincipal={}, newPrincipal={}",
              event.getDebtId(),
              event.getEffectiveDate(),
              originalInterestPortion,
              newAllocation.getInterestPortion(),
              originalPrincipalPortion,
              newAllocation.getPrincipalPortion());
        }
      }
    }
  }

  private static class ReplayState {
    BigDecimal principalBalance = BigDecimal.ZERO;
    BigDecimal accruedInterest = BigDecimal.ZERO;
  }
}
