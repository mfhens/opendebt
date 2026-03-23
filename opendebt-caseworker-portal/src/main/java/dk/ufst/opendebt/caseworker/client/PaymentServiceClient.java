package dk.ufst.opendebt.caseworker.client;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import dk.ufst.opendebt.caseworker.dto.PortalDebtEventDto;
import dk.ufst.opendebt.caseworker.dto.PortalLedgerEntryDto;
import dk.ufst.opendebt.caseworker.dto.PortalLedgerSummaryDto;
import dk.ufst.opendebt.common.dto.DebtEventDto;
import dk.ufst.opendebt.common.exception.OpenDebtException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * BFF client for payment-service. Retrieves ledger entries, events, and summaries for display in
 * the caseworker portal posteringslog.
 */
@Slf4j
@Component
public class PaymentServiceClient {

  private static final String CIRCUIT_BREAKER_NAME = "payment-service";
  private static final String ERR_CLIENT_PREFIX = "Payment service client error: ";
  private static final String ERR_SERVICE_UNAVAILABLE = "Payment service unavailable";
  private static final String ERROR_CODE_CLIENT = "PAYMENT_CLIENT_ERROR";
  private static final String ERROR_CODE_UNAVAILABLE = "PAYMENT_SERVICE_UNAVAILABLE";

  private final WebClient webClient;

  public PaymentServiceClient(
      WebClient.Builder webClientBuilder,
      @Value("${opendebt.services.payment-service.url:http://localhost:8083}") String baseUrl) {
    this.webClient = webClientBuilder.baseUrl(baseUrl).build();
  }

