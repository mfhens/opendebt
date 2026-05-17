package dk.ufst.opendebt.debtservice.limitation.client;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import dk.ufst.opendebt.debtservice.client.JwtBearerPropagationFilter;
import dk.ufst.opendebt.debtservice.limitation.client.dto.WageGarnishmentLimitationFacts;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@Component
public class WageGarnishmentFactClient {

  private final WebClient webClient;

  public WageGarnishmentFactClient(
      WebClient.Builder webClientBuilder,
      @Value("${opendebt.services.wage-garnishment-service.url:http://localhost:8088}")
          String baseUrl) {
    this.webClient =
        webClientBuilder.filter(JwtBearerPropagationFilter.create()).baseUrl(baseUrl).build();
  }

  @CircuitBreaker(name = "wage-garnishment-service", fallbackMethod = "getFactsFallback")
  public WageGarnishmentLimitationFacts getFacts(UUID debtorPersonId) {
    return webClient
        .get()
        .uri(
            "/wage-garnishment-service/api/internal/v1/limitation-facts/debtors/{debtorPersonId}",
            debtorPersonId)
        .retrieve()
        .bodyToMono(WageGarnishmentLimitationFacts.class)
        .block();
  }

  @SuppressWarnings("unused")
  private WageGarnishmentLimitationFacts getFactsFallback(
      UUID debtorPersonId, Throwable throwable) {
    return WageGarnishmentLimitationFacts.builder()
        .debtorPersonId(debtorPersonId)
        .decisionRegistered(false)
        .coveredFordringIds(List.of())
        .build();
  }
}
