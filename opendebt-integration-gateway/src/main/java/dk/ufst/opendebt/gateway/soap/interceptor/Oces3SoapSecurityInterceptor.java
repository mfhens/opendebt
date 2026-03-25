package dk.ufst.opendebt.gateway.soap.interceptor;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.transport.context.TransportContextHolder;
import org.springframework.ws.transport.http.HttpServletConnection;

import dk.ufst.opendebt.common.soap.Oces3AuthContext;
import dk.ufst.opendebt.common.soap.Oces3CertificateParser;
import dk.ufst.opendebt.gateway.soap.fault.Oces3AuthenticationException;
import dk.ufst.opendebt.gateway.soap.fault.Oces3AuthorizationException;

@Component
public class Oces3SoapSecurityInterceptor implements EndpointInterceptor {

  private final Oces3CertificateParser certParser;
  private final String tlsTerminationMode;
  private final List<String> trustedCaSubjects;

  public Oces3SoapSecurityInterceptor(
      Oces3CertificateParser certParser,
      @Value("${opendebt.soap.security.tls-termination-mode:INGRESS}") String tlsTerminationMode,
      @Value("${opendebt.soap.oces3.trusted-ca-subjects:#{null}}") List<String> trustedCaSubjects) {
    this.certParser = certParser;
    this.tlsTerminationMode = tlsTerminationMode;
    this.trustedCaSubjects = trustedCaSubjects != null ? trustedCaSubjects : List.of();
  }

  @Override
  public boolean handleRequest(MessageContext messageContext, Object endpoint) throws Exception {
    // Allow WSDL/metadata GET requests through without security check — no credentials needed
    // to discover the service contract (WsdlDefinition endpoints handle GET /?wsdl requests).
    if (endpoint instanceof org.springframework.ws.wsdl.WsdlDefinition) {
      return true;
    }
    HttpServletRequest httpRequest = getHttpRequest();
    if (httpRequest != null && "GET".equalsIgnoreCase(httpRequest.getMethod())) {
      return true;
    }

    if ("TEST".equalsIgnoreCase(tlsTerminationMode)) {
      return handleTestMode(messageContext, httpRequest);
    }

    X509Certificate cert = extractCertificate(httpRequest);
    if (cert == null) {
      throw new Oces3AuthenticationException("Certifikat mangler", "CERT_MISSING");
    }

    Oces3AuthContext authContext = certParser.parse(cert);

    if (authContext.validTo().isBefore(Instant.now())) {
      throw new Oces3AuthenticationException("Certifikat er udløbet", "CERT_EXPIRED");
    }

    if (!trustedCaSubjects.isEmpty() && !trustedCaSubjects.contains(authContext.issuer())) {
      throw new Oces3AuthorizationException("Udsteder ikke godkendt", "ISSUER_NOT_TRUSTED");
    }

    storeAuthContext(messageContext, authContext, httpRequest);
    return true;
  }

  private boolean handleTestMode(MessageContext messageContext, HttpServletRequest request) {
    if (request == null) {
      throw new Oces3AuthenticationException("Certifikat mangler", "CERT_MISSING");
    }
    String testCreditorId = request.getHeader("X-Test-Fordringshaver-Id");
    if (testCreditorId == null || testCreditorId.isBlank()) {
      throw new Oces3AuthenticationException("Certifikat mangler", "CERT_MISSING");
    }
    if ("true".equalsIgnoreCase(request.getHeader("X-Test-Cert-Expired"))) {
      throw new Oces3AuthenticationException("Certifikat er udløbet", "CERT_EXPIRED");
    }
    Oces3AuthContext authContext =
        new Oces3AuthContext(
            testCreditorId,
            testCreditorId,
            "CN=Test CA,O=Test",
            Instant.now().plusSeconds(3600),
            "test-serial");
    storeAuthContext(messageContext, authContext, request);
    return true;
  }

  private void storeAuthContext(
      MessageContext messageContext, Oces3AuthContext authContext, HttpServletRequest request) {
    messageContext.setProperty("oces3AuthContext", authContext);
    messageContext.setProperty("fordringshaverId", authContext.fordringshaverId());
    String correlationId = request != null ? request.getHeader("X-Correlation-Id") : null;
    if (correlationId == null || correlationId.isBlank()) {
      correlationId = UUID.randomUUID().toString();
    }
    messageContext.setProperty("correlationId", correlationId);
  }

  private HttpServletRequest getHttpRequest() {
    var transportContext = TransportContextHolder.getTransportContext();
    if (transportContext != null
        && transportContext.getConnection() instanceof HttpServletConnection conn) {
      return conn.getHttpServletRequest();
    }
    return null;
  }

  private X509Certificate extractCertificate(HttpServletRequest request) {
    if (request == null) return null;
    if ("EMBEDDED".equalsIgnoreCase(tlsTerminationMode)) {
      Object attr = request.getAttribute("javax.servlet.request.X509Certificate");
      if (attr instanceof X509Certificate[] certs && certs.length > 0) {
        return certs[0];
      }
    } else if ("INGRESS".equalsIgnoreCase(tlsTerminationMode)) {
      String certHeader = request.getHeader("X-Client-Cert");
      if (certHeader == null || certHeader.isBlank()) return null;
      try {
        String cleaned =
            certHeader
                .replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replaceAll("\\s+", "");
        byte[] certBytes = Base64.getDecoder().decode(cleaned);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certBytes));
      } catch (Exception e) {
        return null;
      }
    }
    return null;
  }

  @Override
  public boolean handleResponse(MessageContext messageContext, Object endpoint) {
    return true;
  }

  @Override
  public boolean handleFault(MessageContext messageContext, Object endpoint) {
    return true;
  }

  @Override
  public void afterCompletion(MessageContext messageContext, Object endpoint, Exception ex) {}
}
