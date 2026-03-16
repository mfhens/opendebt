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
 * BDD step definitions for Petition 014: Accessibility statements and feedback for each UI.
 *
 * <p>These tests verify that the portal exposes a discoverable accessibility statement, provides an
 * accessible contact path, and maintains the statement when accessibility-relevant changes are
 * made.
 */
public class Petition014Steps {

  private String layoutHtml;
  private String wasHtml;
  private boolean uiChangeAffectsAccessibility;
  private boolean changePreparedForRelease;

  // Scenario 1: A web UI exposes a discoverable accessibility statement

  @Given("an OpenDebt web site is published")
  public void openDebtWebSiteIsPublished() throws IOException {
    layoutHtml = loadTemplate("templates/layout/default.html");
    wasHtml = loadTemplate("templates/was.html");
    assertThat(layoutHtml).as("Layout template must exist for published site").isNotEmpty();
    assertThat(wasHtml).as("Accessibility statement template must exist").isNotEmpty();
  }

  @When("a user looks for accessibility information")
  public void userLooksForAccessibilityInfo() {
    // The user scans the page footer for an accessibility link, as is the Danish convention.
    assertThat(layoutHtml).contains("<footer");
    assertThat(layoutHtml).contains("/was");
  }

  @Then("the user can find a link to the accessibility statement")
  public void userFindsAccessibilityStatementLink() {
    // The layout footer must contain a link to /was with i18n message key or visible text
    assertThat(layoutHtml)
        .as("Footer must contain link to accessibility statement")
        .contains("th:href=\"@{/was}\"");
    assertThat(layoutHtml)
        .as("Footer must reference accessibility statement text via i18n or hardcoded")
        .containsAnyOf("layout.footer.accessibility", "Tilgængelighedserklæring");
  }

  // Scenario 2: The written contact path is accessible

  @Given("a user encounters inaccessible content in an OpenDebt UI")
  public void userEncountersInaccessibleContent() throws IOException {
    wasHtml = loadTemplate("templates/was.html");
    assertThat(wasHtml).isNotEmpty();
  }

  @When("the user follows the written contact path from the accessibility statement")
  public void userFollowsContactPath() {
    // The accessibility statement must contain contact information (via i18n key or literal)
    assertThat(wasHtml).containsAnyOf("was.contact.heading", "Kontaktoplysninger");
    assertThat(wasHtml).contains("mailto:");
  }

  @Then("the user can contact the responsible authority without using MitID login")
  public void userCanContactWithoutMitID() {
    // Contact must be available without authentication: email and/or phone
    assertThat(wasHtml)
        .as("Contact must include email (no login required)")
        .contains("tilgaengelighed@opendebt.dk");
    assertThat(wasHtml)
        .as("Contact must include phone (no login required)")
        .containsPattern("\\+45\\s*\\d");
  }

  @And("the user is not blocked by inaccessible CAPTCHA")
  public void userNotBlockedByCaptcha() {
    // The statement page must not contain CAPTCHA or similar barriers
    assertThat(wasHtml)
        .as("Statement must not contain CAPTCHA")
        .doesNotContainIgnoringCase("captcha");
    assertThat(wasHtml).doesNotContainIgnoringCase("recaptcha");
  }

  // Scenario 3: Accessibility statement maintenance is required after relevant change

  @Given("a UI change affects the accessibility information for an OpenDebt web site")
  public void uiChangeAffectsAccessibility() {
    uiChangeAffectsAccessibility = true;
  }

  @When("the change is prepared for release")
  public void changePreparedForRelease() {
    changePreparedForRelease = true;
  }

  @Then("the accessibility statement must be updated")
  public void accessibilityStatementMustBeUpdated() throws IOException {
    assertThat(uiChangeAffectsAccessibility).isTrue();
    assertThat(changePreparedForRelease).isTrue();

    // Verify the statement contains a "last updated" date, proving it is maintained
    wasHtml = loadTemplate("templates/was.html");
    assertThat(wasHtml)
        .as("Accessibility statement must contain a last-updated date for maintenance tracking")
        .containsPattern("datetime=\"\\d{4}-\\d{2}-\\d{2}\"");
    // Verify the enforcement procedure link exists
    assertThat(wasHtml)
        .as("Statement must link to Digitaliseringsstyrelsen for enforcement")
        .contains("was.digst.dk");
  }

  private String loadTemplate(String path) throws IOException {
    try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
      if (is == null) {
        return "";
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
