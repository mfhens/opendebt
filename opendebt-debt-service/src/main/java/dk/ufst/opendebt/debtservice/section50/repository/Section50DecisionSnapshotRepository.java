package dk.ufst.opendebt.debtservice.section50.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.ufst.opendebt.debtservice.section50.entity.Section50DecisionSnapshotEntity;

public interface Section50DecisionSnapshotRepository
    extends JpaRepository<Section50DecisionSnapshotEntity, UUID> {
  Optional<Section50DecisionSnapshotEntity> findByWorklistId(UUID worklistId);

  void deleteByWorklistId(UUID worklistId);
}
