package dk.ufst.opendebt.creditor.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import dk.ufst.opendebt.creditor.dto.ClaimSearchParams;
import dk.ufst.opendebt.creditor.dto.HearingApproveRequestDto;
import dk.ufst.opendebt.creditor.dto.HearingClaimDetailDto;
import dk.ufst.opendebt.creditor.dto.HearingClaimListItemDto;
import dk.ufst.opendebt.creditor.dto.HearingWithdrawRequestDto;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class HearingClaimsClient {

  private final WebClient webClient;

  public HearingClaimsClient(
      WebClient.Builder webClientBuilder,
      @Value("${opendebt.services.debt-service.url:http://localhost:8082}") String baseUrl) {
    this.webClient = webClientBuilder.baseUrl(baseUrl).build();
  }

  @CircuitBreaker(
      name = DebtServiceClientSupport.READ_CIRCUIT_BREAKER,
      fallbackMethod = "listHearingClaimsFallback")
  @Retry(name = DebtServiceClientSupport.READ_CIRCUIT_BREAKER)
  public RestPage<HearingClaimListItemDto> listHearingClaims(
      UUID creditorOrgId, ClaimSearchParams params) {
    log.debug("Listing hearing claims for creditor: {}, page: {}", creditorOrgId, params.getPage());

    return DebtServiceClientSupport.get(
        webClient,
        uriBuilder -> {
          uriBuilder
              .path(DebtServiceClientSupport.CLAIMS_PATH)
              .queryParam(DebtServiceClientSupport.PARAM_CREDITOR_ID, creditorOrgId)
              .queryParam(DebtServiceClientSupport.PARAM_STATUS, "HEARING");
          DebtServiceClientSupport.applySearchFilters(uriBuilder, params);
          return uriBuilder.build();
        },
        new ParameterizedTypeReference<RestPage<HearingClaimListItemDto>>() {});
  }

  @CircuitBreaker(
      name = DebtServiceClientSupport.READ_CIRCUIT_BREAKER,
      fallbackMethod = "getHearingClaimDetailFallback")
  @Retry(name = DebtServiceClientSupport.READ_CIRCUIT_BREAKER)
  public HearingClaimDetailDto getHearingClaimDetail(UUID claimId) {
    log.debug("Fetching hearing claim detail for claimId: {}", claimId);

    return DebtServiceClientSupport.get(
        webClient,
        uriBuilder -> uriBuilder.path("/debt-service/api/v1/debts/{id}/hearing").build(claimId),
        HearingClaimDetailDto.class);
  }

  @CircuitBreaker(
      name = DebtServiceClientSupport.READ_CIRCUIT_BREAKER,
      fallbackMethod = "approveHearingClaimFallback")
  public void approveHearingClaim(UUID claimId, HearingApproveRequestDto request) {
    log.debug("Approving hearing claim: {}", claimId);

    DebtServiceClientSupport.post(
        webClient,
        uriBuilder ->
            uriBuilder.path("/debt-service/api/v1/debts/{id}/hearing/approve").build(claimId),
        request);
  }

  @CircuitBreaker(
      name = DebtServiceClientSupport.READ_CIRCUIT_BREAKER,
      fallbackMethod = "withdrawHearingClaimFallback")
  public void withdrawHearingClaim(UUID claimId, HearingWithdrawRequestDto request) {
    log.debug("Withdrawing hearing claim: {}", claimId);

    DebtServiceClientSupport.post(
        webClient,
        uriBuilder ->
            uriBuilder.path("/debt-service/api/v1/debts/{id}/hearing/withdraw").build(claimId),
        request);
  }

  private RestPage<HearingClaimListItemDto> listHearingClaimsFallback(
      UUID creditorOrgId, ClaimSearchParams params, Throwable throwable) {
    if (DebtServiceClientSupport.isClient4xx(throwable)) {
      throw (RuntimeException) throwable;
    }
    log.warn(
        "Circuit breaker fallback triggered for listHearingClaims: {}", throwable.getMessage());
    return new RestPage<>(java.util.List.of(), params.getPage(), params.getSize(), 0, 0);
  }

  private HearingClaimDetailDto getHearingClaimDetailFallback(UUID claimId, Throwable throwable) {
    if (DebtServiceClientSupport.isClient4xx(throwable)) {
      throw (RuntimeException) throwable;
    }
    log.warn(
        "Circuit breaker fallback triggered for getHearingClaimDetail: {}", throwable.getMessage());
    return null;
  }

  private void approveHearingClaimFallback(
      UUID claimId, HearingApproveRequestDto request, Throwable throwable) {
    if (DebtServiceClientSupport.isClient4xx(throwable)) {
      throw (RuntimeException) throwable;
    }
    log.warn(
        "Circuit breaker fallback triggered for approveHearingClaim: {}", throwable.getMessage());
  }

  private void withdrawHearingClaimFallback(
      UUID claimId, HearingWithdrawRequestDto request, Throwable throwable) {
    if (DebtServiceClientSupport.isClient4xx(throwable)) {
      throw (RuntimeException) throwable;
    }
    log.warn(
        "Circuit breaker fallback triggered for withdrawHearingClaim: {}", throwable.getMessage());
  }
}
