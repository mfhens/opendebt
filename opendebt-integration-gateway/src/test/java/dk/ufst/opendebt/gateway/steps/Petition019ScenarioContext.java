package dk.ufst.opendebt.gateway.steps;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Shared scenario-scoped state for Petition 019 BDD tests (Legacy SOAP Endpoints).
 *
 * <p>Traceability: SPECS-019 (all modules), petition019-legacy-soap-endpoints.feature
 *
 * <p>Scoped to "cucumber-glue" so each scenario gets a fresh instance; no state leaks between
 * scenarios.
 */
@Component
@Scope("cucumber-glue")
public class Petition019ScenarioContext {

  // ── Request identity ──────────────────────────────────────────────────────────

  /** The creditor (fordringshaver) identifier driving this scenario. */
  private String creditorId;

  /** The certificate ID used in this scenario (e.g. "CERT-001"). */
  private String certId;

  /** Whether the certificate in this scenario is expired. */
  private boolean certExpired;

  /** Whether the certificate in this scenario is missing entirely. */
  private boolean certMissing;

  /** Explicit DN subject set on the certificate (Scenario 12). */
  private String certSubject;

  /** Whether the creditor is authorized to submit claims. */
  private boolean creditorAuthorized = true;

  // ── SOAP request configuration ────────────────────────────────────────────────

  /** Target service name (e.g. "OIOFordringIndberetService"). */
  private String targetService;

  /** SOAP namespace URI in use (OIO or SKAT). */
  private String soapNamespace;

  /** SOAP operation name (e.g. "MFFordringIndberet_I"). */
  private String soapOperation;

  /**
   * SOAP protocol version: "1.1" or "1.2". Null means auto-detect / not specified by the scenario.
   */
  private String soapProtocol;

  /** Free-form label describing the request body variant for this scenario. */
  private String requestBodyVariant;

  /** Correlation ID injected into the SOAP request for statelessness tests. */
  private String requestCorrelationId;

  // ── Claim / receipt / notification pre-conditions ─────────────────────────────

  private String claimId;
  private String claimStatus;
  private String receiptId;
  private String debtorId;
  private int notificationCount;

  // ── HTTP / WSDL response capture ──────────────────────────────────────────────

  /** HTTP status of the last WSDL GET response. */
  private int wsdlHttpStatus;

  /** Raw body of the last WSDL GET response. */
  private String wsdlBody;

  /** Content-Type header of the last WSDL GET response. */
  private String wsdlContentType;

  // ── SOAP response capture ─────────────────────────────────────────────────────

  /** HTTP status of the last SOAP response. */
  private int soapHttpStatus;

  /** Whether the last SOAP response was a successful response. */
  private boolean soapResponseSuccess;

  /** Whether the last SOAP response was a SOAP fault. */
  private boolean soapResponseFault;

  /** Protocol version found in the SOAP response envelope. */
  private String soapResponseProtocol;

  /** Raw SOAP response body. */
  private String soapResponseBody;

  /** Claim ID extracted from the SOAP response. */
  private String responseClaimId;

  /** Receipt ID extracted from the SOAP response. */
  private String responseReceiptId;

  /** Claim status extracted from the SOAP response. */
  private String responseClaimStatus;

  /** Number of notifications in the SOAP response collection. */
  private int responseNotificationCount;

  /** Correlation ID extracted from the SOAP response. */
  private String responseCorrelationId;

  /** Whether the response contained a WS-Addressing ReplyTo header. */
  private boolean responseContainsReplyTo;

  /** Whether the response contained any async callback reference. */
  private boolean responseContainsAsyncCallback;

  /** Whether the response was returned on the same HTTP connection. */
  private boolean responseSameConnection;

  /** XSD schema namespace used during SOAP message validation. */
  private String validationSchemaNamespace;

  // ── SOAP fault capture ────────────────────────────────────────────────────────

  /** SOAP fault code (e.g. "env:Client.ValidationError"). */
  private String faultCode;

