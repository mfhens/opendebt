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

import dk.ufst.opendebt.caseservice.entity.CaseState;
import dk.ufst.opendebt.caseservice.service.CaseService;
import dk.ufst.opendebt.common.dto.*;
import dk.ufst.opendebt.common.dto.AssignDebtToCaseRequest;
import dk.ufst.opendebt.common.dto.AssignDebtToCaseResponse;

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
      @Parameter(description = "Filter by case state") @RequestParam(required = false)
          CaseDto.CaseState caseState,
      @Parameter(description = "Filter by primary caseworker") @RequestParam(required = false)
          String caseworkerId,
      Pageable pageable) {
    return ResponseEntity.ok(caseService.listCases(caseState, caseworkerId, pageable));
  }

  @PostMapping("/assign-debt")
  @Operation(
      summary = "Assign debt to case",
      description =
          "Finds or creates a case for the debtor and links the debt to it. "
              + "Used by debt-service after a claim is accepted (UDFOERT).")
  public ResponseEntity<AssignDebtToCaseResponse> assignDebtToCase(
      @Valid @RequestBody AssignDebtToCaseRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(caseService.findOrCreateCaseForDebt(request));
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

  @PostMapping("/{id}/close")
  @PreAuthorize("hasRole('SUPERVISOR') or hasRole('ADMIN')")
  @Operation(summary = "Close a case", description = "Closes a case with specified reason")
  public ResponseEntity<CaseDto> closeCase(
      @PathVariable UUID id, @RequestParam CaseDto.CaseState closureState) {
    return ResponseEntity.ok(caseService.closeCase(id, closureState));
  }

  // ── Party endpoints ──────────────────────────────────────────────────

  @GetMapping("/{id}/parties")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(summary = "Get case parties", description = "Returns all parties for a case")
  public ResponseEntity<List<CasePartyDto>> getParties(@PathVariable UUID id) {
    return ResponseEntity.ok(caseService.getParties(id));
  }

  @PostMapping("/{id}/parties")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(summary = "Add party to case", description = "Adds a party to a case")
  public ResponseEntity<CasePartyDto> addParty(
      @PathVariable UUID id, @Valid @RequestBody CasePartyDto partyDto) {
    return ResponseEntity.status(HttpStatus.CREATED).body(caseService.addParty(id, partyDto));
  }

  @DeleteMapping("/{id}/parties/{partyId}")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(summary = "Remove party from case", description = "Removes a party from a case")
  public ResponseEntity<Void> removeParty(@PathVariable UUID id, @PathVariable UUID partyId) {
    caseService.removeParty(id, partyId);
    return ResponseEntity.noContent().build();
  }

  // ── Debt endpoints ───────────────────────────────────────────────────

  @GetMapping("/{id}/debts")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(summary = "Get case debts", description = "Returns all active debts linked to a case")
  public ResponseEntity<List<CaseDebtDto>> getDebts(@PathVariable UUID id) {
    return ResponseEntity.ok(caseService.getDebts(id));
  }

  // ── Journal endpoints ────────────────────────────────────────────────

  @GetMapping("/{id}/journal")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(
      summary = "Get journal entries",
      description = "Returns all journal entries for a case")
  public ResponseEntity<List<CaseJournalEntryDto>> getJournalEntries(@PathVariable UUID id) {
    return ResponseEntity.ok(caseService.getJournalEntries(id));
  }

  @PostMapping("/{id}/journal")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(summary = "Add journal entry", description = "Adds a journal entry to a case")
  public ResponseEntity<CaseJournalEntryDto> addJournalEntry(
      @PathVariable UUID id, @RequestBody CaseJournalEntryDto entryDto) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(caseService.addJournalEntry(id, entryDto));
  }

  @GetMapping("/{id}/journal/notes")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(summary = "Get journal notes", description = "Returns all journal notes for a case")
  public ResponseEntity<List<CaseJournalNoteDto>> getJournalNotes(@PathVariable UUID id) {
    return ResponseEntity.ok(caseService.getJournalNotes(id));
  }

  @PostMapping("/{id}/journal/notes")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(summary = "Add journal note", description = "Adds a journal note to a case")
  public ResponseEntity<CaseJournalNoteDto> addJournalNote(
      @PathVariable UUID id, @RequestBody CaseJournalNoteDto noteDto) {
    return ResponseEntity.status(HttpStatus.CREATED).body(caseService.addJournalNote(id, noteDto));
  }

  // ── Event endpoints ──────────────────────────────────────────────────

  @GetMapping("/{id}/events")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(summary = "Get case events", description = "Returns the event audit trail for a case")
  public ResponseEntity<List<CaseEventDto>> getEvents(@PathVariable UUID id) {
    return ResponseEntity.ok(caseService.getEvents(id));
  }

  // ── State transition endpoint ────────────────────────────────────────

  @PostMapping("/{id}/state")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(summary = "Transition case state", description = "Transitions a case to a new state")
  public ResponseEntity<CaseDto> transitionState(
      @PathVariable UUID id,
      @RequestParam CaseState targetState,
      @RequestParam(defaultValue = "system") String performedBy) {
    return ResponseEntity.ok(caseService.transitionState(id, targetState, performedBy));
  }

  // ── Collection measure endpoints ─────────────────────────────────────

  @GetMapping("/{id}/measures")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(
      summary = "Get collection measures",
      description = "Returns all collection measures for a case")
  public ResponseEntity<List<CollectionMeasureDto>> getMeasures(@PathVariable UUID id) {
    return ResponseEntity.ok(caseService.getMeasures(id));
  }

  @PostMapping("/{id}/measures")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(
      summary = "Add collection measure",
      description = "Adds a collection measure to a case")
  public ResponseEntity<CollectionMeasureDto> addMeasure(
      @PathVariable UUID id, @RequestBody CollectionMeasureDto measureDto) {
    return ResponseEntity.status(HttpStatus.CREATED).body(caseService.addMeasure(id, measureDto));
  }

  // ── Objection endpoints ──────────────────────────────────────────────

  @GetMapping("/{id}/objections")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(summary = "Get objections", description = "Returns all objections for a case")
  public ResponseEntity<List<ObjectionDto>> getObjections(@PathVariable UUID id) {
    return ResponseEntity.ok(caseService.getObjections(id));
  }

  @PostMapping("/{id}/objections")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(summary = "Add objection", description = "Adds an objection to a case")
  public ResponseEntity<ObjectionDto> addObjection(
      @PathVariable UUID id, @RequestBody ObjectionDto objectionDto) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(caseService.addObjection(id, objectionDto));
  }

  @PutMapping("/{id}/objections/{objectionId}")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(summary = "Resolve objection", description = "Resolves an objection on a case")
  public ResponseEntity<ObjectionDto> resolveObjection(
      @PathVariable UUID id, @PathVariable UUID objectionId, @RequestBody ObjectionDto resolution) {
    return ResponseEntity.ok(caseService.resolveObjection(id, objectionId, resolution));
  }

  // ── Legal basis endpoints ────────────────────────────────────────────

  @GetMapping("/{id}/legal-bases")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(summary = "Get legal bases", description = "Returns all legal bases for a case")
  public ResponseEntity<List<CaseLegalBasisDto>> getLegalBases(@PathVariable UUID id) {
    return ResponseEntity.ok(caseService.getLegalBases(id));
  }

  @PostMapping("/{id}/legal-bases")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(summary = "Add legal basis", description = "Adds a legal basis to a case")
  public ResponseEntity<CaseLegalBasisDto> addLegalBasis(
      @PathVariable UUID id, @RequestBody CaseLegalBasisDto basisDto) {
    return ResponseEntity.status(HttpStatus.CREATED).body(caseService.addLegalBasis(id, basisDto));
  }

  // ── Relation endpoints ───────────────────────────────────────────────

  @GetMapping("/{id}/relations")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(summary = "Get case relations", description = "Returns all relations for a case")
  public ResponseEntity<List<CaseRelationDto>> getRelations(@PathVariable UUID id) {
    return ResponseEntity.ok(caseService.getRelations(id));
  }

  @PostMapping("/{id}/relations")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(summary = "Add case relation", description = "Adds a relation between cases")
  public ResponseEntity<CaseRelationDto> addRelation(
      @PathVariable UUID id, @RequestBody CaseRelationDto relationDto) {
    return ResponseEntity.status(HttpStatus.CREATED).body(caseService.addRelation(id, relationDto));
  }
}
