package dk.ufst.opendebt.creditor.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import dk.ufst.opendebt.creditor.dto.DebtorVerificationResultDto;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;

/**
 * BFF client for person-registry service. Handles debtor verification for the claim creation wizard
 * (Step 1).
 *
 * <p>AIDEV-TODO: Implement actual person-registry calls when petition023 (CPR lookup API) is
 * available. Currently returns stubbed verification results.
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
   * <p>AIDEV-TODO: Replace stub with actual person-registry call (petition023). The real
   * implementation should: call GET /person-registry/api/v1/persons/verify-cpr, compare names
   * case-insensitively and accent-stripped, and enforce throttling per user per birth date.
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
    log.debug("Verifying CPR debtor (stubbed): {}", maskIdentifier(cprNumber));

    try {
      // AIDEV-TODO: Replace with actual person-registry call
      // Stub: simulate successful verification for demo purposes
      return DebtorVerificationResultDto.builder()
          .verified(true)
          .displayName(firstName + " " + lastName)
          .personId(UUID.randomUUID())
          .build();
    } catch (Exception ex) {
      log.warn("Person registry unavailable for CPR verification: {}", ex.getMessage());
      return DebtorVerificationResultDto.builder()
          .verified(false)
          .errorMessage(ERR_PERSON_REGISTRY_UNAVAILABLE)
          .build();
    }
  }

  /**
   * Verifies a CVR debtor against the person-registry or external CVR service.
   *
   * <p>AIDEV-TODO: Replace stub with actual CVR verification (petition023).
   *
   * @param cvrNumber the CVR number to verify
   * @return verification result with company information or error
   */
  @CircuitBreaker(name = "person-registry", fallbackMethod = "verifyCvrFallback")
  @Retry(name = "person-registry")
  public DebtorVerificationResultDto verifyCvr(String cvrNumber) {
    log.debug("Verifying CVR debtor (stubbed): {}", cvrNumber);

    try {
      return DebtorVerificationResultDto.builder()
          .verified(true)
          .displayName("Company " + cvrNumber)
          .personId(UUID.randomUUID())
          .build();
    } catch (Exception ex) {
      log.warn("Person registry unavailable for CVR verification: {}", ex.getMessage());
      return DebtorVerificationResultDto.builder()
          .verified(false)
          .errorMessage(ERR_PERSON_REGISTRY_UNAVAILABLE)
          .build();
    }
  }

  /**
   * Verifies an SE debtor against the person-registry or external CVR service.
   *
   * <p>AIDEV-TODO: Replace stub with actual SE verification (petition023).
   *
   * @param seNumber the SE number to verify
   * @return verification result with company information or error
   */
  @CircuitBreaker(name = "person-registry", fallbackMethod = "verifySeeFallback")
  @Retry(name = "person-registry")
  public DebtorVerificationResultDto verifySe(String seNumber) {
    log.debug("Verifying SE debtor (stubbed): {}", seNumber);

    try {
      return DebtorVerificationResultDto.builder()
          .verified(true)
          .displayName("Company SE-" + seNumber)
          .personId(UUID.randomUUID())
          .build();
    } catch (Exception ex) {
      log.warn("Person registry unavailable for SE verification: {}", ex.getMessage());
      return DebtorVerificationResultDto.builder()
          .verified(false)
          .errorMessage(ERR_PERSON_REGISTRY_UNAVAILABLE)
          .build();
    }
  }

  /** Masks an identifier for log output to avoid logging PII. */
  private String maskIdentifier(String identifier) {
    if (identifier == null || identifier.length() < 4) {
      return "****";
    }
    return "****" + identifier.substring(identifier.length() - 4);
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
}
