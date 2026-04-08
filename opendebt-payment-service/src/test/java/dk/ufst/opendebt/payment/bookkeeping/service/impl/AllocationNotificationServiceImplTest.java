package dk.ufst.opendebt.payment.bookkeeping.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dk.ufst.bookkeeping.model.CoverageAllocation;
import dk.ufst.bookkeeping.model.CoverageReversal;
import dk.ufst.bookkeeping.model.TimelineReplayResult;
import dk.ufst.opendebt.payment.bookkeeping.model.AllocationNotification;

class AllocationNotificationServiceImplTest {

  private AllocationNotificationServiceImpl service;
  private static final UUID DEBT_ID = UUID.randomUUID();
  private static final UUID CREDITOR_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    service = new AllocationNotificationServiceImpl();
  }

  @Test
  void generatesNotificationWithAllocations() {
    CoverageAllocation allocation =
        CoverageAllocation.builder()
            .debtId(DEBT_ID)
            .effectiveDate(LocalDate.of(2026, 3, 5))
            .totalAmount(new BigDecimal("10000"))
            .interestPortion(new BigDecimal("500"))
            .principalPortion(new BigDecimal("9500"))
            .build();

    TimelineReplayResult replayResult =
        TimelineReplayResult.builder()
            .debtId(DEBT_ID)
            .replayFromDate(LocalDate.of(2026, 1, 1))
            .replayToDate(LocalDate.now())
            .recalculatedAllocations(List.of(allocation))
            .coverageReversals(List.of())
            .newInterestTotal(new BigDecimal("750"))
            .finalPrincipalBalance(new BigDecimal("40000"))
            .finalInterestBalance(new BigDecimal("250"))
            .build();

    AllocationNotification notification =
        service.generateFromReplayResult(replayResult, CREDITOR_ID);

    assertThat(notification.getDebtId()).isEqualTo(DEBT_ID);
    assertThat(notification.getCreditorOrgId()).isEqualTo(CREDITOR_ID);
    assertThat(notification.getAllocationLines()).hasSize(1);
    assertThat(notification.getReversalLines()).isEmpty();
    assertThat(notification.isHasCrossingReversals()).isFalse();
    assertThat(notification.getNewTotalBalance()).isEqualByComparingTo("40250");
  }

  @Test
  void generatesNotificationWithCrossingReversals() {
    CoverageAllocation original =
        CoverageAllocation.builder()
            .debtId(DEBT_ID)
            .totalAmount(new BigDecimal("10000"))
            .interestPortion(new BigDecimal("200"))
            .principalPortion(new BigDecimal("9800"))
            .build();

    CoverageAllocation replacement =
        CoverageAllocation.builder()
            .debtId(DEBT_ID)
            .effectiveDate(LocalDate.of(2026, 3, 5))
            .totalAmount(new BigDecimal("10000"))
            .interestPortion(new BigDecimal("500"))
            .principalPortion(new BigDecimal("9500"))
            .build();

    CoverageReversal reversal =
        CoverageReversal.builder()
            .debtId(DEBT_ID)
            .effectiveDate(LocalDate.of(2026, 3, 5))
            .originalAllocation(original)
            .replacementAllocation(replacement)
            .interestDelta(new BigDecimal("300"))
            .principalDelta(new BigDecimal("-300"))
            .reason("Krydsende betaling")
            .build();

    TimelineReplayResult replayResult =
        TimelineReplayResult.builder()
            .debtId(DEBT_ID)
            .replayFromDate(LocalDate.of(2026, 1, 1))
            .replayToDate(LocalDate.now())
            .recalculatedAllocations(List.of(replacement))
            .coverageReversals(List.of(reversal))
            .newInterestTotal(new BigDecimal("750"))
            .finalPrincipalBalance(new BigDecimal("40500"))
            .finalInterestBalance(new BigDecimal("250"))
            .build();

    AllocationNotification notification =
        service.generateFromReplayResult(replayResult, CREDITOR_ID);

    assertThat(notification.isHasCrossingReversals()).isTrue();
    assertThat(notification.getReversalLines()).hasSize(1);

    AllocationNotification.ReversalLine reversalLine = notification.getReversalLines().get(0);
    assertThat(reversalLine.getOriginalInterestPortion()).isEqualByComparingTo("200");
    assertThat(reversalLine.getCorrectedInterestPortion()).isEqualByComparingTo("500");
    assertThat(reversalLine.getReason()).contains("Krydsende");
  }

  @Test
  void emptyReplayResult_producesEmptyNotification() {
    TimelineReplayResult replayResult =
        TimelineReplayResult.builder()
            .debtId(DEBT_ID)
            .replayFromDate(LocalDate.of(2026, 1, 1))
            .replayToDate(LocalDate.now())
            .recalculatedAllocations(List.of())
            .coverageReversals(List.of())
            .newInterestTotal(BigDecimal.ZERO)
            .finalPrincipalBalance(new BigDecimal("50000"))
            .finalInterestBalance(BigDecimal.ZERO)
            .build();

    AllocationNotification notification =
        service.generateFromReplayResult(replayResult, CREDITOR_ID);

    assertThat(notification.getAllocationLines()).isEmpty();
    assertThat(notification.getReversalLines()).isEmpty();
    assertThat(notification.isHasCrossingReversals()).isFalse();
  }
}
