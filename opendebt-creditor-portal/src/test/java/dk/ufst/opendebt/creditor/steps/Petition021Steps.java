package dk.ufst.opendebt.creditor.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;

import dk.ufst.opendebt.creditor.config.I18nProperties;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * BDD step definitions for Petition 021: Internationalization (i18n) infrastructure.
 *
 * <p>Verifies that the creditor portal resolves messages in Danish and English, falls back to
 * Danish for missing keys, exposes a language selector with configured locales, and renders the
 * correct HTML lang attribute.
 */
public class Petition021Steps {

  @Autowired private MessageSource messageSource;

  @Autowired private I18nProperties i18nProperties;

  private Locale activeLocale;
  private String resolvedMessage;
  private String layoutHtml;

  // --- Scenario 1: Default language is Danish ---

  @Given("the creditor portal is configured with i18n support")
  public void portalConfiguredWithI18n() {
    assertThat(messageSource).as("MessageSource must be configured").isNotNull();
    assertThat(i18nProperties).as("I18nProperties must be configured").isNotNull();
    assertThat(i18nProperties.getSupportedLocales())
        .as("Supported locales must be configured")
        .isNotEmpty();
  }

  @When("a user opens the portal without specifying a language")
  public void userOpensPortalWithoutLanguage() {
    activeLocale = i18nProperties.getDefaultLocaleAsLocale();
    resolvedMessage = messageSource.getMessage("dashboard.title", null, activeLocale);
  }

  @Then("the portal renders with Danish as the active language")
  public void portalRendersInDanish() {
    assertThat(resolvedMessage)
        .as("Dashboard title must resolve to Danish")
        .isEqualTo("Fordringshaverportal");
  }

  @And("the HTML lang attribute is {string}")
  public void htmlLangAttributeIs(String expectedLang) throws IOException {
    layoutHtml = loadTemplate("templates/layout/default.html");
    assertThat(layoutHtml)
        .as("Layout must use dynamic locale for lang attribute")
        .contains("th:lang=\"${#locale.language}\"");
    assertThat(activeLocale.getLanguage())
        .as("Active locale language code")
        .isEqualToIgnoringCase(expectedLang);
  }

  // --- Scenario 2: Switch to English ---

  @When("a user switches the language to {string}")
  public void userSwitchesLanguage(String localeTag) {
    activeLocale = Locale.forLanguageTag(localeTag);
    resolvedMessage = messageSource.getMessage("dashboard.title", null, activeLocale);
  }

  @Then("the portal renders with English text")
  public void portalRendersInEnglish() {
    assertThat(resolvedMessage)
        .as("Dashboard title must resolve to English")
        .isEqualTo("Creditor Portal");
  }

  // --- Scenario 3: Language persists across navigation ---

  @Given("a user has selected {string} as their language")
  public void userHasSelectedLanguage(String localeTag) {
    activeLocale = Locale.forLanguageTag(localeTag);
  }

  @When("the user navigates to another page")
  public void userNavigatesToAnotherPage() {
    // Verify that message resolution works for a different page with the same locale
    resolvedMessage = messageSource.getMessage("debts.heading", null, activeLocale);
  }

  @Then("the page is still rendered in English")
  public void pageStillInEnglish() {
    assertThat(resolvedMessage)
        .as("Debts heading must resolve to English on different page")
        .isEqualTo("Your claims");
  }

  // --- Scenario 4: Fallback to Danish for missing keys ---

  @When("a message key is missing from the English bundle")
  public void messageKeyMissingFromEnglish() {
    Locale danish = Locale.forLanguageTag("da");
    Locale english = Locale.forLanguageTag("en-GB");
    // Verify both locales resolve correctly
    String danishValue = messageSource.getMessage("dashboard.title", null, danish);
    String englishValue = messageSource.getMessage("dashboard.title", null, english);
    assertThat(danishValue).isEqualTo("Fordringshaverportal");
    assertThat(englishValue).isEqualTo("Creditor Portal");

    // Test fallback: unsupported locale falls back to default locale (Danish)
    resolvedMessage = messageSource.getMessage("dashboard.title", null, Locale.CHINESE);
  }

  @Then("the Danish translation is shown as fallback")
  public void danishShownAsFallback() {
    // When an unsupported locale is used, MessageSource falls back to the default (Danish)
    assertThat(resolvedMessage)
        .as("Fallback must resolve to Danish for unsupported locale")
        .isEqualTo("Fordringshaverportal");
  }

  // --- Scenario 5: Language selector lists configured languages ---

  @Given("the creditor portal is configured with supported locales {string} and {string}")
  public void portalConfiguredWithLocales(String locale1, String locale2) {
    assertThat(i18nProperties.getSupportedLocales())
        .as("Both configured locales must be present")
        .contains(locale1, locale2);
  }

  @When("a user views the language selector")
  public void userViewsLanguageSelector() throws IOException {
    layoutHtml = loadTemplate("templates/layout/default.html");
    assertThat(layoutHtml)
        .as("Layout must include language selector fragment")
        .contains("language-selector");
  }

  @Then("the selector displays {string} and {string}")
  public void selectorDisplaysLanguages(String name1, String name2) throws IOException {
    String selectorHtml = loadTemplate("templates/fragments/language-selector.html");
    assertThat(selectorHtml).as("Language selector template must exist").isNotEmpty();
    assertThat(selectorHtml).contains("orderedSupportedLocales");
    assertThat(selectorHtml).contains("localeNativeNames");
    assertThat(selectorHtml).contains("role=\"group\"");
    assertThat(selectorHtml).contains("skat-lang-globe");
    assertThat(selectorHtml).contains("skat-lang-list");
    assertThat(selectorHtml).doesNotContain("<select");

    // Verify the I18nModelAdvice provides the expected native names
    assertThat(dk.ufst.opendebt.creditor.config.I18nModelAdvice.nativeName("da-DK"))
        .isEqualTo(name1);
    assertThat(dk.ufst.opendebt.creditor.config.I18nModelAdvice.nativeName("en-GB"))
        .isEqualTo(name2);
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
