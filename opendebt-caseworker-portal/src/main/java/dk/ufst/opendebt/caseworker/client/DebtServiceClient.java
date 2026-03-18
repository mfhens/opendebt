package dk.ufst.opendebt.caseworker.client;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import dk.ufst.opendebt.common.dto.DebtDto;
import dk.ufst.opendebt.common.exception.OpenDebtException;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/** BFF client for debt-service. Retrieves debt details for display in the caseworker portal. */
@Slf4j
@Component
public class DebtServiceClient {

  private final WebClient webClient;

  public DebtServiceClient(
      WebClient.Builder webClientBuilder,
      @Value("${opendebt.services.debt-service.url:http://localhost:8082}") String baseUrl) {
    this.webClient = webClientBuilder.baseUrl(baseUrl).build();
  }

  /** Lists debts by their IDs (used to resolve the debtIds list from a case). */
  public RestPage<DebtDto> listDebtsByIds(List<UUID> debtIds) {
    if (debtIds == null || debtIds.isEmpty()) {
      return new RestPage<>(List.of(), 0, 20, 0, 0);
    }
    log.debug("Listing debts by IDs: count={}", debtIds.size());

    String idsParam = String.join(",", debtIds.stream().map(UUID::toString).toList());

    return webClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder.path("/debt-service/api/v1/debts").queryParam("ids", idsParam).build())
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
                                    "Debt service client error: " + body, "DEBT_CLIENT_ERROR"))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        "Debt service unavailable",
                        "DEBT_SERVICE_UNAVAILABLE",
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(new ParameterizedTypeReference<RestPage<DebtDto>>() {})
        .block();
  }

  /** Retrieves a single debt by ID. */
  public DebtDto getDebt(UUID debtId) {
    log.debug("Getting debt: {}", debtId);

    return webClient
        .get()
        .uri("/debt-service/api/v1/debts/{id}", debtId)
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
                                    "Debt not found: " + debtId, "DEBT_NOT_FOUND"))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        "Debt service unavailable",
                        "DEBT_SERVICE_UNAVAILABLE",
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(DebtDto.class)
        .block();
  }
}
