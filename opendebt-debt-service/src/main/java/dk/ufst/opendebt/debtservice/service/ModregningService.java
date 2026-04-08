package dk.ufst.opendebt.debtservice.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dk.ufst.opendebt.common.audit.cls.ClsAuditClient;
import dk.ufst.opendebt.common.audit.cls.ClsAuditEvent;
import dk.ufst.opendebt.debtservice.client.LedgerServiceClient;
import dk.ufst.opendebt.debtservice.entity.CollectionMeasureEntity;
import dk.ufst.opendebt.debtservice.entity.ModregningEvent;
import dk.ufst.opendebt.debtservice.entity.NotificationOutboxEntity;
import dk.ufst.opendebt.debtservice.exception.ModregningEventNotFoundException;
import dk.ufst.opendebt.debtservice.exception.WaiverAlreadyAppliedException;
import dk.ufst.opendebt.debtservice.offsetting.OffsettingResult;
import dk.ufst.opendebt.debtservice.offsetting.OffsettingService;
import dk.ufst.opendebt.debtservice.repository.CollectionMeasureRepository;
import dk.ufst.opendebt.debtservice.repository.ModregningEventRepository;
import dk.ufst.opendebt.debtservice.repository.NotificationOutboxRepository;

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
  private final LedgerServiceClient ledgerServiceClient;
  private final NotificationOutboxRepository notificationOutboxRepository;
  private final ObjectMapper objectMapper;

  public ModregningService(
      ModregningEventRepository modregningEventRepository,
      CollectionMeasureRepository collectionMeasureRepository,
      ModregningsRaekkefoeigenEngine raekkefoeigenEngine,
      RenteGodtgoerelseService renteGodtgoerelseService,
      ClsAuditClient clsAuditClient,
      LedgerServiceClient ledgerServiceClient,
      NotificationOutboxRepository notificationOutboxRepository,
      ObjectMapper objectMapper) {
    this.modregningEventRepository = modregningEventRepository;
    this.collectionMeasureRepository = collectionMeasureRepository;
    this.raekkefoeigenEngine = raekkefoeigenEngine;
    this.renteGodtgoerelseService = renteGodtgoerelseService;
    this.clsAuditClient = clsAuditClient;
    this.ledgerServiceClient = ledgerServiceClient;
    this.notificationOutboxRepository = notificationOutboxRepository;
    this.objectMapper = objectMapper;
  }

  /** Implements {@link OffsettingService#initiateOffsetting}. */
  @Override
  public OffsettingResult initiateOffsetting(
      UUID debtorPersonId, BigDecimal availableAmount, PaymentType paymentType) {
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
   * ModregningEvent 5. Persist SET_OFF CollectionMeasureEntity per allocation 6. Post double-entry
   * ledger entries per allocation (ADR-0018) 7. Write notification outbox entry (MISSING-1) 8. CLS
   * audit per allocation (NFR-2)
   *
   * @param debtorPersonId the debtor UUID (never CPR — NFR-3/ADR-0014)
   * @param availableAmount gross amount available for offsetting
   * @param paymentType payment type code (ADR-0031)
   * @param sourceEvent optional PublicDisbursementEvent for idempotency; null generates new UUID
   * @param restrictedPayment true if børne-og-ungeydelse restrictions apply
   * @return ModregningResult summary
   */
  public ModregningResult initiateModregning(
      UUID debtorPersonId,
      BigDecimal availableAmount,
      PaymentType paymentType,
      Object sourceEvent,
      boolean restrictedPayment) {

    // Derive idempotency key and event metadata
    String refId;
    LocalDate receiptDate;
    LocalDate decisionDate;
    Integer indkomstAar = null;
    UUID payingAuthorityOrgId = null;

    if (sourceEvent instanceof PublicDisbursementEvent pde) {
      refId = pde.nemkontoReferenceId();
      receiptDate = pde.receiptDate();
      decisionDate = pde.decisionDate();
      indkomstAar = pde.indkomstAar();
      payingAuthorityOrgId = pde.payingAuthorityOrgId(); // BUG-F: extract for tier-1 filtering
    } else {
      refId = (sourceEvent instanceof String s) ? s : UUID.randomUUID().toString();
      receiptDate = LocalDate.now();
      decisionDate = LocalDate.now();
    }

    // Idempotency guard
    Optional<ModregningEvent> existing = modregningEventRepository.findByNemkontoReferenceId(refId);
    if (existing.isPresent()) {
      return toResult(existing.get(), List.of());
    }

    // RenteGodtgørelse decision
    RenteGodtgoerelseDecision rg =
        renteGodtgoerelseService.computeDecision(
            receiptDate, decisionDate, paymentType, indkomstAar);

    // 3-tier allocation — BUG-F: pass payingAuthorityOrgId for tier-1 creditor filtering (GIL § 7,
    // stk. 1, nr. 1)
    TierAllocationResult allocation =
        raekkefoeigenEngine.allocate(debtorPersonId, availableAmount, false, payingAuthorityOrgId);

    // Build and persist ModregningEvent (renteGodtgoerelseNonTaxable ALWAYS true per GIL § 8b)
    ModregningEvent event =
        ModregningEvent.builder()
            .nemkontoReferenceId(refId)
            .debtorPersonId(debtorPersonId)
            .receiptDate(receiptDate)
            .decisionDate(decisionDate)
            .paymentType(paymentType)
            .disbursementAmount(availableAmount)
            .tier1Amount(sumTier(allocation.tier1Allocations()))
            .tier2Amount(sumTier(allocation.tier2Allocations()))
            .tier3Amount(sumTier(allocation.tier3Allocations()))
            .residualPayoutAmount(allocation.residualPayoutAmount())
            .klageFristDato(decisionDate.plusYears(1))
            .renteGodtgoerelseStartDate(rg.startDate())
            .renteGodtgoerelseNonTaxable(true)
            .build();
    try {
      event = modregningEventRepository.save(event);
    } catch (DataIntegrityViolationException ex) {
      // BUG-4: race condition on nemkonto_reference_id unique constraint — return existing
      return toResult(
          modregningEventRepository.findByNemkontoReferenceId(refId).orElseThrow(() -> ex),
          List.of());
    }

    // Persist SET_OFF measures, post ledger entries, build coverages
    List<FordringCoverageDto> coverages = new ArrayList<>();
    for (FordringAllocation alloc : getAllAllocations(allocation)) {
      CollectionMeasureEntity measure =
          CollectionMeasureEntity.builder()
              .debtId(alloc.fordringId())
              .measureType(CollectionMeasureEntity.MeasureType.SET_OFF)
              .modregningEventId(event.getId())
              .amount(alloc.amountCovered())
              .tierLevel(alloc.tier())
              .build();
      collectionMeasureRepository.save(measure);

      // ADR-0018: double-entry ledger entry per allocation
      ledgerServiceClient.postLedgerEntry(
          debtorPersonId, alloc.fordringId(), alloc.amountCovered(), event.getId());

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

    // MISSING-1: Write notification outbox entry within @Transactional boundary
    writeNotificationOutbox(event, coverages);

    return toResult(event, coverages);
  }

  /**
   * Applies a tier-2 waiver per GIL § 4, stk. 11.
   *
   * <p>Steps: 1. Load ModregningEvent 2. Guard against double-waiver 3. Set
   * tier2WaiverApplied=true, tier2Amount=0 4. Mark existing tier-2 CollectionMeasure rows as
   * waiver_applied (MISSING-2) 5. Re-run engine with skipTier2=true to compute new tier-3 6. Update
   * tier1Amount, tier3Amount, residualPayoutAmount (BUG-2) 7. Create new tier-3
   * CollectionMeasureEntity rows (MISSING-2) 8. Reverse ledger entries for original tier-2 measures
   * (ADR-0018) 9. Persist updated event 10. CLS audit
   */
  public ModregningResult applyTier2Waiver(
      UUID debtorPersonId, UUID modregningEventId, String waiverReason, UUID caseworkerId) {

    ModregningEvent event =
        modregningEventRepository
            .findById(modregningEventId)
            .orElseThrow(() -> new ModregningEventNotFoundException(modregningEventId));

    if (event.isTier2WaiverApplied()) {
      throw new WaiverAlreadyAppliedException(modregningEventId);
    }

    // BUG-1 fix: use event's own debtorPersonId, not the (possibly null) parameter
    UUID resolvedDebtorPersonId = event.getDebtorPersonId();

    event.setTier2WaiverApplied(true);
    event.setTier2Amount(BigDecimal.ZERO);

    // MISSING-2: Mark existing tier-2 CollectionMeasure rows as waiver_applied
    List<CollectionMeasureEntity> tier2Measures =
        collectionMeasureRepository.findByModregningEventIdAndMeasureTypeAndTierLevel(
            modregningEventId, CollectionMeasureEntity.MeasureType.SET_OFF, 2);
    for (CollectionMeasureEntity cm : tier2Measures) {
      cm.setWaiverApplied(true);
      cm.setCaseworkerId(caseworkerId);
      collectionMeasureRepository.save(cm);

      // ADR-0018: reverse ledger entries for original tier-2 measures
      ledgerServiceClient.reverseLedgerEntry(
          resolvedDebtorPersonId, cm.getDebtId(), cm.getAmount(), modregningEventId);
    }

    // BUG-B fix: re-run uses only the amount available after tier-1, not the full
    // disbursementAmount.
    // tier1Amount is already settled and must be preserved — only tier-3 and residual change.
    BigDecimal waiverAvailableAmount =
        event.getDisbursementAmount().subtract(event.getTier1Amount());
    TierAllocationResult newAllocation =
        raekkefoeigenEngine.allocate(resolvedDebtorPersonId, waiverAvailableAmount, true, null);

    // BUG-B fix: do NOT call event.setTier1Amount(...) — tier1Amount is preserved unchanged.
    // Only tier3Amount and residualPayoutAmount are updated by the waiver re-run.
    event.setTier3Amount(sumTier(newAllocation.tier3Allocations()));
    event.setResidualPayoutAmount(newAllocation.residualPayoutAmount());

    // MISSING-2: Create new tier-3 CollectionMeasureEntity rows from the re-run
    for (FordringAllocation alloc : newAllocation.tier3Allocations()) {
      CollectionMeasureEntity newMeasure =
          CollectionMeasureEntity.builder()
              .debtId(alloc.fordringId())
              .measureType(CollectionMeasureEntity.MeasureType.SET_OFF)
              .modregningEventId(modregningEventId)
              .amount(alloc.amountCovered())
              .caseworkerId(caseworkerId)
              .tierLevel(3)
              .build();
      collectionMeasureRepository.save(newMeasure);
    }

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
                Map.of(
                    "gilParagraf",
                    "GIL § 4, stk. 11",
                    "caseworkerId",
                    caseworkerId.toString(),
                    "waiverReason",
                    waiverReason))
            .build());

    return toResult(event, List.of());
  }

  /**
   * Handles Digital Post notice delivery callback — BUG-5/SPEC-058 §4.5.
   *
   * <p>When notice is delivered, recomputes klageFristDato = deliveryDate + 3 months.
   *
   * @param eventId the modregning event UUID
   * @param success true if notice was delivered successfully
   * @param deliveryDate the date notice was delivered (only used when success=true)
   */
  public void handleNoticeDelivery(UUID eventId, boolean success, LocalDate deliveryDate) {
    ModregningEvent event =
        modregningEventRepository
            .findById(eventId)
            .orElseThrow(() -> new ModregningEventNotFoundException(eventId));
    if (success && deliveryDate != null) {
      event.setNoticeDelivered(true);
      event.setNoticeDeliveryDate(deliveryDate);
      event.setKlageFristDato(deliveryDate.plusMonths(3));
    }
    modregningEventRepository.save(event);
  }

  // ── helpers ──────────────────────────────────────────────────────────────────

  private void writeNotificationOutbox(ModregningEvent event, List<FordringCoverageDto> coverages) {
    String payload;
    try {
      payload =
          objectMapper.writeValueAsString(
              Map.of(
                  "debtorPersonId",
                  event.getDebtorPersonId().toString(),
                  "eventId",
                  event.getId().toString(),
                  "decisionDate",
                  event.getDecisionDate().toString(),
                  "tierBreakdown",
                  Map.of(
                      "tier1Amount", event.getTier1Amount(),
                      "tier2Amount", event.getTier2Amount(),
                      "tier3Amount", event.getTier3Amount())));
    } catch (JsonProcessingException e) {
      payload = "{\"eventId\":\"" + event.getId() + "\"}";
    }
    notificationOutboxRepository.save(
        NotificationOutboxEntity.builder()
            .modregningEventId(event.getId())
            .debtorPersonId(event.getDebtorPersonId())
            .payload(payload)
            .build());
  }

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
