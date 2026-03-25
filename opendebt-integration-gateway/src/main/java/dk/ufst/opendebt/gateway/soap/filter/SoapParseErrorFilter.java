package dk.ufst.opendebt.gateway.soap.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Catches SAAJ / XML parse exceptions that escape {@code MessageDispatcherServlet} before endpoint
 * dispatch begins, and returns a well-formed SOAP 1.1 fault with HTTP 400 instead of allowing
 * Spring Boot's default JSON error handler to produce a non-SOAP response.
 *
 * <p>When a client sends a body that is not valid SOAP (wrong namespace, truncated XML, etc.),
 * SAAJ's {@code MessageFactory.createMessage()} throws before the endpoint interceptor chain runs.
 * The exception escapes the servlet and would otherwise be rendered as {@code
 * {"status":500,"error":"Internal Server Error",...}} by Spring Boot's error controller.
 *
 * <p>Traceability: SPEC-019-03 (malformed SOAP handling)
 */
@Component
@Order(-1)
public class SoapParseErrorFilter extends OncePerRequestFilter {

  private static final String SOAP_11_FAULT_TEMPLATE =
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
        <SOAP-ENV:Body>
          <SOAP-ENV:Fault>
            <faultcode>SOAP-ENV:Client</faultcode>
            <faultstring xml:lang="da">Ugyldig SOAP-meddelelse: skemavalidering fejlede</faultstring>
          </SOAP-ENV:Fault>
        </SOAP-ENV:Body>
      </SOAP-ENV:Envelope>""";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    try {
      filterChain.doFilter(request, response);
    } catch (Exception ex) {
      if (!response.isCommitted() && isSoapPath(request) && isSoapParseFailure(ex)) {
        writeSoapFault(response);
      } else {
        throw ex;
      }
    }
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !"POST".equalsIgnoreCase(request.getMethod()) || !isSoapPath(request);
  }

  private boolean isSoapPath(HttpServletRequest request) {
    String path = request.getServletPath();
    if (path == null || path.isEmpty()) {
      path = request.getRequestURI();
    }
    return path != null && path.startsWith("/soap");
  }

  private boolean isSoapParseFailure(Throwable ex) {
    Throwable t = ex;
    while (t != null) {
      String name = t.getClass().getName();
      if (name.contains("SOAPException")
          || name.contains("SAXParseException")
          || name.contains("SaajSoapMessageCreationException")
          || (t.getMessage() != null
              && (t.getMessage().contains("SOAP")
                  || t.getMessage().contains("XML")
                  || t.getMessage().contains("parse")))) {
        return true;
      }
      t = t.getCause();
    }
    return false;
  }

  private void writeSoapFault(HttpServletResponse response) throws IOException {
    byte[] body = SOAP_11_FAULT_TEMPLATE.getBytes(StandardCharsets.UTF_8);
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    response.setContentType("text/xml;charset=UTF-8");
    response.setContentLength(body.length);
    response.getOutputStream().write(body);
    response.flushBuffer();
  }
}
