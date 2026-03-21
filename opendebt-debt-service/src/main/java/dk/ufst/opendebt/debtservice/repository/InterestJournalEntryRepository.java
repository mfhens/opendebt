package dk.ufst.opendebt.debtservice.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import dk.ufst.opendebt.debtservice.entity.InterestJournalEntry;

@Repository
public interface InterestJournalEntryRepository extends JpaRepository<InterestJournalEntry, UUID> {

  boolean existsByDebtIdAndAccrualDate(UUID debtId, LocalDate accrualDate);

  /**
   * Batch idempotency check: returns debt IDs that already have an entry for the given accrual
   * date. Replaces N per-debt existsBy calls with a single IN-clause query.
   */
  @Query(
      "SELECT e.debtId FROM InterestJournalEntry e "
          + "WHERE e.accrualDate = :accrualDate AND e.debtId IN :debtIds")
  Set<UUID> findAlreadyAccruedDebtIds(
      @Param("accrualDate") LocalDate accrualDate, @Param("debtIds") List<UUID> debtIds);

  /** Finds all journal entries for a debt on or after the given date, ordered by accrual date. */
  List<InterestJournalEntry> findByDebtIdAndAccrualDateGreaterThanEqualOrderByAccrualDate(
      UUID debtId, LocalDate from);

  /**
   * Deletes all journal entries for a debt from the given date forward. Used by
   * InterestRecalculationService when a crossing transaction invalidates previously accrued
   * interest.
   */
  @Modifying
  @Query("DELETE FROM InterestJournalEntry e WHERE e.debtId = :debtId AND e.accrualDate >= :from")
  int deleteByDebtIdFromDate(@Param("debtId") UUID debtId, @Param("from") LocalDate from);
}
