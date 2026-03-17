package dk.ufst.opendebt.creditor.controller;

import java.util.UUID;

import jakarta.servlet.http.HttpSession;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import dk.ufst.opendebt.creditor.client.CreditorServiceClient;
import dk.ufst.opendebt.creditor.client.DebtServiceClient;
import dk.ufst.opendebt.creditor.client.PersonRegistryClient;
import dk.ufst.opendebt.creditor.dto.ClaimSubmissionResultDto;
import dk.ufst.opendebt.creditor.dto.ClaimWizardFormDto;
import dk.ufst.opendebt.creditor.dto.CreditorAgreementDto;
import dk.ufst.opendebt.creditor.dto.DebtorVerificationResultDto;
import dk.ufst.opendebt.creditor.dto.PortalDebtDto;
import dk.ufst.opendebt.creditor.service.PortalSessionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Multi-step claim creation wizard controller (petition 033). Guides the creditor through four
 * steps: debtor identification, claim data entry, review, and submission.
 */
@Slf4j
@Controller
@RequestMapping("/fordring/opret")
@RequiredArgsConstructor
public class ClaimCreateController {

  static final String SESSION_WIZARD_FORM = "claimWizardForm";
  static final String SESSION_WIZARD_RESULT = "claimWizardResult";
  static final String SESSION_WIZARD_AGREEMENT = "claimWizardAgreement";

  private static final int TOTAL_STEPS = 4;

  private final PortalSessionService portalSessionService;
  private final CreditorServiceClient creditorServiceClient;
  private final DebtServiceClient debtServiceClient;
  private final PersonRegistryClient personRegistryClient;
  private final MessageSource messageSource;

  // -----------------------------------------------------------------------
  // Entry point
  // -----------------------------------------------------------------------

  /** GET /fordring/opret — redirects to step 1. */
  @GetMapping
  public String entryPoint(HttpSession session) {
    if (portalSessionService.resolveActingCreditor(null, session) == null) {
      return "redirect:/demo-login";
    }
    return "redirect:/fordring/opret/step/1";
  }

  // -----------------------------------------------------------------------
  // Step 1: Debtor identification
  // -----------------------------------------------------------------------

  /** GET /fordring/opret/step/1 — renders debtor identification form. */
  @GetMapping("/step/1")
  public String showDebtorStep(Model model, HttpSession session) {
    UUID creditorOrgId = portalSessionService.resolveActingCreditor(null, session);
    if (creditorOrgId == null) {
      return "redirect:/demo-login";
    }

    CreditorAgreementDto agreement = loadAgreement(creditorOrgId, session);
    if (agreement == null || !canCreateClaims(agreement)) {
      return "redirect:/";
    }

    ClaimWizardFormDto form = getOrCreateWizardForm(session);
    model.addAttribute("wizardForm", form);
    model.addAttribute("agreement", agreement);
    addWizardModelAttributes(model, 1, session);
    return "claims/create/step-debtor";
  }

