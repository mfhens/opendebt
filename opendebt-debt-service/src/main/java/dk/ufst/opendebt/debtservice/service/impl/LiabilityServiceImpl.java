package dk.ufst.opendebt.debtservice.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.ufst.opendebt.debtservice.dto.LiabilityDto;
import dk.ufst.opendebt.debtservice.entity.LiabilityEntity;
import dk.ufst.opendebt.debtservice.entity.LiabilityEntity.LiabilityType;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;
import dk.ufst.opendebt.debtservice.repository.LiabilityRepository;
import dk.ufst.opendebt.debtservice.service.LiabilityService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LiabilityServiceImpl implements LiabilityService {

  private final LiabilityRepository liabilityRepository;
  private final DebtRepository debtRepository;

  @Override
  @Transactional
  public LiabilityDto addLiability(
      UUID debtId, UUID debtorPersonId, LiabilityType type, BigDecimal sharePercentage) {

    if (!debtRepository.existsById(debtId)) {
      throw new IllegalArgumentException("Debt not found: " + debtId);
    }

    if (liabilityRepository.existsByDebtIdAndDebtorPersonId(debtId, debtorPersonId)) {
      throw new IllegalStateException(
          "Liability already exists for debt=" + debtId + " and debtor=" + debtorPersonId);
    }

    validateLiabilityRules(debtId, type, sharePercentage);

    LiabilityEntity entity =
        LiabilityEntity.builder()
            .debtId(debtId)
            .debtorPersonId(debtorPersonId)
            .liabilityType(type)
            .sharePercentage(type == LiabilityType.PROPORTIONAL ? sharePercentage : null)
            .active(true)
            .build();

    entity = liabilityRepository.save(entity);

    log.info(
        "Added {} liability for debt={}, debtor={}, id={}",
        type,
        debtId,
        debtorPersonId,
        entity.getId());

    return toDto(entity);
  }

  @Override
  @Transactional
  public void removeLiability(UUID liabilityId) {
    LiabilityEntity entity =
        liabilityRepository
            .findById(liabilityId)
            .orElseThrow(() -> new IllegalArgumentException("Liability not found: " + liabilityId));

    entity.setActive(false);
    liabilityRepository.save(entity);

    log.info("Deactivated liability id={}, debt={}", liabilityId, entity.getDebtId());
  }

  @Override
  @Transactional(readOnly = true)
  public List<LiabilityDto> getLiabilities(UUID debtId) {
    return liabilityRepository.findByDebtIdAndActiveTrue(debtId).stream().map(this::toDto).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<LiabilityDto> getDebtorLiabilities(UUID debtorPersonId) {
    return liabilityRepository.findByDebtorPersonIdAndActiveTrue(debtorPersonId).stream()
        .map(this::toDto)
        .toList();
  }

  private void validateLiabilityRules(UUID debtId, LiabilityType type, BigDecimal sharePercentage) {
    List<LiabilityEntity> existing = liabilityRepository.findByDebtIdAndActiveTrue(debtId);

    if (type == LiabilityType.SOLE && !existing.isEmpty()) {
      throw new IllegalStateException("SOLE liability requires exactly one liable party");
    }

    if (!existing.isEmpty()) {
      LiabilityType existingType = existing.get(0).getLiabilityType();
      if (existingType != type) {
        throw new IllegalStateException(
            "Cannot mix liability types on same debt. Existing: "
                + existingType
                + ", new: "
                + type);
      }
    }

    if (type == LiabilityType.PROPORTIONAL) {
      if (sharePercentage == null
          || sharePercentage.compareTo(BigDecimal.ZERO) <= 0
          || sharePercentage.compareTo(new BigDecimal("100")) > 0) {
        throw new IllegalArgumentException(
            "PROPORTIONAL liability requires sharePercentage between 0 and 100");
      }

      BigDecimal totalExisting =
          existing.stream()
              .map(e -> e.getSharePercentage() != null ? e.getSharePercentage() : BigDecimal.ZERO)
              .reduce(BigDecimal.ZERO, BigDecimal::add);

      if (totalExisting.add(sharePercentage).compareTo(new BigDecimal("100")) > 0) {
        throw new IllegalStateException(
            "Total share percentages would exceed 100%. Current total: " + totalExisting);
      }
    }
  }

  private LiabilityDto toDto(LiabilityEntity entity) {
    return LiabilityDto.builder()
        .id(entity.getId())
        .debtId(entity.getDebtId())
        .debtorPersonId(entity.getDebtorPersonId())
        .liabilityType(entity.getLiabilityType().name())
        .shareAmount(entity.getShareAmount())
        .sharePercentage(entity.getSharePercentage())
        .active(entity.isActive())
        .build();
  }
}
