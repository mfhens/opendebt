package dk.ufst.opendebt.creditor.client;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import dk.ufst.opendebt.common.exception.OpenDebtException;
import dk.ufst.opendebt.creditor.dto.ReconciliationBasisDto;
import dk.ufst.opendebt.creditor.dto.ReconciliationDetailDto;
import dk.ufst.opendebt.creditor.dto.ReconciliationListItemDto;
import dk.ufst.opendebt.creditor.dto.ReconciliationResponseDto;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * BFF client for reconciliation operations against debt-service. Uses injected {@link
 * WebClient.Builder} for trace propagation (ADR-0024).
 */
@Slf4j
@Component
public class ReconciliationServiceClient {

  private final WebClient webClient;

  public ReconciliationServiceClient(
      WebClient.Builder webClientBuilder,
      @Value("${opendebt.services.debt-service.url:http://localhost:8082}") String baseUrl) {
    this.webClient = webClientBuilder.baseUrl(baseUrl).build();
  }

  /**
   * Lists reconciliation periods for the given creditor with optional filters.
   *
   * @param creditorOrgId the acting creditor org ID
   * @param status optional status filter (e.g. ACTIVE, CLOSED)
   * @param periodEndFrom optional period end date range start
   * @param periodEndTo optional period end date range end
   * @param reconciliationStartFrom optional reconciliation start date range start
   * @param reconciliationStartTo optional reconciliation start date range end
   * @param reconciliationEndFrom optional reconciliation end date range start
   * @param reconciliationEndTo optional reconciliation end date range end
   * @return list of reconciliation periods, or empty list on failure
   */
  public List<ReconciliationListItemDto> listReconciliations(
      UUID creditorOrgId,
      String status,
      LocalDate periodEndFrom,
      LocalDate periodEndTo,
      LocalDate reconciliationStartFrom,
      LocalDate reconciliationStartTo,
      LocalDate reconciliationEndFrom,
      LocalDate reconciliationEndTo) {
    log.debug("Listing reconciliations for creditor: {}", creditorOrgId);

    try {
      List<ReconciliationListItemDto> result =
          webClient
              .get()
              .uri(
                  uriBuilder -> {
                    uriBuilder
                        .path("/debt-service/api/v1/reconciliations")
                        .queryParam("creditorId", creditorOrgId);
                    if (status != null && !status.isBlank()) {
                      uriBuilder.queryParam("status", status);
                    }
                    if (periodEndFrom != null) {
                      uriBuilder.queryParam("periodEndFrom", periodEndFrom.toString());
                    }
                    if (periodEndTo != null) {
                      uriBuilder.queryParam("periodEndTo", periodEndTo.toString());
                    }
                    if (reconciliationStartFrom != null) {
                      uriBuilder.queryParam(
                          "reconciliationStartFrom", reconciliationStartFrom.toString());
                    }
                    if (reconciliationStartTo != null) {
                      uriBuilder.queryParam(
                          "reconciliationStartTo", reconciliationStartTo.toString());
                    }
                    if (reconciliationEndFrom != null) {
                      uriBuilder.queryParam(
                          "reconciliationEndFrom", reconciliationEndFrom.toString());
                    }
                    if (reconciliationEndTo != null) {
                      uriBuilder.queryParam("reconciliationEndTo", reconciliationEndTo.toString());
                    }
                    return uriBuilder.build();
                  })
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
                                          "Reconciliation service client error: " + body,
                                          "RECONCILIATION_CLIENT_ERROR"))))
              .onStatus(
                  HttpStatusCode::is5xxServerError,
                  response ->
                      Mono.error(
                          new OpenDebtException(
                              "Debt service unavailable",
                              "DEBT_SERVICE_UNAVAILABLE",
                              OpenDebtException.ErrorSeverity.CRITICAL)))
              .bodyToMono(new ParameterizedTypeReference<List<ReconciliationListItemDto>>() {})
              .block();
      return result != null ? result : Collections.emptyList();
    } catch (Exception ex) {
      log.warn("Failed to list reconciliations: {}", ex.getMessage());
      return Collections.emptyList();
    }
  }

  /**
   * Fetches detail for a single reconciliation period including basis data.
   *
   * @param reconciliationId the reconciliation period ID
   * @return the reconciliation detail, or null on failure
   */
  public ReconciliationDetailDto getReconciliationDetail(UUID reconciliationId) {
    log.debug("Fetching reconciliation detail for id: {}", reconciliationId);

    try {
      return webClient
          .get()
          .uri(
              uriBuilder ->
                  uriBuilder
                      .path("/debt-service/api/v1/reconciliations/{id}")
                      .build(reconciliationId))
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
                                      "Reconciliation not found: " + body,
                                      "RECONCILIATION_NOT_FOUND"))))
          .onStatus(
              HttpStatusCode::is5xxServerError,
              response ->
                  Mono.error(
                      new OpenDebtException(
                          "Debt service unavailable",
                          "DEBT_SERVICE_UNAVAILABLE",
                          OpenDebtException.ErrorSeverity.CRITICAL)))
          .bodyToMono(ReconciliationDetailDto.class)
          .block();
    } catch (Exception ex) {
      log.warn("Failed to fetch reconciliation detail: {}", ex.getMessage());
      return null;
    }
  }

  /**
   * Fetches basis data for a reconciliation period.
   *
   * @param reconciliationId the reconciliation period ID
   * @return the basis data, or a zeroed DTO if unavailable
   */
  public ReconciliationBasisDto getReconciliationBasis(UUID reconciliationId) {
    log.debug("Fetching reconciliation basis for id: {}", reconciliationId);

    try {
      ReconciliationBasisDto result =
          webClient
              .get()
              .uri(
                  uriBuilder ->
                      uriBuilder
                          .path("/debt-service/api/v1/reconciliations/{id}/basis")
                          .build(reconciliationId))
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
                                          "Reconciliation basis error: " + body,
                                          "RECONCILIATION_BASIS_ERROR"))))
              .onStatus(
                  HttpStatusCode::is5xxServerError,
                  response ->
                      Mono.error(
                          new OpenDebtException(
                              "Debt service unavailable",
                              "DEBT_SERVICE_UNAVAILABLE",
                              OpenDebtException.ErrorSeverity.CRITICAL)))
              .bodyToMono(ReconciliationBasisDto.class)
              .block();
      return result != null ? result : ReconciliationBasisDto.builder().build();
    } catch (Exception ex) {
      log.warn("Failed to fetch reconciliation basis: {}", ex.getMessage());
      return ReconciliationBasisDto.builder().build();
    }
  }

  /**
   * Submits a reconciliation response for the given reconciliation period.
   *
   * @param reconciliationId the reconciliation period ID
   * @param response the reconciliation response
   */
  public void submitReconciliationResponse(
      UUID reconciliationId, ReconciliationResponseDto response) {
    log.debug("Submitting reconciliation response for id: {}", reconciliationId);

    webClient
        .post()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("/debt-service/api/v1/reconciliations/{id}/response")
                    .build(reconciliationId))
        .bodyValue(response)
        .retrieve()
        .onStatus(
            HttpStatusCode::is4xxClientError,
            resp ->
                resp.bodyToMono(String.class)
                    .flatMap(
                        body ->
                            Mono.error(
                                new OpenDebtException(
                                    "Reconciliation response error: " + body,
                                    "RECONCILIATION_RESPONSE_ERROR",
                                    OpenDebtException.ErrorSeverity.WARNING))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            resp ->
                Mono.error(
                    new OpenDebtException(
                        "Debt service unavailable",
                        "DEBT_SERVICE_UNAVAILABLE",
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .toBodilessEntity()
        .block();
  }
}
