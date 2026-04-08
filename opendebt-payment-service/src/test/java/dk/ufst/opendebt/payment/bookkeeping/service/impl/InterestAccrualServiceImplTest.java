package dk.ufst.opendebt.payment.bookkeeping.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dk.ufst.bookkeeping.domain.EventType;
import dk.ufst.bookkeeping.domain.FinancialEvent;
import dk.ufst.bookkeeping.model.InterestPeriod;
import dk.ufst.bookkeeping.port.FinancialEventStore;
import dk.ufst.bookkeeping.service.impl.InterestAccrualServiceImpl;

@ExtendWith(MockitoExtension.class)
class InterestAccrualServiceImplTest {

  @Mock private FinancialEventStore financialEventStore;

  private InterestAccrualServiceImpl service;

  private static final UUID DEBT_ID = UUID.randomUUID();
  private static final BigDecimal ANNUAL_RATE = new BigDecimal("0.10"); // 10%

  @BeforeEach
  void setUp() {
    service = new InterestAccrualServiceImpl(financialEventStore);
  }

  @Test
  void singleDebtRegistration_calculatesInterestForEntirePeriod() {
    LocalDate from = LocalDate.of(2025, 10, 1);
    LocalDate to = LocalDate.of(2026, 1, 1);
    BigDecimal principal = new BigDecimal("50000");

    FinancialEvent registration = buildEvent(EventType.DEBT_REGISTERED, from, principal);

    when(financialEventStore.findPrincipalAffectingEvents(DEBT_ID))
        .thenReturn(List.of(registration));

    List<InterestPeriod> periods =
        service.calculatePeriodicInterest(DEBT_ID, from, to, ANNUAL_RATE);

    assertThat(periods).hasSize(1);

    InterestPeriod period = periods.get(0);
    assertThat(period.getPrincipalBalance()).isEqualByComparingTo(principal);
    assertThat(period.getPeriodStart()).isEqualTo(from);
    assertThat(period.getPeriodEnd()).isEqualTo(to);

    long expectedDays = ChronoUnit.DAYS.between(from, to);
    assertThat(period.getDays()).isEqualTo(expectedDays);

    // Verify: 50000 * 0.10/365 * 92 days = ~1260.27
    BigDecimal expectedInterest =
        principal
            .multiply(ANNUAL_RATE)
            .divide(new BigDecimal("365"), 10, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(expectedDays))
            .setScale(2, RoundingMode.HALF_UP);

    assertThat(period.getInterestAmount()).isEqualByComparingTo(expectedInterest);
  }

  @Test
  void paymentMidPeriod_splitsPrincipalIntoTwoPeriods() {
    LocalDate from = LocalDate.of(2025, 10, 1);
    LocalDate to = LocalDate.of(2026, 1, 1);
    LocalDate paymentDate = LocalDate.of(2025, 11, 15);
    BigDecimal principal = new BigDecimal("50000");
    BigDecimal payment = new BigDecimal("20000");

    FinancialEvent registration = buildEvent(EventType.DEBT_REGISTERED, from, principal);
    FinancialEvent paymentEvent = buildEvent(EventType.PAYMENT_RECEIVED, paymentDate, payment);

    when(financialEventStore.findPrincipalAffectingEvents(DEBT_ID))
        .thenReturn(List.of(registration, paymentEvent));

    List<InterestPeriod> periods =
        service.calculatePeriodicInterest(DEBT_ID, from, to, ANNUAL_RATE);

    assertThat(periods).hasSize(2);

    // Period 1: full principal
    assertThat(periods.get(0).getPrincipalBalance()).isEqualByComparingTo("50000");
    assertThat(periods.get(0).getPeriodStart()).isEqualTo(from);
    assertThat(periods.get(0).getPeriodEnd()).isEqualTo(paymentDate);

    // Period 2: reduced principal
    assertThat(periods.get(1).getPrincipalBalance()).isEqualByComparingTo("30000");
    assertThat(periods.get(1).getPeriodStart()).isEqualTo(paymentDate);
    assertThat(periods.get(1).getPeriodEnd()).isEqualTo(to);
  }

