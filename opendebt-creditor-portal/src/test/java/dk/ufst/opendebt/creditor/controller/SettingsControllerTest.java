package dk.ufst.opendebt.creditor.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import dk.ufst.opendebt.creditor.client.CreditorServiceClient;
import dk.ufst.opendebt.creditor.dto.ContactEmailUpdateDto;
import dk.ufst.opendebt.creditor.dto.CreditorAgreementDto;
import dk.ufst.opendebt.creditor.service.PortalSessionService;

@ExtendWith(MockitoExtension.class)
class SettingsControllerTest {

  private static final UUID TEST_CREDITOR_ORG_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Mock private CreditorServiceClient creditorServiceClient;
  @Mock private MessageSource messageSource;
  @Mock private PortalSessionService portalSessionService;

  @InjectMocks private SettingsController controller;

  private MockHttpSession session;

  @BeforeEach
  void setUp() {
    session = new MockHttpSession();
  }

  @Test
  void showSettings_redirectsToDemoLogin_whenNoSession() {
    when(portalSessionService.resolveActingCreditor(eq(null), any())).thenReturn(null);

    Model model = new ConcurrentModel();
    String viewName = controller.showSettings(model, session);

    assertThat(viewName).isEqualTo("redirect:/demo-login");
  }

  @Test
  void showSettings_returnsSettingsView_withAgreement() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    CreditorAgreementDto agreement = buildAgreement();
    when(creditorServiceClient.getCreditorAgreement(TEST_CREDITOR_ORG_ID)).thenReturn(agreement);

    Model model = new ConcurrentModel();
    String viewName = controller.showSettings(model, session);

    assertThat(viewName).isEqualTo("indstillinger");
    assertThat(model.getAttribute("agreement")).isEqualTo(agreement);
    assertThat(model.getAttribute("currentPage")).isEqualTo("settings");
  }

  @Test
  void showSettings_showsError_whenBackendUnavailable() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    when(creditorServiceClient.getCreditorAgreement(TEST_CREDITOR_ORG_ID))
        .thenThrow(new RuntimeException("Connection refused"));
    when(messageSource.getMessage(eq("settings.backend.unavailable"), any(), any()))
        .thenReturn("Backend unavailable.");

    Model model = new ConcurrentModel();
    String viewName = controller.showSettings(model, session);

    assertThat(viewName).isEqualTo("indstillinger");
    assertThat(model.getAttribute("backendError")).isNotNull();
  }

  @Test
  void updateContactEmail_redirectsToSettings_onSuccess() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    when(messageSource.getMessage(eq("settings.email.updated"), any(), any()))
        .thenReturn("Updated.");

    ContactEmailUpdateDto form =
        ContactEmailUpdateDto.builder().contactEmail("test@example.com").build();
    BeanPropertyBindingResult bindingResult =
        new BeanPropertyBindingResult(form, "contactEmailForm");

    RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
    Model model = new ConcurrentModel();
    String viewName =
        controller.updateContactEmail(form, bindingResult, model, session, redirectAttributes);

    assertThat(viewName).isEqualTo("redirect:/indstillinger");
    assertThat(redirectAttributes.getFlashAttributes().get("successMessage")).isNotNull();
    verify(creditorServiceClient).updateContactEmail(eq(TEST_CREDITOR_ORG_ID), eq(form));
  }

  @Test
  void updateContactEmail_returnsSettingsView_onValidationErrors() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    when(creditorServiceClient.getCreditorAgreement(TEST_CREDITOR_ORG_ID))
        .thenReturn(buildAgreement());

    ContactEmailUpdateDto form = ContactEmailUpdateDto.builder().contactEmail("").build();
    BeanPropertyBindingResult bindingResult =
        new BeanPropertyBindingResult(form, "contactEmailForm");
    bindingResult.rejectValue("contactEmail", "NotBlank");

    RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
    Model model = new ConcurrentModel();
    String viewName =
        controller.updateContactEmail(form, bindingResult, model, session, redirectAttributes);

    assertThat(viewName).isEqualTo("indstillinger");
    verify(creditorServiceClient, never()).updateContactEmail(any(), any());
  }

  private CreditorAgreementDto buildAgreement() {
    return CreditorAgreementDto.builder()
        .portalActionsAllowed(true)
        .allowedClaimTypes(List.of("TAX", "FEE"))
        .allowedDebtorTypes(List.of("PERSON", "ORGANISATION"))
        .notificationPreference("EMAIL")
        .contactEmail("contact@test.dk")
        .build();
  }
}
