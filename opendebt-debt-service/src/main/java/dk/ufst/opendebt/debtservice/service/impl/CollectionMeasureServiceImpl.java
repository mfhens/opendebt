package dk.ufst.opendebt.debtservice.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.ufst.opendebt.debtservice.dto.CollectionMeasureDto;
import dk.ufst.opendebt.debtservice.entity.ClaimLifecycleState;
import dk.ufst.opendebt.debtservice.entity.CollectionMeasureEntity;
import dk.ufst.opendebt.debtservice.entity.CollectionMeasureEntity.MeasureStatus;
import dk.ufst.opendebt.debtservice.entity.CollectionMeasureEntity.MeasureType;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.repository.CollectionMeasureRepository;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;
import dk.ufst.opendebt.debtservice.service.CollectionMeasureService;
import dk.ufst.opendebt.debtservice.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectionMeasureServiceImpl implements CollectionMeasureService {

  private final CollectionMeasureRepository measureRepository;
  private final DebtRepository debtRepository;
  private final NotificationService notificationService;

  @Override
  @Transactional
  public CollectionMeasureDto initiateMeasure(
      UUID debtId, MeasureType type, BigDecimal amount, String note) {

    DebtEntity debt =
        debtRepository
            .findById(debtId)
            .orElseThrow(() -> new IllegalArgumentException("Debt not found: " + debtId));

    if (debt.getLifecycleState() != ClaimLifecycleState.OVERDRAGET) {
      throw new IllegalStateException(
          "Collection measures require OVERDRAGET state. Current: " + debt.getLifecycleState());
    }

    CollectionMeasureEntity entity =
        CollectionMeasureEntity.builder()
            .debtId(debtId)
            .measureType(type)
            .status(MeasureStatus.INITIATED)
            .initiatedAt(Instant.now())
            .amount(amount)
            .note(note)
            .build();

    entity = measureRepository.save(entity);

    log.info("Initiated {} for debt={}, measure={}", type, debtId, entity.getId());

    // G.A.3.1.4: Notify debtor of modregning (SET_OFF) obligation.
    if (type == MeasureType.SET_OFF) {
      notificationService.notifyModregning(debtId, amount);
    }

    return toDto(entity);
  }

  @Override
  @Transactional
  public CollectionMeasureDto completeMeasure(UUID measureId) {
    CollectionMeasureEntity entity = findMeasureOrThrow(measureId);

    if (entity.getStatus() != MeasureStatus.INITIATED
        && entity.getStatus() != MeasureStatus.IN_PROGRESS) {
      throw new IllegalStateException("Cannot complete measure in status: " + entity.getStatus());
    }

    entity.setStatus(MeasureStatus.COMPLETED);
    entity.setCompletedAt(Instant.now());
    measureRepository.save(entity);

    log.info("Completed measure={} for debt={}", measureId, entity.getDebtId());

    return toDto(entity);
  }

  @Override
  @Transactional
  public CollectionMeasureDto cancelMeasure(UUID measureId, String reason) {
    CollectionMeasureEntity entity = findMeasureOrThrow(measureId);

    if (entity.getStatus() == MeasureStatus.COMPLETED
        || entity.getStatus() == MeasureStatus.CANCELLED) {
      throw new IllegalStateException("Cannot cancel measure in status: " + entity.getStatus());
    }

    entity.setStatus(MeasureStatus.CANCELLED);
    entity.setCompletedAt(Instant.now());
    entity.setNote(reason);
    measureRepository.save(entity);

    log.info("Cancelled measure={} for debt={}, reason={}", measureId, entity.getDebtId(), reason);

    return toDto(entity);
  }

  @Override
  @Transactional(readOnly = true)
  public List<CollectionMeasureDto> getMeasures(UUID debtId) {
    return measureRepository.findByDebtIdOrderByInitiatedAtDesc(debtId).stream()
        .map(this::toDto)
        .toList();
  }

  private CollectionMeasureEntity findMeasureOrThrow(UUID measureId) {
    return measureRepository
        .findById(measureId)
        .orElseThrow(
            () -> new IllegalArgumentException("Collection measure not found: " + measureId));
  }

  private CollectionMeasureDto toDto(CollectionMeasureEntity entity) {
    return CollectionMeasureDto.builder()
        .id(entity.getId())
        .debtId(entity.getDebtId())
        .measureType(entity.getMeasureType().name())
        .status(entity.getStatus().name())
        .initiatedBy(entity.getInitiatedBy())
        .initiatedAt(entity.getInitiatedAt())
        .completedAt(entity.getCompletedAt())
        .amount(entity.getAmount())
        .note(entity.getNote())
        .build();
  }
}
