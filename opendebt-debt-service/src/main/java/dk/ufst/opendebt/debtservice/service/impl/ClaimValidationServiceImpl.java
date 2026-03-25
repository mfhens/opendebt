package dk.ufst.opendebt.debtservice.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import dk.ufst.opendebt.common.dto.DebtDto;
import dk.ufst.opendebt.debtservice.dto.ClaimValidationResult;
import dk.ufst.opendebt.debtservice.dto.ClaimValidationResult.ValidationError;
import dk.ufst.opendebt.debtservice.service.ClaimValidationService;

import lombok.extern.slf4j.Slf4j;

/**
 * Evaluates incoming claim data against the fordring validation rules (petitions 015-018). Rules
 * are implemented as direct Java checks matching the DMN decision tables in
 * rules-dmn/fordring-validation-rules.dmn.
 */
@Slf4j
@Service
public class ClaimValidationServiceImpl implements ClaimValidationService {

  private static final Set<String> VALID_ART_TYPES = Set.of("INDR", "MODR");
  private static final LocalDate EARLIEST_DATE = LocalDate.of(1900, 1, 1);
  private static final String RULE_ID_568 = "Rule568";

  @Override
  public ClaimValidationResult validate(DebtDto claim) {
    List<ValidationError> errors = new ArrayList<>();

    validateArtType(claim, errors);
    validateCurrency(errors);
    validateAmounts(claim, errors);
    validateDates(claim, errors);
    validateDescription(claim, errors);
    validateInterestRate(claim, errors);
    validateRequiredFields(claim, errors);

    log.info(
        "Claim validation complete: {} errors for claim type={}",
        errors.size(),
        claim.getDebtTypeCode());
    return ClaimValidationResult.builder().errors(errors).build();
  }

  /** Rule 411: Art type must be INDR or MODR. */
  private void validateArtType(DebtDto claim, List<ValidationError> errors) {
    String art = claim.getClaimArt();
    if (art != null && !art.isBlank() && !VALID_ART_TYPES.contains(art.toUpperCase())) {
      errors.add(
          ValidationError.builder()
              .ruleId("Rule411")
              .errorCode("FORDRING_TYPE_ERROR")
              .description("Fordringsart skal vaere inddrivelse (INDR) eller modregning (MODR)")
              .build());
    }
  }

  /** Rule 152: Currency must be DKK (implied — OpenDebt only supports DKK). */
  private void validateCurrency(List<ValidationError> errors) {
    // OpenDebt only operates in DKK; no currency field on DebtDto so this always passes.
  }

  /** Rule 215: Claim amount must be greater than lower limit. */
  private void validateAmounts(DebtDto claim, List<ValidationError> errors) {
    if (claim.getPrincipalAmount() != null
        && claim.getPrincipalAmount().compareTo(BigDecimal.ZERO) <= 0) {
      errors.add(
          ValidationError.builder()
              .ruleId("Rule215")
              .errorCode("FORDRING_AMOUNT_TOO_SMALL")
              .description("Fordringsbeloeb skal vaere stoerre end 0")
              .build());
    }

    if (claim.getOutstandingBalance() != null
        && claim.getOutstandingBalance().compareTo(BigDecimal.ZERO) < 0) {
      errors.add(
          ValidationError.builder()
              .ruleId("Rule215")
              .errorCode("FORDRING_AMOUNT_TOO_SMALL")
              .description("Restaeld kan ikke vaere negativ")
              .build());
    }
  }

  /** Rules 548, 568, 569: Date validation. */
  private void validateDates(DebtDto claim, List<ValidationError> errors) {
    LocalDate today = LocalDate.now();

    if (claim.getDueDate() != null && claim.getDueDate().isBefore(EARLIEST_DATE)) {
      errors.add(
          ValidationError.builder()
              .ruleId(RULE_ID_568)
              .errorCode("TIDLIGST_MULIG_DATO")
              .description("Datoer kan ikke ligge foer aar 1900")
              .build());
    }

    if (claim.getLimitationDate() != null) {
      if (claim.getLimitationDate().isBefore(EARLIEST_DATE)) {
        errors.add(
            ValidationError.builder()
                .ruleId(RULE_ID_568)
                .errorCode("TIDLIGST_MULIG_DATO")
                .description("Foraeldelsesdato kan ikke ligge foer aar 1900")
                .build());
      }
      if (claim.getLimitationDate().isBefore(today)) {
        errors.add(
            ValidationError.builder()
                .ruleId(RULE_ID_568)
                .errorCode("LIMITATION_DATE_EXPIRED")
                .description("Foraeldelsesdato er overskredet")
                .build());
      }
    }

    if (claim.getPeriodFrom() != null
        && claim.getPeriodTo() != null
        && claim.getPeriodFrom().isAfter(claim.getPeriodTo())) {
      errors.add(
          ValidationError.builder()
              .ruleId("Rule569")
              .errorCode("PERIODE_TIL_EFTER_PERIODE_FRA")
              .description("Periodens startdato kan ikke vaere efter slutdato")
              .build());
    }
  }

  /** GDPR: Description max 100 characters. */
  private void validateDescription(DebtDto claim, List<ValidationError> errors) {
    if (claim.getDescription() != null && claim.getDescription().length() > 100) {
      errors.add(
          ValidationError.builder()
              .ruleId("Rule413")
              .errorCode("REMARK_TOO_LONG")
              .description("Beskrivelse maa max vaere paa 100 tegn")
              .build());
    }
  }

  /** Rule 438: Interest rate must not be negative. */
  private void validateInterestRate(DebtDto claim, List<ValidationError> errors) {
    if (claim.getAdditionalInterestRate() != null
        && claim.getAdditionalInterestRate().compareTo(BigDecimal.ZERO) < 0) {
      errors.add(
          ValidationError.builder()
              .ruleId("Rule438")
              .errorCode("NEGATIVE_INTEREST_RATE")
              .description("Rentesats kan ikke vaere negativ")
              .build());
    }
  }

  /** Stamdata completeness (Rule 550). */
  private void validateRequiredFields(DebtDto claim, List<ValidationError> errors) {
    if (claim.getPrincipalAmount() == null) {
      errors.add(
          ValidationError.builder()
              .ruleId("Rule550")
              .errorCode("STAMDATA_MANGLER")
              .description("Hovedstol er paakraevet")
              .build());
    }

    if (claim.getDueDate() == null && claim.getPaymentDeadline() == null) {
      errors.add(
          ValidationError.builder()
              .ruleId("Rule550")
              .errorCode("STAMDATA_MANGLER")
              .description("Forfaldsdato eller betalingsfrist er paakraevet")
              .build());
    }

    if (claim.getDebtorId() == null || claim.getDebtorId().isBlank()) {
      errors.add(
          ValidationError.builder()
              .ruleId("Rule005")
              .errorCode("DEBTOR_NOT_FOUND")
              .description("Skyldner er paakraevet")
              .build());
    }

    if (claim.getDebtTypeCode() == null || claim.getDebtTypeCode().isBlank()) {
      errors.add(
          ValidationError.builder()
              .ruleId("Rule151")
              .errorCode("TYPE_AGREEMENT_MISSING")
              .description("Fordringstype er paakraevet")
              .build());
    }
  }
}
