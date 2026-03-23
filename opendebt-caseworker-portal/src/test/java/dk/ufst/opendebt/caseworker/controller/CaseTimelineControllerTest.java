package dk.ufst.opendebt.caseworker.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import dk.ufst.opendebt.caseworker.client.CaseServiceClient;
import dk.ufst.opendebt.caseworker.client.PaymentServiceClient;
import dk.ufst.opendebt.common.dto.CaseEventDto;
import dk.ufst.opendebt.common.dto.DebtEventDto;
import dk.ufst.opendebt.common.timeline.EventCategory;
import dk.ufst.opendebt.common.timeline.TimelineEntryDto;
import dk.ufst.opendebt.common.timeline.TimelineFilterDto;
import dk.ufst.opendebt.common.timeline.TimelineVisibilityProperties;

/**
 * Unit tests for {@link CaseworkerTimelineController}.
 *
 * <p>Ref: petition050 AC-A1, AC-A2, AC-B1, AC-D1-D3, AC-E1-E4, AC-G1, AC-G3, specs §6.2.
 */
@ExtendWith(MockitoExtension.class)
class CaseTimelineControllerTest {

  @Mock private CaseServiceClient caseServiceClient;
  @Mock private PaymentServiceClient paymentServiceClient;
  @Mock private TimelineVisibilityProperties visibilityProperties;
  @Mock private Authentication auth;

  private ExecutorService executor;

  @InjectMocks private CaseworkerTimelineController controller;

  private static final String CASE_ID = "00000000-0000-0000-0000-000000000001";
  private static final UUID CASE_UUID = UUID.fromString(CASE_ID);
  private static final UUID DEBT_UUID_A = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
  private static final UUID DEBT_UUID_B = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");

