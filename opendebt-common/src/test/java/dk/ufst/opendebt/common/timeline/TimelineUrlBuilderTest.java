package dk.ufst.opendebt.common.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TimelineUrlBuilder}.
 *
 * <p>Covers URL construction for the "load more" button and filter chip removal links. Ref:
 * petition050 specs FIX-4, FIX-5.
 */
class TimelineUrlBuilderTest {

  private static final String BASE_URL = "http://localhost/cases/123/tidslinje";
  private static final String ENTRIES_URL = "http://localhost/cases/123/tidslinje/entries";
  private static final LocalDate FROM = LocalDate.of(2025, 1, 1);
  private static final LocalDate TO = LocalDate.of(2025, 12, 31);
  private static final UUID DEBT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

  // ---------------------------------------------------------------------------
  // buildLoadMoreUrl
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("buildLoadMoreUrl")
  class BuildLoadMoreUrl {

    @Test
    @DisplayName("no filters: URL contains only page and size params")
    void noFilters_containsOnlyPageAndSize() {
      TimelineFilterDto filter = TimelineFilterDto.builder().build();
      String url = TimelineUrlBuilder.buildLoadMoreUrl(filter, ENTRIES_URL, 1, 25);
      assertThat(url).isEqualTo(ENTRIES_URL + "?page=2&size=25");
    }

    @Test
    @DisplayName("page incremented by 1")
    void pageIsIncrementedByOne() {
      TimelineFilterDto filter = TimelineFilterDto.builder().build();
      String url = TimelineUrlBuilder.buildLoadMoreUrl(filter, ENTRIES_URL, 3, 10);
      assertThat(url).contains("page=4");
    }

    @Test
    @DisplayName("active eventCategory is appended")
    void activeEventCategory_isAppended() {
      TimelineFilterDto filter =
          TimelineFilterDto.builder().eventCategories(Set.of(EventCategory.CASE)).build();
      String url = TimelineUrlBuilder.buildLoadMoreUrl(filter, ENTRIES_URL, 1, 25);
      assertThat(url).contains("&eventCategory=CASE");
    }

    @Test
    @DisplayName("active fromDate is appended")
    void activeFromDate_isAppended() {
      TimelineFilterDto filter = TimelineFilterDto.builder().fromDate(FROM).build();
      String url = TimelineUrlBuilder.buildLoadMoreUrl(filter, ENTRIES_URL, 1, 25);
      assertThat(url).contains("&fromDate=2025-01-01");
    }

    @Test
    @DisplayName("active toDate is appended")
    void activeToDate_isAppended() {
      TimelineFilterDto filter = TimelineFilterDto.builder().toDate(TO).build();
      String url = TimelineUrlBuilder.buildLoadMoreUrl(filter, ENTRIES_URL, 1, 25);
      assertThat(url).contains("&toDate=2025-12-31");
    }

    @Test
    @DisplayName("active debtId is appended")
    void activeDebtId_isAppended() {
      TimelineFilterDto filter = TimelineFilterDto.builder().debtId(DEBT_ID).build();
      String url = TimelineUrlBuilder.buildLoadMoreUrl(filter, ENTRIES_URL, 1, 25);
      assertThat(url).contains("&debtId=" + DEBT_ID);
    }

    @Test
    @DisplayName("all active filters are all appended")
    void allFilters_allAppended() {
      TimelineFilterDto filter =
          TimelineFilterDto.builder()
              .eventCategories(Set.of(EventCategory.FINANCIAL))
              .fromDate(FROM)
              .toDate(TO)
              .debtId(DEBT_ID)
              .build();
      String url = TimelineUrlBuilder.buildLoadMoreUrl(filter, ENTRIES_URL, 1, 25);
      assertThat(url)
          .contains("eventCategory=FINANCIAL")
          .contains("fromDate=2025-01-01")
          .contains("toDate=2025-12-31")
          .contains("debtId=" + DEBT_ID);
    }
  }

  // ---------------------------------------------------------------------------
  // buildFilterRemoveLinks
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("buildFilterRemoveLinks")
  class BuildFilterRemoveLinks {

    @Test
    @DisplayName("no active filters: empty map returned")
    void noActiveFilters_emptyMap() {
      TimelineFilterDto filter = TimelineFilterDto.builder().build();
      Map<String, String> links = TimelineUrlBuilder.buildFilterRemoveLinks(filter, BASE_URL);
      assertThat(links).isEmpty();
    }

    @Test
    @DisplayName("single active category: map has that category's key")
    void singleActiveCategory_mapHasCategoryKey() {
      TimelineFilterDto filter =
          TimelineFilterDto.builder().eventCategories(Set.of(EventCategory.CASE)).build();
      Map<String, String> links = TimelineUrlBuilder.buildFilterRemoveLinks(filter, BASE_URL);
      assertThat(links).containsKey("CASE");
      assertThat(links.get("CASE")).doesNotContain("eventCategory=");
    }

