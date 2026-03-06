package dk.ufst.opendebt.payment.bookkeeping.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import dk.ufst.opendebt.payment.bookkeeping.entity.DebtEventEntity;

@Repository
public interface DebtEventRepository extends JpaRepository<DebtEventEntity, UUID> {

  List<DebtEventEntity> findByDebtIdOrderByEffectiveDateAscCreatedAtAsc(UUID debtId);

  /** Returns all balance-affecting events for a debt, ordered by effective date. */
  @Query(
      "SELECT e FROM DebtEventEntity e WHERE e.debtId = :debtId "
          + "AND e.eventType NOT IN ('INTEREST_ACCRUED') "
          + "ORDER BY e.effectiveDate ASC, e.createdAt ASC")
  List<DebtEventEntity> findPrincipalAffectingEvents(@Param("debtId") UUID debtId);

  /** Returns events after a given effective date for replay. */
  @Query(
      "SELECT e FROM DebtEventEntity e WHERE e.debtId = :debtId "
          + "AND e.effectiveDate >= :fromDate "
          + "ORDER BY e.effectiveDate ASC, e.createdAt ASC")
  List<DebtEventEntity> findEventsFromDate(
      @Param("debtId") UUID debtId, @Param("fromDate") LocalDate fromDate);
}
