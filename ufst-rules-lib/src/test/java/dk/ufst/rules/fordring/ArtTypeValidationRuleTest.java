package dk.ufst.rules.fordring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dk.ufst.opendebt.common.dto.fordring.FordringErrorCode;
import dk.ufst.opendebt.common.dto.fordring.FordringValidationRequest;
import dk.ufst.opendebt.common.dto.fordring.FordringValidationResult;
import dk.ufst.rules.test.AbstractFordringRuleTest;

/** Tests for petition015 art type validation rules (error code 411). */
class ArtTypeValidationRuleTest extends AbstractFordringRuleTest {

  @Test
  @DisplayName("OPRETFORDRING with art type INDR passes validation")
  void opretfordringWithIndrPasses() {
    FordringValidationRequest request =
        validRequestBuilder("OPRETFORDRING")
            .mfOpretFordringStrukturPresent(true)
            .artType("INDR")
            .build();

    FordringValidationResult result = fireRules(request);

    assertNoError(result, 411);
  }

  @Test
  @DisplayName("OPRETFORDRING with art type MODR passes validation")
  void opretfordringWithModrPasses() {
    FordringValidationRequest request =
        validRequestBuilder("OPRETFORDRING")
            .mfOpretFordringStrukturPresent(true)
            .artType("MODR")
            .build();

    FordringValidationResult result = fireRules(request);

    assertNoError(result, 411);
  }

  @Test
  @DisplayName("OPRETFORDRING with invalid art type is rejected with error 411")
  void opretfordringWithInvalidArtTypeIsRejected() {
    FordringValidationRequest request =
        validRequestBuilder("OPRETFORDRING")
            .mfOpretFordringStrukturPresent(true)
            .artType("INVALID")
            .build();

    FordringValidationResult result = fireRules(request);

    assertHasError(result, FordringErrorCode.FORDRING_TYPE_ERROR);
    assertErrorMessageContains(result, 411, "Fordringsart må være inddrivelse");
  }

  @Test
  @DisplayName("GENINDSENDFORDRING with invalid art type is rejected with error 411")
  void genindsendfordringWithInvalidArtTypeIsRejected() {
    FordringValidationRequest request =
        validRequestBuilder("GENINDSENDFORDRING")
            .mfGenindsendFordringStrukturPresent(true)
            .artType("OTHER")
            .build();

    FordringValidationResult result = fireRules(request);

    assertHasError(result, FordringErrorCode.FORDRING_TYPE_ERROR);
  }
}
