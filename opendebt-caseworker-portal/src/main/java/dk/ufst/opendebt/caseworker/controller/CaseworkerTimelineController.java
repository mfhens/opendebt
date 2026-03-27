package dk.ufst.opendebt.caseworker.controller;

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

import jakarta.servlet.http.HttpSession;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import dk.ufst.opendebt.caseworker.client.CaseServiceClient;
import dk.ufst.opendebt.caseworker.client.PaymentServiceClient;
import dk.ufst.opendebt.caseworker.dto.CaseworkerIdentity;
import dk.ufst.opendebt.caseworker.service.CaseworkerSessionService;
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
 * BFF controller for the unified case timeline in the caseworker portal.
 *
 * <p>Aggregates events from case-service and payment-service, deduplicates, filters by role and
 * user-specified filters, sorts, paginates, and renders the shared {@code timeline-panel} fragment.
 *
 * <p>Ref: petition050 specs §3.5.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class CaseworkerTimelineController {

  private static final String FRAGMENT_PANEL = "fragments/timeline :: timeline-panel";
  private static final String FRAGMENT_ENTRIES = "fragments/timeline :: timeline-entries";
  private static final String WARNING_PARTIAL = "timeline.warning.partial";
  private static final List<String> CASEWORKER_ROLES = List.of("CASEWORKER", "SUPERVISOR", "ADMIN");

  private final CaseServiceClient caseServiceClient;
  private final PaymentServiceClient paymentServiceClient;
  private final CaseworkerSessionService sessionService;
  private final TimelineVisibilityProperties visibilityProperties;
  private final ExecutorService bffFetchExecutor;

  /**
   * GET /cases/{caseId}/tidslinje — renders the full timeline-panel fragment.
   *
   * <p>Called on tab reveal and on filter form submission via HTMX. Returns {@code
   * fragments/timeline :: timeline-panel}.
   */
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
      Authentication auth,
      HttpSession session,
      Model model) {
    return buildTimeline(
        caseId,
        page,
        size,
        eventCategory,
        fromDate,
        toDate,
        debtId,
        auth,
        session,
        model,
        FRAGMENT_PANEL);
  }

  /**
   * GET /cases/{caseId}/tidslinje/entries — returns entry &lt;li&gt; elements plus OOB load-more
   * container.
   *
   * <p>Called by the "Load more" HTMX button. Returns {@code fragments/timeline ::
   * timeline-entries}.
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
      Authentication auth,
      HttpSession session,
      Model model) {
    return buildTimeline(
        caseId,
        page,
        size,
        eventCategory,
        fromDate,
        toDate,
        debtId,
        auth,
        session,
        model,
        FRAGMENT_ENTRIES);
  }

  // ---------------------------------------------------------------------------
  // Internal aggregation pipeline
  // ---------------------------------------------------------------------------

  private String buildTimeline(
      UUID caseId,
      int page,
      int size,
      String[] eventCategory,
      LocalDate fromDate,
      LocalDate toDate,
      UUID debtId,
      Authentication auth,
      HttpSession session,
      Model model,
      String viewName) {

    String role = primaryRole(auth, session);
    Set<EventCategory> allowed = visibilityProperties.getAllowedCategories(role);

    // Build filter DTO (constrained to allowed categories)
    TimelineFilterDto filters =
        TimelineFilterHelper.buildFilter(eventCategory, fromDate, toDate, debtId, allowed);

    // Parallel fetch from both services (4-second join timeout)
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
      log.warn("Timeout or error waiting for timeline futures for {}: {}", caseId, e.getMessage());
      caseEvents = caseFuture.getNow(List.of());
      paymentEvents = paymentFuture.getNow(List.of());
    }

    // Map → concat → deduplicate
    List<TimelineEntryDto> caseEntries =
        caseEvents.stream().map(TimelineEntryMapper::fromCaseEvent).collect(Collectors.toList());
    List<TimelineEntryDto> paymentEntries =
        paymentEvents.stream().map(TimelineEntryMapper::fromDebtEvent).collect(Collectors.toList());
    List<TimelineEntryDto> merged =
        TimelineDeduplicator.deduplicate(
            Stream.concat(caseEntries.stream(), paymentEntries.stream())
                .collect(Collectors.toList()));

    // Role-filter → user-filter → sort
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

    // Deduplicate warnings
    List<String> deduplicatedWarnings = warnings.stream().distinct().collect(Collectors.toList());

    // Available debts for filter dropdown
    List<dk.ufst.opendebt.common.dto.CaseDebtDto> availableDebts = fetchAvailableDebts(caseId);

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

  /** Fetches the list of debts for the filter bar dropdown. Returns empty list on failure. */
  private List<dk.ufst.opendebt.common.dto.CaseDebtDto> fetchAvailableDebts(UUID caseId) {
    try {
      dk.ufst.opendebt.common.dto.CaseDto caseDto = caseServiceClient.getCase(caseId);
      if (caseDto == null || caseDto.getDebtIds() == null) {
        return List.of();
      }
      return caseDto.getDebtIds().stream()
          .map(
              id ->
                  dk.ufst.opendebt.common.dto.CaseDebtDto.builder()
                      .debtId(id)
                      .transferReference(null)
                      .build())
          .collect(Collectors.toList());
    } catch (Exception e) {
      log.debug("Could not fetch case debts for filter bar: {}", e.getMessage());
      return List.of();
    }
  }

  /**
   * Extracts the primary role from the authenticated user. Strips ROLE_ prefix. Ref: specs §3.5.
   */
  private String primaryRole(Authentication auth, HttpSession session) {
    if (auth != null) {
      for (String role : CASEWORKER_ROLES) {
        for (GrantedAuthority authority : auth.getAuthorities()) {
          String name = authority.getAuthority();
          if (name.equals(role) || name.equals("ROLE_" + role)) {
            return role;
          }
        }
      }
    }
    if (session != null) {
      CaseworkerIdentity caseworker = sessionService.getCurrentCaseworker(session);
      if (caseworker != null && CASEWORKER_ROLES.contains(caseworker.getRole())) {
        return caseworker.getRole();
      }
    }
    return "";
  }
}
