package dk.ufst.opendebt.caseworker.controller;

import jakarta.servlet.http.HttpSession;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import dk.ufst.opendebt.caseworker.client.CaseServiceClient;
import dk.ufst.opendebt.caseworker.client.RestPage;
import dk.ufst.opendebt.caseworker.dto.CaseworkerIdentity;
import dk.ufst.opendebt.caseworker.service.CaseworkerSessionService;
import dk.ufst.opendebt.common.dto.CaseDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Displays the list of cases for the caseworker. */
@Slf4j
@Controller
@RequiredArgsConstructor
public class CaseListController {

  private final CaseServiceClient caseServiceClient;
  private final CaseworkerSessionService sessionService;
  private final MessageSource messageSource;

  @GetMapping("/cases")
  public String listCases(
      @RequestParam(name = "status", required = false) String status,
      @RequestParam(name = "caseState", required = false) String caseState,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size,
      HttpSession session,
      Model model) {

    CaseworkerIdentity caseworker = sessionService.getCurrentCaseworker(session);
    if (caseworker == null) {
      return "redirect:/demo-login";
    }

    model.addAttribute("caseworker", caseworker);
    model.addAttribute("currentPage", "cases");
    model.addAttribute("selectedCaseState", caseState);

    try {
      // Use caseState filter if provided, fall back to legacy status
      String filterStatus = caseState != null && !caseState.isBlank() ? caseState : status;
      RestPage<CaseDto> cases =
          caseServiceClient.listCases(filterStatus, caseworker.getId(), page, size);
      model.addAttribute("cases", cases);
    } catch (Exception ex) {
      log.error("Failed to load cases: {}", ex.getMessage());
      model.addAttribute(
          "backendError",
          messageSource.getMessage(
              "cases.error.backend",
              null,
              "Case service is unavailable. Please try again later.",
              LocaleContextHolder.getLocale()));
    }

    return "cases/list";
  }
}
