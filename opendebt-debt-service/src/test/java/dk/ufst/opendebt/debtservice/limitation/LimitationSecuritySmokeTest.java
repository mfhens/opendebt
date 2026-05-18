package dk.ufst.opendebt.debtservice.limitation;

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
import dk.ufst.opendebt.debtservice.config.MethodSecurityConfig;
import dk.ufst.opendebt.debtservice.limitation.controller.LimitationController;
import dk.ufst.opendebt.debtservice.limitation.service.LimitationObjectionFacade;
import dk.ufst.opendebt.debtservice.limitation.service.LimitationStateApplicationService;

/**
 * Verifies that {@code @EnableMethodSecurity} is wired in debt-service and that limitation
 * endpoints enforce {@code @PreAuthorize} role checks (TB-059).
 */
@WebMvcTest(controllers = LimitationController.class)
@Import(MethodSecurityConfig.class)
class LimitationSecuritySmokeTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private LimitationStateApplicationService limitationStateApplicationService;
  @MockitoBean private LimitationObjectionFacade limitationObjectionFacade;
  @MockitoBean private AuditContextService auditContextService;

  @Test
  @WithMockUser(roles = "VIEWER")
  void foraeldelseEndpoint_forbiddenForViewer() throws Exception {
    mockMvc
        .perform(get("/api/v1/foraeldelse/{fordringId}", UUID.randomUUID()))
        .andExpect(status().isForbidden());
  }
}
