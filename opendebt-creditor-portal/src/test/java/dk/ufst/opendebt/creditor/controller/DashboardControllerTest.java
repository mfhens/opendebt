package dk.ufst.opendebt.creditor.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import dk.ufst.opendebt.creditor.client.CreditorServiceClient;
import dk.ufst.opendebt.creditor.dto.PortalCreditorDto;
import dk.ufst.opendebt.creditor.service.PortalSessionService;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

  @Mock private CreditorServiceClient creditorServiceClient;
  @Mock private PortalSessionService portalSessionService;

  @InjectMocks private DashboardController controller;

  private MockHttpSession session;

  @BeforeEach
  void setUp() {
    session = new MockHttpSession();
  }

  @Test
  void index_returnsIndexViewName() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(DashboardController.DEMO_CREDITOR_ORG_ID);
    when(creditorServiceClient.getByCreditorOrgId(any(UUID.class))).thenReturn(buildCreditor());

    Model model = new ConcurrentModel();
    String viewName = controller.index(null, model, session);

    assertThat(viewName).isEqualTo("index");
  }

  @Test
  void index_addsCreditorToModel_whenBackendAvailable() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(DashboardController.DEMO_CREDITOR_ORG_ID);
    PortalCreditorDto creditor = buildCreditor();
    when(creditorServiceClient.getByCreditorOrgId(any(UUID.class))).thenReturn(creditor);

    Model model = new ConcurrentModel();
    controller.index(null, model, session);

    assertThat(model.getAttribute("creditor")).isEqualTo(creditor);
  }

  @Test
  void index_showsError_whenBackendUnavailable() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(DashboardController.DEMO_CREDITOR_ORG_ID);
    when(creditorServiceClient.getByCreditorOrgId(any(UUID.class)))
        .thenThrow(new RuntimeException("Connection refused"));

    Model model = new ConcurrentModel();
    String viewName = controller.index(null, model, session);

    assertThat(viewName).isEqualTo("index");
    assertThat(model.getAttribute("backendError")).isNotNull();
    assertThat((String) model.getAttribute("backendError")).contains("creditor-service");
    assertThat(model.getAttribute("creditor")).isNull();
  }

  @Test
  void index_showsError_whenCreditorNotFound() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(DashboardController.DEMO_CREDITOR_ORG_ID);
    when(creditorServiceClient.getByCreditorOrgId(any(UUID.class))).thenReturn(null);

    Model model = new ConcurrentModel();
    String viewName = controller.index(null, model, session);

    assertThat(viewName).isEqualTo("index");
    assertThat(model.getAttribute("backendError")).isNotNull();
    assertThat((String) model.getAttribute("backendError")).contains("seed-data");
    assertThat(model.getAttribute("creditor")).isNull();
  }

  @Test
  void index_addsActingCreditorToModel() {
    UUID actingCreditor = DashboardController.DEMO_CREDITOR_ORG_ID;
    when(portalSessionService.resolveActingCreditor(eq(null), any())).thenReturn(actingCreditor);
    when(creditorServiceClient.getByCreditorOrgId(any(UUID.class))).thenReturn(buildCreditor());

    Model model = new ConcurrentModel();
    controller.index(null, model, session);

    assertThat(model.getAttribute("actingCreditorOrgId")).isEqualTo(actingCreditor);
  }

  private PortalCreditorDto buildCreditor() {
    return PortalCreditorDto.builder()
        .id(UUID.randomUUID())
        .creditorOrgId(DashboardController.DEMO_CREDITOR_ORG_ID)
        .externalCreditorId("SKAT-DEMO-001")
        .activityStatus("ACTIVE")
        .connectionType("PORTAL")
        .build();
  }
}
