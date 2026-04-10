package dk.ufst.opendebt.rules.interest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.io.IOException;
import java.math.BigDecimal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.internal.io.ResourceFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import dk.ufst.opendebt.rules.model.InterestCalculationRequest;
import dk.ufst.opendebt.rules.model.InterestCalculationResult;

/**
 * Integration tests for interest-calculation.drl (CR-001).
 *
 * <p>Uses a real KieContainer so the DRL actually fires. Each test covers one interest regime
 * branch, the salience guards (NOT_DUE, SMALL_AMOUNT), and the time-aware rate verbatim contract.
 *
 * <p>Legal basis: Gældsinddrivelsesloven § 5, stk. 1-2 (INDR_STD/EXEMPT), EU-toldkodeks art. 114
 * (INDR_TOLD/TOLD_AFD), Gældsinddrivelsesbekendtgørelsen § 9, stk. 3 (INDR_CONTRACT).
 */
class InterestCalculationRulesIntegrationTest {

  private static final String RESULT_GLOBAL = "result";
  private static final String RULES_PATH = "rules/";

  private static KieContainer kieContainer;

  private KieSession kieSession;
  private InterestCalculationResult result;

  @BeforeAll
  static void initKieContainer() throws IOException {
    KieServices kieServices = KieServices.Factory.get();
    KieFileSystem kieFileSystem = kieServices.newKieFileSystem();

    // Load only interest-calculation.drl to avoid global-variable conflicts with other DRL files.
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    Resource[] drlResources =
        resolver.getResources("classpath*:" + RULES_PATH + "interest-calculation.drl");

    for (Resource resource : drlResources) {
      kieFileSystem.write(
          ResourceFactory.newClassPathResource(RULES_PATH + resource.getFilename(), "UTF-8"));
    }

    KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
    kieBuilder.buildAll();

    assertThat(kieBuilder.getResults().getMessages())
        .as("interest-calculation.drl must compile without errors")
        .filteredOn(m -> m.getLevel() == Message.Level.ERROR)
        .isEmpty();

    KieModule kieModule = kieBuilder.getKieModule();
    kieContainer = kieServices.newKieContainer(kieModule.getReleaseId());
  }

  @BeforeEach
  void setUpSession() {
    result = InterestCalculationResult.builder().daysCalculated(0).build();
    kieSession = kieContainer.newKieSession();
    kieSession.setGlobal(RESULT_GLOBAL, result);
  }

  @AfterEach
  void tearDownSession() {
    if (kieSession != null) {
      kieSession.dispose();
    }
  }

  // =========================================================================
  // Helper
  // =========================================================================

  private InterestCalculationResult fireRules(InterestCalculationRequest request) {
    result = InterestCalculationResult.builder().daysCalculated(request.getDaysPastDue()).build();
    kieSession.setGlobal(RESULT_GLOBAL, result);
    kieSession.insert(request);
    kieSession.fireAllRules();
    return result;
  }

  // =========================================================================
  // Salience 200: NOT_DUE guard
  // =========================================================================

