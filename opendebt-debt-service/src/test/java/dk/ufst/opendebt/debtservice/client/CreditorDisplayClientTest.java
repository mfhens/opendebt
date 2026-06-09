package dk.ufst.opendebt.debtservice.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dk.ufst.opendebt.common.exception.OpenDebtException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

class CreditorDisplayClientTest {

  private static MockWebServer mockWebServer;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private CreditorDisplayClient client;

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
    client = new CreditorDisplayClient(WebClient.builder(), baseUrl);
  }

  @Test
  void getDisplayName_returnsResolvedDisplayName() throws JsonProcessingException {
    UUID creditorOrgId = UUID.randomUUID();
    mockWebServer.enqueue(
        new MockResponse()
            .setBody(objectMapper.writeValueAsString(Map.of("displayName", "Skattestyrelsen")))
            .addHeader("Content-Type", "application/json"));

    String displayName = client.getDisplayName(creditorOrgId);

    assertThat(displayName).isEqualTo("Skattestyrelsen");
  }

  @Test
  void getDisplayName_throwsWhenCreditorNotFound() {
    UUID creditorOrgId = UUID.randomUUID();
    mockWebServer.enqueue(new MockResponse().setResponseCode(404));

    assertThatThrownBy(() -> client.getDisplayName(creditorOrgId))
        .isInstanceOf(OpenDebtException.class)
        .hasMessageContaining("Creditor not found");
  }

  @Test
  void getDisplayName_throwsWhenDisplayNameMissing() throws JsonProcessingException {
    UUID creditorOrgId = UUID.randomUUID();
    mockWebServer.enqueue(
        new MockResponse()
            .setBody(objectMapper.writeValueAsString(Map.of("id", UUID.randomUUID().toString())))
            .addHeader("Content-Type", "application/json"));

    assertThatThrownBy(() -> client.getDisplayName(creditorOrgId))
        .isInstanceOf(OpenDebtException.class)
        .hasMessageContaining("display name missing");
  }
}
