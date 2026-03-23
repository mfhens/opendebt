package dk.ufst.opendebt.payment.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import dk.ufst.opendebt.common.dto.DebtEventDto;
import dk.ufst.opendebt.payment.bookkeeping.entity.LedgerEntryEntity;
import dk.ufst.opendebt.payment.dto.LedgerEntryDto;
import dk.ufst.opendebt.payment.dto.LedgerSummaryDto;
import dk.ufst.opendebt.payment.service.LedgerQueryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** REST controller for querying ledger entries, debt events, and balance summaries. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Ledger", description = "Ledger query endpoints for caseworker portal")
@RequiredArgsConstructor
public class LedgerController {

  private final LedgerQueryService ledgerQueryService;

  @GetMapping("/ledger/debt/{debtId}")
  @Operation(
      summary = "Get ledger entries for a debt",
      description =
          "Returns paginated ledger entries for the specified debt with optional filtering"
              + " by date range, entry category, and storno inclusion.")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN') or hasRole('SERVICE')")
  public ResponseEntity<Page<LedgerEntryDto>> getLedgerByDebt(
      @PathVariable UUID debtId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate fromDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate toDate,
      @RequestParam(required = false) LedgerEntryEntity.EntryCategory category,
      @RequestParam(defaultValue = "true") boolean includeStorno,
      Pageable pageable) {
    Page<LedgerEntryDto> result =
        ledgerQueryService.getLedgerEntriesByDebtId(
            debtId, fromDate, toDate, category, includeStorno, pageable);
    return ResponseEntity.ok(result);
  }

  @GetMapping("/ledger/case/{caseId}")
  @Operation(
      summary = "Get ledger entries for a case",
      description =
          "Returns paginated ledger entries for all debts in the specified case with optional"
              + " filtering by date range, entry category, and storno inclusion.")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  public ResponseEntity<Page<LedgerEntryDto>> getLedgerByCase(
      @PathVariable UUID caseId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate fromDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate toDate,
      @RequestParam(required = false) LedgerEntryEntity.EntryCategory category,
      @RequestParam(defaultValue = "true") boolean includeStorno,
      Pageable pageable) {
    Page<LedgerEntryDto> result =
        ledgerQueryService.getLedgerEntriesByCaseId(
            caseId, fromDate, toDate, category, includeStorno, pageable);
    return ResponseEntity.ok(result);
  }

  @GetMapping("/events/debt/{debtId}")
  @Operation(
      summary = "Get debt events for a debt",
      description = "Returns all debt events for the specified debt, ordered by effective date.")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN') or hasRole('SERVICE')")
  public ResponseEntity<List<DebtEventDto>> getEventsByDebt(@PathVariable UUID debtId) {
    List<DebtEventDto> events = ledgerQueryService.getEventsByDebtId(debtId);
    return ResponseEntity.ok(events);
  }

  @GetMapping("/events/case/{caseId}")
  @Operation(
      summary = "Get debt events for a case",
      description =
          "Returns all debt events for all debts in the specified case, ordered by effective date.")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN') or hasRole('SERVICE')")
  public ResponseEntity<List<DebtEventDto>> getEventsByCase(@PathVariable UUID caseId) {
    List<DebtEventDto> events = ledgerQueryService.getEventsByCaseId(caseId);
    return ResponseEntity.ok(events);
  }

  @GetMapping("/ledger/debt/{debtId}/summary")
  @Operation(
      summary = "Get ledger summary for a debt",
      description =
          "Computes and returns a balance summary for the specified debt including principal"
              + " balance, interest balance, total payments, and other aggregates.")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN') or hasRole('SERVICE')")
  public ResponseEntity<LedgerSummaryDto> getLedgerSummary(@PathVariable UUID debtId) {
    LedgerSummaryDto summary = ledgerQueryService.getLedgerSummary(debtId);
    return ResponseEntity.ok(summary);
  }
}