  /** SOAP fault message string (should be in Danish). */
  private String faultMessage;

  /** Whether the fault detail element contains field-level ValidationError children. */
  private boolean faultDetailHasFieldErrors;

  /** Field name from the fault detail ValidationError element. */
  private String faultDetailField;

  /** Error message from the fault detail ValidationError element. */
  private String faultDetailErrorMessage;

  /** SOAP fault envelope element names present (for 1.1/1.2 structure tests). */
  private java.util.List<String> faultEnvelopeElements = new java.util.ArrayList<>();

  // ── CLS audit capture ─────────────────────────────────────────────────────────

  /** The CLS audit event captured after the SOAP call. */
  private Object capturedClsEvent;

  /** Status string from CLS event ("SUCCESS" or "FAULT"). */
  private String clsStatus;

  /** Timestamp from CLS event. */
  private java.time.Instant clsTimestamp;

  /** Calling system identifier from CLS event. */
  private String clsCallingSystem;

  /** Service name from CLS event. */
  private String clsServiceName;

  /** Operation name from CLS event. */
  private String clsOperationName;

  /** Correlation ID from CLS event. */
  private String clsCorrelationId;

  /** Response time in milliseconds from CLS event. */
  private Long clsResponseTimeMs;

  /** Error code from CLS event (fault path). */
  private String clsErrorCode;

  /** Error message from CLS event (fault path). */
  private String clsErrorMessage;

  /** Stack trace from CLS event (fault path). */
  private String clsStackTrace;

  /** Masked request body from CLS event. */
  private String clsRequestBody;

  /** Masked response body from CLS event. */
  private String clsResponseBody;

  /** Fordringshaver identifier extracted from certificate subject. */
  private String extractedFordringshaverId;

  // ── Getters and setters ───────────────────────────────────────────────────────

  public String getCreditorId() {
    return creditorId;
  }

  public void setCreditorId(String creditorId) {
    this.creditorId = creditorId;
  }

  public String getCertId() {
    return certId;
  }

  public void setCertId(String certId) {
    this.certId = certId;
  }

  public boolean isCertExpired() {
    return certExpired;
  }

  public void setCertExpired(boolean certExpired) {
    this.certExpired = certExpired;
  }

  public boolean isCertMissing() {
    return certMissing;
  }

  public void setCertMissing(boolean certMissing) {
    this.certMissing = certMissing;
  }

  public String getCertSubject() {
    return certSubject;
  }

  public void setCertSubject(String certSubject) {
    this.certSubject = certSubject;
  }

  public boolean isCreditorAuthorized() {
    return creditorAuthorized;
  }

  public void setCreditorAuthorized(boolean creditorAuthorized) {
    this.creditorAuthorized = creditorAuthorized;
  }

  public String getTargetService() {
    return targetService;
  }

  public void setTargetService(String targetService) {
    this.targetService = targetService;
  }

  public String getSoapNamespace() {
    return soapNamespace;
  }

  public void setSoapNamespace(String soapNamespace) {
    this.soapNamespace = soapNamespace;
  }

  public String getSoapOperation() {
    return soapOperation;
  }

  public void setSoapOperation(String soapOperation) {
    this.soapOperation = soapOperation;
  }

  public String getSoapProtocol() {
    return soapProtocol;
  }

  public void setSoapProtocol(String soapProtocol) {
    this.soapProtocol = soapProtocol;
  }

  public String getRequestBodyVariant() {
    return requestBodyVariant;
  }

  public void setRequestBodyVariant(String requestBodyVariant) {
    this.requestBodyVariant = requestBodyVariant;
  }

  public String getRequestCorrelationId() {
    return requestCorrelationId;
  }

  public void setRequestCorrelationId(String requestCorrelationId) {
    this.requestCorrelationId = requestCorrelationId;
  }

  public String getClaimId() {
    return claimId;
  }

  public void setClaimId(String claimId) {
    this.claimId = claimId;
  }

