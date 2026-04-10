package dk.ufst.rules.fordring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dk.ufst.opendebt.common.dto.fordring.FordringErrorCode;
import dk.ufst.opendebt.common.dto.fordring.FordringValidationRequest;
import dk.ufst.opendebt.common.dto.fordring.FordringValidationResult;
import dk.ufst.rules.test.AbstractFordringRuleTest;

/** Tests for petition015 structure validation rules (error codes 403-505). */
class StructureValidationRuleTest extends AbstractFordringRuleTest {

  @Test
  @DisplayName("OPRETFORDRING without MFOpretFordringStruktur is rejected with error 444")
  void opretfordringWithoutStrukturIsRejected() {
    FordringValidationRequest request =
        validRequestBuilder("OPRETFORDRING").mfOpretFordringStrukturPresent(false).build();

    FordringValidationResult result = fireRules(request);

    assertHasError(result, FordringErrorCode.OPRETFORDRING_STRUKTUR);
    assertErrorMessageContains(result, 444, "MFOpretFordringStruktur mangler");
  }

  @Test
  @DisplayName("OPRETFORDRING with MFOpretFordringStruktur passes structure validation")
  void opretfordringWithStrukturPasses() {
    FordringValidationRequest request =
        validRequestBuilder("OPRETFORDRING").mfOpretFordringStrukturPresent(true).build();

    FordringValidationResult result = fireRules(request);

    assertNoError(result, 444);
  }

  @Test
  @DisplayName("GENINDSENDFORDRING without MFGenindsendFordringStruktur is rejected with error 403")
  void genindsendfordringWithoutStrukturIsRejected() {
    FordringValidationRequest request =
        validRequestBuilder("GENINDSENDFORDRING")
            .mfGenindsendFordringStrukturPresent(false)
            .build();

    FordringValidationResult result = fireRules(request);

    assertHasError(result, FordringErrorCode.GENINDSEND_STRUCTURE_MISSING);
    assertErrorMessageContains(result, 403, "MFGenindsendFordringStruktur mangler");
  }

  @Test
  @DisplayName("NEDSKRIV without MFNedskrivFordringStruktur is rejected with error 447")
  void nedskrivWithoutStrukturIsRejected() {
    FordringValidationRequest request =
        validRequestBuilder("NEDSKRIV").mfNedskrivFordringStrukturPresent(false).build();

    FordringValidationResult result = fireRules(request);

    assertHasError(result, FordringErrorCode.NEDSKRIV_STRUKTUR_MISSING);
    assertErrorMessageContains(result, 447, "MFNedskrivFordringStruktur mangler");
  }

  @Test
  @DisplayName("TILBAGEKALD without MFTilbagekaldFordringStruktur is rejected with error 448")
  void tilbagekaldWithoutStrukturIsRejected() {
    FordringValidationRequest request =
        validRequestBuilder("TILBAGEKALD").mfTilbagekaldFordringStrukturPresent(false).build();

    FordringValidationResult result = fireRules(request);

    assertHasError(result, FordringErrorCode.TILBAGEKALD_STRUKTUR_MISSING);
    assertErrorMessageContains(result, 448, "MFTilbagekaldFordringStruktur mangler");
  }

  @Test
  @DisplayName("AENDRFORDRING without MFAendrFordringStruktur is rejected with error 458")
  void aendrfordringWithoutStrukturIsRejected() {
    FordringValidationRequest request =
        validRequestBuilder("AENDRFORDRING").mfAendrFordringStrukturPresent(false).build();

    FordringValidationResult result = fireRules(request);

    assertHasError(result, FordringErrorCode.AENDRFORDRING_STRUKTUR_MISSING);
    assertErrorMessageContains(result, 458, "MFAendrFordringStruktur mangler");
  }

  @Test
  @DisplayName("OPSKRIVNINGREGULERING without struktur is rejected with error 404")
  void opskrivningReguleringWithoutStrukturIsRejected() {
    FordringValidationRequest request =
        validRequestBuilder("OPSKRIVNINGREGULERING")
            .mfOpskrivningReguleringStrukturPresent(false)
            .build();

    FordringValidationResult result = fireRules(request);

    assertHasError(result, FordringErrorCode.OPSKRIV_REGULERING_STRUKTUR);
  }

  @Test
  @DisplayName(
      "OPSKRIVNINGANNULLERETNEDSKRIVNINGINDBETALING without struktur is rejected with error 406")
  void opskrivningAnnulleretNedskrivningIndbetalingWithoutStrukturIsRejected() {
    FordringValidationRequest request =
        validRequestBuilder("OPSKRIVNINGANNULLERETNEDSKRIVNINGINDBETALING")
            .mfOpskrivningAnnulleretNedskrivningIndbetalingStrukturPresent(false)
            .build();

    FordringValidationResult result = fireRules(request);

    assertHasError(result, FordringErrorCode.OPSKRIV_ANNULLERET_NEDSKRIV_INDBETALING_STRUKTUR);
  }

  @Test
  @DisplayName(
      "OPSKRIVNINGOMGJORTNEDSKRIVNINGREGULERING without struktur is rejected with error 407")
  void opskrivningOmgjortNedskrivningReguleringWithoutStrukturIsRejected() {
    FordringValidationRequest request =
        validRequestBuilder("OPSKRIVNINGOMGJORTNEDSKRIVNINGREGULERING")
            .mfOpskrivningOmgjortNedskrivningReguleringStrukturPresent(false)
            .build();

    FordringValidationResult result = fireRules(request);

    assertHasError(result, FordringErrorCode.OPSKRIV_OMGJORT_NEDSKRIV_REGULERING_STRUKTUR);
  }

  @Test
  @DisplayName(
      "NEDSKRIVNINGANNULLERETOPSKRIVNINGREGULERING without struktur is rejected with error 412")
  void nedskrivningAnnulleretOpskrivningReguleringWithoutStrukturIsRejected() {
    FordringValidationRequest request =
        validRequestBuilder("NEDSKRIVNINGANNULLERETOPSKRIVNINGREGULERING")
            .mfNedskrivningAnnulleretOpskrivningReguleringStrukturPresent(false)
            .build();

    FordringValidationResult result = fireRules(request);

    assertHasError(result, FordringErrorCode.NEDSKRIV_ANNULLERET_OPSKRIV_REGULERING_STRUKTUR);
  }

  @Test
  @DisplayName(
      "NEDSKRIVNINGANNULLERETOPSKRIVNINGINDBETALING without struktur is rejected with error 505")
  void nedskrivningAnnulleretOpskrivningIndbetalingWithoutStrukturIsRejected() {
    FordringValidationRequest request =
        validRequestBuilder("NEDSKRIVNINGANNULLERETOPSKRIVNINGINDBETALING")
            .mfNedskrivningAnnulleretOpskrivningIndbetalingStrukturPresent(false)
            .build();

    FordringValidationResult result = fireRules(request);

    assertHasError(result, FordringErrorCode.NEDSKRIV_ANNULLERET_OPSKRIV_INDBETALING_STRUKTUR);
  }
}
