package dk.ufst.opendebt.debtservice.section50.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.ufst.opendebt.debtservice.section50.Section50OrderingMode;
import dk.ufst.opendebt.debtservice.section50.dto.GenerateSection50WorklistRequest;
import dk.ufst.opendebt.debtservice.section50.dto.Section50DecisionSnapshotDto;
import dk.ufst.opendebt.debtservice.section50.dto.Section50ModregningDecisionRequest;
import dk.ufst.opendebt.debtservice.section50.dto.Section50OverrideRequest;
import dk.ufst.opendebt.debtservice.section50.dto.Section50WorklistDto;
import dk.ufst.opendebt.debtservice.section50.dto.Section50WorklistEntryDto;
import dk.ufst.opendebt.debtservice.section50.entity.Section50DecisionSnapshotEntity;
import dk.ufst.opendebt.debtservice.section50.entity.Section50WorklistEntity;
import dk.ufst.opendebt.debtservice.section50.entity.Section50WorklistEntryEntity;
import dk.ufst.opendebt.debtservice.section50.repository.Section50CandidateItemRepository;
import dk.ufst.opendebt.debtservice.section50.repository.Section50DecisionSnapshotRepository;
import dk.ufst.opendebt.debtservice.section50.repository.Section50WorklistEntryRepository;
import dk.ufst.opendebt.debtservice.section50.repository.Section50WorklistRepository;
import dk.ufst.opendebt.debtservice.section50.service.Section50OrderingPolicyEngine;
import dk.ufst.opendebt.debtservice.section50.service.Section50WorklistApplicationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class Section50WorklistApplicationServiceImpl
    implements Section50WorklistApplicationService {

  private final Section50CandidateItemRepository candidateItemRepository;
  private final Section50WorklistRepository worklistRepository;
  private final Section50WorklistEntryRepository worklistEntryRepository;
  private final Section50DecisionSnapshotRepository decisionSnapshotRepository;
  private final Section50OrderingPolicyEngine orderingPolicyEngine;

  @Override
  public Section50WorklistDto generateWorklist(
      UUID debtorPersonId, GenerateSection50WorklistRequest request) {
    List<dk.ufst.opendebt.debtservice.section50.entity.Section50CandidateItemEntity> candidates =
        request.candidateClaimIds() == null || request.candidateClaimIds().isEmpty()
            ? candidateItemRepository.findByDebtorPersonId(debtorPersonId)
            : candidateItemRepository.findByDebtorPersonIdAndClaimIdIn(
                debtorPersonId, request.candidateClaimIds());
    Section50OrderingPolicyEngine.ComputedWorklist computed =
        orderingPolicyEngine.compute(
            debtorPersonId, request, candidates, null, "Section 50 default path", false, null);
    return persistComputed(debtorPersonId, request, computed, null, null, null);
  }

  @Override
  @Transactional(readOnly = true)
  public Section50WorklistDto getWorklist(UUID debtorPersonId, UUID worklistId) {
    Section50WorklistEntity worklist = requireWorklist(debtorPersonId, worklistId);
    return toDto(worklist);
  }

  @Override
  public Section50WorklistDto applyOverride(
      UUID debtorPersonId, UUID worklistId, Section50OverrideRequest request) {
    Section50WorklistEntity existing = requireWorklist(debtorPersonId, worklistId);
    GenerateSection50WorklistRequest regenerateRequest =
        new GenerateSection50WorklistRequest(
            existing.getContextType(), existing.getAmountWindow(), null, null, Boolean.FALSE);
    List<dk.ufst.opendebt.debtservice.section50.entity.Section50CandidateItemEntity> candidates =
        candidateItemRepository.findByDebtorPersonId(debtorPersonId);
    Section50OrderingPolicyEngine.ComputedWorklist computed =
        orderingPolicyEngine.compute(
            debtorPersonId,
            regenerateRequest,
            candidates,
            request.overrideReason(),
            request.legalBasis(),
            Boolean.TRUE.equals(request.expedited()),
            request.selectedClaimOrder());
    updatePersisted(
        existing,
        computed,
        request.overrideReason(),
        request.legalBasis(),
        existing.getModregningOutcome());
    return toDto(existing);
  }

  @Override
  public Section50WorklistDto recordModregningDecision(
      UUID debtorPersonId, UUID worklistId, Section50ModregningDecisionRequest request) {
    Section50WorklistEntity existing = requireWorklist(debtorPersonId, worklistId);
    existing.setOrderingMode(Section50OrderingMode.MODREGNING_ABSTAINED);
    existing.setModregningOutcome(request.modregningOutcome());
    existing.setDeviationReason(request.reason());
    existing.setLegalReference("Section 50 subsection 5 abstention");
    worklistRepository.save(existing);

    Section50DecisionSnapshotEntity snapshot =
        decisionSnapshotRepository.findByWorklistId(worklistId).orElseThrow();
    snapshot.setRulePath("SECTION_50_MODREGNING_ABSTAINED_PATH");
    snapshot.setLegalReference(existing.getLegalReference());
    snapshot.setNotes(request.reason());
    snapshot.setOrigin("CASEWORKER");
    decisionSnapshotRepository.save(snapshot);
    return toDto(existing);
  }

  private Section50WorklistDto persistComputed(
      UUID debtorPersonId,
      GenerateSection50WorklistRequest request,
      Section50OrderingPolicyEngine.ComputedWorklist computed,
      String overrideReason,
      String overrideLegalBasis,
      dk.ufst.opendebt.debtservice.section50.Section50ModregningOutcome modregningOutcome) {
    Section50WorklistEntity worklist =
        Section50WorklistEntity.builder()
            .debtorPersonId(debtorPersonId)
            .contextType(request.contextType())
            .orderingMode(computed.orderingMode())
            .legalReference(computed.legalReference())
            .amountWindow(computed.amountWindow())
            .generatedAt(java.time.Instant.now())
            .selectedNextItemId(computed.selectedNextItemId())
            .overrideReason(overrideReason)
            .overrideLegalBasis(overrideLegalBasis)
            .deviationReason(computed.deviationReason())
            .modregningOutcome(modregningOutcome)
            .build();
    worklist = worklistRepository.save(worklist);

    persistEntries(worklist.getId(), computed.entries());
    persistSnapshot(worklist, computed, resolveOrigin(request.requestedBySystem()));
    return toDto(worklist);
  }

  private void updatePersisted(
      Section50WorklistEntity worklist,
      Section50OrderingPolicyEngine.ComputedWorklist computed,
      String overrideReason,
      String overrideLegalBasis,
      dk.ufst.opendebt.debtservice.section50.Section50ModregningOutcome modregningOutcome) {
    worklist.setOrderingMode(computed.orderingMode());
    worklist.setLegalReference(computed.legalReference());
    worklist.setAmountWindow(computed.amountWindow());
    worklist.setSelectedNextItemId(computed.selectedNextItemId());
    worklist.setOverrideReason(overrideReason);
    worklist.setOverrideLegalBasis(overrideLegalBasis);
    worklist.setDeviationReason(computed.deviationReason());
    worklist.setModregningOutcome(modregningOutcome);
    worklistRepository.save(worklist);

    worklistEntryRepository.deleteByWorklistId(worklist.getId());
    persistEntries(worklist.getId(), computed.entries());

    Section50DecisionSnapshotEntity snapshot =
        decisionSnapshotRepository.findByWorklistId(worklist.getId()).orElseThrow();
    snapshot.setRulePath(computed.rulePath());
    snapshot.setInputHash(computed.inputHash());
    snapshot.setSelectedNextItemId(computed.selectedNextItemId());
    snapshot.setLegalReference(computed.legalReference());
    snapshot.setNotes(computed.notes());
    snapshot.setPrioritisationFactors(String.join("|", computed.prioritisationFactors()));
    snapshot.setOrigin("CASEWORKER");
    decisionSnapshotRepository.save(snapshot);
  }

  private void persistEntries(
      UUID worklistId, List<Section50OrderingPolicyEngine.ComputedEntry> entries) {
    worklistEntryRepository.saveAll(
        entries.stream()
            .map(
                entry ->
                    Section50WorklistEntryEntity.builder()
                        .worklistId(worklistId)
                        .rankOrder(entry.rank())
                        .claimId(entry.claimId())
                        .itemType(entry.itemType())
                        .claimCategory(entry.claimCategory())
                        .suspectedDataError(entry.suspectedDataError())
                        .confirmedRetskraft(entry.confirmedRetskraft())
                        .withinAmountWindow(entry.withinAmountWindow())
                        .selectionReason(entry.selectionReason())
                        .prioritisationFactors(String.join("|", entry.prioritisationFactors()))
                        .suppressedReason(entry.suppressedReason())
                        .amount(entry.amount())
                        .build())
            .toList());
  }

  private void persistSnapshot(
      Section50WorklistEntity worklist,
      Section50OrderingPolicyEngine.ComputedWorklist computed,
      String origin) {
    decisionSnapshotRepository.save(
        Section50DecisionSnapshotEntity.builder()
            .worklistId(worklist.getId())
            .rulePath(computed.rulePath())
            .inputHash(computed.inputHash())
            .selectedNextItemId(computed.selectedNextItemId())
            .legalReference(computed.legalReference())
            .auditEventId(UUID.randomUUID())
            .origin(origin)
            .occurredAt(worklist.getGeneratedAt())
            .notes(computed.notes())
            .prioritisationFactors(String.join("|", computed.prioritisationFactors()))
            .build());
  }

  private String resolveOrigin(Boolean requestedBySystem) {
    return Boolean.TRUE.equals(requestedBySystem) ? "SYSTEM" : "CASEWORKER";
  }

  private Section50WorklistEntity requireWorklist(UUID debtorPersonId, UUID worklistId) {
    return worklistRepository
        .findByIdAndDebtorPersonId(worklistId, debtorPersonId)
        .orElseThrow(
            () -> new IllegalArgumentException("Section50 worklist not found: " + worklistId));
  }

  private Section50WorklistDto toDto(Section50WorklistEntity worklist) {
    List<Section50WorklistEntryDto> entries =
        worklistEntryRepository.findByWorklistIdOrderByRankOrder(worklist.getId()).stream()
            .map(
                entry ->
                    new Section50WorklistEntryDto(
                        entry.getRankOrder(),
                        entry.getClaimId(),
                        entry.getItemType(),
                        entry.getClaimCategory(),
                        entry.isSuspectedDataError(),
                        entry.isConfirmedRetskraft(),
                        entry.isWithinAmountWindow(),
                        entry.getSelectionReason(),
                        split(entry.getPrioritisationFactors()),
                        entry.getSuppressedReason(),
                        entry.getAmount()))
            .toList();
    Section50DecisionSnapshotEntity snapshot =
        decisionSnapshotRepository.findByWorklistId(worklist.getId()).orElseThrow();
    return new Section50WorklistDto(
        worklist.getId(),
        worklist.getDebtorPersonId(),
        worklist.getOrderingMode(),
        worklist.getLegalReference(),
        worklist.getContextType(),
        worklist.getAmountWindow(),
        worklist.getGeneratedAt(),
        worklist.getSelectedNextItemId(),
        worklist.getOverrideReason(),
        worklist.getOverrideLegalBasis(),
        worklist.getDeviationReason(),
        worklist.getModregningOutcome(),
        entries,
        new Section50DecisionSnapshotDto(
            snapshot.getId(),
            snapshot.getWorklistId(),
            snapshot.getRulePath(),
            snapshot.getInputHash(),
            snapshot.getSelectedNextItemId(),
            snapshot.getLegalReference(),
            snapshot.getAuditEventId(),
            snapshot.getOrigin(),
            snapshot.getOccurredAt(),
            snapshot.getNotes(),
            split(snapshot.getPrioritisationFactors())));
  }

  private List<String> split(String raw) {
    if (raw == null || raw.isBlank()) {
      return List.of();
    }
    return List.of(raw.split("\\|"));
  }
}
