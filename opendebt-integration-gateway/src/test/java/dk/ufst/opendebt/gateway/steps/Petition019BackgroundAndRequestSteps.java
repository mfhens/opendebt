package dk.ufst.opendebt.gateway.steps;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import com.github.tomakehurst.wiremock.WireMockServer;

import dk.ufst.opendebt.common.audit.cls.ClsAuditEvent;
import dk.ufst.opendebt.gateway.TestClsAuditCapture;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;

public class Petition019BackgroundAndRequestSteps {

  @Autowired private Petition019ScenarioContext ctx;
  @Autowired private TestClsAuditCapture clsCapture;
  @Autowired private WireMockServer wireMockServer;
  @LocalServerPort private int serverPort;

  private final RestTemplate restTemplate = new RestTemplate();

  @Before
  public void setUpWireMock() {
    wireMockServer.resetAll();
    clsCapture.reset();
  }

  @After
  public void tearDown() {
    // WireMock lifecycle managed by Spring — no explicit stop needed
  }

  // ═══════════════════ Background steps ═══════════════════

  @Given("integration-gateway is running and listening for SOAP requests")
  public void integrationGatewayIsRunning() {
    // Spring Boot already started with RANDOM_PORT
  }

  @And("CLS logging is configured and accessible")
  public void clsLoggingIsConfigured() {
    // TestClsAuditCapture captures all events
  }

  @And("SOAP endpoint security is configured for OCES3 certificate validation")
  public void soapSecurityConfigured() {
    // Configured via application-test.yml: tls-termination-mode=TEST
  }

  @And("fordringshaver {string} has a valid OCES3 certificate")
  public void fordringsshaverHasValidOces3Certificate(String creditorId) {
    if (ctx.getCreditorId() == null) {
      ctx.setCreditorId(creditorId);
    }
  }

  @And("fordringshaver {string} is authorized to submit claims")
  public void fordringsshaverIsAuthorizedToSubmitClaims(String creditorId) {
    ctx.setCreditorAuthorized(true);
  }

  // ═══════════════════ Certificate setup steps ═══════════════════

  @Given("fordringshaver {string} has OCES3 certificate {string}")
  public void fordringsshaverHasOces3Certificate(String creditorId, String certId) {
    ctx.setCreditorId(creditorId);
    ctx.setCertId(certId);
    ctx.setCertExpired(false);
    ctx.setCertMissing(false);
  }

  @Given("fordringshaver {string} has expired OCES3 certificate {string}")
  public void fordringsshaverHasExpiredOces3Certificate(String creditorId, String certId) {
    ctx.setCreditorId(creditorId);
    ctx.setCertId(certId);
    ctx.setCertExpired(true);
    ctx.setCertMissing(false);
  }

  @Given("fordringshaver {string} has valid OCES3 certificate {string}")
  public void fordringsshaverHasValidNamedOces3Certificate(String creditorId, String certId) {
    ctx.setCreditorId(creditorId);
    ctx.setCertId(certId);
    ctx.setCertExpired(false);
    ctx.setCertMissing(false);
  }

  @Given("no OCES3 certificate is provided with the SOAP request")
  public void noOces3CertificateProvided() {
    ctx.setCertMissing(true);
  }

  @And("fordringshaver {string} is not authorized to submit claims")
  public void fordringsshaverIsNotAuthorizedToSubmitClaims(String creditorId) {
    ctx.setCreditorAuthorized(false);
  }

  @Given("OCES3 certificate {string} has subject {string}")
  public void oces3CertificateHasSubject(String certId, String dnSubject) {
    ctx.setCertId(certId);
    ctx.setCertSubject(dnSubject);
    String cn = extractCnFromDn(dnSubject);
    ctx.setCreditorId(cn);
    ctx.setExtractedFordringshaverId(cn);
  }

  // ═══════════════════ Claim pre-conditions ═══════════════════

