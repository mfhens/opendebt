package dk.ufst.opendebt.payment.client;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import dk.ufst.opendebt.common.dto.CaseDto;
import dk.ufst.opendebt.common.exception.OpenDebtException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * REST client for the case-service API. Uses WebClient.Builder for trace propagation (ADR-0024). No
 * cross-service database access (ADR-0007).
 */
@Slf4j
@Component
public class CaseServiceClient {

  private final WebClient webClient;

  public CaseServiceClient(
      WebClient.Builder webClientBuilder,
      @Value("${opendebt.services.case-service.url}") String baseUrl) {
    this.webClient = webClientBuilder.baseUrl(baseUrl).build();
  }

  /**
   * Retrieves the list of debt IDs associated with a case.
   *
   * @param caseId the case ID
   * @return list of debt IDs belonging to the case
   */
  @CircuitBreaker(name = "case-service", fallbackMethod = "getDebtIdsForCaseFallback")
  @Retry(name = "case-service")
  public List<UUID> getDebtIdsForCase(UUID caseId) {
    log.debug("Fetching debt IDs for case {}", caseId);

    CaseDto caseDto =
        webClient
            .get()
            .uri("/case-service/api/v1/cases/{caseId}", caseId)
            .retrieve()
            .onStatus(
                HttpStatusCode::is4xxClientError,
                response ->
                    response
                        .bodyToMono(String.class)
                        .flatMap(
                            body ->
                                Mono.error(
                                    new OpenDebtException(
                                        "Case service client error: " + body,
                                        "CASE_CLIENT_ERROR"))))
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

    if (caseDto == null || caseDto.getDebtIds() == null) {
      return List.of();
    }
    return caseDto.getDebtIds();
  }

  private List<UUID> getDebtIdsForCaseFallback(UUID caseId, Throwable t) {
    if (t
            instanceof
            org.springframework.web.reactive.function.client.WebClientResponseException wcre
        && wcre.getStatusCode().is4xxClientError()) {
      throw wcre;
    }
    log.warn("Circuit breaker fallback triggered for getDebtIdsForCase: {}", t.getMessage());
    return List.of();
  }
}
