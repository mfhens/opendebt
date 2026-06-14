package dk.ufst.opendebt.gateway.fogedret;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/external/v1/fogedret")
@RequiredArgsConstructor
public class FogedretCallbackController {

  private final FogedretReplayGuard replayGuard;
  private final AttachmentGatewayClient attachmentGatewayClient;

  @PostMapping("/attachment-callbacks")
  public ResponseEntity<Void> handleCallback(@RequestBody FogedretAttachmentCallbackRequest request) {
    replayGuard.assertNotReplay(request);
    attachmentGatewayClient.callbackToDebtService(request);
    return ResponseEntity.accepted().build();
  }

  @PostMapping("/attachment-dispatch")
  public ResponseEntity<Void> handleDispatch(@RequestBody FogedretAttachmentDispatchRequest request) {
    attachmentGatewayClient.dispatchToDebtService(request);
    return ResponseEntity.accepted().build();
  }
}
