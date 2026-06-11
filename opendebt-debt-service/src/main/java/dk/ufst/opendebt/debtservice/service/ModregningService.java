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

import dk.ufst.opendebt.common.audit.cls.ClsAuditClient;
import dk.ufst.opendebt.common.audit.cls.ClsAuditEvent;
import dk.ufst.opendebt.debtservice.client.LedgerServiceClient;
import dk.ufst.opendebt.debtservice.entity.CollectionMeasureEntity;
import dk.ufst.opendebt.debtservice.entity.KorrektionspuljeEntry;
import dk.ufst.opendebt.debtservice.entity.ModregningEvent;
import dk.ufst.opendebt.debtservice.exception.ModregningEventNotFoundException;
import dk.ufst.opendebt.debtservice.exception.WaiverAlreadyAppliedException;
import dk.ufst.opendebt.debtservice.offsetting.OffsettingResult;
import dk.ufst.opendebt.debtservice.offsetting.OffsettingService;
import dk.ufst.opendebt.debtservice.repository.CollectionMeasureRepository;
import dk.ufst.opendebt.debtservice.repository.ModregningEventRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ModregningService implements OffsettingService {

  private final ModregningEventRepository modregningEventRepository;
  private final CollectionMeasureRepository collectionMeasureRepository;
  private final ModregningsRaekkefoeigenEngine raekkefoeigenEngine;
  private final RenteGodtgoerelseService renteGodtgoerelseService;
  private final ClsAuditClient clsAuditClient;
  private final LedgerServiceClient ledgerServiceClient;
  private final FordringQueryPort fordringQueryPort;
  private final ModregningResultMapper modregningResultMapper;
  private final ModregningNotificationOutboxWriter notificationOutboxWriter;

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
   * <p>Implements the full FR-1 workflow.
   */
  public ModregningResult initiateModregning(
      UUID debtorPersonId,
      BigDecimal availableAmount,
      PaymentType paymentType,
      Object sourceEvent,
      boolean restrictedPayment) {

    InitialDecisionContext context = resolveInitialDecisionContext(sourceEvent);

    Optional<ModregningEvent> existing =
        modregningEventRepository.findByNemkontoReferenceId(context.nemkontoReferenceId());
    if (existing.isPresent()) {
      return modregningResultMapper.toResult(existing.get(), List.of());
    }

    RenteGodtgoerelseDecision rg =
        renteGodtgoerelseService.computeDecision(
            context.receiptDate(), context.decisionDate(), paymentType, context.indkomstAar());

    TierAllocationResult allocation =
        raekkefoeigenEngine.allocate(
            debtorPersonId, availableAmount, false, context.payingAuthorityOrgId());

    ModregningEvent event =
        ModregningEvent.builder()
            .nemkontoReferenceId(context.nemkontoReferenceId())
            .decisionReference(
                ModregningReferenceFactory.decisionReference(context.nemkontoReferenceId()))
            .lineageReference(
                ModregningReferenceFactory.lineageReference(context.nemkontoReferenceId()))
            .decisionKind(ModregningDecisionKind.EXTERNAL_DISBURSEMENT_DECISION)
            .operative(true)
            .debtorPersonId(debtorPersonId)
            .receiptDate(context.receiptDate())
            .decisionDate(context.decisionDate())
            .paymentType(paymentType)
            .indkomstAar(context.indkomstAar())
            .disbursementAmount(availableAmount)
            .tier1Amount(sumTier(allocation.tier1Allocations()))
            .tier2Amount(sumTier(allocation.tier2Allocations()))
            .tier3Amount(sumTier(allocation.tier3Allocations()))
            .residualPayoutAmount(allocation.residualPayoutAmount())
            .klageFristDato(context.decisionDate().plusYears(1))
            .renteGodtgoerelseStartDate(rg.startDate())
            .renteGodtgoerelseNonTaxable(true)
            .build();
    try {
      event = modregningEventRepository.save(event);
    } catch (DataIntegrityViolationException ex) {
      return modregningResultMapper.toResult(
          modregningEventRepository
              .findByNemkontoReferenceId(context.nemkontoReferenceId())
              .orElseThrow(() -> ex),
          List.of());
    }

    List<FordringCoverageDto> coverages =
        persistAllocationsAndLedgers(event, allocation, debtorPersonId, false, null, true);
    notificationOutboxWriter.write(event, coverages);
    return modregningResultMapper.toResult(event, coverages);
  }

  /** Applies a tier-2 waiver per GIL § 4, stk. 11 by creating a superseding lineage decision. */
  public ModregningResult applyTier2Waiver(
      UUID debtorPersonId, UUID modregningEventId, String waiverReason, UUID caseworkerId) {

    ModregningEvent predecessor =
        modregningEventRepository
            .findByIdAndDebtorPersonId(modregningEventId, debtorPersonId)
            .orElseThrow(() -> new ModregningEventNotFoundException(modregningEventId));

    if (!predecessor.isOperative()
        || predecessor.isTier2WaiverApplied()
        || modregningEventRepository.existsBySupersedesEventId(predecessor.getId())) {
      throw new WaiverAlreadyAppliedException(modregningEventId);
    }

    predecessor.setOperative(false);
    modregningEventRepository.save(predecessor);

    List<CollectionMeasureEntity> tier2Measures =
        collectionMeasureRepository.findByModregningEventIdAndMeasureTypeAndTierLevel(
            modregningEventId, CollectionMeasureEntity.MeasureType.SET_OFF, 2);
    for (CollectionMeasureEntity cm : tier2Measures) {
      cm.setWaiverApplied(true);
      cm.setCaseworkerId(caseworkerId);
      collectionMeasureRepository.save(cm);
      ledgerServiceClient.reverseLedgerEntry(
          debtorPersonId, cm.getDebtId(), cm.getAmount(), modregningEventId);
    }

    BigDecimal waiverAvailableAmount =
        predecessor.getDisbursementAmount().subtract(predecessor.getTier1Amount());
    TierAllocationResult newAllocation =
        raekkefoeigenEngine.allocate(debtorPersonId, waiverAvailableAmount, true, true, null);

    LocalDate waiverDecisionDate = LocalDate.now();
    ModregningEvent successor =
        ModregningEvent.builder()
            .nemkontoReferenceId(predecessor.getNemkontoReferenceId() + "-WAIVER")
            .decisionReference(predecessor.getDecisionReference() + "-WAIVER")
            .lineageReference(predecessor.getLineageReference())
            .decisionKind(ModregningDecisionKind.SUPERSEDING_WAIVER_DECISION)
            .supersedesEventId(predecessor.getId())
            .operative(true)
            .debtorPersonId(debtorPersonId)
            .receiptDate(predecessor.getReceiptDate())
            .decisionDate(waiverDecisionDate)
            .paymentType(predecessor.getPaymentType())
            .indkomstAar(predecessor.getIndkomstAar())
            .disbursementAmount(predecessor.getDisbursementAmount())
            .tier1Amount(predecessor.getTier1Amount())
            .tier2Amount(BigDecimal.ZERO)
            .tier3Amount(sumTier(newAllocation.tier3Allocations()))
            .residualPayoutAmount(newAllocation.residualPayoutAmount())
            .tier2WaiverApplied(true)
            .klageFristDato(waiverDecisionDate.plusYears(1))
            .renteGodtgoerelseStartDate(predecessor.getRenteGodtgoerelseStartDate())
            .renteGodtgoerelseNonTaxable(predecessor.isRenteGodtgoerelseNonTaxable())
            .build();
    successor = modregningEventRepository.save(successor);
    List<FordringCoverageDto> coverages =
        persistAllocationsAndLedgers(
            successor, newAllocation, debtorPersonId, true, caseworkerId, false);
    notificationOutboxWriter.write(successor, coverages);

    clsAuditClient.shipEvent(
        ClsAuditEvent.builder()
            .eventId(UUID.randomUUID())
            .timestamp(Instant.now())
            .serviceName("debt-service")
            .operation("UPDATE")
            .resourceType("modregning_event")
            .resourceId(successor.getId())
            .newValues(
                Map.of(
                    "gilParagraf", "GIL § 4, stk. 11",
                    "caseworkerId", caseworkerId.toString(),
                    "waiverReason", waiverReason,
                    "predecessorDecisionReference", predecessor.getDecisionReference(),
                    "decisionReference", successor.getDecisionReference()))
            .build());

    return modregningResultMapper.toResult(successor, coverages);
  }

  /** Creates a same-lineage settlement decision for a correction-pool entry. */
  public ModregningResult createCorrectionPoolSettlementDecision(
      KorrektionspuljeEntry entry,
      ModregningEvent originEvent,
      BigDecimal total,
      LocalDate settlementDate,
      boolean restrictedPayment) {
    originEvent.setOperative(false);
    modregningEventRepository.save(originEvent);

    TierAllocationResult allocation =
        raekkefoeigenEngine.allocate(originEvent.getDebtorPersonId(), total, true, false, null);
    if (restrictedPayment) {
      allocation =
          applyRestrictedSettlementAllocation(originEvent.getDebtorPersonId(), total, allocation);
    }

    String settlementSuffix =
        ModregningReferenceFactory.settlementSuffix(settlementDate, entry.getId());
    ModregningEvent settlementEvent =
        ModregningEvent.builder()
            .nemkontoReferenceId(
                originEvent.getNemkontoReferenceId() + "-SETTLEMENT-" + settlementSuffix)
            .decisionReference(
                originEvent.getDecisionReference() + "-SETTLEMENT-" + settlementSuffix)
            .lineageReference(originEvent.getLineageReference())
            .decisionKind(ModregningDecisionKind.CORRECTION_POOL_SETTLEMENT_DECISION)
            .supersedesEventId(originEvent.getId())
            .operative(true)
            .debtorPersonId(originEvent.getDebtorPersonId())
            .receiptDate(settlementDate)
            .decisionDate(settlementDate)
            .paymentType(originEvent.getPaymentType())
            .indkomstAar(originEvent.getIndkomstAar())
            .disbursementAmount(total)
            .tier1Amount(BigDecimal.ZERO)
            .tier2Amount(sumTier(allocation.tier2Allocations()))
            .tier3Amount(sumTier(allocation.tier3Allocations()))
            .residualPayoutAmount(allocation.residualPayoutAmount())
            .tier2WaiverApplied(originEvent.isTier2WaiverApplied())
            .klageFristDato(settlementDate.plusYears(1))
            .renteGodtgoerelseStartDate(originEvent.getRenteGodtgoerelseStartDate())
            .renteGodtgoerelseNonTaxable(true)
            .build();
    settlementEvent = modregningEventRepository.save(settlementEvent);

    List<FordringCoverageDto> coverages =
        persistAllocationsAndLedgers(
            settlementEvent, allocation, originEvent.getDebtorPersonId(), false, null, true);
    notificationOutboxWriter.write(settlementEvent, coverages);
    return modregningResultMapper.toResult(settlementEvent, coverages);
  }

  /**
   * Handles Digital Post notice delivery callback — BUG-5/SPEC-058 §4.5.
   *
   * <p>When notice is delivered, recomputes klageFristDato = deliveryDate + 3 months.
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

  private InitialDecisionContext resolveInitialDecisionContext(Object sourceEvent) {
    if (sourceEvent instanceof PublicDisbursementEvent pde) {
      return new InitialDecisionContext(
          pde.nemkontoReferenceId(),
          pde.receiptDate(),
          pde.decisionDate(),
          pde.indkomstAar(),
          pde.payingAuthorityOrgId());
    }
    String refId = (sourceEvent instanceof String s) ? s : UUID.randomUUID().toString();
    return new InitialDecisionContext(refId, LocalDate.now(), LocalDate.now(), null, null);
  }

  private List<FordringCoverageDto> persistAllocationsAndLedgers(
      ModregningEvent event,
      TierAllocationResult allocation,
      UUID debtorPersonId,
      boolean waiverApplied,
      UUID caseworkerId,
      boolean shipPerAllocationAudit) {
    List<FordringCoverageDto> coverages = new ArrayList<>();
    for (FordringAllocation alloc : getAllAllocations(allocation)) {
      CollectionMeasureEntity measure =
          CollectionMeasureEntity.builder()
              .debtId(alloc.fordringId())
              .measureType(CollectionMeasureEntity.MeasureType.SET_OFF)
              .modregningEventId(event.getId())
              .amount(alloc.amountCovered())
              .tierLevel(alloc.tier())
              .waiverApplied(waiverApplied)
              .caseworkerId(caseworkerId)
              .build();
      collectionMeasureRepository.save(measure);

      ledgerServiceClient.postLedgerEntry(
          debtorPersonId, alloc.fordringId(), alloc.amountCovered(), event.getId());
      coverages.add(
          new FordringCoverageDto(alloc.fordringId(), alloc.amountCovered(), alloc.tier()));

      if (shipPerAllocationAudit) {
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
                        "decisionReference", event.getDecisionReference(),
                        "debtorPersonId", debtorPersonId.toString(),
                        "fordringId", alloc.fordringId().toString()))
                .build());
      }
    }
    return coverages;
  }

  private BigDecimal sumTier(List<FordringAllocation> allocations) {
    return allocations.stream()
        .map(FordringAllocation::amountCovered)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private TierAllocationResult applyRestrictedSettlementAllocation(
      UUID debtorPersonId, BigDecimal total, TierAllocationResult unrestrictedAllocation) {
    List<FordringAllocation> restrictedTier2 =
        unrestrictedAllocation.tier2Allocations().stream()
            .filter(
                allocation ->
                    fordringQueryPort.isChildBenefitOffsetEligible(allocation.fordringId()))
            .toList();
    BigDecimal remainingForTier3 = total.subtract(sumTier(restrictedTier2));
    List<FordringAllocation> restrictedTier3 =
        allocateSimpleTier(
            fordringQueryPort.getActiveChildBenefitEligibleFordringer(debtorPersonId, 3, null),
            remainingForTier3,
            3);
    BigDecimal residual = remainingForTier3.subtract(sumTier(restrictedTier3));
    return new TierAllocationResult(List.of(), restrictedTier2, restrictedTier3, residual);
  }

  private List<FordringAllocation> allocateSimpleTier(
      List<FordringProjection> fordringer, BigDecimal availableAmount, int tier) {
    List<FordringAllocation> allocations = new ArrayList<>();
    BigDecimal remaining = availableAmount;
    for (FordringProjection fordring : fordringer) {
      if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
        break;
      }
      BigDecimal covered = remaining.min(fordring.tilbaestaaendeBeloeb());
      if (covered.compareTo(BigDecimal.ZERO) > 0) {
        allocations.add(new FordringAllocation(fordring.fordringId(), covered, tier));
        remaining = remaining.subtract(covered);
      }
    }
    return allocations;
  }

  private List<FordringAllocation> getAllAllocations(TierAllocationResult allocation) {
    List<FordringAllocation> all = new ArrayList<>();
    all.addAll(allocation.tier1Allocations());
    all.addAll(allocation.tier2Allocations());
    all.addAll(allocation.tier3Allocations());
    return all;
  }

  private record InitialDecisionContext(
      String nemkontoReferenceId,
      LocalDate receiptDate,
      LocalDate decisionDate,
      Integer indkomstAar,
      UUID payingAuthorityOrgId) {}
}
