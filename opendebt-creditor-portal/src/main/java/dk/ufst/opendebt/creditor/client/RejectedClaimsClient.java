package dk.ufst.opendebt.creditor.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import dk.ufst.opendebt.creditor.dto.ClaimListItemDto;
import dk.ufst.opendebt.creditor.dto.ClaimSearchParams;
import dk.ufst.opendebt.creditor.dto.RejectedClaimDetailDto;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RejectedClaimsClient {

  private final WebClient webClient;

  public RejectedClaimsClient(
      WebClient.Builder webClientBuilder,
      @Value("${opendebt.services.debt-service.url:http://localhost:8082}") String baseUrl) {
    this.webClient = webClientBuilder.baseUrl(baseUrl).build();
  }

  @CircuitBreaker(
      name = DebtServiceClientSupport.READ_CIRCUIT_BREAKER,
      fallbackMethod = "listRejectedClaimsFallback")
  @Retry(name = DebtServiceClientSupport.READ_CIRCUIT_BREAKER)
  public RestPage<ClaimListItemDto> listRejectedClaims(
      UUID creditorOrgId, ClaimSearchParams params) {
    log.debug(
        "Listing rejected claims for creditor: {}, page: {}", creditorOrgId, params.getPage());

    return DebtServiceClientSupport.get(
        webClient,
        uriBuilder -> {
          uriBuilder
              .path(DebtServiceClientSupport.CLAIMS_PATH)
              .queryParam(DebtServiceClientSupport.PARAM_CREDITOR_ID, creditorOrgId)
              .queryParam(
                  DebtServiceClientSupport.PARAM_STATUS, DebtServiceClientSupport.STATUS_REJECTED);
          DebtServiceClientSupport.applySearchFilters(uriBuilder, params);
          return uriBuilder.build();
        },
        new ParameterizedTypeReference<RestPage<ClaimListItemDto>>() {});
  }

  @CircuitBreaker(
      name = DebtServiceClientSupport.READ_CIRCUIT_BREAKER,
      fallbackMethod = "getRejectedClaimDetailFallback")
  @Retry(name = DebtServiceClientSupport.READ_CIRCUIT_BREAKER)
  public RejectedClaimDetailDto getRejectedClaimDetail(UUID claimId) {
    log.debug("Fetching rejected claim detail for claimId: {}", claimId);

    return DebtServiceClientSupport.get(
        webClient,
        uriBuilder -> uriBuilder.path("/debt-service/api/v1/debts/{id}/rejection").build(claimId),
        RejectedClaimDetailDto.class);
  }

  private RestPage<ClaimListItemDto> listRejectedClaimsFallback(
      UUID creditorOrgId, ClaimSearchParams params, Throwable throwable) {
    if (DebtServiceClientSupport.isClient4xx(throwable)) {
      throw (RuntimeException) throwable;
    }
    log.warn(
        "Circuit breaker fallback triggered for listRejectedClaims: {}", throwable.getMessage());
    return new RestPage<>(java.util.List.of(), params.getPage(), params.getSize(), 0, 0);
  }

  private RejectedClaimDetailDto getRejectedClaimDetailFallback(UUID claimId, Throwable throwable) {
    if (DebtServiceClientSupport.isClient4xx(throwable)) {
      throw (RuntimeException) throwable;
    }
    log.warn(
        "Circuit breaker fallback triggered for getRejectedClaimDetail: {}",
        throwable.getMessage());
    return null;
  }
}
