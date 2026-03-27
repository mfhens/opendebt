package dk.ufst.opendebt.caseworker.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/** BFF client for person-registry service. */
@Slf4j
@Component
public class PersonRegistryClient {

  private final WebClient webClient;

  public PersonRegistryClient(
      WebClient.Builder webClientBuilder,
      @Value("${opendebt.services.person-registry.url:http://localhost:8083}") String baseUrl) {
    this.webClient = webClientBuilder.baseUrl(baseUrl).build();
  }

  @CircuitBreaker(name = "person-registry", fallbackMethod = "getDisplayNameFallback")
  public String getDisplayName(UUID personId) {
    if (personId == null) {
      return "—";
    }
    log.debug("Fetching display name for person: {}", personId);

    PersonFetchResponse fetched =
        webClient
            .get()
            .uri("/person-registry/api/v1/persons/{id}", personId)
            .retrieve()
            .onStatus(
                status -> status.value() == 404 || status.value() == 410,
                response -> response.releaseBody().then(Mono.<Throwable>empty()))
            .bodyToMono(PersonFetchResponse.class)
            .block();

    if (fetched == null || fetched.name() == null || fetched.name().isBlank()) {
      return "—";
    }
    return fetched.name();
  }

  private String getDisplayNameFallback(UUID personId, Throwable t) {
    log.warn(
        "Person registry unavailable for display name (personId={}): {}", personId, t.getMessage());
    return "—";
  }

  private record PersonFetchResponse(UUID id, String name) {}
}
