package dk.ufst.opendebt.creditor.client;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import dk.ufst.opendebt.common.exception.OpenDebtException;
import dk.ufst.opendebt.creditor.dto.AccessResolutionRequest;
import dk.ufst.opendebt.creditor.dto.AccessResolutionResponse;
import dk.ufst.opendebt.creditor.dto.ContactEmailUpdateDto;
import dk.ufst.opendebt.creditor.dto.CreditorAgreementDto;
import dk.ufst.opendebt.creditor.dto.PortalCreditorDto;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class CreditorServiceClient {

  private static final String CIRCUIT_BREAKER_NAME = "creditor-service";
  private static final String ERR_SERVICE_UNAVAILABLE = "Creditor service unavailable";
  private static final String ERROR_CODE_UNAVAILABLE = "CREDITOR_SERVICE_UNAVAILABLE";

  private final WebClient webClient;

  public CreditorServiceClient(
      WebClient.Builder webClientBuilder,
      @Value("${opendebt.services.creditor-service.url:http://localhost:8092}") String baseUrl) {
    this.webClient = webClientBuilder.baseUrl(baseUrl).build();
  }

  @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "listAllActiveFallback")
  @Retry(name = CIRCUIT_BREAKER_NAME)
  public List<PortalCreditorDto> listAllActive() {
    try {
      List<PortalCreditorDto> result =
          webClient
              .get()
              .uri("/creditor-service/api/v1/creditors")
              .retrieve()
              .bodyToMono(new ParameterizedTypeReference<List<PortalCreditorDto>>() {})
              .block();
      return result != null ? result : Collections.emptyList();
    } catch (Exception ex) {
      log.warn("Failed to list creditors: {}", ex.getMessage());
      return Collections.emptyList();
    }
  }

  @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getByCreditorOrgIdFallback")
  @Retry(name = CIRCUIT_BREAKER_NAME)
  public PortalCreditorDto getByCreditorOrgId(UUID creditorOrgId) {
    log.debug("Fetching creditor by orgId: {}", creditorOrgId);

    return webClient
        .get()
        .uri("/creditor-service/api/v1/creditors/{creditorOrgId}", creditorOrgId)
        .retrieve()
        .onStatus(
            HttpStatusCode::is4xxClientError,
            response -> {
              if (response.statusCode() == HttpStatus.NOT_FOUND) {
                return Mono.error(
                    new OpenDebtException(
                        "Creditor not found: " + creditorOrgId,
                        "CREDITOR_NOT_FOUND",
                        OpenDebtException.ErrorSeverity.WARNING));
              }
              return response
                  .bodyToMono(String.class)
                  .flatMap(
                      body ->
                          Mono.error(
                              new OpenDebtException(
                                  "Creditor service client error: " + body,
                                  "CREDITOR_CLIENT_ERROR")));
            })
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        ERR_SERVICE_UNAVAILABLE,
                        ERROR_CODE_UNAVAILABLE,
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(PortalCreditorDto.class)
        .block();
  }

  /** Fetches the creditor agreement configuration. */
  @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getCreditorAgreementFallback")
  @Retry(name = CIRCUIT_BREAKER_NAME)
  public CreditorAgreementDto getCreditorAgreement(UUID creditorOrgId) {
    log.debug("Fetching creditor agreement for orgId: {}", creditorOrgId);

    try {
      return webClient
          .get()
          .uri("/creditor-service/api/v1/creditors/{creditorOrgId}/agreement", creditorOrgId)
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
                                      "Creditor service client error: " + body,
                                      "CREDITOR_CLIENT_ERROR"))))
          .onStatus(
              HttpStatusCode::is5xxServerError,
              response ->
                  Mono.error(
                      new OpenDebtException(
                          ERR_SERVICE_UNAVAILABLE,
                          ERROR_CODE_UNAVAILABLE,
                          OpenDebtException.ErrorSeverity.CRITICAL)))
          .bodyToMono(CreditorAgreementDto.class)
          .block();
    } catch (Exception ex) {
      log.warn("Failed to fetch creditor agreement: {}", ex.getMessage());
      return null;
    }
  }

  /** Updates the creditor contact email. */
  @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "updateContactEmailFallback")
  public void updateContactEmail(UUID creditorOrgId, ContactEmailUpdateDto updateDto) {
    log.debug("Updating contact email for creditor: {}", creditorOrgId);

    webClient
        .put()
        .uri("/creditor-service/api/v1/creditors/{creditorOrgId}/contact", creditorOrgId)
        .bodyValue(updateDto)
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
                                    "Contact email update failed: " + body,
                                    "CONTACT_UPDATE_ERROR",
                                    OpenDebtException.ErrorSeverity.WARNING))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        ERR_SERVICE_UNAVAILABLE,
                        ERROR_CODE_UNAVAILABLE,
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .toBodilessEntity()
        .block();
  }

  @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "resolveAccessFallback")
  @Retry(name = CIRCUIT_BREAKER_NAME)
  public AccessResolutionResponse resolveAccess(AccessResolutionRequest request) {
    log.debug("Resolving access for channel: {}", request.getChannelType());

    return webClient
        .post()
        .uri("/creditor-service/api/v1/creditors/access/resolve")
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
                                    "Access resolution failed: " + body,
                                    "ACCESS_RESOLUTION_ERROR"))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        ERR_SERVICE_UNAVAILABLE,
                        ERROR_CODE_UNAVAILABLE,
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(AccessResolutionResponse.class)
        .block();
  }

  private List<PortalCreditorDto> listAllActiveFallback(Throwable t) {
    if (t
            instanceof
            org.springframework.web.reactive.function.client.WebClientResponseException wcre
        && wcre.getStatusCode().is4xxClientError()) {
      throw wcre;
    }
    log.warn("Circuit breaker fallback triggered for listAllActive: {}", t.getMessage());
    return Collections.emptyList();
  }

  private PortalCreditorDto getByCreditorOrgIdFallback(UUID creditorOrgId, Throwable t) {
    if (t
            instanceof
            org.springframework.web.reactive.function.client.WebClientResponseException wcre
        && wcre.getStatusCode().is4xxClientError()) {
      throw wcre;
    }
    log.warn("Circuit breaker fallback triggered for getByCreditorOrgId: {}", t.getMessage());
    return null;
  }

  private CreditorAgreementDto getCreditorAgreementFallback(UUID creditorOrgId, Throwable t) {
    if (t
            instanceof
            org.springframework.web.reactive.function.client.WebClientResponseException wcre
        && wcre.getStatusCode().is4xxClientError()) {
      throw wcre;
    }
    log.warn("Circuit breaker fallback triggered for getCreditorAgreement: {}", t.getMessage());
    return null;
  }

  private void updateContactEmailFallback(
      UUID creditorOrgId, ContactEmailUpdateDto updateDto, Throwable t) {
    if (t
            instanceof
            org.springframework.web.reactive.function.client.WebClientResponseException wcre
        && wcre.getStatusCode().is4xxClientError()) {
      throw wcre;
    }
    log.warn("Circuit breaker fallback triggered for updateContactEmail: {}", t.getMessage());
  }

  private AccessResolutionResponse resolveAccessFallback(
      AccessResolutionRequest request, Throwable t) {
    if (t
            instanceof
            org.springframework.web.reactive.function.client.WebClientResponseException wcre
        && wcre.getStatusCode().is4xxClientError()) {
      throw wcre;
    }
    log.warn("Circuit breaker fallback triggered for resolveAccess: {}", t.getMessage());
    return null;
  }
}
