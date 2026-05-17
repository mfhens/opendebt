package dk.ufst.opendebt.debtservice.limitation.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import dk.ufst.opendebt.common.exception.OpenDebtException;
import dk.ufst.opendebt.debtservice.client.JwtBearerPropagationFilter;
import dk.ufst.opendebt.debtservice.limitation.client.dto.CreateObjectionWorkflowRequest;
import dk.ufst.opendebt.debtservice.limitation.client.dto.ObjectionDecisionRequest;
import dk.ufst.opendebt.debtservice.limitation.client.dto.ObjectionWorkflowResult;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@Component
public class LimitationObjectionWorkflowClient {

  private final WebClient webClient;

  public LimitationObjectionWorkflowClient(
      WebClient.Builder webClientBuilder,
      @Value("${opendebt.services.case-service.url:http://localhost:8081}") String caseServiceUrl) {
    this.webClient =
        webClientBuilder
            .filter(JwtBearerPropagationFilter.create())
            .baseUrl(caseServiceUrl)
            .build();
  }

  @CircuitBreaker(name = "case-service", fallbackMethod = "createWorkflowFallback")
  public ObjectionWorkflowResult createWorkflow(CreateObjectionWorkflowRequest request) {
    return webClient
        .post()
        .uri("/case-service/api/internal/v1/limitation-objections")
        .bodyValue(request)
        .retrieve()
        .bodyToMono(ObjectionWorkflowResult.class)
        .block();
  }

  @CircuitBreaker(name = "case-service", fallbackMethod = "recordDecisionFallback")
  public ObjectionWorkflowResult recordDecision(
      UUID indsigelsesId, ObjectionDecisionRequest request) {
    return webClient
        .put()
        .uri(
            "/case-service/api/internal/v1/limitation-objections/{indsigelsesId}/decision",
            indsigelsesId)
        .bodyValue(request)
        .retrieve()
        .bodyToMono(ObjectionWorkflowResult.class)
        .block();
  }

  @SuppressWarnings("unused")
  private ObjectionWorkflowResult createWorkflowFallback(
      CreateObjectionWorkflowRequest request, Throwable throwable) {
    throw new OpenDebtException(
        "Case service unavailable: " + throwable.getMessage(),
        "CASE_SERVICE_UNAVAILABLE",
        OpenDebtException.ErrorSeverity.CRITICAL);
  }

  @SuppressWarnings("unused")
  private ObjectionWorkflowResult recordDecisionFallback(
      UUID indsigelsesId, ObjectionDecisionRequest request, Throwable throwable) {
    throw new OpenDebtException(
        "Case service unavailable: " + throwable.getMessage(),
        "CASE_SERVICE_UNAVAILABLE",
        OpenDebtException.ErrorSeverity.CRITICAL);
  }
}
