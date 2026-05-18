package dk.ufst.opendebt.creditor.client;

import java.net.URI;
import java.util.function.Function;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriBuilder;

import dk.ufst.opendebt.common.exception.OpenDebtException;
import dk.ufst.opendebt.creditor.dto.ClaimSearchParams;

import reactor.core.publisher.Mono;

final class DebtServiceClientSupport {

  static final String READ_CIRCUIT_BREAKER = "debt-service-read";
  static final String WRITE_CIRCUIT_BREAKER = "debt-service-write";

  static final String ERROR_CODE_CLIENT = "DEBT_CLIENT_ERROR";
  static final String ERROR_CODE_UNAVAILABLE = "DEBT_SERVICE_UNAVAILABLE";
  static final String STATUS_REJECTED = "REJECTED";
  static final String STATUS_AFVIST = "AFVIST";

  static final String CLAIM_INGRESS_PATH_HEADER = "X-OpenDebt-Claim-Ingress-Path";
  static final String CLAIM_INGRESS_PATH_PORTAL = "PORTAL";

  static final String CLAIMS_PATH = "/debt-service/api/v1/debts/claims";

  static final String PARAM_CREDITOR_ID = "creditorId";
  static final String PARAM_SEARCH_QUERY = "searchQuery";
  static final String PARAM_SEARCH_TYPE = "searchType";
  static final String PARAM_SORT_BY = "sortBy";
  static final String PARAM_SORT_DIRECTION = "sortDirection";
  static final String PARAM_DATE_FROM = "dateFrom";
  static final String PARAM_DATE_TO = "dateTo";
  static final String PARAM_STATUS = "status";

  private static final String ERR_CLIENT_MSG = "Debt service client error: ";
  private static final String ERR_UNAVAILABLE_MSG = "Debt service unavailable";

  private DebtServiceClientSupport() {}

  static <T> T get(
      WebClient webClient, Function<UriBuilder, URI> uriFunction, Class<T> responseType) {
    return webClient
        .get()
        .uri(uriFunction)
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, DebtServiceClientSupport::standardClientError)
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response -> Mono.error(DebtServiceClientSupport.unavailableError()))
        .bodyToMono(responseType)
        .block();
  }

  static <T> T get(
      WebClient webClient,
      Function<UriBuilder, URI> uriFunction,
      ParameterizedTypeReference<T> responseType) {
    return webClient
        .get()
        .uri(uriFunction)
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, DebtServiceClientSupport::standardClientError)
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response -> Mono.error(DebtServiceClientSupport.unavailableError()))
        .bodyToMono(responseType)
        .block();
  }

  static <T> T post(WebClient webClient, String path, Object request, Class<T> responseType) {
    return webClient
        .post()
        .uri(path)
        .bodyValue(request)
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, DebtServiceClientSupport::standardClientError)
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response -> Mono.error(DebtServiceClientSupport.unavailableError()))
        .bodyToMono(responseType)
        .block();
  }

  static void post(WebClient webClient, Function<UriBuilder, URI> uriFunction, Object request) {
    webClient
        .post()
        .uri(uriFunction)
        .bodyValue(request)
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, DebtServiceClientSupport::standardClientError)
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response -> Mono.error(DebtServiceClientSupport.unavailableError()))
        .bodyToMono(Void.class)
        .block();
  }

  static <T> T put(
      WebClient webClient,
      Function<UriBuilder, URI> uriFunction,
      Object request,
      Class<T> responseType) {
    return webClient
        .put()
        .uri(uriFunction)
        .bodyValue(request)
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, DebtServiceClientSupport::standardClientError)
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response -> Mono.error(DebtServiceClientSupport.unavailableError()))
        .bodyToMono(responseType)
        .block();
  }

  static Mono<? extends Throwable> standardClientError(ClientResponse response) {
    return response
        .bodyToMono(String.class)
        .flatMap(
            body -> Mono.error(new OpenDebtException(ERR_CLIENT_MSG + body, ERROR_CODE_CLIENT)));
  }

  static OpenDebtException unavailableError() {
    return new OpenDebtException(
        ERR_UNAVAILABLE_MSG, ERROR_CODE_UNAVAILABLE, OpenDebtException.ErrorSeverity.CRITICAL);
  }

  static boolean isClient4xx(Throwable throwable) {
    return throwable instanceof WebClientResponseException exception
        && exception.getStatusCode().is4xxClientError();
  }

  static void applySearchFilters(UriBuilder uriBuilder, ClaimSearchParams params) {
    uriBuilder.queryParam("page", params.getPage()).queryParam("size", params.getSize());
    if (params.getSortBy() != null && !params.getSortBy().isBlank()) {
      uriBuilder.queryParam(PARAM_SORT_BY, params.getSortBy());
      uriBuilder.queryParam(
          PARAM_SORT_DIRECTION,
          params.getSortDirection() != null ? params.getSortDirection() : "asc");
    }
    if (params.getSearchQuery() != null && !params.getSearchQuery().isBlank()) {
      uriBuilder.queryParam(PARAM_SEARCH_QUERY, params.getSearchQuery());
      if (params.getSearchType() != null && !params.getSearchType().isBlank()) {
        uriBuilder.queryParam(PARAM_SEARCH_TYPE, params.getSearchType());
      }
    }
    if (params.getDateFrom() != null) {
      uriBuilder.queryParam(PARAM_DATE_FROM, params.getDateFrom().toString());
    }
    if (params.getDateTo() != null) {
      uriBuilder.queryParam(PARAM_DATE_TO, params.getDateTo().toString());
    }
  }
}
