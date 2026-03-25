package dk.ufst.opendebt.gateway.soap.skat;

import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import dk.ufst.opendebt.common.dto.soap.ClaimSubmissionResponse;
import dk.ufst.opendebt.common.dto.soap.FordringSubmitRequest;
import dk.ufst.opendebt.gateway.soap.DebtServiceSoapClient;
import dk.ufst.opendebt.gateway.soap.skat.generated.MFFordringIndberet_IRequest;
import dk.ufst.opendebt.gateway.soap.skat.generated.MFFordringIndberet_IResponse;

@Endpoint
public class SkatFordringIndberetEndpoint {
  private static final String NAMESPACE = "http://skat.dk/begrebsmodel/2009/01/15/";
  private final DebtServiceSoapClient debtServiceSoapClient;
  private final SkatClaimMapper mapper;

  public SkatFordringIndberetEndpoint(
      DebtServiceSoapClient debtServiceSoapClient, SkatClaimMapper mapper) {
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
