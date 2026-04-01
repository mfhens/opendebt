package dk.ufst.opendebt.payment.bookkeeping.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import dk.ufst.bookkeeping.domain.EntryCategory;
import dk.ufst.bookkeeping.domain.EntryType;
import dk.ufst.bookkeeping.domain.EventType;
import dk.ufst.bookkeeping.domain.FinancialEvent;
import dk.ufst.bookkeeping.domain.LedgerEntry;
import dk.ufst.bookkeeping.model.CoverageAllocation;
import dk.ufst.bookkeeping.model.TimelineReplayResult;
import dk.ufst.bookkeeping.port.CoveragePriorityPort;
import dk.ufst.bookkeeping.port.FinancialEventStore;
import dk.ufst.bookkeeping.port.LedgerEntryStore;
import dk.ufst.bookkeeping.service.impl.TimelineReplayServiceImpl;
import dk.ufst.bookkeeping.spi.Kontoplan;
import dk.ufst.opendebt.payment.bookkeeping.AccountCode;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TimelineReplayServiceImplTest {

  @Mock private FinancialEventStore financialEventStore;
  @Mock private LedgerEntryStore ledgerEntryStore;
  @Mock private CoveragePriorityPort coveragePriorityPort;
  @Mock private Kontoplan kontoplan;

  private TimelineReplayServiceImpl service;

  private static final UUID DEBT_ID = UUID.randomUUID();
  private static final BigDecimal RATE = new BigDecimal("0.0575");
  private static final LocalDate CROSSING_POINT = LocalDate.of(2026, 1, 1);

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
        new TimelineReplayServiceImpl(
            financialEventStore, ledgerEntryStore, coveragePriorityPort, kontoplan);
  }

  @Test
  void replayTimeline_withDebtRegistrationOnly_calculatesInterestToToday() {
    FinancialEvent registration =
        buildEvent(CROSSING_POINT, EventType.DEBT_REGISTERED, new BigDecimal("50000"));

    when(financialEventStore.findByDebtIdOrderByEffectiveDateAscCreatedAtAsc(DEBT_ID))
        .thenReturn(List.of(registration));
    when(ledgerEntryStore.findActiveEntriesByDebtId(DEBT_ID)).thenReturn(List.of());

    TimelineReplayResult result = service.replayTimeline(DEBT_ID, CROSSING_POINT, RATE, "TEST-REF");

    assertThat(result.getDebtId()).isEqualTo(DEBT_ID);
    assertThat(result.getReplayFromDate()).isEqualTo(CROSSING_POINT);
    assertThat(result.getRecalculatedInterestPeriods()).isNotEmpty();
    assertThat(result.getFinalPrincipalBalance()).isEqualByComparingTo("50000");
    assertThat(result.getNewEntriesPosted()).isPositive();
  }

  @Test
  void replayTimeline_withPaymentCrossingInterest_recalculatesCorrectly() {
    LocalDate regDate = LocalDate.of(2026, 1, 1);
    LocalDate payDate = LocalDate.of(2026, 1, 5);

    FinancialEvent registration =
        buildEvent(regDate, EventType.DEBT_REGISTERED, new BigDecimal("50000"));
    FinancialEvent payment =
        buildEvent(payDate, EventType.PAYMENT_RECEIVED, new BigDecimal("10000"));

    when(financialEventStore.findByDebtIdOrderByEffectiveDateAscCreatedAtAsc(DEBT_ID))
        .thenReturn(List.of(registration, payment));
    when(ledgerEntryStore.findActiveEntriesByDebtId(DEBT_ID)).thenReturn(List.of());

    CoverageAllocation allocation =
        CoverageAllocation.builder()
            .debtId(DEBT_ID)
            .totalAmount(new BigDecimal("10000"))
            .interestPortion(BigDecimal.ZERO)
            .principalPortion(new BigDecimal("10000"))
            .accruedInterestAtDate(BigDecimal.ZERO)
            .principalBalanceAtDate(new BigDecimal("50000"))
            .build();
    when(coveragePriorityPort.allocatePayment(eq(DEBT_ID), any(), any(), any(), any()))
        .thenReturn(allocation);

    TimelineReplayResult result =
        service.replayTimeline(DEBT_ID, CROSSING_POINT, RATE, "CROSSING-001");

    assertThat(result.getRecalculatedAllocations()).hasSize(1);
    assertThat(result.getFinalPrincipalBalance()).isEqualByComparingTo("40000");
    assertThat(result.getStornoEntriesPosted()).isZero();
  }

  @Test
  void replayTimeline_withCrossingPayment_producesInterestPeriodsBeforeAndAfterPayment() {
    LocalDate regDate = LocalDate.of(2026, 2, 1);
    LocalDate payDate = LocalDate.of(2026, 2, 15);

    FinancialEvent registration =
        buildEvent(regDate, EventType.DEBT_REGISTERED, new BigDecimal("100000"));
    FinancialEvent payment =
        buildEvent(payDate, EventType.PAYMENT_RECEIVED, new BigDecimal("20000"));

    when(financialEventStore.findByDebtIdOrderByEffectiveDateAscCreatedAtAsc(DEBT_ID))
        .thenReturn(List.of(registration, payment));
    when(ledgerEntryStore.findActiveEntriesByDebtId(DEBT_ID)).thenReturn(List.of());

    BigDecimal interestBeforePayment = new BigDecimal("220.55");
    CoverageAllocation allocation =
        CoverageAllocation.builder()
            .debtId(DEBT_ID)
            .totalAmount(new BigDecimal("20000"))
            .interestPortion(interestBeforePayment)
            .principalPortion(new BigDecimal("20000").subtract(interestBeforePayment))
            .accruedInterestAtDate(interestBeforePayment)
            .principalBalanceAtDate(new BigDecimal("100000"))
            .build();
    when(coveragePriorityPort.allocatePayment(eq(DEBT_ID), any(), any(), any(), any()))
        .thenReturn(allocation);

    TimelineReplayResult result = service.replayTimeline(DEBT_ID, regDate, RATE, "CROSSING-002");

    // Should have: interest for Feb 1-15 on 100k, then interest for Feb 15-today on reduced
    // principal
    assertThat(result.getRecalculatedInterestPeriods().size()).isGreaterThanOrEqualTo(2);
    assertThat(result.getRecalculatedAllocations()).hasSize(1);
  }

  @Test
  void replayTimeline_stornosExistingEntriesFromCrossingPoint() {
    UUID existingTxnId = UUID.randomUUID();
    LedgerEntry existingDebit =
        LedgerEntry.builder()
            .transactionId(existingTxnId)
            .debtId(DEBT_ID)
            .accountCode("1100")
            .accountName("Renter tilgodehavende")
            .entryType(EntryType.DEBIT)
            .amount(new BigDecimal("500"))
            .effectiveDate(CROSSING_POINT.plusDays(5))
            .postingDate(CROSSING_POINT.plusDays(10))
            .reference("OLD-REF")
            .entryCategory(EntryCategory.INTEREST_ACCRUAL)
            .build();
    LedgerEntry existingCredit =
        LedgerEntry.builder()
            .transactionId(existingTxnId)
            .debtId(DEBT_ID)
            .accountCode("3100")
            .accountName("Renteindtaegter")
            .entryType(EntryType.CREDIT)
            .amount(new BigDecimal("500"))
            .effectiveDate(CROSSING_POINT.plusDays(5))
            .postingDate(CROSSING_POINT.plusDays(10))
            .reference("OLD-REF")
            .entryCategory(EntryCategory.INTEREST_ACCRUAL)
            .build();

    FinancialEvent registration =
        buildEvent(CROSSING_POINT, EventType.DEBT_REGISTERED, new BigDecimal("50000"));

    when(ledgerEntryStore.findActiveEntriesByDebtId(DEBT_ID))
        .thenReturn(List.of(existingDebit, existingCredit));
    when(ledgerEntryStore.existsByReversalOfTransactionId(existingTxnId)).thenReturn(false);
    when(ledgerEntryStore.findByTransactionId(existingTxnId))
        .thenReturn(List.of(existingDebit, existingCredit));
    when(financialEventStore.findByDebtIdOrderByEffectiveDateAscCreatedAtAsc(DEBT_ID))
        .thenReturn(List.of(registration));

    TimelineReplayResult result =
        service.replayTimeline(DEBT_ID, CROSSING_POINT, RATE, "CROSSING-003");

    assertThat(result.getStornoEntriesPosted()).isEqualTo(2);
  }

  @Test
  void replayTimeline_isIdempotent_producesConsistentResults() {
    FinancialEvent registration =
        buildEvent(CROSSING_POINT, EventType.DEBT_REGISTERED, new BigDecimal("50000"));

    when(financialEventStore.findByDebtIdOrderByEffectiveDateAscCreatedAtAsc(DEBT_ID))
        .thenReturn(List.of(registration));
    when(ledgerEntryStore.findActiveEntriesByDebtId(DEBT_ID)).thenReturn(List.of());

    TimelineReplayResult result1 = service.replayTimeline(DEBT_ID, CROSSING_POINT, RATE, "REF");
    TimelineReplayResult result2 = service.replayTimeline(DEBT_ID, CROSSING_POINT, RATE, "REF");

    assertThat(result1.getFinalPrincipalBalance())
        .isEqualByComparingTo(result2.getFinalPrincipalBalance());
    assertThat(result1.getNewInterestTotal()).isEqualByComparingTo(result2.getNewInterestTotal());
    assertThat(result1.getRecalculatedInterestPeriods().size())
        .isEqualTo(result2.getRecalculatedInterestPeriods().size());
  }

  private FinancialEvent buildEvent(LocalDate effectiveDate, EventType type, BigDecimal amount) {
    return FinancialEvent.builder()
        .id(UUID.randomUUID())
        .debtId(DEBT_ID)
        .effectiveDate(effectiveDate)
        .eventType(type)
        .amount(amount)
        .reference("REF-" + type.name())
        .description("Test " + type.name())
        .createdAt(
            LocalDateTime.of(
                effectiveDate.getYear(),
                effectiveDate.getMonthValue(),
                effectiveDate.getDayOfMonth(),
                12,
                0))
        .build();
  }
}
