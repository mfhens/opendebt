package dk.ufst.opendebt.debtservice.section50.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dk.ufst.opendebt.debtservice.section50.Section50ContextType;
import dk.ufst.opendebt.debtservice.section50.Section50ModregningOutcome;
import dk.ufst.opendebt.debtservice.section50.Section50OrderingMode;
import dk.ufst.opendebt.debtservice.section50.dto.GenerateSection50WorklistRequest;
import dk.ufst.opendebt.debtservice.section50.dto.Section50ModregningDecisionRequest;
import dk.ufst.opendebt.debtservice.section50.dto.Section50OverrideRequest;
import dk.ufst.opendebt.debtservice.section50.entity.Section50CandidateItemEntity;
import dk.ufst.opendebt.debtservice.section50.entity.Section50DecisionSnapshotEntity;
import dk.ufst.opendebt.debtservice.section50.entity.Section50WorklistEntity;
import dk.ufst.opendebt.debtservice.section50.repository.Section50CandidateItemRepository;
import dk.ufst.opendebt.debtservice.section50.repository.Section50DecisionSnapshotRepository;
import dk.ufst.opendebt.debtservice.section50.repository.Section50WorklistEntryRepository;
import dk.ufst.opendebt.debtservice.section50.repository.Section50WorklistRepository;
import dk.ufst.opendebt.debtservice.section50.service.Section50OrderingPolicyEngine;
import dk.ufst.opendebt.debtservice.section50.service.Section50OrderingPolicyEngine.ComputedWorklist;

@ExtendWith(MockitoExtension.class)
class Section50WorklistApplicationServiceImplTest {

  private static final UUID DEBTOR_ID = UUID.fromString("06000000-0000-0000-0000-000000000060");
  private static final Instant GENERATED_AT = Instant.parse("2026-05-27T10:15:30Z");

  @Mock private Section50CandidateItemRepository candidateItemRepository;
  @Mock private Section50WorklistRepository worklistRepository;
  @Mock private Section50WorklistEntryRepository worklistEntryRepository;
  @Mock private Section50DecisionSnapshotRepository decisionSnapshotRepository;
  @Mock private Section50OrderingPolicyEngine orderingPolicyEngine;

  private final Map<UUID, Section50WorklistEntity> worklists = new HashMap<>();
  private final Map<UUID, Section50DecisionSnapshotEntity> snapshots = new HashMap<>();

  private Section50WorklistApplicationServiceImpl service;

