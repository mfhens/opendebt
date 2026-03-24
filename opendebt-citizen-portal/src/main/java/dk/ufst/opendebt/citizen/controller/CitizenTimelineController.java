package dk.ufst.opendebt.citizen.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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
    TimelineFilterDto filters = buildFilter(eventCategory, fromDate, toDate, debtId, allowed);

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
            .filter(e -> matchesFilter(e, filters))
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
    String loadMoreUrl = buildLoadMoreUrl(filters, timelineEntriesUrl, page, size);
    Map<String, String> filterRemoveLinks = buildFilterRemoveLinks(filters, timelineBaseUrl);

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

  private TimelineFilterDto buildFilter(
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

  private boolean matchesFilter(TimelineEntryDto entry, TimelineFilterDto filter) {
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

  private String buildLoadMoreUrl(
      TimelineFilterDto filters, String entriesUrl, int page, int size) {
    StringBuilder sb = new StringBuilder(entriesUrl);
    sb.append("?page=").append(page + 1).append("&size=").append(size);
    for (EventCategory cat : filters.getEventCategories()) {
      sb.append("&eventCategory=").append(cat.name());
    }
    if (filters.getFromDate() != null) {
      sb.append("&fromDate=").append(filters.getFromDate());
    }
    if (filters.getToDate() != null) {
      sb.append("&toDate=").append(filters.getToDate());
    }
    if (filters.getDebtId() != null) {
      sb.append("&debtId=").append(filters.getDebtId());
    }
    return sb.toString();
  }

  private Map<String, String> buildFilterRemoveLinks(TimelineFilterDto filters, String baseUrl) {
    Map<String, String> links = new HashMap<>();
    for (EventCategory removedCat : filters.getEventCategories()) {
      StringBuilder sb = new StringBuilder(baseUrl);
      boolean first = true;
      for (EventCategory cat : filters.getEventCategories()) {
        if (!cat.equals(removedCat)) {
          sb.append(first ? "?" : "&").append("eventCategory=").append(cat.name());
          first = false;
        }
      }
      if (filters.getFromDate() != null) {
        sb.append(first ? "?" : "&").append("fromDate=").append(filters.getFromDate());
        first = false;
      }
      if (filters.getToDate() != null) {
        sb.append(first ? "?" : "&").append("toDate=").append(filters.getToDate());
        first = false;
      }
      if (filters.getDebtId() != null) {
        sb.append(first ? "?" : "&").append("debtId=").append(filters.getDebtId());
      }
      links.put(removedCat.name(), sb.toString());
    }
    if (filters.getFromDate() != null) {
      StringBuilder sb = new StringBuilder(baseUrl);
      boolean first = true;
      for (EventCategory cat : filters.getEventCategories()) {
        sb.append(first ? "?" : "&").append("eventCategory=").append(cat.name());
        first = false;
      }
      if (filters.getToDate() != null) {
        sb.append(first ? "?" : "&").append("toDate=").append(filters.getToDate());
        first = false;
      }
      if (filters.getDebtId() != null) {
        sb.append(first ? "?" : "&").append("debtId=").append(filters.getDebtId());
      }
      links.put("fromDate", sb.toString());
    }
    if (filters.getToDate() != null) {
      StringBuilder sb = new StringBuilder(baseUrl);
      boolean first = true;
      for (EventCategory cat : filters.getEventCategories()) {
        sb.append(first ? "?" : "&").append("eventCategory=").append(cat.name());
        first = false;
      }
      if (filters.getFromDate() != null) {
        sb.append(first ? "?" : "&").append("fromDate=").append(filters.getFromDate());
        first = false;
      }
      if (filters.getDebtId() != null) {
        sb.append(first ? "?" : "&").append("debtId=").append(filters.getDebtId());
      }
      links.put("toDate", sb.toString());
    }
    if (filters.getDebtId() != null) {
      StringBuilder sb = new StringBuilder(baseUrl);
      boolean first = true;
      for (EventCategory cat : filters.getEventCategories()) {
        sb.append(first ? "?" : "&").append("eventCategory=").append(cat.name());
        first = false;
      }
      if (filters.getFromDate() != null) {
        sb.append(first ? "?" : "&").append("fromDate=").append(filters.getFromDate());
        first = false;
      }
      if (filters.getToDate() != null) {
        sb.append(first ? "?" : "&").append("toDate=").append(filters.getToDate());
      }
      links.put("debtId", sb.toString());
    }
    return links;
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
