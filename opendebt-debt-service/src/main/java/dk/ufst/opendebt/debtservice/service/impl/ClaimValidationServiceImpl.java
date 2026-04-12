package dk.ufst.opendebt.debtservice.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import dk.ufst.opendebt.common.dto.DebtDto;
import dk.ufst.opendebt.common.dto.fordring.FordringValidationError;
import dk.ufst.opendebt.common.dto.fordring.FordringValidationResult;
import dk.ufst.opendebt.debtservice.dto.ClaimValidationResult;
import dk.ufst.opendebt.debtservice.dto.ClaimValidationResult.ValidationError;
import dk.ufst.opendebt.debtservice.mapper.ClaimValidationRequestMapper;
import dk.ufst.opendebt.debtservice.service.ClaimValidationContext;
import dk.ufst.opendebt.debtservice.service.ClaimValidationService;
import dk.ufst.rules.service.FordringValidationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Adapts the debt-service submission contract to the shared fordring Drools validation library. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimValidationServiceImpl implements ClaimValidationService {

  private final ClaimValidationRequestMapper requestMapper;
  private final FordringValidationService fordringValidationService;

  @Override
  public ClaimValidationResult validate(DebtDto claim, ClaimValidationContext context) {
    FordringValidationResult droolsResult =
        fordringValidationService.validateFordring(requestMapper.toRequest(claim, context));
    List<ValidationError> errors =
        droolsResult.getErrors().stream().map(this::toValidationError).toList();

    log.info(
        "Claim validation complete: {} errors for claim type={} via {}",
        errors.size(),
        claim.getDebtTypeCode(),
        context.ingressPath());
    return ClaimValidationResult.builder().errors(errors).build();
  }

  private ValidationError toValidationError(FordringValidationError error) {
    return ValidationError.builder()
        .ruleId("Rule" + error.getErrorCode())
        .errorCode(
            error.getErrorCodeEnum() != null
                ? error.getErrorCodeEnum().name()
                : Integer.toString(error.getErrorCode()))
        .description(error.getMessage())
        .build();
  }
}
