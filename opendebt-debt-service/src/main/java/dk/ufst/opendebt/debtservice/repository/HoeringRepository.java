package dk.ufst.opendebt.debtservice.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dk.ufst.opendebt.debtservice.entity.HoeringEntity;
import dk.ufst.opendebt.debtservice.entity.HoeringStatus;

@Repository
public interface HoeringRepository extends JpaRepository<HoeringEntity, UUID> {

  Optional<HoeringEntity> findByDebtId(UUID debtId);

  Page<HoeringEntity> findByHoeringStatus(HoeringStatus status, Pageable pageable);
}
