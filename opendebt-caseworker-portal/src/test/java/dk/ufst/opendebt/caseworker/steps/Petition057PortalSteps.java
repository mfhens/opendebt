package dk.ufst.opendebt.caseworker.steps;

import static org.assertj.core.api.Assertions.fail;

import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Failing BDD step definitions for Petition 057 — Dækningsrækkefølge (GIL § 4) — FR-8 only.
 *
 * <p><strong>Module scope:</strong> {@code opendebt-caseworker-portal}. FR-1 through FR-7 (rule
 * engine and REST API) are in {@code opendebt-payment-service Petition057Steps}.
 *
 * <p><strong>Legal basis:</strong> GIL § 4 stk. 1 (priority labels), GIL § 6a stk. 1 og stk. 12.
 *
 * <p><strong>Every step in this file actively fails (RED).</strong> These steps are executable
 * contracts for the following components that do not yet exist:
 *
 * <ul>
 *   <li>{@code DaekningsRaekkefoeigenViewController} — Spring MVC controller (FR-8)
 *   <li>{@code daekningsraekkefoelge.html} — Thymeleaf template with ranked list (FR-8)
 *   <li>{@code messages_da.properties} — i18n bundle entries for category/component labels (AC-17)
 *   <li>{@code messages_en_GB.properties} — i18n bundle entries (AC-17)
 *   <li>Navigation link from the debtor case overview page (AC-16)
 * </ul>
 *
 * <p><strong>AC-16:</strong> The sagsbehandler portal must display a read-only ranked list of
 * fordringer ordered by GIL § 4 priority, with translated Danish category labels and cost component
 * type labels. Opskrivningsfordringer must be visually marked with a reference link to their
 * stamfordring and positioned immediately after it.
 *
 * <p>Spec reference: SPEC-P057 §FR-8 · design/specs-p057-daekningsraekkefoeigen.md
 *
 * <p>Architecture: design/solution-architecture-p057-daekningsraekkefoeigen.md §3 (FR-8)
 */
public class Petition057PortalSteps {

  // ─────────────────────────────────────────────────────────────────────────────
  // Per-scenario mutable state
  // ─────────────────────────────────────────────────────────────────────────────

  /** Debtor identifier for the current scenario. */
  private String currentDebtorId;

  /**
   * Rendered HTML content of the dækningsrækkefølge view (populated by navigation steps).
   * Assertions inspect this to verify markup presence.
   */
  private String renderedViewHtml;

  @Before("@petition057")
  public void resetScenarioState() {
    currentDebtorId = null;
    renderedViewHtml = null;
  }

  // =========================================================================
  // BACKGROUND — no-op context steps (shared with payment-service)
  // =========================================================================

  @Given("the payment-service rule engine is active")
  public void thePaymentServiceRuleEngineIsActive() {
    // No-op: background context step shared with payment-service module.
    // DaekningsRaekkefoeigenService in payment-service provides data via REST client.
  }

  @Given("the sagsbehandler portal is running")
  public void theSagsbehandlerPortalIsRunning() {
    // No-op: Spring context for caseworker-portal is bootstrapped by CucumberSpringConfiguration.
    // DaekningsRaekkefoeigenViewController must be present as a @Controller bean.
  }

  // =========================================================================
  // FR-8 — Sagsbehandler portal dækningsrækkefølge view
  // AC-16: read-only ranked list with GIL § 4 category labels and component type labels
  // Components: DaekningsRaekkefoeigenViewController, daekningsraekkefoelge.html,
  //             messages_da.properties, messages_en_GB.properties
  // =========================================================================

  /**
   * Seeds fordringer in two specific priority categories for the portal display test (SKY-3022).
   *
   * <p>FR-8: AC-16
   */
  @Given("debtor {string} has active fordringer in categories {word} and {word}")
  public void debtorHasActiveFordringerInCategories(
      String debtorId, String category1, String category2) {
    // FR-8: AC-16 — seed fordringer in PrioritetKategori.valueOf(category1) and valueOf(category2)
    // for portal display ordering verification
    fail(
        "Not implemented: FR-8/AC-16 — seed fordringer in categories "
            + category1
            + " and "
            + category2
            + " for debtor "
            + debtorId
            + ". Implement DaekningsRaekkefoeigenViewController data layer.");
  }

  /**
   * Establishes an authenticated sagsbehandler session with access to the specified debtor.
   *
   * <p>FR-8: AC-16
   */
  @And("a sagsbehandler is authenticated with access to debtor {string}")
  public void aSagsbehandlerIsAuthenticatedWithAccessToDebtor(String debtorId) {
    // FR-8: AC-16 — establish authenticated HTTP session for sagsbehandler role
    // Security: sagsbehandler must have SAGSBEHANDLER role and debtor access grant
    fail(
        "Not implemented: FR-8/AC-16 — authenticate sagsbehandler with access to debtor "
            + debtorId
            + ". Implement Spring Security test support (MockMvc with WithMockUser or TestSecurityContextHolder).");
  }

