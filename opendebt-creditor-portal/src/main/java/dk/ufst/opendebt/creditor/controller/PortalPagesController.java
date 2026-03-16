package dk.ufst.opendebt.creditor.controller;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import dk.ufst.opendebt.creditor.client.DebtServiceClient;
import dk.ufst.opendebt.creditor.dto.PortalDebtDto;
import dk.ufst.opendebt.creditor.service.PortalSessionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class PortalPagesController {

  private final DebtServiceClient debtServiceClient;
  private final PortalSessionService portalSessionService;

  @GetMapping("/fordringer")
  public String fordringer(Model model, HttpSession session) {
    UUID actingCreditor = portalSessionService.resolveActingCreditor(null, session);
    if (actingCreditor == null) {
      return "redirect:/demo-login";
    }
    List<PortalDebtDto> debts = loadDebts(actingCreditor);
    model.addAttribute("debts", debts);
    return "fordringer";
  }

  @GetMapping("/sager")
  public String sager() {
    return "sager";
  }

  private List<PortalDebtDto> loadDebts(UUID creditorOrgId) {
    try {
      var page = debtServiceClient.listDebts(creditorOrgId);
      if (page != null && page.getContent() != null) {
        return page.getContent();
      }
      return Collections.emptyList();
    } catch (Exception ex) {
      log.warn("Debt service unavailable: {}", ex.getMessage());
      return Collections.emptyList();
    }
  }
}
