package dk.ufst.opendebt.citizen.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

class PersonRegistryClientTest {

  private MockWebServer mockWebServer;
  private PersonRegistryClient client;

  @BeforeEach
  void setUp() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();

    String baseUrl = mockWebServer.url("/").toString();
    client = new PersonRegistryClient(WebClient.builder(), baseUrl);
  }

  @AfterEach
  void tearDown() throws IOException {
    mockWebServer.shutdown();
  }

  @Test
  void lookupOrCreatePerson_returnsPersonId() throws InterruptedException {
    UUID expectedId = UUID.randomUUID();
    mockWebServer.enqueue(
        new MockResponse()
            .setBody("{\"personId\":\"" + expectedId + "\",\"created\":false}")
            .addHeader("Content-Type", "application/json"));

    UUID result = client.lookupOrCreatePerson("0101011234");

    assertThat(result).isEqualTo(expectedId);

    RecordedRequest request = mockWebServer.takeRequest();
    assertThat(request.getMethod()).isEqualTo("POST");
    assertThat(request.getPath()).isEqualTo("/api/v1/persons/lookup");
    assertThat(request.getBody().readUtf8()).contains("\"identifier\":\"0101011234\"");
  }

  @Test
  void lookupOrCreatePerson_createsNewPerson() throws InterruptedException {
    UUID expectedId = UUID.randomUUID();
    mockWebServer.enqueue(
        new MockResponse()
            .setBody("{\"personId\":\"" + expectedId + "\",\"created\":true}")
            .addHeader("Content-Type", "application/json"));

    UUID result = client.lookupOrCreatePerson("0202021234");

    assertThat(result).isEqualTo(expectedId);
  }

  @Test
  void lookupOrCreatePerson_throwsOnServerError() {
    mockWebServer.enqueue(new MockResponse().setResponseCode(500));

    assertThatThrownBy(() -> client.lookupOrCreatePerson("0303031234"))
        .isInstanceOf(Exception.class);
  }

  @Test
  void lookupOrCreatePerson_requestContainsIdentifierType() throws InterruptedException {
    UUID expectedId = UUID.randomUUID();
    mockWebServer.enqueue(
        new MockResponse()
            .setBody("{\"personId\":\"" + expectedId + "\",\"created\":false}")
            .addHeader("Content-Type", "application/json"));

    client.lookupOrCreatePerson("0101011234");

    RecordedRequest request = mockWebServer.takeRequest();
    assertThat(request.getBody().readUtf8()).contains("\"identifierType\":\"CPR\"");
  }
}
