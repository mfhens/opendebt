package dk.ufst.opendebt.payment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import dk.ufst.opendebt.payment.dto.OverpaymentOutcome;

class OverpaymentRulesServiceImplTest {

  private final OverpaymentRulesServiceImpl service = new OverpaymentRulesServiceImpl();

  @Test
  void resolveOutcome_returnsPayoutAsDefault() {
    OverpaymentOutcome result = service.resolveOutcome(UUID.randomUUID());

    assertThat(result).isEqualTo(OverpaymentOutcome.PAYOUT);
  }
}
