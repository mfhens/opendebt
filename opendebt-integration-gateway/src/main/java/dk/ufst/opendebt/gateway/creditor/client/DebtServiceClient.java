package dk.ufst.opendebt.gateway.creditor.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import dk.ufst.opendebt.common.dto.DebtDto;
import dk.ufst.opendebt.common.exception.OpenDebtException;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class DebtServiceClient {

  private final WebClient webClient;

  public DebtServiceClient(
      WebClient.Builder webClientBuilder,
      @Value("${opendebt.services.debt-service.url:http://localhost:8082}") String baseUrl) {
    this.webClient = webClientBuilder.baseUrl(baseUrl).build();
  }

  public ClaimSubmissionResult submitClaim(DebtDto debtDto) {
    log.debug(
        "Submitting claim for creditor={} debtor={}",
        debtDto.getCreditorId(),
        debtDto.getDebtorId());

    return webClient
        .post()
        .uri("/debt-service/api/v1/debts/submit")
        .bodyValue(debtDto)
        .retrieve()
        .onStatus(
            status -> status == HttpStatus.UNPROCESSABLE_ENTITY,
            response ->
                response
                    .bodyToMono(ClaimSubmissionResult.class)
                    .flatMap(result -> Mono.error(new ClaimRejectedException(result))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        "Debt service unavailable",
                        "DEBT_SERVICE_UNAVAILABLE",
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(ClaimSubmissionResult.class)
        .block();
  }
}
