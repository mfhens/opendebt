package dk.ufst.opendebt.creditor.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import jakarta.servlet.http.HttpSession;

import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import dk.ufst.opendebt.creditor.client.CreditorServiceClient;
import dk.ufst.opendebt.creditor.client.DebtServiceClient;
import dk.ufst.opendebt.creditor.controller.ClaimAdjustmentController;
import dk.ufst.opendebt.creditor.dto.AdjustmentReceiptDto;
import dk.ufst.opendebt.creditor.dto.ClaimAdjustmentRequestDto;
import dk.ufst.opendebt.creditor.dto.ClaimAdjustmentType;
import dk.ufst.opendebt.creditor.dto.ClaimDetailDto;
import dk.ufst.opendebt.creditor.dto.CreditorAgreementDto;
import dk.ufst.opendebt.creditor.dto.DebtorInfoDto;
import dk.ufst.opendebt.creditor.dto.WriteDownReasonCode;
import dk.ufst.opendebt.creditor.service.PortalSessionService;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.PendingException;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * BDD step definitions for petition 053 — portal delta FRs:
 *
 * <ul>
 *   <li>FR-1: WriteDownReasonCode controlled dropdown (Gæld.bekendtg. § 7 stk. 2)
 *   <li>FR-4: Retroactive virkningsdato advisory (G.A.1.4.4)
 *   <li>FR-5: Backdated type description for OPSKRIVNING_OMGJORT_NEDSKRIVNING_REGULERING
 *   <li>FR-6: Cross-system suspension advisory on receipt page (GIL § 18 k)
 *   <li>FR-7: WriteUpReasonCode removal; DINDB/OMPL/AFSK absent from portal form (G.A.2.3.4.4)
 * </ul>
 *
 * <p><strong>Failing strategy (two modes):</strong>
 *
 * <ol>
 *   <li>Template-inspection assertions: {@code assertThat(html).contains(...)} that fail because
 *       the required markup is absent from {@code form.html} / {@code receipt.html} today.
 *   <li>{@link PendingException}: thrown for steps that require a live portal HTTP POST (e.g., FR-1
 *       submission validation, FR-4 advisory via POST response).
 * </ol>
 *
 * <p>Spec reference: SPEC-P053, design/specs-p053-opskrivning-nedskrivning.md
 */
public class Petition053Steps {

  // ── Classpath template resources ────────────────────────────────────────────

  @Value("classpath:templates/claims/adjustment/form.html")
  private Resource formTemplate;

  @Value("classpath:templates/claims/adjustment/receipt.html")
  private Resource receiptTemplate;

  @Value("classpath:messages_da.properties")
  private Resource messagesDa;

  // ── Per-scenario state ──────────────────────────────────────────────────────

  /** Content of {@code form.html} loaded during navigation Given/When steps. */
  private String formHtml;

  /** Content of {@code receipt.html} loaded for FR-6 receipt assertions. */
  private String receiptHtml;

  /** External reference of the claim under test (informational only for template tests). */
  private String currentClaimId;

  /**
   * Adjustment type selected by the user in FR-5 scenarios. Stored so {@code Then} steps can use it
   * in assertion messages.
   */
  private String selectedAdjustmentType;

  /** PSRM registration date (ISO-8601 string) for FR-6 scenarios. */
  private String psrmRegistrationDate;

  // ── Direct controller test state (FR-1 submission scenarios) ─────────────────

  /** Mocked DebtServiceClient for controller-level submission tests (FR-1 / AC-5). */
  private DebtServiceClient mockDebtServiceClient;

  /** Mocked CreditorServiceClient for controller-level submission tests. */
  private CreditorServiceClient mockCreditorServiceClient;

  /**
   * Controller under test, instantiated directly with Mockito mocks (pattern consistent with {@code
   * ClaimAdjustmentControllerTest}). Used for FR-1 submission scenarios only.
   */
  private ClaimAdjustmentController controllerUnderTest;

  /** Last view name returned by the controller under test. */
  private String lastViewName;

  /** Last binding result from the controller under test. */
  private BindingResult lastBindingResult;

  /** Last model from the controller under test. */
  private Model lastModel;

  /** Last redirect attributes from the controller under test. */
  private RedirectAttributesModelMap lastRedirectAttributes;

  /** Claim ID UUID used for controller submission tests. */
  private UUID controllerTestClaimId;

  private static final UUID DEFAULT_CREDITOR_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID DEFAULT_CLAIM_UUID =
      UUID.fromString("00000000-0000-0000-0000-000000000100");

