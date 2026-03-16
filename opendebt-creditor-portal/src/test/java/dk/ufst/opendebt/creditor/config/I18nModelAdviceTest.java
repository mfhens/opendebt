package dk.ufst.opendebt.creditor.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class I18nModelAdviceTest {

  @Test
  void orderedSupportedLocales_placesActiveLanguageFirst_thenUsesSpecOrder() {
    I18nProperties properties = new I18nProperties();
    properties.setSupportedLocales(List.of("uk-UA", "en-GB", "da-DK", "ro-RO"));

    I18nModelAdvice advice = new I18nModelAdvice(properties);

    assertThat(advice.orderedSupportedLocales(Locale.forLanguageTag("ro-RO")))
        .containsExactly("ro-RO", "da-DK", "en-GB", "uk-UA");
  }

  @Test
  void nativeName_returnsNativeDisplayNames_fromLanguageSelectorSpec() {
    assertThat(I18nModelAdvice.nativeName("da-DK")).isEqualTo("Dansk");
    assertThat(I18nModelAdvice.nativeName("en-GB")).isEqualTo("English");
    assertThat(I18nModelAdvice.nativeName("de-DE")).isEqualTo("Deutsch");
    assertThat(I18nModelAdvice.nativeName("pl-PL")).isEqualTo("Polski");
    assertThat(I18nModelAdvice.nativeName("lt-LT")).isEqualTo("Lietuvių");
    assertThat(I18nModelAdvice.nativeName("ro-RO")).isEqualTo("Română");
    assertThat(I18nModelAdvice.nativeName("uk-UA")).isEqualTo("Українська");
  }
}
