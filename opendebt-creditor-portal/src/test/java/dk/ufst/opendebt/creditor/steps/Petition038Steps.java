package dk.ufst.opendebt.creditor.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.core.io.Resource;

import dk.ufst.opendebt.creditor.config.PortalLinksProperties;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/** BDD step definitions for petition 038 — Dashboard, Navigation, and Settings. */
public class Petition038Steps {

  @Autowired private MessageSource messageSource;

  @Autowired private PortalLinksProperties portalLinksProperties;

  @Value("classpath:templates/index.html")
  private Resource indexTemplate;

  @Value("classpath:templates/fragments/portal-nav.html")
  private Resource portalNavTemplate;

  @Value("classpath:templates/indstillinger.html")
  private Resource settingsTemplate;

  @Value("classpath:templates/vaelg-fordringshaver.html")
  private Resource selectionTemplate;

  @Value("classpath:templates/fragments/claim-counts.html")
  private Resource claimCountsTemplate;

  private String indexHtml;
  private String navHtml;
  private String settingsHtml;
  private String selectionHtml;

  @Given("the creditor portal is running")
  public void theCreditorPortalIsRunning() {
    assertThat(messageSource).isNotNull();
  }

  @Given("a creditor user with acting creditor context")
  public void aCreditorUserWithActingCreditorContext() throws IOException {
    indexHtml = readResource(indexTemplate);
    navHtml = readResource(portalNavTemplate);
    assertThat(indexHtml).isNotBlank();
  }

  @When("the user opens the dashboard homepage")
  public void theUserOpensTheDashboardHomepage() {
    // Template has already been loaded in the Given step
    assertThat(indexHtml).contains("layout:fragment=\"content\"");
  }

  @Then("the page contains an HTMX-loadable claim counts section")
  public void thePageContainsAnHtmxLoadableClaimCountsSection() {
    assertThat(indexHtml).contains("hx-get");
    assertThat(indexHtml).contains("claim-counts");
  }

  @And("the claim counts section references the claims count endpoint")
  public void theClaimCountsSectionReferencesTheClaimsCountEndpoint() {
    assertThat(indexHtml).contains("/api/claim-counts");
  }

  @Then("the creditor profile card is displayed")
  public void theCreditorProfileCardIsDisplayed() {
    assertThat(indexHtml).contains("dashboard.profile.heading");
  }

  @Then("a secondary navigation element is present with an aria-label")
  public void aSecondaryNavigationElementIsPresentWithAnAriaLabel() {
    assertThat(navHtml).contains("<nav");
    assertThat(navHtml).contains("aria-label");
    assertThat(navHtml).contains("nav.portal.label");
  }

  @And("the navigation contains links to portal sections")
  public void theNavigationContainsLinksToPortalSections() {
    assertThat(navHtml).contains("/fordringer");
    assertThat(navHtml).contains("/indstillinger");
    assertThat(navHtml).contains("/underretninger");
    assertThat(navHtml).contains("/rapporter");
    assertThat(navHtml).contains("/afstemning");
  }

  @Given("the portal is configured with external link URLs")
  public void thePortalIsConfiguredWithExternalLinkUrls() throws IOException {
    navHtml = readResource(portalNavTemplate);
    assertThat(portalLinksProperties).isNotNull();
    assertThat(portalLinksProperties.getAgreementMaterial()).isNotBlank();
    assertThat(portalLinksProperties.getContact()).isNotBlank();
    assertThat(portalLinksProperties.getGuides()).isNotBlank();
  }

  @When("the user views the navigation")
  public void theUserViewsTheNavigation() {
    assertThat(navHtml).isNotBlank();
  }

  @Then("external links open in a new tab")
  public void externalLinksOpenInANewTab() {
    assertThat(navHtml).contains("target=\"_blank\"");
    assertThat(navHtml).contains("rel=\"noopener noreferrer\"");
  }

  @And("external link URLs come from application configuration")
  public void externalLinkUrlsComeFromApplicationConfiguration() {
    assertThat(navHtml).contains("${portalLinks.agreementMaterial}");
    assertThat(navHtml).contains("${portalLinks.contact}");
    assertThat(navHtml).contains("${portalLinks.guides}");
  }

  @Then("all navigation labels are rendered from i18n message bundles")
  public void allNavigationLabelsAreRenderedFromI18nMessageBundles() {
    // Verify the nav fragment uses th:text with message keys
    assertThat(navHtml).contains("#{nav.home}");
    assertThat(navHtml).contains("#{nav.claims.recovery}");
    assertThat(navHtml).contains("#{nav.settings}");

    // Verify the message keys resolve in both Danish and English
    assertThat(messageSource.getMessage("nav.home", null, Locale.forLanguageTag("da")))
        .isEqualTo("Forside");
    assertThat(messageSource.getMessage("nav.home", null, Locale.forLanguageTag("en-GB")))
        .isEqualTo("Home");
  }

  @When("the user navigates to the settings page")
  public void theUserNavigatesToTheSettingsPage() throws IOException {
    settingsHtml = readResource(settingsTemplate);
    assertThat(settingsHtml).isNotBlank();
  }

  @Then("the settings page displays agreement configuration details")
  public void theSettingsPageDisplaysAgreementConfigurationDetails() {
    assertThat(settingsHtml).contains("settings.agreement.heading");
    assertThat(settingsHtml).contains("settings.agreement.portalActions");
    assertThat(settingsHtml).contains("settings.agreement.claimTypes");
  }

  @And("a contact email management section is shown")
  public void aContactEmailManagementSectionIsShown() {
    assertThat(settingsHtml).contains("settings.contact.heading");
    assertThat(settingsHtml).contains("contactEmail");
    assertThat(settingsHtml).contains("settings.contact.email.save");
  }

  @Given("an umbrella-user is authenticated")
  public void anUmbrellaUserIsAuthenticated() throws IOException {
    selectionHtml = readResource(selectionTemplate);
    assertThat(selectionHtml).isNotBlank();
  }

  @When("the umbrella-user opens the claimant selection page")
  public void theUmbrellaUserOpensTheClaimantSelectionPage() {
    assertThat(selectionHtml).contains("layout:fragment=\"content\"");
  }

  @Then("a list of creditors is displayed for selection")
  public void aListOfCreditorsIsDisplayedForSelection() {
    assertThat(selectionHtml).contains("th:each=\"creditor : ${creditors}\"");
    assertThat(selectionHtml).contains("selection.select.button");
  }

  @Then("a system status indicator is visible in the navigation area")
  public void aSystemStatusIndicatorIsVisibleInTheNavigationArea() {
    assertThat(navHtml).contains("skat-portal-nav__status");
    assertThat(navHtml).contains("nav.status.operational");
    assertThat(navHtml).contains("role=\"status\"");
  }

  private String readResource(Resource resource) throws IOException {
    return Files.readString(Path.of(resource.getURI()), StandardCharsets.UTF_8);
  }
}