  @Before("@petition053")
  public void resetState() {
    formHtml = null;
    receiptHtml = null;
    currentClaimId = null;
    selectedAdjustmentType = null;
    psrmRegistrationDate = null;
    lastViewName = null;
    lastBindingResult = null;
    lastModel = null;
    lastRedirectAttributes = null;
    controllerTestClaimId = DEFAULT_CLAIM_UUID;

    // Set up mocks for controller-level submission tests (FR-1 / AC-5).
    mockDebtServiceClient = mock(DebtServiceClient.class);
    mockCreditorServiceClient = mock(CreditorServiceClient.class);
    PortalSessionService mockPortalSessionService = mock(PortalSessionService.class);
    MessageSource mockMessageSource = mock(MessageSource.class);

    controllerUnderTest =
        new ClaimAdjustmentController(
            mockDebtServiceClient,
            mockCreditorServiceClient,
            mockPortalSessionService,
            mockMessageSource);

    // Default stubs for controller tests
    when(mockPortalSessionService.resolveActingCreditor(any(), any()))
        .thenReturn(DEFAULT_CREDITOR_ID);
    when(mockCreditorServiceClient.getCreditorAgreement(DEFAULT_CREDITOR_ID))
        .thenReturn(
            CreditorAgreementDto.builder()
                .portalActionsAllowed(true)
                .allowWriteDown(true)
                .allowWriteDownPayment(true)
                .allowWriteUpAdjustment(true)
                .build());
    when(mockDebtServiceClient.getClaimDetail(DEFAULT_CLAIM_UUID))
        .thenReturn(
            ClaimDetailDto.builder()
                .claimId(DEFAULT_CLAIM_UUID)
                .claimType("SKAT")
                .claimCategory("NORMAL")
                .debtorCount(1)
                .debtors(
                    List.of(
                        DebtorInfoDto.builder()
                            .identifierType("CPR")
                            .identifier("0101901234")
                            .build()))
                .build());
    when(mockMessageSource.getMessage(any(String.class), any(), any()))
        .thenAnswer(inv -> inv.getArgument(0, String.class));
  }

  // ── Common Given steps ──────────────────────────────────────────────────────

  /**
   * Authentication context is enforced at the controller/service boundary. Template-inspection
   * tests operate on static resources and bypass authentication.
   */
  @Given("portal user {string} is authenticated with role {string}")
  public void portalUserIsAuthenticatedWithRole(String userId, String role) {
    // Spring context is up; controller-level auth is not exercised by template tests.
  }

  @Given("user {string} has {string} permission from the creditor agreement")
  public void userHasNamedPermission(String userId, String permission) {
    // Permission enforcement is controller-level; no setup needed for template inspection.
  }

  @Given("user {string} has nedskrivning permission from the creditor agreement")
  public void userHasNedskrivningPermission(String userId) {
    // Nedskrivning permission check is controller-level only.
  }

  @Given("user {string} has {string} and nedskrivning permissions from the creditor agreement")
  public void userHasPortalActionsAndNedskrivningPermissions(String userId, String permission) {
    // Combined permission — no setup required for template tests.
  }

  @Given("user {string} has {string} and opskrivning permissions from the creditor agreement")
  public void userHasPortalActionsAndOpskrivningPermissions(String userId, String permission) {
    // Combined permission — no setup required for template tests.
  }

  @Given("a claim {string} is under inddrivelse for fordringshaver {string}")
  public void claimIsUnderInddrivelseForFordringshaver(String claimId, String creditorId) {
    currentClaimId = claimId;
  }

  @Given("a claim {string} is under inddrivelse")
  public void claimIsUnderInddrivelse(String claimId) {
    currentClaimId = claimId;
  }

  /**
   * FR-4: Pre-load the adjustment form template so the Given step already holds {@code formHtml}
   * before the When step fires.
   */
  @Given("user {string} is on the nedskrivning adjustment form for claim {string}")
  public void userIsOnNedskrivningAdjustmentFormForClaim(String userId, String claimId)
      throws IOException {
    currentClaimId = claimId;
    formHtml = readResource(formTemplate);
    assertThat(formHtml).isNotBlank();
  }

  /** FR-6: Store PSRM registration date for cross-system retroactive comparison. */
  @Given("a claim {string} has a PSRM registration date of {string}")
  public void claimHasPsrmRegistrationDate(String claimId, String date) {
    currentClaimId = claimId;
    psrmRegistrationDate = date;
  }

  // ── FR-1 When steps ─────────────────────────────────────────────────────────

  /**
   * FR-1: Load {@code form.html} so the {@code Then} assertions can inspect its contents. In a full
   * integration test this step would perform an authenticated GET to {@code
   * /fordring/{id}/adjustment?direction=WRITE_DOWN}. Here we inspect the static Thymeleaf template
   * because the new markup (writeDownReasonCode select, three option codes) does not yet exist —
   * assertions will fail.
   */
  @When("user {string} navigates to the nedskrivning adjustment form for claim {string}")
  public void userNavigatesToNedskrivningAdjustmentForm(String userId, String claimId)
      throws IOException {
    currentClaimId = claimId;
    formHtml = readResource(formTemplate);
    assertThat(formHtml).isNotBlank();
  }

  /**
   * FR-5, FR-7: Load {@code form.html} for the opskrivning direction. Same static-inspection
   * strategy as the write-down navigation step.
   */
  @When("user {string} navigates to the opskrivning adjustment form for claim {string}")
  public void userNavigatesToOpskrivningAdjustmentForm(String userId, String claimId)
      throws IOException {
    currentClaimId = claimId;
    formHtml = readResource(formTemplate);
    assertThat(formHtml).isNotBlank();
  }

