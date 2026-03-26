package dk.ufst.opendebt.caseworker.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import dk.ufst.opendebt.caseworker.client.CaseServiceClient;
import dk.ufst.opendebt.caseworker.client.RestPage;
import dk.ufst.opendebt.caseworker.dto.CaseworkerIdentity;
import dk.ufst.opendebt.caseworker.service.CaseworkerSessionService;
import dk.ufst.opendebt.common.dto.CaseDto;

@ExtendWith(MockitoExtension.class)
class CaseListControllerTest {

  @Mock private CaseServiceClient caseServiceClient;
  @Mock private CaseworkerSessionService sessionService;
  @Mock private MessageSource messageSource;
  @InjectMocks private CaseListController controller;

  private final MockHttpSession session = new MockHttpSession();
  private final Model model = new ExtendedModelMap();

  @Test
  void listCases_whenNoSession_redirectsToLogin() {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(null);

    String view = controller.listCases(null, null, 0, 20, session, model);

    assertThat(view).isEqualTo("redirect:/demo-login");
  }

  @Test
  void listCases_success_populatesModel() {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(caseworker());
    RestPage<CaseDto> page = new RestPage<>(List.of(CaseDto.builder().build()), 0, 20, 1, 1);
    when(caseServiceClient.listCases(isNull(), anyString(), anyInt(), anyInt())).thenReturn(page);

    String view = controller.listCases(null, null, 0, 20, session, model);

    assertThat(view).isEqualTo("cases/list");
    assertThat(model.asMap()).containsKey("cases");
  }

  @Test
  void listCases_withCaseStateFilter_passesCaseStateToClient() {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(caseworker());
    RestPage<CaseDto> page = new RestPage<>(List.of(), 0, 20, 0, 0);
    when(caseServiceClient.listCases(anyString(), anyString(), anyInt(), anyInt()))
        .thenReturn(page);

    String view = controller.listCases(null, "ACTIVE", 0, 20, session, model);

    assertThat(view).isEqualTo("cases/list");
    assertThat(model.asMap()).containsEntry("selectedCaseState", "ACTIVE");
  }

  @Test
  void listCases_whenBackendFails_setsErrorInModel() {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(caseworker());
    when(caseServiceClient.listCases(any(), anyString(), anyInt(), anyInt()))
        .thenThrow(new RuntimeException("backend down"));
    when(messageSource.getMessage(anyString(), any(), anyString(), any()))
        .thenReturn("Case service unavailable");

    String view = controller.listCases(null, null, 0, 20, session, model);

    assertThat(view).isEqualTo("cases/list");
    assertThat(model.asMap()).containsKey("backendError");
  }

  private CaseworkerIdentity caseworker() {
    return CaseworkerIdentity.builder().id("u1").name("Test").role("CASEWORKER").build();
  }
}
