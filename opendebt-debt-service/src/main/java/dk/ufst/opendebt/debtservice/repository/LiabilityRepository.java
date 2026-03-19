package dk.ufst.opendebt.debtservice.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dk.ufst.opendebt.debtservice.entity.LiabilityEntity;

@Repository
public interface LiabilityRepository extends JpaRepository<LiabilityEntity, UUID> {

  List<LiabilityEntity> findByDebtIdAndActiveTrue(UUID debtId);

  List<LiabilityEntity> findByDebtId(UUID debtId);

  List<LiabilityEntity> findByDebtorPersonIdAndActiveTrue(UUID debtorPersonId);

  boolean existsByDebtIdAndDebtorPersonId(UUID debtId, UUID debtorPersonId);
}
