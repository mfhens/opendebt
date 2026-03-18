package dk.ufst.opendebt.creditor.client;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import dk.ufst.opendebt.common.exception.OpenDebtException;
import dk.ufst.opendebt.creditor.dto.AdjustmentReceiptDto;
import dk.ufst.opendebt.creditor.dto.ClaimAdjustmentRequestDto;
import dk.ufst.opendebt.creditor.dto.ClaimCountsDto;
import dk.ufst.opendebt.creditor.dto.ClaimDetailDto;
import dk.ufst.opendebt.creditor.dto.ClaimListItemDto;
import dk.ufst.opendebt.creditor.dto.ClaimSubmissionResultDto;
import dk.ufst.opendebt.creditor.dto.HearingApproveRequestDto;
import dk.ufst.opendebt.creditor.dto.HearingClaimDetailDto;
import dk.ufst.opendebt.creditor.dto.HearingClaimListItemDto;
import dk.ufst.opendebt.creditor.dto.HearingWithdrawRequestDto;
import dk.ufst.opendebt.creditor.dto.PortalDebtDto;
import dk.ufst.opendebt.creditor.dto.RejectedClaimDetailDto;
import dk.ufst.opendebt.creditor.dto.ValidationErrorDto;

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

  public RestPage<PortalDebtDto> listDebts(UUID creditorOrgId) {
    log.debug("Listing debts for creditor: {}", creditorOrgId);

    return webClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("/debt-service/api/v1/debts")
                    .queryParam("creditorId", creditorOrgId)
                    .build())
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
                                    "Debt service client error: " + body, "DEBT_CLIENT_ERROR"))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        "Debt service unavailable",
                        "DEBT_SERVICE_UNAVAILABLE",
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(new ParameterizedTypeReference<RestPage<PortalDebtDto>>() {})
        .block();
  }

  /** Fetches summary claim counts for the given creditor from debt-service. */
  public ClaimCountsDto getClaimCounts(UUID creditorOrgId) {
    log.debug("Fetching claim counts for creditor: {}", creditorOrgId);

    return webClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("/debt-service/api/v1/debts/counts")
                    .queryParam("creditorId", creditorOrgId)
                    .build())
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
                                    "Debt service client error: " + body, "DEBT_CLIENT_ERROR"))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        "Debt service unavailable",
                        "DEBT_SERVICE_UNAVAILABLE",
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(ClaimCountsDto.class)
        .block();
  }

  /**
   * Lists claims in recovery for the given creditor with pagination, sorting, search, and date
   * range filtering.
   */
  public RestPage<ClaimListItemDto> listClaimsInRecovery(
      UUID creditorOrgId,
      int page,
      int size,
      String sortBy,
      String sortDirection,
      String searchQuery,
      String searchType,
      LocalDate dateFrom,
      LocalDate dateTo) {
    log.debug("Listing claims in recovery for creditor: {}, page: {}", creditorOrgId, page);

    return webClient
        .get()
        .uri(
            uriBuilder -> {
              uriBuilder
                  .path("/debt-service/api/v1/debts/claims")
                  .queryParam("creditorId", creditorOrgId)
                  .queryParam("status", "IN_RECOVERY")
                  .queryParam("excludeZeroBalance", true)
                  .queryParam("page", page)
                  .queryParam("size", size);
              if (sortBy != null && !sortBy.isBlank()) {
                uriBuilder.queryParam("sortBy", sortBy);
                uriBuilder.queryParam(
                    "sortDirection", sortDirection != null ? sortDirection : "asc");
              }
              if (searchQuery != null && !searchQuery.isBlank()) {
                uriBuilder.queryParam("searchQuery", searchQuery);
                if (searchType != null && !searchType.isBlank()) {
                  uriBuilder.queryParam("searchType", searchType);
                }
              }
              if (dateFrom != null) {
                uriBuilder.queryParam("dateFrom", dateFrom.toString());
              }
              if (dateTo != null) {
                uriBuilder.queryParam("dateTo", dateTo.toString());
              }
              return uriBuilder.build();
            })
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
                                    "Debt service client error: " + body, "DEBT_CLIENT_ERROR"))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        "Debt service unavailable",
                        "DEBT_SERVICE_UNAVAILABLE",
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(new ParameterizedTypeReference<RestPage<ClaimListItemDto>>() {})
        .block();
  }

  /**
   * Lists zero-balance claims for the given creditor with pagination, sorting, search, and date
   * range filtering.
   */
  public RestPage<ClaimListItemDto> listZeroBalanceClaims(
      UUID creditorOrgId,
      int page,
      int size,
      String sortBy,
      String sortDirection,
      String searchQuery,
      String searchType,
      LocalDate dateFrom,
      LocalDate dateTo) {
    log.debug("Listing zero-balance claims for creditor: {}, page: {}", creditorOrgId, page);

    return webClient
        .get()
        .uri(
            uriBuilder -> {
              uriBuilder
                  .path("/debt-service/api/v1/debts/claims")
                  .queryParam("creditorId", creditorOrgId)
                  .queryParam("status", "ZERO_BALANCE")
                  .queryParam("page", page)
                  .queryParam("size", size);
              if (sortBy != null && !sortBy.isBlank()) {
                uriBuilder.queryParam("sortBy", sortBy);
                uriBuilder.queryParam(
                    "sortDirection", sortDirection != null ? sortDirection : "asc");
              }
              if (searchQuery != null && !searchQuery.isBlank()) {
                uriBuilder.queryParam("searchQuery", searchQuery);
                if (searchType != null && !searchType.isBlank()) {
                  uriBuilder.queryParam("searchType", searchType);
                }
              }
              if (dateFrom != null) {
                uriBuilder.queryParam("dateFrom", dateFrom.toString());
              }
              if (dateTo != null) {
                uriBuilder.queryParam("dateTo", dateTo.toString());
              }
              return uriBuilder.build();
            })
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
                                    "Debt service client error: " + body, "DEBT_CLIENT_ERROR"))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        "Debt service unavailable",
                        "DEBT_SERVICE_UNAVAILABLE",
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(new ParameterizedTypeReference<RestPage<ClaimListItemDto>>() {})
        .block();
  }

  /**
   * Fetches claim counts for the given creditor filtered by date range. Returns both recovery and
   * zero-balance counts.
   */
  public ClaimCountsDto getClaimCountsForDateRange(
      UUID creditorOrgId, LocalDate dateFrom, LocalDate dateTo) {
    log.debug("Fetching claim counts with date range for creditor: {}", creditorOrgId);

    return webClient
        .get()
        .uri(
            uriBuilder -> {
              uriBuilder
                  .path("/debt-service/api/v1/debts/counts")
                  .queryParam("creditorId", creditorOrgId);
              if (dateFrom != null) {
                uriBuilder.queryParam("dateFrom", dateFrom.toString());
              }
              if (dateTo != null) {
                uriBuilder.queryParam("dateTo", dateTo.toString());
              }
              return uriBuilder.build();
            })
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
                                    "Debt service client error: " + body, "DEBT_CLIENT_ERROR"))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        "Debt service unavailable",
                        "DEBT_SERVICE_UNAVAILABLE",
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(ClaimCountsDto.class)
        .block();
  }

  /** Fetches detailed claim information for the claim detail view. */
  public ClaimDetailDto getClaimDetail(UUID claimId) {
    log.debug("Fetching claim detail for claimId: {}", claimId);

    return webClient
        .get()
        .uri(
            uriBuilder -> uriBuilder.path("/debt-service/api/v1/debts/{id}/details").build(claimId))
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
                                    "Debt service client error: " + body, "DEBT_CLIENT_ERROR"))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        "Debt service unavailable",
                        "DEBT_SERVICE_UNAVAILABLE",
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(ClaimDetailDto.class)
        .block();
  }

  /** Fetches a receipt for a claim operation by delivery ID. */
  public byte[] getReceipt(UUID claimId, String deliveryId) {
    log.debug("Fetching receipt for claimId: {}, deliveryId: {}", claimId, deliveryId);

    return webClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("/debt-service/api/v1/debts/{id}/receipts/{deliveryId}")
                    .build(claimId, deliveryId))
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
                                    "Debt service client error: " + body, "DEBT_CLIENT_ERROR"))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        "Debt service unavailable",
                        "DEBT_SERVICE_UNAVAILABLE",
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(byte[].class)
        .block();
  }

  /**
   * Lists claims in hearing for the given creditor with pagination, sorting, search, and date range
   * filtering.
   */
  public RestPage<HearingClaimListItemDto> listHearingClaims(
      UUID creditorOrgId,
      int page,
      int size,
      String sortBy,
      String sortDirection,
      String searchQuery,
      String searchType,
      LocalDate dateFrom,
      LocalDate dateTo) {
    log.debug("Listing hearing claims for creditor: {}, page: {}", creditorOrgId, page);

    return webClient
        .get()
        .uri(
            uriBuilder -> {
              uriBuilder
                  .path("/debt-service/api/v1/debts/claims")
                  .queryParam("creditorId", creditorOrgId)
                  .queryParam("status", "HEARING")
                  .queryParam("page", page)
                  .queryParam("size", size);
              if (sortBy != null && !sortBy.isBlank()) {
                uriBuilder.queryParam("sortBy", sortBy);
                uriBuilder.queryParam(
                    "sortDirection", sortDirection != null ? sortDirection : "asc");
              }
              if (searchQuery != null && !searchQuery.isBlank()) {
                uriBuilder.queryParam("searchQuery", searchQuery);
                if (searchType != null && !searchType.isBlank()) {
                  uriBuilder.queryParam("searchType", searchType);
                }
              }
              if (dateFrom != null) {
                uriBuilder.queryParam("dateFrom", dateFrom.toString());
              }
              if (dateTo != null) {
                uriBuilder.queryParam("dateTo", dateTo.toString());
              }
              return uriBuilder.build();
            })
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
                                    "Debt service client error: " + body, "DEBT_CLIENT_ERROR"))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        "Debt service unavailable",
                        "DEBT_SERVICE_UNAVAILABLE",
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(new ParameterizedTypeReference<RestPage<HearingClaimListItemDto>>() {})
        .block();
  }

  /** Fetches detailed hearing claim information for the hearing detail view. */
  public HearingClaimDetailDto getHearingClaimDetail(UUID claimId) {
    log.debug("Fetching hearing claim detail for claimId: {}", claimId);

    return webClient
        .get()
        .uri(
            uriBuilder -> uriBuilder.path("/debt-service/api/v1/debts/{id}/hearing").build(claimId))
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
                                    "Debt service client error: " + body, "DEBT_CLIENT_ERROR"))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        "Debt service unavailable",
                        "DEBT_SERVICE_UNAVAILABLE",
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(HearingClaimDetailDto.class)
        .block();
  }

  /** Approves a hearing claim with a written justification. */
  public void approveHearingClaim(UUID claimId, HearingApproveRequestDto request) {
    log.debug("Approving hearing claim: {}", claimId);

    webClient
        .post()
        .uri(
            uriBuilder ->
                uriBuilder.path("/debt-service/api/v1/debts/{id}/hearing/approve").build(claimId))
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
                                    "Debt service client error: " + body, "DEBT_CLIENT_ERROR"))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        "Debt service unavailable",
                        "DEBT_SERVICE_UNAVAILABLE",
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(Void.class)
        .block();
  }

  /** Withdraws a hearing claim with a reason. */
  public void withdrawHearingClaim(UUID claimId, HearingWithdrawRequestDto request) {
    log.debug("Withdrawing hearing claim: {}", claimId);

    webClient
        .post()
        .uri(
            uriBuilder ->
                uriBuilder.path("/debt-service/api/v1/debts/{id}/hearing/withdraw").build(claimId))
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
                                    "Debt service client error: " + body, "DEBT_CLIENT_ERROR"))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        "Debt service unavailable",
                        "DEBT_SERVICE_UNAVAILABLE",
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(Void.class)
        .block();
  }

  /**
   * Lists rejected claims for the given creditor with pagination, sorting, search, and date range
   * filtering.
   */
  public RestPage<ClaimListItemDto> listRejectedClaims(
      UUID creditorOrgId,
      int page,
      int size,
      String sortBy,
      String sortDirection,
      String searchQuery,
      String searchType,
      LocalDate dateFrom,
      LocalDate dateTo) {
    log.debug("Listing rejected claims for creditor: {}, page: {}", creditorOrgId, page);

    return webClient
        .get()
        .uri(
            uriBuilder -> {
              uriBuilder
                  .path("/debt-service/api/v1/debts/claims")
                  .queryParam("creditorId", creditorOrgId)
                  .queryParam("status", "REJECTED")
                  .queryParam("page", page)
                  .queryParam("size", size);
              if (sortBy != null && !sortBy.isBlank()) {
                uriBuilder.queryParam("sortBy", sortBy);
                uriBuilder.queryParam(
                    "sortDirection", sortDirection != null ? sortDirection : "asc");
              }
              if (searchQuery != null && !searchQuery.isBlank()) {
                uriBuilder.queryParam("searchQuery", searchQuery);
                if (searchType != null && !searchType.isBlank()) {
                  uriBuilder.queryParam("searchType", searchType);
                }
              }
              if (dateFrom != null) {
                uriBuilder.queryParam("dateFrom", dateFrom.toString());
              }
              if (dateTo != null) {
                uriBuilder.queryParam("dateTo", dateTo.toString());
              }
              return uriBuilder.build();
            })
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
                                    "Debt service client error: " + body, "DEBT_CLIENT_ERROR"))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        "Debt service unavailable",
                        "DEBT_SERVICE_UNAVAILABLE",
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(new ParameterizedTypeReference<RestPage<ClaimListItemDto>>() {})
        .block();
  }

  /** Fetches detailed rejection information for a rejected claim. */
  public RejectedClaimDetailDto getRejectedClaimDetail(UUID claimId) {
    log.debug("Fetching rejected claim detail for claimId: {}", claimId);

    return webClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder.path("/debt-service/api/v1/debts/{id}/rejection").build(claimId))
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
                                    "Debt service client error: " + body, "DEBT_CLIENT_ERROR"))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        "Debt service unavailable",
                        "DEBT_SERVICE_UNAVAILABLE",
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(RejectedClaimDetailDto.class)
        .block();
  }

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
              return response
                  .bodyToMono(String.class)
                  .flatMap(
                      body ->
                          Mono.error(
                              new OpenDebtException(
                                  "Debt service client error: " + body, "DEBT_CLIENT_ERROR")));
            })
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        "Debt service unavailable",
                        "DEBT_SERVICE_UNAVAILABLE",
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(PortalDebtDto.class)
        .block();
  }

  /**
   * Submits a new claim via the portal wizard and returns a structured result indicating the
   * outcome (UDFOERT, AFVIST, or HOERING). Unlike {@link #createDebt}, this method captures
   * validation errors instead of throwing, so the wizard can display them inline.
   */
  public ClaimSubmissionResultDto submitClaimWizard(PortalDebtDto request) {
    log.debug("Submitting claim via wizard for creditor: {}", request.getCreditorOrgId());

    try {
      // Build a full DebtDto for the submit endpoint
      java.util.Map<String, Object> submitRequest = new java.util.LinkedHashMap<>();
      submitRequest.put(
          "debtorId",
          request.getDebtorPersonId() != null ? request.getDebtorPersonId().toString() : null);
      submitRequest.put(
          "creditorId",
          request.getCreditorOrgId() != null ? request.getCreditorOrgId().toString() : null);
      submitRequest.put("debtTypeCode", request.getDebtTypeCode());
      submitRequest.put("principalAmount", request.getPrincipalAmount());
      submitRequest.put("outstandingBalance", request.getOutstandingBalance());
      submitRequest.put("dueDate", request.getDueDate());
      submitRequest.put("description", request.getDescription());
      submitRequest.put("claimArt", "INDR");

      java.util.Map<String, Object> response =
          webClient
              .post()
              .uri("/debt-service/api/v1/debts/submit")
              .bodyValue(submitRequest)
              .retrieve()
              .bodyToMono(
                  new org.springframework.core.ParameterizedTypeReference<
                      java.util.Map<String, Object>>() {})
              .block();

      if (response == null) {
        return ClaimSubmissionResultDto.builder()
            .outcome("AFVIST")
            .processingStatus("No response from debt-service")
            .errors(java.util.Collections.emptyList())
            .build();
      }

      String outcome = (String) response.get("outcome");

      if ("AFVIST".equals(outcome)) {
        @SuppressWarnings("unchecked")
        var rawErrors = (java.util.List<java.util.Map<String, Object>>) response.get("errors");
        java.util.List<ValidationErrorDto> validationErrors = java.util.Collections.emptyList();
        if (rawErrors != null) {
          validationErrors =
              rawErrors.stream()
                  .map(
                      e ->
                          ValidationErrorDto.builder()
                              .errorCode(0)
                              .description(
                                  e.getOrDefault("errorCode", "")
                                      + ": "
                                      + e.getOrDefault("description", ""))
                              .build())
                  .toList();
        }
        return ClaimSubmissionResultDto.builder()
            .outcome("AFVIST")
            .processingStatus("REJECTED")
            .errors(validationErrors)
            .build();
      }

      String claimIdStr =
          response.get("claimId") != null ? response.get("claimId").toString() : null;
      UUID claimId = claimIdStr != null ? UUID.fromString(claimIdStr) : null;

      return ClaimSubmissionResultDto.builder()
          .outcome(outcome != null ? outcome : "UDFOERT")
          .claimId(claimId)
          .processingStatus(outcome != null ? outcome : "ACCEPTED")
          .build();
    } catch (OpenDebtException ex) {
      if ("DEBT_VALIDATION_ERROR".equals(ex.getErrorCode())) {
        return ClaimSubmissionResultDto.builder()
            .outcome("AFVIST")
            .processingStatus("REJECTED")
            .errors(
                java.util.Collections.singletonList(
                    ValidationErrorDto.builder()
                        .errorCode(400)
                        .description(ex.getMessage())
                        .build()))
            .build();
      }
      throw ex;
    }
  }

  /** Submits a claim adjustment (write-up or write-down) to debt-service (petition 034). */
  public AdjustmentReceiptDto submitAdjustment(UUID claimId, ClaimAdjustmentRequestDto request) {
    log.debug(
        "Submitting adjustment for claimId: {}, type: {}", claimId, request.getAdjustmentType());

    return webClient
        .put()
        .uri(
            uriBuilder ->
                uriBuilder.path("/debt-service/api/v1/debts/{id}/adjustments").build(claimId))
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
                                    "Invalid adjustment request: " + body,
                                    "ADJUSTMENT_VALIDATION_ERROR",
                                    OpenDebtException.ErrorSeverity.WARNING)));
              }
              return response
                  .bodyToMono(String.class)
                  .flatMap(
                      body ->
                          Mono.error(
                              new OpenDebtException(
                                  "Debt service client error: " + body, "DEBT_CLIENT_ERROR")));
            })
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        "Debt service unavailable",
                        "DEBT_SERVICE_UNAVAILABLE",
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(AdjustmentReceiptDto.class)
        .block();
  }
}
