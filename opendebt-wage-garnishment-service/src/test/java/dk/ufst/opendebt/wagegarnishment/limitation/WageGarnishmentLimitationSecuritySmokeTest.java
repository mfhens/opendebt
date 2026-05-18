package dk.ufst.opendebt.wagegarnishment.limitation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dk.ufst.opendebt.common.audit.AuditContextService;
import dk.ufst.opendebt.wagegarnishment.config.MethodSecurityConfig;

/**
 * Verifies that {@code @EnableMethodSecurity} is wired in wage-garnishment-service and that the
 * internal limitation facts endpoint enforces {@code @PreAuthorize} role checks (TB-059).
 */
@WebMvcTest(controllers = WageGarnishmentLimitationFactsController.class)
@Import(MethodSecurityConfig.class)
class WageGarnishmentLimitationSecuritySmokeTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private AuditContextService auditContextService;

  @Test
  @WithMockUser(roles = "VIEWER")
  void factsEndpoint_forbiddenForViewer() throws Exception {
    mockMvc
        .perform(
            get("/api/internal/v1/limitation-facts/debtors/{debtorPersonId}", UUID.randomUUID()))
        .andExpect(status().isForbidden());
  }
}
