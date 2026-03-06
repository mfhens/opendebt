package dk.ufst.opendebt.payment.bookkeeping.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.ufst.opendebt.payment.bookkeeping.AccountCode;
import dk.ufst.opendebt.payment.bookkeeping.BookkeepingService;
import dk.ufst.opendebt.payment.bookkeeping.entity.DebtEventEntity;
import dk.ufst.opendebt.payment.bookkeeping.entity.LedgerEntryEntity;
import dk.ufst.opendebt.payment.bookkeeping.repository.DebtEventRepository;
import dk.ufst.opendebt.payment.bookkeeping.repository.LedgerEntryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// AIDEV-NOTE: Implements double-entry bookkeeping (ADR-0018). Every financial event produces
// exactly two LedgerEntryEntity rows (debit + credit) sharing the same transactionId.
// AIDEV-NOTE: postingDate is always LocalDate.now() (system date). effectiveDate is the economic
// date supplied by the caller. These two dates enable bi-temporal querying.
// AIDEV-TODO: Add balance validation (debit sum == credit sum per transactionId) as a
// @Transactional
// post-commit assertion or a scheduled reconciliation job.
@Slf4j
@Service
@RequiredArgsConstructor
public class BookkeepingServiceImpl implements BookkeepingService {

  private final LedgerEntryRepository ledgerEntryRepository;
  private final DebtEventRepository debtEventRepository;

  @Override
  @Transactional
  public void recordDebtRegistered(
      UUID debtId, BigDecimal principalAmount, LocalDate effectiveDate, String reference) {
    log.info("BOGFOERING: Debt registered, debtId={}, amount={}", debtId, principalAmount);
    UUID txnId =
        postDoubleEntry(
            debtId,
            AccountCode.RECEIVABLES,
            AccountCode.COLLECTION_REVENUE,
            principalAmount,
            effectiveDate,
            reference,
            "Fordring registreret",
            LedgerEntryEntity.EntryCategory.DEBT_REGISTRATION);
    recordEvent(
        debtId,
        DebtEventEntity.EventType.DEBT_REGISTERED,
        effectiveDate,
        principalAmount,
        reference,
        "Fordring registreret",
        txnId);
  }

  @Override
  @Transactional
  public void recordPaymentReceived(
      UUID debtId, BigDecimal amount, LocalDate effectiveDate, String cremulReference) {
    log.info(
        "BOGFOERING: Payment received (CREMUL), debtId={}, amount={}, ref={}",
        debtId,
        amount,
        cremulReference);
    UUID txnId =
        postDoubleEntry(
            debtId,
            AccountCode.SKB_BANK,
            AccountCode.RECEIVABLES,
            amount,
            effectiveDate,
            cremulReference,
            "Betaling modtaget via CREMUL",
            LedgerEntryEntity.EntryCategory.PAYMENT);
    recordEvent(
        debtId,
        DebtEventEntity.EventType.PAYMENT_RECEIVED,
        effectiveDate,
        amount,
        cremulReference,
        "Betaling modtaget via CREMUL",
        txnId);
  }

  @Override
  @Transactional
  public void recordInterestAccrued(
      UUID debtId, BigDecimal interestAmount, LocalDate effectiveDate, String reference) {
    log.info("BOGFOERING: Interest accrued, debtId={}, amount={}", debtId, interestAmount);
    UUID txnId =
        postDoubleEntry(
            debtId,
            AccountCode.INTEREST_RECEIVABLE,
            AccountCode.INTEREST_REVENUE,
            interestAmount,
            effectiveDate,
            reference,
            "Rente tilskrevet",
            LedgerEntryEntity.EntryCategory.INTEREST_ACCRUAL);
    recordEvent(
        debtId,
        DebtEventEntity.EventType.INTEREST_ACCRUED,
        effectiveDate,
        interestAmount,
        reference,
        "Rente tilskrevet",
        txnId);
  }

  @Override
  @Transactional
  public void recordOffsetting(
      UUID debtId, BigDecimal amount, LocalDate effectiveDate, String reference) {
    log.info("BOGFOERING: Offsetting, debtId={}, amount={}", debtId, amount);
    UUID txnId =
        postDoubleEntry(
            debtId,
            AccountCode.OFFSETTING_CLEARING,
            AccountCode.RECEIVABLES,
            amount,
            effectiveDate,
            reference,
            "Modregning gennemfoert",
            LedgerEntryEntity.EntryCategory.OFFSETTING);
    recordEvent(
        debtId,
        DebtEventEntity.EventType.OFFSETTING_EXECUTED,
        effectiveDate,
        amount,
        reference,
        "Modregning gennemfoert",
        txnId);
  }

