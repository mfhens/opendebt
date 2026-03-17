package dk.ufst.opendebt.payment.bookkeeping.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.ufst.opendebt.payment.bookkeeping.AccountCode;
import dk.ufst.opendebt.payment.bookkeeping.entity.DebtEventEntity;
import dk.ufst.opendebt.payment.bookkeeping.entity.LedgerEntryEntity;
import dk.ufst.opendebt.payment.bookkeeping.model.CorrectionResult;
import dk.ufst.opendebt.payment.bookkeeping.model.InterestPeriod;
import dk.ufst.opendebt.payment.bookkeeping.repository.DebtEventRepository;
import dk.ufst.opendebt.payment.bookkeeping.repository.LedgerEntryRepository;
import dk.ufst.opendebt.payment.bookkeeping.service.InterestAccrualService;
import dk.ufst.opendebt.payment.bookkeeping.service.RetroactiveCorrectionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetroactiveCorrectionServiceImpl implements RetroactiveCorrectionService {

  private static final String CORRECTION_PREFIX = "KORREKTION: ";

  private final LedgerEntryRepository ledgerEntryRepository;
  private final DebtEventRepository debtEventRepository;
  private final InterestAccrualService interestAccrualService;

  @Override
  @Transactional
  public CorrectionResult applyRetroactiveCorrection(
      UUID debtId,
      LocalDate effectiveDate,
      BigDecimal originalAmount,
      BigDecimal correctedAmount,
      BigDecimal annualInterestRate,
      String reference,
      String reason) {

    log.info(
        "CORRECTION: debtId={}, effectiveDate={}, original={}, corrected={}, reason={}",
        debtId,
        effectiveDate,
        originalAmount,
        correctedAmount,
        reason);

    BigDecimal principalDelta = correctedAmount.subtract(originalAmount);

    // 1. Record correction event in timeline
    DebtEventEntity correctionEvent =
        recordCorrectionEvent(debtId, effectiveDate, principalDelta, reference, reason);

    // 2. Post the principal correction to the ledger
    postPrincipalCorrection(debtId, effectiveDate, principalDelta, reference, reason);

    // 3. Find and storno all interest accruals after the effective date
    List<LedgerEntryEntity> affectedInterestEntries =
        ledgerEntryRepository.findInterestAccrualsAfterDate(debtId, effectiveDate);

    BigDecimal oldInterestTotal = calculateInterestTotal(affectedInterestEntries);
    int stornoCount = stornoInterestEntries(debtId, affectedInterestEntries, reason);

    // 4. Recalculate interest from effective date to today
    LocalDate recalcEndDate = LocalDate.now();
    List<InterestPeriod> newPeriods =
        interestAccrualService.calculatePeriodicInterest(
            debtId, effectiveDate, recalcEndDate, annualInterestRate);

    // 5. Post new interest accrual entries
    int newEntryCount = postRecalculatedInterest(debtId, newPeriods, reference);

    BigDecimal newInterestTotal =
        newPeriods.stream()
            .map(InterestPeriod::getInterestAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal interestDelta = newInterestTotal.subtract(oldInterestTotal);

    log.info(
        "CORRECTION COMPLETE: debtId={}, principalDelta={}, storno={}, newEntries={}, "
            + "oldInterest={}, newInterest={}, interestDelta={}",
        debtId,
        principalDelta,
        stornoCount,
        newEntryCount,
        oldInterestTotal,
        newInterestTotal,
        interestDelta);

    return CorrectionResult.builder()
        .debtId(debtId)
        .correctionEventId(correctionEvent.getId())
        .principalDelta(principalDelta)
        .stornoEntriesPosted(stornoCount)
        .newInterestEntriesPosted(newEntryCount)
        .oldInterestTotal(oldInterestTotal)
        .newInterestTotal(newInterestTotal)
        .interestDelta(interestDelta)
        .recalculatedPeriods(newPeriods)
        .build();
  }

  private DebtEventEntity recordCorrectionEvent(
      UUID debtId, LocalDate effectiveDate, BigDecimal delta, String reference, String reason) {

    DebtEventEntity event =
        DebtEventEntity.builder()
            .debtId(debtId)
            .eventType(DebtEventEntity.EventType.UDLAEG_CORRECTED)
            .effectiveDate(effectiveDate)
            .amount(delta)
            .reference(reference)
            .description(reason)
            .build();

    return debtEventRepository.save(event);
  }

  private void postPrincipalCorrection(
      UUID debtId, LocalDate effectiveDate, BigDecimal delta, String reference, String reason) {

    UUID transactionId = UUID.randomUUID();
    LocalDate today = LocalDate.now();

    if (delta.compareTo(BigDecimal.ZERO) < 0) {
      // Principal decreased (e.g., udlaeg reduced): reverse the receivable
      BigDecimal absDelta = delta.abs();
      saveLedgerEntry(
          new LedgerEntryRequest(
              transactionId,
              debtId,
              AccountCode.COLLECTION_REVENUE,
              LedgerEntryEntity.EntryType.DEBIT,
              absDelta,
              effectiveDate,
              today,
              reference,
              correctionDescription(reason),
              null,
              LedgerEntryEntity.EntryCategory.CORRECTION));
      saveLedgerEntry(
          new LedgerEntryRequest(
              transactionId,
              debtId,
              AccountCode.RECEIVABLES,
              LedgerEntryEntity.EntryType.CREDIT,
              absDelta,
              effectiveDate,
              today,
              reference,
              correctionDescription(reason),
              null,
              LedgerEntryEntity.EntryCategory.CORRECTION));
    } else if (delta.compareTo(BigDecimal.ZERO) > 0) {
      // Principal increased
      saveLedgerEntry(
          new LedgerEntryRequest(
              transactionId,
              debtId,
              AccountCode.RECEIVABLES,
              LedgerEntryEntity.EntryType.DEBIT,
              delta,
              effectiveDate,
              today,
              reference,
              correctionDescription(reason),
              null,
              LedgerEntryEntity.EntryCategory.CORRECTION));
      saveLedgerEntry(
          new LedgerEntryRequest(
              transactionId,
              debtId,
              AccountCode.COLLECTION_REVENUE,
              LedgerEntryEntity.EntryType.CREDIT,
              delta,
              effectiveDate,
              today,
              reference,
              correctionDescription(reason),
              null,
              LedgerEntryEntity.EntryCategory.CORRECTION));
    }
  }

  private int stornoInterestEntries(UUID debtId, List<LedgerEntryEntity> entries, String reason) {

    // Group by transaction ID to storno complete double-entry pairs
    Set<UUID> transactionIds =
        entries.stream().map(LedgerEntryEntity::getTransactionId).collect(Collectors.toSet());

    int count = 0;
    LocalDate today = LocalDate.now();

    for (UUID originalTxnId : transactionIds) {
      if (ledgerEntryRepository.existsByReversalOfTransactionId(originalTxnId)) {
        log.debug("Transaction {} already reversed, skipping", originalTxnId);
        continue;
      }

      List<LedgerEntryEntity> txnEntries = ledgerEntryRepository.findByTransactionId(originalTxnId);
      UUID stornoTxnId = UUID.randomUUID();

      for (LedgerEntryEntity original : txnEntries) {
        // Reverse: swap debit/credit
        LedgerEntryEntity.EntryType reversedType =
            original.getEntryType() == LedgerEntryEntity.EntryType.DEBIT
                ? LedgerEntryEntity.EntryType.CREDIT
                : LedgerEntryEntity.EntryType.DEBIT;

        saveLedgerEntry(
            new LedgerEntryRequest(
                stornoTxnId,
                debtId,
                findAccountCode(original.getAccountCode()),
                reversedType,
                original.getAmount(),
                original.getEffectiveDate(),
                today,
                original.getReference(),
                "STORNO: " + reason,
                originalTxnId,
                LedgerEntryEntity.EntryCategory.STORNO));
        count++;
      }
    }

    log.info("Storno: reversed {} entries across {} transactions", count, transactionIds.size());
    return count;
  }

  private int postRecalculatedInterest(
      UUID debtId, List<InterestPeriod> periods, String reference) {

    int count = 0;
    LocalDate today = LocalDate.now();

    for (InterestPeriod period : periods) {
      if (period.getInterestAmount().compareTo(BigDecimal.ZERO) <= 0) {
        continue;
      }

      UUID transactionId = UUID.randomUUID();
      String desc =
          String.format(
              "Interest recalculated: %s to %s, principal=%s, days=%d",
              period.getPeriodStart(),
              period.getPeriodEnd(),
              period.getPrincipalBalance().toPlainString(),
              period.getDays());

      saveLedgerEntry(
          new LedgerEntryRequest(
              transactionId,
              debtId,
              AccountCode.INTEREST_RECEIVABLE,
              LedgerEntryEntity.EntryType.DEBIT,
              period.getInterestAmount(),
              period.getPeriodStart(),
              today,
              reference,
              desc,
              null,
              LedgerEntryEntity.EntryCategory.INTEREST_ACCRUAL));
      saveLedgerEntry(
          new LedgerEntryRequest(
              transactionId,
              debtId,
              AccountCode.INTEREST_REVENUE,
              LedgerEntryEntity.EntryType.CREDIT,
              period.getInterestAmount(),
              period.getPeriodStart(),
              today,
              reference,
              desc,
              null,
              LedgerEntryEntity.EntryCategory.INTEREST_ACCRUAL));
      count += 2;
    }

    return count;
  }

  private BigDecimal calculateInterestTotal(List<LedgerEntryEntity> entries) {
    return entries.stream()
        .filter(e -> e.getEntryType() == LedgerEntryEntity.EntryType.DEBIT)
        .filter(e -> AccountCode.INTEREST_RECEIVABLE.getCode().equals(e.getAccountCode()))
        .map(LedgerEntryEntity::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private void saveLedgerEntry(LedgerEntryRequest request) {

    LedgerEntryEntity entry =
        LedgerEntryEntity.builder()
            .transactionId(request.transactionId())
            .debtId(request.debtId())
            .accountCode(request.account().getCode())
            .accountName(request.account().getName())
            .entryType(request.entryType())
            .amount(request.amount())
            .effectiveDate(request.effectiveDate())
            .postingDate(request.postingDate())
            .reference(request.reference())
            .description(request.description())
            .reversalOfTransactionId(request.reversalOfTransactionId())
            .entryCategory(request.category())
            .build();

    ledgerEntryRepository.save(entry);
  }

  private String correctionDescription(String reason) {
    return CORRECTION_PREFIX + reason;
  }

  private AccountCode findAccountCode(String code) {
    for (AccountCode ac : AccountCode.values()) {
      if (ac.getCode().equals(code)) {
        return ac;
      }
    }
    throw new IllegalArgumentException("Unknown account code: " + code);
  }

  private record LedgerEntryRequest(
      UUID transactionId,
      UUID debtId,
      AccountCode account,
      LedgerEntryEntity.EntryType entryType,
      BigDecimal amount,
      LocalDate effectiveDate,
      LocalDate postingDate,
      String reference,
      String description,
      UUID reversalOfTransactionId,
      LedgerEntryEntity.EntryCategory category) {}
}
