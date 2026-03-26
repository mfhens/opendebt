package dk.ufst.opendebt.gateway.soap.interceptor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.EndpointInterceptor;

import dk.ufst.opendebt.common.audit.cls.ClsAuditClient;
import dk.ufst.opendebt.common.audit.cls.ClsAuditEvent;
import dk.ufst.opendebt.common.soap.SoapPiiMaskingUtil;
import dk.ufst.opendebt.gateway.soap.fault.Oces3AuthenticationException;
import dk.ufst.opendebt.gateway.soap.fault.Oces3AuthorizationException;

@Component
public class ClsSoapAuditInterceptor implements EndpointInterceptor {

  private static final Map<String, String[]> SERVICE_OP_MAP =
      Map.of(
          "urn:oio:skat:efi:ws:1.0.1|MFFordringIndberet_IRequest",
              new String[] {"OIOFordringIndberetService", "MFFordringIndberet_I"},
          "urn:oio:skat:efi:ws:1.0.1|MFKvitteringHent_IRequest",
              new String[] {"OIOKvitteringHentService", "MFKvitteringHent_I"},
          "urn:oio:skat:efi:ws:1.0.1|MFUnderretSamlingHent_IRequest",
              new String[] {"OIOUnderretSamlingHentService", "MFUnderretSamlingHent_I"},
          "http://skat.dk/begrebsmodel/2009/01/15/|MFFordringIndberet_IRequest",
              new String[] {"SkatFordringIndberetService", "MFFordringIndberet_I"},
          "http://skat.dk/begrebsmodel/2009/01/15/|MFKvitteringHent_IRequest",
              new String[] {"SkatKvitteringHentService", "MFKvitteringHent_I"},
          "http://skat.dk/begrebsmodel/2009/01/15/|MFUnderretSamlingHent_IRequest",
              new String[] {"SkatUnderretSamlingHentService", "MFUnderretSamlingHent_I"});

  private final ClsAuditClient clsAuditClient;
  private final SoapPiiMaskingUtil soapPiiMaskingUtil;

  public ClsSoapAuditInterceptor(
      ClsAuditClient clsAuditClient, SoapPiiMaskingUtil soapPiiMaskingUtil) {
    this.clsAuditClient = clsAuditClient;
    this.soapPiiMaskingUtil = soapPiiMaskingUtil;
  }

  @Override
  public boolean handleRequest(MessageContext messageContext, Object endpoint) throws Exception {
    messageContext.setProperty("soapAuditStartTime", Instant.now());
    String requestBody = extractBody(messageContext.getRequest());
    String[] serviceOp = resolveServiceAndOperation(requestBody);
    messageContext.setProperty("maskedRequestBody", soapPiiMaskingUtil.mask(requestBody));
    messageContext.setProperty("soapServiceName", serviceOp[0]);
    messageContext.setProperty("soapOperationName", serviceOp[1]);
    return true;
  }

  @Override
  public boolean handleResponse(MessageContext messageContext, Object endpoint) throws Exception {
    messageContext.setProperty(
        "maskedResponseBody", soapPiiMaskingUtil.mask(extractBody(messageContext.getResponse())));
    messageContext.setProperty("soapStatus", "SUCCESS");
    return true;
  }

  @Override
  public boolean handleFault(MessageContext messageContext, Object endpoint) throws Exception {
    String responseBody = extractBody(messageContext.getResponse());
    messageContext.setProperty("maskedResponseBody", soapPiiMaskingUtil.mask(responseBody));
    messageContext.setProperty("soapStatus", "FAULT");
    if (responseBody != null) {
      messageContext.setProperty(
          "soapFaultCode", extractXmlElement(responseBody, "faultcode", "Code"));
      messageContext.setProperty(
          "soapFaultMessage", extractXmlElement(responseBody, "faultstring", "Reason"));
    }
    return true;
  }