  @Given("claim {string} exists with status {string}")
  public void claimExistsWithStatus(String claimId, String status) {
    ctx.setClaimId(claimId);
    ctx.setClaimStatus(status);
  }

  @And("claim {string} has receipt {string}")
  public void claimHasReceipt(String claimId, String receiptId) {
    ctx.setReceiptId(receiptId);
  }

  @Given("claim {string} exists with debtor {string}")
  public void claimExistsWithDebtor(String claimId, String debtorId) {
    ctx.setClaimId(claimId);
    ctx.setDebtorId(debtorId);
  }

  @And("claim {string} has {int} notifications")
  public void claimHasNotifications(String claimId, int count) {
    ctx.setNotificationCount(count);
  }

  // ═══════════════════ SOAP request configuration ═══════════════════

  @And("the SOAP request is addressed to {word}")
  public void theSoapRequestIsAddressedTo(String serviceName) {
    ctx.setTargetService(serviceName);
  }

  @And("the SOAP request is in OIO namespace {string}")
  public void theSoapRequestIsInOioNamespace(String namespace) {
    ctx.setSoapNamespace(namespace);
  }

  @And("the SOAP request is in SKAT namespace {string}")
  public void theSoapRequestIsInSkatNamespace(String namespace) {
    ctx.setSoapNamespace(namespace);
  }

  @And("the SOAP request operation is {string}")
  public void theSoapRequestOperationIs(String operation) {
    ctx.setSoapOperation(operation);
  }

  // ═══════════════════ Request body variants ═══════════════════

  @And("the SOAP request body contains a valid OIO-formatted claim")
  public void theSoapRequestBodyContainsValidOioFormattedClaim() {
    ctx.setRequestBodyVariant("oio-valid-claim");
  }

  @And("the SOAP request body contains an OIO-formatted claim with invalid amount \\(0 DKK\\)")
  public void theSoapRequestBodyContainsOioFormattedClaimWithInvalidAmountZero() {
    ctx.setRequestBodyVariant("oio-invalid-amount-zero");
  }

  @And("the SOAP request contains an OIO-formatted claim with amount -100 DKK")
  public void theSoapRequestContainsOioFormattedClaimWithAmountMinusHundred() {
    ctx.setRequestBodyVariant("oio-invalid-amount-negative");
  }

  @And("the SOAP request body contains an OIO-formatted claim with invalid amount")
  public void theSoapRequestBodyContainsOioFormattedClaimWithInvalidAmount() {
    ctx.setRequestBodyVariant("oio-invalid-amount-general");
  }

  @And("the SOAP request body is a valid instance of OIO XSD schema {string}")
  public void theSoapRequestBodyIsValidInstanceOfOioXsdSchema(String schemaNamespace) {
    ctx.setSoapNamespace(schemaNamespace);
    ctx.setRequestBodyVariant("oio-xsd-valid");
  }

  @And("the SOAP request body contains a valid SKAT-formatted claim")
  public void theSoapRequestBodyContainsValidSkatFormattedClaim() {
    ctx.setRequestBodyVariant("skat-valid-claim");
  }

  @And("the SOAP request body contains a SKAT-formatted claim with missing required field")
  public void theSoapRequestBodyContainsSkatFormattedClaimWithMissingRequiredField() {
    ctx.setRequestBodyVariant("skat-missing-required-field");
  }

  @And("the SOAP request body is a valid instance of SKAT XSD schema {string}")
  public void theSoapRequestBodyIsValidInstanceOfSkatXsdSchema(String schemaNamespace) {
    ctx.setSoapNamespace(schemaNamespace);
    ctx.setRequestBodyVariant("skat-xsd-valid");
  }

  @And("the SOAP request body contains claim ID {string}")
  public void theSoapRequestBodyContainsClaimId(String claimId) {
    ctx.setClaimId(claimId);
    ctx.setRequestBodyVariant("kvittering-or-underret-request");
  }

