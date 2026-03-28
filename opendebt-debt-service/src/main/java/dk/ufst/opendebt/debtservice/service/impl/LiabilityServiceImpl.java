package dk.ufst.opendebt.debtservice.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.ufst.opendebt.debtservice.dto.LiabilityDto;
import dk.ufst.opendebt.debtservice.entity.ClaimLifecycleState;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
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

    // G.A.1.3.3: Delt hæftelse must not be modelled within the inddrivelse layer.
    // Fordringshavere must split proportional shares as separate fordringer before submission.
    if (type == LiabilityType.PROPORTIONAL) {
      throw new IllegalArgumentException(
          "PROPORTIONAL liability type is not supported in the inddrivelse layer per G.A.1.3.3. "
              + "Split delt hæftelse into separate fordringer before submission.");
    }

    DebtEntity debt =
        debtRepository
            .findById(debtId)
            .orElseThrow(() -> new IllegalArgumentException("Debt not found: " + debtId));

    // G.A.1.3.3 / GIL § 2 stk. 5: Hæftelsesstruktur is immutable once claim is OVERDRAGET.
    // Only claims in RESTANCE (pre-overdragelse) may have their liability structure changed.
    if (debt.getLifecycleState() != ClaimLifecycleState.RESTANCE) {
      throw new IllegalStateException(
          "Hæftelsesstruktur is immutable on claim "
              + debtId
              + " in state "
              + debt.getLifecycleState()
              + ". Changes require tilbagekaldelse and re-submission per G.A.1.3.3 / GIL § 2, stk. 5.");
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
