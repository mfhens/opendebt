package dk.ufst.opendebt.creditor.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
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
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import dk.ufst.opendebt.creditor.client.CreditorServiceClient;
import dk.ufst.opendebt.creditor.client.DebtServiceClient;
import dk.ufst.opendebt.creditor.dto.*;
import dk.ufst.opendebt.creditor.service.PortalSessionService;

@ExtendWith(MockitoExtension.class)
class ClaimAdjustmentControllerTest {

  private static final UUID TEST_CREDITOR_ORG_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID TEST_CLAIM_ID = UUID.fromString("00000000-0000-0000-0000-000000000100");

  @Mock private DebtServiceClient debtServiceClient;
  @Mock private CreditorServiceClient creditorServiceClient;
  @Mock private PortalSessionService portalSessionService;
  @Mock private MessageSource messageSource;

  @InjectMocks private ClaimAdjustmentController controller;

  private MockHttpSession session;

  @BeforeEach
  void setUp() {
    session = new MockHttpSession();
  }

  // --- GET form tests ---

  @Test
  void showForm_redirectsToDemoLogin_whenNoSession() {
    when(portalSessionService.resolveActingCreditor(eq(null), any())).thenReturn(null);

    Model model = new ConcurrentModel();
    String viewName = controller.showAdjustmentForm(TEST_CLAIM_ID, "WRITE_DOWN", model, session);

    assertThat(viewName).isEqualTo("redirect:/demo-login");
  }

  @Test
  void showForm_showsPermissionError_whenAgreementDisallowsPortalActions() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    CreditorAgreementDto agreement =
        CreditorAgreementDto.builder().portalActionsAllowed(false).build();
    when(creditorServiceClient.getCreditorAgreement(TEST_CREDITOR_ORG_ID)).thenReturn(agreement);
    when(messageSource.getMessage(eq("adjustment.error.nopermission"), any(), any(Locale.class)))
        .thenReturn("No permission.");

    Model model = new ConcurrentModel();
    String viewName = controller.showAdjustmentForm(TEST_CLAIM_ID, "WRITE_DOWN", model, session);