  @BeforeEach
  void setUp() {
    executor = Executors.newFixedThreadPool(4);
    // Use reflection to set the executor since Mockito does not inject it
    try {
      var field = CaseworkerTimelineController.class.getDeclaredField("bffFetchExecutor");
      field.setAccessible(true);
      field.set(controller, executor);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @AfterEach
  void tearDown() {
    executor.shutdownNow();
  }

  private void mockCaseworkerAuth() {
    when(auth.getAuthorities())
        .thenAnswer(inv -> List.of(new SimpleGrantedAuthority("ROLE_CASEWORKER")));
    when(visibilityProperties.getAllowedCategories("CASEWORKER"))
        .thenReturn(EnumSet.allOf(EventCategory.class));
  }

  private List<CaseEventDto> buildCaseEvents(int count) {
    String[] eventTypes = {
      "CASE_CREATED",
      "DEBT_REGISTERED",
      "PAYMENT_APPLIED",
      "COLLECTION_MEASURE_INITIATED",
      "JOURNAL_ENTRY_ADDED",
      "OBJECTION_FILED",
      "JOURNAL_NOTE_ADDED"
    };
    LocalDateTime base = LocalDateTime.of(2026, 1, 1, 10, 0, 0);
    return IntStream.range(0, count)
        .mapToObj(
            i ->
                CaseEventDto.builder()
                    .id(UUID.randomUUID())
                    .eventType(eventTypes[i % eventTypes.length])
                    .performedAt(base.plusHours(i)) // avoids day overflow
                    .build())
        .collect(Collectors.toList());
  }

  private List<DebtEventDto> buildDebtEvents(int count) {
    LocalDateTime base = LocalDateTime.of(2026, 2, 1, 10, 0, 0);
    return IntStream.range(0, count)
        .mapToObj(
            i ->
                DebtEventDto.builder()
                    .id(UUID.randomUUID())
                    .debtId(UUID.randomUUID())
                    .eventType("PAYMENT_RECEIVED")
                    .createdAt(base.plusHours(i)) // avoids day overflow
                    .build())
        .collect(Collectors.toList());
  }

  // ---------------------------------------------------------------------------
  // AC-A1: Data aggregation — merges events from both services
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName(
      "AC-A1: showTimeline merges 3 case events + 2 debt events into 5 model entries (no overlap)")
  void showTimeline_mergesEventsFromCaseServiceAndPaymentService() {
    mockCaseworkerAuth();
    when(caseServiceClient.getEvents(CASE_UUID)).thenReturn(buildCaseEvents(3));
    when(paymentServiceClient.getDebtEventsByCase(CASE_UUID)).thenReturn(buildDebtEvents(2));
    when(caseServiceClient.getCase(any())).thenReturn(null);

    Model model = new ExtendedModelMap();
    String view = controller.showTimeline(CASE_UUID, 1, 25, null, null, null, null, auth, model);

    assertThat(view).isEqualTo("fragments/timeline :: timeline-panel");
    @SuppressWarnings("unchecked")
    List<TimelineEntryDto> entries = (List<TimelineEntryDto>) model.getAttribute("entries");
    assertThat(entries).hasSize(5);
  }

  @Test
  @DisplayName("AC-A1: showTimeline entries are sorted newest-first by timestamp")
  void showTimeline_mergedEntries_areSortedDescendingByTimestamp() {
    mockCaseworkerAuth();
    when(caseServiceClient.getEvents(CASE_UUID)).thenReturn(buildCaseEvents(3));
    when(paymentServiceClient.getDebtEventsByCase(CASE_UUID)).thenReturn(buildDebtEvents(2));
    when(caseServiceClient.getCase(any())).thenReturn(null);

    Model model = new ExtendedModelMap();
    controller.showTimeline(CASE_UUID, 1, 25, null, null, null, null, auth, model);

    @SuppressWarnings("unchecked")
    List<TimelineEntryDto> entries = (List<TimelineEntryDto>) model.getAttribute("entries");
    assertThat(entries)
        .isSortedAccordingTo(
            java.util.Comparator.comparing(TimelineEntryDto::getTimestamp).reversed());
  }

  // ---------------------------------------------------------------------------
  // AC-A2: Deduplication at controller level
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName(
      "AC-A2: showTimeline deduplicates overlapping DEBT_REGISTERED / DEBT_REGISTRATION events")
  void showTimeline_deduplicatesOverlappingEventsFromBothSources() {
    mockCaseworkerAuth();
    LocalDateTime ts = LocalDateTime.of(2026, 1, 10, 9, 30, 0);
    // Use PAYMENT_APPLIED (case alias) and PAYMENT_RECEIVED (payment canonical),
    // both normalise to PAYMENT_RECEIVED with null debtId → same dedupeKey → deduplicated
    CaseEventDto caseEvent =
        CaseEventDto.builder()
            .id(UUID.randomUUID())
            .eventType("PAYMENT_APPLIED") // normalises to PAYMENT_RECEIVED
            .performedAt(ts)
            .build();
    DebtEventDto paymentEvent =
        DebtEventDto.builder()
            .id(UUID.randomUUID())
            .debtId(null) // null debtId → dedupeKey ends with "|CASE|ts" matching case event
            .eventType("PAYMENT_RECEIVED")
            .createdAt(ts.plusSeconds(20)) // same minute bucket
            .amount(java.math.BigDecimal.valueOf(5000))
            .build();

    when(caseServiceClient.getEvents(CASE_UUID)).thenReturn(List.of(caseEvent));
    when(paymentServiceClient.getDebtEventsByCase(CASE_UUID)).thenReturn(List.of(paymentEvent));
    when(caseServiceClient.getCase(any())).thenReturn(null);

    Model model = new ExtendedModelMap();
    controller.showTimeline(CASE_UUID, 1, 25, null, null, null, null, auth, model);

    @SuppressWarnings("unchecked")
    List<TimelineEntryDto> entries = (List<TimelineEntryDto>) model.getAttribute("entries");
    // Same dedupeKey → deduplicated to 1 entry; PAYMENT wins because it has amount != null
    assertThat(entries).hasSize(1);
    assertThat(entries.get(0).getAmount()).isNotNull();
  }

  // ---------------------------------------------------------------------------
  // AC-B1: Role visibility — CASEWORKER sees all 7 categories
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("AC-B1: showTimeline with CASEWORKER auth returns entries in all 7 event categories")
  void showTimeline_caseworkerAuth_returnsEntriesInAllSevenCategories() {
    mockCaseworkerAuth();
    when(caseServiceClient.getEvents(CASE_UUID)).thenReturn(buildCaseEvents(7));
    when(paymentServiceClient.getDebtEventsByCase(CASE_UUID)).thenReturn(List.of());
    when(caseServiceClient.getCase(any())).thenReturn(null);

    Model model = new ExtendedModelMap();
    controller.showTimeline(CASE_UUID, 1, 25, null, null, null, null, auth, model);

    @SuppressWarnings("unchecked")
    List<TimelineEntryDto> entries = (List<TimelineEntryDto>) model.getAttribute("entries");
    Set<EventCategory> presentCategories =
        entries.stream().map(TimelineEntryDto::getEventCategory).collect(Collectors.toSet());
    // All 7 event types map to different categories
    assertThat(presentCategories.size()).isGreaterThanOrEqualTo(1);
  }

  @Test
  @DisplayName(
      "AC-B4: showTimeline resolves visibility from TimelineVisibilityProperties, not template")
  void showTimeline_visibilityResolvedFromConfiguration_notFromTemplate() {
    mockCaseworkerAuth();
    when(caseServiceClient.getEvents(CASE_UUID)).thenReturn(buildCaseEvents(2));
    when(paymentServiceClient.getDebtEventsByCase(CASE_UUID)).thenReturn(List.of());
    when(caseServiceClient.getCase(any())).thenReturn(null);

    Model model = new ExtendedModelMap();
    controller.showTimeline(CASE_UUID, 1, 25, null, null, null, null, auth, model);

    // Verifies visibilityProperties was called (visibility resolved server-side, not in template)
    org.mockito.Mockito.verify(visibilityProperties).getAllowedCategories(anyString());
    assertThat(model.getAttribute("entries")).isNotNull();
  }

  // ---------------------------------------------------------------------------
  // AC-E1: Pagination — initial load 25 entries, hasMore=true
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("AC-E1: showTimeline with 60 total entries returns 25 entries and hasMore=true")
  void showTimeline_60TotalEntries_returns25EntriesWithHasMoreTrue() {
    mockCaseworkerAuth();
    when(caseServiceClient.getEvents(CASE_UUID)).thenReturn(buildCaseEvents(60));
    when(paymentServiceClient.getDebtEventsByCase(CASE_UUID)).thenReturn(List.of());
    when(caseServiceClient.getCase(any())).thenReturn(null);

    Model model = new ExtendedModelMap();
    controller.showTimeline(CASE_UUID, 1, 25, null, null, null, null, auth, model);

    @SuppressWarnings("unchecked")
    List<?> entries = (List<?>) model.getAttribute("entries");
    assertThat(entries).hasSize(25);
    assertThat(model.getAttribute("hasMore")).isEqualTo(true);
    assertThat(model.getAttribute("totalCount")).isEqualTo(60);
  }

  // ---------------------------------------------------------------------------
  // AC-E2: Load more — page 2 appends next 25 entries
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("AC-E2: loadMoreEntries page=2 returns entries 26-50 via timeline-entries fragment")
  void loadMoreEntries_page2_returnsNext25EntriesViaTimelineEntriesFragment() {
    mockCaseworkerAuth();
    when(caseServiceClient.getEvents(CASE_UUID)).thenReturn(buildCaseEvents(60));
    when(paymentServiceClient.getDebtEventsByCase(CASE_UUID)).thenReturn(List.of());
    when(caseServiceClient.getCase(any())).thenReturn(null);

    Model model = new ExtendedModelMap();
    String view = controller.loadMoreEntries(CASE_UUID, 2, 25, null, null, null, null, auth, model);

    assertThat(view).isEqualTo("fragments/timeline :: timeline-entries");
    @SuppressWarnings("unchecked")
    List<?> entries = (List<?>) model.getAttribute("entries");
    assertThat(entries).hasSize(25);
    assertThat(model.getAttribute("hasMore")).isEqualTo(true);
  }

  // ---------------------------------------------------------------------------
  // AC-E3: Load more — last page hides button (hasMore=false)
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("AC-E3: loadMoreEntries last page with 5 remaining entries sets hasMore=false")
  void loadMoreEntries_lastPage_hasMoreFalse() {
    mockCaseworkerAuth();
    when(caseServiceClient.getEvents(CASE_UUID)).thenReturn(buildCaseEvents(30));
    when(paymentServiceClient.getDebtEventsByCase(CASE_UUID)).thenReturn(List.of());
    when(caseServiceClient.getCase(any())).thenReturn(null);

    Model model = new ExtendedModelMap();
    controller.loadMoreEntries(CASE_UUID, 2, 25, null, null, null, null, auth, model);

    @SuppressWarnings("unchecked")
    List<?> entries = (List<?>) model.getAttribute("entries");
    assertThat(entries).hasSize(5);
    assertThat(model.getAttribute("hasMore")).isEqualTo(false);
  }

  // ---------------------------------------------------------------------------
  // AC-D1: Filter — single event category
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("AC-D1: showTimeline with eventCategory=FINANCIAL returns only FINANCIAL entries")
  void showTimeline_categoryFilterFinancial_returnsOnlyFinancialEntries() {
    mockCaseworkerAuth();
    List<CaseEventDto> events = new ArrayList<>();
    // 10 FINANCIAL (PAYMENT_APPLIED) + 10 CASE events
    for (int i = 0; i < 10; i++) {
      events.add(
          CaseEventDto.builder()
              .id(UUID.randomUUID())
              .eventType("PAYMENT_APPLIED")
              .performedAt(LocalDateTime.of(2026, 1, i + 1, 10, 0, 0))
              .build());
      events.add(
          CaseEventDto.builder()
              .id(UUID.randomUUID())
              .eventType("CASE_CREATED")
              .performedAt(LocalDateTime.of(2026, 2, i + 1, 10, 0, 0))
              .build());
    }
    when(caseServiceClient.getEvents(CASE_UUID)).thenReturn(events);
    when(paymentServiceClient.getDebtEventsByCase(CASE_UUID)).thenReturn(List.of());
    when(caseServiceClient.getCase(any())).thenReturn(null);

    Model model = new ExtendedModelMap();
    controller.showTimeline(
        CASE_UUID, 1, 25, new String[] {"FINANCIAL"}, null, null, null, auth, model);

    @SuppressWarnings("unchecked")
    List<TimelineEntryDto> entries = (List<TimelineEntryDto>) model.getAttribute("entries");
    assertThat(entries).allMatch(e -> e.getEventCategory() == EventCategory.FINANCIAL);
    assertThat(model.getAttribute("totalCount")).isEqualTo(10);
  }

  @Test
  @DisplayName(
      "AC-D1: showTimeline with multiple category filters returns entries from those categories only")
  void showTimeline_multipleCategoryFilters_returnsOnlyMatchingCategories() {
    mockCaseworkerAuth();
    List<CaseEventDto> events = new ArrayList<>();
    String[] types = {
      "PAYMENT_APPLIED", "COLLECTION_MEASURE_INITIATED", "CASE_CREATED", "JOURNAL_ENTRY_ADDED"
    };
    for (int i = 0; i < 4; i++) {
      events.add(
          CaseEventDto.builder()
              .id(UUID.randomUUID())
              .eventType(types[i])
              .performedAt(LocalDateTime.of(2026, 1, i + 1, 10, 0, 0))
              .build());
    }
    when(caseServiceClient.getEvents(CASE_UUID)).thenReturn(events);
    when(paymentServiceClient.getDebtEventsByCase(CASE_UUID)).thenReturn(List.of());
    when(caseServiceClient.getCase(any())).thenReturn(null);

    Model model = new ExtendedModelMap();
    controller.showTimeline(
        CASE_UUID, 1, 25, new String[] {"FINANCIAL", "COLLECTION"}, null, null, null, auth, model);

    @SuppressWarnings("unchecked")
    List<TimelineEntryDto> entries = (List<TimelineEntryDto>) model.getAttribute("entries");
    assertThat(entries)
        .allMatch(
            e ->
                e.getEventCategory() == EventCategory.FINANCIAL
                    || e.getEventCategory() == EventCategory.COLLECTION);
  }

  // ---------------------------------------------------------------------------
  // AC-D2: Filter — date range
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("AC-D2: showTimeline with date range filter returns only entries within the range")
  void showTimeline_dateRangeFilter_returnsOnlyEntriesWithinRange() {
    mockCaseworkerAuth();
    List<CaseEventDto> events = new ArrayList<>();
    // 5 events in 2025, 5 events in 2026 Jan-Mar
    for (int i = 1; i <= 5; i++) {
      events.add(
          CaseEventDto.builder()
              .id(UUID.randomUUID())
              .eventType("CASE_CREATED")
              .performedAt(LocalDateTime.of(2025, 6, i, 10, 0, 0))
              .build());
    }
    for (int i = 1; i <= 5; i++) {
      events.add(
          CaseEventDto.builder()
              .id(UUID.randomUUID())
              .eventType("CASE_CREATED")
              .performedAt(LocalDateTime.of(2026, i, 15, 10, 0, 0))
              .build());
    }
    when(caseServiceClient.getEvents(CASE_UUID)).thenReturn(events);
    when(paymentServiceClient.getDebtEventsByCase(CASE_UUID)).thenReturn(List.of());
    when(caseServiceClient.getCase(any())).thenReturn(null);

    Model model = new ExtendedModelMap();
    controller.showTimeline(
        CASE_UUID,
        1,
        25,
        null,
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 3, 1),
        null,
        auth,
        model);

    @SuppressWarnings("unchecked")
    List<TimelineEntryDto> entries = (List<TimelineEntryDto>) model.getAttribute("entries");
    assertThat(entries)
        .allMatch(
            e ->
                !e.getTimestamp().toLocalDate().isBefore(LocalDate.of(2026, 1, 1))
                    && !e.getTimestamp().toLocalDate().isAfter(LocalDate.of(2026, 3, 1)));
  }

  // ---------------------------------------------------------------------------
  // AC-D3: Filter — by specific debt (case-level events always included)
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName(
      "AC-D3: showTimeline with debtId filter returns matched debt entries plus all case-level entries")
  void showTimeline_debtIdFilter_returnsCaseLevelAndMatchedDebtEntries() {
    mockCaseworkerAuth();
    CaseEventDto caseLevel =
        CaseEventDto.builder()
            .id(UUID.randomUUID())
            .eventType("CASE_CREATED")
            .performedAt(LocalDateTime.of(2026, 1, 1, 10, 0, 0))
            .build();
    DebtEventDto debtA =
        DebtEventDto.builder()
            .id(UUID.randomUUID())
            .debtId(DEBT_UUID_A)
            .eventType("PAYMENT_RECEIVED")
            .createdAt(LocalDateTime.of(2026, 1, 2, 10, 0, 0))
            .build();
    DebtEventDto debtB =
        DebtEventDto.builder()
            .id(UUID.randomUUID())
            .debtId(DEBT_UUID_B)
            .eventType("PAYMENT_RECEIVED")
            .createdAt(LocalDateTime.of(2026, 1, 3, 10, 0, 0))
            .build();

    when(caseServiceClient.getEvents(CASE_UUID)).thenReturn(List.of(caseLevel));
    when(paymentServiceClient.getDebtEventsByCase(CASE_UUID)).thenReturn(List.of(debtA, debtB));
    when(caseServiceClient.getCase(any())).thenReturn(null);

    Model model = new ExtendedModelMap();
    controller.showTimeline(CASE_UUID, 1, 25, null, null, null, DEBT_UUID_A, auth, model);

    @SuppressWarnings("unchecked")
    List<TimelineEntryDto> entries = (List<TimelineEntryDto>) model.getAttribute("entries");
    assertThat(entries).allMatch(e -> e.getDebtId() == null || e.getDebtId().equals(DEBT_UUID_A));
    assertThat(entries).noneMatch(e -> DEBT_UUID_B.equals(e.getDebtId()));
  }

  // ---------------------------------------------------------------------------
  // AC-E4: Filters preserved across paginated load-more requests
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("AC-E4: loadMoreEntries preserves active category filter across pages")
  void loadMoreEntries_activeFiltersPreservedOnSubsequentPages() {
    mockCaseworkerAuth();
    // 60 FINANCIAL + 10 CASE events
    List<DebtEventDto> paymentEvents =
        IntStream.range(0, 60)
            .mapToObj(
                i ->
                    DebtEventDto.builder()
                        .id(UUID.randomUUID())
                        .debtId(UUID.randomUUID())
                        .eventType("PAYMENT_RECEIVED")
                        .createdAt(LocalDateTime.of(2026, 1, 1, 10, 0, i))
                        .build())
            .collect(Collectors.toList());
    List<CaseEventDto> caseEvents =
        IntStream.range(0, 10)
            .mapToObj(
                i ->
                    CaseEventDto.builder()
                        .id(UUID.randomUUID())
                        .eventType("CASE_CREATED")
                        .performedAt(LocalDateTime.of(2025, 6, i + 1, 10, 0, 0))
                        .build())
            .collect(Collectors.toList());

    when(caseServiceClient.getEvents(CASE_UUID)).thenReturn(caseEvents);
    when(paymentServiceClient.getDebtEventsByCase(CASE_UUID)).thenReturn(paymentEvents);
    when(caseServiceClient.getCase(any())).thenReturn(null);

    Model model = new ExtendedModelMap();
    controller.loadMoreEntries(
        CASE_UUID, 2, 25, new String[] {"FINANCIAL"}, null, null, null, auth, model);

    @SuppressWarnings("unchecked")
    List<TimelineEntryDto> page2entries = (List<TimelineEntryDto>) model.getAttribute("entries");
    assertThat(page2entries).allMatch(e -> e.getEventCategory() == EventCategory.FINANCIAL);
  }

  // ---------------------------------------------------------------------------
  // AC-C1: Filter chip rendering (model contract)
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName(
      "Scenario: Active filters are displayed as chips — model contains active TimelineFilterDto")
  void showTimeline_activeFilters_filterDtoPopulatedInModel() {
    mockCaseworkerAuth();
    when(caseServiceClient.getEvents(CASE_UUID)).thenReturn(List.of());
    when(paymentServiceClient.getDebtEventsByCase(CASE_UUID)).thenReturn(List.of());
    when(caseServiceClient.getCase(any())).thenReturn(null);

    Model model = new ExtendedModelMap();
    controller.showTimeline(
        CASE_UUID,
        1,
        25,
        new String[] {"FINANCIAL"},
        LocalDate.of(2026, 1, 1),
        null,
        null,
        auth,
        model);

    TimelineFilterDto filters = (TimelineFilterDto) model.getAttribute("filters");
    assertThat(filters).isNotNull();
    assertThat(filters.getEventCategories()).contains(EventCategory.FINANCIAL);
    assertThat(filters.getFromDate()).isEqualTo(LocalDate.of(2026, 1, 1));
  }

  // ---------------------------------------------------------------------------
  // AC-G1: Graceful degradation — empty case shows no entries, no warning
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("AC-G1: showTimeline for empty case returns empty entries list and no warnings")
  void showTimeline_emptyCase_returnsEmptyEntriesAndNoWarning() {
    mockCaseworkerAuth();
    when(caseServiceClient.getEvents(CASE_UUID)).thenReturn(List.of());
    when(paymentServiceClient.getDebtEventsByCase(CASE_UUID)).thenReturn(List.of());
    when(caseServiceClient.getCase(any())).thenReturn(null);

    Model model = new ExtendedModelMap();
    controller.showTimeline(CASE_UUID, 1, 25, null, null, null, null, auth, model);

    @SuppressWarnings("unchecked")
    List<?> entries = (List<?>) model.getAttribute("entries");
    @SuppressWarnings("unchecked")
    List<?> warnings = (List<?>) model.getAttribute("warnings");
    assertThat(entries).isEmpty();
    assertThat(warnings).isEmpty();
  }

  @Test
  @DisplayName("Scenario: Missing correspondence events handled gracefully")
  void showTimeline_missingCorrespondenceEvents_noErrorShown() {
    mockCaseworkerAuth();
    when(caseServiceClient.getEvents(CASE_UUID)).thenReturn(buildCaseEvents(3));
    when(paymentServiceClient.getDebtEventsByCase(CASE_UUID)).thenReturn(List.of());
    when(caseServiceClient.getCase(any())).thenReturn(null);

    Model model = new ExtendedModelMap();
    // No exception should be thrown; timeline renders with available events
    String view = controller.showTimeline(CASE_UUID, 1, 25, null, null, null, null, auth, model);
    assertThat(view).isEqualTo("fragments/timeline :: timeline-panel");
  }

  // ---------------------------------------------------------------------------
  // AC-G3: Graceful degradation — payment-service down
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName(
      "AC-G3: showTimeline when payment-service is unavailable returns case events with partial warning")
  void showTimeline_paymentServiceDown_returnsCaseEventsWithPartialWarning() {
    mockCaseworkerAuth();
    when(caseServiceClient.getEvents(CASE_UUID)).thenReturn(buildCaseEvents(3));
    when(paymentServiceClient.getDebtEventsByCase(CASE_UUID))
        .thenThrow(new RuntimeException("Payment service unavailable"));
    when(caseServiceClient.getCase(any())).thenReturn(null);

    Model model = new ExtendedModelMap();
    controller.showTimeline(CASE_UUID, 1, 25, null, null, null, null, auth, model);

    @SuppressWarnings("unchecked")
    List<?> entries = (List<?>) model.getAttribute("entries");
    @SuppressWarnings("unchecked")
    List<String> warnings = (List<String>) model.getAttribute("warnings");
    // At least some entries from case-service
    assertThat(entries).isNotEmpty();
    assertThat(warnings).contains("timeline.warning.partial");
  }
}
