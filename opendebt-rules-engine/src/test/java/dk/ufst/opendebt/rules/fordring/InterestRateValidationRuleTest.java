package dk.ufst.opendebt.rules.fordring;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dk.ufst.opendebt.common.dto.fordring.FordringErrorCode;
import dk.ufst.opendebt.common.dto.fordring.FordringValidationRequest;
import dk.ufst.opendebt.common.dto.fordring.FordringValidationResult;
import dk.ufst.opendebt.rules.test.AbstractFordringRuleTest;

/** Tests for petition015 interest rate validation rules (error code 438). */
class InterestRateValidationRuleTest extends AbstractFordringRuleTest {

  @Test
  @DisplayName("Claim with positive interest rate passes validation")
  void positiveInterestRatePasses() {
    FordringValidationRequest request =
        validRequestBuilder("OPRETFORDRING")
            .mfOpretFordringStrukturPresent(true)
            .merRenteSats(new BigDecimal("5.5"))
            .build();

    FordringValidationResult result = fireRules(request);

    assertNoError(result, 438);
  }

  @Test
  @DisplayName("Claim with zero interest rate passes validation")
  void zeroInterestRatePasses() {
    FordringValidationRequest request =
        validRequestBuilder("OPRETFORDRING")
            .mfOpretFordringStrukturPresent(true)
            .merRenteSats(BigDecimal.ZERO)
            .build();

    FordringValidationResult result = fireRules(request);

    assertNoError(result, 438);
  }

  @Test
  @DisplayName("Claim with negative interest rate is rejected with error 438")
  void negativeInterestRateIsRejected() {
    FordringValidationRequest request =
        validRequestBuilder("OPRETFORDRING")
            .mfOpretFordringStrukturPresent(true)
            .merRenteSats(new BigDecimal("-2.0"))
            .build();

    FordringValidationResult result = fireRules(request);

    assertHasError(result, FordringErrorCode.NEGATIVE_INTEREST_RATE);
    assertErrorMessageContains(result, 438, "MerRenteSats kan ikke være negativ");
  }
}
