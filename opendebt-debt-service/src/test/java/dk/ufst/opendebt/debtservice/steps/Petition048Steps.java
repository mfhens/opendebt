package dk.ufst.opendebt.debtservice.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dk.ufst.opendebt.common.security.AuthContext;
import dk.ufst.opendebt.common.security.CreditorAccessChecker;
import dk.ufst.opendebt.common.security.DebtorAccessChecker;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class Petition048Steps {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Autowired private DebtRepository debtRepository;
  @Autowired private DebtorAccessChecker debtorAccessChecker;
  @Autowired private CreditorAccessChecker creditorAccessChecker;

  private UUID personA;
  private UUID personB;
  private UUID orgA;
  private UUID orgB;
  private UUID debtOwnedByPersonA;
  private UUID debtOwnedByPersonB;
  private UUID claimOwnedByOrgA;
  private UUID claimOwnedByOrgB;

  private AuthContext authContext;
  private String upstreamClaimedPerson;
  private Boolean lastAccessDecision;
  private JsonNode rbacDashboard;
  private String rbacAlertRules;

  @Before
  public void resetScenarioState() {
    debtRepository.deleteAll();
    personA = UUID.randomUUID();
    personB = UUID.randomUUID();
    orgA = UUID.randomUUID();
    orgB = UUID.randomUUID();
    upstreamClaimedPerson = null;
    authContext = null;
    lastAccessDecision = null;
    rbacDashboard = null;
    rbacAlertRules = null;
  }

  @Given("RBAC test debts are seeded for multiple debtors and creditor organizations")
  public void seedRbacDebtFixtures() {
    debtOwnedByPersonA = createDebt(personA, orgA, "RBAC-A-1");
    debtOwnedByPersonB = createDebt(personB, orgB, "RBAC-B-1");

    // Claims are represented by debt records in this service.
    claimOwnedByOrgA = debtOwnedByPersonA;
    claimOwnedByOrgB = debtOwnedByPersonB;
  }

  @Given("a citizen auth context for debtor person A")
  public void citizenAuthForPersonA() {
    authContext =
        AuthContext.builder()
            .userId("citizen-a")
            .personId(personA)
            .roles(Set.of("CITIZEN"))
            .build();
  }

  @Given("a citizen auth context for debtor person B")
  public void citizenAuthForPersonB() {
    authContext =
        AuthContext.builder()
            .userId("citizen-b")
            .personId(personB)
            .roles(Set.of("CITIZEN"))
            .build();
  }

  @Given("a creditor auth context for organization A")
  public void creditorAuthForOrgA() {
    authContext =
        AuthContext.builder()
            .userId("creditor-a")
            .organizationId(orgA)
            .roles(Set.of("CREDITOR"))
            .build();
  }

  @Given("an admin auth context")
  public void adminAuthContext() {
    authContext = AuthContext.builder().userId("admin-1").roles(Set.of("ADMIN")).build();
  }

  @Given("upstream case-service context claims debtor person A")
  public void upstreamClaimsPersonA() {
    upstreamClaimedPerson = personA.toString();
  }

  @When("the citizen requests access to a debt owned by person A")
  public void citizenRequestsDebtOwnedByPersonA() {
    lastAccessDecision = debtorAccessChecker.canAccessDebt(debtOwnedByPersonA, authContext);
  }

  @When("the citizen requests access to a debt owned by person B")
  public void citizenRequestsDebtOwnedByPersonB() {
    lastAccessDecision = debtorAccessChecker.canAccessDebt(debtOwnedByPersonB, authContext);
  }

  @When("the creditor requests access to a claim owned by organization A")
  public void creditorRequestsClaimOwnedByOrgA() {
    lastAccessDecision = creditorAccessChecker.canAccessClaim(claimOwnedByOrgA, authContext);
  }

  @When("the creditor requests access to a claim owned by organization B")
  public void creditorRequestsClaimOwnedByOrgB() {
    lastAccessDecision = creditorAccessChecker.canAccessClaim(claimOwnedByOrgB, authContext);
  }

  @When("debt-service re-validates access to a debt owned by person A")
  public void debtServiceRevalidatesDebtAccessForPersonA() {
    // Upstream claims are intentionally ignored; debt-service re-validates from its own data.
    assertThat(upstreamClaimedPerson).isEqualTo(personA.toString());
    lastAccessDecision = debtorAccessChecker.canAccessDebt(debtOwnedByPersonA, authContext);
  }

  @When("the admin requests access to a debt owned by person B")
  public void adminRequestsDebtOwnedByPersonB() {
    lastAccessDecision = debtorAccessChecker.canAccessDebt(debtOwnedByPersonB, authContext);
  }

  @When("the admin requests access to a claim owned by organization B")
  public void adminRequestsClaimOwnedByOrgB() {
    lastAccessDecision = creditorAccessChecker.canAccessClaim(claimOwnedByOrgB, authContext);
  }

  @When("operators inspect the RBAC Grafana dashboard template")
  public void operatorsInspectTheRbacGrafanaDashboardTemplate() throws IOException {
    rbacDashboard =
        OBJECT_MAPPER.readTree(
            Files.readString(
                resolveRepoRoot().resolve(rbcaDashboardPath()), StandardCharsets.UTF_8));
    rbacAlertRules =
        Files.readString(resolveRepoRoot().resolve(rbacAlertPath()), StandardCharsets.UTF_8);
  }

  @Then("debt-service should grant debt access")
  public void shouldGrantDebtAccess() {
    assertThat(lastAccessDecision).isTrue();
  }

  @Then("debt-service should deny debt access")
  public void shouldDenyDebtAccess() {
    assertThat(lastAccessDecision).isFalse();
  }

  @Then("debt-service should grant claim access")
  public void shouldGrantClaimAccess() {
    assertThat(lastAccessDecision).isTrue();
  }

  @Then("debt-service should deny claim access")
  public void shouldDenyClaimAccess() {
    assertThat(lastAccessDecision).isFalse();
  }

  @Then("the RBAC dashboard should expose denial rate panels by role and resource type")
  public void dashboardShouldExposeDenialRatePanels() {
    assertThat(findPanel("Authorization Denial Rate by Role")).isNotNull();
    assertThat(findPanel("Authorization Denial Rate by Resource Type")).isNotNull();
    assertThat(findPanel("Authorization Denial Rate by Role").toString())
        .contains("authorization_denied_total")
        .contains("role");
    assertThat(findPanel("Authorization Denial Rate by Resource Type").toString())
        .contains("authorization_denied_total")
        .contains("resource_type");
  }

  @Then("the RBAC dashboard should expose authorization latency panels for p50, p95, and p99")
  public void dashboardShouldExposeAuthorizationLatencyPanels() {
    JsonNode latencyPanel = findPanel("Authorization Check Latency");
    assertThat(latencyPanel).isNotNull();
    assertThat(latencyPanel.toString())
        .contains("histogram_quantile(0.50")
        .contains("histogram_quantile(0.95")
        .contains("histogram_quantile(0.99")
        .contains("authorization_check_seconds_bucket");
  }

  @Then("the RBAC dashboard should expose person-registry circuit breaker state")
  public void dashboardShouldExposeCircuitBreakerState() {
    JsonNode circuitBreakerPanel = findPanel("Person Registry Circuit Breaker State");
    assertThat(circuitBreakerPanel).isNotNull();
    assertThat(circuitBreakerPanel.toString())
        .contains("resilience4j_circuitbreaker_state")
        .contains("person-registry");
  }

  @Then("the RBAC dashboard should expose unauthorized query attempts")
  public void dashboardShouldExposeUnauthorizedQueryAttempts() {
    JsonNode unauthorizedPanel = findPanel("Unauthorized Query Attempts");
    assertThat(unauthorizedPanel).isNotNull();
    assertThat(unauthorizedPanel.toString())
        .contains("increase(authorization_denied_total")
        .contains("resource_type");
  }

  @Then("RBAC alert templates should cover denial spikes and circuit breaker open state")
  public void alertTemplatesShouldCoverRequiredSignals() {
    assertThat(rbacAlertRules)
        .contains("HighAuthorizationDenialRate")
        .contains("sum(rate(authorization_denied_total[5m]))")
        .contains("PersonRegistryCircuitBreakerOpen")
        .contains("resilience4j_circuitbreaker_state{name=\"person-registry\",state=\"open\"}");
  }

  private UUID createDebt(UUID debtorPersonId, UUID creditorOrgId, String reference) {
    DebtEntity debt =
        DebtEntity.builder()
            .debtorPersonId(debtorPersonId)
            .creditorOrgId(creditorOrgId)
            .debtTypeCode("MUNICIPALFEE")
            .principalAmount(BigDecimal.valueOf(1000))
            .interestAmount(BigDecimal.ZERO)
            .feesAmount(BigDecimal.ZERO)
            .outstandingBalance(BigDecimal.valueOf(1000))
            .dueDate(LocalDate.now().minusDays(7))
            .externalReference(reference)
            .status(DebtEntity.DebtStatus.ACTIVE)
            .readinessStatus(DebtEntity.ReadinessStatus.READY_FOR_COLLECTION)
            .build();
    return debtRepository.save(debt).getId();
  }

  private JsonNode findPanel(String title) {
    assertThat(rbacDashboard).as("RBAC dashboard must be loaded").isNotNull();
    for (JsonNode panel : rbacDashboard.path("panels")) {
      if (title.equals(panel.path("title").asText())) {
        return panel;
      }
    }
    return null;
  }

  private Path resolveRepoRoot() {
    Path current = Path.of("").toAbsolutePath().normalize();
    if (current.getFileName() != null && current.getFileName().toString().startsWith("opendebt-")) {
      return current.getParent();
    }
    return current;
  }

  private Path rbcaDashboardPath() {
    return Path.of(
        "config", "grafana", "provisioning", "dashboards", "opendebt-rbac-authorization.json");
  }

  private Path rbacAlertPath() {
    return Path.of(
        "config", "grafana", "provisioning", "alerting", "opendebt-rbac-authorization-alerts.yaml");
  }
}
