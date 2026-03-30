package dk.ufst.opendebt.debtservice.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import dk.ufst.opendebt.debtservice.entity.ClaimLifecycleState;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.entity.DebtEntity.DebtStatus;
import dk.ufst.opendebt.debtservice.entity.DebtEntity.ReadinessStatus;

@Repository
public interface DebtRepository extends JpaRepository<DebtEntity, UUID> {

  @Query("SELECT d FROM DebtEntity d WHERE d.debtorPersonId = :debtorPersonId")
  List<DebtEntity> findByDebtorPersonId(UUID debtorPersonId);

  @Query("SELECT d FROM DebtEntity d WHERE d.creditorOrgId = :creditorOrgId")
  List<DebtEntity> findByCreditorOrgId(UUID creditorOrgId);

  Page<DebtEntity> findByStatus(DebtStatus status, Pageable pageable);

  Page<DebtEntity> findByReadinessStatus(ReadinessStatus readinessStatus, Pageable pageable);

  @Query(
      "SELECT d FROM DebtEntity d WHERE "
          + "(:creditorOrgId IS NULL OR d.creditorOrgId = :creditorOrgId) AND "
          + "(:debtorPersonId IS NULL OR d.debtorPersonId = :debtorPersonId) AND "
          + "(:status IS NULL OR d.status = :status) AND "
          + "(:readinessStatus IS NULL OR d.readinessStatus = :readinessStatus)")
  Page<DebtEntity> findByFilters(
      @Param("creditorOrgId") UUID creditorOrgId,
      @Param("debtorPersonId") UUID debtorPersonId,
      @Param("status") DebtStatus status,
      @Param("readinessStatus") ReadinessStatus readinessStatus,
      Pageable pageable);

  @Query(
      "SELECT COUNT(d) FROM DebtEntity d WHERE d.creditorOrgId = :creditorOrgId AND d.readinessStatus = :status")
  long countByCreditorAndReadinessStatus(
      @Param("creditorOrgId") UUID creditorOrgId, @Param("status") ReadinessStatus status);

  @Query(
      "SELECT d FROM DebtEntity d WHERE d.creditorOrgId = :creditorOrgId AND d.readinessStatus = :readinessStatus")
  List<DebtEntity> findByCreditorOrgIdAndReadinessStatus(
      @Param("creditorOrgId") UUID creditorOrgId,
      @Param("readinessStatus") ReadinessStatus readinessStatus);

  List<DebtEntity> findByOcrLine(String ocrLine);

  @Query(
      "SELECT d FROM DebtEntity d WHERE d.lifecycleState = :state "
          + "AND d.paymentDeadline < :cutoffDate "
          + "AND d.outstandingBalance > 0")
  Page<DebtEntity> findEligibleForRestanceTransition(
      @Param("state") ClaimLifecycleState state,
      @Param("cutoffDate") LocalDate cutoffDate,
      Pageable pageable);

  @Query(
      "SELECT d FROM DebtEntity d WHERE d.lifecycleState = :state "
          + "AND d.outstandingBalance > 0")
  Page<DebtEntity> findByLifecycleStateAndPositiveBalance(
      @Param("state") ClaimLifecycleState state, Pageable pageable);

  /**
   * Finds debts eligible for interest accrual: OVERDRAGET, positive balance, the debt type is
   * interest-applicable, and the debt is not temporarily ikkeinddrivelsesparat. Uses a subquery
   * against debt_types to filter out straffebøder and other interest-exempt types at the DB level.
   * Excludes ikkeinddrivelsesparate fordringer per G.A.2.4.3 (P043).
   */
  @Query(
      "SELECT d FROM DebtEntity d WHERE d.lifecycleState = :state "
          + "AND d.outstandingBalance > 0 "
          + "AND d.ikkeinddrivelsesparat = false "
          + "AND EXISTS (SELECT 1 FROM DebtTypeEntity dt WHERE dt.code = d.debtTypeCode AND dt.interestApplicable = true)")
  Page<DebtEntity> findInterestEligibleDebts(
      @Param("state") ClaimLifecycleState state, Pageable pageable);

  /**
   * Returns true if any sibling debt (same parentClaimId, different id) is already in the given
   * lifecycle state. Used to enforce the dual-phase prohibition per G.A.1.3.3 (P005).
   */
  @Query(
      "SELECT COUNT(d) > 0 FROM DebtEntity d WHERE d.parentClaimId = :parentClaimId "
          + "AND d.lifecycleState = :state AND d.id != :excludeId")
  boolean existsSiblingInState(
      @Param("parentClaimId") UUID parentClaimId,
      @Param("state") ClaimLifecycleState state,
      @Param("excludeId") UUID excludeId);

  /**
   * Returns all active fordringer for the given debtor: positive remaining balance and not in a
   * terminal status (PAID, WRITTEN_OFF, CANCELLED). Results are ordered by sekvensNummer ASC (NULLS
   * LAST), then by receivedAt ASC as FIFO tie-breaker, consistent with P057 § dækningsrækkefølge.
   */
  @Query(
      "SELECT d FROM DebtEntity d "
          + "WHERE d.debtorPersonId = :debtorPersonId "
          + "AND d.outstandingBalance > 0 "
          + "AND d.status NOT IN ('PAID', 'WRITTEN_OFF', 'CANCELLED') "
          + "ORDER BY d.sekvensNummer ASC NULLS LAST, d.receivedAt ASC NULLS LAST")
  List<DebtEntity> findActiveFordringerByDebtorPersonId(
      @Param("debtorPersonId") UUID debtorPersonId);

  @Query(
      "SELECT d FROM DebtEntity d WHERE d.limitationDate IS NOT NULL "
          + "AND d.limitationDate <= :warningDate "
          + "AND d.lifecycleState NOT IN ('TILBAGEKALDT', 'AFSKREVET', 'INDFRIET')")
  List<DebtEntity> findApproachingLimitation(@Param("warningDate") LocalDate warningDate);

  @Query(
      "SELECT COUNT(d) FROM DebtEntity d WHERE d.creditorOrgId = :creditorOrgId AND d.lifecycleState = :state")
  long countByCreditorAndLifecycleState(
      @Param("creditorOrgId") UUID creditorOrgId, @Param("state") ClaimLifecycleState state);

  @Query(
      "SELECT COUNT(d) FROM DebtEntity d WHERE d.creditorOrgId = :creditorOrgId "
          + "AND d.outstandingBalance = 0 "
          + "AND d.lifecycleState NOT IN ('INDFRIET', 'TILBAGEKALDT', 'AFSKREVET')")
  long countZeroBalanceByCreditor(@Param("creditorOrgId") UUID creditorOrgId);

  @Query(
      "SELECT d FROM DebtEntity d WHERE d.creditorOrgId = :creditorOrgId "
          + "AND (:state IS NULL OR d.lifecycleState = :state) "
          + "AND (:zeroBalanceOnly = false OR d.outstandingBalance = 0) "
          + "AND (:excludeZeroBalance = false OR d.outstandingBalance > 0 OR d.lifecycleState != 'OVERDRAGET') "
          + "ORDER BY d.createdAt DESC")
  Page<DebtEntity> findClaimsForCreditor(
      @Param("creditorOrgId") UUID creditorOrgId,
      @Param("state") ClaimLifecycleState state,
      @Param("zeroBalanceOnly") boolean zeroBalanceOnly,
      @Param("excludeZeroBalance") boolean excludeZeroBalance,
      Pageable pageable);
}
