package dk.ufst.opendebt.caseservice.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dk.ufst.opendebt.caseservice.entity.CaseJournalEntryEntity;

@Repository
public interface CaseJournalEntryRepository extends JpaRepository<CaseJournalEntryEntity, UUID> {

  List<CaseJournalEntryEntity> findByCaseIdOrderByJournalEntryTimeDesc(UUID caseId);
}
