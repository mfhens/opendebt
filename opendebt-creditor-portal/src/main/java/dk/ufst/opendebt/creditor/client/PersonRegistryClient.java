package dk.ufst.opendebt.creditor.client;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import dk.ufst.opendebt.creditor.dto.DebtorVerificationResultDto;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * BFF client for person-registry service. Handles debtor verification for the claim creation wizard
 * (Step 1).
 */
@Slf4j
@Component
public class PersonRegistryClient {

  private static final String ERR_PERSON_REGISTRY_UNAVAILABLE =
      "Person registry is unavailable. Please try again later.";

  private final WebClient webClient;

  public PersonRegistryClient(
      WebClient.Builder webClientBuilder,
      @Value("${opendebt.services.person-registry.url:http://localhost:8083}") String baseUrl) {
    this.webClient = webClientBuilder.baseUrl(baseUrl).build();
  }

  /**
   * Verifies a CPR debtor by matching first and last names against the person-registry.
   *
   * @param cprNumber the CPR number to verify
   * @param firstName the first name to match
   * @param lastName the last name to match
   * @return verification result with matched person data or error
   */
  @CircuitBreaker(name = "person-registry", fallbackMethod = "verifyCprFallback")
  @Retry(name = "person-registry")
  public DebtorVerificationResultDto verifyCpr(
      String cprNumber, String firstName, String lastName) {
    log.debug("Verifying CPR debtor: {}", maskIdentifier(cprNumber));

    // Ref: tb021 §4.2 step 1 — POST /persons/lookup
    PersonLookupResponse lookupResp =
        webClient
            .post()
            .uri("/person-registry/api/v1/persons/lookup")
            .bodyValue(new PersonLookupRequest(cprNumber, "CPR", "PERSONAL"))
            .retrieve()
            .bodyToMono(PersonLookupResponse.class)
            .block();

    // Ref: tb021 §4.2 step 2 — branch on created
    if (lookupResp == null) {
      log.warn("Person registry returned empty body for CPR lookup");
      return DebtorVerificationResultDto.builder()
          .verified(false)
          .errorMessage(ERR_PERSON_REGISTRY_UNAVAILABLE)
          .build();
    }
    if (lookupResp.created()) {
      return DebtorVerificationResultDto.builder()
          .verified(true)
          .personId(lookupResp.personId())
          .displayName(firstName + " " + lastName)
          .build();
    }

    // Ref: tb021 §4.2 step 2a — GET /persons/{personId}, 404/410 → null
    log.debug("Fetching stored name for person: {}", lookupResp.personId());
    PersonFetchResponse fetched =
        webClient
            .get()
            .uri("/person-registry/api/v1/persons/{id}", lookupResp.personId())
            .retrieve()
            .onStatus(
                status -> status.value() == 404 || status.value() == 410,
                response -> response.releaseBody().then(Mono.<Throwable>empty()))
            .bodyToMono(PersonFetchResponse.class)
            .block();

    // Ref: tb021 §4.2 step 2b — null-name guard and name comparison
    if (fetched == null
        || fetched.name() == null
        || !namesMatch(fetched.name(), firstName, lastName)) {
      return DebtorVerificationResultDto.builder()
          .verified(false)
          .errorMessage("Navn matcher ikke")
          .build();
    }

    return DebtorVerificationResultDto.builder()
        .verified(true)
        .personId(lookupResp.personId())
        .displayName(fetched.name())
        .build();
  }

  /**
   * Verifies a CVR debtor against the person-registry.
   *
   * @param cvrNumber the CVR number to verify
   * @return verification result with company information or error
   */
  @CircuitBreaker(name = "person-registry", fallbackMethod = "verifyCvrFallback")
  @Retry(name = "person-registry")
  public DebtorVerificationResultDto verifyCvr(String cvrNumber) {
    log.debug("Verifying CVR debtor: {}", cvrNumber);

    // Ref: tb021 §4.3 — POST /organizations/lookup
    OrganizationLookupResponse resp =
        webClient
            .post()
            .uri("/person-registry/api/v1/organizations/lookup")
            .bodyValue(new OrganizationLookupRequest(cvrNumber))
            .retrieve()
            .bodyToMono(OrganizationLookupResponse.class)
            .block();

    return DebtorVerificationResultDto.builder()
        .verified(true)
        .personId(resp != null ? resp.organizationId() : null)
        .displayName("CVR: " + cvrNumber)
        .build();
  }

