package dk.ufst.opendebt.payment.bookkeeping.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import dk.ufst.bookkeeping.domain.EntryCategory;
import dk.ufst.bookkeeping.domain.EntryType;
import dk.ufst.bookkeeping.domain.EventType;
import dk.ufst.bookkeeping.domain.FinancialEvent;
import dk.ufst.bookkeeping.domain.LedgerEntry;
import dk.ufst.bookkeeping.model.CorrectionResult;
import dk.ufst.bookkeeping.model.InterestPeriod;
import dk.ufst.bookkeeping.port.FinancialEventStore;
import dk.ufst.bookkeeping.port.LedgerEntryStore;
import dk.ufst.bookkeeping.service.InterestAccrualService;
import dk.ufst.bookkeeping.service.impl.RetroactiveCorrectionServiceImpl;
import dk.ufst.bookkeeping.spi.Kontoplan;
import dk.ufst.opendebt.payment.bookkeeping.AccountCode;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RetroactiveCorrectionServiceImplTest {

  @Mock private LedgerEntryStore ledgerEntryStore;
  @Mock private FinancialEventStore financialEventStore;
  @Mock private InterestAccrualService interestAccrualService;
  @Mock private Kontoplan kontoplan;

  private RetroactiveCorrectionServiceImpl service;

  private static final UUID DEBT_ID = UUID.randomUUID();
  private static final LocalDate EFFECTIVE_DATE = LocalDate.of(2025, 12, 1);
  private static final BigDecimal ORIGINAL_AMOUNT = new BigDecimal("50000");
  private static final BigDecimal CORRECTED_AMOUNT = new BigDecimal("30000");
  private static final BigDecimal ANNUAL_RATE = new BigDecimal("0.10");

  @BeforeEach
  void setUp() {
    when(kontoplan.receivables()).thenReturn(AccountCode.RECEIVABLES);
    when(kontoplan.interestReceivable()).thenReturn(AccountCode.INTEREST_RECEIVABLE);
    when(kontoplan.bank()).thenReturn(AccountCode.SKB_BANK);
    when(kontoplan.collectionRevenue()).thenReturn(AccountCode.COLLECTION_REVENUE);
    when(kontoplan.interestRevenue()).thenReturn(AccountCode.INTEREST_REVENUE);
    when(kontoplan.writeOffExpense()).thenReturn(AccountCode.WRITE_OFF_EXPENSE);
    when(kontoplan.offsettingClearing()).thenReturn(AccountCode.OFFSETTING_CLEARING);

    service =
        new RetroactiveCorrectionServiceImpl(
            ledgerEntryStore, financialEventStore, interestAccrualService, kontoplan);
  }

  @Test
  void applyRetroactiveCorrection_recordsCorrectionEventInTimeline() {
    setupMocksForBasicCorrection();

    service.applyRetroactiveCorrection(
        DEBT_ID,
        EFFECTIVE_DATE,
        ORIGINAL_AMOUNT,
        CORRECTED_AMOUNT,
        ANNUAL_RATE,
        "RET-001",
        "Udlaeg nedsat");

    ArgumentCaptor<FinancialEvent> captor = ArgumentCaptor.forClass(FinancialEvent.class);
    verify(financialEventStore).save(captor.capture());

    FinancialEvent event = captor.getValue();
    assertThat(event.getDebtId()).isEqualTo(DEBT_ID);
    assertThat(event.getEventType()).isEqualTo(EventType.UDLAEG_CORRECTED);
    assertThat(event.getEffectiveDate()).isEqualTo(EFFECTIVE_DATE);
    assertThat(event.getAmount()).isEqualByComparingTo("-20000"); // delta = 30k - 50k
  }

  @Test
  void applyRetroactiveCorrection_postsPrincipalCorrectionToLedger() {
    setupMocksForBasicCorrection();

    service.applyRetroactiveCorrection(
        DEBT_ID,
        EFFECTIVE_DATE,
        ORIGINAL_AMOUNT,
        CORRECTED_AMOUNT,
        ANNUAL_RATE,
        "RET-001",
        "Udlaeg nedsat");

    // 2 entries for principal correction (no interest entries since no affected accruals)
    ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
    verify(ledgerEntryStore, atLeast(2)).saveSingle(captor.capture());

    List<LedgerEntry> correctionEntries =
        captor.getAllValues().stream()
            .filter(e -> e.getEntryCategory() == EntryCategory.CORRECTION)
            .toList();

    assertThat(correctionEntries).hasSize(2);

    LedgerEntry debit = correctionEntries.get(0);
    LedgerEntry credit = correctionEntries.get(1);

    // Principal decreased: debit revenue, credit receivables
    assertThat(debit.getAccountCode()).isEqualTo("3000"); // Indrivelsesindtaegter
    assertThat(debit.getEntryType()).isEqualTo(EntryType.DEBIT);
    assertThat(debit.getAmount()).isEqualByComparingTo("20000");

    assertThat(credit.getAccountCode()).isEqualTo("1000"); // Fordringer
    assertThat(credit.getEntryType()).isEqualTo(EntryType.CREDIT);
    assertThat(credit.getAmount()).isEqualByComparingTo("20000");

    assertThat(debit.getEffectiveDate()).isEqualTo(EFFECTIVE_DATE);
  }

  @Test
  void applyRetroactiveCorrection_stornosAffectedInterestEntries() {
    UUID originalTxnId = UUID.randomUUID();
    LedgerEntry interestDebit =
        buildInterestEntry(originalTxnId, "1100", EntryType.DEBIT, "500.00");
    LedgerEntry interestCredit =
        buildInterestEntry(originalTxnId, "3100", EntryType.CREDIT, "500.00");

    when(ledgerEntryStore.findInterestAccrualsAfterDate(DEBT_ID, EFFECTIVE_DATE))
        .thenReturn(List.of(interestDebit));
    when(ledgerEntryStore.findByTransactionId(originalTxnId))
        .thenReturn(List.of(interestDebit, interestCredit));
    when(ledgerEntryStore.existsByReversalOfTransactionId(originalTxnId)).thenReturn(false);
    when(interestAccrualService.calculatePeriodicInterest(
            eq(DEBT_ID), eq(EFFECTIVE_DATE), any(), eq(ANNUAL_RATE)))
        .thenReturn(List.of(buildInterestPeriod("300.00")));
    when(financialEventStore.save(any())).thenAnswer(i -> i.getArgument(0));

    CorrectionResult result =
        service.applyRetroactiveCorrection(
            DEBT_ID,
            EFFECTIVE_DATE,
            ORIGINAL_AMOUNT,
            CORRECTED_AMOUNT,
            ANNUAL_RATE,
            "RET-001",
            "Udlaeg nedsat");

    // Verify storno entries were posted (2 reversal entries for the original pair)
    ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
    verify(ledgerEntryStore, atLeast(4)).saveSingle(captor.capture());

    List<LedgerEntry> stornoEntries =
        captor.getAllValues().stream()
            .filter(e -> e.getEntryCategory() == EntryCategory.STORNO)
            .toList();

    assertThat(stornoEntries).hasSize(2);
    // Storno reverses: original DEBIT becomes CREDIT and vice versa
    assertThat(stornoEntries.get(0).getEntryType()).isEqualTo(EntryType.CREDIT);
    assertThat(stornoEntries.get(1).getEntryType()).isEqualTo(EntryType.DEBIT);
    assertThat(stornoEntries.get(0).getReversalOfTransactionId()).isEqualTo(originalTxnId);

    assertThat(result.getStornoEntriesPosted()).isEqualTo(2);
  }

  @Test
  void applyRetroactiveCorrection_recalculatesAndPostsNewInterest() {
    InterestPeriod period1 = buildInterestPeriod("300.00");
    InterestPeriod period2 = buildInterestPeriod("200.00");

    when(ledgerEntryStore.findInterestAccrualsAfterDate(DEBT_ID, EFFECTIVE_DATE))
        .thenReturn(Collections.emptyList());
    when(interestAccrualService.calculatePeriodicInterest(
            eq(DEBT_ID), eq(EFFECTIVE_DATE), any(), eq(ANNUAL_RATE)))
        .thenReturn(List.of(period1, period2));
    when(financialEventStore.save(any())).thenAnswer(i -> i.getArgument(0));

    CorrectionResult result =
        service.applyRetroactiveCorrection(
            DEBT_ID,
            EFFECTIVE_DATE,
            ORIGINAL_AMOUNT,
            CORRECTED_AMOUNT,
            ANNUAL_RATE,
            "RET-001",
            "Udlaeg nedsat");

    assertThat(result.getNewInterestEntriesPosted()).isEqualTo(4); // 2 periods x 2 entries each
    assertThat(result.getNewInterestTotal()).isEqualByComparingTo("500.00");
    assertThat(result.getRecalculatedPeriods()).hasSize(2);
  }

  @Test
  void applyRetroactiveCorrection_skipsAlreadyReversedTransactions() {
    UUID alreadyReversedTxnId = UUID.randomUUID();
    LedgerEntry entry = buildInterestEntry(alreadyReversedTxnId, "1100", EntryType.DEBIT, "500.00");

    when(ledgerEntryStore.findInterestAccrualsAfterDate(DEBT_ID, EFFECTIVE_DATE))
        .thenReturn(List.of(entry));
    when(ledgerEntryStore.existsByReversalOfTransactionId(alreadyReversedTxnId))
        .thenReturn(true); // Already reversed
    when(interestAccrualService.calculatePeriodicInterest(
            eq(DEBT_ID), eq(EFFECTIVE_DATE), any(), eq(ANNUAL_RATE)))
        .thenReturn(Collections.emptyList());
    when(financialEventStore.save(any())).thenAnswer(i -> i.getArgument(0));

    CorrectionResult result =
        service.applyRetroactiveCorrection(
            DEBT_ID,
            EFFECTIVE_DATE,
            ORIGINAL_AMOUNT,
            CORRECTED_AMOUNT,
            ANNUAL_RATE,
            "RET-001",
            "Udlaeg nedsat");

    // Should not attempt to fetch entries for the already-reversed transaction
    verify(ledgerEntryStore, never()).findByTransactionId(alreadyReversedTxnId);
    assertThat(result.getStornoEntriesPosted()).isEqualTo(0);
  }

  @Test
  void applyRetroactiveCorrection_returnsCorrectDeltaCalculation() {
    UUID txnId = UUID.randomUUID();
    LedgerEntry oldDebit = buildInterestEntry(txnId, "1100", EntryType.DEBIT, "500.00");
    LedgerEntry oldCredit = buildInterestEntry(txnId, "3100", EntryType.CREDIT, "500.00");

    when(ledgerEntryStore.findInterestAccrualsAfterDate(DEBT_ID, EFFECTIVE_DATE))
        .thenReturn(List.of(oldDebit));
    when(ledgerEntryStore.findByTransactionId(txnId)).thenReturn(List.of(oldDebit, oldCredit));
    when(ledgerEntryStore.existsByReversalOfTransactionId(txnId)).thenReturn(false);
    when(interestAccrualService.calculatePeriodicInterest(
            eq(DEBT_ID), eq(EFFECTIVE_DATE), any(), eq(ANNUAL_RATE)))
        .thenReturn(List.of(buildInterestPeriod("300.00")));
    when(financialEventStore.save(any())).thenAnswer(i -> i.getArgument(0));

    CorrectionResult result =
        service.applyRetroactiveCorrection(
            DEBT_ID,
            EFFECTIVE_DATE,
            ORIGINAL_AMOUNT,
            CORRECTED_AMOUNT,
            ANNUAL_RATE,
            "RET-001",
            "Fogedrettens afgoerelse");

    assertThat(result.getDebtId()).isEqualTo(DEBT_ID);
    assertThat(result.getPrincipalDelta()).isEqualByComparingTo("-20000");
    assertThat(result.getOldInterestTotal()).isEqualByComparingTo("500.00");
    assertThat(result.getNewInterestTotal()).isEqualByComparingTo("300.00");
    assertThat(result.getInterestDelta()).isEqualByComparingTo("-200.00");
  }

  private void setupMocksForBasicCorrection() {
    when(ledgerEntryStore.findInterestAccrualsAfterDate(DEBT_ID, EFFECTIVE_DATE))
        .thenReturn(Collections.emptyList());
    when(interestAccrualService.calculatePeriodicInterest(
            eq(DEBT_ID), eq(EFFECTIVE_DATE), any(), eq(ANNUAL_RATE)))
        .thenReturn(Collections.emptyList());
    when(financialEventStore.save(any())).thenAnswer(i -> i.getArgument(0));
  }

  private LedgerEntry buildInterestEntry(
      UUID txnId, String accountCode, EntryType type, String amount) {
    return LedgerEntry.builder()
        .transactionId(txnId)
        .debtId(DEBT_ID)
        .accountCode(accountCode)
        .accountName("Test")
        .entryType(type)
        .amount(new BigDecimal(amount))
        .effectiveDate(EFFECTIVE_DATE)
        .postingDate(LocalDate.now())
        .reference("REF")
        .entryCategory(EntryCategory.INTEREST_ACCRUAL)
        .build();
  }

  private InterestPeriod buildInterestPeriod(String amount) {
    return InterestPeriod.builder()
        .periodStart(EFFECTIVE_DATE)
        .periodEnd(LocalDate.now())
        .principalBalance(CORRECTED_AMOUNT)
        .annualRate(ANNUAL_RATE)
        .days(90)
        .interestAmount(new BigDecimal(amount))
        .build();
  }
}
