package dk.ufst.opendebt.gateway.soap;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import dk.ufst.opendebt.common.dto.soap.*;
import dk.ufst.opendebt.common.exception.OpenDebtException;
import dk.ufst.opendebt.gateway.soap.fault.FordringValidationException;
import dk.ufst.opendebt.gateway.soap.fault.Oces3AuthorizationException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@Component
public class DebtServiceSoapClient {

  private static final String HEADER_FORDRINGSHAVER_ID = "X-Fordringshaver-Id";
  private static final String HEADER_CORRELATION_ID = "X-Correlation-Id";
  private static final String ERR_CODE_FORDRINGSHAVER_NOT_AUTHORIZED =
      "FORDRINGSHAVER_NOT_AUTHORIZED";
  private static final String ERR_MSG_DEBT_SERVICE_UNAVAILABLE = "Debt service unavailable";
  private static final String ERR_CODE_DEBT_SERVICE_UNAVAILABLE = "DEBT_SERVICE_UNAVAILABLE";

  private final WebClient webClient;

  public DebtServiceSoapClient(
      WebClient.Builder webClientBuilder,
      @Value("${opendebt.services.debt-service.url:http://localhost:8082}") String debtServiceUrl) {
    this.webClient = webClientBuilder.baseUrl(debtServiceUrl).build();
  }

  @CircuitBreaker(name = "debtService")
  public ClaimSubmissionResponse submitClaim(
      FordringSubmitRequest request, String fordringshaverId, String correlationId) {
    try {
      return webClient
          .post()
          .uri("/internal/fordringer")
          .header(HEADER_FORDRINGSHAVER_ID, fordringshaverId != null ? fordringshaverId : "")
          .header(HEADER_CORRELATION_ID, correlationId != null ? correlationId : "")
          .bodyValue(request)
          .retrieve()
          .onStatus(
              s -> s.value() == 422,
              resp ->
                  resp.bodyToMono(ClaimSubmissionResponse.class)
                      .flatMap(
                          r ->
                              reactor.core.publisher.Mono.error(
                                  new FordringValidationException(
                                      r.getErrors() != null ? r.getErrors() : List.of()))))
          .onStatus(
              s -> s.value() == 403,
              resp ->
                  reactor.core.publisher.Mono.error(
                      new Oces3AuthorizationException(
                          "Fordringshaveren er ikke autoriseret til at indsende fordringer",
                          ERR_CODE_FORDRINGSHAVER_NOT_AUTHORIZED)))
          .onStatus(
              HttpStatusCode::is5xxServerError,
              resp ->
                  reactor.core.publisher.Mono.error(
                      new OpenDebtException(
                          ERR_MSG_DEBT_SERVICE_UNAVAILABLE,
                          ERR_CODE_DEBT_SERVICE_UNAVAILABLE,
                          OpenDebtException.ErrorSeverity.CRITICAL)))
          .bodyToMono(ClaimSubmissionResponse.class)
          .block();
    } catch (FordringValidationException | Oces3AuthorizationException | OpenDebtException e) {
      throw e;
    } catch (WebClientResponseException e) {
      throw new OpenDebtException(
          "Debt service error: " + e.getStatusCode(),
          "DEBT_SERVICE_ERROR",
          OpenDebtException.ErrorSeverity.CRITICAL);
    }
  }

  @CircuitBreaker(name = "debtService")
  public KvitteringResponse getReceipt(
      String claimId, String fordringshaverId, String correlationId) {
    try {
      return webClient
          .get()
          .uri("/internal/fordringer/{id}/kvittering", claimId)
          .header(HEADER_FORDRINGSHAVER_ID, fordringshaverId != null ? fordringshaverId : "")
          .header(HEADER_CORRELATION_ID, correlationId != null ? correlationId : "")
          .retrieve()
          .onStatus(
              s -> s.value() == 403,
              resp ->
                  reactor.core.publisher.Mono.error(
                      new Oces3AuthorizationException(
                          "Fordringshaveren er ikke autoriseret",
                          ERR_CODE_FORDRINGSHAVER_NOT_AUTHORIZED)))
          .onStatus(
              HttpStatusCode::is5xxServerError,
              resp ->
                  reactor.core.publisher.Mono.error(
                      new OpenDebtException(
                          ERR_MSG_DEBT_SERVICE_UNAVAILABLE,
                          ERR_CODE_DEBT_SERVICE_UNAVAILABLE,
                          OpenDebtException.ErrorSeverity.CRITICAL)))
          .bodyToMono(KvitteringResponse.class)
          .block();
    } catch (Oces3AuthorizationException | OpenDebtException e) {
      throw e;
    } catch (WebClientResponseException e) {
      throw new OpenDebtException(
          "Debt service error", "DEBT_SERVICE_ERROR", OpenDebtException.ErrorSeverity.CRITICAL);
    }
  }

  @CircuitBreaker(name = "debtService")
  public NotificationCollectionResult getNotifications(
      String claimId, String debtorId, String fordringshaverId, String correlationId) {
    try {
      return webClient
          .get()
          .uri(
              uriBuilder -> {
                var b = uriBuilder.path("/internal/fordringer/{id}/underretninger");
                if (debtorId != null && !debtorId.isBlank()) {
                  b = b.queryParam("debtorId", debtorId);
                }
                return b.build(claimId);
              })
          .header(HEADER_FORDRINGSHAVER_ID, fordringshaverId != null ? fordringshaverId : "")
          .header(HEADER_CORRELATION_ID, correlationId != null ? correlationId : "")
          .retrieve()
          .onStatus(
              s -> s.value() == 403,
              resp ->
                  reactor.core.publisher.Mono.error(
                      new Oces3AuthorizationException(
                          "Fordringshaveren er ikke autoriseret",
                          ERR_CODE_FORDRINGSHAVER_NOT_AUTHORIZED)))
          .onStatus(
              HttpStatusCode::is5xxServerError,
              resp ->
                  reactor.core.publisher.Mono.error(
                      new OpenDebtException(
                          ERR_MSG_DEBT_SERVICE_UNAVAILABLE,
                          ERR_CODE_DEBT_SERVICE_UNAVAILABLE,
                          OpenDebtException.ErrorSeverity.CRITICAL)))
          .bodyToMono(NotificationCollectionResult.class)
          .block();
    } catch (Oces3AuthorizationException | OpenDebtException e) {
      throw e;
    } catch (WebClientResponseException e) {
      throw new OpenDebtException(
          "Debt service error", "DEBT_SERVICE_ERROR", OpenDebtException.ErrorSeverity.CRITICAL);
    }
  }
}
