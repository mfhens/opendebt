package dk.ufst.opendebt.caseservice.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import dk.ufst.opendebt.caseservice.entity.CaseState;
import dk.ufst.opendebt.common.dto.*;
import dk.ufst.opendebt.common.dto.AssignDebtToCaseRequest;
import dk.ufst.opendebt.common.dto.AssignDebtToCaseResponse;

public interface CaseService {

  Page<CaseDto> listCases(CaseDto.CaseState caseState, String caseworkerId, Pageable pageable);

  CaseDto getCaseById(UUID id);

  List<CaseDto> getCasesByDebtor(String debtorId);

  CaseDto createCase(CaseDto caseDto);

  CaseDto updateCase(UUID id, CaseDto caseDto);

  CaseDto assignCase(UUID id, String caseworkerId);

  CaseDto closeCase(UUID id, CaseDto.CaseState closureState);

  void addDebtToCase(UUID caseId, UUID debtId);

  AssignDebtToCaseResponse findOrCreateCaseForDebt(AssignDebtToCaseRequest request);

  void removeDebtFromCase(UUID caseId, UUID debtId);

  // Party management
  List<CasePartyDto> getParties(UUID caseId);

  CasePartyDto addParty(UUID caseId, CasePartyDto partyDto);

  void removeParty(UUID caseId, UUID partyId);

  // Debt management
  List<CaseDebtDto> getDebts(UUID caseId);

  // Journal
  List<CaseJournalEntryDto> getJournalEntries(UUID caseId);

  CaseJournalEntryDto addJournalEntry(UUID caseId, CaseJournalEntryDto entryDto);

  List<CaseJournalNoteDto> getJournalNotes(UUID caseId);

  CaseJournalNoteDto addJournalNote(UUID caseId, CaseJournalNoteDto noteDto);

  // Events
  List<CaseEventDto> getEvents(UUID caseId);

  // State transitions
  CaseDto transitionState(UUID caseId, CaseState targetState, String performedBy);

  // Collection measures
  List<CollectionMeasureDto> getMeasures(UUID caseId);

  CollectionMeasureDto addMeasure(UUID caseId, CollectionMeasureDto measureDto);

  // Objections
  List<ObjectionDto> getObjections(UUID caseId);

  ObjectionDto addObjection(UUID caseId, ObjectionDto objectionDto);

  ObjectionDto resolveObjection(UUID caseId, UUID objectionId, ObjectionDto resolution);

  // Legal bases
  List<CaseLegalBasisDto> getLegalBases(UUID caseId);

  CaseLegalBasisDto addLegalBasis(UUID caseId, CaseLegalBasisDto basisDto);

  // Relations
  List<CaseRelationDto> getRelations(UUID caseId);

  CaseRelationDto addRelation(UUID caseId, CaseRelationDto relationDto);
}
