package dk.ufst.opendebt.caseworker.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import dk.ufst.opendebt.common.timeline.TimelineVisibilityProperties;

@Configuration
@EnableConfigurationProperties(TimelineVisibilityProperties.class)
public class WebClientConfig {

  /**
   * Plain WebClient.Builder for dev/test profiles. OAuth2 autoconfiguration is disabled in these
   * profiles (see application-dev.yml), so no token relay is possible or needed.
   *
   * <p>In production the OAuth2-enabled builder is provided by {@link WebClientOAuth2Config}.
   */
  @Bean
  @Profile({"dev", "test"})
  public WebClient.Builder webClientBuilder() {
    return WebClient.builder().defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);
  }

  /**
   * Fixed thread pool for parallel BFF timeline aggregation fetches. Isolates blocking WebClient
   * calls from the common pool. Ref: specs §3.1.
   */
  @Bean(destroyMethod = "shutdown")
  public ExecutorService bffFetchExecutor() {
    return Executors.newFixedThreadPool(10);
  }
}
