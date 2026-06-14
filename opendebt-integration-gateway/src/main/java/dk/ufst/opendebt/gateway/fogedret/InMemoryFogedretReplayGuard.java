package dk.ufst.opendebt.gateway.fogedret;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class InMemoryFogedretReplayGuard implements FogedretReplayGuard {

  private final Set<String> processedCallbacks = ConcurrentHashMap.newKeySet();

  @Override
  public void assertNotReplay(FogedretAttachmentCallbackRequest request) {
    String key = request.getWorkflowReference() + "|" + request.getOutcomeDate() + "|" + request.getCallbackMessageId();
    if (!processedCallbacks.add(key)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Callback replay detected");
    }
  }
}
