package dk.ufst.opendebt.caseworker.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

// Ref: tb021-person-registry-client-integration.md §9.2
class PersonRegistryClientTest {

  private MockWebServer mockWebServer;
  private PersonRegistryClient client;

  @BeforeEach
  void setUp() throws Exception {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
    String baseUrl = mockWebServer.url("/").toString();
    baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    client = new PersonRegistryClient(WebClient.builder(), baseUrl);
  }

  @AfterEach
  void tearDown() throws Exception {
    mockWebServer.shutdown();
  }

  // Ref: §8 AC-13 — null personId returns "—" without HTTP call
  @Test
  void getDisplayName_whenPersonIdIsNull_returnsDash() {
    String result = client.getDisplayName(null);

    assertThat(result).isEqualTo("—");
    assertThat(mockWebServer.getRequestCount()).isZero();
  }

  // Ref: §8 AC-14 — success with valid name
  @Test
  void getDisplayName_whenPersonFound_returnsName() throws InterruptedException {
    UUID personId = UUID.randomUUID();
    String personResponse =
        """
        {"id":"%s","name":"Jens Jensen"}
        """
            .formatted(personId);

    mockWebServer.enqueue(
        new MockResponse().setBody(personResponse).addHeader("Content-Type", "application/json"));

    String result = client.getDisplayName(personId);

    assertThat(result).isEqualTo("Jens Jensen");

    RecordedRequest request = mockWebServer.takeRequest();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getPath()).contains("/person-registry/api/v1/persons/");
  }

  // Ref: §8 AC-15 — name is null in response → "—"
  @Test
  void getDisplayName_whenNameIsNull_returnsDash() throws InterruptedException {
    UUID personId = UUID.randomUUID();
    String personResponse =
        """
        {"id":"%s","name":null}
        """
            .formatted(personId);

    mockWebServer.enqueue(
        new MockResponse().setBody(personResponse).addHeader("Content-Type", "application/json"));

    String result = client.getDisplayName(personId);

    assertThat(result).isEqualTo("—");
  }

  // Ref: §8 AC-16 — name is blank → "—"
  @Test
  void getDisplayName_whenNameIsBlank_returnsDash() throws InterruptedException {
    UUID personId = UUID.randomUUID();
    String personResponse =
        """
        {"id":"%s","name":"   "}
        """
            .formatted(personId);

    mockWebServer.enqueue(
        new MockResponse().setBody(personResponse).addHeader("Content-Type", "application/json"));

    String result = client.getDisplayName(personId);

    assertThat(result).isEqualTo("—");
  }

  // Ref: §8 AC-17 — person not found (404) → "—", no exception
  @Test
  void getDisplayName_whenPersonNotFound_returnsDash() throws InterruptedException {
    UUID personId = UUID.randomUUID();

    mockWebServer.enqueue(new MockResponse().setResponseCode(404));

    String result = client.getDisplayName(personId);

    assertThat(result).isEqualTo("—");
  }

  // Ref: §8 AC-18 — 5xx → WebClientResponseException (circuit breaker not active in unit tests;
  // fallback returns "—" in Spring context)
  @Test
  void getDisplayName_whenServerError_throwsWebClientResponseException() {
    UUID personId = UUID.randomUUID();

    mockWebServer.enqueue(new MockResponse().setResponseCode(503));

    assertThatThrownBy(() -> client.getDisplayName(personId))
        .isInstanceOf(WebClientResponseException.class);
  }
}
