package dk.ufst.opendebt.citizen.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AccessibilityController {

  @GetMapping("/was")
  public String accessibilityStatement() {
    return "was";
  }
}
