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
@Slf4j
@Service
@RequiredArgsConstructor
public class OverpaymentRulesServiceImpl implements OverpaymentRulesService {

  // TODO: Inject RulesService from opendebt-rules-engine once sagstype/frivillig rules are defined

  @Override
  public OverpaymentOutcome resolveOutcome(UUID debtId) {
    log.info(
        "Resolving overpayment outcome for debt {} (placeholder: defaulting to PAYOUT)", debtId);
    // Placeholder: default to PAYOUT until Drools rules for sagstype / frivillig indbetaling
    // are implemented in a future petition.
    return OverpaymentOutcome.PAYOUT;
  }
}
