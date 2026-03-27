package dk.ufst.opendebt.creditor.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.databind.ObjectMapper;

import dk.ufst.opendebt.creditor.dto.DebtorVerificationResultDto;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

// Ref: tb021-person-registry-client-integration.md §9.1
class PersonRegistryClientTest {

  private MockWebServer mockWebServer;
  private PersonRegistryClient client;
  private final ObjectMapper objectMapper = new ObjectMapper();

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

  // Ref: §8 AC-1 — new person (created=true) returns verified=true
  @Test
  void verifyCpr_whenNewPersonCreated_returnsVerifiedTrue() throws InterruptedException {
    UUID personId = UUID.randomUUID();
    String lookupResponse =
        """
        {"personId":"%s","created":true,"role":"PERSONAL"}
        """
            .formatted(personId);

    mockWebServer.enqueue(
        new MockResponse().setBody(lookupResponse).addHeader("Content-Type", "application/json"));

    DebtorVerificationResultDto result = client.verifyCpr("0101800001", "Jens", "Jensen");

    assertThat(result.isVerified()).isTrue();
    assertThat(result.getPersonId()).isEqualTo(personId);
    assertThat(result.getDisplayName()).isEqualTo("Jens Jensen");
    assertThat(result.getErrorMessage()).isNull();

    RecordedRequest request = mockWebServer.takeRequest();
    assertThat(request.getMethod()).isEqualTo("POST");
    assertThat(request.getPath()).isEqualTo("/person-registry/api/v1/persons/lookup");
    assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
  }

  // Ref: §8 AC-2 — existing person, names match → verified=true
  @Test
  void verifyCpr_whenExistingPersonNamesMatch_returnsVerifiedTrue() throws InterruptedException {
    UUID personId = UUID.randomUUID();
    String lookupResponse =
        """
        {"personId":"%s","created":false,"role":"PERSONAL"}
        """
            .formatted(personId);
    String personResponse =
        """
        {"id":"%s","name":"Jens Jensen"}
        """
            .formatted(personId);

    mockWebServer.enqueue(
        new MockResponse().setBody(lookupResponse).addHeader("Content-Type", "application/json"));
    mockWebServer.enqueue(
        new MockResponse().setBody(personResponse).addHeader("Content-Type", "application/json"));

    DebtorVerificationResultDto result = client.verifyCpr("0101800001", "Jens", "Jensen");

    assertThat(result.isVerified()).isTrue();
    assertThat(result.getPersonId()).isEqualTo(personId);
    assertThat(result.getDisplayName()).isEqualTo("Jens Jensen");

    mockWebServer.takeRequest(); // POST lookup
    RecordedRequest getRequest = mockWebServer.takeRequest();
    assertThat(getRequest.getMethod()).isEqualTo("GET");
    assertThat(getRequest.getPath()).contains("/person-registry/api/v1/persons/");
  }

  // Ref: §8 AC-3 — names match with accent normalization (Ångström case)
  @Test
  void verifyCpr_whenNamesMatchWithAccentNormalization_returnsVerifiedTrue()
      throws InterruptedException {
    UUID personId = UUID.randomUUID();
    String lookupResponse =
        """
        {"personId":"%s","created":false,"role":"PERSONAL"}
        """
            .formatted(personId);
    // Stored name has accent; input is uppercase — after NFD+strip+lowercase both equal "soren
    // angstrom"
    String personResponse =
        """
        {"id":"%s","name":"S\u00f8ren \u00c5ngstr\u00f6m"}
        """
            .formatted(personId);

    mockWebServer.enqueue(
        new MockResponse().setBody(lookupResponse).addHeader("Content-Type", "application/json"));
    mockWebServer.enqueue(
        new MockResponse().setBody(personResponse).addHeader("Content-Type", "application/json"));

    // Input matches after normalization: "Søren Ångström" → NFD+strip+lower = "søren angstrom"
    // "Søren Ångström" → same normalization
    DebtorVerificationResultDto result =
        client.verifyCpr("0101800001", "S\u00f8ren", "\u00c5ngstr\u00f6m");

    assertThat(result.isVerified()).isTrue();
    assertThat(result.getDisplayName()).isEqualTo("S\u00f8ren \u00c5ngstr\u00f6m");
  }

  // Ref: §8 AC-4 — existing person, names do not match → verified=false
  @Test
  void verifyCpr_whenNamesDoNotMatch_returnsVerifiedFalse() throws InterruptedException {
    UUID personId = UUID.randomUUID();
    String lookupResponse =
        """
        {"personId":"%s","created":false,"role":"PERSONAL"}
        """
            .formatted(personId);
    String personResponse =
        """
        {"id":"%s","name":"Søren Jensen"}
        """
            .formatted(personId);

    mockWebServer.enqueue(
        new MockResponse().setBody(lookupResponse).addHeader("Content-Type", "application/json"));
    mockWebServer.enqueue(
        new MockResponse().setBody(personResponse).addHeader("Content-Type", "application/json"));

    DebtorVerificationResultDto result = client.verifyCpr("0101800001", "Jens", "Jensen");

    assertThat(result.isVerified()).isFalse();
    assertThat(result.getErrorMessage()).isEqualTo("Navn matcher ikke");
    assertThat(result.getPersonId()).isNull();
  }

