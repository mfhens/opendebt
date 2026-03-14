package dk.ufst.opendebt.rules.fordring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dk.ufst.opendebt.common.dto.fordring.FordringErrorCode;
import dk.ufst.opendebt.common.dto.fordring.FordringValidationRequest;
import dk.ufst.opendebt.common.dto.fordring.FordringValidationResult;
import dk.ufst.opendebt.rules.test.AbstractFordringRuleTest;

/** Tests for petition015 debtor validation rules (error code 5). */
class DebtorValidationRuleTest extends AbstractFordringRuleTest {

  @Test
  @DisplayName("Claim with valid debtor ID passes validation")
  void validDebtorIdPasses() {
    FordringValidationRequest request =
        validRequestBuilder("OPRETFORDRING")
            .mfOpretFordringStrukturPresent(true)
            .debtorId("12345678")
            .build();

    FordringValidationResult result = fireRules(request);

    assertNoError(result, 5);
  }

  @Test
  @DisplayName("Claim with zero debtor ID is rejected with error 5")
  void zeroDebtorIdIsRejected() {
    FordringValidationRequest request =
        validRequestBuilder("OPRETFORDRING")
            .mfOpretFordringStrukturPresent(true)
            .debtorId("0")
            .build();

    FordringValidationResult result = fireRules(request);

    assertHasError(result, FordringErrorCode.DEBTOR_NOT_FOUND);
    assertErrorMessageContains(result, 5, "Skyldner der er angivet findes ikke");
  }

  @Test
  @DisplayName("Claim with all-zeros UUID debtor ID is rejected with error 5")
  void allZerosUuidDebtorIdIsRejected() {
    FordringValidationRequest request =
        validRequestBuilder("OPRETFORDRING")
            .mfOpretFordringStrukturPresent(true)
            .debtorId("00000000-0000-0000-0000-000000000000")
            .build();

    FordringValidationResult result = fireRules(request);

    assertHasError(result, FordringErrorCode.DEBTOR_NOT_FOUND);
  }
}
