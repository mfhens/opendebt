package dk.ufst.opendebt.citizen.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
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
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import dk.ufst.opendebt.citizen.client.CaseServiceClient;
import dk.ufst.opendebt.citizen.client.PaymentServiceClient;
import dk.ufst.opendebt.common.dto.CaseEventDto;
import dk.ufst.opendebt.common.dto.DebtEventDto;
import dk.ufst.opendebt.common.timeline.EventCategory;
import dk.ufst.opendebt.common.timeline.TimelineEntryDto;
import dk.ufst.opendebt.common.timeline.TimelineVisibilityProperties;

/**
 * Unit tests for {@link CitizenTimelineController}. Ref: petition050 AC-B2, AC-F3, AC-A3, AC-E1,
 * AC-G1, AC-G3, specs §6.2.
 */
@ExtendWith(MockitoExtension.class)
class CitizenTimelineControllerTest {

  @Mock private CaseServiceClient caseServiceClient;
  @Mock private PaymentServiceClient paymentServiceClient;
  @Mock private TimelineVisibilityProperties visibilityProperties;

  private ExecutorService executor;

  @InjectMocks private CitizenTimelineController controller;

  private static final String CASE_ID = "00000000-0000-0000-0000-000000000002";
  private static final UUID CASE_UUID = UUID.fromString(CASE_ID);
  private static final Set<EventCategory> CITIZEN_ALLOWED =
      Set.of(
          EventCategory.FINANCIAL,
          EventCategory.DEBT_LIFECYCLE,
          EventCategory.CORRESPONDENCE,
          EventCategory.COLLECTION);

