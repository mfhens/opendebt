package dk.ufst.rules.fordring;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dk.ufst.opendebt.common.dto.fordring.FordringErrorCode;
import dk.ufst.opendebt.common.dto.fordring.FordringValidationError;
import dk.ufst.opendebt.common.dto.fordring.FordringValidationResult;

class FordringValidationResultTest {

  @Test
  void newResultShouldBeValid() {
    FordringValidationResult result = FordringValidationResult.builder().build();
    assertThat(result.isValid()).isTrue();
    assertThat(result.getErrors()).isEmpty();
  }

  @Test
  void addErrorShouldMakeResultInvalid() {
    FordringValidationResult result = FordringValidationResult.builder().build();
    result.addError(FordringErrorCode.INVALID_CURRENCY);
    assertThat(result.isValid()).isFalse();
    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors().get(0).getErrorCode()).isEqualTo(152);
  }

  @Test
  void addErrorWithFieldShouldSetField() {
    FordringValidationResult result = FordringValidationResult.builder().build();
    result.addError(FordringErrorCode.INVALID_CURRENCY, "valutaKode");
    assertThat(result.getErrors().get(0).getField()).isEqualTo("valutaKode");
  }

  @Test
  void addMultipleErrors() {
    FordringValidationResult result = FordringValidationResult.builder().build();
    result.addError(FordringErrorCode.INVALID_CURRENCY);
    result.addError(FordringErrorCode.NEGATIVE_INTEREST_RATE);
    result.addError(FordringErrorCode.NO_AGREEMENT_FOUND);

    assertThat(result.isValid()).isFalse();
    assertThat(result.getErrors()).hasSize(3);
    assertThat(result.getErrors())
        .extracting(FordringValidationError::getErrorCode)
        .containsExactly(152, 438, 2);
  }

  @Test
  void addErrorObjectDirectly() {
    FordringValidationResult result = FordringValidationResult.builder().build();
    FordringValidationError error = FordringValidationError.of(FordringErrorCode.DEBTOR_NOT_FOUND);
    result.addError(error);

    assertThat(result.isValid()).isFalse();
    assertThat(result.getErrors().get(0).getErrorCodeEnum())
        .isEqualTo(FordringErrorCode.DEBTOR_NOT_FOUND);
    assertThat(result.getErrors().get(0).getMessage()).contains("Skyldner");
  }
}
