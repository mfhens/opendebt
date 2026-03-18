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
import dk.ufst.opendebt.common.exception.OpenDebtException;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * BFF client for payment-service. Retrieves ledger entries, events, and summaries for display in
 * the caseworker portal posteringslog.
 */
@Slf4j
@Component
public class PaymentServiceClient {

  private final WebClient webClient;

  public PaymentServiceClient(
      WebClient.Builder webClientBuilder,
      @Value("${opendebt.services.payment-service.url:http://localhost:8083}") String baseUrl) {
    this.webClient = webClientBuilder.baseUrl(baseUrl).build();
  }

  /** Retrieves ledger entries for a specific debt. */
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
        .bodyToMono(new ParameterizedTypeReference<RestPage<PortalLedgerEntryDto>>() {})
        .block();
  }

  /** Retrieves ledger entries for all debts in a case. */
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
        .bodyToMono(new ParameterizedTypeReference<RestPage<PortalLedgerEntryDto>>() {})
        .block();
  }

  /** Retrieves all events for a specific debt. */
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
        .bodyToMono(new ParameterizedTypeReference<List<PortalDebtEventDto>>() {})
        .block();
  }

  /** Retrieves the ledger summary (balances) for a specific debt. */
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
        .bodyToMono(PortalLedgerSummaryDto.class)
        .block();
  }
}
