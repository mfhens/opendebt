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

import dk.ufst.opendebt.payment.bookkeeping.entity.DebtEventEntity;
import dk.ufst.opendebt.payment.bookkeeping.entity.LedgerEntryEntity;
import dk.ufst.opendebt.payment.bookkeeping.model.CoverageAllocation;
import dk.ufst.opendebt.payment.bookkeeping.model.TimelineReplayResult;
import dk.ufst.opendebt.payment.bookkeeping.repository.DebtEventRepository;
import dk.ufst.opendebt.payment.bookkeeping.repository.LedgerEntryRepository;
import dk.ufst.opendebt.payment.bookkeeping.service.CoveragePriorityService;

@ExtendWith(MockitoExtension.class)
class TimelineReplayServiceImplTest {

  @Mock private DebtEventRepository debtEventRepository;
  @Mock private LedgerEntryRepository ledgerEntryRepository;
  @Mock private CoveragePriorityService coveragePriorityService;

  private TimelineReplayServiceImpl service;

  private static final UUID DEBT_ID = UUID.randomUUID();
  private static final BigDecimal RATE = new BigDecimal("0.0575");
  private static final LocalDate CROSSING_POINT = LocalDate.of(2026, 1, 1);

  @BeforeEach
  void setUp() {
    service =
        new TimelineReplayServiceImpl(
            debtEventRepository, ledgerEntryRepository, coveragePriorityService);
  }

