package dk.ufst.rules.fordring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import dk.ufst.opendebt.common.dto.fordring.FordringErrorCode;
import dk.ufst.opendebt.common.dto.fordring.FordringValidationRequest;
import dk.ufst.opendebt.common.dto.fordring.FordringValidationResult;
import dk.ufst.rules.test.AbstractFordringRuleTest;

/**
 * Tests for petition016 claimant authorization validation rules.
 *
 * <p>Covers system reporter validation (error 400), INDR permission (error 416), MODR permission
 * (error 419), nedskriv permission (error 420), tilbagekald permission (error 421), portal
 * agreement (error 437), complex action permissions (errors 465, 466, 497, 501, 508), hovedstol
 * permission (error 511), genindsend permission (error 543), and SSO access validation (error 480).
 */
class AuthorizationValidationRuleTest extends AbstractFordringRuleTest {

  @Nested
  @DisplayName("System Reporter Validation (Rule 400)")
  class SystemReporterValidation {

    @Test
    @DisplayName("System reporter authorized for fordringshaver passes validation")
    void authorizedSystemReporterPasses() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .systemToSystem(true)
              .systemReporterId("SYS001")
              .systemReporterAuthorized(true)
              .fordringshaverKode("FH001")
              .hasIndrPermission(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertNoError(result, FordringErrorCode.SYSTEM_REPORTER_UNAUTHORIZED.getCode());
    }

