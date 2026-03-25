package dk.ufst.opendebt.gateway.soap.skat;

import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import dk.ufst.opendebt.common.dto.soap.NotificationCollectionResult;
import dk.ufst.opendebt.gateway.soap.DebtServiceSoapClient;
import dk.ufst.opendebt.gateway.soap.skat.generated.MFUnderretSamlingHent_IRequest;
import dk.ufst.opendebt.gateway.soap.skat.generated.MFUnderretSamlingHent_IResponse;

@Endpoint
public class SkatUnderretSamlingHentEndpoint {
  private static final String NAMESPACE = "http://skat.dk/begrebsmodel/2009/01/15/";
  private final DebtServiceSoapClient debtServiceSoapClient;
  private final SkatClaimMapper mapper;

  public SkatUnderretSamlingHentEndpoint(
      DebtServiceSoapClient debtServiceSoapClient, SkatClaimMapper mapper) {
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
