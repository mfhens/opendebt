package dk.ufst.opendebt.gateway.creditor.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import dk.ufst.opendebt.common.exception.OpenDebtException;
import dk.ufst.opendebt.gateway.creditor.dto.AccessResolutionRequest;
import dk.ufst.opendebt.gateway.creditor.dto.AccessResolutionResponse;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class CreditorServiceClient {

  private final WebClient webClient;

  public CreditorServiceClient(
      WebClient.Builder webClientBuilder,
      @Value("${opendebt.services.creditor-service.url:http://localhost:8092}") String baseUrl) {
    this.webClient = webClientBuilder.baseUrl(baseUrl).build();
  }

  @CircuitBreaker(name = "creditor-service", fallbackMethod = "resolveAccessFallback")
  @Retry(name = "creditor-service")
  public AccessResolutionResponse resolveAccess(AccessResolutionRequest request) {
    log.debug(
        "Resolving access for channel={} identity={}",
        request.getChannelType(),
        request.getPresentedIdentity());

    return webClient
        .post()
        .uri("/creditor-service/api/v1/creditors/access/resolve")
        .bodyValue(request)
        .retrieve()
        .onStatus(
            HttpStatusCode::is4xxClientError,
            response -> {
              if (response.statusCode() == HttpStatus.FORBIDDEN) {
                return response
                    .bodyToMono(String.class)
                    .flatMap(
                        body ->
                            Mono.error(
                                new OpenDebtException(
                                    "Access resolution denied: " + body,
                                    "ACCESS_DENIED",
                                    OpenDebtException.ErrorSeverity.WARNING)));
              }
              return response
                  .bodyToMono(String.class)
                  .flatMap(
                      body ->
                          Mono.error(
                              new OpenDebtException(
                                  "Access resolution failed: " + body, "ACCESS_RESOLUTION_ERROR")));
            })
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        "Creditor service unavailable",
                        "CREDITOR_SERVICE_UNAVAILABLE",
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(AccessResolutionResponse.class)
        .block();
  }

  private AccessResolutionResponse resolveAccessFallback(
      AccessResolutionRequest request, Throwable t) {
    if (t
            instanceof
            org.springframework.web.reactive.function.client.WebClientResponseException wcre
        && wcre.getStatusCode().is4xxClientError()) {
      throw wcre;
    }
    throw new OpenDebtException(
        "Creditor service unavailable",
        "CREDITOR_SERVICE_UNAVAILABLE",
        OpenDebtException.ErrorSeverity.CRITICAL);
  }
}
