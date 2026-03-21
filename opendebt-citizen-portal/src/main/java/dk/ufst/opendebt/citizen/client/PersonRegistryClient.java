package dk.ufst.opendebt.citizen.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;

/**
 * Client for person-registry service. Resolves CPR to person_id UUID. CPR is sent only to
 * person-registry and NEVER logged or stored locally.
 */
@Slf4j
@Component
public class PersonRegistryClient {

  private final WebClient webClient;

  public PersonRegistryClient(
      WebClient.Builder webClientBuilder,
      @Value("${opendebt.services.person-registry.url}") String baseUrl) {
    this.webClient = webClientBuilder.baseUrl(baseUrl).build();
  }

  /**
   * Calls person-registry POST /api/v1/persons/lookup to resolve CPR to person_id. Creates a new
   * person record if one does not already exist.
   *
   * @param cpr the CPR number (NEVER logged)
   * @return the resolved person_id UUID
   */
  @CircuitBreaker(name = "person-registry", fallbackMethod = "lookupOrCreatePersonFallback")
  public UUID lookupOrCreatePerson(String cpr) {
    log.debug("Resolving person_id via person-registry");

    PersonLookupResponse response =
        webClient
            .post()
            .uri("/api/v1/persons/lookup")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(new PersonLookupRequest(cpr, "CPR"))
            .retrieve()
            .bodyToMono(PersonLookupResponse.class)
            .block();

    if (response == null || response.personId() == null) {
      throw new IllegalStateException("Person registry returned null response");
    }

    log.debug("Resolved person_id={}", response.personId());
    return response.personId();
  }

  private UUID lookupOrCreatePersonFallback(String cpr, Throwable t) {
    if (t
            instanceof
            org.springframework.web.reactive.function.client.WebClientResponseException wcre
        && wcre.getStatusCode().is4xxClientError()) {
      throw wcre;
    }
    throw new IllegalStateException("Person registry unavailable: " + t.getMessage(), t);
  }

  record PersonLookupRequest(String identifier, String identifierType) {}

  record PersonLookupResponse(UUID personId, boolean created) {}
}
