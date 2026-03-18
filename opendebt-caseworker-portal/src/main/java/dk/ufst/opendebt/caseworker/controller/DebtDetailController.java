package dk.ufst.opendebt.caseworker.controller;

import java.util.UUID;

import jakarta.servlet.http.HttpSession;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import dk.ufst.opendebt.caseworker.client.CaseServiceClient;
import dk.ufst.opendebt.caseworker.client.DebtServiceClient;
import dk.ufst.opendebt.caseworker.client.PaymentServiceClient;
import dk.ufst.opendebt.caseworker.client.RestPage;
import dk.ufst.opendebt.caseworker.dto.CaseworkerIdentity;
import dk.ufst.opendebt.caseworker.dto.PortalLedgerEntryDto;
import dk.ufst.opendebt.caseworker.dto.PortalLedgerSummaryDto;
import dk.ufst.opendebt.caseworker.service.CaseworkerSessionService;
import dk.ufst.opendebt.common.dto.CaseDto;
import dk.ufst.opendebt.common.dto.DebtDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Displays debt detail with posteringslog and balance summary. */
@Slf4j
@Controller
@RequiredArgsConstructor
public class DebtDetailController {

  private final CaseServiceClient caseServiceClient;
  private final DebtServiceClient debtServiceClient;
  private final PaymentServiceClient paymentServiceClient;
  private final CaseworkerSessionService sessionService;
  private final MessageSource messageSource;

  @GetMapping("/cases/{caseId}/debts/{debtId}")
  public String debtDetail(
      @PathVariable UUID caseId, @PathVariable UUID debtId, HttpSession session, Model model) {

    CaseworkerIdentity caseworker = sessionService.getCurrentCaseworker(session);
    if (caseworker == null) {
      return "redirect:/demo-login";
    }

    model.addAttribute("caseworker", caseworker);
    model.addAttribute("currentPage", "cases");
    model.addAttribute("caseId", caseId);

    try {
      CaseDto caseDto = caseServiceClient.getCase(caseId);
      model.addAttribute("caseDto", caseDto);

      DebtDto debt = debtServiceClient.getDebt(debtId);
      model.addAttribute("debt", debt);

      // Load ledger entries for this debt
      RestPage<PortalLedgerEntryDto> ledger =
          paymentServiceClient.getLedgerByDebt(debtId, null, null, null, 0, 50);
      model.addAttribute("ledgerEntries", ledger.getContent());

      // Load balance summary
      PortalLedgerSummaryDto summary = paymentServiceClient.getLedgerSummary(debtId);
      model.addAttribute("summary", summary);
    } catch (Exception ex) {
      log.error("Failed to load debt {} in case {}: {}", debtId, caseId, ex.getMessage());
      model.addAttribute(
          "backendError",
          messageSource.getMessage(
              "debt.detail.error.backend",
              null,
              "Failed to load debt details. Please try again later.",
              LocaleContextHolder.getLocale()));
    }

    return "cases/debt-detail";
  }
}
