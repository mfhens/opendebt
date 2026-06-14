package dk.ufst.opendebt.gateway.fogedret;

public interface FogedretReplayGuard {
  void assertNotReplay(FogedretAttachmentCallbackRequest request);
}
