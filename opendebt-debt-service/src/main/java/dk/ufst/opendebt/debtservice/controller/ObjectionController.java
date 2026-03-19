package dk.ufst.opendebt.debtservice.controller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import dk.ufst.opendebt.debtservice.dto.ObjectionDto;
import dk.ufst.opendebt.debtservice.dto.RegisterObjectionRequest;
import dk.ufst.opendebt.debtservice.dto.ResolveObjectionRequest;
import dk.ufst.opendebt.debtservice.service.ObjectionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/debts/{debtId}/objections")
@RequiredArgsConstructor
@Tag(name = "Objections", description = "Objection (indsigelse) management for debts")
public class ObjectionController {

  private final ObjectionService objectionService;

  @PostMapping
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(summary = "Register an objection against a debt")
  public ResponseEntity<ObjectionDto> registerObjection(
      @PathVariable UUID debtId, @Valid @RequestBody RegisterObjectionRequest request) {
    ObjectionDto objection =
        objectionService.registerObjection(
            debtId, request.getDebtorPersonId(), request.getReason());
    return ResponseEntity.status(HttpStatus.CREATED).body(objection);
  }

  @PutMapping("/{objectionId}/resolve")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(summary = "Resolve an objection (upheld or rejected)")
  public ResponseEntity<ObjectionDto> resolveObjection(
      @PathVariable UUID debtId,
      @PathVariable UUID objectionId,
      @Valid @RequestBody ResolveObjectionRequest request) {
    ObjectionDto objection =
        objectionService.resolveObjection(objectionId, request.getOutcome(), request.getNote());
    return ResponseEntity.ok(objection);
  }

  @GetMapping
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(summary = "Get all objections for a debt")
  public ResponseEntity<List<ObjectionDto>> getObjections(@PathVariable UUID debtId) {
    return ResponseEntity.ok(objectionService.getObjections(debtId));
  }
}
