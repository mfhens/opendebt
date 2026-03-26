package dk.ufst.opendebt.caseworker.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import dk.ufst.opendebt.caseworker.dto.CaseworkerIdentity;
import dk.ufst.opendebt.caseworker.service.CaseworkerSessionService;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

  @Mock private CaseworkerSessionService sessionService;
  @InjectMocks private DashboardController controller;

  private final MockHttpSession session = new MockHttpSession();
  private final Model model = new ExtendedModelMap();

  @Test
  void index_whenNoSession_redirectsToLogin() {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(null);

    String view = controller.index(session, model);

    assertThat(view).isEqualTo("redirect:/demo-login");
  }

  @Test
  void index_whenLoggedIn_redirectsToCases() {
    CaseworkerIdentity cw = caseworker("CASEWORKER");
    when(sessionService.getCurrentCaseworker(session)).thenReturn(cw);

    String view = controller.index(session, model);

    assertThat(view).isEqualTo("redirect:/cases");
    assertThat(model.asMap()).containsKey("caseworker");
  }

  private CaseworkerIdentity caseworker(String role) {
    return CaseworkerIdentity.builder().id("u1").name("Test User").role(role).build();
  }
}