  /** POST /fordring/opret/step/1 — validates debtor and advances to step 2. */
  @PostMapping("/step/1")
  public String processDebtorStep(
      @RequestParam("debtorType") String debtorType,
      @RequestParam("debtorIdentifier") String debtorIdentifier,
      @RequestParam(value = "debtorFirstName", required = false) String debtorFirstName,
      @RequestParam(value = "debtorLastName", required = false) String debtorLastName,
      Model model,
      HttpSession session,
      RedirectAttributes redirectAttributes) {

    UUID creditorOrgId = portalSessionService.resolveActingCreditor(null, session);
    if (creditorOrgId == null) {
      return "redirect:/demo-login";
    }

    CreditorAgreementDto agreement = loadAgreement(creditorOrgId, session);
    if (agreement == null || !canCreateClaims(agreement)) {
      return "redirect:/";
    }

    ClaimWizardFormDto form = getOrCreateWizardForm(session);
    form.setDebtorType(debtorType);
    form.setDebtorIdentifier(debtorIdentifier);
    form.setDebtorFirstName(debtorFirstName);
    form.setDebtorLastName(debtorLastName);

    // Validate required fields
    if (debtorType == null || debtorType.isBlank()) {
      model.addAttribute(
          "debtorTypeError", resolveMessage("wizard.validation.debtorType.required"));
      model.addAttribute("wizardForm", form);
      model.addAttribute("agreement", agreement);
      addWizardModelAttributes(model, 1, session);
      return "claims/create/step-debtor";
    }

    if (debtorIdentifier == null || debtorIdentifier.isBlank()) {
      model.addAttribute(
          "debtorIdentifierError", resolveMessage("wizard.validation.debtorIdentifier.required"));
      model.addAttribute("wizardForm", form);
      model.addAttribute("agreement", agreement);
      addWizardModelAttributes(model, 1, session);
      return "claims/create/step-debtor";
    }

    // Verify debtor against person-registry
    DebtorVerificationResultDto verificationResult = verifyDebtor(form);

    if (!verificationResult.isVerified()) {
      model.addAttribute("verificationError", verificationResult.getErrorMessage());
      model.addAttribute("wizardForm", form);
      model.addAttribute("agreement", agreement);
      addWizardModelAttributes(model, 1, session);
      return "claims/create/step-debtor";
    }

    form.setDebtorVerified(true);
    form.setDebtorDisplayName(verificationResult.getDisplayName());
    form.setDebtorPersonId(verificationResult.getPersonId());
    session.setAttribute(SESSION_WIZARD_FORM, form);

    return "redirect:/fordring/opret/step/2";
  }

  // -----------------------------------------------------------------------
  // Step 2: Claim data entry
  // -----------------------------------------------------------------------

  /** GET /fordring/opret/step/2 — renders claim data entry form. */
  @GetMapping("/step/2")
  public String showDetailsStep(Model model, HttpSession session) {
    UUID creditorOrgId = portalSessionService.resolveActingCreditor(null, session);
    if (creditorOrgId == null) {
      return "redirect:/demo-login";
    }

    ClaimWizardFormDto form = (ClaimWizardFormDto) session.getAttribute(SESSION_WIZARD_FORM);
    if (form == null || !form.isDebtorVerified()) {
      return "redirect:/fordring/opret/step/1";
    }

    CreditorAgreementDto agreement = loadAgreement(creditorOrgId, session);
    model.addAttribute("wizardForm", form);
    model.addAttribute("agreement", agreement);
    addWizardModelAttributes(model, 2, session);
    return "claims/create/step-details";
  }

