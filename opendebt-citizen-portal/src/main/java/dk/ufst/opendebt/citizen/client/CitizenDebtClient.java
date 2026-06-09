package dk.ufst.opendebt.citizen.client;

import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import dk.ufst.opendebt.citizen.dto.CitizenDebtSummaryResponse;
import dk.ufst.opendebt.citizen.exception.DebtOverviewServiceUnavailableException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class CitizenDebtClient {

  private static final String CIRCUIT_BREAKER_NAME = "debt-service-read";

  private final WebClient webClient;
  private final int pageSize;

  public CitizenDebtClient(
      WebClient.Builder webClientBuilder,
      @Value("${opendebt.services.debt-service.url:http://localhost:8082/debt-service}")
          String baseUrl,
      @Value("${opendebt.citizen.debt-overview.page-size:100}") int pageSize) {
    this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    this.pageSize = pageSize;
  }

  @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getDebtSummaryFallback")
  @Retry(name = CIRCUIT_BREAKER_NAME)
  public CitizenDebtSummaryResponse getDebtSummary(UUID personId, int pageNumber) {
    log.debug("Loading citizen debt summary for person_id={}, page={}", personId, pageNumber);

    return webClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("/api/v1/citizen/debts")
                    .queryParam("pageNumber", pageNumber)
                    .queryParam("pageSize", pageSize)
                    .build())
        .header("X-Person-Id", personId.toString())
        .retrieve()
        .onStatus(
            HttpStatusCode::is4xxClientError,
            response -> response.createException().flatMap(Mono::error))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                response
                    .createException()
                    .flatMap(
                        exception ->
                            Mono.error(
                                new DebtOverviewServiceUnavailableException(
                                    "Debt service unavailable", exception))))
        .bodyToMono(CitizenDebtSummaryResponse.class)
        .timeout(Duration.ofSeconds(5))
        .block();
  }

  private CitizenDebtSummaryResponse getDebtSummaryFallback(
      UUID personId, int pageNumber, Throwable throwable) {
    if (throwable instanceof WebClientResponseException webClientException
        && webClientException.getStatusCode().is4xxClientError()) {
      throw webClientException;
    }
    throw new DebtOverviewServiceUnavailableException(
        "Unable to load debt overview for person_id=" + personId + ", page=" + pageNumber,
        throwable);
  }
}
