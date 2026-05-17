package dk.ufst.opendebt.debtservice.limitation.service;

import java.time.LocalDate;
import java.util.UUID;

import dk.ufst.opendebt.debtservice.limitation.dto.CreateFordringskompleksRequest;
import dk.ufst.opendebt.debtservice.limitation.dto.ForaeldelseStatusDto;
import dk.ufst.opendebt.debtservice.limitation.dto.FordringskompleksMemberListDto;
import dk.ufst.opendebt.debtservice.limitation.dto.RegisterAfbrydelseRequest;
import dk.ufst.opendebt.debtservice.limitation.dto.RegisterTillaegsfristRequest;
import dk.ufst.opendebt.debtservice.limitation.entity.ObjectionStatus;
import dk.ufst.opendebt.debtservice.limitation.entity.Retsgrundlag;

public interface LimitationStateApplicationService {

  ForaeldelseStatusDto acceptClaim(
      UUID fordringId,
      UUID debtorPersonId,
      LocalDate registrationDate,
      String sourceSystem,
      Retsgrundlag retsgrundlag);

  ForaeldelseStatusDto acceptClaimFromEmptyComplex(
      UUID fordringId,
      UUID debtorPersonId,
      LocalDate registrationDate,
      String sourceSystem,
      Retsgrundlag retsgrundlag);

  ForaeldelseStatusDto getStatus(UUID fordringId);

  ForaeldelseStatusDto registerInterruption(UUID fordringId, RegisterAfbrydelseRequest request);

  ForaeldelseStatusDto registerSupplementaryPeriod(
      UUID fordringId, RegisterTillaegsfristRequest request);

  FordringskompleksMemberListDto createClaimComplex(CreateFordringskompleksRequest request);

  FordringskompleksMemberListDto addMemberToClaimComplex(UUID kompleksId, UUID fordringId);

  FordringskompleksMemberListDto getClaimComplexMembers(UUID kompleksId);

  UUID getDebtorPersonId(UUID fordringId);

  void markObjectionPending(UUID fordringId, UUID indsigelsesId, UUID workflowCaseId);

  ForaeldelseStatusDto resolveObjection(
      UUID fordringId, UUID indsigelsesId, ObjectionStatus status, String rationale);

  ForaeldelseStatusDto applyWageGarnishmentInactivityReset(
      UUID fordringId, LocalDate effectiveDate);
}
