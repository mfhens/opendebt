package dk.ufst.opendebt.rules.service;

import dk.ufst.opendebt.common.dto.fordring.FordringValidationRequest;
import dk.ufst.opendebt.common.dto.fordring.FordringValidationResult;

/**
 * Service interface for fordring (claim action) validation using Drools rules.
 *
 * <p>Validates incoming claim actions against the fordring integration specification rules. Uses a
 * separate KIE session (stateless) from the existing debt readiness/interest/priority rules to
 * ensure isolation.
 *
 * <p>The validation covers:
 *
 * <ul>
 *   <li>Structure validation (petition015) — required MF*Struktur presence
 *   <li>Currency validation (petition015) — DKK only
 *   <li>Art type validation (petition015) — INDR/MODR
 *   <li>Interest rate validation (petition015) — non-negative
 *   <li>Date validation (petition015) — range and logical consistency
 *   <li>Agreement validation (petition015) — existence and type
 *   <li>Debtor validation (petition015) — valid identifier
 * </ul>
 *
 * <p>Extension points exist for:
 *
 * <ul>
 *   <li>Claimant authorization rules (petition016)
 *   <li>Lifecycle/reference rules (petition017)
 *   <li>Content validation rules (petition018)
 * </ul>
 */
public interface FordringValidationService {

  /**
   * Validates a fordring action against all applicable core validation rules.
   *
   * <p>Evaluates structure, currency, art type, interest rate, date, agreement, and debtor
   * validation rules using a stateless Drools session. Returns all applicable errors; validation
   * does not short-circuit on first failure.
   *
   * @param request the fordring action to validate
   * @return the validation result with any errors found
   */
  FordringValidationResult validateFordring(FordringValidationRequest request);
}
