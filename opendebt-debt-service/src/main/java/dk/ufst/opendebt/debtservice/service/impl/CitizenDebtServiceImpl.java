package dk.ufst.opendebt.debtservice.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.ufst.opendebt.debtservice.dto.CitizenDebtItemDto;
import dk.ufst.opendebt.debtservice.dto.CitizenDebtSummaryResponse;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;
import dk.ufst.opendebt.debtservice.service.CitizenDebtService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CitizenDebtServiceImpl implements CitizenDebtService {

  private final DebtRepository debtRepository;

  @Override
  @Transactional(readOnly = true)
  public CitizenDebtSummaryResponse getDebtSummary(
      UUID personId, DebtEntity.DebtStatus status, Pageable pageable) {

    log.info(
        "Fetching debt summary for person_id={}, status={}, page={}, size={}",
        personId,
        status,
        pageable.getPageNumber(),
        pageable.getPageSize());

    // Query debts by person_id with optional status filter
    Page<DebtEntity> debtsPage;
    if (status != null) {
      debtsPage = debtRepository.findByFilters(null, personId, status, null, pageable);
    } else {
      List<DebtEntity> allDebts = debtRepository.findByDebtorPersonId(personId);
      // Convert list to page (simple in-memory pagination for now)
      int start = (int) pageable.getOffset();
      int end = Math.min((start + pageable.getPageSize()), allDebts.size());
      List<DebtEntity> pageContent =
          start < allDebts.size() ? allDebts.subList(start, end) : List.of();
      debtsPage =
          new org.springframework.data.domain.PageImpl<>(pageContent, pageable, allDebts.size());
    }

    // Map to citizen DTOs (NO PII, NO creditor internals, NO readinessStatus)
    List<CitizenDebtItemDto> debtItems =
        debtsPage.getContent().stream().map(this::mapToCitizenDto).collect(Collectors.toList());

    // Calculate totals across ALL debts (not just current page)
    List<DebtEntity> allDebts = debtRepository.findByDebtorPersonId(personId);
    BigDecimal totalOutstanding =
        allDebts.stream()
            .map(DebtEntity::getOutstandingBalance)
            .filter(balance -> balance != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    log.info(
        "Debt summary for person_id={}: {} total debts, {} outstanding",
        personId,
        allDebts.size(),
        totalOutstanding);

    return CitizenDebtSummaryResponse.builder()
        .debts(debtItems)
        .totalOutstandingAmount(totalOutstanding)
        .totalDebtCount(allDebts.size())
        .pageNumber(debtsPage.getNumber())
        .pageSize(debtsPage.getSize())
        .totalPages(debtsPage.getTotalPages())
        .totalElements(debtsPage.getTotalElements())
        .build();
  }

  private CitizenDebtItemDto mapToCitizenDto(DebtEntity debt) {
    return CitizenDebtItemDto.builder()
        .debtId(debt.getId())
        .debtTypeCode(debt.getDebtTypeCode())
        .debtTypeName(getDebtTypeName(debt.getDebtTypeCode()))
        .principalAmount(debt.getPrincipalAmount())
        .outstandingAmount(debt.getOutstandingBalance())
        .interestAmount(debt.getInterestAmount())
        .feesAmount(debt.getFeesAmount())
        .dueDate(debt.getDueDate())
        .status(debt.getStatus() != null ? debt.getStatus().name() : null)
        .lifecycleState(debt.getLifecycleState() != null ? debt.getLifecycleState().name() : null)
        .build();
  }

  private String getDebtTypeName(String debtTypeCode) {
    // AIDEV-TODO: Replace with lookup from debt_types table (petition008)
    // For now, return the code itself
    return switch (debtTypeCode) {
      case "RESTSKAT" -> "Restskat";
      case "MOMSGAELD" -> "Momsgæld";
      case "UNDERHOLDSBIDRAG" -> "Underholdsbidrag";
      case "STUDIEGAELD" -> "Studiegæld";
      case "DAGPENGEGAELD" -> "Dagpengegæld";
      case "ERSTATNING" -> "Erstatning";
      default -> debtTypeCode;
    };
  }
}
