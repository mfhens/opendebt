package dk.ufst.opendebt.caseworker.client;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import dk.ufst.opendebt.common.dto.CaseDto;
import dk.ufst.opendebt.common.dto.CaseEventDto;
import dk.ufst.opendebt.common.dto.CaseJournalEntryDto;
import dk.ufst.opendebt.common.dto.CaseJournalNoteDto;
import dk.ufst.opendebt.common.dto.CasePartyDto;
import dk.ufst.opendebt.common.dto.CollectionMeasureDto;
import dk.ufst.opendebt.common.dto.ObjectionDto;
import dk.ufst.opendebt.common.exception.OpenDebtException;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/** BFF client for case-service. Provides case listing and detail retrieval for caseworkers. */
@Slf4j
@Component
public class CaseServiceClient {

  private final WebClient webClient;

  public CaseServiceClient(
      WebClient.Builder webClientBuilder,
      @Value("${opendebt.services.case-service.url:http://localhost:8081}") String baseUrl) {
    this.webClient = webClientBuilder.baseUrl(baseUrl).build();
  }

  /** Lists cases with optional filters for status and caseworker assignment. */
  public RestPage<CaseDto> listCases(String status, String caseworkerId, int page, int size) {
    log.debug(
        "Listing cases: status={}, caseworkerId={}, page={}, size={}",
        status,
        caseworkerId,
        page,
        size);

    return webClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("/case-service/api/v1/cases")
                    .queryParamIfPresent(
                        "status",
                        status != null && !status.isBlank()
                            ? java.util.Optional.of(status)
                            : java.util.Optional.empty())
                    .queryParamIfPresent(
                        "caseworkerId",
                        caseworkerId != null && !caseworkerId.isBlank()
                            ? java.util.Optional.of(caseworkerId)
                            : java.util.Optional.empty())
                    .queryParam("page", page)
                    .queryParam("size", size)
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
                                    "Case service client error: " + body, "CASE_CLIENT_ERROR"))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        "Case service unavailable",
                        "CASE_SERVICE_UNAVAILABLE",
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(new ParameterizedTypeReference<RestPage<CaseDto>>() {})
        .block();
  }

  /** Retrieves a single case by ID. */
  public CaseDto getCase(UUID caseId) {
    log.debug("Getting case: {}", caseId);

    return webClient
        .get()
        .uri("/case-service/api/v1/cases/{id}", caseId)
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
                                    "Case not found: " + caseId, "CASE_NOT_FOUND"))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response ->
                Mono.error(
                    new OpenDebtException(
                        "Case service unavailable",
                        "CASE_SERVICE_UNAVAILABLE",
                        OpenDebtException.ErrorSeverity.CRITICAL)))
        .bodyToMono(CaseDto.class)
        .block();
  }

  /** Retrieves the parties (sagsparter) for a case. */
  public List<CasePartyDto> getParties(UUID caseId) {
    log.debug("Getting parties for case: {}", caseId);

    return webClient
        .get()
        .uri("/case-service/api/v1/cases/{id}/parties", caseId)
        .retrieve()
        .onStatus(HttpStatusCode::isError, response -> handleError(caseId, "parties"))
        .bodyToMono(new ParameterizedTypeReference<List<CasePartyDto>>() {})
        .block();
  }

  /** Retrieves events (hændelseslog) for a case. */
  public List<CaseEventDto> getEvents(UUID caseId) {
    log.debug("Getting events for case: {}", caseId);

    return webClient
        .get()
        .uri("/case-service/api/v1/cases/{id}/events", caseId)
        .retrieve()
        .onStatus(HttpStatusCode::isError, response -> handleError(caseId, "events"))
        .bodyToMono(new ParameterizedTypeReference<List<CaseEventDto>>() {})
        .block();
  }

  /** Retrieves collection measures (inddrivelsesskridt) for a case. */
  public List<CollectionMeasureDto> getMeasures(UUID caseId) {
    log.debug("Getting collection measures for case: {}", caseId);

    return webClient
        .get()
        .uri("/case-service/api/v1/cases/{id}/measures", caseId)
        .retrieve()
        .onStatus(HttpStatusCode::isError, response -> handleError(caseId, "measures"))
        .bodyToMono(new ParameterizedTypeReference<List<CollectionMeasureDto>>() {})
        .block();
  }

  /** Retrieves objections (indsigelser) for a case. */
  public List<ObjectionDto> getObjections(UUID caseId) {
    log.debug("Getting objections for case: {}", caseId);

    return webClient
        .get()
        .uri("/case-service/api/v1/cases/{id}/objections", caseId)
        .retrieve()
        .onStatus(HttpStatusCode::isError, response -> handleError(caseId, "objections"))
        .bodyToMono(new ParameterizedTypeReference<List<ObjectionDto>>() {})
        .block();
  }

  /** Retrieves journal entries for a case. */
  public List<CaseJournalEntryDto> getJournalEntries(UUID caseId) {
    log.debug("Getting journal entries for case: {}", caseId);

    return webClient
        .get()
        .uri("/case-service/api/v1/cases/{id}/journal", caseId)
        .retrieve()
        .onStatus(HttpStatusCode::isError, response -> handleError(caseId, "journal entries"))
        .bodyToMono(new ParameterizedTypeReference<List<CaseJournalEntryDto>>() {})
        .block();
  }

  /** Retrieves journal notes for a case. */
  public List<CaseJournalNoteDto> getJournalNotes(UUID caseId) {
    log.debug("Getting journal notes for case: {}", caseId);

    return webClient
        .get()
        .uri("/case-service/api/v1/cases/{id}/journal/notes", caseId)
        .retrieve()
        .onStatus(HttpStatusCode::isError, response -> handleError(caseId, "journal notes"))
        .bodyToMono(new ParameterizedTypeReference<List<CaseJournalNoteDto>>() {})
        .block();
  }

  private Mono<Throwable> handleError(UUID caseId, String resource) {
    return Mono.error(
        new OpenDebtException(
            "Failed to load " + resource + " for case: " + caseId, "CASE_RESOURCE_ERROR"));
  }
}
