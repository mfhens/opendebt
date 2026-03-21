package dk.ufst.opendebt.payment.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import dk.ufst.opendebt.common.dto.DebtDto;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;

// AIDEV-NOTE: This is the only permitted way for payment-service to read/write debt data
// (ADR-0007).
// Never add a DebtRepository dependency to payment-service — all access must go through this
// client.
/** REST client for the debt-service API. No cross-service database access (ADR-0007). */
@Slf4j
@Component
public class DebtServiceClient {

  private final RestClient restClient;

  public DebtServiceClient(
      RestClient.Builder restClientBuilder,
      @Value("${opendebt.services.debt-service.url}") String debtServiceUrl) {
    // AIDEV-NOTE: Base URL includes "/debt-service" context path matching the K8s ingress config.
    // If running locally without ingress, set
    // opendebt.services.debt-service.url=http://localhost:8081
    this.restClient =
        restClientBuilder.baseUrl(debtServiceUrl + "/debt-service/api/v1/debts").build();
  }

  /**
   * Finds debts matching the given OCR-linje.
   *
   * @param ocrLine the Betalingsservice OCR-linje
   * @return list of matching debts (empty if none)
   */
  @CircuitBreaker(name = "debt-service-read", fallbackMethod = "findByOcrLineFallback")
  @Retry(name = "debt-service-read")
  public List<DebtDto> findByOcrLine(String ocrLine) {
    log.debug("Looking up debts by OCR-linje: {}", ocrLine);
    return restClient
        .get()
        .uri(uriBuilder -> uriBuilder.path("/by-ocr").queryParam("ocrLine", ocrLine).build())
        .retrieve()
        .body(new ParameterizedTypeReference<>() {});
  }

  private List<DebtDto> findByOcrLineFallback(String ocrLine, Throwable t) {
    if (t instanceof HttpClientErrorException) {
      throw (HttpClientErrorException) t;
    }
    log.warn("Circuit breaker fallback triggered for findByOcrLine: {}", t.getMessage());
    return List.of();
  }

  /**
   * Writes down the outstanding balance of a debt by the specified amount.
   *
   * @param debtId the debt ID
   * @param amount the write-down amount
   * @return the updated debt
   */
  @CircuitBreaker(name = "debt-service-write", fallbackMethod = "writeDownFallback")
  public DebtDto writeDown(UUID debtId, BigDecimal amount) {
    log.info("Writing down debt {}, amount={}", debtId, amount);
    return restClient
        .post()
        .uri(
            uriBuilder ->
                uriBuilder.path("/{id}/write-down").queryParam("amount", amount).build(debtId))
        .retrieve()
        .body(DebtDto.class);
  }

  private DebtDto writeDownFallback(UUID debtId, BigDecimal amount, Throwable t) {
    throw t instanceof RuntimeException ? (RuntimeException) t : new RuntimeException(t);
  }

  /**
   * Retroactively recalculates interest journal entries in debt-service from the given date
   * forward. Called after a crossing transaction has been detected and the timeline replayed in
   * payment-service (petition039).
   *
   * <p>Orchestrated by Flowable in case-service (ADR-0019): write-down is called first (to correct
   * the outstanding balance), then this method is called so that interest entries are recalculated
   * against the corrected balance.
   *
   * @param debtId the debt whose interest journal needs correction
   * @param from the earliest accrual date to delete and recalculate (inclusive)
   */
  @CircuitBreaker(name = "debt-service-write", fallbackMethod = "recalculateInterestFallback")
  public void recalculateInterest(UUID debtId, LocalDate from) {
    log.info("Requesting interest recalculation for debt {}, from={}", debtId, from);
    restClient
        .post()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("/{id}/interest/recalculate")
                    .queryParam("from", from)
                    .build(debtId))
        .retrieve()
        .toBodilessEntity();
  }

  private void recalculateInterestFallback(UUID debtId, LocalDate from, Throwable t) {
    if (t instanceof HttpClientErrorException) {
      throw (HttpClientErrorException) t;
    }
    log.warn("Circuit breaker fallback triggered for recalculateInterest: {}", t.getMessage());
  }
}
