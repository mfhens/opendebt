package dk.ufst.opendebt.creditor.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
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
import dk.ufst.opendebt.creditor.client.NotificationServiceClient;
import dk.ufst.opendebt.creditor.dto.CreditorAgreementDto;
import dk.ufst.opendebt.creditor.dto.NotificationSearchDto;
import dk.ufst.opendebt.creditor.dto.NotificationSearchResultDto;
import dk.ufst.opendebt.creditor.dto.NotificationType;
import dk.ufst.opendebt.creditor.service.PortalSessionService;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

  private static final UUID TEST_CREDITOR_ORG_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Mock private CreditorServiceClient creditorServiceClient;
  @Mock private NotificationServiceClient notificationServiceClient;
  @Mock private MessageSource messageSource;
  @Mock private PortalSessionService portalSessionService;

  @InjectMocks private NotificationController controller;

  private MockHttpSession session;

  @BeforeEach
  void setUp() {
    session = new MockHttpSession();
  }

  @Test
  void showSearchPage_redirectsToDemoLogin_whenNoSession() {
    when(portalSessionService.resolveActingCreditor(eq(null), any())).thenReturn(null);

    Model model = new ConcurrentModel();
    String viewName = controller.showSearchPage(model, session);

    assertThat(viewName).isEqualTo("redirect:/demo-login");
  }

  @Test
  void showSearchPage_returnsNotificationsSearchView() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);

    Model model = new ConcurrentModel();
    String viewName = controller.showSearchPage(model, session);

    assertThat(viewName).isEqualTo("notifications/search");
    assertThat(model.getAttribute("currentPage")).isEqualTo("notifications");
  }

  @Test
  void showSearchPage_addsAvailableTypesToModel() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);

    Model model = new ConcurrentModel();
    controller.showSearchPage(model, session);

    @SuppressWarnings("unchecked")
    List<NotificationType> types = (List<NotificationType>) model.getAttribute("availableTypes");
    // When agreement unavailable, all 7 types returned
    assertThat(types).isNotNull().hasSize(7);
  }

  @Test
  void showSearchPage_filtersTypesByAgreement() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    CreditorAgreementDto agreement =
        CreditorAgreementDto.builder()
            .enabledNotificationTypes(Arrays.asList("INTEREST", "SETTLEMENT", "RETURN"))
            .build();
    when(creditorServiceClient.getCreditorAgreement(TEST_CREDITOR_ORG_ID)).thenReturn(agreement);

    Model model = new ConcurrentModel();
    controller.showSearchPage(model, session);

    @SuppressWarnings("unchecked")
    List<NotificationType> types = (List<NotificationType>) model.getAttribute("availableTypes");
    assertThat(types)
        .hasSize(3)
        .containsExactly(
            NotificationType.INTEREST, NotificationType.SETTLEMENT, NotificationType.RETURN);
  }

  @Test
  void showSearchPage_addsEmptySearchFormToModel() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);

    Model model = new ConcurrentModel();
    controller.showSearchPage(model, session);

    assertThat(model.getAttribute("searchForm")).isInstanceOf(NotificationSearchDto.class);
  }

  @Test
  void searchNotifications_redirectsToDemoLogin_whenNoSession() {
    when(portalSessionService.resolveActingCreditor(eq(null), any())).thenReturn(null);

    Model model = new ConcurrentModel();
    String viewName = controller.searchNotifications(new NotificationSearchDto(), model, session);

    assertThat(viewName).isEqualTo("redirect:/demo-login");
  }

  @Test
  void searchNotifications_returnsSearchViewWithResults() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    NotificationSearchResultDto result =
        NotificationSearchResultDto.builder().matchingCount(42).build();
    when(notificationServiceClient.searchNotifications(any(), any(), any(), any()))
        .thenReturn(result);

    Model model = new ConcurrentModel();
    NotificationSearchDto searchForm = new NotificationSearchDto();
    String viewName = controller.searchNotifications(searchForm, model, session);

    assertThat(viewName).isEqualTo("notifications/search");
    assertThat(model.getAttribute("searchPerformed")).isEqualTo(true);
    NotificationSearchResultDto searchResult =
        (NotificationSearchResultDto) model.getAttribute("searchResult");
    assertThat(searchResult).isNotNull();
    assertThat(searchResult.getMatchingCount()).isEqualTo(42);
  }

  @Test
  void searchNotifications_handlesBackendUnavailable() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    when(notificationServiceClient.searchNotifications(any(), any(), any(), any()))
        .thenReturn(null);
    when(messageSource.getMessage(eq("notifications.backend.unavailable"), any(), any()))
        .thenReturn("Service unavailable");

    Model model = new ConcurrentModel();
    String viewName = controller.searchNotifications(new NotificationSearchDto(), model, session);

    assertThat(viewName).isEqualTo("notifications/search");
    assertThat(model.getAttribute("backendWarning")).isEqualTo("Service unavailable");
    NotificationSearchResultDto searchResult =
        (NotificationSearchResultDto) model.getAttribute("searchResult");
    assertThat(searchResult).isNotNull();
    assertThat(searchResult.getMatchingCount()).isZero();
  }

  @Test
  void searchNotifications_handlesException() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    when(notificationServiceClient.searchNotifications(any(), any(), any(), any()))
        .thenThrow(new RuntimeException("Connection refused"));
    when(messageSource.getMessage(eq("notifications.search.error"), any(), any()))
        .thenReturn("Search error");

    Model model = new ConcurrentModel();
    String viewName = controller.searchNotifications(new NotificationSearchDto(), model, session);

    assertThat(viewName).isEqualTo("notifications/search");
    assertThat(model.getAttribute("backendError")).isEqualTo("Search error");
  }

  @Test
  void searchNotificationsFragment_returnsFragmentView() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    NotificationSearchResultDto result =
        NotificationSearchResultDto.builder().matchingCount(10).build();
    when(notificationServiceClient.searchNotifications(any(), any(), any(), any()))
        .thenReturn(result);

    Model model = new ConcurrentModel();
    String viewName =
        controller.searchNotificationsFragment(new NotificationSearchDto(), model, session);

    assertThat(viewName).isEqualTo("notifications/fragments/results :: notificationResults");
    assertThat(model.getAttribute("searchPerformed")).isEqualTo(true);
  }

  @Test
  void downloadNotifications_redirectsToDemoLogin_whenNoSession() {
    when(portalSessionService.resolveActingCreditor(eq(null), any())).thenReturn(null);

    Model model = new ConcurrentModel();
    Object result = controller.downloadNotifications(new NotificationSearchDto(), model, session);

    assertThat(result).isEqualTo("redirect:/demo-login");
  }

  @Test
  void downloadNotifications_showsError_whenNoFormatSelected() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    when(messageSource.getMessage(eq("notifications.download.noformat"), any(), any()))
        .thenReturn("Select a format");

    Model model = new ConcurrentModel();
    NotificationSearchDto searchForm = new NotificationSearchDto();
    searchForm.setFormatPdf(false);
    searchForm.setFormatXml(false);
    Object result = controller.downloadNotifications(searchForm, model, session);

    assertThat(result).isEqualTo("notifications/search");
    assertThat(model.getAttribute("downloadError")).isEqualTo("Select a format");
  }

  @Test
  void downloadNotifications_handlesBackendUnavailable() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    when(notificationServiceClient.downloadNotifications(
            any(), any(), any(), any(), anyBoolean(), anyBoolean()))
        .thenReturn(null);
    when(messageSource.getMessage(eq("notifications.backend.unavailable"), any(), any()))
        .thenReturn("Service unavailable");

    Model model = new ConcurrentModel();
    NotificationSearchDto searchForm = new NotificationSearchDto();
    searchForm.setFormatPdf(true);
    Object result = controller.downloadNotifications(searchForm, model, session);

    assertThat(result).isEqualTo("notifications/search");
    assertThat(model.getAttribute("downloadError")).isEqualTo("Service unavailable");
  }

  @Test
  void showSearchPage_addsActingCreditorToModel() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);

    Model model = new ConcurrentModel();
    controller.showSearchPage(model, session);

    assertThat(model.getAttribute("actingCreditorOrgId")).isEqualTo(TEST_CREDITOR_ORG_ID);
  }

  @Test
  void showSearchPage_showsAllTypes_whenAgreementFails() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    when(creditorServiceClient.getCreditorAgreement(any()))
        .thenThrow(new RuntimeException("Connection refused"));

    Model model = new ConcurrentModel();
    controller.showSearchPage(model, session);

    @SuppressWarnings("unchecked")
    List<NotificationType> types = (List<NotificationType>) model.getAttribute("availableTypes");
    assertThat(types).hasSize(7);
  }
}