  /** POST /fordring/opret/step/2 — validates claim details and advances to step 3. */
  @PostMapping("/step/2")
  public String processDetailsStep(
      @RequestParam("claimType") String claimType,
      @RequestParam("amount") String amountStr,
      @RequestParam("principalAmount") String principalAmountStr,
      @RequestParam(value = "creditorReference", required = false) String creditorReference,
      @RequestParam(value = "description", required = false) String description,
      @RequestParam(value = "periodFrom", required = false) String periodFromStr,
      @RequestParam(value = "periodTo", required = false) String periodToStr,
      @RequestParam(value = "incorporationDate", required = false) String incorporationDateStr,
      @RequestParam(value = "dueDate", required = false) String dueDateStr,
      @RequestParam(value = "lastTimelyPaymentDate", required = false) String lastTimelyPaymentStr,
      @RequestParam("limitationDate") String limitationDateStr,
      @RequestParam("estateProcessing") String estateProcessingStr,
      @RequestParam(value = "courtDate", required = false) String courtDateStr,
      @RequestParam(value = "settlementDate", required = false) String settlementDateStr,
      @RequestParam(value = "interestRule", required = false) String interestRule,
      @RequestParam(value = "interestRateCode", required = false) String interestRateCode,
      @RequestParam(value = "interestRate", required = false) String interestRateStr,
      @RequestParam(value = "claimNote", required = false) String claimNote,
      @RequestParam(value = "debtorNote", required = false) String debtorNote,
      Model model,
      HttpSession session) {

    UUID creditorOrgId = portalSessionService.resolveActingCreditor(null, session);
    if (creditorOrgId == null) {
      return "redirect:/demo-login";
    }

    ClaimWizardFormDto form = (ClaimWizardFormDto) session.getAttribute(SESSION_WIZARD_FORM);
    if (form == null || !form.isDebtorVerified()) {
      return "redirect:/fordring/opret/step/1";
    }

    CreditorAgreementDto agreement = loadAgreement(creditorOrgId, session);
    boolean hasErrors = false;

    // Populate form from request parameters
    form.setClaimType(claimType);
    form.setCreditorReference(creditorReference);
    form.setDescription(description);
    form.setInterestRule(interestRule);
    form.setInterestRateCode(interestRateCode);
    form.setClaimNote(claimNote);
    form.setDebtorNote(debtorNote);
    form.setEstateProcessing(parseBoolean(estateProcessingStr));

    // Parse and validate numeric fields
    try {
      form.setAmount(new java.math.BigDecimal(amountStr));
    } catch (Exception ex) {
      model.addAttribute("amountError", resolveMessage("wizard.validation.amount.required"));
      hasErrors = true;
    }

    try {
      form.setPrincipalAmount(new java.math.BigDecimal(principalAmountStr));
    } catch (Exception ex) {
      model.addAttribute(
          "principalAmountError", resolveMessage("wizard.validation.principalAmount.required"));
      hasErrors = true;
    }

    if (interestRateStr != null && !interestRateStr.isBlank()) {
      try {
        form.setInterestRate(new java.math.BigDecimal(interestRateStr));
      } catch (Exception ex) {
        // Optional field — ignore parse errors
      }
    }

    // Parse date fields
    form.setPeriodFrom(parseDate(periodFromStr));
    form.setPeriodTo(parseDate(periodToStr));
    form.setIncorporationDate(parseDate(incorporationDateStr));
    form.setDueDate(parseDate(dueDateStr));
    form.setLastTimelyPaymentDate(parseDate(lastTimelyPaymentStr));
    form.setCourtDate(parseDate(courtDateStr));
    form.setSettlementDate(parseDate(settlementDateStr));

    java.time.LocalDate limitationDate = parseDate(limitationDateStr);
    form.setLimitationDate(limitationDate);

    // Validate required fields
    if (claimType == null || claimType.isBlank()) {
      model.addAttribute("claimTypeError", resolveMessage("wizard.validation.claimType.required"));
      hasErrors = true;
    }

    if (limitationDate == null) {
      model.addAttribute(
          "limitationDateError", resolveMessage("wizard.validation.limitationDate.required"));
      hasErrors = true;
    }

    if (form.getEstateProcessing() == null) {
      model.addAttribute(
          "estateProcessingError", resolveMessage("wizard.validation.estateProcessing.required"));
      hasErrors = true;
    }

    // Validate description length
    if (description != null && description.length() > 100) {
      model.addAttribute(
          "descriptionError", resolveMessage("wizard.validation.description.maxlength"));
      hasErrors = true;
    }

    if (hasErrors) {
      model.addAttribute("wizardForm", form);
      model.addAttribute("agreement", agreement);
      addWizardModelAttributes(model, 2, session);
      return "claims/create/step-details";
    }

    session.setAttribute(SESSION_WIZARD_FORM, form);
    return "redirect:/fordring/opret/step/3";
  }

  // -----------------------------------------------------------------------
  // Step 3: Review
  // -----------------------------------------------------------------------

