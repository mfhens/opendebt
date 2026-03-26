package dk.ufst.opendebt.caseworker.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import dk.ufst.opendebt.caseworker.client.ConfigServiceClient;
import dk.ufst.opendebt.caseworker.dto.CaseworkerIdentity;
import dk.ufst.opendebt.caseworker.dto.config.ConfigEntryPortalDto;
import dk.ufst.opendebt.caseworker.dto.config.CreateConfigPortalRequest;
import dk.ufst.opendebt.caseworker.service.CaseworkerSessionService;

@ExtendWith(MockitoExtension.class)
class ConfigurationControllerTest {

  @Mock private ConfigServiceClient configServiceClient;
  @Mock private CaseworkerSessionService sessionService;
  @InjectMocks private ConfigurationController controller;

  private final MockHttpSession session = new MockHttpSession();
  private final Model model = new ExtendedModelMap();
  private static final UUID ENTRY_ID = UUID.randomUUID();

  // --- list ---

  @Test
  void list_whenNoSession_redirectsToLogin() {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(null);

    String view = controller.list(session, model);

    assertThat(view).isEqualTo("redirect:/demo-login");
  }

  @Test
  void list_success_populatesGroupedEntries() {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(admin());
    Map<String, List<ConfigEntryPortalDto>> grouped =
        Map.of("MORARENTE", List.of(ConfigEntryPortalDto.builder().configKey("MORARENTE").build()));
    when(configServiceClient.listAllGrouped()).thenReturn(grouped);

    String view = controller.list(session, model);

    assertThat(view).isEqualTo("config/list");
    assertThat(model.asMap()).containsKey("grouped");
    assertThat(model.asMap()).containsEntry("canEdit", true);
  }

  @Test
  void list_whenBackendFails_setsError() {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(caseworker());
    when(configServiceClient.listAllGrouped()).thenThrow(new RuntimeException("unavailable"));

    String view = controller.list(session, model);

    assertThat(view).isEqualTo("config/list");
    assertThat(model.asMap()).containsKey("backendError");
    assertThat(model.asMap()).containsEntry("canEdit", false);
  }

  // --- detail ---

  @Test
  void detail_whenNoSession_redirectsToLogin() {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(null);

    String view = controller.detail("MORARENTE", session, model);

    assertThat(view).isEqualTo("redirect:/demo-login");
  }

  @Test
  void detail_success_populatesHistory() {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(configManager());
    List<ConfigEntryPortalDto> history =
        List.of(ConfigEntryPortalDto.builder().configKey("MORARENTE").build());
    when(configServiceClient.getHistory("MORARENTE")).thenReturn(history);

    String view = controller.detail("MORARENTE", session, model);

    assertThat(view).isEqualTo("config/detail");
    assertThat(model.asMap()).containsKey("history");
    assertThat(model.asMap()).containsEntry("configKey", "MORARENTE");
    assertThat(model.asMap()).containsEntry("canEdit", true);
  }

  @Test
  void detail_whenBackendFails_setsError() {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(caseworker());
    when(configServiceClient.getHistory("KEY")).thenThrow(new RuntimeException("down"));

    String view = controller.detail("KEY", session, model);

    assertThat(view).isEqualTo("config/detail");
    assertThat(model.asMap()).containsKey("backendError");
  }

  // --- previewDerived ---

  @Test
  void previewDerived_whenNoSession_redirectsToLogin() {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(null);

    String view =
        controller.previewDerived("MORARENTE", BigDecimal.ONE, LocalDate.now(), session, model);

    assertThat(view).isEqualTo("redirect:/demo-login");
  }

  @Test
  void previewDerived_whenCaseworkerRole_redirectsToLogin() {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(caseworker());

    String view =
        controller.previewDerived("MORARENTE", BigDecimal.ONE, LocalDate.now(), session, model);

    assertThat(view).isEqualTo("redirect:/demo-login");
  }

  @Test
  void previewDerived_success_populatesPreview() {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(admin());
    List<ConfigEntryPortalDto> preview =
        List.of(ConfigEntryPortalDto.builder().configKey("MORARENTE").build());
    when(configServiceClient.previewDerived(eq("MORARENTE"), any(), any())).thenReturn(preview);

    String view =
        controller.previewDerived(
            "MORARENTE", new BigDecimal("3.5"), LocalDate.now(), session, model);

    assertThat(view).isEqualTo("config/fragments/derived-preview :: derivedPreview");
    assertThat(model.asMap()).containsKey("preview");
  }