  @And("the SOAP request contains a valid OIO-formatted claim")
  public void theSoapRequestContainsValidOioFormattedClaim() {
    ctx.setRequestBodyVariant("oio-valid-claim-security");
  }

  @And("the SOAP request contains an invalid OIO-formatted claim")
  public void theSoapRequestContainsInvalidOioFormattedClaim() {
    ctx.setRequestBodyVariant("oio-invalid-for-logging");
  }

  @And("the SOAP request contains a valid OIO-formatted claim with debtor CPR")
  public void theSoapRequestContainsValidOioFormattedClaimWithDebtorCpr() {
    ctx.setRequestBodyVariant("oio-claim-with-cpr");
  }

  @And("the SOAP request does not conform to SOAP 1.1 or SOAP 1.2 specification")
  public void theSoapRequestDoesNotConformToSoapSpecification() {
    ctx.setRequestBodyVariant("malformed-soap");
  }

  @And("the SOAP request uses SOAP 1.1 protocol")
  public void theSoapRequestUsesSoap11Protocol() {
    ctx.setSoapProtocol("1.1");
  }

  @And("the SOAP request uses SOAP 1.2 protocol")
  public void theSoapRequestUsesSoap12Protocol() {
    ctx.setSoapProtocol("1.2");
  }

  @Given("a SOAP request to {word} contains correlation ID {string}")
  public void aSoapRequestToServiceContainsCorrelationId(String serviceName, String correlationId) {
    ctx.setTargetService(serviceName);
    ctx.setRequestCorrelationId(correlationId);
    ctx.setRequestBodyVariant("oio-valid-claim");
  }

  // ═══════════════════ When steps ═══════════════════