  /**
   * Navigates the sagsbehandler to the dækningsrækkefølge view for the debtor and captures the
   * rendered HTML for subsequent Then assertions.
   *
   * <p>FR-8: AC-16
   */
  @When("the sagsbehandler navigates to the dækningsrækkefølge view for debtor {string}")
  public void sagsbehandlerNavigatesToDaekningsraekkefoelgeView(String debtorId) {
    // FR-8: AC-16 — MockMvc GET /caseworker/debtors/{debtorId}/daekningsraekkefoelge
    // Store rendered Thymeleaf HTML in renderedViewHtml for assertion inspection
    fail(
        "Not implemented: FR-8/AC-16 — GET /caseworker/debtors/"
            + debtorId
            + "/daekningsraekkefoelge via MockMvc. "
            + "Implement DaekningsRaekkefoeigenViewController and daekningsraekkefoelge.html.");
  }

  /**
   * Navigates the sagsbehandler to the dækningsrækkefølge view (Given variant, SKY-3023).
   *
   * <p>FR-8: AC-16
   */
  @And("a sagsbehandler navigates to the dækningsrækkefølge view for debtor {string}")
  public void sagsbehandlerNavigatesGivenVariant(String debtorId) {
    // FR-8: AC-16 — same navigation as the When variant; Given keyword used in SKY-3023
    fail(
        "Not implemented: FR-8/AC-16 — GET /caseworker/debtors/"
            + debtorId
            + "/daekningsraekkefoelge via MockMvc (Given variant / SKY-3023).");
  }

  /**
   * Asserts the portal displays a ranked list ordered by GIL § 4 priority.
   *
   * <p>FR-8: AC-16
   */
  @Then("the portal displays a ranked list ordered by GIL § 4 priority")
  public void portalDisplaysRankedListOrderedByGilPriority() {
    // FR-8: AC-16 — daekningsraekkefoelge.html must render a ranked list
    // Inspect renderedViewHtml for ordered <ol> or ranked <table> with GIL § 4 ordering
    fail(
        "Not implemented: FR-8/AC-16 — assert rendered HTML contains ranked list ordered by GIL § 4 priority. "
            + "Implement daekningsraekkefoelge.html with Thymeleaf th:each over ordered positions.");
  }

  /**
   * Asserts each row in the portal shows the translated Danish category label. The parenthetical
   * example in the step text is descriptive and not captured.
   *
   * <p>FR-8: AC-16
   */
  @And("^each row shows the translated Danish category label \\(.*\\)$")
  public void eachRowShowsTranslatedDanishCategoryLabel() {
    // FR-8: AC-16 — daekningsraekkefoelge.html must use th:text="#{prioritetKategori.LABEL}"
    // messages_da.properties must define keys for all 5 PrioritetKategori values:
    //   daekningsraekkefoelge.kategori.RIMELIGE_OMKOSTNINGER = "Rimelige omkostninger"
    //   daekningsraekkefoelge.kategori.BOEDER_TVANGSBOEEDER_TILBAGEBETALING = "Bøder, tvangs­bøder
    // og tilbagebetaling"
    //   daekningsraekkefoelge.kategori.UNDERHOLDSBIDRAG_PRIVATRETLIG = "Underholdsbidrag —
    // privatretlig"
    //   daekningsraekkefoelge.kategori.UNDERHOLDSBIDRAG_OFFENTLIG = "Underholdsbidrag — offentlig"
    //   daekningsraekkefoelge.kategori.ANDRE_FORDRINGER = "Andre fordringer"
    fail(
        "Not implemented: FR-8/AC-16 — assert each row displays translated Danish PrioritetKategori label. "
            + "Implement messages_da.properties with daekningsraekkefoelge.kategori.* keys.");
  }

  /**
   * Asserts each row in the portal shows the cost component type in Danish. The parenthetical
   * examples in the step text are descriptive and not captured.
   *
   * <p>FR-8: AC-16
   */
  @And("^each row shows the cost component type in Danish \\(.*\\)$")
  public void eachRowShowsCostComponentTypeInDanish() {
    // FR-8: AC-16 — daekningsraekkefoelge.html must render Danish label for each RenteKomponent
    // messages_da.properties must define keys for all 6 RenteKomponent values:
    //   daekningsraekkefoelge.komponent.OPKRAEVNINGSRENTER = "Opkrævningsrenter"
    //   daekningsraekkefoelge.komponent.INDDRIVELSESRENTER_FORDRINGSHAVER_STK3 =
    // "Inddrivelsesrenter § 9, stk. 3"
    //   daekningsraekkefoelge.komponent.INDDRIVELSESRENTER_FOER_TILBAGEFOERSEL =
    // "Inddrivelsesrenter (før tilbageførsel)"
    //   daekningsraekkefoelge.komponent.INDDRIVELSESRENTER_STK1 = "Inddrivelsesrenter § 9, stk. 1"
    //   daekningsraekkefoelge.komponent.OEVRIGE_RENTER_PSRM = "Øvrige renter (PSRM)"
    //   daekningsraekkefoelge.komponent.HOOFDFORDRING = "Hovedfordring"
    fail(
        "Not implemented: FR-8/AC-16 — assert each row displays Danish RenteKomponent label. "
            + "Implement messages_da.properties with daekningsraekkefoelge.komponent.* keys.");
  }

