package dk.ufst.opendebt.creditor.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import dk.ufst.opendebt.creditor.client.CreditorServiceClient;
import dk.ufst.opendebt.creditor.client.DebtServiceClient;
import dk.ufst.opendebt.creditor.client.PersonRegistryClient;
import dk.ufst.opendebt.creditor.dto.ClaimSubmissionResultDto;
import dk.ufst.opendebt.creditor.dto.ClaimWizardFormDto;
import dk.ufst.opendebt.creditor.dto.CreditorAgreementDto;
import dk.ufst.opendebt.creditor.dto.DebtorVerificationResultDto;
import dk.ufst.opendebt.creditor.service.PortalSessionService;

@ExtendWith(MockitoExtension.class)
class ClaimCreateControllerTest {

  @Mock private PortalSessionService portalSessionService;
  @Mock private CreditorServiceClient creditorServiceClient;
  @Mock private DebtServiceClient debtServiceClient;
  @Mock private PersonRegistryClient personRegistryClient;
  @Mock private MessageSource messageSource;

  @InjectMocks private ClaimCreateController controller;

  private MockHttpSession session;
  private static final UUID ACTING_CREDITOR =
      UUID.fromString("00000000-0000-0000-0000-000000000001");

  @BeforeEach
  void setUp() {
    session = new MockHttpSession();
  }

  // -----------------------------------------------------------------------
  // Entry point
  // -----------------------------------------------------------------------

  @Test
  void entryPoint_redirectsToStep1WhenAuthenticated() {
    when(portalSessionService.resolveActingCreditor(null, session)).thenReturn(ACTING_CREDITOR);

    String view = controller.entryPoint(session);

    assertThat(view).isEqualTo("redirect:/fordring/opret/step/1");
  }

  @Test
  void entryPoint_redirectsToDemoLoginWhenUnauthenticated() {
    when(portalSessionService.resolveActingCreditor(null, session)).thenReturn(null);

    String view = controller.entryPoint(session);

    assertThat(view).isEqualTo("redirect:/demo-login");
  }

  // -----------------------------------------------------------------------
  // Step 1: Debtor identification
  // -----------------------------------------------------------------------

  @Test
  void showDebtorStep_returnsStep1ViewWithForm() {
    when(portalSessionService.resolveActingCreditor(null, session)).thenReturn(ACTING_CREDITOR);
    when(creditorServiceClient.getCreditorAgreement(ACTING_CREDITOR)).thenReturn(buildAgreement());

    Model model = new ConcurrentModel();
    String view = controller.showDebtorStep(model, session);

    assertThat(view).isEqualTo("claims/create/step-debtor");
    assertThat(model.getAttribute("wizardForm")).isInstanceOf(ClaimWizardFormDto.class);
    assertThat(model.getAttribute("currentStep")).isEqualTo(1);
    assertThat(model.getAttribute("totalSteps")).isEqualTo(4);
  }

  @Test
  void showDebtorStep_redirectsWhenAgreementDisallowsClaimCreation() {
    when(portalSessionService.resolveActingCreditor(null, session)).thenReturn(ACTING_CREDITOR);
    CreditorAgreementDto agreement = buildAgreement();
    agreement.setAllowCreateRecoveryClaims(false);
    when(creditorServiceClient.getCreditorAgreement(ACTING_CREDITOR)).thenReturn(agreement);

    Model model = new ConcurrentModel();
    String view = controller.showDebtorStep(model, session);

    assertThat(view).isEqualTo("redirect:/");
  }

  @Test
  void processDebtorStep_advancesToStep2OnSuccessfulCprVerification() {
    when(portalSessionService.resolveActingCreditor(null, session)).thenReturn(ACTING_CREDITOR);
    when(creditorServiceClient.getCreditorAgreement(ACTING_CREDITOR)).thenReturn(buildAgreement());
    when(personRegistryClient.verifyCpr("1234567890", "John", "Doe"))
        .thenReturn(
            DebtorVerificationResultDto.builder()
                .verified(true)
                .displayName("John Doe")
                .personId(UUID.randomUUID())
                .build());

    Model model = new ConcurrentModel();
    RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

    String view =
        controller.processDebtorStep("CPR", "1234567890", "John", "Doe", model, session, redirect);

    assertThat(view).isEqualTo("redirect:/fordring/opret/step/2");
    ClaimWizardFormDto saved =
        (ClaimWizardFormDto) session.getAttribute(ClaimCreateController.SESSION_WIZARD_FORM);
    assertThat(saved).isNotNull();
    assertThat(saved.isDebtorVerified()).isTrue();
    assertThat(saved.getDebtorDisplayName()).isEqualTo("John Doe");
  }

