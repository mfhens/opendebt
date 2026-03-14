package dk.ufst.opendebt.rules.fordring;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import dk.ufst.opendebt.common.dto.fordring.FordringValidationRequest;

class FordringValidationRequestTest {

  @Test
  void builderShouldCreateRequestWithAllFields() {
    LocalDate today = LocalDate.now();
    LocalDateTime now = LocalDateTime.now();

    FordringValidationRequest request =
        FordringValidationRequest.builder()
            .aktionKode("OPRETFORDRING")
            .mfOpretFordringStrukturPresent(true)
            .valutaKode("DKK")
            .artType("INDR")
            .merRenteSats(BigDecimal.valueOf(5.5))
            .virkningsDato(today)
            .modtagelsesTidspunkt(now)
            .periodeFra(today.minusMonths(6))
            .periodeTil(today)
            .virkningsDatoRequired(true)
            .fordringhaveraftaleId("FA001")
            .agreementFound(true)
            .dmiFordringTypeKode("HF01")
            .claimTypeAllowedByAgreement(true)
            .mfAftaleSystemIntegration(true)
            .systemToSystem(false)
            .debtorId("12345678")
            .build();

    assertThat(request.getAktionKode()).isEqualTo("OPRETFORDRING");
    assertThat(request.isMfOpretFordringStrukturPresent()).isTrue();
    assertThat(request.getValutaKode()).isEqualTo("DKK");
    assertThat(request.getArtType()).isEqualTo("INDR");
    assertThat(request.getMerRenteSats()).isEqualByComparingTo("5.5");
    assertThat(request.getVirkningsDato()).isEqualTo(today);
    assertThat(request.getModtagelsesTidspunkt()).isEqualTo(now);
    assertThat(request.isVirkningsDatoRequired()).isTrue();
    assertThat(request.isAgreementFound()).isTrue();
    assertThat(request.isSystemToSystem()).isFalse();
    assertThat(request.getDebtorId()).isEqualTo("12345678");
  }

  @Test
  void defaultValuesShouldBeFalseForBooleans() {
    FordringValidationRequest request =
        FordringValidationRequest.builder().aktionKode("OPRETFORDRING").build();

    assertThat(request.isMfOpretFordringStrukturPresent()).isFalse();
    assertThat(request.isMfGenindsendFordringStrukturPresent()).isFalse();
    assertThat(request.isAgreementFound()).isFalse();
    assertThat(request.isSystemToSystem()).isFalse();
    assertThat(request.isVirkningsDatoRequired()).isFalse();
  }

  @Test
  void noArgsConstructorShouldWork() {
    FordringValidationRequest request = new FordringValidationRequest();
    request.setAktionKode("NEDSKRIV");
    assertThat(request.getAktionKode()).isEqualTo("NEDSKRIV");
  }
}