  @BeforeEach
  void setUp() {
    executor = Executors.newFixedThreadPool(4);
    try {
      var field = CitizenTimelineController.class.getDeclaredField("bffFetchExecutor");
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

  private void mockCitizenVisibility() {
    when(visibilityProperties.getAllowedCategories("CITIZEN")).thenReturn(CITIZEN_ALLOWED);
  }

  private List<CaseEventDto> buildEvents(int count, String eventType) {
    return IntStream.range(0, count)
        .mapToObj(
            i ->
                CaseEventDto.builder()
                    .id(UUID.randomUUID())
                    .eventType(eventType)
                    .performedAt(LocalDateTime.of(2026, 1, i + 1, 10, 0, 0))
                    .build())
        .collect(Collectors.toList());
  }

  // ---------------------------------------------------------------------------
  // AC-B2: Role visibility — CITIZEN sees only 4 allowed categories
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName(
      "AC-B2: showTimeline with CITIZEN auth returns only FINANCIAL, DEBT_LIFECYCLE, CORRESPONDENCE, COLLECTION events")
  void showTimeline_citizenAuth_returnsOnlyCitizenVisibleCategories() {
    mockCitizenVisibility();
    // Return events in ALL 7 categories (CASE, DEBT_LIFECYCLE, FINANCIAL, etc.)
    List<CaseEventDto> allCategoryEvents =
        List.of(
            CaseEventDto.builder()
                .id(UUID.randomUUID())
                .eventType("CASE_CREATED")
                .performedAt(LocalDateTime.of(2026, 1, 1, 10, 0, 0))
                .build(),
            CaseEventDto.builder()
                .id(UUID.randomUUID())
                .eventType("DEBT_REGISTERED")
                .performedAt(LocalDateTime.of(2026, 1, 2, 10, 0, 0))
                .build(),
            CaseEventDto.builder()
                .id(UUID.randomUUID())
                .eventType("PAYMENT_APPLIED")
                .performedAt(LocalDateTime.of(2026, 1, 3, 10, 0, 0))
                .build(),
            CaseEventDto.builder()
                .id(UUID.randomUUID())
                .eventType("COLLECTION_MEASURE_INITIATED")
                .performedAt(LocalDateTime.of(2026, 1, 4, 10, 0, 0))
                .build(),
            CaseEventDto.builder()
                .id(UUID.randomUUID())
                .eventType("OBJECTION_FILED")
                .performedAt(LocalDateTime.of(2026, 1, 5, 10, 0, 0))
                .build(),
            CaseEventDto.builder()
                .id(UUID.randomUUID())
                .eventType("JOURNAL_ENTRY_ADDED")
                .performedAt(LocalDateTime.of(2026, 1, 6, 10, 0, 0))
                .build());
    when(caseServiceClient.getEvents(CASE_UUID)).thenReturn(allCategoryEvents);
    when(paymentServiceClient.getDebtEventsByCase(CASE_UUID)).thenReturn(List.of());
    when(caseServiceClient.getCase(any())).thenReturn(null);

    Model model = new ExtendedModelMap();
    controller.showTimeline(CASE_UUID, 1, 25, null, null, null, null, model);

    @SuppressWarnings("unchecked")
    List<TimelineEntryDto> entries = (List<TimelineEntryDto>) model.getAttribute("entries");
    assertThat(entries).allMatch(e -> CITIZEN_ALLOWED.contains(e.getEventCategory()));
    assertThat(entries)
        .noneMatch(
            e ->
                e.getEventCategory() == EventCategory.CASE
                    || e.getEventCategory() == EventCategory.OBJECTION
                    || e.getEventCategory() == EventCategory.JOURNAL);
  }

  @Test
  @DisplayName("AC-F3: Citizen portal renders the timeline with restricted events on Tidslinje tab")
  void showTimeline_citizenPortal_rendersTimelineFragmentWithRestrictedEvents() {
    mockCitizenVisibility();
    when(caseServiceClient.getEvents(CASE_UUID)).thenReturn(List.of());
    when(paymentServiceClient.getDebtEventsByCase(CASE_UUID)).thenReturn(List.of());
    when(caseServiceClient.getCase(any())).thenReturn(null);

    Model model = new ExtendedModelMap();
    String view = controller.showTimeline(CASE_UUID, 1, 25, null, null, null, null, model);

    assertThat(view).isEqualTo("fragments/timeline :: timeline-panel");
    assertThat(model.getAttribute("entries")).isNotNull();
    assertThat(model.getAttribute("caseId")).isEqualTo(CASE_UUID);
  }

  // ---------------------------------------------------------------------------
  // AC-A3: Normalised structure — PAYMENT_RECEIVED has amount, CASE_CREATED has null amount
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName(
      "AC-A3: timeline normalises PAYMENT_RECEIVED entry with non-null amount and non-null debtId")
  void showTimeline_paymentReceivedEntry_hasNonNullAmountAndDebtId() {
    mockCitizenVisibility();
    UUID debtId = UUID.fromString("d2010000-0000-0000-0000-000000000001");
    DebtEventDto paymentEvent =
        DebtEventDto.builder()
            .id(UUID.randomUUID())
            .debtId(debtId)
            .eventType("PAYMENT_RECEIVED")
            .createdAt(LocalDateTime.of(2026, 1, 10, 10, 0, 0))
            .amount(new java.math.BigDecimal("1234.56"))
            .build();
    when(caseServiceClient.getEvents(CASE_UUID)).thenReturn(List.of());
    when(paymentServiceClient.getDebtEventsByCase(CASE_UUID)).thenReturn(List.of(paymentEvent));
    when(caseServiceClient.getCase(any())).thenReturn(null);

    Model model = new ExtendedModelMap();
    controller.showTimeline(CASE_UUID, 1, 25, null, null, null, null, model);

    @SuppressWarnings("unchecked")
    List<TimelineEntryDto> entries = (List<TimelineEntryDto>) model.getAttribute("entries");
    TimelineEntryDto paymentEntry =
        entries.stream()
            .filter(e -> "PAYMENT_RECEIVED".equals(e.getEventType()))
            .findFirst()
            .orElseThrow();
    assertThat(paymentEntry.getAmount()).isEqualByComparingTo("1234.56");
    assertThat(paymentEntry.getDebtId()).isNotNull();
  }

  // ---------------------------------------------------------------------------
  // AC-E1: Pagination (citizen portal)
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName(
      "AC-E1: citizen showTimeline with 60 total events returns 25 entries and hasMore=true")
  void showTimeline_citizenPortal_60Events_returns25EntriesWithHasMoreTrue() {
    mockCitizenVisibility();
    LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
    List<DebtEventDto> events =
        IntStream.range(0, 60)
            .mapToObj(
                i ->
                    DebtEventDto.builder()
                        .id(UUID.randomUUID())
                        .debtId(UUID.randomUUID()) // unique debtId per entry → unique dedupeKey
                        .eventType("PAYMENT_RECEIVED")
                        .createdAt(base.plusHours(i)) // unique timestamp per entry
                        .build())
            .collect(Collectors.toList());
    when(caseServiceClient.getEvents(CASE_UUID)).thenReturn(List.of());
    when(paymentServiceClient.getDebtEventsByCase(CASE_UUID)).thenReturn(events);
    when(caseServiceClient.getCase(any())).thenReturn(null);

    Model model = new ExtendedModelMap();
    controller.showTimeline(CASE_UUID, 1, 25, null, null, null, null, model);

    assertThat((List<?>) model.getAttribute("entries")).hasSize(25);
    assertThat(model.getAttribute("hasMore")).isEqualTo(true);
  }

  // ---------------------------------------------------------------------------
  // AC-G1: Empty case (citizen portal)
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("AC-G1: citizen showTimeline for empty case returns empty entries and no warnings")
  void showTimeline_citizenPortal_emptyCase_noEntriesNoWarning() {
    mockCitizenVisibility();
    when(caseServiceClient.getEvents(CASE_UUID)).thenReturn(List.of());
    when(paymentServiceClient.getDebtEventsByCase(CASE_UUID)).thenReturn(List.of());
    when(caseServiceClient.getCase(any())).thenReturn(null);

    Model model = new ExtendedModelMap();
    controller.showTimeline(CASE_UUID, 1, 25, null, null, null, null, model);

    assertThat((List<?>) model.getAttribute("entries")).isEmpty();
    assertThat((List<?>) model.getAttribute("warnings")).isEmpty();
  }

  // ---------------------------------------------------------------------------
  // AC-G3: Graceful degradation — payment-service down (citizen portal)
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName(
      "AC-G3: citizen showTimeline when payment-service is unavailable returns case events with partial warning")
  void showTimeline_citizenPortal_paymentServiceDown_returnsCaseEventsWithPartialWarning() {
    mockCitizenVisibility();
    // 3 DEBT_LIFECYCLE events (visible to CITIZEN)
    List<CaseEventDto> debtEvents = buildEvents(3, "DEBT_REGISTERED");
    when(caseServiceClient.getEvents(CASE_UUID)).thenReturn(debtEvents);
    when(paymentServiceClient.getDebtEventsByCase(CASE_UUID))
        .thenThrow(new RuntimeException("Payment service unavailable"));
    when(caseServiceClient.getCase(any())).thenReturn(null);

    Model model = new ExtendedModelMap();
    controller.showTimeline(CASE_UUID, 1, 25, null, null, null, null, model);

    assertThat((List<?>) model.getAttribute("entries")).hasSize(3);
    @SuppressWarnings("unchecked")
    List<String> warnings = (List<String>) model.getAttribute("warnings");
    assertThat(warnings).contains("timeline.warning.partial");
  }
}