  @Test
  void processDebtorStep_redisplaysStep1OnVerificationFailure() {
    when(portalSessionService.resolveActingCreditor(null, session)).thenReturn(ACTING_CREDITOR);
    when(creditorServiceClient.getCreditorAgreement(ACTING_CREDITOR)).thenReturn(buildAgreement());
    when(personRegistryClient.verifyCpr("1234567890", "Wrong", "Name"))
        .thenReturn(
            DebtorVerificationResultDto.builder()
                .verified(false)
                .errorMessage("Name mismatch")
                .build());

    Model model = new ConcurrentModel();
    RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

    String view =
        controller.processDebtorStep(
            "CPR", "1234567890", "Wrong", "Name", model, session, redirect);

    assertThat(view).isEqualTo("claims/create/step-debtor");
    assertThat(model.getAttribute("verificationError")).isEqualTo("Name mismatch");
  }

  @Test
  void processDebtorStep_rejectsEmptyDebtorType() {
    when(portalSessionService.resolveActingCreditor(null, session)).thenReturn(ACTING_CREDITOR);
    when(creditorServiceClient.getCreditorAgreement(ACTING_CREDITOR)).thenReturn(buildAgreement());
    when(messageSource.getMessage(eq("wizard.validation.debtorType.required"), any(), any(), any()))
        .thenReturn("Debtor type is required.");

    Model model = new ConcurrentModel();
    RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

    String view =
        controller.processDebtorStep("", "1234567890", null, null, model, session, redirect);

    assertThat(view).isEqualTo("claims/create/step-debtor");
    assertThat(model.getAttribute("debtorTypeError")).isNotNull();
  }

  @Test
  void processDebtorStep_acceptsAkrWithoutExternalVerification() {
    when(portalSessionService.resolveActingCreditor(null, session)).thenReturn(ACTING_CREDITOR);
    when(creditorServiceClient.getCreditorAgreement(ACTING_CREDITOR)).thenReturn(buildAgreement());

    Model model = new ConcurrentModel();
    RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

    String view =
        controller.processDebtorStep("AKR", "AKR-12345", null, null, model, session, redirect);

    assertThat(view).isEqualTo("redirect:/fordring/opret/step/2");
    ClaimWizardFormDto saved =
        (ClaimWizardFormDto) session.getAttribute(ClaimCreateController.SESSION_WIZARD_FORM);
    assertThat(saved).isNotNull();
    assertThat(saved.isDebtorVerified()).isTrue();
  }

  @Test
  void processDebtorStep_verifiesCvrDebtor() {
    when(portalSessionService.resolveActingCreditor(null, session)).thenReturn(ACTING_CREDITOR);
    when(creditorServiceClient.getCreditorAgreement(ACTING_CREDITOR)).thenReturn(buildAgreement());
    when(personRegistryClient.verifyCvr("12345678"))
        .thenReturn(
            DebtorVerificationResultDto.builder()
                .verified(true)
                .displayName("Company 12345678")
                .personId(UUID.randomUUID())
                .build());

    Model model = new ConcurrentModel();
    RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

    String view =
        controller.processDebtorStep("CVR", "12345678", null, null, model, session, redirect);

    assertThat(view).isEqualTo("redirect:/fordring/opret/step/2");
  }

  // -----------------------------------------------------------------------
  // Step 2: Claim details
  // -----------------------------------------------------------------------

  @Test
  void showDetailsStep_redirectsToStep1WhenNoVerifiedDebtor() {
    when(portalSessionService.resolveActingCreditor(null, session)).thenReturn(ACTING_CREDITOR);

    Model model = new ConcurrentModel();
    String view = controller.showDetailsStep(model, session);

    assertThat(view).isEqualTo("redirect:/fordring/opret/step/1");
  }

