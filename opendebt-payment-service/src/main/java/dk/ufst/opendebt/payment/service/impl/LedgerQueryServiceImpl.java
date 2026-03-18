package dk.ufst.opendebt.payment.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.ufst.opendebt.payment.bookkeeping.entity.DebtEventEntity;
import dk.ufst.opendebt.payment.bookkeeping.entity.LedgerEntryEntity;
import dk.ufst.opendebt.payment.bookkeeping.repository.DebtEventRepository;
import dk.ufst.opendebt.payment.bookkeeping.repository.LedgerEntryRepository;
import dk.ufst.opendebt.payment.client.CaseServiceClient;
import dk.ufst.opendebt.payment.dto.DebtEventDto;
import dk.ufst.opendebt.payment.dto.LedgerEntryDto;
import dk.ufst.opendebt.payment.dto.LedgerSummaryDto;
import dk.ufst.opendebt.payment.service.LedgerQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Implementation of {@link LedgerQueryService}. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LedgerQueryServiceImpl implements LedgerQueryService {

  private final LedgerEntryRepository ledgerEntryRepository;
  private final DebtEventRepository debtEventRepository;
  private final CaseServiceClient caseServiceClient;

  @Override
  public Page<LedgerEntryDto> getLedgerEntriesByDebtId(
      UUID debtId,
      LocalDate fromDate,
      LocalDate toDate,
      LedgerEntryEntity.EntryCategory category,
      boolean includeStorno,
      Pageable pageable) {
    log.debug(
        "Querying ledger entries for debt={}, from={}, to={}, category={}, includeStorno={}",
        debtId,
        fromDate,
        toDate,
        category,
        includeStorno);

    return ledgerEntryRepository
        .findByDebtIdFiltered(debtId, fromDate, toDate, category, includeStorno, pageable)
        .map(this::toDto);
  }

  @Override
  public Page<LedgerEntryDto> getLedgerEntriesByCaseId(
      UUID caseId,
      LocalDate fromDate,
      LocalDate toDate,
      LedgerEntryEntity.EntryCategory category,
      boolean includeStorno,
      Pageable pageable) {
    log.debug("Querying ledger entries for case={}", caseId);

    List<UUID> debtIds = caseServiceClient.getDebtIdsForCase(caseId);
    if (debtIds.isEmpty()) {
      return Page.empty(pageable);
    }

    return ledgerEntryRepository
        .findByDebtIdsFiltered(debtIds, fromDate, toDate, category, includeStorno, pageable)
        .map(this::toDto);
  }

  @Override
  public List<DebtEventDto> getEventsByDebtId(UUID debtId) {
    log.debug("Querying events for debt={}", debtId);

    return debtEventRepository.findByDebtIdOrderByEffectiveDateAscCreatedAtAsc(debtId).stream()
        .map(this::toEventDto)
        .toList();
  }

  @Override
  public List<DebtEventDto> getEventsByCaseId(UUID caseId) {
    log.debug("Querying events for case={}", caseId);

    List<UUID> debtIds = caseServiceClient.getDebtIdsForCase(caseId);
    if (debtIds.isEmpty()) {
      return List.of();
    }

    return debtIds.stream()
        .flatMap(
            debtId ->
                debtEventRepository
                    .findByDebtIdOrderByEffectiveDateAscCreatedAtAsc(debtId)
                    .stream())
        .sorted(
            Comparator.comparing(DebtEventEntity::getEffectiveDate)
                .thenComparing(DebtEventEntity::getCreatedAt))
        .map(this::toEventDto)
        .toList();
  }

  @Override
  public LedgerSummaryDto getLedgerSummary(UUID debtId) {
    log.debug("Computing ledger summary for debt={}", debtId);

    List<LedgerEntryEntity> entries =
        ledgerEntryRepository.findByDebtIdOrderByEffectiveDateAscCreatedAtAsc(debtId);

    BigDecimal principalBalance = BigDecimal.ZERO;
    BigDecimal interestBalance = BigDecimal.ZERO;
    BigDecimal totalPayments = BigDecimal.ZERO;
    BigDecimal totalInterestAccrued = BigDecimal.ZERO;
    BigDecimal totalWriteOffs = BigDecimal.ZERO;
    BigDecimal totalCorrections = BigDecimal.ZERO;
    long stornoCount = 0;
    LocalDate lastEventDate = null;
    LocalDate lastPostingDate = null;

    for (LedgerEntryEntity entry : entries) {
      BigDecimal signedAmount = signedAmount(entry);

      switch (entry.getEntryCategory()) {
        case DEBT_REGISTRATION -> principalBalance = principalBalance.add(signedAmount);
        case PAYMENT -> {
          principalBalance = principalBalance.add(signedAmount);
          totalPayments = totalPayments.add(entry.getAmount());
        }
        case INTEREST_ACCRUAL -> {
          interestBalance = interestBalance.add(signedAmount);
          totalInterestAccrued = totalInterestAccrued.add(entry.getAmount());
        }
        case WRITE_OFF -> {
          principalBalance = principalBalance.add(signedAmount);
          totalWriteOffs = totalWriteOffs.add(entry.getAmount());
        }
        case CORRECTION -> {
          principalBalance = principalBalance.add(signedAmount);
          totalCorrections = totalCorrections.add(entry.getAmount());
        }
        case STORNO -> {
          principalBalance = principalBalance.add(signedAmount);
          stornoCount++;
        }
        case OFFSETTING -> principalBalance = principalBalance.add(signedAmount);
        case REFUND -> principalBalance = principalBalance.add(signedAmount);
        case COVERAGE_REVERSAL -> principalBalance = principalBalance.add(signedAmount);
      }

      if (lastEventDate == null || entry.getEffectiveDate().isAfter(lastEventDate)) {
        lastEventDate = entry.getEffectiveDate();
      }
      if (lastPostingDate == null || entry.getPostingDate().isAfter(lastPostingDate)) {
        lastPostingDate = entry.getPostingDate();
      }
    }

    BigDecimal totalBalance = principalBalance.add(interestBalance);

    return LedgerSummaryDto.builder()
        .debtId(debtId)
        .principalBalance(principalBalance)
        .interestBalance(interestBalance)
        .totalBalance(totalBalance)
        .totalPayments(totalPayments)
        .totalInterestAccrued(totalInterestAccrued)
        .totalWriteOffs(totalWriteOffs)
        .totalCorrections(totalCorrections)
        .lastEventDate(lastEventDate)
        .lastPostingDate(lastPostingDate)
        .entryCount(entries.size())
        .stornoCount(stornoCount)
        .build();
  }

  /**
   * Returns the signed amount: positive for DEBIT, negative for CREDIT. This reflects the
   * double-entry convention where DEBIT increases the debt balance and CREDIT decreases it.
   */
  private BigDecimal signedAmount(LedgerEntryEntity entry) {
    return entry.getEntryType() == LedgerEntryEntity.EntryType.DEBIT
        ? entry.getAmount()
        : entry.getAmount().negate();
  }

  private LedgerEntryDto toDto(LedgerEntryEntity entity) {
    return LedgerEntryDto.builder()
        .id(entity.getId())
        .transactionId(entity.getTransactionId())
        .debtId(entity.getDebtId())
        .accountCode(entity.getAccountCode())
        .accountName(entity.getAccountName())
        .entryType(entity.getEntryType().name())
        .amount(entity.getAmount())
        .effectiveDate(entity.getEffectiveDate())
        .postingDate(entity.getPostingDate())
        .reference(entity.getReference())
        .description(entity.getDescription())
        .entryCategory(entity.getEntryCategory().name())
        .reversalOfTransactionId(entity.getReversalOfTransactionId())
        .createdAt(entity.getCreatedAt())
        .build();
  }

  private DebtEventDto toEventDto(DebtEventEntity entity) {
    return DebtEventDto.builder()
        .id(entity.getId())
        .debtId(entity.getDebtId())
        .eventType(entity.getEventType().name())
        .effectiveDate(entity.getEffectiveDate())
        .amount(entity.getAmount())
        .correctsEventId(entity.getCorrectsEventId())
        .reference(entity.getReference())
        .description(entity.getDescription())
        .ledgerTransactionId(entity.getLedgerTransactionId())
        .createdAt(entity.getCreatedAt())
        .build();
  }
}
