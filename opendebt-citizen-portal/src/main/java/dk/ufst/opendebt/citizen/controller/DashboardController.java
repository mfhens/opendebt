package dk.ufst.opendebt.citizen.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import dk.ufst.opendebt.citizen.config.CitizenOidcUser;

@Controller
public class DashboardController {

  @GetMapping("/dashboard")
  public String dashboard(@AuthenticationPrincipal CitizenOidcUser user, Model model) {
    if (user != null) {
      model.addAttribute("personId", user.getPersonId());
    }
    return "dashboard";
  }
}
