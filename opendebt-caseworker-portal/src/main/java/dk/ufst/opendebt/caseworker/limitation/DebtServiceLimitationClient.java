package dk.ufst.opendebt.caseworker.limitation;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@Component
public class DebtServiceLimitationClient {

  private final WebClient webClient;

  public DebtServiceLimitationClient(
      WebClient.Builder webClientBuilder,
      @Value("${opendebt.services.debt-service.url:http://localhost:8082}") String baseUrl) {
    this.webClient = webClientBuilder.baseUrl(baseUrl).build();
  }

  @CircuitBreaker(name = "debt-service-read", fallbackMethod = "getLimitationStatusFallback")
  public LimitationPanelData getLimitationStatus(UUID fordringId) {
    return webClient
        .get()
        .uri("/debt-service/api/v1/foraeldelse/{fordringId}", fordringId)
        .retrieve()
        .bodyToMono(LimitationPanelData.class)
        .block();
  }

  @CircuitBreaker(name = "debt-service-read", fallbackMethod = "getClaimComplexMembersFallback")
  public FordringskompleksMemberListData getClaimComplexMembers(UUID kompleksId) {
    return webClient
        .get()
        .uri("/debt-service/api/v1/fordringskompleks/{kompleksId}/members", kompleksId)
        .retrieve()
        .bodyToMono(FordringskompleksMemberListData.class)
        .block();
  }

  @SuppressWarnings("unused")
  private LimitationPanelData getLimitationStatusFallback(UUID fordringId, Throwable throwable) {
    return LimitationPanelData.builder().fordringId(fordringId).build();
  }

  @SuppressWarnings("unused")
  private FordringskompleksMemberListData getClaimComplexMembersFallback(
      UUID kompleksId, Throwable throwable) {
    return FordringskompleksMemberListData.builder()
        .kompleksId(kompleksId)
        .memberFordringIds(java.util.List.of())
        .build();
  }
}
