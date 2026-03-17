package dk.ufst.opendebt.creditor.controller;

import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import dk.ufst.opendebt.creditor.client.CreditorServiceClient;
import dk.ufst.opendebt.creditor.dto.PortalCreditorDto;
import dk.ufst.opendebt.creditor.service.PortalSessionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for the umbrella-user claimant selection page. Allows users associated with a parent
 * creditor to select which creditor they wish to act on behalf of.
 */
@Slf4j
@Controller
@RequestMapping("/vaelg-fordringshaver")
@RequiredArgsConstructor
public class ClaimantSelectionController {

  private final CreditorServiceClient creditorServiceClient;
  private final PortalSessionService portalSessionService;

  /** GET /vaelg-fordringshaver — displays the claimant selection page. */
  @GetMapping
  public String showSelection(Model model, HttpSession session) {
    model.addAttribute("currentPage", "claimant-selection");
    List<PortalCreditorDto> creditors = creditorServiceClient.listAllActive();
    model.addAttribute("creditors", creditors);
    return "vaelg-fordringshaver";
  }

  /** POST /vaelg-fordringshaver — sets the selected creditor in the session. */
  @PostMapping
  public String selectCreditor(
      @RequestParam("creditorOrgId") UUID creditorOrgId, HttpSession session) {
    portalSessionService.setActingCreditor(creditorOrgId, session);
    log.info("Umbrella-user selected creditor: {}", creditorOrgId);
    return "redirect:/";
  }
}
