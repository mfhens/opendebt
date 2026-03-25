package dk.ufst.opendebt.gateway.soap.fault;

import java.io.StringReader;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.endpoint.AbstractEndpointExceptionResolver;
import org.springframework.ws.soap.SoapBody;
import org.springframework.ws.soap.SoapFault;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.transport.context.TransportContextHolder;
import org.springframework.ws.transport.http.HttpServletConnection;

import dk.ufst.opendebt.common.dto.soap.FieldError;
import dk.ufst.opendebt.common.exception.OpenDebtException;
import dk.ufst.opendebt.gateway.soap.filter.SoapHttpStatusFilter;

@Component
public class SoapFaultMappingResolver extends AbstractEndpointExceptionResolver {

  private static final Logger log = LoggerFactory.getLogger(SoapFaultMappingResolver.class);

  public SoapFaultMappingResolver() {
    setOrder(Ordered.HIGHEST_PRECEDENCE);
  }

  @Override
  protected final boolean resolveExceptionInternal(
      MessageContext messageContext, Object endpoint, Exception ex) {
    try {
      int httpStatus = determineHttpStatus(ex);
      setHttpStatus(messageContext, httpStatus);

      SoapMessage response = (SoapMessage) messageContext.getResponse();
      SoapBody body = response.getSoapBody();

      if (ex instanceof Oces3AuthenticationException authEx) {
        if ("CERT_EXPIRED".equals(authEx.getErrorCode())) {
          body.addClientOrSenderFault("Certifikat er udløbet", Locale.forLanguageTag("da"));
        } else {
          body.addClientOrSenderFault(
              "Autentificering fejlede: certifikat mangler eller er ugyldigt",
              Locale.forLanguageTag("da"));
        }
      } else if (ex instanceof Oces3AuthorizationException) {
        body.addClientOrSenderFault(
            "System er ikke autoriseret til denne operation", Locale.forLanguageTag("da"));
      } else if (ex instanceof FordringValidationException valEx) {
        SoapFault fault =
            body.addClientOrSenderFault(
                "Valideringsfejl i fordringen", Locale.forLanguageTag("da"));
        addValidationDetail(fault, valEx.getFieldErrors());
      } else if (ex instanceof OpenDebtException) {
        body.addServerOrReceiverFault(
            "Intern serverfejl — kontakt UFST support", Locale.forLanguageTag("da"));
      } else {
        body.addClientOrSenderFault(
            "Ugyldig SOAP-meddelelse: skemavalidering fejlede", Locale.forLanguageTag("da"));
      }
      return true;
    } catch (Exception e) {
      log.error("SOAP fault resolver failed to write fault response", e);
      return false;
    }
  }

  private void addValidationDetail(SoapFault fault, List<FieldError> fieldErrors) {
    if (fault == null || fieldErrors == null || fieldErrors.isEmpty()) return;
    try {
      var detail = fault.addFaultDetail();
      if (detail != null) {
        StringBuilder sb = new StringBuilder("<ValidationErrors>");
        for (FieldError fe : fieldErrors) {
          sb.append("<ValidationError>")
              .append("<Field>")
              .append(escapeXml(fe.getField()))
              .append("</Field>")
              .append("<Message>")
              .append(escapeXml(fe.getMessage()))
              .append("</Message>")
              .append("</ValidationError>");
        }
        sb.append("</ValidationErrors>");
        javax.xml.transform.TransformerFactory.newInstance()
            .newTransformer()
            .transform(
                new javax.xml.transform.stream.StreamSource(new StringReader(sb.toString())),
                detail.getResult());
      }
    } catch (Exception ignored) {
    }
  }

  private String escapeXml(String s) {
    if (s == null) return "";
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }

  private int determineHttpStatus(Exception ex) {
    if (ex instanceof Oces3AuthenticationException authEx) {
      return "CERT_MISSING".equals(authEx.getErrorCode()) ? 401 : 403;
    } else if (ex instanceof Oces3AuthorizationException) {
      return 403;
    } else if (ex instanceof FordringValidationException) {
      return 400;
    } else if (ex instanceof OpenDebtException) {
      return 500;
    }
    return 400;
  }

  private void setHttpStatus(MessageContext messageContext, int status) {
    try {
      var connection = TransportContextHolder.getTransportContext().getConnection();
      if (connection instanceof HttpServletConnection httpConn) {
        // Only set the attribute — SoapHttpStatusFilter intercepts Spring-WS's unconditional
        // setStatus(500) after the fault body is written and substitutes the desired code.
        // Calling setStatus() here would prematurely commit the response before the body
        // is written, resulting in an empty fault body for non-500 status codes (e.g. 401).
        httpConn
            .getHttpServletRequest()
            .setAttribute(SoapHttpStatusFilter.DESIRED_STATUS_ATTR, status);
      }
    } catch (Exception ignored) {
    }
  }
}