  @Test
  @DisplayName("Salience 200: daysPastDue=0 → interestAmount=0, rateType=NOT_DUE")
  void notDue_producesZeroInterest() {
    InterestCalculationRequest request =
        InterestCalculationRequest.builder()
            .interestRule("INDR_STD")
            .annualRate(new BigDecimal("0.0575"))
            .principalAmount(new BigDecimal("10000.00"))
            .daysPastDue(0)
            .build();

    InterestCalculationResult res = fireRules(request);

    assertThat(res.getInterestAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(res.getRateType()).isEqualTo("NOT_DUE");
  }

  // =========================================================================
  // Salience 100: INDR_EXEMPT guard
  // =========================================================================

  @Test
  @DisplayName("Salience 100: INDR_EXEMPT (straffebøder) → zero interest regardless of principal")
  void indrExempt_producesZeroInterest() {
    InterestCalculationRequest request =
        InterestCalculationRequest.builder()
            .interestRule("INDR_EXEMPT")
            .principalAmount(new BigDecimal("50000.00"))
            .daysPastDue(365)
            .build();

    InterestCalculationResult res = fireRules(request);

    assertThat(res.getInterestAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(res.getRateType()).isEqualTo("INDR_EXEMPT");
    assertThat(res.getLegalBasis())
        .isEqualTo("Gældsinddrivelsesloven § 5, stk. 1; Retsplejeloven § 997, stk. 3");
  }

  // =========================================================================
  // Salience 90: SMALL_AMOUNT guard
  // =========================================================================

  @Test
  @DisplayName("Salience 90: principalAmount < 100 → interestAmount=0, rateType=SMALL_AMOUNT")
  void smallAmount_producesZeroInterest() {
    InterestCalculationRequest request =
        InterestCalculationRequest.builder()
            .interestRule("INDR_STD")
            .annualRate(new BigDecimal("0.0575"))
            .principalAmount(new BigDecimal("99.99"))
            .daysPastDue(365)
            .build();

    InterestCalculationResult res = fireRules(request);

    assertThat(res.getInterestAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(res.getRateType()).isEqualTo("SMALL_AMOUNT");
  }

  // =========================================================================
  // Salience 50: INDR_STD
  // =========================================================================

  @Test
  @DisplayName("INDR_STD: 10000 × (0.0575/365) × 365 = 575.00")
  void indrStd_producesCorrectInterest() {
    InterestCalculationRequest request =
        InterestCalculationRequest.builder()
            .interestRule("INDR_STD")
            .annualRate(new BigDecimal("0.0575"))
            .principalAmount(new BigDecimal("10000.00"))
            .daysPastDue(365)
            .build();

    InterestCalculationResult res = fireRules(request);

    assertThat(res.getInterestAmount().doubleValue()).isCloseTo(575.00, within(0.01));
    assertThat(res.getRateType()).isEqualTo("INDR_STD");
    assertThat(res.getLegalBasis()).isEqualTo("Gældsinddrivelsesloven § 5, stk. 1-2");
  }

  // =========================================================================
  // Salience 50: INDR_TOLD
  // =========================================================================

  @Test
  @DisplayName("INDR_TOLD: 10000 × (0.0375/365) × 365 = 375.00")
  void indrTold_producesCorrectInterest() {
    InterestCalculationRequest request =
        InterestCalculationRequest.builder()
            .interestRule("INDR_TOLD")
            .annualRate(new BigDecimal("0.0375"))
            .principalAmount(new BigDecimal("10000.00"))
            .daysPastDue(365)
            .build();

    InterestCalculationResult res = fireRules(request);

    assertThat(res.getInterestAmount().doubleValue()).isCloseTo(375.00, within(0.01));
    assertThat(res.getRateType()).isEqualTo("INDR_TOLD");
    assertThat(res.getLegalBasis()).isEqualTo("EU-toldkodeks art. 114; Toldloven § 30a");
  }

  // =========================================================================
  // Salience 50: INDR_TOLD_AFD
  // =========================================================================

  @Test
  @DisplayName("INDR_TOLD_AFD: 10000 × (0.0275/365) × 365 = 275.00")
  void indrToldAfd_producesCorrectInterest() {
    InterestCalculationRequest request =
        InterestCalculationRequest.builder()
            .interestRule("INDR_TOLD_AFD")
            .annualRate(new BigDecimal("0.0275"))
            .principalAmount(new BigDecimal("10000.00"))
            .daysPastDue(365)
            .build();

    InterestCalculationResult res = fireRules(request);

    assertThat(res.getInterestAmount().doubleValue()).isCloseTo(275.00, within(0.01));
    assertThat(res.getRateType()).isEqualTo("INDR_TOLD_AFD");
    assertThat(res.getLegalBasis()).isEqualTo("EU-toldkodeks art. 114 (med afdragsordning)");
  }

  // =========================================================================
  // Salience 50: INDR_CONTRACT
  // =========================================================================

  @Test
  @DisplayName("INDR_CONTRACT: 10000 × (0.0800/365) × 365 = 800.00")
  void indrContract_producesCorrectInterest() {
    InterestCalculationRequest request =
        InterestCalculationRequest.builder()
            .interestRule("INDR_CONTRACT")
            .annualRate(new BigDecimal("0.0800"))
            .principalAmount(new BigDecimal("10000.00"))
            .daysPastDue(365)
            .build();

    InterestCalculationResult res = fireRules(request);

    assertThat(res.getInterestAmount().doubleValue()).isCloseTo(800.00, within(0.01));
    assertThat(res.getRateType()).isEqualTo("INDR_CONTRACT");
    assertThat(res.getLegalBasis()).isEqualTo("Gældsinddrivelsesbekendtgørelsen § 9, stk. 3");
  }

  // =========================================================================
  // Time-aware contract: DRL uses annualRate verbatim, no internal override
  // =========================================================================

  @Test
  @DisplayName("Historical rate 0.0775 produces 775.00 (not today's 575.00) — DRL rate-agnostic")
  void historicalRate_usedVerbatim() {
    InterestCalculationRequest request =
        InterestCalculationRequest.builder()
            .interestRule("INDR_STD")
            .annualRate(new BigDecimal("0.0775"))
            .principalAmount(new BigDecimal("10000.00"))
            .daysPastDue(365)
            .build();

    InterestCalculationResult res = fireRules(request);

    assertThat(res.getInterestAmount().doubleValue()).isCloseTo(775.00, within(0.01));
    assertThat(res.getRateType()).isEqualTo("INDR_STD");
  }

  @Test
  @DisplayName("Different annualRate values produce different amounts — no hardcoded override")
  void noHardcodedRate_differentAnnualRatesProduceDifferentResults() {
    InterestCalculationRequest req575 =
        InterestCalculationRequest.builder()
            .interestRule("INDR_STD")
            .annualRate(new BigDecimal("0.0575"))
            .principalAmount(new BigDecimal("10000.00"))
            .daysPastDue(365)
            .build();

    InterestCalculationRequest req775 =
        InterestCalculationRequest.builder()
            .interestRule("INDR_STD")
            .annualRate(new BigDecimal("0.0775"))
            .principalAmount(new BigDecimal("10000.00"))
            .daysPastDue(365)
            .build();

    BigDecimal amount575 = fireRules(req575).getInterestAmount();

    // New session for second firing to avoid fact contamination
    kieSession.dispose();
    result = InterestCalculationResult.builder().daysCalculated(365).build();
    kieSession = kieContainer.newKieSession();
    kieSession.setGlobal(RESULT_GLOBAL, result);

    BigDecimal amount775 = fireRules(req775).getInterestAmount();

    assertThat(amount575).isNotEqualByComparingTo(amount775);
    assertThat(amount575.doubleValue()).isCloseTo(575.00, within(0.01));
    assertThat(amount775.doubleValue()).isCloseTo(775.00, within(0.01));
  }
}
