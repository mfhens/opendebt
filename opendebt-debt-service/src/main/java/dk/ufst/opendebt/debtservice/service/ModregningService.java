package dk.ufst.opendebt.debtservice.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.ufst.opendebt.common.audit.cls.ClsAuditClient;
import dk.ufst.opendebt.common.audit.cls.ClsAuditEvent;
import dk.ufst.opendebt.debtservice.entity.CollectionMeasureEntity;
import dk.ufst.opendebt.debtservice.entity.ModregningEvent;
import dk.ufst.opendebt.debtservice.exception.ModregningEventNotFoundException;
import dk.ufst.opendebt.debtservice.exception.WaiverAlreadyAppliedException;
import dk.ufst.opendebt.debtservice.offsetting.OffsettingResult;
import dk.ufst.opendebt.debtservice.offsetting.OffsettingService;
import dk.ufst.opendebt.debtservice.repository.CollectionMeasureRepository;
import dk.ufst.opendebt.debtservice.repository.ModregningEventRepository;

/**
 * Primary implementation of {@link OffsettingService} for petition P058.
 *
 * <p>Implements the FR-1/FR-2/FR-5 modregning workflow per GIL § 7.
 */
@Service
@Transactional
public class ModregningService implements OffsettingService {

  private final ModregningEventRepository modregningEventRepository;
  private final CollectionMeasureRepository collectionMeasureRepository;
  private final ModregningsRaekkefoeigenEngine raekkefoeigenEngine;
  private final RenteGodtgoerelseService renteGodtgoerelseService;
  private final ClsAuditClient clsAuditClient;

  public ModregningService(
      ModregningEventRepository modregningEventRepository,
      CollectionMeasureRepository collectionMeasureRepository,
      ModregningsRaekkefoeigenEngine raekkefoeigenEngine,
      RenteGodtgoerelseService renteGodtgoerelseService,
      ClsAuditClient clsAuditClient) {
    this.modregningEventRepository = modregningEventRepository;
    this.collectionMeasureRepository = collectionMeasureRepository;
    this.raekkefoeigenEngine = raekkefoeigenEngine;
    this.renteGodtgoerelseService = renteGodtgoerelseService;
    this.clsAuditClient = clsAuditClient;
  }

  /** Implements {@link OffsettingService#initiateOffsetting}. */
  @Override
  public OffsettingResult initiateOffsetting(
      UUID debtorPersonId, BigDecimal availableAmount, String paymentType) {
    ModregningResult result =
        initiateModregning(debtorPersonId, availableAmount, paymentType, null, false);
    List<OffsettingResult.OffsetAllocation> applied =
        result.coverages().stream()
            .map(
                c -> new OffsettingResult.OffsetAllocation(c.fordringId(), c.amountCovered(), null))
            .toList();
    return new OffsettingResult(
        debtorPersonId,
        result.tier1Amount().add(result.tier2Amount()).add(result.tier3Amount()),
        result.residualPayoutAmount(),
        applied);
  }

