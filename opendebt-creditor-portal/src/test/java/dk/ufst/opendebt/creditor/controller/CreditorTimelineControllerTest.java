package dk.ufst.opendebt.creditor.controller;

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

import dk.ufst.opendebt.common.dto.CaseEventDto;
import dk.ufst.opendebt.common.dto.DebtEventDto;
import dk.ufst.opendebt.common.timeline.EventCategory;
import dk.ufst.opendebt.common.timeline.TimelineEntryDto;
import dk.ufst.opendebt.common.timeline.TimelineVisibilityProperties;
import dk.ufst.opendebt.creditor.client.CaseServiceClient;
import dk.ufst.opendebt.creditor.client.PaymentServiceClient;

/**
 * Unit tests for {@link CreditorTimelineController}. Ref: petition050 AC-B3, AC-F3, AC-A1, AC-E1,
 * AC-G1, AC-G3, specs §6.2.
 */
@ExtendWith(MockitoExtension.class)
class CreditorTimelineControllerTest {

  @Mock private CaseServiceClient caseServiceClient;
  @Mock private PaymentServiceClient paymentServiceClient;
  @Mock private TimelineVisibilityProperties visibilityProperties;

  private ExecutorService executor;

  @InjectMocks private CreditorTimelineController controller;

  private static final String CLAIM_ID = "00000000-0000-0000-0000-000000000003";
  private static final UUID CLAIM_UUID = UUID.fromString(CLAIM_ID);
  private static final Set<EventCategory> CREDITOR_ALLOWED =
      Set.of(EventCategory.FINANCIAL, EventCategory.DEBT_LIFECYCLE, EventCategory.COLLECTION);

