Feature: Internationalization (i18n) for OpenDebt portals

  Scenario: Default language is Danish
    Given the creditor portal is configured with i18n support
    When a user opens the portal without specifying a language
    Then the portal renders with Danish as the active language
    And the HTML lang attribute is "da"

  Scenario: Switch to English
    Given the creditor portal is configured with i18n support
    When a user switches the language to "en-GB"
    Then the portal renders with English text
    And the HTML lang attribute is "en"

  Scenario: Language persists across navigation
    Given a user has selected "en-GB" as their language
    When the user navigates to another page
    Then the page is still rendered in English

  Scenario: Fallback to Danish for missing keys
    Given the creditor portal is configured with i18n support
    When a message key is missing from the English bundle
    Then the Danish translation is shown as fallback

  Scenario: Language selector lists configured languages
    Given the creditor portal is configured with supported locales "da-DK" and "en-GB"
    When a user views the language selector
    Then the selector displays "Dansk" and "English"
