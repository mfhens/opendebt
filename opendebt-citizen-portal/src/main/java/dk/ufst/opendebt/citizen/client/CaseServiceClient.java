package dk.ufst.opendebt.citizen.client;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import dk.ufst.opendebt.common.dto.CaseDto;
import dk.ufst.opendebt.common.dto.CaseEventDto;
import dk.ufst.opendebt.common.exception.OpenDebtException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * BFF client for case-service in the citizen portal. Provides case and event retrieval for the
 * citizen timeline. Ref: petition050 specs §4.2.
 */
@Slf4j
@Component
public class CaseServiceClient {

  private final WebClient webClient;

  public CaseServiceClient(
      WebClient.Builder webClientBuilder,
      @Value("${opendebt.services.case-service.url:http://localhost:8081}") String baseUrl) {
    this.webClient = webClientBuilder.baseUrl(baseUrl).build();
  }

  /** Retrieves events (hændelseslog) for a case. Fallback returns empty list. */
  @CircuitBreaker(name = "case-service", fallbackMethod = "getEventsFallback")
  @Retry(name = "case-service")
  public List<CaseEventDto> getEvents(UUID caseId) {
    log.debug("Getting events for case: {}", caseId);

    return webClient
        .get()
        .uri("/case-service/api/v1/cases/{id}/events", caseId)
        .retrieve()
        .onStatus(
            HttpStatusCode::isError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        "Failed to load events for case: " + caseId, "CASE_RESOURCE_ERROR")))
        .bodyToMono(new ParameterizedTypeReference<List<CaseEventDto>>() {})
        .block();
  }

  private List<CaseEventDto> getEventsFallback(UUID caseId, Throwable t) {
    if (t instanceof WebClientResponseException wcre && wcre.getStatusCode().is4xxClientError()) {
      throw wcre;
    }
    log.warn("Circuit breaker fallback triggered for getEvents: {}", t.getMessage());
    return List.of();
  }

  /** Retrieves a single case by ID. Fallback returns null. */
  @CircuitBreaker(name = "case-service", fallbackMethod = "getCaseFallback")
  @Retry(name = "case-service")
  public CaseDto getCase(UUID caseId) {
    log.debug("Getting case: {}", caseId);

    return webClient
        .get()
        .uri("/case-service/api/v1/cases/{id}", caseId)
        .retrieve()
        .onStatus(
            HttpStatusCode::is4xxClientError,
            response ->
                Mono.error(new OpenDebtException("Case not found: " + caseId, "CASE_NOT_FOUND")))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        "Case service unavailable",
                        "CASE_SERVICE_UNAVAILABLE",
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(CaseDto.class)
        .block();
  }

  private CaseDto getCaseFallback(UUID caseId, Throwable t) {
    if (t instanceof WebClientResponseException wcre && wcre.getStatusCode().is4xxClientError()) {
      throw wcre;
    }
    log.warn("Circuit breaker fallback triggered for getCase: {}", t.getMessage());
    return null;
  }
}