  @BeforeEach
  void setUp() {
    executor = Executors.newFixedThreadPool(4);
    try {
      var field = CreditorTimelineController.class.getDeclaredField("bffFetchExecutor");
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

  private void mockCreditorVisibility() {
    when(visibilityProperties.getAllowedCategories("CREDITOR")).thenReturn(CREDITOR_ALLOWED);
  }

  private List<DebtEventDto> buildDebtEvents(int count) {
    LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
    return IntStream.range(0, count)
        .mapToObj(
            i ->
                DebtEventDto.builder()
                    .id(UUID.randomUUID())
                    .debtId(UUID.randomUUID()) // unique debtId → unique dedupeKey per entry
                    .eventType("PAYMENT_RECEIVED")
                    .createdAt(base.plusHours(i)) // unique timestamp, no day overflow
                    .build())
        .collect(Collectors.toList());
  }

  private List<CaseEventDto> buildDebtLifecycleEvents(int count) {
    LocalDateTime base = LocalDateTime.of(2026, 2, 1, 0, 0, 0);
    return IntStream.range(0, count)
        .mapToObj(
            i ->
                CaseEventDto.builder()
                    .id(UUID.randomUUID())
                    .eventType("DEBT_REGISTERED")
                    .performedAt(base.plusHours(i))
                    .build())
        .collect(Collectors.toList());
  }

  // ---------------------------------------------------------------------------
  // AC-B3: Role visibility — CREDITOR sees only 3 allowed categories
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName(
      "AC-B3: showTimeline with CREDITOR auth returns only FINANCIAL, DEBT_LIFECYCLE, COLLECTION events")
  void showTimeline_creditorAuth_returnsOnlyCreditorVisibleCategories() {
    mockCreditorVisibility();
    List<CaseEventDto> allEvents =
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
                .eventType("COLLECTION_MEASURE_INITIATED")
                .performedAt(LocalDateTime.of(2026, 1, 3, 10, 0, 0))
                .build(),
            CaseEventDto.builder()
                .id(UUID.randomUUID())
                .eventType("OBJECTION_FILED")
                .performedAt(LocalDateTime.of(2026, 1, 4, 10, 0, 0))
                .build(),
            CaseEventDto.builder()
                .id(UUID.randomUUID())
                .eventType("JOURNAL_ENTRY_ADDED")
                .performedAt(LocalDateTime.of(2026, 1, 5, 10, 0, 0))
                .build());
    when(caseServiceClient.getEvents(CLAIM_UUID)).thenReturn(allEvents);
    when(paymentServiceClient.getDebtEventsByCase(CLAIM_UUID)).thenReturn(List.of());
    when(caseServiceClient.getCase(any())).thenReturn(null);

    Model model = new ExtendedModelMap();
    controller.showTimeline(CLAIM_UUID, 1, 25, null, null, null, null, model);

    @SuppressWarnings("unchecked")
    List<TimelineEntryDto> entries = (List<TimelineEntryDto>) model.getAttribute("entries");
    assertThat(entries).allMatch(e -> CREDITOR_ALLOWED.contains(e.getEventCategory()));
    assertThat(entries)
        .noneMatch(
            e ->
                e.getEventCategory() == EventCategory.CASE
                    || e.getEventCategory() == EventCategory.CORRESPONDENCE
                    || e.getEventCategory() == EventCategory.OBJECTION
                    || e.getEventCategory() == EventCategory.JOURNAL);
  }

  @Test
  @DisplayName(
      "AC-F3: Creditor portal renders timeline with restricted events — uses claimId not caseId in model")
  void showTimeline_creditorPortal_rendersTimelineWithClaimIdInModel() {
    mockCreditorVisibility();
    when(caseServiceClient.getEvents(CLAIM_UUID)).thenReturn(List.of());
    when(paymentServiceClient.getDebtEventsByCase(CLAIM_UUID)).thenReturn(List.of());
    when(caseServiceClient.getCase(any())).thenReturn(null);

    Model model = new ExtendedModelMap();
    controller.showTimeline(CLAIM_UUID, 1, 25, null, null, null, null, model);

    assertThat(model.getAttribute("claimId")).isNotNull();
    assertThat(model.getAttribute("caseId")).isNull(); // must NOT be in model
    assertThat(model.getAttribute("entries")).isNotNull();
  }

  // ---------------------------------------------------------------------------
  // AC-A1: Data aggregation — merges case + payment events (creditor portal)
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("AC-A1: creditor showTimeline merges case events + debt events from both services")
  void showTimeline_creditorPortal_mergesEventsFromBothServices() {
    mockCreditorVisibility();
    when(caseServiceClient.getEvents(CLAIM_UUID)).thenReturn(buildDebtLifecycleEvents(2));
    when(paymentServiceClient.getDebtEventsByCase(CLAIM_UUID)).thenReturn(buildDebtEvents(3));
    when(caseServiceClient.getCase(any())).thenReturn(null);

    Model model = new ExtendedModelMap();
    controller.showTimeline(CLAIM_UUID, 1, 25, null, null, null, null, model);

    @SuppressWarnings("unchecked")
    List<?> entries = (List<?>) model.getAttribute("entries");
    assertThat(entries).hasSize(5);
  }

  // ---------------------------------------------------------------------------
  // AC-E1: Pagination (creditor portal)
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName(
      "AC-E1: creditor showTimeline with 60 total events returns 25 entries and hasMore=true")
  void showTimeline_creditorPortal_60Events_returns25EntriesWithHasMoreTrue() {
    mockCreditorVisibility();
    when(caseServiceClient.getEvents(CLAIM_UUID)).thenReturn(List.of());
    when(paymentServiceClient.getDebtEventsByCase(CLAIM_UUID)).thenReturn(buildDebtEvents(60));
    when(caseServiceClient.getCase(any())).thenReturn(null);

    Model model = new ExtendedModelMap();
    controller.showTimeline(CLAIM_UUID, 1, 25, null, null, null, null, model);

    assertThat((List<?>) model.getAttribute("entries")).hasSize(25);
    assertThat(model.getAttribute("hasMore")).isEqualTo(true);
  }

  @Test
  @DisplayName(
      "AC-E2: creditor loadMoreEntries (poster) endpoint returns next page entries fragment")
  void loadMorePosterEntries_page2_returnsTimelineEntriesFragment() {
    mockCreditorVisibility();
    when(caseServiceClient.getEvents(CLAIM_UUID)).thenReturn(List.of());
    when(paymentServiceClient.getDebtEventsByCase(CLAIM_UUID)).thenReturn(buildDebtEvents(60));
    when(caseServiceClient.getCase(any())).thenReturn(null);

    Model model = new ExtendedModelMap();
    String view =
        controller.loadMorePosterEntries(CLAIM_UUID, 2, 25, null, null, null, null, model);

    assertThat(view).isEqualTo("fragments/timeline :: timeline-entries");
    assertThat((List<?>) model.getAttribute("entries")).hasSize(25);
  }

  // ---------------------------------------------------------------------------
  // AC-G1: Empty case (creditor portal)
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("AC-G1: creditor showTimeline for empty case returns empty entries and no warnings")
  void showTimeline_creditorPortal_emptyCase_noEntriesNoWarning() {
    mockCreditorVisibility();
    when(caseServiceClient.getEvents(CLAIM_UUID)).thenReturn(List.of());
    when(paymentServiceClient.getDebtEventsByCase(CLAIM_UUID)).thenReturn(List.of());
    when(caseServiceClient.getCase(any())).thenReturn(null);

    Model model = new ExtendedModelMap();
    controller.showTimeline(CLAIM_UUID, 1, 25, null, null, null, null, model);

    assertThat((List<?>) model.getAttribute("entries")).isEmpty();
    assertThat((List<?>) model.getAttribute("warnings")).isEmpty();
  }

  // ---------------------------------------------------------------------------
  // AC-G3: Graceful degradation — payment-service down (creditor portal)
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName(
      "AC-G3: creditor showTimeline when payment-service is unavailable returns case events with partial warning")
  void showTimeline_creditorPortal_paymentServiceDown_returnsCaseEventsWithPartialWarning() {
    mockCreditorVisibility();
    when(caseServiceClient.getEvents(CLAIM_UUID)).thenReturn(buildDebtLifecycleEvents(4));
    when(paymentServiceClient.getDebtEventsByCase(CLAIM_UUID))
        .thenThrow(new RuntimeException("Payment service unavailable"));
    when(caseServiceClient.getCase(any())).thenReturn(null);

    Model model = new ExtendedModelMap();
    controller.showTimeline(CLAIM_UUID, 1, 25, null, null, null, null, model);

    assertThat((List<?>) model.getAttribute("entries")).hasSize(4);
    @SuppressWarnings("unchecked")
    List<String> warnings = (List<String>) model.getAttribute("warnings");
    assertThat(warnings).contains("timeline.warning.partial");
  }
}