  @Test
  void udlaegCorrection_adjustsPrincipalForSubsequentPeriods() {
    LocalDate from = LocalDate.of(2025, 10, 1);
    LocalDate to = LocalDate.of(2026, 3, 1);
    LocalDate correctionDate = LocalDate.of(2025, 12, 1);

    FinancialEvent registration =
        buildEvent(EventType.DEBT_REGISTERED, from, new BigDecimal("50000"));
    FinancialEvent correction =
        buildEvent(EventType.UDLAEG_CORRECTED, correctionDate, new BigDecimal("-20000"));

    when(financialEventStore.findPrincipalAffectingEvents(DEBT_ID))
        .thenReturn(List.of(registration, correction));

    List<InterestPeriod> periods =
        service.calculatePeriodicInterest(DEBT_ID, from, to, ANNUAL_RATE);

    assertThat(periods).hasSize(2);

    // Period 1: original principal
    assertThat(periods.get(0).getPrincipalBalance()).isEqualByComparingTo("50000");
    assertThat(periods.get(0).getPeriodEnd()).isEqualTo(correctionDate);

    // Period 2: corrected principal (50000 - 20000 = 30000)
    assertThat(periods.get(1).getPrincipalBalance()).isEqualByComparingTo("30000");
    assertThat(periods.get(1).getPeriodStart()).isEqualTo(correctionDate);
  }

  @Test
  void noEvents_returnsEmptyPeriods() {
    when(financialEventStore.findPrincipalAffectingEvents(DEBT_ID))
        .thenReturn(Collections.emptyList());

    List<InterestPeriod> periods =
        service.calculatePeriodicInterest(
            DEBT_ID, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), ANNUAL_RATE);

    assertThat(periods).isEmpty();
  }

  @Test
  void fullyPaidDebt_skipsZeroBalancePeriod() {
    LocalDate from = LocalDate.of(2025, 10, 1);
    LocalDate to = LocalDate.of(2026, 1, 1);
    LocalDate paymentDate = LocalDate.of(2025, 11, 1);

    FinancialEvent registration =
        buildEvent(EventType.DEBT_REGISTERED, from, new BigDecimal("10000"));
    FinancialEvent fullPayment =
        buildEvent(EventType.PAYMENT_RECEIVED, paymentDate, new BigDecimal("10000"));

    when(financialEventStore.findPrincipalAffectingEvents(DEBT_ID))
        .thenReturn(List.of(registration, fullPayment));

    List<InterestPeriod> periods =
        service.calculatePeriodicInterest(DEBT_ID, from, to, ANNUAL_RATE);

    // Only one period (before payment), zero-balance period is skipped
    assertThat(periods).hasSize(1);
    assertThat(periods.get(0).getPrincipalBalance()).isEqualByComparingTo("10000");
    assertThat(periods.get(0).getPeriodEnd()).isEqualTo(paymentDate);
  }

  @Test
  void multipleEvents_calculatesTotalInterestAcrossAllPeriods() {
    LocalDate from = LocalDate.of(2025, 10, 1);
    LocalDate to = LocalDate.of(2026, 1, 1);

    FinancialEvent registration =
        buildEvent(EventType.DEBT_REGISTERED, from, new BigDecimal("100000"));
    FinancialEvent payment1 =
        buildEvent(EventType.PAYMENT_RECEIVED, LocalDate.of(2025, 11, 1), new BigDecimal("30000"));
    FinancialEvent payment2 =
        buildEvent(EventType.PAYMENT_RECEIVED, LocalDate.of(2025, 12, 1), new BigDecimal("20000"));

    when(financialEventStore.findPrincipalAffectingEvents(DEBT_ID))
        .thenReturn(List.of(registration, payment1, payment2));

    List<InterestPeriod> periods =
        service.calculatePeriodicInterest(DEBT_ID, from, to, ANNUAL_RATE);

    assertThat(periods).hasSize(3);
    assertThat(periods.get(0).getPrincipalBalance()).isEqualByComparingTo("100000");
    assertThat(periods.get(1).getPrincipalBalance()).isEqualByComparingTo("70000");
    assertThat(periods.get(2).getPrincipalBalance()).isEqualByComparingTo("50000");

    BigDecimal totalInterest =
        periods.stream()
            .map(InterestPeriod::getInterestAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    assertThat(totalInterest).isGreaterThan(BigDecimal.ZERO);
  }

  private FinancialEvent buildEvent(EventType type, LocalDate effectiveDate, BigDecimal amount) {
    return FinancialEvent.builder()
        .id(UUID.randomUUID())
        .debtId(DEBT_ID)
        .eventType(type)
        .effectiveDate(effectiveDate)
        .amount(amount)
        .build();
  }
}
