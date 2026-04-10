package dk.ufst.rules.fordring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dk.ufst.opendebt.common.dto.fordring.FordringErrorCode;
import dk.ufst.opendebt.common.dto.fordring.FordringValidationRequest;
import dk.ufst.opendebt.common.dto.fordring.FordringValidationResult;
import dk.ufst.rules.test.AbstractFordringRuleTest;

/** Tests for petition015 agreement validation rules (error codes 2, 151, 156). */
class AgreementValidationRuleTest extends AbstractFordringRuleTest {

  @Test
  @DisplayName("Claim with non-existent agreement is rejected with error 2")
  void nonExistentAgreementIsRejected() {
    FordringValidationRequest request =
        validRequestBuilder("OPRETFORDRING")
            .mfOpretFordringStrukturPresent(true)
            .fordringhaveraftaleId("NONEXISTENT")
            .agreementFound(false)
            .build();

    FordringValidationResult result = fireRules(request);

    assertHasError(result, FordringErrorCode.NO_AGREEMENT_FOUND);
    assertErrorMessageContains(result, 2, "Fordringhaveraftale findes ikke");
  }

  @Test
  @DisplayName("Claim with disallowed claim type is rejected with error 151")
  void disallowedClaimTypeIsRejected() {
    FordringValidationRequest request =
        validRequestBuilder("OPRETFORDRING")
            .mfOpretFordringStrukturPresent(true)
            .agreementFound(true)
            .dmiFordringTypeKode("HF99")
            .claimTypeAllowedByAgreement(false)
            .build();

    FordringValidationResult result = fireRules(request);

    assertHasError(result, FordringErrorCode.TYPE_AGREEMENT_MISSING);
    assertErrorMessageContains(result, 151, "må ikke indberettes på denne DMIFordringTypeKode");
  }

  @Test
  @DisplayName("System-to-system submission without integration flag is rejected with error 156")
  void systemToSystemWithoutIntegrationFlagIsRejected() {
    FordringValidationRequest request =
        validRequestBuilder("OPRETFORDRING")
            .mfOpretFordringStrukturPresent(true)
            .systemToSystem(true)
            .mfAftaleSystemIntegration(false)
            .build();

    FordringValidationResult result = fireRules(request);

    assertHasError(result, FordringErrorCode.NO_SYSTEM_TO_SYSTEM_INTEGRATION);
    assertErrorMessageContains(
        result, 156, "MFAftaleSystemIntegration på fordringhaveraftale er falsk");
  }

  @Test
  @DisplayName("Valid agreement with allowed claim type passes validation")
  void validAgreementPasses() {
    FordringValidationRequest request =
        validRequestBuilder("OPRETFORDRING")
            .mfOpretFordringStrukturPresent(true)
            .agreementFound(true)
            .dmiFordringTypeKode("HF01")
            .claimTypeAllowedByAgreement(true)
            .mfAftaleSystemIntegration(true)
            .systemToSystem(false)
            .build();

    FordringValidationResult result = fireRules(request);

    assertNoError(result, 2);
    assertNoError(result, 151);
    assertNoError(result, 156);
  }
}
