package dk.ufst.opendebt.debtservice.limitation.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.ufst.opendebt.debtservice.limitation.entity.LimitationObjectionLinkage;

public interface LimitationObjectionLinkageRepository
    extends JpaRepository<LimitationObjectionLinkage, UUID> {

  Optional<LimitationObjectionLinkage> findByFordringId(UUID fordringId);

  Optional<LimitationObjectionLinkage> findByIndsigelsesId(UUID indsigelsesId);
}
