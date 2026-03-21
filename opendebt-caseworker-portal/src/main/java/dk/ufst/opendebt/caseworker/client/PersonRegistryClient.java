package dk.ufst.opendebt.caseworker.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;

/**
 * BFF client for person-registry service. Provides debtor display name lookup for the caseworker
 * portal. Returns initials or pseudonym to avoid exposing PII in the portal UI.
 *
 * <p>AIDEV-TODO: Implement actual person-registry call when the display-name endpoint is available.
 * Currently returns a placeholder based on the person ID.
 */
@Slf4j
@Component
public class PersonRegistryClient {

  private final WebClient webClient;

  public PersonRegistryClient(
      WebClient.Builder webClientBuilder,
      @Value("${opendebt.services.person-registry.url:http://localhost:8083}") String baseUrl) {
    this.webClient = webClientBuilder.baseUrl(baseUrl).build();
  }

  /**
   * Returns a display-safe name for a person (initials or pseudonym). Does not return full PII.
   *
   * <p>AIDEV-TODO: Replace stub with actual person-registry call for display name.
   *
   * @param personId the person UUID
   * @return a display-safe name string
   */
  @CircuitBreaker(name = "person-registry", fallbackMethod = "getDisplayNameFallback")
  public String getDisplayName(UUID personId) {
    if (personId == null) {
      return "—";
    }
    log.debug("Getting display name for person: {}", personId);

    try {
      // AIDEV-TODO: Replace with actual person-registry call
      // Stub: return abbreviated UUID as placeholder
      String shortId = personId.toString().substring(0, 8);
      return "Person-" + shortId;
    } catch (Exception ex) {
      log.warn("Person registry unavailable for display name lookup: {}", ex.getMessage());
      return "—";
    }
  }

  private String getDisplayNameFallback(UUID personId, Throwable t) {
    log.warn("Person registry circuit breaker open: {}", t.getMessage());
    return "—";
  }
}
