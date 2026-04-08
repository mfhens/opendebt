package dk.ufst.opendebt.caseworker.controller;

import java.util.List;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import dk.ufst.opendebt.caseworker.client.PaymentServiceClient;
import dk.ufst.opendebt.caseworker.dto.CaseworkerIdentity;
import dk.ufst.opendebt.caseworker.service.CaseworkerSessionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Renders the GIL § 4 payment application order (dækningsrækkefølge) as a read-only ranked list.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class DaekningsRaekkefoeigenViewController {

  private final PaymentServiceClient paymentServiceClient;
  private final CaseworkerSessionService sessionService;

  @GetMapping("/caseworker/debtors/{debtorId}/daekningsraekkefoelge")
  public String view(@PathVariable String debtorId, HttpSession session, Model model) {
    CaseworkerIdentity caseworker = sessionService.getCurrentCaseworker(session);
    if (caseworker == null) {
      return "redirect:/demo-login";
    }
    model.addAttribute("caseworker", caseworker);
    model.addAttribute("debtorId", debtorId);
    model.addAttribute("currentPage", "cases");
    try {
      model.addAttribute("positions", paymentServiceClient.getDaekningsraekkefoelge(debtorId));
    } catch (Exception ex) {
      log.warn("Failed to load daekningsraekkefoelge for debtor {}: {}", debtorId, ex.getMessage());
      model.addAttribute("positions", List.of());
    }
    return "daekningsraekkefoelge";
  }
}
