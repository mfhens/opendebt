package dk.ufst.opendebt.citizen.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

class CitizenDebtClientTest {

  private MockWebServer mockWebServer;

  @BeforeEach
  void setUp() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();
  }

  @AfterEach
  void tearDown() throws IOException {
    mockWebServer.shutdown();
  }

  @Test
  void getDebtSummary_mapsBrowserPageToPageNumberAndUsesConfiguredPageSize() throws Exception {
    // Ref: petitions/specs/petition026-specs.yaml:107-117, 268-273
    mockWebServer.enqueue(
        new MockResponse()
            .setBody(
                """
                {
                  "debts": [],
                  "totalOutstandingAmount": 0,
                  "totalDebtCount": 0,
                  "pageNumber": 2,
                  "pageSize": 100,
                  "totalPages": 0,
                  "totalElements": 0,
                  "effectiveInterestRates": []
                }
                """)
            .addHeader("Content-Type", "application/json"));

    Object client = instantiateClient(mockWebServer.url("/").toString(), 100);
    invokeGetDebtSummary(client, UUID.randomUUID(), 2);

    RecordedRequest request = mockWebServer.takeRequest();
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getPath()).startsWith("/api/v1/citizen/debts");
    assertThat(request.getPath()).contains("pageNumber=2");
    assertThat(request.getPath()).contains("pageSize=100");
    assertThat(request.getHeader("X-Person-Id")).isNotBlank();
  }

  private Object instantiateClient(String baseUrl, int pageSize) throws Exception {
    Class<?> clientClass = Class.forName("dk.ufst.opendebt.citizen.client.CitizenDebtClient");
    Constructor<?> constructor =
        clientClass.getDeclaredConstructor(WebClient.Builder.class, String.class, int.class);
    return constructor.newInstance(WebClient.builder(), trimTrailingSlash(baseUrl), pageSize);
  }

  private void invokeGetDebtSummary(Object client, UUID personId, int pageNumber) throws Exception {
    Method method =
        findMethod(client.getClass(), "getDebtSummary", UUID.class, int.class, int.class);
    if (method.getParameterCount() == 2) {
      method.invoke(client, personId, pageNumber);
      return;
    }
    if (method.getParameterCount() == 1) {
      method.invoke(client, pageNumber);
      return;
    }
    throw new AssertionError("CitizenDebtClient#getDebtSummary must accept page input directly.");
  }

  private Method findMethod(Class<?> type, String name, Class<?>... preferredParameters)
      throws NoSuchMethodException {
    for (Method method : type.getMethods()) {
      if (!method.getName().equals(name)) {
        continue;
      }
      Class<?>[] parameterTypes = method.getParameterTypes();
      if (parameterTypes.length == 2
          && parameterTypes[0].equals(preferredParameters[0])
          && parameterTypes[1].equals(preferredParameters[1])) {
        return method;
      }
      if (parameterTypes.length == 1 && parameterTypes[0].equals(preferredParameters[1])) {
        return method;
      }
    }
    throw new NoSuchMethodException(type.getName() + "#" + name);
  }

  private String trimTrailingSlash(String baseUrl) {
    return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
  }
}
