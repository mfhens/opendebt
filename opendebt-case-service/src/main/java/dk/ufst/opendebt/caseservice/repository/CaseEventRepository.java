package dk.ufst.opendebt.caseservice.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dk.ufst.opendebt.caseservice.entity.CaseEventEntity;

@Repository
public interface CaseEventRepository extends JpaRepository<CaseEventEntity, UUID> {

  List<CaseEventEntity> findByCaseIdOrderByPerformedAtDesc(UUID caseId);
}
