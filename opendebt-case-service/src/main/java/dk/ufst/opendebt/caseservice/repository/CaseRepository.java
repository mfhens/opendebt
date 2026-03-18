package dk.ufst.opendebt.caseservice.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import dk.ufst.opendebt.caseservice.entity.CaseEntity;
import dk.ufst.opendebt.caseservice.entity.CaseState;

@Repository
public interface CaseRepository extends JpaRepository<CaseEntity, UUID> {

  Optional<CaseEntity> findByCaseNumber(String caseNumber);

  Page<CaseEntity> findByCaseState(CaseState caseState, Pageable pageable);

  Page<CaseEntity> findByPrimaryCaseworkerId(String caseworkerId, Pageable pageable);

  @Query(
      "SELECT c FROM CaseEntity c WHERE "
          + "(:caseState IS NULL OR c.caseState = :caseState) AND "
          + "(:caseworkerId IS NULL OR c.primaryCaseworkerId = :caseworkerId)")
  Page<CaseEntity> findByFilters(
      @Param("caseState") CaseState caseState,
      @Param("caseworkerId") String caseworkerId,
      Pageable pageable);

  @Query("SELECT COUNT(c) FROM CaseEntity c WHERE c.caseState = :caseState")
  long countByCaseState(@Param("caseState") CaseState caseState);
}