  public String getClaimStatus() {
    return claimStatus;
  }

  public void setClaimStatus(String claimStatus) {
    this.claimStatus = claimStatus;
  }

  public String getReceiptId() {
    return receiptId;
  }

  public void setReceiptId(String receiptId) {
    this.receiptId = receiptId;
  }

  public String getDebtorId() {
    return debtorId;
  }

  public void setDebtorId(String debtorId) {
    this.debtorId = debtorId;
  }

  public int getNotificationCount() {
    return notificationCount;
  }

  public void setNotificationCount(int notificationCount) {
    this.notificationCount = notificationCount;
  }

  public int getWsdlHttpStatus() {
    return wsdlHttpStatus;
  }

  public void setWsdlHttpStatus(int wsdlHttpStatus) {
    this.wsdlHttpStatus = wsdlHttpStatus;
  }

  public String getWsdlBody() {
    return wsdlBody;
  }

  public void setWsdlBody(String wsdlBody) {
    this.wsdlBody = wsdlBody;
  }

  public String getWsdlContentType() {
    return wsdlContentType;
  }

  public void setWsdlContentType(String wsdlContentType) {
    this.wsdlContentType = wsdlContentType;
  }

  public int getSoapHttpStatus() {
    return soapHttpStatus;
  }

  public void setSoapHttpStatus(int soapHttpStatus) {
    this.soapHttpStatus = soapHttpStatus;
  }

  public boolean isSoapResponseSuccess() {
    return soapResponseSuccess;
  }

  public void setSoapResponseSuccess(boolean soapResponseSuccess) {
    this.soapResponseSuccess = soapResponseSuccess;
  }

  public boolean isSoapResponseFault() {
    return soapResponseFault;
  }

  public void setSoapResponseFault(boolean soapResponseFault) {
    this.soapResponseFault = soapResponseFault;
  }

  public String getSoapResponseProtocol() {
    return soapResponseProtocol;
  }

  public void setSoapResponseProtocol(String soapResponseProtocol) {
    this.soapResponseProtocol = soapResponseProtocol;
  }

  public String getSoapResponseBody() {
    return soapResponseBody;
  }

  public void setSoapResponseBody(String soapResponseBody) {
    this.soapResponseBody = soapResponseBody;
  }

  public String getResponseClaimId() {
    return responseClaimId;
  }

  public void setResponseClaimId(String responseClaimId) {
    this.responseClaimId = responseClaimId;
  }

  public String getResponseReceiptId() {
    return responseReceiptId;
  }

  public void setResponseReceiptId(String responseReceiptId) {
    this.responseReceiptId = responseReceiptId;
  }

  public String getResponseClaimStatus() {
    return responseClaimStatus;
  }

  public void setResponseClaimStatus(String responseClaimStatus) {
    this.responseClaimStatus = responseClaimStatus;
  }

  public int getResponseNotificationCount() {
    return responseNotificationCount;
  }

  public void setResponseNotificationCount(int responseNotificationCount) {
    this.responseNotificationCount = responseNotificationCount;
  }

  public String getResponseCorrelationId() {
    return responseCorrelationId;
  }

  public void setResponseCorrelationId(String responseCorrelationId) {
    this.responseCorrelationId = responseCorrelationId;
  }

  public boolean isResponseContainsReplyTo() {
    return responseContainsReplyTo;
  }

  public void setResponseContainsReplyTo(boolean v) {
    this.responseContainsReplyTo = v;
  }

  public boolean isResponseContainsAsyncCallback() {
    return responseContainsAsyncCallback;
  }

  public void setResponseContainsAsyncCallback(boolean v) {
    this.responseContainsAsyncCallback = v;
  }

  public boolean isResponseSameConnection() {
    return responseSameConnection;
  }

  public void setResponseSameConnection(boolean v) {
    this.responseSameConnection = v;
  }

  public String getValidationSchemaNamespace() {
    return validationSchemaNamespace;
  }

