package dk.ufst.opendebt.caseservice.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dk.ufst.opendebt.caseservice.entity.CollectionMeasureEntity;
import dk.ufst.opendebt.caseservice.entity.MeasureStatus;

@Repository
public interface CollectionMeasureRepository extends JpaRepository<CollectionMeasureEntity, UUID> {

  List<CollectionMeasureEntity> findByCaseId(UUID caseId);

  List<CollectionMeasureEntity> findByCaseIdAndStatus(UUID caseId, MeasureStatus status);
}
