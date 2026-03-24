package dk.ufst.opendebt.debtservice.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import dk.ufst.opendebt.debtservice.dto.ClaimSubmissionResponse;
import dk.ufst.opendebt.debtservice.dto.InterestRecalculationResult;
import dk.ufst.opendebt.debtservice.dto.TransferForCollectionRequest;
import dk.ufst.opendebt.debtservice.service.ClaimLifecycleService;
import dk.ufst.opendebt.debtservice.service.ClaimSubmissionService;
import dk.ufst.opendebt.debtservice.service.DebtService;
import dk.ufst.opendebt.debtservice.service.InterestRecalculationService;
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
  private final ClaimSubmissionService claimSubmissionService;
  private final ClaimLifecycleService claimLifecycleService;
  private final InterestRecalculationService interestRecalculationService;

  @GetMapping
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN') or hasRole('CREDITOR')")
  @Operation(
      summary = "List debts",
      description = "Returns a paginated list of debts, or a specific set by IDs")
  public ResponseEntity<Page<DebtDto>> listDebts(
      @Parameter(
              description =
                  "Filter by specific debt IDs (comma-separated); ignores other filters when present")
          @RequestParam(required = false)
          List<UUID> ids,
      @Parameter(description = "Filter by creditor ID") @RequestParam(required = false)
          String creditorId,
      @Parameter(description = "Filter by debtor ID") @RequestParam(required = false)
          String debtorId,
      @Parameter(description = "Filter by status") @RequestParam(required = false)
          DebtDto.DebtStatus status,
      @Parameter(description = "Filter by readiness status") @RequestParam(required = false)
          DebtDto.ReadinessStatus readinessStatus,
      Pageable pageable) {
    if (ids != null && !ids.isEmpty()) {
      List<DebtDto> result = debtService.getDebtsByIds(ids);
      return ResponseEntity.ok(new org.springframework.data.domain.PageImpl<>(result));
    }
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

  @PostMapping("/submit")
  @PreAuthorize("hasRole('CREDITOR') or hasRole('ADMIN')")
  @Operation(
      summary = "Submit a claim for collection",
      description =
          "Validates the claim against inddrivelsesparathed rules and returns "
              + "UDFOERT (accepted), AFVIST (rejected), or HOERING (pending review)")
  public ResponseEntity<ClaimSubmissionResponse> submitClaim(@Valid @RequestBody DebtDto debtDto) {
    ClaimSubmissionResponse response = claimSubmissionService.submitClaim(debtDto);
    HttpStatus httpStatus =
        response.getOutcome() == ClaimSubmissionResponse.Outcome.AFVIST
            ? HttpStatus.UNPROCESSABLE_ENTITY
            : HttpStatus.CREATED;
    return ResponseEntity.status(httpStatus).body(response);
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

  @PostMapping("/{id}/evaluate-state")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN') or hasRole('SERVICE')")
  @Operation(
      summary = "Evaluate claim lifecycle state",
      description =
          "Evaluates whether a claim should transition to RESTANCE based on payment deadline and balance")
  public ResponseEntity<DebtDto> evaluateClaimState(
      @PathVariable UUID id,
      @Parameter(description = "Evaluation date (defaults to today)")
          @RequestParam(required = false)
          LocalDate evaluationDate) {
    LocalDate date = evaluationDate != null ? evaluationDate : LocalDate.now();
    return ResponseEntity.ok(debtService.toDto(claimLifecycleService.evaluateClaimState(id, date)));
  }

  @PostMapping("/{id}/transfer-for-collection")
  @PreAuthorize("hasRole('CREDITOR') or hasRole('ADMIN')")
  @Operation(
      summary = "Transfer a restance for collection",
      description =
          "Transfers a restance to the restanceinddrivelsesmyndighed (overdragelse til inddrivelse)")
  public ResponseEntity<DebtDto> transferForCollection(
      @PathVariable UUID id, @Valid @RequestBody TransferForCollectionRequest request) {
    return ResponseEntity.ok(
        debtService.toDto(
            claimLifecycleService.transferForCollection(id, request.getRecipientId())));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Cancel a debt", description = "Cancels a debt (soft delete)")
  public ResponseEntity<Void> cancelDebt(@PathVariable UUID id) {
    debtService.cancelDebt(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/by-ocr")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN') or hasRole('SERVICE')")
  @Operation(
      summary = "Find debts by OCR-linje",
      description = "Returns debts matching the given Betalingsservice OCR-linje")
  public ResponseEntity<List<DebtDto>> findByOcrLine(@RequestParam String ocrLine) {
    return ResponseEntity.ok(debtService.findByOcrLine(ocrLine));
  }

  @PostMapping("/{id}/write-down")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN') or hasRole('SERVICE')")
  @Operation(
      summary = "Write down a debt",
      description = "Reduces the outstanding balance of a debt by the specified amount")
  public ResponseEntity<DebtDto> writeDown(@PathVariable UUID id, @RequestParam BigDecimal amount) {
    return ResponseEntity.ok(debtService.writeDown(id, amount));
  }

  @PostMapping("/{id}/interest/recalculate")
  @PreAuthorize("hasRole('SERVICE')")
  @Operation(
      summary = "Retroactively recalculate interest after a crossing transaction",
      description =
          "Deletes interest_journal_entries for the debt from the given date forward and"
              + " recalculates them using the current (post write-down) outstanding balance."
              + " Called by payment-service via Flowable orchestration (ADR-0019) when a crossing"
              + " transaction is detected (petition039).")
  public ResponseEntity<InterestRecalculationResult> recalculateInterest(
      @PathVariable UUID id,
      @Parameter(
              description =
                  "Earliest accrual date to recalculate (inclusive). Must be in the past.")
          @RequestParam
          LocalDate from) {
    return ResponseEntity.ok(interestRecalculationService.recalculateFromDate(id, from));
  }
}