  @BeforeEach
  void setUp() {
    service =
        new Section50WorklistApplicationServiceImpl(
            candidateItemRepository,
            worklistRepository,
            worklistEntryRepository,
            decisionSnapshotRepository,
            orderingPolicyEngine);

    when(worklistRepository.save(any(Section50WorklistEntity.class)))
        .thenAnswer(
            invocation -> {
              Section50WorklistEntity worklist = invocation.getArgument(0);
              if (worklist.getId() == null) {
                worklist.setId(UUID.randomUUID());
              }
              if (worklist.getGeneratedAt() == null) {
                worklist.setGeneratedAt(GENERATED_AT);
              }
              worklists.put(worklist.getId(), worklist);
              return worklist;
            });
    when(decisionSnapshotRepository.save(any(Section50DecisionSnapshotEntity.class)))
        .thenAnswer(
            invocation -> {
              Section50DecisionSnapshotEntity snapshot = invocation.getArgument(0);
              if (snapshot.getId() == null) {
                snapshot.setId(UUID.randomUUID());
              }
              snapshots.put(snapshot.getWorklistId(), snapshot);
              return snapshot;
            });
    when(decisionSnapshotRepository.findByWorklistId(any(UUID.class)))
        .thenAnswer(invocation -> Optional.ofNullable(snapshots.get(invocation.getArgument(0))));
    lenient()
        .when(worklistEntryRepository.findByWorklistIdOrderByRankOrder(any(UUID.class)))
        .thenReturn(List.of());
    lenient()
        .when(worklistEntryRepository.saveAll(anyList()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    lenient()
        .when(candidateItemRepository.findByDebtorPersonId(any(UUID.class)))
        .thenReturn(List.of(candidate("C-1")));
    lenient()
        .when(candidateItemRepository.findByDebtorPersonIdAndClaimIdIn(any(UUID.class), anyList()))
        .thenReturn(List.of(candidate("C-1")));
    lenient()
        .when(
            orderingPolicyEngine.compute(
                any(), any(), anyList(), any(), any(), any(Boolean.class), any()))
        .thenReturn(computedWorklist());
  }

  @Test
  void generateWorklist_persistsSystemOriginWhenRequestedBySystem() {
    service.generateWorklist(
        DEBTOR_ID,
        new GenerateSection50WorklistRequest(
            Section50ContextType.DEFAULT, null, null, null, Boolean.TRUE));

    assertThat(snapshots.values())
        .singleElement()
        .extracting(Section50DecisionSnapshotEntity::getOrigin)
        .isEqualTo("SYSTEM");
  }

  @Test
  void generateWorklist_persistsCaseworkerOriginWhenRequestedBySystemIsFalse() {
    service.generateWorklist(
        DEBTOR_ID,
        new GenerateSection50WorklistRequest(
            Section50ContextType.DEFAULT, null, null, null, Boolean.FALSE));

    assertThat(snapshots.values())
        .singleElement()
        .extracting(Section50DecisionSnapshotEntity::getOrigin)
        .isEqualTo("CASEWORKER");
  }

  @Test
  void applyOverride_updatesSnapshotOriginToCaseworker() {
    Section50WorklistEntity worklist = persistedWorklist();
    Section50DecisionSnapshotEntity snapshot = persistedSnapshot(worklist, "SYSTEM");
    when(worklistRepository.findByIdAndDebtorPersonId(worklist.getId(), DEBTOR_ID))
        .thenReturn(Optional.of(worklist));
    when(decisionSnapshotRepository.findByWorklistId(worklist.getId()))
        .thenReturn(Optional.of(snapshot));

    service.applyOverride(
        DEBTOR_ID,
        worklist.getId(),
        new Section50OverrideRequest(
            "Urgent court deadline", "Section 50 subsection 2", Boolean.TRUE, List.of("C-1")));

    assertThat(snapshot.getOrigin()).isEqualTo("CASEWORKER");
  }

  @Test
  void recordModregningDecision_updatesSnapshotOriginToCaseworker() {
    Section50WorklistEntity worklist = persistedWorklist();
    Section50DecisionSnapshotEntity snapshot = persistedSnapshot(worklist, "SYSTEM");
    when(worklistRepository.findByIdAndDebtorPersonId(worklist.getId(), DEBTOR_ID))
        .thenReturn(Optional.of(worklist));
    when(decisionSnapshotRepository.findByWorklistId(worklist.getId()))
        .thenReturn(Optional.of(snapshot));

    service.recordModregningDecision(
        DEBTOR_ID,
        worklist.getId(),
        new Section50ModregningDecisionRequest(
            Section50ModregningOutcome.NO_MODREGNING, "Deadline pressure"));

    assertThat(snapshot.getOrigin()).isEqualTo("CASEWORKER");
  }

  private Section50CandidateItemEntity candidate(String claimId) {
    return Section50CandidateItemEntity.builder()
        .id(UUID.randomUUID())
        .debtorPersonId(DEBTOR_ID)
        .claimId(claimId)
        .build();
  }

  private ComputedWorklist computedWorklist() {
    return new ComputedWorklist(
        Section50OrderingMode.DEFAULT_SECTION_50,
        "Section 50 default",
        new BigDecimal("400.00"),
        "C-1",
        List.of(),
        "DEFAULT_SECTION_50_PATH",
        "hash",
        "notes",
        List.of("factor"),
        null);
  }

  private Section50WorklistEntity persistedWorklist() {
    Section50WorklistEntity worklist =
        Section50WorklistEntity.builder()
            .id(UUID.randomUUID())
            .debtorPersonId(DEBTOR_ID)
            .contextType(Section50ContextType.DEFAULT)
            .orderingMode(Section50OrderingMode.DEFAULT_SECTION_50)
            .legalReference("Section 50 default")
            .amountWindow(new BigDecimal("400.00"))
            .generatedAt(GENERATED_AT)
            .selectedNextItemId("C-1")
            .build();
    worklists.put(worklist.getId(), worklist);
    return worklist;
  }

  private Section50DecisionSnapshotEntity persistedSnapshot(
      Section50WorklistEntity worklist, String origin) {
    Section50DecisionSnapshotEntity snapshot =
        Section50DecisionSnapshotEntity.builder()
            .id(UUID.randomUUID())
            .worklistId(worklist.getId())
            .rulePath("DEFAULT_SECTION_50_PATH")
            .inputHash("hash")
            .selectedNextItemId("C-1")
            .legalReference("Section 50 default")
            .auditEventId(UUID.randomUUID())
            .origin(origin)
            .occurredAt(GENERATED_AT)
            .notes("notes")
            .prioritisationFactors("factor")
            .build();
    snapshots.put(worklist.getId(), snapshot);
    return snapshot;
  }
}
