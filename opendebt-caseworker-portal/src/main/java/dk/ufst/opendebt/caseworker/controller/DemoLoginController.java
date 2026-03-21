package dk.ufst.opendebt.caseworker.controller;

import java.util.List;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import dk.ufst.opendebt.caseworker.dto.CaseworkerIdentity;
import dk.ufst.opendebt.caseworker.service.CaseworkerSessionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Demo login page that lets the user pick a caseworker identity from hard-coded options. */
@Slf4j
@Controller
@RequiredArgsConstructor
public class DemoLoginController {

  private static final List<CaseworkerIdentity> DEMO_CASEWORKERS =
      List.of(
          CaseworkerIdentity.builder()
              .id("anna-jensen")
              .name("Anna Jensen")
              .role("CASEWORKER")
              .description("Sagsbehandler")
              .build(),
          CaseworkerIdentity.builder()
              .id("erik-sorensen")
              .name("Erik S\u00f8rensen")
              .role("SENIOR_CASEWORKER")
              .description("Senior sagsbehandler")
              .build(),
          CaseworkerIdentity.builder()
              .id("mette-larsen")
              .name("Mette Larsen")
              .role("TEAM_LEAD")
              .description("Teamleder")
              .build(),
          CaseworkerIdentity.builder()
              .id("bro-karsten")
              .name("Bro Karsten")
              .role("CONFIGURATION_MANAGER")
              .description("Konfigurationsoperatør")
              .build(),
          CaseworkerIdentity.builder()
              .id("lars-nielsen")
              .name("Lars Nielsen")
              .role("CONFIGURATION_MANAGER")
              .description("Konfigurationsansvarlig")
              .build(),
          CaseworkerIdentity.builder()
              .id("system-admin")
              .name("System Administrator")
              .role("ADMIN")
              .description("Systemadministrator")
              .build());

  private final CaseworkerSessionService sessionService;

  @GetMapping("/demo-login")
  public String showLoginPage(Model model) {
    model.addAttribute("caseworkers", DEMO_CASEWORKERS);
    return "demo-login";
  }

  @PostMapping("/demo-login")
  public String handleLogin(
      @RequestParam("caseworkerId") String caseworkerId, HttpSession session) {
    CaseworkerIdentity selected =
        DEMO_CASEWORKERS.stream()
            .filter(c -> c.getId().equals(caseworkerId))
            .findFirst()
            .orElse(DEMO_CASEWORKERS.get(0));

    sessionService.setCurrentCaseworker(selected, session);
    log.info("Demo login: selected caseworker={}", selected.getName());
    return "redirect:/cases";
  }

  @GetMapping("/demo-logout")
  public String handleLogout(HttpSession session) {
    sessionService.clearSession(session);
    return "redirect:/demo-login";
  }
}
