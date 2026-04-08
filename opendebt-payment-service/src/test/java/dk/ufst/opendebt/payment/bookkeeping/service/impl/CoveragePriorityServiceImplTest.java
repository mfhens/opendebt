package dk.ufst.opendebt.payment.bookkeeping.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dk.ufst.bookkeeping.model.CoverageAllocation;

class CoveragePriorityServiceImplTest {

  private CoveragePriorityServiceImpl service;
  private static final UUID DEBT_ID = UUID.randomUUID();
  private static final BigDecimal NO_FEES = BigDecimal.ZERO;

  @BeforeEach
  void setUp() {
    service = new CoveragePriorityServiceImpl();
  }

  @Test
  void paymentSmallerThanInterest_allGoesToInterest() {
    CoverageAllocation result =
        service.allocatePayment(
            DEBT_ID,
            new BigDecimal("100"),
            new BigDecimal("500"),
            NO_FEES,
            new BigDecimal("50000"));

    assertThat(result.getInterestPortion()).isEqualByComparingTo("100");
    assertThat(result.getFeesPortion()).isEqualByComparingTo("0");
    assertThat(result.getPrincipalPortion()).isEqualByComparingTo("0");
  }

  @Test
  void paymentEqualToInterest_allGoesToInterest() {
    CoverageAllocation result =
        service.allocatePayment(
            DEBT_ID,
            new BigDecimal("500"),
            new BigDecimal("500"),
            NO_FEES,
            new BigDecimal("50000"));

    assertThat(result.getInterestPortion()).isEqualByComparingTo("500");
    assertThat(result.getFeesPortion()).isEqualByComparingTo("0");
    assertThat(result.getPrincipalPortion()).isEqualByComparingTo("0");
  }

  @Test
  void paymentExceedsInterest_remainderGoesToPrincipal() {
    CoverageAllocation result =
        service.allocatePayment(
            DEBT_ID,
            new BigDecimal("1500"),
            new BigDecimal("500"),
            NO_FEES,
            new BigDecimal("50000"));

    assertThat(result.getInterestPortion()).isEqualByComparingTo("500");
    assertThat(result.getFeesPortion()).isEqualByComparingTo("0");
    assertThat(result.getPrincipalPortion()).isEqualByComparingTo("1000");
  }

  @Test
  void paymentExceedsInterestAndPrincipal_principalCappedAtBalance() {
    CoverageAllocation result =
        service.allocatePayment(
            DEBT_ID,
            new BigDecimal("60000"),
            new BigDecimal("500"),
            NO_FEES,
            new BigDecimal("50000"));

    assertThat(result.getInterestPortion()).isEqualByComparingTo("500");
    assertThat(result.getFeesPortion()).isEqualByComparingTo("0");
    assertThat(result.getPrincipalPortion()).isEqualByComparingTo("50000");
  }

  @Test
  void zeroInterest_allGoesToPrincipal() {
    CoverageAllocation result =
        service.allocatePayment(
            DEBT_ID, new BigDecimal("10000"), BigDecimal.ZERO, NO_FEES, new BigDecimal("50000"));

    assertThat(result.getInterestPortion()).isEqualByComparingTo("0");
    assertThat(result.getFeesPortion()).isEqualByComparingTo("0");
    assertThat(result.getPrincipalPortion()).isEqualByComparingTo("10000");
  }

  @Test
  void zeroPayment_nothingAllocated() {
    CoverageAllocation result =
        service.allocatePayment(
            DEBT_ID, BigDecimal.ZERO, new BigDecimal("500"), NO_FEES, new BigDecimal("50000"));

    assertThat(result.getInterestPortion()).isEqualByComparingTo("0");
    assertThat(result.getFeesPortion()).isEqualByComparingTo("0");
    assertThat(result.getPrincipalPortion()).isEqualByComparingTo("0");
  }

  @Test
  void preservesDebtIdAndTotalAmount() {
    CoverageAllocation result =
        service.allocatePayment(
            DEBT_ID,
            new BigDecimal("2000"),
            new BigDecimal("300"),
            NO_FEES,
            new BigDecimal("50000"));

    assertThat(result.getDebtId()).isEqualTo(DEBT_ID);
    assertThat(result.getTotalAmount()).isEqualByComparingTo("2000");
    assertThat(result.getAccruedInterestAtDate()).isEqualByComparingTo("300");
    assertThat(result.getPrincipalBalanceAtDate()).isEqualByComparingTo("50000");
  }

  @Test
  void threeWaySplit_interestThenFeeThenPrincipal() {
    CoverageAllocation result =
        service.allocatePayment(
            DEBT_ID,
            new BigDecimal("2000"),
            new BigDecimal("500"),
            new BigDecimal("300"),
            new BigDecimal("50000"));

    assertThat(result.getInterestPortion()).isEqualByComparingTo("500");
    assertThat(result.getFeesPortion()).isEqualByComparingTo("300");
    assertThat(result.getPrincipalPortion()).isEqualByComparingTo("1200");
  }

  @Test
  void paymentCoversInterestAndPartialFees() {
    CoverageAllocation result =
        service.allocatePayment(
            DEBT_ID,
            new BigDecimal("700"),
            new BigDecimal("500"),
            new BigDecimal("300"),
            new BigDecimal("50000"));

    assertThat(result.getInterestPortion()).isEqualByComparingTo("500");
    assertThat(result.getFeesPortion()).isEqualByComparingTo("200");
    assertThat(result.getPrincipalPortion()).isEqualByComparingTo("0");
  }

  @Test
  void feesTrackedInAllocation() {
    CoverageAllocation result =
        service.allocatePayment(
            DEBT_ID,
            new BigDecimal("1000"),
            BigDecimal.ZERO,
            new BigDecimal("65"),
            new BigDecimal("50000"));

    assertThat(result.getInterestPortion()).isEqualByComparingTo("0");
    assertThat(result.getFeesPortion()).isEqualByComparingTo("65");
    assertThat(result.getPrincipalPortion()).isEqualByComparingTo("935");
    assertThat(result.getOutstandingFeesAtDate()).isEqualByComparingTo("65");
  }
}
