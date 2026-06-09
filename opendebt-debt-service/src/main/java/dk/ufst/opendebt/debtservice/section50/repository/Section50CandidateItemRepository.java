package dk.ufst.opendebt.debtservice.section50.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.ufst.opendebt.debtservice.section50.entity.Section50CandidateItemEntity;

public interface Section50CandidateItemRepository
    extends JpaRepository<Section50CandidateItemEntity, UUID> {
  List<Section50CandidateItemEntity> findByDebtorPersonId(UUID debtorPersonId);

  List<Section50CandidateItemEntity> findByDebtorPersonIdAndClaimIdIn(
      UUID debtorPersonId, List<String> claimIds);

  void deleteByDebtorPersonId(UUID debtorPersonId);
}
