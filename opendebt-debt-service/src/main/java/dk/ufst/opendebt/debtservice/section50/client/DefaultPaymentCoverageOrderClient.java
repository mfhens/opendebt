package dk.ufst.opendebt.debtservice.section50.client;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import dk.ufst.opendebt.common.exception.OpenDebtException;
import dk.ufst.opendebt.debtservice.client.JwtBearerPropagationFilter;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DefaultPaymentCoverageOrderClient implements PaymentCoverageOrderClient {

  private final WebClient webClient;

  public DefaultPaymentCoverageOrderClient(
      WebClient.Builder webClientBuilder,
      @Value("${opendebt.services.payment-service.url:http://localhost:8083}") String baseUrl) {
    this.webClient =
        webClientBuilder.filter(JwtBearerPropagationFilter.create()).baseUrl(baseUrl).build();
  }

  @CircuitBreaker(name = "payment-service", fallbackMethod = "orderPrincipalClaimIdsFallback")
  @Override
  public List<String> orderPrincipalClaimIds(
      UUID debtorPersonId, BigDecimal availableAmount, List<String> candidateClaimIds) {
    if (candidateClaimIds == null || candidateClaimIds.isEmpty()) {
      return List.of();
    }

    List<CoverageSimulationPositionDto> positions =
        webClient
            .post()
            .uri(
                "/payment-service/api/v1/debtors/{debtorId}/daekningsraekkefoelge/simulate",
                debtorPersonId)
            .bodyValue(
                new CoverageSimulationRequestDto(
                    availableAmount, "FRIVILLIG", List.copyOf(candidateClaimIds)))
            .retrieve()
            .onStatus(
                HttpStatusCode::is5xxServerError,
                response ->
                    response
                        .bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(
                            body ->
                                reactor.core.publisher.Mono.error(
                                    new OpenDebtException(
                                        "Payment service unavailable"
                                            + (body == null || body.isBlank() ? "" : ": " + body),
                                        "PAYMENT_SERVICE_UNAVAILABLE",
                                        OpenDebtException.ErrorSeverity.CRITICAL))))
            .bodyToFlux(CoverageSimulationPositionDto.class)
            .collectList()
            .block();

    if (positions == null || positions.isEmpty()) {
      return List.copyOf(candidateClaimIds);
    }

    LinkedHashSet<String> orderedClaimIds = new LinkedHashSet<>();
    for (CoverageSimulationPositionDto position : positions) {
      if (position.fordringId() != null && candidateClaimIds.contains(position.fordringId())) {
        orderedClaimIds.add(position.fordringId());
      }
    }

    for (String candidateClaimId : candidateClaimIds) {
      orderedClaimIds.add(candidateClaimId);
    }

    return List.copyOf(orderedClaimIds);
  }

  @SuppressWarnings("unused")
  private List<String> orderPrincipalClaimIdsFallback(
      UUID debtorPersonId,
      BigDecimal availableAmount,
      List<String> candidateClaimIds,
      Throwable throwable) {
    log.warn(
        "Payment-service simulate unavailable for debtor {} and amount {}: {}",
        debtorPersonId,
        availableAmount,
        throwable.getMessage());
    throw new OpenDebtException(
        "Payment service unavailable: " + throwable.getMessage(),
        "PAYMENT_SERVICE_UNAVAILABLE",
        OpenDebtException.ErrorSeverity.CRITICAL);
  }

  private record CoverageSimulationRequestDto(
      BigDecimal beloeb, String inddrivelsesindsatsType, List<String> candidatePrincipalClaimIds) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record CoverageSimulationPositionDto(String fordringId) {}
}
