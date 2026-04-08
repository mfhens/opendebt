package dk.ufst.opendebt.payment.bookkeeping.adapter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.ufst.bookkeeping.domain.EntryCategory;
import dk.ufst.bookkeeping.domain.EntryType;
import dk.ufst.bookkeeping.domain.LedgerEntry;
import dk.ufst.bookkeeping.port.LedgerEntryStore;
import dk.ufst.opendebt.payment.bookkeeping.entity.LedgerEntryEntity;
import dk.ufst.opendebt.payment.bookkeeping.repository.LedgerEntryRepository;
import dk.ufst.opendebt.payment.immudb.ImmuLedgerAppender;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JpaLedgerEntryStore implements LedgerEntryStore {

  private final LedgerEntryRepository ledgerEntryRepository;

  // Optional: NoOpImmuLedgerAppender provides the production fallback; null-safe for test contexts
  // where BookkeepingService is mocked and this store is not exercised via double-entry paths.
  @Autowired(required = false)
  private ImmuLedgerAppender immuLedgerAppender;

  @Override
  public void saveDoubleEntry(LedgerEntry debit, LedgerEntry credit) {
    LedgerEntryEntity savedDebit = ledgerEntryRepository.save(toEntity(debit));
    LedgerEntryEntity savedCredit = ledgerEntryRepository.save(toEntity(credit));
    if (immuLedgerAppender != null) {
      immuLedgerAppender.appendAsync(savedDebit, savedCredit);
    }
  }

  @Override
  public void saveSingle(LedgerEntry entry) {
    ledgerEntryRepository.save(toEntity(entry));
  }

  @Override
  public List<LedgerEntry> findInterestAccrualsAfterDate(UUID debtId, LocalDate fromDate) {
    return ledgerEntryRepository.findInterestAccrualsAfterDate(debtId, fromDate).stream()
        .map(this::fromEntity)
        .toList();
  }

  @Override
  public List<LedgerEntry> findActiveEntriesByDebtId(UUID debtId) {
    return ledgerEntryRepository.findActiveEntriesByDebtId(debtId).stream()
        .map(this::fromEntity)
        .toList();
  }

  @Override
  public List<LedgerEntry> findByTransactionId(UUID transactionId) {
    return ledgerEntryRepository.findByTransactionId(transactionId).stream()
        .map(this::fromEntity)
        .toList();
  }

  @Override
  public boolean existsByReversalOfTransactionId(UUID transactionId) {
    return ledgerEntryRepository.existsByReversalOfTransactionId(transactionId);
  }

  private LedgerEntryEntity toEntity(LedgerEntry entry) {
    return LedgerEntryEntity.builder()
        .transactionId(entry.getTransactionId())
        .debtId(entry.getDebtId())
        .accountCode(entry.getAccountCode())
        .accountName(entry.getAccountName())
        .entryType(toEntityEntryType(entry.getEntryType()))
        .amount(entry.getAmount())
        .effectiveDate(entry.getEffectiveDate())
        .postingDate(entry.getPostingDate())
        .reference(entry.getReference())
        .description(entry.getDescription())
        .reversalOfTransactionId(entry.getReversalOfTransactionId())
        .entryCategory(toEntityEntryCategory(entry.getEntryCategory()))
        .build();
  }

  private LedgerEntry fromEntity(LedgerEntryEntity entity) {
    return LedgerEntry.builder()
        .transactionId(entity.getTransactionId())
        .debtId(entity.getDebtId())
        .accountCode(entity.getAccountCode())
        .accountName(entity.getAccountName())
        .entryType(fromEntityEntryType(entity.getEntryType()))
        .amount(entity.getAmount())
        .effectiveDate(entity.getEffectiveDate())
        .postingDate(entity.getPostingDate())
        .reference(entity.getReference())
        .description(entity.getDescription())
        .reversalOfTransactionId(entity.getReversalOfTransactionId())
        .entryCategory(fromEntityEntryCategory(entity.getEntryCategory()))
        .build();
  }

  private LedgerEntryEntity.EntryType toEntityEntryType(EntryType type) {
    return switch (type) {
      case DEBIT -> LedgerEntryEntity.EntryType.DEBIT;
      case CREDIT -> LedgerEntryEntity.EntryType.CREDIT;
    };
  }

  private EntryType fromEntityEntryType(LedgerEntryEntity.EntryType type) {
    return switch (type) {
      case DEBIT -> EntryType.DEBIT;
      case CREDIT -> EntryType.CREDIT;
    };
  }

  private LedgerEntryEntity.EntryCategory toEntityEntryCategory(EntryCategory category) {
    return switch (category) {
      case DEBT_REGISTRATION -> LedgerEntryEntity.EntryCategory.DEBT_REGISTRATION;
      case PAYMENT -> LedgerEntryEntity.EntryCategory.PAYMENT;
      case INTEREST_ACCRUAL -> LedgerEntryEntity.EntryCategory.INTEREST_ACCRUAL;
      case OFFSETTING -> LedgerEntryEntity.EntryCategory.OFFSETTING;
      case WRITE_OFF -> LedgerEntryEntity.EntryCategory.WRITE_OFF;
      case REFUND -> LedgerEntryEntity.EntryCategory.REFUND;
      case STORNO -> LedgerEntryEntity.EntryCategory.STORNO;
      case CORRECTION -> LedgerEntryEntity.EntryCategory.CORRECTION;
      case COVERAGE_REVERSAL -> LedgerEntryEntity.EntryCategory.COVERAGE_REVERSAL;
    };
  }

  private EntryCategory fromEntityEntryCategory(LedgerEntryEntity.EntryCategory category) {
    return switch (category) {
      case DEBT_REGISTRATION -> EntryCategory.DEBT_REGISTRATION;
      case PAYMENT -> EntryCategory.PAYMENT;
      case INTEREST_ACCRUAL -> EntryCategory.INTEREST_ACCRUAL;
      case OFFSETTING -> EntryCategory.OFFSETTING;
      case WRITE_OFF -> EntryCategory.WRITE_OFF;
      case REFUND -> EntryCategory.REFUND;
      case STORNO -> EntryCategory.STORNO;
      case CORRECTION -> EntryCategory.CORRECTION;
      case COVERAGE_REVERSAL -> EntryCategory.COVERAGE_REVERSAL;
    };
  }
}