  @When("the SOAP request is processed")
  public void theSoapRequestIsProcessed() {
    setupWireMockStubs();

    String targetService =
        ctx.getTargetService() != null ? ctx.getTargetService() : "OIOFordringIndberetService";
    String baseUrl = isSkatService(targetService) ? "/soap/skat" : "/soap/oio";
    String url = "http://localhost:" + serverPort + baseUrl;

    String soapProtocol = ctx.getSoapProtocol() != null ? ctx.getSoapProtocol() : "1.1";
    String contentType =
        "1.2".equals(soapProtocol)
            ? "application/soap+xml; charset=utf-8"
            : "text/xml; charset=utf-8";

    String body = buildRequestBody(targetService);
    if ("1.2".equals(soapProtocol)) {
      body = convertToSoap12(body);
    }

    HttpHeaders headers = new HttpHeaders();
    headers.set("Content-Type", contentType);
    headers.set("SOAPAction", "");

    if (!ctx.isCertMissing()) {
      String creditorId = resolveCreditorId();
      headers.set("X-Test-Fordringshaver-Id", creditorId);
    }
    if (ctx.isCertExpired()) {
      headers.set("X-Test-Cert-Expired", "true");
    }
    if (ctx.getRequestCorrelationId() != null) {
      headers.set("X-Correlation-Id", ctx.getRequestCorrelationId());
    }

    HttpEntity<String> request = new HttpEntity<>(body, headers);

    String responseBody;
    int statusCode;
    try {
      ResponseEntity<String> response =
          restTemplate.exchange(url, HttpMethod.POST, request, String.class);
      statusCode = response.getStatusCode().value();
      responseBody = response.getBody() != null ? response.getBody() : "";
    } catch (RestClientResponseException e) {
      statusCode = e.getStatusCode().value();
      responseBody = e.getResponseBodyAsString();
    } catch (Exception e) {
      statusCode = 500;
      responseBody = "<error>" + e.getMessage() + "</error>";
    }

    ctx.setSoapHttpStatus(statusCode);
    ctx.setSoapResponseBody(responseBody);
    // Treat any non-2xx response as a fault (SOAP fault body, auth rejection, or parse error)
    ctx.setSoapResponseFault(
        responseBody.contains("Fault") || responseBody.contains("fault") || statusCode >= 400);
    ctx.setSoapResponseSuccess(!ctx.isSoapResponseFault() && statusCode < 300);

    // Determine protocol from response
    if (responseBody.contains("http://schemas.xmlsoap.org/soap/envelope/")) {
      ctx.setSoapResponseProtocol("1.1");
    } else if (responseBody.contains("http://www.w3.org/2003/05/soap-envelope")) {
      ctx.setSoapResponseProtocol("1.2");
    }

    // Parse fault info
    if (ctx.isSoapResponseFault()) {
      ctx.setFaultCode(extractXml(responseBody, "faultcode", "Code"));
      String faultMsg = extractXml(responseBody, "faultstring", "Reason");
      if (faultMsg == null && responseBody.contains("faultstring")) {
        // faultstring present but regex failed — extract with simple indexOf fallback
        int start = responseBody.indexOf(">", responseBody.indexOf("faultstring")) + 1;
        int end = responseBody.indexOf("<", start);
        if (start > 0 && end > start) faultMsg = responseBody.substring(start, end).trim();
      }
      ctx.setFaultMessage(faultMsg);
      boolean hasFaultDetail = responseBody.contains("ValidationError");
      ctx.setFaultDetailHasFieldErrors(hasFaultDetail);
      if (hasFaultDetail) {
        ctx.setFaultDetailField(extractXml(responseBody, "Field"));
        ctx.setFaultDetailErrorMessage(extractXml(responseBody, "Message"));
      }
      checkAndAddElement(responseBody, "faultcode");
      checkAndAddElement(responseBody, "faultstring");
      checkAndAddElement(responseBody, "detail");
      checkAndAddElement(responseBody, "Code");
      checkAndAddElement(responseBody, "Reason");
      checkAndAddElement(responseBody, "Detail");
    }

    // Parse success response fields
    if (!ctx.isSoapResponseFault()) {
      ctx.setResponseClaimId(extractXml(responseBody, "FordringsId"));
      ctx.setResponseReceiptId(extractXml(responseBody, "KvitteringId"));
      ctx.setResponseClaimStatus(extractXml(responseBody, "Status"));
      ctx.setResponseNotificationCount(countElements(responseBody, "Underretning"));
      ctx.setResponseSameConnection(true);
      ctx.setResponseContainsReplyTo(responseBody.contains("ReplyTo"));
      ctx.setResponseContainsAsyncCallback(
          responseBody.contains("AsyncCallback") || responseBody.contains("callback"));
    }

    // Capture CLS event
    ClsAuditEvent lastEvent = clsCapture.getLastEvent();
    if (lastEvent != null) {
      ctx.setCapturedClsEvent(lastEvent);
      ctx.setClsTimestamp(lastEvent.getTimestamp());
      ctx.setClsCallingSystem(lastEvent.getUserId());
      String operation = lastEvent.getOperation();
      if (operation != null && operation.contains(".")) {
        ctx.setClsServiceName(operation.substring(0, operation.lastIndexOf(".")));
        ctx.setClsOperationName(operation.substring(operation.lastIndexOf(".") + 1));
      }
      ctx.setClsCorrelationId(lastEvent.getCorrelationId());
      Object rtMs =
          lastEvent.getNewValues() != null ? lastEvent.getNewValues().get("responseTimeMs") : null;
      if (rtMs != null) ctx.setClsResponseTimeMs(Long.parseLong(String.valueOf(rtMs)));
      Object status =
          lastEvent.getNewValues() != null ? lastEvent.getNewValues().get("status") : null;
      if (status != null) ctx.setClsStatus(String.valueOf(status));
      Object faultCode =
          lastEvent.getNewValues() != null ? lastEvent.getNewValues().get("faultCode") : null;
      if (faultCode != null) ctx.setClsErrorCode(String.valueOf(faultCode));
      Object faultMsg =
          lastEvent.getNewValues() != null ? lastEvent.getNewValues().get("faultMessage") : null;
      if (faultMsg != null) ctx.setClsErrorMessage(String.valueOf(faultMsg));
      Object stackTrace =
          lastEvent.getNewValues() != null ? lastEvent.getNewValues().get("stackTrace") : null;
      if (stackTrace != null) ctx.setClsStackTrace(String.valueOf(stackTrace));
      Object reqBody =
          lastEvent.getNewValues() != null ? lastEvent.getNewValues().get("requestBody") : null;
      if (reqBody != null) ctx.setClsRequestBody(String.valueOf(reqBody));
      Object respBody =
          lastEvent.getNewValues() != null ? lastEvent.getNewValues().get("responseBody") : null;
      if (respBody != null) ctx.setClsResponseBody(String.valueOf(respBody));
    }

    // Correlation ID echo
    if (ctx.getRequestCorrelationId() != null && ctx.getClsCorrelationId() != null) {
      ctx.setResponseCorrelationId(ctx.getClsCorrelationId());
    }
  }

