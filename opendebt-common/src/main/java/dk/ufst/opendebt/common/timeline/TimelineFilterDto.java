package dk.ufst.opendebt.common.timeline;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Carries active filter state for the timeline BFF; also passed to Thymeleaf for chip rendering.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimelineFilterDto {

  @Builder.Default private Set<EventCategory> eventCategories = new HashSet<>();
  private LocalDate fromDate;
  private LocalDate toDate;
  private UUID debtId;

  /** Returns true if any filter is active (at least one field is set). */
  public boolean hasActiveFilters() {
    return !eventCategories.isEmpty() || fromDate != null || toDate != null || debtId != null;
  }
}