  /**
   * Verifies an SE debtor against the person-registry.
   *
   * @param seNumber the SE number to verify
   * @return verification result with company information or error
   */
  @CircuitBreaker(name = "person-registry", fallbackMethod = "verifySeeFallback")
  @Retry(name = "person-registry")
  public DebtorVerificationResultDto verifySe(String seNumber) {
    log.debug("Verifying SE debtor: {}", seNumber);

    // Ref: tb021 §4.4 — POST /organizations/lookup (SE passed as "cvr" field)
    OrganizationLookupResponse resp =
        webClient
            .post()
            .uri("/person-registry/api/v1/organizations/lookup")
            .bodyValue(new OrganizationLookupRequest(seNumber))
            .retrieve()
            .bodyToMono(OrganizationLookupResponse.class)
            .block();

    return DebtorVerificationResultDto.builder()
        .verified(true)
        .personId(resp != null ? resp.organizationId() : null)
        .displayName("SE: " + seNumber)
        .build();
  }

  /** Masks an identifier for log output to avoid logging PII. */
  private String maskIdentifier(String identifier) {
    if (identifier == null || identifier.length() < 4) {
      return "****";
    }
    return "****" + identifier.substring(identifier.length() - 4);
  }

  // Ref: tb021 §3 — name comparison algorithm
  private static String normalize(String s) {
    return Normalizer.normalize(s, Normalizer.Form.NFD)
        .replaceAll("\\p{InCombiningDiacriticalMarks}", "")
        .toLowerCase(Locale.ROOT)
        .strip();
  }

  private static boolean namesMatch(String stored, String firstName, String lastName) {
    return normalize(stored).equals(normalize(firstName + " " + lastName));
  }

  private DebtorVerificationResultDto verifyCprFallback(
      String cprNumber, String firstName, String lastName, Throwable t) {
    if (t
            instanceof
            org.springframework.web.reactive.function.client.WebClientResponseException wcre
        && wcre.getStatusCode().is4xxClientError()) {
      throw wcre;
    }
    log.warn("Circuit breaker fallback triggered for verifyCpr: {}", t.getMessage());
    return DebtorVerificationResultDto.builder()
        .verified(false)
        .errorMessage(ERR_PERSON_REGISTRY_UNAVAILABLE)
        .build();
  }

  private DebtorVerificationResultDto verifyCvrFallback(String cvrNumber, Throwable t) {
    if (t
            instanceof
            org.springframework.web.reactive.function.client.WebClientResponseException wcre
        && wcre.getStatusCode().is4xxClientError()) {
      throw wcre;
    }
    log.warn("Circuit breaker fallback triggered for verifyCvr: {}", t.getMessage());
    return DebtorVerificationResultDto.builder()
        .verified(false)
        .errorMessage(ERR_PERSON_REGISTRY_UNAVAILABLE)
        .build();
  }

  private DebtorVerificationResultDto verifySeeFallback(String seNumber, Throwable t) {
    if (t
            instanceof
            org.springframework.web.reactive.function.client.WebClientResponseException wcre
        && wcre.getStatusCode().is4xxClientError()) {
      throw wcre;
    }
    log.warn("Circuit breaker fallback triggered for verifySe: {}", t.getMessage());
    return DebtorVerificationResultDto.builder()
        .verified(false)
        .errorMessage(ERR_PERSON_REGISTRY_UNAVAILABLE)
        .build();
  }

  // Ref: tb021 §4.1 — inner DTOs (package-private static records)
  private record PersonLookupRequest(String identifier, String identifierType, String role) {}

  private record PersonLookupResponse(UUID personId, boolean created, String role) {}

  private record PersonFetchResponse(UUID id, String name) {}

  private record OrganizationLookupRequest(String cvr) {}

  private record OrganizationLookupResponse(UUID organizationId) {}
}
