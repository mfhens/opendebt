package dk.ufst.opendebt.caseworker.controller;

import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import dk.ufst.opendebt.caseworker.client.PaymentServiceClient;
import dk.ufst.opendebt.caseworker.client.RestPage;
import dk.ufst.opendebt.caseworker.dto.CaseworkerIdentity;
import dk.ufst.opendebt.caseworker.dto.PortalLedgerEntryDto;
import dk.ufst.opendebt.caseworker.service.CaseworkerSessionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** HTMX fragment controller for posteringslog (transaction log) tabs. */
@Slf4j
@Controller
@RequiredArgsConstructor
public class TransactionLogController {

  private final PaymentServiceClient paymentServiceClient;
  private final CaseworkerSessionService sessionService;

  /** Returns the posteringslog fragment for a case (all debts). */
  @GetMapping("/cases/{caseId}/posteringslog")
  public String casePosteringslog(
      @PathVariable UUID caseId,
      @RequestParam(name = "category", required = false) String category,
      @RequestParam(name = "fromDate", required = false) String fromDate,
      @RequestParam(name = "toDate", required = false) String toDate,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "50") int size,
      HttpSession session,
      Model model) {

    CaseworkerIdentity caseworker = sessionService.getCurrentCaseworker(session);
    if (caseworker == null) {
      return "redirect:/demo-login";
    }

    try {
      RestPage<PortalLedgerEntryDto> ledger =
          paymentServiceClient.getLedgerByCase(caseId, category, fromDate, toDate, page, size);
      model.addAttribute("ledgerEntries", ledger.getContent());
      model.addAttribute("ledgerPage", ledger);
    } catch (Exception ex) {
      log.error("Failed to load posteringslog for case {}: {}", caseId, ex.getMessage());
      model.addAttribute("ledgerEntries", List.of());
    }

    model.addAttribute("caseId", caseId);
    model.addAttribute("category", category);
    model.addAttribute("fromDate", fromDate);
    model.addAttribute("toDate", toDate);

    return "fragments/posteringslog :: posteringslog";
  }

  /** Returns the posteringslog fragment for a specific debt. */
  @GetMapping("/cases/{caseId}/debts/{debtId}/posteringslog")
  public String debtPosteringslog(
      @PathVariable UUID caseId,
      @PathVariable UUID debtId,
      @RequestParam(name = "category", required = false) String category,
      @RequestParam(name = "fromDate", required = false) String fromDate,
      @RequestParam(name = "toDate", required = false) String toDate,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "50") int size,
      HttpSession session,
      Model model) {

    CaseworkerIdentity caseworker = sessionService.getCurrentCaseworker(session);
    if (caseworker == null) {
      return "redirect:/demo-login";
    }

    try {
      RestPage<PortalLedgerEntryDto> ledger =
          paymentServiceClient.getLedgerByDebt(debtId, category, fromDate, toDate, page, size);
      model.addAttribute("ledgerEntries", ledger.getContent());
      model.addAttribute("ledgerPage", ledger);
    } catch (Exception ex) {
      log.error(
          "Failed to load posteringslog for debt {} in case {}: {}",
          debtId,
          caseId,
          ex.getMessage());
      model.addAttribute("ledgerEntries", List.of());
    }

    model.addAttribute("caseId", caseId);
    model.addAttribute("debtId", debtId);
    model.addAttribute("category", category);
    model.addAttribute("fromDate", fromDate);
    model.addAttribute("toDate", toDate);

    return "fragments/posteringslog :: posteringslog";
  }
}
