package dk.ufst.opendebt.creditor.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import dk.ufst.opendebt.common.exception.OpenDebtException;
import dk.ufst.opendebt.creditor.dto.PortalCaseDto;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

class CaseServiceClientTest {

  private MockWebServer mockWebServer;
  private CaseServiceClient client;

  @BeforeEach
  void setUp() throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    String baseUrl = mockWebServer.url("/").toString();
    baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    client = new CaseServiceClient(WebClient.builder(), baseUrl);
  }

  @AfterEach
  void tearDown() throws Exception {
    mockWebServer.shutdown();
  }

  @Test
  void listCases_returnsPageOfCases() throws InterruptedException {
    String pageJson =
        """
        {
          "content": [{"id": "%s", "caseNumber": "SAG-001", "status": "OPEN"}],
          "number": 0,
          "size": 20,
          "totalElements": 1,
          "totalPages": 1
        }
        """
            .formatted(UUID.randomUUID());
    mockWebServer.enqueue(
        new MockResponse().setBody(pageJson).addHeader("Content-Type", "application/json"));

    RestPage<PortalCaseDto> result = client.listCases();

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getCaseNumber()).isEqualTo("SAG-001");
    assertThat(result.getTotalElements()).isEqualTo(1);

    RecordedRequest request = mockWebServer.takeRequest();
    assertThat(request.getPath()).isEqualTo("/case-service/api/v1/cases");
    assertThat(request.getMethod()).isEqualTo("GET");
  }

  @Test
  void listCases_throwsOnServerError() {
    mockWebServer.enqueue(new MockResponse().setResponseCode(500));

    assertThatThrownBy(() -> client.listCases())
        .isInstanceOf(OpenDebtException.class)
        .hasMessageContaining("unavailable");
  }
}
