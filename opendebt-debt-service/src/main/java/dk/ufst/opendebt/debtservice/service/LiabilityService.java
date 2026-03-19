package dk.ufst.opendebt.debtservice.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import dk.ufst.opendebt.debtservice.dto.LiabilityDto;
import dk.ufst.opendebt.debtservice.entity.LiabilityEntity.LiabilityType;

public interface LiabilityService {

  LiabilityDto addLiability(
      UUID debtId, UUID debtorPersonId, LiabilityType type, BigDecimal sharePercentage);

  void removeLiability(UUID liabilityId);

  List<LiabilityDto> getLiabilities(UUID debtId);

  List<LiabilityDto> getDebtorLiabilities(UUID debtorPersonId);
}
