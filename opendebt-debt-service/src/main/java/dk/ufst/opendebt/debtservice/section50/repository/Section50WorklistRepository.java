package dk.ufst.opendebt.debtservice.section50.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.ufst.opendebt.debtservice.section50.entity.Section50WorklistEntity;

public interface Section50WorklistRepository extends JpaRepository<Section50WorklistEntity, UUID> {
  Optional<Section50WorklistEntity> findByIdAndDebtorPersonId(UUID id, UUID debtorPersonId);
}
