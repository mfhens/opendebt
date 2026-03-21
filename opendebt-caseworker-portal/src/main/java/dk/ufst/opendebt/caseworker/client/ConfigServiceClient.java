package dk.ufst.opendebt.caseworker.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import dk.ufst.opendebt.caseworker.dto.config.ConfigEntryPortalDto;
import dk.ufst.opendebt.caseworker.dto.config.CreateConfigPortalRequest;
import dk.ufst.opendebt.common.exception.OpenDebtException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * BFF client for the debt-service business configuration REST API. Implements petition 047 §FR-1:
 * the caseworker portal calls debt-service to manage versioned business config values.
 */
@Slf4j
@Component
public class ConfigServiceClient {

  private final WebClient webClient;

  public ConfigServiceClient(
      WebClient.Builder webClientBuilder,
      @Value("${opendebt.services.debt-service.url:http://localhost:8082}") String baseUrl) {
    this.webClient = webClientBuilder.baseUrl(baseUrl).build();
  }

  /** Lists all config entries grouped by key. */
  @CircuitBreaker(name = "debt-service-read", fallbackMethod = "listAllGroupedFallback")
  @Retry(name = "debt-service-read")
  public Map<String, List<ConfigEntryPortalDto>> listAllGrouped() {
    log.debug("Listing all grouped config entries");
    return webClient
        .get()
        .uri("/debt-service/api/v1/config")
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
                                    "Config client error: " + body, "CONFIG_CLIENT_ERROR"))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        "Config service unavailable",
                        "CONFIG_SERVICE_UNAVAILABLE",
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(new ParameterizedTypeReference<Map<String, List<ConfigEntryPortalDto>>>() {})
        .block();
  }

  /** Gets the version history for a config key. */
  @CircuitBreaker(name = "debt-service-read", fallbackMethod = "getHistoryFallback")
  @Retry(name = "debt-service-read")
  public List<ConfigEntryPortalDto> getHistory(String key) {
    log.debug("Getting config history for key={}", key);
    return webClient
        .get()
        .uri("/debt-service/api/v1/config/{key}/history", key)
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
                                    "Config key not found: " + key, "CONFIG_NOT_FOUND"))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        "Config service unavailable",
                        "CONFIG_SERVICE_UNAVAILABLE",
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(new ParameterizedTypeReference<List<ConfigEntryPortalDto>>() {})
        .block();
  }

  /** Previews derived rates for an NB rate value. */
  @CircuitBreaker(name = "debt-service-read", fallbackMethod = "previewDerivedFallback")
  @Retry(name = "debt-service-read")
  public List<ConfigEntryPortalDto> previewDerived(
      String key, BigDecimal nbRate, LocalDate validFrom) {
    log.debug("Previewing derived rates for key={} nbRate={} validFrom={}", key, nbRate, validFrom);
    return webClient
        .get()
        .uri(
            u ->
                u.path("/debt-service/api/v1/config/{key}/preview")
                    .queryParam("nbRate", nbRate)
                    .queryParam("validFrom", validFrom)
                    .build(key))
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
                                    "Preview failed: " + body, "CONFIG_PREVIEW_ERROR"))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        "Config service unavailable",
                        "CONFIG_SERVICE_UNAVAILABLE",
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(new ParameterizedTypeReference<List<ConfigEntryPortalDto>>() {})
        .block();
  }

  /** Creates a new config version. */
  @CircuitBreaker(name = "debt-service-read", fallbackMethod = "createEntryFallback")
  @Retry(name = "debt-service-read")
  public ConfigEntryPortalDto createEntry(CreateConfigPortalRequest request) {
    log.info("Creating config entry: key={}", request.getConfigKey());
    return webClient
        .post()
        .uri("/debt-service/api/v1/config")
        .contentType(MediaType.APPLICATION_JSON)
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
                                    "Config validation error: " + body, "CONFIG_VALIDATION"))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        "Config service unavailable",
                        "CONFIG_SERVICE_UNAVAILABLE",
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
        .map(
            result -> {
              @SuppressWarnings("unchecked")
              Map<String, Object> entry = (Map<String, Object>) result.get("entry");
              return mapToDto(entry);
            })
        .block();
  }

  /** Approves a PENDING_REVIEW entry. */
  @CircuitBreaker(name = "debt-service-read", fallbackMethod = "approveEntryFallback")
  @Retry(name = "debt-service-read")
  public ConfigEntryPortalDto approveEntry(UUID id) {
    log.info("Approving config entry: id={}", id);
    return webClient
        .put()
        .uri("/debt-service/api/v1/config/{id}/approve", id)
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
                                    "Approve failed: " + body, "CONFIG_APPROVE_ERROR"))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        "Config service unavailable",
                        "CONFIG_SERVICE_UNAVAILABLE",
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(ConfigEntryPortalDto.class)
        .block();
  }

  /** Rejects (deletes) a PENDING_REVIEW entry. */
  @CircuitBreaker(name = "debt-service-read", fallbackMethod = "rejectEntryFallback")
  @Retry(name = "debt-service-read")
  public void rejectEntry(UUID id) {
    log.info("Rejecting config entry: id={}", id);
    webClient
        .put()
        .uri("/debt-service/api/v1/config/{id}/reject", id)
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
                                    "Reject failed: " + body, "CONFIG_REJECT_ERROR"))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        "Config service unavailable",
                        "CONFIG_SERVICE_UNAVAILABLE",
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .toBodilessEntity()
        .block();
  }

  /** Deletes a future (not yet effective) entry. */
  @CircuitBreaker(name = "debt-service-read", fallbackMethod = "deleteEntryFallback")
  @Retry(name = "debt-service-read")
  public void deleteEntry(UUID id) {
    log.info("Deleting config entry: id={}", id);
    webClient
        .delete()
        .uri("/debt-service/api/v1/config/{id}", id)
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
                                    "Delete failed: " + body, "CONFIG_DELETE_ERROR"))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        "Config service unavailable",
                        "CONFIG_SERVICE_UNAVAILABLE",
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .toBodilessEntity()
        .block();
  }

  // ── Fallback methods ────────────────────────────────────────────────────────

  private Map<String, List<ConfigEntryPortalDto>> listAllGroupedFallback(Throwable t) {
    log.warn("Circuit breaker fallback for listAllGrouped: {}", t.getMessage());
    return Map.of();
  }

  private List<ConfigEntryPortalDto> getHistoryFallback(String key, Throwable t) {
    log.warn("Circuit breaker fallback for getHistory key={}: {}", key, t.getMessage());
    return List.of();
  }

  private List<ConfigEntryPortalDto> previewDerivedFallback(
      String key, BigDecimal nbRate, LocalDate validFrom, Throwable t) {
    log.warn("Circuit breaker fallback for previewDerived key={}: {}", key, t.getMessage());
    return List.of();
  }

  private ConfigEntryPortalDto createEntryFallback(CreateConfigPortalRequest request, Throwable t) {
    if (t
            instanceof
            org.springframework.web.reactive.function.client.WebClientResponseException wcre
        && wcre.getStatusCode().is4xxClientError()) {
      throw wcre;
    }
    log.warn("Circuit breaker fallback for createEntry: {}", t.getMessage());
    return null;
  }

  private ConfigEntryPortalDto approveEntryFallback(UUID id, Throwable t) {
    if (t
            instanceof
            org.springframework.web.reactive.function.client.WebClientResponseException wcre
        && wcre.getStatusCode().is4xxClientError()) {
      throw wcre;
    }
    log.warn("Circuit breaker fallback for approveEntry id={}: {}", id, t.getMessage());
    return null;
  }

  private void rejectEntryFallback(UUID id, Throwable t) {
    log.warn("Circuit breaker fallback for rejectEntry id={}: {}", id, t.getMessage());
  }

  private void deleteEntryFallback(UUID id, Throwable t) {
    log.warn("Circuit breaker fallback for deleteEntry id={}: {}", id, t.getMessage());
  }

  @SuppressWarnings("unchecked")
  private ConfigEntryPortalDto mapToDto(Map<String, Object> m) {
    if (m == null) return null;
    ConfigEntryPortalDto dto = new ConfigEntryPortalDto();
    dto.setConfigKey((String) m.get("configKey"));
    dto.setConfigValue((String) m.get("configValue"));
    dto.setValueType((String) m.get("valueType"));
    dto.setDescription((String) m.get("description"));
    dto.setLegalBasis((String) m.get("legalBasis"));
    dto.setReviewStatus((String) m.get("reviewStatus"));
    dto.setComputedStatus((String) m.get("computedStatus"));
    if (m.get("id") != null) dto.setId(UUID.fromString((String) m.get("id")));
    if (m.get("validFrom") != null) dto.setValidFrom(LocalDate.parse((String) m.get("validFrom")));
    if (m.get("validTo") != null) dto.setValidTo(LocalDate.parse((String) m.get("validTo")));
    return dto;
  }
}
