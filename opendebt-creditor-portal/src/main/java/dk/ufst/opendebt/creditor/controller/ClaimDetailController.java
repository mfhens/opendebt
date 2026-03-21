package dk.ufst.opendebt.creditor.controller;

import java.util.UUID;

import jakarta.servlet.http.HttpSession;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import dk.ufst.opendebt.creditor.client.CreditorServiceClient;
import dk.ufst.opendebt.creditor.client.DebtServiceClient;
import dk.ufst.opendebt.creditor.dto.ClaimDetailDto;
import dk.ufst.opendebt.creditor.dto.CreditorAgreementDto;
import dk.ufst.opendebt.creditor.dto.DebtorInfoDto;
import dk.ufst.opendebt.creditor.service.PortalSessionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Controller for the claim detail view page (petition 030). */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ClaimDetailController {

  private static final String CPR_TYPE = "CPR";
  private static final int CPR_VISIBLE_DIGITS = 6;
  private static final String CPR_MASK = "****";
  private static final String MODEL_ADJUSTMENT_ALLOWED = "adjustmentAllowed";
  private static final String MODEL_WRITE_UP_ALLOWED = "writeUpAllowed";
  private static final String MODEL_WRITE_DOWN_ALLOWED = "writeDownAllowed";

  private final DebtServiceClient debtServiceClient;
  private final CreditorServiceClient creditorServiceClient;
  private final PortalSessionService portalSessionService;
  private final MessageSource messageSource;

  /** Renders the claim detail page for the given claim ID. */
  @GetMapping("/fordring/{id}")
  public String claimDetail(@PathVariable UUID id, Model model, HttpSession session) {
    UUID actingCreditor = portalSessionService.resolveActingCreditor(null, session);
    if (actingCreditor == null) {
      return "redirect:/demo-login";
    }

    ClaimDetailDto detail = loadClaimDetail(id, model);
    if (detail == null) {
      return "claims/detail";
    }

    censorDebtorCprNumbers(detail);

    model.addAttribute("claim", detail);
    model.addAttribute("claimId", id);
    model.addAttribute("singleDebtor", detail.getDebtorCount() == 1);
    model.addAttribute("currentPage", "claims-recovery");

    // Populate adjustment action button flags (petition 034)
    populateAdjustmentFlags(actingCreditor, model);

    return "claims/detail";
  }

  /** Endpoint for downloading a receipt (kvittering) for a claim delivery. */
  @GetMapping("/fordring/{id}/kvittering/{deliveryId}")
  public ResponseEntity<byte[]> downloadReceipt(
      @PathVariable UUID id, @PathVariable String deliveryId, HttpSession session) {
    UUID actingCreditor = portalSessionService.resolveActingCreditor(null, session);
    if (actingCreditor == null) {
      return ResponseEntity.status(302)
          .header(HttpHeaders.LOCATION, "/creditor-portal/demo-login")
          .build();
    }

    try {
      byte[] receiptData = debtServiceClient.getReceipt(id, deliveryId);
      if (receiptData == null || receiptData.length == 0) {
        return ResponseEntity.notFound().build();
      }
      return ResponseEntity.ok()
          .contentType(MediaType.APPLICATION_PDF)
          .header(
              HttpHeaders.CONTENT_DISPOSITION,
              "attachment; filename=\"kvittering-" + deliveryId + ".pdf\"")
          .body(receiptData);
    } catch (Exception ex) {
      log.warn(
          "Failed to fetch receipt for claim {} delivery {}: {}", id, deliveryId, ex.getMessage());
      return ResponseEntity.internalServerError().build();
    }
  }

  /**
   * Loads the creditor agreement and populates model attributes controlling the visibility of
   * write-up / write-down action buttons on the claim detail page (petition 034).
   */
  private void populateAdjustmentFlags(UUID creditorOrgId, Model model) {
    try {
      CreditorAgreementDto agreement = creditorServiceClient.getCreditorAgreement(creditorOrgId);
      if (agreement != null && agreement.isPortalActionsAllowed()) {
        boolean writeUp =
            agreement.isAllowWriteUpAdjustment()
                || agreement.isAllowWriteUpPayment()
                || agreement.isAllowPrincipalCorrection();
        boolean writeDown = agreement.isAllowWriteDown() || agreement.isAllowWriteDownPayment();
        model.addAttribute(MODEL_ADJUSTMENT_ALLOWED, writeUp || writeDown);
        model.addAttribute(MODEL_WRITE_UP_ALLOWED, writeUp);
        model.addAttribute(MODEL_WRITE_DOWN_ALLOWED, writeDown);
      } else {
        model.addAttribute(MODEL_ADJUSTMENT_ALLOWED, false);
        model.addAttribute(MODEL_WRITE_UP_ALLOWED, false);
        model.addAttribute(MODEL_WRITE_DOWN_ALLOWED, false);
      }
    } catch (Exception ex) {
      log.debug("Could not load creditor agreement for adjustment flags: {}", ex.getMessage());
      model.addAttribute(MODEL_ADJUSTMENT_ALLOWED, false);
      model.addAttribute(MODEL_WRITE_UP_ALLOWED, false);
      model.addAttribute(MODEL_WRITE_DOWN_ALLOWED, false);
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
}
