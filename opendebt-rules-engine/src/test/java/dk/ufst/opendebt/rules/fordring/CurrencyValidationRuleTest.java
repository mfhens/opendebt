package dk.ufst.opendebt.rules.fordring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import dk.ufst.opendebt.common.dto.fordring.FordringErrorCode;
import dk.ufst.opendebt.common.dto.fordring.FordringValidationRequest;
import dk.ufst.opendebt.common.dto.fordring.FordringValidationResult;
import dk.ufst.opendebt.rules.test.AbstractFordringRuleTest;

/** Tests for petition015 currency validation rules (error code 152). */
class CurrencyValidationRuleTest extends AbstractFordringRuleTest {

  @Test
  @DisplayName("Claim with DKK currency passes validation")
  void claimWithDkkPasses() {
    FordringValidationRequest request =
        validRequestBuilder("OPRETFORDRING")
            .mfOpretFordringStrukturPresent(true)
            .valutaKode("DKK")
            .build();

    FordringValidationResult result = fireRules(request);

    assertNoError(result, 152);
  }

  @Test
  @DisplayName("Claim with non-DKK currency (EUR) is rejected with error 152")
  void claimWithEurIsRejected() {
    FordringValidationRequest request =
        validRequestBuilder("OPRETFORDRING")
            .mfOpretFordringStrukturPresent(true)
            .valutaKode("EUR")
            .build();

    FordringValidationResult result = fireRules(request);

    assertHasError(result, FordringErrorCode.INVALID_CURRENCY);
    assertErrorMessageContains(result, 152, "ValutaKode ifølge fordringhaveraftale");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "OPRETFORDRING",
        "GENINDSENDFORDRING",
        "NEDSKRIV",
        "OPSKRIVNINGREGULERING",
        "OPSKRIVNINGOMGJORTNEDSKRIVNINGREGULERING",
        "OPSKRIVNINGANNULLERETNEDSKRIVNINGINDBETALING"
      })
  @DisplayName("Currency validation applies to applicable action types")
  void currencyValidationAppliesToActionTypes(String actionType) {
    FordringValidationRequest.FordringValidationRequestBuilder builder =
        validRequestBuilder(actionType).valutaKode("USD");

    // Set appropriate structure flags
    switch (actionType) {
      case "OPRETFORDRING" -> builder.mfOpretFordringStrukturPresent(true);
      case "GENINDSENDFORDRING" -> builder.mfGenindsendFordringStrukturPresent(true);
      case "NEDSKRIV" -> builder.mfNedskrivFordringStrukturPresent(true);
      case "OPSKRIVNINGREGULERING" -> builder.mfOpskrivningReguleringStrukturPresent(true);
      case "OPSKRIVNINGOMGJORTNEDSKRIVNINGREGULERING" ->
          builder.mfOpskrivningOmgjortNedskrivningReguleringStrukturPresent(true);
      case "OPSKRIVNINGANNULLERETNEDSKRIVNINGINDBETALING" ->
          builder.mfOpskrivningAnnulleretNedskrivningIndbetalingStrukturPresent(true);
      default -> throw new IllegalArgumentException("Unexpected action type: " + actionType);
    }

    FordringValidationResult result = fireRules(builder.build());

    assertHasError(result, 152);
  }
}
