package dk.ufst.opendebt.common.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.IWebExchange;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

/**
 * Thymeleaf fragment rendering tests for {@code fragments/timeline.html}. Ref: petition050
 * AC-C1-C4, AC-D4-D5, AC-E1-E3, AC-G1, AC-G3, specs §6.3.
 */
class TimelineFragmentTest {

  private SpringTemplateEngine engine;
  private WebContext context;
  private UUID caseId;

  @BeforeEach
  void setUpEngine() {
    ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("templates/");
    resolver.setSuffix(".html");
    resolver.setTemplateMode(TemplateMode.HTML);
    resolver.setCharacterEncoding("UTF-8");
    resolver.setCacheable(false);

    org.thymeleaf.messageresolver.StandardMessageResolver messageResolver =
        new org.thymeleaf.messageresolver.StandardMessageResolver();

    engine = new SpringTemplateEngine();
    engine.addTemplateResolver(resolver);
    engine.addMessageResolver(messageResolver);

    caseId = UUID.randomUUID();

    // Web context required for @{/...} URL expressions in the template
    MockServletContext servletContext = new MockServletContext();
    MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
    request.setMethod("GET");
    request.setRequestURI("/");
    MockHttpServletResponse response = new MockHttpServletResponse();

    JakartaServletWebApplication webApp =
        JakartaServletWebApplication.buildApplication(servletContext);
    IWebExchange webExchange = webApp.buildExchange(request, response);
    context = new WebContext(webExchange, Locale.forLanguageTag("da"));

    context.setVariable("entries", List.of());
    context.setVariable("page", 1);
    context.setVariable("size", 25);
    context.setVariable("hasMore", false);
    context.setVariable("totalCount", 0);
    context.setVariable("filters", new TimelineFilterDto());
    context.setVariable("warnings", List.of());
    context.setVariable("caseId", caseId);
    context.setVariable("availableDebts", List.of());
    // FIX-3/4/5: new model attributes required by updated template
    String baseUrl = "/cases/" + caseId + "/tidslinje";
    String entriesUrl = "/cases/" + caseId + "/tidslinje/entries";
    context.setVariable("timelineBaseUrl", baseUrl);
    context.setVariable("timelineEntriesUrl", entriesUrl);
    context.setVariable("loadMoreUrl", entriesUrl + "?page=2&size=25");
    context.setVariable("filterRemoveLinks", new HashMap<String, String>());
  }

  private TimelineEntryDto buildCaseEntry(LocalDateTime ts) {
    String key = "CASE_CREATED|CASE|" + ts.truncatedTo(java.time.temporal.ChronoUnit.MINUTES);
    return TimelineEntryDto.builder()
        .id(UUID.randomUUID())
        .timestamp(ts)
        .eventCategory(EventCategory.CASE)
        .eventType("CASE_CREATED")
        .title("timeline.event.title.CASE_CREATED")
        .description("Case opened for debtor")
        .source(TimelineSource.CASE)
        .dedupeKey(key)
        .build();
  }

  private TimelineEntryDto buildFinancialEntry(LocalDateTime ts, BigDecimal amount) {
    UUID debtId = UUID.randomUUID();
    String key =
        "PAYMENT_RECEIVED|" + debtId + "|" + ts.truncatedTo(java.time.temporal.ChronoUnit.MINUTES);
    return TimelineEntryDto.builder()
        .id(UUID.randomUUID())
        .timestamp(ts)
        .eventCategory(EventCategory.FINANCIAL)
        .eventType("PAYMENT_RECEIVED")
        .title("timeline.event.title.PAYMENT_RECEIVED")
        .amount(amount)
        .debtId(debtId)
        .source(TimelineSource.PAYMENT)
        .dedupeKey(key)
        .build();
  }

