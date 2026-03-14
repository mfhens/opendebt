package dk.ufst.opendebt.rules.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.internal.io.ResourceFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import dk.ufst.opendebt.common.dto.fordring.FordringErrorCode;
import dk.ufst.opendebt.common.dto.fordring.FordringValidationError;
import dk.ufst.opendebt.common.dto.fordring.FordringValidationRequest;
import dk.ufst.opendebt.common.dto.fordring.FordringValidationResult;

/**
 * Base test class for fordring Drools rule evaluation.
 *
 * <p>Provides:
 *
 * <ul>
 *   <li>Shared KIE container initialized once per test class from classpath rules
 *   <li>Per-test KIE session with automatic disposal
 *   <li>Helper methods to build valid and invalid FordringValidationRequest objects
 *   <li>Assertion helpers for checking specific error codes in validation results
 * </ul>
 *
 * <p>Subclasses should focus on specific rule categories (structure, currency, dates, etc.).
 */
public abstract class AbstractFordringRuleTest {

  private static final String FORDRING_VALIDATION_AGENDA_GROUP = "fordring-validation";
  private static final String RESULT_GLOBAL = "validationResult";
  private static final String RULES_PATH = "rules/";

  private static KieContainer kieContainer;

  protected KieSession kieSession;
  protected FordringValidationResult result;

  @BeforeAll
  static void initKieContainer() throws IOException {
    KieServices kieServices = KieServices.Factory.get();
    KieFileSystem kieFileSystem = kieServices.newKieFileSystem();

    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    // Load only fordring DRL files to avoid global conflicts with existing rules
    Resource[] fordringDrlResources =
        resolver.getResources("classpath*:" + RULES_PATH + "fordring/**/*.drl");
    for (Resource resource : fordringDrlResources) {
      String path = resolveRulePath(resource);
      kieFileSystem.write(ResourceFactory.newClassPathResource(path, "UTF-8"));
    }

    KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
    kieBuilder.buildAll();

    assertThat(kieBuilder.getResults().getMessages())
        .as("Drools compilation should produce no errors")
        .filteredOn(m -> m.getLevel() == org.kie.api.builder.Message.Level.ERROR)
        .isEmpty();

    KieModule kieModule = kieBuilder.getKieModule();
    kieContainer = kieServices.newKieContainer(kieModule.getReleaseId());
  }

  @BeforeEach
  void setUpSession() {
    result = FordringValidationResult.builder().build();
    kieSession = kieContainer.newKieSession();
    kieSession.setGlobal(RESULT_GLOBAL, result);
  }

  @AfterEach
  void tearDownSession() {
    if (kieSession != null) {
      kieSession.dispose();
    }
  }

  // ===== Rule execution helpers =====

  /**
   * Fires all fordring validation rules against the given request.
   *
   * @param request the validation request
   * @return the validation result
   */
  protected FordringValidationResult fireRules(FordringValidationRequest request) {
    kieSession.insert(request);
    kieSession.getAgenda().getAgendaGroup(FORDRING_VALIDATION_AGENDA_GROUP).setFocus();
    kieSession.fireAllRules();
    return result;
  }

  // ===== Assertion helpers =====

  /** Asserts that the result contains the specified error code. */
  protected void assertHasError(FordringValidationResult result, int expectedErrorCode) {
    assertThat(result.isValid()).as("Result should be invalid").isFalse();
    assertThat(result.getErrors())
        .as("Result should contain error code %d", expectedErrorCode)
        .extracting(FordringValidationError::getErrorCode)
        .contains(expectedErrorCode);
  }

  /** Asserts that the result contains the specified FordringErrorCode. */
  protected void assertHasError(
      FordringValidationResult result, FordringErrorCode expectedErrorCode) {
    assertHasError(result, expectedErrorCode.getCode());
  }

  /** Asserts that the result does not contain the specified error code. */
  protected void assertNoError(FordringValidationResult result, int errorCode) {
    assertThat(result.getErrors())
        .as("Result should not contain error code %d", errorCode)
        .extracting(FordringValidationError::getErrorCode)
        .doesNotContain(errorCode);
  }

  /** Asserts that the result is valid (no errors). */
  protected void assertValid(FordringValidationResult result) {
    assertThat(result.isValid()).as("Result should be valid").isTrue();
    assertThat(result.getErrors()).as("Result should have no errors").isEmpty();
  }

  /** Asserts that the result contains exactly the specified error codes (in any order). */
  protected void assertExactErrors(FordringValidationResult result, int... expectedErrorCodes) {
    assertThat(result.isValid()).as("Result should be invalid").isFalse();
    List<Integer> actualCodes =
        result.getErrors().stream().map(FordringValidationError::getErrorCode).toList();
    assertThat(actualCodes)
        .as("Result should contain exactly the expected error codes")
        .containsExactlyInAnyOrder(
            java.util.Arrays.stream(expectedErrorCodes).boxed().toArray(Integer[]::new));
  }

  /** Asserts that an error message contains the expected substring. */
  protected void assertErrorMessageContains(
      FordringValidationResult result, int errorCode, String expectedSubstring) {
    FordringValidationError error =
        result.getErrors().stream()
            .filter(e -> e.getErrorCode() == errorCode)
            .findFirst()
            .orElseThrow(
                () ->
                    new AssertionError(
                        "Expected error code " + errorCode + " not found in result"));
    assertThat(error.getMessage())
        .as("Error message for code %d should contain '%s'", errorCode, expectedSubstring)
        .contains(expectedSubstring);
  }

