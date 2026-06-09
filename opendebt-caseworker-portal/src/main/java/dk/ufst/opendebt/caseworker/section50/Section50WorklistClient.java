package dk.ufst.opendebt.caseworker.section50;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import dk.ufst.opendebt.common.exception.OpenDebtException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import reactor.core.publisher.Mono;

@Component
public class Section50WorklistClient {

  private static final String CIRCUIT_BREAKER_NAME = "debt-service-read";
  private static final String ERR_CLIENT_PREFIX = "Section 50 worklist client error: ";
  private static final String ERR_SERVICE_UNAVAILABLE = "Debt service unavailable";
  private static final String ERROR_CODE_CLIENT = "SECTION50_WORKLIST_CLIENT_ERROR";
  private static final String ERROR_CODE_UNAVAILABLE = "DEBT_SERVICE_UNAVAILABLE";

  private final WebClient webClient;

  public Section50WorklistClient(
      WebClient.Builder webClientBuilder,
      @Value("${opendebt.services.debt-service.url:http://localhost:8082}") String baseUrl) {
    this.webClient = webClientBuilder.baseUrl(baseUrl).build();
  }

  @CircuitBreaker(name = CIRCUIT_BREAKER_NAME)
  @Retry(name = CIRCUIT_BREAKER_NAME)
  public PortalSection50WorklistDto getWorklist(UUID debtorId, UUID worklistId) {
    return webClient
        .get()
        .uri(
            "/debt-service/api/v1/debtors/{debtorId}/retskraft-worklists/{worklistId}",
            debtorId,
            worklistId)
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
        .bodyToMono(PortalSection50WorklistDto.class)
        .block();
  }

  @CircuitBreaker(name = CIRCUIT_BREAKER_NAME)
  @Retry(name = CIRCUIT_BREAKER_NAME)
  public PortalSection50WorklistDto applyOverride(
      UUID debtorId, UUID worklistId, Section50OverrideSubmission request) {
    return webClient
        .post()
        .uri(
            "/debt-service/api/v1/debtors/{debtorId}/retskraft-worklists/{worklistId}/override",
            debtorId,
            worklistId)
        .bodyValue(request)
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
        .bodyToMono(PortalSection50WorklistDto.class)
        .block();
  }

  @CircuitBreaker(name = CIRCUIT_BREAKER_NAME)
  @Retry(name = CIRCUIT_BREAKER_NAME)
  public PortalSection50WorklistDto recordModregningDecision(
      UUID debtorId, UUID worklistId, Section50ModregningDecisionSubmission request) {
    return webClient
        .post()
        .uri(
            "/debt-service/api/v1/debtors/{debtorId}/retskraft-worklists/{worklistId}/modregning-decision",
            debtorId,
            worklistId)
        .bodyValue(request)
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
        .bodyToMono(PortalSection50WorklistDto.class)
        .block();
  }
}