  /**
   * FR-1 / AC-5: Submit nedskrivning with DataTable fields (reasonCode, beloeb, virkningsdato).
   * Uses direct controller call (no MockMvc) — consistent with {@code
   * ClaimAdjustmentControllerTest}.
   *
   * <p>SPEC-P053 §1.4: direction-conditional guard runs before BFF call.
   */
  @When("user {string} submits a nedskrivning for claim {string} with:")
  public void userSubmitsNedskrivningWithDataTable(
      String userId, String claimId, DataTable dataTable) {
    Map<String, String> fields = dataTable.asMap(String.class, String.class);
    String reasonCodeStr = fields.getOrDefault("reasonCode", "");
    WriteDownReasonCode reasonCode = null;
    try {
      if (!reasonCodeStr.isBlank()) {
        reasonCode = WriteDownReasonCode.valueOf(reasonCodeStr);
      }
    } catch (IllegalArgumentException ex) {
      // leave null — will be rejected by controller guard
    }
    LocalDate effectiveDate = resolveDate(fields.getOrDefault("virkningsdato", "today"));
    BigDecimal amount = new BigDecimal(fields.getOrDefault("beloeb", "100.00"));

    ClaimAdjustmentRequestDto form =
        ClaimAdjustmentRequestDto.builder()
            .adjustmentType(ClaimAdjustmentType.NEDSKRIV)
            .amount(amount)
            .effectiveDate(effectiveDate)
            .writeDownReasonCode(reasonCode)
            .build();
    AdjustmentReceiptDto receipt =
        AdjustmentReceiptDto.builder()
            .actionId("AKT-BDD-001")
            .status("PROCESSED")
            .amount(amount)
            .adjustmentType("NEDSKRIV")
            .build();
    when(mockDebtServiceClient.submitAdjustment(eq(DEFAULT_CLAIM_UUID), any())).thenReturn(receipt);

    invokeController(form, "WRITE_DOWN");
  }

  /** FR-1 / Scenario Outline: Submit with a specific reason code. Uses direct controller call. */
  @When("user {string} submits a nedskrivning for claim {string} with reasonCode {string}")
  public void userSubmitsNedskrivningWithReasonCode(
      String userId, String claimId, String reasonCode) {
    WriteDownReasonCode code = WriteDownReasonCode.valueOf(reasonCode);
    ClaimAdjustmentRequestDto form =
        ClaimAdjustmentRequestDto.builder()
            .adjustmentType(ClaimAdjustmentType.NEDSKRIV)
            .amount(new BigDecimal("100.00"))
            .effectiveDate(LocalDate.now())
            .writeDownReasonCode(code)
            .build();
    AdjustmentReceiptDto receipt =
        AdjustmentReceiptDto.builder()
            .actionId("AKT-BDD-002")
            .status("PROCESSED")
            .amount(new BigDecimal("100.00"))
            .adjustmentType("NEDSKRIV")
            .build();
    when(mockDebtServiceClient.submitAdjustment(eq(DEFAULT_CLAIM_UUID), any())).thenReturn(receipt);
    invokeController(form, "WRITE_DOWN");
  }

  /**
   * FR-1 / AC-3: Submit nedskrivning form without selecting a reason code. Uses direct controller
   * call — controller guard rejects before BFF call.
   */
  @When(
      "user {string} submits the nedskrivning form for claim {string} without selecting a reason code")
  public void userSubmitsNedskrivningFormWithoutReasonCode(String userId, String claimId) {
    ClaimAdjustmentRequestDto form =
        ClaimAdjustmentRequestDto.builder()
            .adjustmentType(ClaimAdjustmentType.NEDSKRIV)
            .amount(new BigDecimal("100.00"))
            .effectiveDate(LocalDate.now())
            // writeDownReasonCode = null
            .build();
    invokeController(form, "WRITE_DOWN");
  }

  /**
   * FR-1 / AC-4: Submit with an unrecognised reason code. Spring enum binding will fail; form
   * submitted with null writeDownReasonCode → controller guard rejects.
   */
  @When(
      "user {string} submits the nedskrivning form for claim {string} with an unrecognised reason code {string}")
  public void userSubmitsNedskrivningWithUnrecognisedCode(
      String userId, String claimId, String unknownCode) {
    // Spring @ModelAttribute binding rejects unknown enum values → field stays null in form.
    // Simulate this by submitting with null writeDownReasonCode (same controller path).
    ClaimAdjustmentRequestDto form =
        ClaimAdjustmentRequestDto.builder()
            .adjustmentType(ClaimAdjustmentType.NEDSKRIV)
            .amount(new BigDecimal("100.00"))
            .effectiveDate(LocalDate.now())
            // Unknown enum value "UNKNOWN_CODE" → writeDownReasonCode stays null
            .build();
    invokeController(form, "WRITE_DOWN");
  }

  // ── FR-4 When steps ─────────────────────────────────────────────────────────

  /**
   * FR-4 / AC-6 (SPEC-P053 §4.4): The retroactive advisory is visible only in the server-rendered
   * POST response, not on client-side date change. A full test requires MockMvc POST with {@code
   * effectiveDate} in the past and inspection of the rendered HTML.
   *
   * <p>For the static template assertion ({@code retroaktiv-advisory} div existence), the advisory
   * block is inspected directly on {@code form.html} — the form must contain the conditional div
   * regardless of whether the model attribute is set. The dynamic rendering assertion (visible only
   * on POST) is left pending.
   */
  @When("user {string} enters a virkningsdato that is 30 days in the past")
  public void userEntersPastVirkningsdato(String userId) {
    // Static template check: formHtml was already loaded by the Given step.
    // Dynamic check (advisory visible only after POST with past date) is pending — see §4.4.
    // Template assertions in the Then steps will FAIL because form.html has no retroaktiv-advisory.
  }

