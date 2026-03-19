package dk.ufst.opendebt.creditor.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * BDD step definitions for Petition 013: UI webtilgaengelighed compliance.
 *
 * <p>These tests verify that the portal's Thymeleaf templates satisfy WCAG 2.1 AA accessibility
 * requirements at the markup level: keyboard navigation structure, form error association, and
 * release readiness gates.
 */
public class Petition013Steps {

  private String layoutHtml;
  private String formFieldHtml;
  private String fordringFormHtml;
  private boolean releaseReady;
  private boolean accessibilityVerified;

  // Scenario 1: A core portal flow can be completed by keyboard only

  @Given("a user opens an OpenDebt portal")
  public void userOpensPortal() throws IOException {
    layoutHtml = loadTemplate("templates/layout/default.html");
    assertThat(layoutHtml).isNotEmpty();
  }

  @When("the user navigates a core flow using only the keyboard")
  public void userNavigatesByKeyboard() {
    // Keyboard navigation requires: skip link, focusable nav links, focusable main content,
    // proper tab order via semantic HTML. We verify the structural prerequisites.
    assertThat(layoutHtml).contains("skat-skip-link");
    assertThat(layoutHtml).contains("href=\"#main-content\"");
    assertThat(layoutHtml).contains("id=\"main-content\"");
  }

  @Then("the user can complete the flow without requiring a mouse")
  public void userCanCompleteFlowWithoutMouse() {
    // Verify all interactive elements are keyboard-accessible semantic elements
    assertThat(layoutHtml).as("Layout must have <nav> for keyboard navigation").contains("<nav");
    assertThat(layoutHtml).as("Layout must have <main> landmark").contains("<main");
    assertThat(layoutHtml)
        .as("Layout must have <a> links, not non-semantic click handlers")
        .contains("<a");
    // Verify a11y.js is loaded for HTMX focus management
    assertThat(layoutHtml).contains("a11y.js");
  }

  // Scenario 2: Form validation is accessible

  @Given("a user submits an invalid form in an OpenDebt UI")
  public void userSubmitsInvalidForm() throws IOException {
    formFieldHtml = loadTemplate("templates/fragments/form-field.html");
    fordringFormHtml = loadTemplate("templates/claims/create/step-details.html");
    assertThat(formFieldHtml).isNotEmpty();
    assertThat(fordringFormHtml).isNotEmpty();
  }

  @When("validation errors are shown")
  public void validationErrorsAreShown() {
    // Verify the form-field fragment has error display mechanism
    assertThat(formFieldHtml).contains("skat-error-message");
    assertThat(formFieldHtml).contains("role=\"alert\"");
  }

  @Then("the errors are programmatically associated with the relevant fields")
  public void errorsAreProgrammaticallyAssociated() {
    // Verify aria-describedby links input to error span
    assertThat(formFieldHtml).contains("aria-describedby");
    assertThat(formFieldHtml).contains("aria-invalid");
    // Verify the error span ID matches the describedby reference pattern
    assertThat(formFieldHtml).contains("${fieldId + '-error'}");
    assertThat(formFieldHtml).contains("th:aria-describedby=\"${fieldId + '-error'}\"");
  }

  @And("the user can perceive the errors without relying on color alone")
  public void errorsPerceivableWithoutColorAlone() {
    // Verify text-based error messages exist alongside any color indicators
    assertThat(formFieldHtml)
        .as("Error messages must have text content, not just color")
        .contains("skat-error-message");
    // The wizard step form has inline error messages with role="alert"
    assertThat(fordringFormHtml).contains("role=\"alert\"");
  }

  // Scenario 3: Accessibility is part of release readiness

  @Given("a UI change is ready for release")
  public void uiChangeReadyForRelease() {
    releaseReady = true;
  }

  @When("release readiness is evaluated")
  public void releaseReadinessEvaluated() {
    // Verify accessibility infrastructure is in place as part of release readiness:
    // 1. Layout has skip link and landmarks
    // 2. Form-field fragment enforces aria patterns
    // 3. a11y.js handles HTMX focus management
    // 4. Accessibility statement page exists
    accessibilityVerified =
        templateExists("templates/layout/default.html")
            && templateExists("templates/fragments/form-field.html")
            && templateExists("templates/was.html")
            && staticResourceExists("static/js/a11y.js");
  }

  @Then("accessibility verification is included in the release gate")
  public void accessibilityInReleaseGate() {
    assertThat(releaseReady).isTrue();
    assertThat(accessibilityVerified)
        .as(
            "All accessibility infrastructure must be present: layout with landmarks, "
                + "form-field fragment with aria, accessibility statement, and a11y.js")
        .isTrue();
  }

  private String loadTemplate(String path) throws IOException {
    try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
      if (is == null) {
        return "";
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private boolean templateExists(String path) {
    return getClass().getClassLoader().getResource(path) != null;
  }

  private boolean staticResourceExists(String path) {
    return getClass().getClassLoader().getResource(path) != null;
  }
}
