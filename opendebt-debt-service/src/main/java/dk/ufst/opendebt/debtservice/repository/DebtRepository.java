package dk.ufst.opendebt.debtservice.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.entity.DebtEntity.DebtStatus;
import dk.ufst.opendebt.debtservice.entity.DebtEntity.ReadinessStatus;

@Repository
public interface DebtRepository extends JpaRepository<DebtEntity, UUID> {

  List<DebtEntity> findByDebtorId(String debtorId);

  List<DebtEntity> findByCreditorId(String creditorId);

  Page<DebtEntity> findByStatus(DebtStatus status, Pageable pageable);

  Page<DebtEntity> findByReadinessStatus(ReadinessStatus readinessStatus, Pageable pageable);

  @Query(
      "SELECT d FROM DebtEntity d WHERE "
          + "(:creditorId IS NULL OR d.creditorId = :creditorId) AND "
          + "(:debtorId IS NULL OR d.debtorId = :debtorId) AND "
          + "(:status IS NULL OR d.status = :status) AND "
          + "(:readinessStatus IS NULL OR d.readinessStatus = :readinessStatus)")
  Page<DebtEntity> findByFilters(
      @Param("creditorId") String creditorId,
      @Param("debtorId") String debtorId,
      @Param("status") DebtStatus status,
      @Param("readinessStatus") ReadinessStatus readinessStatus,
      Pageable pageable);

  @Query(
      "SELECT COUNT(d) FROM DebtEntity d WHERE d.creditorId = :creditorId AND d.readinessStatus = :status")
  long countByCreditorAndReadinessStatus(
      @Param("creditorId") String creditorId, @Param("status") ReadinessStatus status);

  List<DebtEntity> findByCreditorIdAndReadinessStatus(
      String creditorId, ReadinessStatus readinessStatus);
}
