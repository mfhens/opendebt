package dk.ufst.opendebt.caseworker.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import dk.ufst.opendebt.caseworker.dto.CaseworkerIdentity;
import dk.ufst.opendebt.caseworker.limitation.DebtServiceLimitationClient;
import dk.ufst.opendebt.caseworker.limitation.FordringskompleksMemberListData;
import dk.ufst.opendebt.caseworker.limitation.LimitationPanelData;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class Petition059PortalSteps {

  @Autowired private MockMvc mockMvc;

  @Autowired private DebtServiceLimitationClient debtServiceLimitationClient;

  private final Map<String, LimitationPanelData> limitationStates = new HashMap<>();
  private final Map<UUID, List<UUID>> complexMembers = new HashMap<>();

  private String renderedViewHtml;
  private String currentRole;

  @Before("@petition059")
  public void resetScenarioState() {
    limitationStates.clear();
    complexMembers.clear();
    renderedViewHtml = null;
    currentRole = "CASEWORKER";
    reset(debtServiceLimitationClient);
  }

  @Given("en sagsbehandler er autentificeret med rollen {string}")
  public void caseworkerAuthenticatedWithRole(String role) {
    currentRole = role;
  }

  @Given("en sagsbehandler er autentificeret med læseadgang til fordringen {string}")
  public void caseworkerAuthenticatedReadOnly(String alias) {
    currentRole = "READ_ONLY";
    ensureState(alias);
  }

  @Given("fordringen {string} har følgende forældelsesstatus:")
  public void claimHasLimitationStatus(String alias, DataTable table) {
    LimitationPanelData data = ensureState(alias);
    for (List<String> row : table.asLists(String.class)) {
      applyField(data, row.getFirst(), row.get(1));
    }
  }

  @Given("en fordring {string} har to registrerede afbrydelseshændelser")
  public void claimHasTwoInterruptions(String alias) {
    LimitationPanelData data = ensureState(alias);
    data.setStatus("ACTIVE");
    data.setAfbrydelseHistory(
        List.of(
            LimitationPanelData.AfbrydelsePanelRow.builder()
                .type("MODREGNING")
                .eventDate(LocalDate.of(2024, 1, 15))
                .legalReference("G.A.2.4.2")
                .newFristExpires(LocalDate.of(2027, 1, 15))
                .build(),
            LimitationPanelData.AfbrydelsePanelRow.builder()
                .type("BEROSTILLELSE")
                .eventDate(LocalDate.of(2024, 5, 20))
                .legalReference("G.A.2.4.2")
                .newFristExpires(LocalDate.of(2027, 5, 20))
                .build()));
  }

  @Given("fordringen {string} er medlem af kompleks {string} med medlemmet {string}")
  public void claimIsComplexMember(String alias, String complexAlias, String memberAlias) {
    LimitationPanelData data = ensureState(alias);
    data.setKompleksId(complexId(complexAlias));
    data.setMemberFordringIds(List.of(claimId(alias), claimId(memberAlias)));
    complexMembers.put(data.getKompleksId(), List.of(claimId(alias), claimId(memberAlias)));
  }

  @Given("fordringen {string} har en registreret tillægsfrist")
  public void claimHasRegisteredExtension(String alias) {
    LimitationPanelData data = ensureState(alias);
    data.setTillaegsfristHistory(
        List.of(
            LimitationPanelData.TillaegsfristPanelRow.builder()
                .type("AFVENTER_KLAGE")
                .appliedDate(LocalDate.of(2024, 3, 1))
                .extensionYears(1)
                .newFristExpires(LocalDate.of(2028, 3, 1))
                .build()));
  }

  @Given("fordringen {string} har en propageret afbrydelseshændelse med {string} = {string}")
  public void claimHasPropagatedInterruption(String alias, String field, String referencedAlias) {
    LimitationPanelData data = ensureState(alias);
    List<LimitationPanelData.AfbrydelsePanelRow> history =
        data.getAfbrydelseHistory() == null
            ? new ArrayList<>()
            : new ArrayList<>(data.getAfbrydelseHistory());
    LimitationPanelData.AfbrydelsePanelRow row =
        history.isEmpty()
            ? LimitationPanelData.AfbrydelsePanelRow.builder()
                .type("PROPAGERET")
                .eventDate(LocalDate.of(2024, 3, 15))
                .legalReference("G.A.2.4.2")
                .newFristExpires(LocalDate.of(2027, 3, 15))
                .build()
            : history.getFirst();
    if ("sourceFordringId".equals(field)) {
      row.setSourceFordringId(claimId(referencedAlias));
    }
    if ("targetFordringId".equals(field)) {
      row.setTargetFordringId(claimId(referencedAlias));
    }
    if (history.isEmpty()) {
      history.add(row);
    }
    data.setAfbrydelseHistory(history);
  }

  @Given("en fordring {string} har status {string}")
  public void claimHasStatus(String alias, String status) {
    ensureState(alias).setStatus(status);
  }

  @Given("fordringen {string} har status {string}")
  public void fordringenHarStatus(String alias, String status) {
    claimHasStatus(alias, status);
  }

  @Given("sagsbehandleren har rolle {string} med skriveadgang")
  public void caseworkerHasWriteAccess(String role) {
    currentRole = role;
  }

  @Given("den seneste indsigelse blev vurderet som {string} med rationale {string}")
  public void latestObjectionEvaluated(String outcome, String rationale) {
    LimitationPanelData data = ensureState(lastAlias());
    data.setObjectionRationale(rationale);
    if ("VALID".equals(outcome)) {
      data.setStatus("FORAELDET");
    }
  }

  @When("sagsbehandleren navigerer til detaljevisningen for fordringen {string}")
  public void navigateToDebtDetail(String alias) throws Exception {
    LimitationPanelData data = ensureState(alias);
    when(debtServiceLimitationClient.getLimitationStatus(claimId(alias))).thenReturn(data);
    if (data.getKompleksId() != null) {
      when(debtServiceLimitationClient.getClaimComplexMembers(data.getKompleksId()))
          .thenReturn(
              FordringskompleksMemberListData.builder()
                  .kompleksId(data.getKompleksId())
                  .memberFordringIds(
                      complexMembers.getOrDefault(
                          data.getKompleksId(), data.getMemberFordringIds()))
                  .build());
    }
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.get(
                        "/cases/"
                            + caseId(alias)
                            + "/debts/"
                            + claimId(alias)
                            + "/limitation-panel")
                    .session(buildAuthSession()))
            .andExpect(status().isOk())
            .andReturn();
    renderedViewHtml = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
  }

  @Then("vises forældelsesstatus-panelet med:")
  public void statusPanelShownWith(DataTable table) {
    assertThat(renderedViewHtml).contains("data-testid=\"limitation-status-panel\"");
    for (List<String> row : table.asLists(String.class)) {
      assertThat(renderedViewHtml).contains(row.getFirst()).contains(row.get(1));
    }
  }

  @Then("vises afbrydelseshistorik-tabellen med {int} rækker i kronologisk rækkefølge")
  public void interruptionHistoryShown(int rows) {
    assertThat(renderedViewHtml.split("data-afbrydelse-row=\"true\"", -1).length - 1)
        .isEqualTo(rows);
    assertThat(renderedViewHtml.indexOf("2024-01-15"))
        .isLessThan(renderedViewHtml.indexOf("2024-05-20"));
  }

  @And("indeholder hver række type, dato, juridisk reference og resulting new frist")
  public void interruptionHistoryContainsColumns() {
    assertThat(renderedViewHtml)
        .contains("Type")
        .contains("Dato")
        .contains("Juridisk reference")
        .contains("Resulting new frist")
        .contains("MODREGNING")
        .contains("BEROSTILLELSE");
  }

  @Then("vises afsnittet {string} i panelet")
  public void sectionShown(String section) {
    assertThat(renderedViewHtml).contains(section);
  }

  @And("listes fordringen {string} som medlem af komplekset")
  public void complexMemberListed(String alias) {
    assertThat(renderedViewHtml).contains(claimId(alias).toString());
  }

  @And("vises tillægsfristhistorikken med type, dato, extension og ny frist")
  public void extensionHistoryShown() {
    assertThat(renderedViewHtml)
        .contains("Tillægsfristhistorik")
        .contains("Type")
        .contains("Dato")
        .contains("Extension")
        .contains("Ny frist")
        .contains("AFVENTER_KLAGE")
        .contains("2024-03-01")
        .contains("2028-03-01");
  }

  @And("vises {string} = {string} for den propagerede afbrydelseshændelse")
  public void propagatedInterruptionFieldShown(String field, String alias) {
    assertThat(renderedViewHtml).contains(field).contains(claimId(alias).toString());
  }

  @Then("vises knappen {string}")
  public void buttonShown(String label) {
    assertThat(renderedViewHtml).contains(label);
  }

  @Then("vises evalueringsformularen med valg for {string} og {string}")
  public void evaluationFormShown(String validLabel, String invalidLabel) {
    assertThat(renderedViewHtml).contains(validLabel).contains(invalidLabel);
  }

  @And("vises et tekstfelt til {string}")
  public void textFieldShown(String fieldName) {
    assertThat(renderedViewHtml).contains("textarea").contains(fieldName);
  }

  @And("er registreringsknappen ikke tilgængelig")
  public void registerButtonNotAvailable() {
    assertThat(renderedViewHtml).doesNotContain("Registrer forældelsesindsigelse");
  }

  @Then("vises udfaldet {string}")
  public void outcomeShown(String outcome) {
    assertThat(renderedViewHtml).contains("Udfald").contains(outcome);
  }

  @And("vises rationalet {string}")
  public void rationaleShown(String rationale) {
    assertThat(renderedViewHtml).contains("Rationale").contains(rationale);
  }

  @And("vises knappen {string} ikke")
  public void buttonNotShown(String label) {
    assertThat(renderedViewHtml).doesNotContain(label);
  }

  @Then("vises forældelsesstatus-panelet")
  public void statusPanelShown() {
    assertThat(renderedViewHtml).contains("data-testid=\"limitation-status-panel\"");
  }

  @And("kan sagsbehandleren ikke registrere afbrydelseshændelser")
  public void noInterruptionActionsAvailable() {
    assertThat(renderedViewHtml).doesNotContain("Registrer afbrydelseshændelse");
  }

  @And("kan sagsbehandleren ikke registrere eller evaluere indsigelser")
  public void noObjectionActionsAvailable() {
    assertThat(renderedViewHtml)
        .doesNotContain("Registrer forældelsesindsigelse")
        .doesNotContain("textarea")
        .doesNotContain("value=\"VALID\"")
        .doesNotContain("value=\"INVALID\"");
  }

  private LimitationPanelData ensureState(String alias) {
    return limitationStates.computeIfAbsent(
        alias,
        key ->
            LimitationPanelData.builder()
                .fordringId(claimId(key))
                .status("ACTIVE")
                .currentFristExpires(LocalDate.of(2027, 1, 1))
                .isInUdskydelse(false)
                .memberFordringIds(List.of())
                .afbrydelseHistory(List.of())
                .tillaegsfristHistory(List.of())
                .build());
  }

  private void applyField(LimitationPanelData data, String field, String value) {
    switch (field) {
      case "status" -> data.setStatus(value);
      case "currentFristExpires" -> data.setCurrentFristExpires(LocalDate.parse(value));
      case "udskydelseDato" -> data.setUdskydelseDato(LocalDate.parse(value));
      case "isInUdskydelse" -> data.setIsInUdskydelse(Boolean.parseBoolean(value));
      default -> throw new IllegalArgumentException("Unknown field: " + field);
    }
  }

  private MockHttpSession buildAuthSession() {
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(
        "currentCaseworker",
        CaseworkerIdentity.builder()
            .id("petition059-caseworker")
            .name("Petition059 Caseworker")
            .role(currentRole)
            .description("BDD session")
            .build());
    return session;
  }

  private String lastAlias() {
    return limitationStates.keySet().stream().reduce((first, second) -> second).orElseThrow();
  }

  private UUID claimId(String alias) {
    return UUID.nameUUIDFromBytes(("fordring-" + alias).getBytes(StandardCharsets.UTF_8));
  }

  private UUID complexId(String alias) {
    return UUID.nameUUIDFromBytes(("complex-" + alias).getBytes(StandardCharsets.UTF_8));
  }

  private UUID caseId(String alias) {
    return UUID.nameUUIDFromBytes(("case-" + alias).getBytes(StandardCharsets.UTF_8));
  }
}
