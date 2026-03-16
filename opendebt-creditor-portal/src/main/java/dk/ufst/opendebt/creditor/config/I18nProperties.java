package dk.ufst.opendebt.creditor.config;

import java.util.List;
import java.util.Locale;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "opendebt.i18n")
public class I18nProperties {

  private String defaultLocale = "da-DK";

  private List<String> supportedLocales = List.of("da-DK", "en-GB");

  public Locale getDefaultLocaleAsLocale() {
    return Locale.forLanguageTag(defaultLocale);
  }
}
