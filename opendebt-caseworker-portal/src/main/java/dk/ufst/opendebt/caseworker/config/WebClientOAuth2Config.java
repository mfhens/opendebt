package dk.ufst.opendebt.caseworker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Production WebClient configuration that forwards the current user's OAuth2/OIDC bearer token to
 * all downstream service calls (TB-052).
 *
 * <p>Active when neither the {@code dev} nor {@code test} Spring profile is set. In those profiles,
 * OAuth2 autoconfiguration is disabled (see application-dev.yml) and {@link WebClientConfig}
 * provides a plain builder instead.
 *
 * <p>{@code setDefaultOAuth2AuthorizedClient(true)} ensures that every WebClient request
 * automatically attaches the token of the currently authenticated user, so {@code @PreAuthorize}
 * checks on the backend services receive a valid bearer token and do not return HTTP 403.
 */
@Configuration
@Profile("!dev & !test")
public class WebClientOAuth2Config {

  /**
   * Builds an {@link OAuth2AuthorizedClientManager} backed by the Spring Boot auto-configured
   * {@link ClientRegistrationRepository} and {@link OAuth2AuthorizedClientRepository}. Supports the
   * {@code authorization_code} grant used by the Keycloak registration in application.yml.
   */
  @Bean
  public OAuth2AuthorizedClientManager authorizedClientManager(
      ClientRegistrationRepository clientRegistrationRepository,
      OAuth2AuthorizedClientRepository authorizedClientRepository) {
    DefaultOAuth2AuthorizedClientManager manager =
        new DefaultOAuth2AuthorizedClientManager(
            clientRegistrationRepository, authorizedClientRepository);
    manager.setAuthorizedClientProvider(
        OAuth2AuthorizedClientProviderBuilder.builder().authorizationCode().refreshToken().build());
    return manager;
  }

  /**
   * OAuth2-aware WebClient.Builder shared by all BFF service clients. The {@link
   * ServletOAuth2AuthorizedClientExchangeFilterFunction} intercepts every outgoing request and
   * attaches the current user's bearer token as an {@code Authorization} header.
   *
   * <p>All client classes (CaseServiceClient, DebtServiceClient, PaymentServiceClient,
   * PersonRegistryClient, ConfigServiceClient) inject this builder and call {@code
   * .baseUrl(url).build()} to obtain their per-service WebClient instance, so token relay is
   * applied automatically to all backend calls without any changes to those classes.
   */
  @Bean
  public WebClient.Builder webClientBuilder(OAuth2AuthorizedClientManager authorizedClientManager) {
    ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2 =
        new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
    oauth2.setDefaultOAuth2AuthorizedClient(true);
    return WebClient.builder()
        .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        .apply(oauth2.oauth2Configuration());
  }
}
