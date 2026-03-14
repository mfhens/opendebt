package dk.ufst.opendebt.rules.fordring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import dk.ufst.opendebt.common.dto.fordring.FordringErrorCode;

class FordringErrorCodeTest {

  @ParameterizedTest
  @EnumSource(FordringErrorCode.class)
  void allErrorCodesShouldHavePositiveCode(FordringErrorCode errorCode) {
    assertThat(errorCode.getCode()).as("Error code should be positive").isPositive();
  }

  @ParameterizedTest
  @EnumSource(FordringErrorCode.class)
  void allErrorCodesShouldHaveDanishDescription(FordringErrorCode errorCode) {
    assertThat(errorCode.getDanishDescription())
        .as("Danish description should not be blank")
        .isNotBlank();
  }

  @ParameterizedTest
  @EnumSource(FordringErrorCode.class)
  void fromCodeShouldResolveAllEnumValues(FordringErrorCode errorCode) {
    assertThat(FordringErrorCode.fromCode(errorCode.getCode()))
        .as("fromCode should resolve to the same enum constant")
        .isEqualTo(errorCode);
  }

  @Test
  void fromCodeShouldThrowForUnknownCode() {
    assertThatThrownBy(() -> FordringErrorCode.fromCode(99999))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("99999");
  }

  @Test
  void allCodesAreUnique() {
    int[] codes =
        java.util.Arrays.stream(FordringErrorCode.values())
            .mapToInt(FordringErrorCode::getCode)
            .toArray();
    assertThat(codes).as("Error codes should be unique").doesNotHaveDuplicates();
  }

  @Test
  void coreStructureCodesArePresent() {
    assertThat(FordringErrorCode.fromCode(403))
        .isEqualTo(FordringErrorCode.GENINDSEND_STRUCTURE_MISSING);
    assertThat(FordringErrorCode.fromCode(404))
        .isEqualTo(FordringErrorCode.OPSKRIV_REGULERING_STRUKTUR);
    assertThat(FordringErrorCode.fromCode(444)).isEqualTo(FordringErrorCode.OPRETFORDRING_STRUKTUR);
    assertThat(FordringErrorCode.fromCode(447))
        .isEqualTo(FordringErrorCode.NEDSKRIV_STRUKTUR_MISSING);
    assertThat(FordringErrorCode.fromCode(448))
        .isEqualTo(FordringErrorCode.TILBAGEKALD_STRUKTUR_MISSING);
    assertThat(FordringErrorCode.fromCode(458))
        .isEqualTo(FordringErrorCode.AENDRFORDRING_STRUKTUR_MISSING);
  }

  @Test
  void currencyCodeIsPresent() {
    assertThat(FordringErrorCode.fromCode(152)).isEqualTo(FordringErrorCode.INVALID_CURRENCY);
  }

  @Test
  void dateCodesArePresent() {
    assertThat(FordringErrorCode.fromCode(409)).isEqualTo(FordringErrorCode.VIRKNINGSDATO_MISSING);
    assertThat(FordringErrorCode.fromCode(464))
        .isEqualTo(FordringErrorCode.VIRKNINGSDATO_SENERE_END_MODTAGELSE);
    assertThat(FordringErrorCode.fromCode(548)).isEqualTo(FordringErrorCode.NO_FUTURE_VIRKNINGDATO);
    assertThat(FordringErrorCode.fromCode(568)).isEqualTo(FordringErrorCode.TIDLIGST_MULIG_DATO);
    assertThat(FordringErrorCode.fromCode(569))
        .isEqualTo(FordringErrorCode.PERIODE_TIL_EFTER_PERIODE_FRA);
  }

  @Test
  void agreementCodesArePresent() {
    assertThat(FordringErrorCode.fromCode(2)).isEqualTo(FordringErrorCode.NO_AGREEMENT_FOUND);
    assertThat(FordringErrorCode.fromCode(151)).isEqualTo(FordringErrorCode.TYPE_AGREEMENT_MISSING);
    assertThat(FordringErrorCode.fromCode(156))
        .isEqualTo(FordringErrorCode.NO_SYSTEM_TO_SYSTEM_INTEGRATION);
  }
}
