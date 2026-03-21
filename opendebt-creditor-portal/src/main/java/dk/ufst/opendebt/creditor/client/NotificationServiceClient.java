package dk.ufst.opendebt.creditor.client;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import dk.ufst.opendebt.creditor.dto.NotificationSearchResultDto;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;

/**
 * BFF client for notification search and download against debt-service (or a future dedicated
 * notification service). Uses injected {@link WebClient.Builder} for trace propagation (ADR-0024).
 *
 * <p>The backend notification endpoints (W7-UND-01) are not yet implemented. All methods degrade
 * gracefully when the backend is unavailable.
 */
@Slf4j
@Component
public class NotificationServiceClient {

  private final WebClient webClient;

  public NotificationServiceClient(
      WebClient.Builder webClientBuilder,
      @Value("${opendebt.services.debt-service.url:http://localhost:8082}") String baseUrl) {
    this.webClient = webClientBuilder.baseUrl(baseUrl).build();
  }

  /**
   * Searches for notifications matching the given criteria and returns the count.
   *
   * @param creditorOrgId the acting creditor organisation ID
   * @param dateFrom start of date range (inclusive), may be {@code null}
   * @param dateTo end of date range (inclusive), may be {@code null}
   * @param notificationTypes list of notification type codes to filter, may be {@code null}
   * @return search result with matching count, or {@code null} if backend is unavailable
   */
  @CircuitBreaker(name = "notification-service", fallbackMethod = "searchNotificationsFallback")
  public NotificationSearchResultDto searchNotifications(
      UUID creditorOrgId, LocalDate dateFrom, LocalDate dateTo, List<String> notificationTypes) {
    log.debug(
        "Searching notifications for creditor: {}, dateFrom: {}, dateTo: {}",
        creditorOrgId,
        dateFrom,
        dateTo);

    try {
      return webClient
          .get()
          .uri(
              uriBuilder -> {
                uriBuilder
                    .path("/debt-service/api/v1/notifications")
                    .queryParam("creditorId", creditorOrgId);
                if (dateFrom != null) {
                  uriBuilder.queryParam("dateFrom", dateFrom.toString());
                }
                if (dateTo != null) {
                  uriBuilder.queryParam("dateTo", dateTo.toString());
                }
                if (notificationTypes != null && !notificationTypes.isEmpty()) {
                  uriBuilder.queryParam("types", String.join(",", notificationTypes));
                }
                return uriBuilder.build();
              })
          .retrieve()
          .bodyToMono(NotificationSearchResultDto.class)
          .block();
    } catch (Exception ex) {
      log.warn("Notification search unavailable: {}", ex.getMessage());
      return null;
    }
  }

  /**
   * Downloads notifications matching the given criteria as a zip file.
   *
   * @param creditorOrgId the acting creditor organisation ID
   * @param dateFrom start of date range (inclusive), may be {@code null}
   * @param dateTo end of date range (inclusive), may be {@code null}
   * @param notificationTypes list of notification type codes to filter, may be {@code null}
   * @param formatPdf whether to include PDF format
   * @param formatXml whether to include XML format
   * @return the zip file bytes, or {@code null} if backend is unavailable
   */
  @CircuitBreaker(name = "notification-service", fallbackMethod = "downloadNotificationsFallback")
  public byte[] downloadNotifications(
      UUID creditorOrgId,
      LocalDate dateFrom,
      LocalDate dateTo,
      List<String> notificationTypes,
      boolean formatPdf,
      boolean formatXml) {
    log.debug(
        "Downloading notifications for creditor: {}, pdf: {}, xml: {}",
        creditorOrgId,
        formatPdf,
        formatXml);

    try {
      return webClient
          .get()
          .uri(
              uriBuilder -> {
                uriBuilder
                    .path("/debt-service/api/v1/notifications/download")
                    .queryParam("creditorId", creditorOrgId)
                    .queryParam("formatPdf", formatPdf)
                    .queryParam("formatXml", formatXml);
                if (dateFrom != null) {
                  uriBuilder.queryParam("dateFrom", dateFrom.toString());
                }
                if (dateTo != null) {
                  uriBuilder.queryParam("dateTo", dateTo.toString());
                }
                if (notificationTypes != null && !notificationTypes.isEmpty()) {
                  uriBuilder.queryParam("types", String.join(",", notificationTypes));
                }
                return uriBuilder.build();
              })
          .retrieve()
          .bodyToMono(byte[].class)
          .block();
    } catch (Exception ex) {
      log.warn("Notification download unavailable: {}", ex.getMessage());
      return null;
    }
  }

  private NotificationSearchResultDto searchNotificationsFallback(
      UUID creditorOrgId,
      LocalDate dateFrom,
      LocalDate dateTo,
      List<String> notificationTypes,
      Throwable t) {
    log.warn("Notification service circuit breaker open: {}", t.getMessage());
    return null;
  }

  private byte[] downloadNotificationsFallback(
      UUID creditorOrgId,
      LocalDate dateFrom,
      LocalDate dateTo,
      List<String> notificationTypes,
      boolean formatPdf,
      boolean formatXml,
      Throwable t) {
    log.warn("Notification service circuit breaker open: {}", t.getMessage());
    return null;
  }
}
