package dk.ufst.opendebt.debtservice.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dk.ufst.opendebt.debtservice.service.ModregningDecisionKind;
import dk.ufst.opendebt.debtservice.service.ModregningReadModelService;
import dk.ufst.opendebt.debtservice.service.ModregningResult;
import dk.ufst.opendebt.debtservice.service.ModregningService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/debtors")
@RequiredArgsConstructor
public class ModregningController {

  private final ModregningService modregningService;
  private final ModregningReadModelService modregningReadModelService;

  /**
   * Applies a tier-2 waiver (GIL § 4, stk. 11). Spec §5.1.
   *
   * <p>SEC-1: caseworkerId extracted from JWT sub claim. SEC-1: SCOPE_modregning:waiver per
   * SecurityConfig pattern.
   */
  @PostMapping("/{debtorId}/modregning-events/{eventId}/tier2-waiver")
  @PreAuthorize("hasAuthority('SCOPE_modregning:waiver')")
  public ResponseEntity<ModregningEventSummary> postTier2Waiver(
      @PathVariable UUID debtorId,
      @PathVariable UUID eventId,
      @RequestBody @Valid WaiverRequest request,
      Authentication authentication) {
    // SEC-1: extract caseworkerId from JWT sub claim
    UUID caseworkerId = UUID.fromString(authentication.getName());
    ModregningResult result =
        modregningService.applyTier2Waiver(debtorId, eventId, request.waiverReason(), caseworkerId);
    return ResponseEntity.ok(toSummary(result));
  }

  /**
   * Returns paginated list of modregning events for a debtor. Spec §5.2.
   *
   * <p>Requires SCOPE_modregning:read.
   */
  @GetMapping("/{debtorId}/modregning-events")
  @PreAuthorize("hasAuthority('SCOPE_modregning:read')")
  public ResponseEntity<List<ModregningEventSummary>> getModregningEvents(
      @PathVariable UUID debtorId) {
    List<ModregningEventSummary> summaries =
        modregningReadModelService.listOperativeEvents(debtorId).stream()
            .map(this::toSummary)
            .toList();
    return ResponseEntity.ok(summaries);
  }

  private ModregningEventSummary toSummary(ModregningResult result) {
    return new ModregningEventSummary(
        result.decisionReference(),
        result.lineageReference(),
        result.decisionKind(),
        result.operative(),
        result.supersedesDecisionReference(),
        result.hasHistory(),
        result.eventId(),
        result.debtorPersonId(),
        result.decisionDate(),
        result.disbursementAmount(),
        result.tier1Amount(),
        result.tier2Amount(),
        result.tier3Amount(),
        result.residualPayoutAmount(),
        result.tier1Amount().add(result.tier2Amount()).add(result.tier3Amount()),
        result.noticeDelivered(),
        result.tier2WaiverApplied(),
        result.klageFristDato());
  }

  // ── DTOs ──────────────────────────────────────────────────────────────────────

  public record WaiverRequest(@NotNull String waiverReason) {}

  public record ModregningEventSummary(
      String decisionReference,
      String lineageReference,
      ModregningDecisionKind decisionKind,
      boolean operative,
      String supersedesDecisionReference,
      boolean hasHistory,
      UUID eventId,
      UUID debtorPersonId,
      LocalDate decisionDate,
      BigDecimal disbursementAmount,
      BigDecimal tier1Amount,
      BigDecimal tier2Amount,
      BigDecimal tier3Amount,
      BigDecimal residualPayoutAmount,
      BigDecimal totalOffsetAmount,
      boolean noticeDelivered,
      boolean tier2WaiverApplied,
      LocalDate klageFristDato) {}
}
