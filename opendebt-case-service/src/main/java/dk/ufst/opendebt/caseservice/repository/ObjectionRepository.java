package dk.ufst.opendebt.caseservice.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dk.ufst.opendebt.caseservice.entity.ObjectionEntity;
import dk.ufst.opendebt.caseservice.entity.ObjectionStatus;

@Repository
public interface ObjectionRepository extends JpaRepository<ObjectionEntity, UUID> {

  List<ObjectionEntity> findByCaseId(UUID caseId);

  List<ObjectionEntity> findByCaseIdAndStatus(UUID caseId, ObjectionStatus status);
}