  // ===== Request builder helpers =====

  /**
   * Creates a minimal valid OPRETFORDRING request that should pass all core validations.
   *
   * @return a valid FordringValidationRequest
   */
  protected FordringValidationRequest createValidOpretFordringRequest() {
    return FordringValidationRequest.builder()
        .aktionKode("OPRETFORDRING")
        .mfOpretFordringStrukturPresent(true)
        .valutaKode("DKK")
        .artType("INDR")
        .merRenteSats(BigDecimal.ZERO)
        .virkningsDato(LocalDate.now().minusDays(1))
        .modtagelsesTidspunkt(LocalDateTime.now())
        .virkningsDatoRequired(true)
        .periodeFra(LocalDate.of(2024, 1, 1))
        .periodeTil(LocalDate.of(2024, 6, 30))
        .fordringhaveraftaleId("FA001")
        .agreementFound(true)
        .dmiFordringTypeKode("HF01")
        .claimTypeAllowedByAgreement(true)
        .mfAftaleSystemIntegration(true)
        .systemToSystem(false)
        .debtorId("12345678")
        // Authorization permissions (petition016)
        .hasIndrPermission(true)
        .hasModrPermission(true)
        .portalSubmission(false)
        // Content validation defaults (petition018)
        .hovedfordringHasKategoriHf(true)
        .subclaimTypeAllowed(true)
        .fordringIdKnown(true)
        .build();
  }

  /**
   * Creates a minimal valid GENINDSENDFORDRING request.
   *
   * @return a valid FordringValidationRequest
   */
  protected FordringValidationRequest createValidGenindsendFordringRequest() {
    return FordringValidationRequest.builder()
        .aktionKode("GENINDSENDFORDRING")
        .mfGenindsendFordringStrukturPresent(true)
        .valutaKode("DKK")
        .artType("INDR")
        .merRenteSats(BigDecimal.ZERO)
        .virkningsDato(LocalDate.now().minusDays(1))
        .modtagelsesTidspunkt(LocalDateTime.now())
        .virkningsDatoRequired(true)
        .fordringhaveraftaleId("FA001")
        .agreementFound(true)
        .dmiFordringTypeKode("HF01")
        .claimTypeAllowedByAgreement(true)
        .mfAftaleSystemIntegration(true)
        .systemToSystem(false)
        .debtorId("12345678")
        // Authorization permissions (petition016)
        .hasIndrPermission(true)
        .hasModrPermission(true)
        .hasGenindsendPermission(true)
        .portalSubmission(false)
        // Lifecycle context (petition017)
        .originalClaimWithdrawn(true)
        .originalWithdrawalReason("HENS")
        .fordringshaverKode("FH001")
        .originalFordringshaverKode("FH001")
        .stamdataMatchesOriginal(true)
        .originalClaimIsModr(false)
        // Content validation defaults (petition018)
        .hovedfordringHasKategoriHf(true)
        .subclaimTypeAllowed(true)
        .fordringIdKnown(true)
        .build();
  }

  /**
   * Creates a request builder pre-populated with valid defaults, allowing tests to override only
   * the fields being tested.
   *
   * @param aktionKode the action type
   * @return a builder with valid defaults
   */
  protected FordringValidationRequest.FordringValidationRequestBuilder validRequestBuilder(
      String aktionKode) {
    return FordringValidationRequest.builder()
        .aktionKode(aktionKode)
        .valutaKode("DKK")
        .artType("INDR")
        .merRenteSats(BigDecimal.ZERO)
        .virkningsDato(LocalDate.now().minusDays(1))
        .modtagelsesTidspunkt(LocalDateTime.now())
        .virkningsDatoRequired(false)
        .fordringhaveraftaleId("FA001")
        .agreementFound(true)
        .dmiFordringTypeKode("HF01")
        .claimTypeAllowedByAgreement(true)
        .mfAftaleSystemIntegration(true)
        .systemToSystem(false)
        .debtorId("12345678")
        // Authorization permissions (petition016) - default to authorized
        .hasIndrPermission(true)
        .hasModrPermission(true)
        .hasNedskrivPermission(true)
        .hasTilbagekaldPermission(true)
        .hasGenindsendPermission(true)
        .hasHovedstolPermission(true)
        .hasOaniPermission(true)
        .hasOpskrivningReguleringPermission(true)
        .hasOonrPermission(true)
        .hasNaorPermission(true)
        .hasNaoiPermission(true)
        .portalSubmission(false)
        .systemReporterAuthorized(true)
        // Content validation defaults (petition018) - default to valid
        .hovedfordringHasKategoriHf(true)
        .subclaimTypeAllowed(true)
        .fordringIdKnown(true);
  }

  // ===== Internal helpers =====

  private static String resolveRulePath(Resource resource) throws IOException {
    String uri = resource.getURI().toString();
    int rulesIndex = uri.indexOf(RULES_PATH);
    if (rulesIndex >= 0) {
      return uri.substring(rulesIndex);
    }
    // Fallback: use filename in root rules/ directory
    return RULES_PATH + resource.getFilename();
  }
}
