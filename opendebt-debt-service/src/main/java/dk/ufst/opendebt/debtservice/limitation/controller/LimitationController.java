package dk.ufst.opendebt.debtservice.limitation.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dk.ufst.opendebt.debtservice.limitation.dto.CreateFordringskompleksRequest;
import dk.ufst.opendebt.debtservice.limitation.dto.EvaluateObjectionRequest;
import dk.ufst.opendebt.debtservice.limitation.dto.ForaeldelseStatusDto;
import dk.ufst.opendebt.debtservice.limitation.dto.FordringskompleksMemberListDto;
import dk.ufst.opendebt.debtservice.limitation.dto.ObjectionRegistrationResult;
import dk.ufst.opendebt.debtservice.limitation.dto.RegisterAfbrydelseRequest;
import dk.ufst.opendebt.debtservice.limitation.dto.RegisterObjectionRequest;
import dk.ufst.opendebt.debtservice.limitation.dto.RegisterTillaegsfristRequest;
import dk.ufst.opendebt.debtservice.limitation.service.LimitationObjectionFacade;
import dk.ufst.opendebt.debtservice.limitation.service.LimitationStateApplicationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Limitation", description = "Limitation state and objection operations")
public class LimitationController {

  private final LimitationStateApplicationService limitationStateApplicationService;
  private final LimitationObjectionFacade limitationObjectionFacade;

  @GetMapping("/foraeldelse/{fordringId}")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(summary = "Get limitation status")
  public ResponseEntity<ForaeldelseStatusDto> getStatus(@PathVariable UUID fordringId) {
    return ResponseEntity.ok(limitationStateApplicationService.getStatus(fordringId));
  }

  @PostMapping("/foraeldelse/{fordringId}/afbrydelse")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(summary = "Register interruption")
  public ResponseEntity<ForaeldelseStatusDto> registerInterruption(
      @PathVariable UUID fordringId, @RequestBody RegisterAfbrydelseRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(limitationStateApplicationService.registerInterruption(fordringId, request));
  }

  @PostMapping("/foraeldelse/{fordringId}/tillaegsfrist")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(summary = "Register supplementary period")
  public ResponseEntity<ForaeldelseStatusDto> registerSupplementaryPeriod(
      @PathVariable UUID fordringId, @RequestBody RegisterTillaegsfristRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(limitationStateApplicationService.registerSupplementaryPeriod(fordringId, request));
  }

  @PostMapping("/fordringskompleks")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(summary = "Create claim complex")
  public ResponseEntity<FordringskompleksMemberListDto> createClaimComplex(
      @RequestBody CreateFordringskompleksRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(limitationStateApplicationService.createClaimComplex(request));
  }

  @PostMapping("/fordringskompleks/{kompleksId}/members/{fordringId}")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(summary = "Add claim to claim complex")
  public ResponseEntity<FordringskompleksMemberListDto> addMember(
      @PathVariable UUID kompleksId, @PathVariable UUID fordringId) {
    return ResponseEntity.ok(
        limitationStateApplicationService.addMemberToClaimComplex(kompleksId, fordringId));
  }

  @GetMapping("/fordringskompleks/{kompleksId}/members")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(summary = "Get claim complex members")
  public ResponseEntity<FordringskompleksMemberListDto> getMembers(@PathVariable UUID kompleksId) {
    return ResponseEntity.ok(limitationStateApplicationService.getClaimComplexMembers(kompleksId));
  }

  @PostMapping("/foraeldelse/{fordringId}/indsigelse")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(summary = "Register limitation objection")
  public ResponseEntity<ObjectionRegistrationResult> registerObjection(
      @PathVariable UUID fordringId, @RequestBody RegisterObjectionRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(limitationObjectionFacade.registerObjection(fordringId, request));
  }

  @PutMapping("/foraeldelse/{fordringId}/indsigelse/{indsigelsesId}")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(summary = "Evaluate limitation objection")
  public ResponseEntity<ForaeldelseStatusDto> evaluateObjection(
      @PathVariable UUID fordringId,
      @PathVariable UUID indsigelsesId,
      @RequestBody EvaluateObjectionRequest request) {
    return ResponseEntity.ok(
        limitationObjectionFacade.evaluateObjection(fordringId, indsigelsesId, request));
  }
}
