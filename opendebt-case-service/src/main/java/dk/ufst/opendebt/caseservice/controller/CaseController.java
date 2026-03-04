package dk.ufst.opendebt.caseservice.controller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import dk.ufst.opendebt.caseservice.service.CaseService;
import dk.ufst.opendebt.common.dto.CaseDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/cases")
@RequiredArgsConstructor
@Tag(name = "Case Management", description = "APIs for managing debt collection cases")
public class CaseController {

  private final CaseService caseService;

  @GetMapping
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(summary = "List all cases", description = "Returns a paginated list of cases")
  public ResponseEntity<Page<CaseDto>> listCases(
      @Parameter(description = "Filter by status") @RequestParam(required = false)
          CaseDto.CaseStatus status,
      @Parameter(description = "Filter by assigned caseworker") @RequestParam(required = false)
          String caseworkerId,
      Pageable pageable) {
    return ResponseEntity.ok(caseService.listCases(status, caseworkerId, pageable));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(summary = "Get case by ID", description = "Returns a single case by its ID")
  public ResponseEntity<CaseDto> getCase(@PathVariable UUID id) {
    return ResponseEntity.ok(caseService.getCaseById(id));
  }

  @GetMapping("/debtor/{debtorId}")
  @PreAuthorize(
      "hasRole('CASEWORKER') or hasRole('ADMIN') or @debtorAccessChecker.canAccess(#debtorId)")
  @Operation(summary = "Get cases by debtor", description = "Returns all cases for a debtor")
  public ResponseEntity<List<CaseDto>> getCasesByDebtor(@PathVariable String debtorId) {
    return ResponseEntity.ok(caseService.getCasesByDebtor(debtorId));
  }

  @PostMapping
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(summary = "Create a new case", description = "Creates a new debt collection case")
  public ResponseEntity<CaseDto> createCase(@Valid @RequestBody CaseDto caseDto) {
    return ResponseEntity.status(HttpStatus.CREATED).body(caseService.createCase(caseDto));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(summary = "Update a case", description = "Updates an existing case")
  public ResponseEntity<CaseDto> updateCase(
      @PathVariable UUID id, @Valid @RequestBody CaseDto caseDto) {
    return ResponseEntity.ok(caseService.updateCase(id, caseDto));
  }

  @PostMapping("/{id}/assign")
  @PreAuthorize("hasRole('SUPERVISOR') or hasRole('ADMIN')")
  @Operation(summary = "Assign case to caseworker", description = "Assigns a case to a caseworker")
  public ResponseEntity<CaseDto> assignCase(
      @PathVariable UUID id, @RequestParam String caseworkerId) {
    return ResponseEntity.ok(caseService.assignCase(id, caseworkerId));
  }

  @PostMapping("/{id}/strategy")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(
      summary = "Set collection strategy",
      description = "Sets the active collection strategy for a case")
  public ResponseEntity<CaseDto> setStrategy(
      @PathVariable UUID id, @RequestParam CaseDto.CollectionStrategy strategy) {
    return ResponseEntity.ok(caseService.setCollectionStrategy(id, strategy));
  }

  @PostMapping("/{id}/close")
  @PreAuthorize("hasRole('SUPERVISOR') or hasRole('ADMIN')")
  @Operation(summary = "Close a case", description = "Closes a case with specified reason")
  public ResponseEntity<CaseDto> closeCase(
      @PathVariable UUID id, @RequestParam CaseDto.CaseStatus closureStatus) {
    return ResponseEntity.ok(caseService.closeCase(id, closureStatus));
  }
}