  @Test
  void showDetailsStep_returnsStep2ViewWhenDebtorVerified() {
    when(portalSessionService.resolveActingCreditor(null, session)).thenReturn(ACTING_CREDITOR);
    when(creditorServiceClient.getCreditorAgreement(ACTING_CREDITOR)).thenReturn(buildAgreement());

    session.setAttribute(ClaimCreateController.SESSION_WIZARD_FORM, buildVerifiedWizardForm());

    Model model = new ConcurrentModel();
    String view = controller.showDetailsStep(model, session);

    assertThat(view).isEqualTo("claims/create/step-details");
    assertThat(model.getAttribute("currentStep")).isEqualTo(2);
  }

  @Test
  void processDetailsStep_advancesToStep3OnValidInput() {
    when(portalSessionService.resolveActingCreditor(null, session)).thenReturn(ACTING_CREDITOR);
    when(creditorServiceClient.getCreditorAgreement(ACTING_CREDITOR)).thenReturn(buildAgreement());

    session.setAttribute(ClaimCreateController.SESSION_WIZARD_FORM, buildVerifiedWizardForm());

    Model model = new ConcurrentModel();

    String view =
        controller.processDetailsStep(
            "SKAT",
            "1000.00",
            "500.00",
            "REF-001",
            "Test description",
            null,
            null,
            null,
            null,
            null,
            "2027-12-31",
            "false",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            model,
            session);

    assertThat(view).isEqualTo("redirect:/fordring/opret/step/3");
  }

  @Test
  void processDetailsStep_redisplaysStep2OnMissingClaimType() {
    when(portalSessionService.resolveActingCreditor(null, session)).thenReturn(ACTING_CREDITOR);
    when(creditorServiceClient.getCreditorAgreement(ACTING_CREDITOR)).thenReturn(buildAgreement());
    when(messageSource.getMessage(eq("wizard.validation.claimType.required"), any(), any(), any()))
        .thenReturn("Claim type is required.");

    session.setAttribute(ClaimCreateController.SESSION_WIZARD_FORM, buildVerifiedWizardForm());

    Model model = new ConcurrentModel();

    String view =
        controller.processDetailsStep(
            "",
            "1000.00",
            "500.00",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "2027-12-31",
            "false",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            model,
            session);

    assertThat(view).isEqualTo("claims/create/step-details");
    assertThat(model.getAttribute("claimTypeError")).isNotNull();
  }

  @Test
  void processDetailsStep_rejectsDescriptionOver100Characters() {
    when(portalSessionService.resolveActingCreditor(null, session)).thenReturn(ACTING_CREDITOR);
    when(creditorServiceClient.getCreditorAgreement(ACTING_CREDITOR)).thenReturn(buildAgreement());
    when(messageSource.getMessage(
            eq("wizard.validation.description.maxlength"), any(), any(), any()))
        .thenReturn("Description must not exceed 100 characters.");

    session.setAttribute(ClaimCreateController.SESSION_WIZARD_FORM, buildVerifiedWizardForm());

    String longDescription = "A".repeat(101);
    Model model = new ConcurrentModel();

    String view =
        controller.processDetailsStep(
            "SKAT",
            "1000.00",
            "500.00",
            null,
            longDescription,
            null,
            null,
            null,
            null,
            null,
            "2027-12-31",
            "false",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            model,
            session);

    assertThat(view).isEqualTo("claims/create/step-details");
    assertThat(model.getAttribute("descriptionError")).isNotNull();
  }

  // -----------------------------------------------------------------------
  // Step 3: Review
  // -----------------------------------------------------------------------

  @Test
  void showReviewStep_redirectsToStep1WhenNoForm() {
    when(portalSessionService.resolveActingCreditor(null, session)).thenReturn(ACTING_CREDITOR);

    Model model = new ConcurrentModel();
    String view = controller.showReviewStep(model, session);

    assertThat(view).isEqualTo("redirect:/fordring/opret/step/1");
  }

