package dk.ufst.opendebt.caseservice.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.ufst.opendebt.caseservice.entity.*;
import dk.ufst.opendebt.caseservice.repository.*;
import dk.ufst.opendebt.caseservice.service.CaseService;
import dk.ufst.opendebt.common.dto.*;
import dk.ufst.opendebt.common.dto.AssignDebtToCaseRequest;
import dk.ufst.opendebt.common.dto.AssignDebtToCaseResponse;
import dk.ufst.opendebt.common.exception.OpenDebtException;
import dk.ufst.opendebt.common.security.AuthContext;
import dk.ufst.opendebt.common.security.CaseAccessChecker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaseServiceImpl implements CaseService {

  private static final String CASE_NOT_FOUND_PREFIX = "Case not found: ";
  private static final String CASE_NOT_FOUND_CODE = "CASE_NOT_FOUND";
  private static final String SYSTEM_USER = "system";

  private final CaseRepository caseRepository;
  private final CaseAccessChecker caseAccessChecker;
  private final CasePartyRepository casePartyRepository;
  private final CaseDebtRepository caseDebtRepository;
  private final CaseEventRepository caseEventRepository;
  private final ObjectionRepository objectionRepository;
  private final CaseJournalEntryRepository caseJournalEntryRepository;
  private final CaseJournalNoteRepository caseJournalNoteRepository;
  private final CollectionMeasureRepository collectionMeasureRepository;
  private final CaseLegalBasisRepository caseLegalBasisRepository;
  private final CaseRelationRepository caseRelationRepository;

  @Override
  @Transactional(readOnly = true)
  public Page<CaseDto> listCases(
      CaseDto.CaseState caseState, String caseworkerId, Pageable pageable) {
    CaseState entityState = caseState != null ? mapCaseState(caseState) : null;

    // In dev/local demo mode the security filter chain permits anonymous access.
    // In that mode, skip JWT-derived role filtering and return the seeded demo cases as-is.
    AuthContext authContext = tryGetAuthContext();
    String effectiveCaseworkerId = caseworkerId;

    // Rule 1.1: Caseworkers can only see their assigned cases (unless supervisor or admin)
    if (authContext != null
        && authContext.hasRole("CASEWORKER")
        && !authContext.isSupervisorOrAdmin()) {
      effectiveCaseworkerId = authContext.getUserId();
      if (caseworkerId != null && !caseworkerId.equals(effectiveCaseworkerId)) {
        log.warn(
            "Caseworker {} attempted to filter by another caseworker: {}",
            effectiveCaseworkerId,
            caseworkerId);
      }
    }

    return caseRepository
        .findByFilters(entityState, effectiveCaseworkerId, pageable)
        .map(this::toDto);
  }

  @Override
  @Transactional(readOnly = true)
  public CaseDto getCaseById(UUID id) {
    CaseEntity entity =
        caseRepository
            .findById(id)
            .orElseThrow(
                () -> new OpenDebtException(CASE_NOT_FOUND_PREFIX + id, CASE_NOT_FOUND_CODE));

    // In dev/local demo mode the security filter chain permits anonymous access.
    // In that mode, skip JWT-derived access checks so the seeded demo case can be viewed.
    AuthContext authContext = tryGetAuthContext();
    if (authContext != null && !caseAccessChecker.canAccessCase(id, authContext)) {
      log.warn("Access denied to case {} for user {}", id, authContext.getUserId());
      throw new org.springframework.security.access.AccessDeniedException(
          "You do not have permission to access case: " + id);
    }

    return toDto(entity);
  }

  private AuthContext tryGetAuthContext() {
    try {
      return AuthContext.fromSecurityContext();
    } catch (IllegalStateException e) {
      log.debug("No JWT authentication available; using demo-mode unrestricted case access");
      return null;
    }
  }

  @Override
  @Transactional(readOnly = true)
  public List<CaseDto> getCasesByDebtor(String debtorId) {
    UUID debtorPersonId = UUID.fromString(debtorId);
    // Look up cases via case_parties with PRIMARY_DEBTOR role
    List<CasePartyEntity> parties =
        casePartyRepository.findByCaseIdAndPartyRole(null, PartyRole.PRIMARY_DEBTOR).stream()
            .filter(p -> p.getPersonId().equals(debtorPersonId))
            .toList();

    // Fallback: query all parties for this person
    if (parties.isEmpty()) {
      return List.of();
    }

    return parties.stream()
        .map(p -> caseRepository.findById(p.getCaseId()).map(this::toDto).orElse(null))
        .filter(dto -> dto != null)
        .toList();
  }

  @Override
  @Transactional
  public CaseDto createCase(CaseDto dto) {
    CaseEntity entity =
        CaseEntity.builder()
            .caseNumber(dto.getCaseNumber())
            .title(
                dto.getTitle() != null ? dto.getTitle() : "Inddrivelsessag " + dto.getCaseNumber())
            .description(dto.getDescription())
            .caseState(
                dto.getCaseState() != null ? mapCaseState(dto.getCaseState()) : CaseState.CREATED)
            .stateChangedAt(LocalDateTime.now())
            .caseType(
                dto.getCaseType() != null
                    ? CaseType.valueOf(dto.getCaseType())
                    : CaseType.DEBT_COLLECTION)
            .primaryCaseworkerId(dto.getPrimaryCaseworkerId())
            .ownerOrganisationId(dto.getOwnerOrganisationId())
            .build();
    CaseEntity saved = caseRepository.save(entity);
    log.info("Created case: id={}, number={}", saved.getId(), saved.getCaseNumber());
    return toDto(saved);
  }

  @Override
  @Transactional
  public CaseDto updateCase(UUID id, CaseDto dto) {
    CaseEntity entity =
        caseRepository
            .findById(id)
            .orElseThrow(
                () -> new OpenDebtException(CASE_NOT_FOUND_PREFIX + id, CASE_NOT_FOUND_CODE));
    if (dto.getTitle() != null) entity.setTitle(dto.getTitle());
    if (dto.getDescription() != null) entity.setDescription(dto.getDescription());
    if (dto.getCaseState() != null) {
      CaseState newState = mapCaseState(dto.getCaseState());
      entity.setCaseState(newState);
      entity.setStateChangedAt(LocalDateTime.now());
    }
    if (dto.getPrimaryCaseworkerId() != null)
      entity.setPrimaryCaseworkerId(dto.getPrimaryCaseworkerId());
    return toDto(caseRepository.save(entity));
  }

  @Override
  @Transactional
  public CaseDto assignCase(UUID id, String caseworkerId) {
    CaseEntity entity =
        caseRepository
            .findById(id)
            .orElseThrow(
                () -> new OpenDebtException(CASE_NOT_FOUND_PREFIX + id, CASE_NOT_FOUND_CODE));
    entity.setPrimaryCaseworkerId(caseworkerId);
    log.info("Assigned case {} to caseworker {}", id, caseworkerId);
    return toDto(caseRepository.save(entity));
  }

  @Override
  @Transactional
  public CaseDto closeCase(UUID id, CaseDto.CaseState closureState) {
    CaseEntity entity =
        caseRepository
            .findById(id)
            .orElseThrow(
                () -> new OpenDebtException(CASE_NOT_FOUND_PREFIX + id, CASE_NOT_FOUND_CODE));
    CaseState newState = mapCaseState(closureState);
    entity.setCaseState(newState);
    entity.setStateChangedAt(LocalDateTime.now());
    log.info("Closed case {} with state {}", id, closureState);
    return toDto(caseRepository.save(entity));
  }

  @Override
  @Transactional
  public void addDebtToCase(UUID caseId, UUID debtId) {
    linkDebtToCase(caseId, debtId);
  }

  private void linkDebtToCase(UUID caseId, UUID debtId) {
    if (!caseRepository.existsById(caseId)) {
      throw new OpenDebtException(CASE_NOT_FOUND_PREFIX + caseId, CASE_NOT_FOUND_CODE);
    }
    if (!caseDebtRepository.existsByCaseIdAndDebtIdAndRemovedAtIsNull(caseId, debtId)) {
      CaseDebtEntity caseDebt =
          CaseDebtEntity.builder().caseId(caseId).debtId(debtId).addedBy(SYSTEM_USER).build();
      caseDebtRepository.save(caseDebt);
      log.info("Added debt {} to case {}", debtId, caseId);
    }
  }

  @Override
  @Transactional
  public void removeDebtFromCase(UUID caseId, UUID debtId) {
    List<CaseDebtEntity> activeDebts = caseDebtRepository.findByCaseIdAndRemovedAtIsNull(caseId);
    activeDebts.stream()
        .filter(cd -> cd.getDebtId().equals(debtId))
        .findFirst()
        .ifPresent(
            cd -> {
              cd.setRemovedAt(LocalDateTime.now());
              cd.setRemovedBy(SYSTEM_USER);
              caseDebtRepository.save(cd);
              log.info("Removed debt {} from case {}", debtId, caseId);
            });
  }

  @Override
  @Transactional
  public AssignDebtToCaseResponse findOrCreateCaseForDebt(AssignDebtToCaseRequest request) {
    UUID debtorPersonId = request.getDebtorPersonId();
    UUID debtId = request.getDebtId();

    // Find existing open case for this debtor
    List<CasePartyEntity> debtorParties =
        casePartyRepository.findByPersonIdAndPartyRole(debtorPersonId, PartyRole.PRIMARY_DEBTOR);

    CaseEntity existingCase = null;
    for (CasePartyEntity party : debtorParties) {
      CaseEntity candidate = caseRepository.findById(party.getCaseId()).orElse(null);
      if (candidate != null && !candidate.getCaseState().isClosed()) {
        existingCase = candidate;
        break;
      }
    }

    if (existingCase != null) {
      linkDebtToCase(existingCase.getId(), debtId);
      recordEvent(existingCase.getId(), CaseEventType.DEBT_ADDED, "Debt added to existing case");
      log.info("Assigned debt {} to existing case {}", debtId, existingCase.getCaseNumber());
      return AssignDebtToCaseResponse.builder()
          .caseId(existingCase.getId())
          .caseNumber(existingCase.getCaseNumber())
          .newCase(false)
          .build();
    }

    // Create new case
    String caseNumber = generateCaseNumber();
    CaseEntity newCase =
        CaseEntity.builder()
            .caseNumber(caseNumber)
            .title("Inddrivelsessag " + caseNumber)
            .caseState(CaseState.CREATED)
            .stateChangedAt(LocalDateTime.now())
            .caseType(CaseType.DEBT_COLLECTION)
            .ownerOrganisationId("UFST")
            .createdBy(SYSTEM_USER)
            .build();
    CaseEntity savedCase = caseRepository.save(newCase);

    // Add debtor as primary party
    CasePartyEntity partyEntity =
        CasePartyEntity.builder()
            .caseId(savedCase.getId())
            .personId(debtorPersonId)
            .partyRole(PartyRole.PRIMARY_DEBTOR)
            .partyType(PartyType.PERSON)
            .activeFrom(LocalDate.now())
            .addedBy(SYSTEM_USER)
            .build();
    casePartyRepository.save(partyEntity);

    // Link debt
    linkDebtToCase(savedCase.getId(), debtId);

    // Record events
    recordEvent(savedCase.getId(), CaseEventType.CASE_CREATED, "Case created for incoming claim");
    recordEvent(savedCase.getId(), CaseEventType.DEBT_ADDED, "Initial debt added to case");

    log.info("Created new case {} for debtor {} with debt {}", caseNumber, debtorPersonId, debtId);
    return AssignDebtToCaseResponse.builder()
        .caseId(savedCase.getId())
        .caseNumber(caseNumber)
        .newCase(true)
        .build();
  }

  private String generateCaseNumber() {
    int year = Year.now().getValue();
    long count = caseRepository.count() + 1;
    return String.format("SAG-%d-%05d", year, count);
  }

  // ── Party management ─────────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public List<CasePartyDto> getParties(UUID caseId) {
    findOrThrow(caseId);
    return casePartyRepository.findByCaseId(caseId).stream().map(this::toPartyDto).toList();
  }

  @Override
  @Transactional
  public CasePartyDto addParty(UUID caseId, CasePartyDto partyDto) {
    findOrThrow(caseId);
    CasePartyEntity entity =
        CasePartyEntity.builder()
            .caseId(caseId)
            .personId(partyDto.getPersonId())
            .partyRole(PartyRole.valueOf(partyDto.getPartyRole()))
            .partyType(PartyType.valueOf(partyDto.getPartyType()))
            .activeFrom(partyDto.getActiveFrom())
            .activeTo(partyDto.getActiveTo())
            .build();
    CasePartyEntity saved = casePartyRepository.save(entity);
    recordEvent(caseId, CaseEventType.PARTY_ADDED, "Party added: " + partyDto.getPartyRole());
    log.info("Added party {} to case {}", saved.getId(), caseId);
    return toPartyDto(saved);
  }

  @Override
  @Transactional
  public void removeParty(UUID caseId, UUID partyId) {
    findOrThrow(caseId);
    CasePartyEntity party =
        casePartyRepository
            .findById(partyId)
            .orElseThrow(
                () -> new OpenDebtException("Party not found: " + partyId, "PARTY_NOT_FOUND"));
    casePartyRepository.delete(party);
    recordEvent(caseId, CaseEventType.PARTY_REMOVED, "Party removed: " + partyId);
    log.info("Removed party {} from case {}", partyId, caseId);
  }

  // ── Debt management ──────────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public List<CaseDebtDto> getDebts(UUID caseId) {
    findOrThrow(caseId);
    return caseDebtRepository.findByCaseIdAndRemovedAtIsNull(caseId).stream()
        .map(this::toDebtDto)
        .toList();
  }

  // ── Journal ──────────────────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public List<CaseJournalEntryDto> getJournalEntries(UUID caseId) {
    findOrThrow(caseId);
    return caseJournalEntryRepository.findByCaseIdOrderByJournalEntryTimeDesc(caseId).stream()
        .map(this::toJournalEntryDto)
        .toList();
  }

  @Override
  @Transactional
  public CaseJournalEntryDto addJournalEntry(UUID caseId, CaseJournalEntryDto entryDto) {
    findOrThrow(caseId);
    CaseJournalEntryEntity entity =
        CaseJournalEntryEntity.builder()
            .caseId(caseId)
            .journalEntryTitle(entryDto.getJournalEntryTitle())
            .journalEntryTime(
                entryDto.getJournalEntryTime() != null
                    ? entryDto.getJournalEntryTime()
                    : LocalDateTime.now())
            .documentId(entryDto.getDocumentId())
            .documentDirection(
                entryDto.getDocumentDirection() != null
                    ? DocumentDirection.valueOf(entryDto.getDocumentDirection())
                    : null)
            .documentType(entryDto.getDocumentType())
            .confidentialTitle(entryDto.getConfidentialTitle())
            .registeredBy(entryDto.getRegisteredBy())
            .build();
    CaseJournalEntryEntity saved = caseJournalEntryRepository.save(entity);
    recordEvent(
        caseId,
        CaseEventType.JOURNAL_ENTRY_ADDED,
        "Journal entry added: " + entryDto.getJournalEntryTitle());
    log.info("Added journal entry {} to case {}", saved.getId(), caseId);
    return toJournalEntryDto(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public List<CaseJournalNoteDto> getJournalNotes(UUID caseId) {
    findOrThrow(caseId);
    return caseJournalNoteRepository.findByCaseIdOrderByCreatedAtDesc(caseId).stream()
        .map(this::toJournalNoteDto)
        .toList();
  }

  @Override
  @Transactional
  public CaseJournalNoteDto addJournalNote(UUID caseId, CaseJournalNoteDto noteDto) {
    findOrThrow(caseId);
    CaseJournalNoteEntity entity =
        CaseJournalNoteEntity.builder()
            .caseId(caseId)
            .noteTitle(noteDto.getNoteTitle())
            .noteText(noteDto.getNoteText())
            .authorId(noteDto.getAuthorId())
            .build();
    CaseJournalNoteEntity saved = caseJournalNoteRepository.save(entity);
    recordEvent(caseId, CaseEventType.NOTE_ADDED, "Note added: " + noteDto.getNoteTitle());
    log.info("Added journal note {} to case {}", saved.getId(), caseId);
    return toJournalNoteDto(saved);
  }

  // ── Events ───────────────────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public List<CaseEventDto> getEvents(UUID caseId) {
    findOrThrow(caseId);
    return caseEventRepository.findByCaseIdOrderByPerformedAtDesc(caseId).stream()
        .map(this::toEventDto)
        .toList();
  }

  // ── State transitions ────────────────────────────────────────────────

  @Override
  @Transactional
  public CaseDto transitionState(UUID caseId, CaseState targetState, String performedBy) {
    CaseEntity entity = findOrThrow(caseId);
    CaseState current = entity.getCaseState();
    if (!current.canTransitionTo(targetState)) {
      throw new OpenDebtException(
          "Invalid transition from " + current + " to " + targetState, "INVALID_STATE_TRANSITION");
    }
    entity.setCaseState(targetState);
    entity.setStateChangedAt(LocalDateTime.now());
    caseRepository.save(entity);

    CaseEventEntity event =
        CaseEventEntity.builder()
            .caseId(caseId)
            .eventType(CaseEventType.STATE_CHANGED)
            .description("State changed from " + current + " to " + targetState)
            .metadata("{\"fromState\":\"" + current + "\",\"toState\":\"" + targetState + "\"}")
            .performedBy(performedBy)
            .performedAt(LocalDateTime.now())
            .build();
    caseEventRepository.save(event);

    log.info("Case {} transitioned from {} to {}", caseId, current, targetState);
    return toDto(entity);
  }

  // ── Collection measures ──────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public List<CollectionMeasureDto> getMeasures(UUID caseId) {
    findOrThrow(caseId);
    return collectionMeasureRepository.findByCaseId(caseId).stream()
        .map(this::toMeasureDto)
        .toList();
  }

  @Override
  @Transactional
  public CollectionMeasureDto addMeasure(UUID caseId, CollectionMeasureDto measureDto) {
    findOrThrow(caseId);
    CollectionMeasureEntity entity =
        CollectionMeasureEntity.builder()
            .caseId(caseId)
            .debtId(measureDto.getDebtId())
            .measureType(MeasureType.valueOf(measureDto.getMeasureType()))
            .status(
                measureDto.getStatus() != null
                    ? MeasureStatus.valueOf(measureDto.getStatus())
                    : MeasureStatus.PLANNED)
            .startDate(measureDto.getStartDate())
            .endDate(measureDto.getEndDate())
            .amount(measureDto.getAmount())
            .build();
    CollectionMeasureEntity saved = collectionMeasureRepository.save(entity);
    recordEvent(
        caseId,
        CaseEventType.COLLECTION_MEASURE_INITIATED,
        "Measure initiated: " + measureDto.getMeasureType());
    log.info("Added measure {} to case {}", saved.getId(), caseId);
    return toMeasureDto(saved);
  }

  // ── Objections ───────────────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public List<ObjectionDto> getObjections(UUID caseId) {
    findOrThrow(caseId);
    return objectionRepository.findByCaseId(caseId).stream().map(this::toObjectionDto).toList();
  }

  @Override
  @Transactional
  public ObjectionDto addObjection(UUID caseId, ObjectionDto objectionDto) {
    findOrThrow(caseId);
    ObjectionEntity entity =
        ObjectionEntity.builder()
            .caseId(caseId)
            .debtId(objectionDto.getDebtId())
            .objectionType(ObjectionType.valueOf(objectionDto.getObjectionType()))
            .status(ObjectionStatus.RECEIVED)
            .description(objectionDto.getDescription())
            .receivedAt(
                objectionDto.getReceivedAt() != null
                    ? objectionDto.getReceivedAt()
                    : LocalDateTime.now())
            .build();
    ObjectionEntity saved = objectionRepository.save(entity);
    recordEvent(
        caseId,
        CaseEventType.OBJECTION_RECEIVED,
        "Objection received: " + objectionDto.getObjectionType());
    log.info("Added objection {} to case {}", saved.getId(), caseId);
    return toObjectionDto(saved);
  }

  @Override
  @Transactional
  public ObjectionDto resolveObjection(UUID caseId, UUID objectionId, ObjectionDto resolution) {
    findOrThrow(caseId);
    ObjectionEntity entity =
        objectionRepository
            .findById(objectionId)
            .orElseThrow(
                () ->
                    new OpenDebtException(
                        "Objection not found: " + objectionId, "OBJECTION_NOT_FOUND"));
    entity.setStatus(ObjectionStatus.valueOf(resolution.getStatus()));
    entity.setCaseworkerAssessment(resolution.getDescription());
    entity.setResolvedAt(LocalDateTime.now());
    entity.setResolvedBy(SYSTEM_USER);
    ObjectionEntity saved = objectionRepository.save(entity);
    recordEvent(
        caseId, CaseEventType.HEARING_RESOLVED, "Objection resolved: " + resolution.getStatus());
    log.info("Resolved objection {} on case {}", objectionId, caseId);
    return toObjectionDto(saved);
  }

  // ── Legal bases ──────────────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public List<CaseLegalBasisDto> getLegalBases(UUID caseId) {
    findOrThrow(caseId);
    return caseLegalBasisRepository.findByCaseId(caseId).stream()
        .map(this::toLegalBasisDto)
        .toList();
  }

  @Override
  @Transactional
  public CaseLegalBasisDto addLegalBasis(UUID caseId, CaseLegalBasisDto basisDto) {
    findOrThrow(caseId);
    CaseLegalBasisEntity entity =
        CaseLegalBasisEntity.builder()
            .caseId(caseId)
            .legalSourceUri(basisDto.getLegalSourceUri())
            .legalSourceTitle(basisDto.getLegalSourceTitle())
            .paragraphReference(basisDto.getParagraphReference())
            .description(basisDto.getDescription())
            .build();
    CaseLegalBasisEntity saved = caseLegalBasisRepository.save(entity);
    log.info("Added legal basis {} to case {}", saved.getId(), caseId);
    return toLegalBasisDto(saved);
  }

  // ── Relations ────────────────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public List<CaseRelationDto> getRelations(UUID caseId) {
    findOrThrow(caseId);
    return caseRelationRepository.findBySourceCaseIdOrTargetCaseId(caseId, caseId).stream()
        .map(this::toRelationDto)
        .toList();
  }

  @Override
  @Transactional
  public CaseRelationDto addRelation(UUID caseId, CaseRelationDto relationDto) {
    findOrThrow(caseId);
    CaseRelationEntity entity =
        CaseRelationEntity.builder()
            .sourceCaseId(caseId)
            .targetCaseId(relationDto.getTargetCaseId())
            .relationType(CaseRelationType.valueOf(relationDto.getRelationType()))
            .description(relationDto.getDescription())
            .createdBy(relationDto.getCreatedBy())
            .build();
    CaseRelationEntity saved = caseRelationRepository.save(entity);
    log.info("Added relation {} to case {}", saved.getId(), caseId);
    return toRelationDto(saved);
  }

  // ── Private helpers ──────────────────────────────────────────────────

  private CaseEntity findOrThrow(UUID caseId) {
    return caseRepository
        .findById(caseId)
        .orElseThrow(
            () -> new OpenDebtException(CASE_NOT_FOUND_PREFIX + caseId, CASE_NOT_FOUND_CODE));
  }

  private void recordEvent(UUID caseId, CaseEventType eventType, String description) {
    CaseEventEntity event =
        CaseEventEntity.builder()
            .caseId(caseId)
            .eventType(eventType)
            .description(description)
            .performedBy(SYSTEM_USER)
            .performedAt(LocalDateTime.now())
            .build();
    caseEventRepository.save(event);
  }

  private CaseDto toDto(CaseEntity entity) {
    // Look up parties for the case
    List<CasePartyEntity> parties = casePartyRepository.findByCaseId(entity.getId());
    List<CasePartyDto> partyDtos =
        parties.stream()
            .map(
                p ->
                    CasePartyDto.builder()
                        .id(p.getId())
                        .caseId(p.getCaseId())
                        .personId(p.getPersonId())
                        .partyRole(p.getPartyRole().name())
                        .partyType(p.getPartyType().name())
                        .activeFrom(p.getActiveFrom())
                        .activeTo(p.getActiveTo())
                        .build())
            .toList();

    // Derive debtorId from PRIMARY_DEBTOR party
    String debtorId =
        parties.stream()
            .filter(p -> p.getPartyRole() == PartyRole.PRIMARY_DEBTOR)
            .map(p -> p.getPersonId().toString())
            .findFirst()
            .orElse(null);

    // Derive debtIds from case_debts (backward compat)
    List<UUID> debtIds =
        caseDebtRepository.findByCaseIdAndRemovedAtIsNull(entity.getId()).stream()
            .map(CaseDebtEntity::getDebtId)
            .toList();

    // Count open objections
    int openObjections =
        (int)
            objectionRepository.findByCaseId(entity.getId()).stream()
                .filter(
                    o ->
                        o.getStatus() == ObjectionStatus.RECEIVED
                            || o.getStatus() == ObjectionStatus.UNDER_REVIEW)
                .count();

    return CaseDto.builder()
        .id(entity.getId())
        .caseNumber(entity.getCaseNumber())
        .title(entity.getTitle())
        .caseState(CaseDto.CaseState.valueOf(entity.getCaseState().name()))
        .stateChangedAt(entity.getStateChangedAt())
        .caseType(entity.getCaseType() != null ? entity.getCaseType().name() : null)
        .description(entity.getDescription())
        .confidentialTitle(entity.getConfidentialTitle())
        .subjectClassification(entity.getSubjectClassification())
        .actionClassification(entity.getActionClassification())
        .precedentIndicator(entity.isPrecedentIndicator())
        .retentionOverride(entity.getRetentionOverride())
        .ownerOrganisationId(entity.getOwnerOrganisationId())
        .responsibleUnitId(entity.getResponsibleUnitId())
        .primaryCaseworkerId(entity.getPrimaryCaseworkerId())
        .parentCaseId(entity.getParentCaseId())
        .workflowProcessInstanceId(entity.getWorkflowProcessInstanceId())
        .parties(partyDtos)
        .openObjections(openObjections)
        // Backward-compatible fields
        .debtorId(debtorId)
        .debtIds(debtIds)
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getUpdatedAt())
        .build();
  }

  private CaseState mapCaseState(CaseDto.CaseState state) {
    return CaseState.valueOf(state.name());
  }

  private CasePartyDto toPartyDto(CasePartyEntity entity) {
    return CasePartyDto.builder()
        .id(entity.getId())
        .caseId(entity.getCaseId())
        .personId(entity.getPersonId())
        .partyRole(entity.getPartyRole().name())
        .partyType(entity.getPartyType().name())
        .activeFrom(entity.getActiveFrom())
        .activeTo(entity.getActiveTo())
        .build();
  }

  private CaseDebtDto toDebtDto(CaseDebtEntity entity) {
    return CaseDebtDto.builder()
        .id(entity.getId())
        .caseId(entity.getCaseId())
        .debtId(entity.getDebtId())
        .addedAt(entity.getAddedAt())
        .addedBy(entity.getAddedBy())
        .removedAt(entity.getRemovedAt())
        .removedBy(entity.getRemovedBy())
        .transferReference(entity.getTransferReference())
        .notes(entity.getNotes())
        .build();
  }

  private CaseJournalEntryDto toJournalEntryDto(CaseJournalEntryEntity entity) {
    return CaseJournalEntryDto.builder()
        .id(entity.getId())
        .caseId(entity.getCaseId())
        .journalEntryTitle(entity.getJournalEntryTitle())
        .journalEntryTime(entity.getJournalEntryTime())
        .documentId(entity.getDocumentId())
        .documentDirection(
            entity.getDocumentDirection() != null ? entity.getDocumentDirection().name() : null)
        .documentType(entity.getDocumentType())
        .confidentialTitle(entity.getConfidentialTitle())
        .registeredBy(entity.getRegisteredBy())
        .createdAt(entity.getCreatedAt())
        .build();
  }

  private CaseJournalNoteDto toJournalNoteDto(CaseJournalNoteEntity entity) {
    return CaseJournalNoteDto.builder()
        .id(entity.getId())
        .caseId(entity.getCaseId())
        .noteTitle(entity.getNoteTitle())
        .noteText(entity.getNoteText())
        .authorId(entity.getAuthorId())
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getUpdatedAt())
        .build();
  }

  private CaseEventDto toEventDto(CaseEventEntity entity) {
    return CaseEventDto.builder()
        .id(entity.getId())
        .caseId(entity.getCaseId())
        .eventType(entity.getEventType().name())
        .description(entity.getDescription())
        .metadata(entity.getMetadata())
        .performedBy(entity.getPerformedBy())
        .performedAt(entity.getPerformedAt())
        .build();
  }

  private CollectionMeasureDto toMeasureDto(CollectionMeasureEntity entity) {
    return CollectionMeasureDto.builder()
        .id(entity.getId())
        .caseId(entity.getCaseId())
        .debtId(entity.getDebtId())
        .measureType(entity.getMeasureType().name())
        .status(entity.getStatus().name())
        .startDate(entity.getStartDate())
        .endDate(entity.getEndDate())
        .amount(entity.getAmount())
        .build();
  }

  private ObjectionDto toObjectionDto(ObjectionEntity entity) {
    return ObjectionDto.builder()
        .id(entity.getId())
        .caseId(entity.getCaseId())
        .debtId(entity.getDebtId())
        .objectionType(entity.getObjectionType().name())
        .status(entity.getStatus().name())
        .description(entity.getDescription())
        .receivedAt(entity.getReceivedAt())
        .resolvedAt(entity.getResolvedAt())
        .build();
  }

  private CaseLegalBasisDto toLegalBasisDto(CaseLegalBasisEntity entity) {
    return CaseLegalBasisDto.builder()
        .id(entity.getId())
        .caseId(entity.getCaseId())
        .legalSourceUri(entity.getLegalSourceUri())
        .legalSourceTitle(entity.getLegalSourceTitle())
        .paragraphReference(entity.getParagraphReference())
        .description(entity.getDescription())
        .createdAt(entity.getCreatedAt())
        .build();
  }

  private CaseRelationDto toRelationDto(CaseRelationEntity entity) {
    return CaseRelationDto.builder()
        .id(entity.getId())
        .sourceCaseId(entity.getSourceCaseId())
        .targetCaseId(entity.getTargetCaseId())
        .relationType(entity.getRelationType().name())
        .description(entity.getDescription())
        .createdBy(entity.getCreatedBy())
        .createdAt(entity.getCreatedAt())
        .build();
  }
}
