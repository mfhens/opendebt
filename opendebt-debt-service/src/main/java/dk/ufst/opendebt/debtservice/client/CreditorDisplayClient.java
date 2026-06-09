package dk.ufst.opendebt.debtservice.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import dk.ufst.opendebt.common.exception.OpenDebtException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class CreditorDisplayClient {

  private final WebClient webClient;

  public CreditorDisplayClient(
      WebClient.Builder webClientBuilder,
      @Value("${opendebt.services.creditor-service.url:http://localhost:8092}") String baseUrl) {
    this.webClient =
        webClientBuilder.filter(JwtBearerPropagationFilter.create()).baseUrl(baseUrl).build();
  }

  @CircuitBreaker(name = "creditor-service", fallbackMethod = "getDisplayNameFallback")
  @Retry(name = "creditor-service")
  public String getDisplayName(UUID creditorOrgId) {
    log.debug("Resolving creditor display name for creditorOrgId={}", creditorOrgId);

    CreditorDisplayResponse response =
        webClient
            .get()
            .uri("/creditor-service/api/v1/creditors/{creditorOrgId}", creditorOrgId)
            .retrieve()
            .onStatus(
                HttpStatusCode::is4xxClientError,
                clientResponse -> {
                  if (clientResponse.statusCode() == HttpStatus.NOT_FOUND) {
                    return Mono.error(
                        new OpenDebtException(
                            "Creditor not found: " + creditorOrgId,
                            "CREDITOR_NOT_FOUND",
                            OpenDebtException.ErrorSeverity.WARNING));
                  }
                  return clientResponse
                      .bodyToMono(String.class)
                      .defaultIfEmpty("")
                      .flatMap(
                          body ->
                              Mono.error(
                                  new OpenDebtException(
                                      "Creditor display lookup failed: " + body,
                                      "CREDITOR_LOOKUP_ERROR")));
                })
            .onStatus(
                HttpStatusCode::is5xxServerError,
                clientResponse ->
                    Mono.error(
                        new OpenDebtException(
                            "Creditor service unavailable",
                            "CREDITOR_SERVICE_UNAVAILABLE",
                            OpenDebtException.ErrorSeverity.CRITICAL)))
            .bodyToMono(CreditorDisplayResponse.class)
            .block();

    if (response == null
        || response.getDisplayName() == null
        || response.getDisplayName().isBlank()) {
      throw new OpenDebtException(
          "Creditor display name missing for creditorOrgId=" + creditorOrgId,
          "CREDITOR_DISPLAY_NAME_MISSING");
    }

    return response.getDisplayName();
  }

  @SuppressWarnings("unused")
  private String getDisplayNameFallback(UUID creditorOrgId, Throwable throwable) {
    if (throwable instanceof OpenDebtException openDebtException) {
      throw openDebtException;
    }
    throw new OpenDebtException(
        "Creditor display lookup unavailable for creditorOrgId=" + creditorOrgId,
        "CREDITOR_SERVICE_UNAVAILABLE",
        OpenDebtException.ErrorSeverity.CRITICAL);
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  private static class CreditorDisplayResponse {
    private String displayName;
  }
}
