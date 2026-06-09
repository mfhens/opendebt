package dk.ufst.opendebt.debtservice.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.ufst.opendebt.debtservice.client.CreditorDisplayClient;
import dk.ufst.opendebt.debtservice.dto.CitizenDebtItemDto;
import dk.ufst.opendebt.debtservice.dto.CitizenDebtStatus;
import dk.ufst.opendebt.debtservice.dto.CitizenDebtSummaryResponse;
import dk.ufst.opendebt.debtservice.dto.CitizenEffectiveInterestRateDto;
import dk.ufst.opendebt.debtservice.dto.InterestAccrualState;
import dk.ufst.opendebt.debtservice.dto.InterestPauseReasonCode;
import dk.ufst.opendebt.debtservice.dto.WrittenOffReasonCode;
import dk.ufst.opendebt.debtservice.dto.config.ConfigEntryDto;
import dk.ufst.opendebt.debtservice.entity.ClaimLifecycleState;
import dk.ufst.opendebt.debtservice.entity.DebtEntity;
import dk.ufst.opendebt.debtservice.entity.InterestRuleCode;
import dk.ufst.opendebt.debtservice.entity.InterestSelectionEmbeddable;
import dk.ufst.opendebt.debtservice.repository.DebtRepository;
import dk.ufst.opendebt.debtservice.service.BusinessConfigService;
import dk.ufst.opendebt.debtservice.service.CitizenDebtService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CitizenDebtServiceImpl implements CitizenDebtService {

  private static final BigDecimal ZERO = BigDecimal.ZERO;
  private static final BigDecimal FALLBACK_ANNUAL_RATE = new BigDecimal("0.0575");
  private static final InterestRuleCode DEFAULT_INTEREST_RULE = InterestRuleCode.INDR_STD;

  private final DebtRepository debtRepository;
  private final CreditorDisplayClient creditorDisplayClient;
  private final BusinessConfigService businessConfigService;

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

    Map<UUID, String> creditorDisplayNames = resolveCreditorDisplayNames(debtsPage.getContent());
    Map<String, CitizenEffectiveInterestRateDto> effectiveInterestRates = new LinkedHashMap<>();

    List<CitizenDebtItemDto> debtItems =
        debtsPage.getContent().stream()
            .map(
                debt ->
                    mapToCitizenDto(
                        debt,
                        creditorDisplayNames.get(debt.getCreditorOrgId()),
                        effectiveInterestRates))
            .toList();

    // Calculate totals across ALL debts (not just current page)
    List<DebtEntity> allDebts = debtRepository.findByDebtorPersonId(personId);
    BigDecimal totalOutstanding =
        allDebts.stream()
            .map(DebtEntity::getOutstandingBalance)
            .filter(balance -> balance != null)
            .reduce(ZERO, BigDecimal::add);

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
        .effectiveInterestRates(List.copyOf(effectiveInterestRates.values()))
        .build();
  }

  private Map<UUID, String> resolveCreditorDisplayNames(List<DebtEntity> debts) {
    return debts.stream()
        .map(DebtEntity::getCreditorOrgId)
        .distinct()
        .collect(
            Collectors.toMap(
                creditorOrgId -> creditorOrgId,
                creditorDisplayClient::getDisplayName,
                (left, right) -> left,
                LinkedHashMap::new));
  }

  private CitizenDebtItemDto mapToCitizenDto(
      DebtEntity debt,
      String creditorDisplayName,
      Map<String, CitizenEffectiveInterestRateDto> effectiveInterestRates) {
    CitizenDebtStatus citizenStatus = resolveCitizenStatus(debt);
    InterestAccrualState interestAccrualState =
        debt.isIkkeinddrivelsesparat() ? InterestAccrualState.PAUSED : InterestAccrualState.ACTIVE;
    InterestPauseReasonCode interestPauseReasonCode =
        resolveInterestPauseReasonCode(debt, interestAccrualState);
    WrittenOffReasonCode writtenOffReasonCode = resolveWrittenOffReasonCode(debt, citizenStatus);
    String statusReasonCode = writtenOffReasonCode != null ? writtenOffReasonCode.name() : null;

    InterestRuleCode interestRuleCode = resolveInterestRuleCode(debt);
    InterestRateMetadata interestRateMetadata =
        shouldExposeInterestRateMetadata(citizenStatus, interestAccrualState, debt)
            ? resolveInterestRateMetadata(debt, interestRuleCode)
            : null;

    if (interestRateMetadata != null) {
      effectiveInterestRates.putIfAbsent(
          interestRateMetadata.uniqueKey(),
          CitizenEffectiveInterestRateDto.builder()
              .interestRuleCode(interestRateMetadata.interestRuleCode())
              .annualRate(interestRateMetadata.annualRate())
              .validFrom(interestRateMetadata.validFrom())
              .build());
    }

    return CitizenDebtItemDto.builder()
        .debtId(debt.getId())
        .debtTypeCode(debt.getDebtTypeCode())
        .debtTypeName(getDebtTypeName(debt.getDebtTypeCode()))
        .creditorDisplayName(creditorDisplayName)
        .principalAmount(monetaryOrZero(debt.getPrincipalAmount()))
        .outstandingAmount(monetaryOrZero(debt.getOutstandingBalance()))
        .interestAmount(monetaryOrZero(debt.getInterestAmount()))
        .feesAmount(monetaryOrZero(debt.getFeesAmount()))
        .dueDate(debt.getDueDate())
        .status(debt.getStatus() != null ? debt.getStatus().name() : null)
        .citizenStatus(citizenStatus)
        .statusReasonCode(statusReasonCode)
        .interestAccrualState(interestAccrualState)
        .interestPauseReasonCode(interestPauseReasonCode)
        .interestRuleCode(interestRuleCode.name())
        .currentInterestRate(
            interestRateMetadata != null ? interestRateMetadata.annualRate() : null)
        .writtenOffReasonCode(writtenOffReasonCode)
        .build();
  }

  private BigDecimal monetaryOrZero(BigDecimal value) {
    return value != null ? value : ZERO;
  }

  private CitizenDebtStatus resolveCitizenStatus(DebtEntity debt) {
    ClaimLifecycleState lifecycleState = debt.getLifecycleState();
    if (debt.getStatus() == DebtEntity.DebtStatus.WRITTEN_OFF
        || lifecycleState == ClaimLifecycleState.AFSKREVET) {
      return CitizenDebtStatus.WRITTEN_OFF;
    }
    if (debt.getStatus() == DebtEntity.DebtStatus.PAID
        || lifecycleState == ClaimLifecycleState.INDFRIET) {
      return CitizenDebtStatus.PAID;
    }
    if (debt.getModregningTier() != null) {
      return CitizenDebtStatus.SET_OFF;
    }
    if (debt.getStatus() == DebtEntity.DebtStatus.DISPUTED
        || lifecycleState == ClaimLifecycleState.HOERING) {
      return CitizenDebtStatus.DISPUTED;
    }
    return CitizenDebtStatus.IN_COLLECTION;
  }

  private InterestPauseReasonCode resolveInterestPauseReasonCode(
      DebtEntity debt, InterestAccrualState interestAccrualState) {
    if (interestAccrualState != InterestAccrualState.PAUSED) {
      return null;
    }
    InterestPauseReasonCode explicitCode =
        parseEnumCode(debt.getClaimNote(), InterestPauseReasonCode.class);
    return explicitCode != null
        ? explicitCode
        : InterestPauseReasonCode.CLAIM_UNCLEAR_DEBTOR_CANNOT_PAY;
  }

  private WrittenOffReasonCode resolveWrittenOffReasonCode(
      DebtEntity debt, CitizenDebtStatus citizenStatus) {
    if (citizenStatus != CitizenDebtStatus.WRITTEN_OFF) {
      return null;
    }

    WrittenOffReasonCode explicitCode =
        parseEnumCode(debt.getClaimNote(), WrittenOffReasonCode.class);
    if (explicitCode != null) {
      return explicitCode;
    }
    if (Boolean.TRUE.equals(debt.getEstateProcessing())) {
      return WrittenOffReasonCode.ESTATE_OF_DECEASED;
    }
    if (debt.getLimitationDate() != null && !debt.getLimitationDate().isAfter(LocalDate.now())) {
      return WrittenOffReasonCode.LIMITATION_EXPIRED;
    }
    return null;
  }

  private InterestRuleCode resolveInterestRuleCode(DebtEntity debt) {
    InterestSelectionEmbeddable interestSelection = debt.getInterestSelection();
    if (interestSelection != null
        && interestSelection.getInterestRule() != null
        && !interestSelection.getInterestRule().isBlank()) {
      try {
        return InterestRuleCode.valueOf(
            interestSelection.getInterestRule().trim().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException ex) {
        log.warn(
            "Unknown interest rule '{}' for debt={}, defaulting to {}",
            interestSelection.getInterestRule(),
            debt.getId(),
            DEFAULT_INTEREST_RULE);
      }
    }
    return DEFAULT_INTEREST_RULE;
  }

  private boolean shouldExposeInterestRateMetadata(
      CitizenDebtStatus citizenStatus, InterestAccrualState interestAccrualState, DebtEntity debt) {
    return debt.getInterestSelection() != null
        && interestAccrualState == InterestAccrualState.ACTIVE
        && citizenStatus != CitizenDebtStatus.PAID
        && citizenStatus != CitizenDebtStatus.WRITTEN_OFF;
  }

  private InterestRateMetadata resolveInterestRateMetadata(
      DebtEntity debt, InterestRuleCode interestRuleCode) {
    if (interestRuleCode.isExempt()) {
      return null;
    }

    InterestSelectionEmbeddable interestSelection = debt.getInterestSelection();
    if (interestRuleCode.usesContractualRate()) {
      if (interestSelection == null || interestSelection.getAdditionalInterestRate() == null) {
        return null;
      }
      return new InterestRateMetadata(
          interestRuleCode.name(), interestSelection.getAdditionalInterestRate(), LocalDate.now());
    }

    String configKey = resolveConfigKey(interestSelection, interestRuleCode);
    if (configKey == null) {
      return null;
    }

    LocalDate effectiveDate = LocalDate.now();
    ConfigEntryDto configEntry =
        businessConfigService.findEffectiveEntry(configKey, effectiveDate).orElse(null);
    if (configEntry == null) {
      log.warn(
          "No effective business config for key={} on {}; falling back to {}",
          configKey,
          effectiveDate,
          FALLBACK_ANNUAL_RATE);
      return new InterestRateMetadata(interestRuleCode.name(), FALLBACK_ANNUAL_RATE, effectiveDate);
    }

    try {
      return new InterestRateMetadata(
          interestRuleCode.name(),
          new BigDecimal(configEntry.getConfigValue()),
          configEntry.getValidFrom() != null ? configEntry.getValidFrom() : effectiveDate);
    } catch (NumberFormatException ex) {
      log.warn(
          "No effective business config for key={} on {}; falling back to {}",
          configKey,
          effectiveDate,
          FALLBACK_ANNUAL_RATE);
      return new InterestRateMetadata(interestRuleCode.name(), FALLBACK_ANNUAL_RATE, effectiveDate);
    }
  }

  private String resolveConfigKey(
      InterestSelectionEmbeddable interestSelection, InterestRuleCode interestRuleCode) {
    if (interestSelection != null
        && interestSelection.getInterestRateCode() != null
        && !interestSelection.getInterestRateCode().isBlank()) {
      return interestSelection.getInterestRateCode();
    }
    return interestRuleCode.getConfigKey();
  }

  private <T extends Enum<T>> T parseEnumCode(String rawCode, Class<T> enumType) {
    if (rawCode == null || rawCode.isBlank()) {
      return null;
    }
    try {
      return Enum.valueOf(enumType, rawCode.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      return null;
    }
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

  private record InterestRateMetadata(
      String interestRuleCode, BigDecimal annualRate, LocalDate validFrom) {

    String uniqueKey() {
      return interestRuleCode
          + "|"
          + annualRate.stripTrailingZeros().toPlainString()
          + "|"
          + validFrom;
    }
  }
}
