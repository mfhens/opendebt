package dk.ufst.opendebt.debtservice.service.impl;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.ufst.opendebt.debtservice.dto.ObjectionDto;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.entity.ObjectionEntity;
import dk.ufst.opendebt.debtservice.entity.ObjectionEntity.ObjectionStatus;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;
import dk.ufst.opendebt.debtservice.repository.ObjectionRepository;
import dk.ufst.opendebt.debtservice.service.ObjectionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ObjectionServiceImpl implements ObjectionService {

  private final ObjectionRepository objectionRepository;
  private final DebtRepository debtRepository;

  @Override
  @Transactional
  public ObjectionDto registerObjection(UUID debtId, UUID debtorPersonId, String reason) {
    if (!debtRepository.existsById(debtId)) {
      throw new IllegalArgumentException("Debt not found: " + debtId);
    }

    if (objectionRepository.existsByDebtIdAndStatus(debtId, ObjectionStatus.ACTIVE)) {
      throw new IllegalStateException("Active objection already exists for debt: " + debtId);
    }

    DebtEntity debt = debtRepository.findById(debtId).orElseThrow();
    debt.setReadinessStatus(DebtEntity.ReadinessStatus.UNDER_APPEAL);
    debtRepository.save(debt);

    ObjectionEntity entity =
        ObjectionEntity.builder()
            .debtId(debtId)
            .debtorPersonId(debtorPersonId)
            .reason(reason)
            .status(ObjectionStatus.ACTIVE)
            .registeredAt(Instant.now())
            .build();

    entity = objectionRepository.save(entity);

    log.info(
        "Registered objection for debt={}, debtor={}, objection={}",
        debtId,
        debtorPersonId,
        entity.getId());

    return toDto(entity);
  }

  @Override
  @Transactional
  public ObjectionDto resolveObjection(UUID objectionId, ObjectionStatus outcome, String note) {
    if (outcome == ObjectionStatus.ACTIVE) {
      throw new IllegalArgumentException("Outcome must be UPHELD or REJECTED, not ACTIVE");
    }

    ObjectionEntity entity =
        objectionRepository
            .findById(objectionId)
            .orElseThrow(() -> new IllegalArgumentException("Objection not found: " + objectionId));

    if (entity.getStatus() != ObjectionStatus.ACTIVE) {
      throw new IllegalStateException(
          "Objection is already resolved with status: " + entity.getStatus());
    }

    entity.setStatus(outcome);
    entity.setResolvedAt(Instant.now());
    entity.setResolutionNote(note);
    objectionRepository.save(entity);

    DebtEntity debt = debtRepository.findById(entity.getDebtId()).orElseThrow();
    if (outcome == ObjectionStatus.REJECTED) {
      debt.setReadinessStatus(DebtEntity.ReadinessStatus.READY_FOR_COLLECTION);
    }
    debtRepository.save(debt);

    log.info(
        "Resolved objection={} for debt={} with outcome={}",
        objectionId,
        entity.getDebtId(),
        outcome);

    return toDto(entity);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean hasActiveObjection(UUID debtId) {
    return objectionRepository.existsByDebtIdAndStatus(debtId, ObjectionStatus.ACTIVE);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ObjectionDto> getObjections(UUID debtId) {
    return objectionRepository.findByDebtIdOrderByRegisteredAtDesc(debtId).stream()
        .map(this::toDto)
        .toList();
  }

  private ObjectionDto toDto(ObjectionEntity entity) {
    return ObjectionDto.builder()
        .id(entity.getId())
        .debtId(entity.getDebtId())
        .debtorPersonId(entity.getDebtorPersonId())
        .reason(entity.getReason())
        .status(entity.getStatus().name())
        .registeredAt(entity.getRegisteredAt())
        .resolvedAt(entity.getResolvedAt())
        .resolutionNote(entity.getResolutionNote())
        .build();
  }
}
