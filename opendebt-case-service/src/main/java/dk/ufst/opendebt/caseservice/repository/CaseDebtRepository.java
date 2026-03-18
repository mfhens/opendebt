package dk.ufst.opendebt.caseservice.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dk.ufst.opendebt.caseservice.entity.CaseDebtEntity;

@Repository
public interface CaseDebtRepository extends JpaRepository<CaseDebtEntity, UUID> {

  List<CaseDebtEntity> findByCaseIdAndRemovedAtIsNull(UUID caseId);

  boolean existsByCaseIdAndDebtIdAndRemovedAtIsNull(UUID caseId, UUID debtId);
}
