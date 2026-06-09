package dk.ufst.opendebt.debtservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dk.ufst.opendebt.common.audit.AuditContextService;
import dk.ufst.opendebt.debtservice.config.MethodSecurityConfig;
import dk.ufst.opendebt.debtservice.service.CitizenDebtService;

@WebMvcTest(CitizenDebtController.class)
@Import(MethodSecurityConfig.class)
@ActiveProfiles("test")
class CitizenDebtControllerSecurityTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CitizenDebtService citizenDebtService;
  @MockitoBean private AuditContextService auditContextService;

  @Test
  void getDebtSummary_withoutAuthentication_returns401() throws Exception {
    // Ref: petitions/specs/petition026-specs.yaml:437-439, 455-463
    mockMvc.perform(get("/api/v1/citizen/debts")).andExpect(status().isUnauthorized());
  }

  @Test
  void getDebtSummary_openApiDeclaresBearerAuthSecurityContract() throws IOException {
    // Ref: petitions/specs/petition026-specs.yaml:437-439, 455-463
    String openApi =
        Files.readString(
            Path.of("..", "api-specs", "openapi-debt-service.yaml"), StandardCharsets.UTF_8);
    String operation = operationBlock(openApi, "/api/v1/citizen/debts");

    assertThat(operation).contains("'401':");
    assertThat(operation).contains("security:");
    assertThat(operation).contains("- bearerAuth: []");
  }

  private String operationBlock(String openApi, String path) {
    String startMarker = "  " + path + ":";
    int start = openApi.indexOf(startMarker);
    int nextPath = openApi.indexOf("\n  /", start + startMarker.length());
    int components = openApi.indexOf("\ncomponents:", start + startMarker.length());
    int end = nextPath >= 0 ? nextPath : components;
    if (end < 0) {
      end = openApi.length();
    }
    return openApi.substring(start, end);
  }
}
