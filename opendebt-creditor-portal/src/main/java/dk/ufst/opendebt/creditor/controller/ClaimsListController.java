package dk.ufst.opendebt.creditor.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;

import jakarta.servlet.http.HttpSession;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import dk.ufst.opendebt.creditor.client.DebtServiceClient;
import dk.ufst.opendebt.creditor.client.RestPage;
import dk.ufst.opendebt.creditor.dto.ClaimCountsDto;
import dk.ufst.opendebt.creditor.dto.ClaimListItemDto;
import dk.ufst.opendebt.creditor.dto.ClaimSearchParams;
import dk.ufst.opendebt.creditor.service.PortalSessionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Controller for the claims in recovery and zero-balance claims list pages (petition 029). */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ClaimsListController {

  private static final int DEFAULT_PAGE_SIZE = 20;
  private static final String CPR_TYPE = "CPR";
  private static final int CPR_VISIBLE_DIGITS = 6;
  private static final String CPR_MASK = "****";
  private static final String REDIRECT_DEMO_LOGIN = "redirect:/demo-login";
  private static final String MODEL_CURRENT_PAGE = "currentPage";
  private static final String MODEL_LIST_TYPE = "listType";

  private final DebtServiceClient debtServiceClient;
  private final PortalSessionService portalSessionService;

  /** Renders the claims-in-recovery list page shell. Table body loaded via HTMX. */
  @GetMapping("/fordringer")
  public String recoveryList(Model model, HttpSession session) {
    UUID actingCreditor = portalSessionService.resolveActingCreditor(null, session);
    if (actingCreditor == null) {
      return REDIRECT_DEMO_LOGIN;
    }
    model.addAttribute(MODEL_CURRENT_PAGE, "claims-recovery");
    model.addAttribute(MODEL_LIST_TYPE, "recovery");
    return "claims/recovery-list";
  }

  /** HTMX endpoint that returns the table body fragment for recovery claims. */
  @GetMapping("/api/claims/recovery")
  public String recoveryTableFragment(
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
        loadRecoveryClaims(
            actingCreditor,
            ClaimSearchParams.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .searchQuery(searchQuery)
                .searchType(searchType)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .build());

    censorCprNumbers(claims);
    addClaimsModelAttributes(
        model,
        claims,
        "recovery",
        sortBy,
        sortDirection,
        searchQuery,
        searchType,
        dateFrom,
        dateTo);
    return "claims/fragments/claims-table :: claimsTable";
  }

  /** Renders the zero-balance claims list page shell. Table body loaded via HTMX. */
  @GetMapping("/fordringer/nulfordringer")
  public String zeroBalanceList(Model model, HttpSession session) {
    UUID actingCreditor = portalSessionService.resolveActingCreditor(null, session);
    if (actingCreditor == null) {
      return REDIRECT_DEMO_LOGIN;
    }
    model.addAttribute(MODEL_CURRENT_PAGE, "claims-zerobalance");
    model.addAttribute(MODEL_LIST_TYPE, "zerobalance");
    return "claims/zero-balance-list";
  }

  /** HTMX endpoint that returns the table body fragment for zero-balance claims. */
  @GetMapping("/api/claims/zero-balance")
  public String zeroBalanceTableFragment(
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
        loadZeroBalanceClaims(
            actingCreditor,
            ClaimSearchParams.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .searchQuery(searchQuery)
                .searchType(searchType)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .build());

    censorCprNumbers(claims);
    addClaimsModelAttributes(
        model,
        claims,
        "zerobalance",
        sortBy,
        sortDirection,
        searchQuery,
        searchType,
        dateFrom,
        dateTo);
    return "claims/fragments/claims-table :: claimsTable";
  }

  /** Renders the claims counts page with optional date range filtering. */
  @GetMapping("/fordringer/optaellinger")
  public String claimsCounts(
      @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dateFrom,
      @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dateTo,
      Model model,
      HttpSession session) {

    UUID actingCreditor = portalSessionService.resolveActingCreditor(null, session);
    if (actingCreditor == null) {
      return REDIRECT_DEMO_LOGIN;
    }

    ClaimCountsDto counts = loadClaimCounts(actingCreditor, dateFrom, dateTo);
    model.addAttribute("counts", counts);
    model.addAttribute("dateFrom", dateFrom);
    model.addAttribute("dateTo", dateTo);
    model.addAttribute(MODEL_CURRENT_PAGE, "claims-counts");
    return "claims/counts";
  }

  private RestPage<ClaimListItemDto> loadRecoveryClaims(
      UUID creditorOrgId, ClaimSearchParams params) {
    try {
      RestPage<ClaimListItemDto> result =
          debtServiceClient.listClaimsInRecovery(
              creditorOrgId,
              ClaimSearchParams.builder()
                  .page(params.getPage())
                  .size(clampSize(params.getSize()))
                  .sortBy(params.getSortBy())
                  .sortDirection(params.getSortDirection())
                  .searchQuery(params.getSearchQuery())
                  .searchType(params.getSearchType())
                  .dateFrom(params.getDateFrom())
                  .dateTo(params.getDateTo())
                  .build());
      return result != null ? result : emptyPage();
    } catch (Exception ex) {
      log.warn("Failed to load recovery claims: {}", ex.getMessage());
      return emptyPage();
    }
  }

  private RestPage<ClaimListItemDto> loadZeroBalanceClaims(
      UUID creditorOrgId, ClaimSearchParams params) {
    try {
      RestPage<ClaimListItemDto> result =
          debtServiceClient.listZeroBalanceClaims(
              creditorOrgId,
              ClaimSearchParams.builder()
                  .page(params.getPage())
                  .size(clampSize(params.getSize()))
                  .sortBy(params.getSortBy())
                  .sortDirection(params.getSortDirection())
                  .searchQuery(params.getSearchQuery())
                  .searchType(params.getSearchType())
                  .dateFrom(params.getDateFrom())
                  .dateTo(params.getDateTo())
                  .build());
      return result != null ? result : emptyPage();
    } catch (Exception ex) {
      log.warn("Failed to load zero-balance claims: {}", ex.getMessage());
      return emptyPage();
    }
  }

  private ClaimCountsDto loadClaimCounts(UUID creditorOrgId, LocalDate dateFrom, LocalDate dateTo) {
    try {
      ClaimCountsDto counts;
      if (dateFrom != null || dateTo != null) {
        counts = debtServiceClient.getClaimCountsForDateRange(creditorOrgId, dateFrom, dateTo);
      } else {
        counts = debtServiceClient.getClaimCounts(creditorOrgId);
      }
      return counts != null ? counts : ClaimCountsDto.builder().build();
    } catch (Exception ex) {
      log.warn("Failed to load claim counts: {}", ex.getMessage());
      return ClaimCountsDto.builder().build();
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
    model.addAttribute(MODEL_CURRENT_PAGE, claims.getNumber());
    model.addAttribute("totalPages", claims.getTotalPages());
    model.addAttribute("totalElements", claims.getTotalElements());
    model.addAttribute("pageSize", claims.getSize());
    model.addAttribute(MODEL_LIST_TYPE, listType);
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
