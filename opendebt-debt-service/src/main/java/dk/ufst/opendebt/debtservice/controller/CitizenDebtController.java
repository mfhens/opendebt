package dk.ufst.opendebt.debtservice.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import dk.ufst.opendebt.debtservice.dto.CitizenDebtSummaryResponse;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.service.CitizenDebtService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/citizen/debts")
@RequiredArgsConstructor
@Tag(name = "Citizen Debts", description = "Citizen-facing debt operations")
public class CitizenDebtController {

  private static final int DEFAULT_PAGE_NUMBER = 0;
  private static final int DEFAULT_PAGE_SIZE = 20;
  private static final int MAX_PAGE_SIZE = 100;

  private final CitizenDebtService citizenDebtService;

  @GetMapping
  @PreAuthorize("hasRole('CITIZEN')")
  @Operation(
      summary = "Get debt summary for authenticated citizen",
      description =
          "Returns debt list and totals for the authenticated citizen. NO PII, NO creditor internals.")
  public ResponseEntity<CitizenDebtSummaryResponse> getDebtSummary(
      @RequestParam(required = false) String status,
      @RequestParam(name = "pageNumber", required = false) Integer pageNumber,
      @RequestParam(name = "page", required = false) Integer legacyPageNumber,
      @RequestParam(name = "pageSize", required = false) Integer pageSize,
      @RequestParam(name = "size", required = false) Integer legacyPageSize,
      @RequestParam(defaultValue = "dueDate") String sortBy,
      @RequestParam(defaultValue = "ASC") String sortDirection,
      @RequestHeader(value = "X-Person-Id", required = false) String personIdHeader) {

    int resolvedPageNumber =
        pageNumber != null
            ? pageNumber
            : legacyPageNumber != null ? legacyPageNumber : DEFAULT_PAGE_NUMBER;
    int resolvedPageSize =
        pageSize != null ? pageSize : legacyPageSize != null ? legacyPageSize : DEFAULT_PAGE_SIZE;

    if (resolvedPageNumber < 0 || resolvedPageSize < 1 || resolvedPageSize > MAX_PAGE_SIZE) {
      log.warn(
          "Invalid citizen debt pagination parameters: pageNumber={}, pageSize={}",
          resolvedPageNumber,
          resolvedPageSize);
      return ResponseEntity.badRequest().build();
    }

    UUID personId = extractPersonId(personIdHeader);
    if (personId == null) {
      log.error("No person_id in security context for authenticated CITIZEN user");
      return ResponseEntity.status(500).build();
    }

    log.info("Citizen debt summary request: person_id={}, status={}", personId, status);

    // Parse status filter
    DebtEntity.DebtStatus statusFilter = null;
    if (status != null) {
      try {
        statusFilter = DebtEntity.DebtStatus.valueOf(status.toUpperCase());
      } catch (IllegalArgumentException e) {
        log.warn("Invalid status filter: {}", status);
        return ResponseEntity.badRequest().build();
      }
    }

    // Create pageable with sort
    Sort sort =
        "DESC".equalsIgnoreCase(sortDirection)
            ? Sort.by(sortBy).descending()
            : Sort.by(sortBy).ascending();
    Pageable pageable = PageRequest.of(resolvedPageNumber, resolvedPageSize, sort);

    CitizenDebtSummaryResponse response =
        citizenDebtService.getDebtSummary(personId, statusFilter, pageable);

    return ResponseEntity.ok(response);
  }

  private UUID extractPersonId(String personIdHeader) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    UUID jwtPersonId = extractPersonIdFromJwt(authentication);
    if (jwtPersonId != null) {
      return jwtPersonId;
    }

    UUID headerPersonId = extractPersonIdFromHeader(personIdHeader);
    if (headerPersonId != null) {
      return headerPersonId;
    }

    return extractPersonIdFromAuthenticationDetails(authentication);
  }

  private UUID extractPersonIdFromJwt(Authentication authentication) {
    if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
      return null;
    }

    String personIdClaim = jwtAuthenticationToken.getToken().getClaimAsString("person_id");
    if (personIdClaim == null || personIdClaim.isBlank()) {
      log.debug("JWT authentication present without person_id claim");
      return null;
    }
    return parseUuid(personIdClaim, "JWT person_id claim");
  }

  private UUID extractPersonIdFromHeader(String personIdHeader) {
    if (personIdHeader == null || personIdHeader.isBlank()) {
      return null;
    }
    return parseUuid(personIdHeader, "X-Person-Id header");
  }

  private UUID extractPersonIdFromAuthenticationDetails(Authentication authentication) {
    if (authentication == null) {
      return null;
    }

    Object details = authentication.getDetails();
    if (details instanceof Map<?, ?> detailsMap) {
      Object personIdObj = detailsMap.get("person_id");
      if (personIdObj instanceof UUID uuid) {
        return uuid;
      }
      if (personIdObj instanceof String s) {
        return parseUuid(s, "authentication details person_id");
      }
    }

    Object principalName = authentication.getPrincipal();
    log.warn(
        "person_id not found in authentication details, principal type: {}",
        principalName != null ? principalName.getClass().getName() : "null");

    return null;
  }

  private UUID parseUuid(String rawValue, String source) {
    try {
      return UUID.fromString(rawValue);
    } catch (IllegalArgumentException ex) {
      log.warn("Invalid {}: {}", source, rawValue);
      return null;
    }
  }
}