  /** GET /fordring/opret/step/3 — renders read-only review of all entered data. */
  @GetMapping("/step/3")
  public String showReviewStep(Model model, HttpSession session) {
    UUID creditorOrgId = portalSessionService.resolveActingCreditor(null, session);
    if (creditorOrgId == null) {
      return "redirect:/demo-login";
    }

    ClaimWizardFormDto form = (ClaimWizardFormDto) session.getAttribute(SESSION_WIZARD_FORM);
    if (form == null || !form.isDebtorVerified()) {
      return "redirect:/fordring/opret/step/1";
    }
    if (form.getClaimType() == null || form.getClaimType().isBlank()) {
      return "redirect:/fordring/opret/step/2";
    }

    model.addAttribute("wizardForm", form);
    addWizardModelAttributes(model, 3, session);
    return "claims/create/step-review";
  }

  /** POST /fordring/opret/step/3 — submits the claim to debt-service via BFF. */
  @PostMapping("/step/3")
  public String processSubmission(Model model, HttpSession session) {
    UUID creditorOrgId = portalSessionService.resolveActingCreditor(null, session);
    if (creditorOrgId == null) {
      return "redirect:/demo-login";
    }

    ClaimWizardFormDto form = (ClaimWizardFormDto) session.getAttribute(SESSION_WIZARD_FORM);
    if (form == null || !form.isDebtorVerified() || form.getClaimType() == null) {
      return "redirect:/fordring/opret/step/1";
    }

    try {
      PortalDebtDto debtRequest = mapWizardFormToDebtRequest(form, creditorOrgId);
      ClaimSubmissionResultDto result = debtServiceClient.submitClaimWizard(debtRequest);

      log.info(
          "Claim submission result: outcome={}, claimId={}",
          result.getOutcome(),
          result.getClaimId());

      session.setAttribute(SESSION_WIZARD_RESULT, result);
      // Keep wizard form in session for result display
      return "redirect:/fordring/opret/step/4";
    } catch (Exception ex) {
      log.error("Error submitting claim via wizard: {}", ex.getMessage(), ex);
      model.addAttribute("submissionError", resolveMessage("wizard.submit.error"));
      model.addAttribute("wizardForm", form);
      addWizardModelAttributes(model, 3, session);
      return "claims/create/step-review";
    }
  }

  // -----------------------------------------------------------------------
  // Step 4: Result
  // -----------------------------------------------------------------------

  /** GET /fordring/opret/step/4 — displays submission result (receipt, rejection, or hearing). */
  @GetMapping("/step/4")
  public String showResultStep(Model model, HttpSession session) {
    UUID creditorOrgId = portalSessionService.resolveActingCreditor(null, session);
    if (creditorOrgId == null) {
      return "redirect:/demo-login";
    }

    ClaimSubmissionResultDto result =
        (ClaimSubmissionResultDto) session.getAttribute(SESSION_WIZARD_RESULT);
    ClaimWizardFormDto form = (ClaimWizardFormDto) session.getAttribute(SESSION_WIZARD_FORM);

    if (result == null) {
      return "redirect:/fordring/opret/step/1";
    }

    model.addAttribute("result", result);
    model.addAttribute("wizardForm", form);
    addWizardModelAttributes(model, 4, session);

    // Clean up session
    session.removeAttribute(SESSION_WIZARD_FORM);
    session.removeAttribute(SESSION_WIZARD_RESULT);
    session.removeAttribute(SESSION_WIZARD_AGREEMENT);

    return "claims/create/step-result";
  }

  // -----------------------------------------------------------------------
  // Helper methods
  // -----------------------------------------------------------------------

  private ClaimWizardFormDto getOrCreateWizardForm(HttpSession session) {
    ClaimWizardFormDto form = (ClaimWizardFormDto) session.getAttribute(SESSION_WIZARD_FORM);
    if (form == null) {
      form = new ClaimWizardFormDto();
      session.setAttribute(SESSION_WIZARD_FORM, form);
    }
    return form;
  }

