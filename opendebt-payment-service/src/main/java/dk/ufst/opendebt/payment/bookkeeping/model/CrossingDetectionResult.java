package dk.ufst.opendebt.payment.bookkeeping.model;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import dk.ufst.opendebt.payment.bookkeeping.entity.DebtEventEntity;

import lombok.*;

/**
 * Result of crossing transaction detection. If crossing is detected, contains the earliest crossing
 * point and the list of events that are affected.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrossingDetectionResult {

  private boolean crossingDetected;
  private UUID debtId;
  private LocalDate crossingPoint;
  private DebtEventEntity triggeringEvent;
  private List<DebtEventEntity> affectedEvents;
}