  public void setValidationSchemaNamespace(String v) {
    this.validationSchemaNamespace = v;
  }

  public String getFaultCode() {
    return faultCode;
  }

  public void setFaultCode(String faultCode) {
    this.faultCode = faultCode;
  }

  public String getFaultMessage() {
    return faultMessage;
  }

  public void setFaultMessage(String faultMessage) {
    this.faultMessage = faultMessage;
  }

  public boolean isFaultDetailHasFieldErrors() {
    return faultDetailHasFieldErrors;
  }

  public void setFaultDetailHasFieldErrors(boolean v) {
    this.faultDetailHasFieldErrors = v;
  }

  public String getFaultDetailField() {
    return faultDetailField;
  }

  public void setFaultDetailField(String faultDetailField) {
    this.faultDetailField = faultDetailField;
  }

  public String getFaultDetailErrorMessage() {
    return faultDetailErrorMessage;
  }

  public void setFaultDetailErrorMessage(String faultDetailErrorMessage) {
    this.faultDetailErrorMessage = faultDetailErrorMessage;
  }

  public java.util.List<String> getFaultEnvelopeElements() {
    return faultEnvelopeElements;
  }

  public void addFaultEnvelopeElement(String element) {
    faultEnvelopeElements.add(element);
  }

  public Object getCapturedClsEvent() {
    return capturedClsEvent;
  }

  public void setCapturedClsEvent(Object capturedClsEvent) {
    this.capturedClsEvent = capturedClsEvent;
  }

  public String getClsStatus() {
    return clsStatus;
  }

  public void setClsStatus(String clsStatus) {
    this.clsStatus = clsStatus;
  }

  public java.time.Instant getClsTimestamp() {
    return clsTimestamp;
  }

  public void setClsTimestamp(java.time.Instant clsTimestamp) {
    this.clsTimestamp = clsTimestamp;
  }

  public String getClsCallingSystem() {
    return clsCallingSystem;
  }

  public void setClsCallingSystem(String clsCallingSystem) {
    this.clsCallingSystem = clsCallingSystem;
  }

  public String getClsServiceName() {
    return clsServiceName;
  }

  public void setClsServiceName(String clsServiceName) {
    this.clsServiceName = clsServiceName;
  }

  public String getClsOperationName() {
    return clsOperationName;
  }

  public void setClsOperationName(String clsOperationName) {
    this.clsOperationName = clsOperationName;
  }

  public String getClsCorrelationId() {
    return clsCorrelationId;
  }

  public void setClsCorrelationId(String clsCorrelationId) {
    this.clsCorrelationId = clsCorrelationId;
  }

  public Long getClsResponseTimeMs() {
    return clsResponseTimeMs;
  }

  public void setClsResponseTimeMs(Long clsResponseTimeMs) {
    this.clsResponseTimeMs = clsResponseTimeMs;
  }

  public String getClsErrorCode() {
    return clsErrorCode;
  }

  public void setClsErrorCode(String clsErrorCode) {
    this.clsErrorCode = clsErrorCode;
  }

  public String getClsErrorMessage() {
    return clsErrorMessage;
  }

  public void setClsErrorMessage(String clsErrorMessage) {
    this.clsErrorMessage = clsErrorMessage;
  }

  public String getClsStackTrace() {
    return clsStackTrace;
  }

  public void setClsStackTrace(String clsStackTrace) {
    this.clsStackTrace = clsStackTrace;
  }

  public String getClsRequestBody() {
    return clsRequestBody;
  }

  public void setClsRequestBody(String clsRequestBody) {
    this.clsRequestBody = clsRequestBody;
  }

  public String getClsResponseBody() {
    return clsResponseBody;
  }

  public void setClsResponseBody(String clsResponseBody) {
    this.clsResponseBody = clsResponseBody;
  }

  public String getExtractedFordringshaverId() {
    return extractedFordringshaverId;
  }

  public void setExtractedFordringshaverId(String v) {
    this.extractedFordringshaverId = v;
  }
}