  @Test
  void showReviewStep_redirectsToStep2WhenNoClaimType() {
    when(portalSessionService.resolveActingCreditor(null, session)).thenReturn(ACTING_CREDITOR);

    ClaimWizardFormDto form = buildVerifiedWizardForm();
    form.setClaimType(null);
    session.setAttribute(ClaimCreateController.SESSION_WIZARD_FORM, form);

    Model model = new ConcurrentModel();
    String view = controller.showReviewStep(model, session);

    assertThat(view).isEqualTo("redirect:/fordring/opret/step/2");
  }

  @Test
  void showReviewStep_returnsStep3ViewWithCompletedForm() {
    when(portalSessionService.resolveActingCreditor(null, session)).thenReturn(ACTING_CREDITOR);

    session.setAttribute(ClaimCreateController.SESSION_WIZARD_FORM, buildCompletedWizardForm());

    Model model = new ConcurrentModel();
    String view = controller.showReviewStep(model, session);

    assertThat(view).isEqualTo("claims/create/step-review");
    assertThat(model.getAttribute("currentStep")).isEqualTo(3);
  }

  // -----------------------------------------------------------------------
  // Step 3 POST: Submission
  // -----------------------------------------------------------------------

  @Test
  void processSubmission_redirectsToStep4OnSuccess() {
    when(portalSessionService.resolveActingCreditor(null, session)).thenReturn(ACTING_CREDITOR);
    when(debtServiceClient.submitClaimWizard(any()))
        .thenReturn(
            ClaimSubmissionResultDto.builder()
                .outcome("UDFOERT")
                .claimId(UUID.randomUUID())
                .processingStatus("ACCEPTED")
                .build());

    session.setAttribute(ClaimCreateController.SESSION_WIZARD_FORM, buildCompletedWizardForm());

    Model model = new ConcurrentModel();
    String view = controller.processSubmission(model, session);

    assertThat(view).isEqualTo("redirect:/fordring/opret/step/4");
    assertThat(session.getAttribute(ClaimCreateController.SESSION_WIZARD_RESULT)).isNotNull();
  }

  @Test
  void processSubmission_redisplaysReviewOnError() {
    when(portalSessionService.resolveActingCreditor(null, session)).thenReturn(ACTING_CREDITOR);
    when(debtServiceClient.submitClaimWizard(any()))
        .thenThrow(new RuntimeException("Connection refused"));
    when(messageSource.getMessage(eq("wizard.submit.error"), any(), any(), any()))
        .thenReturn("Submission failed.");

    session.setAttribute(ClaimCreateController.SESSION_WIZARD_FORM, buildCompletedWizardForm());

    Model model = new ConcurrentModel();
    String view = controller.processSubmission(model, session);

    assertThat(view).isEqualTo("claims/create/step-review");
    assertThat(model.getAttribute("submissionError")).isNotNull();
  }

  // -----------------------------------------------------------------------
  // Step 4: Result
  // -----------------------------------------------------------------------

  @Test
  void showResultStep_redirectsToStep1WhenNoResult() {
    when(portalSessionService.resolveActingCreditor(null, session)).thenReturn(ACTING_CREDITOR);

    Model model = new ConcurrentModel();
    String view = controller.showResultStep(model, session);

    assertThat(view).isEqualTo("redirect:/fordring/opret/step/1");
  }

  @Test
  void showResultStep_displaysAcceptedReceipt() {
    when(portalSessionService.resolveActingCreditor(null, session)).thenReturn(ACTING_CREDITOR);

    ClaimSubmissionResultDto result =
        ClaimSubmissionResultDto.builder()
            .outcome("UDFOERT")
            .claimId(UUID.randomUUID())
            .processingStatus("ACCEPTED")
            .build();
    session.setAttribute(ClaimCreateController.SESSION_WIZARD_RESULT, result);
    session.setAttribute(ClaimCreateController.SESSION_WIZARD_FORM, buildCompletedWizardForm());

    Model model = new ConcurrentModel();
    String view = controller.showResultStep(model, session);

    assertThat(view).isEqualTo("claims/create/step-result");
    assertThat(model.getAttribute("result")).isEqualTo(result);
    assertThat(model.getAttribute("currentStep")).isEqualTo(4);
    // Session should be cleaned up
    assertThat(session.getAttribute(ClaimCreateController.SESSION_WIZARD_FORM)).isNull();
    assertThat(session.getAttribute(ClaimCreateController.SESSION_WIZARD_RESULT)).isNull();
  }

