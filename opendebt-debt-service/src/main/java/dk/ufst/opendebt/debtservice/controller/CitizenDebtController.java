package dk.ufst.opendebt.debtservice.controller;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

  private final CitizenDebtService citizenDebtService;

  @GetMapping
  @PreAuthorize("hasRole('CITIZEN')")
  @Operation(
      summary = "Get debt summary for authenticated citizen",
      description =
          "Returns debt list and totals for the authenticated citizen. NO PII, NO creditor internals.")
  public ResponseEntity<CitizenDebtSummaryResponse> getDebtSummary(
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "dueDate") String sortBy,
      @RequestParam(defaultValue = "ASC") String sortDirection) {

    // Validate and limit page size
    if (size > 100) {
      size = 100;
    }
    if (size < 1) {
      size = 20;
    }

    // Extract person_id from SecurityContext (set by authentication handler)
    UUID personId = extractPersonIdFromSecurityContext();
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
    Pageable pageable = PageRequest.of(page, size, sort);

    CitizenDebtSummaryResponse response =
        citizenDebtService.getDebtSummary(personId, statusFilter, pageable);

    return ResponseEntity.ok(response);
  }

  /**
   * Extract person_id from SecurityContext. The authentication success handler (petition 025)
   * stores the person_id as an attribute in the Authentication object.
   */
  private UUID extractPersonIdFromSecurityContext() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      return null;
    }

    // Check for person_id in authentication details (set by OAuth2 success handler)
    Object details = authentication.getDetails();
    if (details instanceof java.util.Map) {
      @SuppressWarnings("unchecked")
      java.util.Map<String, Object> detailsMap = (java.util.Map<String, Object>) details;
      Object personIdObj = detailsMap.get("person_id");
      if (personIdObj instanceof UUID uuid) {
        return uuid;
      }
      if (personIdObj instanceof String s) {
        try {
          return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
          log.error("Invalid person_id format in security context: {}", personIdObj);
        }
      }
    }

    // Fallback: check for person_id attribute (session-based storage)
    Object principalName = authentication.getPrincipal();
    log.warn(
        "person_id not found in authentication details, principal type: {}",
        principalName != null ? principalName.getClass().getName() : "null");

    return null;
  }
}
