package dk.ufst.opendebt.gateway.soap.oio;

import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import dk.ufst.opendebt.common.dto.soap.ClaimSubmissionResponse;
import dk.ufst.opendebt.common.dto.soap.FordringSubmitRequest;
import dk.ufst.opendebt.gateway.soap.DebtServiceSoapClient;
import dk.ufst.opendebt.gateway.soap.oio.generated.MFFordringIndberet_IRequest;
import dk.ufst.opendebt.gateway.soap.oio.generated.MFFordringIndberet_IResponse;

@Endpoint
public class OIOFordringIndberetEndpoint {
  private static final String NAMESPACE = "urn:oio:skat:efi:ws:1.0.1";
  private final DebtServiceSoapClient debtServiceSoapClient;
  private final OioClaimMapper mapper;

  public OIOFordringIndberetEndpoint(
      DebtServiceSoapClient debtServiceSoapClient, OioClaimMapper mapper) {
    this.debtServiceSoapClient = debtServiceSoapClient;
    this.mapper = mapper;
  }

  @PayloadRoot(namespace = NAMESPACE, localPart = "MFFordringIndberet_IRequest")
  @ResponsePayload
  public MFFordringIndberet_IResponse submitClaim(
      @RequestPayload MFFordringIndberet_IRequest request, MessageContext messageContext) {
    String fordringshaverId = (String) messageContext.getProperty("fordringshaverId");
    String correlationId = (String) messageContext.getProperty("correlationId");
    FordringSubmitRequest submitRequest = mapper.toSubmitRequest(request);
    ClaimSubmissionResponse response =
        debtServiceSoapClient.submitClaim(submitRequest, fordringshaverId, correlationId);
    return mapper.toSubmitResponse(response);
  }
}
