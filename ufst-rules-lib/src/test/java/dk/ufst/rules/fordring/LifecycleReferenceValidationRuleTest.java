package dk.ufst.rules.fordring;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import dk.ufst.opendebt.common.dto.fordring.FordringErrorCode;
import dk.ufst.opendebt.common.dto.fordring.FordringValidationRequest;
import dk.ufst.opendebt.common.dto.fordring.FordringValidationResult;
import dk.ufst.rules.test.AbstractFordringRuleTest;

/**
 * Tests for petition017 lifecycle and reference validation rules.
 *
 * <p>Covers:
 *
 * <ul>
 *   <li>Genindsend (resubmit) rules - errors 539, 540, 541, 542, 544
 *   <li>Tilbagekald (withdrawal) rules - errors 434, 538, 546, 547, 570
 *   <li>Action reference rules - errors 418, 429, 526, 527, 530
 *   <li>Opskrivning/Nedskrivning reference rules - errors 469, 470, 471, 473, 474, 477, 493, 494,
 *       502, 503, 504, 506
 *   <li>State validation rules - errors 428, 488, 496, 498
 * </ul>
 */
class LifecycleReferenceValidationRuleTest extends AbstractFordringRuleTest {

  // =========================================================================
  // Genindsend (Resubmit) Rules
  // =========================================================================

  @Nested
  @DisplayName("Genindsend (Resubmit) Rules")
  class GenindsendValidation {

    @ParameterizedTest
    @ValueSource(strings = {"HENS", "KLAG", "BORD", "HAFT"})
    @DisplayName("Resubmission with valid withdrawal reason passes (HENS, KLAG, BORD, HAFT)")
    void resubmissionWithValidWithdrawalReasonPasses(String reason) {
      FordringValidationRequest request =
          validRequestBuilder("GENINDSENDFORDRING")
              .mfGenindsendFordringStrukturPresent(true)
              .originalClaimWithdrawn(true)
              .originalWithdrawalReason(reason)
              .fordringshaverKode("FH001")
              .originalFordringshaverKode("FH001")
              .stamdataMatchesOriginal(true)
              .originalClaimIsModr(false)
              .hasGenindsendPermission(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertNoError(result, FordringErrorCode.GENINDSEND_INVALID_WITHDRAWAL_REASON.getCode());
      assertNoError(result, FordringErrorCode.GENINDSEND_NOT_WITHDRAWN.getCode());
    }

    @Test
    @DisplayName("Resubmission with invalid withdrawal reason (FEJL) is rejected with error 539")
    void resubmissionWithInvalidWithdrawalReasonIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("GENINDSENDFORDRING")
              .mfGenindsendFordringStrukturPresent(true)
              .originalClaimWithdrawn(true)
              .originalWithdrawalReason("FEJL")
              .fordringshaverKode("FH001")
              .originalFordringshaverKode("FH001")
              .stamdataMatchesOriginal(true)
              .originalClaimIsModr(false)
              .hasGenindsendPermission(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.GENINDSEND_INVALID_WITHDRAWAL_REASON);
      assertErrorMessageContains(result, 539, "tilbagekaldt med HENS KLAG BORD eller HAFT");
    }

    @Test
    @DisplayName("Resubmission of non-withdrawn claim is rejected with error 540")
    void resubmissionOfNonWithdrawnClaimIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("GENINDSENDFORDRING")
              .mfGenindsendFordringStrukturPresent(true)
              .originalClaimWithdrawn(false)
              .fordringshaverKode("FH001")
              .originalFordringshaverKode("FH001")
              .stamdataMatchesOriginal(true)
              .originalClaimIsModr(false)
              .hasGenindsendPermission(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.GENINDSEND_NOT_WITHDRAWN);
      assertErrorMessageContains(result, 540, "fordringen er tilbagekaldt");
    }