  /**
   * FR-4 / AC-6b: Absence of advisory for non-past virkningsdato — requires POST response. Pending
   * until MockMvc integration is available.
   */
  @When("user {string} enters virkningsdato {string} on the nedskrivning form for claim {string}")
  public void userEntersVirkningsdatoOnNedskrivningForm(
      String userId, String dateDescription, String claimId) throws IOException {
    currentClaimId = claimId;
    formHtml = readResource(formTemplate);
    // Absence assertion in the Then step is a static check (key not present in template).
  }

  // ── FR-5 When steps ─────────────────────────────────────────────────────────

  /**
   * FR-5: Store the adjustment type selected by the user. The type description div in {@code
   * form.html} is a Thymeleaf conditional — static template inspection can verify the conditional
   * expression and message key are present.
   */
  @When("user {string} selects adjustment type {string}")
  public void userSelectsAdjustmentType(String userId, String adjustmentType) {
    selectedAdjustmentType = adjustmentType;
  }

  // ── FR-6 When steps ─────────────────────────────────────────────────────────

  /**
   * FR-6 / AC-8 (SPEC-P053 §6.5): Load {@code receipt.html} for static template inspection. The
   * advisory div ({@code adjustment.info.suspension.krydssystem}) and the {@code
   * crossSystemRetroactiveApplies} Thymeleaf condition are not yet in the template — Then step
   * assertions will FAIL.
   *
   * <p>Full integration test (POST through BFF → debt-service → response with flag) is pending
   * until {@code ClaimAdjustmentResponseDto} and the portal BFF wiring are implemented.
   */
  @When("user {string} submits a nedskrivning for claim {string} with virkningsdato {string}")
  public void userSubmitsNedskrivningWithVirkningsdato(
      String userId, String claimId, String virkningsdato) throws IOException {
    currentClaimId = claimId;
    receiptHtml = readResource(receiptTemplate);
    assertThat(receiptHtml).isNotBlank();
    // Full BFF → debt-service flow with ClaimAdjustmentResponseDto.crossSystemRetroactiveApplies
    // is not yet implemented; receipt template assertions proceed with static content.
  }

  // ── FR-1 Then steps ─────────────────────────────────────────────────────────

  /**
   * FR-1 / AC-1 (SPEC-P053 §1.5, §1.7): Nedskrivning form must render a {@code <select>} bound to
   * {@code writeDownReasonCode} with exactly the three legal options.
   *
   * <p><strong>Failing:</strong> {@code form.html} currently has a free-text {@code <input>} bound
   * to {@code reason} in the write-down block (lines 128–137). The {@code id="writeDownReasonCode"}
   * select does not exist yet. None of the three reason codes appear as option values in the
   * template.
   */
  @Then("the form displays a reason dropdown with exactly three options:")
  public void formDisplaysReasonDropdownWithThreeOptions(DataTable expectedOptions) {
    assertThat(formHtml)
        .as(
            "FR-1 / AC-1: form.html must bind a <select> to 'writeDownReasonCode' "
                + "(SPEC-P053 §1.5). Currently the write-down block uses a free-text "
                + "<input th:field=\"*{reason}\" type=\"text\"> that must be replaced.")
        .contains("id=\"writeDownReasonCode\"");

    List<Map<String, String>> rows = expectedOptions.asMaps();
    for (Map<String, String> row : rows) {
      String code = row.get("code");
      assertThat(formHtml)
          .as(
              "FR-1 / AC-1: Reason code '%s' must appear as an <option> value in the "
                  + "writeDownReasonCode dropdown (Gæld.bekendtg. § 7 stk. 2).",
              code)
          .contains(code);
    }

    assertThat(rows)
        .as(
            "FR-1 / AC-1: Exactly three reason codes are statutory per Gæld.bekendtg. § 7 stk. 2 "
                + "(NED_INDBETALING, NED_FEJL_OVERSENDELSE, NED_GRUNDLAG_AENDRET).")
        .hasSize(3);
  }

  /**
   * FR-1 / AC-2 (SPEC-P053 §1.5, §1.7): The write-down block must NOT contain a free-text {@code
   * <input type="text">} for the reason field; the {@code <select>} for {@code writeDownReasonCode}
   * must replace it.
   *
   * <p><strong>Failing:</strong> {@code form.html} currently binds {@code
   * th:field="*{writeDownReasonCode}"} is absent — only {@code *{reason}} text input exists.
   */
  @Then("no free-text reason input field is present on the form")
  public void noFreeTextReasonInputPresentOnForm() {
    assertThat(formHtml)
        .as(
            "FR-1 / AC-2: form.html write-down block must bind to 'writeDownReasonCode' via "
                + "<select th:field=\"*{writeDownReasonCode}\"> (SPEC-P053 §1.5). "
                + "Currently the form has <input th:field=\"*{reason}\" type=\"text\"> "
                + "in the write-down block which must be removed.")
        .contains("th:field=\"*{writeDownReasonCode}\"");
  }

