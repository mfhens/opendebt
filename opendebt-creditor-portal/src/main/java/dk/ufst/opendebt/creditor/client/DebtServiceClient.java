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

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class DebtServiceClient {

  private static final String CIRCUIT_BREAKER_READ = "debt-service-read";
  private static final String ERR_CLIENT_MSG = "Debt service client error: ";
  private static final String ERR_UNAVAILABLE_MSG = "Debt service unavailable";
  private static final String ERROR_CODE_CLIENT = "DEBT_CLIENT_ERROR";
  private static final String ERROR_CODE_UNAVAILABLE = "DEBT_SERVICE_UNAVAILABLE";
  private static final String CLAIMS_PATH = "/debt-service/api/v1/debts/claims";
  private static final String PARAM_CREDITOR_ID = "creditorId";
  private static final String PARAM_SEARCH_QUERY = "searchQuery";
  private static final String PARAM_SEARCH_TYPE = "searchType";
  private static final String PARAM_SORT_BY = "sortBy";
  private static final String PARAM_SORT_DIRECTION = "sortDirection";
  private static final String PARAM_DATE_FROM = "dateFrom";
  private static final String PARAM_DATE_TO = "dateTo";
  private static final String PARAM_STATUS = "status";
  private static final String STATUS_REJECTED = "REJECTED";
  private static final String STATUS_AFVIST = "AFVIST";

  private final WebClient webClient;

  public DebtServiceClient(
      WebClient.Builder webClientBuilder,
      @Value("${opendebt.services.debt-service.url:http://localhost:8082}") String baseUrl) {
    this.webClient = webClientBuilder.baseUrl(baseUrl).build();
  }

  @CircuitBreaker(name = CIRCUIT_BREAKER_READ, fallbackMethod = "listDebtsFallback")
  @Retry(name = CIRCUIT_BREAKER_READ)
  public RestPage<PortalDebtDto> listDebts(UUID creditorOrgId) {
    log.debug("Listing debts for creditor: {}", creditorOrgId);

    return webClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("/debt-service/api/v1/debts")
                    .queryParam(PARAM_CREDITOR_ID, creditorOrgId)
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
                                new OpenDebtException(ERR_CLIENT_MSG + body, ERROR_CODE_CLIENT))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        ERR_UNAVAILABLE_MSG,
                        ERROR_CODE_UNAVAILABLE,
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(new ParameterizedTypeReference<RestPage<PortalDebtDto>>() {})
        .block();
  }

  /** Fetches summary claim counts for the given creditor from debt-service. */
  @CircuitBreaker(name = CIRCUIT_BREAKER_READ, fallbackMethod = "getClaimCountsFallback")
  @Retry(name = CIRCUIT_BREAKER_READ)
  public ClaimCountsDto getClaimCounts(UUID creditorOrgId) {
    log.debug("Fetching claim counts for creditor: {}", creditorOrgId);

    return webClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("/debt-service/api/v1/debts/counts")
                    .queryParam(PARAM_CREDITOR_ID, creditorOrgId)
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
                                new OpenDebtException(ERR_CLIENT_MSG + body, ERROR_CODE_CLIENT))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        ERR_UNAVAILABLE_MSG,
                        ERROR_CODE_UNAVAILABLE,
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(ClaimCountsDto.class)
        .block();
  }

  /**
   * Lists claims in recovery for the given creditor with pagination, sorting, search, and date
   * range filtering.
   */
  @CircuitBreaker(name = CIRCUIT_BREAKER_READ, fallbackMethod = "listClaimsInRecoveryFallback")
  @Retry(name = CIRCUIT_BREAKER_READ)
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
                  .path(CLAIMS_PATH)
                  .queryParam(PARAM_CREDITOR_ID, creditorOrgId)
                  .queryParam(PARAM_STATUS, "IN_RECOVERY")
                  .queryParam("excludeZeroBalance", true)
                  .queryParam("page", page)
                  .queryParam("size", size);
              if (sortBy != null && !sortBy.isBlank()) {
                uriBuilder.queryParam(PARAM_SORT_BY, sortBy);
                uriBuilder.queryParam(
                    PARAM_SORT_DIRECTION, sortDirection != null ? sortDirection : "asc");
              }
              if (searchQuery != null && !searchQuery.isBlank()) {
                uriBuilder.queryParam(PARAM_SEARCH_QUERY, searchQuery);
                if (searchType != null && !searchType.isBlank()) {
                  uriBuilder.queryParam(PARAM_SEARCH_TYPE, searchType);
                }
              }
              if (dateFrom != null) {
                uriBuilder.queryParam(PARAM_DATE_FROM, dateFrom.toString());
              }
              if (dateTo != null) {
                uriBuilder.queryParam(PARAM_DATE_TO, dateTo.toString());
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
                                new OpenDebtException(ERR_CLIENT_MSG + body, ERROR_CODE_CLIENT))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        ERR_UNAVAILABLE_MSG,
                        ERROR_CODE_UNAVAILABLE,
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(new ParameterizedTypeReference<RestPage<ClaimListItemDto>>() {})
        .block();
  }

  /**
   * Lists zero-balance claims for the given creditor with pagination, sorting, search, and date
   * range filtering.
   */
  @CircuitBreaker(name = CIRCUIT_BREAKER_READ, fallbackMethod = "listZeroBalanceClaimsFallback")
  @Retry(name = CIRCUIT_BREAKER_READ)
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
                  .path(CLAIMS_PATH)
                  .queryParam(PARAM_CREDITOR_ID, creditorOrgId)
                  .queryParam(PARAM_STATUS, "ZERO_BALANCE")
                  .queryParam("page", page)
                  .queryParam("size", size);
              if (sortBy != null && !sortBy.isBlank()) {
                uriBuilder.queryParam(PARAM_SORT_BY, sortBy);
                uriBuilder.queryParam(
                    PARAM_SORT_DIRECTION, sortDirection != null ? sortDirection : "asc");
              }
              if (searchQuery != null && !searchQuery.isBlank()) {
                uriBuilder.queryParam(PARAM_SEARCH_QUERY, searchQuery);
                if (searchType != null && !searchType.isBlank()) {
                  uriBuilder.queryParam(PARAM_SEARCH_TYPE, searchType);
                }
              }
              if (dateFrom != null) {
                uriBuilder.queryParam(PARAM_DATE_FROM, dateFrom.toString());
              }
              if (dateTo != null) {
                uriBuilder.queryParam(PARAM_DATE_TO, dateTo.toString());
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
                                new OpenDebtException(ERR_CLIENT_MSG + body, ERROR_CODE_CLIENT))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        ERR_UNAVAILABLE_MSG,
                        ERROR_CODE_UNAVAILABLE,
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(new ParameterizedTypeReference<RestPage<ClaimListItemDto>>() {})
        .block();
  }

  /**
   * Fetches claim counts for the given creditor filtered by date range. Returns both recovery and
   * zero-balance counts.
   */
  @CircuitBreaker(
      name = CIRCUIT_BREAKER_READ,
      fallbackMethod = "getClaimCountsForDateRangeFallback")
  @Retry(name = CIRCUIT_BREAKER_READ)
  public ClaimCountsDto getClaimCountsForDateRange(
      UUID creditorOrgId, LocalDate dateFrom, LocalDate dateTo) {
    log.debug("Fetching claim counts with date range for creditor: {}", creditorOrgId);

    return webClient
        .get()
        .uri(
            uriBuilder -> {
              uriBuilder
                  .path("/debt-service/api/v1/debts/counts")
                  .queryParam(PARAM_CREDITOR_ID, creditorOrgId);
              if (dateFrom != null) {
                uriBuilder.queryParam(PARAM_DATE_FROM, dateFrom.toString());
              }
              if (dateTo != null) {
                uriBuilder.queryParam(PARAM_DATE_TO, dateTo.toString());
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
                                new OpenDebtException(ERR_CLIENT_MSG + body, ERROR_CODE_CLIENT))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        ERR_UNAVAILABLE_MSG,
                        ERROR_CODE_UNAVAILABLE,
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(ClaimCountsDto.class)
        .block();
  }

  /** Fetches detailed claim information for the claim detail view. */
  @CircuitBreaker(name = CIRCUIT_BREAKER_READ, fallbackMethod = "getClaimDetailFallback")
  @Retry(name = CIRCUIT_BREAKER_READ)
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
                                new OpenDebtException(ERR_CLIENT_MSG + body, ERROR_CODE_CLIENT))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        ERR_UNAVAILABLE_MSG,
                        ERROR_CODE_UNAVAILABLE,
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(ClaimDetailDto.class)
        .block();
  }

  /** Fetches a receipt for a claim operation by delivery ID. */
  @CircuitBreaker(name = CIRCUIT_BREAKER_READ, fallbackMethod = "getReceiptFallback")
  @Retry(name = CIRCUIT_BREAKER_READ)
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
                                new OpenDebtException(ERR_CLIENT_MSG + body, ERROR_CODE_CLIENT))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        ERR_UNAVAILABLE_MSG,
                        ERROR_CODE_UNAVAILABLE,
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(byte[].class)
        .block();
  }

  /**
   * Lists claims in hearing for the given creditor with pagination, sorting, search, and date range
   * filtering.
   */
  @CircuitBreaker(name = CIRCUIT_BREAKER_READ, fallbackMethod = "listHearingClaimsFallback")
  @Retry(name = CIRCUIT_BREAKER_READ)
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
                  .path(CLAIMS_PATH)
                  .queryParam(PARAM_CREDITOR_ID, creditorOrgId)
                  .queryParam(PARAM_STATUS, "HEARING")
                  .queryParam("page", page)
                  .queryParam("size", size);
              if (sortBy != null && !sortBy.isBlank()) {
                uriBuilder.queryParam(PARAM_SORT_BY, sortBy);
                uriBuilder.queryParam(
                    PARAM_SORT_DIRECTION, sortDirection != null ? sortDirection : "asc");
              }
              if (searchQuery != null && !searchQuery.isBlank()) {
                uriBuilder.queryParam(PARAM_SEARCH_QUERY, searchQuery);
                if (searchType != null && !searchType.isBlank()) {
                  uriBuilder.queryParam(PARAM_SEARCH_TYPE, searchType);
                }
              }
              if (dateFrom != null) {
                uriBuilder.queryParam(PARAM_DATE_FROM, dateFrom.toString());
              }
              if (dateTo != null) {
                uriBuilder.queryParam(PARAM_DATE_TO, dateTo.toString());
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
                                new OpenDebtException(ERR_CLIENT_MSG + body, ERROR_CODE_CLIENT))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        ERR_UNAVAILABLE_MSG,
                        ERROR_CODE_UNAVAILABLE,
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(new ParameterizedTypeReference<RestPage<HearingClaimListItemDto>>() {})
        .block();
  }

  /** Fetches detailed hearing claim information for the hearing detail view. */
  @CircuitBreaker(name = CIRCUIT_BREAKER_READ, fallbackMethod = "getHearingClaimDetailFallback")
  @Retry(name = CIRCUIT_BREAKER_READ)
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
                                new OpenDebtException(ERR_CLIENT_MSG + body, ERROR_CODE_CLIENT))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        ERR_UNAVAILABLE_MSG,
                        ERROR_CODE_UNAVAILABLE,
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(HearingClaimDetailDto.class)
        .block();
  }

  /** Approves a hearing claim with a written justification. */
  @CircuitBreaker(name = CIRCUIT_BREAKER_READ, fallbackMethod = "approveHearingClaimFallback")
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
                                new OpenDebtException(ERR_CLIENT_MSG + body, ERROR_CODE_CLIENT))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        ERR_UNAVAILABLE_MSG,
                        ERROR_CODE_UNAVAILABLE,
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(Void.class)
        .block();
  }

  /** Withdraws a hearing claim with a reason. */
  @CircuitBreaker(name = CIRCUIT_BREAKER_READ, fallbackMethod = "withdrawHearingClaimFallback")
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
                                new OpenDebtException(ERR_CLIENT_MSG + body, ERROR_CODE_CLIENT))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        ERR_UNAVAILABLE_MSG,
                        ERROR_CODE_UNAVAILABLE,
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(Void.class)
        .block();
  }

  /**
   * Lists rejected claims for the given creditor with pagination, sorting, search, and date range
   * filtering.
   */
  @CircuitBreaker(name = CIRCUIT_BREAKER_READ, fallbackMethod = "listRejectedClaimsFallback")
  @Retry(name = CIRCUIT_BREAKER_READ)
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
                  .path(CLAIMS_PATH)
                  .queryParam(PARAM_CREDITOR_ID, creditorOrgId)
                  .queryParam(PARAM_STATUS, STATUS_REJECTED)
                  .queryParam("page", page)
                  .queryParam("size", size);
              if (sortBy != null && !sortBy.isBlank()) {
                uriBuilder.queryParam(PARAM_SORT_BY, sortBy);
                uriBuilder.queryParam(
                    PARAM_SORT_DIRECTION, sortDirection != null ? sortDirection : "asc");
              }
              if (searchQuery != null && !searchQuery.isBlank()) {
                uriBuilder.queryParam(PARAM_SEARCH_QUERY, searchQuery);
                if (searchType != null && !searchType.isBlank()) {
                  uriBuilder.queryParam(PARAM_SEARCH_TYPE, searchType);
                }
              }
              if (dateFrom != null) {
                uriBuilder.queryParam(PARAM_DATE_FROM, dateFrom.toString());
              }
              if (dateTo != null) {
                uriBuilder.queryParam(PARAM_DATE_TO, dateTo.toString());
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
                                new OpenDebtException(ERR_CLIENT_MSG + body, ERROR_CODE_CLIENT))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        ERR_UNAVAILABLE_MSG,
                        ERROR_CODE_UNAVAILABLE,
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(new ParameterizedTypeReference<RestPage<ClaimListItemDto>>() {})
        .block();
  }

  /** Fetches detailed rejection information for a rejected claim. */
  @CircuitBreaker(name = CIRCUIT_BREAKER_READ, fallbackMethod = "getRejectedClaimDetailFallback")
  @Retry(name = CIRCUIT_BREAKER_READ)
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
                                new OpenDebtException(ERR_CLIENT_MSG + body, ERROR_CODE_CLIENT))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        ERR_UNAVAILABLE_MSG,
                        ERROR_CODE_UNAVAILABLE,
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(RejectedClaimDetailDto.class)
        .block();
  }

  @CircuitBreaker(name = CIRCUIT_BREAKER_READ, fallbackMethod = "createDebtFallback")
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
                              new OpenDebtException(ERR_CLIENT_MSG + body, ERROR_CODE_CLIENT)));
            })
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        ERR_UNAVAILABLE_MSG,
                        ERROR_CODE_UNAVAILABLE,
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(PortalDebtDto.class)
        .block();
  }

  /**
   * Submits a new claim via the portal wizard and returns a structured result indicating the
   * outcome (UDFOERT, AFVIST, or HOERING). Unlike {@link #createDebt}, this method captures
   * validation errors instead of throwing, so the wizard can display them inline.
   */
  @CircuitBreaker(name = CIRCUIT_BREAKER_READ, fallbackMethod = "submitClaimWizardFallback")
  public ClaimSubmissionResultDto submitClaimWizard(PortalDebtDto request) {
    log.debug("Submitting claim via wizard for creditor: {}", request.getCreditorOrgId());

    try {
      // Build a full DebtDto for the submit endpoint
      java.util.Map<String, Object> submitRequest = new java.util.LinkedHashMap<>();
      submitRequest.put(
          "debtorId",
          request.getDebtorPersonId() != null ? request.getDebtorPersonId().toString() : null);
      submitRequest.put(
          PARAM_CREDITOR_ID,
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
            .outcome(STATUS_AFVIST)
            .processingStatus("No response from debt-service")
            .errors(java.util.Collections.emptyList())
            .build();
      }

      String outcome = (String) response.get("outcome");

      if (STATUS_AFVIST.equals(outcome)) {
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
            .outcome(STATUS_AFVIST)
            .processingStatus(STATUS_REJECTED)
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
            .outcome(STATUS_AFVIST)
            .processingStatus(STATUS_REJECTED)
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
  @CircuitBreaker(name = CIRCUIT_BREAKER_READ, fallbackMethod = "submitAdjustmentFallback")
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
                              new OpenDebtException(ERR_CLIENT_MSG + body, ERROR_CODE_CLIENT)));
            })
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        ERR_UNAVAILABLE_MSG,
                        ERROR_CODE_UNAVAILABLE,
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(AdjustmentReceiptDto.class)
        .block();
  }

  private RestPage<PortalDebtDto> listDebtsFallback(UUID creditorOrgId, Throwable t) {
    if (t
            instanceof
            org.springframework.web.reactive.function.client.WebClientResponseException wcre
        && wcre.getStatusCode().is4xxClientError()) {
      throw wcre;
    }
    log.warn("Circuit breaker fallback triggered for listDebts: {}", t.getMessage());
    return new RestPage<>(java.util.List.of(), 0, 20, 0, 0);
  }

  private ClaimCountsDto getClaimCountsFallback(UUID creditorOrgId, Throwable t) {
    if (t
            instanceof
            org.springframework.web.reactive.function.client.WebClientResponseException wcre
        && wcre.getStatusCode().is4xxClientError()) {
      throw wcre;
    }
    log.warn("Circuit breaker fallback triggered for getClaimCounts: {}", t.getMessage());
    return null;
  }

  private RestPage<ClaimListItemDto> listClaimsInRecoveryFallback(
      UUID creditorOrgId,
      int page,
      int size,
      String sortBy,
      String sortDirection,
      String searchQuery,
      String searchType,
      LocalDate dateFrom,
      LocalDate dateTo,
      Throwable t) {
    if (t
            instanceof
            org.springframework.web.reactive.function.client.WebClientResponseException wcre
        && wcre.getStatusCode().is4xxClientError()) {
      throw wcre;
    }
    log.warn("Circuit breaker fallback triggered for listClaimsInRecovery: {}", t.getMessage());
    return new RestPage<>(java.util.List.of(), page, size, 0, 0);
  }

  private RestPage<ClaimListItemDto> listZeroBalanceClaimsFallback(
      UUID creditorOrgId,
      int page,
      int size,
      String sortBy,
      String sortDirection,
      String searchQuery,
      String searchType,
      LocalDate dateFrom,
      LocalDate dateTo,
      Throwable t) {
    if (t
            instanceof
            org.springframework.web.reactive.function.client.WebClientResponseException wcre
        && wcre.getStatusCode().is4xxClientError()) {
      throw wcre;
    }
    log.warn("Circuit breaker fallback triggered for listZeroBalanceClaims: {}", t.getMessage());
    return new RestPage<>(java.util.List.of(), page, size, 0, 0);
  }

  private ClaimCountsDto getClaimCountsForDateRangeFallback(
      UUID creditorOrgId, LocalDate dateFrom, LocalDate dateTo, Throwable t) {
    if (t
            instanceof
            org.springframework.web.reactive.function.client.WebClientResponseException wcre
        && wcre.getStatusCode().is4xxClientError()) {
      throw wcre;
    }
    log.warn(
        "Circuit breaker fallback triggered for getClaimCountsForDateRange: {}", t.getMessage());
    return null;
  }

  private ClaimDetailDto getClaimDetailFallback(UUID claimId, Throwable t) {
    if (t
            instanceof
            org.springframework.web.reactive.function.client.WebClientResponseException wcre
        && wcre.getStatusCode().is4xxClientError()) {
      throw wcre;
    }
    log.warn("Circuit breaker fallback triggered for getClaimDetail: {}", t.getMessage());
    return null;
  }

  private byte[] getReceiptFallback(UUID claimId, String deliveryId, Throwable t) {
    if (t
            instanceof
            org.springframework.web.reactive.function.client.WebClientResponseException wcre
        && wcre.getStatusCode().is4xxClientError()) {
      throw wcre;
    }
    log.warn("Circuit breaker fallback triggered for getReceipt: {}", t.getMessage());
    return null;
  }

  private RestPage<HearingClaimListItemDto> listHearingClaimsFallback(
      UUID creditorOrgId,
      int page,
      int size,
      String sortBy,
      String sortDirection,
      String searchQuery,
      String searchType,
      LocalDate dateFrom,
      LocalDate dateTo,
      Throwable t) {
    if (t
            instanceof
            org.springframework.web.reactive.function.client.WebClientResponseException wcre
        && wcre.getStatusCode().is4xxClientError()) {
      throw wcre;
    }
    log.warn("Circuit breaker fallback triggered for listHearingClaims: {}", t.getMessage());
    return new RestPage<>(java.util.List.of(), page, size, 0, 0);
  }

  private HearingClaimDetailDto getHearingClaimDetailFallback(UUID claimId, Throwable t) {
    if (t
            instanceof
            org.springframework.web.reactive.function.client.WebClientResponseException wcre
        && wcre.getStatusCode().is4xxClientError()) {
      throw wcre;
    }
    log.warn("Circuit breaker fallback triggered for getHearingClaimDetail: {}", t.getMessage());
    return null;
  }

  private void approveHearingClaimFallback(
      UUID claimId, HearingApproveRequestDto request, Throwable t) {
    if (t
            instanceof
            org.springframework.web.reactive.function.client.WebClientResponseException wcre
        && wcre.getStatusCode().is4xxClientError()) {
      throw wcre;
    }
    log.warn("Circuit breaker fallback triggered for approveHearingClaim: {}", t.getMessage());
  }

  private void withdrawHearingClaimFallback(
      UUID claimId, HearingWithdrawRequestDto request, Throwable t) {
    if (t
            instanceof
            org.springframework.web.reactive.function.client.WebClientResponseException wcre
        && wcre.getStatusCode().is4xxClientError()) {
      throw wcre;
    }
    log.warn("Circuit breaker fallback triggered for withdrawHearingClaim: {}", t.getMessage());
  }

  private RestPage<ClaimListItemDto> listRejectedClaimsFallback(
      UUID creditorOrgId,
      int page,
      int size,
      String sortBy,
      String sortDirection,
      String searchQuery,
      String searchType,
      LocalDate dateFrom,
      LocalDate dateTo,
      Throwable t) {
    if (t
            instanceof
            org.springframework.web.reactive.function.client.WebClientResponseException wcre
        && wcre.getStatusCode().is4xxClientError()) {
      throw wcre;
    }
    log.warn("Circuit breaker fallback triggered for listRejectedClaims: {}", t.getMessage());
    return new RestPage<>(java.util.List.of(), page, size, 0, 0);
  }

  private RejectedClaimDetailDto getRejectedClaimDetailFallback(UUID claimId, Throwable t) {
    if (t
            instanceof
            org.springframework.web.reactive.function.client.WebClientResponseException wcre
        && wcre.getStatusCode().is4xxClientError()) {
      throw wcre;
    }
    log.warn("Circuit breaker fallback triggered for getRejectedClaimDetail: {}", t.getMessage());
    return null;
  }

  private PortalDebtDto createDebtFallback(PortalDebtDto request, Throwable t) {
    if (t
            instanceof
            org.springframework.web.reactive.function.client.WebClientResponseException wcre
        && wcre.getStatusCode().is4xxClientError()) {
      throw wcre;
    }
    log.warn("Circuit breaker fallback triggered for createDebt: {}", t.getMessage());
    return null;
  }

  private ClaimSubmissionResultDto submitClaimWizardFallback(PortalDebtDto request, Throwable t) {
    if (t
            instanceof
            org.springframework.web.reactive.function.client.WebClientResponseException wcre
        && wcre.getStatusCode().is4xxClientError()) {
      throw wcre;
    }
    log.warn("Circuit breaker fallback triggered for submitClaimWizard: {}", t.getMessage());
    return ClaimSubmissionResultDto.builder()
        .outcome(STATUS_AFVIST)
        .processingStatus("SERVICE_UNAVAILABLE")
        .errors(java.util.Collections.emptyList())
        .build();
  }

  private AdjustmentReceiptDto submitAdjustmentFallback(
      UUID claimId, ClaimAdjustmentRequestDto request, Throwable t) {
    if (t
            instanceof
            org.springframework.web.reactive.function.client.WebClientResponseException wcre
        && wcre.getStatusCode().is4xxClientError()) {
      throw wcre;
    }
    log.warn("Circuit breaker fallback triggered for submitAdjustment: {}", t.getMessage());
    return null;
  }
}
