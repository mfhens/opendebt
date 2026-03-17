package dk.ufst.opendebt.creditor.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles remaining portal page routes not covered by dedicated controllers. Claims list routes
 * ({@code /fordringer}, {@code /fordringer/nulfordringer}) are handled by {@link
 * ClaimsListController} (petition 029). Rejected claims ({@code /fordringer/afviste}) are handled
 * by {@link RejectedClaimsController} (petition 032).
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class PortalPagesController {

  @GetMapping("/sager")
  public String sager(Model model) {
    model.addAttribute("currentPage", "cases");
    return "sager";
  }

  // Notifications routes moved to NotificationController (petition 035)

  // Reconciliation routes moved to ReconciliationController (petition 036)
}