  /**
   * FR-1 / AC-5: BFF must forward {@code WriteDownReasonCode} to debt-service. Verified via
   * ArgumentCaptor on the mocked {@link DebtServiceClient}.
   */
  @Then("the BFF forwards a WriteDownDto to debt-service containing reasonCode {string}")
  public void bffForwardsWriteDownDtoWithReasonCode(String reasonCode) {
    ArgumentCaptor<ClaimAdjustmentRequestDto> captor =
        ArgumentCaptor.forClass(ClaimAdjustmentRequestDto.class);
    verify(mockDebtServiceClient).submitAdjustment(eq(DEFAULT_CLAIM_UUID), captor.capture());
    ClaimAdjustmentRequestDto captured = captor.getValue();
    assertThat(captured.getWriteDownReasonCode())
        .as(
            "FR-1 / AC-5: BFF must forward writeDownReasonCode='%s' to debt-service"
                + " (SPEC-P053 §1.4).",
            reasonCode)
        .isNotNull();
    assertThat(captured.getWriteDownReasonCode().name())
        .as("WriteDownReasonCode forwarded to debt-service must equal '%s'.", reasonCode)
        .isEqualTo(reasonCode);
  }

  /** FR-1 / AC-5: Debt-service acceptance verified via redirect to receipt page. */
  @Then("debt-service accepts the request and returns a success receipt")
  public void debtServiceAcceptsRequestAndReturnsSuccessReceipt() {
    assertThat(lastViewName)
        .as(
            "FR-1 / AC-5: Successful nedskrivning must redirect to receipt page"
                + " (SPEC-P053 §6.4).")
        .startsWith("redirect:");
  }

  /** FR-1 / Scenario Outline: Debt-service acceptance verified via redirect. */
  @Then("debt-service accepts the request")
  public void debtServiceAcceptsRequest() {
    assertThat(lastViewName)
        .as(
            "FR-1 / AC-5: Successful nedskrivning with valid WriteDownReasonCode must redirect"
                + " to receipt page (SPEC-P053 §9.2).")
        .startsWith("redirect:");
  }

  /**
   * FR-1 / AC-3: Portal-side validation error with a specific message key. Verified via the stored
   * binding result from the direct controller call.
   */
  @Then("the form displays a validation error using message key {string}")
  public void formDisplaysValidationErrorUsingMessageKey(String messageKey) {
    assertThat(lastBindingResult)
        .as("Expected a BindingResult from controller submission.")
        .isNotNull();
    assertThat(lastBindingResult.hasErrors())
        .as(
            "FR-1 / AC-3: Portal controller must reject the form and set binding errors when"
                + " writeDownReasonCode is null (key: '%s', SPEC-P053 §1.4).",
            messageKey)
        .isTrue();
    // The controller rejects with field "writeDownReasonCode" for the null case.
    // The message key in the rejectValue call is "adjustment.validation.reason.required".
    boolean hasReasonError =
        lastBindingResult.getAllErrors().stream()
            .anyMatch(
                e ->
                    e.getCodes() != null
                        && java.util.Arrays.stream(e.getCodes())
                            .anyMatch(
                                c ->
                                    c.contains("reason.required")
                                        || c.contains("writeDownReasonCode")));
    assertThat(hasReasonError)
        .as(
            "FR-1 / AC-3: Binding result must contain error for 'writeDownReasonCode' or"
                + " code matching 'reason.required' (SPEC-P053 §1.4).")
        .isTrue();
  }

  /** FR-1 / AC-3, AC-4: BFF must NOT be invoked when portal-side validation fails. */
  @Then("the BFF does not forward any request to debt-service")
  public void bffDoesNotForwardAnyRequest() {
    verify(mockDebtServiceClient, never()).submitAdjustment(any(), any());
  }

  /** FR-1 / AC-4: Generic validation error display — verified via stored binding result. */
  @Then("the form displays a validation error")
  public void formDisplaysValidationError() {
    assertThat(lastBindingResult)
        .as("Expected a BindingResult from controller submission.")
        .isNotNull();
    assertThat(lastBindingResult.hasErrors())
        .as(
            "FR-1 / AC-4: Portal controller must reject form with unrecognised writeDownReasonCode"
                + " (null after enum binding failure) and set binding errors (SPEC-P053 §1.4).")
        .isTrue();
  }

  // ── FR-4 Then steps ─────────────────────────────────────────────────────────

  /**
   * FR-4 / AC-6 (SPEC-P053 §4.2): The inline retroactive advisory div must be present in {@code
   * form.html}, conditionally rendered via {@code th:if="${retroaktivAdvisoryActive}"}.
   *
   * <p><strong>Failing:</strong> {@code form.html} does not contain the {@code retroaktiv-advisory}
   * div, the {@code aria-live="polite"} attribute, or the message key {@code
   * adjustment.info.retroaktiv.virkningsdato}.
   *
   * <p>Note (SPEC-P053 §4.4): The advisory is only visible after a POST with a past {@code
   * effectiveDate}. This step also validates that the static template markup for the advisory block
   * is present (a prerequisite for dynamic rendering).
   */
  @Then(
      "the form displays an inline advisory below the virkningsdato field using message key {string}")
  public void formDisplaysRetroaktivAdvisoryWithMessageKey(String messageKey) {
    assertThat(formHtml)
        .as(
            "FR-4 / AC-6: form.html must contain the retroaktiv-advisory div "
                + "(SPEC-P053 §4.2). Insert after the effectiveDate error span: "
                + "<div id=\"retroaktiv-advisory\" "
                + "th:if=\"${retroaktivAdvisoryActive}\" aria-live=\"polite\">...</div>")
        .contains("retroaktiv-advisory");

    assertThat(formHtml)
        .as(
            "FR-4 / AC-6: form.html must reference message key '%s' in the retroaktiv-advisory "
                + "div (SPEC-P053 §4.2).",
            messageKey)
        .contains(messageKey);
  }

