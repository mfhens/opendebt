package dk.ufst.opendebt.debtservice.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

class CreditorServiceClientTest {

  private static MockWebServer mockWebServer;
  private CreditorServiceClient client;
  private final ObjectMapper objectMapper = new ObjectMapper();

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
    client = new CreditorServiceClient(WebClient.builder(), baseUrl);
  }

  @Test
  void validateAction_returnsAllowedResponse() throws JsonProcessingException {
    UUID creditorOrgId = UUID.randomUUID();
    ValidateActionResponse response =
        ValidateActionResponse.builder()
            .allowed(true)
            .requestedAction(CreditorAction.CREATE_CLAIM)
            .build();
    mockWebServer.enqueue(
        new MockResponse()
            .setBody(objectMapper.writeValueAsString(response))
            .addHeader("Content-Type", "application/json"));

    ValidateActionRequest request =
        ValidateActionRequest.builder().requestedAction(CreditorAction.CREATE_CLAIM).build();
    ValidateActionResponse result = client.validateAction(creditorOrgId, request);

    assertThat(result.isAllowed()).isTrue();
    assertThat(result.getRequestedAction()).isEqualTo(CreditorAction.CREATE_CLAIM);
  }

  @Test
  void validateAction_returnsDeniedResponse() throws JsonProcessingException {
    UUID creditorOrgId = UUID.randomUUID();
    ValidateActionResponse response =
        ValidateActionResponse.builder()
            .allowed(false)
            .requestedAction(CreditorAction.CREATE_CLAIM)
            .reasonCode("CREDITOR_INACTIVE")
            .message("Creditor is not active")
            .build();
    mockWebServer.enqueue(
        new MockResponse()
            .setBody(objectMapper.writeValueAsString(response))
            .addHeader("Content-Type", "application/json"));

    ValidateActionRequest request =
        ValidateActionRequest.builder().requestedAction(CreditorAction.CREATE_CLAIM).build();
    ValidateActionResponse result = client.validateAction(creditorOrgId, request);

    assertThat(result.isAllowed()).isFalse();
    assertThat(result.getReasonCode()).isEqualTo("CREDITOR_INACTIVE");
    assertThat(result.getMessage()).isEqualTo("Creditor is not active");
  }

  @Test
  void validateAction_throwsWhenCreditorNotFound() {
    UUID creditorOrgId = UUID.randomUUID();
    mockWebServer.enqueue(new MockResponse().setResponseCode(404));

    ValidateActionRequest request =
        ValidateActionRequest.builder().requestedAction(CreditorAction.CREATE_CLAIM).build();

    assertThatThrownBy(() -> client.validateAction(creditorOrgId, request))
        .isInstanceOf(OpenDebtException.class)
        .hasMessageContaining("Creditor not found");
  }

  @Test
  void validateAction_throwsOnServerError() {
    UUID creditorOrgId = UUID.randomUUID();
    mockWebServer.enqueue(new MockResponse().setResponseCode(500));

    ValidateActionRequest request =
        ValidateActionRequest.builder().requestedAction(CreditorAction.CREATE_CLAIM).build();

    assertThatThrownBy(() -> client.validateAction(creditorOrgId, request))
        .isInstanceOf(OpenDebtException.class)
        .hasMessageContaining("unavailable");
  }

  @Test
  void isCreditorAllowedToCreateClaim_returnsTrueWhenAllowed() throws JsonProcessingException {
    UUID creditorOrgId = UUID.randomUUID();
    ValidateActionResponse response = ValidateActionResponse.builder().allowed(true).build();
    mockWebServer.enqueue(
        new MockResponse()
            .setBody(objectMapper.writeValueAsString(response))
            .addHeader("Content-Type", "application/json"));

    boolean result = client.isCreditorAllowedToCreateClaim(creditorOrgId);

    assertThat(result).isTrue();
  }

  @Test
  void isCreditorAllowedToCreateClaim_returnsFalseWhenDenied() throws JsonProcessingException {
    UUID creditorOrgId = UUID.randomUUID();
    ValidateActionResponse response = ValidateActionResponse.builder().allowed(false).build();
    mockWebServer.enqueue(
        new MockResponse()
            .setBody(objectMapper.writeValueAsString(response))
            .addHeader("Content-Type", "application/json"));

    boolean result = client.isCreditorAllowedToCreateClaim(creditorOrgId);

    assertThat(result).isFalse();
  }

  @Test
  void isCreditorAllowedToUpdateClaim_returnsTrueWhenAllowed() throws JsonProcessingException {
    UUID creditorOrgId = UUID.randomUUID();
    ValidateActionResponse response = ValidateActionResponse.builder().allowed(true).build();
    mockWebServer.enqueue(
        new MockResponse()
            .setBody(objectMapper.writeValueAsString(response))
            .addHeader("Content-Type", "application/json"));

    boolean result = client.isCreditorAllowedToUpdateClaim(creditorOrgId);

    assertThat(result).isTrue();
  }
}