  @Test
  @DisplayName(
      "Scenario: Timeline UI renders required fields — timestamp, category badge, title, description present")
  void fragment_nonFinancialEntry_rendersAllRequiredFields() {
    LocalDateTime ts = LocalDateTime.of(2026, 1, 15, 10, 30, 0);
    context.setVariable("entries", List.of(buildCaseEntry(ts)));
    String html = engine.process("fragments/timeline", Set.of("timeline-panel"), context);
    assertThat(html).contains("15-01-2026 10:30");
    assertThat(html).contains("skat-badge--timeline-case");
    assertThat(html).contains("Case opened for debtor");
  }

  @Test
  @DisplayName(
      "Scenario: Financial entries display formatted amount — 1234.56 renders as '1.234,56 DKK'")
  void fragment_financialEntryWithAmount_rendersFormattedDkk() {
    LocalDateTime ts = LocalDateTime.of(2026, 2, 1, 10, 0, 0);
    context.setVariable("entries", List.of(buildFinancialEntry(ts, new BigDecimal("1234.56"))));
    String html = engine.process("fragments/timeline", Set.of("timeline-panel"), context);
    assertThat(html).contains("1.234,56 DKK");
  }

  @Test
  @DisplayName("Scenario: Non-financial entries do not render any DKK amount string")
  void fragment_nullAmount_noDkkStringRendered() {
    LocalDateTime ts = LocalDateTime.of(2026, 1, 15, 10, 30, 0);
    context.setVariable("entries", List.of(buildCaseEntry(ts)));
    String html = engine.process("fragments/timeline", Set.of("timeline-panel"), context);
    assertThat(html).doesNotContain(" DKK");
  }

  @Test
  @DisplayName("Scenario: Debt-linked entries display the debt reference as a clickable link")
  void fragment_debtLinkedEntry_rendersClickableLink() {
    LocalDateTime ts = LocalDateTime.of(2026, 2, 1, 10, 0, 0);
    UUID debtId = UUID.fromString("d2010000-0000-0000-0000-000000000001");
    String key =
        "PAYMENT_RECEIVED|" + debtId + "|" + ts.truncatedTo(java.time.temporal.ChronoUnit.MINUTES);
    TimelineEntryDto entry =
        TimelineEntryDto.builder()
            .id(UUID.randomUUID())
            .timestamp(ts)
            .eventCategory(EventCategory.FINANCIAL)
            .eventType("PAYMENT_RECEIVED")
            .title("timeline.event.title.PAYMENT_RECEIVED")
            .amount(new BigDecimal("100"))
            .debtId(debtId)
            .source(TimelineSource.PAYMENT)
            .dedupeKey(key)
            .build();
    context.setVariable("entries", List.of(entry));
    String html = engine.process("fragments/timeline", Set.of("timeline-panel"), context);
    assertThat(html).contains("<a");
    assertThat(html).contains(debtId.toString());
  }

  @Test
  @DisplayName(
      "Scenario: Entries are visually categorised with badges — each category has correct CSS class")
  void fragment_entriesWithDifferentCategories_renderCategoryBadgesWithCorrectCssClasses() {
    LocalDateTime ts = LocalDateTime.of(2026, 2, 1, 10, 0, 0);
    TimelineEntryDto caseEntry = buildCaseEntry(ts);
    TimelineEntryDto financialEntry = buildFinancialEntry(ts.plusHours(1), new BigDecimal("500"));
    String collKey =
        "COLLECTION_MEASURE_INITIATED|CASE|"
            + ts.plusHours(2).truncatedTo(java.time.temporal.ChronoUnit.MINUTES);
    TimelineEntryDto collectionEntry =
        TimelineEntryDto.builder()
            .id(UUID.randomUUID())
            .timestamp(ts.plusHours(2))
            .eventCategory(EventCategory.COLLECTION)
            .eventType("COLLECTION_MEASURE_INITIATED")
            .title("timeline.event.title.COLLECTION_MEASURE_INITIATED")
            .source(TimelineSource.CASE)
            .dedupeKey(collKey)
            .build();

    context.setVariable("entries", List.of(caseEntry, financialEntry, collectionEntry));
    String html = engine.process("fragments/timeline", Set.of("timeline-panel"), context);
    assertThat(html).contains("skat-badge--timeline-case");
    assertThat(html).contains("skat-badge--timeline-financial");
    assertThat(html).contains("skat-badge--timeline-collection");
  }

