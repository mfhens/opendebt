package dk.ufst.opendebt.rules.fordring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dk.ufst.opendebt.common.dto.fordring.FordringValidationRequest;
import dk.ufst.opendebt.common.dto.fordring.FordringValidationResult;
import dk.ufst.opendebt.rules.test.AbstractFordringRuleTest;

/** Tests for petition015 combined validation — verifies all rules run together. */
class CombinedCoreValidationRuleTest extends AbstractFordringRuleTest {

  @Test
  @DisplayName("Valid OPRETFORDRING passes all core validations")
  void validOpretfordringPassesAll() {
    FordringValidationRequest request = createValidOpretFordringRequest();

    FordringValidationResult result = fireRules(request);

    assertValid(result);
  }

  @Test
  @DisplayName("Valid GENINDSENDFORDRING passes all core validations")
  void validGenindsendfordringPassesAll() {
    FordringValidationRequest request = createValidGenindsendFordringRequest();

    FordringValidationResult result = fireRules(request);

    assertValid(result);
  }

  @Test
  @DisplayName("Multiple violations produce multiple errors")
  void multipleViolationsProduceMultipleErrors() {
    FordringValidationRequest request =
        validRequestBuilder("OPRETFORDRING")
            .mfOpretFordringStrukturPresent(false) // Error 444
            .valutaKode("EUR") // Error 152
            .artType("INVALID") // Error 411
            .debtorId("0") // Error 5
            .agreementFound(false) // Error 2
            .build();

    FordringValidationResult result = fireRules(request);

    assertHasError(result, 444);
    assertHasError(result, 152);
    assertHasError(result, 411);
    assertHasError(result, 5);
    assertHasError(result, 2);
  }
}