  @Test
  void showResultStep_displaysRejectionErrors() {
    when(portalSessionService.resolveActingCreditor(null, session)).thenReturn(ACTING_CREDITOR);

    ClaimSubmissionResultDto result =
        ClaimSubmissionResultDto.builder()
            .outcome("AFVIST")
            .processingStatus("REJECTED")
            .errors(
                List.of(
                    dk.ufst.opendebt.creditor.dto.ValidationErrorDto.builder()
                        .errorCode(1001)
                        .description("Invalid claim type")
                        .build()))
            .build();
    session.setAttribute(ClaimCreateController.SESSION_WIZARD_RESULT, result);
    session.setAttribute(ClaimCreateController.SESSION_WIZARD_FORM, buildCompletedWizardForm());

    Model model = new ConcurrentModel();
    String view = controller.showResultStep(model, session);

    assertThat(view).isEqualTo("claims/create/step-result");
    ClaimSubmissionResultDto displayed = (ClaimSubmissionResultDto) model.getAttribute("result");
    assertThat(displayed.getOutcome()).isEqualTo("AFVIST");
    assertThat(displayed.getErrors()).hasSize(1);
  }

  @Test
  void showResultStep_displaysHearingInfo() {
    when(portalSessionService.resolveActingCreditor(null, session)).thenReturn(ACTING_CREDITOR);

    ClaimSubmissionResultDto result =
        ClaimSubmissionResultDto.builder()
            .outcome("HOERING")
            .claimId(UUID.randomUUID())
            .processingStatus("HEARING")
            .build();
    session.setAttribute(ClaimCreateController.SESSION_WIZARD_RESULT, result);
    session.setAttribute(ClaimCreateController.SESSION_WIZARD_FORM, buildCompletedWizardForm());

    Model model = new ConcurrentModel();
    String view = controller.showResultStep(model, session);

    assertThat(view).isEqualTo("claims/create/step-result");
    ClaimSubmissionResultDto displayed = (ClaimSubmissionResultDto) model.getAttribute("result");
    assertThat(displayed.getOutcome()).isEqualTo("HOERING");
  }

  // -----------------------------------------------------------------------
  // Helper methods
  // -----------------------------------------------------------------------

  private CreditorAgreementDto buildAgreement() {
    return CreditorAgreementDto.builder()
        .portalActionsAllowed(true)
        .allowCreateRecoveryClaims(true)
        .allowedClaimTypes(List.of("SKAT", "KOMMUNE"))
        .allowedDebtorTypes(List.of("CPR", "CVR", "SE", "AKR"))
        .allowedInterestRules(List.of("STANDARD", "CUSTOM"))
        .notificationPreference("EMAIL")
        .build();
  }

  private ClaimWizardFormDto buildVerifiedWizardForm() {
    return ClaimWizardFormDto.builder()
        .debtorType("CPR")
        .debtorIdentifier("1234567890")
        .debtorFirstName("John")
        .debtorLastName("Doe")
        .debtorVerified(true)
        .debtorDisplayName("John Doe")
        .debtorPersonId(UUID.randomUUID())
        .build();
  }

  private ClaimWizardFormDto buildCompletedWizardForm() {
    return ClaimWizardFormDto.builder()
        .debtorType("CPR")
        .debtorIdentifier("1234567890")
        .debtorFirstName("John")
        .debtorLastName("Doe")
        .debtorVerified(true)
        .debtorDisplayName("John Doe")
        .debtorPersonId(UUID.randomUUID())
        .claimType("SKAT")
        .amount(new BigDecimal("1000.00"))
        .principalAmount(new BigDecimal("500.00"))
        .creditorReference("REF-001")
        .description("Test claim description")
        .limitationDate(java.time.LocalDate.of(2027, 12, 31))
        .estateProcessing(false)
        .build();
  }
}
