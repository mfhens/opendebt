package dk.ufst.opendebt.creditor.controller;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import dk.ufst.opendebt.creditor.client.CreditorServiceClient;
import dk.ufst.opendebt.creditor.client.DebtServiceClient;
import dk.ufst.opendebt.creditor.dto.AdjustmentReceiptDto;
import dk.ufst.opendebt.creditor.dto.ClaimAdjustmentRequestDto;
import dk.ufst.opendebt.creditor.dto.ClaimAdjustmentType;
import dk.ufst.opendebt.creditor.dto.ClaimDetailDto;
import dk.ufst.opendebt.creditor.dto.CreditorAgreementDto;
import dk.ufst.opendebt.creditor.dto.DebtorInfoDto;
import dk.ufst.opendebt.creditor.service.PortalSessionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for claim write-up and write-down adjustment forms (petition 034). Provides form
 * display, validation, submission through the BFF to debt-service, and receipt display.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ClaimAdjustmentController {

  private static final String CPR_TYPE = "CPR";
  private static final int CPR_VISIBLE_DIGITS = 6;
  private static final String CPR_MASK = "****";

  private static final String REDIRECT_DEMO_LOGIN = "redirect:/demo-login";
  private static final String VIEW_ADJUSTMENT_FORM = "claims/adjustment/form";
  private static final String PAGE_CLAIMS_RECOVERY = "claims-recovery";
  private static final String MODEL_CURRENT_PAGE = "currentPage";
  private static final String MODEL_SERVICE_ERROR = "serviceError";
  private static final String MODEL_CLAIM_ID = "claimId";
  private static final String MODEL_ADJUSTMENT_FORM = "adjustmentForm";

  private final DebtServiceClient debtServiceClient;
  private final CreditorServiceClient creditorServiceClient;
  private final PortalSessionService portalSessionService;
  private final MessageSource messageSource;

  /** Renders the adjustment form for a claim (write-up or write-down). */
  @GetMapping("/fordring/{id}/adjustment")
  public String showAdjustmentForm(
      @PathVariable UUID id,
      @RequestParam(defaultValue = "WRITE_DOWN") String direction,
      Model model,
      HttpSession session) {

    UUID actingCreditor = portalSessionService.resolveActingCreditor(null, session);
    if (actingCreditor == null) {
      return REDIRECT_DEMO_LOGIN;
    }

    CreditorAgreementDto agreement = creditorServiceClient.getCreditorAgreement(actingCreditor);
    if (!isAdjustmentAllowed(agreement)) {
      model.addAttribute(
          "serviceError",
          messageSource.getMessage(
              "adjustment.error.nopermission", null, LocaleContextHolder.getLocale()));
      model.addAttribute(MODEL_CURRENT_PAGE, PAGE_CLAIMS_RECOVERY);
      return VIEW_ADJUSTMENT_FORM;
    }

    ClaimAdjustmentType.Direction dir = parseDirection(direction);
    Set<String> permissions = agreement.getGrantedAdjustmentPermissions();
    List<ClaimAdjustmentType> allowedTypes =
        ClaimAdjustmentType.filterByPermissions(dir, permissions);

    if (allowedTypes.isEmpty()) {
      model.addAttribute(
          "serviceError",
          messageSource.getMessage(
              "adjustment.error.notypes", null, LocaleContextHolder.getLocale()));
      model.addAttribute(MODEL_CURRENT_PAGE, PAGE_CLAIMS_RECOVERY);
      return VIEW_ADJUSTMENT_FORM;
    }

    ClaimDetailDto claimDetail = loadClaimDetail(id, model);
    if (claimDetail == null) {
      return VIEW_ADJUSTMENT_FORM;
    }
    censorDebtorCprNumbers(claimDetail);

    model.addAttribute(MODEL_CLAIM_ID, id);
    model.addAttribute("claim", claimDetail);
    model.addAttribute("direction", dir.name());
    model.addAttribute("allowedTypes", allowedTypes);
    model.addAttribute("multiDebtor", claimDetail.getDebtorCount() > 1);
    model.addAttribute("debtors", claimDetail.getDebtors());
    model.addAttribute(MODEL_CURRENT_PAGE, PAGE_CLAIMS_RECOVERY);

    if (!model.containsAttribute("adjustmentForm")) {
      model.addAttribute(MODEL_ADJUSTMENT_FORM, new ClaimAdjustmentRequestDto());
    }

    return VIEW_ADJUSTMENT_FORM;
  }

  /** Processes the adjustment form submission. */
  @PostMapping("/fordring/{id}/adjustment")
  public String submitAdjustment(
      @PathVariable UUID id,
      @Valid @ModelAttribute("adjustmentForm") ClaimAdjustmentRequestDto adjustmentForm,
      BindingResult bindingResult,
      @RequestParam(defaultValue = "WRITE_DOWN") String direction,
      Model model,
      HttpSession session,
      RedirectAttributes redirectAttributes) {

    UUID actingCreditor = portalSessionService.resolveActingCreditor(null, session);
    if (actingCreditor == null) {
      return REDIRECT_DEMO_LOGIN;
    }

    CreditorAgreementDto agreement = creditorServiceClient.getCreditorAgreement(actingCreditor);
    if (!isAdjustmentAllowed(agreement)) {
      model.addAttribute(
          "serviceError",
          messageSource.getMessage(
              "adjustment.error.nopermission", null, LocaleContextHolder.getLocale()));
      model.addAttribute(MODEL_CURRENT_PAGE, PAGE_CLAIMS_RECOVERY);
      return VIEW_ADJUSTMENT_FORM;
    }

    // Validate the selected adjustment type is permitted
    if (adjustmentForm.getAdjustmentType() != null) {
      Set<String> permissions = agreement.getGrantedAdjustmentPermissions();
      if (!permissions.contains(adjustmentForm.getAdjustmentType().getRequiredPermission())) {
        bindingResult.rejectValue(
            "adjustmentType",
            "adjustment.validation.type.notpermitted",
            messageSource.getMessage(
                "adjustment.validation.type.notpermitted", null, LocaleContextHolder.getLocale()));
      }
    }

    // Validate debtor selection for payment-related types with multiple debtors
    ClaimDetailDto claimDetail = loadClaimDetail(id, model);
    if (claimDetail != null
        && adjustmentForm.getAdjustmentType() != null
        && adjustmentForm.getAdjustmentType().isPaymentRelated()
        && claimDetail.getDebtorCount() > 1
        && adjustmentForm.getDebtorIndex() == null) {
      bindingResult.rejectValue(
          "debtorIndex",
          "adjustment.validation.debtor.required",
          messageSource.getMessage(
              "adjustment.validation.debtor.required", null, LocaleContextHolder.getLocale()));
    }

    if (bindingResult.hasErrors()) {
      return reloadFormWithErrors(id, direction, model, session, agreement);
    }

    // Resolve debtor ID from haeftelsesstruktur for payment-related types
    if (claimDetail != null
        && adjustmentForm.getAdjustmentType() != null
        && adjustmentForm.getAdjustmentType().isPaymentRelated()
        && claimDetail.getDebtorCount() > 1
        && adjustmentForm.getDebtorIndex() != null) {
      int idx = adjustmentForm.getDebtorIndex();
      if (idx >= 0 && claimDetail.getDebtors() != null && idx < claimDetail.getDebtors().size()) {
        log.debug("Resolved debtor index {} for payment adjustment on claim {}", idx, id);
      }
    }

    try {
      AdjustmentReceiptDto receipt = debtServiceClient.submitAdjustment(id, adjustmentForm);
      log.info(
          "Adjustment submitted for claim {} by creditor {}, type: {}",
          id,
          actingCreditor,
          adjustmentForm.getAdjustmentType());

      // Censor debtor identifier in receipt
      if (receipt != null && receipt.getDebtorIdentifier() != null) {
        receipt.setDebtorIdentifier(censorCprIdentifier(receipt.getDebtorIdentifier()));
      }

      redirectAttributes.addFlashAttribute("receipt", receipt);
      redirectAttributes.addFlashAttribute("claimId", id);
      return "redirect:/fordring/" + id + "/adjustment/receipt";
    } catch (Exception ex) {
      log.error("Failed to submit adjustment for claim {}: {}", id, ex.getMessage(), ex);
      model.addAttribute(
          "actionError",
          messageSource.getMessage(
              "adjustment.submit.error", null, LocaleContextHolder.getLocale()));
      return reloadFormWithErrors(id, direction, model, session, agreement);
    }
  }

  /** Displays the adjustment receipt page after a successful submission. */
  @GetMapping("/fordring/{id}/adjustment/receipt")
  public String showReceipt(@PathVariable UUID id, Model model, HttpSession session) {
    UUID actingCreditor = portalSessionService.resolveActingCreditor(null, session);
    if (actingCreditor == null) {
      return REDIRECT_DEMO_LOGIN;
    }

    model.addAttribute(MODEL_CLAIM_ID, id);
    model.addAttribute(MODEL_CURRENT_PAGE, PAGE_CLAIMS_RECOVERY);

    // Receipt is passed via flash attributes from the POST redirect
    if (!model.containsAttribute("receipt")) {
      return "redirect:/fordring/" + id;
    }

    return "claims/adjustment/receipt";
  }

  // --- Private helpers ---

  private boolean isAdjustmentAllowed(CreditorAgreementDto agreement) {
    return agreement != null && agreement.isPortalActionsAllowed();
  }

  private ClaimAdjustmentType.Direction parseDirection(String direction) {
    try {
      return ClaimAdjustmentType.Direction.valueOf(direction);
    } catch (IllegalArgumentException ex) {
      return ClaimAdjustmentType.Direction.WRITE_DOWN;
    }
  }

  private ClaimDetailDto loadClaimDetail(UUID claimId, Model model) {
    try {
      ClaimDetailDto detail = debtServiceClient.getClaimDetail(claimId);
      if (detail == null) {
        model.addAttribute(
            "serviceError",
            messageSource.getMessage(
                "claim.detail.error.notfound", null, LocaleContextHolder.getLocale()));
      }
      return detail;
    } catch (Exception ex) {
      log.warn("Failed to load claim detail for {}: {}", claimId, ex.getMessage());
      model.addAttribute(
          "serviceError",
          messageSource.getMessage(
              "claim.detail.error.service", null, LocaleContextHolder.getLocale()));
      return null;
    }
  }

  private String reloadFormWithErrors(
      UUID claimId,
      String direction,
      Model model,
      HttpSession session,
      CreditorAgreementDto agreement) {

    ClaimAdjustmentType.Direction dir = parseDirection(direction);
    Set<String> permissions = agreement.getGrantedAdjustmentPermissions();
    List<ClaimAdjustmentType> allowedTypes =
        ClaimAdjustmentType.filterByPermissions(dir, permissions);

    ClaimDetailDto claimDetail = loadClaimDetail(claimId, model);
    if (claimDetail != null) {
      censorDebtorCprNumbers(claimDetail);
    }

    model.addAttribute(MODEL_CLAIM_ID, claimId);
    model.addAttribute("claim", claimDetail);
    model.addAttribute("direction", dir.name());
    model.addAttribute("allowedTypes", allowedTypes);
    model.addAttribute("multiDebtor", claimDetail != null && claimDetail.getDebtorCount() > 1);
    model.addAttribute("debtors", claimDetail != null ? claimDetail.getDebtors() : null);
    model.addAttribute(MODEL_CURRENT_PAGE, PAGE_CLAIMS_RECOVERY);
    return VIEW_ADJUSTMENT_FORM;
  }

  /** Censors CPR numbers in debtor list, showing only the first 6 digits. */
  private void censorDebtorCprNumbers(ClaimDetailDto detail) {
    if (detail.getDebtors() == null) {
      return;
    }
    for (DebtorInfoDto debtor : detail.getDebtors()) {
      if (CPR_TYPE.equalsIgnoreCase(debtor.getIdentifierType())
          && debtor.getIdentifier() != null
          && debtor.getIdentifier().length() > CPR_VISIBLE_DIGITS) {
        debtor.setIdentifier(debtor.getIdentifier().substring(0, CPR_VISIBLE_DIGITS) + CPR_MASK);
      }
    }
  }

  /** Censors a single CPR identifier string (for receipt display). */
  private String censorCprIdentifier(String identifier) {
    if (identifier != null && identifier.length() > CPR_VISIBLE_DIGITS) {
      return identifier.substring(0, CPR_VISIBLE_DIGITS) + CPR_MASK;
    }
    return identifier;
  }
}