    @Test
    @DisplayName("two active categories: removing one keeps the other")
    void twoActiveCategories_removingOneKeepsOther() {
      TimelineFilterDto filter =
          TimelineFilterDto.builder()
              .eventCategories(Set.of(EventCategory.CASE, EventCategory.FINANCIAL))
              .build();
      Map<String, String> links = TimelineUrlBuilder.buildFilterRemoveLinks(filter, BASE_URL);
      assertThat(links).containsKeys("CASE", "FINANCIAL");
      assertThat(links.get("CASE")).contains("eventCategory=FINANCIAL");
      assertThat(links.get("FINANCIAL")).contains("eventCategory=CASE");
    }

    @Test
    @DisplayName("active fromDate: map has 'fromDate' key without fromDate param")
    void activeFromDate_mapHasFromDateKey() {
      TimelineFilterDto filter = TimelineFilterDto.builder().fromDate(FROM).build();
      Map<String, String> links = TimelineUrlBuilder.buildFilterRemoveLinks(filter, BASE_URL);
      assertThat(links).containsKey("fromDate");
      assertThat(links.get("fromDate")).doesNotContain("fromDate=");
    }

    @Test
    @DisplayName("active toDate: map has 'toDate' key without toDate param")
    void activeToDate_mapHasToDateKey() {
      TimelineFilterDto filter = TimelineFilterDto.builder().toDate(TO).build();
      Map<String, String> links = TimelineUrlBuilder.buildFilterRemoveLinks(filter, BASE_URL);
      assertThat(links).containsKey("toDate");
      assertThat(links.get("toDate")).doesNotContain("toDate=");
    }

    @Test
    @DisplayName("active debtId: map has 'debtId' key without debtId param")
    void activeDebtId_mapHasDebtIdKey() {
      TimelineFilterDto filter = TimelineFilterDto.builder().debtId(DEBT_ID).build();
      Map<String, String> links = TimelineUrlBuilder.buildFilterRemoveLinks(filter, BASE_URL);
      assertThat(links).containsKey("debtId");
      assertThat(links.get("debtId")).doesNotContain("debtId=");
    }

    @Test
    @DisplayName("removing fromDate preserves toDate and debtId")
    void removingFromDate_preservesToDateAndDebtId() {
      TimelineFilterDto filter =
          TimelineFilterDto.builder().fromDate(FROM).toDate(TO).debtId(DEBT_ID).build();
      Map<String, String> links = TimelineUrlBuilder.buildFilterRemoveLinks(filter, BASE_URL);
      String fromDateRemovalUrl = links.get("fromDate");
      assertThat(fromDateRemovalUrl)
          .doesNotContain("fromDate=")
          .contains("toDate=2025-12-31")
          .contains("debtId=" + DEBT_ID);
    }

    @Test
    @DisplayName("removing toDate preserves fromDate and debtId")
    void removingToDate_preservesFromDateAndDebtId() {
      TimelineFilterDto filter =
          TimelineFilterDto.builder().fromDate(FROM).toDate(TO).debtId(DEBT_ID).build();
      Map<String, String> links = TimelineUrlBuilder.buildFilterRemoveLinks(filter, BASE_URL);
      String toDateRemovalUrl = links.get("toDate");
      assertThat(toDateRemovalUrl)
          .doesNotContain("toDate=")
          .contains("fromDate=2025-01-01")
          .contains("debtId=" + DEBT_ID);
    }

    @Test
    @DisplayName("removing debtId preserves fromDate and toDate")
    void removingDebtId_preservesFromDateAndToDate() {
      TimelineFilterDto filter =
          TimelineFilterDto.builder().fromDate(FROM).toDate(TO).debtId(DEBT_ID).build();
      Map<String, String> links = TimelineUrlBuilder.buildFilterRemoveLinks(filter, BASE_URL);
      String debtIdRemovalUrl = links.get("debtId");
      assertThat(debtIdRemovalUrl)
          .doesNotContain("debtId=")
          .contains("fromDate=2025-01-01")
          .contains("toDate=2025-12-31");
    }

    @Test
    @DisplayName("category removal URL starts with ? when it is the first param")
    void categoryRemoval_startsWithQuestionMarkWhenFirstParam() {
      TimelineFilterDto filter =
          TimelineFilterDto.builder()
              .eventCategories(Set.of(EventCategory.CASE, EventCategory.FINANCIAL))
              .build();
      Map<String, String> links = TimelineUrlBuilder.buildFilterRemoveLinks(filter, BASE_URL);
      // Removing one category, the surviving category must use ? prefix
      String caseRemoval = links.get("CASE");
      String financialRemoval = links.get("FINANCIAL");
      // One of them retains the other as the only param — it should start with ?
      assertThat(caseRemoval + financialRemoval).containsPattern("\\?eventCategory=");
    }
  }
}