    @Test
    @DisplayName("System reporter not authorized for fordringshaver is rejected with error 400")
    void unauthorizedSystemReporterIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .systemToSystem(true)
              .systemReporterId("SYS002")
              .systemReporterAuthorized(false)
              .fordringshaverKode("FH001")
              .hasIndrPermission(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.SYSTEM_REPORTER_UNAUTHORIZED);
      assertErrorMessageContains(
          result,
          400,
          "Systemleverandør der indberetter kan ikke indberette for den angivne fordringshaver");
    }
  }

  @Nested
  @DisplayName("INDR Permission (Rule 416)")
  class IndrPermissionValidation {

    @Test
    @DisplayName("Fordringshaver with INDR permission can submit INDR claims")
    void fordringshaverWithIndrPermissionPasses() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .artType("INDR")
              .hasIndrPermission(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertNoError(result, FordringErrorCode.INDR_PERMISSION_MISSING.getCode());
    }

    @Test
    @DisplayName("Fordringshaver without INDR permission cannot submit INDR claims (OPRETFORDRING)")
    void fordringshaverWithoutIndrPermissionIsRejectedOpret() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .artType("INDR")
              .hasIndrPermission(false)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.INDR_PERMISSION_MISSING);
      assertErrorMessageContains(result, 416, "må ikke indberette inddrivelsesfordringer");
    }

    @Test
    @DisplayName(
        "Fordringshaver without INDR permission cannot resubmit INDR claims (GENINDSENDFORDRING)")
    void fordringshaverWithoutIndrPermissionIsRejectedGenindsend() {
      FordringValidationRequest request =
          validRequestBuilder("GENINDSENDFORDRING")
              .mfGenindsendFordringStrukturPresent(true)
              .artType("INDR")
              .hasIndrPermission(false)
              .hasGenindsendPermission(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.INDR_PERMISSION_MISSING);
    }
  }

  @Nested
  @DisplayName("MODR Permission (Rule 419)")
  class ModrPermissionValidation {

    @Test
    @DisplayName("Fordringshaver with MODR permission can submit MODR claims")
    void fordringshaverWithModrPermissionPasses() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .artType("MODR")
              .hasModrPermission(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertNoError(result, FordringErrorCode.MODR_PERMISSION_MISSING.getCode());
    }

    @Test
    @DisplayName("Fordringshaver without MODR permission cannot submit MODR claims (OPRETFORDRING)")
    void fordringshaverWithoutModrPermissionIsRejectedOpret() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .artType("MODR")
              .hasModrPermission(false)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.MODR_PERMISSION_MISSING);
      assertErrorMessageContains(result, 419, "må ikke indberette modregningsfordringer");
    }

    @Test
    @DisplayName(
        "Fordringshaver without MODR permission cannot resubmit MODR claims (GENINDSENDFORDRING)")
    void fordringshaverWithoutModrPermissionIsRejectedGenindsend() {
      FordringValidationRequest request =
          validRequestBuilder("GENINDSENDFORDRING")
              .mfGenindsendFordringStrukturPresent(true)
              .artType("MODR")
              .hasModrPermission(false)
              .hasGenindsendPermission(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.MODR_PERMISSION_MISSING);
    }
  }

  @Nested
  @DisplayName("Nedskriv Permission (Rule 420)")
  class NedskrivPermissionValidation {

    @Test
    @DisplayName("Fordringshaver with nedskriv permission can perform write-downs")
    void fordringshaverWithNedskrivPermissionPasses() {
      FordringValidationRequest request =
          validRequestBuilder("NEDSKRIV")
              .mfNedskrivFordringStrukturPresent(true)
              .hasNedskrivPermission(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertNoError(result, FordringErrorCode.NEDSKRIV_PERMISSION_MISSING.getCode());
    }

    @Test
    @DisplayName("Fordringshaver without nedskriv permission cannot perform write-downs")
    void fordringshaverWithoutNedskrivPermissionIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("NEDSKRIV")
              .mfNedskrivFordringStrukturPresent(true)
              .hasNedskrivPermission(false)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.NEDSKRIV_PERMISSION_MISSING);
      assertErrorMessageContains(result, 420, "må ikke indberette nedskrivninger");
    }
  }

  @Nested
  @DisplayName("Tilbagekald Permission (Rule 421)")
  class TilbagekaldPermissionValidation {

    @Test
    @DisplayName("Fordringshaver with tilbagekald permission can withdraw claims")
    void fordringshaverWithTilbagekaldPermissionPasses() {
      FordringValidationRequest request =
          validRequestBuilder("TILBAGEKALD")
              .mfTilbagekaldFordringStrukturPresent(true)
              .hasTilbagekaldPermission(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertNoError(result, FordringErrorCode.TILBAGEKALD_PERMISSION_MISSING.getCode());
    }

    @Test
    @DisplayName("Fordringshaver without tilbagekald permission cannot withdraw claims")
    void fordringshaverWithoutTilbagekaldPermissionIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("TILBAGEKALD")
              .mfTilbagekaldFordringStrukturPresent(true)
              .hasTilbagekaldPermission(false)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.TILBAGEKALD_PERMISSION_MISSING);
      assertErrorMessageContains(result, 421, "må ikke indberette tilbagekald");
    }
  }

  @Nested
  @DisplayName("Portal Permission (Rule 437)")
  class PortalPermissionValidation {

    @Test
    @DisplayName("Fordringshaver with portal agreement can submit via portal")
    void fordringshaverWithPortalAgreementPasses() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .portalSubmission(true)
              .hasPortalAgreement(true)
              .validSsoAccess(true)
              .hasIndrPermission(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertNoError(result, FordringErrorCode.PORTAL_AGREEMENT_MISSING.getCode());
    }

    @Test
    @DisplayName("Fordringshaver without portal agreement cannot submit via portal")
    void fordringshaverWithoutPortalAgreementIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .portalSubmission(true)
              .hasPortalAgreement(false)
              .hasIndrPermission(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.PORTAL_AGREEMENT_MISSING);
      assertErrorMessageContains(result, 437, "ikke oprettet en aftale om indberetning via portal");
    }
  }

  @Nested
  @DisplayName("Complex Correction Action Permissions (Rules 465, 466, 497, 501, 508)")
  class ComplexActionPermissionValidation {

    @Test
    @DisplayName("Fordringshaver without OANI permission is rejected with error 465")
    void fordringshaverWithoutOaniPermissionIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("OPSKRIVNINGANNULLERETNEDSKRIVNINGINDBETALING")
              .mfOpskrivningAnnulleretNedskrivningIndbetalingStrukturPresent(true)
              .hasOaniPermission(false)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.OANI_PERMISSION_MISSING);
      assertErrorMessageContains(
          result, 465, "må ikke indberette OpskrivningAnnulleretNedskrivningIndbetaling");
    }

    @Test
    @DisplayName("Fordringshaver with OANI permission passes")
    void fordringshaverWithOaniPermissionPasses() {
      FordringValidationRequest request =
          validRequestBuilder("OPSKRIVNINGANNULLERETNEDSKRIVNINGINDBETALING")
              .mfOpskrivningAnnulleretNedskrivningIndbetalingStrukturPresent(true)
              .hasOaniPermission(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertNoError(result, FordringErrorCode.OANI_PERMISSION_MISSING.getCode());
    }

    @Test
    @DisplayName(
        "Fordringshaver without opskrivning regulering permission is rejected with error 466")
    void fordringshaverWithoutOpskrivningReguleringPermissionIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("OPSKRIVNINGREGULERING")
              .mfOpskrivningReguleringStrukturPresent(true)
              .hasOpskrivningReguleringPermission(false)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.OPSKRIVNING_REGULERING_PERMISSION_MISSING);
      assertErrorMessageContains(
          result, 466, "må ikke indberette Opskrivninger med årsag opskrivningRegulering");
    }

    @Test
    @DisplayName("Fordringshaver with opskrivning regulering permission passes")
    void fordringshaverWithOpskrivningReguleringPermissionPasses() {
      FordringValidationRequest request =
          validRequestBuilder("OPSKRIVNINGREGULERING")
              .mfOpskrivningReguleringStrukturPresent(true)
              .hasOpskrivningReguleringPermission(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertNoError(result, FordringErrorCode.OPSKRIVNING_REGULERING_PERMISSION_MISSING.getCode());
    }

    @Test
    @DisplayName("Fordringshaver without OONR permission is rejected with error 497")
    void fordringshaverWithoutOonrPermissionIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("OPSKRIVNINGOMGJORTNEDSKRIVNINGREGULERING")
              .mfOpskrivningOmgjortNedskrivningReguleringStrukturPresent(true)
              .hasOonrPermission(false)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.OONR_PERMISSION_MISSING);
      assertErrorMessageContains(
          result, 497, "må ikke indberette OpskrivningOmgjortNedskrivningRegulering");
    }

    @Test
    @DisplayName("Fordringshaver with OONR permission passes")
    void fordringshaverWithOonrPermissionPasses() {
      FordringValidationRequest request =
          validRequestBuilder("OPSKRIVNINGOMGJORTNEDSKRIVNINGREGULERING")
              .mfOpskrivningOmgjortNedskrivningReguleringStrukturPresent(true)
              .hasOonrPermission(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertNoError(result, FordringErrorCode.OONR_PERMISSION_MISSING.getCode());
    }

    @Test
    @DisplayName("Fordringshaver without NAOR permission is rejected with error 501")
    void fordringshaverWithoutNaorPermissionIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("NEDSKRIVNINGANNULLERETOPSKRIVNINGREGULERING")
              .mfNedskrivningAnnulleretOpskrivningReguleringStrukturPresent(true)
              .hasNaorPermission(false)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.NAOR_PERMISSION_MISSING);
      assertErrorMessageContains(
          result, 501, "må ikke indberette NedskrivningAnnulleretOpskrivningRegulering");
    }

    @Test
    @DisplayName("Fordringshaver with NAOR permission passes")
    void fordringshaverWithNaorPermissionPasses() {
      FordringValidationRequest request =
          validRequestBuilder("NEDSKRIVNINGANNULLERETOPSKRIVNINGREGULERING")
              .mfNedskrivningAnnulleretOpskrivningReguleringStrukturPresent(true)
              .hasNaorPermission(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertNoError(result, FordringErrorCode.NAOR_PERMISSION_MISSING.getCode());
    }

    @Test
    @DisplayName("Fordringshaver without NAOI permission is rejected with error 508")
    void fordringshaverWithoutNaoiPermissionIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("NEDSKRIVNINGANNULLERETOPSKRIVNINGINDBETALING")
              .mfNedskrivningAnnulleretOpskrivningIndbetalingStrukturPresent(true)
              .hasNaoiPermission(false)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.NAOI_PERMISSION_MISSING);
      assertErrorMessageContains(
          result, 508, "må ikke indberette NedskrivningAnnulleretOpskrivningIndbetaling");
    }

    @Test
    @DisplayName("Fordringshaver with NAOI permission passes")
    void fordringshaverWithNaoiPermissionPasses() {
      FordringValidationRequest request =
          validRequestBuilder("NEDSKRIVNINGANNULLERETOPSKRIVNINGINDBETALING")
              .mfNedskrivningAnnulleretOpskrivningIndbetalingStrukturPresent(true)
              .hasNaoiPermission(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertNoError(result, FordringErrorCode.NAOI_PERMISSION_MISSING.getCode());
    }
  }

  @Nested
  @DisplayName("Hovedstol Permission (Rule 511)")
  class HovedstolPermissionValidation {

    @Test
    @DisplayName("Fordringshaver with hovedstol permission can modify principal")
    void fordringshaverWithHovedstolPermissionPasses() {
      FordringValidationRequest request =
          validRequestBuilder("AENDRFORDRING")
              .mfAendrFordringStrukturPresent(true)
              .modifyingHovedstol(true)
              .hasHovedstolPermission(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertNoError(result, FordringErrorCode.HOVEDSTOL_PERMISSION_MISSING.getCode());
    }

    @Test
    @DisplayName("Fordringshaver without hovedstol permission cannot modify principal")
    void fordringshaverWithoutHovedstolPermissionIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("AENDRFORDRING")
              .mfAendrFordringStrukturPresent(true)
              .modifyingHovedstol(true)
              .hasHovedstolPermission(false)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.HOVEDSTOL_PERMISSION_MISSING);
      assertErrorMessageContains(result, 511, "må ikke indberette hovedstolændringer");
    }

    @Test
    @DisplayName("AENDRFORDRING not modifying hovedstol does not require hovedstol permission")
    void aendrFordringNotModifyingHovedstolPasses() {
      FordringValidationRequest request =
          validRequestBuilder("AENDRFORDRING")
              .mfAendrFordringStrukturPresent(true)
              .modifyingHovedstol(false)
              .hasHovedstolPermission(false)
              .build();

      FordringValidationResult result = fireRules(request);

      assertNoError(result, FordringErrorCode.HOVEDSTOL_PERMISSION_MISSING.getCode());
    }
  }

  @Nested
  @DisplayName("Genindsend Permission (Rule 543)")
  class GenindsendPermissionValidation {

    @Test
    @DisplayName("Fordringshaver with resubmit permission can resubmit claims")
    void fordringshaverWithResubmitPermissionPasses() {
      FordringValidationRequest request =
          validRequestBuilder("GENINDSENDFORDRING")
              .mfGenindsendFordringStrukturPresent(true)
              .hasGenindsendPermission(true)
              .hasIndrPermission(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertNoError(result, FordringErrorCode.GENINDSEND_PERMISSION_MISSING.getCode());
    }

    @Test
    @DisplayName("Fordringshaver without resubmit permission cannot resubmit claims")
    void fordringshaverWithoutResubmitPermissionIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("GENINDSENDFORDRING")
              .mfGenindsendFordringStrukturPresent(true)
              .hasGenindsendPermission(false)
              .hasIndrPermission(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.GENINDSEND_PERMISSION_MISSING);
      assertErrorMessageContains(result, 543, "må ikke indberette genindsend aktioner");
    }
  }

  @Nested
  @DisplayName("SSO Access Validation (Rule 480)")
  class SsoAccessValidation {

    @Test
    @DisplayName("Portal user with valid SSO access passes validation")
    void portalUserWithValidSsoAccessPasses() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .portalSubmission(true)
              .hasPortalAgreement(true)
              .portalUserId("User1")
              .validSsoAccess(true)
              .hasIndrPermission(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertNoError(result, FordringErrorCode.SSO_ACCESS_INVALID.getCode());
    }

    @Test
    @DisplayName("Portal user with invalid SSO access is rejected with error 480")
    void portalUserWithInvalidSsoAccessIsRejected() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .portalSubmission(true)
              .hasPortalAgreement(true)
              .portalUserId("User2")
              .validSsoAccess(false)
              .hasIndrPermission(true)
              .build();

      FordringValidationResult result = fireRules(request);

      assertHasError(result, FordringErrorCode.SSO_ACCESS_INVALID);
      assertErrorMessageContains(
          result, 480, "Adgang nægtet. Ugyldig sagsbehandler- eller fordringshaver adgang");
    }
  }

  @Nested
  @DisplayName("Combined Authorization")
  class CombinedAuthorizationValidation {

    @Test
    @DisplayName("Fully authorized OPRETFORDRING passes all authorization checks")
    void fullyAuthorizedOpretFordringPasses() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .systemToSystem(true)
              .systemReporterId("SYS001")
              .systemReporterAuthorized(true)
              .fordringshaverKode("FH001")
              .artType("INDR")
              .hasIndrPermission(true)
              .build();

      FordringValidationResult result = fireRules(request);

      // Check no authorization errors
      assertNoError(result, FordringErrorCode.SYSTEM_REPORTER_UNAUTHORIZED.getCode());
      assertNoError(result, FordringErrorCode.INDR_PERMISSION_MISSING.getCode());
      assertNoError(result, FordringErrorCode.MODR_PERMISSION_MISSING.getCode());
      assertNoError(result, FordringErrorCode.PORTAL_AGREEMENT_MISSING.getCode());
      assertNoError(result, FordringErrorCode.SSO_ACCESS_INVALID.getCode());
    }

    @Test
    @DisplayName("Portal submission with all permissions passes all authorization checks")
    void fullyAuthorizedPortalSubmissionPasses() {
      FordringValidationRequest request =
          validRequestBuilder("OPRETFORDRING")
              .mfOpretFordringStrukturPresent(true)
              .systemToSystem(false)
              .portalSubmission(true)
              .hasPortalAgreement(true)
              .portalUserId("User1")
              .validSsoAccess(true)
              .fordringshaverKode("FH001")
              .artType("INDR")
              .hasIndrPermission(true)
              .build();

      FordringValidationResult result = fireRules(request);

      // Check no authorization errors
      assertNoError(result, FordringErrorCode.PORTAL_AGREEMENT_MISSING.getCode());
      assertNoError(result, FordringErrorCode.SSO_ACCESS_INVALID.getCode());
      assertNoError(result, FordringErrorCode.INDR_PERMISSION_MISSING.getCode());
    }
  }
}
