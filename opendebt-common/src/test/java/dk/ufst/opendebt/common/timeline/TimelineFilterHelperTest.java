package dk.ufst.opendebt.common.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TimelineFilterHelper}.
 *
 * <p>Covers buildFilter and matchesFilter logic extracted from the three portal timeline
 * controllers. Ref: petition050 specs §2.4, §3.5, §4.5, §5.5.
 */
class TimelineFilterHelperTest {

  private static final Set<EventCategory> ALL_CATEGORIES = Set.of(EventCategory.values());
  private static final UUID DEBT_ID = UUID.randomUUID();

  // ---------------------------------------------------------------------------
  // buildFilter
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("buildFilter")
  class BuildFilter {

    @Test
    @DisplayName("null eventCategory array produces empty category set")
    void nullEventCategory_producesEmptyCategorySet() {
      TimelineFilterDto filter =
          TimelineFilterHelper.buildFilter(null, null, null, null, ALL_CATEGORIES);
      assertThat(filter.getEventCategories()).isEmpty();
    }

    @Test
    @DisplayName("valid allowed category is included")
    void validAllowedCategory_isIncluded() {
      TimelineFilterDto filter =
          TimelineFilterHelper.buildFilter(new String[] {"CASE"}, null, null, null, ALL_CATEGORIES);
      assertThat(filter.getEventCategories()).containsExactly(EventCategory.CASE);
    }

    @Test
    @DisplayName("category not in allowed set is excluded")
    void categoryNotAllowed_isExcluded() {
      Set<EventCategory> allowed = Set.of(EventCategory.FINANCIAL);
      TimelineFilterDto filter =
          TimelineFilterHelper.buildFilter(new String[] {"CASE"}, null, null, null, allowed);
      assertThat(filter.getEventCategories()).isEmpty();
    }

    @Test
    @DisplayName("unknown category string is silently ignored")
    void unknownCategoryString_isSilentlyIgnored() {
      TimelineFilterDto filter =
          TimelineFilterHelper.buildFilter(
              new String[] {"NOT_A_REAL_CATEGORY"}, null, null, null, ALL_CATEGORIES);
      assertThat(filter.getEventCategories()).isEmpty();
    }

    @Test
    @DisplayName("date and debtId params are passed through unchanged")
    void dateAndDebtId_arePassedThrough() {
      LocalDate from = LocalDate.of(2025, 1, 1);
      LocalDate to = LocalDate.of(2025, 12, 31);
      UUID debtId = UUID.randomUUID();

      TimelineFilterDto filter =
          TimelineFilterHelper.buildFilter(null, from, to, debtId, ALL_CATEGORIES);

      assertThat(filter.getFromDate()).isEqualTo(from);
      assertThat(filter.getToDate()).isEqualTo(to);
      assertThat(filter.getDebtId()).isEqualTo(debtId);
    }

    @Test
    @DisplayName("multiple valid categories are all included")
    void multipleValidCategories_allIncluded() {
      TimelineFilterDto filter =
          TimelineFilterHelper.buildFilter(
              new String[] {"CASE", "FINANCIAL"}, null, null, null, ALL_CATEGORIES);
      assertThat(filter.getEventCategories())
          .containsExactlyInAnyOrder(EventCategory.CASE, EventCategory.FINANCIAL);
    }
  }

  // ---------------------------------------------------------------------------
  // matchesFilter
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("matchesFilter")
  class MatchesFilter {

    @Test
    @DisplayName("empty filter accepts any entry")
    void emptyFilter_acceptsAnyEntry() {
      TimelineFilterDto filter = TimelineFilterDto.builder().build();
      TimelineEntryDto entry = entryWithCategory(EventCategory.CASE);
      assertThat(TimelineFilterHelper.matchesFilter(entry, filter)).isTrue();
    }

    @Test
    @DisplayName("category filter: matching category passes")
    void categoryFilter_matchingCategory_passes() {
      TimelineFilterDto filter =
          TimelineFilterDto.builder().eventCategories(Set.of(EventCategory.CASE)).build();
      assertThat(TimelineFilterHelper.matchesFilter(entryWithCategory(EventCategory.CASE), filter))
          .isTrue();
    }

    @Test
    @DisplayName("category filter: non-matching category is rejected")
    void categoryFilter_nonMatchingCategory_isRejected() {
      TimelineFilterDto filter =
          TimelineFilterDto.builder().eventCategories(Set.of(EventCategory.FINANCIAL)).build();
      assertThat(TimelineFilterHelper.matchesFilter(entryWithCategory(EventCategory.CASE), filter))
          .isFalse();
    }

    @Test
    @DisplayName("fromDate filter: entry before fromDate is rejected")
    void fromDateFilter_entryBefore_isRejected() {
      TimelineFilterDto filter =
          TimelineFilterDto.builder().fromDate(LocalDate.of(2025, 6, 1)).build();
      TimelineEntryDto entry = entryWithTimestamp(LocalDateTime.of(2025, 5, 31, 12, 0));
      assertThat(TimelineFilterHelper.matchesFilter(entry, filter)).isFalse();
    }

