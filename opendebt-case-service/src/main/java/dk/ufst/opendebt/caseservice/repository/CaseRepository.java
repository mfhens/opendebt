package dk.ufst.opendebt.caseservice.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import dk.ufst.opendebt.caseservice.entity.CaseEntity;
import dk.ufst.opendebt.caseservice.entity.CaseEntity.CaseStatus;

@Repository
public interface CaseRepository extends JpaRepository<CaseEntity, UUID> {

  Optional<CaseEntity> findByCaseNumber(String caseNumber);

  List<CaseEntity> findByDebtorId(String debtorId);

  Page<CaseEntity> findByStatus(CaseStatus status, Pageable pageable);

  Page<CaseEntity> findByAssignedCaseworkerId(String caseworkerId, Pageable pageable);

  @Query(
      "SELECT c FROM CaseEntity c WHERE "
          + "(:status IS NULL OR c.status = :status) AND "
          + "(:caseworkerId IS NULL OR c.assignedCaseworkerId = :caseworkerId)")
  Page<CaseEntity> findByFilters(
      @Param("status") CaseStatus status,
      @Param("caseworkerId") String caseworkerId,
      Pageable pageable);

  @Query("SELECT COUNT(c) FROM CaseEntity c WHERE c.status = :status")
  long countByStatus(@Param("status") CaseStatus status);
}
