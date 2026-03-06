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

import dk.ufst.opendebt.payment.bookkeeping.entity.DebtEventEntity;
import dk.ufst.opendebt.payment.bookkeeping.entity.LedgerEntryEntity;
import dk.ufst.opendebt.payment.bookkeeping.model.CorrectionResult;
import dk.ufst.opendebt.payment.bookkeeping.model.InterestPeriod;
import dk.ufst.opendebt.payment.bookkeeping.repository.DebtEventRepository;
import dk.ufst.opendebt.payment.bookkeeping.repository.LedgerEntryRepository;
import dk.ufst.opendebt.payment.bookkeeping.service.InterestAccrualService;

@ExtendWith(MockitoExtension.class)
class RetroactiveCorrectionServiceImplTest {

  @Mock private LedgerEntryRepository ledgerEntryRepository;
  @Mock private DebtEventRepository debtEventRepository;
  @Mock private InterestAccrualService interestAccrualService;

  private RetroactiveCorrectionServiceImpl service;

  private static final UUID DEBT_ID = UUID.randomUUID();
  private static final LocalDate EFFECTIVE_DATE = LocalDate.of(2025, 12, 1);
  private static final BigDecimal ORIGINAL_AMOUNT = new BigDecimal("50000");
  private static final BigDecimal CORRECTED_AMOUNT = new BigDecimal("30000");
  private static final BigDecimal ANNUAL_RATE = new BigDecimal("0.10");

  @BeforeEach
  void setUp() {
    service =
        new RetroactiveCorrectionServiceImpl(
            ledgerEntryRepository, debtEventRepository, interestAccrualService);
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

    ArgumentCaptor<DebtEventEntity> captor = ArgumentCaptor.forClass(DebtEventEntity.class);
    verify(debtEventRepository).save(captor.capture());

    DebtEventEntity event = captor.getValue();
    assertThat(event.getDebtId()).isEqualTo(DEBT_ID);
    assertThat(event.getEventType()).isEqualTo(DebtEventEntity.EventType.UDLAEG_CORRECTED);
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
    ArgumentCaptor<LedgerEntryEntity> captor = ArgumentCaptor.forClass(LedgerEntryEntity.class);
    verify(ledgerEntryRepository, atLeast(2)).save(captor.capture());

    List<LedgerEntryEntity> correctionEntries =
        captor.getAllValues().stream()
            .filter(e -> e.getEntryCategory() == LedgerEntryEntity.EntryCategory.CORRECTION)
            .toList();

    assertThat(correctionEntries).hasSize(2);

    LedgerEntryEntity debit = correctionEntries.get(0);
    LedgerEntryEntity credit = correctionEntries.get(1);

    // Principal decreased: debit revenue, credit receivables
    assertThat(debit.getAccountCode()).isEqualTo("3000"); // Indrivelsesindtaegter
    assertThat(debit.getEntryType()).isEqualTo(LedgerEntryEntity.EntryType.DEBIT);
    assertThat(debit.getAmount()).isEqualByComparingTo("20000");

    assertThat(credit.getAccountCode()).isEqualTo("1000"); // Fordringer
    assertThat(credit.getEntryType()).isEqualTo(LedgerEntryEntity.EntryType.CREDIT);
    assertThat(credit.getAmount()).isEqualByComparingTo("20000");

    assertThat(debit.getEffectiveDate()).isEqualTo(EFFECTIVE_DATE);
  }

