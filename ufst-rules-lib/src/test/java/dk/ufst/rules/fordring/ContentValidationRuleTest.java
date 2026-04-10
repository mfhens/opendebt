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
 * Tests for petition018 content validation rules.
 *
 * <p>Covers:
 *
 * <ul>
 *   <li>Document and Note Validation - errors 164, 181, 220, 413, 415, 516
 *   <li>Claim Amount Validation - errors 201, 215, 227, 408, 425
 *   <li>Sub-Claim Validation - errors 270, 459, 461, 423
 *   <li>Interest Validation - errors 436, 441, 442, 443
 *   <li>Nedskriv Reason Validation - errors 410, 433, 519, 571
 *   <li>Hovedstol Validation - errors 510, 512, 517, 518
 *   <li>Hæftelse Validation - errors 528, 531, 532, 533, 557, 559
 *   <li>Routing Validation - errors 422, 426, 565, 572
 *   <li>Claim Type Validation - errors 509, 537, 550, 574, 575
 *   <li>Identifier Validation - errors 486, 602, 603
 * </ul>
 */
class ContentValidationRuleTest extends AbstractFordringRuleTest {

  // =========================================================================
  // Document and Note Validation
  // =========================================================================

  @Nested
  @DisplayName("Document and Note Validation")
  class DocumentAndNoteValidation {

    @Test
    @DisplayName("Document exceeding max size is rejected with error 164")
    void documentExceedingMaxSizeIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .maxDocumentSizeBytes(15_000_000L) // 15MB
              .maxAllowedDocumentSizeBytes(10_000_000L) // 10MB limit
              .hovedfordringHasKategoriHf(true)
              .fordringIdKnown(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.DOCUMENT_SIZE_EXCEEDED);
      assertErrorMessageContains(result, 164, "filstørrelse er større end den tilladte grænse");
    }

    @Test
    @DisplayName("Document within max size passes")
    void documentWithinMaxSizePasses() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .maxDocumentSizeBytes(5_000_000L) // 5MB
              .maxAllowedDocumentSizeBytes(10_000_000L) // 10MB limit
              .hovedfordringHasKategoriHf(true)
              .fordringIdKnown(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertNoError(result, FordringErrorCode.DOCUMENT_SIZE_EXCEEDED.getCode());
    }

