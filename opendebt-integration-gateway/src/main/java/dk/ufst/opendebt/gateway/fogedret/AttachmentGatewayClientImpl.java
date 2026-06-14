package dk.ufst.opendebt.gateway.fogedret;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class AttachmentGatewayClientImpl implements AttachmentGatewayClient {

  private final WebClient webClient;

  public AttachmentGatewayClientImpl(
      WebClient.Builder webClientBuilder,
      @Value("${opendebt.services.debt-service.url:http://localhost:8082}") String baseUrl) {
    this.webClient = webClientBuilder.baseUrl(baseUrl).build();
  }

  @Override
  public void dispatchToDebtService(FogedretAttachmentDispatchRequest request) {
    webClient
        .post()
        .uri("/debt-service/internal/debtors/{debtorId}/attachment-workflows/{workflowId}/dispatch", request.getDebtorId(), request.getWorkflowId())
        .retrieve()
        .toBodilessEntity()
        .block();
  }

  @Override
  public void callbackToDebtService(FogedretAttachmentCallbackRequest request) {
    webClient
        .post()
        .uri("/debt-service/internal/debtors/{debtorId}/attachment-workflows/callbacks", request.getDebtorId())
        .bodyValue(
            Map.of(
                "workflowReference", request.getWorkflowReference(),
                "status", request.getStatus(),
                "outcomeDate", request.getOutcomeDate(),
                "reasonCode", request.getReasonCode(),
                "callbackMessageId", request.getCallbackMessageId(),
                "externalCaseNumber", request.getExternalCaseNumber()))
        .retrieve()
        .toBodilessEntity()
        .block();
  }
}
