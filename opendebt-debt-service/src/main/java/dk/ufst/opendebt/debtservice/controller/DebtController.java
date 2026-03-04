package dk.ufst.opendebt.debtservice.controller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import dk.ufst.opendebt.common.dto.DebtDto;
import dk.ufst.opendebt.debtservice.service.DebtService;
import dk.ufst.opendebt.debtservice.service.ReadinessValidationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/debts")
@RequiredArgsConstructor
@Tag(name = "Debt Management", description = "APIs for managing debts and readiness validation")
public class DebtController {

  private final DebtService debtService;
  private final ReadinessValidationService readinessValidationService;

  @GetMapping
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN') or hasRole('CREDITOR')")
  @Operation(summary = "List debts", description = "Returns a paginated list of debts")
  public ResponseEntity<Page<DebtDto>> listDebts(
      @Parameter(description = "Filter by creditor ID") @RequestParam(required = false)
          String creditorId,
      @Parameter(description = "Filter by debtor ID") @RequestParam(required = false)
          String debtorId,
      @Parameter(description = "Filter by status") @RequestParam(required = false)
          DebtDto.DebtStatus status,
      @Parameter(description = "Filter by readiness status") @RequestParam(required = false)
          DebtDto.ReadinessStatus readinessStatus,
      Pageable pageable) {
    return ResponseEntity.ok(
        debtService.listDebts(creditorId, debtorId, status, readinessStatus, pageable));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN') or hasRole('CREDITOR')")
  @Operation(summary = "Get debt by ID", description = "Returns a single debt by its ID")
  public ResponseEntity<DebtDto> getDebt(@PathVariable UUID id) {
    return ResponseEntity.ok(debtService.getDebtById(id));
  }

  @GetMapping("/debtor/{debtorId}")
  @PreAuthorize(
      "hasRole('CASEWORKER') or hasRole('ADMIN') or @debtorAccessChecker.canAccess(#debtorId)")
  @Operation(summary = "Get debts by debtor", description = "Returns all debts for a debtor")
  public ResponseEntity<List<DebtDto>> getDebtsByDebtor(@PathVariable String debtorId) {
    return ResponseEntity.ok(debtService.getDebtsByDebtor(debtorId));
  }

  @PostMapping
  @PreAuthorize("hasRole('CREDITOR') or hasRole('ADMIN')")
  @Operation(summary = "Register a new debt", description = "Registers a new debt for collection")
  public ResponseEntity<DebtDto> createDebt(@Valid @RequestBody DebtDto debtDto) {
    return ResponseEntity.status(HttpStatus.CREATED).body(debtService.createDebt(debtDto));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('CREDITOR') or hasRole('ADMIN')")
  @Operation(summary = "Update a debt", description = "Updates an existing debt")
  public ResponseEntity<DebtDto> updateDebt(
      @PathVariable UUID id, @Valid @RequestBody DebtDto debtDto) {
    return ResponseEntity.ok(debtService.updateDebt(id, debtDto));
  }

  @PostMapping("/{id}/validate-readiness")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(
      summary = "Validate debt readiness",
      description = "Validates if a debt is ready for collection (indrivelsesparat)")
  public ResponseEntity<DebtDto> validateReadiness(@PathVariable UUID id) {
    return ResponseEntity.ok(readinessValidationService.validateReadiness(id));
  }

  @PostMapping("/{id}/approve-readiness")
  @PreAuthorize("hasRole('SUPERVISOR') or hasRole('ADMIN')")
  @Operation(
      summary = "Approve debt readiness",
      description = "Manually approves a debt as ready for collection")
  public ResponseEntity<DebtDto> approveReadiness(@PathVariable UUID id) {
    return ResponseEntity.ok(readinessValidationService.approveReadiness(id));
  }

  @PostMapping("/{id}/reject-readiness")
  @PreAuthorize("hasRole('SUPERVISOR') or hasRole('ADMIN')")
  @Operation(
      summary = "Reject debt readiness",
      description = "Rejects a debt as not ready for collection with reason")
  public ResponseEntity<DebtDto> rejectReadiness(
      @PathVariable UUID id, @RequestParam String reason) {
    return ResponseEntity.ok(readinessValidationService.rejectReadiness(id, reason));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Cancel a debt", description = "Cancels a debt (soft delete)")
  public ResponseEntity<Void> cancelDebt(@PathVariable UUID id) {
    debtService.cancelDebt(id);
    return ResponseEntity.noContent().build();
  }
}
