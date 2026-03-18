package dk.ufst.opendebt.debtservice.service;

import dk.ufst.opendebt.common.dto.DebtDto;
import dk.ufst.opendebt.debtservice.dto.ClaimValidationResult;

/**
 * Validates incoming claim data against the fordring DMN decision tables (petitions 015-018).
 * Returns a structured result with error codes and Danish descriptions.
 */
public interface ClaimValidationService {

  ClaimValidationResult validate(DebtDto claim);
}
