package dk.ufst.opendebt.debtservice.limitation.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import dk.ufst.opendebt.debtservice.limitation.dto.CreateFordringskompleksRequest;
import dk.ufst.opendebt.debtservice.limitation.dto.FordringskompleksMemberListDto;
import dk.ufst.opendebt.debtservice.limitation.entity.ForaeldelseRecord;
import dk.ufst.opendebt.debtservice.limitation.entity.FordringskompleksLink;
import dk.ufst.opendebt.debtservice.limitation.repository.ForaeldelseRecordRepository;
import dk.ufst.opendebt.debtservice.limitation.repository.FordringskompleksLinkRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ClaimComplexManager {

  private final FordringskompleksLinkRepository linkRepository;
  private final ForaeldelseRecordRepository recordRepository;

  public FordringskompleksMemberListDto createComplex(CreateFordringskompleksRequest request) {
    UUID kompleksId = UUID.randomUUID();
    List<UUID> members =
        request.getMemberFordringIds() == null ? List.of() : request.getMemberFordringIds();
    for (UUID memberId : members) {
      ForaeldelseRecord record = requireRecord(memberId);
      record.setKompleksId(kompleksId);
      recordRepository.save(record);
      linkRepository.save(
          FordringskompleksLink.builder()
              .kompleksId(kompleksId)
              .fordringId(memberId)
              .linkedAt(LocalDateTime.now())
              .build());
    }
    return FordringskompleksMemberListDto.builder()
        .kompleksId(kompleksId)
        .memberFordringIds(members)
        .build();
  }

  public void addMember(UUID kompleksId, UUID fordringId) {
    ForaeldelseRecord record = requireRecord(fordringId);
    record.setKompleksId(kompleksId);
    recordRepository.save(record);
    if (!linkRepository.existsById(
        new dk.ufst.opendebt.debtservice.limitation.entity.FordringskompleksLinkId(
            kompleksId, fordringId))) {
      linkRepository.save(
          FordringskompleksLink.builder()
              .kompleksId(kompleksId)
              .fordringId(fordringId)
              .linkedAt(LocalDateTime.now())
              .build());
    }
  }

  public FordringskompleksMemberListDto getMembers(UUID kompleksId) {
    List<UUID> members =
        linkRepository.findByKompleksId(kompleksId).stream()
            .map(FordringskompleksLink::getFordringId)
            .sorted(Comparator.comparing(UUID::toString))
            .toList();
    return FordringskompleksMemberListDto.builder()
        .kompleksId(kompleksId)
        .memberFordringIds(members)
        .build();
  }

  public List<ForaeldelseRecord> getAffectedRecords(ForaeldelseRecord originRecord) {
    if (originRecord.getKompleksId() == null) {
      return List.of(originRecord);
    }
    List<UUID> memberIds =
        linkRepository.findByKompleksId(originRecord.getKompleksId()).stream()
            .map(FordringskompleksLink::getFordringId)
            .toList();
    if (memberIds.isEmpty()) {
      return List.of(originRecord);
    }
    return memberIds.stream().map(this::requireRecord).toList();
  }

  private ForaeldelseRecord requireRecord(UUID fordringId) {
    return recordRepository
        .findByFordringId(fordringId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown fordringId"));
  }
}
