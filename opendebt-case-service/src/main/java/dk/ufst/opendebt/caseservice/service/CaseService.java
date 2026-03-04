package dk.ufst.opendebt.caseservice.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import dk.ufst.opendebt.common.dto.CaseDto;

public interface CaseService {

  Page<CaseDto> listCases(CaseDto.CaseStatus status, String caseworkerId, Pageable pageable);

  CaseDto getCaseById(UUID id);

  List<CaseDto> getCasesByDebtor(String debtorId);

  CaseDto createCase(CaseDto caseDto);

  CaseDto updateCase(UUID id, CaseDto caseDto);

  CaseDto assignCase(UUID id, String caseworkerId);

  CaseDto setCollectionStrategy(UUID id, CaseDto.CollectionStrategy strategy);

  CaseDto closeCase(UUID id, CaseDto.CaseStatus closureStatus);

  void addDebtToCase(UUID caseId, UUID debtId);

  void removeDebtFromCase(UUID caseId, UUID debtId);
}
