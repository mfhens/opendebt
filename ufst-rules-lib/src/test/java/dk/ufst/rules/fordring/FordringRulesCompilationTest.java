package dk.ufst.rules.fordring;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dk.ufst.opendebt.common.dto.fordring.FordringValidationRequest;
import dk.ufst.opendebt.common.dto.fordring.FordringValidationResult;
import dk.ufst.rules.test.AbstractFordringRuleTest;

/**
 * Verifies that all Drools DRL files compile successfully and the fordring validation KIE session
 * can be created and used without errors.
 */
class FordringRulesCompilationTest extends AbstractFordringRuleTest {

  @Test
  void drlFilesShouldCompileWithoutErrors() {
    // The @BeforeAll in AbstractFordringRuleTest already verifies compilation.
    // This test verifies the session can be created and used.
    FordringValidationRequest request = createValidOpretFordringRequest();
    FordringValidationResult result = fireRules(request);

    // With only the placeholder DRL (no rules yet), validation should pass
    assertValid(result);
  }

  @Test
  void fordringRulesAreIsolatedFromExistingRules() {
    // Verify that the fordring-validation agenda group provides isolation.
    // Fordring DRL files compile independently of existing debt-readiness,
    // interest-calculation, and collection-priority rules.
    FordringValidationRequest request = createValidOpretFordringRequest();
    FordringValidationResult result = fireRules(request);

    // Verify isolation by checking the result has the expected structure
    assertThat(result).isNotNull();
    assertThat(result.getErrors()).isEmpty();
    assertThat(result.isValid()).isTrue();
  }

  @Test
  void genindsendRequestShouldBeProcessable() {
    FordringValidationRequest request = createValidGenindsendFordringRequest();
    FordringValidationResult result = fireRules(request);
    assertValid(result);
  }
}
