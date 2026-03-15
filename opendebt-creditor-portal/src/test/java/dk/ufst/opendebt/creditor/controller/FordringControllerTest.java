package dk.ufst.opendebt.creditor.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import dk.ufst.opendebt.creditor.client.DebtServiceClient;
import dk.ufst.opendebt.creditor.dto.FordringFormDto;
import dk.ufst.opendebt.creditor.dto.PortalDebtDto;
import dk.ufst.opendebt.creditor.mapper.FordringMapper;
import dk.ufst.opendebt.creditor.service.PortalSessionService;

@ExtendWith(MockitoExtension.class)
class FordringControllerTest {

  @Mock private DebtServiceClient debtServiceClient;
  @Mock private FordringMapper fordringMapper;
  @Mock private PortalSessionService portalSessionService;

  @InjectMocks private FordringController controller;

  private MockHttpSession session;
  private static final UUID ACTING_CREDITOR =
      UUID.fromString("00000000-0000-0000-0000-000000000001");

  @BeforeEach
  void setUp() {
    session = new MockHttpSession();
  }

  @Test
  void showForm_returnsFormViewWithEmptyDto() {
    when(portalSessionService.resolveActingCreditor(null, session)).thenReturn(ACTING_CREDITOR);

    Model model = new ConcurrentModel();
    String viewName = controller.showForm(model, session);

    assertThat(viewName).isEqualTo("fordring-ny");
    assertThat(model.getAttribute("fordringForm")).isInstanceOf(FordringFormDto.class);
    assertThat(model.getAttribute("actingCreditorOrgId")).isEqualTo(ACTING_CREDITOR);
  }

  @Test
  void showForm_preservesExistingFormDataOnRedisplay() {
    when(portalSessionService.resolveActingCreditor(null, session)).thenReturn(ACTING_CREDITOR);

    FordringFormDto existingForm = buildValidForm();
    Model model = new ConcurrentModel();
    model.addAttribute("fordringForm", existingForm);

    String viewName = controller.showForm(model, session);

    assertThat(viewName).isEqualTo("fordring-ny");
    assertThat(model.getAttribute("fordringForm")).isSameAs(existingForm);
  }

  @Test
  void submitForm_redirectsOnSuccess() {
    when(portalSessionService.resolveActingCreditor(null, session)).thenReturn(ACTING_CREDITOR);

    FordringFormDto form = buildValidForm();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "fordringForm");
    PortalDebtDto debtRequest = PortalDebtDto.builder().build();

    when(fordringMapper.toDebtRequest(form, ACTING_CREDITOR)).thenReturn(debtRequest);
    when(debtServiceClient.createDebt(debtRequest))
        .thenReturn(PortalDebtDto.builder().id(UUID.randomUUID()).build());

    Model model = new ConcurrentModel();
    RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

    String viewName =
        controller.submitForm(form, bindingResult, model, session, redirectAttributes);

    assertThat(viewName).isEqualTo("redirect:/fordringer");
    assertThat(redirectAttributes.getFlashAttributes()).containsKey("successMessage");
    verify(debtServiceClient).createDebt(debtRequest);
  }

  @Test
  void submitForm_redisplaysFormOnValidationErrors() {
    when(portalSessionService.resolveActingCreditor(null, session)).thenReturn(ACTING_CREDITOR);
    when(portalSessionService.getRepresentedCreditor(session)).thenReturn(null);

    FordringFormDto form = new FordringFormDto(); // empty = invalid
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "fordringForm");
    bindingResult.rejectValue("debtorPersonId", "NotNull", "Skyldner-ID skal udfyldes");

    Model model = new ConcurrentModel();
    RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

    String viewName =
        controller.submitForm(form, bindingResult, model, session, redirectAttributes);

    assertThat(viewName).isEqualTo("fordring-ny");
    verify(debtServiceClient, never()).createDebt(any());
  }

  @Test
  void submitForm_showsErrorOnBackendFailure() {
    when(portalSessionService.resolveActingCreditor(null, session)).thenReturn(ACTING_CREDITOR);
    when(portalSessionService.getRepresentedCreditor(session)).thenReturn(null);

    FordringFormDto form = buildValidForm();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "fordringForm");
    PortalDebtDto debtRequest = PortalDebtDto.builder().build();

    when(fordringMapper.toDebtRequest(form, ACTING_CREDITOR)).thenReturn(debtRequest);
    when(debtServiceClient.createDebt(debtRequest))
        .thenThrow(new RuntimeException("Connection refused"));

    Model model = new ConcurrentModel();
    RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

    String viewName =
        controller.submitForm(form, bindingResult, model, session, redirectAttributes);

    assertThat(viewName).isEqualTo("fordring-ny");
    assertThat(model.getAttribute("backendError")).isNotNull();
    assertThat((String) model.getAttribute("backendError"))
        .contains("Fordringen kunne ikke indsendes");
  }

  private FordringFormDto buildValidForm() {
    return FordringFormDto.builder()
        .debtorPersonId(UUID.randomUUID())
        .principalAmount(new BigDecimal("1000.00"))
        .debtTypeCode("SKAT")
        .dueDate(LocalDate.now().plusDays(30))
        .description("Test fordring")
        .build();
  }
}
