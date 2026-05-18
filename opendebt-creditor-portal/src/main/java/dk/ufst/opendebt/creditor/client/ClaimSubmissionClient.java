package dk.ufst.opendebt.creditor.client;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import dk.ufst.opendebt.common.dto.DebtDto;
import dk.ufst.opendebt.common.exception.OpenDebtException;
import dk.ufst.opendebt.creditor.dto.ClaimSubmissionApiResponse;
import dk.ufst.opendebt.creditor.dto.ClaimSubmissionResultDto;
import dk.ufst.opendebt.creditor.dto.PortalDebtDto;
import dk.ufst.opendebt.creditor.dto.ValidationErrorDto;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class ClaimSubmissionClient {

  private final WebClient webClient;

  public ClaimSubmissionClient(
      WebClient.Builder webClientBuilder,
      @Value("${opendebt.services.debt-service.url:http://localhost:8082}") String baseUrl) {
    this.webClient = webClientBuilder.baseUrl(baseUrl).build();
  }

  @CircuitBreaker(
      name = DebtServiceClientSupport.WRITE_CIRCUIT_BREAKER,
      fallbackMethod = "createDebtFallback")
  public PortalDebtDto createDebt(PortalDebtDto request) {
    log.debug("Creating debt for creditor: {}", request.getCreditorOrgId());

    return webClient
        .post()
        .uri("/debt-service/api/v1/debts")
        .bodyValue(request)
        .retrieve()
        .onStatus(
            HttpStatusCode::is4xxClientError,
            response -> {
              if (response.statusCode() == HttpStatus.BAD_REQUEST) {
                return response
                    .bodyToMono(String.class)
                    .flatMap(
                        body ->
                            Mono.error(
                                new OpenDebtException(
                                    "Invalid debt request: " + body,
                                    "DEBT_VALIDATION_ERROR",
                                    OpenDebtException.ErrorSeverity.WARNING)));
              }
              return DebtServiceClientSupport.standardClientError(response);
            })
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response -> Mono.error(DebtServiceClientSupport.unavailableError()))
        .bodyToMono(PortalDebtDto.class)
        .block();
  }

  /**
   * Not wrapped in a circuit breaker: a 5xx or transport error must become an AFVIST result for the
   * wizard, not a recorded breaker failure with a generic fallback.
   */
  public ClaimSubmissionResultDto submitClaimWizard(PortalDebtDto request) {
    log.debug("Submitting claim via wizard for creditor: {}", request.getCreditorOrgId());

    try {
      DebtDto submitRequest = toWizardSubmitDebtDto(request);
      ClaimSubmissionApiResponse api = postClaimSubmitWithRetry(submitRequest);

      if (api == null) {
        return ClaimSubmissionResultDto.builder()
            .outcome(DebtServiceClientSupport.STATUS_AFVIST)
            .processingStatus("No response from debt-service")
            .errors(
                Collections.singletonList(
                    ValidationErrorDto.builder()
                        .errorCode(502)
                        .description("No response body from debt-service submit endpoint")
                        .build()))
            .build();
      }

      return mapClaimSubmissionApiResponse(api);
    } catch (OpenDebtException exception) {
      if ("DEBT_VALIDATION_ERROR".equals(exception.getErrorCode())) {
        return ClaimSubmissionResultDto.builder()
            .outcome(DebtServiceClientSupport.STATUS_AFVIST)
            .processingStatus(DebtServiceClientSupport.STATUS_REJECTED)
            .errors(
                Collections.singletonList(
                    ValidationErrorDto.builder()
                        .errorCode(400)
                        .description(exception.getMessage())
                        .build()))
            .build();
      }
      int errorCode =
          DebtServiceClientSupport.ERROR_CODE_UNAVAILABLE.equals(exception.getErrorCode())
              ? 503
              : 502;
      return ClaimSubmissionResultDto.builder()
          .outcome(DebtServiceClientSupport.STATUS_AFVIST)
          .processingStatus(DebtServiceClientSupport.STATUS_REJECTED)
          .errors(
              Collections.singletonList(
                  ValidationErrorDto.builder()
                      .errorCode(errorCode)
                      .description(exception.getMessage())
                      .build()))
          .build();
    } catch (Exception exception) {
      log.error("Claim wizard submit failed: {}", exception.getMessage(), exception);
      return ClaimSubmissionResultDto.builder()
          .outcome(DebtServiceClientSupport.STATUS_AFVIST)
          .processingStatus("SUBMIT_FAILED")
          .errors(
              Collections.singletonList(
                  ValidationErrorDto.builder()
                      .errorCode(503)
                      .description(
                          exception.getMessage() != null
                              ? exception.getMessage()
                              : exception.getClass().getSimpleName())
                      .build()))
          .build();
    }
  }

  private ClaimSubmissionApiResponse postClaimSubmitWithRetry(DebtDto submitRequest) {
    for (int attempt = 1; attempt <= 2; attempt++) {
      try {
        return webClient
            .post()
            .uri("/debt-service/api/v1/debts/submit")
            .header(
                DebtServiceClientSupport.CLAIM_INGRESS_PATH_HEADER,
                DebtServiceClientSupport.CLAIM_INGRESS_PATH_PORTAL)
            .bodyValue(submitRequest)
            .exchangeToMono(
                response -> {
                  HttpStatusCode status = response.statusCode();
                  int code = status.value();
                  if (code == HttpStatus.CREATED.value()
                      || code == HttpStatus.UNPROCESSABLE_ENTITY.value()) {
                    return response
                        .bodyToMono(ClaimSubmissionApiResponse.class)
                        .switchIfEmpty(
                            Mono.error(
                                new OpenDebtException(
                                    "Empty response body from debt-service submit",
                                    DebtServiceClientSupport.ERROR_CODE_CLIENT)));
                  }
                  if (status.is4xxClientError()) {
                    return response
                        .bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(
                            body ->
                                Mono.error(
                                    new OpenDebtException(
                                        "Debt service client error: " + body,
                                        DebtServiceClientSupport.ERROR_CODE_CLIENT)));
                  }
                  if (status.is5xxServerError()) {
                    return Mono.error(DebtServiceClientSupport.unavailableError());
                  }
                  return response.createException().flatMap(Mono::error);
                })
            .block();
      } catch (OpenDebtException exception) {
        if (attempt < 2
            && DebtServiceClientSupport.ERROR_CODE_UNAVAILABLE.equals(exception.getErrorCode())) {
          log.warn(
              "Debt-service claim submit returned 5xx (attempt {}); retrying once after short delay",
              attempt);
          sleepQuietly(750);
          continue;
        }
        throw exception;
      }
    }
    throw new IllegalStateException("postClaimSubmitWithRetry: exhausted attempts without result");
  }

  private static void sleepQuietly(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
    }
  }

  private static DebtDto toWizardSubmitDebtDto(PortalDebtDto request) {
    return DebtDto.builder()
        .debtorId(
            request.getDebtorPersonId() != null ? request.getDebtorPersonId().toString() : null)
        .creditorId(
            request.getCreditorOrgId() != null ? request.getCreditorOrgId().toString() : null)
        .debtTypeCode(request.getDebtTypeCode())
        .principalAmount(request.getPrincipalAmount())
        .outstandingBalance(request.getOutstandingBalance())
        .dueDate(request.getDueDate())
        .description(request.getDescription())
        .claimArt("INDR")
        .creditorReference(request.getCreditorReference())
        .periodFrom(request.getPeriodFrom())
        .periodTo(request.getPeriodTo())
        .inceptionDate(request.getInceptionDate())
        .limitationDate(request.getLimitationDate())
        .estateProcessing(request.getEstateProcessing())
        .judgmentDate(request.getJudgmentDate())
        .settlementDate(request.getSettlementDate())
        .interestRule(request.getInterestRule())
        .interestRateCode(request.getInterestRateCode())
        .additionalInterestRate(request.getAdditionalInterestRate())
        .claimNote(request.getClaimNote())
        .customerNote(request.getCustomerNote())
        .build();
  }

  private ClaimSubmissionResultDto mapClaimSubmissionApiResponse(ClaimSubmissionApiResponse api) {
    if (api.getOutcome() == ClaimSubmissionApiResponse.Outcome.AFVIST) {
      java.util.List<ValidationErrorDto> validationErrors = Collections.emptyList();
      if (api.getErrors() != null && !api.getErrors().isEmpty()) {
        validationErrors =
            api.getErrors().stream()
                .map(
                    error ->
                        ValidationErrorDto.builder()
                            .errorCode(0)
                            .description(
                                (error.getErrorCode() != null ? error.getErrorCode() : "")
                                    + ": "
                                    + (error.getDescription() != null
                                        ? error.getDescription()
                                        : ""))
                            .build())
                .toList();
      }
      if (validationErrors.isEmpty()) {
        validationErrors =
            Collections.singletonList(
                ValidationErrorDto.builder()
                    .errorCode(422)
                    .description("Claim rejected by debt-service with no error details in response")
                    .build());
      }
      return ClaimSubmissionResultDto.builder()
          .outcome(DebtServiceClientSupport.STATUS_AFVIST)
          .processingStatus(DebtServiceClientSupport.STATUS_REJECTED)
          .errors(validationErrors)
          .build();
    }

    String outcomeName = api.getOutcome() != null ? api.getOutcome().name() : "UDFOERT";
    return ClaimSubmissionResultDto.builder()
        .outcome(outcomeName)
        .claimId(api.getClaimId())
        .processingStatus(api.getOutcome() != null ? outcomeName : "ACCEPTED")
        .build();
  }

  private PortalDebtDto createDebtFallback(PortalDebtDto request, Throwable throwable) {
    if (DebtServiceClientSupport.isClient4xx(throwable)) {
      throw (RuntimeException) throwable;
    }
    log.warn("Circuit breaker fallback triggered for createDebt: {}", throwable.getMessage());
    return null;
  }
}