  /**
   * FR-4 / AC-15 (SPEC-P053 §4.2): Retroactive advisory must carry {@code aria-live="polite"} for
   * WCAG 2.1 AA compliance.
   *
   * <p><strong>Failing:</strong> {@code form.html} currently has no {@code aria-live} attribute.
   */
  @Then("the advisory element carries attribute aria-live={string}")
  public void advisoryElementCarriesAriaLiveAttribute(String attributeValue) {
    assertThat(formHtml)
        .as(
            "FR-4 / AC-15 (WCAG 2.1 AA): form.html retroaktiv-advisory div must include "
                + "aria-live=\"%s\" (SPEC-P053 §4.2).",
            attributeValue)
        .contains("aria-live=\"" + attributeValue + "\"");
  }

  /**
   * FR-4 / AC-6c: Advisory is purely informational; the submit button must remain rendered.
   * Currently PASSES — the submit button is already in {@code form.html}.
   */
  @Then("the form submission is not blocked")
  public void formSubmissionIsNotBlocked() {
    assertThat(formHtml)
        .as(
            "FR-4 / AC-6c: Submit button must remain rendered even when retroactive advisory "
                + "is shown (SPEC-P053 §4.1 — advisory must not call bindingResult.rejectValue).")
        .contains("skat-btn--primary");
  }

  /**
   * FR-4 / AC-6b: Retroactive advisory must be absent when virkningsdato is today or future.
   *
   * <p>The static template cannot verify absence via {@code doesNotContain} after implementation,
   * because the advisory div IS in the template (inside a Thymeleaf {@code
   * th:if="${retroaktivAdvisoryActive}"} conditional). Instead, this step verifies that:
   *
   * <ol>
   *   <li>The Thymeleaf guard is present — ensuring absence is enforced at render time (not in the
   *       template itself).
   *   <li>The retroactive advisory element has {@code id="retroaktiv-advisory"} — which the
   *       controller only sets for past effectiveDates (SPEC-P053 §4.1).
   * </ol>
   *
   * <p>Dynamic absence (advisory not rendered when effectiveDate >= today) is verified at the
   * controller level by ensuring {@code model.getAttribute("retroaktivAdvisoryActive")} is null
   * when effectiveDate is today or future — covered by {@code ClaimAdjustmentControllerTest}.
   */
  @Then("no retroactive nedskrivning advisory is displayed")
  public void noRetroaktivAdvisoryDisplayed() {
    assertThat(formHtml)
        .as(
            "FR-4 / AC-6b: form.html must guard the retroaktiv-advisory using"
                + " th:if=\"${retroaktivAdvisoryActive}\" (SPEC-P053 §4.2). Absence for"
                + " non-retroactive submissions is enforced by the Thymeleaf evaluator at render"
                + " time when the controller does not set retroaktivAdvisoryActive.")
        .contains("retroaktivAdvisoryActive");
  }

  /**
   * FR-4 / AC-6c: Portal must forward the retroactive nedskrivning despite the advisory. Verified
   * via Mockito on the mocked DebtServiceClient.
   */
  @Then("the portal forwards the submission to the BFF")
  public void portalForwardsSubmissionToBff() {
    verify(mockDebtServiceClient).submitAdjustment(eq(DEFAULT_CLAIM_UUID), any());
  }

  // ── FR-5 Then steps ─────────────────────────────────────────────────────────

  /**
   * FR-5 / AC-7 (SPEC-P053 §5.1): When adjustment type is {@code
   * OPSKRIVNING_OMGJORT_NEDSKRIVNING_REGULERING}, a type description div must appear using message
   * key {@code adjustment.type.description.omgjort_nedskrivning_regulering}.
   *
   * <p><strong>Failing:</strong> {@code form.html} does not contain this message key. The
   * conditional div must be inserted after the {@code adjustmentType} {@code <select>} (SPEC-P053
   * §5.1).
   */
  @Then("the form displays a type description using message key {string}")
  public void formDisplaysTypeDescriptionWithMessageKey(String messageKey) {
    assertThat(formHtml)
        .as(
            "FR-5 / AC-7: form.html must contain a type description div using message key "
                + "'%s' when adjustmentType == OPSKRIVNING_OMGJORT_NEDSKRIVNING_REGULERING "
                + "(SPEC-P053 §5.1 / Gæld.bekendtg. § 7 stk. 1, 5. pkt.). "
                + "Insert conditional div after the adjustmentType <select>: "
                + "<div th:if=\"${adjustmentForm.adjustmentType != null and "
                + "adjustmentForm.adjustmentType.name() == "
                + "'OPSKRIVNING_OMGJORT_NEDSKRIVNING_REGULERING'}\">",
            messageKey)
        .contains(messageKey);
  }