  /**
   * Asserts the portal view is read-only (no dækning action buttons or links).
   *
   * <p>FR-8: AC-16
   */
  @And("^the view is read-only \\(no dækning actions available\\)$")
  public void theViewIsReadOnly() {
    // FR-8: AC-16 — daekningsraekkefoelge.html must NOT render any form, button, or action link
    // for applying payments. View is informational only.
    fail(
        "Not implemented: FR-8/AC-16 — assert rendered HTML contains no dækning action elements "
            + "(no <form>, no action buttons). Implement daekningsraekkefoelge.html as read-only Thymeleaf template.");
  }

  /**
   * Asserts the dækningsrækkefølge view is reachable via a navigation link from the debtor's case
   * overview page.
   *
   * <p>FR-8: AC-16
   */
  @And("the view is reachable from the debtor's case overview page")
  public void theViewIsReachableFromCaseOverviewPage() {
    // FR-8: AC-16 — the debtor case overview page (or sidebar) must include a navigation link
    // to /caseworker/debtors/{debtorId}/daekningsraekkefoelge
    fail(
        "Not implemented: FR-8/AC-16 — assert case overview page contains navigation link to daekningsraekkefoelge view. "
            + "Implement link in caseworker case overview template.");
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // SKY-3023 — Opskrivningsfordring portal marking
  // ─────────────────────────────────────────────────────────────────────────────

  /**
   * Seeds a fordring with an associated opskrivningsfordring for the portal marking test
   * (SKY-3023).
   *
   * <p>FR-8: AC-16
   */
  @Given("debtor {string} has a fordring {string} with an associated opskrivningsfordring {string}")
  public void debtorHasFordringWithAssociatedOpskrivningsfordring(
      String debtorId, String stamFordringId, String opskrivningsFordringId) {
    // FR-8: AC-16 — seed stamfordring and opskrivningsfordring with opskrivningAfFordringId linkage
    fail(
        "Not implemented: FR-8/AC-16 — seed stamfordring "
            + stamFordringId
            + " with linked opskrivningsfordring "
            + opskrivningsFordringId
            + " for debtor "
            + debtorId);
  }

  /**
   * Asserts the row for the opskrivningsfordring is visually marked (e.g., CSS class or icon).
   *
   * <p>FR-8: AC-16
   */
  @Then("the row for {string} is visually marked as an opskrivningsfordring")
  public void rowIsVisuallyMarkedAsOpskrivningsfordring(String fordringId) {
    // FR-8: AC-16 — daekningsraekkefoelge.html: row for opskrivningsfordring has CSS class
    // "opskrivningsfordring" or equivalent visual marker
    fail(
        "Not implemented: FR-8/AC-16 — assert row for "
            + fordringId
            + " has visual opskrivningsfordring marker in rendered HTML. "
            + "Implement Thymeleaf conditional CSS class: th:classappend=\"${pos.opskrivningAfFordringId != null} ? 'opskrivningsfordring'\".");
  }

  /**
   * Asserts the row displays a reference link from the opskrivningsfordring to its parent.
   *
   * <p>FR-8: AC-16
   */
  @And("the row displays a reference linking {string} to its parent {string}")
  public void rowDisplaysReferenceLinkingToParent(String fordringId, String parentFordringId) {
    // FR-8: AC-16 — daekningsraekkefoelge.html: opskrivningsfordring row contains link or label
    // referencing parentFordringId (e.g., "Opskrivning af FDR-30231")
    fail(
        "Not implemented: FR-8/AC-16 — assert row for "
            + fordringId
            + " displays reference to parent fordring "
            + parentFordringId
            + " in rendered HTML. "
            + "Implement Thymeleaf: th:text=\"'Opskrivning af ' + ${pos.opskrivningAfFordringId}\".");
  }

  /**
   * Asserts the opskrivningsfordring row appears immediately after the stamfordring row in the
   * list.
   *
   * <p>FR-8: AC-16
   */
  @And("the row for {string} appears immediately after the row for {string} in the list")
  public void rowAppearsImmediatelyAfterRowForInList(
      String opskrivningsFordringId, String stamFordringId) {
    // FR-8: AC-16 — in rendered HTML, opskrivningsFordringId row must follow stamFordringId row
    // immediately (rank(opskrivning) == rank(stamfordring) + 1)
    fail(
        "Not implemented: FR-8/AC-16 — assert row for "
            + opskrivningsFordringId
            + " appears immediately after row for "
            + stamFordringId
            + " in rendered HTML. "
            + "Implement DaekningsRaekkefoeigenService.buildOrderedList opskrivning positioning (FR-5 rule).");
  }
}
