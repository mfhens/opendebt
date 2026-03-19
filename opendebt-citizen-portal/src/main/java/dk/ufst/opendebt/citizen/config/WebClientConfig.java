package dk.ufst.opendebt.citizen.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/** WebClient configuration using injected Builder for trace propagation (ADR-0024). */
@Configuration
public class WebClientConfig {

  @Bean
  public WebClient.Builder webClientBuilder() {
    return WebClient.builder().defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);
  }
}
