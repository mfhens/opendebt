package dk.ufst.opendebt.debtservice.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dk.ufst.opendebt.debtservice.service.ModregningResult;
import dk.ufst.opendebt.debtservice.service.ModregningService;

/** REST controller for modregning (offsetting) operations — petition P058. */
@RestController
@RequestMapping("/api/v1/modregning")
public class ModregningController {

  private final ModregningService modregningService;

  public ModregningController(ModregningService modregningService) {
    this.modregningService = modregningService;
  }

  /** Applies a tier-2 waiver (GIL § 4, stk. 11). Requires modregning:waiver scope (AC-7). */
  @PostMapping("/{modregningEventId}/waiver")
  @PreAuthorize("hasAuthority('modregning:waiver')")
  public ResponseEntity<ModregningEventSummary> postTier2Waiver(
      @PathVariable UUID modregningEventId, @RequestBody WaiverRequest request) {
    // In a real implementation, debtorPersonId and caseworkerId would come from security context
    UUID caseworkerId = UUID.randomUUID(); // Placeholder — real impl reads from JWT
    ModregningResult result =
        modregningService.applyTier2Waiver(
            null, modregningEventId, request.waiverReason(), caseworkerId);
    return ResponseEntity.ok(toSummary(result));
  }

  /** Initiates a modregning cycle for a debtor. */
  @PostMapping("/{modregningEventId}/initiate")
  @PreAuthorize("hasAuthority('modregning:write')")
  public ResponseEntity<ModregningEventSummary> initiateModregning(
      @PathVariable UUID modregningEventId, @RequestBody InitiateRequest request) {
    ModregningResult result =
        modregningService.initiateModregning(
            request.debtorPersonId(),
            request.availableAmount(),
            request.paymentType(),
            request.nemkontoReferenceId(),
            false);
    return ResponseEntity.ok(toSummary(result));
  }

  private ModregningEventSummary toSummary(ModregningResult result) {
    return new ModregningEventSummary(
        result.eventId(),
        result.debtorPersonId(),
        result.decisionDate(),
        result.disbursementAmount(),
        result.tier1Amount(),
        result.tier2Amount(),
        result.tier3Amount(),
        result.tier2WaiverApplied(),
        result.klageFristDato());
  }

  // ── DTOs ──────────────────────────────────────────────────────────────────────

  public record WaiverRequest(String waiverReason) {}

  public record InitiateRequest(
      UUID debtorPersonId,
      BigDecimal availableAmount,
      String paymentType,
      String nemkontoReferenceId) {}

  public record ModregningEventSummary(
      UUID id,
      UUID debtorPersonId,
      LocalDate decisionDate,
      BigDecimal disbursementAmount,
      BigDecimal tier1Amount,
      BigDecimal tier2Amount,
      BigDecimal tier3Amount,
      boolean tier2WaiverApplied,
      LocalDate klageFristDato) {}
}
