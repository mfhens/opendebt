package dk.ufst.opendebt.debtservice.section50.controller;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dk.ufst.opendebt.debtservice.section50.dto.GenerateSection50WorklistRequest;
import dk.ufst.opendebt.debtservice.section50.dto.Section50ModregningDecisionRequest;
import dk.ufst.opendebt.debtservice.section50.dto.Section50OverrideRequest;
import dk.ufst.opendebt.debtservice.section50.dto.Section50WorklistDto;
import dk.ufst.opendebt.debtservice.section50.service.Section50WorklistApplicationService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/debtors/{debtorId}/retskraft-worklists")
@Tag(name = "Section 50 worklists", description = "Section-50 retskraft evaluation worklists")
@RequiredArgsConstructor
public class Section50WorklistController {

  private final Section50WorklistApplicationService section50WorklistApplicationService;

  @PostMapping
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN') or hasRole('SERVICE')")
  public ResponseEntity<Section50WorklistDto> generateWorklist(
      @PathVariable UUID debtorId, @Valid @RequestBody GenerateSection50WorklistRequest request) {
    return ResponseEntity.ok(
        section50WorklistApplicationService.generateWorklist(debtorId, request));
  }

  @GetMapping("/{worklistId}")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN') or hasRole('SERVICE')")
  public ResponseEntity<Section50WorklistDto> getWorklist(
      @PathVariable UUID debtorId, @PathVariable UUID worklistId) {
    return ResponseEntity.ok(section50WorklistApplicationService.getWorklist(debtorId, worklistId));
  }

  @PostMapping("/{worklistId}/override")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  public ResponseEntity<Section50WorklistDto> applyOverride(
      @PathVariable UUID debtorId,
      @PathVariable UUID worklistId,
      @Valid @RequestBody Section50OverrideRequest request) {
    return ResponseEntity.ok(
        section50WorklistApplicationService.applyOverride(debtorId, worklistId, request));
  }

  @PostMapping("/{worklistId}/modregning-decision")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN') or hasRole('SERVICE')")
  public ResponseEntity<Section50WorklistDto> recordModregningDecision(
      @PathVariable UUID debtorId,
      @PathVariable UUID worklistId,
      @Valid @RequestBody Section50ModregningDecisionRequest request) {
    return ResponseEntity.ok(
        section50WorklistApplicationService.recordModregningDecision(
            debtorId, worklistId, request));
  }
}
