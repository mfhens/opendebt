package dk.ufst.opendebt.creditor.client;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import dk.ufst.opendebt.common.dto.DebtEventDto;
import dk.ufst.opendebt.common.exception.OpenDebtException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * BFF client for payment-service in the creditor portal. Provides debt event retrieval for the
 * creditor timeline. Ref: petition050 specs §5.3.
 */
@Slf4j
@Component
public class PaymentServiceClient {

  private static final String CIRCUIT_BREAKER_NAME = "payment-service";

  private final WebClient webClient;

  public PaymentServiceClient(
      WebClient.Builder webClientBuilder,
      @Value("${opendebt.services.payment-service.url:http://localhost:8083}") String baseUrl) {
    this.webClient = webClientBuilder.baseUrl(baseUrl).build();
  }

  /** Retrieves all debt events for all debts linked to a case. Fallback returns empty list. */
  @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getDebtEventsByCaseFallback")
  @Retry(name = CIRCUIT_BREAKER_NAME)
  public List<DebtEventDto> getDebtEventsByCase(UUID caseId) {
    log.debug("Getting debt events for case (creditor timeline): {}", caseId);

    return webClient
        .get()
        .uri("/api/v1/events/case/{caseId}", caseId)
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
                                    "Payment service client error: " + body,
                                    "PAYMENT_CLIENT_ERROR"))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        "Payment service unavailable",
                        "PAYMENT_SERVICE_UNAVAILABLE",
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(new ParameterizedTypeReference<List<DebtEventDto>>() {})
        .block();
  }

  private List<DebtEventDto> getDebtEventsByCaseFallback(UUID caseId, Throwable t) {
    if (t instanceof WebClientResponseException wcre && wcre.getStatusCode().is4xxClientError()) {
      throw wcre;
    }
    log.warn("Circuit breaker fallback triggered for getDebtEventsByCase: {}", t.getMessage());
    return List.of();
  }
}
