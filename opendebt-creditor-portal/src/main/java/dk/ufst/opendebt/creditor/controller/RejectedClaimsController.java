package dk.ufst.opendebt.creditor.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import dk.ufst.opendebt.creditor.client.DebtServiceClient;
import dk.ufst.opendebt.creditor.client.RestPage;
import dk.ufst.opendebt.creditor.dto.ClaimListItemDto;
import dk.ufst.opendebt.creditor.dto.RejectedClaimDebtorDto;
import dk.ufst.opendebt.creditor.dto.RejectedClaimDetailDto;
import dk.ufst.opendebt.creditor.service.PortalSessionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Controller for the rejected claims list and detail views (petition 032). */
@Slf4j
@Controller
@RequiredArgsConstructor
public class RejectedClaimsController {

  private static final int DEFAULT_PAGE_SIZE = 20;
  private static final String CPR_TYPE = "CPR";
  private static final int CPR_VISIBLE_DIGITS = 6;
  private static final String CPR_MASK = "****";

  private final DebtServiceClient debtServiceClient;
  private final PortalSessionService portalSessionService;
  private final MessageSource messageSource;

  @Value("${opendebt.portal.rejected-claims.show-debtor-details:true}")
  private boolean showDebtorDetails;

  /** Renders the rejected claims list page shell. Table body loaded via HTMX. */
  @GetMapping("/fordringer/afviste")
  public String rejectedList(Model model, HttpSession session) {
    UUID actingCreditor = portalSessionService.resolveActingCreditor(null, session);
    if (actingCreditor == null) {
      return "redirect:/demo-login";
    }
    model.addAttribute("currentPage", "claims-rejected");
    model.addAttribute("listType", "rejected");
    return "claims/rejected-list";
  }

  /** HTMX endpoint that returns the table body fragment for rejected claims. */
  @GetMapping("/api/claims/rejected")
  public String rejectedTableFragment(
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
    RestPage<ClaimListItemDto> claims =
        loadRejectedClaims(
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
        model,
        claims,
        "rejected",
        sortBy,
        sortDirection,
        searchQuery,
        searchType,
        dateFrom,
        dateTo);
    return "claims/fragments/rejected-claims-table :: rejectedClaimsTable";
  }

  /** Renders the rejected claim detail page. */
  @GetMapping("/fordring/afvist/{id}")
  public String rejectedClaimDetail(@PathVariable UUID id, Model model, HttpSession session) {
    UUID actingCreditor = portalSessionService.resolveActingCreditor(null, session);
    if (actingCreditor == null) {
      return "redirect:/demo-login";
    }

    RejectedClaimDetailDto detail = loadRejectedClaimDetail(id, model);
    if (detail != null) {
      censorDebtorCprNumbers(detail);
    }

    model.addAttribute("claim", detail);
    model.addAttribute("claimId", id);
    model.addAttribute("showDebtorDetails", showDebtorDetails);
    model.addAttribute("currentPage", "claims-rejected");
    return "claims/rejected-detail";
  }

  private RestPage<ClaimListItemDto> loadRejectedClaims(
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
      RestPage<ClaimListItemDto> result =
          debtServiceClient.listRejectedClaims(
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
      log.warn("Failed to load rejected claims: {}", ex.getMessage());
      return emptyPage();
    }
  }

  private RejectedClaimDetailDto loadRejectedClaimDetail(UUID claimId, Model model) {
    try {
      RejectedClaimDetailDto detail = debtServiceClient.getRejectedClaimDetail(claimId);
      if (detail == null) {
        model.addAttribute(
            "serviceError",
            messageSource.getMessage(
                "rejected.detail.error.notfound", null, LocaleContextHolder.getLocale()));
      }
      return detail;
    } catch (Exception ex) {
      log.warn("Failed to load rejected claim detail for {}: {}", claimId, ex.getMessage());
      model.addAttribute(
          "serviceError",
          messageSource.getMessage(
              "rejected.detail.error.service", null, LocaleContextHolder.getLocale()));
      return null;
    }
  }

  /** Censors CPR numbers in the claim list, showing only the first 6 digits. */
  private void censorCprNumbers(RestPage<ClaimListItemDto> claims) {
    if (claims == null || claims.getContent() == null) {
      return;
    }
    for (ClaimListItemDto claim : claims.getContent()) {
      if (CPR_TYPE.equalsIgnoreCase(claim.getDebtorType())
          && claim.getDebtorIdentifier() != null
          && claim.getDebtorIdentifier().length() > CPR_VISIBLE_DIGITS) {
        claim.setDebtorIdentifier(
            claim.getDebtorIdentifier().substring(0, CPR_VISIBLE_DIGITS) + CPR_MASK);
      }
    }
  }

  /** Censors CPR numbers in the rejected claim detail debtor list. */
  private void censorDebtorCprNumbers(RejectedClaimDetailDto detail) {
    if (detail.getDebtors() == null) {
      return;
    }
    for (RejectedClaimDebtorDto debtor : detail.getDebtors()) {
      if (CPR_TYPE.equalsIgnoreCase(debtor.getIdentifierType())
          && debtor.getIdentifier() != null
          && debtor.getIdentifier().length() > CPR_VISIBLE_DIGITS) {
        debtor.setIdentifier(debtor.getIdentifier().substring(0, CPR_VISIBLE_DIGITS) + CPR_MASK);
      }
    }
  }

  private void addClaimsModelAttributes(
      Model model,
      RestPage<ClaimListItemDto> claims,
      String listType,
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
    model.addAttribute("listType", listType);
    model.addAttribute("sortBy", sortBy);
    model.addAttribute("sortDirection", sortDirection);
    model.addAttribute("searchQuery", searchQuery);
    model.addAttribute("searchType", searchType);
    model.addAttribute("dateFrom", dateFrom);
    model.addAttribute("dateTo", dateTo);
  }

  private RestPage<ClaimListItemDto> emptyPage() {
    return new RestPage<>(new ArrayList<>(), 0, DEFAULT_PAGE_SIZE, 0, 0);
  }

  private int clampSize(int size) {
    return size > 0 && size <= 100 ? size : DEFAULT_PAGE_SIZE;
  }
}
