package dk.ufst.opendebt.caseworker.controller;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import dk.ufst.opendebt.caseworker.dto.CaseworkerIdentity;
import dk.ufst.opendebt.caseworker.service.CaseworkerSessionService;

import lombok.RequiredArgsConstructor;

/** Landing page controller for the caseworker portal. Redirects to case list. */
@Controller
@RequiredArgsConstructor
public class DashboardController {

  private final CaseworkerSessionService sessionService;

  @GetMapping("/")
  public String index(HttpSession session, Model model) {
    CaseworkerIdentity caseworker = sessionService.getCurrentCaseworker(session);
    if (caseworker == null) {
      return "redirect:/demo-login";
    }
    model.addAttribute("caseworker", caseworker);
    return "redirect:/cases";
  }
}
