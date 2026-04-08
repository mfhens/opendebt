package dk.ufst.opendebt.debtservice.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dk.ufst.opendebt.debtservice.entity.ModregningEvent;
import dk.ufst.opendebt.debtservice.repository.ModregningEventRepository;
import dk.ufst.opendebt.debtservice.service.ModregningResult;
import dk.ufst.opendebt.debtservice.service.ModregningService;

/** REST controller for modregning (offsetting) operations — petition P058. */
@RestController
@RequestMapping("/api/v1/debtors")
public class ModregningController {

  private final ModregningService modregningService;
  private final ModregningEventRepository modregningEventRepository;

  public ModregningController(
      ModregningService modregningService, ModregningEventRepository modregningEventRepository) {
    this.modregningService = modregningService;
    this.modregningEventRepository = modregningEventRepository;
  }

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
    List<ModregningEvent> events =
        modregningEventRepository.findByDebtorPersonId(debtorId, PageRequest.of(0, 100));
    List<ModregningEventSummary> summaries =
        events.stream()
            .map(
                e ->
                    new ModregningEventSummary(
                        e.getId(),
                        e.getDebtorPersonId(),
                        e.getDecisionDate(),
                        e.getDisbursementAmount(),
                        e.getTier1Amount(),
                        e.getTier2Amount(),
                        e.getTier3Amount(),
                        e.getResidualPayoutAmount(),
                        e.isNoticeDelivered(),
                        e.isTier2WaiverApplied(),
                        e.getKlageFristDato()))
            .toList();
    return ResponseEntity.ok(summaries);
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
        result.residualPayoutAmount(),
        result.noticeDelivered(),
        result.tier2WaiverApplied(),
        result.klageFristDato());
  }

  // ── DTOs ──────────────────────────────────────────────────────────────────────

  public record WaiverRequest(@NotNull String waiverReason) {}

  public record ModregningEventSummary(
      UUID id,
      UUID debtorPersonId,
      LocalDate decisionDate,
      BigDecimal disbursementAmount,
      BigDecimal tier1Amount,
      BigDecimal tier2Amount,
      BigDecimal tier3Amount,
      BigDecimal residualPayoutAmount,
      boolean noticeDelivered,
      boolean tier2WaiverApplied,
      LocalDate klageFristDato) {}
}