    @Test
    @DisplayName("fromDate filter: entry on fromDate passes")
    void fromDateFilter_entryOnDate_passes() {
      TimelineFilterDto filter =
          TimelineFilterDto.builder().fromDate(LocalDate.of(2025, 6, 1)).build();
      TimelineEntryDto entry = entryWithTimestamp(LocalDateTime.of(2025, 6, 1, 0, 0));
      assertThat(TimelineFilterHelper.matchesFilter(entry, filter)).isTrue();
    }

    @Test
    @DisplayName("fromDate filter: null entry timestamp passes")
    void fromDateFilter_nullTimestamp_passes() {
      TimelineFilterDto filter =
          TimelineFilterDto.builder().fromDate(LocalDate.of(2025, 6, 1)).build();
      TimelineEntryDto entry = entryWithTimestamp(null);
      assertThat(TimelineFilterHelper.matchesFilter(entry, filter)).isTrue();
    }

    @Test
    @DisplayName("toDate filter: entry after toDate is rejected")
    void toDateFilter_entryAfter_isRejected() {
      TimelineFilterDto filter =
          TimelineFilterDto.builder().toDate(LocalDate.of(2025, 6, 1)).build();
      TimelineEntryDto entry = entryWithTimestamp(LocalDateTime.of(2025, 6, 2, 0, 0));
      assertThat(TimelineFilterHelper.matchesFilter(entry, filter)).isFalse();
    }

    @Test
    @DisplayName("toDate filter: entry on toDate passes")
    void toDateFilter_entryOnDate_passes() {
      TimelineFilterDto filter =
          TimelineFilterDto.builder().toDate(LocalDate.of(2025, 6, 1)).build();
      TimelineEntryDto entry = entryWithTimestamp(LocalDateTime.of(2025, 6, 1, 23, 59));
      assertThat(TimelineFilterHelper.matchesFilter(entry, filter)).isTrue();
    }

    @Test
    @DisplayName("toDate filter: null entry timestamp passes")
    void toDateFilter_nullTimestamp_passes() {
      TimelineFilterDto filter =
          TimelineFilterDto.builder().toDate(LocalDate.of(2025, 6, 1)).build();
      TimelineEntryDto entry = entryWithTimestamp(null);
      assertThat(TimelineFilterHelper.matchesFilter(entry, filter)).isTrue();
    }

    @Test
    @DisplayName("debtId filter: matching debtId passes")
    void debtIdFilter_matchingDebtId_passes() {
      TimelineFilterDto filter = TimelineFilterDto.builder().debtId(DEBT_ID).build();
      TimelineEntryDto entry = entryWithDebtId(DEBT_ID);
      assertThat(TimelineFilterHelper.matchesFilter(entry, filter)).isTrue();
    }

    @Test
    @DisplayName("debtId filter: different debtId is rejected")
    void debtIdFilter_differentDebtId_isRejected() {
      TimelineFilterDto filter = TimelineFilterDto.builder().debtId(DEBT_ID).build();
      TimelineEntryDto entry = entryWithDebtId(UUID.randomUUID());
      assertThat(TimelineFilterHelper.matchesFilter(entry, filter)).isFalse();
    }

    @Test
    @DisplayName("debtId filter: null entry debtId is rejected when filter is set")
    void debtIdFilter_nullEntryDebtId_isRejected() {
      TimelineFilterDto filter = TimelineFilterDto.builder().debtId(DEBT_ID).build();
      TimelineEntryDto entry = entryWithDebtId(null);
      assertThat(TimelineFilterHelper.matchesFilter(entry, filter)).isFalse();
    }

    @Test
    @DisplayName("all filters combined: entry matching all passes")
    void allFilters_entryMatchingAll_passes() {
      UUID debtId = UUID.randomUUID();
      TimelineFilterDto filter =
          TimelineFilterDto.builder()
              .eventCategories(Set.of(EventCategory.FINANCIAL))
              .fromDate(LocalDate.of(2025, 1, 1))
              .toDate(LocalDate.of(2025, 12, 31))
              .debtId(debtId)
              .build();
      TimelineEntryDto entry =
          TimelineEntryDto.builder()
              .eventCategory(EventCategory.FINANCIAL)
              .timestamp(LocalDateTime.of(2025, 6, 15, 10, 0))
              .debtId(debtId)
              .amount(BigDecimal.TEN)
              .build();
      assertThat(TimelineFilterHelper.matchesFilter(entry, filter)).isTrue();
    }
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private static TimelineEntryDto entryWithCategory(EventCategory category) {
    return TimelineEntryDto.builder()
        .eventCategory(category)
        .timestamp(LocalDateTime.now())
        .build();
  }

  private static TimelineEntryDto entryWithTimestamp(LocalDateTime timestamp) {
    return TimelineEntryDto.builder()
        .eventCategory(EventCategory.CASE)
        .timestamp(timestamp)
        .build();
  }

  private static TimelineEntryDto entryWithDebtId(UUID debtId) {
    return TimelineEntryDto.builder()
        .eventCategory(EventCategory.FINANCIAL)
        .timestamp(LocalDateTime.now())
        .debtId(debtId)
        .build();
  }
}