  @Override
  @Transactional
  public void recordWriteOff(
      UUID debtId, BigDecimal amount, LocalDate effectiveDate, String reference) {
    log.info("BOGFOERING: Write-off, debtId={}, amount={}", debtId, amount);
    UUID txnId =
        postDoubleEntry(
            debtId,
            AccountCode.WRITE_OFF_EXPENSE,
            AccountCode.RECEIVABLES,
            amount,
            effectiveDate,
            reference,
            "Fordring afskrevet",
            LedgerEntryEntity.EntryCategory.WRITE_OFF);
    recordEvent(
        debtId,
        DebtEventEntity.EventType.WRITE_OFF,
        effectiveDate,
        amount,
        reference,
        "Fordring afskrevet",
        txnId);
  }

  @Override
  @Transactional
  public void recordRefund(
      UUID debtId, BigDecimal amount, LocalDate effectiveDate, String debmulReference) {
    log.info(
        "BOGFOERING: Refund (DEBMUL), debtId={}, amount={}, ref={}",
        debtId,
        amount,
        debmulReference);
    UUID txnId =
        postDoubleEntry(
            debtId,
            AccountCode.RECEIVABLES,
            AccountCode.SKB_BANK,
            amount,
            effectiveDate,
            debmulReference,
            "Tilbagebetaling via DEBMUL",
            LedgerEntryEntity.EntryCategory.REFUND);
    recordEvent(
        debtId,
        DebtEventEntity.EventType.REFUND,
        effectiveDate,
        amount,
        debmulReference,
        "Tilbagebetaling via DEBMUL",
        txnId);
  }

  private UUID postDoubleEntry(
      UUID debtId,
      AccountCode debitAccount,
      AccountCode creditAccount,
      BigDecimal amount,
      LocalDate effectiveDate,
      String reference,
      String description,
      LedgerEntryEntity.EntryCategory category) {

    UUID transactionId = UUID.randomUUID();
    LocalDate postingDate = LocalDate.now();

    LedgerEntryEntity debitEntry =
        LedgerEntryEntity.builder()
            .transactionId(transactionId)
            .debtId(debtId)
            .accountCode(debitAccount.getCode())
            .accountName(debitAccount.getName())
            .entryType(LedgerEntryEntity.EntryType.DEBIT)
            .amount(amount)
            .effectiveDate(effectiveDate)
            .postingDate(postingDate)
            .reference(reference)
            .description(description)
            .entryCategory(category)
            .build();

    LedgerEntryEntity creditEntry =
        LedgerEntryEntity.builder()
            .transactionId(transactionId)
            .debtId(debtId)
            .accountCode(creditAccount.getCode())
            .accountName(creditAccount.getName())
            .entryType(LedgerEntryEntity.EntryType.CREDIT)
            .amount(amount)
            .effectiveDate(effectiveDate)
            .postingDate(postingDate)
            .reference(reference)
            .description(description)
            .entryCategory(category)
            .build();

    ledgerEntryRepository.save(debitEntry);
    ledgerEntryRepository.save(creditEntry);

    log.debug(
        "BOGFOERING: Posted txn={}, debit={} credit={} amount={}, effective={}, posting={}",
        transactionId,
        debitAccount.getCode(),
        creditAccount.getCode(),
        amount,
        effectiveDate,
        postingDate);

    return transactionId;
  }

  private void recordEvent(
      UUID debtId,
      DebtEventEntity.EventType eventType,
      LocalDate effectiveDate,
      BigDecimal amount,
      String reference,
      String description,
      UUID ledgerTransactionId) {

    DebtEventEntity event =
        DebtEventEntity.builder()
            .debtId(debtId)
            .eventType(eventType)
            .effectiveDate(effectiveDate)
            .amount(amount)
            .reference(reference)
            .description(description)
            .ledgerTransactionId(ledgerTransactionId)
            .build();

    debtEventRepository.save(event);
  }
}
