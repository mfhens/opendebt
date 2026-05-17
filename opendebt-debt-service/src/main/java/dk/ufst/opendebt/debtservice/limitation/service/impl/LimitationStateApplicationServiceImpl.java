package dk.ufst.opendebt.debtservice.limitation.service.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import dk.ufst.opendebt.debtservice.limitation.client.WageGarnishmentFactClient;
import dk.ufst.opendebt.debtservice.limitation.client.dto.WageGarnishmentLimitationFacts;
import dk.ufst.opendebt.debtservice.limitation.dto.CreateFordringskompleksRequest;
import dk.ufst.opendebt.debtservice.limitation.dto.ForaeldelseStatusDto;
import dk.ufst.opendebt.debtservice.limitation.dto.FordringskompleksMemberListDto;
import dk.ufst.opendebt.debtservice.limitation.dto.RegisterAfbrydelseRequest;
import dk.ufst.opendebt.debtservice.limitation.dto.RegisterTillaegsfristRequest;
import dk.ufst.opendebt.debtservice.limitation.entity.AfbrydelseEvent;
import dk.ufst.opendebt.debtservice.limitation.entity.AfbrydelsesType;
import dk.ufst.opendebt.debtservice.limitation.entity.ForaeldelseRecord;
import dk.ufst.opendebt.debtservice.limitation.entity.ForaeldelseStatus;
import dk.ufst.opendebt.debtservice.limitation.entity.LimitationObjectionLinkage;
import dk.ufst.opendebt.debtservice.limitation.entity.ObjectionStatus;
import dk.ufst.opendebt.debtservice.limitation.entity.Retsgrundlag;
import dk.ufst.opendebt.debtservice.limitation.entity.TillaegsfristEvent;
import dk.ufst.opendebt.debtservice.limitation.repository.AfbrydelseEventRepository;
import dk.ufst.opendebt.debtservice.limitation.repository.ForaeldelseRecordRepository;
import dk.ufst.opendebt.debtservice.limitation.repository.LimitationObjectionLinkageRepository;
import dk.ufst.opendebt.debtservice.limitation.repository.TillaegsfristEventRepository;
import dk.ufst.opendebt.debtservice.limitation.service.ClaimComplexManager;
import dk.ufst.opendebt.debtservice.limitation.service.LimitationAuditPublisher;
import dk.ufst.opendebt.debtservice.limitation.service.LimitationPolicyEngine;
import dk.ufst.opendebt.debtservice.limitation.service.LimitationStateApplicationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LimitationStateApplicationServiceImpl implements LimitationStateApplicationService {

  private final ForaeldelseRecordRepository recordRepository;
  private final AfbrydelseEventRepository afbrydelseEventRepository;
  private final TillaegsfristEventRepository tillaegsfristEventRepository;
  private final LimitationObjectionLinkageRepository objectionLinkageRepository;
  private final LimitationPolicyEngine policyEngine;
  private final ClaimComplexManager claimComplexManager;
  private final WageGarnishmentFactClient wageGarnishmentFactClient;
  private final LimitationAuditPublisher auditPublisher;

  @Override
  @Transactional
  public ForaeldelseStatusDto acceptClaim(
      UUID fordringId,
      UUID debtorPersonId,
      LocalDate registrationDate,
      String sourceSystem,
      Retsgrundlag retsgrundlag) {
    LocalDate udskydelseDato =
        policyEngine.determinePostponementDate(registrationDate, sourceSystem);
    ForaeldelseRecord record =
        recordRepository
            .findByFordringId(fordringId)
            .orElse(
                ForaeldelseRecord.builder().id(UUID.randomUUID()).fordringId(fordringId).build());
    record.setDebtorPersonId(debtorPersonId);
    record.setRetsgrundlag(retsgrundlag);
    if (record.getUdskydelseDato() == null) {
      record.setUdskydelseDato(udskydelseDato);
    }
    record.setInUdskydelse(policyEngine.isInUdskydelse(record.getUdskydelseDato()));
    if (record.getCurrentFristExpires() == null) {
      record.setCurrentFristExpires(
          policyEngine.calculateInitialExpiry(
              registrationDate, retsgrundlag, record.getUdskydelseDato()));
    }
    record.setStatus(
        policyEngine.deriveCurrentStatus(
            ForaeldelseStatus.ACTIVE, record.getCurrentFristExpires()));
    recordRepository.save(record);
    return toDto(record);
  }

  @Override
  @Transactional
  public ForaeldelseStatusDto acceptClaimFromEmptyComplex(
      UUID fordringId,
      UUID debtorPersonId,
      LocalDate registrationDate,
      String sourceSystem,
      Retsgrundlag retsgrundlag) {
    acceptClaim(fordringId, debtorPersonId, registrationDate, sourceSystem, retsgrundlag);
    ForaeldelseRecord record = requireRecord(fordringId);
    LocalDate newExpiry =
        policyEngine.calculateInterruptedExpiry(
            AfbrydelsesType.BEROSTILLELSE, retsgrundlag, registrationDate);
    persistInterruption(
        record,
        AfbrydelsesType.BEROSTILLELSE,
        registrationDate,
        newExpiry,
        "GIL § 18a, stk. 7",
        null,
        null,
        null,
        false);
    return toDto(recordRepository.save(record));
  }

  @Override
  public ForaeldelseStatusDto getStatus(UUID fordringId) {
    ForaeldelseRecord record = requireRecord(fordringId);
    record.setInUdskydelse(policyEngine.isInUdskydelse(record.getUdskydelseDato()));
    record.setStatus(
        policyEngine.deriveCurrentStatus(record.getStatus(), record.getCurrentFristExpires()));
    recordRepository.save(record);
    return toDto(record);
  }

  @Override
  @Transactional
  public ForaeldelseStatusDto registerInterruption(
      UUID fordringId, RegisterAfbrydelseRequest request) {
    ForaeldelseRecord record = requireRecord(fordringId);
    AfbrydelsesType type = parseType(request.getType());
    LocalDate eventDate = requireEventDate(request.getEventDate());

    if (type == AfbrydelsesType.LOENINDEHOLDELSE) {
      if (!Boolean.TRUE.equals(request.getAfgoerelseRegistreret())) {
        throw new ResponseStatusException(
            HttpStatus.UNPROCESSABLE_ENTITY, "Varsel alone does not interrupt limitation");
      }
      WageGarnishmentLimitationFacts facts = wageFacts(record.getDebtorPersonId());
      LocalDate notificationDate =
          facts.getDebtorNotificationDate() != null ? facts.getDebtorNotificationDate() : eventDate;
      LocalDate newExpiry =
          policyEngine.calculateInterruptedExpiry(type, record.getRetsgrundlag(), notificationDate);
      List<UUID> affectedIds =
          facts.getCoveredFordringIds() == null || facts.getCoveredFordringIds().isEmpty()
              ? List.of(record.getFordringId())
              : facts.getCoveredFordringIds();
      for (UUID affectedId : affectedIds) {
        ForaeldelseRecord affectedRecord = requireRecord(affectedId);
        persistInterruption(
            affectedRecord,
            type,
            notificationDate,
            newExpiry,
            requestedLegalReference(
                request.getLegalReference(), type, affectedRecord.getRetsgrundlag()),
            null,
            null,
            null,
            false);
      }
      return getStatus(fordringId);
    }

    LocalDate newExpiry =
        policyEngine.calculateInterruptedExpiry(type, record.getRetsgrundlag(), eventDate);
    List<ForaeldelseRecord> affectedRecords = claimComplexManager.getAffectedRecords(record);
    UUID sourceFordringId = record.getFordringId();
    for (ForaeldelseRecord affectedRecord : affectedRecords) {
      boolean propagated = !affectedRecord.getFordringId().equals(sourceFordringId);
      persistInterruption(
          affectedRecord,
          type,
          eventDate,
          newExpiry,
          propagated
              ? propagatedLegalReference(type, affectedRecord.getRetsgrundlag())
              : requestedLegalReference(
                  request.getLegalReference(), type, affectedRecord.getRetsgrundlag()),
          propagated ? sourceFordringId : null,
          propagated ? affectedRecord.getFordringId() : null,
          propagated ? "FORDRINGSKOMPLEKS_PROPAGATION" : null,
          propagated);
    }
    return getStatus(fordringId);
  }

  @Override
  @Transactional
  public ForaeldelseStatusDto registerSupplementaryPeriod(
      UUID fordringId, RegisterTillaegsfristRequest request) {
    ForaeldelseRecord record = requireRecord(fordringId);
    if (request.getAppliedDate() == null) {
      throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Missing appliedDate");
    }
    LocalDate newExpiry =
        policyEngine.calculateSupplementaryExpiry(
            record.getCurrentFristExpires(), request.getAppliedDate());
    record.setCurrentFristExpires(newExpiry);
    record.setStatus(policyEngine.deriveCurrentStatus(record.getStatus(), newExpiry));
    recordRepository.save(record);
    TillaegsfristEvent event =
        tillaegsfristEventRepository.save(
            TillaegsfristEvent.builder()
                .id(UUID.randomUUID())
                .fordringId(fordringId)
                .type(request.getType())
                .appliedDate(request.getAppliedDate())
                .extensionYears(2)
                .newFristExpires(newExpiry)
                .legalReference(
                    request.getLegalReference() != null
                        ? request.getLegalReference()
                        : "G.A.2.4.4.2")
                .build());
    auditPublisher.publishSupplementary(event);
    return toDto(record);
  }

  @Override
  @Transactional
  public FordringskompleksMemberListDto createClaimComplex(CreateFordringskompleksRequest request) {
    return claimComplexManager.createComplex(request);
  }

  @Override
  @Transactional
  public FordringskompleksMemberListDto addMemberToClaimComplex(UUID kompleksId, UUID fordringId) {
    claimComplexManager.addMember(kompleksId, fordringId);
    return claimComplexManager.getMembers(kompleksId);
  }

  @Override
  public FordringskompleksMemberListDto getClaimComplexMembers(UUID kompleksId) {
    return claimComplexManager.getMembers(kompleksId);
  }

  @Override
  public UUID getDebtorPersonId(UUID fordringId) {
    return requireRecord(fordringId).getDebtorPersonId();
  }

  @Override
  @Transactional
  public void markObjectionPending(UUID fordringId, UUID indsigelsesId, UUID workflowCaseId) {
    ForaeldelseRecord record = requireRecord(fordringId);
    record.setStatus(ForaeldelseStatus.INDSIGELSE_PENDING);
    recordRepository.save(record);
    objectionLinkageRepository.save(
        LimitationObjectionLinkage.builder()
            .id(UUID.randomUUID())
            .fordringId(fordringId)
            .indsigelsesId(indsigelsesId)
            .workflowCaseId(workflowCaseId)
            .status(ObjectionStatus.INDSIGELSE_PENDING)
            .build());
  }

  @Override
  @Transactional
  public ForaeldelseStatusDto resolveObjection(
      UUID fordringId, UUID indsigelsesId, ObjectionStatus status, String rationale) {
    ForaeldelseRecord record = requireRecord(fordringId);
    LimitationObjectionLinkage linkage =
        objectionLinkageRepository
            .findByIndsigelsesId(indsigelsesId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown indsigelsesId"));
    linkage.setStatus(status);
    linkage.setRationale(rationale);
    objectionLinkageRepository.save(linkage);
    record.setStatus(
        status == ObjectionStatus.FORAELDET
            ? ForaeldelseStatus.FORAELDET
            : ForaeldelseStatus.ACTIVE);
    recordRepository.save(record);
    return toDto(record);
  }

  @Override
  @Transactional
  public ForaeldelseStatusDto applyWageGarnishmentInactivityReset(
      UUID fordringId, LocalDate effectiveDate) {
    ForaeldelseRecord record = requireRecord(fordringId);
    WageGarnishmentLimitationFacts facts = wageFacts(record.getDebtorPersonId());
    if (facts.getInactiveSince() == null
        || facts.getInactiveSince().plusYears(1).isAfter(effectiveDate)) {
      return toDto(record);
    }
    record.setCurrentFristExpires(effectiveDate.plusYears(3));
    record.setStatus(
        policyEngine.deriveCurrentStatus(record.getStatus(), record.getCurrentFristExpires()));
    recordRepository.save(record);
    return toDto(record);
  }

  private void persistInterruption(
      ForaeldelseRecord record,
      AfbrydelsesType type,
      LocalDate eventDate,
      LocalDate newExpiry,
      String legalReference,
      UUID sourceFordringId,
      UUID targetFordringId,
      String propagationReason,
      boolean propagated) {
    record.setCurrentFristExpires(newExpiry);
    record.setInUdskydelse(policyEngine.isInUdskydelse(record.getUdskydelseDato()));
    record.setStatus(policyEngine.deriveCurrentStatus(ForaeldelseStatus.ACTIVE, newExpiry));
    recordRepository.save(record);
    AfbrydelseEvent event =
        afbrydelseEventRepository.save(
            AfbrydelseEvent.builder()
                .id(UUID.randomUUID())
                .fordringId(record.getFordringId())
                .type(type)
                .eventDate(eventDate)
                .legalReference(legalReference)
                .newFristExpires(newExpiry)
                .sourceFordringId(sourceFordringId)
                .targetFordringId(targetFordringId)
                .propagationReason(propagationReason)
                .build());
    auditPublisher.publishInterruption(
        event, propagated ? "afbrydelsepropagering" : "afbrydelsesregistrering for " + type.name());
  }

  private ForaeldelseRecord requireRecord(UUID fordringId) {
    return recordRepository
        .findByFordringId(fordringId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown fordringId"));
  }

  private AfbrydelsesType parseType(String type) {
    try {
      return AfbrydelsesType.valueOf(type);
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Unknown afbrydelse type");
    }
  }

  private LocalDate requireEventDate(LocalDate eventDate) {
    if (eventDate == null) {
      throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Missing eventDate");
    }
    return eventDate;
  }

  private WageGarnishmentLimitationFacts wageFacts(UUID debtorPersonId) {
    WageGarnishmentLimitationFacts facts = wageGarnishmentFactClient.getFacts(debtorPersonId);
    return facts == null
        ? WageGarnishmentLimitationFacts.builder()
            .debtorPersonId(debtorPersonId)
            .decisionRegistered(false)
            .build()
        : facts;
  }

  private String requestedLegalReference(
      String requestedLegalReference, AfbrydelsesType type, Retsgrundlag retsgrundlag) {
    return requestedLegalReference != null
        ? requestedLegalReference
        : legalReference(type, retsgrundlag);
  }

  private String legalReference(AfbrydelsesType type, Retsgrundlag retsgrundlag) {
    return switch (type) {
      case BEROSTILLELSE -> "GIL § 18a, stk. 8";
      case UDLAEG -> "Forældelsesl. § 18, stk. 1";
      case MODREGNING -> "Forældelsesl. § 18, stk. 4";
      case LOENINDEHOLDELSE -> "GIL § 18, stk. 4 + Forældelsesl. § 18, stk. 4";
    };
  }

  private String propagatedLegalReference(AfbrydelsesType type, Retsgrundlag retsgrundlag) {
    return switch (type) {
      case BEROSTILLELSE -> "GIL § 18a, stk. 2";
      default -> legalReference(type, retsgrundlag);
    };
  }

  private ForaeldelseStatusDto toDto(ForaeldelseRecord record) {
    List<ForaeldelseStatusDto.AfbrydelseHistoryEntryDto> afbrydelseHistory =
        afbrydelseEventRepository
            .findByFordringIdOrderByEventDateAsc(record.getFordringId())
            .stream()
            .map(
                event ->
                    ForaeldelseStatusDto.AfbrydelseHistoryEntryDto.builder()
                        .type(event.getType().name())
                        .eventDate(event.getEventDate())
                        .legalReference(event.getLegalReference())
                        .newFristExpires(event.getNewFristExpires())
                        .sourceFordringId(event.getSourceFordringId())
                        .targetFordringId(event.getTargetFordringId())
                        .propagationReason(event.getPropagationReason())
                        .build())
            .toList();
    List<ForaeldelseStatusDto.TillaegsfristHistoryEntryDto> tillaegsfristHistory =
        tillaegsfristEventRepository
            .findByFordringIdOrderByAppliedDateAsc(record.getFordringId())
            .stream()
            .map(
                event ->
                    ForaeldelseStatusDto.TillaegsfristHistoryEntryDto.builder()
                        .type(event.getType())
                        .appliedDate(event.getAppliedDate())
                        .extensionYears(event.getExtensionYears())
                        .newFristExpires(event.getNewFristExpires())
                        .legalReference(event.getLegalReference())
                        .build())
            .toList();
    String objectionRationale =
        objectionLinkageRepository
            .findByFordringId(record.getFordringId())
            .map(LimitationObjectionLinkage::getRationale)
            .orElse(null);
    return ForaeldelseStatusDto.builder()
        .fordringId(record.getFordringId())
        .currentFristExpires(record.getCurrentFristExpires())
        .udskydelseDato(record.getUdskydelseDato())
        .isInUdskydelse(record.isInUdskydelse())
        .retsgrundlag(record.getRetsgrundlag())
        .afbrydelseHistory(afbrydelseHistory)
        .tillaegsfristHistory(tillaegsfristHistory)
        .status(record.getStatus())
        .kompleksId(record.getKompleksId())
        .objectionRationale(objectionRationale)
        .build();
  }
}
