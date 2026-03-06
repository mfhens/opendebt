package dk.ufst.opendebt.payment.client;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import dk.ufst.opendebt.common.dto.DebtDto;

import lombok.extern.slf4j.Slf4j;

// AIDEV-NOTE: This is the only permitted way for payment-service to read/write debt data
// (ADR-0007).
// Never add a DebtRepository dependency to payment-service — all access must go through this
// client.
// AIDEV-TODO: Add Resilience4j @CircuitBreaker and @Retry on findByOcrLine and writeDown.
// A dead debt-service must not block payment ingestion; consider a fallback queue.
/** REST client for the debt-service API. No cross-service database access (ADR-0007). */
@Slf4j
@Component
public class DebtServiceClient {

  private final RestClient restClient;

  public DebtServiceClient(
      RestClient.Builder restClientBuilder,
      @Value("${opendebt.services.debt-service.url}") String debtServiceUrl) {
    // AIDEV-NOTE: Base URL includes "/debt-service" context path matching the K8s ingress config.
    // If running locally without ingress, set
    // opendebt.services.debt-service.url=http://localhost:8081
    this.restClient =
        restClientBuilder.baseUrl(debtServiceUrl + "/debt-service/api/v1/debts").build();
  }

  /**
   * Finds debts matching the given OCR-linje.
   *
   * @param ocrLine the Betalingsservice OCR-linje
   * @return list of matching debts (empty if none)
   */
  public List<DebtDto> findByOcrLine(String ocrLine) {
    log.debug("Looking up debts by OCR-linje: {}", ocrLine);
    return restClient
        .get()
        .uri(uriBuilder -> uriBuilder.path("/by-ocr").queryParam("ocrLine", ocrLine).build())
        .retrieve()
        .body(new ParameterizedTypeReference<>() {});
  }

  /**
   * Writes down the outstanding balance of a debt by the specified amount.
   *
   * @param debtId the debt ID
   * @param amount the write-down amount
   * @return the updated debt
   */
  public DebtDto writeDown(UUID debtId, BigDecimal amount) {
    log.info("Writing down debt {}, amount={}", debtId, amount);
    return restClient
        .post()
        .uri(
            uriBuilder ->
                uriBuilder.path("/{id}/write-down").queryParam("amount", amount).build(debtId))
        .retrieve()
        .body(DebtDto.class);
  }
}
