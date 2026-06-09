package dk.ufst.opendebt.caseworker.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import dk.ufst.opendebt.caseworker.dto.CaseworkerIdentity;
import dk.ufst.opendebt.caseworker.section50.PortalSection50DecisionSnapshotDto;
import dk.ufst.opendebt.caseworker.section50.PortalSection50WorklistDto;
import dk.ufst.opendebt.caseworker.section50.PortalSection50WorklistEntryDto;
import dk.ufst.opendebt.caseworker.section50.Section50WorklistClient;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class Petition060PortalSteps {

  @Autowired private MockMvc mockMvc;

  @Autowired private Section50WorklistClient section50WorklistClient;

  private UUID debtorId;
  private UUID worklistId;
  private String renderedViewHtml;
  private PortalSection50DecisionSnapshotDto decisionSnapshot;
  private final List<PortalSection50WorklistEntryDto> entries = new ArrayList<>();
  private final List<String> snapshotPrioritisationFactors = new ArrayList<>();
  private final List<String> defaultSnapshotFactors = List.of("timing", "deadline");
  private final List<String> defaultEntryFactors = List.of("urgent", "deadline");
  private final java.util.LinkedHashMap<String, String> worklistFields =
      new java.util.LinkedHashMap<>();

  @Before("@petition060")
  public void resetScenarioState() {
    debtorId = null;
    worklistId = null;
    renderedViewHtml = null;
    decisionSnapshot = null;
    entries.clear();
    snapshotPrioritisationFactors.clear();
    worklistFields.clear();
    reset(section50WorklistClient);
  }

  @Given("a caseworker is authenticated with write access")
  public void aCaseworkerIsAuthenticatedWithWriteAccess() {
    // session is created in buildAuthSession()
  }

  @Given("debtor {string} has a stored section 50 worklist {string} with:")
  public void debtorHasStoredWorklistWith(
      String debtorIdValue, String worklistIdValue, DataTable table) {
    debtorId = UUID.fromString(debtorIdValue);
    worklistId = UUID.fromString(worklistIdValue);
    for (List<String> row : table.asLists(String.class)) {
      worklistFields.put(row.getFirst(), row.get(1));
    }
  }

  @And("the worklist contains these ranked entries:")
  public void theWorklistContainsTheseRankedEntries(DataTable table) {
    for (Map<String, String> row : table.asMaps(String.class, String.class)) {
      entries.add(
          new PortalSection50WorklistEntryDto(
              Integer.parseInt(row.get("rank")),
              row.get("claimId"),
              row.get("itemType"),
              row.get("claimCategory"),
              Boolean.parseBoolean(row.get("suspectedDataError")),
              Boolean.parseBoolean(row.get("confirmedRetskraft")),
              Boolean.parseBoolean(row.get("withinAmountWindow")),
              row.get("selectionReason"),
              parseFactors(row.get("prioritisationFactors"), defaultEntryFactors),
              blankToNull(row.get("suppressedReason")),
              new BigDecimal(row.get("amount"))));
    }
  }

  @And("the decision snapshot contains:")
  public void theDecisionSnapshotContains(DataTable table) {
    java.util.LinkedHashMap<String, String> values = new java.util.LinkedHashMap<>();
    for (List<String> row : table.asLists(String.class)) {
      values.put(row.getFirst(), row.get(1));
    }
    snapshotPrioritisationFactors.clear();
    snapshotPrioritisationFactors.addAll(
        parseFactors(values.get("prioritisationFactors"), defaultSnapshotFactors));
    decisionSnapshot =
        new PortalSection50DecisionSnapshotDto(
            UUID.fromString(
                values.getOrDefault("decisionId", "06000000-0000-0000-0000-000000000061")),
            worklistId,
            values.get("rulePath"),
            values.get("inputHash"),
            values.get("selectedNextItemId"),
            values.get("legalReference"),
            UUID.fromString(
                values.getOrDefault("auditEventId", "06000000-0000-0000-0000-000000000062")),
            values.get("origin"),
            Instant.parse(values.get("occurredAt")),
            values.get("notes"),
            List.copyOf(snapshotPrioritisationFactors));
  }

  @When("the caseworker opens the petition060 worklist page")
  public void theCaseworkerOpensThePetition060WorklistPage() throws Exception {
    when(section50WorklistClient.getWorklist(debtorId, worklistId)).thenReturn(buildWorklist());
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.get(
                        "/debtors/" + debtorId + "/retskraft-worklists/" + worklistId)
                    .session(buildAuthSession()))
            .andExpect(status().isOk())
            .andReturn();
    renderedViewHtml = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
  }

  @Then("the page shows the override reason {string}")
  public void thePageShowsTheOverrideReason(String overrideReason) {
    assertThat(renderedViewHtml)
        .contains("data-testid=\"section50-override\"")
        .contains(overrideReason);
  }

  @And("the page shows the deviation reason {string}")
  public void thePageShowsTheDeviationReason(String deviationReason) {
    assertThat(renderedViewHtml)
        .contains("data-testid=\"section50-modregning\"")
        .contains(deviationReason);
  }

  @And("the page shows the modregning outcome {string}")
  public void thePageShowsTheModregningOutcome(String modregningOutcome) {
    assertThat(renderedViewHtml)
        .contains("data-testid=\"section50-modregning\"")
        .contains(modregningOutcome);
  }

  @And("the page shows the decision rule path {string}")
  public void thePageShowsTheDecisionRulePath(String rulePath) {
    assertThat(renderedViewHtml)
        .contains("data-testid=\"section50-decision-snapshot\"")
        .contains(rulePath);
  }

  @And("the page shows the technical identifier {string}")
  public void thePageShowsTheTechnicalIdentifier(String identifier) {
    assertThat(renderedViewHtml)
        .contains("data-testid=\"section50-worklist\"")
        .contains(identifier)
        .doesNotContain("Anna Jensen")
        .doesNotContain("0101701234");
  }

  private PortalSection50WorklistDto buildWorklist() {
    return new PortalSection50WorklistDto(
        worklistId,
        debtorId,
        worklistFields.get("orderingMode"),
        worklistFields.get("legalReference"),
        worklistFields.get("contextType"),
        parseBigDecimal(worklistFields.get("amountWindow")),
        Instant.parse(worklistFields.getOrDefault("generatedAt", "2026-05-27T08:15:00Z")),
        worklistFields.get("selectedNextItemId"),
        blankToNull(worklistFields.get("overrideReason")),
        blankToNull(worklistFields.get("overrideLegalBasis")),
        blankToNull(worklistFields.get("deviationReason")),
        blankToNull(worklistFields.get("modregningOutcome")),
        List.copyOf(entries),
        decisionSnapshot);
  }

  private BigDecimal parseBigDecimal(String value) {
    return value == null || value.isBlank() ? null : new BigDecimal(value);
  }

  private List<String> parseFactors(String value, List<String> defaultValue) {
    if (value == null) {
      return defaultValue;
    }
    if (value.isBlank()) {
      return List.of();
    }
    return java.util.Arrays.stream(value.split(","))
        .map(String::trim)
        .filter(part -> !part.isEmpty())
        .toList();
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private MockHttpSession buildAuthSession() {
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(
        "currentCaseworker",
        CaseworkerIdentity.builder()
            .id("petition060-caseworker")
            .name("Petition060 Caseworker")
            .role("CASEWORKER")
            .description("BDD session")
            .build());
    return session;
  }
}
