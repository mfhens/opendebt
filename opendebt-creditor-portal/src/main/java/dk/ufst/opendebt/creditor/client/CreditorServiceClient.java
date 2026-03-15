package dk.ufst.opendebt.creditor.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import dk.ufst.opendebt.common.exception.OpenDebtException;
import dk.ufst.opendebt.creditor.dto.AccessResolutionRequest;
import dk.ufst.opendebt.creditor.dto.AccessResolutionResponse;
import dk.ufst.opendebt.creditor.dto.PortalCreditorDto;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class CreditorServiceClient {

  private final WebClient webClient;

  public CreditorServiceClient(
      WebClient.Builder webClientBuilder,
      @Value("${opendebt.services.creditor-service.url:http://localhost:8092}") String baseUrl) {
    this.webClient = webClientBuilder.baseUrl(baseUrl).build();
  }

  public PortalCreditorDto getByCreditorOrgId(UUID creditorOrgId) {
    log.debug("Fetching creditor by orgId: {}", creditorOrgId);

    return webClient
        .get()
        .uri("/creditor-service/api/v1/creditors/{creditorOrgId}", creditorOrgId)
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
                                  "Creditor service client error: " + body,
                                  "CREDITOR_CLIENT_ERROR")));
            })
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        "Creditor service unavailable",
                        "CREDITOR_SERVICE_UNAVAILABLE",
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(PortalCreditorDto.class)
        .block();
  }

  public AccessResolutionResponse resolveAccess(AccessResolutionRequest request) {
    log.debug("Resolving access for channel: {}", request.getChannelType());

    return webClient
        .post()
        .uri("/creditor-service/api/v1/creditors/access/resolve")
        .bodyValue(request)
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
                                    "Access resolution failed: " + body,
                                    "ACCESS_RESOLUTION_ERROR"))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        "Creditor service unavailable",
                        "CREDITOR_SERVICE_UNAVAILABLE",
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(AccessResolutionResponse.class)
        .block();
  }
}
