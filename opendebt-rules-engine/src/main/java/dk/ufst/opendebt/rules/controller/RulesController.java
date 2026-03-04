package dk.ufst.opendebt.rules.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import dk.ufst.opendebt.rules.model.*;
import dk.ufst.opendebt.rules.service.RulesService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/rules")
@RequiredArgsConstructor
@Tag(name = "Rules Engine", description = "Business rules evaluation for debt collection")
public class RulesController {

  private final RulesService rulesService;

  @PostMapping("/readiness/evaluate")
  @PreAuthorize("hasRole('SERVICE') or hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(
      summary = "Evaluate debt readiness",
      description = "Evaluates if a debt is ready for collection (indrivelsesparat)")
  public ResponseEntity<DebtReadinessResult> evaluateReadiness(
      @Valid @RequestBody DebtReadinessRequest request) {
    return ResponseEntity.ok(rulesService.evaluateReadiness(request));
  }

  @PostMapping("/interest/calculate")
  @PreAuthorize("hasRole('SERVICE') or hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(
      summary = "Calculate interest",
      description = "Calculates interest amount based on debt type and duration")
  public ResponseEntity<InterestCalculationResult> calculateInterest(
      @Valid @RequestBody InterestCalculationRequest request) {
    return ResponseEntity.ok(rulesService.calculateInterest(request));
  }

  @PostMapping("/priority/evaluate")
  @PreAuthorize("hasRole('SERVICE') or hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(
      summary = "Determine collection priority",
      description = "Determines priority ranking for offsetting and garnishment")
  public ResponseEntity<CollectionPriorityResult> evaluatePriority(
      @Valid @RequestBody CollectionPriorityRequest request) {
    return ResponseEntity.ok(rulesService.determineCollectionPriority(request));
  }

  @PostMapping("/priority/sort")
  @PreAuthorize("hasRole('SERVICE') or hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(
      summary = "Sort debts by collection priority",
      description = "Returns debts sorted by collection priority for offsetting")
  public ResponseEntity<List<CollectionPriorityResult>> sortByPriority(
      @Valid @RequestBody List<CollectionPriorityRequest> debts) {
    return ResponseEntity.ok(rulesService.sortByCollectionPriority(debts));
  }
}
