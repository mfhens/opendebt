package dk.ufst.opendebt.debtservice.service;

import dk.ufst.opendebt.common.dto.DebtDto;
import dk.ufst.opendebt.debtservice.dto.ClaimValidationResult;

/** Validates incoming claim data against the shared fordring Drools rules (petitions 015-018). */
public interface ClaimValidationService {

  ClaimValidationResult validate(DebtDto claim, ClaimValidationContext context);

  default ClaimValidationResult validate(DebtDto claim) {
    return validate(claim, ClaimValidationContext.internal());
  }
}
