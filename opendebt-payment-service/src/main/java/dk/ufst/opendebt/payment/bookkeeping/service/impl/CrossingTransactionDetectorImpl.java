package dk.ufst.opendebt.payment.bookkeeping.service.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import dk.ufst.opendebt.payment.bookkeeping.entity.DebtEventEntity;
import dk.ufst.opendebt.payment.bookkeeping.model.CrossingDetectionResult;
import dk.ufst.opendebt.payment.bookkeeping.repository.DebtEventRepository;
import dk.ufst.opendebt.payment.bookkeeping.service.CrossingTransactionDetector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrossingTransactionDetectorImpl implements CrossingTransactionDetector {

  private final DebtEventRepository debtEventRepository;

  @Value("${opendebt.bookkeeping.crossing.max-lookback-months:12}")
  private int maxLookbackMonths;

  @Override
  public CrossingDetectionResult detectCrossing(UUID debtId, LocalDate newEventEffectiveDate) {
    LocalDate earliestAllowed = LocalDate.now().minusMonths(maxLookbackMonths);
    LocalDate crossingPoint =
        newEventEffectiveDate.isBefore(earliestAllowed) ? earliestAllowed : newEventEffectiveDate;

    List<DebtEventEntity> eventsAfterCrossingPoint =
        debtEventRepository.findEventsFromDate(debtId, crossingPoint);

    List<DebtEventEntity> affectedEvents =
        eventsAfterCrossingPoint.stream()
            .filter(
                e ->
                    e.getEffectiveDate().isAfter(newEventEffectiveDate)
                        || (e.getEffectiveDate().isEqual(newEventEffectiveDate)
                            && e.getCreatedAt() != null
                            && e.getCreatedAt().toLocalDate().isAfter(newEventEffectiveDate)))
            .toList();

    boolean crossing = !affectedEvents.isEmpty();

    if (crossing) {
      log.info(
          "CROSSING DETECTED: debtId={}, newEventDate={}, affectedEvents={}, crossingPoint={}",
          debtId,
          newEventEffectiveDate,
          affectedEvents.size(),
          crossingPoint);
    }

    return CrossingDetectionResult.builder()
        .crossingDetected(crossing)
        .debtId(debtId)
        .crossingPoint(crossingPoint)
        .affectedEvents(affectedEvents)
        .build();
  }
}
