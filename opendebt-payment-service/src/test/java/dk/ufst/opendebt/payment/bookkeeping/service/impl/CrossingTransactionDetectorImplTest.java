package dk.ufst.opendebt.payment.bookkeeping.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import dk.ufst.opendebt.payment.bookkeeping.entity.DebtEventEntity;
import dk.ufst.opendebt.payment.bookkeeping.model.CrossingDetectionResult;
import dk.ufst.opendebt.payment.bookkeeping.repository.DebtEventRepository;

@ExtendWith(MockitoExtension.class)
class CrossingTransactionDetectorImplTest {

  @Mock private DebtEventRepository debtEventRepository;

  private CrossingTransactionDetectorImpl detector;

  private static final UUID DEBT_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    detector = new CrossingTransactionDetectorImpl(debtEventRepository);
    ReflectionTestUtils.setField(detector, "maxLookbackMonths", 12);
  }

  @Test
  void noCrossing_whenNoEventsExistAfterDate() {
    when(debtEventRepository.findEventsFromDate(eq(DEBT_ID), any()))
        .thenReturn(Collections.emptyList());

    CrossingDetectionResult result = detector.detectCrossing(DEBT_ID, LocalDate.of(2026, 3, 1));

    assertThat(result.isCrossingDetected()).isFalse();
    assertThat(result.getAffectedEvents()).isEmpty();
  }

  @Test
  void crossingDetected_whenEventsExistAfterNewEventDate() {
    LocalDate newEventDate = LocalDate.of(2026, 3, 1);
    DebtEventEntity laterEvent =
        buildEvent(LocalDate.of(2026, 3, 5), DebtEventEntity.EventType.INTEREST_ACCRUED);

    when(debtEventRepository.findEventsFromDate(eq(DEBT_ID), any()))
        .thenReturn(List.of(laterEvent));

    CrossingDetectionResult result = detector.detectCrossing(DEBT_ID, newEventDate);

    assertThat(result.isCrossingDetected()).isTrue();
    assertThat(result.getAffectedEvents()).hasSize(1);
    assertThat(result.getDebtId()).isEqualTo(DEBT_ID);
  }

  @Test
  void noCrossing_whenEventsAreOnOrBeforeNewEventDate() {
    LocalDate newEventDate = LocalDate.of(2026, 3, 5);
    DebtEventEntity earlierEvent =
        buildEvent(LocalDate.of(2026, 3, 1), DebtEventEntity.EventType.DEBT_REGISTERED);

    when(debtEventRepository.findEventsFromDate(eq(DEBT_ID), any()))
        .thenReturn(List.of(earlierEvent));

    CrossingDetectionResult result = detector.detectCrossing(DEBT_ID, newEventDate);

    assertThat(result.isCrossingDetected()).isFalse();
  }

  @Test
  void crossingDetected_multipleAffectedEvents() {
    LocalDate newEventDate = LocalDate.of(2026, 3, 1);
    DebtEventEntity event1 =
        buildEvent(LocalDate.of(2026, 3, 3), DebtEventEntity.EventType.INTEREST_ACCRUED);
    DebtEventEntity event2 =
        buildEvent(LocalDate.of(2026, 3, 5), DebtEventEntity.EventType.PAYMENT_RECEIVED);

    when(debtEventRepository.findEventsFromDate(eq(DEBT_ID), any()))
        .thenReturn(List.of(event1, event2));

    CrossingDetectionResult result = detector.detectCrossing(DEBT_ID, newEventDate);

    assertThat(result.isCrossingDetected()).isTrue();
    assertThat(result.getAffectedEvents()).hasSize(2);
  }

  private DebtEventEntity buildEvent(LocalDate effectiveDate, DebtEventEntity.EventType type) {
    DebtEventEntity event = new DebtEventEntity();
    event.setId(UUID.randomUUID());
    event.setDebtId(DEBT_ID);
    event.setEffectiveDate(effectiveDate);
    event.setEventType(type);
    event.setAmount(BigDecimal.ONE);
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
