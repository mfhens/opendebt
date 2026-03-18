package dk.ufst.opendebt.payment.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import dk.ufst.opendebt.payment.bookkeeping.entity.LedgerEntryEntity;
import dk.ufst.opendebt.payment.dto.DebtEventDto;
import dk.ufst.opendebt.payment.dto.LedgerEntryDto;
import dk.ufst.opendebt.payment.dto.LedgerSummaryDto;

/** Service for querying ledger entries, debt events, and computing balance summaries. */
public interface LedgerQueryService {

  /**
   * Returns paginated ledger entries for a single debt, with optional date range and category
   * filters.
   */
  Page<LedgerEntryDto> getLedgerEntriesByDebtId(
      UUID debtId,
      LocalDate fromDate,
      LocalDate toDate,
      LedgerEntryEntity.EntryCategory category,
      boolean includeStorno,
      Pageable pageable);

  /**
   * Returns paginated ledger entries for all debts in a case, with optional date range and category
   * filters.
   */
  Page<LedgerEntryDto> getLedgerEntriesByCaseId(
      UUID caseId,
      LocalDate fromDate,
      LocalDate toDate,
      LedgerEntryEntity.EntryCategory category,
      boolean includeStorno,
      Pageable pageable);

  /** Returns all debt events for a single debt, ordered by effective date. */
  List<DebtEventDto> getEventsByDebtId(UUID debtId);

  /** Returns all debt events for all debts in a case, ordered by effective date. */
  List<DebtEventDto> getEventsByCaseId(UUID caseId);

  /** Computes a balance summary for a single debt from its ledger entries. */
  LedgerSummaryDto getLedgerSummary(UUID debtId);
}
