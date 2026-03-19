package dk.ufst.opendebt.debtservice.controller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import dk.ufst.opendebt.debtservice.dto.AddLiabilityRequest;
import dk.ufst.opendebt.debtservice.dto.LiabilityDto;
import dk.ufst.opendebt.debtservice.service.LiabilityService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/debts/{debtId}/liabilities")
@RequiredArgsConstructor
@Tag(name = "Liabilities", description = "Liability (hæftelse) management for debts")
public class LiabilityController {

  private final LiabilityService liabilityService;

  @PostMapping
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(summary = "Add a liability relationship to a debt")
  public ResponseEntity<LiabilityDto> addLiability(
      @PathVariable UUID debtId, @Valid @RequestBody AddLiabilityRequest request) {
    LiabilityDto liability =
        liabilityService.addLiability(
            debtId,
            request.getDebtorPersonId(),
            request.getLiabilityType(),
            request.getSharePercentage());
    return ResponseEntity.status(HttpStatus.CREATED).body(liability);
  }

  @DeleteMapping("/{liabilityId}")
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(summary = "Remove (deactivate) a liability")
  public ResponseEntity<Void> removeLiability(
      @PathVariable UUID debtId, @PathVariable UUID liabilityId) {
    liabilityService.removeLiability(liabilityId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping
  @PreAuthorize("hasRole('CASEWORKER') or hasRole('ADMIN')")
  @Operation(summary = "Get all active liabilities for a debt")
  public ResponseEntity<List<LiabilityDto>> getLiabilities(@PathVariable UUID debtId) {
    return ResponseEntity.ok(liabilityService.getLiabilities(debtId));
  }
}