    @Test
    @DisplayName("Too many documents is rejected with error 181")
    void tooManyDocumentsIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .documentCount(15)
              .maxAllowedDocuments(10)
              .hovedfordringHasKategoriHf(true)
              .fordringIdKnown(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.DOCUMENT_COUNT_EXCEEDED);
      assertErrorMessageContains(result, 181, "Antal dokumenter indsendt per aktion større end");
    }

    @Test
    @DisplayName("Empty note is rejected with error 220")
    void emptyNoteIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .hasEmptyNote(true)
              .hovedfordringHasKategoriHf(true)
              .fordringIdKnown(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.EMPTY_NOTE);
      assertErrorMessageContains(
          result, 220, "Sagsbemærkninger på fordringen har ikke noget indhold");
    }

    @Test
    @DisplayName("Note exceeding max length is rejected with error 413")
    void noteExceedingMaxLengthIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .noteLength(5000)
              .maxAllowedNoteLength(4000)
              .hovedfordringHasKategoriHf(true)
              .fordringIdKnown(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.NOTE_LENGTH_EXCEEDED);
      assertErrorMessageContains(result, 413, "Sagsbemærkninger må max være på");
    }

    @Test
    @DisplayName("Disallowed document type is rejected with error 415")
    void disallowedDocumentTypeIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .hasDisallowedDocumentType(true)
              .hovedfordringHasKategoriHf(true)
              .fordringIdKnown(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.DISALLOWED_DOCUMENT_TYPE);
      assertErrorMessageContains(result, 415, "Dokumentets filtype er ikke tilladt");
    }

    @Test
    @DisplayName("OANI with documents on underlying fordring is rejected with error 516")
    void oaniWithDocumentsOnUnderlyingFordringIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("OPSKRIVNINGANNULLERETNEDSKRIVNINGINDBETALING")
              .mfOpskrivningAnnulleretNedskrivningIndbetalingStrukturPresent(true)
              .underlyingFordringHasDocuments(true)
              .fordringIdKnown(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.DOCUMENTS_ON_UNDERLYING_FORDRING);
      assertErrorMessageContains(result, 516, "underliggende fordring");
    }
  }

  // =========================================================================
  // Claim Amount Validation
  // =========================================================================

  @Nested
  @DisplayName("Claim Amount Validation")
  class ClaimAmountValidation {

    @Test
    @DisplayName("Hovedfordring without category HF is rejected with error 201")
    void hovedfordringWithoutCategoryHfIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .hovedfordringHasKategoriHf(false)
              .fordringIdKnown(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.HOVEDFORDRING_KATEGORI_MISSING);
      assertErrorMessageContains(result, 201, "hovedfordring skal have fordringtypekategori HF");
    }

    @Test
    @DisplayName("Claim with amount below lower limit is rejected with error 215")
    void claimWithAmountBelowLimitIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .claimAmount(BigDecimal.ZERO)
              .claimAmountLowerLimit(BigDecimal.ONE)
              .hovedfordringHasKategoriHf(true)
              .fordringIdKnown(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.CLAIM_AMOUNT_BELOW_LIMIT);
      assertErrorMessageContains(result, 215, "Fordringsbeløb ikke større end nedre grænse");
    }

    @Test
    @DisplayName("Opskrivning with zero correction amount is rejected with error 227")
    void opskrivningWithZeroCorrectionIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("OPSKRIVNINGREGULERING")
              .mfOpskrivningReguleringStrukturPresent(true)
              .hasZeroCorrectionAmount(true)
              .fordringIdKnown(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.ZERO_CORRECTION_AMOUNT);
      assertErrorMessageContains(result, 227, "Korrektion på kr. 0 ikke muligt");
    }

    @Test
    @DisplayName("NEDSKRIV with zero amount is rejected with error 408")
    void nedskrivWithZeroAmountIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("NEDSKRIV")
              .mfNedskrivFordringStrukturPresent(true)
              .nedskrivningBeloebValue(BigDecimal.ZERO)
              .fordringIdKnown(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.NEDSKRIV_BELOEB_NOT_POSITIVE);
      assertErrorMessageContains(result, 408, "Nedskrivningsbeløb skal være større end 0");
    }

    @Test
    @DisplayName("NEDSKRIV with negative amount is rejected with error 408")
    void nedskrivWithNegativeAmountIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("NEDSKRIV")
              .mfNedskrivFordringStrukturPresent(true)
              .nedskrivningBeloebValue(new BigDecimal("-100"))
              .fordringIdKnown(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.NEDSKRIV_BELOEB_NOT_POSITIVE);
    }

    @Test
    @DisplayName("Multiple hovedfordringer in one action is rejected with error 425")
    void multipleHovedfordringerIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .hovedfordringCount(2)
              .hovedfordringHasKategoriHf(true)
              .fordringIdKnown(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.MULTIPLE_HOVEDFORDRINGER);
      assertErrorMessageContains(result, 425, "kun oprettes en hovedfordring per aktion");
    }
  }

  // =========================================================================
  // Sub-Claim Validation
  // =========================================================================

  @Nested
  @DisplayName("Sub-Claim Validation")
  class SubClaimValidation {

    @Test
    @DisplayName("Sub-claim with different art type is rejected with error 270")
    void subclaimWithDifferentArtTypeIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .hasSubclaimArtTypeMismatch(true)
              .hovedfordringHasKategoriHf(true)
              .fordringIdKnown(true)
              .subclaimTypeAllowed(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.SUBCLAIM_ART_TYPE_MISMATCH);
      assertErrorMessageContains(result, 270, "Underfordringen matcher ikke art med hovedfordring");
    }

    @Test
    @DisplayName("Sub-claim with matching art type passes")
    void subclaimWithMatchingArtTypePasses() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .hasSubclaimArtTypeMismatch(false)
              .subclaimArtTypeMatches(true)
              .hovedfordringHasKategoriHf(true)
              .fordringIdKnown(true)
              .subclaimTypeAllowed(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertNoError(result, FordringErrorCode.SUBCLAIM_ART_TYPE_MISMATCH.getCode());
    }

    @Test
    @DisplayName("Sub-claim type not allowed for fordringshaver is rejected with error 459")
    void subclaimTypeNotAllowedIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .subclaimTypeAllowed(false)
              .hovedfordringHasKategoriHf(true)
              .fordringIdKnown(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.SUBCLAIM_TYPE_NOT_ALLOWED);
      assertErrorMessageContains(result, 459, "Underfordringstypen er ikke tilladt");
    }

    @Test
    @DisplayName("Related claim without HovedfordringID is rejected with error 461")
    void relatedClaimWithoutHovedfordringIdIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .relatedClaimMissingHovedfordringId(true)
              .hovedfordringHasKategoriHf(true)
              .fordringIdKnown(true)
              .subclaimTypeAllowed(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.RELATED_CLAIM_MISSING_HOVEDFORDRING_ID);
      assertErrorMessageContains(result, 461, "HovedfordringsID ikke angivet eller eksisterende");
    }

    @Test
    @DisplayName(
        "Related claim with HovedfordringID filled by fordringshaver is rejected with error 423")
    void hovedfordringIdFilledByFordringshaverIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .hovedfordringIdFilledByFordringshaver(true)
              .hovedfordringHasKategoriHf(true)
              .fordringIdKnown(true)
              .subclaimTypeAllowed(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.HOVEDFORDRING_ID_FILLED_BY_FORDRINGSHAVER);
      assertErrorMessageContains(result, 423, "HovedfordringID må ikke udfyldes af fordringshaver");
    }
  }

  // =========================================================================
  // Interest Validation
  // =========================================================================

  @Nested
  @DisplayName("Interest Validation")
  class InterestValidation {

    @ParameterizedTest
    @ValueSource(strings = {"03", "04", "07"})
    @DisplayName("MerRenteSats allowed for RenteSatsKode 03, 04, 07")
    void merRenteSatsAllowedForValidKode(String kode) {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .merRenteSats(new BigDecimal("5.0"))
              .renteSatsKode(kode)
              .hovedfordringHasKategoriHf(true)
              .fordringIdKnown(true)
              .subclaimTypeAllowed(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertNoError(result, FordringErrorCode.MER_RENTESATS_INVALID_FOR_KODE.getCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"01", "02"})
    @DisplayName("MerRenteSats rejected for RenteSatsKode 01, 02 with error 436")
    void merRenteSatsRejectedForInvalidKode(String kode) {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .merRenteSats(new BigDecimal("5.0"))
              .renteSatsKode(kode)
              .hovedfordringHasKategoriHf(true)
              .fordringIdKnown(true)
              .subclaimTypeAllowed(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.MER_RENTESATS_INVALID_FOR_KODE);
      assertErrorMessageContains(result, 436, "MerRenteSats må kun benyttes med RenteSatsKode");
    }

    @Test
    @DisplayName("RenteRegel 002 with invalid combination is rejected with error 441")
    void renteRegel002InvalidCombinationIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .renteRegel("002")
              .renteRegel002InvalidCombination(true)
              .hovedfordringHasKategoriHf(true)
              .fordringIdKnown(true)
              .subclaimTypeAllowed(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.RENTE_REGEL_002_INVALID_COMBINATION);
      assertErrorMessageContains(result, 441, "Rente regel 002 kan kun bruges");
    }

    @Test
    @DisplayName("RenteRegel 002 with valid combination passes")
    void renteRegel002ValidCombinationPasses() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .renteRegel("002")
              .renteRegel002InvalidCombination(false)
              .hovedfordringHasKategoriHf(true)
              .fordringIdKnown(true)
              .subclaimTypeAllowed(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertNoError(result, FordringErrorCode.RENTE_REGEL_002_INVALID_COMBINATION.getCode());
    }

    @Test
    @DisplayName("Invalid RenteSatsKode for PSRM is rejected with error 442")
    void invalidRenteSatsKodeForPsrmIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .targetsPsrm(true)
              .renteSatsKodeInvalidForPsrm(true)
              .hovedfordringHasKategoriHf(true)
              .fordringIdKnown(true)
              .subclaimTypeAllowed(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.RENTE_SATS_KODE_INVALID_FOR_PSRM);
      assertErrorMessageContains(result, 442, "RenteSatsKode kan ikke benyttes i PSRM");
    }

    @Test
    @DisplayName("Interest on non-interest-bearing claim type is rejected with error 443")
    void interestOnNonBearingTypeIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .interestOnNonBearingType(true)
              .hovedfordringHasKategoriHf(true)
              .fordringIdKnown(true)
              .subclaimTypeAllowed(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.INTEREST_ON_NON_BEARING_TYPE);
      assertErrorMessageContains(result, 443, "Fordringstypen er ikke rentebærende");
    }
  }

  // =========================================================================
  // Nedskriv Reason Validation
  // =========================================================================

  @Nested
  @DisplayName("Nedskriv Reason Validation")
  class NedskrivReasonValidation {

    @Test
    @DisplayName("ÅrsagKode REGU at hæftelse level is rejected with error 410")
    void reguAtHaeftelseLevelIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("NEDSKRIV")
              .mfNedskrivFordringStrukturPresent(true)
              .reguAtHaeftelseLevel(true)
              .fordringIdKnown(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.REGU_AT_HAEFTELSE_LEVEL);
      assertErrorMessageContains(
          result, 410, "Årsagskoden REGU kan kun benyttes på fordringsniveau");
    }

    @Test
    @DisplayName("Nedskrivning requiring debtor without debtor is rejected with error 433")
    void nedskrivRequiringDebtorWithoutDebtorIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("NEDSKRIV")
              .mfNedskrivFordringStrukturPresent(true)
              .nedskrivRequiresDebtor(true)
              .nedskrivDebtorProvided(false)
              .fordringIdKnown(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.NEDSKRIV_REQUIRES_DEBTOR);
      assertErrorMessageContains(result, 433, "skyldners identitet");
    }

    @Test
    @DisplayName("REGU nedskrivning with VirkningsDato is rejected with error 519")
    void reguWithVirkningsDatoIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("NEDSKRIV")
              .mfNedskrivFordringStrukturPresent(true)
              .nedskrivAarsagKode("REGU")
              .virkningsDato(LocalDate.of(2024, 3, 1))
              .virkningsDatoRequired(false)
              .fordringIdKnown(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.REGU_WITH_VIRKNINGSDATO);
      assertErrorMessageContains(result, 519, "må ikke medsendes en virkningsdato");
    }

    @Test
    @DisplayName("ÅrsagKode FAST for PSRM claim is rejected with error 571")
    void fastForPsrmClaimIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("NEDSKRIV")
              .mfNedskrivFordringStrukturPresent(true)
              .nedskrivAarsagKode("FAST")
              .targetsPsrm(true)
              .fordringIdKnown(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.FAST_FOR_PSRM_CLAIM);
      assertErrorMessageContains(result, 571, "ÅrsagKode FAST kan ikke benyttes for PSRM");
    }
  }

  // =========================================================================
  // Hovedstol Validation
  // =========================================================================

  @Nested
  @DisplayName("Hovedstol Validation")
  class HovedstolValidation {

    @Test
    @DisplayName("New hovedstol not higher than previous is rejected with error 510")
    void newHovedstolNotHigherIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("AENDRFORDRING")
              .mfAendrFordringStrukturPresent(true)
              .modifyingHovedstol(true)
              .currentHovedstol(new BigDecimal("10000"))
              .newHovedstol(new BigDecimal("9000"))
              .fhiStrukturPresent(true)
              .fordringIdKnown(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.NEW_HOVEDSTOL_NOT_HIGHER);
      assertErrorMessageContains(result, 510, "hovedstol er lavere eller ens");
    }

    @Test
    @DisplayName("New hovedstol higher than previous passes")
    void newHovedstolHigherPasses() {
      FordringValidationRequest request =
          validRequestBuilder("AENDRFORDRING")
              .mfAendrFordringStrukturPresent(true)
              .modifyingHovedstol(true)
              .currentHovedstol(new BigDecimal("10000"))
              .newHovedstol(new BigDecimal("12000"))
              .fhiStrukturPresent(true)
              .fordringIdKnown(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertNoError(result, FordringErrorCode.NEW_HOVEDSTOL_NOT_HIGHER.getCode());
    }

    @Test
    @DisplayName("Hovedstol change for DMI claim is rejected with error 512")
    void hovedstolChangeForDmiIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("AENDRFORDRING")
              .mfAendrFordringStrukturPresent(true)
              .modifyingHovedstol(true)
              .routedToDmi(true)
              .fhiStrukturPresent(true)
              .fordringIdKnown(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.HOVEDSTOL_CHANGE_FOR_DMI);
      assertErrorMessageContains(result, 512, "DMI understøtter ikke ændring af hovedstol");
    }

    @Test
    @DisplayName("AENDRFORDRING without FHI struktur is rejected with error 517")
    void aendrfordringWithoutFhiStrukturIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("AENDRFORDRING")
              .mfAendrFordringStrukturPresent(true)
              .modifyingHovedstol(true)
              .fhiStrukturPresent(false)
              .fordringIdKnown(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.FHI_STRUKTUR_MISSING);
      assertErrorMessageContains(result, 517, "FejlagtigHovedstolIndberetningStruktur mangler");
    }

    @Test
    @DisplayName("Hovedstol change on withdrawn claim is rejected with error 518")
    void hovedstolChangeOnWithdrawnIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("AENDRFORDRING")
              .mfAendrFordringStrukturPresent(true)
              .modifyingHovedstol(true)
              .claimIsWithdrawn(true)
              .fhiStrukturPresent(true)
              .fordringIdKnown(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.HOVEDSTOL_CHANGE_ON_WITHDRAWN);
      assertErrorMessageContains(result, 518, "tilbagekaldt");
    }
  }

  // =========================================================================
  // Hæftelse Validation
  // =========================================================================

  @Nested
  @DisplayName("Hæftelse Validation")
  class HaeftelseValidation {

    @Test
    @DisplayName("Duplicate hæftere is rejected with error 528")
    void duplicateHaeftereIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .hasDuplicateHaeftere(true)
              .hovedfordringHasKategoriHf(true)
              .fordringIdKnown(true)
              .subclaimTypeAllowed(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.DUPLICATE_HAEFTERE);
      assertErrorMessageContains(result, 528, "hæfter hvor hæfteren er den samme");
    }

    @Test
    @DisplayName("HaeftelseDomId without date is rejected with error 531")
    void haeftelseDomIdWithoutDateIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .haeftelseDomIdWithoutDate(true)
              .hovedfordringHasKategoriHf(true)
              .fordringIdKnown(true)
              .subclaimTypeAllowed(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.HAEFTELSE_DOM_ID_WITHOUT_DATE);
      assertErrorMessageContains(result, 531, "HaeftelseDomId men ingen HaeftelseDomDato");
    }

    @Test
    @DisplayName("HaeftelseDomDato without id is rejected with error 532")
    void haeftelseDomDateWithoutIdIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .haeftelseDomDateWithoutId(true)
              .hovedfordringHasKategoriHf(true)
              .fordringIdKnown(true)
              .subclaimTypeAllowed(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.HAEFTELSE_DOM_DATE_WITHOUT_ID);
      assertErrorMessageContains(
          result, 532, "ikke et HaeftelseDomId men har alligevel en HaeftelseDomDato");
    }

    @Test
    @DisplayName("Future DomsDato is rejected with error 533")
    void futureDomsDatoIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .hasFutureDomDate(true)
              .hovedfordringHasKategoriHf(true)
              .fordringIdKnown(true)
              .subclaimTypeAllowed(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.FUTURE_DOM_DATE);
      assertErrorMessageContains(
          result, 533, "Domsdato eller Forligsdato kan ikke være i fremtiden");
    }

    @Test
    @DisplayName("DMI claim with mismatched SRB/Forfald is rejected with error 557")
    void dmiMismatchedSrbForfaldIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .routedToDmi(true)
              .dmiMismatchedSrbForfald(true)
              .hovedfordringHasKategoriHf(true)
              .fordringIdKnown(true)
              .subclaimTypeAllowed(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.DMI_MISMATCHED_SRB_FORFALD);
      assertErrorMessageContains(result, 557, "SRB og Forfald er forskellig på to hæftere");
    }

    @Test
    @DisplayName("DMI claim with hæftelse-level documents is rejected with error 559")
    void dmiHaeftelseDocumentsIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .routedToDmi(true)
              .dmiHaeftelseDocuments(true)
              .hovedfordringHasKategoriHf(true)
              .fordringIdKnown(true)
              .subclaimTypeAllowed(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.DMI_HAEFTELSE_DOCUMENTS);
      assertErrorMessageContains(result, 559, "dokumenter på hæftelsesniveau");
    }
  }

  // =========================================================================
  // Routing Validation
  // =========================================================================

  @Nested
  @DisplayName("Routing Validation")
  class RoutingValidation {

    @Test
    @DisplayName("Synchronous portal action for DMI claim is rejected with error 422")
    void syncPortalActionForDmiIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .syncPortalActionForDmi(true)
              .hovedfordringHasKategoriHf(true)
              .fordringIdKnown(true)
              .subclaimTypeAllowed(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.SYNC_PORTAL_DMI);
      assertErrorMessageContains(result, 422, "via portalen på fordringer i DMI");
    }

    @Test
    @DisplayName("NAOR to DMI is rejected with error 426")
    void naorToDmiIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("NEDSKRIVNINGANNULLERETOPSKRIVNINGREGULERING")
              .mfNedskrivningAnnulleretOpskrivningReguleringStrukturPresent(true)
              .routedToDmi(true)
              .fordringIdKnown(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.NAOR_TO_DMI);
      assertErrorMessageContains(
          result, 426, "NedskrivningAnnulleretOpskrivningRegulering understøttes ikke af DMI");
    }

    @Test
    @DisplayName("DMI claim with invalid AKR length is rejected with error 565")
    void dmiInvalidAkrLengthIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .routedToDmi(true)
              .akrLengthExceedsForDmi(true)
              .hovedfordringHasKategoriHf(true)
              .fordringIdKnown(true)
              .subclaimTypeAllowed(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.DMI_INVALID_AKR_LENGTH);
      assertErrorMessageContains(result, 565, "AKR-nummer");
    }

    @Test
    @DisplayName("ForeløbigFastsat claim to PSRM is rejected with error 572")
    void foreloebigFastsatToPsrmIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .foreloebigFastsatToPsrm(true)
              .hovedfordringHasKategoriHf(true)
              .fordringIdKnown(true)
              .subclaimTypeAllowed(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.FORELOEBIG_FASTSAT_TO_PSRM);
      assertErrorMessageContains(result, 572, "forløbig fastsat kan ikke sendes til PSRM");
    }
  }

  // =========================================================================
  // Claim Type Validation
  // =========================================================================

  @Nested
  @DisplayName("Claim Type Validation")
  class ClaimTypeValidation {

    @Test
    @DisplayName("Unknown FordringID is rejected with error 509")
    void unknownFordringIdIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .fordringIdKnown(false)
              .hovedfordringHasKategoriHf(true)
              .subclaimTypeAllowed(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.UNKNOWN_FORDRING_ID);
      assertErrorMessageContains(result, 509, "FordringID er ikke kendt af Fordring");
    }

    @Test
    @DisplayName("AENDRFORDRING on INDR claim is rejected with error 537")
    void aendrfordringOnIndrIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("AENDRFORDRING")
              .mfAendrFordringStrukturPresent(true)
              .aendrfordringOnIndr(true)
              .fordringIdKnown(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.AENDRFORDRING_ON_INDR);
      assertErrorMessageContains(result, 537, "FordringAendr aktioner på inddrivelsesfordringer");
    }

    @Test
    @DisplayName("Missing required stamdata is rejected with error 550")
    void missingRequiredStamdataIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .missingRequiredStamdata(true)
              .hovedfordringHasKategoriHf(true)
              .fordringIdKnown(true)
              .subclaimTypeAllowed(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.MISSING_REQUIRED_STAMDATA);
      assertErrorMessageContains(result, 550, "Stamdata felter på fordringsaktionen mangler");
    }

    @Test
    @DisplayName("Inactive claim type in PSRM is rejected with error 574")
    void inactiveClaimTypePsrmIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .targetsPsrm(true)
              .claimTypeInactiveInPsrm(true)
              .hovedfordringHasKategoriHf(true)
              .fordringIdKnown(true)
              .subclaimTypeAllowed(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.INACTIVE_CLAIM_TYPE_PSRM);
      assertErrorMessageContains(result, 574, "Fordringstypen er inaktiv i PSRM");
    }

    @Test
    @DisplayName("Missing required BFE field is rejected with error 575")
    void missingRequiredBfeIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .missingRequiredBfe(true)
              .hovedfordringHasKategoriHf(true)
              .fordringIdKnown(true)
              .subclaimTypeAllowed(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.MISSING_REQUIRED_BFE);
      assertErrorMessageContains(result, 575, "Feltet BFE skal være udfyldt");
    }
  }

  // =========================================================================
  // Identifier Validation
  // =========================================================================

  @Nested
  @DisplayName("Identifier Validation")
  class IdentifierValidation {

    @Test
    @DisplayName("Non-unique FordringshaverRefID is rejected with error 486")
    void nonUniqueFordringshaverRefIdIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .nonUniqueFordringshaverRefId(true)
              .hovedfordringHasKategoriHf(true)
              .fordringIdKnown(true)
              .subclaimTypeAllowed(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.NON_UNIQUE_FORDRINGSHAVER_REF_ID);
      assertErrorMessageContains(result, 486, "FordringshaverRefID er ikke entydig");
    }

    @Test
    @DisplayName("Modification of error-withdrawn claim is rejected with error 602")
    void modificationOfErrorWithdrawnIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("AENDRFORDRING")
              .mfAendrFordringStrukturPresent(true)
              .alreadyWithdrawnWithFejl(true)
              .fordringIdKnown(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.MODIFICATION_OF_ERROR_WITHDRAWN);
      assertErrorMessageContains(
          result, 602, "allerede tilbagekaldt/tilbagesend med årsagskode FEJL");
    }

    @Test
    @DisplayName("Non-FEJL withdrawal after HENS/KLAG/BORD is rejected with error 603")
    void nonFejlWithdrawalAfterHensKlagBordIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("TILBAGEKALD")
              .mfTilbagekaldFordringStrukturPresent(true)
              .nonFejlWithdrawalAfterHensKlagBord(true)
              .fordringIdKnown(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.NON_FEJL_WITHDRAWAL_AFTER_HENS_KLAG_BORD);
      assertErrorMessageContains(result, 603, "kun returneres eller tilbagesendes med FEJL");
    }
  }

  // =========================================================================
  // Combined Content Validation
  // =========================================================================

  @Nested
  @DisplayName("Combined Content Validation")
  class CombinedContentValidation {

    @Test
    @DisplayName("Valid OPRETFORDRING passes all content validation checks")
    void validOpretfordringPassesAllChecks() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              // Document/Note - all valid
              .maxDocumentSizeBytes(5_000_000L)
              .maxAllowedDocumentSizeBytes(10_000_000L)
              .documentCount(5)
              .maxAllowedDocuments(10)
              .hasEmptyNote(false)
              .noteLength(1000)
              .maxAllowedNoteLength(4000)
              .hasDisallowedDocumentType(false)
              // Claim Amount - all valid
              .hovedfordringHasKategoriHf(true)
              .claimAmount(new BigDecimal("1000"))
              .claimAmountLowerLimit(BigDecimal.ZERO)
              .hovedfordringCount(1)
              // Sub-claim - all valid
              .hasSubclaimArtTypeMismatch(false)
              .subclaimTypeAllowed(true)
              .relatedClaimMissingHovedfordringId(false)
              .hovedfordringIdFilledByFordringshaver(false)
              // Interest - all valid
              .interestOnNonBearingType(false)
              .renteRegel002InvalidCombination(false)
              .renteSatsKodeInvalidForPsrm(false)
              // Hæftelse - all valid
              .hasDuplicateHaeftere(false)
              .haeftelseDomIdWithoutDate(false)
              .haeftelseDomDateWithoutId(false)
              .hasFutureDomDate(false)
              // Routing - all valid
              .syncPortalActionForDmi(false)
              .foreloebigFastsatToPsrm(false)
              // Claim Type - all valid
              .fordringIdKnown(true)
              .missingRequiredStamdata(false)
              .missingRequiredBfe(false)
              // Identifier - all valid
              .nonUniqueFordringshaverRefId(false)
              .build();

      FordringValidationResult result = fireRules(request);

      // No content validation errors
      assertNoError(result, FordringErrorCode.DOCUMENT_SIZE_EXCEEDED.getCode());
      assertNoError(result, FordringErrorCode.DOCUMENT_COUNT_EXCEEDED.getCode());
      assertNoError(result, FordringErrorCode.EMPTY_NOTE.getCode());
      assertNoError(result, FordringErrorCode.NOTE_LENGTH_EXCEEDED.getCode());
      assertNoError(result, FordringErrorCode.DISALLOWED_DOCUMENT_TYPE.getCode());
      assertNoError(result, FordringErrorCode.HOVEDFORDRING_KATEGORI_MISSING.getCode());
      assertNoError(result, FordringErrorCode.CLAIM_AMOUNT_BELOW_LIMIT.getCode());
      assertNoError(result, FordringErrorCode.MULTIPLE_HOVEDFORDRINGER.getCode());
      assertNoError(result, FordringErrorCode.SUBCLAIM_ART_TYPE_MISMATCH.getCode());
      assertNoError(result, FordringErrorCode.SUBCLAIM_TYPE_NOT_ALLOWED.getCode());
      assertNoError(result, FordringErrorCode.DUPLICATE_HAEFTERE.getCode());
      assertNoError(result, FordringErrorCode.UNKNOWN_FORDRING_ID.getCode());
    }

    @Test
    @DisplayName("Multiple content validation errors are reported together")
    void multipleContentErrorsReportedTogether() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              // Multiple errors
              .hovedfordringHasKategoriHf(false) // Error 201
              .claimAmount(BigDecimal.ZERO)
              .claimAmountLowerLimit(BigDecimal.ONE) // Error 215
              .hasEmptyNote(true) // Error 220
              .fordringIdKnown(true)
              .subclaimTypeAllowed(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.HOVEDFORDRING_KATEGORI_MISSING);
      assertHasError(result, FordringErrorCode.CLAIM_AMOUNT_BELOW_LIMIT);
      assertHasError(result, FordringErrorCode.EMPTY_NOTE);
    }
  }
}
