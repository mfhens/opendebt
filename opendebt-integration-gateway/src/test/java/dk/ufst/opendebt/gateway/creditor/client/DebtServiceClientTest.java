package dk.ufst.opendebt.gateway.creditor.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import dk.ufst.opendebt.common.dto.DebtDto;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

class DebtServiceClientTest {

  private MockWebServer mockWebServer;
  private DebtServiceClient client;

  @BeforeEach
  void setUp() throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    String baseUrl = mockWebServer.url("/").toString();
    baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    client = new DebtServiceClient(WebClient.builder(), baseUrl);
  }

  @AfterEach
  void tearDown() throws Exception {
    mockWebServer.shutdown();
  }

  @Test
  void submitClaim_sendsSystemToSystemIngressHeader() throws InterruptedException {
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(201)
            .setBody("{\"outcome\":\"UDFOERT\",\"claimId\":\"" + UUID.randomUUID() + "\"}")
            .addHeader("Content-Type", "application/json"));

    ClaimSubmissionResult result = client.submitClaim(sampleDebt());

    assertThat(result.getOutcome()).isEqualTo(ClaimSubmissionResult.Outcome.UDFOERT);

    RecordedRequest request = mockWebServer.takeRequest();
    assertThat(request.getPath()).isEqualTo("/debt-service/api/v1/debts/submit");
    assertThat(request.getHeader("X-OpenDebt-Claim-Ingress-Path")).isEqualTo("SYSTEM_TO_SYSTEM");
  }

  @Test
  void submitClaim_mapsDebtServiceValidationErrorsOnRejectedResponse() {
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(422)
            .setBody(
                """
                {
                  "outcome": "AFVIST",
                  "errors": [
                    {
                      "ruleId": "Rule411",
                      "errorCode": "FORDRING_TYPE_ERROR",
                      "description": "Invalid art type"
                    }
                  ]
                }
                """)
            .addHeader("Content-Type", "application/json"));

    assertThatThrownBy(() -> client.submitClaim(sampleDebt()))
        .isInstanceOf(ClaimRejectedException.class)
        .satisfies(
            ex -> {
              ClaimRejectedException rejected = (ClaimRejectedException) ex;
              assertThat(rejected.getResult().getOutcome())
                  .isEqualTo(ClaimSubmissionResult.Outcome.AFVIST);
              assertThat(rejected.getResult().getErrors()).hasSize(1);
              assertThat(rejected.getResult().getErrors().get(0).getRuleCode())
                  .isEqualTo("Rule411");
              assertThat(rejected.getResult().getErrors().get(0).getErrorCode())
                  .isEqualTo("FORDRING_TYPE_ERROR");
              assertThat(rejected.getResult().getErrors().get(0).getMessage())
                  .isEqualTo("Invalid art type");
            });
  }

  private DebtDto sampleDebt() {
    return DebtDto.builder()
        .debtorId(UUID.randomUUID().toString())
        .creditorId(UUID.randomUUID().toString())
        .debtTypeCode("HF01")
        .claimArt("INDR")
        .principalAmount(new BigDecimal("5000.00"))
        .dueDate(LocalDate.now().plusDays(30))
        .build();
  }
}
