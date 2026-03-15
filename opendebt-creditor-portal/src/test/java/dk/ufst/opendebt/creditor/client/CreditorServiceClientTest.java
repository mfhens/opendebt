package dk.ufst.opendebt.creditor.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dk.ufst.opendebt.common.exception.OpenDebtException;
import dk.ufst.opendebt.creditor.dto.AccessResolutionRequest;
import dk.ufst.opendebt.creditor.dto.AccessResolutionResponse;
import dk.ufst.opendebt.creditor.dto.PortalCreditorDto;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

class CreditorServiceClientTest {

  private MockWebServer mockWebServer;
  private CreditorServiceClient client;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    String baseUrl = mockWebServer.url("/").toString();
    baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    client = new CreditorServiceClient(WebClient.builder(), baseUrl);
  }

  @AfterEach
  void tearDown() throws Exception {
    mockWebServer.shutdown();
  }

  @Test
  void getByCreditorOrgId_returnsCreditor() throws JsonProcessingException, InterruptedException {
    UUID creditorOrgId = UUID.randomUUID();
    PortalCreditorDto dto =
        PortalCreditorDto.builder()
            .id(UUID.randomUUID())
            .creditorOrgId(creditorOrgId)
            .externalCreditorId("EXT-001")
            .activityStatus("ACTIVE")
            .connectionType("DIRECT")
            .build();
    mockWebServer.enqueue(
        new MockResponse()
            .setBody(objectMapper.writeValueAsString(dto))
            .addHeader("Content-Type", "application/json"));

    PortalCreditorDto result = client.getByCreditorOrgId(creditorOrgId);

    assertThat(result.getCreditorOrgId()).isEqualTo(creditorOrgId);
    assertThat(result.getExternalCreditorId()).isEqualTo("EXT-001");
    assertThat(result.getActivityStatus()).isEqualTo("ACTIVE");

    RecordedRequest request = mockWebServer.takeRequest();
    assertThat(request.getPath()).isEqualTo("/creditor-service/api/v1/creditors/" + creditorOrgId);
    assertThat(request.getMethod()).isEqualTo("GET");
  }

  @Test
  void getByCreditorOrgId_throwsWhenNotFound() {
    UUID creditorOrgId = UUID.randomUUID();
    mockWebServer.enqueue(new MockResponse().setResponseCode(404));

    assertThatThrownBy(() -> client.getByCreditorOrgId(creditorOrgId))
        .isInstanceOf(OpenDebtException.class)
        .hasMessageContaining("Creditor not found");
  }

  @Test
  void getByCreditorOrgId_throwsOnServerError() {
    UUID creditorOrgId = UUID.randomUUID();
    mockWebServer.enqueue(new MockResponse().setResponseCode(500));

    assertThatThrownBy(() -> client.getByCreditorOrgId(creditorOrgId))
        .isInstanceOf(OpenDebtException.class)
        .hasMessageContaining("unavailable");
  }

  @Test
  void resolveAccess_returnsAllowedResponse() throws JsonProcessingException, InterruptedException {
    UUID actingId = UUID.randomUUID();
    AccessResolutionResponse response =
        AccessResolutionResponse.builder()
            .channelType("PORTAL")
            .actingCreditorOrgId(actingId)
            .allowed(true)
            .build();
    mockWebServer.enqueue(
        new MockResponse()
            .setBody(objectMapper.writeValueAsString(response))
            .addHeader("Content-Type", "application/json"));

    AccessResolutionRequest request =
        AccessResolutionRequest.builder()
            .channelType("PORTAL")
            .presentedIdentity("test-identity")
            .build();
    AccessResolutionResponse result = client.resolveAccess(request);

    assertThat(result.isAllowed()).isTrue();
    assertThat(result.getActingCreditorOrgId()).isEqualTo(actingId);

    RecordedRequest recorded = mockWebServer.takeRequest();
    assertThat(recorded.getPath()).isEqualTo("/creditor-service/api/v1/creditors/access/resolve");
    assertThat(recorded.getMethod()).isEqualTo("POST");
  }

  @Test
  void resolveAccess_throwsOnServerError() {
    mockWebServer.enqueue(new MockResponse().setResponseCode(500));

    AccessResolutionRequest request =
        AccessResolutionRequest.builder()
            .channelType("PORTAL")
            .presentedIdentity("test-identity")
            .build();

    assertThatThrownBy(() -> client.resolveAccess(request))
        .isInstanceOf(OpenDebtException.class)
        .hasMessageContaining("unavailable");
  }
}
