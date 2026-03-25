package dk.ufst.opendebt.gateway.soap.config;

import java.util.List;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurerAdapter;
import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.soap.saaj.SaajSoapMessageFactory;
import org.springframework.ws.transport.context.TransportContextHolder;
import org.springframework.ws.transport.http.HttpServletConnection;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.SimpleWsdl11Definition;

import dk.ufst.opendebt.gateway.soap.interceptor.ClsSoapAuditInterceptor;
import dk.ufst.opendebt.gateway.soap.interceptor.Oces3SoapSecurityInterceptor;

@EnableWs
@Configuration
public class SoapConfig extends WsConfigurerAdapter {

  private final ClsSoapAuditInterceptor clsAuditInterceptor;
  private final Oces3SoapSecurityInterceptor oces3SecurityInterceptor;

  public SoapConfig(
      ClsSoapAuditInterceptor clsAuditInterceptor,
      Oces3SoapSecurityInterceptor oces3SecurityInterceptor) {
    this.clsAuditInterceptor = clsAuditInterceptor;
    this.oces3SecurityInterceptor = oces3SecurityInterceptor;
  }

  @Bean
  public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(
      ApplicationContext applicationContext) {
    MessageDispatcherServlet servlet = new MessageDispatcherServlet();
    servlet.setApplicationContext(applicationContext);
    servlet.setTransformWsdlLocations(true);
    return new ServletRegistrationBean<>(servlet, "/soap/*");
  }

  // Bean name must match URL path segment: "oio" -> /soap/oio?wsdl, "skat" -> /soap/skat?wsdl
  @Bean(name = "oio")
  public SimpleWsdl11Definition oioWsdl() {
    return new SimpleWsdl11Definition(new ClassPathResource("wsdl/oio/oio-fordring.wsdl"));
  }

  @Bean(name = "skat")
  public SimpleWsdl11Definition skatWsdl() {
    return new SimpleWsdl11Definition(new ClassPathResource("wsdl/skat/skat-fordring.wsdl"));
  }

  @Bean
  public SaajSoapMessageFactory messageFactory() throws jakarta.xml.soap.SOAPException {
    // Hybrid MessageFactory for dual SOAP 1.1 / SOAP 1.2 support (SPEC-019-01).
    // ThreadLocal tracks the current request protocol so createMessage() (no-arg, used for
    // creating outgoing responses/faults) creates a message of the SAME version as the request.
    jakarta.xml.soap.MessageFactory dynamic =
        jakarta.xml.soap.MessageFactory.newInstance(
            jakarta.xml.soap.SOAPConstants.DYNAMIC_SOAP_PROTOCOL);
    jakarta.xml.soap.MessageFactory soap11 =
        jakarta.xml.soap.MessageFactory.newInstance("SOAP 1.1 Protocol");
    jakarta.xml.soap.MessageFactory soap12 =
        jakarta.xml.soap.MessageFactory.newInstance("SOAP 1.2 Protocol");
    ThreadLocal<jakarta.xml.soap.MessageFactory> current = new ThreadLocal<>();

    jakarta.xml.soap.MessageFactory hybrid =
        new jakarta.xml.soap.MessageFactory() {
          @Override
          public jakarta.xml.soap.SOAPMessage createMessage()
              throws jakarta.xml.soap.SOAPException {
            // Respond in the same protocol as the current request (tracked via ThreadLocal)
            jakarta.xml.soap.MessageFactory f = current.get();
            return (f != null ? f : soap11).createMessage();
          }

          @Override
          public jakarta.xml.soap.SOAPMessage createMessage(
              jakarta.xml.soap.MimeHeaders headers, java.io.InputStream in)
              throws java.io.IOException, jakarta.xml.soap.SOAPException {
            // Inject Content-Type only for SOAP 1.2 so DYNAMIC factory detects the protocol.
            // SOAP 1.1 (text/xml) works fine with empty headers; don't modify those to avoid
            // changing DYNAMIC's default parsing behavior.
            boolean isSoap12 = false;
            try {
              var tc = TransportContextHolder.getTransportContext();
              if (tc != null && tc.getConnection() instanceof HttpServletConnection hc) {
                String ct = hc.getHttpServletRequest().getContentType();
                if (ct != null && ct.contains("application/soap+xml")) {
                  headers.setHeader("Content-Type", ct);
                  isSoap12 = true;
                }
              }
            } catch (Exception ignored) {
            }
            current.set(isSoap12 ? soap12 : soap11);
            return dynamic.createMessage(headers, in);
          }
        };

    return new SaajSoapMessageFactory(hybrid);
  }

  // Interceptor order: CLS audit first (always fires, even on auth failures), then OCES3 security
  @Override
  public void addInterceptors(List<EndpointInterceptor> interceptors) {
    interceptors.add(clsAuditInterceptor);
    interceptors.add(oces3SecurityInterceptor);
  }
}