  @Test
  @DisplayName("Scenario: Empty case shows no-events message 'Ingen haendelser registreret'")
  void fragment_emptyEntriesList_showsEmptyStateMessage() {
    context.setVariable("entries", List.of());
    String html = engine.process("fragments/timeline", Set.of("timeline-panel"), context);
    assertThat(html).contains("Ingen h");
    // The full text is "Ingen hændelser registreret" from messages_da.properties
  }

  @Test
  @DisplayName("Scenario: Load more button is visible when hasMore=true")
  void fragment_hasMoreTrue_loadMoreButtonPresent() {
    context.setVariable("hasMore", true);
    String html = engine.process("fragments/timeline", Set.of("timeline-panel"), context);
    assertThat(html).containsPattern("<button[^>]+skat-btn--secondary[^>]*>");
  }

  @Test
  @DisplayName("Scenario: Load more button is hidden when hasMore=false")
  void fragment_hasMoreFalse_loadMoreButtonAbsent() {
    context.setVariable("hasMore", false);
    String html = engine.process("fragments/timeline", Set.of("timeline-panel"), context);
    assertThat(html).doesNotContain("hx-target=\"#timeline-entries\"");
  }

  @Test
  @DisplayName("AC-E3: timeline-entries fragment includes OOB load-more-container div")
  void fragment_entriesFragment_containsOobLoadMoreContainer() {
    context.setVariable("hasMore", false);
    String entriesHtml = engine.process("fragments/timeline", Set.of("timeline-entries"), context);
    assertThat(entriesHtml).contains("id=\"load-more-container\"");
  }

  @Test
  @DisplayName("Scenario: Partial data warning banner shown when warnings list is non-empty")
  void fragment_warningsList_showsWarningBanner() {
    context.setVariable("warnings", List.of("timeline.warning.partial"));
    String html = engine.process("fragments/timeline", Set.of("timeline-panel"), context);
    assertThat(html).contains("skat-alert--warning");
  }

  @Test
  @DisplayName("Scenario: No warning banner shown when warnings list is empty")
  void fragment_emptyWarningsList_noWarningBannerRendered() {
    context.setVariable("warnings", List.of());
    String html = engine.process("fragments/timeline", Set.of("timeline-panel"), context);
    assertThat(html).doesNotContain("skat-alert--warning");
  }

  @Test
  @DisplayName(
      "Scenario: Active filter chips rendered — chip present for active FINANCIAL category filter")
  void fragment_activeFinancialCategoryFilter_rendersFilterChip() {
    TimelineFilterDto filters = new TimelineFilterDto();
    filters.getEventCategories().add(EventCategory.FINANCIAL);
    context.setVariable("filters", filters);
    // Provide the filterRemoveLinks map for the FINANCIAL category (FIX-4)
    Map<String, String> removeLinks = new HashMap<>();
    removeLinks.put("FINANCIAL", "/cases/" + caseId + "/tidslinje");
    context.setVariable("filterRemoveLinks", removeLinks);
    String html = engine.process("fragments/timeline", Set.of("timeline-panel"), context);
    assertThat(html).contains("skat-chip");
  }

  @Test
  @DisplayName("Scenario: No filter chips rendered when no filters are active")
  void fragment_noActiveFilters_noChipsRendered() {
    context.setVariable("filters", new TimelineFilterDto());
    String html = engine.process("fragments/timeline", Set.of("timeline-panel"), context);
    assertThat(html).doesNotContain("skat-timeline-chips");
  }
}
