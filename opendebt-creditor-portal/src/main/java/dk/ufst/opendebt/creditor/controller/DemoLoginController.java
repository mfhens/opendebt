package dk.ufst.opendebt.creditor.controller;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import dk.ufst.opendebt.creditor.client.CreditorServiceClient;
import dk.ufst.opendebt.creditor.dto.PortalCreditorDto;
import dk.ufst.opendebt.creditor.service.PortalSessionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Demo login page that lets the user pick a fordringshaver from a dropdown. */
@Slf4j
@Controller
@RequiredArgsConstructor
public class DemoLoginController {

  private final CreditorServiceClient creditorServiceClient;
  private final PortalSessionService portalSessionService;

  @GetMapping("/demo-login")
  public String showLoginPage(Model model) {
    List<PortalCreditorDto> creditors = creditorServiceClient.listAllActive();
    Map<String, List<PortalCreditorDto>> grouped =
        creditors.stream()
            .collect(
                Collectors.groupingBy(
                    c -> c.getCreditorType() != null ? c.getCreditorType() : "OTHER",
                    TreeMap::new,
                    Collectors.toList()));
    model.addAttribute("creditorsByType", grouped);
    return "demo-login";
  }

  @PostMapping("/demo-login")
  public String handleLogin(
      @RequestParam("creditorOrgId") UUID creditorOrgId, HttpSession session) {
    portalSessionService.setActingCreditor(creditorOrgId, session);
    log.info("Demo login: selected creditorOrgId={}", creditorOrgId);
    return "redirect:/";
  }

  @GetMapping("/demo-logout")
  public String handleLogout(HttpSession session) {
    portalSessionService.clearActingOnBehalfOf(session);
    return "redirect:/demo-login";
  }
}
