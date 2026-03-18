package dk.ufst.opendebt.payment.bookkeeping.service;

import java.util.Comparator;
import java.util.Map;

import dk.ufst.opendebt.payment.bookkeeping.entity.DebtEventEntity;

/**
 * Deterministic ordering for debt events. Primary sort: effective date ascending. Secondary sort
 * (same effective date): event type priority. Tertiary sort: created_at ascending (posting order).
 *
 * <p>Priority order for same-date events: DEBT_REGISTERED first, then balance-reducing events
 * (payments, offsetting), then corrections, then interest accruals last.
 */
public final class EventOrderComparator implements Comparator<DebtEventEntity> {

  private static final Map<DebtEventEntity.EventType, Integer> TYPE_PRIORITY =
      Map.ofEntries(
          Map.entry(DebtEventEntity.EventType.DEBT_REGISTERED, 0),
          Map.entry(DebtEventEntity.EventType.UDLAEG_REGISTERED, 1),
          Map.entry(DebtEventEntity.EventType.PAYMENT_RECEIVED, 2),
          Map.entry(DebtEventEntity.EventType.OFFSETTING_EXECUTED, 3),
          Map.entry(DebtEventEntity.EventType.WRITE_OFF, 4),
          Map.entry(DebtEventEntity.EventType.REFUND, 5),
          Map.entry(DebtEventEntity.EventType.UDLAEG_CORRECTED, 6),
          Map.entry(DebtEventEntity.EventType.CORRECTION, 7),
          Map.entry(DebtEventEntity.EventType.COVERAGE_REVERSED, 8),
          Map.entry(DebtEventEntity.EventType.INTEREST_ACCRUED, 9));

  public static final EventOrderComparator INSTANCE = new EventOrderComparator();

  private EventOrderComparator() {}

  @Override
  public int compare(DebtEventEntity a, DebtEventEntity b) {
    int dateCompare = a.getEffectiveDate().compareTo(b.getEffectiveDate());
    if (dateCompare != 0) {
      return dateCompare;
    }

    int priorityA = TYPE_PRIORITY.getOrDefault(a.getEventType(), 99);
    int priorityB = TYPE_PRIORITY.getOrDefault(b.getEventType(), 99);
    int typeCompare = Integer.compare(priorityA, priorityB);
    if (typeCompare != 0) {
      return typeCompare;
    }

    if (a.getCreatedAt() != null && b.getCreatedAt() != null) {
      return a.getCreatedAt().compareTo(b.getCreatedAt());
    }
    return 0;
  }
}
