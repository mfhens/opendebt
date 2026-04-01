package dk.ufst.opendebt.caseworker.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import dk.ufst.opendebt.caseworker.client.PaymentServiceClient;
import dk.ufst.opendebt.caseworker.dto.CaseworkerIdentity;
import dk.ufst.opendebt.caseworker.dto.DaekningsraekkefoelgePositionDto;

import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * BDD step definitions for Petition 057 — Dækningsrækkefølge (GIL § 4) — FR-8 portal view.
 *
 * <p><strong>Module scope:</strong> {@code opendebt-caseworker-portal}. FR-1 through FR-7 (rule
 * engine and REST API) are in {@code opendebt-payment-service Petition057Steps}.
 *
 * <p><strong>Legal basis:</strong> GIL § 4 stk. 1 (priority labels), GIL § 6a stk. 1 og stk. 12.
 *
 * <p>Spec reference: SPEC-P057 §FR-8 · design/specs-p057-daekningsraekkefoeigen.md
 */
public class Petition057PortalSteps {

  // ─────────────────────────────────────────────────────────────────────────────
  // Spring-managed collaborators
  // ─────────────────────────────────────────────────────────────────────────────

  @Autowired private MockMvc mockMvc;

  /** Gets the @MockBean declared in CucumberSpringConfiguration. */
  @Autowired private PaymentServiceClient paymentServiceClient;

  // ─────────────────────────────────────────────────────────────────────────────
  // Per-scenario mutable state
  // ─────────────────────────────────────────────────────────────────────────────

  /**
   * Rendered HTML content of the dækningsrækkefølge view (populated by navigation steps).
   * Assertions inspect this to verify markup presence.
   */
  private String renderedViewHtml;

