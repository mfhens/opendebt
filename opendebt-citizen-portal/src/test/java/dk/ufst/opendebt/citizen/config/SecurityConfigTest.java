package dk.ufst.opendebt.citizen.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void landingPage_isPublic() throws Exception {
    mockMvc.perform(get("/")).andExpect(status().isOk());
  }

  @Test
  void accessibilityStatement_isPublic() throws Exception {
    mockMvc.perform(get("/was")).andExpect(status().isOk());
  }

  @Test
  void dashboard_requiresAuthentication() throws Exception {
    // Without OAuth2 configured, unauthenticated request returns 403
    mockMvc.perform(get("/dashboard")).andExpect(status().isForbidden());
  }
}
