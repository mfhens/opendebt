package dk.ufst.opendebt.caseworker.section50;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpSession;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import dk.ufst.opendebt.caseworker.dto.CaseworkerIdentity;
import dk.ufst.opendebt.caseworker.service.CaseworkerSessionService;
import dk.ufst.opendebt.common.exception.OpenDebtException;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class Section50WorklistController {

  private static final String REDIRECT_DEMO_LOGIN = "redirect:/demo-login";
  private static final String VIEW_NAME = "section50/worklist";

  private final Section50WorklistClient section50WorklistClient;
  private final CaseworkerSessionService caseworkerSessionService;
  private final MessageSource messageSource;

  @GetMapping("/debtors/{debtorId}/retskraft-worklists/{worklistId}")
  public String worklist(
      @PathVariable UUID debtorId,
      @PathVariable UUID worklistId,
      HttpSession session,
      Model model) {
    CaseworkerIdentity caseworker = caseworkerSessionService.getCurrentCaseworker(session);
    if (caseworker == null) {
      return REDIRECT_DEMO_LOGIN;
    }

    model.addAttribute("caseworker", caseworker);
    model.addAttribute("currentPage", "cases");
    model.addAttribute("debtorId", debtorId);
    model.addAttribute("worklistId", worklistId);
    model.addAttribute("canEdit", hasWriteAccess(caseworker));
    model.addAttribute("overrideForm", new Section50OverrideForm());
    model.addAttribute("modregningDecisionForm", new Section50ModregningDecisionForm());
    model.addAttribute("showExpeditedForm", false);

    try {
      PortalSection50WorklistDto worklist =
          section50WorklistClient.getWorklist(debtorId, worklistId);
      model.addAttribute("worklist", worklist);
      model.addAttribute(
          "showExpeditedForm",
          hasWriteAccess(caseworker) && "VOLUNTARY_PAYMENT_SURPLUS".equals(worklist.contextType()));
      model.addAttribute(
          "showModregningForm",
          hasWriteAccess(caseworker) && "MODREGNING".equals(worklist.contextType()));
    } catch (RuntimeException ex) {
      log.error(
          "Failed to load section50 worklist {} for debtor {}: {}",
          worklistId,
          debtorId,
          ex.getMessage());
      model.addAttribute(
          "backendError",
          messageSource.getMessage(
              "section50.worklist.error.backend",
              null,
              "Failed to load the Section 50 worklist. Please try again later.",
              LocaleContextHolder.getLocale()));
      model.addAttribute("showModregningForm", false);
    }

    return VIEW_NAME;
  }

  @PostMapping("/debtors/{debtorId}/retskraft-worklists/{worklistId}/override")
  public String applyOverride(
      @PathVariable UUID debtorId,
      @PathVariable UUID worklistId,
      @ModelAttribute Section50OverrideForm form,
      HttpSession session,
      RedirectAttributes redirectAttributes) {
    CaseworkerIdentity caseworker = caseworkerSessionService.getCurrentCaseworker(session);
    if (caseworker == null || !hasWriteAccess(caseworker)) {
      return REDIRECT_DEMO_LOGIN;
    }

    try {
      section50WorklistClient.applyOverride(
          debtorId,
          worklistId,
          new Section50OverrideSubmission(
              form.getOverrideReason(),
              form.getLegalBasis(),
              form.getExpedited(),
              parseSelectedClaimOrder(form.getSelectedClaimOrder())));
      redirectAttributes.addFlashAttribute(
          "successMessage",
          messageSource.getMessage(
              "section50.worklist.success.override",
              null,
              "Retskraftvurderingen er opdateret.",
              LocaleContextHolder.getLocale()));
    } catch (OpenDebtException | WebClientResponseException | CallNotPermittedException ex) {
      log.warn(
          "Failed to apply section50 override for worklist {} and debtor {}: {}",
          worklistId,
          debtorId,
          ex.getMessage());
      redirectAttributes.addFlashAttribute(
          "errorMessage",
          messageSource.getMessage(
              "section50.worklist.error.override",
              new Object[] {ex.getMessage()},
              "Kunne ikke gemme override: " + ex.getMessage(),
              LocaleContextHolder.getLocale()));
    }

    return redirectToWorklist(debtorId, worklistId);
  }

  @PostMapping("/debtors/{debtorId}/retskraft-worklists/{worklistId}/modregning-decision")
  public String recordModregningDecision(
      @PathVariable UUID debtorId,
      @PathVariable UUID worklistId,
      @ModelAttribute Section50ModregningDecisionForm form,
      HttpSession session,
      RedirectAttributes redirectAttributes) {
    CaseworkerIdentity caseworker = caseworkerSessionService.getCurrentCaseworker(session);
    if (caseworker == null || !hasWriteAccess(caseworker)) {
      return REDIRECT_DEMO_LOGIN;
    }

    try {
      section50WorklistClient.recordModregningDecision(
          debtorId,
          worklistId,
          new Section50ModregningDecisionSubmission(form.getModregningOutcome(), form.getReason()));
      redirectAttributes.addFlashAttribute(
          "successMessage",
          messageSource.getMessage(
              "section50.worklist.success.modregning",
              null,
              "Modregningsbeslutningen er registreret.",
              LocaleContextHolder.getLocale()));
    } catch (OpenDebtException | WebClientResponseException | CallNotPermittedException ex) {
      log.warn(
          "Failed to record section50 modregning decision for worklist {} and debtor {}: {}",
          worklistId,
          debtorId,
          ex.getMessage());
      redirectAttributes.addFlashAttribute(
          "errorMessage",
          messageSource.getMessage(
              "section50.worklist.error.modregning",
              new Object[] {ex.getMessage()},
              "Kunne ikke gemme modregningsbeslutningen: " + ex.getMessage(),
              LocaleContextHolder.getLocale()));
    }

    return redirectToWorklist(debtorId, worklistId);
  }

  private boolean hasWriteAccess(CaseworkerIdentity caseworker) {
    return caseworker != null
        && ("CASEWORKER".equals(caseworker.getRole()) || "ADMIN".equals(caseworker.getRole()));
  }

  private List<String> parseSelectedClaimOrder(String raw) {
    if (raw == null || raw.isBlank()) {
      return List.of();
    }
    return Arrays.stream(raw.split("[,\\r\\n]+"))
        .map(String::trim)
        .filter(token -> !token.isBlank())
        .toList();
  }

  private String redirectToWorklist(UUID debtorId, UUID worklistId) {
    return "redirect:/debtors/" + debtorId + "/retskraft-worklists/" + worklistId;
  }

  @Data
  public static class Section50OverrideForm {
    private String overrideReason;
    private String legalBasis;
    private Boolean expedited;
    private String selectedClaimOrder;
  }

  @Data
  public static class Section50ModregningDecisionForm {
    private String modregningOutcome;
    private String reason;
  }
}
