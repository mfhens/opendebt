package dk.ufst.opendebt.creditor.client;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import dk.ufst.opendebt.common.exception.OpenDebtException;
import dk.ufst.opendebt.creditor.dto.ReportListItemDto;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * BFF client for the reporting service. Fetches monthly reports for a creditor filtered by year and
 * month (petition 037).
 *
 * <p>AIDEV-TODO: Replace graceful-fallback stubs when reporting-service is implemented. The client
 * is wired to the reporting-service base URL and uses the expected API contract.
 */
@Slf4j
@Component
public class ReportingServiceClient {

  private final WebClient webClient;

  public ReportingServiceClient(
      WebClient.Builder webClientBuilder,
      @Value("${opendebt.services.reporting-service.url:http://localhost:8094}") String baseUrl) {
    this.webClient = webClientBuilder.baseUrl(baseUrl).build();
  }

  /**
   * Lists reports for a creditor for the given year and month.
   *
   * @param creditorOrgId the creditor organisation UUID
   * @param year the report year
   * @param month the report month (1-12)
   * @return list of report items, or an empty list if the backend is unavailable
   */
  @CircuitBreaker(name = "creditor-service", fallbackMethod = "listReportsFallback")
  @Retry(name = "creditor-service")
  public List<ReportListItemDto> listReports(UUID creditorOrgId, int year, int month) {
    log.debug("Listing reports for creditor: {}, year: {}, month: {}", creditorOrgId, year, month);

    try {
      List<ReportListItemDto> result =
          webClient
              .get()
              .uri(
                  uriBuilder ->
                      uriBuilder
                          .path("/reporting-service/api/v1/reports")
                          .queryParam("creditorId", creditorOrgId)
                          .queryParam("year", year)
                          .queryParam("month", month)
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
                                          "Reporting service client error: " + body,
                                          "REPORTING_CLIENT_ERROR"))))
              .onStatus(
                  HttpStatusCode::is5xxServerError,
                  response ->
                      Mono.error(
                          new OpenDebtException(
                              "Reporting service unavailable",
                              "REPORTING_SERVICE_UNAVAILABLE",
                              OpenDebtException.ErrorSeverity.CRITICAL)))
              .bodyToMono(new ParameterizedTypeReference<List<ReportListItemDto>>() {})
              .block();
      return result != null ? result : Collections.emptyList();
    } catch (Exception ex) {
      log.warn("Reporting service unavailable: {}", ex.getMessage());
      return Collections.emptyList();
    }
  }

  /**
   * Downloads a report as a byte array (zip content).
   *
   * @param reportId the report UUID
   * @return the raw zip bytes, or {@code null} if the backend is unavailable
   */
  @CircuitBreaker(name = "creditor-service", fallbackMethod = "downloadReportFallback")
  @Retry(name = "creditor-service")
  public byte[] downloadReport(UUID reportId) {
    log.debug("Downloading report: {}", reportId);

    return webClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("/reporting-service/api/v1/reports/{reportId}/download")
                    .build(reportId))
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
                                    "Reporting service client error: " + body,
                                    "REPORTING_CLIENT_ERROR"))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        "Reporting service unavailable",
                        "REPORTING_SERVICE_UNAVAILABLE",
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(byte[].class)
        .block();
  }

  private List<ReportListItemDto> listReportsFallback(
      UUID creditorOrgId, int year, int month, Throwable t) {
    if (t
            instanceof
            org.springframework.web.reactive.function.client.WebClientResponseException wcre
        && wcre.getStatusCode().is4xxClientError()) {
      throw wcre;
    }
    log.warn("Circuit breaker fallback triggered for listReports: {}", t.getMessage());
    return Collections.emptyList();
  }

  private byte[] downloadReportFallback(UUID reportId, Throwable t) {
    if (t
            instanceof
            org.springframework.web.reactive.function.client.WebClientResponseException wcre
        && wcre.getStatusCode().is4xxClientError()) {
      throw wcre;
    }
    log.warn("Circuit breaker fallback triggered for downloadReport: {}", t.getMessage());
    return null;
  }
}
