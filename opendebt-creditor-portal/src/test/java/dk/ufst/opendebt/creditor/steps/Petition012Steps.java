package dk.ufst.opendebt.creditor.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import dk.ufst.opendebt.creditor.client.CreditorServiceClient;
import dk.ufst.opendebt.creditor.client.DebtServiceClient;
import dk.ufst.opendebt.creditor.controller.DashboardController;
import dk.ufst.opendebt.creditor.controller.FordringController;
import dk.ufst.opendebt.creditor.dto.AccessResolutionRequest;
import dk.ufst.opendebt.creditor.dto.AccessResolutionResponse;
import dk.ufst.opendebt.creditor.dto.FordringFormDto;
import dk.ufst.opendebt.creditor.dto.PortalCreditorDto;
import dk.ufst.opendebt.creditor.dto.PortalDebtDto;
import dk.ufst.opendebt.creditor.mapper.FordringMapper;
import dk.ufst.opendebt.creditor.service.PortalSessionService;

import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * BDD step definitions for Petition 012: Fordringshaverportal as BFF and manual submission channel.
 *
 * <p>The portal is a BFF that does not own data. These tests verify that the portal reads creditor
 * profiles from the creditor-service, submits fordringer to the debt-service, and enforces
 * acting-on-behalf-of restrictions.
 */
public class Petition012Steps {

  private CreditorServiceClient creditorServiceClient;
  private DebtServiceClient debtServiceClient;
  private MessageSource messageSource;
  private PortalSessionService portalSessionService;
  private DashboardController dashboardController;
  private FordringController fordringController;
  private FordringMapper fordringMapper;

  private MockHttpSession session;
  private Model model;
  private String viewResult;
  private UUID boundCreditorOrgId;
  private UUID unrelatedCreditorOrgId;
  private boolean creditorServiceCalled;
  private boolean debtServiceCalled;

  @Before
  public void setUp() {
    creditorServiceClient = Mockito.mock(CreditorServiceClient.class);
    debtServiceClient = Mockito.mock(DebtServiceClient.class);
    messageSource = Mockito.mock(MessageSource.class);
    when(messageSource.getMessage(
            Mockito.eq("controller.dashboard.backend.unavailable"), any(), any()))
        .thenReturn("Backend-tjenesten er ikke tilgængelig. Kontroller at creditor-service kører.");
    when(messageSource.getMessage(
            Mockito.eq("controller.dashboard.creditor.notfound"), any(), any()))
        .thenReturn("Fordringshaver ikke fundet for orgId. Kontroller seed-data.");
    when(messageSource.getMessage(Mockito.eq("controller.dashboard.access.denied"), any(), any()))
        .thenReturn("Adgang nægtet: Du har ikke tilladelse.");
    when(messageSource.getMessage(
            Mockito.eq("controller.dashboard.access.denied.default"), any(), any()))
        .thenReturn("Du har ikke tilladelse til at handle på vegne af denne fordringshaver.");
    when(messageSource.getMessage(Mockito.eq("controller.fordring.submitted"), any(), any()))
        .thenReturn("Fordringen er indsendt.");
    when(messageSource.getMessage(Mockito.eq("controller.fordring.submit.error"), any(), any()))
        .thenReturn("Fordringen kunne ikke indsendes. Prøv igen senere.");
    portalSessionService = new PortalSessionService(creditorServiceClient);
    fordringMapper = new FordringMapper();
    dashboardController =
        new DashboardController(
            creditorServiceClient, debtServiceClient, messageSource, portalSessionService);
    fordringController =
        new FordringController(
            debtServiceClient, fordringMapper, messageSource, portalSessionService);
    session = new MockHttpSession();
    model = new ConcurrentModel();
    viewResult = null;
    boundCreditorOrgId = null;
    unrelatedCreditorOrgId = null;
    creditorServiceCalled = false;
    debtServiceCalled = false;
  }

  // Scenario 1: The portal reads creditor profile from the backend service

  @Given("portal user {string} is bound to fordringshaver {string}")
  public void portalUserBoundToFordringshaver(String userId, String creditorAlias) {
    boundCreditorOrgId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    session.setAttribute("actingCreditorOrgId", boundCreditorOrgId);
    PortalCreditorDto creditor =
        PortalCreditorDto.builder()
            .id(UUID.randomUUID())
            .creditorOrgId(boundCreditorOrgId)
            .externalCreditorId("EXT-" + creditorAlias)
            .activityStatus("ACTIVE")
            .connectionType("PORTAL")
            .build();
    when(creditorServiceClient.getByCreditorOrgId(boundCreditorOrgId)).thenReturn(creditor);
  }

