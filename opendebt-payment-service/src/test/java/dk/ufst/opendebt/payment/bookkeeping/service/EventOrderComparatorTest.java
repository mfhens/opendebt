package dk.ufst.opendebt.payment.bookkeeping.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dk.ufst.opendebt.payment.bookkeeping.entity.DebtEventEntity;

class EventOrderComparatorTest {

  private static final UUID DEBT_ID = UUID.randomUUID();

  @Test
  void sortsByEffectiveDateFirst() {
    DebtEventEntity early =
        buildEvent(LocalDate.of(2026, 1, 1), DebtEventEntity.EventType.PAYMENT_RECEIVED);
    DebtEventEntity late =
        buildEvent(LocalDate.of(2026, 1, 5), DebtEventEntity.EventType.DEBT_REGISTERED);

    List<DebtEventEntity> sorted = Arrays.asList(late, early);
    sorted.sort(EventOrderComparator.INSTANCE);

    assertThat(sorted.get(0).getEffectiveDate()).isEqualTo(LocalDate.of(2026, 1, 1));
    assertThat(sorted.get(1).getEffectiveDate()).isEqualTo(LocalDate.of(2026, 1, 5));
  }

  @Test
  void sameDateSortsByEventTypePriority() {
    LocalDate date = LocalDate.of(2026, 3, 1);
    DebtEventEntity interest = buildEvent(date, DebtEventEntity.EventType.INTEREST_ACCRUED);
    DebtEventEntity payment = buildEvent(date, DebtEventEntity.EventType.PAYMENT_RECEIVED);
    DebtEventEntity registration = buildEvent(date, DebtEventEntity.EventType.DEBT_REGISTERED);

    List<DebtEventEntity> sorted = Arrays.asList(interest, payment, registration);
    sorted.sort(EventOrderComparator.INSTANCE);

    assertThat(sorted.get(0).getEventType()).isEqualTo(DebtEventEntity.EventType.DEBT_REGISTERED);
    assertThat(sorted.get(1).getEventType()).isEqualTo(DebtEventEntity.EventType.PAYMENT_RECEIVED);
    assertThat(sorted.get(2).getEventType()).isEqualTo(DebtEventEntity.EventType.INTEREST_ACCRUED);
  }

  @Test
  void sameTypeAndDateSortsByCreatedAt() {
    LocalDate date = LocalDate.of(2026, 3, 1);
    DebtEventEntity first = buildEvent(date, DebtEventEntity.EventType.PAYMENT_RECEIVED);
    first.setCreatedAt(LocalDateTime.of(2026, 3, 2, 10, 0));
    DebtEventEntity second = buildEvent(date, DebtEventEntity.EventType.PAYMENT_RECEIVED);
    second.setCreatedAt(LocalDateTime.of(2026, 3, 2, 14, 0));

    List<DebtEventEntity> sorted = Arrays.asList(second, first);
    sorted.sort(EventOrderComparator.INSTANCE);

    assertThat(sorted.get(0).getCreatedAt()).isBefore(sorted.get(1).getCreatedAt());
  }

  @Test
  void registrationBeforePaymentBeforeInterest() {
    LocalDate date = LocalDate.of(2026, 6, 15);
    DebtEventEntity reg = buildEvent(date, DebtEventEntity.EventType.DEBT_REGISTERED);
    DebtEventEntity pay = buildEvent(date, DebtEventEntity.EventType.PAYMENT_RECEIVED);
    DebtEventEntity intr = buildEvent(date, DebtEventEntity.EventType.INTEREST_ACCRUED);
    DebtEventEntity corr = buildEvent(date, DebtEventEntity.EventType.CORRECTION);

    List<DebtEventEntity> sorted = Arrays.asList(intr, corr, pay, reg);
    sorted.sort(EventOrderComparator.INSTANCE);

    assertThat(sorted.get(0).getEventType()).isEqualTo(DebtEventEntity.EventType.DEBT_REGISTERED);
    assertThat(sorted.get(1).getEventType()).isEqualTo(DebtEventEntity.EventType.PAYMENT_RECEIVED);
    assertThat(sorted.get(2).getEventType()).isEqualTo(DebtEventEntity.EventType.CORRECTION);
    assertThat(sorted.get(3).getEventType()).isEqualTo(DebtEventEntity.EventType.INTEREST_ACCRUED);
  }

  private DebtEventEntity buildEvent(LocalDate effectiveDate, DebtEventEntity.EventType type) {
    DebtEventEntity event = new DebtEventEntity();
    event.setId(UUID.randomUUID());
    event.setDebtId(DEBT_ID);
    event.setEffectiveDate(effectiveDate);
    event.setEventType(type);
    event.setAmount(BigDecimal.ONE);
    event.setCreatedAt(LocalDateTime.now());
    return event;
  }
}
