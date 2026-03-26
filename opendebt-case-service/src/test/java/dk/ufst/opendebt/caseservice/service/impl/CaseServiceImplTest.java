package dk.ufst.opendebt.caseservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;

import dk.ufst.opendebt.caseservice.entity.*;
import dk.ufst.opendebt.caseservice.repository.*;
import dk.ufst.opendebt.common.dto.*;
import dk.ufst.opendebt.common.exception.OpenDebtException;
import dk.ufst.opendebt.common.security.CaseAccessChecker;

@ExtendWith(MockitoExtension.class)
class CaseServiceImplTest {

  @Mock private CaseRepository caseRepository;
  @Mock private CaseAccessChecker caseAccessChecker;
  @Mock private CasePartyRepository casePartyRepository;
  @Mock private CaseDebtRepository caseDebtRepository;
  @Mock private CaseEventRepository caseEventRepository;
  @Mock private ObjectionRepository objectionRepository;
  @Mock private CaseJournalEntryRepository caseJournalEntryRepository;
  @Mock private CaseJournalNoteRepository caseJournalNoteRepository;
  @Mock private CollectionMeasureRepository collectionMeasureRepository;
  @Mock private CaseLegalBasisRepository caseLegalBasisRepository;
  @Mock private CaseRelationRepository caseRelationRepository;

  @InjectMocks private CaseServiceImpl service;

  @BeforeEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  // ── createCase ───────────────────────────────────────────────────────

  @Test
  void testCreateCase() {
    CaseDto input =
        CaseDto.builder()
            .caseNumber("SAG-001")
            .title("Test case")
            .description("A test case")
            .build();
    when(caseRepository.save(any(CaseEntity.class)))
        .thenAnswer(
            inv -> {
              CaseEntity e = inv.getArgument(0);
              e.setId(UUID.randomUUID());
              return e;
            });

    CaseDto result = service.createCase(input);

    assertThat(result.getCaseNumber()).isEqualTo("SAG-001");
    assertThat(result.getTitle()).isEqualTo("Test case");
    assertThat(result.getCaseState()).isEqualTo(CaseDto.CaseState.CREATED);
    verify(caseRepository).save(any(CaseEntity.class));
  }

  // ── getCaseById ──────────────────────────────────────────────────────

  @Test
  void testGetCaseById() {
    UUID caseId = UUID.randomUUID();
    CaseEntity entity = baseCaseEntity(caseId);
    when(caseRepository.findById(caseId)).thenReturn(Optional.of(entity));
    when(casePartyRepository.findByCaseId(caseId)).thenReturn(List.of());
    when(caseDebtRepository.findByCaseIdAndRemovedAtIsNull(caseId)).thenReturn(List.of());
    when(objectionRepository.findByCaseId(caseId)).thenReturn(List.of());

    CaseDto result = service.getCaseById(caseId);

    assertThat(result.getId()).isEqualTo(caseId);
    assertThat(result.getCaseNumber()).isEqualTo("SAG-TEST");
  }

  // ── listCases ────────────────────────────────────────────────────────

