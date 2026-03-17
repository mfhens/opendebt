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
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import dk.ufst.opendebt.creditor.client.CreditorServiceClient;
import dk.ufst.opendebt.creditor.client.DebtServiceClient;
import dk.ufst.opendebt.creditor.dto.ClaimCountsDto;
import dk.ufst.opendebt.creditor.dto.PortalCreditorDto;
import dk.ufst.opendebt.creditor.service.PortalSessionService;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

  private static final UUID TEST_CREDITOR_ORG_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Mock private CreditorServiceClient creditorServiceClient;
  @Mock private DebtServiceClient debtServiceClient;
  @Mock private MessageSource messageSource;
  @Mock private PortalSessionService portalSessionService;

  @InjectMocks private DashboardController controller;

  private MockHttpSession session;

  @BeforeEach
  void setUp() {
    session = new MockHttpSession();
  }

  @Test
  void index_redirectsToDemoLogin_whenNoSessionCreditor() {
    when(portalSessionService.resolveActingCreditor(eq(null), any())).thenReturn(null);

    Model model = new ConcurrentModel();
    String viewName = controller.index(null, model, session);

    assertThat(viewName).isEqualTo("redirect:/demo-login");
  }

  @Test
  void index_returnsIndexViewName() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    when(creditorServiceClient.getByCreditorOrgId(any(UUID.class))).thenReturn(buildCreditor());

    Model model = new ConcurrentModel();
    String viewName = controller.index(null, model, session);

    assertThat(viewName).isEqualTo("index");
  }

  @Test
  void index_addsCreditorToModel_whenBackendAvailable() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    PortalCreditorDto creditor = buildCreditor();
    when(creditorServiceClient.getByCreditorOrgId(any(UUID.class))).thenReturn(creditor);

    Model model = new ConcurrentModel();
    controller.index(null, model, session);

    assertThat(model.getAttribute("creditor")).isEqualTo(creditor);
  }

  @Test
  void index_showsError_whenBackendUnavailable() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    when(creditorServiceClient.getByCreditorOrgId(any(UUID.class)))
        .thenThrow(new RuntimeException("Connection refused"));
    when(messageSource.getMessage(eq("controller.dashboard.backend.unavailable"), any(), any()))
        .thenReturn("Backend unavailable.");

    Model model = new ConcurrentModel();
    String viewName = controller.index(null, model, session);

    assertThat(viewName).isEqualTo("index");
    assertThat(model.getAttribute("backendError")).isNotNull();
    assertThat(model.getAttribute("creditor")).isNull();
  }

  @Test
  void index_showsError_whenCreditorNotFound() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    when(creditorServiceClient.getByCreditorOrgId(any(UUID.class))).thenReturn(null);
    when(messageSource.getMessage(eq("controller.dashboard.creditor.notfound"), any(), any()))
        .thenReturn("Creditor not found. Check seed-data.");

    Model model = new ConcurrentModel();
    String viewName = controller.index(null, model, session);

    assertThat(viewName).isEqualTo("index");
    assertThat(model.getAttribute("backendError")).isNotNull();
    assertThat((String) model.getAttribute("backendError")).contains("seed-data");
  }

  @Test
  void index_addsActingCreditorToModel() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    when(creditorServiceClient.getByCreditorOrgId(any(UUID.class))).thenReturn(buildCreditor());

    Model model = new ConcurrentModel();
    controller.index(null, model, session);

    assertThat(model.getAttribute("actingCreditorOrgId")).isEqualTo(TEST_CREDITOR_ORG_ID);
  }

  @Test
  void claimCounts_returnsFragment_withCounts() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    ClaimCountsDto counts =
        ClaimCountsDto.builder().inRecovery(5).inHearing(2).rejected(1).zeroBalance(3).build();
    when(debtServiceClient.getClaimCounts(TEST_CREDITOR_ORG_ID)).thenReturn(counts);

    Model model = new ConcurrentModel();
    String viewName = controller.claimCounts(model, session);

    assertThat(viewName).isEqualTo("fragments/claim-counts :: claimCounts");
    assertThat(model.getAttribute("counts")).isEqualTo(counts);
  }

  @Test
  void claimCounts_returnsEmptyCounts_whenNoSession() {
    when(portalSessionService.resolveActingCreditor(eq(null), any())).thenReturn(null);

    Model model = new ConcurrentModel();
    String viewName = controller.claimCounts(model, session);

    assertThat(viewName).isEqualTo("fragments/claim-counts :: claimCounts");
    ClaimCountsDto counts = (ClaimCountsDto) model.getAttribute("counts");
    assertThat(counts).isNotNull();
    assertThat(counts.getInRecovery()).isZero();
  }

  @Test
  void claimCounts_returnsEmptyCounts_whenBackendFails() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    when(debtServiceClient.getClaimCounts(any())).thenThrow(new RuntimeException("timeout"));

    Model model = new ConcurrentModel();
    String viewName = controller.claimCounts(model, session);

    assertThat(viewName).isEqualTo("fragments/claim-counts :: claimCounts");
    ClaimCountsDto counts = (ClaimCountsDto) model.getAttribute("counts");
    assertThat(counts).isNotNull();
  }

  @Test
  void index_addsCurrentPageToModel() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    when(creditorServiceClient.getByCreditorOrgId(any(UUID.class))).thenReturn(buildCreditor());

    Model model = new ConcurrentModel();
    controller.index(null, model, session);

    assertThat(model.getAttribute("currentPage")).isEqualTo("home");
  }

  private PortalCreditorDto buildCreditor() {
    return PortalCreditorDto.builder()
        .id(UUID.randomUUID())
        .creditorOrgId(TEST_CREDITOR_ORG_ID)
        .externalCreditorId("SKAT-DEMO-001")
        .activityStatus("ACTIVE")
        .connectionType("PORTAL")
        .build();
  }
}
