package dk.ufst.opendebt.creditor.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import dk.ufst.opendebt.common.exception.OpenDebtException;
import dk.ufst.opendebt.creditor.dto.PortalDebtDto;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

class DebtServiceClientTest {

  private MockWebServer mockWebServer;
  private DebtServiceClient client;
  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

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
  void listDebts_returnsPageOfDebts() throws InterruptedException {
    UUID creditorOrgId = UUID.randomUUID();
    String pageJson =
        """
        {
          "content": [{"id": "%s", "creditorOrgId": "%s", "principalAmount": 1000.00, "status": "ACTIVE"}],
          "number": 0,
          "size": 20,
          "totalElements": 1,
          "totalPages": 1
        }
        """
            .formatted(UUID.randomUUID(), creditorOrgId);
    mockWebServer.enqueue(
        new MockResponse().setBody(pageJson).addHeader("Content-Type", "application/json"));

    RestPage<PortalDebtDto> result = client.listDebts(creditorOrgId);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getTotalElements()).isEqualTo(1);

    RecordedRequest request = mockWebServer.takeRequest();
    assertThat(request.getPath())
        .contains("/debt-service/api/v1/debts")
        .contains("creditorId=" + creditorOrgId);
    assertThat(request.getMethod()).isEqualTo("GET");
  }

  @Test
  void listDebts_throwsOnServerError() {
    mockWebServer.enqueue(new MockResponse().setResponseCode(500));

    assertThatThrownBy(() -> client.listDebts(UUID.randomUUID()))
        .isInstanceOf(OpenDebtException.class)
        .hasMessageContaining("unavailable");
  }

  @Test
  void createDebt_returnsCreatedDebt() throws JsonProcessingException, InterruptedException {
    UUID debtId = UUID.randomUUID();
    UUID creditorOrgId = UUID.randomUUID();
    PortalDebtDto response =
        PortalDebtDto.builder()
            .id(debtId)
            .creditorOrgId(creditorOrgId)
            .principalAmount(new BigDecimal("5000.00"))
            .status("REGISTERED")
            .build();
    mockWebServer.enqueue(
        new MockResponse()
            .setBody(objectMapper.writeValueAsString(response))
            .addHeader("Content-Type", "application/json"));

    PortalDebtDto request =
        PortalDebtDto.builder()
            .creditorOrgId(creditorOrgId)
            .principalAmount(new BigDecimal("5000.00"))
            .build();
    PortalDebtDto result = client.createDebt(request);

    assertThat(result.getId()).isEqualTo(debtId);
    assertThat(result.getStatus()).isEqualTo("REGISTERED");

    RecordedRequest recorded = mockWebServer.takeRequest();
    assertThat(recorded.getPath()).isEqualTo("/debt-service/api/v1/debts");
    assertThat(recorded.getMethod()).isEqualTo("POST");
  }

  @Test
  void createDebt_throwsOnBadRequest() {
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(400)
            .setBody("Invalid debt")
            .addHeader("Content-Type", "application/json"));

    PortalDebtDto request = PortalDebtDto.builder().build();

    assertThatThrownBy(() -> client.createDebt(request))
        .isInstanceOf(OpenDebtException.class)
        .hasMessageContaining("Invalid debt request");
  }

  @Test
  void createDebt_throwsOnServerError() {
    mockWebServer.enqueue(new MockResponse().setResponseCode(500));

    PortalDebtDto request = PortalDebtDto.builder().build();

    assertThatThrownBy(() -> client.createDebt(request))
        .isInstanceOf(OpenDebtException.class)
        .hasMessageContaining("unavailable");
  }
}
