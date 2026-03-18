package dk.ufst.opendebt.payment.bookkeeping.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import dk.ufst.opendebt.payment.bookkeeping.entity.LedgerEntryEntity;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntryEntity, UUID> {

  List<LedgerEntryEntity> findByDebtIdOrderByEffectiveDateAscCreatedAtAsc(UUID debtId);

  List<LedgerEntryEntity> findByTransactionId(UUID transactionId);

  List<LedgerEntryEntity> findByAccountCodeOrderByEffectiveDateAsc(String accountCode);

  /** Finds interest accrual entries for a debt on or after the given effective date. */
  @Query(
      "SELECT e FROM LedgerEntryEntity e WHERE e.debtId = :debtId "
          + "AND e.entryCategory = 'INTEREST_ACCRUAL' "
          + "AND e.effectiveDate >= :fromDate "
          + "AND e.reversalOfTransactionId IS NULL "
          + "ORDER BY e.effectiveDate ASC")
  List<LedgerEntryEntity> findInterestAccrualsAfterDate(
      @Param("debtId") UUID debtId, @Param("fromDate") LocalDate fromDate);

  /** Finds all non-reversed entries for a debt, ordered by effective date. */
  @Query(
      "SELECT e FROM LedgerEntryEntity e WHERE e.debtId = :debtId "
          + "AND e.reversalOfTransactionId IS NULL "
          + "AND e.entryCategory != 'STORNO' "
          + "ORDER BY e.effectiveDate ASC, e.createdAt ASC")
  List<LedgerEntryEntity> findActiveEntriesByDebtId(@Param("debtId") UUID debtId);

  /** Finds entries that were reversed by a specific storno transaction. */
  List<LedgerEntryEntity> findByReversalOfTransactionId(UUID reversalOfTransactionId);

  /** Checks if a transaction has already been reversed. */
  boolean existsByReversalOfTransactionId(UUID transactionId);

  // --- Paginated query methods for ledger query API ---

  Page<LedgerEntryEntity> findByDebtIdOrderByEffectiveDateAscPostingDateAsc(
      UUID debtId, Pageable pageable);

  Page<LedgerEntryEntity> findByDebtIdInOrderByEffectiveDateAscPostingDateAsc(
      List<UUID> debtIds, Pageable pageable);

  @Query(
      "SELECT e FROM LedgerEntryEntity e WHERE e.debtId = :debtId "
          + "AND (:fromDate IS NULL OR e.effectiveDate >= :fromDate) "
          + "AND (:toDate IS NULL OR e.effectiveDate <= :toDate) "
          + "AND (:category IS NULL OR e.entryCategory = :category) "
          + "AND (:includeStorno = true OR e.entryCategory != 'STORNO') "
          + "ORDER BY e.effectiveDate ASC, e.postingDate ASC")
  Page<LedgerEntryEntity> findByDebtIdFiltered(
      @Param("debtId") UUID debtId,
      @Param("fromDate") LocalDate fromDate,
      @Param("toDate") LocalDate toDate,
      @Param("category") LedgerEntryEntity.EntryCategory category,
      @Param("includeStorno") boolean includeStorno,
      Pageable pageable);

  @Query(
      "SELECT e FROM LedgerEntryEntity e WHERE e.debtId IN :debtIds "
          + "AND (:fromDate IS NULL OR e.effectiveDate >= :fromDate) "
          + "AND (:toDate IS NULL OR e.effectiveDate <= :toDate) "
          + "AND (:category IS NULL OR e.entryCategory = :category) "
          + "AND (:includeStorno = true OR e.entryCategory != 'STORNO') "
          + "ORDER BY e.effectiveDate ASC, e.postingDate ASC")
  Page<LedgerEntryEntity> findByDebtIdsFiltered(
      @Param("debtIds") List<UUID> debtIds,
      @Param("fromDate") LocalDate fromDate,
      @Param("toDate") LocalDate toDate,
      @Param("category") LedgerEntryEntity.EntryCategory category,
      @Param("includeStorno") boolean includeStorno,
      Pageable pageable);
}
