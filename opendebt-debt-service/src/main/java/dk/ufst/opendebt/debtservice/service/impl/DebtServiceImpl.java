package dk.ufst.opendebt.debtservice.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.ufst.opendebt.common.dto.DebtDto;
import dk.ufst.opendebt.common.exception.OpenDebtException;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;
import dk.ufst.opendebt.debtservice.service.DebtService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DebtServiceImpl implements DebtService {

  private final DebtRepository debtRepository;

  @Override
  public Page<DebtDto> listDebts(
      String creditorId,
      String debtorId,
      DebtDto.DebtStatus status,
      DebtDto.ReadinessStatus readinessStatus,
      Pageable pageable) {
    DebtEntity.DebtStatus entityStatus = status != null ? mapStatus(status) : null;
    DebtEntity.ReadinessStatus entityReadiness =
        readinessStatus != null ? mapReadiness(readinessStatus) : null;

    UUID creditorOrgId = creditorId != null ? UUID.fromString(creditorId) : null;
    UUID debtorPersonId = debtorId != null ? UUID.fromString(debtorId) : null;

    return debtRepository
        .findByFilters(creditorOrgId, debtorPersonId, entityStatus, entityReadiness, pageable)
        .map(this::toDto);
  }

  @Override
  public DebtDto getDebtById(UUID id) {
    return toDto(findEntityById(id));
  }

  @Override
  public List<DebtDto> getDebtsByDebtor(String debtorId) {
    UUID debtorPersonId = UUID.fromString(debtorId);
    return debtRepository.findByDebtorPersonId(debtorPersonId).stream()
        .map(this::toDto)
        .collect(Collectors.toList());
  }

  @Override
  public List<DebtDto> getDebtsByCreditor(String creditorId) {
    UUID creditorOrgId = UUID.fromString(creditorId);
    return debtRepository.findByCreditorOrgId(creditorOrgId).stream()
        .map(this::toDto)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional
  public DebtDto createDebt(DebtDto dto) {
    DebtEntity entity =
        DebtEntity.builder()
            .debtorPersonId(UUID.fromString(dto.getDebtorId()))
            .creditorOrgId(UUID.fromString(dto.getCreditorId()))
            .debtTypeCode(dto.getDebtTypeCode())
            .principalAmount(dto.getPrincipalAmount())
            .interestAmount(dto.getInterestAmount())
            .feesAmount(dto.getFeesAmount())
            .dueDate(dto.getDueDate())
            .originalDueDate(dto.getOriginalDueDate())
            .externalReference(dto.getExternalReference())
            .ocrLine(dto.getOcrLine())
            .outstandingBalance(calculateTotal(dto))
            .status(DebtEntity.DebtStatus.PENDING)
            .readinessStatus(DebtEntity.ReadinessStatus.PENDING_REVIEW)
            .build();
    DebtEntity saved = debtRepository.save(entity);
    log.info(
        "Created debt {}, type={}, amount={}",
        saved.getId(),
        dto.getDebtTypeCode(),
        dto.getPrincipalAmount());
    return toDto(saved);
  }

  @Override
  @Transactional
  public DebtDto updateDebt(UUID id, DebtDto dto) {
    DebtEntity entity = findEntityById(id);
    entity.setDebtTypeCode(dto.getDebtTypeCode());
    entity.setPrincipalAmount(dto.getPrincipalAmount());
    entity.setInterestAmount(dto.getInterestAmount());
    entity.setFeesAmount(dto.getFeesAmount());
    entity.setDueDate(dto.getDueDate());
    entity.setExternalReference(dto.getExternalReference());
    entity.setOcrLine(dto.getOcrLine());
    DebtEntity saved = debtRepository.save(entity);
    log.info("Updated debt {}", id);
    return toDto(saved);
  }

  @Override
  @Transactional
  public void cancelDebt(UUID id) {
    DebtEntity entity = findEntityById(id);
    entity.setStatus(DebtEntity.DebtStatus.CANCELLED);
    debtRepository.save(entity);
    log.info("Cancelled debt {}", id);
  }

  @Override
  public List<String> getDebtTypes() {
    // AIDEV-TODO: Load debt type codes from a configuration table or ingest_specs equivalent.
    // "600" is the SAP FI-CA debt type code for standard public-sector claims (EFI/PSRM).
    return List.of("600");
  }

  @Override
  public List<DebtDto> findByOcrLine(String ocrLine) {
    log.debug("Finding debts by OCR-linje: {}", ocrLine);
    return debtRepository.findByOcrLine(ocrLine).stream()
        .map(this::toDto)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional
  public DebtDto writeDown(UUID id, BigDecimal amount) {
    DebtEntity entity = findEntityById(id);
    BigDecimal currentBalance =
        entity.getOutstandingBalance() != null
            ? entity.getOutstandingBalance()
            : entity.getTotalAmount();

    BigDecimal newBalance = currentBalance.subtract(amount);
    if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
      newBalance = BigDecimal.ZERO;
    }
    entity.setOutstandingBalance(newBalance);

    if (newBalance.compareTo(BigDecimal.ZERO) == 0) {
      entity.setStatus(DebtEntity.DebtStatus.PAID);
    } else {
      entity.setStatus(DebtEntity.DebtStatus.PARTIALLY_PAID);
    }

    DebtEntity saved = debtRepository.save(entity);
    log.info("Wrote down debt {}, amount={}, newBalance={}", id, amount, newBalance);
    return toDto(saved);
  }

  private DebtEntity findEntityById(UUID id) {
    return debtRepository
        .findById(id)
        .orElseThrow(
            () ->
                new OpenDebtException(
                    "Debt not found: " + id,
                    "DEBT_NOT_FOUND",
                    OpenDebtException.ErrorSeverity.WARNING));
  }

  // AIDEV-NOTE: debtorId and creditorId are serialised as UUID strings in DebtDto (no PII).
  // Per ADR-0014, names/addresses are never stored here — only person-registry UUIDs.
  private DebtDto toDto(DebtEntity entity) {
    return DebtDto.builder()
        .id(entity.getId())
        .debtorId(entity.getDebtorPersonId() != null ? entity.getDebtorPersonId().toString() : null)
        .creditorId(entity.getCreditorOrgId() != null ? entity.getCreditorOrgId().toString() : null)
        .debtTypeCode(entity.getDebtTypeCode())
        .principalAmount(entity.getPrincipalAmount())
        .interestAmount(entity.getInterestAmount())
        .feesAmount(entity.getFeesAmount())
        .dueDate(entity.getDueDate())
        .originalDueDate(entity.getOriginalDueDate())
        .externalReference(entity.getExternalReference())
        .ocrLine(entity.getOcrLine())
        .outstandingBalance(entity.getOutstandingBalance())
        .status(
            entity.getStatus() != null
                ? DebtDto.DebtStatus.valueOf(entity.getStatus().name())
                : null)
        .readinessStatus(
            entity.getReadinessStatus() != null
                ? DebtDto.ReadinessStatus.valueOf(entity.getReadinessStatus().name())
                : null)
        .readinessRejectionReason(entity.getReadinessRejectionReason())
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getUpdatedAt())
        .createdBy(entity.getCreatedBy())
        .build();
  }

  private BigDecimal calculateTotal(DebtDto dto) {
    BigDecimal total =
        dto.getPrincipalAmount() != null ? dto.getPrincipalAmount() : BigDecimal.ZERO;
    if (dto.getInterestAmount() != null) {
      total = total.add(dto.getInterestAmount());
    }
    if (dto.getFeesAmount() != null) {
      total = total.add(dto.getFeesAmount());
    }
    return total;
  }

  private DebtEntity.DebtStatus mapStatus(DebtDto.DebtStatus status) {
    return DebtEntity.DebtStatus.valueOf(status.name());
  }

  private DebtEntity.ReadinessStatus mapReadiness(DebtDto.ReadinessStatus status) {
    return DebtEntity.ReadinessStatus.valueOf(status.name());
  }
}
