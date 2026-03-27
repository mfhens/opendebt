package dk.ufst.opendebt.common.timeline;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

/**
 * Shared filter-building and filter-matching utilities for the timeline BFF controllers.
 *
 * <p>Extracted from the caseworker, citizen, and creditor portal timeline controllers to eliminate
 * identical private method duplication across all three.
 */
@Slf4j
public final class TimelineFilterHelper {

  private TimelineFilterHelper() {}

  /**
   * Builds a {@link TimelineFilterDto} from raw request parameters, constrained to the role's
   * allowed event categories. Unknown category values are silently ignored.
   */
  public static TimelineFilterDto buildFilter(
      String[] eventCategory,
      LocalDate fromDate,
      LocalDate toDate,
      UUID debtId,
      Set<EventCategory> allowed) {

    Set<EventCategory> categories = new HashSet<>();
    if (eventCategory != null) {
      for (String cat : eventCategory) {
        try {
          EventCategory ec = EventCategory.valueOf(cat);
          if (allowed.contains(ec)) {
            categories.add(ec);
          }
        } catch (IllegalArgumentException e) {
          log.debug("Ignoring unknown eventCategory filter value: {}", cat);
        }
      }
    }
    return TimelineFilterDto.builder()
        .eventCategories(categories)
        .fromDate(fromDate)
        .toDate(toDate)
        .debtId(debtId)
        .build();
  }

  /**
   * Returns {@code true} when the entry satisfies all active filter conditions (AND-combined). Spec
   * §2.4.
   */
  public static boolean matchesFilter(TimelineEntryDto entry, TimelineFilterDto filter) {
    if (!filter.getEventCategories().isEmpty()
        && !filter.getEventCategories().contains(entry.getEventCategory())) {
      return false;
    }
    if (filter.getFromDate() != null
        && entry.getTimestamp() != null
        && entry.getTimestamp().toLocalDate().isBefore(filter.getFromDate())) {
      return false;
    }
    if (filter.getToDate() != null
        && entry.getTimestamp() != null
        && entry.getTimestamp().toLocalDate().isAfter(filter.getToDate())) {
      return false;
    }
    if (filter.getDebtId() != null && !filter.getDebtId().equals(entry.getDebtId())) {
      return false;
    }
    return true;
  }
}