  /**
   * Initiates a modregning (offsetting) cycle.
   *
   * <p>Implements the full FR-1 workflow: 1. Idempotency check via nemkontoReferenceId 2.
   * RenteGodtgørelse decision 3. 3-tier allocation via ModregningsRaekkefoeigenEngine 4. Persist
   * ModregningEvent 5. Persist SET_OFF CollectionMeasureEntity per allocation 6. CLS audit per
   * allocation (NFR-2)
   *
   * @param debtorPersonId the debtor UUID (never CPR — NFR-3/ADR-0014)
   * @param availableAmount gross amount available for offsetting
   * @param paymentType payment type code
   * @param sourceEvent optional nemkontoReferenceId (String) for idempotency; null generates new
   *     UUID
   * @param restrictedPayment true if børne-og-ungeydelse restrictions apply
   * @return ModregningResult summary
   */
  @Transactional
  public ModregningResult initiateModregning(
      UUID debtorPersonId,
      BigDecimal availableAmount,
      String paymentType,
      Object sourceEvent,
      boolean restrictedPayment) {

    // Derive idempotency key
    String refId = (sourceEvent instanceof String s) ? s : UUID.randomUUID().toString();

    // Idempotency guard
    Optional<ModregningEvent> existing = modregningEventRepository.findByNemkontoReferenceId(refId);
    if (existing.isPresent()) {
      return toResult(existing.get(), List.of());
    }

    // RenteGodtgørelse decision
    LocalDate today = LocalDate.now();
    RenteGodtgoerelseDecision rg =
        renteGodtgoerelseService.computeDecision(today, today, paymentType, null);

    // 3-tier allocation
    TierAllocationResult allocation =
        raekkefoeigenEngine.allocate(debtorPersonId, availableAmount, false, null);

    // Build and persist ModregningEvent (renteGodtgoerelseNonTaxable ALWAYS true per GIL § 8b)
    ModregningEvent event =
        ModregningEvent.builder()
            .nemkontoReferenceId(refId)
            .debtorPersonId(debtorPersonId)
            .receiptDate(today)
            .decisionDate(today)
            .paymentType(paymentType)
            .disbursementAmount(availableAmount)
            .tier1Amount(sumTier(allocation.tier1Allocations()))
            .tier2Amount(sumTier(allocation.tier2Allocations()))
            .tier3Amount(sumTier(allocation.tier3Allocations()))
            .residualPayoutAmount(allocation.residualPayoutAmount())
            .klageFristDato(today.plusYears(1))
            .renteGodtgoerelseStartDate(rg.startDate())
            .renteGodtgoerelseNonTaxable(true)
            .build();
    event = modregningEventRepository.save(event);

    // Persist SET_OFF measures and build coverages
    List<FordringCoverageDto> coverages = new ArrayList<>();
    for (FordringAllocation alloc : getAllAllocations(allocation)) {
      CollectionMeasureEntity measure =
          CollectionMeasureEntity.builder()
              .debtId(alloc.fordringId())
              .measureType(CollectionMeasureEntity.MeasureType.SET_OFF)
              .modregningEventId(event.getId())
              .amount(alloc.amountCovered())
              .build();
      collectionMeasureRepository.save(measure);
      coverages.add(
          new FordringCoverageDto(alloc.fordringId(), alloc.amountCovered(), alloc.tier()));

      // CLS audit per allocation (NFR-2) — debtorPersonId is UUID, never CPR (NFR-3)
      clsAuditClient.shipEvent(
          ClsAuditEvent.builder()
              .eventId(UUID.randomUUID())
              .timestamp(Instant.now())
              .serviceName("debt-service")
              .operation("INSERT")
              .resourceType("modregning_event")
              .resourceId(event.getId())
              .newValues(
                  Map.of(
                      "gilParagraf", "GIL § 7, stk. 1, nr. " + alloc.tier(),
                      "modregningEventId", event.getId().toString(),
                      "debtorPersonId", debtorPersonId.toString(),
                      "fordringId", alloc.fordringId().toString()))
              .build());
    }

    return toResult(event, coverages);
  }

  /**
   * Applies a tier-2 waiver per GIL § 4, stk. 11.
   *
   * <p>Steps: 1. Load ModregningEvent 2. Guard against double-waiver 3. Set
   * tier2WaiverApplied=true, tier2Amount=0 4. Re-run engine with skipTier2=true to compute new
   * tier-3 5. Persist updated event 6. CLS audit
   */
  @Transactional
  public ModregningResult applyTier2Waiver(
      UUID debtorPersonId, UUID modregningEventId, String waiverReason, UUID caseworkerId) {

    ModregningEvent event =
        modregningEventRepository
            .findById(modregningEventId)
            .orElseThrow(() -> new ModregningEventNotFoundException(modregningEventId));

    if (event.isTier2WaiverApplied()) {
      throw new WaiverAlreadyAppliedException(modregningEventId);
    }

    event.setTier2WaiverApplied(true);
    event.setTier2Amount(BigDecimal.ZERO);

    TierAllocationResult newAllocation =
        raekkefoeigenEngine.allocate(debtorPersonId, event.getDisbursementAmount(), true, null);
    event.setTier3Amount(sumTier(newAllocation.tier3Allocations()));

    modregningEventRepository.save(event);

    clsAuditClient.shipEvent(
        ClsAuditEvent.builder()
            .eventId(UUID.randomUUID())
            .timestamp(Instant.now())
            .serviceName("debt-service")
            .operation("UPDATE")
            .resourceType("modregning_event")
            .resourceId(modregningEventId)
            .newValues(
                Map.of("gilParagraf", "GIL § 4, stk. 11", "caseworkerId", caseworkerId.toString()))
            .build());

    return toResult(event, List.of());
  }

  // ── helpers ──────────────────────────────────────────────────────────────────

  private BigDecimal sumTier(List<FordringAllocation> allocations) {
    return allocations.stream()
        .map(FordringAllocation::amountCovered)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private List<FordringAllocation> getAllAllocations(TierAllocationResult allocation) {
    List<FordringAllocation> all = new ArrayList<>();
    all.addAll(allocation.tier1Allocations());
    all.addAll(allocation.tier2Allocations());
    all.addAll(allocation.tier3Allocations());
    return all;
  }

  private ModregningResult toResult(ModregningEvent event, List<FordringCoverageDto> coverages) {
    return new ModregningResult(
        event.getId(),
        event.getDebtorPersonId(),
        event.getDecisionDate(),
        event.getDisbursementAmount(),
        event.getTier1Amount(),
        event.getTier2Amount(),
        event.getTier3Amount(),
        event.getResidualPayoutAmount(),
        event.isTier2WaiverApplied(),
        event.isNoticeDelivered(),
        event.getNoticeDeliveryDate(),
        event.getKlageFristDato(),
        event.getRenteGodtgoerelseStartDate(),
        event.isRenteGodtgoerelseNonTaxable(),
        coverages);
  }
}
