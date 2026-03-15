package dk.ufst.opendebt.creditor.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Controller for the accessibility statement page (tilgængelighedserklæring). */
@Controller
public class AccessibilityController {

  @GetMapping("/was")
  public String accessibilityStatement() {
    return "was";
  }
}
