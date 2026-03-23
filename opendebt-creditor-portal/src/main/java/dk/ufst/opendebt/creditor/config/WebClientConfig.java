package dk.ufst.opendebt.creditor.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import dk.ufst.opendebt.common.timeline.TimelineVisibilityProperties;

@Configuration
@EnableConfigurationProperties(TimelineVisibilityProperties.class)
public class WebClientConfig {

  @Bean
  public WebClient.Builder webClientBuilder() {
    return WebClient.builder().defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);
  }

  /**
   * Fixed thread pool for parallel BFF timeline aggregation fetches. Ref: petition050 specs §5.1.
   */
  @Bean(destroyMethod = "shutdown")
  public ExecutorService bffFetchExecutor() {
    return Executors.newFixedThreadPool(10);
  }
}
