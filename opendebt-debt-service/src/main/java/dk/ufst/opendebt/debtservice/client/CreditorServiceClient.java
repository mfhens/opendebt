package dk.ufst.opendebt.debtservice.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import dk.ufst.opendebt.common.exception.OpenDebtException;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class CreditorServiceClient {

  private final WebClient webClient;
  private final String creditorServiceUrl;

  public CreditorServiceClient(
      WebClient.Builder webClientBuilder,
      @Value("${opendebt.services.creditor-service.url:http://localhost:8092}") String baseUrl) {
    this.creditorServiceUrl = baseUrl;
    this.webClient = webClientBuilder.baseUrl(baseUrl).build();
  }

  public ValidateActionResponse validateAction(UUID creditorOrgId, ValidateActionRequest request) {
    log.debug("Validating action {} for creditor {}", request.getRequestedAction(), creditorOrgId);

    return webClient
        .post()
        .uri("/creditor-service/api/v1/creditors/{creditorOrgId}/validate-action", creditorOrgId)
        .bodyValue(request)
        .retrieve()
        .onStatus(
            HttpStatusCode::is4xxClientError,
            response -> {
              if (response.statusCode() == HttpStatus.NOT_FOUND) {
                return Mono.error(
                    new OpenDebtException(
                        "Creditor not found: " + creditorOrgId,
                        "CREDITOR_NOT_FOUND",
                        OpenDebtException.ErrorSeverity.WARNING));
              }
              return response
                  .bodyToMono(String.class)
                  .flatMap(
                      body ->
                          Mono.error(
                              new OpenDebtException(
                                  "Creditor validation failed: " + body,
                                  "CREDITOR_VALIDATION_ERROR")));
            })
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        "Creditor service unavailable",
                        "CREDITOR_SERVICE_UNAVAILABLE",
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(ValidateActionResponse.class)
        .block();
  }

  public boolean isCreditorAllowedToCreateClaim(UUID creditorOrgId) {
    ValidateActionRequest request =
        ValidateActionRequest.builder().requestedAction(CreditorAction.CREATE_CLAIM).build();
    ValidateActionResponse response = validateAction(creditorOrgId, request);
    return response.isAllowed();
  }

  public boolean isCreditorAllowedToUpdateClaim(UUID creditorOrgId) {
    ValidateActionRequest request =
        ValidateActionRequest.builder().requestedAction(CreditorAction.UPDATE_CLAIM).build();
    ValidateActionResponse response = validateAction(creditorOrgId, request);
    return response.isAllowed();
  }
}