  // Ref: §8 AC-5 — GET /persons/{id} returns 404 → verified=false, no exception
  @Test
  void verifyCpr_whenPersonGetReturns404_returnsVerifiedFalse() throws InterruptedException {
    UUID personId = UUID.randomUUID();
    String lookupResponse =
        """
        {"personId":"%s","created":false,"role":"PERSONAL"}
        """
            .formatted(personId);

    mockWebServer.enqueue(
        new MockResponse().setBody(lookupResponse).addHeader("Content-Type", "application/json"));
    mockWebServer.enqueue(new MockResponse().setResponseCode(404));

    DebtorVerificationResultDto result = client.verifyCpr("0101800001", "Jens", "Jensen");

    assertThat(result.isVerified()).isFalse();
    assertThat(result.getErrorMessage()).isEqualTo("Navn matcher ikke");
  }

  // Ref: §8 AC-5a — GET /persons/{id} returns 200 with name=null → verified=false, no NPE
  @Test
  void verifyCpr_whenStoredNameIsNull_returnsVerifiedFalse() throws InterruptedException {
    UUID personId = UUID.randomUUID();
    String lookupResponse =
        """
        {"personId":"%s","created":false,"role":"PERSONAL"}
        """
            .formatted(personId);
    String personResponse =
        """
        {"id":"%s","name":null}
        """
            .formatted(personId);

    mockWebServer.enqueue(
        new MockResponse().setBody(lookupResponse).addHeader("Content-Type", "application/json"));
    mockWebServer.enqueue(
        new MockResponse().setBody(personResponse).addHeader("Content-Type", "application/json"));

    DebtorVerificationResultDto result = client.verifyCpr("0101800001", "Jens", "Jensen");

    assertThat(result.isVerified()).isFalse();
    assertThat(result.getErrorMessage()).isEqualTo("Navn matcher ikke");
  }

  // Ref: §8 AC-7 — POST /persons/lookup returns 4xx → WebClientResponseException propagated
  @Test
  void verifyCpr_whenLookupReturns4xx_throwsWebClientResponseException() {
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(400)
            .setBody("Bad request")
            .addHeader("Content-Type", "application/json"));

    assertThatThrownBy(() -> client.verifyCpr("0101800001", "Jens", "Jensen"))
        .isInstanceOf(WebClientResponseException.class);
  }

  // Ref: §8 AC-9 — verifyCvr success
  @Test
  void verifyCvr_happyPath_returnsVerifiedTrueWithCvrDisplayName() throws InterruptedException {
    UUID orgId = UUID.randomUUID();
    String orgResponse =
        """
        {"organizationId":"%s"}
        """
            .formatted(orgId);

    mockWebServer.enqueue(
        new MockResponse().setBody(orgResponse).addHeader("Content-Type", "application/json"));

    DebtorVerificationResultDto result = client.verifyCvr("12345678");

    assertThat(result.isVerified()).isTrue();
    assertThat(result.getPersonId()).isEqualTo(orgId);
    assertThat(result.getDisplayName()).isEqualTo("CVR: 12345678");

    RecordedRequest request = mockWebServer.takeRequest();
    assertThat(request.getMethod()).isEqualTo("POST");
    assertThat(request.getPath()).isEqualTo("/person-registry/api/v1/organizations/lookup");
    String body = request.getBody().readUtf8();
    assertThat(body).contains("\"cvr\"").contains("12345678");
  }

  // Ref: §8 AC-11 — verifySe success
  @Test
  void verifySe_happyPath_returnsVerifiedTrueWithSeDisplayName() throws InterruptedException {
    UUID orgId = UUID.randomUUID();
    String orgResponse =
        """
        {"organizationId":"%s"}
        """
            .formatted(orgId);

    mockWebServer.enqueue(
        new MockResponse().setBody(orgResponse).addHeader("Content-Type", "application/json"));

    DebtorVerificationResultDto result = client.verifySe("87654321");

    assertThat(result.isVerified()).isTrue();
    assertThat(result.getPersonId()).isEqualTo(orgId);
    assertThat(result.getDisplayName()).isEqualTo("SE: 87654321");

    RecordedRequest request = mockWebServer.takeRequest();
    assertThat(request.getMethod()).isEqualTo("POST");
    assertThat(request.getPath()).isEqualTo("/person-registry/api/v1/organizations/lookup");
    String body = request.getBody().readUtf8();
    assertThat(body).contains("\"cvr\"").contains("87654321");
  }
}
