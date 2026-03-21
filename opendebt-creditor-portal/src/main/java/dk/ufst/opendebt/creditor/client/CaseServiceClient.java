package dk.ufst.opendebt.creditor.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import dk.ufst.opendebt.common.exception.OpenDebtException;
import dk.ufst.opendebt.creditor.dto.PortalCaseDto;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class CaseServiceClient {

  private final WebClient webClient;

  public CaseServiceClient(
      WebClient.Builder webClientBuilder,
      @Value("${opendebt.services.case-service.url:http://localhost:8081}") String baseUrl) {
    this.webClient = webClientBuilder.baseUrl(baseUrl).build();
  }

  @CircuitBreaker(name = "case-service", fallbackMethod = "listCasesFallback")
  @Retry(name = "case-service")
  public RestPage<PortalCaseDto> listCases() {
    log.debug("Listing cases");

    return webClient
        .get()
        .uri("/case-service/api/v1/cases")
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
                                    "Case service client error: " + body, "CASE_CLIENT_ERROR"))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        "Case service unavailable",
                        "CASE_SERVICE_UNAVAILABLE",
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(new ParameterizedTypeReference<RestPage<PortalCaseDto>>() {})
        .block();
  }

  private RestPage<PortalCaseDto> listCasesFallback(Throwable t) {
    if (t
            instanceof
            org.springframework.web.reactive.function.client.WebClientResponseException wcre
        && wcre.getStatusCode().is4xxClientError()) {
      throw wcre;
    }
    log.warn("Circuit breaker fallback triggered for listCases: {}", t.getMessage());
    return new RestPage<>(java.util.List.of(), 0, 20, 0, 0);
  }
}