  @When("a GET request is made to {string}")
  public void aGetRequestIsMadeTo(String path) {
    String url = "http://localhost:" + serverPort + path;
    try {
      ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
      ctx.setWsdlHttpStatus(response.getStatusCode().value());
      ctx.setWsdlBody(response.getBody() != null ? response.getBody() : "");
      String contentType = response.getHeaders().getFirst("Content-Type");
      ctx.setWsdlContentType(contentType != null ? contentType : "");
    } catch (RestClientResponseException e) {
      ctx.setWsdlHttpStatus(e.getStatusCode().value());
      ctx.setWsdlBody(e.getResponseBodyAsString());
      ctx.setWsdlContentType("");
    }
  }

  // ═══════════════════ Private helpers ═══════════════════

  private void setupWireMockStubs() {
    wireMockServer.resetAll();

    if (!ctx.isCreditorAuthorized()) {
      wireMockServer.stubFor(
          post(urlPathEqualTo("/internal/fordringer"))
              .willReturn(
                  aResponse()
                      .withStatus(403)
                      .withHeader("Content-Type", "application/json")
                      .withBody("{\"error\":\"Unauthorized\"}")));
    } else if (isInvalidVariant()) {
      String fieldName =
          "skat-missing-required-field".equals(ctx.getRequestBodyVariant())
              ? "claimType"
              : "amount";
      String fieldMsg =
          "skat-missing-required-field".equals(ctx.getRequestBodyVariant())
              ? "Claim type is required"
              : "Amount must be positive";
      wireMockServer.stubFor(
          post(urlPathEqualTo("/internal/fordringer"))
              .willReturn(
                  aResponse()
                      .withStatus(422)
                      .withHeader("Content-Type", "application/json")
                      .withBody(
                          "{\"claimId\":null,\"outcome\":\"REJECTED\",\"errors\":[{\"field\":\""
                              + fieldName
                              + "\",\"message\":\""
                              + fieldMsg
                              + "\"}]}")));
    } else {
      boolean isSkat = isSkatService(ctx.getTargetService());
      String claimId = isSkat ? "CLAIM-002" : "CLAIM-001";
      wireMockServer.stubFor(
          post(urlPathEqualTo("/internal/fordringer"))
              .willReturn(
                  aResponse()
                      .withStatus(201)
                      .withHeader("Content-Type", "application/json")
                      .withBody(
                          "{\"claimId\":\""
                              + claimId
                              + "\",\"outcome\":\"SUCCESS\",\"errors\":[]}")));
    }

    String kvitteringClaimId = ctx.getClaimId() != null ? ctx.getClaimId() : "CLAIM-001";
    String receiptId = ctx.getReceiptId() != null ? ctx.getReceiptId() : "RECEIPT-001";
    String claimStatus = ctx.getClaimStatus() != null ? ctx.getClaimStatus() : "SUBMITTED";
    wireMockServer.stubFor(
        get(urlPathMatching("/internal/fordringer/.+/kvittering"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        "{\"kvitteringId\":\""
                            + receiptId
                            + "\",\"claimId\":\""
                            + kvitteringClaimId
                            + "\",\"status\":\""
                            + claimStatus
                            + "\"}")));

    int notifCount = ctx.getNotificationCount();
    StringBuilder notifs = new StringBuilder("[");
    for (int i = 0; i < notifCount; i++) {
      if (i > 0) notifs.append(",");
      notifs
          .append("{\"notificationId\":\"NOTIF-00")
          .append(i + 1)
          .append("\",\"claimId\":\"")
          .append(kvitteringClaimId)
          .append(
              "\",\"type\":\"PAYMENT\",\"status\":\"SENT\",\"createdAt\":\"2026-01-01T00:00:00Z\",\"description\":\"Test notification ")
          .append(i + 1)
          .append("\"}");
    }
    notifs.append("]");
    wireMockServer.stubFor(
        get(urlPathMatching("/internal/fordringer/.+/underretninger"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        "{\"claimId\":\""
                            + kvitteringClaimId
                            + "\",\"underretninger\":"
                            + notifs
                            + ",\"total\":"
                            + notifCount
                            + "}")));
  }

