package dk.ufst.opendebt.caseservice.limitation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dk.ufst.opendebt.caseservice.config.MethodSecurityConfig;
import dk.ufst.opendebt.common.audit.AuditContextService;

/**
 * Verifies that {@code @EnableMethodSecurity} is wired in case-service and that the internal
 * limitation objection workflow endpoint enforces {@code @PreAuthorize} role checks (TB-059).
 */
@WebMvcTest(controllers = LimitationObjectionWorkflowInternalController.class)
@Import(MethodSecurityConfig.class)
class LimitationObjectionSecuritySmokeTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private LimitationObjectionWorkflowService workflowService;
  @MockitoBean private AuditContextService auditContextService;

  @Test
  @WithMockUser(roles = "VIEWER")
  void createWorkflowEndpoint_forbiddenForViewer() throws Exception {
    mockMvc
        .perform(
            post("/api/internal/v1/limitation-objections")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isForbidden());
  }
}