  @Test
  void testListCases() {
    Pageable pageable = PageRequest.of(0, 10);
    UUID caseId = UUID.randomUUID();
    CaseEntity entity = baseCaseEntity(caseId);
    when(caseRepository.findByFilters(null, null, pageable))
        .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));
    when(casePartyRepository.findByCaseId(caseId)).thenReturn(List.of());
    when(caseDebtRepository.findByCaseIdAndRemovedAtIsNull(caseId)).thenReturn(List.of());
    when(objectionRepository.findByCaseId(caseId)).thenReturn(List.of());

    Page<CaseDto> result = service.listCases(null, null, pageable);

    assertThat(result.getContent()).hasSize(1);
    verify(caseRepository).findByFilters(null, null, pageable);
  }

  // ── transitionState: valid ───────────────────────────────────────────

  @Test
  void testTransitionState_valid() {
    UUID caseId = UUID.randomUUID();
    CaseEntity entity = baseCaseEntity(caseId);
    entity.setCaseState(CaseState.CREATED);
    when(caseRepository.findById(caseId)).thenReturn(Optional.of(entity));
    when(caseRepository.save(any(CaseEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    when(caseEventRepository.save(any(CaseEventEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    // Stubs for toDto
    when(casePartyRepository.findByCaseId(caseId)).thenReturn(List.of());
    when(caseDebtRepository.findByCaseIdAndRemovedAtIsNull(caseId)).thenReturn(List.of());
    when(objectionRepository.findByCaseId(caseId)).thenReturn(List.of());

    CaseDto result = service.transitionState(caseId, CaseState.ASSESSED, "caseworker1");

    assertThat(result.getCaseState()).isEqualTo(CaseDto.CaseState.ASSESSED);
    ArgumentCaptor<CaseEventEntity> eventCaptor = ArgumentCaptor.forClass(CaseEventEntity.class);
    verify(caseEventRepository).save(eventCaptor.capture());
    assertThat(eventCaptor.getValue().getEventType()).isEqualTo(CaseEventType.STATE_CHANGED);
    assertThat(eventCaptor.getValue().getPerformedBy()).isEqualTo("caseworker1");
  }

  // ── transitionState: invalid ─────────────────────────────────────────

  @Test
  void testTransitionState_invalid() {
    UUID caseId = UUID.randomUUID();
    CaseEntity entity = baseCaseEntity(caseId);
    entity.setCaseState(CaseState.CREATED);
    when(caseRepository.findById(caseId)).thenReturn(Optional.of(entity));

    assertThatThrownBy(() -> service.transitionState(caseId, CaseState.CLOSED_PAID, "system"))
        .isInstanceOf(OpenDebtException.class)
        .hasMessageContaining("Invalid transition");
  }

  // ── addParty ─────────────────────────────────────────────────────────

  @Test
  void testAddParty() {
    UUID caseId = UUID.randomUUID();
    CaseEntity entity = baseCaseEntity(caseId);
    when(caseRepository.findById(caseId)).thenReturn(Optional.of(entity));
    when(casePartyRepository.save(any(CasePartyEntity.class)))
        .thenAnswer(
            inv -> {
              CasePartyEntity p = inv.getArgument(0);
              p.setId(UUID.randomUUID());
              return p;
            });
    when(caseEventRepository.save(any(CaseEventEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    CasePartyDto input =
        CasePartyDto.builder()
            .personId(UUID.randomUUID())
            .partyRole("PRIMARY_DEBTOR")
            .partyType("PERSON")
            .activeFrom(LocalDate.now())
            .build();

    CasePartyDto result = service.addParty(caseId, input);

    assertThat(result.getPartyRole()).isEqualTo("PRIMARY_DEBTOR");
    assertThat(result.getId()).isNotNull();
    verify(casePartyRepository).save(any(CasePartyEntity.class));
    verify(caseEventRepository).save(any(CaseEventEntity.class));
  }

  // ── removeParty ──────────────────────────────────────────────────────

  @Test
  void testRemoveParty() {
    UUID caseId = UUID.randomUUID();
    UUID partyId = UUID.randomUUID();
    CaseEntity caseEntity = baseCaseEntity(caseId);
    CasePartyEntity partyEntity =
        CasePartyEntity.builder()
            .id(partyId)
            .caseId(caseId)
            .personId(UUID.randomUUID())
            .partyRole(PartyRole.CO_DEBTOR)
            .partyType(PartyType.PERSON)
            .build();
    when(caseRepository.findById(caseId)).thenReturn(Optional.of(caseEntity));
    when(casePartyRepository.findById(partyId)).thenReturn(Optional.of(partyEntity));
    when(caseEventRepository.save(any(CaseEventEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    service.removeParty(caseId, partyId);

    verify(casePartyRepository).delete(partyEntity);
    verify(caseEventRepository).save(any(CaseEventEntity.class));
  }

  // ── addDebtToCase ────────────────────────────────────────────────────

  @Test
  void testAddDebt() {
    UUID caseId = UUID.randomUUID();
    UUID debtId = UUID.randomUUID();
    when(caseRepository.existsById(caseId)).thenReturn(true);
    when(caseDebtRepository.existsByCaseIdAndDebtIdAndRemovedAtIsNull(caseId, debtId))
        .thenReturn(false);
    when(caseDebtRepository.save(any(CaseDebtEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    service.addDebtToCase(caseId, debtId);

    verify(caseDebtRepository).save(any(CaseDebtEntity.class));
  }

  // ── addJournalEntry ──────────────────────────────────────────────────

  @Test
  void testAddJournalEntry() {
    UUID caseId = UUID.randomUUID();
    CaseEntity entity = baseCaseEntity(caseId);
    when(caseRepository.findById(caseId)).thenReturn(Optional.of(entity));
    when(caseJournalEntryRepository.save(any(CaseJournalEntryEntity.class)))
        .thenAnswer(
            inv -> {
              CaseJournalEntryEntity e = inv.getArgument(0);
              e.setId(UUID.randomUUID());
              return e;
            });
    when(caseEventRepository.save(any(CaseEventEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    CaseJournalEntryDto input =
        CaseJournalEntryDto.builder()
            .journalEntryTitle("Test Entry")
            .registeredBy("caseworker1")
            .build();

    CaseJournalEntryDto result = service.addJournalEntry(caseId, input);

    assertThat(result.getJournalEntryTitle()).isEqualTo("Test Entry");
    assertThat(result.getId()).isNotNull();
    verify(caseJournalEntryRepository).save(any(CaseJournalEntryEntity.class));
    verify(caseEventRepository).save(any(CaseEventEntity.class));
  }

  // ── addJournalNote ───────────────────────────────────────────────────

  @Test
  void testAddJournalNote() {
    UUID caseId = UUID.randomUUID();
    CaseEntity entity = baseCaseEntity(caseId);
    when(caseRepository.findById(caseId)).thenReturn(Optional.of(entity));
    when(caseJournalNoteRepository.save(any(CaseJournalNoteEntity.class)))
        .thenAnswer(
            inv -> {
              CaseJournalNoteEntity n = inv.getArgument(0);
              n.setId(UUID.randomUUID());
              return n;
            });
    when(caseEventRepository.save(any(CaseEventEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    CaseJournalNoteDto input =
        CaseJournalNoteDto.builder()
            .noteTitle("Important note")
            .noteText("Details here")
            .authorId("caseworker1")
            .build();

    CaseJournalNoteDto result = service.addJournalNote(caseId, input);

    assertThat(result.getNoteTitle()).isEqualTo("Important note");
    assertThat(result.getId()).isNotNull();
    verify(caseJournalNoteRepository).save(any(CaseJournalNoteEntity.class));
    verify(caseEventRepository).save(any(CaseEventEntity.class));
  }

  // ── addMeasure ───────────────────────────────────────────────────────

  @Test
  void testAddMeasure() {
    UUID caseId = UUID.randomUUID();
    CaseEntity entity = baseCaseEntity(caseId);
    when(caseRepository.findById(caseId)).thenReturn(Optional.of(entity));
    when(collectionMeasureRepository.save(any(CollectionMeasureEntity.class)))
        .thenAnswer(
            inv -> {
              CollectionMeasureEntity m = inv.getArgument(0);
              m.setId(UUID.randomUUID());
              return m;
            });
    when(caseEventRepository.save(any(CaseEventEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    CollectionMeasureDto input =
        CollectionMeasureDto.builder()
            .measureType("WAGE_GARNISHMENT")
            .startDate(LocalDate.now())
            .build();

    CollectionMeasureDto result = service.addMeasure(caseId, input);

    assertThat(result.getMeasureType()).isEqualTo("WAGE_GARNISHMENT");
    assertThat(result.getStatus()).isEqualTo("PLANNED");
    assertThat(result.getId()).isNotNull();
    verify(collectionMeasureRepository).save(any(CollectionMeasureEntity.class));
    verify(caseEventRepository).save(any(CaseEventEntity.class));
  }

  // ── addObjection ─────────────────────────────────────────────────────

  @Test
  void testAddObjection() {
    UUID caseId = UUID.randomUUID();
    CaseEntity entity = baseCaseEntity(caseId);
    when(caseRepository.findById(caseId)).thenReturn(Optional.of(entity));
    when(objectionRepository.save(any(ObjectionEntity.class)))
        .thenAnswer(
            inv -> {
              ObjectionEntity o = inv.getArgument(0);
              o.setId(UUID.randomUUID());
              return o;
            });
    when(caseEventRepository.save(any(CaseEventEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    ObjectionDto input =
        ObjectionDto.builder().objectionType("AMOUNT").description("Disputed amount").build();

    ObjectionDto result = service.addObjection(caseId, input);

    assertThat(result.getObjectionType()).isEqualTo("AMOUNT");
    assertThat(result.getStatus()).isEqualTo("RECEIVED");
    assertThat(result.getId()).isNotNull();
    verify(objectionRepository).save(any(ObjectionEntity.class));
    verify(caseEventRepository).save(any(CaseEventEntity.class));
  }

  // ── resolveObjection ─────────────────────────────────────────────────

  @Test
  void testResolveObjection() {
    UUID caseId = UUID.randomUUID();
    UUID objectionId = UUID.randomUUID();
    CaseEntity caseEntity = baseCaseEntity(caseId);
    ObjectionEntity objEntity =
        ObjectionEntity.builder()
            .id(objectionId)
            .caseId(caseId)
            .objectionType(ObjectionType.AMOUNT)
            .status(ObjectionStatus.RECEIVED)
            .description("Original complaint")
            .receivedAt(LocalDateTime.now())
            .build();
    when(caseRepository.findById(caseId)).thenReturn(Optional.of(caseEntity));
    when(objectionRepository.findById(objectionId)).thenReturn(Optional.of(objEntity));
    when(objectionRepository.save(any(ObjectionEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    when(caseEventRepository.save(any(CaseEventEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    ObjectionDto resolution =
        ObjectionDto.builder().status("REJECTED").description("Not valid").build();

    ObjectionDto result = service.resolveObjection(caseId, objectionId, resolution);

    assertThat(result.getStatus()).isEqualTo("REJECTED");
    assertThat(result.getResolvedAt()).isNotNull();
    verify(objectionRepository).save(any(ObjectionEntity.class));
    verify(caseEventRepository).save(any(CaseEventEntity.class));
  }

  // ── addLegalBasis ────────────────────────────────────────────────────

  @Test
  void testAddLegalBasis() {
    UUID caseId = UUID.randomUUID();
    CaseEntity entity = baseCaseEntity(caseId);
    when(caseRepository.findById(caseId)).thenReturn(Optional.of(entity));
    when(caseLegalBasisRepository.save(any(CaseLegalBasisEntity.class)))
        .thenAnswer(
            inv -> {
              CaseLegalBasisEntity b = inv.getArgument(0);
              b.setId(UUID.randomUUID());
              return b;
            });

    CaseLegalBasisDto input =
        CaseLegalBasisDto.builder()
            .legalSourceTitle("Inddrivelsesloven § 10")
            .paragraphReference("§ 10")
            .build();

    CaseLegalBasisDto result = service.addLegalBasis(caseId, input);

    assertThat(result.getLegalSourceTitle()).isEqualTo("Inddrivelsesloven § 10");
    assertThat(result.getId()).isNotNull();
    verify(caseLegalBasisRepository).save(any(CaseLegalBasisEntity.class));
  }

  // ── addRelation ──────────────────────────────────────────────────────

  @Test
  void testAddRelation() {
    UUID caseId = UUID.randomUUID();
    UUID targetCaseId = UUID.randomUUID();
    CaseEntity entity = baseCaseEntity(caseId);
    when(caseRepository.findById(caseId)).thenReturn(Optional.of(entity));
    when(caseRelationRepository.save(any(CaseRelationEntity.class)))
        .thenAnswer(
            inv -> {
              CaseRelationEntity r = inv.getArgument(0);
              r.setId(UUID.randomUUID());
              return r;
            });

    CaseRelationDto input =
        CaseRelationDto.builder()
            .targetCaseId(targetCaseId)
            .relationType("RELATED")
            .description("Related case")
            .createdBy("caseworker1")
            .build();

    CaseRelationDto result = service.addRelation(caseId, input);

    assertThat(result.getRelationType()).isEqualTo("RELATED");
    assertThat(result.getSourceCaseId()).isEqualTo(caseId);
    assertThat(result.getTargetCaseId()).isEqualTo(targetCaseId);
    assertThat(result.getId()).isNotNull();
    verify(caseRelationRepository).save(any(CaseRelationEntity.class));
  }

  // ── getEvents ────────────────────────────────────────────────────────

  @Test
  void testGetEvents() {
    UUID caseId = UUID.randomUUID();
    CaseEntity entity = baseCaseEntity(caseId);
    when(caseRepository.findById(caseId)).thenReturn(Optional.of(entity));
    CaseEventEntity event1 =
        CaseEventEntity.builder()
            .id(UUID.randomUUID())
            .caseId(caseId)
            .eventType(CaseEventType.CASE_CREATED)
            .description("Case created")
            .performedBy("system")
            .performedAt(LocalDateTime.now().minusHours(2))
            .build();
    CaseEventEntity event2 =
        CaseEventEntity.builder()
            .id(UUID.randomUUID())
            .caseId(caseId)
            .eventType(CaseEventType.STATE_CHANGED)
            .description("State changed")
            .performedBy("caseworker1")
            .performedAt(LocalDateTime.now())
            .build();
    when(caseEventRepository.findByCaseIdOrderByPerformedAtDesc(caseId))
        .thenReturn(List.of(event2, event1));

    List<CaseEventDto> result = service.getEvents(caseId);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).getEventType()).isEqualTo("STATE_CHANGED");
    assertThat(result.get(1).getEventType()).isEqualTo("CASE_CREATED");
  }

  // ── assignCase (caseworker assignment event) ─────────────────────────

  @Test
  void testAssignCaseworker() {
    UUID caseId = UUID.randomUUID();
    CaseEntity entity = baseCaseEntity(caseId);
    when(caseRepository.findById(caseId)).thenReturn(Optional.of(entity));
    when(caseRepository.save(any(CaseEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    when(casePartyRepository.findByCaseId(caseId)).thenReturn(List.of());
    when(caseDebtRepository.findByCaseIdAndRemovedAtIsNull(caseId)).thenReturn(List.of());
    when(objectionRepository.findByCaseId(caseId)).thenReturn(List.of());

    CaseDto result = service.assignCase(caseId, "caseworker42");

    assertThat(result.getPrimaryCaseworkerId()).isEqualTo("caseworker42");
    verify(caseRepository).save(any(CaseEntity.class));
  }

  // ── Helpers ──────────────────────────────────────────────────────────

  private CaseEntity baseCaseEntity(UUID caseId) {
    return CaseEntity.builder()
        .id(caseId)
        .caseNumber("SAG-TEST")
        .title("Test Case")
        .caseState(CaseState.CREATED)
        .stateChangedAt(LocalDateTime.now())
        .caseType(CaseType.DEBT_COLLECTION)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }
}
