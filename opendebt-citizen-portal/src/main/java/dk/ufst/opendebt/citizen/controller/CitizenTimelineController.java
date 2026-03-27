package dk.ufst.opendebt.citizen.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import dk.ufst.opendebt.citizen.client.CaseServiceClient;
import dk.ufst.opendebt.citizen.client.PaymentServiceClient;
import dk.ufst.opendebt.common.dto.CaseDebtDto;
import dk.ufst.opendebt.common.dto.CaseEventDto;
import dk.ufst.opendebt.common.dto.DebtEventDto;
import dk.ufst.opendebt.common.timeline.EventCategory;
import dk.ufst.opendebt.common.timeline.TimelineDeduplicator;
import dk.ufst.opendebt.common.timeline.TimelineEntryDto;
import dk.ufst.opendebt.common.timeline.TimelineEntryMapper;
import dk.ufst.opendebt.common.timeline.TimelineFilterDto;
import dk.ufst.opendebt.common.timeline.TimelineFilterHelper;
import dk.ufst.opendebt.common.timeline.TimelineUrlBuilder;
import dk.ufst.opendebt.common.timeline.TimelineVisibilityProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * BFF controller for the unified case timeline in the citizen portal.
 *
 * <p>Citizens see only events in {FINANCIAL, DEBT_LIFECYCLE, CORRESPONDENCE, COLLECTION} per
 * application.yml visibility configuration. Ref: petition050 specs §4.5.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class CitizenTimelineController {

  private static final String FRAGMENT_PANEL = "fragments/timeline :: timeline-panel";
  private static final String FRAGMENT_ENTRIES = "fragments/timeline :: timeline-entries";
  private static final String WARNING_PARTIAL = "timeline.warning.partial";
  private static final String CITIZEN_ROLE = "CITIZEN";

  private final CaseServiceClient caseServiceClient;
  private final PaymentServiceClient paymentServiceClient;
  private final TimelineVisibilityProperties visibilityProperties;
  private final ExecutorService bffFetchExecutor;

  /** GET /cases/{caseId}/tidslinje — renders the full timeline-panel fragment for citizens. */
  @GetMapping("/cases/{caseId}/tidslinje")
  public String showTimeline(
      @PathVariable UUID caseId,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "25") int size,
      @RequestParam(required = false) String[] eventCategory,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate fromDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate toDate,
      @RequestParam(required = false) UUID debtId,
      Model model) {
    return buildTimeline(
        caseId, page, size, eventCategory, fromDate, toDate, debtId, model, FRAGMENT_PANEL);
  }

  /**
   * GET /cases/{caseId}/tidslinje/entries — returns entry elements plus OOB load-more container for
   * citizens.
   */
  @GetMapping("/cases/{caseId}/tidslinje/entries")
  public String loadMoreEntries(
      @PathVariable UUID caseId,
      @RequestParam int page,
      @RequestParam(defaultValue = "25") int size,
      @RequestParam(required = false) String[] eventCategory,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate fromDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate toDate,
      @RequestParam(required = false) UUID debtId,
      Model model) {
    return buildTimeline(
        caseId, page, size, eventCategory, fromDate, toDate, debtId, model, FRAGMENT_ENTRIES);
  }

  private String buildTimeline(
      UUID caseId,
      int page,
      int size,
      String[] eventCategory,
      LocalDate fromDate,
      LocalDate toDate,
      UUID debtId,
      Model model,
      String viewName) {

    Set<EventCategory> allowed = visibilityProperties.getAllowedCategories(CITIZEN_ROLE);
    TimelineFilterDto filters =
        TimelineFilterHelper.buildFilter(eventCategory, fromDate, toDate, debtId, allowed);

    List<String> warnings = Collections.synchronizedList(new ArrayList<>());
    CompletableFuture<List<CaseEventDto>> caseFuture =
        CompletableFuture.supplyAsync(() -> caseServiceClient.getEvents(caseId), bffFetchExecutor)
            .exceptionally(
                t -> {
                  log.warn("Failed to fetch case events for {}: {}", caseId, t.getMessage());
                  warnings.add(WARNING_PARTIAL);
                  return List.of();
                });
    CompletableFuture<List<DebtEventDto>> paymentFuture =
        CompletableFuture.supplyAsync(
                () -> paymentServiceClient.getDebtEventsByCase(caseId), bffFetchExecutor)
            .exceptionally(
                t -> {
                  log.warn("Failed to fetch payment events for {}: {}", caseId, t.getMessage());
                  warnings.add(WARNING_PARTIAL);
                  return List.of();
                });

    List<CaseEventDto> caseEvents;
    List<DebtEventDto> paymentEvents;
    try {
      CompletableFuture.allOf(caseFuture, paymentFuture).get(4, TimeUnit.SECONDS);
      caseEvents = caseFuture.join();
      paymentEvents = paymentFuture.join();
    } catch (Exception e) {
      log.warn("Timeout waiting for timeline futures for {}: {}", caseId, e.getMessage());
      caseEvents = caseFuture.getNow(List.of());
      paymentEvents = paymentFuture.getNow(List.of());
    }

    List<TimelineEntryDto> caseEntries =
        caseEvents.stream().map(TimelineEntryMapper::fromCaseEvent).collect(Collectors.toList());
    List<TimelineEntryDto> paymentEntries =
        paymentEvents.stream().map(TimelineEntryMapper::fromDebtEvent).collect(Collectors.toList());
    List<TimelineEntryDto> merged =
        TimelineDeduplicator.deduplicate(
            Stream.concat(caseEntries.stream(), paymentEntries.stream())
                .collect(Collectors.toList()));

    List<TimelineEntryDto> filtered =
        merged.stream()
            .filter(e -> allowed.contains(e.getEventCategory()))
            .filter(e -> TimelineFilterHelper.matchesFilter(e, filters))
            .sorted(
                Comparator.comparing(
                        TimelineEntryDto::getTimestamp,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                    .reversed())
            .collect(Collectors.toList());

    int totalCount = filtered.size();
    int offset = (page - 1) * size;
    List<TimelineEntryDto> pageEntries =
        filtered.subList(Math.min(offset, totalCount), Math.min(offset + size, totalCount));
    boolean hasMore = totalCount > (long) page * size;

    List<String> deduplicatedWarnings = warnings.stream().distinct().collect(Collectors.toList());
    List<CaseDebtDto> availableDebts = fetchAvailableDebts(caseId);

    String timelineBaseUrl = buildPortalUrl("/cases/" + caseId + "/tidslinje");
    String timelineEntriesUrl = buildPortalUrl("/cases/" + caseId + "/tidslinje/entries");
    String loadMoreUrl =
        TimelineUrlBuilder.buildLoadMoreUrl(filters, timelineEntriesUrl, page, size);
    Map<String, String> filterRemoveLinks =
        TimelineUrlBuilder.buildFilterRemoveLinks(filters, timelineBaseUrl);

    model.addAttribute("entries", pageEntries);
    model.addAttribute("page", page);
    model.addAttribute("size", size);
    model.addAttribute("hasMore", hasMore);
    model.addAttribute("totalCount", totalCount);
    model.addAttribute("filters", filters);
    model.addAttribute("warnings", deduplicatedWarnings);
    model.addAttribute("caseId", caseId);
    model.addAttribute("availableDebts", availableDebts);
    model.addAttribute("timelineBaseUrl", timelineBaseUrl);
    model.addAttribute("timelineEntriesUrl", timelineEntriesUrl);
    model.addAttribute("loadMoreUrl", loadMoreUrl);
    model.addAttribute("filterRemoveLinks", filterRemoveLinks);

    return viewName;
  }

  private String buildPortalUrl(String portalPath) {
    try {
      return ServletUriComponentsBuilder.fromCurrentContextPath().path(portalPath).toUriString();
    } catch (IllegalStateException ex) {
      return portalPath;
    }
  }

  private List<CaseDebtDto> fetchAvailableDebts(UUID caseId) {
    try {
      dk.ufst.opendebt.common.dto.CaseDto caseDto = caseServiceClient.getCase(caseId);
      if (caseDto == null || caseDto.getDebtIds() == null) {
        return List.of();
      }
      return caseDto.getDebtIds().stream()
          .map(id -> CaseDebtDto.builder().debtId(id).transferReference(null).build())
          .collect(Collectors.toList());
    } catch (Exception e) {
      log.debug("Could not fetch case debts for filter bar: {}", e.getMessage());
      return List.of();
    }
  }
}
