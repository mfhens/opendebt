package dk.ufst.opendebt.debtservice.controller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import dk.ufst.opendebt.debtservice.dto.CollectionMeasureDto;
import dk.ufst.opendebt.debtservice.dto.InitiateMeasureRequest;
import dk.ufst.opendebt.debtservice.service.CollectionMeasureService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/debts/{debtId}/collection-measures")
@RequiredArgsConstructor
@Tag(
    name = "Collection Measures",
    description = "Collection measure (inddrivelsesskridt) operations")
public class CollectionMeasureController {

  private final CollectionMeasureService measureService;

  @PostMapping
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(summary = "Initiate a collection measure for a debt")
  public ResponseEntity<CollectionMeasureDto> initiateMeasure(
      @PathVariable UUID debtId, @Valid @RequestBody InitiateMeasureRequest request) {
    CollectionMeasureDto measure =
        measureService.initiateMeasure(
            debtId, request.getMeasureType(), request.getAmount(), request.getNote());
    return ResponseEntity.status(HttpStatus.CREATED).body(measure);
  }

  @PostMapping("/{measureId}/complete")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(summary = "Complete a collection measure")
  public ResponseEntity<CollectionMeasureDto> completeMeasure(
      @PathVariable UUID debtId, @PathVariable UUID measureId) {
    return ResponseEntity.ok(measureService.completeMeasure(measureId));
  }

  @PostMapping("/{measureId}/cancel")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(summary = "Cancel a collection measure")
  public ResponseEntity<CollectionMeasureDto> cancelMeasure(
      @PathVariable UUID debtId,
      @PathVariable UUID measureId,
      @RequestBody(required = false) String reason) {
    return ResponseEntity.ok(measureService.cancelMeasure(measureId, reason));
  }

  @GetMapping
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(summary = "Get all collection measures for a debt")
  public ResponseEntity<List<CollectionMeasureDto>> getMeasures(@PathVariable UUID debtId) {
    return ResponseEntity.ok(measureService.getMeasures(debtId));
  }
}