  private boolean isInvalidVariant() {
    String v = ctx.getRequestBodyVariant();
    return v != null && (v.contains("invalid") || v.contains("missing"));
  }

  private boolean isSkatService(String serviceName) {
    return serviceName != null && serviceName.toLowerCase().contains("skat");
  }

  private String resolveCreditorId() {
    if (ctx.getCertSubject() != null) {
      return extractCnFromDn(ctx.getCertSubject());
    }
    return ctx.getCreditorId() != null ? ctx.getCreditorId() : "CREDITOR-001";
  }

  private String extractCnFromDn(String dn) {
    if (dn == null) return "";
    for (String part : dn.split(",")) {
      String trimmed = part.trim();
      if (trimmed.startsWith("CN=")) {
        return trimmed.substring(3).trim();
      }
    }
    return dn;
  }

  private String buildRequestBody(String targetService) {
    String variant = ctx.getRequestBodyVariant();
    if (variant == null) variant = "oio-valid-claim";

    return switch (variant) {
      case "oio-valid-claim", "oio-valid-claim-security", "oio-xsd-valid" ->
          loadResource("soap/oio-valid-claim-soap11.xml");
      case "skat-valid-claim", "skat-missing-required-field", "skat-xsd-valid" ->
          loadResource("soap/skat-valid-claim-soap11.xml");
      case "oio-invalid-amount-zero" -> loadResource("soap/oio-invalid-amount-zero-soap11.xml");
      case "oio-invalid-amount-negative" ->
          loadResource("soap/oio-invalid-amount-negative-soap11.xml");
      case "oio-invalid-amount-general", "oio-invalid-for-logging" ->
          loadResource("soap/oio-invalid-amount-zero-soap11.xml");
      case "malformed-soap" -> loadResource("soap/malformed-soap.xml");
      case "oio-claim-with-cpr" -> buildOioClaimWithCpr();
      case "kvittering-or-underret-request" -> buildKvitteringOrUnderretRequest(targetService);
      default -> loadResource("soap/oio-valid-claim-soap11.xml");
    };
  }