  private CreditorAgreementDto loadAgreement(UUID creditorOrgId, HttpSession session) {
    CreditorAgreementDto cached =
        (CreditorAgreementDto) session.getAttribute(SESSION_WIZARD_AGREEMENT);
    if (cached != null) {
      return cached;
    }
    try {
      CreditorAgreementDto agreement = creditorServiceClient.getCreditorAgreement(creditorOrgId);
      if (agreement != null) {
        session.setAttribute(SESSION_WIZARD_AGREEMENT, agreement);
      }
      return agreement;
    } catch (Exception ex) {
      log.warn("Failed to load creditor agreement: {}", ex.getMessage());
      return null;
    }
  }

  private boolean canCreateClaims(CreditorAgreementDto agreement) {
    return agreement.isPortalActionsAllowed() && agreement.isAllowCreateRecoveryClaims();
  }

  private DebtorVerificationResultDto verifyDebtor(ClaimWizardFormDto form) {
    String type = form.getDebtorType();
    String identifier = form.getDebtorIdentifier();

    switch (type.toUpperCase()) {
      case "CPR":
        if (form.getDebtorFirstName() == null
            || form.getDebtorFirstName().isBlank()
            || form.getDebtorLastName() == null
            || form.getDebtorLastName().isBlank()) {
          return DebtorVerificationResultDto.builder()
              .verified(false)
              .errorMessage(resolveMessage("wizard.step1.cpr.name.required"))
              .build();
        }
        return personRegistryClient.verifyCpr(
            identifier, form.getDebtorFirstName(), form.getDebtorLastName());

      case "CVR":
        return personRegistryClient.verifyCvr(identifier);

      case "SE":
        return personRegistryClient.verifySe(identifier);

      case "AKR":
        // AKR entries are accepted without external verification
        return DebtorVerificationResultDto.builder()
            .verified(true)
            .displayName("AKR-" + identifier)
            .personId(UUID.randomUUID())
            .build();

      default:
        return DebtorVerificationResultDto.builder()
            .verified(false)
            .errorMessage(resolveMessage("wizard.validation.debtorType.required"))
            .build();
    }
  }

  private PortalDebtDto mapWizardFormToDebtRequest(ClaimWizardFormDto form, UUID creditorOrgId) {
    return PortalDebtDto.builder()
        .debtorPersonId(form.getDebtorPersonId())
        .creditorOrgId(creditorOrgId)
        .principalAmount(form.getPrincipalAmount())
        .outstandingBalance(form.getAmount())
        .dueDate(form.getDueDate())
        .debtTypeCode(form.getClaimType())
        .description(form.getDescription())
        .status("SUBMITTED")
        .build();
  }

  private void addWizardModelAttributes(Model model, int currentStep, HttpSession session) {
    model.addAttribute("currentStep", currentStep);
    model.addAttribute("totalSteps", TOTAL_STEPS);
    model.addAttribute("currentPage", "claim-new");

    UUID actingCreditor = portalSessionService.resolveActingCreditor(null, session);
    model.addAttribute("actingCreditorOrgId", actingCreditor);

    UUID representedCreditor = portalSessionService.getRepresentedCreditor(session);
    if (representedCreditor != null) {
      model.addAttribute("representedCreditorOrgId", representedCreditor);
    }
  }

  private String resolveMessage(String code) {
    return messageSource.getMessage(code, null, code, LocaleContextHolder.getLocale());
  }

  private java.time.LocalDate parseDate(String dateStr) {
    if (dateStr == null || dateStr.isBlank()) {
      return null;
    }
    try {
      return java.time.LocalDate.parse(dateStr);
    } catch (Exception ex) {
      return null;
    }
  }

  private Boolean parseBoolean(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return "true".equalsIgnoreCase(value)
        || "yes".equalsIgnoreCase(value)
        || "1".equals(value)
        || "ja".equalsIgnoreCase(value);
  }
}
