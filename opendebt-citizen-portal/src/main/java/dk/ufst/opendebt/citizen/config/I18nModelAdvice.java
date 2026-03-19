package dk.ufst.opendebt.citizen.config;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import lombok.RequiredArgsConstructor;

@ControllerAdvice
@RequiredArgsConstructor
public class I18nModelAdvice {

  private static final List<String> LANGUAGE_DISPLAY_ORDER = List.of("da", "en");

  private static final Map<String, String> LOCALE_NATIVE_NAMES =
      Map.of("da", "Dansk", "en", "English");

  private final I18nProperties i18nProperties;

  @ModelAttribute("supportedLocales")
  public List<String> supportedLocales() {
    return i18nProperties.getSupportedLocales();
  }

  @ModelAttribute("orderedSupportedLocales")
  public List<String> orderedSupportedLocales(Locale currentLocale) {
    String currentLanguage =
        currentLocale != null
            ? currentLocale.getLanguage()
            : i18nProperties.getDefaultLocaleAsLocale().getLanguage();

    return i18nProperties.getSupportedLocales().stream()
        .sorted(
            Comparator.comparing((String localeTag) -> !language(localeTag).equals(currentLanguage))
                .thenComparingInt(this::displayOrder))
        .toList();
  }

  @ModelAttribute("localeNativeNames")
  public Map<String, String> localeNativeNames() {
    return LOCALE_NATIVE_NAMES;
  }

  private int displayOrder(String localeTag) {
    int index = LANGUAGE_DISPLAY_ORDER.indexOf(language(localeTag));
    return index >= 0 ? index : Integer.MAX_VALUE;
  }

  private static String language(String localeTag) {
    return Locale.forLanguageTag(localeTag).getLanguage();
  }
}