    assertThat(viewName).isEqualTo("claims/adjustment/form");
    assertThat(model.getAttribute("serviceError")).isEqualTo("No permission.");
  }

  @Test
  void showForm_showsNoTypesError_whenNoTypesPermitted() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    CreditorAgreementDto agreement =
        CreditorAgreementDto.builder().portalActionsAllowed(true).build();
    when(creditorServiceClient.getCreditorAgreement(TEST_CREDITOR_ORG_ID)).thenReturn(agreement);
    when(messageSource.getMessage(eq("adjustment.error.notypes"), any(), any(Locale.class)))
        .thenReturn("No types.");

    Model model = new ConcurrentModel();
    String viewName = controller.showAdjustmentForm(TEST_CLAIM_ID, "WRITE_DOWN", model, session);

    assertThat(viewName).isEqualTo("claims/adjustment/form");
    assertThat(model.getAttribute("serviceError")).isEqualTo("No types.");
  }

  @Test
  void showForm_displaysWriteDownForm_withAllowedTypes() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    CreditorAgreementDto agreement =
        CreditorAgreementDto.builder().portalActionsAllowed(true).allowWriteDown(true).build();
    when(creditorServiceClient.getCreditorAgreement(TEST_CREDITOR_ORG_ID)).thenReturn(agreement);
    when(debtServiceClient.getClaimDetail(TEST_CLAIM_ID)).thenReturn(buildSingleDebtorClaim());

    Model model = new ConcurrentModel();
    String viewName = controller.showAdjustmentForm(TEST_CLAIM_ID, "WRITE_DOWN", model, session);

    assertThat(viewName).isEqualTo("claims/adjustment/form");
    assertThat(model.getAttribute("direction")).isEqualTo("WRITE_DOWN");
    assertThat(model.getAttribute("allowedTypes")).isNotNull();
    @SuppressWarnings("unchecked")
    List<ClaimAdjustmentType> types =
        (List<ClaimAdjustmentType>) model.getAttribute("allowedTypes");
    assertThat(types).contains(ClaimAdjustmentType.NEDSKRIV);
    assertThat(model.getAttribute("multiDebtor")).isEqualTo(false);
  }

  @Test
  void showForm_displaysWriteUpForm_withAllowedTypes() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    CreditorAgreementDto agreement =
        CreditorAgreementDto.builder()
            .portalActionsAllowed(true)
            .allowWriteUpAdjustment(true)
            .build();
    when(creditorServiceClient.getCreditorAgreement(TEST_CREDITOR_ORG_ID)).thenReturn(agreement);
    when(debtServiceClient.getClaimDetail(TEST_CLAIM_ID)).thenReturn(buildSingleDebtorClaim());

    Model model = new ConcurrentModel();
    String viewName = controller.showAdjustmentForm(TEST_CLAIM_ID, "WRITE_UP", model, session);

    assertThat(viewName).isEqualTo("claims/adjustment/form");
    assertThat(model.getAttribute("direction")).isEqualTo("WRITE_UP");
    @SuppressWarnings("unchecked")
    List<ClaimAdjustmentType> types =
        (List<ClaimAdjustmentType>) model.getAttribute("allowedTypes");
    assertThat(types).contains(ClaimAdjustmentType.OPSKRIVNING_REGULERING);
  }

  @Test
  void showForm_setsMultiDebtorFlag_forMultiDebtorClaims() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    CreditorAgreementDto agreement =
        CreditorAgreementDto.builder()
            .portalActionsAllowed(true)
            .allowWriteDownPayment(true)
            .build();
    when(creditorServiceClient.getCreditorAgreement(TEST_CREDITOR_ORG_ID)).thenReturn(agreement);
    when(debtServiceClient.getClaimDetail(TEST_CLAIM_ID)).thenReturn(buildMultiDebtorClaim());

    Model model = new ConcurrentModel();
    controller.showAdjustmentForm(TEST_CLAIM_ID, "WRITE_DOWN", model, session);

    assertThat(model.getAttribute("multiDebtor")).isEqualTo(true);
  }

  @Test
  void showForm_censorsDebtorCprNumbers() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    CreditorAgreementDto agreement =
        CreditorAgreementDto.builder().portalActionsAllowed(true).allowWriteDown(true).build();
    when(creditorServiceClient.getCreditorAgreement(TEST_CREDITOR_ORG_ID)).thenReturn(agreement);
    ClaimDetailDto claim = buildMultiDebtorClaim();
    when(debtServiceClient.getClaimDetail(TEST_CLAIM_ID)).thenReturn(claim);

    Model model = new ConcurrentModel();
    controller.showAdjustmentForm(TEST_CLAIM_ID, "WRITE_DOWN", model, session);

    ClaimDetailDto result = (ClaimDetailDto) model.getAttribute("claim");
    assertThat(result).isNotNull();
    // CPR debtor should be censored
    DebtorInfoDto cprDebtor =
        result.getDebtors().stream()
            .filter(d -> "CPR".equals(d.getIdentifierType()))
            .findFirst()
            .orElse(null);
    assertThat(cprDebtor).isNotNull();
    assertThat(cprDebtor.getIdentifier()).isEqualTo("010190****");
    // CVR debtor should not be censored
    DebtorInfoDto cvrDebtor =
        result.getDebtors().stream()
            .filter(d -> "CVR".equals(d.getIdentifierType()))
            .findFirst()
            .orElse(null);
    assertThat(cvrDebtor).isNotNull();
    assertThat(cvrDebtor.getIdentifier()).isEqualTo("12345678");
  }

  // --- POST submit tests ---

  @Test
  void submitAdjustment_redirectsToDemoLogin_whenNoSession() {
    when(portalSessionService.resolveActingCreditor(eq(null), any())).thenReturn(null);

    ClaimAdjustmentRequestDto form = new ClaimAdjustmentRequestDto();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "adjustmentForm");
    Model model = new ConcurrentModel();
    RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

    String viewName =
        controller.submitAdjustment(
            TEST_CLAIM_ID, form, bindingResult, "WRITE_DOWN", model, session, redirectAttributes);

    assertThat(viewName).isEqualTo("redirect:/demo-login");
  }

  @Test
  void submitAdjustment_showsPermissionError_whenNotAllowed() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    CreditorAgreementDto agreement =
        CreditorAgreementDto.builder().portalActionsAllowed(false).build();
    when(creditorServiceClient.getCreditorAgreement(TEST_CREDITOR_ORG_ID)).thenReturn(agreement);
    when(messageSource.getMessage(eq("adjustment.error.nopermission"), any(), any(Locale.class)))
        .thenReturn("No permission.");

    ClaimAdjustmentRequestDto form = new ClaimAdjustmentRequestDto();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "adjustmentForm");
    Model model = new ConcurrentModel();
    RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

    String viewName =
        controller.submitAdjustment(
            TEST_CLAIM_ID, form, bindingResult, "WRITE_DOWN", model, session, redirectAttributes);

    assertThat(viewName).isEqualTo("claims/adjustment/form");
    assertThat(model.getAttribute("serviceError")).isEqualTo("No permission.");
  }

  @Test
  void submitAdjustment_rejectsUnpermittedType() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    CreditorAgreementDto agreement =
        CreditorAgreementDto.builder().portalActionsAllowed(true).allowWriteDown(true).build();
    when(creditorServiceClient.getCreditorAgreement(TEST_CREDITOR_ORG_ID)).thenReturn(agreement);
    when(debtServiceClient.getClaimDetail(TEST_CLAIM_ID)).thenReturn(buildSingleDebtorClaim());
    when(messageSource.getMessage(
            eq("adjustment.validation.type.notpermitted"), any(), any(Locale.class)))
        .thenReturn("Type not permitted.");

    ClaimAdjustmentRequestDto form =
        ClaimAdjustmentRequestDto.builder()
            .adjustmentType(ClaimAdjustmentType.OPSKRIVNING_REGULERING) // not permitted
            .amount(new BigDecimal("100.00"))
            .effectiveDate(LocalDate.of(2026, 1, 1))
            .reason("Test")
            .build();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "adjustmentForm");
    Model model = new ConcurrentModel();
    RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

    String viewName =
        controller.submitAdjustment(
            TEST_CLAIM_ID, form, bindingResult, "WRITE_DOWN", model, session, redirectAttributes);

    assertThat(viewName).isEqualTo("claims/adjustment/form");
    assertThat(bindingResult.hasErrors()).isTrue();
  }

  @Test
  void submitAdjustment_requiresDebtorForPaymentType_withMultiDebtors() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    CreditorAgreementDto agreement =
        CreditorAgreementDto.builder()
            .portalActionsAllowed(true)
            .allowWriteDownPayment(true)
            .build();
    when(creditorServiceClient.getCreditorAgreement(TEST_CREDITOR_ORG_ID)).thenReturn(agreement);
    when(debtServiceClient.getClaimDetail(TEST_CLAIM_ID)).thenReturn(buildMultiDebtorClaim());
    when(messageSource.getMessage(
            eq("adjustment.validation.debtor.required"), any(), any(Locale.class)))
        .thenReturn("Select a debtor.");

    ClaimAdjustmentRequestDto form =
        ClaimAdjustmentRequestDto.builder()
            .adjustmentType(ClaimAdjustmentType.NEDSKRIVNING_INDBETALING)
            .amount(new BigDecimal("100.00"))
            .effectiveDate(LocalDate.of(2026, 1, 1))
            .reason("Payment received")
            .debtorIndex(null) // not selected
            .build();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "adjustmentForm");
    Model model = new ConcurrentModel();
    RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

    String viewName =
        controller.submitAdjustment(
            TEST_CLAIM_ID, form, bindingResult, "WRITE_DOWN", model, session, redirectAttributes);

    assertThat(viewName).isEqualTo("claims/adjustment/form");
    assertThat(bindingResult.hasFieldErrors("debtorIndex")).isTrue();
  }

  @Test
  void submitAdjustment_successfulSubmission_redirectsToReceipt() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    CreditorAgreementDto agreement =
        CreditorAgreementDto.builder().portalActionsAllowed(true).allowWriteDown(true).build();
    when(creditorServiceClient.getCreditorAgreement(TEST_CREDITOR_ORG_ID)).thenReturn(agreement);
    when(debtServiceClient.getClaimDetail(TEST_CLAIM_ID)).thenReturn(buildSingleDebtorClaim());

    AdjustmentReceiptDto receipt =
        AdjustmentReceiptDto.builder()
            .actionId("AKT-001")
            .status("PROCESSED")
            .amount(new BigDecimal("500.00"))
            .adjustmentType("NEDSKRIV")
            .build();
    when(debtServiceClient.submitAdjustment(eq(TEST_CLAIM_ID), any())).thenReturn(receipt);

    ClaimAdjustmentRequestDto form =
        ClaimAdjustmentRequestDto.builder()
            .adjustmentType(ClaimAdjustmentType.NEDSKRIV)
            .amount(new BigDecimal("500.00"))
            .effectiveDate(LocalDate.of(2026, 1, 1))
            .reason("Correction")
            .build();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "adjustmentForm");
    Model model = new ConcurrentModel();
    RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

    String viewName =
        controller.submitAdjustment(
            TEST_CLAIM_ID, form, bindingResult, "WRITE_DOWN", model, session, redirectAttributes);

    assertThat(viewName).isEqualTo("redirect:/fordring/" + TEST_CLAIM_ID + "/adjustment/receipt");
    assertThat(redirectAttributes.getFlashAttributes().get("receipt")).isEqualTo(receipt);
    verify(debtServiceClient).submitAdjustment(eq(TEST_CLAIM_ID), any());
  }

  @Test
  void submitAdjustment_censorsDebtorIdInReceipt() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    CreditorAgreementDto agreement =
        CreditorAgreementDto.builder()
            .portalActionsAllowed(true)
            .allowWriteDownPayment(true)
            .build();
    when(creditorServiceClient.getCreditorAgreement(TEST_CREDITOR_ORG_ID)).thenReturn(agreement);
    when(debtServiceClient.getClaimDetail(TEST_CLAIM_ID)).thenReturn(buildSingleDebtorClaim());

    AdjustmentReceiptDto receipt =
        AdjustmentReceiptDto.builder()
            .actionId("AKT-002")
            .status("PROCESSED")
            .amount(new BigDecimal("200.00"))
            .debtorIdentifier("0101901234")
            .adjustmentType("NEDSKRIVNING_INDBETALING")
            .build();
    when(debtServiceClient.submitAdjustment(eq(TEST_CLAIM_ID), any())).thenReturn(receipt);

    ClaimAdjustmentRequestDto form =
        ClaimAdjustmentRequestDto.builder()
            .adjustmentType(ClaimAdjustmentType.NEDSKRIVNING_INDBETALING)
            .amount(new BigDecimal("200.00"))
            .effectiveDate(LocalDate.of(2026, 1, 1))
            .reason("Payment")
            .build();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "adjustmentForm");
    Model model = new ConcurrentModel();
    RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

    controller.submitAdjustment(
        TEST_CLAIM_ID, form, bindingResult, "WRITE_DOWN", model, session, redirectAttributes);

    AdjustmentReceiptDto result =
        (AdjustmentReceiptDto) redirectAttributes.getFlashAttributes().get("receipt");
    assertThat(result.getDebtorIdentifier()).isEqualTo("010190****");
  }

  @Test
  void submitAdjustment_handlesBackendError_reloadsForm() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);
    CreditorAgreementDto agreement =
        CreditorAgreementDto.builder().portalActionsAllowed(true).allowWriteDown(true).build();
    when(creditorServiceClient.getCreditorAgreement(TEST_CREDITOR_ORG_ID)).thenReturn(agreement);
    when(debtServiceClient.getClaimDetail(TEST_CLAIM_ID)).thenReturn(buildSingleDebtorClaim());
    when(debtServiceClient.submitAdjustment(eq(TEST_CLAIM_ID), any()))
        .thenThrow(new RuntimeException("Connection refused"));
    when(messageSource.getMessage(eq("adjustment.submit.error"), any(), any(Locale.class)))
        .thenReturn("Submission failed.");

    ClaimAdjustmentRequestDto form =
        ClaimAdjustmentRequestDto.builder()
            .adjustmentType(ClaimAdjustmentType.NEDSKRIV)
            .amount(new BigDecimal("500.00"))
            .effectiveDate(LocalDate.of(2026, 1, 1))
            .reason("Correction")
            .build();
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "adjustmentForm");
    Model model = new ConcurrentModel();
    RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

    String viewName =
        controller.submitAdjustment(
            TEST_CLAIM_ID, form, bindingResult, "WRITE_DOWN", model, session, redirectAttributes);

    assertThat(viewName).isEqualTo("claims/adjustment/form");
    assertThat(model.getAttribute("actionError")).isEqualTo("Submission failed.");
  }

  // --- Receipt page tests ---

  @Test
  void showReceipt_redirectsToDemoLogin_whenNoSession() {
    when(portalSessionService.resolveActingCreditor(eq(null), any())).thenReturn(null);

    Model model = new ConcurrentModel();
    String viewName = controller.showReceipt(TEST_CLAIM_ID, model, session);

    assertThat(viewName).isEqualTo("redirect:/demo-login");
  }

  @Test
  void showReceipt_redirectsToClaimDetail_whenNoReceiptInModel() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);

    Model model = new ConcurrentModel();
    String viewName = controller.showReceipt(TEST_CLAIM_ID, model, session);

    assertThat(viewName).isEqualTo("redirect:/fordring/" + TEST_CLAIM_ID);
  }

  @Test
  void showReceipt_displaysReceipt_whenFlashAttributePresent() {
    when(portalSessionService.resolveActingCreditor(eq(null), any()))
        .thenReturn(TEST_CREDITOR_ORG_ID);

    Model model = new ConcurrentModel();
    AdjustmentReceiptDto receipt =
        AdjustmentReceiptDto.builder().actionId("AKT-001").status("PROCESSED").build();
    model.addAttribute("receipt", receipt);

    String viewName = controller.showReceipt(TEST_CLAIM_ID, model, session);

    assertThat(viewName).isEqualTo("claims/adjustment/receipt");
    assertThat(model.getAttribute("claimId")).isEqualTo(TEST_CLAIM_ID);
  }

  // --- Helper methods ---

  private ClaimDetailDto buildSingleDebtorClaim() {
    return ClaimDetailDto.builder()
        .claimId(TEST_CLAIM_ID)
        .claimType("SKAT")
        .claimCategory("HOVEDFORDRING")
        .debtorCount(1)
        .debtors(
            List.of(DebtorInfoDto.builder().identifierType("CPR").identifier("0101901234").build()))
        .zeroBalanceExpired(false)
        .build();
  }

  private ClaimDetailDto buildMultiDebtorClaim() {
    return ClaimDetailDto.builder()
        .claimId(TEST_CLAIM_ID)
        .claimType("SKAT")
        .claimCategory("HOVEDFORDRING")
        .debtorCount(3)
        .debtors(
            List.of(
                DebtorInfoDto.builder().identifierType("CPR").identifier("0101901234").build(),
                DebtorInfoDto.builder().identifierType("CVR").identifier("12345678").build(),
                DebtorInfoDto.builder().identifierType("SE").identifier("87654321").build()))
        .zeroBalanceExpired(false)
        .build();
  }
}
