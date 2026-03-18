package dk.ufst.opendebt.caseservice.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dk.ufst.opendebt.caseservice.entity.CaseRelationEntity;

@Repository
public interface CaseRelationRepository extends JpaRepository<CaseRelationEntity, UUID> {

  List<CaseRelationEntity> findBySourceCaseIdOrTargetCaseId(UUID sourceCaseId, UUID targetCaseId);
}