  @Test
  void previewDerived_whenBackendFails_setsPreviewError() {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(admin());
    when(configServiceClient.previewDerived(any(), any(), any()))
        .thenThrow(new RuntimeException("calculation failed"));

    String view =
        controller.previewDerived("MORARENTE", BigDecimal.ONE, LocalDate.now(), session, model);

    assertThat(view).isEqualTo("config/fragments/derived-preview :: derivedPreview");
    assertThat(model.asMap()).containsKey("previewError");
  }

  // --- create ---

  @Test
  void create_whenNoSession_redirectsToLogin() {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(null);

    String view =
        controller.create(
            new CreateConfigPortalRequest(), session, new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("redirect:/demo-login");
  }

  @Test
  void create_success_redirectsToList() {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(admin());
    CreateConfigPortalRequest request = new CreateConfigPortalRequest();
    request.setConfigKey("MORARENTE");

    String view = controller.create(request, session, new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("redirect:/konfiguration");
    verify(configServiceClient).createEntry(request);
  }

  @Test
  void create_whenBackendFails_setsErrorFlash() {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(admin());
    doThrow(new RuntimeException("validation")).when(configServiceClient).createEntry(any());

    RedirectAttributesModelMap redirectAttrs = new RedirectAttributesModelMap();
    String view = controller.create(new CreateConfigPortalRequest(), session, redirectAttrs);

    assertThat(view).isEqualTo("redirect:/konfiguration");
    assertThat(redirectAttrs.getFlashAttributes()).containsKey("errorMessage");
  }

  // --- approve ---

  @Test
  void approve_whenNoSession_redirectsToLogin() {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(null);

    String view = controller.approve(ENTRY_ID, session, new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("redirect:/demo-login");
  }

  @Test
  void approve_success_redirectsToList() {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(configManager());

    String view = controller.approve(ENTRY_ID, session, new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("redirect:/konfiguration");
    verify(configServiceClient).approveEntry(ENTRY_ID);
  }

  @Test
  void approve_whenBackendFails_setsErrorFlash() {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(admin());
    doThrow(new RuntimeException("not found")).when(configServiceClient).approveEntry(any());

    RedirectAttributesModelMap redirectAttrs = new RedirectAttributesModelMap();
    String view = controller.approve(ENTRY_ID, session, redirectAttrs);

    assertThat(view).isEqualTo("redirect:/konfiguration");
    assertThat(redirectAttrs.getFlashAttributes()).containsKey("errorMessage");
  }

  // --- reject ---

  @Test
  void reject_success_redirectsToList() {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(admin());

    String view = controller.reject(ENTRY_ID, session, new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("redirect:/konfiguration");
    verify(configServiceClient).rejectEntry(ENTRY_ID);
  }

  @Test
  void reject_whenBackendFails_setsErrorFlash() {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(admin());
    doThrow(new RuntimeException("conflict")).when(configServiceClient).rejectEntry(any());

    RedirectAttributesModelMap redirectAttrs = new RedirectAttributesModelMap();
    controller.reject(ENTRY_ID, session, redirectAttrs);

    assertThat(redirectAttrs.getFlashAttributes()).containsKey("errorMessage");
  }

  // --- delete ---

  @Test
  void delete_withoutReturnKey_redirectsToList() {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(admin());

    String view = controller.delete(ENTRY_ID, "", session, new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("redirect:/konfiguration");
    verify(configServiceClient).deleteEntry(ENTRY_ID);
  }

  @Test
  void delete_withReturnKey_redirectsToKey() {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(admin());

    String view =
        controller.delete(ENTRY_ID, "MORARENTE", session, new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("redirect:/konfiguration/MORARENTE");
  }

  @Test
  void delete_whenBackendFails_setsErrorFlash() {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(admin());
    doThrow(new RuntimeException("locked")).when(configServiceClient).deleteEntry(any());

    RedirectAttributesModelMap redirectAttrs = new RedirectAttributesModelMap();
    controller.delete(ENTRY_ID, "", session, redirectAttrs);

    assertThat(redirectAttrs.getFlashAttributes()).containsKey("errorMessage");
  }

  @Test
  void delete_whenNoSession_redirectsToLogin() {
    when(sessionService.getCurrentCaseworker(session)).thenReturn(null);

    String view = controller.delete(ENTRY_ID, "", session, new RedirectAttributesModelMap());

    assertThat(view).isEqualTo("redirect:/demo-login");
  }

  // --- helpers ---

  private CaseworkerIdentity caseworker() {
    return CaseworkerIdentity.builder().id("u1").name("Test").role("CASEWORKER").build();
  }

  private CaseworkerIdentity admin() {
    return CaseworkerIdentity.builder().id("u2").name("Admin").role("ADMIN").build();
  }

  private CaseworkerIdentity configManager() {
    return CaseworkerIdentity.builder()
        .id("u3")
        .name("Config")
        .role("CONFIGURATION_MANAGER")
        .build();
  }
}
