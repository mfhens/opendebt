package dk.ufst.opendebt.gateway.soap.oio;

import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import dk.ufst.opendebt.common.dto.soap.NotificationCollectionResult;
import dk.ufst.opendebt.gateway.soap.DebtServiceSoapClient;
import dk.ufst.opendebt.gateway.soap.oio.generated.MFUnderretSamlingHent_IRequest;
import dk.ufst.opendebt.gateway.soap.oio.generated.MFUnderretSamlingHent_IResponse;

@Endpoint
public class OIOUnderretSamlingHentEndpoint {
  private static final String NAMESPACE = "urn:oio:skat:efi:ws:1.0.1";
  private final DebtServiceSoapClient debtServiceSoapClient;
  private final OioClaimMapper mapper;

  public OIOUnderretSamlingHentEndpoint(
      DebtServiceSoapClient debtServiceSoapClient, OioClaimMapper mapper) {
    this.debtServiceSoapClient = debtServiceSoapClient;
    this.mapper = mapper;
  }

  @PayloadRoot(namespace = NAMESPACE, localPart = "MFUnderretSamlingHent_IRequest")
  @ResponsePayload
  public MFUnderretSamlingHent_IResponse getNotifications(
      @RequestPayload MFUnderretSamlingHent_IRequest request, MessageContext messageContext) {
    String fordringshaverId = (String) messageContext.getProperty("fordringshaverId");
    String correlationId = (String) messageContext.getProperty("correlationId");
    String claimId = mapper.toClaimId(request);
    String debtorId = mapper.toDebtorId(request);
    NotificationCollectionResult result =
        debtServiceSoapClient.getNotifications(claimId, debtorId, fordringshaverId, correlationId);
    return mapper.toNotificationResponse(result);
  }
}
