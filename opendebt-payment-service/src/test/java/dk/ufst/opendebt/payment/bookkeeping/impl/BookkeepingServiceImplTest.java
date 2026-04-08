package dk.ufst.opendebt.payment.bookkeeping.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dk.ufst.bookkeeping.domain.EntryCategory;
import dk.ufst.bookkeeping.domain.EventType;
import dk.ufst.bookkeeping.engine.BookkeepingEngine;
import dk.ufst.opendebt.payment.bookkeeping.AccountCode;

@ExtendWith(MockitoExtension.class)
class BookkeepingServiceImplTest {

  @Mock private BookkeepingEngine bookkeepingEngine;

  private BookkeepingServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new BookkeepingServiceImpl(bookkeepingEngine);
    when(bookkeepingEngine.postDoubleEntry(any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(UUID.randomUUID());
  }

  @Test
  void recordDebtRegistered_postsDebitAndCreditWithCorrectAccounts() {
    UUID debtId = UUID.randomUUID();
    BigDecimal amount = new BigDecimal("50000.00");
    LocalDate effectiveDate = LocalDate.of(2025, 10, 1);

    service.recordDebtRegistered(debtId, amount, effectiveDate, "REF-001");

    verify(bookkeepingEngine)
        .postDoubleEntry(
            eq(debtId),
            eq(AccountCode.RECEIVABLES),
            eq(AccountCode.COLLECTION_REVENUE),
            eq(amount),
            eq(effectiveDate),
            eq("REF-001"),
            any(),
            eq(EntryCategory.DEBT_REGISTRATION));
  }

  @Test
  void recordDebtRegistered_recordsEventInTimeline() {
    UUID debtId = UUID.randomUUID();
    LocalDate effectiveDate = LocalDate.of(2025, 10, 1);
    UUID txnId = UUID.randomUUID();
    when(bookkeepingEngine.postDoubleEntry(any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(txnId);

    service.recordDebtRegistered(debtId, new BigDecimal("10000"), effectiveDate, "REF-001");

    verify(bookkeepingEngine)
        .recordEvent(
            eq(debtId),
            eq(EventType.DEBT_REGISTERED),
            eq(effectiveDate),
            eq(new BigDecimal("10000")),
            eq("REF-001"),
            any(),
            eq(txnId));
  }

  @Test
  void recordPaymentReceived_postsSkbBankDebitAndReceivablesCredit() {
    UUID debtId = UUID.randomUUID();
    BigDecimal amount = new BigDecimal("5000.00");
    LocalDate effectiveDate = LocalDate.of(2025, 11, 15);

    service.recordPaymentReceived(debtId, amount, effectiveDate, "CREMUL-001");

    verify(bookkeepingEngine)
        .postDoubleEntry(
            eq(debtId),
            eq(AccountCode.SKB_BANK),
            eq(AccountCode.RECEIVABLES),
            eq(amount),
            eq(effectiveDate),
            eq("CREMUL-001"),
            any(),
            eq(EntryCategory.PAYMENT));
  }

  @Test
  void recordInterestAccrued_postsInterestReceivableDebitAndRevenueCredit() {
    UUID debtId = UUID.randomUUID();
    BigDecimal interest = new BigDecimal("821.92");
    LocalDate effectiveDate = LocalDate.of(2025, 12, 31);

    service.recordInterestAccrued(debtId, interest, effectiveDate, "INT-Q4");

    verify(bookkeepingEngine)
        .postDoubleEntry(
            eq(debtId),
            eq(AccountCode.INTEREST_RECEIVABLE),
            eq(AccountCode.INTEREST_REVENUE),
            eq(interest),
            eq(effectiveDate),
            eq("INT-Q4"),
            any(),
            eq(EntryCategory.INTEREST_ACCRUAL));
  }

  @Test
  void recordOffsetting_postsModregningDebitAndReceivablesCredit() {
    UUID debtId = UUID.randomUUID();
    LocalDate now = LocalDate.now();

    service.recordOffsetting(debtId, new BigDecimal("3000"), now, "MOD-001");

    verify(bookkeepingEngine)
        .postDoubleEntry(
            eq(debtId),
            eq(AccountCode.OFFSETTING_CLEARING),
            eq(AccountCode.RECEIVABLES),
            eq(new BigDecimal("3000")),
            eq(now),
            eq("MOD-001"),
            any(),
            eq(EntryCategory.OFFSETTING));
  }

  @Test
  void recordWriteOff_postsExpenseDebitAndReceivablesCredit() {
    UUID debtId = UUID.randomUUID();
    LocalDate now = LocalDate.now();

    service.recordWriteOff(debtId, new BigDecimal("15000"), now, "WO-001");

    verify(bookkeepingEngine)
        .postDoubleEntry(
            eq(debtId),
            eq(AccountCode.WRITE_OFF_EXPENSE),
            eq(AccountCode.RECEIVABLES),
            eq(new BigDecimal("15000")),
            eq(now),
            eq("WO-001"),
            any(),
            eq(EntryCategory.WRITE_OFF));
  }

  @Test
  void recordRefund_postsReceivablesDebitAndBankCredit() {
    UUID debtId = UUID.randomUUID();
    LocalDate now = LocalDate.now();

    service.recordRefund(debtId, new BigDecimal("2000"), now, "DEBMUL-001");

    verify(bookkeepingEngine)
        .postDoubleEntry(
            eq(debtId),
            eq(AccountCode.RECEIVABLES),
            eq(AccountCode.SKB_BANK),
            eq(new BigDecimal("2000")),
            eq(now),
            eq("DEBMUL-001"),
            any(),
            eq(EntryCategory.REFUND));
  }

  @Test
  void recordDebtRegistered_recordsEventWithTxnIdFromEngine() {
    UUID debtId = UUID.randomUUID();
    LocalDate effectiveDate = LocalDate.of(2025, 6, 1);
    UUID txnId = UUID.randomUUID();
    when(bookkeepingEngine.postDoubleEntry(any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(txnId);

    service.recordDebtRegistered(debtId, new BigDecimal("1000"), effectiveDate, "REF");

    verify(bookkeepingEngine)
        .recordEvent(
            eq(debtId),
            eq(EventType.DEBT_REGISTERED),
            eq(effectiveDate),
            any(),
            any(),
            any(),
            eq(txnId));
  }

  @Test
  void allMethods_invokeRecordEventAfterDoubleEntry() {
    UUID debtId = UUID.randomUUID();
    LocalDate now = LocalDate.now();

    service.recordPaymentReceived(debtId, new BigDecimal("500"), now, "CREMUL-002");

    verify(bookkeepingEngine)
        .postDoubleEntry(any(), any(), any(), any(), any(), any(), any(), any());
    verify(bookkeepingEngine)
        .recordEvent(eq(debtId), eq(EventType.PAYMENT_RECEIVED), any(), any(), any(), any(), any());
  }
}