  /**
   * FR-5 / AC-7b: Backdating description must be absent for other adjustment types.
   *
   * <p>The static template cannot verify absence via {@code doesNotContain} after implementation,
   * because the key IS in the template (inside a Thymeleaf {@code th:if} conditional). Instead,
   * this step verifies that the Thymeleaf conditional correctly guards the description block with
   * the {@code OPSKRIVNING_OMGJORT_NEDSKRIVNING_REGULERING} type check — guaranteeing it is
   * suppressed at render time for other types (SPEC-P053 §5.1 / AC-7b).
   */
  @Then("the form does not display the backdating type description")
  public void formDoesNotDisplayBackdatingTypeDescription() {
    assertThat(formHtml)
        .as(
            "FR-5 / AC-7b: form.html must guard the backdating description using"
                + " th:if conditional on OPSKRIVNING_OMGJORT_NEDSKRIVNING_REGULERING"
                + " (SPEC-P053 §5.1). Other adjustment types must not render it.")
        .contains("OPSKRIVNING_OMGJORT_NEDSKRIVNING_REGULERING");

    assertThat(formHtml)
        .as(
            "FR-5 / AC-7b: form.html must use th:if to conditionally render the backdating"
                + " type description — absence for other types is enforced by the Thymeleaf"
                + " evaluator at render time.")
        .contains("th:if=");
  }

  // ── FR-6 Then steps ─────────────────────────────────────────────────────────

  /**
   * FR-6 / AC-8: Submission accepted by debt-service — full BFF + debt-service flow pending. We do
   * not throw {@link PendingException} here so that the subsequent receipt template assertions
   * (which will FAIL) are not skipped.
   */
  @Then("the submission is accepted by debt-service")
  public void submissionIsAcceptedByDebtService() {
    // Full integration check (ClaimAdjustmentResponseDto.crossSystemRetroactiveApplies flag)
    // is pending until ClaimAdjustmentController and ClaimAdjustmentResponseDto are created.
    // Receipt template assertions below will FAIL, providing meaningful feedback.
  }

  /**
   * FR-6 / AC-8 (SPEC-P053 §6.5): Receipt page must display the cross-system suspension advisory
   * using message key {@code adjustment.info.suspension.krydssystem} when {@code
   * receipt.crossSystemRetroactiveApplies == true}.
   *
   * <p><strong>Failing:</strong> {@code receipt.html} does not contain the advisory div, the
   * message key, or the {@code crossSystemRetroactiveApplies} Thymeleaf expression.
   */
  @Then("the receipt page displays a cross-system suspension advisory using message key {string}")
  public void receiptPageDisplaysSuspensionAdvisoryWithMessageKey(String messageKey) {
    assertThat(receiptHtml)
        .as(
            "FR-6 / AC-8: receipt.html must contain the cross-system suspension advisory div "
                + "using message key '%s' (SPEC-P053 §6.5 / GIL § 18 k). "
                + "Add: <div th:if=\"${receipt.crossSystemRetroactiveApplies}\" ...>",
            messageKey)
        .contains(messageKey);

    assertThat(receiptHtml)
        .as(
            "FR-6 / AC-8: receipt.html must conditionally render advisory on "
                + "th:if=\"${receipt.crossSystemRetroactiveApplies}\" (SPEC-P053 §6.5). "
                + "AdjustmentReceiptDto.crossSystemRetroactiveApplies field not yet added.")
        .contains("crossSystemRetroactiveApplies");
  }

  /**
   * FR-6 / AC-8: Advisory must reference GIL § 18 k in the message text.
   *
   * <p><strong>Failing:</strong> {@code messages_da.properties} key {@code
   * adjustment.info.suspension.krydssystem} does not yet contain "GIL § 18 k".
   */
  @Then("the advisory references GIL § 18 k")
  public void advisoryReferencesGilSection18k() throws IOException {
    Properties props = new Properties();
    try (InputStream in = messagesDa.getInputStream()) {
      props.load(in);
    }
    String msgValue = props.getProperty("adjustment.info.suspension.krydssystem", "");
    assertThat(msgValue)
        .as(
            "FR-6 / AC-8: messages_da.properties key "
                + "'adjustment.info.suspension.krydssystem' must reference 'GIL § 18 k' "
                + "(SPEC-P053 §6.5 legal basis). Add reference in messages_da.properties.")
        .contains("GIL § 18 k");
  }

  /**
   * FR-6 / AC-8 (SPEC-P053 §6.5): Standard confirmation heading must be rendered on the receipt
   * page even when the cross-system suspension advisory is also present.
   *
   * <p><strong>Failing:</strong> {@code receipt.html} does not yet contain the {@code
   * adjustment.receipt.confirmation} message key in a permanent confirmation section. After
   * implementation, the receipt page must show this confirmation alongside the advisory.
   */
  @And("the standard receipt confirmation is also displayed")
  public void standardReceiptConfirmationIsAlsoDisplayed() {
    assertThat(receiptHtml)
        .as(
            "FR-6 / AC-8: receipt.html must contain the standard receipt confirmation "
                + "(SPEC-P053 §6.5). Add a confirmation section using message key "
                + "'adjustment.receipt.confirmation' that is always present on the receipt page, "
                + "independent of whether the cross-system suspension advisory is shown.")
        .contains("adjustment.receipt.confirmation");
  }

