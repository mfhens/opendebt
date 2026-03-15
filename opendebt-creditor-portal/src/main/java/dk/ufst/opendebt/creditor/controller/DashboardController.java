package dk.ufst.opendebt.creditor.controller;

import java.util.UUID;

import jakarta.servlet.http.HttpSession;

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

  /**
   * Hardcoded demo creditor org ID. In production this would be resolved from the OAuth2 security
   * context after Keycloak browser flow is wired.
   */
  static final UUID DEMO_CREDITOR_ORG_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

  private final CreditorServiceClient creditorServiceClient;
  private final PortalSessionService portalSessionService;

  @GetMapping("/")
  public String index(
      @RequestParam(name = "actAs", required = false) String actAsParam,
      Model model,
      HttpSession session) {

    UUID actingCreditor = portalSessionService.resolveActingCreditor(actAsParam, session);
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
          "Backend-tjenesten er ikke tilgængelig. Kontroller at creditor-service kører.");
      return "index";
    }

    if (creditor == null) {
      log.error("Creditor not found for orgId {}", actingCreditor);
      model.addAttribute(
          "backendError",
          "Fordringshaver ikke fundet for orgId: " + actingCreditor + ". Kontroller seed-data.");
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
          model.addAttribute(
              "actAsDeniedMessage",
              "Adgang nægtet: "
                  + (response.getMessage() != null
                      ? response.getMessage()
                      : "Du har ikke tilladelse til at handle på vegne af denne fordringshaver."));
        }
      }
    } catch (IllegalArgumentException ignored) {
      // Invalid UUID – already handled in session service
    }
  }
}
