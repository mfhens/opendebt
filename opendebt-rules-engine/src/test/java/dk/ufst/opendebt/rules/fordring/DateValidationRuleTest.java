package dk.ufst.opendebt.rules.fordring;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dk.ufst.opendebt.common.dto.fordring.FordringErrorCode;
import dk.ufst.opendebt.common.dto.fordring.FordringValidationRequest;
import dk.ufst.opendebt.common.dto.fordring.FordringValidationResult;
import dk.ufst.opendebt.rules.test.AbstractFordringRuleTest;

/** Tests for petition015 date validation rules (error codes 409, 464, 467, 548, 568, 569). */
class DateValidationRuleTest extends AbstractFordringRuleTest {

  @Test
  @DisplayName("Claim with missing required VirkningsDato is rejected with error 409")
  void missingRequiredVirkningsDatoIsRejected() {
    FordringValidationRequest request =
        validRequestBuilder("OPRETFORDRING")
            .mfOpretFordringStrukturPresent(true)
            .virkningsDatoRequired(true)
            .virkningsDato(null)
            .build();

    FordringValidationResult result = fireRules(request);

    assertHasError(result, FordringErrorCode.VIRKNINGSDATO_MISSING);
    assertErrorMessageContains(result, 409, "Virkningsdato skal være udfyldt");
  }

  @Test
  @DisplayName("Claim with VirkningsDato later than receipt is rejected with error 464")
  void virkningsDatoLaterThanReceiptIsRejected() {
    FordringValidationRequest request =
        validRequestBuilder("OPRETFORDRING")
            .mfOpretFordringStrukturPresent(true)
            .virkningsDatoRequired(false)
            .modtagelsesTidspunkt(LocalDateTime.of(2024, 3, 1, 0, 0))
            .virkningsDato(LocalDate.of(2024, 3, 15))
            .build();

    FordringValidationResult result = fireRules(request);

    assertHasError(result, FordringErrorCode.VIRKNINGSDATO_SENERE_END_MODTAGELSE);
    assertErrorMessageContains(
        result, 464, "Virkningsdato må ikke være senere end modtagelsestidspunktet");
  }

  @Test
  @DisplayName("Withdrawal VirkningsDato before main claim receipt is rejected with error 467")
  void withdrawalVirkningsDatoBeforeMainClaimReceiptIsRejected() {
    FordringValidationRequest request =
        validRequestBuilder("TILBAGEKALD")
            .mfTilbagekaldFordringStrukturPresent(true)
            .virkningsDatoRequired(false)
            .virkningsDato(LocalDate.of(2024, 1, 1))
            .modtagelsesTidspunkt(LocalDateTime.of(2025, 6, 1, 0, 0))
            .hovedfordringModtagelsesTidspunkt(LocalDateTime.of(2024, 3, 1, 0, 0))
            .build();

    FordringValidationResult result = fireRules(request);

    assertHasError(result, FordringErrorCode.VIRKNINGSDATO_FOER_HOVEDFORDRING_MODTAGELSE);
    assertErrorMessageContains(result, 467, "Virkningsdato for tilbagekald");
  }

  @Test
  @DisplayName("Claim with future VirkningsDato is rejected with error 548")
  void futureVirkningsDatoIsRejected() {
    FordringValidationRequest request =
        validRequestBuilder("OPRETFORDRING")
            .mfOpretFordringStrukturPresent(true)
            .virkningsDatoRequired(false)
            .virkningsDato(LocalDate.now().plusDays(1))
            .modtagelsesTidspunkt(LocalDateTime.now().plusDays(2))
            .build();

    FordringValidationResult result = fireRules(request);

    assertHasError(result, FordringErrorCode.NO_FUTURE_VIRKNINGDATO);
    assertErrorMessageContains(result, 548, "virkningsdato må ikke være fremtidig");
  }

  @Test
  @DisplayName("Claim with date before 1900 is rejected with error 568")
  void dateBefore1900IsRejected() {
    FordringValidationRequest request =
        validRequestBuilder("OPRETFORDRING")
            .mfOpretFordringStrukturPresent(true)
            .virkningsDatoRequired(false)
            .virkningsDato(LocalDate.of(1899, 12, 31))
            .modtagelsesTidspunkt(LocalDateTime.now())
            .build();

    FordringValidationResult result = fireRules(request);

    assertHasError(result, FordringErrorCode.TIDLIGST_MULIG_DATO);
    assertErrorMessageContains(result, 568, "datoer der ligger før år 1900");
  }

  @Test
  @DisplayName("Claim with PeriodeFra after PeriodeTil is rejected with error 569")
  void periodeFraAfterPeriodeTilIsRejected() {
    FordringValidationRequest request =
        validRequestBuilder("OPRETFORDRING")
            .mfOpretFordringStrukturPresent(true)
            .periodeFra(LocalDate.of(2024, 6, 1))
            .periodeTil(LocalDate.of(2024, 3, 1))
            .build();

    FordringValidationResult result = fireRules(request);

    assertHasError(result, FordringErrorCode.PERIODE_TIL_EFTER_PERIODE_FRA);
    assertErrorMessageContains(result, 569, "PeriodeFra må ikke være efter PeriodeTil");
  }

  @Test
  @DisplayName("Claim with valid date range passes date validation")
  void validDateRangePasses() {
    FordringValidationRequest request =
        validRequestBuilder("OPRETFORDRING")
            .mfOpretFordringStrukturPresent(true)
            .virkningsDatoRequired(true)
            .periodeFra(LocalDate.of(2024, 1, 1))
            .periodeTil(LocalDate.of(2024, 6, 30))
            .virkningsDato(LocalDate.of(2024, 3, 15))
            .modtagelsesTidspunkt(LocalDateTime.of(2024, 3, 15, 12, 0))
            .build();

    FordringValidationResult result = fireRules(request);

    assertNoError(result, 409);
    assertNoError(result, 464);
    assertNoError(result, 548);
    assertNoError(result, 568);
    assertNoError(result, 569);
  }
}
