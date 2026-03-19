package dk.ufst.opendebt.debtservice.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dk.ufst.opendebt.debtservice.entity.ObjectionEntity;
import dk.ufst.opendebt.debtservice.entity.ObjectionEntity.ObjectionStatus;

@Repository
public interface ObjectionRepository extends JpaRepository<ObjectionEntity, UUID> {

  List<ObjectionEntity> findByDebtIdOrderByRegisteredAtDesc(UUID debtId);

  boolean existsByDebtIdAndStatus(UUID debtId, ObjectionStatus status);
}
