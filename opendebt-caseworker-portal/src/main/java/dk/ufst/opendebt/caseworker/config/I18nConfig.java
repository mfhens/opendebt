package dk.ufst.opendebt.caseworker.config;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class I18nConfig implements WebMvcConfigurer {

  private final I18nProperties i18nProperties;

  @Bean
  public MessageSource messageSource() {
    var source = new ReloadableResourceBundleMessageSource();
    source.setBasenames("classpath:messages", "classpath:timeline-messages");
    source.setDefaultEncoding("UTF-8");
    source.setFallbackToSystemLocale(false);
    source.setDefaultLocale(Locale.forLanguageTag("da"));
    return source;
  }

  @Bean
  public LocaleResolver localeResolver() {
    var resolver = new CookieLocaleResolver("opendebt-lang");
    resolver.setDefaultLocale(i18nProperties.getDefaultLocaleAsLocale());
    resolver.setCookieMaxAge(java.time.Duration.ofDays(30));
    return resolver;
  }

  @Bean
  public LocaleChangeInterceptor localeChangeInterceptor() {
    var interceptor = new LocaleChangeInterceptor();
    interceptor.setParamName("lang");
    return interceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(localeChangeInterceptor());
  }
}
