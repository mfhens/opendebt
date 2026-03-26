package dk.ufst.opendebt.caseworker.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

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
class DemoLoginControllerTest {

  @Mock private CaseworkerSessionService sessionService;
  @InjectMocks private DemoLoginController controller;

  private final MockHttpSession session = new MockHttpSession();
  private final Model model = new ExtendedModelMap();

  @Test
  void showLoginPage_populatesModelWithCaseworkers() {
    String view = controller.showLoginPage(model);

    assertThat(view).isEqualTo("demo-login");
    assertThat(model.asMap()).containsKey("caseworkers");
    @SuppressWarnings("unchecked")
    java.util.List<CaseworkerIdentity> list =
        (java.util.List<CaseworkerIdentity>) model.asMap().get("caseworkers");
    assertThat(list).isNotEmpty();
  }

  @Test
  void handleLogin_withKnownId_setsIdentityAndRedirects() {
    String view = controller.handleLogin("anna-jensen", session);

    assertThat(view).isEqualTo("redirect:/cases");
    verify(sessionService).setCurrentCaseworker(any(CaseworkerIdentity.class), eq(session));
  }

  @Test
  void handleLogin_withUnknownId_fallsBackToFirstCaseworker() {
    String view = controller.handleLogin("unknown-id", session);

    assertThat(view).isEqualTo("redirect:/cases");
    verify(sessionService).setCurrentCaseworker(any(CaseworkerIdentity.class), eq(session));
  }

  @Test
  void handleLogin_withAdminId_setsAdminIdentity() {
    String view = controller.handleLogin("system-admin", session);

    assertThat(view).isEqualTo("redirect:/cases");
    verify(sessionService).setCurrentCaseworker(any(CaseworkerIdentity.class), eq(session));
  }

  @Test
  void handleLogout_clearsSessionAndRedirects() {
    String view = controller.handleLogout(session);

    assertThat(view).isEqualTo("redirect:/demo-login");
    verify(sessionService).clearSession(session);
  }
}