  private String loadResource(String path) {
    try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
      if (is == null) throw new IllegalArgumentException("Resource not found: " + path);
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new RuntimeException("Failed to load resource: " + path, e);
    }
  }

  private String buildOioClaimWithCpr() {
    return """
            <?xml version="1.0" encoding="UTF-8"?>
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:oio="urn:oio:skat:efi:ws:1.0.1">
              <soapenv:Header/>
              <soapenv:Body>
                <oio:MFFordringIndberet_IRequest>
                  <oio:FordringsType>SKAT_RESTSKAT</oio:FordringsType>
                  <oio:Beloeb>10000.00</oio:Beloeb>
                  <oio:SkyldnerPersonId>550e8400-e29b-41d4-a716-446655440001</oio:SkyldnerPersonId>
                  <oio:CPRNummer>1234567890</oio:CPRNummer>
                  <oio:FordringsDato>2026-01-15</oio:FordringsDato>
                  <oio:ForfaldsDato>2026-03-01</oio:ForfaldsDato>
                  <oio:EksternId>CLAIM-CPR</oio:EksternId>
                </oio:MFFordringIndberet_IRequest>
              </soapenv:Body>
            </soapenv:Envelope>
            """;
  }

  private String buildKvitteringOrUnderretRequest(String targetService) {
    String ns = ctx.getSoapNamespace();
    if (ns == null) ns = "urn:oio:skat:efi:ws:1.0.1";
    String nsPrefix = isSkatService(targetService) ? "skat" : "oio";
    String envNs = "http://schemas.xmlsoap.org/soap/envelope/";
    String claimId = ctx.getClaimId() != null ? ctx.getClaimId() : "CLAIM-001";
    String elementName =
        (targetService != null && targetService.contains("Kvittering"))
            ? "MFKvitteringHent_IRequest"
            : "MFUnderretSamlingHent_IRequest";

    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<soapenv:Envelope xmlns:soapenv=\""
        + envNs
        + "\" xmlns:"
        + nsPrefix
        + "=\""
        + ns
        + "\">"
        + "<soapenv:Header/>"
        + "<soapenv:Body>"
        + "<"
        + nsPrefix
        + ":"
        + elementName
        + ">"
        + "<"
        + nsPrefix
        + ":FordringsId>"
        + claimId
        + "</"
        + nsPrefix
        + ":FordringsId>"
        + "</"
        + nsPrefix
        + ":"
        + elementName
        + ">"
        + "</soapenv:Body>"
        + "</soapenv:Envelope>";
  }

  private String convertToSoap12(String soap11Body) {
    return soap11Body.replace(
        "http://schemas.xmlsoap.org/soap/envelope/", "http://www.w3.org/2003/05/soap-envelope");
  }

  private String extractXml(String xml, String... elementNames) {
    for (String name : elementNames) {
      // Allow optional attributes between element name and closing >
      java.util.regex.Pattern p =
          java.util.regex.Pattern.compile(
              "<(?:[^:>]+:)?"
                  + java.util.regex.Pattern.quote(name)
                  + "(?:\\s[^>]*)?>([^<]+)</(?:[^:>]+:)?"
                  + java.util.regex.Pattern.quote(name)
                  + ">",
              java.util.regex.Pattern.CASE_INSENSITIVE);
      java.util.regex.Matcher m = p.matcher(xml);
      if (m.find()) return m.group(1).trim();
    }
    return null;
  }

  private int countElements(String xml, String elementName) {
    // Count only opening element tags (not closing tags like </ns:Underretning>).
    // (?!/) excludes closing tags; (?:[\s/>]) prevents prefix-matches (e.g. UnderretningId).
    java.util.regex.Pattern p =
        java.util.regex.Pattern.compile(
            "<(?!/)" + "(?:[^:>]+:)?" + java.util.regex.Pattern.quote(elementName) + "(?:[\\s/>])",
            java.util.regex.Pattern.CASE_INSENSITIVE);
    java.util.regex.Matcher m = p.matcher(xml);
    int count = 0;
    while (m.find()) count++;
    return count;
  }

  private void checkAndAddElement(String xml, String elementName) {
    if (xml.contains("<" + elementName + ">")
        || xml.contains("<" + elementName + " ")
        || xml.contains(":" + elementName + ">")
        || xml.contains(":" + elementName + " ")) {
      ctx.addFaultEnvelopeElement(elementName);
    }
  }
}