  @Override
  public void afterCompletion(MessageContext messageContext, Object endpoint, Exception ex) {
    try {
      Instant startTime = (Instant) messageContext.getProperty("soapAuditStartTime");
      if (startTime == null) startTime = Instant.now();
      long responseTimeMs = Instant.now().toEpochMilli() - startTime.toEpochMilli();

      String status = (String) messageContext.getProperty("soapStatus");
      if (status == null) status = ex != null ? "FAULT" : "SUCCESS";

      String faultCode = (String) messageContext.getProperty("soapFaultCode");
      String faultMessage = (String) messageContext.getProperty("soapFaultMessage");
      String stackTrace = null;
      if (ex != null && faultCode == null) {
        faultCode = deriveCodeFromException(ex);
        faultMessage = ex.getMessage();
        stackTrace = ex.getClass().getName() + ": " + ex.getMessage();
      } else if (ex != null) {
        stackTrace = ex.getClass().getName() + ": " + ex.getMessage();
      }

      String fordringshaverId = (String) messageContext.getProperty("fordringshaverId");
      String correlationId = (String) messageContext.getProperty("correlationId");
      String soapServiceName = (String) messageContext.getProperty("soapServiceName");
      if (soapServiceName == null) soapServiceName = "UnknownService";
      String soapOperationName = (String) messageContext.getProperty("soapOperationName");
      if (soapOperationName == null) soapOperationName = "UnknownOperation";

      String maskedReq = (String) messageContext.getProperty("maskedRequestBody");
      String maskedResp = (String) messageContext.getProperty("maskedResponseBody");

      Map<String, Object> newValues = new HashMap<>();
      newValues.put("responseTimeMs", String.valueOf(responseTimeMs));
      newValues.put("status", status);
      newValues.put("requestBody", maskedReq);
      newValues.put("responseBody", maskedResp);
      if (faultCode != null) newValues.put("faultCode", faultCode);
      if (faultMessage != null) newValues.put("faultMessage", faultMessage);
      if (stackTrace != null) newValues.put("stackTrace", stackTrace);

      ClsAuditEvent event =
          ClsAuditEvent.builder()
              .eventId(UUID.randomUUID())
              .timestamp(startTime)
              .serviceName("integration-gateway")
              .operation(soapServiceName + "." + soapOperationName)
              .resourceType("SOAP_CALL")
              .userId(fordringshaverId)
              .correlationId(correlationId)
              .changedFields(List.of())
              .oldValues(Map.of())
              .newValues(newValues)
              .build();

      clsAuditClient.shipEvent(event);
    } catch (Exception ignored) {
      // Never fail the request due to audit failure
    }
  }

  private String extractBody(WebServiceMessage message) {
    if (message == null) return null;
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      message.writeTo(baos);
      return baos.toString(java.nio.charset.StandardCharsets.UTF_8);
    } catch (IOException e) {
      return null;
    }
  }

  private String[] resolveServiceAndOperation(String body) {
    if (body == null) return new String[] {"UnknownService", "UnknownOperation"};
    for (Map.Entry<String, String[]> e : SERVICE_OP_MAP.entrySet()) {
      String[] parts = e.getKey().split("\\|");
      if (body.contains(parts[0]) && body.contains(parts[1])) {
        return e.getValue();
      }
    }
    return new String[] {"UnknownService", "UnknownOperation"};
  }

  private String extractXmlElement(String xml, String... elementNames) {
    for (String elementName : elementNames) {
      // Allow optional attributes between element name and closing > (e.g. xml:lang="da")
      Pattern p =
          Pattern.compile("<(?:[^:>]+:)?" + Pattern.quote(elementName) + "(?:\\s[^>]*)?>([^<]+)<");
      Matcher m = p.matcher(xml);
      if (m.find()) return m.group(1).trim();
    }
    return null;
  }

  private String deriveCodeFromException(Exception ex) {
    if (ex instanceof Oces3AuthenticationException authEx) {
      return "CERT_EXPIRED".equals(authEx.getErrorCode())
          ? "env:Client.CertificateExpired"
          : "env:Client.Authentication";
    } else if (ex instanceof Oces3AuthorizationException) {
      return "env:Client.Authorization";
    }
    return "env:Server";
  }
}