    @Test
    @DisplayName("Resubmission from different fordringshaver is rejected with error 541")
    void resubmissionFromDifferentFordringshaverIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("GENINDSENDFORDRING")
              .mfGenindsendFordringStrukturPresent(true)
              .originalClaimWithdrawn(true)
              .originalWithdrawalReason("HENS")
              .fordringshaverKode("FH002")
              .originalFordringshaverKode("FH001")
              .stamdataMatchesOriginal(true)
              .originalClaimIsModr(false)
              .hasGenindsendPermission(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.GENINDSEND_DIFFERENT_FORDRINGSHAVER);
      assertErrorMessageContains(result, 541, "samme fordringshaver");
    }

    @Test
    @DisplayName("Resubmission with different stamdata is rejected with error 542")
    void resubmissionWithDifferentStamdataIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("GENINDSENDFORDRING")
              .mfGenindsendFordringStrukturPresent(true)
              .originalClaimWithdrawn(true)
              .originalWithdrawalReason("HENS")
              .fordringshaverKode("FH001")
              .originalFordringshaverKode("FH001")
              .stamdataMatchesOriginal(false)
              .originalClaimIsModr(false)
              .hasGenindsendPermission(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.GENINDSEND_STAMDATA_MISMATCH);
      assertErrorMessageContains(
          result, 542, "Stamdata på aktionen der genindsendes er forskellig");
    }

    @Test
    @DisplayName("Resubmission of MODR claim is rejected with error 544")
    void resubmissionOfModrClaimIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("GENINDSENDFORDRING")
              .mfGenindsendFordringStrukturPresent(true)
              .originalClaimWithdrawn(true)
              .originalWithdrawalReason("HENS")
              .fordringshaverKode("FH001")
              .originalFordringshaverKode("FH001")
              .stamdataMatchesOriginal(true)
              .originalClaimIsModr(true)
              .hasGenindsendPermission(true)
              .artType("MODR")
              .hasModrPermission(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.GENINDSEND_MODR_NOT_ALLOWED);
      assertErrorMessageContains(result, 544, "kan ikke genindsendes modregningsfordringer");
    }
  }

  // =========================================================================
  // Tilbagekald (Withdrawal) Rules
  // =========================================================================

  @Nested
  @DisplayName("Tilbagekald (Withdrawal) Rules")
  class TilbagekaldValidation {

    @Test
    @DisplayName("Action on claim withdrawn during conversion is rejected with error 434")
    void actionOnClaimWithdrawnDuringConversionIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("AENDRFORDRING")
              .mfAendrFordringStrukturPresent(true)
              .withdrawnDuringConversion(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.WITHDRAWN_DURING_CONVERSION);
      assertErrorMessageContains(result, 434, "tilbagekaldt i konverteringsprocessen");
    }

    @Test
    @DisplayName("TILBAGEKALD with BORT reason for DMI-routed claim is rejected with error 538")
    void tilbagekaldBortForDmiRoutedClaimIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("TILBAGEKALD")
              .mfTilbagekaldFordringStrukturPresent(true)
              .routedToDmi(true)
              .tilbagekaldAarsagKode("BORT")
              .hasTilbagekaldPermission(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.BORT_NOT_ALLOWED_FOR_DMI);
      assertErrorMessageContains(result, 538, "BORT tilbagekaldårsag");
    }

    @Test
    @DisplayName("TILBAGEKALD with FEJL reason and VirkningsDato is rejected with error 546")
    void tilbagekaldFejlWithVirkningsDatoIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("TILBAGEKALD")
              .mfTilbagekaldFordringStrukturPresent(true)
              .tilbagekaldAarsagKode("FEJL")
              .virkningsDato(LocalDate.of(2024, 3, 1))
              .hasTilbagekaldPermission(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.TILBAGEKALD_FEJL_WITH_VIRKNINGSDATO);
      assertErrorMessageContains(result, 546, "Virkningsdato må ikke være udfyldt");
    }

    @Test
    @DisplayName("TILBAGEKALD with FEJL reason without VirkningsDato passes")
    void tilbagekaldFejlWithoutVirkningsDatoPasses() {
      FordringValidationRequest request =
          validRequestBuilder("TILBAGEKALD")
              .mfTilbagekaldFordringStrukturPresent(true)
              .tilbagekaldAarsagKode("FEJL")
              .virkningsDato(null)
              .virkningsDatoRequired(false)
              .hasTilbagekaldPermission(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertNoError(result, FordringErrorCode.TILBAGEKALD_FEJL_WITH_VIRKNINGSDATO.getCode());
    }

    @Test
    @DisplayName("Eftersendt claim referencing withdrawn hovedfordring is rejected with error 547")
    void eftersendtClaimReferencingWithdrawnHovedfordringIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .hovedfordringWithdrawn(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.EFTERSENDT_REFERENCES_WITHDRAWN);
      assertErrorMessageContains(result, 547, "relation til en tilbagekaldt fordring");
    }

    @Test
    @DisplayName(
        "TILBAGEKALD of hovedfordring with un-withdrawn divided claims is rejected with error 570")
    void tilbagekaldHovedfordringWithUnwithdrawDividedClaimsIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("TILBAGEKALD")
              .mfTilbagekaldFordringStrukturPresent(true)
              .hasUnwithdrawDividedClaims(true)
              .hasTilbagekaldPermission(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.TILBAGEKALD_HOVEDFORDRING_WITH_UNDIVIDED);
      assertErrorMessageContains(result, 570, "opsplittede relaterede fordringer er tilbagekaldt");
    }
  }

  // =========================================================================
  // Action Reference Rules
  // =========================================================================

  @Nested
  @DisplayName("Action Reference Rules")
  class ActionReferenceValidation {

    @Test
    @DisplayName("Action while previous action pending is rejected with error 418")
    void actionWhilePreviousActionPendingIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .hasPendingAction(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.PENDING_ACTION_EXISTS);
      assertErrorMessageContains(result, 418, "allerede indberettet en aktion der ikke er UDFØRT");
    }

    @Test
    @DisplayName("Reference to unknown AktionID is rejected with error 429")
    void referenceToUnknownAktionIdIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("NEDSKRIV")
              .mfNedskrivFordringStrukturPresent(true)
              .mfAktionIdRef("A999")
              .referencedAktionExists(false)
              .mfAktionIdRefExists(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.UNKNOWN_AKTION_ID);
      assertErrorMessageContains(result, 429, "aktionID som er ukendt for Fordring");
    }

    @Test
    @DisplayName("Invalid MFAktionIDRef is rejected with error 526")
    void invalidMfAktionIdRefIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("NEDSKRIV")
              .mfNedskrivFordringStrukturPresent(true)
              .mfAktionIdRef("REF999")
              .mfAktionIdRefExists(false)
              .referencedAktionExists(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.MF_AKTION_ID_REF_NOT_FOUND);
      assertErrorMessageContains(result, 526, "AktionIDRef findes ikke");
    }

    @Test
    @DisplayName("OANI without MFAktionIDRef when FordringID known is rejected with error 527")
    void oaniWithoutMfAktionIdRefWhenFordringIdKnownIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("OPSKRIVNINGANNULLERETNEDSKRIVNINGINDBETALING")
              .mfOpskrivningAnnulleretNedskrivningIndbetalingStrukturPresent(true)
              .mfAktionIdRefRequired(true)
              .mfAktionIdRef(null)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.OANI_MISSING_AKTION_ID_REF);
      assertErrorMessageContains(result, 527, "MFAktionIDRef er ikke udfyldt");
    }

    @Test
    @DisplayName("Reference to withdrawn claim/action is rejected with error 530")
    void referenceToWithdrawnClaimIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("NEDSKRIV")
              .mfNedskrivFordringStrukturPresent(true)
              .mfAktionIdRef("A001")
              .mfAktionIdRefExists(true)
              .referencedAktionExists(true)
              .referencedClaimWithdrawn(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.REFERENCED_CLAIM_WITHDRAWN);
      assertErrorMessageContains(result, 530, "tilbagekaldt");
    }
  }

  // =========================================================================
  // Opskrivning/Nedskrivning Reference Rules
  // =========================================================================

  @Nested
  @DisplayName("Opskrivning/Nedskrivning Reference Rules")
  class OpskrivningNedskrivningValidation {

    @Test
    @DisplayName("OANI with mismatched beløb is rejected with error 469")
    void oaniWithMismatchedBeloebIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("OPSKRIVNINGANNULLERETNEDSKRIVNINGINDBETALING")
              .mfOpskrivningAnnulleretNedskrivningIndbetalingStrukturPresent(true)
              .opskrivningBeloeb(new BigDecimal("4000"))
              .referencedNedskrivningBeloeb(new BigDecimal("5000"))
              .mfAktionIdRef("N001")
              .mfAktionIdRefExists(true)
              .referencedAktionExists(true)
              .referencedActionType("NEDSKRIV")
              .referencedNedskrivningAarsagKode("INDB")
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.BELOEB_MISMATCH);
      assertErrorMessageContains(result, 469, "FordringOpskrivningBeløb må ikke være forskellig");
    }

    @Test
    @DisplayName("Action with mismatched VirkningsDato is rejected with error 470")
    void actionWithMismatchedVirkningsDatoIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("OPSKRIVNINGANNULLERETNEDSKRIVNINGINDBETALING")
              .mfOpskrivningAnnulleretNedskrivningIndbetalingStrukturPresent(true)
              .mfAktionIdRef("N001")
              .mfAktionIdRefExists(true)
              .referencedAktionExists(true)
              .virkningsDato(LocalDate.of(2024, 2, 1))
              .referencedVirkningFra(LocalDate.of(2024, 1, 15))
              .referencedActionType("NEDSKRIV")
              .referencedNedskrivningAarsagKode("INDB")
              .opskrivningBeloeb(new BigDecimal("5000"))
              .referencedNedskrivningBeloeb(new BigDecimal("5000"))
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.VIRKNINGSDATO_MISMATCH);
      assertErrorMessageContains(result, 470, "VirkningFra må ikke være forskellig");
    }

    @Test
    @DisplayName("OANI referencing nedskrivning without INDB reason is rejected with error 471")
    void oaniReferencingNedskrivningWithoutIndbReasonIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("OPSKRIVNINGANNULLERETNEDSKRIVNINGINDBETALING")
              .mfOpskrivningAnnulleretNedskrivningIndbetalingStrukturPresent(true)
              .mfAktionIdRef("N001")
              .mfAktionIdRefExists(true)
              .referencedAktionExists(true)
              .referencedNedskrivningAarsagKode("REGU")
              .referencedActionType("NEDSKRIV")
              .opskrivningBeloeb(new BigDecimal("5000"))
              .referencedNedskrivningBeloeb(new BigDecimal("5000"))
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.OANI_WRONG_AARSAGKODE);
      assertErrorMessageContains(result, 471, "FordringNedskrivningÅrsagKode være INDB");
    }

    @Test
    @DisplayName("OANI/OONR not referencing nedskriv action is rejected with error 473")
    void oaniNotReferencingNedskrivActionIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("OPSKRIVNINGANNULLERETNEDSKRIVNINGINDBETALING")
              .mfOpskrivningAnnulleretNedskrivningIndbetalingStrukturPresent(true)
              .mfAktionIdRef("A001")
              .mfAktionIdRefExists(true)
              .referencedAktionExists(true)
              .referencedActionType("OPRETFORDRING")
              .opskrivningBeloeb(new BigDecimal("5000"))
              .referencedNedskrivningBeloeb(new BigDecimal("5000"))
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.NOT_REFERENCING_NEDSKRIV);
      assertErrorMessageContains(result, 473, "MFAktionIDRef skal pege på nedskriv aktion");
    }

    @Test
    @DisplayName("Opskrivning on interest claim (rente) is rejected with error 474")
    void opskrivningOnInterestClaimIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("OPSKRIVNINGREGULERING")
              .mfOpskrivningReguleringStrukturPresent(true)
              .claimIsRente(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.OPSKRIVNING_ON_RENTE);
      assertErrorMessageContains(result, 474, "opskrivningsfordringer på renter");
    }

    @Test
    @DisplayName("OONR referencing nedskrivning without REGU reason is rejected with error 477")
    void oonrReferencingNedskrivningWithoutReguReasonIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("OPSKRIVNINGOMGJORTNEDSKRIVNINGREGULERING")
              .mfOpskrivningOmgjortNedskrivningReguleringStrukturPresent(true)
              .mfAktionIdRef("N001")
              .mfAktionIdRefExists(true)
              .referencedAktionExists(true)
              .referencedNedskrivningAarsagKode("INDB")
              .referencedActionType("NEDSKRIV")
              .opskrivningBeloeb(new BigDecimal("5000"))
              .referencedNedskrivningBeloeb(new BigDecimal("5000"))
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.OONR_WRONG_AARSAGKODE);
      assertErrorMessageContains(result, 477, "FordringNedskrivningÅrsagKode være REGU");
    }

    @Test
    @DisplayName("Reference to rejected opskrivning/nedskrivning is rejected with error 493")
    void referenceToRejectedActionIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("NEDSKRIVNINGANNULLERETOPSKRIVNINGREGULERING")
              .mfNedskrivningAnnulleretOpskrivningReguleringStrukturPresent(true)
              .mfAktionIdRef("N001")
              .mfAktionIdRefExists(true)
              .referencedAktionExists(true)
              .referencedActionRejected(true)
              .referencedActionType("OPSKRIVNINGREGULERING")
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.REFERENCED_ACTION_REJECTED);
      assertErrorMessageContains(result, 493, "aktion er afvist");
    }

    @Test
    @DisplayName("Action with mismatched FordringID is rejected with error 494")
    void actionWithMismatchedFordringIdIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("NEDSKRIV")
              .mfNedskrivFordringStrukturPresent(true)
              .mfAktionIdRef("A001")
              .mfAktionIdRefExists(true)
              .referencedAktionExists(true)
              .fordringhaveraftaleId("F002")
              .referencedFordringId("F001")
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.FORDRING_ID_MISMATCH);
      assertErrorMessageContains(result, 494, "FordringsID på aktionen matcher ikke");
    }

    @Test
    @DisplayName("NAOR not referencing OR or OONR type is rejected with error 502")
    void naorNotReferencingOrOrOonrTypeIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("NEDSKRIVNINGANNULLERETOPSKRIVNINGREGULERING")
              .mfNedskrivningAnnulleretOpskrivningReguleringStrukturPresent(true)
              .mfAktionIdRef("A001")
              .mfAktionIdRefExists(true)
              .referencedAktionExists(true)
              .referencedActionType("OPSKRIVNINGANNULLERETNEDSKRIVNINGINDBETALING")
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.NAOR_WRONG_REFERENCE_TYPE);
      assertErrorMessageContains(result, 502, "skal være OpskrivningRegulering eller OONR");
    }

    @Test
    @DisplayName("Annullering when prior annullering exists is rejected with error 503")
    void annulleringWhenPriorAnnulleringExistsIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("NEDSKRIVNINGANNULLERETOPSKRIVNINGREGULERING")
              .mfNedskrivningAnnulleretOpskrivningReguleringStrukturPresent(true)
              .mfAktionIdRef("N001")
              .mfAktionIdRefExists(true)
              .referencedAktionExists(true)
              .existingAnnulleringForAction(true)
              .referencedActionType("OPSKRIVNINGREGULERING")
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.ANNULLERING_ALREADY_EXISTS);
      assertErrorMessageContains(result, 503, "allerede modtaget en annullering");
    }

    @Test
    @DisplayName("NAOR with mismatched beløb is rejected with error 504")
    void naorWithMismatchedBeloebIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("NEDSKRIVNINGANNULLERETOPSKRIVNINGREGULERING")
              .mfNedskrivningAnnulleretOpskrivningReguleringStrukturPresent(true)
              .mfAktionIdRef("O001")
              .mfAktionIdRefExists(true)
              .referencedAktionExists(true)
              .nedskrivningBeloeb(new BigDecimal("4000"))
              .referencedOpskrivningBeloeb(new BigDecimal("5000"))
              .referencedActionType("OPSKRIVNINGREGULERING")
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.NAOR_BELOEB_MISMATCH);
      assertErrorMessageContains(result, 504, "FordringNedskrivningBeløb skal være lig med");
    }

    @Test
    @DisplayName("NAOI not referencing OANI type is rejected with error 506")
    void naoiNotReferencingOaniTypeIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("NEDSKRIVNINGANNULLERETOPSKRIVNINGINDBETALING")
              .mfNedskrivningAnnulleretOpskrivningIndbetalingStrukturPresent(true)
              .mfAktionIdRef("A001")
              .mfAktionIdRefExists(true)
              .referencedAktionExists(true)
              .referencedActionType("OPSKRIVNINGOMGJORTNEDSKRIVNINGREGULERING")
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.NAOI_WRONG_REFERENCE_TYPE);
      assertErrorMessageContains(result, 506, "skal være af typen OANI");
    }
  }

  // =========================================================================
  // State Validation Rules
  // =========================================================================

  @Nested
  @DisplayName("State Validation Rules")
  class StateValidation {

    @Test
    @DisplayName("Reference to rejected FordringID is rejected with error 428")
    void referenceToRejectedFordringIdIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("NEDSKRIV")
              .mfNedskrivFordringStrukturPresent(true)
              .referencedFordringRejected(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.REFERENCED_FORDRING_REJECTED);
      assertErrorMessageContains(result, 428, "fordringID eller hovedfordringID er afvist");
    }

    @Test
    @DisplayName("Annullering of DMI action not yet UDFØRT is rejected with error 488")
    void annulleringOfDmiActionNotYetUdfoertIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("NEDSKRIVNINGANNULLERETOPSKRIVNINGREGULERING")
              .mfNedskrivningAnnulleretOpskrivningReguleringStrukturPresent(true)
              .mfAktionIdRef("A001")
              .mfAktionIdRefExists(true)
              .referencedAktionExists(true)
              .referencedActionInDmiNotUdfoert(true)
              .referencedActionType("OPSKRIVNINGREGULERING")
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.DMI_ACTION_NOT_UDFOERT);
      assertErrorMessageContains(result, 488, "endnu ikke udført i DMI");
    }

    @Test
    @DisplayName("Opskrivning on withdrawn original claim is rejected with error 496")
    void opskrivningOnWithdrawnOriginalClaimIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("OPSKRIVNINGREGULERING")
              .mfOpskrivningReguleringStrukturPresent(true)
              .originalClaimForOpskrivningWithdrawn(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.OPSKRIVNING_ON_WITHDRAWN);
      assertErrorMessageContains(
          result, 496, "oprindelig fordring som er tilbagesendt tilbagekaldt eller returneret");
    }

    @Test
    @DisplayName("Nedskrivning on withdrawn FordringID is rejected with error 498")
    void nedskrivningOnWithdrawnFordringIdIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("NEDSKRIV")
              .mfNedskrivFordringStrukturPresent(true)
              .fordringIdForNedskrivningWithdrawn(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.NEDSKRIVNING_ON_WITHDRAWN);
      assertErrorMessageContains(
          result, 498, "fordringsID som er tilbagesendt tilbagekaldt eller returneret");
    }
  }

  // =========================================================================
  // Combined Lifecycle Validation
  // =========================================================================

  @Nested
  @DisplayName("Combined Lifecycle Validation")
  class CombinedLifecycleValidation {

    @Test
    @DisplayName("Valid GENINDSENDFORDRING passes all lifecycle checks")
    void validGenindsendFordringPassesAllChecks() {
      FordringValidationRequest request =
          validRequestBuilder("GENINDSENDFORDRING")
              .mfGenindsendFordringStrukturPresent(true)
              .originalClaimWithdrawn(true)
              .originalWithdrawalReason("HENS")
              .fordringshaverKode("FH001")
              .originalFordringshaverKode("FH001")
              .stamdataMatchesOriginal(true)
              .originalClaimIsModr(false)
              .hasGenindsendPermission(true)
              .withdrawnDuringConversion(false)
              .hasPendingAction(false)
              .build();

      FordringValidationResult result = fireRules(request);

      // Check no lifecycle errors
      assertNoError(result, FordringErrorCode.GENINDSEND_NOT_WITHDRAWN.getCode());
      assertNoError(result, FordringErrorCode.GENINDSEND_INVALID_WITHDRAWAL_REASON.getCode());
      assertNoError(result, FordringErrorCode.GENINDSEND_DIFFERENT_FORDRINGSHAVER.getCode());
      assertNoError(result, FordringErrorCode.GENINDSEND_STAMDATA_MISMATCH.getCode());
      assertNoError(result, FordringErrorCode.GENINDSEND_MODR_NOT_ALLOWED.getCode());
    }

    @Test
    @DisplayName("Valid TILBAGEKALD passes all lifecycle checks")
    void validTilbagekaldPassesAllChecks() {
      FordringValidationRequest request =
          validRequestBuilder("TILBAGEKALD")
              .mfTilbagekaldFordringStrukturPresent(true)
              .hasTilbagekaldPermission(true)
              .withdrawnDuringConversion(false)
              .routedToDmi(false)
              .tilbagekaldAarsagKode("HENS")
              .hasUnwithdrawDividedClaims(false)
              .hasPendingAction(false)
              .build();

      FordringValidationResult result = fireRules(request);

      // Check no tilbagekald errors
      assertNoError(result, FordringErrorCode.WITHDRAWN_DURING_CONVERSION.getCode());
      assertNoError(result, FordringErrorCode.BORT_NOT_ALLOWED_FOR_DMI.getCode());
      assertNoError(result, FordringErrorCode.TILBAGEKALD_FEJL_WITH_VIRKNINGSDATO.getCode());
      assertNoError(result, FordringErrorCode.TILBAGEKALD_HOVEDFORDRING_WITH_UNDIVIDED.getCode());
    }

    @Test
    @DisplayName("Valid OANI with matching beløb and correct årsagkode passes")
    void validOaniWithMatchingBeloebAndAarsagkodePasses() {
      FordringValidationRequest request =
          validRequestBuilder("OPSKRIVNINGANNULLERETNEDSKRIVNINGINDBETALING")
              .mfOpskrivningAnnulleretNedskrivningIndbetalingStrukturPresent(true)
              .mfAktionIdRef("N001")
              .mfAktionIdRefExists(true)
              .referencedAktionExists(true)
              .opskrivningBeloeb(new BigDecimal("5000"))
              .referencedNedskrivningBeloeb(new BigDecimal("5000"))
              .referencedNedskrivningAarsagKode("INDB")
              .referencedActionType("NEDSKRIV")
              .referencedClaimWithdrawn(false)
              .referencedActionRejected(false)
              .claimIsRente(false)
              .originalClaimForOpskrivningWithdrawn(false)
              .virkningsDato(LocalDate.of(2024, 1, 15))
              .referencedVirkningFra(LocalDate.of(2024, 1, 15))
              .build();

      FordringValidationResult result = fireRules(request);

      // Check no opskrivning reference errors
      assertNoError(result, FordringErrorCode.BELOEB_MISMATCH.getCode());
      assertNoError(result, FordringErrorCode.VIRKNINGSDATO_MISMATCH.getCode());
      assertNoError(result, FordringErrorCode.OANI_WRONG_AARSAGKODE.getCode());
      assertNoError(result, FordringErrorCode.NOT_REFERENCING_NEDSKRIV.getCode());
      assertNoError(result, FordringErrorCode.OPSKRIVNING_ON_RENTE.getCode());
    }

    @Test
    @DisplayName("Valid NAOR with correct reference type and matching beløb passes")
    void validNaorWithCorrectReferenceAndBeloebPasses() {
      FordringValidationRequest request =
          validRequestBuilder("NEDSKRIVNINGANNULLERETOPSKRIVNINGREGULERING")
              .mfNedskrivningAnnulleretOpskrivningReguleringStrukturPresent(true)
              .mfAktionIdRef("O001")
              .mfAktionIdRefExists(true)
              .referencedAktionExists(true)
              .nedskrivningBeloeb(new BigDecimal("5000"))
              .referencedOpskrivningBeloeb(new BigDecimal("5000"))
              .referencedActionType("OPSKRIVNINGREGULERING")
              .referencedClaimWithdrawn(false)
              .referencedActionRejected(false)
              .referencedActionInDmiNotUdfoert(false)
              .existingAnnulleringForAction(false)
              .build();

      FordringValidationResult result = fireRules(request);

      // Check no NAOR errors
      assertNoError(result, FordringErrorCode.NAOR_WRONG_REFERENCE_TYPE.getCode());
      assertNoError(result, FordringErrorCode.NAOR_BELOEB_MISMATCH.getCode());
      assertNoError(result, FordringErrorCode.ANNULLERING_ALREADY_EXISTS.getCode());
      assertNoError(result, FordringErrorCode.REFERENCED_CLAIM_WITHDRAWN.getCode());
      assertNoError(result, FordringErrorCode.REFERENCED_ACTION_REJECTED.getCode());
    }
  }
}
