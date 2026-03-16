package dk.ufst.opendebt.creditor.controller;

import java.util.UUID;

import jakarta.servlet.http.HttpSession;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import dk.ufst.opendebt.creditor.client.CreditorServiceClient;
import dk.ufst.opendebt.creditor.dto.AccessResolutionResponse;
import dk.ufst.opendebt.creditor.dto.PortalCreditorDto;
import dk.ufst.opendebt.creditor.service.PortalSessionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Landing page controller for the creditor portal. */
@Slf4j
@Controller
@RequiredArgsConstructor
public class DashboardController {

  private final CreditorServiceClient creditorServiceClient;
  private final MessageSource messageSource;
  private final PortalSessionService portalSessionService;

  @GetMapping("/")
  public String index(
      @RequestParam(name = "actAs", required = false) String actAsParam,
      Model model,
      HttpSession session) {

    UUID actingCreditor = portalSessionService.resolveActingCreditor(actAsParam, session);
    if (actingCreditor == null) {
      return "redirect:/demo-login";
    }
    UUID representedCreditor = portalSessionService.getRepresentedCreditor(session);

    // Check for acting-on-behalf-of denial when an explicit actAs was requested
    if (actAsParam != null && !actAsParam.isBlank() && representedCreditor == null) {
      handleActAsDenialIfApplicable(actAsParam, actingCreditor, model);
    }

    PortalCreditorDto creditor;
    try {
      creditor = creditorServiceClient.getByCreditorOrgId(actingCreditor);
    } catch (Exception ex) {
      log.error("Creditor service unavailable: {}", ex.getMessage());
      model.addAttribute(
          "backendError",
          messageSource.getMessage(
              "controller.dashboard.backend.unavailable", null, LocaleContextHolder.getLocale()));
      return "index";
    }

    if (creditor == null) {
      log.error("Creditor not found for orgId {}", actingCreditor);
      model.addAttribute(
          "backendError",
          messageSource.getMessage(
              "controller.dashboard.creditor.notfound",
              new Object[] {actingCreditor},
              LocaleContextHolder.getLocale()));
      return "index";
    }

    model.addAttribute("creditor", creditor);
    model.addAttribute("actingCreditorOrgId", actingCreditor);
    if (representedCreditor != null) {
      model.addAttribute("representedCreditorOrgId", representedCreditor);
    }
    model.addAttribute("showActingSelector", creditor.getParentCreditorId() != null);
    return "index";
  }

  private void handleActAsDenialIfApplicable(String actAsParam, UUID actingCreditor, Model model) {
    try {
      UUID requestedOrgId = UUID.fromString(actAsParam);
      if (!requestedOrgId.equals(actingCreditor)) {
        AccessResolutionResponse response =
            portalSessionService.tryResolveAccess(actingCreditor, requestedOrgId);
        if (response != null && !response.isAllowed()) {
          String reason =
              response.getMessage() != null
                  ? response.getMessage()
                  : messageSource.getMessage(
                      "controller.dashboard.access.denied.default",
                      null,
                      LocaleContextHolder.getLocale());
          model.addAttribute(
              "actAsDeniedMessage",
              messageSource.getMessage(
                  "controller.dashboard.access.denied",
                  new Object[] {reason},
                  LocaleContextHolder.getLocale()));
        }
      }
    } catch (IllegalArgumentException ignored) {
      // Invalid UUID – already handled in session service
    }
  }
}