  /** Retrieves ledger entries for a specific debt. */
  @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getLedgerByDebtFallback")
  @Retry(name = CIRCUIT_BREAKER_NAME)
  public RestPage<PortalLedgerEntryDto> getLedgerByDebt(
      UUID debtId, String category, String fromDate, String toDate, int page, int size) {
    log.debug("Getting ledger for debt: {}", debtId);

    return webClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("/payment-service/api/v1/ledger/debt/{debtId}")
                    .queryParamIfPresent(
                        "category",
                        category != null && !category.isBlank()
                            ? java.util.Optional.of(category)
                            : java.util.Optional.empty())
                    .queryParamIfPresent(
                        "fromDate",
                        fromDate != null && !fromDate.isBlank()
                            ? java.util.Optional.of(fromDate)
                            : java.util.Optional.empty())
                    .queryParamIfPresent(
                        "toDate",
                        toDate != null && !toDate.isBlank()
                            ? java.util.Optional.of(toDate)
                            : java.util.Optional.empty())
                    .queryParam("page", page)
                    .queryParam("size", size)
                    .build(debtId))
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
                                    ERR_CLIENT_PREFIX + body, ERROR_CODE_CLIENT))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        ERR_SERVICE_UNAVAILABLE,
                        ERROR_CODE_UNAVAILABLE,
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(new ParameterizedTypeReference<RestPage<PortalLedgerEntryDto>>() {})
        .block();
  }

  /** Retrieves ledger entries for all debts in a case. */
  @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getLedgerByCaseFallback")
  @Retry(name = CIRCUIT_BREAKER_NAME)
  public RestPage<PortalLedgerEntryDto> getLedgerByCase(
      UUID caseId, String category, String fromDate, String toDate, int page, int size) {
    log.debug("Getting ledger for case: {}", caseId);

    return webClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("/payment-service/api/v1/ledger/case/{caseId}")
                    .queryParamIfPresent(
                        "category",
                        category != null && !category.isBlank()
                            ? java.util.Optional.of(category)
                            : java.util.Optional.empty())
                    .queryParamIfPresent(
                        "fromDate",
                        fromDate != null && !fromDate.isBlank()
                            ? java.util.Optional.of(fromDate)
                            : java.util.Optional.empty())
                    .queryParamIfPresent(
                        "toDate",
                        toDate != null && !toDate.isBlank()
                            ? java.util.Optional.of(toDate)
                            : java.util.Optional.empty())
                    .queryParam("page", page)
                    .queryParam("size", size)
                    .build(caseId))
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
                                    ERR_CLIENT_PREFIX + body, ERROR_CODE_CLIENT))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        ERR_SERVICE_UNAVAILABLE,
                        ERROR_CODE_UNAVAILABLE,
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(new ParameterizedTypeReference<RestPage<PortalLedgerEntryDto>>() {})
        .block();
  }

  /** Retrieves all events for a specific debt. */
  @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getEventsByDebtFallback")
  @Retry(name = CIRCUIT_BREAKER_NAME)
  public List<PortalDebtEventDto> getEventsByDebt(UUID debtId) {
    log.debug("Getting events for debt: {}", debtId);

    return webClient
        .get()
        .uri("/payment-service/api/v1/events/debt/{debtId}", debtId)
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
                                    ERR_CLIENT_PREFIX + body, ERROR_CODE_CLIENT))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        ERR_SERVICE_UNAVAILABLE,
                        ERROR_CODE_UNAVAILABLE,
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(new ParameterizedTypeReference<List<PortalDebtEventDto>>() {})
        .block();
  }

  /**
   * Retrieves all debt events for all debts linked to a case.
   *
   * <p>Used exclusively by the timeline BFF aggregation. Returns {@link DebtEventDto} from
   * opendebt-common (not the portal-specific PortalDebtEventDto).
   *
   * <p>Ref: specs §3.3 — OI-2 (DebtEventDto promoted to common).
   */
  @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getDebtEventsByCaseFallback")
  @Retry(name = CIRCUIT_BREAKER_NAME)
  public List<DebtEventDto> getDebtEventsByCase(UUID caseId) {
    log.debug("Getting debt events for case (timeline): {}", caseId);

    return webClient
        .get()
        .uri("/payment-service/api/v1/events/case/{caseId}", caseId)
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
                                    ERR_CLIENT_PREFIX + body, ERROR_CODE_CLIENT))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        ERR_SERVICE_UNAVAILABLE,
                        ERROR_CODE_UNAVAILABLE,
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(new ParameterizedTypeReference<List<DebtEventDto>>() {})
        .block();
  }

  private List<DebtEventDto> getDebtEventsByCaseFallback(UUID caseId, Throwable t) {
    if (t
            instanceof
            org.springframework.web.reactive.function.client.WebClientResponseException wcre
        && wcre.getStatusCode().is4xxClientError()) {
      throw wcre;
    }
    log.warn("Circuit breaker fallback triggered for getDebtEventsByCase: {}", t.getMessage());
    return List.of();
  }

  /** Retrieves the ledger summary (balances) for a specific debt. */
  @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getLedgerSummaryFallback")
  @Retry(name = CIRCUIT_BREAKER_NAME)
  public PortalLedgerSummaryDto getLedgerSummary(UUID debtId) {
    log.debug("Getting ledger summary for debt: {}", debtId);

    return webClient
        .get()
        .uri("/payment-service/api/v1/ledger/debt/{debtId}/summary", debtId)
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
                                    ERR_CLIENT_PREFIX + body, ERROR_CODE_CLIENT))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        ERR_SERVICE_UNAVAILABLE,
                        ERROR_CODE_UNAVAILABLE,
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(PortalLedgerSummaryDto.class)
        .block();
  }

  private RestPage<PortalLedgerEntryDto> getLedgerByDebtFallback(
      UUID debtId,
      String category,
      String fromDate,
      String toDate,
      int page,
      int size,
      Throwable t) {
    if (t
            instanceof
            org.springframework.web.reactive.function.client.WebClientResponseException wcre
        && wcre.getStatusCode().is4xxClientError()) {
      throw wcre;
    }
    log.warn("Circuit breaker fallback triggered for getLedgerByDebt: {}", t.getMessage());
    return new RestPage<>(List.of(), page, size, 0, 0);
  }

  private RestPage<PortalLedgerEntryDto> getLedgerByCaseFallback(
      UUID caseId,
      String category,
      String fromDate,
      String toDate,
      int page,
      int size,
      Throwable t) {
    if (t
            instanceof
            org.springframework.web.reactive.function.client.WebClientResponseException wcre
        && wcre.getStatusCode().is4xxClientError()) {
      throw wcre;
    }
    log.warn("Circuit breaker fallback triggered for getLedgerByCase: {}", t.getMessage());
    return new RestPage<>(List.of(), page, size, 0, 0);
  }

  private List<PortalDebtEventDto> getEventsByDebtFallback(UUID debtId, Throwable t) {
    if (t
            instanceof
            org.springframework.web.reactive.function.client.WebClientResponseException wcre
        && wcre.getStatusCode().is4xxClientError()) {
      throw wcre;
    }
    log.warn("Circuit breaker fallback triggered for getEventsByDebt: {}", t.getMessage());
    return List.of();
  }

  private PortalLedgerSummaryDto getLedgerSummaryFallback(UUID debtId, Throwable t) {
    if (t
            instanceof
            org.springframework.web.reactive.function.client.WebClientResponseException wcre
        && wcre.getStatusCode().is4xxClientError()) {
      throw wcre;
    }
    log.warn("Circuit breaker fallback triggered for getLedgerSummary: {}", t.getMessage());
    return null;
  }
}
