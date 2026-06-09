package dk.ufst.opendebt.citizen.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@SpringBootTest(
    properties = {
      "spring.autoconfigure.exclude="
          + "org.springframework.boot.autoconfigure.security.oauth2.resource.servlet."
          + "OAuth2ResourceServerAutoConfiguration",
      "spring.security.oauth2.client.registration.tastselv.client-id=test-client",
      "spring.security.oauth2.client.registration.tastselv.client-secret=test-secret",
      "spring.security.oauth2.client.registration.tastselv.scope=openid,profile",
      "spring.security.oauth2.client.registration.tastselv.authorization-grant-type=authorization_code",
      "spring.security.oauth2.client.registration.tastselv.redirect-uri="
          + "{baseUrl}/login/oauth2/code/{registrationId}",
      "spring.security.oauth2.client.provider.tastselv.authorization-uri="
          + "https://example.test/oauth2/authorize",
      "spring.security.oauth2.client.provider.tastselv.token-uri=https://example.test/oauth2/token",
      "spring.security.oauth2.client.provider.tastselv.user-info-uri="
          + "https://example.test/oauth2/userinfo",
      "spring.security.oauth2.client.provider.tastselv.jwk-set-uri=https://example.test/oauth2/jwks",
      "opendebt.citizen.external-links.mit-gaeldsoverblik=/min-gaeld",
      "opendebt.citizen.debt-overview.page-size=100"
    })
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DebtOverviewPageMvcTest {

  private static final MockWebServer DEBT_SERVICE = startDebtService();
  private static final UUID PERSON_ID = UUID.randomUUID();

  @Autowired private MockMvc mockMvc;

  @BeforeEach
  void clearRecordedRequests() throws InterruptedException {
    while (DEBT_SERVICE.takeRequest(10, TimeUnit.MILLISECONDS) != null) {
      // Drain requests from prior test methods so per-test assertions stay deterministic.
    }
  }

  @DynamicPropertySource
  static void registerDebtServiceBaseUrl(DynamicPropertyRegistry registry) {
    registry.add(
        "opendebt.services.debt-service.url",
        () -> trimTrailingSlash(DEBT_SERVICE.url("/").toString()));
  }

  @AfterAll
  static void stopDebtService() throws IOException {
    DEBT_SERVICE.shutdown();
  }

  @Test
  void getMinGaeld_unauthenticatedRedirectsIntoTastSelvLoginFlow() throws Exception {
    // Ref: petitions/specs/petition026-specs.yaml:89-99, 268-280
    mockMvc
        .perform(get("/min-gaeld"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrlPattern("**/oauth2/authorization/tastselv"));
  }

  @Test
  void getMinGaeld_authenticatedCallsDebtServiceWithConfiguredPagingAndSemanticTable()
      throws Exception {
    // Ref: petitions/specs/petition026-specs.yaml:112-170, 268-296
    DEBT_SERVICE.enqueue(
        new MockResponse()
            .setBody(
                """
                {
                  "debts": [
                    {
                      "debtId": "24b0be63-7d38-48bb-a628-d263ba9fa8ba",
                      "debtTypeName": "Property tax",
                      "debtTypeCode": "PROPERTY_TAX",
                      "creditorDisplayName": "Skattestyrelsen",
                      "principalAmount": 1000.00,
                      "outstandingAmount": 1200.00,
                      "interestAmount": 200.00,
                      "feesAmount": 0.00,
                      "dueDate": "2026-01-15",
                      "status": "IN_COLLECTION",
                      "citizenStatus": "IN_COLLECTION",
                      "interestAccrualState": "ACTIVE",
                      "interestRuleCode": "INDR_STD"
                    }
                  ],
                  "totalOutstandingAmount": 1200.00,
                  "totalDebtCount": 1,
                  "pageNumber": 2,
                  "pageSize": 100,
                  "totalPages": 3,
                  "totalElements": 201,
                  "effectiveInterestRates": [
                    {
                      "interestRuleCode": "INDR_STD",
                      "annualRate": 5.75,
                      "validFrom": "2026-01-01"
                    }
                  ]
                }
                """)
            .addHeader("Content-Type", "application/json"));

    mockMvc
        .perform(get("/min-gaeld").param("page", "2").session(authenticatedCitizenSession()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("<caption")))
        .andExpect(content().string(containsString("<thead")))
        .andExpect(content().string(containsString("scope=\"col\"")));

    RecordedRequest request = DEBT_SERVICE.takeRequest(1, TimeUnit.SECONDS);
    assertThat(request).isNotNull();
    assertThat(request.getPath()).startsWith("/api/v1/citizen/debts");
    assertThat(request.getPath()).contains("pageNumber=2");
    assertThat(request.getPath()).contains("pageSize=100");
    assertThat(request.getHeader("X-Person-Id")).isEqualTo(PERSON_ID.toString());
  }

  @Test
  void getMinGaeld_noDebtRendersAccessibleEmptyStateWithoutEmptyTable() throws Exception {
    // Ref: petitions/specs/petition026-specs.yaml:156-170, 268-296
    DEBT_SERVICE.enqueue(
        new MockResponse()
            .setBody(
                """
                {
                  "debts": [],
                  "totalOutstandingAmount": 0,
                  "totalDebtCount": 0,
                  "pageNumber": 0,
                  "pageSize": 100,
                  "totalPages": 0,
                  "totalElements": 0,
                  "effectiveInterestRates": []
                }
                """)
            .addHeader("Content-Type", "application/json"));

    mockMvc
        .perform(get("/min-gaeld").session(authenticatedCitizenSession()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("role=\"status\"")))
        .andExpect(content().string(not(containsString("<table"))));
  }

  @Test
  void getMinGaeld_serviceUnavailableRendersAccessibleAlertWithoutStackTrace() throws Exception {
    // Ref: petitions/specs/petition026-specs.yaml:156-159, 283-296
    DEBT_SERVICE.enqueue(new MockResponse().setResponseCode(503));

    mockMvc
        .perform(get("/min-gaeld").session(authenticatedCitizenSession()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("role=\"alert\"")))
        .andExpect(content().string(not(containsString("Exception"))))
        .andExpect(content().string(not(containsString("java.lang"))));
  }

  private MockHttpSession authenticatedCitizenSession() {
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(
            "citizen-user", "n/a", List.of(new SimpleGrantedAuthority("ROLE_CITIZEN")));
    SecurityContext context = new SecurityContextImpl(authentication);

    MockHttpSession session = new MockHttpSession();
    session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    session.setAttribute("person_id", PERSON_ID);
    return session;
  }

  private static MockWebServer startDebtService() {
    MockWebServer server = new MockWebServer();
    try {
      server.start();
    } catch (IOException e) {
      throw new IllegalStateException("Unable to start debt-service mock server", e);
    }
    return server;
  }

  private static String trimTrailingSlash(String baseUrl) {
    return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
  }
}
