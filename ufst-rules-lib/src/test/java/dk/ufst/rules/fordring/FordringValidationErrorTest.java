package dk.ufst.rules.fordring;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dk.ufst.opendebt.common.dto.fordring.FordringErrorCode;
import dk.ufst.opendebt.common.dto.fordring.FordringValidationError;

class FordringValidationErrorTest {

  @Test
  void ofShouldCreateErrorFromCode() {
    FordringValidationError error =
        FordringValidationError.of(FordringErrorCode.OPRETFORDRING_STRUKTUR);

    assertThat(error.getErrorCode()).isEqualTo(444);
    assertThat(error.getErrorCodeEnum()).isEqualTo(FordringErrorCode.OPRETFORDRING_STRUKTUR);
    assertThat(error.getMessage()).isEqualTo("MFOpretFordringStruktur mangler");
    assertThat(error.getField()).isNull();
  }

  @Test
  void ofWithFieldShouldSetField() {
    FordringValidationError error =
        FordringValidationError.of(FordringErrorCode.VIRKNINGSDATO_MISSING, "virkningsDato");

    assertThat(error.getErrorCode()).isEqualTo(409);
    assertThat(error.getField()).isEqualTo("virkningsDato");
    assertThat(error.getMessage()).contains("Virkningsdato");
  }

  @Test
  void builderShouldCreateError() {
    FordringValidationError error =
        FordringValidationError.builder()
            .errorCode(999)
            .message("Custom error")
            .field("customField")
            .build();

    assertThat(error.getErrorCode()).isEqualTo(999);
    assertThat(error.getMessage()).isEqualTo("Custom error");
    assertThat(error.getField()).isEqualTo("customField");
  }
}
