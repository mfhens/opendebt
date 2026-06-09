package dk.ufst.opendebt.debtservice.section50.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import dk.ufst.opendebt.common.exception.OpenDebtException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

class DefaultPaymentCoverageOrderClientTest {

  private static MockWebServer mockWebServer;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private DefaultPaymentCoverageOrderClient client;

  @BeforeAll
  static void startServer() throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
  }

  @AfterAll
  static void stopServer() throws Exception {
    mockWebServer.shutdown();
  }

  @BeforeEach
  void setUp() {
    String baseUrl = mockWebServer.url("/").toString();
    baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    client = new DefaultPaymentCoverageOrderClient(WebClient.builder(), baseUrl);
  }

  @Test
  void orderPrincipalClaimIds_postsSimulationRequestAndFiltersDistinctOrder() throws Exception {
    UUID debtorId = UUID.randomUUID();
    mockWebServer.enqueue(
        new MockResponse()
            .setBody(
                objectMapper.writeValueAsString(
                    List.of(
                        new SimulationResponsePosition("C-2"),
                        new SimulationResponsePosition("C-2"),
                        new SimulationResponsePosition("C-1"),
                        new SimulationResponsePosition("OTHER"))))
            .addHeader("Content-Type", "application/json"));

    List<String> result =
        client.orderPrincipalClaimIds(
            debtorId, new BigDecimal("400.00"), List.of("C-1", "C-2", "C-3"));

    assertThat(result).containsExactly("C-2", "C-1", "C-3");

    RecordedRequest request = mockWebServer.takeRequest();
    assertThat(request.getMethod()).isEqualTo("POST");
    assertThat(request.getPath())
        .isEqualTo(
            "/payment-service/api/v1/debtors/" + debtorId + "/daekningsraekkefoelge/simulate");
    var requestBody = objectMapper.readTree(request.getBody().readUtf8());
    assertThat(requestBody.get("beloeb").decimalValue()).isEqualByComparingTo("400.00");
    assertThat(requestBody.get("inddrivelsesindsatsType").textValue()).isEqualTo("FRIVILLIG");
    assertThat(requestBody.get("candidatePrincipalClaimIds")).hasSize(3);
    assertThat(requestBody.get("candidatePrincipalClaimIds").get(0).textValue()).isEqualTo("C-1");
    assertThat(requestBody.get("candidatePrincipalClaimIds").get(1).textValue()).isEqualTo("C-2");
    assertThat(requestBody.get("candidatePrincipalClaimIds").get(2).textValue()).isEqualTo("C-3");
  }

  @Test
  void orderPrincipalClaimIds_throwsOnServerError() {
    UUID debtorId = UUID.randomUUID();
    mockWebServer.enqueue(new MockResponse().setResponseCode(500));

    assertThatThrownBy(
            () ->
                client.orderPrincipalClaimIds(
                    debtorId, new BigDecimal("400.00"), List.of("C-1", "C-2")))
        .isInstanceOf(OpenDebtException.class)
        .hasMessageContaining("Payment service unavailable");
  }

  private record SimulationResponsePosition(String fordringId) {}
}
