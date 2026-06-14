package dk.ufst.opendebt.gateway.fogedret;

public interface AttachmentGatewayClient {
  void dispatchToDebtService(FogedretAttachmentDispatchRequest request);

  void callbackToDebtService(FogedretAttachmentCallbackRequest request);
}
