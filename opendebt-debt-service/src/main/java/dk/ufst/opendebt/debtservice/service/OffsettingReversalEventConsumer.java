package dk.ufst.opendebt.debtservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Consumer for OffsettingReversalEvent — triggers FR-3 korrektionspulje workflow (SPEC-058 §3.3).
 *
 * <p>Listens for reversal events and delegates to {@link KorrektionspuljeService}.
 */
@Component
public class OffsettingReversalEventConsumer {

  private static final Logger log = LoggerFactory.getLogger(OffsettingReversalEventConsumer.class);

  private final KorrektionspuljeService korrektionspuljeService;

  public OffsettingReversalEventConsumer(KorrektionspuljeService korrektionspuljeService) {
    this.korrektionspuljeService = korrektionspuljeService;
  }

  @EventListener
  public void onOffsettingReversalEvent(OffsettingReversalEvent event) {
    log.info(
        "Processing OffsettingReversalEvent for debtorPersonId={}, originEventId={}",
        event.debtorPersonId(),
        event.originModregningEventId());
    korrektionspuljeService.processReversal(event);
  }
}
