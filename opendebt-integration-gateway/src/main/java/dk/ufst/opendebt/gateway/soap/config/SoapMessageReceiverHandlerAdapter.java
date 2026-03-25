package dk.ufst.opendebt.gateway.soap.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ws.InvalidXmlException;
import org.springframework.ws.WebServiceMessageFactory;
import org.springframework.ws.transport.http.WebServiceMessageReceiverHandlerAdapter;

/**
 * Extends Spring-WS{@link WebServiceMessageReceiverHandlerAdapter} to return a proper SOAP 1.1
 * fault when a malformed or non-SOAP XML message is submitted.
 *
 * <p>Bean name {@code messageReceiverHandlerAdapter} matches {@code
 * MessageDispatcherServlet.DEFAULT_MESSAGE_RECEIVER_HANDLER_ADAPTER_BEAN_NAME}.
 *
 * <p>Traceability: SPEC-019-06 (malformed XML -> Client.SchemaValidation fault)
 */
@Component("messageReceiverHandlerAdapter")
public class SoapMessageReceiverHandlerAdapter extends WebServiceMessageReceiverHandlerAdapter {

  private static final String SOAP_11_FAULT_TEMPLATE =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
          + "<SOAP-ENV:Envelope"
          + " xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">"
          + "<SOAP-ENV:Body>"
          + "<SOAP-ENV:Fault>"
          + "<faultcode>SOAP-ENV:Client</faultcode>"
          + "<faultstring xml:lang=\"da\">"
          + "Ugyldig SOAP-meddelelse: skemavalidering fejlede"
          + "</faultstring>"
          + "</SOAP-ENV:Fault>"
          + "</SOAP-ENV:Body>"
          + "</SOAP-ENV:Envelope>";

  @Autowired
  public SoapMessageReceiverHandlerAdapter(WebServiceMessageFactory messageFactory) {
    setMessageFactory(messageFactory);
  }

  @Override
  protected void handleInvalidXmlException(
      HttpServletRequest request,
      HttpServletResponse response,
      Object handler,
      InvalidXmlException ex)
      throws Exception {
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    response.setContentType("text/xml;charset=UTF-8");
    response.getWriter().write(SOAP_11_FAULT_TEMPLATE);
    response.getWriter().flush();
  }
}
