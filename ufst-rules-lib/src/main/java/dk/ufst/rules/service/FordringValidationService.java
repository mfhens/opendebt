package dk.ufst.rules.service;

import dk.ufst.opendebt.common.dto.fordring.FordringValidationRequest;
import dk.ufst.opendebt.common.dto.fordring.FordringValidationResult;

/**
 * Service interface for fordring (claim action) validation using Drools rules.
 *
 * <p>Validates incoming claim actions against the fordring integration specification rules. Uses a
 * separate KIE session (stateless) from the existing debt readiness/interest/priority rules to
 * ensure isolation.
 */
public interface FordringValidationService {

  /**
   * Validates a fordring action against all applicable core validation rules.
   *
   * @param request the fordring action to validate
   * @return the validation result with any errors found
   */
  FordringValidationResult validateFordring(FordringValidationRequest request);
}
