package dk.ufst.opendebt.payment.bookkeeping.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dk.ufst.bookkeeping.domain.EventType;
import dk.ufst.bookkeeping.domain.FinancialEvent;
import dk.ufst.bookkeeping.service.EventOrderComparator;

class EventOrderComparatorTest {

  private static final UUID DEBT_ID = UUID.randomUUID();

  @Test
  void sortsByEffectiveDateFirst() {
    FinancialEvent early = buildEvent(LocalDate.of(2026, 1, 1), EventType.PAYMENT_RECEIVED);
    FinancialEvent late = buildEvent(LocalDate.of(2026, 1, 5), EventType.DEBT_REGISTERED);

    List<FinancialEvent> sorted = Arrays.asList(late, early);
    sorted.sort(EventOrderComparator.INSTANCE);

    assertThat(sorted.get(0).getEffectiveDate()).isEqualTo(LocalDate.of(2026, 1, 1));
    assertThat(sorted.get(1).getEffectiveDate()).isEqualTo(LocalDate.of(2026, 1, 5));
  }

  @Test
  void sameDateSortsByEventTypePriority() {
    LocalDate date = LocalDate.of(2026, 3, 1);
    FinancialEvent interest = buildEvent(date, EventType.INTEREST_ACCRUED);
    FinancialEvent payment = buildEvent(date, EventType.PAYMENT_RECEIVED);
    FinancialEvent registration = buildEvent(date, EventType.DEBT_REGISTERED);

    List<FinancialEvent> sorted = Arrays.asList(interest, payment, registration);
    sorted.sort(EventOrderComparator.INSTANCE);

    assertThat(sorted.get(0).getEventType()).isEqualTo(EventType.DEBT_REGISTERED);
    assertThat(sorted.get(1).getEventType()).isEqualTo(EventType.PAYMENT_RECEIVED);
    assertThat(sorted.get(2).getEventType()).isEqualTo(EventType.INTEREST_ACCRUED);
  }

  @Test
  void sameTypeAndDateSortsByCreatedAt() {
    LocalDate date = LocalDate.of(2026, 3, 1);
    FinancialEvent first = buildEvent(date, EventType.PAYMENT_RECEIVED);
    first.setCreatedAt(LocalDateTime.of(2026, 3, 2, 10, 0));
    FinancialEvent second = buildEvent(date, EventType.PAYMENT_RECEIVED);
    second.setCreatedAt(LocalDateTime.of(2026, 3, 2, 14, 0));

    List<FinancialEvent> sorted = Arrays.asList(second, first);
    sorted.sort(EventOrderComparator.INSTANCE);

    assertThat(sorted.get(0).getCreatedAt()).isBefore(sorted.get(1).getCreatedAt());
  }

  @Test
  void registrationBeforePaymentBeforeInterest() {
    LocalDate date = LocalDate.of(2026, 6, 15);
    FinancialEvent reg = buildEvent(date, EventType.DEBT_REGISTERED);
    FinancialEvent pay = buildEvent(date, EventType.PAYMENT_RECEIVED);
    FinancialEvent intr = buildEvent(date, EventType.INTEREST_ACCRUED);
    FinancialEvent corr = buildEvent(date, EventType.CORRECTION);

    List<FinancialEvent> sorted = Arrays.asList(intr, corr, pay, reg);
    sorted.sort(EventOrderComparator.INSTANCE);

    assertThat(sorted.get(0).getEventType()).isEqualTo(EventType.DEBT_REGISTERED);
    assertThat(sorted.get(1).getEventType()).isEqualTo(EventType.PAYMENT_RECEIVED);
    assertThat(sorted.get(2).getEventType()).isEqualTo(EventType.CORRECTION);
    assertThat(sorted.get(3).getEventType()).isEqualTo(EventType.INTEREST_ACCRUED);
  }

  private FinancialEvent buildEvent(LocalDate effectiveDate, EventType type) {
    return FinancialEvent.builder()
        .id(UUID.randomUUID())
        .debtId(DEBT_ID)
        .effectiveDate(effectiveDate)
        .eventType(type)
        .amount(BigDecimal.ONE)
        .createdAt(LocalDateTime.now())
        .build();
  }
}
