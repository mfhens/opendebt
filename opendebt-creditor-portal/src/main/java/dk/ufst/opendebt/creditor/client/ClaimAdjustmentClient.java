package dk.ufst.opendebt.creditor.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import dk.ufst.opendebt.common.exception.OpenDebtException;
import dk.ufst.opendebt.creditor.dto.AdjustmentReceiptDto;
import dk.ufst.opendebt.creditor.dto.ClaimAdjustmentRequestDto;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class ClaimAdjustmentClient {

  private final WebClient webClient;

  public ClaimAdjustmentClient(
      WebClient.Builder webClientBuilder,
      @Value("${opendebt.services.debt-service.url:http://localhost:8082}") String baseUrl) {
    this.webClient = webClientBuilder.baseUrl(baseUrl).build();
  }

  @CircuitBreaker(
      name = DebtServiceClientSupport.READ_CIRCUIT_BREAKER,
      fallbackMethod = "submitAdjustmentFallback")
  public AdjustmentReceiptDto submitAdjustment(UUID claimId, ClaimAdjustmentRequestDto request) {
    log.debug(
        "Submitting adjustment for claimId: {}, type: {}", claimId, request.getAdjustmentType());

    return webClient
        .put()
        .uri(
            uriBuilder ->
                uriBuilder.path("/debt-service/api/v1/debts/{id}/adjustments").build(claimId))
        .bodyValue(request)
        .retrieve()
        .onStatus(
            HttpStatusCode::is4xxClientError,
            response -> {
              if (response.statusCode() == HttpStatus.BAD_REQUEST) {
                return response
                    .bodyToMono(String.class)
                    .flatMap(
                        body ->
                            Mono.error(
                                new OpenDebtException(
                                    "Invalid adjustment request: " + body,
                                    "ADJUSTMENT_VALIDATION_ERROR",
                                    OpenDebtException.ErrorSeverity.WARNING)));
              }
              return DebtServiceClientSupport.standardClientError(response);
            })
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response -> Mono.error(DebtServiceClientSupport.unavailableError()))
        .bodyToMono(AdjustmentReceiptDto.class)
        .block();
  }

  private AdjustmentReceiptDto submitAdjustmentFallback(
      UUID claimId, ClaimAdjustmentRequestDto request, Throwable throwable) {
    if (DebtServiceClientSupport.isClient4xx(throwable)) {
      throw (RuntimeException) throwable;
    }
    log.warn("Circuit breaker fallback triggered for submitAdjustment: {}", throwable.getMessage());
    return null;
  }
}