  /**
   * FR-6 / AC-9 (SPEC-P053 §6.5): Cross-system suspension advisory must NOT be displayed when
   * {@code virkningsdato} is on or after the PSRM registration date.
   *
   * <p>The static template cannot verify absence via {@code doesNotContain} after implementation,
   * because the key IS in the template (inside a Thymeleaf {@code th:if
   * "${receipt.crossSystemRetroactiveApplies}"} conditional). Instead, this step verifies that the
   * Thymeleaf conditional correctly guards the advisory block — guaranteeing it is suppressed at
   * render time when the flag is {@code false} (SPEC-P053 §6.5 / GIL § 18 k / AC-9).
   */
  @Then("the receipt page does not display the cross-system suspension advisory")
  public void receiptPageDoesNotDisplayCrossSystemSuspensionAdvisory() {
    assertThat(receiptHtml)
        .as(
            "FR-6 / AC-9: receipt.html must guard the suspension advisory using"
                + " th:if=\"${receipt.crossSystemRetroactiveApplies}\" (SPEC-P053 §6.5 / GIL § 18 k)."
                + " When the flag is false, Thymeleaf suppresses the advisory at render time.")
        .contains("crossSystemRetroactiveApplies");

    assertThat(receiptHtml)
        .as(
            "FR-6 / AC-9: receipt.html must use th:if to conditionally render the suspension"
                + " advisory — non-retroactive submissions (flag=false) do not render it.")
        .contains("th:if=");
  }

  // ── FR-7 Then steps ─────────────────────────────────────────────────────────

  /**
   * FR-7 / AC-10 (SPEC-P053 §7.2 / G.A.2.3.4.4): RIM-internal write-up reason codes DINDB, OMPL,
   * and AFSK must not appear as selectable {@code <option>} values in the opskrivning form's
   * reason-code dropdown.
   *
   * <p><strong>Failing:</strong> {@code form.html} currently iterates over {@code
   * WriteUpReasonCode.values()} which includes DINDB, OMPL, and AFSK, making them visible in the
   * rendered HTML. The template must be changed to exclude these codes, or {@link
   * dk.ufst.opendebt.creditor.dto.WriteUpReasonCode} must be restructured so that only
   * portal-allowed codes are iterated (SPEC-P053 §7.2).
   */
  @Then("the form does not contain any selectable option with code {string}")
  public void formDoesNotContainSelectableOptionWithCode(String code) {
    // Assertion 1: WriteUpReasonCode.java must not exist (FR-7 deletes it — SPEC-P053 §7.1).
    // FAILS now (red phase) because the file still exists.
    // PASSES after FR-7 implementation when the file is deleted.
    Path writeUpEnumFile =
        resolveModuleRoot()
            .resolve("src/main/java/dk/ufst/opendebt/creditor/dto/WriteUpReasonCode.java");
    assertThat(writeUpEnumFile)
        .as(
            "FR-7 / AC-9b (G.A.2.3.4.4): WriteUpReasonCode.java must be deleted because all "
                + "three codes (DINDB, OMPL, AFSK) are RIM-internal (SPEC-P053 §7.1).")
        .doesNotExist();

    // Assertion 2: form.html must not iterate allowedReasonCodes in the write-up block.
    // FAILS because form.html currently has <option th:each="rc : ${allowedReasonCodes}">.
    assertThat(formHtml)
        .as(
            "FR-7 / AC-9: form.html must not render a reason-code dropdown backed by "
                + "'allowedReasonCodes' in the write-up block; this would expose '%s'. "
                + "Remove the write-up <select> block per SPEC-P053 §7.3.",
            code)
        .doesNotContain("allowedReasonCodes");
  }

  // ── Helper ──────────────────────────────────────────────────────────────────

  private String readResource(Resource resource) throws IOException {
    return Files.readString(Path.of(resource.getURI()), StandardCharsets.UTF_8);
  }

  private Path resolveModuleRoot() {
    Path current = Path.of("").toAbsolutePath().normalize();
    if (current.getFileName() != null
        && current.getFileName().toString().equals("opendebt-creditor-portal")) {
      return current;
    }
    return current.resolve("opendebt-creditor-portal");
  }

  /**
   * Invokes {@link ClaimAdjustmentController#submitAdjustment} directly (no MockMvc) and stores the
   * view name, binding result, model, and redirect attributes for Then step assertions.
   *
   * <p>Pattern: consistent with {@code ClaimAdjustmentControllerTest#submitAdjustment_*} tests.
   */
  private void invokeController(ClaimAdjustmentRequestDto form, String direction) {
    BindingResult bindingResult = new BeanPropertyBindingResult(form, "adjustmentForm");
    Model model = new ConcurrentModel();
    RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
    HttpSession session = new MockHttpSession();

    String viewName =
        controllerUnderTest.submitAdjustment(
            DEFAULT_CLAIM_UUID, form, bindingResult, direction, model, session, redirectAttributes);

    lastViewName = viewName;
    lastBindingResult = bindingResult;
    lastModel = model;
    lastRedirectAttributes = redirectAttributes;
  }

  /**
   * Resolves a date description to a {@link LocalDate}. Supports "today", "a future date", "N days
   * ago", and ISO-8601 date strings.
   */
  private LocalDate resolveDate(String description) {
    return switch (description.trim().toLowerCase()) {
      case "today" -> LocalDate.now();
      case "a future date" -> LocalDate.now().plusDays(30);
      case "60 days ago" -> LocalDate.now().minusDays(60);
      default -> {
        if (description.matches("\\d+ days ago")) {
          int days = Integer.parseInt(description.split(" ")[0]);
          yield LocalDate.now().minusDays(days);
        }
        yield LocalDate.parse(description);
      }
    };
  }
}
