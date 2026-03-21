package dk.ufst.opendebt.debtservice.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/** Client for case-service. Used to auto-assign debts to cases after acceptance (UDFOERT). */
@Slf4j
@Component
public class CaseServiceClient {

  private final WebClient webClient;

  public CaseServiceClient(
      WebClient.Builder webClientBuilder,
      @Value("${opendebt.services.case-service.url:http://localhost:8081}") String baseUrl) {
    this.webClient = webClientBuilder.baseUrl(baseUrl).build();
  }

  @CircuitBreaker(name = "case-service", fallbackMethod = "assignDebtToCaseFallback")
  public CaseAssignmentResult assignDebtToCase(String debtId, String debtorPersonId) {
    log.debug("Assigning debt {} for debtor {} to case", debtId, debtorPersonId);

    AssignDebtRequest request =
        new AssignDebtRequest(UUID.fromString(debtId), UUID.fromString(debtorPersonId));

    return webClient
        .post()
        .uri("/case-service/api/v1/cases/assign-debt")
        .bodyValue(request)
        .retrieve()
        .bodyToMono(CaseAssignmentResult.class)
        .block();
  }

  private CaseAssignmentResult assignDebtToCaseFallback(
      String debtId, String debtorPersonId, Throwable t) {
    if (t
            instanceof
            org.springframework.web.reactive.function.client.WebClientResponseException wcre
        && wcre.getStatusCode().is4xxClientError()) {
      throw wcre;
    }
    throw new RuntimeException("Case service unavailable: " + t.getMessage(), t);
  }

  @Data
  public static class AssignDebtRequest {
    private final UUID debtId;
    private final UUID debtorPersonId;
  }

  @Data
  public static class CaseAssignmentResult {
    private UUID caseId;
    private String caseNumber;
    private boolean newCase;
  }
}
