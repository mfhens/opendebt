package dk.ufst.opendebt.debtservice.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.ufst.opendebt.common.dto.DebtDto;
import dk.ufst.opendebt.common.exception.OpenDebtException;
import dk.ufst.opendebt.debtservice.client.CreditorAction;
import dk.ufst.opendebt.debtservice.client.CreditorServiceClient;
import dk.ufst.opendebt.debtservice.client.ValidateActionRequest;
import dk.ufst.opendebt.debtservice.client.ValidateActionResponse;
import dk.ufst.opendebt.debtservice.config.FordringMetrics;
import dk.ufst.opendebt.debtservice.dto.ClaimCountsDto;
import dk.ufst.opendebt.debtservice.dto.ClaimDetailResponseDto;
import dk.ufst.opendebt.debtservice.dto.CreditorClaimListItemDto;
import dk.ufst.opendebt.debtservice.entity.ClaimArtEnum;
import dk.ufst.opendebt.debtservice.entity.ClaimLifecycleState;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.entity.InterestSelectionEmbeddable;
import dk.ufst.opendebt.debtservice.exception.CreditorValidationException;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;
import dk.ufst.opendebt.debtservice.service.DebtService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DebtServiceImpl implements DebtService {

  private final DebtRepository debtRepository;
  private final CreditorServiceClient creditorServiceClient;
  private final FordringMetrics fordringMetrics;

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
  public List<DebtDto> getDebtsByIds(List<UUID> ids) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    return debtRepository.findAllById(ids).stream().map(this::toDto).toList();
  }

  @Override
  public List<DebtDto> getDebtsByDebtor(String debtorId) {
    UUID debtorPersonId = UUID.fromString(debtorId);
    return debtRepository.findByDebtorPersonId(debtorPersonId).stream().map(this::toDto).toList();
  }

  @Override
  public List<DebtDto> getDebtsByCreditor(String creditorId) {
    UUID creditorOrgId = UUID.fromString(creditorId);
    return debtRepository.findByCreditorOrgId(creditorOrgId).stream().map(this::toDto).toList();
  }

  @Override
  @Transactional
  public DebtDto createDebt(DebtDto dto) {
    UUID creditorOrgId = UUID.fromString(dto.getCreditorId());
    validateCreditorAction(creditorOrgId, CreditorAction.CREATE_CLAIM);

    DebtEntity entity =
        DebtEntity.builder()
            .debtorPersonId(UUID.fromString(dto.getDebtorId()))
            .creditorOrgId(creditorOrgId)
            .debtTypeCode(dto.getDebtTypeCode())
            .principalAmount(dto.getPrincipalAmount())
            .interestAmount(dto.getInterestAmount())
            .feesAmount(dto.getFeesAmount())
            .dueDate(dto.getDueDate())
            .originalDueDate(dto.getOriginalDueDate())
            .externalReference(dto.getExternalReference())
            .ocrLine(dto.getOcrLine())
            .outstandingBalance(calculateTotal(dto))
            .claimArt(parseClaimArt(dto.getClaimArt()))
            .creditorReference(dto.getCreditorReference())
            .description(dto.getDescription())
            .limitationDate(dto.getLimitationDate())
            .periodFrom(dto.getPeriodFrom())
            .periodTo(dto.getPeriodTo())
            .inceptionDate(dto.getInceptionDate())
            .paymentDeadline(dto.getPaymentDeadline())
            .lastPaymentDate(dto.getLastPaymentDate())
            .estateProcessing(dto.getEstateProcessing())
            .judgmentDate(dto.getJudgmentDate())
            .settlementDate(dto.getSettlementDate())
            .interestSelection(
                InterestSelectionEmbeddable.builder()
                    .interestRule(dto.getInterestRule())
                    .interestRateCode(dto.getInterestRateCode())
                    .additionalInterestRate(dto.getAdditionalInterestRate())
                    .build())
            .claimNote(dto.getClaimNote())
            .customerNote(dto.getCustomerNote())
            .status(DebtEntity.DebtStatus.PENDING)
            .readinessStatus(DebtEntity.ReadinessStatus.PENDING_REVIEW)
            .build();
    DebtEntity saved = debtRepository.save(entity);
    fordringMetrics.recordSubmission();
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
    validateCreditorAction(entity.getCreditorOrgId(), CreditorAction.UPDATE_CLAIM);

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
    return debtRepository.findByOcrLine(ocrLine).stream().map(this::toDto).toList();
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

  @Override
  public DebtDto toDto(DebtEntity entity) {
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
        .claimArt(entity.getClaimArt() != null ? entity.getClaimArt().name() : null)
        .claimCategory(entity.getClaimCategory() != null ? entity.getClaimCategory().name() : null)
        .creditorReference(entity.getCreditorReference())
        .description(entity.getDescription())
        .limitationDate(entity.getLimitationDate())
        .periodFrom(entity.getPeriodFrom())
        .periodTo(entity.getPeriodTo())
        .inceptionDate(entity.getInceptionDate())
        .paymentDeadline(entity.getPaymentDeadline())
        .lastPaymentDate(entity.getLastPaymentDate())
        .estateProcessing(entity.getEstateProcessing())
        .judgmentDate(entity.getJudgmentDate())
        .settlementDate(entity.getSettlementDate())
        .interestRule(
            entity.getInterestSelection() != null
                ? entity.getInterestSelection().getInterestRule()
                : null)
        .interestRateCode(
            entity.getInterestSelection() != null
                ? entity.getInterestSelection().getInterestRateCode()
                : null)
        .additionalInterestRate(
            entity.getInterestSelection() != null
                ? entity.getInterestSelection().getAdditionalInterestRate()
                : null)
        .claimNote(entity.getClaimNote())
        .customerNote(entity.getCustomerNote())
        .lifecycleState(
            entity.getLifecycleState() != null ? entity.getLifecycleState().name() : null)
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

  private ClaimArtEnum parseClaimArt(String claimArt) {
    if (claimArt == null || claimArt.isBlank()) {
      return ClaimArtEnum.INDR;
    }
    try {
      return ClaimArtEnum.valueOf(claimArt.toUpperCase());
    } catch (IllegalArgumentException e) {
      return ClaimArtEnum.INDR;
    }
  }

  @Override
  public ClaimCountsDto getClaimCounts(UUID creditorOrgId) {
    return ClaimCountsDto.builder()
        .inRecovery(
            debtRepository.countByCreditorAndLifecycleState(
                creditorOrgId, ClaimLifecycleState.OVERDRAGET))
        .inHearing(
            debtRepository.countByCreditorAndLifecycleState(
                creditorOrgId, ClaimLifecycleState.HOERING))
        .rejected(
            debtRepository.countByCreditorAndLifecycleState(
                creditorOrgId, ClaimLifecycleState.TILBAGEKALDT))
        .zeroBalance(debtRepository.countZeroBalanceByCreditor(creditorOrgId))
        .build();
  }

  @Override
  public Page<CreditorClaimListItemDto> getClaimsForCreditor(
      UUID creditorId, String status, boolean excludeZeroBalance, Pageable pageable) {

    ClaimLifecycleState state =
        switch (status != null ? status.toUpperCase() : "") {
          case "IN_RECOVERY" -> ClaimLifecycleState.OVERDRAGET;
          case "HEARING" -> ClaimLifecycleState.HOERING;
          case "REJECTED" -> ClaimLifecycleState.TILBAGEKALDT;
          default -> null;
        };
    boolean zeroBalanceOnly = "ZERO_BALANCE".equalsIgnoreCase(status);

    return debtRepository
        .findClaimsForCreditor(creditorId, state, zeroBalanceOnly, excludeZeroBalance, pageable)
        .map(this::toClaimListItemDto);
  }

  private CreditorClaimListItemDto toClaimListItemDto(DebtEntity e) {
    BigDecimal interest = e.getInterestAmount() != null ? e.getInterestAmount() : BigDecimal.ZERO;
    BigDecimal fees = e.getFeesAmount() != null ? e.getFeesAmount() : BigDecimal.ZERO;
    BigDecimal principal =
        e.getPrincipalAmount() != null ? e.getPrincipalAmount() : BigDecimal.ZERO;

    return CreditorClaimListItemDto.builder()
        .claimId(e.getId())
        .creditorReference(e.getCreditorReference())
        .claimTypeName(e.getDebtTypeCode())
        .debtorType(null)
        .debtorIdentifier(null)
        .debtorCount(1)
        .receivedDate(e.getCreatedAt() != null ? e.getCreatedAt().toLocalDate() : null)
        .reportingTimestamp(e.getCreatedAt())
        .claimStatus(e.getLifecycleState() != null ? e.getLifecycleState().name() : null)
        .hearingStatus(e.getLifecycleState() != null ? e.getLifecycleState().name() : null)
        .incorporationDate(e.getInceptionDate())
        .periodFrom(e.getPeriodFrom())
        .periodTo(e.getPeriodTo())
        .amountSentForRecovery(principal)
        .balance(e.getOutstandingBalance())
        .balanceWithInterestAndFees(principal.add(interest).add(fees))
        .zeroBalanceReachedDate(null)
        .errorDescription(null)
        .errorCount(0)
        .caseId(null)
        .actionCode(null)
        .build();
  }

  private void validateCreditorAction(UUID creditorOrgId, CreditorAction action) {
    ValidateActionRequest request = ValidateActionRequest.builder().requestedAction(action).build();
    ValidateActionResponse response = creditorServiceClient.validateAction(creditorOrgId, request);

    if (!response.isAllowed()) {
      String reasonCode = response.getReasonCode() != null ? response.getReasonCode() : "UNKNOWN";
      String message =
          response.getMessage() != null
              ? response.getMessage()
              : "Creditor is not permitted to perform action: " + action;
      log.warn("Creditor {} denied action {}: {} - {}", creditorOrgId, action, reasonCode, message);
      throw new CreditorValidationException(message, reasonCode);
    }
    log.debug("Creditor {} authorized for action {}", creditorOrgId, action);
  }

  @Override
  public ClaimDetailResponseDto getClaimDetail(UUID claimId) {
    return debtRepository.findById(claimId).map(this::toClaimDetailResponseDto).orElse(null);
  }

  private ClaimDetailResponseDto toClaimDetailResponseDto(DebtEntity e) {
    BigDecimal principal =
        e.getPrincipalAmount() != null ? e.getPrincipalAmount() : BigDecimal.ZERO;
    BigDecimal interest = e.getInterestAmount() != null ? e.getInterestAmount() : BigDecimal.ZERO;
    BigDecimal fees = e.getFeesAmount() != null ? e.getFeesAmount() : BigDecimal.ZERO;
    BigDecimal outstanding =
        e.getOutstandingBalance() != null ? e.getOutstandingBalance() : principal;

    InterestSelectionEmbeddable sel = e.getInterestSelection();

    return ClaimDetailResponseDto.builder()
        .claimId(e.getId())
        .claimType(e.getDebtTypeCode())
        .claimCategory(e.getClaimCategory() != null ? e.getClaimCategory().name() : null)
        .creditorDescription(e.getDescription())
        .receivedDate(e.getReceivedAt() != null ? e.getReceivedAt().toLocalDate() : null)
        .periodFrom(e.getPeriodFrom())
        .periodTo(e.getPeriodTo())
        .incorporationDate(e.getInceptionDate())
        .dueDate(e.getDueDate())
        .limitationDate(e.getLimitationDate())
        .courtDate(e.getJudgmentDate())
        .lastTimelyPaymentDate(e.getLastPaymentDate())
        .creditorReference(e.getCreditorReference())
        .obligationId(e.getExternalReference())
        .relatedObligationId(null)
        .interestRule(sel != null ? sel.getInterestRule() : null)
        .interestRate(sel != null ? sel.getAdditionalInterestRate() : null)
        .extraInterestRate(null)
        .totalDebt(principal.add(interest).add(fees))
        .latestInterestAccrualDate(null)
        .originalPrincipal(principal)
        .receivedAmount(BigDecimal.ZERO)
        .claimBalance(outstanding)
        .totalCreditorBalance(outstanding)
        .amountSentForRecovery(principal)
        .amountSentForRecoveryWithWriteUps(principal)
        .debtorCount(1)
        .zeroBalanceExpired(outstanding.compareTo(BigDecimal.ZERO) == 0)
        .build();
  }
}
