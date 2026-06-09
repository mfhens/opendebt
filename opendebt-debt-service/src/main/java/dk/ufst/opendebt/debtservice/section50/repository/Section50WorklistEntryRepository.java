package dk.ufst.opendebt.debtservice.section50.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.ufst.opendebt.debtservice.section50.entity.Section50WorklistEntryEntity;

public interface Section50WorklistEntryRepository
    extends JpaRepository<Section50WorklistEntryEntity, UUID> {
  List<Section50WorklistEntryEntity> findByWorklistIdOrderByRankOrder(UUID worklistId);

  void deleteByWorklistId(UUID worklistId);
}
