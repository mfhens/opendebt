package dk.ufst.opendebt.payment.service.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;

import dk.ufst.opendebt.payment.dto.OverpaymentOutcome;
import dk.ufst.opendebt.payment.service.OverpaymentRulesService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Placeholder implementation for overpayment outcome resolution. The detailed rules (based on
 * sagstype and whether the payment is a frivillig indbetaling) are not yet defined and will be
 * implemented via the Drools rules engine (ADR-0015) in a future petition.
 *
 * <p>Currently defaults to {@link OverpaymentOutcome#PAYOUT}.
 */
// AIDEV-NOTE: Deliberate placeholder — DO NOT add business logic here. Outcome rules belong in
// Drools (.drl) once sagstype/frivillig indbetaling classification is formally specified.
@Slf4j
@Service
@RequiredArgsConstructor
public class OverpaymentRulesServiceImpl implements OverpaymentRulesService {

  // AIDEV-TODO: Inject RulesService from ufst-rules-lib and replace the hardcoded default.
  // Blocked on: case type (sagstype) model and frivillig indbetaling flag in DebtEntity.

  @Override
  public OverpaymentOutcome resolveOutcome(UUID debtId) {
    log.info(
        "Resolving overpayment outcome for debt {} (placeholder: defaulting to PAYOUT)", debtId);
    // AIDEV-NOTE: Defaults to PAYOUT — safe fallback; real rules will cover COVER_OTHER_DEBTS path.
    return OverpaymentOutcome.PAYOUT;
  }
}
