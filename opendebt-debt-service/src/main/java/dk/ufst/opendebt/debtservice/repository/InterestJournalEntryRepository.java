package dk.ufst.opendebt.debtservice.repository;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dk.ufst.opendebt.debtservice.entity.InterestJournalEntry;

@Repository
public interface InterestJournalEntryRepository extends JpaRepository<InterestJournalEntry, UUID> {

  boolean existsByDebtIdAndAccrualDate(UUID debtId, LocalDate accrualDate);
}