  @Test
  void applyRetroactiveCorrection_stornosAffectedInterestEntries() {
    UUID originalTxnId = UUID.randomUUID();
    LedgerEntryEntity interestDebit =
        buildInterestEntry(originalTxnId, "1100", LedgerEntryEntity.EntryType.DEBIT, "500.00");
    LedgerEntryEntity interestCredit =
        buildInterestEntry(originalTxnId, "3100", LedgerEntryEntity.EntryType.CREDIT, "500.00");

    when(ledgerEntryRepository.findInterestAccrualsAfterDate(DEBT_ID, EFFECTIVE_DATE))
        .thenReturn(List.of(interestDebit));
    when(ledgerEntryRepository.findByTransactionId(originalTxnId))
        .thenReturn(List.of(interestDebit, interestCredit));
    when(ledgerEntryRepository.existsByReversalOfTransactionId(originalTxnId)).thenReturn(false);
    when(interestAccrualService.calculatePeriodicInterest(
            eq(DEBT_ID), eq(EFFECTIVE_DATE), any(), eq(ANNUAL_RATE)))
        .thenReturn(List.of(buildInterestPeriod("300.00")));
    when(ledgerEntryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    when(debtEventRepository.save(any())).thenAnswer(i -> i.getArgument(0));

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
    ArgumentCaptor<LedgerEntryEntity> captor = ArgumentCaptor.forClass(LedgerEntryEntity.class);
    verify(ledgerEntryRepository, atLeast(4)).save(captor.capture());

    List<LedgerEntryEntity> stornoEntries =
        captor.getAllValues().stream()
            .filter(e -> e.getEntryCategory() == LedgerEntryEntity.EntryCategory.STORNO)
            .toList();

    assertThat(stornoEntries).hasSize(2);
    // Storno reverses: original DEBIT becomes CREDIT and vice versa
    assertThat(stornoEntries.get(0).getEntryType()).isEqualTo(LedgerEntryEntity.EntryType.CREDIT);
    assertThat(stornoEntries.get(1).getEntryType()).isEqualTo(LedgerEntryEntity.EntryType.DEBIT);
    assertThat(stornoEntries.get(0).getReversalOfTransactionId()).isEqualTo(originalTxnId);

    assertThat(result.getStornoEntriesPosted()).isEqualTo(2);
  }

  @Test
  void applyRetroactiveCorrection_recalculatesAndPostsNewInterest() {
    InterestPeriod period1 = buildInterestPeriod("300.00");
    InterestPeriod period2 = buildInterestPeriod("200.00");

    when(ledgerEntryRepository.findInterestAccrualsAfterDate(DEBT_ID, EFFECTIVE_DATE))
        .thenReturn(Collections.emptyList());
    when(interestAccrualService.calculatePeriodicInterest(
            eq(DEBT_ID), eq(EFFECTIVE_DATE), any(), eq(ANNUAL_RATE)))
        .thenReturn(List.of(period1, period2));
    when(ledgerEntryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    when(debtEventRepository.save(any())).thenAnswer(i -> i.getArgument(0));

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
    LedgerEntryEntity entry =
        buildInterestEntry(
            alreadyReversedTxnId, "1100", LedgerEntryEntity.EntryType.DEBIT, "500.00");

    when(ledgerEntryRepository.findInterestAccrualsAfterDate(DEBT_ID, EFFECTIVE_DATE))
        .thenReturn(List.of(entry));
    when(ledgerEntryRepository.existsByReversalOfTransactionId(alreadyReversedTxnId))
        .thenReturn(true); // Already reversed
    when(interestAccrualService.calculatePeriodicInterest(
            eq(DEBT_ID), eq(EFFECTIVE_DATE), any(), eq(ANNUAL_RATE)))
        .thenReturn(Collections.emptyList());
    when(ledgerEntryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    when(debtEventRepository.save(any())).thenAnswer(i -> i.getArgument(0));

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
    verify(ledgerEntryRepository, never()).findByTransactionId(alreadyReversedTxnId);
    assertThat(result.getStornoEntriesPosted()).isEqualTo(0);
  }

  @Test
  void applyRetroactiveCorrection_returnsCorrectDeltaCalculation() {
    UUID txnId = UUID.randomUUID();
    LedgerEntryEntity oldDebit =
        buildInterestEntry(txnId, "1100", LedgerEntryEntity.EntryType.DEBIT, "500.00");
    LedgerEntryEntity oldCredit =
        buildInterestEntry(txnId, "3100", LedgerEntryEntity.EntryType.CREDIT, "500.00");

    when(ledgerEntryRepository.findInterestAccrualsAfterDate(DEBT_ID, EFFECTIVE_DATE))
        .thenReturn(List.of(oldDebit));
    when(ledgerEntryRepository.findByTransactionId(txnId)).thenReturn(List.of(oldDebit, oldCredit));
    when(ledgerEntryRepository.existsByReversalOfTransactionId(txnId)).thenReturn(false);
    when(interestAccrualService.calculatePeriodicInterest(
            eq(DEBT_ID), eq(EFFECTIVE_DATE), any(), eq(ANNUAL_RATE)))
        .thenReturn(List.of(buildInterestPeriod("300.00")));
    when(ledgerEntryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    when(debtEventRepository.save(any())).thenAnswer(i -> i.getArgument(0));

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
    when(ledgerEntryRepository.findInterestAccrualsAfterDate(DEBT_ID, EFFECTIVE_DATE))
        .thenReturn(Collections.emptyList());
    when(interestAccrualService.calculatePeriodicInterest(
            eq(DEBT_ID), eq(EFFECTIVE_DATE), any(), eq(ANNUAL_RATE)))
        .thenReturn(Collections.emptyList());
    when(ledgerEntryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    when(debtEventRepository.save(any())).thenAnswer(i -> i.getArgument(0));
  }

  private LedgerEntryEntity buildInterestEntry(
      UUID txnId, String accountCode, LedgerEntryEntity.EntryType type, String amount) {
    return LedgerEntryEntity.builder()
        .id(UUID.randomUUID())
        .transactionId(txnId)
        .debtId(DEBT_ID)
        .accountCode(accountCode)
        .accountName("Test")
        .entryType(type)
        .amount(new BigDecimal(amount))
        .effectiveDate(EFFECTIVE_DATE)
        .postingDate(LocalDate.now())
        .reference("REF")
        .entryCategory(LedgerEntryEntity.EntryCategory.INTEREST_ACCRUAL)
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
