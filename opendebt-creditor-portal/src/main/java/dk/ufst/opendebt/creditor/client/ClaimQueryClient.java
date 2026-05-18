package dk.ufst.opendebt.creditor.client;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import dk.ufst.opendebt.creditor.dto.ClaimCountsDto;
import dk.ufst.opendebt.creditor.dto.ClaimDetailDto;
import dk.ufst.opendebt.creditor.dto.ClaimListItemDto;
import dk.ufst.opendebt.creditor.dto.ClaimSearchParams;
import dk.ufst.opendebt.creditor.dto.PortalDebtDto;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ClaimQueryClient {

  private final WebClient webClient;

  public ClaimQueryClient(
      WebClient.Builder webClientBuilder,
      @Value("${opendebt.services.debt-service.url:http://localhost:8082}") String baseUrl) {
    this.webClient = webClientBuilder.baseUrl(baseUrl).build();
  }

  @CircuitBreaker(
      name = DebtServiceClientSupport.READ_CIRCUIT_BREAKER,
      fallbackMethod = "listDebtsFallback")
  @Retry(name = DebtServiceClientSupport.READ_CIRCUIT_BREAKER)
  public RestPage<PortalDebtDto> listDebts(UUID creditorOrgId) {
    log.debug("Listing debts for creditor: {}", creditorOrgId);

    return DebtServiceClientSupport.get(
        webClient,
        uriBuilder ->
            uriBuilder
                .path("/debt-service/api/v1/debts")
                .queryParam(DebtServiceClientSupport.PARAM_CREDITOR_ID, creditorOrgId)
                .build(),
        new ParameterizedTypeReference<RestPage<PortalDebtDto>>() {});
  }

  @CircuitBreaker(
      name = DebtServiceClientSupport.READ_CIRCUIT_BREAKER,
      fallbackMethod = "getClaimCountsFallback")
  @Retry(name = DebtServiceClientSupport.READ_CIRCUIT_BREAKER)
  public ClaimCountsDto getClaimCounts(UUID creditorOrgId) {
    log.debug("Fetching claim counts for creditor: {}", creditorOrgId);

    return DebtServiceClientSupport.get(
        webClient,
        uriBuilder ->
            uriBuilder
                .path("/debt-service/api/v1/debts/counts")
                .queryParam(DebtServiceClientSupport.PARAM_CREDITOR_ID, creditorOrgId)
                .build(),
        ClaimCountsDto.class);
  }

  @CircuitBreaker(
      name = DebtServiceClientSupport.READ_CIRCUIT_BREAKER,
      fallbackMethod = "listClaimsInRecoveryFallback")
  @Retry(name = DebtServiceClientSupport.READ_CIRCUIT_BREAKER)
  public RestPage<ClaimListItemDto> listClaimsInRecovery(
      UUID creditorOrgId, ClaimSearchParams params) {
    log.debug(
        "Listing claims in recovery for creditor: {}, page: {}", creditorOrgId, params.getPage());

    return DebtServiceClientSupport.get(
        webClient,
        uriBuilder -> {
          uriBuilder
              .path(DebtServiceClientSupport.CLAIMS_PATH)
              .queryParam(DebtServiceClientSupport.PARAM_CREDITOR_ID, creditorOrgId)
              .queryParam(DebtServiceClientSupport.PARAM_STATUS, "IN_RECOVERY")
              .queryParam("excludeZeroBalance", true);
          DebtServiceClientSupport.applySearchFilters(uriBuilder, params);
          return uriBuilder.build();
        },
        new ParameterizedTypeReference<RestPage<ClaimListItemDto>>() {});
  }

  @CircuitBreaker(
      name = DebtServiceClientSupport.READ_CIRCUIT_BREAKER,
      fallbackMethod = "listZeroBalanceClaimsFallback")
  @Retry(name = DebtServiceClientSupport.READ_CIRCUIT_BREAKER)
  public RestPage<ClaimListItemDto> listZeroBalanceClaims(
      UUID creditorOrgId, ClaimSearchParams params) {
    log.debug(
        "Listing zero-balance claims for creditor: {}, page: {}", creditorOrgId, params.getPage());

    return DebtServiceClientSupport.get(
        webClient,
        uriBuilder -> {
          uriBuilder
              .path(DebtServiceClientSupport.CLAIMS_PATH)
              .queryParam(DebtServiceClientSupport.PARAM_CREDITOR_ID, creditorOrgId)
              .queryParam(DebtServiceClientSupport.PARAM_STATUS, "ZERO_BALANCE");
          DebtServiceClientSupport.applySearchFilters(uriBuilder, params);
          return uriBuilder.build();
        },
        new ParameterizedTypeReference<RestPage<ClaimListItemDto>>() {});
  }

  @CircuitBreaker(
      name = DebtServiceClientSupport.READ_CIRCUIT_BREAKER,
      fallbackMethod = "getClaimCountsForDateRangeFallback")
  @Retry(name = DebtServiceClientSupport.READ_CIRCUIT_BREAKER)
  public ClaimCountsDto getClaimCountsForDateRange(
      UUID creditorOrgId, LocalDate dateFrom, LocalDate dateTo) {
    log.debug("Fetching claim counts with date range for creditor: {}", creditorOrgId);

    return DebtServiceClientSupport.get(
        webClient,
        uriBuilder -> {
          uriBuilder
              .path("/debt-service/api/v1/debts/counts")
              .queryParam(DebtServiceClientSupport.PARAM_CREDITOR_ID, creditorOrgId);
          if (dateFrom != null) {
            uriBuilder.queryParam(DebtServiceClientSupport.PARAM_DATE_FROM, dateFrom.toString());
          }
          if (dateTo != null) {
            uriBuilder.queryParam(DebtServiceClientSupport.PARAM_DATE_TO, dateTo.toString());
          }
          return uriBuilder.build();
        },
        ClaimCountsDto.class);
  }

  @CircuitBreaker(
      name = DebtServiceClientSupport.READ_CIRCUIT_BREAKER,
      fallbackMethod = "getClaimDetailFallback")
  @Retry(name = DebtServiceClientSupport.READ_CIRCUIT_BREAKER)
  public ClaimDetailDto getClaimDetail(UUID claimId) {
    log.debug("Fetching claim detail for claimId: {}", claimId);

    return DebtServiceClientSupport.get(
        webClient,
        uriBuilder -> uriBuilder.path("/debt-service/api/v1/debts/{id}/details").build(claimId),
        ClaimDetailDto.class);
  }

  @CircuitBreaker(
      name = DebtServiceClientSupport.READ_CIRCUIT_BREAKER,
      fallbackMethod = "getReceiptFallback")
  @Retry(name = DebtServiceClientSupport.READ_CIRCUIT_BREAKER)
  public byte[] getReceipt(UUID claimId, String deliveryId) {
    log.debug("Fetching receipt for claimId: {}, deliveryId: {}", claimId, deliveryId);

    return DebtServiceClientSupport.get(
        webClient,
        uriBuilder ->
            uriBuilder
                .path("/debt-service/api/v1/debts/{id}/receipts/{deliveryId}")
                .build(claimId, deliveryId),
        byte[].class);
  }

  private RestPage<PortalDebtDto> listDebtsFallback(UUID creditorOrgId, Throwable throwable) {
    if (DebtServiceClientSupport.isClient4xx(throwable)) {
      throw (RuntimeException) throwable;
    }
    log.warn("Circuit breaker fallback triggered for listDebts: {}", throwable.getMessage());
    return new RestPage<>(java.util.List.of(), 0, 20, 0, 0);
  }

  private ClaimCountsDto getClaimCountsFallback(UUID creditorOrgId, Throwable throwable) {
    if (DebtServiceClientSupport.isClient4xx(throwable)) {
      throw (RuntimeException) throwable;
    }
    log.warn("Circuit breaker fallback triggered for getClaimCounts: {}", throwable.getMessage());
    return null;
  }

  private RestPage<ClaimListItemDto> listClaimsInRecoveryFallback(
      UUID creditorOrgId, ClaimSearchParams params, Throwable throwable) {
    if (DebtServiceClientSupport.isClient4xx(throwable)) {
      throw (RuntimeException) throwable;
    }
    log.warn(
        "Circuit breaker fallback triggered for listClaimsInRecovery: {}", throwable.getMessage());
    return new RestPage<>(java.util.List.of(), params.getPage(), params.getSize(), 0, 0);
  }

  private RestPage<ClaimListItemDto> listZeroBalanceClaimsFallback(
      UUID creditorOrgId, ClaimSearchParams params, Throwable throwable) {
    if (DebtServiceClientSupport.isClient4xx(throwable)) {
      throw (RuntimeException) throwable;
    }
    log.warn(
        "Circuit breaker fallback triggered for listZeroBalanceClaims: {}", throwable.getMessage());
    return new RestPage<>(java.util.List.of(), params.getPage(), params.getSize(), 0, 0);
  }

  private ClaimCountsDto getClaimCountsForDateRangeFallback(
      UUID creditorOrgId, LocalDate dateFrom, LocalDate dateTo, Throwable throwable) {
    if (DebtServiceClientSupport.isClient4xx(throwable)) {
      throw (RuntimeException) throwable;
    }
    log.warn(
        "Circuit breaker fallback triggered for getClaimCountsForDateRange: {}",
        throwable.getMessage());
    return null;
  }

  private ClaimDetailDto getClaimDetailFallback(UUID claimId, Throwable throwable) {
    if (DebtServiceClientSupport.isClient4xx(throwable)) {
      throw (RuntimeException) throwable;
    }
    log.warn("Circuit breaker fallback triggered for getClaimDetail: {}", throwable.getMessage());
    return null;
  }

  private byte[] getReceiptFallback(UUID claimId, String deliveryId, Throwable throwable) {
    if (DebtServiceClientSupport.isClient4xx(throwable)) {
      throw (RuntimeException) throwable;
    }
    log.warn("Circuit breaker fallback triggered for getReceipt: {}", throwable.getMessage());
    return new byte[0];
  }
}