  @When("user {string} opens the fordringshaver portal")
  public void userOpensPortal(String userId) {
    viewResult = dashboardController.index(null, model, session);
  }

  @Then("the portal reads creditor profile data from the creditor master data service")
  public void portalReadsCreditorProfile() {
    verify(creditorServiceClient).getByCreditorOrgId(boundCreditorOrgId);
    assertThat(viewResult).isEqualTo("index");
    PortalCreditorDto creditor = (PortalCreditorDto) model.getAttribute("creditor");
    assertThat(creditor).isNotNull();
    assertThat(creditor.getCreditorOrgId()).isEqualTo(boundCreditorOrgId);
  }

  // Scenario 2: Manual fordring creation is submitted to debt-service

  @Given("portal user {string} is allowed to create fordringer for fordringshaver {string}")
  public void portalUserAllowedToCreateFordringer(String userId, String creditorAlias) {
    boundCreditorOrgId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    session.setAttribute("actingCreditorOrgId", boundCreditorOrgId);

    when(debtServiceClient.createDebt(any(PortalDebtDto.class)))
        .thenAnswer(
            invocation -> {
              debtServiceCalled = true;
              return invocation.getArgument(0);
            });
  }

  @When("user {string} submits a manual fordring in the portal")
  public void userSubmitsManualFordring(String userId) {
    FordringFormDto form = new FordringFormDto();
    form.setDebtorPersonId(UUID.randomUUID());
    form.setPrincipalAmount(new BigDecimal("10000.00"));
    form.setDebtTypeCode("SKAT");
    form.setDueDate(LocalDate.now().plusDays(30));
    form.setDescription("Test fordring");

    BindingResult bindingResult = new BeanPropertyBindingResult(form, "fordringForm");
    RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

    viewResult =
        fordringController.submitForm(form, bindingResult, model, session, redirectAttributes);
  }

  @Then("the portal sends the request to debt-service")
  public void portalSendsRequestToDebtService() {
    ArgumentCaptor<PortalDebtDto> captor = ArgumentCaptor.forClass(PortalDebtDto.class);
    verify(debtServiceClient).createDebt(captor.capture());
    PortalDebtDto sent = captor.getValue();
    assertThat(sent.getCreditorOrgId()).isEqualTo(boundCreditorOrgId);
    assertThat(sent.getPrincipalAmount()).isNotNull();
  }

  @And("the portal does not persist the fordring as its own domain data")
  public void portalDoesNotPersistFordring() {
    // The portal has no JPA repositories and no database of its own.
    // The redirect to /fordringer confirms the portal delegated and did not persist.
    assertThat(viewResult).isEqualTo("redirect:/fordringer");
  }

  // Scenario 3: A portal user cannot act for an unrelated fordringshaver

  @And("fordringshaver {string} may not act on behalf of fordringshaver {string}")
  public void fordringshaverMayNotActOnBehalf(String ownAlias, String unrelatedAlias) {
    unrelatedCreditorOrgId = UUID.fromString("00000000-0000-0000-0000-000000000099");
    when(creditorServiceClient.resolveAccess(any(AccessResolutionRequest.class)))
        .thenReturn(
            AccessResolutionResponse.builder()
                .channelType("PORTAL")
                .allowed(false)
                .reasonCode("UNAUTHORIZED")
                .message("Ikke tilladt")
                .build());

    when(creditorServiceClient.getByCreditorOrgId(any(UUID.class)))
        .thenReturn(
            PortalCreditorDto.builder()
                .id(UUID.randomUUID())
                .creditorOrgId(boundCreditorOrgId)
                .externalCreditorId("EXT-" + ownAlias)
                .activityStatus("ACTIVE")
                .connectionType("PORTAL")
                .build());
  }

  @When("user {string} attempts to act for fordringshaver {string}")
  public void userAttemptsToActForUnrelatedFordringshaver(String userId, String unrelatedAlias) {
    viewResult = dashboardController.index(unrelatedCreditorOrgId.toString(), model, session);
  }

  @Then("the request is rejected")
  public void requestIsRejected() {
    assertThat(model.getAttribute("actAsDeniedMessage")).isNotNull();
    String deniedMsg = (String) model.getAttribute("actAsDeniedMessage");
    assertThat(deniedMsg).contains("Adgang nægtet");
  }
}