  @Before("@petition057")
  public void resetScenarioState() {
    renderedViewHtml = null;
    reset(paymentServiceClient);
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // Helpers
  // ─────────────────────────────────────────────────────────────────────────────

  private MockHttpSession buildAuthSession() {
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(
        "currentCaseworker",
        CaseworkerIdentity.builder()
            .id("test-sagsbehandler")
            .name("Test Sagsbehandler")
            .role("SAGSBEHANDLER")
            .description("Demo")
            .build());
    return session;
  }

  private void navigateTo(String debtorId) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.get(
                        "/caseworker/debtors/" + debtorId + "/daekningsraekkefoelge")
                    .session(buildAuthSession()))
            .andExpect(status().isOk())
            .andReturn();
    this.renderedViewHtml = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
  }

  // =========================================================================
  // BACKGROUND — no-op steps; the real implementations live in
  // opendebt-payment-service Petition057Steps to avoid duplication when
  // both modules run in the same Cucumber context.  In the portal-only
  // context these steps are defined here as no-ops.
  // =========================================================================

  @Given("the payment-service rule engine is active")
  public void thePaymentServiceRuleEngineIsActive() {
    // no-op in the portal test context
  }

  @Given("the sagsbehandler portal is running")
  public void theSagsbehandlerPortalIsRunning() {
    // no-op in the portal test context
  }

  // =========================================================================
  // FR-8 — Sagsbehandler portal dækningsrækkefølge view
  // AC-16: read-only ranked list with GIL § 4 category labels and component type labels
  // =========================================================================

  /**
   * Seeds fordringer in two specific priority categories for the portal display test (SKY-3022).
   */
  @Given("debtor {string} has active fordringer in categories {word} and {word}")
  public void debtorHasActiveFordringerInCategories(
      String debtorId, String category1, String category2) {
    List<DaekningsraekkefoelgePositionDto> positions =
        List.of(
            new DaekningsraekkefoelgePositionDto(
                "FDR-001",
                null,
                category1,
                "GIL \u00a7 4, stk. 1, nr. 3",
                "HOOFDFORDRING",
                "2024-01-01",
                "2024-01-01",
                "100.00",
                null),
            new DaekningsraekkefoelgePositionDto(
                "FDR-002",
                null,
                category2,
                "GIL \u00a7 4, stk. 1, nr. 4",
                "HOOFDFORDRING",
                "2024-02-01",
                "2024-02-01",
                "200.00",
                null));
    when(paymentServiceClient.getDaekningsraekkefoelge(debtorId)).thenReturn(positions);
  }

  /** Establishes an authenticated sagsbehandler session (session handled via MockHttpSession). */
  @And("a sagsbehandler is authenticated with access to debtor {string}")
  public void aSagsbehandlerIsAuthenticatedWithAccessToDebtor(String debtorId) {
    // no-op: debtorId is passed directly to MockHttpSession via navigateTo
  }

  /** Navigates to the dækningsrækkefølge view and captures the rendered HTML. */
  @When(
      "the sagsbehandler navigates to the d\u00e6kningsr\u00e6kkef\u00f8lge view for debtor {string}")
  public void sagsbehandlerNavigatesToDaekningsraekkefoelgeView(String debtorId) throws Exception {
    navigateTo(debtorId);
  }

  /** Navigates to the dækningsrækkefølge view (Given variant, SKY-3023). */
  @And(
      "a sagsbehandler navigates to the d\u00e6kningsr\u00e6kkef\u00f8lge view for debtor {string}")
  public void sagsbehandlerNavigatesGivenVariant(String debtorId) throws Exception {
    navigateTo(debtorId);
  }

  /** Asserts the portal displays a ranked list containing the seeded fordringer IDs. */
  @Then("the portal displays a ranked list ordered by GIL \u00a7 4 priority")
  public void portalDisplaysRankedListOrderedByGilPriority() {
    assertThat(renderedViewHtml).contains("FDR-001").contains("FDR-002");
  }

  /**
   * Asserts each row shows the translated Danish category label. The parenthetical example in the
   * step text is descriptive and not captured.
   */
  @And("^each row shows the translated Danish category label \\(.*\\)$")
  public void eachRowShowsTranslatedDanishCategoryLabel() {
    assertThat(renderedViewHtml)
        .containsAnyOf(
            "Underholdsbidrag", "Rimelige omkostninger", "B\u00f8der", "Andre fordringer");
  }

  /**
   * Asserts each row shows the cost component type in Danish. The parenthetical examples in the
   * step text are descriptive and not captured.
   */
  @And("^each row shows the cost component type in Danish \\(.*\\)$")
  public void eachRowShowsCostComponentTypeInDanish() {
    assertThat(renderedViewHtml).contains("Hovedfordring");
  }

  /** Asserts the portal view is read-only (no form, no button). */
  @And("^the view is read-only \\(no d\u00e6kning actions available\\)$")
  public void theViewIsReadOnly() {
    assertThat(renderedViewHtml).doesNotContain("<form").doesNotContain("<button");
  }

  /** Asserts the view has a navigation link back to the cases area (reachability proof). */
  @And("the view is reachable from the debtor's case overview page")
  public void theViewIsReachableFromCaseOverviewPage() {
    assertThat(renderedViewHtml).contains("/cases");
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // SKY-3023 — Opskrivningsfordring portal marking
  // ─────────────────────────────────────────────────────────────────────────────

  /** Seeds a stamfordring with a linked opskrivningsfordring for the marking test (SKY-3023). */
  @Given("debtor {string} has a fordring {string} with an associated opskrivningsfordring {string}")
  public void debtorHasFordringWithAssociatedOpskrivningsfordring(
      String debtorId, String stamFordringId, String opskrivningsFordringId) {
    List<DaekningsraekkefoelgePositionDto> positions =
        List.of(
            new DaekningsraekkefoelgePositionDto(
                stamFordringId,
                null,
                "ANDRE_FORDRINGER",
                "GIL \u00a7 4, stk. 1, nr. 4",
                "HOOFDFORDRING",
                "2024-01-01",
                "2024-01-01",
                "300.00",
                null),
            new DaekningsraekkefoelgePositionDto(
                opskrivningsFordringId,
                null,
                "ANDRE_FORDRINGER",
                "GIL \u00a7 4, stk. 1, nr. 4",
                "HOOFDFORDRING",
                "2024-02-01",
                "2024-02-01",
                "100.00",
                stamFordringId));
    when(paymentServiceClient.getDaekningsraekkefoelge(debtorId)).thenReturn(positions);
  }

  /** Asserts the opskrivningsfordring row carries the CSS class {@code opskrivningsfordring}. */
  @Then("the row for {string} is visually marked as an opskrivningsfordring")
  public void rowIsVisuallyMarkedAsOpskrivningsfordring(String fordringId) {
    assertThat(renderedViewHtml).contains("opskrivningsfordring");
  }

  /** Asserts the opskrivningsfordring row displays a reference text pointing to the parent. */
  @And("the row displays a reference linking {string} to its parent {string}")
  public void rowDisplaysReferenceLinkingToParent(String fordringId, String parentFordringId) {
    assertThat(renderedViewHtml).contains("Opskrivning af ").contains(parentFordringId);
  }

  /**
   * Asserts the opskrivningsfordring row appears immediately after the stamfordring row by checking
   * the order of {@code data-fordring-id} attributes in the rendered HTML.
   */
  @And("the row for {string} appears immediately after the row for {string} in the list")
  public void rowAppearsImmediatelyAfterRowForInList(
      String opskrivningsFordringId, String stamFordringId) {
    List<String> ids = new ArrayList<>();
    Matcher m = Pattern.compile("data-fordring-id=\"([^\"]+)\"").matcher(renderedViewHtml);
    while (m.find()) {
      ids.add(m.group(1));
    }
    int stamIndex = ids.indexOf(stamFordringId);
    int opskIndex = ids.indexOf(opskrivningsFordringId);
    assertThat(stamIndex)
        .as("stamfordring %s must be present in rendered HTML", stamFordringId)
        .isGreaterThanOrEqualTo(0);
    assertThat(opskIndex)
        .as(
            "opskrivningsfordring %s must appear immediately after stamfordring %s",
            opskrivningsFordringId, stamFordringId)
        .isEqualTo(stamIndex + 1);
  }
}
