package dk.ufst.opendebt.gateway.soap.oio;

import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import dk.ufst.opendebt.common.dto.soap.KvitteringResponse;
import dk.ufst.opendebt.gateway.soap.DebtServiceSoapClient;
import dk.ufst.opendebt.gateway.soap.oio.generated.MFKvitteringHent_IRequest;
import dk.ufst.opendebt.gateway.soap.oio.generated.MFKvitteringHent_IResponse;

@Endpoint
public class OIOKvitteringHentEndpoint {
  private static final String NAMESPACE = "urn:oio:skat:efi:ws:1.0.1";
  private final DebtServiceSoapClient debtServiceSoapClient;
  private final OioClaimMapper mapper;

  public OIOKvitteringHentEndpoint(
      DebtServiceSoapClient debtServiceSoapClient, OioClaimMapper mapper) {
    this.debtServiceSoapClient = debtServiceSoapClient;
    this.mapper = mapper;
  }

  @PayloadRoot(namespace = NAMESPACE, localPart = "MFKvitteringHent_IRequest")
  @ResponsePayload
  public MFKvitteringHent_IResponse getReceipt(
      @RequestPayload MFKvitteringHent_IRequest request, MessageContext messageContext) {
    String fordringshaverId = (String) messageContext.getProperty("fordringshaverId");
    String correlationId = (String) messageContext.getProperty("correlationId");
    String claimId = mapper.toClaimId(request);
    KvitteringResponse kvittering =
        debtServiceSoapClient.getReceipt(claimId, fordringshaverId, correlationId);
    return mapper.toReceiptResponse(kvittering);
  }
}