  @Test
  void replayTimeline_withDebtRegistrationOnly_calculatesInterestToToday() {
    DebtEventEntity registration =
        buildEvent(
            CROSSING_POINT, DebtEventEntity.EventType.DEBT_REGISTERED, new BigDecimal("50000"));

    when(debtEventRepository.findByDebtIdOrderByEffectiveDateAscCreatedAtAsc(DEBT_ID))
        .thenReturn(List.of(registration));
    when(ledgerEntryRepository.findActiveEntriesByDebtId(DEBT_ID)).thenReturn(List.of());
    when(ledgerEntryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

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

    DebtEventEntity registration =
        buildEvent(regDate, DebtEventEntity.EventType.DEBT_REGISTERED, new BigDecimal("50000"));
    DebtEventEntity payment =
        buildEvent(payDate, DebtEventEntity.EventType.PAYMENT_RECEIVED, new BigDecimal("10000"));

    when(debtEventRepository.findByDebtIdOrderByEffectiveDateAscCreatedAtAsc(DEBT_ID))
        .thenReturn(List.of(registration, payment));
    when(ledgerEntryRepository.findActiveEntriesByDebtId(DEBT_ID)).thenReturn(List.of());
    when(ledgerEntryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    CoverageAllocation allocation =
        CoverageAllocation.builder()
            .debtId(DEBT_ID)
            .totalAmount(new BigDecimal("10000"))
            .interestPortion(BigDecimal.ZERO)
            .principalPortion(new BigDecimal("10000"))
            .accruedInterestAtDate(BigDecimal.ZERO)
            .principalBalanceAtDate(new BigDecimal("50000"))
            .build();
    when(coveragePriorityService.allocatePayment(eq(DEBT_ID), any(), any(), any(), any()))
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

    DebtEventEntity registration =
        buildEvent(regDate, DebtEventEntity.EventType.DEBT_REGISTERED, new BigDecimal("100000"));
    DebtEventEntity payment =
        buildEvent(payDate, DebtEventEntity.EventType.PAYMENT_RECEIVED, new BigDecimal("20000"));

    when(debtEventRepository.findByDebtIdOrderByEffectiveDateAscCreatedAtAsc(DEBT_ID))
        .thenReturn(List.of(registration, payment));
    when(ledgerEntryRepository.findActiveEntriesByDebtId(DEBT_ID)).thenReturn(List.of());
    when(ledgerEntryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

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
    when(coveragePriorityService.allocatePayment(eq(DEBT_ID), any(), any(), any(), any()))
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
    LedgerEntryEntity existingDebit =
        LedgerEntryEntity.builder()
            .id(UUID.randomUUID())
            .transactionId(existingTxnId)
            .debtId(DEBT_ID)
            .accountCode("1100")
            .accountName("Renter tilgodehavende")
            .entryType(LedgerEntryEntity.EntryType.DEBIT)
            .amount(new BigDecimal("500"))
            .effectiveDate(CROSSING_POINT.plusDays(5))
            .postingDate(CROSSING_POINT.plusDays(10))
            .reference("OLD-REF")
            .entryCategory(LedgerEntryEntity.EntryCategory.INTEREST_ACCRUAL)
            .build();
    LedgerEntryEntity existingCredit =
        LedgerEntryEntity.builder()
            .id(UUID.randomUUID())
            .transactionId(existingTxnId)
            .debtId(DEBT_ID)
            .accountCode("3100")
            .accountName("Renteindtaegter")
            .entryType(LedgerEntryEntity.EntryType.CREDIT)
            .amount(new BigDecimal("500"))
            .effectiveDate(CROSSING_POINT.plusDays(5))
            .postingDate(CROSSING_POINT.plusDays(10))
            .reference("OLD-REF")
            .entryCategory(LedgerEntryEntity.EntryCategory.INTEREST_ACCRUAL)
            .build();

    DebtEventEntity registration =
        buildEvent(
            CROSSING_POINT, DebtEventEntity.EventType.DEBT_REGISTERED, new BigDecimal("50000"));

    when(ledgerEntryRepository.findActiveEntriesByDebtId(DEBT_ID))
        .thenReturn(List.of(existingDebit, existingCredit));
    when(ledgerEntryRepository.existsByReversalOfTransactionId(existingTxnId)).thenReturn(false);
    when(ledgerEntryRepository.findByTransactionId(existingTxnId))
        .thenReturn(List.of(existingDebit, existingCredit));
    when(debtEventRepository.findByDebtIdOrderByEffectiveDateAscCreatedAtAsc(DEBT_ID))
        .thenReturn(List.of(registration));
    when(ledgerEntryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    TimelineReplayResult result =
        service.replayTimeline(DEBT_ID, CROSSING_POINT, RATE, "CROSSING-003");

    assertThat(result.getStornoEntriesPosted()).isEqualTo(2);
  }

  @Test
  void replayTimeline_isIdempotent_producesConsistentResults() {
    DebtEventEntity registration =
        buildEvent(
            CROSSING_POINT, DebtEventEntity.EventType.DEBT_REGISTERED, new BigDecimal("50000"));

    when(debtEventRepository.findByDebtIdOrderByEffectiveDateAscCreatedAtAsc(DEBT_ID))
        .thenReturn(List.of(registration));
    when(ledgerEntryRepository.findActiveEntriesByDebtId(DEBT_ID)).thenReturn(List.of());
    when(ledgerEntryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    TimelineReplayResult result1 = service.replayTimeline(DEBT_ID, CROSSING_POINT, RATE, "REF");
    TimelineReplayResult result2 = service.replayTimeline(DEBT_ID, CROSSING_POINT, RATE, "REF");

    assertThat(result1.getFinalPrincipalBalance())
        .isEqualByComparingTo(result2.getFinalPrincipalBalance());
    assertThat(result1.getNewInterestTotal()).isEqualByComparingTo(result2.getNewInterestTotal());
    assertThat(result1.getRecalculatedInterestPeriods().size())
        .isEqualTo(result2.getRecalculatedInterestPeriods().size());
  }

  private DebtEventEntity buildEvent(
      LocalDate effectiveDate, DebtEventEntity.EventType type, BigDecimal amount) {
    DebtEventEntity event = new DebtEventEntity();
    event.setId(UUID.randomUUID());
    event.setDebtId(DEBT_ID);
    event.setEffectiveDate(effectiveDate);
    event.setEventType(type);
    event.setAmount(amount);
    event.setReference("REF-" + type.name());
    event.setDescription("Test " + type.name());
    event.setCreatedAt(
        LocalDateTime.of(
            effectiveDate.getYear(),
            effectiveDate.getMonthValue(),
            effectiveDate.getDayOfMonth(),
            12,
            0));
    return event;
  }
}
