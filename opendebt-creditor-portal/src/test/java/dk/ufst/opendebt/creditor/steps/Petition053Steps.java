package dk.ufst.opendebt.creditor.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

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

  @Before("@petition053")
  public void resetState() {
    formHtml = null;
    receiptHtml = null;
    currentClaimId = null;
    selectedAdjustmentType = null;
    psrmRegistrationDate = null;
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
   * Requires a live portal HTTP POST to {@code /fordring/{id}/adjustment} — pending until {@code
   * ClaimAdjustmentRequestDto.writeDownReasonCode} and the controller guard are added.
   *
   * <p>SPEC-P053 §1.4: direction-conditional guard must run before {@code
   * debtServiceClient.submitAdjustment()}.
   */
  @When("user {string} submits a nedskrivning for claim {string} with:")
  public void userSubmitsNedskrivningWithDataTable(
      String userId, String claimId, DataTable dataTable) {
    throw new PendingException(
        "Not implemented: portal POST /fordring/{id}/adjustment with writeDownReasonCode field "
            + "(FR-1, FR-4 / SPEC-P053 §1.4, §4.1). "
            + "ClaimAdjustmentRequestDto.writeDownReasonCode (portal DTO) not yet added; "
            + "direction-conditional guard not yet in ClaimAdjustmentController.submitAdjustment().");
  }

  /**
   * FR-1 / Scenario Outline: Submit with a specific reason code. Pending for same reason as the
   * DataTable variant above.
   */
  @When("user {string} submits a nedskrivning for claim {string} with reasonCode {string}")
  public void userSubmitsNedskrivningWithReasonCode(
      String userId, String claimId, String reasonCode) {
    throw new PendingException(
        "Not implemented: portal POST /fordring/{id}/adjustment with writeDownReasonCode='"
            + reasonCode
            + "' (FR-1 / SPEC-P053 §1.4). "
            + "WriteDownReasonCode portal enum "
            + "(dk.ufst.opendebt.creditor.dto.WriteDownReasonCode) not yet created.");
  }

  /**
   * FR-1 / AC-3: Submit nedskrivning form without selecting a reason code. Pending until the
   * direction-conditional guard is in {@code ClaimAdjustmentController}.
   */
  @When(
      "user {string} submits the nedskrivning form for claim {string} without selecting a reason code")
  public void userSubmitsNedskrivningFormWithoutReasonCode(String userId, String claimId) {
    throw new PendingException(
        "Not implemented: portal POST /fordring/{id}/adjustment without writeDownReasonCode "
            + "(FR-1 / AC-3, SPEC-P053 §1.4). "
            + "Controller guard 'if (direction==WRITE_DOWN && writeDownReasonCode==null)' "
            + "not yet added to ClaimAdjustmentController.submitAdjustment().");
  }

  /**
   * FR-1 / AC-4: Submit with an unrecognised reason code. Pending until the portal enum binding
   * rejects unrecognised values.
   */
  @When(
      "user {string} submits the nedskrivning form for claim {string} with an unrecognised reason code {string}")
  public void userSubmitsNedskrivningWithUnrecognisedCode(
      String userId, String claimId, String unknownCode) {
    throw new PendingException(
        "Not implemented: portal POST with unrecognised writeDownReasonCode='"
            + unknownCode
            + "' (FR-1 / AC-4, SPEC-P053 §1.4). "
            + "Spring @ModelAttribute binding of WriteDownReasonCode enum will reject "
            + "unknown values; controller must return form with validation error.");
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
   * FR-1 / AC-5: BFF must forward {@code WriteDownReasonCode} to debt-service — requires live POST
   * through the controller and a mocked {@code DebtServiceClient}.
   */
  @Then("the BFF forwards a WriteDownDto to debt-service containing reasonCode {string}")
  public void bffForwardsWriteDownDtoWithReasonCode(String reasonCode) {
    throw new PendingException(
        "Not implemented: verify BFF forwarding of writeDownReasonCode='"
            + reasonCode
            + "' to debt-service (FR-1 / AC-5, SPEC-P053 §1.4). "
            + "Requires MockMvc POST and DebtServiceClient mock capture. "
            + "ClaimAdjustmentRequestDto.writeDownReasonCode (portal DTO) not yet added.");
  }

  /** FR-1 / AC-5: Debt-service acceptance — requires full BFF + debt-service HTTP flow. */
  @Then("debt-service accepts the request and returns a success receipt")
  public void debtServiceAcceptsRequestAndReturnsSuccessReceipt() {
    throw new PendingException(
        "Not implemented: verify debt-service returns HTTP 201 and success receipt "
            + "(FR-1 / AC-5, SPEC-P053 §9.2). "
            + "ClaimAdjustmentController endpoint POST /api/v1/debts/{id}/adjustments "
            + "does not exist yet.");
  }

  /** FR-1 / Scenario Outline: Debt-service acceptance for each valid reason code. */
  @Then("debt-service accepts the request")
  public void debtServiceAcceptsRequest() {
    throw new PendingException(
        "Not implemented: verify debt-service accepts nedskrivning with WriteDownReasonCode "
            + "(FR-1 / AC-5, SPEC-P053 §9.2). "
            + "ClaimAdjustmentController and WriteDownReasonCode (debt-service enum) "
            + "do not exist yet.");
  }

  /**
   * FR-1 / AC-3: Portal-side validation error with a specific message key — requires a POST
   * response to inspect the re-rendered form's error span.
   */
  @Then("the form displays a validation error using message key {string}")
  public void formDisplaysValidationErrorUsingMessageKey(String messageKey) {
    throw new PendingException(
        "Not implemented: inspect POST response for validation error using key '"
            + messageKey
            + "' (FR-1 / AC-3, SPEC-P053 §1.4). "
            + "Requires MockMvc POST and HTML assertion on the re-rendered form error span. "
            + "Direction-conditional bindingResult.rejectValue() guard not yet in controller.");
  }

  /** FR-1 / AC-3, AC-4: BFF must NOT be invoked when portal-side validation fails. */
  @Then("the BFF does not forward any request to debt-service")
  public void bffDoesNotForwardAnyRequest() {
    throw new PendingException(
        "Not implemented: verify DebtServiceClient.submitAdjustment() is not called on "
            + "portal validation failure (FR-1 / AC-3, AC-4, SPEC-P053 §1.4). "
            + "Requires MockMvc POST + Mockito.verify(debtServiceClient, never()).submitAdjustment(...).");
  }

  /** FR-1 / AC-4: Generic validation error display — requires POST response inspection. */
  @Then("the form displays a validation error")
  public void formDisplaysValidationError() {
    throw new PendingException(
        "Not implemented: verify portal form shows validation error for invalid reasonCode "
            + "(FR-1 / AC-4, SPEC-P053 §1.4). "
            + "Requires MockMvc POST and inspection of the re-rendered form's error section.");
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
   * Requires POST response — pending until MockMvc integration is available.
   */
  @Then("no retroactive nedskrivning advisory is displayed")
  public void noRetroaktivAdvisoryDisplayed() {
    throw new PendingException(
        "Not implemented: verify retroaktiv-advisory is absent from POST response HTML when "
            + "effectiveDate >= today (FR-4 / AC-6b, SPEC-P053 §4.4). "
            + "Requires MockMvc POST with effectiveDate=today/future and assertion that "
            + "retroaktivAdvisoryActive model attribute is not set "
            + "(ClaimAdjustmentController.submitAdjustment() not yet updated).");
  }

  /**
   * FR-4 / AC-6c: Portal must forward the retroactive nedskrivning despite the advisory. Requires
   * full BFF POST flow — pending.
   */
  @Then("the portal forwards the submission to the BFF")
  public void portalForwardsSubmissionToBff() {
    throw new PendingException(
        "Not implemented: verify portal forwards retroactive nedskrivning to debt-service "
            + "despite the advisory being shown (FR-4 / AC-6c, SPEC-P053 §4.1). "
            + "Requires MockMvc POST and DebtServiceClient.submitAdjustment() invocation capture.");
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
   * <p>Currently PASSES — the key is not yet in {@code form.html}. After implementation, the
   * Thymeleaf {@code th:if} conditional ensures the div is absent for other types.
   */
  @Then("the form does not display the backdating type description")
  public void formDoesNotDisplayBackdatingTypeDescription() {
    assertThat(formHtml)
        .as(
            "FR-5 / AC-7b: Backdating description must NOT appear for adjustmentType='%s'. "
                + "Thymeleaf th:if condition must evaluate to false for non-OMGJORT types.",
            selectedAdjustmentType)
        .doesNotContain("adjustment.type.description.omgjort_nedskrivning_regulering");
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
   * {@code virkningsdato} is on or after the PSRM registration date, i.e. {@code
   * receipt.crossSystemRetroactiveApplies == false}.
   *
   * <p>Currently PASSES because {@code receipt.html} has no cross-system advisory block yet. After
   * implementation the {@code th:if="${receipt.crossSystemRetroactiveApplies}"} condition must
   * evaluate to {@code false} and suppress the advisory for non-retroactive submissions.
   */
  @Then("the receipt page does not display the cross-system suspension advisory")
  public void receiptPageDoesNotDisplayCrossSystemSuspensionAdvisory() {
    assertThat(receiptHtml)
        .as(
            "FR-6 / AC-9: receipt.html must NOT render the suspension advisory when "
                + "crossSystemRetroactiveApplies=false (SPEC-P053 §6.5 / GIL § 18 k). "
                + "After implementation: verify th:if=\"${receipt.crossSystemRetroactiveApplies}\" "
                + "suppresses the advisory div when the flag is false.")
        .doesNotContain("adjustment.info.suspension.krydssystem");
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
}
