package dk.ufst.opendebt.payment.bookkeeping.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.ufst.bookkeeping.domain.EntryCategory;
import dk.ufst.bookkeeping.domain.EventType;
import dk.ufst.bookkeeping.engine.BookkeepingEngine;
import dk.ufst.opendebt.payment.bookkeeping.AccountCode;
import dk.ufst.opendebt.payment.bookkeeping.BookkeepingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// AIDEV-NOTE: Implements double-entry bookkeeping (ADR-0018). Delegates ledger and event
// persistence to BookkeepingEngine (core module). @Transactional boundaries stay here so the
// Spring transaction context covers both the ledger entry saves and the debt event save.
// AIDEV-NOTE: postingDate is always LocalDate.now() (system date). effectiveDate is the economic
// date supplied by the caller. These two dates enable bi-temporal querying.
// AIDEV-NOTE: Dual-write to immudb (ADR-0029) is wired inside JpaLedgerEntryStore.saveDoubleEntry.
@Slf4j
@Service
@RequiredArgsConstructor
public class BookkeepingServiceImpl implements BookkeepingService {

  private final BookkeepingEngine bookkeepingEngine;

  @Override
  @Transactional
  public void recordDebtRegistered(
      UUID debtId, BigDecimal principalAmount, LocalDate effectiveDate, String reference) {
    log.info("BOGFOERING: Debt registered, debtId={}, amount={}", debtId, principalAmount);
    UUID txnId =
        bookkeepingEngine.postDoubleEntry(
            debtId,
            AccountCode.RECEIVABLES,
            AccountCode.COLLECTION_REVENUE,
            principalAmount,
            effectiveDate,
            reference,
            "Fordring registreret",
            EntryCategory.DEBT_REGISTRATION);
    bookkeepingEngine.recordEvent(
        debtId,
        EventType.DEBT_REGISTERED,
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
        bookkeepingEngine.postDoubleEntry(
            debtId,
            AccountCode.SKB_BANK,
            AccountCode.RECEIVABLES,
            amount,
            effectiveDate,
            cremulReference,
            "Betaling modtaget via CREMUL",
            EntryCategory.PAYMENT);
    bookkeepingEngine.recordEvent(
        debtId,
        EventType.PAYMENT_RECEIVED,
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
        bookkeepingEngine.postDoubleEntry(
            debtId,
            AccountCode.INTEREST_RECEIVABLE,
            AccountCode.INTEREST_REVENUE,
            interestAmount,
            effectiveDate,
            reference,
            "Rente tilskrevet",
            EntryCategory.INTEREST_ACCRUAL);
    bookkeepingEngine.recordEvent(
        debtId,
        EventType.INTEREST_ACCRUED,
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
        bookkeepingEngine.postDoubleEntry(
            debtId,
            AccountCode.OFFSETTING_CLEARING,
            AccountCode.RECEIVABLES,
            amount,
            effectiveDate,
            reference,
            "Modregning gennemfoert",
            EntryCategory.OFFSETTING);
    bookkeepingEngine.recordEvent(
        debtId,
        EventType.OFFSETTING_EXECUTED,
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
        bookkeepingEngine.postDoubleEntry(
            debtId,
            AccountCode.WRITE_OFF_EXPENSE,
            AccountCode.RECEIVABLES,
            amount,
            effectiveDate,
            reference,
            "Fordring afskrevet",
            EntryCategory.WRITE_OFF);
    bookkeepingEngine.recordEvent(
        debtId, EventType.WRITE_OFF, effectiveDate, amount, reference, "Fordring afskrevet", txnId);
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
        bookkeepingEngine.postDoubleEntry(
            debtId,
            AccountCode.RECEIVABLES,
            AccountCode.SKB_BANK,
            amount,
            effectiveDate,
            debmulReference,
            "Tilbagebetaling via DEBMUL",
            EntryCategory.REFUND);
    bookkeepingEngine.recordEvent(
        debtId,
        EventType.REFUND,
        effectiveDate,
        amount,
        debmulReference,
        "Tilbagebetaling via DEBMUL",
        txnId);
  }
}
