package dk.ufst.opendebt.creditor.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import dk.ufst.opendebt.creditor.client.DebtServiceClient;
import dk.ufst.opendebt.creditor.client.RestPage;
import dk.ufst.opendebt.creditor.dto.HearingApproveRequestDto;
import dk.ufst.opendebt.creditor.dto.HearingClaimDetailDto;
import dk.ufst.opendebt.creditor.dto.HearingClaimListItemDto;
import dk.ufst.opendebt.creditor.dto.HearingDebtorErrorDto;
import dk.ufst.opendebt.creditor.dto.HearingWithdrawRequestDto;
import dk.ufst.opendebt.creditor.service.PortalSessionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for hearing claims list and detail views (petition 031). Provides paginated list,
 * detail view with write-up info, and approve/withdraw actions.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class HearingClaimsController {

  private static final int DEFAULT_PAGE_SIZE = 20;
  private static final String CPR_TYPE = "CPR";
  private static final int CPR_VISIBLE_DIGITS = 6;
  private static final String CPR_MASK = "****";

  /** Action codes that indicate a write-up (opskrivning). */
  private static final Set<String> WRITE_UP_ACTION_CODES =
      Set.of(
          "OPSKRIVNING_REGULERING",
          "FEJLAGTIG_HOVEDSTOL_INDBERETNING",
          "OPSKRIVNING_OMGJORT_NEDSKRIVNING_REGULERING",
          "OPSKRIVNING_ANNULLERET_NEDSKRIVNING_INDBETALING");

  private static final String FEJLAGTIG_HOVEDSTOL_CODE = "FEJLAGTIG_HOVEDSTOL_INDBERETNING";

  private final DebtServiceClient debtServiceClient;
  private final PortalSessionService portalSessionService;
  private final MessageSource messageSource;

  /** Renders the hearing claims list page shell. Table body loaded via HTMX. */
  @GetMapping("/fordringer/hoering")
  public String hearingList(Model model, HttpSession session) {
    UUID actingCreditor = portalSessionService.resolveActingCreditor(null, session);
    if (actingCreditor == null) {
      return "redirect:/demo-login";
    }
    model.addAttribute("currentPage", "claims-hearing");
    model.addAttribute("listType", "hearing");
    return "claims/hearing-list";
  }

  /** HTMX endpoint that returns the table body fragment for hearing claims. */
  @GetMapping("/api/claims/hearing")
  public String hearingTableFragment(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String sortBy,
      @RequestParam(required = false, defaultValue = "asc") String sortDirection,
      @RequestParam(required = false) String searchQuery,
      @RequestParam(required = false) String searchType,
      @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dateFrom,
      @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dateTo,
      Model model,
      HttpSession session) {

    UUID actingCreditor = portalSessionService.resolveActingCreditor(null, session);
    RestPage<HearingClaimListItemDto> claims =
        loadHearingClaims(
            actingCreditor,
            page,
            size,
            sortBy,
            sortDirection,
            searchQuery,
            searchType,
            dateFrom,
            dateTo);

    censorCprNumbers(claims);
    addClaimsModelAttributes(
        model, claims, sortBy, sortDirection, searchQuery, searchType, dateFrom, dateTo);
    return "claims/fragments/hearing-table :: hearingTable";
  }

  /** Renders the hearing claim detail page. */
  @GetMapping("/fordringer/hoering/{id}")
  public String hearingDetail(@PathVariable UUID id, Model model, HttpSession session) {
    UUID actingCreditor = portalSessionService.resolveActingCreditor(null, session);
    if (actingCreditor == null) {
      return "redirect:/demo-login";
    }

    HearingClaimDetailDto detail = loadHearingClaimDetail(id, model);
    if (detail != null) {
      censorDetailCprNumbers(detail);
      model.addAttribute("claim", detail);
      model.addAttribute(
          "isWriteUp",
          detail.getActionCode() != null && WRITE_UP_ACTION_CODES.contains(detail.getActionCode()));
      model.addAttribute(
          "showChangedPrincipal", FEJLAGTIG_HOVEDSTOL_CODE.equals(detail.getActionCode()));
    }

    model.addAttribute("claimId", id);
    model.addAttribute("currentPage", "claims-hearing");
    model.addAttribute("approveForm", new HearingApproveRequestDto());
    model.addAttribute("withdrawForm", new HearingWithdrawRequestDto());
    return "claims/hearing-detail";
  }

  /** POST: Approves a hearing claim with justification. */
  @PostMapping("/fordringer/hoering/{id}/approve")
  public String approveHearing(
      @PathVariable UUID id,
      @Valid @ModelAttribute("approveForm") HearingApproveRequestDto approveForm,
      BindingResult bindingResult,
      Model model,
      HttpSession session,
      RedirectAttributes redirectAttributes) {

    UUID actingCreditor = portalSessionService.resolveActingCreditor(null, session);
    if (actingCreditor == null) {
      return "redirect:/demo-login";
    }

    if (bindingResult.hasErrors()) {
      return reloadDetailWithError(id, model, session, null);
    }

    try {
      debtServiceClient.approveHearingClaim(id, approveForm);
      log.info("Hearing claim {} approved by creditor {}", id, actingCreditor);
      redirectAttributes.addFlashAttribute(
          "successMessage",
          messageSource.getMessage(
              "hearing.approve.success", null, LocaleContextHolder.getLocale()));
      return "redirect:/fordringer/hoering/" + id;
    } catch (Exception ex) {
      log.error("Failed to approve hearing claim {}: {}", id, ex.getMessage(), ex);
      return reloadDetailWithError(
          id,
          model,
          session,
          messageSource.getMessage("hearing.approve.error", null, LocaleContextHolder.getLocale()));
    }
  }

  /** POST: Withdraws a hearing claim with reason. */
  @PostMapping("/fordringer/hoering/{id}/withdraw")
  public String withdrawHearing(
      @PathVariable UUID id,
      @Valid @ModelAttribute("withdrawForm") HearingWithdrawRequestDto withdrawForm,
      BindingResult bindingResult,
      Model model,
      HttpSession session,
      RedirectAttributes redirectAttributes) {

    UUID actingCreditor = portalSessionService.resolveActingCreditor(null, session);
    if (actingCreditor == null) {
      return "redirect:/demo-login";
    }

    if (bindingResult.hasErrors()) {
      return reloadDetailWithError(id, model, session, null);
    }

    try {
      debtServiceClient.withdrawHearingClaim(id, withdrawForm);
      log.info("Hearing claim {} withdrawn by creditor {}", id, actingCreditor);
      redirectAttributes.addFlashAttribute(
          "successMessage",
          messageSource.getMessage(
              "hearing.withdraw.success", null, LocaleContextHolder.getLocale()));
      return "redirect:/fordringer/hoering/" + id;
    } catch (Exception ex) {
      log.error("Failed to withdraw hearing claim {}: {}", id, ex.getMessage(), ex);
      return reloadDetailWithError(
          id,
          model,
          session,
          messageSource.getMessage(
              "hearing.withdraw.error", null, LocaleContextHolder.getLocale()));
    }
  }

  // --- Private helpers ---

  private String reloadDetailWithError(UUID id, Model model, HttpSession session, String error) {
    HearingClaimDetailDto detail = loadHearingClaimDetail(id, model);
    if (detail != null) {
      censorDetailCprNumbers(detail);
      model.addAttribute("claim", detail);
      model.addAttribute(
          "isWriteUp",
          detail.getActionCode() != null && WRITE_UP_ACTION_CODES.contains(detail.getActionCode()));
      model.addAttribute(
          "showChangedPrincipal", FEJLAGTIG_HOVEDSTOL_CODE.equals(detail.getActionCode()));
    }
    model.addAttribute("claimId", id);
    model.addAttribute("currentPage", "claims-hearing");
    if (!model.containsAttribute("approveForm")) {
      model.addAttribute("approveForm", new HearingApproveRequestDto());
    }
    if (!model.containsAttribute("withdrawForm")) {
      model.addAttribute("withdrawForm", new HearingWithdrawRequestDto());
    }
    if (error != null) {
      model.addAttribute("actionError", error);
    }
    return "claims/hearing-detail";
  }

  private RestPage<HearingClaimListItemDto> loadHearingClaims(
      UUID creditorOrgId,
      int page,
      int size,
      String sortBy,
      String sortDirection,
      String searchQuery,
      String searchType,
      LocalDate dateFrom,
      LocalDate dateTo) {
    try {
      RestPage<HearingClaimListItemDto> result =
          debtServiceClient.listHearingClaims(
              creditorOrgId,
              page,
              clampSize(size),
              sortBy,
              sortDirection,
              searchQuery,
              searchType,
              dateFrom,
              dateTo);
      return result != null ? result : emptyPage();
    } catch (Exception ex) {
      log.warn("Failed to load hearing claims: {}", ex.getMessage());
      return emptyPage();
    }
  }

  private HearingClaimDetailDto loadHearingClaimDetail(UUID claimId, Model model) {
    try {
      HearingClaimDetailDto detail = debtServiceClient.getHearingClaimDetail(claimId);
      if (detail == null) {
        model.addAttribute(
            "serviceError",
            messageSource.getMessage(
                "hearing.detail.error.notfound", null, LocaleContextHolder.getLocale()));
      }
      return detail;
    } catch (Exception ex) {
      log.warn("Failed to load hearing claim detail for {}: {}", claimId, ex.getMessage());
      model.addAttribute(
          "serviceError",
          messageSource.getMessage(
              "hearing.detail.error.service", null, LocaleContextHolder.getLocale()));
      return null;
    }
  }

  /** Censors CPR numbers in the hearing claim list, showing only the first 6 digits. */
  private void censorCprNumbers(RestPage<HearingClaimListItemDto> claims) {
    if (claims == null || claims.getContent() == null) {
      return;
    }
    for (HearingClaimListItemDto claim : claims.getContent()) {
      if (CPR_TYPE.equalsIgnoreCase(claim.getDebtorType())
          && claim.getDebtorIdentifier() != null
          && claim.getDebtorIdentifier().length() > CPR_VISIBLE_DIGITS) {
        claim.setDebtorIdentifier(
            claim.getDebtorIdentifier().substring(0, CPR_VISIBLE_DIGITS) + CPR_MASK);
      }
    }
  }

  /** Censors CPR numbers in the hearing claim detail debtor list. */
  private void censorDetailCprNumbers(HearingClaimDetailDto detail) {
    if (detail.getDebtorsWithErrors() == null) {
      return;
    }
    for (HearingDebtorErrorDto debtor : detail.getDebtorsWithErrors()) {
      if (CPR_TYPE.equalsIgnoreCase(debtor.getDebtorType())
          && debtor.getDebtorIdentifier() != null
          && debtor.getDebtorIdentifier().length() > CPR_VISIBLE_DIGITS) {
        debtor.setDebtorIdentifier(
            debtor.getDebtorIdentifier().substring(0, CPR_VISIBLE_DIGITS) + CPR_MASK);
      }
    }
  }

  private void addClaimsModelAttributes(
      Model model,
      RestPage<HearingClaimListItemDto> claims,
      String sortBy,
      String sortDirection,
      String searchQuery,
      String searchType,
      LocalDate dateFrom,
      LocalDate dateTo) {
    model.addAttribute("claims", claims.getContent());
    model.addAttribute("currentPage", claims.getNumber());
    model.addAttribute("totalPages", claims.getTotalPages());
    model.addAttribute("totalElements", claims.getTotalElements());
    model.addAttribute("pageSize", claims.getSize());
    model.addAttribute("listType", "hearing");
    model.addAttribute("sortBy", sortBy);
    model.addAttribute("sortDirection", sortDirection);
    model.addAttribute("searchQuery", searchQuery);
    model.addAttribute("searchType", searchType);
    model.addAttribute("dateFrom", dateFrom);
    model.addAttribute("dateTo", dateTo);
  }

  private RestPage<HearingClaimListItemDto> emptyPage() {
    return new RestPage<>(new ArrayList<>(), 0, DEFAULT_PAGE_SIZE, 0, 0);
  }

  private int clampSize(int size) {
    return size > 0 && size <= 100 ? size : DEFAULT_PAGE_SIZE;
  }
}
