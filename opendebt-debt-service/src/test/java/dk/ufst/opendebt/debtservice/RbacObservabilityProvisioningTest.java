package dk.ufst.opendebt.debtservice;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@DisplayName("RBAC observability provisioning")
class RbacObservabilityProvisioningTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Test
  @DisplayName("pins the Prometheus datasource UID for dashboard and alert provisioning")
  void pinsPrometheusDatasourceUid() throws IOException {
    String datasources = read(repoRoot().resolve(datasourcePath()));

    assertThat(datasources)
        .contains("uid: prometheus")
        .contains("tracesToMetrics:")
        .contains("datasourceUid: prometheus");
  }

  @Test
  @DisplayName("defines the RBAC dashboard with the required monitoring panels")
  void definesRbacDashboardPanels() throws IOException {
    JsonNode dashboard = OBJECT_MAPPER.readTree(read(repoRoot().resolve(dashboardPath())));

    assertThat(dashboard.path("uid").asText()).isEqualTo("opendebt-rbac-authorization");
    assertThat(panelTitles(dashboard))
        .contains(
            "Authorization Denial Rate by Role",
            "Authorization Denial Rate by Resource Type",
            "Authorization Check Latency",
            "Unauthorized Query Attempts",
            "Person Registry Circuit Breaker State");
    assertThat(dashboard.toString())
        .contains("authorization_denied_total")
        .contains("authorization_check_seconds_bucket")
        .contains("resilience4j_circuitbreaker_state")
        .contains("person-registry")
        .contains("increase(authorization_denied_total");
  }

  @Test
  @DisplayName("defines alert templates for RBAC denial spikes and person-registry outages")
  void definesRbacAlertTemplates() throws IOException {
    String alerts = read(repoRoot().resolve(alertPath()));

    assertThat(alerts)
        .contains("HighAuthorizationDenialRate")
        .contains("sum(rate(authorization_denied_total[5m]))")
        .contains("PersonRegistryCircuitBreakerOpen")
        .contains("resilience4j_circuitbreaker_state{name=\"person-registry\",state=\"open\"}");
  }

  private List<String> panelTitles(JsonNode dashboard) {
    List<String> titles = new ArrayList<>();
    for (JsonNode panel : dashboard.path("panels")) {
      titles.add(panel.path("title").asText());
    }
    return titles;
  }

  private String read(Path path) throws IOException {
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  private Path repoRoot() {
    Path current = Path.of("").toAbsolutePath().normalize();
    if (current.getFileName() != null && current.getFileName().toString().startsWith("opendebt-")) {
      return current.getParent();
    }
    return current;
  }

  private Path datasourcePath() {
    return Path.of("config", "grafana", "provisioning", "datasources", "datasources.yaml");
  }

  private Path dashboardPath() {
    return Path.of(
        "config", "grafana", "provisioning", "dashboards", "opendebt-rbac-authorization.json");
  }

  private Path alertPath() {
    return Path.of(
        "config", "grafana", "provisioning", "alerting", "opendebt-rbac-authorization-alerts.yaml");
  }
}
